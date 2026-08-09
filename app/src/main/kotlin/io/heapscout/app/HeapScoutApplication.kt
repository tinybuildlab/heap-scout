package io.heapscout.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HeapScoutApplication

fun main(args: Array<String>) {
    runApplication<HeapScoutApplication>(*args)
}
