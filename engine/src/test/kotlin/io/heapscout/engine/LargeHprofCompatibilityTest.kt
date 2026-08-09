package io.heapscout.engine

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("HprofStreamingAnalyzer: 1GiB 대용량 HPROF 경계")
@EnabledIfSystemProperty(named = "heapscout.runLargeHprofIntegration", matches = "true")
class LargeHprofCompatibilityTest {
    private lateinit var sut: HprofStreamingAnalyzer

    @TempDir
    lateinit var temporaryDirectory: Path

    @BeforeEach
    fun setUp() {
        sut = HprofStreamingAnalyzer()
    }

    // ═══════════════════════════════════════════
    // 정책 1: 대용량 스트리밍 — 1GiB 레코드를 분석기 힙에 적재하지 않는다
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("정책: 파일 크기가 JVM 테스트 힙보다 커도 레코드 전체를 메모리에 적재하면 안 된다")
    inner class BoundedMemoryPolicy {
        @Test
        @DisplayName("1GiB byte 배열 HPROF를 256MiB 테스트 힙에서 분석하면 byte[] 1개로 완료된다")
        fun analyze_oneGiBSparseByteArray_completesWithinBoundedHeap() {
            val heapDump = temporaryDirectory.resolve("one-gib.hprof")
            writeSparseByteArrayDump(heapDump)

            val analysis = sut.analyze(heapDump, HeapAnalysisProgressListener.NO_OP)

            val byteArrays = analysis.histogram.single()
            assertTrue(analysis.summary.fileSizeBytes > ONE_GIB)
            assertEquals(1, analysis.summary.objectCount)
            assertEquals("byte[]", byteArrays.className)
            assertEquals(1, byteArrays.instanceCount)
            assertEquals(ONE_GIB + ARRAY_HEADER_BYTES, byteArrays.shallowHeapBytes)
        }
    }

    private fun writeSparseByteArrayDump(path: Path) {
        val heapBodyLength = PRIMITIVE_ARRAY_HEADER_BYTES + ONE_GIB
        val prefix = encode {
            write(HPROF_HEADER.toByteArray(StandardCharsets.US_ASCII))
            writeByte(0)
            writeInt(IDENTIFIER_SIZE)
            writeLong(0L)
            writeByte(HEAP_DUMP_SEGMENT_TAG)
            writeInt(0)
            writeInt(heapBodyLength.toInt())
            writeByte(PRIMITIVE_ARRAY_DUMP_TAG)
            writeLong(1L)
            writeInt(0)
            writeInt(ONE_GIB.toInt())
            writeByte(BYTE_TYPE)
        }
        val heapDumpEnd = encode {
            writeByte(HEAP_DUMP_END_TAG)
            writeInt(0)
            writeInt(0)
        }

        FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel ->
            channel.writeFully(prefix)
            val payloadEnd = channel.position() + ONE_GIB
            channel.position(payloadEnd - 1)
            channel.writeFully(byteArrayOf(0))
            channel.writeFully(heapDumpEnd)
        }
    }

    private fun FileChannel.writeFully(bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes)
        while (buffer.hasRemaining()) write(buffer)
    }

    private fun encode(writer: DataOutputStream.() -> Unit): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output -> output.writer() }
        return bytes.toByteArray()
    }

    private companion object {
        const val HPROF_HEADER = "JAVA PROFILE 1.0.2"
        const val IDENTIFIER_SIZE = 8
        const val HEAP_DUMP_SEGMENT_TAG = 0x1c
        const val HEAP_DUMP_END_TAG = 0x2c
        const val PRIMITIVE_ARRAY_DUMP_TAG = 0x23
        const val BYTE_TYPE = 8
        const val ONE_GIB = 1_073_741_824L
        const val PRIMITIVE_ARRAY_HEADER_BYTES = 18L
        const val ARRAY_HEADER_BYTES = 24L
    }
}
