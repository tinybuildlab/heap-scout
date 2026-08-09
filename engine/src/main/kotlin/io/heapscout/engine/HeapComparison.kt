package io.heapscout.engine

import kotlin.math.absoluteValue

data class HeapComparisonEntry(
    val className: String,
    val baselineCount: Long,
    val targetCount: Long,
    val countDelta: Long,
    val baselineShallowHeapBytes: Long,
    val targetShallowHeapBytes: Long,
    val shallowHeapBytesDelta: Long,
    val sizeIsEstimated: Boolean,
)

object HeapComparison {
    fun compare(
        baseline: HeapAnalysis,
        target: HeapAnalysis,
    ): List<HeapComparisonEntry> {
        val baselineByName = baseline.histogram.associateBy(ClassHistogramEntry::className)
        val targetByName = target.histogram.associateBy(ClassHistogramEntry::className)

        return (baselineByName.keys + targetByName.keys)
            .map { className ->
                val before = baselineByName[className]
                val after = targetByName[className]
                val baselineCount = before?.instanceCount ?: 0L
                val targetCount = after?.instanceCount ?: 0L
                val baselineBytes = before?.shallowHeapBytes ?: 0L
                val targetBytes = after?.shallowHeapBytes ?: 0L

                HeapComparisonEntry(
                    className = className,
                    baselineCount = baselineCount,
                    targetCount = targetCount,
                    countDelta = targetCount - baselineCount,
                    baselineShallowHeapBytes = baselineBytes,
                    targetShallowHeapBytes = targetBytes,
                    shallowHeapBytesDelta = targetBytes - baselineBytes,
                    sizeIsEstimated = before?.sizeIsEstimated == true || after?.sizeIsEstimated == true,
                )
            }
            .sortedWith(
                compareByDescending<HeapComparisonEntry> { it.shallowHeapBytesDelta.absoluteValue }
                    .thenBy { it.className },
            )
    }
}
