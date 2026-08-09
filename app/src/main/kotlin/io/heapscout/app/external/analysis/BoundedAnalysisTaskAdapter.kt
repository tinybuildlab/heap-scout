package io.heapscout.app.external.analysis

import io.heapscout.app.application.port.AnalysisTaskPort
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Component
class BoundedAnalysisTaskAdapter : AnalysisTaskPort {
    private val threadNumber = AtomicInteger()
    private val executor = ThreadPoolExecutor(
        CORE_THREADS,
        MAXIMUM_THREADS,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAXIMUM_QUEUED_TASKS),
        { runnable ->
            Thread(runnable, "heapscout-analysis-${threadNumber.incrementAndGet()}").apply {
                isDaemon = true
            }
        },
        ThreadPoolExecutor.AbortPolicy(),
    )
    private val tasks = ConcurrentHashMap<UUID, Future<*>>()

    override fun submit(jobId: UUID, task: () -> Unit) {
        val future = object : FutureTask<Unit>({
            task()
        }) {
            override fun done() {
                tasks.remove(jobId, this)
            }
        }
        tasks[jobId] = future
        try {
            executor.execute(future)
        } catch (exception: RuntimeException) {
            tasks.remove(jobId, future)
            throw exception
        }
    }

    override fun cancel(jobId: UUID): Boolean = tasks.remove(jobId)?.cancel(true) ?: false

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    private companion object {
        const val CORE_THREADS = 1
        const val MAXIMUM_THREADS = 1
        const val MAXIMUM_QUEUED_TASKS = 1
    }
}
