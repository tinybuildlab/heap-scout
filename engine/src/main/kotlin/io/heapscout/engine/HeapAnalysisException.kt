package io.heapscout.engine

open class HeapAnalysisException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class InvalidHprofException(
    message: String,
    val byteOffset: Long,
    val recordTag: Int? = null,
    cause: Throwable? = null,
) : HeapAnalysisException(
    buildString {
        append(message)
        append(" (offset=")
        append(byteOffset)
        recordTag?.let {
            append(", tag=0x")
            append(it.toString(16).padStart(2, '0'))
        }
        append(')')
    },
    cause,
)

class HeapAnalysisCancelledException : HeapAnalysisException("Heap dump analysis was cancelled")
