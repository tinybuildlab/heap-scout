package io.heapscout.app.external.api

import io.heapscout.app.application.service.HeapDumpAnalysisService
import io.heapscout.app.application.service.HistogramSort
import io.heapscout.app.application.service.PageSlice
import io.heapscout.app.application.service.SortDirection
import io.heapscout.app.domain.model.AnalysisFailure
import io.heapscout.app.domain.model.AnalysisJob
import io.heapscout.app.domain.model.AnalysisJobStatus
import io.heapscout.engine.ClassHistogramEntry
import io.heapscout.engine.HeapComparisonEntry
import io.heapscout.engine.ParsePhase
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api")
class HeapDumpController(
    private val service: HeapDumpAnalysisService,
) {
    @PostMapping("/dumps")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun open(@Valid @RequestBody request: OpenHeapDumpRequest): AnalysisJobResponse =
        service.start(request.path).toResponse()

    @GetMapping("/dumps")
    fun recent(): List<AnalysisJobResponse> = service.recent().map(AnalysisJob::toResponse)

    @GetMapping("/dumps/{jobId}")
    fun status(@PathVariable jobId: UUID): AnalysisJobResponse = service.get(jobId).toResponse()

    @GetMapping("/dumps/{jobId}/histogram")
    fun histogram(
        @PathVariable jobId: UUID,
        @RequestParam(defaultValue = "") @Size(max = 512) query: String,
        @RequestParam(defaultValue = "SHALLOW_BYTES") sort: HistogramSort,
        @RequestParam(defaultValue = "DESCENDING") direction: SortDirection,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") pageSize: Int,
    ): PageSlice<ClassHistogramEntry> = service.histogram(jobId, query, sort, direction, page, pageSize)

    @GetMapping("/comparisons")
    fun compare(
        @RequestParam baseline: UUID,
        @RequestParam target: UUID,
        @RequestParam(defaultValue = "") @Size(max = 512) query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") pageSize: Int,
    ): PageSlice<HeapComparisonEntry> = service.compare(baseline, target, query, page, pageSize)

    @DeleteMapping("/dumps/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun close(@PathVariable jobId: UUID) {
        service.close(jobId)
    }
}

data class OpenHeapDumpRequest(
    @field:NotBlank
    @field:Size(max = 8_192)
    val path: String,
)

data class AnalysisJobResponse(
    val id: UUID,
    val fileName: String,
    val sourcePath: String,
    val status: AnalysisJobStatus,
    val phase: ParsePhase?,
    val processedBytes: Long,
    val totalBytes: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val summary: HeapSummaryResponse?,
    val error: AnalysisFailure?,
)

data class HeapSummaryResponse(
    val fileSizeBytes: Long,
    val format: String,
    val identifierSizeBytes: Int,
    val capturedAt: Instant,
    val objectCount: Long,
    val classCount: Int,
    val shallowHeapBytes: Long,
    val containsEstimatedSizes: Boolean,
    val parseDurationMillis: Long,
)

private fun AnalysisJob.toResponse(): AnalysisJobResponse = AnalysisJobResponse(
    id = id,
    fileName = source.fileName.toString(),
    sourcePath = source.toString(),
    status = status,
    phase = phase,
    processedBytes = processedBytes,
    totalBytes = totalBytes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    summary = analysis?.summary?.let {
        HeapSummaryResponse(
            fileSizeBytes = it.fileSizeBytes,
            format = it.format,
            identifierSizeBytes = it.identifierSizeBytes,
            capturedAt = it.capturedAt,
            objectCount = it.objectCount,
            classCount = it.classCount,
            shallowHeapBytes = it.shallowHeapBytes,
            containsEstimatedSizes = it.containsEstimatedSizes,
            parseDurationMillis = it.parseDurationMillis,
        )
    },
    error = error,
)
