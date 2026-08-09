package io.heapscout.app.domain.model

import io.heapscout.engine.HeapAnalysis
import io.heapscout.engine.ParsePhase
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

enum class AnalysisJobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class AnalysisJob(
    val id: UUID,
    val source: Path,
    val status: AnalysisJobStatus,
    val phase: ParsePhase?,
    val processedBytes: Long,
    val totalBytes: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val analysis: HeapAnalysis? = null,
    val error: AnalysisFailure? = null,
) {
    val isActive: Boolean
        get() = status == AnalysisJobStatus.QUEUED || status == AnalysisJobStatus.RUNNING
}

data class AnalysisFailure(
    val code: String,
    val message: String,
)
