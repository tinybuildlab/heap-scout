package io.heapscout.engine

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisplayName("HprofStreamingAnalyzer: 스트리밍 HPROF 요약 분석")
class HprofStreamingAnalyzerTest {
    private lateinit var sut: HprofStreamingAnalyzer

    @TempDir
    lateinit var temporaryDirectory: Path

    @BeforeEach
    fun setUp() {
        sut = HprofStreamingAnalyzer()
    }

    // ═══════════════════════════════════════════
    // 정책 1: 유효 덤프 — 클래스별 개수와 크기를 집계한다
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("정책: 유효한 HotSpot HPROF를 클래스 히스토그램으로 집계해야 한다")
    inner class ValidDumpPolicy {
        @Test
        @DisplayName("24바이트 CacheEntry 2개와 길이 5 byte 배열이면 총 3개 객체와 72바이트를 반환한다")
        fun analyze_validFixture_returnsExpectedHistogramAndSummary() {
            val heapDump = temporaryDirectory.resolve("valid.hprof")
            HprofFixture.writeValid(heapDump)

            val analysis = sut.analyze(heapDump, HeapAnalysisProgressListener.NO_OP)

            assertEquals(3, analysis.summary.objectCount)
            assertEquals(2, analysis.summary.classCount)
            assertEquals(72, analysis.summary.shallowHeapBytes)
            assertTrue(analysis.summary.containsEstimatedSizes)

            val cacheEntry = analysis.histogram.single { it.className == "com.example.CacheEntry" }
            assertEquals(2, cacheEntry.instanceCount)
            assertEquals(48, cacheEntry.shallowHeapBytes)
            assertEquals(false, cacheEntry.sizeIsEstimated)

            val byteArray = analysis.histogram.single { it.className == "byte[]" }
            assertEquals(1, byteArray.instanceCount)
            assertEquals(24, byteArray.shallowHeapBytes)
            assertTrue(byteArray.sizeIsEstimated)
        }

        @Test
        @DisplayName("유효한 HPROF 분석이 끝나면 마지막 진행 상태는 COMPLETE와 전체 파일 크기이다")
        fun analyze_validFixture_emitsCompleteProgress() {
            val heapDump = temporaryDirectory.resolve("progress.hprof")
            HprofFixture.writeValid(heapDump)
            val updates = mutableListOf<ParseProgress>()

            sut.analyze(heapDump) { updates += it }

            assertEquals(ParsePhase.HEADER, updates.first().phase)
            assertEquals(ParsePhase.COMPLETE, updates.last().phase)
            assertEquals(updates.last().totalBytes, updates.last().processedBytes)
        }
    }

    // ═══════════════════════════════════════════
    // 정책 2: 손상 덤프 — 위치를 포함한 명시적 오류로 실패한다
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("정책: 손상되거나 잘린 HPROF는 조용히 부분 결과를 반환하면 안 된다")
    inner class CorruptDumpPolicy {
        @Test
        @DisplayName("JAVA PROFILE 헤더가 없으면 offset을 포함한 InvalidHprofException이 발생한다")
        fun analyze_invalidHeader_throwsOffsetAwareError() {
            val heapDump = temporaryDirectory.resolve("invalid-header.hprof")
            HprofFixture.writeInvalidHeader(heapDump)

            val failure = assertFailsWith<InvalidHprofException> {
                sut.analyze(heapDump, HeapAnalysisProgressListener.NO_OP)
            }

            assertTrue(failure.message.orEmpty().contains("offset="))
        }

        @Test
        @DisplayName("본문 길이 100인 레코드에 4바이트만 있으면 파일 크기 초과 오류가 발생한다")
        fun analyze_truncatedRecord_throwsRecordBoundaryError() {
            val heapDump = temporaryDirectory.resolve("truncated.hprof")
            HprofFixture.writeTruncatedRecord(heapDump)

            val failure = assertFailsWith<InvalidHprofException> {
                sut.analyze(heapDump, HeapAnalysisProgressListener.NO_OP)
            }

            assertTrue(failure.message.orEmpty().contains("exceeds file size"))
        }
    }
}
