package io.heapscout.app.application.port

import java.util.UUID

interface AnalysisTaskPort {
    fun submit(jobId: UUID, task: () -> Unit)

    fun cancel(jobId: UUID): Boolean
}
