package io.heapscout.engine

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DisplayName("HistogramSearch: 클래스 히스토그램 검색")
class HistogramSearchTest {
    private lateinit var sut: List<ClassHistogramEntry>

    @BeforeEach
    fun setUp() {
        sut = listOf(
            ClassHistogramEntry("com.example.UserCache", 2_000, 20_000_000, false),
            ClassHistogramEntry("com.example.Order", 500, 4_000_000, false),
            ClassHistogramEntry("java.lang.String", 50_000, 8_000_000, false),
        )
    }

    // ═══════════════════════════════════════════
    // 정책 1: 복합 검색 — 모든 문자열·수치 조건을 만족해야 한다
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("정책: 이름, 개수, 크기 조건은 AND로 결합해야 한다")
    inner class CompoundFilterPolicy {
        @Test
        @DisplayName("name:cache count>1000 size>=10MB이면 UserCache 한 건만 반환한다")
        fun search_compoundExpression_returnsOnlyMatchingClass() {
            val matches = HistogramSearch.search(sut, "name:cache count>1000 size>=10MB")

            assertEquals(listOf("com.example.UserCache"), matches.map { it.className })
        }

        @Test
        @DisplayName("단위 없는 size>7000000이면 7,000,000바이트보다 큰 두 클래스를 반환한다")
        fun search_sizeWithoutUnit_treatsValueAsBytes() {
            val matches = HistogramSearch.search(sut, "size>7000000")

            assertEquals(2, matches.size)
        }
    }

    // ═══════════════════════════════════════════
    // 정책 2: 잘못된 검색 — 모호한 숫자를 허용하지 않는다
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("정책: 잘못된 수치 필터는 전체 결과로 대체하지 말고 명시적으로 실패해야 한다")
    inner class InvalidFilterPolicy {
        @Test
        @DisplayName("count>many는 Invalid count 예외 메시지를 반환한다")
        fun search_invalidCount_throwsActionableError() {
            val failure = assertFailsWith<IllegalArgumentException> {
                HistogramSearch.search(sut, "count>many")
            }

            assertEquals("Invalid count: many", failure.message)
        }
    }
}
