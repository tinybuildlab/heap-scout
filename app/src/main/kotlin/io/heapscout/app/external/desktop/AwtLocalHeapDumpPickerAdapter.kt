package io.heapscout.app.external.desktop

import io.heapscout.app.application.port.LocalHeapDumpPickerPort
import io.heapscout.app.application.service.LocalFilePickerUnavailableException
import org.springframework.stereotype.Component
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.HeadlessException
import java.io.File
import java.io.FilenameFilter
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

@Component
class AwtLocalHeapDumpPickerAdapter : LocalHeapDumpPickerPort {
    override fun isAvailable(): Boolean = !GraphicsEnvironment.isHeadless()

    override fun pick(): Path? {
        if (!isAvailable()) {
            throw LocalFilePickerUnavailableException(
                "The system file picker is unavailable in headless mode. Enter the heap dump path manually.",
            )
        }

        if (EventQueue.isDispatchThread()) return showDialog()

        val selectedPath = AtomicReference<Path?>()
        val failure = AtomicReference<Exception?>()
        try {
            EventQueue.invokeAndWait {
                try {
                    selectedPath.set(showDialog())
                } catch (exception: Exception) {
                    failure.set(exception)
                }
            }
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw LocalFilePickerUnavailableException("The system file picker was interrupted. Try again.", exception)
        } catch (exception: Exception) {
            throw exception.toUnavailableException()
        }

        failure.get()?.let { throw it.toUnavailableException() }
        return selectedPath.get()
    }

    private fun showDialog(): Path? {
        val dialog = try {
            FileDialog(null as Frame?, "Open Java heap dump", FileDialog.LOAD)
        } catch (exception: HeadlessException) {
            throw LocalFilePickerUnavailableException(
                "The system file picker is unavailable in headless mode. Enter the heap dump path manually.",
                exception,
            )
        }
        return try {
            dialog.isMultipleMode = false
            dialog.filenameFilter = HEAP_DUMP_FILTER
            dialog.isVisible = true
            dialog.files.firstOrNull()?.toPath()?.toAbsolutePath()?.normalize()
        } finally {
            dialog.dispose()
        }
    }

    private fun Exception.toUnavailableException(): LocalFilePickerUnavailableException =
        if (this is LocalFilePickerUnavailableException) {
            this
        } else {
            LocalFilePickerUnavailableException(
                "The system file picker could not be opened. Enter the heap dump path manually.",
                this,
            )
        }

    private companion object {
        val HEAP_DUMP_FILTER = FilenameFilter { directory, name ->
            File(directory, name).isDirectory ||
                name.endsWith(".hprof", ignoreCase = true) ||
                name.endsWith(".bin", ignoreCase = true)
        }
    }
}
