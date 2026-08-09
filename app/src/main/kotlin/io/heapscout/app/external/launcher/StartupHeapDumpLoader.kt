package io.heapscout.app.external.launcher

import io.heapscout.app.application.service.AnalysisCapacityExceededException
import io.heapscout.app.application.service.HeapDumpAnalysisService
import io.heapscout.app.application.service.InvalidHeapDumpPathException
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class StartupHeapDumpLoader(
    private val analysisService: HeapDumpAnalysisService,
) : ApplicationRunner {
    override fun run(arguments: ApplicationArguments) {
        for ((index, source) in arguments.nonOptionArgs.withIndex()) {
            try {
                val job = analysisService.start(source)
                logger.info("Queued startup heap dump analysis job {}", job.id)
            } catch (exception: AnalysisCapacityExceededException) {
                logger.warn("Ignored startup arguments after position {} because analysis capacity is full", index + 1)
                break
            } catch (_: InvalidHeapDumpPathException) {
                logger.warn(
                    "Ignored heap dump startup argument at position {} because the path is invalid or unreadable",
                    index + 1,
                )
            } catch (exception: RuntimeException) {
                logger.warn(
                    "Ignored heap dump startup argument at position {} because it could not be opened ({})",
                    index + 1,
                    exception.javaClass.simpleName,
                )
            }
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(StartupHeapDumpLoader::class.java)
    }
}
