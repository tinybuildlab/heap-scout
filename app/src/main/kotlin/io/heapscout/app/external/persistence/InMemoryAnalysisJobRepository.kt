package io.heapscout.app.external.persistence

import io.heapscout.app.domain.model.AnalysisJob
import io.heapscout.app.domain.port.AnalysisJobRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryAnalysisJobRepository : AnalysisJobRepository {
    private val jobs = ConcurrentHashMap<UUID, AnalysisJob>()
    private val capacityLock = Any()

    override fun createIfCapacity(
        job: AnalysisJob,
        maximumActiveJobs: Int,
        maximumRetainedJobs: Int,
    ): Boolean = synchronized(capacityLock) {
        require(maximumActiveJobs > 0) { "maximumActiveJobs must be greater than zero" }
        require(maximumRetainedJobs >= maximumActiveJobs) {
            "maximumRetainedJobs must be greater than or equal to maximumActiveJobs"
        }
        if (jobs.values.count(AnalysisJob::isActive) >= maximumActiveJobs) {
            false
        } else {
            val removalCount = (jobs.size - maximumRetainedJobs + 1).coerceAtLeast(0)
            jobs.values
                .asSequence()
                .filterNot(AnalysisJob::isActive)
                .sortedWith(compareBy<AnalysisJob>(AnalysisJob::createdAt).thenBy(AnalysisJob::id))
                .take(removalCount)
                .forEach { jobs.remove(it.id, it) }
            jobs[job.id] = job
            true
        }
    }

    override fun findById(id: UUID): AnalysisJob? = jobs[id]

    override fun findRecent(limit: Int): List<AnalysisJob> {
        require(limit > 0) { "limit must be greater than zero" }
        return jobs.values
            .sortedWith(compareByDescending<AnalysisJob>(AnalysisJob::createdAt).thenByDescending(AnalysisJob::id))
            .take(limit)
    }

    override fun update(id: UUID, updater: (AnalysisJob) -> AnalysisJob): AnalysisJob? =
        jobs.computeIfPresent(id) { _, current -> updater(current) }

    override fun remove(id: UUID): AnalysisJob? = jobs.remove(id)
}
