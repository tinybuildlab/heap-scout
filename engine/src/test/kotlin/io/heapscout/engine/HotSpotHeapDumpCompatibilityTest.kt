package io.heapscout.engine

import com.sun.management.HotSpotDiagnosticMXBean
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.junit.jupiter.api.io.TempDir
import java.lang.management.ManagementFactory
import java.nio.file.Path
import kotlin.test.assertTrue

@DisplayName("HprofStreamingAnalyzer: 실제 HotSpot 힙 덤프 호환성")
@EnabledIfSystemProperty(named = "heapscout.runHprofIntegration", matches = "true")
class HotSpotHeapDumpCompatibilityTest {
    private lateinit var sut: HprofStreamingAnalyzer

    @TempDir
    lateinit var temporaryDirectory: Path

    @BeforeEach
    fun setUp() {
        sut = HprofStreamingAnalyzer()
    }

    // ═══════════════════════════════════════════
    // 정책 1: 실제 JVM 호환 — 현재 HotSpot이 생성한 덤프를 끝까지 읽는다
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("정책: 현재 HotSpot JVM이 생성한 HPROF를 합성 fixture와 동일한 파서로 분석해야 한다")
    inner class CurrentHotSpotPolicy {
        @Test
        @DisplayName("HotSpotDiagnosticMXBean 덤프이면 객체와 java.lang.String 클래스가 존재한다")
        fun analyze_currentHotSpotDump_returnsNonEmptyHistogram() {
            val heapDump = temporaryDirectory.resolve("hotspot.hprof")
            val diagnostic = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean::class.java)
            diagnostic.dumpHeap(heapDump.toString(), false)

            val analysis = sut.analyze(heapDump, HeapAnalysisProgressListener.NO_OP)

            assertTrue(analysis.summary.objectCount > 0)
            assertTrue(analysis.histogram.any { it.className == "java.lang.String" })
        }
    }
}
