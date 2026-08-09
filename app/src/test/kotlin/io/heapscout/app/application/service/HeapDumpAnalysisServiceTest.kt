package io.heapscout.app.application.service

import io.heapscout.app.application.port.AnalysisTaskPort
import io.heapscout.app.domain.model.AnalysisJobStatus
import io.heapscout.app.external.persistence.InMemoryAnalysisJobRepository
import io.heapscout.engine.ClassHistogramEntry
import io.heapscout.engine.HeapAnalysis
import io.heapscout.engine.HeapAnalysisProgressListener
import io.heapscout.engine.HeapDumpAnalyzer
import io.heapscout.engine.HeapSummary
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@DisplayName("HeapDumpAnalysisService: 로컬 힙 덤프 분석 작업 관리")
class HeapDumpAnalysisServiceTest {
    private lateinit var sut: HeapDumpAnalysisService
    private lateinit var repository: InMemoryAnalysisJobRepository
    private lateinit var analyzer: FakeHeapDumpAnalyzer
    private lateinit var taskPort: ImmediateAnalysisTaskPort

    @TempDir
    lateinit var temporaryDirectory: Path

    @BeforeEach
    fun setUp() {
        repository = InMemoryAnalysisJobRepository()
        analyzer = FakeHeapDumpAnalyzer()
        taskPort = ImmediateAnalysisTaskPort()
        sut = service(taskPort)
    }

    // ═══════════════════════════════════════════
    // 정책 1: 분석 작업 — 유효 경로는 완료된 분석으로 전이한다
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("정책: 유효한 로컬 파일은 백그라운드 분석 작업으로 실행해야 한다")
    inner class AnalysisJobPolicy {
        @Test
        @DisplayName("읽을 수 있는 HPROF 경로이면 작업 상태는 COMPLETED이고 분석 결과가 저장된다")
        fun start_readableHeapDump_completesAndStoresAnalysis() {
            val heapDump = Files.write(temporaryDirectory.resolve("valid.hprof"), byteArrayOf(1, 2, 3))

            val started = sut.start(heapDump.toString())
            val completed = sut.get(started.id)

            assertEquals(AnalysisJobStatus.COMPLETED, completed.status)
            assertNotNull(completed.analysis)
            assertEquals(heapDump, completed.analysis.summary.source)
        }

        @Test
        @DisplayName("존재하지 않는 경로이면 InvalidHeapDumpPathException이 발생하고 작업은 생성되지 않는다")
        fun start_missingHeapDump_throwsInvalidPath() {
            val missing = temporaryDirectory.resolve("missing.hprof")

            assertFailsWith<InvalidHeapDumpPathException> {
                sut.start(missing.toString())
            }
        }
    }

    // ═══════════════════════════════════════════
    // 정책 2: 자원 경계 — 활성 분석은 최대 2개만 허용한다
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("정책: 큐 대기와 실행을 합쳐 활성 분석 작업은 2개를 넘으면 안 된다")
    inner class CapacityPolicy {
        @Test
        @DisplayName("작업 2개가 대기 중일 때 세 번째 요청은 AnalysisCapacityExceededException이 발생한다")
        fun start_twoActiveJobs_rejectsThirdJob() {
            val holdingTaskPort = HoldingAnalysisTaskPort()
            sut = service(holdingTaskPort)
            val first = Files.write(temporaryDirectory.resolve("first.hprof"), byteArrayOf(1))
            val second = Files.write(temporaryDirectory.resolve("second.hprof"), byteArrayOf(2))
            val third = Files.write(temporaryDirectory.resolve("third.hprof"), byteArrayOf(3))
            sut.start(first.toString())
            sut.start(second.toString())

            assertFailsWith<AnalysisCapacityExceededException> {
                sut.start(third.toString())
            }
        }
    }

    // ═══════════════════════════════════════════
    // 정책 3: 히스토그램 — 검색과 정렬 뒤에 페이지를 적용한다
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("정책: 클래스 히스토그램은 검색한 전체 집합을 정렬한 뒤 페이지로 잘라야 한다")
    inner class HistogramPolicy {
        @Test
        @DisplayName("name:cache를 크기 내림차순으로 조회하면 BigCache 다음 SmallCache 순서이다")
        fun histogram_cacheQuery_sortsByShallowBytesDescending() {
            val heapDump = Files.write(temporaryDirectory.resolve("histogram.hprof"), byteArrayOf(1))
            val job = sut.start(heapDump.toString())

            val page = sut.histogram(
                jobId = job.id,
                query = "name:cache",
                sort = HistogramSort.SHALLOW_BYTES,
                direction = SortDirection.DESCENDING,
                page = 0,
                pageSize = 20,
            )

            assertEquals(listOf("com.example.BigCache", "com.example.SmallCache"), page.items.map { it.className })
        }
    }

    private fun service(analysisTaskPort: AnalysisTaskPort): HeapDumpAnalysisService = HeapDumpAnalysisService(
        analyzer = analyzer,
        repository = repository,
        taskPort = analysisTaskPort,
        clock = Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC),
    )

    private class ImmediateAnalysisTaskPort : AnalysisTaskPort {
        override fun submit(jobId: UUID, task: () -> Unit) {
            task()
        }

        override fun cancel(jobId: UUID): Boolean = false
    }

    private class HoldingAnalysisTaskPort : AnalysisTaskPort {
        override fun submit(jobId: UUID, task: () -> Unit) = Unit

        override fun cancel(jobId: UUID): Boolean = true
    }

    private class FakeHeapDumpAnalyzer : HeapDumpAnalyzer {
        override fun analyze(
            path: Path,
            progressListener: HeapAnalysisProgressListener,
        ): HeapAnalysis {
            val histogram = listOf(
                ClassHistogramEntry("com.example.BigCache", 100, 2_000, false),
                ClassHistogramEntry("com.example.SmallCache", 20, 500, false),
                ClassHistogramEntry("java.lang.String", 1_000, 1_000, false),
            )
            return HeapAnalysis(
                summary = HeapSummary(
                    source = path,
                    fileSizeBytes = Files.size(path),
                    format = "JAVA PROFILE 1.0.2",
                    identifierSizeBytes = 4,
                    capturedAt = Instant.EPOCH,
                    objectCount = histogram.sumOf(ClassHistogramEntry::instanceCount),
                    classCount = histogram.size,
                    shallowHeapBytes = histogram.sumOf(ClassHistogramEntry::shallowHeapBytes),
                    containsEstimatedSizes = false,
                    parseDurationMillis = 1,
                ),
                histogram = histogram,
            )
        }
    }
}
