package io.heapscout.engine

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.math.min

class HprofStreamingAnalyzer : HeapDumpAnalyzer {
    override fun analyze(
        path: Path,
        progressListener: HeapAnalysisProgressListener,
    ): HeapAnalysis {
        require(Files.isRegularFile(path)) { "Heap dump does not exist or is not a file: $path" }

        val startedAt = Instant.now()
        val fileSize = Files.size(path)
        val parser = Parser(path, fileSize, progressListener)
        val parsed = parser.parse()

        return HeapAnalysis(
            summary = HeapSummary(
                source = path.toAbsolutePath().normalize(),
                fileSizeBytes = fileSize,
                format = parsed.format,
                identifierSizeBytes = parsed.identifierSize,
                capturedAt = Instant.ofEpochMilli(parsed.capturedAtMillis),
                objectCount = parsed.histogram.sumOf(ClassHistogramEntry::instanceCount),
                classCount = parsed.histogram.size,
                shallowHeapBytes = parsed.histogram.sumOf(ClassHistogramEntry::shallowHeapBytes),
                containsEstimatedSizes = parsed.histogram.any(ClassHistogramEntry::sizeIsEstimated),
                parseDurationMillis = Duration.between(startedAt, Instant.now()).toMillis(),
            ),
            histogram = parsed.histogram,
        )
    }

    private data class ParsedHprof(
        val format: String,
        val identifierSize: Int,
        val capturedAtMillis: Long,
        val histogram: List<ClassHistogramEntry>,
    )

    private data class AggregateKey(
        val classObjectId: Long? = null,
        val syntheticName: String? = null,
    )

    private data class MutableAggregate(
        var count: Long = 0,
        var shallowBytes: Long = 0,
        var estimated: Boolean = false,
    )

    private class Parser(
        private val path: Path,
        private val fileSize: Long,
        private val progressListener: HeapAnalysisProgressListener,
    ) {
        private val strings = HashMap<Long, String>()
        private val classNameStringIds = HashMap<Long, Long>()
        private val classInstanceSizes = HashMap<Long, Long>()
        private val aggregates = HashMap<AggregateKey, MutableAggregate>()

        private var identifierSize = 0
        private var lastProgressOffset = -PROGRESS_INTERVAL_BYTES
        private var currentTopLevelTag: Int? = null

        fun parse(): ParsedHprof {
            HprofInput(path).use { input ->
                reportProgress(ParsePhase.HEADER, input.offset, force = true)
                val format = input.readNullTerminatedAscii(MAX_HEADER_BYTES)
                if (!format.startsWith("JAVA PROFILE ")) {
                    invalid(input, "Unsupported HPROF header: $format")
                }

                identifierSize = input.readInt()
                if (identifierSize != 4 && identifierSize != 8) {
                    invalid(input, "Identifier size must be 4 or 8 bytes, got $identifierSize")
                }
                val capturedAtMillis = input.readLong()

                while (input.offset < fileSize) {
                    checkCancellation()
                    if (fileSize - input.offset < TOP_LEVEL_RECORD_HEADER_BYTES) {
                        invalid(input, "Truncated top-level record header")
                    }

                    val tag = input.readUnsignedByte()
                    currentTopLevelTag = tag
                    input.readUnsignedInt()
                    val bodyLength = input.readUnsignedInt()
                    ensureRemaining(input, bodyLength, "Top-level record exceeds file size")
                    val bodyEnd = input.offset + bodyLength

                    when (tag) {
                        TAG_STRING -> parseString(input, bodyLength)
                        TAG_LOAD_CLASS -> parseLoadClass(input, bodyLength)
                        TAG_HEAP_DUMP, TAG_HEAP_DUMP_SEGMENT -> parseHeapSegment(input, bodyEnd)
                        else -> input.skipFully(bodyLength, ::checkCancellation)
                    }

                    if (input.offset != bodyEnd) {
                        invalid(input, "Record parser ended at ${input.offset}, expected $bodyEnd")
                    }
                    val phase = if (tag == TAG_HEAP_DUMP || tag == TAG_HEAP_DUMP_SEGMENT) {
                        ParsePhase.HEAP
                    } else {
                        ParsePhase.METADATA
                    }
                    reportProgress(phase, input.offset)
                }

                currentTopLevelTag = null
                reportProgress(ParsePhase.COMPLETE, fileSize, force = true)
                return ParsedHprof(
                    format = format,
                    identifierSize = identifierSize,
                    capturedAtMillis = capturedAtMillis,
                    histogram = buildHistogram(),
                )
            }
        }

        private fun parseString(input: HprofInput, bodyLength: Long) {
            if (bodyLength < identifierSize) invalid(input, "String record is shorter than its identifier")
            val id = input.readId(identifierSize)
            val stringLength = bodyLength - identifierSize
            strings[id] = input.readUtf8(stringLength, MAX_STRING_BYTES)
        }

        private fun parseLoadClass(input: HprofInput, bodyLength: Long) {
            val expected = 8L + (identifierSize * 2L)
            if (bodyLength != expected) invalid(input, "LOAD_CLASS length must be $expected, got $bodyLength")
            input.readUnsignedInt()
            val classObjectId = input.readId(identifierSize)
            input.readUnsignedInt()
            classNameStringIds[classObjectId] = input.readId(identifierSize)
        }

        private fun parseHeapSegment(input: HprofInput, segmentEnd: Long) {
            while (input.offset < segmentEnd) {
                checkCancellation()
                val subTagOffset = input.offset
                val subTag = input.readUnsignedByte()
                try {
                    when (subTag) {
                        ROOT_UNKNOWN,
                        ROOT_STICKY_CLASS,
                        ROOT_MONITOR_USED,
                        ROOT_INTERNED_STRING,
                        ROOT_FINALIZING,
                        ROOT_DEBUGGER,
                        ROOT_REFERENCE_CLEANUP,
                        ROOT_VM_INTERNAL,
                        ROOT_UNREACHABLE,
                        -> input.skipFully(identifierSize.toLong(), ::checkCancellation)

                        ROOT_JNI_GLOBAL -> input.skipFully(identifierSize * 2L, ::checkCancellation)
                        ROOT_JNI_LOCAL, ROOT_JAVA_FRAME -> {
                            input.skipFully(identifierSize + 8L, ::checkCancellation)
                        }

                        ROOT_NATIVE_STACK, ROOT_THREAD_BLOCK -> {
                            input.skipFully(identifierSize + 4L, ::checkCancellation)
                        }

                        ROOT_THREAD_OBJECT, ROOT_JNI_MONITOR -> {
                            input.skipFully(identifierSize + 8L, ::checkCancellation)
                        }

                        HEAP_DUMP_INFO -> input.skipFully(4L + identifierSize, ::checkCancellation)
                        CLASS_DUMP -> parseClassDump(input)
                        INSTANCE_DUMP -> parseInstanceDump(input)
                        OBJECT_ARRAY_DUMP -> parseObjectArrayDump(input)
                        PRIMITIVE_ARRAY_DUMP -> parsePrimitiveArrayDump(input, hasData = true)
                        PRIMITIVE_ARRAY_NODATA_DUMP -> parsePrimitiveArrayDump(input, hasData = false)
                        else -> invalid(input, "Unsupported heap sub-record", subTag, subTagOffset)
                    }
                } catch (exception: EOFException) {
                    invalid(input, "Truncated heap sub-record", subTag, subTagOffset, exception)
                }

                if (input.offset > segmentEnd) {
                    invalid(input, "Heap sub-record exceeds enclosing segment", subTag, subTagOffset)
                }
                reportProgress(ParsePhase.HEAP, input.offset)
            }
        }

        private fun parseClassDump(input: HprofInput) {
            val classObjectId = input.readId(identifierSize)
            input.readUnsignedInt()
            repeat(6) { input.readId(identifierSize) }
            classInstanceSizes[classObjectId] = input.readUnsignedInt()

            val constantPoolCount = input.readUnsignedShort()
            repeat(constantPoolCount) {
                input.readUnsignedShort()
                skipTypedValue(input)
            }

            val staticFieldCount = input.readUnsignedShort()
            repeat(staticFieldCount) {
                input.readId(identifierSize)
                skipTypedValue(input)
            }

            val instanceFieldCount = input.readUnsignedShort()
            repeat(instanceFieldCount) {
                input.readId(identifierSize)
                val type = input.readUnsignedByte()
                requireTypeSize(input, type)
            }
        }

        private fun parseInstanceDump(input: HprofInput) {
            input.readId(identifierSize)
            input.readUnsignedInt()
            val classObjectId = input.readId(identifierSize)
            val fieldDataLength = input.readUnsignedInt()
            val knownInstanceSize = classInstanceSizes[classObjectId]
            val shallowSize = knownInstanceSize ?: fieldDataLength
            addAggregate(
                key = AggregateKey(classObjectId = classObjectId),
                shallowBytes = shallowSize,
                estimated = knownInstanceSize == null,
            )
            input.skipFully(fieldDataLength, ::checkCancellation)
        }

        private fun parseObjectArrayDump(input: HprofInput) {
            input.readId(identifierSize)
            input.readUnsignedInt()
            val elementCount = input.readUnsignedInt()
            val arrayClassObjectId = input.readId(identifierSize)
            val payloadBytes = Math.multiplyExact(elementCount, identifierSize.toLong())
            val shallowBytes = alignedArraySize(payloadBytes)
            addAggregate(
                key = AggregateKey(classObjectId = arrayClassObjectId),
                shallowBytes = shallowBytes,
                estimated = true,
            )
            input.skipFully(payloadBytes, ::checkCancellation)
        }

        private fun parsePrimitiveArrayDump(input: HprofInput, hasData: Boolean) {
            input.readId(identifierSize)
            input.readUnsignedInt()
            val elementCount = input.readUnsignedInt()
            val elementType = input.readUnsignedByte()
            val elementSize = requirePrimitiveTypeSize(input, elementType)
            val payloadBytes = Math.multiplyExact(elementCount, elementSize.toLong())
            val typeName = primitiveTypeName(input, elementType)
            addAggregate(
                key = AggregateKey(syntheticName = "$typeName[]"),
                shallowBytes = alignedArraySize(payloadBytes),
                estimated = true,
            )
            if (hasData) input.skipFully(payloadBytes, ::checkCancellation)
        }

        private fun skipTypedValue(input: HprofInput) {
            val type = input.readUnsignedByte()
            input.skipFully(requireTypeSize(input, type).toLong(), ::checkCancellation)
        }

        private fun requireTypeSize(input: HprofInput, type: Int): Int = when (type) {
            TYPE_OBJECT -> identifierSize
            TYPE_BOOLEAN, TYPE_BYTE -> 1
            TYPE_CHAR, TYPE_SHORT -> 2
            TYPE_FLOAT, TYPE_INT -> 4
            TYPE_DOUBLE, TYPE_LONG -> 8
            else -> invalid(input, "Unknown HPROF value type $type")
        }

        private fun requirePrimitiveTypeSize(input: HprofInput, type: Int): Int {
            if (type == TYPE_OBJECT) invalid(input, "Primitive array cannot use object type")
            return requireTypeSize(input, type)
        }

        private fun primitiveTypeName(input: HprofInput, type: Int): String = when (type) {
            TYPE_BOOLEAN -> "boolean"
            TYPE_CHAR -> "char"
            TYPE_FLOAT -> "float"
            TYPE_DOUBLE -> "double"
            TYPE_BYTE -> "byte"
            TYPE_SHORT -> "short"
            TYPE_INT -> "int"
            TYPE_LONG -> "long"
            else -> invalid(input, "Unknown primitive array type $type")
        }

        private fun alignedArraySize(payloadBytes: Long): Long {
            val headerBytes = if (identifierSize == 4) 16L else 24L
            val unaligned = Math.addExact(headerBytes, payloadBytes)
            return Math.multiplyExact((Math.addExact(unaligned, 7L) / 8L), 8L)
        }

        private fun addAggregate(key: AggregateKey, shallowBytes: Long, estimated: Boolean) {
            val aggregate = aggregates.getOrPut(key) { MutableAggregate() }
            aggregate.count = Math.addExact(aggregate.count, 1L)
            aggregate.shallowBytes = Math.addExact(aggregate.shallowBytes, shallowBytes)
            aggregate.estimated = aggregate.estimated || estimated
        }

        private fun buildHistogram(): List<ClassHistogramEntry> {
            val byClassName = HashMap<String, MutableAggregate>()
            aggregates.forEach { (key, aggregate) ->
                val rawName = key.syntheticName ?: classNameStringIds[key.classObjectId]
                    ?.let(strings::get)
                    ?: "<unknown@${key.classObjectId?.toHex() ?: "synthetic"}>"
                val normalizedName = normalizeClassName(rawName)
                val combined = byClassName.getOrPut(normalizedName) { MutableAggregate() }
                combined.count = Math.addExact(combined.count, aggregate.count)
                combined.shallowBytes = Math.addExact(combined.shallowBytes, aggregate.shallowBytes)
                combined.estimated = combined.estimated || aggregate.estimated
            }

            return byClassName.map { (className, aggregate) ->
                ClassHistogramEntry(
                    className = className,
                    instanceCount = aggregate.count,
                    shallowHeapBytes = aggregate.shallowBytes,
                    sizeIsEstimated = aggregate.estimated,
                )
            }.sortedWith(
                compareByDescending<ClassHistogramEntry>(ClassHistogramEntry::shallowHeapBytes)
                    .thenBy(ClassHistogramEntry::className),
            )
        }

        private fun normalizeClassName(rawName: String): String {
            var dimensions = 0
            while (dimensions < rawName.length && rawName[dimensions] == '[') dimensions++
            if (dimensions == 0) return rawName.replace('/', '.')

            val componentDescriptor = rawName.substring(dimensions)
            val componentName = when {
                componentDescriptor.startsWith('L') && componentDescriptor.endsWith(';') -> {
                    componentDescriptor.substring(1, componentDescriptor.length - 1).replace('/', '.')
                }

                componentDescriptor.length == 1 -> descriptorPrimitiveName(componentDescriptor[0])
                else -> componentDescriptor.replace('/', '.')
            }
            return componentName + "[]".repeat(dimensions)
        }

        private fun descriptorPrimitiveName(descriptor: Char): String = when (descriptor) {
            'Z' -> "boolean"
            'C' -> "char"
            'F' -> "float"
            'D' -> "double"
            'B' -> "byte"
            'S' -> "short"
            'I' -> "int"
            'J' -> "long"
            else -> "<unknown:$descriptor>"
        }

        private fun ensureRemaining(input: HprofInput, length: Long, message: String) {
            if (length > fileSize - input.offset) invalid(input, message)
        }

        private fun reportProgress(phase: ParsePhase, offset: Long, force: Boolean = false) {
            if (force || offset - lastProgressOffset >= PROGRESS_INTERVAL_BYTES) {
                progressListener.onProgress(ParseProgress(phase, min(offset, fileSize), fileSize))
                lastProgressOffset = offset
            }
        }

        private fun checkCancellation() {
            if (Thread.currentThread().isInterrupted) throw HeapAnalysisCancelledException()
        }

        private fun invalid(
            input: HprofInput,
            message: String,
            subTag: Int? = currentTopLevelTag,
            offset: Long = input.offset,
            cause: Throwable? = null,
        ): Nothing = throw InvalidHprofException(message, offset, subTag, cause)

        private fun Long.toHex(): String = java.lang.Long.toUnsignedString(this, 16)
    }

    private class HprofInput(path: Path) : AutoCloseable {
        private val input = DataInputStream(BufferedInputStream(Files.newInputStream(path), INPUT_BUFFER_BYTES))

        var offset: Long = 0
            private set

        fun readUnsignedByte(): Int = read(1) { input.readUnsignedByte() }

        fun readUnsignedShort(): Int = read(2) { input.readUnsignedShort() }

        fun readInt(): Int = read(4) { input.readInt() }

        fun readUnsignedInt(): Long = read(4) { input.readInt().toLong() and UNSIGNED_INT_MASK }

        fun readLong(): Long = read(8) { input.readLong() }

        fun readId(identifierSize: Int): Long = when (identifierSize) {
            4 -> readUnsignedInt()
            8 -> readLong()
            else -> error("Identifier size must already be validated")
        }

        fun readNullTerminatedAscii(maxBytes: Int): String {
            val bytes = ArrayList<Byte>(32)
            repeat(maxBytes) {
                val next = readUnsignedByte()
                if (next == 0) {
                    return bytes.toByteArray().toString(StandardCharsets.US_ASCII)
                }
                bytes += next.toByte()
            }
            throw InvalidHprofException("HPROF header is not null-terminated", offset)
        }

        fun readUtf8(length: Long, maxBytes: Int): String {
            if (length > maxBytes) {
                throw InvalidHprofException("String record exceeds $maxBytes bytes", offset)
            }
            val bytes = ByteArray(length.toInt())
            try {
                input.readFully(bytes)
                offset += length
            } catch (exception: EOFException) {
                throw InvalidHprofException("Unexpected end of string record", offset, cause = exception)
            }
            return bytes.toString(StandardCharsets.UTF_8)
        }

        fun skipFully(length: Long, cancellationCheck: () -> Unit) {
            require(length >= 0) { "Skip length must not be negative" }
            var remaining = length
            while (remaining > 0) {
                cancellationCheck()
                val step = min(remaining, SKIP_CHUNK_BYTES)
                input.skipNBytes(step)
                offset += step
                remaining -= step
            }
        }

        override fun close() {
            input.close()
        }

        private inline fun <T> read(byteCount: Int, operation: () -> T): T {
            try {
                val value = operation()
                offset += byteCount
                return value
            } catch (exception: EOFException) {
                throw InvalidHprofException("Unexpected end of HPROF file", offset, cause = exception)
            }
        }
    }

    private companion object {
        const val TAG_STRING = 0x01
        const val TAG_LOAD_CLASS = 0x02
        const val TAG_HEAP_DUMP = 0x0c
        const val TAG_HEAP_DUMP_SEGMENT = 0x1c

        const val ROOT_JNI_GLOBAL = 0x01
        const val ROOT_JNI_LOCAL = 0x02
        const val ROOT_JAVA_FRAME = 0x03
        const val ROOT_NATIVE_STACK = 0x04
        const val ROOT_STICKY_CLASS = 0x05
        const val ROOT_THREAD_BLOCK = 0x06
        const val ROOT_MONITOR_USED = 0x07
        const val ROOT_THREAD_OBJECT = 0x08
        const val CLASS_DUMP = 0x20
        const val INSTANCE_DUMP = 0x21
        const val OBJECT_ARRAY_DUMP = 0x22
        const val PRIMITIVE_ARRAY_DUMP = 0x23
        const val ROOT_INTERNED_STRING = 0x89
        const val ROOT_FINALIZING = 0x8a
        const val ROOT_DEBUGGER = 0x8b
        const val ROOT_REFERENCE_CLEANUP = 0x8c
        const val ROOT_VM_INTERNAL = 0x8d
        const val ROOT_JNI_MONITOR = 0x8e
        const val ROOT_UNREACHABLE = 0x90
        const val HEAP_DUMP_INFO = 0xfe
        const val ROOT_UNKNOWN = 0xff
        const val PRIMITIVE_ARRAY_NODATA_DUMP = 0xc3

        const val TYPE_OBJECT = 2
        const val TYPE_BOOLEAN = 4
        const val TYPE_CHAR = 5
        const val TYPE_FLOAT = 6
        const val TYPE_DOUBLE = 7
        const val TYPE_BYTE = 8
        const val TYPE_SHORT = 9
        const val TYPE_INT = 10
        const val TYPE_LONG = 11

        const val MAX_HEADER_BYTES = 128
        const val MAX_STRING_BYTES = 16 * 1024 * 1024
        const val INPUT_BUFFER_BYTES = 1024 * 1024
        const val TOP_LEVEL_RECORD_HEADER_BYTES = 9L
        const val PROGRESS_INTERVAL_BYTES = 8L * 1024 * 1024
        const val SKIP_CHUNK_BYTES = 8L * 1024 * 1024
        const val UNSIGNED_INT_MASK = 0xffff_ffffL
    }
}
