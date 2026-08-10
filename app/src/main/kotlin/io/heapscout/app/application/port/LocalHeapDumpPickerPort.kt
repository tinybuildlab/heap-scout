package io.heapscout.app.application.port

import java.nio.file.Path

fun interface LocalHeapDumpPickerPort {
    fun pick(): Path?

    fun isAvailable(): Boolean = true
}
