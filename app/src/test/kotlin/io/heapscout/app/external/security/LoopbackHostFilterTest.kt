package io.heapscout.app.external.security

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("LoopbackHostFilter: 브라우저 요청 출처 경계")
class LoopbackHostFilterTest {
    private lateinit var sut: LoopbackHostFilter

    @BeforeEach
    fun setUp() {
        sut = LoopbackHostFilter()
    }

    @Nested
    @DisplayName("정책: 루프백 호스트 이름으로 온 요청만 애플리케이션에 전달해야 한다")
    inner class HostPolicy {
        @Test
        @DisplayName("127.0.0.1 요청이면 다음 필터로 전달한다")
        fun doFilter_loopbackAddress_invokesFilterChain() {
            val request = MockHttpServletRequest("GET", "/api/dumps").apply {
                serverName = "127.0.0.1"
            }
            val response = MockHttpServletResponse()
            var invoked = false
            val filterChain = FilterChain { _, _ -> invoked = true }

            sut.doFilter(request, response, filterChain)

            assertTrue(invoked)
            assertEquals(HttpStatus.OK.value(), response.status)
        }

        @Test
        @DisplayName("외부 도메인 Host 요청이면 421로 거절하고 다음 필터를 호출하지 않는다")
        fun doFilter_externalHost_rejectsRequest() {
            val request = MockHttpServletRequest("POST", "/api/local-files/pick").apply {
                serverName = "attacker.example"
            }
            val response = MockHttpServletResponse()
            var invoked = false
            val filterChain = FilterChain { _, _ -> invoked = true }

            sut.doFilter(request, response, filterChain)

            assertFalse(invoked)
            assertEquals(MISDIRECTED_REQUEST_STATUS, response.status)
        }
    }
}
