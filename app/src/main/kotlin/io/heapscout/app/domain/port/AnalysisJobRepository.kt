package io.heapscout.app.domain.port

import io.heapscout.app.domain.model.AnalysisJob
import java.util.UUID

interface AnalysisJobRepository {
    fun createIfCapacity(
        job: AnalysisJob,
        maximumActiveJobs: Int,
        maximumRetainedJobs: Int,
    ): Boolean

    fun findById(id: UUID): AnalysisJob?

    fun findRecent(limit: Int): List<AnalysisJob>

    fun update(id: UUID, updater: (AnalysisJob) -> AnalysisJob): AnalysisJob?

    fun remove(id: UUID): AnalysisJob?
}
