package io.heapscout.app.external.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LoopbackHostFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.serverName.lowercase() !in ALLOWED_HOSTS) {
            response.sendError(MISDIRECTED_REQUEST_STATUS)
            return
        }
        filterChain.doFilter(request, response)
    }

    private companion object {
        val ALLOWED_HOSTS = setOf("127.0.0.1", "localhost")
    }
}

internal const val MISDIRECTED_REQUEST_STATUS = 421
