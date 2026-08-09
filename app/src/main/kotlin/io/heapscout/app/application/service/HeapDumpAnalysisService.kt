package io.heapscout.app.application.service

import io.heapscout.app.application.port.AnalysisTaskPort
import io.heapscout.app.domain.model.AnalysisFailure
import io.heapscout.app.domain.model.AnalysisJob
import io.heapscout.app.domain.model.AnalysisJobStatus
import io.heapscout.app.domain.port.AnalysisJobRepository
import io.heapscout.engine.ClassHistogramEntry
import io.heapscout.engine.HeapAnalysis
import io.heapscout.engine.HeapAnalysisCancelledException
import io.heapscout.engine.HeapComparison
import io.heapscout.engine.HeapComparisonEntry
import io.heapscout.engine.HeapDumpAnalyzer
import io.heapscout.engine.HistogramSearch
import io.heapscout.engine.InvalidHprofException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class HeapDumpAnalysisService(
    private val analyzer: HeapDumpAnalyzer,
    private val repository: AnalysisJobRepository,
    private val taskPort: AnalysisTaskPort,
    private val clock: Clock,
) {
    fun start(sourceText: String): AnalysisJob {
        val source = validateSource(sourceText)
        val now = clock.instant()
        val job = AnalysisJob(
            id = UUID.randomUUID(),
            source = source,
            status = AnalysisJobStatus.QUEUED,
            phase = null,
            processedBytes = 0,
            totalBytes = Files.size(source),
            createdAt = now,
            updatedAt = now,
        )
        if (!repository.createIfCapacity(job, MAXIMUM_ACTIVE_JOBS, MAXIMUM_RETAINED_JOBS)) {
            throw AnalysisCapacityExceededException(MAXIMUM_ACTIVE_JOBS)
        }

        try {
            taskPort.submit(job.id) { analyze(job) }
        } catch (exception: RuntimeException) {
            repository.remove(job.id)
            throw exception
        }
        return job
    }

    fun get(jobId: UUID): AnalysisJob = repository.findById(jobId)
        ?: throw AnalysisJobNotFoundException(jobId)

    fun recent(): List<AnalysisJob> = repository.findRecent(MAXIMUM_RETAINED_JOBS)

    fun close(jobId: UUID) {
        val job = get(jobId)
        if (job.isActive) taskPort.cancel(jobId)
        repository.remove(jobId)
    }

    fun histogram(
        jobId: UUID,
        query: String,
        sort: HistogramSort,
        direction: SortDirection,
        page: Int,
        pageSize: Int,
    ): PageSlice<ClassHistogramEntry> {
        val analysis = completedAnalysis(jobId)
        val matches = HistogramSearch.search(analysis.histogram, query)
        val comparator = when (sort) {
            HistogramSort.SHALLOW_BYTES -> compareBy<ClassHistogramEntry>(ClassHistogramEntry::shallowHeapBytes)
            HistogramSort.INSTANCES -> compareBy(ClassHistogramEntry::instanceCount)
            HistogramSort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER, ClassHistogramEntry::className)
        }
        val sorted = if (direction == SortDirection.ASCENDING) {
            matches.sortedWith(comparator)
        } else {
            matches.sortedWith(comparator.reversed())
        }
        return sorted.toPage(page, pageSize)
    }

    fun compare(
        baselineJobId: UUID,
        targetJobId: UUID,
        query: String,
        page: Int,
        pageSize: Int,
    ): PageSlice<HeapComparisonEntry> {
        val baseline = completedAnalysis(baselineJobId)
        val target = completedAnalysis(targetJobId)
        val changes = HeapComparison.compare(baseline, target)
            .filter { query.isBlank() || it.className.contains(query, ignoreCase = true) }
        return changes.toPage(page, pageSize)
    }

    private fun analyze(initialJob: AnalysisJob) {
        repository.update(initialJob.id) {
            it.copy(status = AnalysisJobStatus.RUNNING, updatedAt = clock.instant())
        }
        try {
            val analysis = analyzer.analyze(initialJob.source) { progress ->
                repository.update(initialJob.id) {
                    it.copy(
                        phase = progress.phase,
                        processedBytes = progress.processedBytes,
                        totalBytes = progress.totalBytes,
                        updatedAt = clock.instant(),
                    )
                }
            }
            repository.update(initialJob.id) {
                it.copy(
                    status = AnalysisJobStatus.COMPLETED,
                    processedBytes = analysis.summary.fileSizeBytes,
                    totalBytes = analysis.summary.fileSizeBytes,
                    analysis = analysis,
                    updatedAt = clock.instant(),
                )
            }
        } catch (exception: HeapAnalysisCancelledException) {
            repository.update(initialJob.id) {
                it.copy(status = AnalysisJobStatus.CANCELLED, updatedAt = clock.instant())
            }
        } catch (exception: RuntimeException) {
            logger.error("Heap dump analysis job {} failed with {}", initialJob.id, exception.javaClass.simpleName, exception)
            repository.update(initialJob.id) {
                it.copy(
                    status = AnalysisJobStatus.FAILED,
                    error = exception.toFailure(),
                    updatedAt = clock.instant(),
                )
            }
        }
    }

    private fun completedAnalysis(jobId: UUID): HeapAnalysis {
        val job = get(jobId)
        return job.analysis ?: throw AnalysisNotReadyException(jobId, job.status)
    }

    private fun validateSource(sourceText: String): Path {
        val source = try {
            Path.of(sourceText).toAbsolutePath().normalize()
        } catch (exception: InvalidPathException) {
            throw InvalidHeapDumpPathException("Invalid heap dump path", exception)
        }
        if (!Files.isRegularFile(source)) {
            throw InvalidHeapDumpPathException("Heap dump does not exist or is not a regular file: $source")
        }
        if (!Files.isReadable(source)) {
            throw InvalidHeapDumpPathException("Heap dump is not readable: $source")
        }
        return source
    }

    private fun RuntimeException.toFailure(): AnalysisFailure = when (this) {
        is InvalidHprofException -> AnalysisFailure("INVALID_HPROF", message.orEmpty())
        else -> AnalysisFailure("ANALYSIS_FAILED", "Heap dump analysis failed. Check the application log for details.")
    }

    private fun <T> List<T>.toPage(page: Int, pageSize: Int): PageSlice<T> {
        require(page >= 0) { "page must be 0 or greater" }
        require(pageSize in 1..MAXIMUM_PAGE_SIZE) { "pageSize must be between 1 and $MAXIMUM_PAGE_SIZE" }
        val fromIndex = Math.min(Math.multiplyExact(page, pageSize), size)
        val toIndex = Math.min(fromIndex + pageSize, size)
        return PageSlice(
            items = subList(fromIndex, toIndex),
            page = page,
            pageSize = pageSize,
            totalItems = size.toLong(),
            totalPages = if (isEmpty()) 0 else (size + pageSize - 1) / pageSize,
        )
    }

    private companion object {
        val logger = LoggerFactory.getLogger(HeapDumpAnalysisService::class.java)
        const val MAXIMUM_ACTIVE_JOBS = 2
        const val MAXIMUM_RETAINED_JOBS = 10
        const val MAXIMUM_PAGE_SIZE = 500
    }
}

enum class HistogramSort {
    SHALLOW_BYTES,
    INSTANCES,
    NAME,
}

enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

data class PageSlice<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
)

class AnalysisCapacityExceededException(maximumActiveJobs: Int) :
    RuntimeException("At most $maximumActiveJobs heap dumps can be analyzed at once")

class AnalysisJobNotFoundException(jobId: UUID) : RuntimeException("Analysis job was not found: $jobId")

class AnalysisNotReadyException(jobId: UUID, status: AnalysisJobStatus) :
    RuntimeException("Analysis job $jobId is not complete: $status")

class InvalidHeapDumpPathException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
