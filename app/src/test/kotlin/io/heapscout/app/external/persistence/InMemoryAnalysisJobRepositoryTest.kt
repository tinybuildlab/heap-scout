package io.heapscout.app.external.persistence

import io.heapscout.app.domain.model.AnalysisJob
import io.heapscout.app.domain.model.AnalysisJobStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("InMemoryAnalysisJobRepository: 분석 결과 보관 경계")
class InMemoryAnalysisJobRepositoryTest {
    private lateinit var sut: InMemoryAnalysisJobRepository

    @BeforeEach
    fun setUp() {
        sut = InMemoryAnalysisJobRepository()
    }

    @Nested
    @DisplayName("정책: 보관 상한을 넘으면 가장 오래된 비활성 작업부터 제거해야 한다")
    inner class RetentionPolicy {
        @Test
        @DisplayName("완료 작업이 3개일 때 네 번째 작업을 만들면 가장 오래된 완료 작업만 제거한다")
        fun createIfCapacity_retentionLimit_evictsOldestInactiveJob() {
            val first = job(1, AnalysisJobStatus.COMPLETED)
            val second = job(2, AnalysisJobStatus.COMPLETED)
            val third = job(3, AnalysisJobStatus.COMPLETED)
            val fourth = job(4, AnalysisJobStatus.COMPLETED)
            sut.createIfCapacity(first, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)
            sut.createIfCapacity(second, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)
            sut.createIfCapacity(third, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)

            val created = sut.createIfCapacity(fourth, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)

            assertTrue(created)
            assertNull(sut.findById(first.id))
            assertEquals(listOf(fourth.id, third.id, second.id), sut.findRecent(MAXIMUM_RETAINED_JOBS).map { it.id })
        }

        @Test
        @DisplayName("보관 상한을 정리할 때 실행 중인 작업은 오래되어도 제거하지 않는다")
        fun createIfCapacity_activeOldJob_preservesActiveJob() {
            val active = job(1, AnalysisJobStatus.RUNNING)
            val oldestCompleted = job(2, AnalysisJobStatus.COMPLETED)
            val newestCompleted = job(3, AnalysisJobStatus.COMPLETED)
            val nextActive = job(4, AnalysisJobStatus.QUEUED)
            sut.createIfCapacity(active, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)
            sut.createIfCapacity(oldestCompleted, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)
            sut.createIfCapacity(newestCompleted, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)

            val created = sut.createIfCapacity(nextActive, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)

            assertTrue(created)
            assertEquals(active, sut.findById(active.id))
            assertNull(sut.findById(oldestCompleted.id))
        }
    }

    @Nested
    @DisplayName("정책: 활성 작업 상한은 생성과 같은 임계 구역에서 검사해야 한다")
    inner class CapacityPolicy {
        @Test
        @DisplayName("활성 작업이 이미 2개이면 새 작업을 저장하지 않는다")
        fun createIfCapacity_twoActiveJobs_rejectsNewJob() {
            val first = job(1, AnalysisJobStatus.RUNNING)
            val second = job(2, AnalysisJobStatus.QUEUED)
            val rejected = job(3, AnalysisJobStatus.QUEUED)
            sut.createIfCapacity(first, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)
            sut.createIfCapacity(second, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)

            val created = sut.createIfCapacity(rejected, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)

            assertFalse(created)
            assertNull(sut.findById(rejected.id))
        }
    }

    private fun job(sequence: Int, status: AnalysisJobStatus): AnalysisJob = AnalysisJob(
        id = UUID(0, sequence.toLong()),
        source = Path.of("dump-$sequence.hprof"),
        status = status,
        phase = null,
        processedBytes = 0,
        totalBytes = 1,
        createdAt = Instant.EPOCH.plusSeconds(sequence.toLong()),
        updatedAt = Instant.EPOCH.plusSeconds(sequence.toLong()),
    )

    private companion object {
        const val MAXIMUM_ACTIVE_JOBS = 2
        const val MAXIMUM_RETAINED_JOBS = 3
    }
}
