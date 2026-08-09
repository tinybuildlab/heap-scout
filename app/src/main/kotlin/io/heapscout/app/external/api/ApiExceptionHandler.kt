package io.heapscout.app.external.api

import io.heapscout.app.application.service.AnalysisCapacityExceededException
import io.heapscout.app.application.service.AnalysisJobNotFoundException
import io.heapscout.app.application.service.AnalysisNotReadyException
import io.heapscout.app.application.service.InvalidHeapDumpPathException
import io.heapscout.app.application.service.LocalFilePickerBusyException
import io.heapscout.app.application.service.LocalFilePickerUnavailableException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Clock
import java.time.Instant

@RestControllerAdvice
class ApiExceptionHandler(
    private val clock: Clock,
) {
    @ExceptionHandler(InvalidHeapDumpPathException::class, IllegalArgumentException::class)
    fun badRequest(exception: RuntimeException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.message.orEmpty())

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(exception: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val message = exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Invalid request"
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message)
    }

    @ExceptionHandler(AnalysisJobNotFoundException::class)
    fun notFound(exception: AnalysisJobNotFoundException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", exception.message.orEmpty())

    @ExceptionHandler(AnalysisNotReadyException::class)
    fun notReady(exception: AnalysisNotReadyException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.CONFLICT, "ANALYSIS_NOT_READY", exception.message.orEmpty())

    @ExceptionHandler(AnalysisCapacityExceededException::class)
    fun capacity(exception: AnalysisCapacityExceededException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.TOO_MANY_REQUESTS, "ANALYSIS_CAPACITY_EXCEEDED", exception.message.orEmpty())

    @ExceptionHandler(LocalFilePickerBusyException::class)
    fun filePickerBusy(exception: LocalFilePickerBusyException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.CONFLICT, "FILE_PICKER_BUSY", exception.message.orEmpty())

    @ExceptionHandler(LocalFilePickerUnavailableException::class)
    fun filePickerUnavailable(exception: LocalFilePickerUnavailableException): ResponseEntity<ApiErrorResponse> =
        response(HttpStatus.SERVICE_UNAVAILABLE, "FILE_PICKER_UNAVAILABLE", exception.message.orEmpty())

    private fun response(
        status: HttpStatus,
        code: String,
        message: String,
    ): ResponseEntity<ApiErrorResponse> = ResponseEntity.status(status).body(
        ApiErrorResponse(
            code = code,
            message = message,
            timestamp = clock.instant(),
        ),
    )
}

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant,
)
