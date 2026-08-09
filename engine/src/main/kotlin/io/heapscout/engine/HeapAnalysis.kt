package io.heapscout.engine

import java.nio.file.Path
import java.time.Instant

data class HeapAnalysis(
    val summary: HeapSummary,
    val histogram: List<ClassHistogramEntry>,
)

data class HeapSummary(
    val source: Path,
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

data class ClassHistogramEntry(
    val className: String,
    val instanceCount: Long,
    val shallowHeapBytes: Long,
    val sizeIsEstimated: Boolean,
)

enum class ParsePhase {
    HEADER,
    METADATA,
    HEAP,
    COMPLETE,
}

data class ParseProgress(
    val phase: ParsePhase,
    val processedBytes: Long,
    val totalBytes: Long,
)

fun interface HeapAnalysisProgressListener {
    fun onProgress(progress: ParseProgress)

    companion object {
        val NO_OP = HeapAnalysisProgressListener { }
    }
}

fun interface HeapDumpAnalyzer {
    fun analyze(
        path: Path,
        progressListener: HeapAnalysisProgressListener,
    ): HeapAnalysis
}
