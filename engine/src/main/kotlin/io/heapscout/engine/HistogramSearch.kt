package io.heapscout.engine

private enum class NumericOperator {
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    EQUAL,
}

private data class NumericFilter(
    val operator: NumericOperator,
    val expected: Long,
) {
    fun matches(value: Long): Boolean = when (operator) {
        NumericOperator.GREATER_THAN -> value > expected
        NumericOperator.GREATER_THAN_OR_EQUAL -> value >= expected
        NumericOperator.LESS_THAN -> value < expected
        NumericOperator.LESS_THAN_OR_EQUAL -> value <= expected
        NumericOperator.EQUAL -> value == expected
    }
}

private data class HistogramQuery(
    val nameTerms: List<String>,
    val countFilters: List<NumericFilter>,
    val sizeFilters: List<NumericFilter>,
)

object HistogramSearch {
    private val numericPattern = Regex("^(count|size)(>=|<=|>|<|=)(.+)$", RegexOption.IGNORE_CASE)
    private val sizePattern = Regex("^([0-9]+)(b|kb|kib|mb|mib|gb|gib)?$", RegexOption.IGNORE_CASE)

    fun search(
        histogram: List<ClassHistogramEntry>,
        expression: String,
    ): List<ClassHistogramEntry> {
        val query = parse(expression)

        return histogram.filter { entry ->
            query.nameTerms.all { entry.className.contains(it, ignoreCase = true) } &&
                query.countFilters.all { it.matches(entry.instanceCount) } &&
                query.sizeFilters.all { it.matches(entry.shallowHeapBytes) }
        }
    }

    private fun parse(expression: String): HistogramQuery {
        val nameTerms = mutableListOf<String>()
        val countFilters = mutableListOf<NumericFilter>()
        val sizeFilters = mutableListOf<NumericFilter>()

        expression.trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .forEach { token ->
                when {
                    token.startsWith("name:", ignoreCase = true) -> {
                        val value = token.substringAfter(':')
                        require(value.isNotBlank()) { "name filter must not be empty" }
                        nameTerms += value
                    }

                    token.startsWith("package:", ignoreCase = true) -> {
                        val value = token.substringAfter(':')
                        require(value.isNotBlank()) { "package filter must not be empty" }
                        nameTerms += value
                    }

                    numericPattern.matches(token) -> {
                        val match = requireNotNull(numericPattern.matchEntire(token))
                        val field = match.groupValues[1].lowercase()
                        val filter = NumericFilter(
                            operator = parseOperator(match.groupValues[2]),
                            expected = if (field == "size") {
                                parseSize(match.groupValues[3])
                            } else {
                                match.groupValues[3].toLongOrNull()
                                    ?: throw IllegalArgumentException("Invalid count: ${match.groupValues[3]}")
                            },
                        )
                        if (field == "size") sizeFilters += filter else countFilters += filter
                    }

                    else -> nameTerms += token
                }
            }

        return HistogramQuery(nameTerms, countFilters, sizeFilters)
    }

    private fun parseOperator(value: String): NumericOperator = when (value) {
        ">" -> NumericOperator.GREATER_THAN
        ">=" -> NumericOperator.GREATER_THAN_OR_EQUAL
        "<" -> NumericOperator.LESS_THAN
        "<=" -> NumericOperator.LESS_THAN_OR_EQUAL
        "=" -> NumericOperator.EQUAL
        else -> error("Unsupported numeric operator: $value")
    }

    private fun parseSize(value: String): Long {
        val match = sizePattern.matchEntire(value)
            ?: throw IllegalArgumentException("Invalid size: $value")
        val amount = match.groupValues[1].toLong()
        val multiplier = when (match.groupValues[2].lowercase()) {
            "", "b" -> 1L
            "kb" -> 1_000L
            "kib" -> 1_024L
            "mb" -> 1_000_000L
            "mib" -> 1_048_576L
            "gb" -> 1_000_000_000L
            "gib" -> 1_073_741_824L
            else -> error("Unsupported size unit")
        }
        return Math.multiplyExact(amount, multiplier)
    }
}
