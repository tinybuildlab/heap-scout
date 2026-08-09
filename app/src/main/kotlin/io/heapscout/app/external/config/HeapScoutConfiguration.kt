package io.heapscout.app.external.config

import io.heapscout.engine.HeapDumpAnalyzer
import io.heapscout.engine.HprofStreamingAnalyzer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class HeapScoutConfiguration {
    @Bean
    fun heapDumpAnalyzer(): HeapDumpAnalyzer = HprofStreamingAnalyzer()

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
