package io.heapscout.app.application.service

import io.heapscout.app.application.port.LocalHeapDumpPickerPort
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

@Service
class LocalHeapDumpSelectionService(
    private val picker: LocalHeapDumpPickerPort,
) {
    private val selectionInProgress = AtomicBoolean(false)

    fun select(): Path? {
        if (!selectionInProgress.compareAndSet(false, true)) {
            throw LocalFilePickerBusyException()
        }
        return try {
            picker.pick()
        } finally {
            selectionInProgress.set(false)
        }
    }
}

class LocalFilePickerBusyException : RuntimeException("A file picker is already open")

class LocalFilePickerUnavailableException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
