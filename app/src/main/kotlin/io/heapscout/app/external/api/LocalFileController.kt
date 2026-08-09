package io.heapscout.app.external.api

import io.heapscout.app.application.service.LocalHeapDumpSelectionService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/local-files")
class LocalFileController(
    private val selectionService: LocalHeapDumpSelectionService,
) {
    @PostMapping("/pick", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun pick(): LocalFileSelectionResponse {
        val selectedPath = selectionService.select()
        return LocalFileSelectionResponse(
            selected = selectedPath != null,
            path = selectedPath?.toString(),
        )
    }
}

data class LocalFileSelectionResponse(
    val selected: Boolean,
    val path: String?,
)
