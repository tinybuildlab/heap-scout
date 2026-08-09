package io.heapscout.engine

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant
import kotlin.test.assertEquals

@DisplayName("HeapComparison: 두 힙 덤프의 클래스별 변화 비교")
class HeapComparisonTest {
    private lateinit var sut: HeapAnalysis

    @BeforeEach
    fun setUp() {
        sut = analysis(
            ClassHistogramEntry("com.example.Cache", 100, 1_000, false),
            ClassHistogramEntry("com.example.Removed", 10, 500, false),
        )
    }

    // ═══════════════════════════════════════════
    // 정책 1: 델타 계산 — 신규·증가·삭제 클래스를 모두 보존한다
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("정책: baseline과 target 중 한쪽에만 존재하는 클래스도 0 기준으로 비교해야 한다")
    inner class DeltaPolicy {
        @Test
        @DisplayName("Cache가 1,000→3,000바이트이면 +2,000이고 삭제 클래스는 -500이다")
        fun compare_addedAndRemovedClasses_returnsSignedDeltas() {
            val target = analysis(
                ClassHistogramEntry("com.example.Cache", 250, 3_000, false),
                ClassHistogramEntry("com.example.Added", 3, 800, true),
            )

            val changes = HeapComparison.compare(sut, target)

            assertEquals(2_000, changes.single { it.className.endsWith("Cache") }.shallowHeapBytesDelta)
            assertEquals(-500, changes.single { it.className.endsWith("Removed") }.shallowHeapBytesDelta)
            assertEquals(800, changes.single { it.className.endsWith("Added") }.shallowHeapBytesDelta)
        }
    }

    private fun analysis(vararg entries: ClassHistogramEntry): HeapAnalysis = HeapAnalysis(
        summary = HeapSummary(
            source = Path.of("fixture.hprof"),
            fileSizeBytes = 0,
            format = "JAVA PROFILE 1.0.2",
            identifierSizeBytes = 4,
            capturedAt = Instant.EPOCH,
            objectCount = entries.sumOf(ClassHistogramEntry::instanceCount),
            classCount = entries.size,
            shallowHeapBytes = entries.sumOf(ClassHistogramEntry::shallowHeapBytes),
            containsEstimatedSizes = entries.any(ClassHistogramEntry::sizeIsEstimated),
            parseDurationMillis = 0,
        ),
        histogram = entries.toList(),
    )
}
