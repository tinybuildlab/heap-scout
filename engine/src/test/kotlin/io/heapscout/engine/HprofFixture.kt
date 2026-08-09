package io.heapscout.engine

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

internal object HprofFixture {
    private const val IDENTIFIER_SIZE = 4

    fun writeValid(path: Path) {
        DataOutputStream(Files.newOutputStream(path)).use { output ->
            output.write("JAVA PROFILE 1.0.2".toByteArray(StandardCharsets.US_ASCII))
            output.writeByte(0)
            output.writeInt(IDENTIFIER_SIZE)
            output.writeLong(1_786_275_600_000L)

            output.writeRecord(0x01) {
                writeInt(101)
                write("com/example/CacheEntry".toByteArray(StandardCharsets.UTF_8))
            }
            output.writeRecord(0x02) {
                writeInt(1)
                writeInt(201)
                writeInt(0)
                writeInt(101)
            }
            output.writeRecord(0x1c) {
                writeClassDump(classObjectId = 201, instanceSize = 24)
                writeInstanceDump(objectId = 301, classObjectId = 201, byteArrayOf(0, 0, 0, 1))
                writeInstanceDump(objectId = 302, classObjectId = 201, byteArrayOf(0, 0, 0, 2))
                writePrimitiveByteArray(objectId = 401, byteArrayOf(1, 2, 3, 4, 5))
            }
            output.writeRecord(0x2c) { }
        }
    }

    fun writeInvalidHeader(path: Path) {
        Files.write(path, "NOT HPROF\u0000".toByteArray(StandardCharsets.US_ASCII))
    }

    fun writeTruncatedRecord(path: Path) {
        DataOutputStream(Files.newOutputStream(path)).use { output ->
            output.write("JAVA PROFILE 1.0.2".toByteArray(StandardCharsets.US_ASCII))
            output.writeByte(0)
            output.writeInt(IDENTIFIER_SIZE)
            output.writeLong(0L)
            output.writeByte(0x01)
            output.writeInt(0)
            output.writeInt(100)
            output.writeInt(1)
        }
    }

    private fun DataOutputStream.writeRecord(tag: Int, bodyWriter: DataOutputStream.() -> Unit) {
        val bodyBytes = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { body -> body.bodyWriter() }
            bytes.toByteArray()
        }
        writeByte(tag)
        writeInt(0)
        writeInt(bodyBytes.size)
        write(bodyBytes)
    }

    private fun DataOutputStream.writeClassDump(classObjectId: Int, instanceSize: Int) {
        writeByte(0x20)
        writeInt(classObjectId)
        writeInt(0)
        repeat(6) { writeInt(0) }
        writeInt(instanceSize)
        writeShort(0)
        writeShort(0)
        writeShort(0)
    }

    private fun DataOutputStream.writeInstanceDump(
        objectId: Int,
        classObjectId: Int,
        fieldBytes: ByteArray,
    ) {
        writeByte(0x21)
        writeInt(objectId)
        writeInt(0)
        writeInt(classObjectId)
        writeInt(fieldBytes.size)
        write(fieldBytes)
    }

    private fun DataOutputStream.writePrimitiveByteArray(objectId: Int, values: ByteArray) {
        writeByte(0x23)
        writeInt(objectId)
        writeInt(0)
        writeInt(values.size)
        writeByte(8)
        write(values)
    }
}
