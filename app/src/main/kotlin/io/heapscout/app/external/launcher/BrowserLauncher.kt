package io.heapscout.app.external.launcher

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.web.context.WebServerApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.awt.Desktop
import java.net.URI

@Component
class BrowserLauncher(
    @param:Value("\${heapscout.open-browser:true}") private val openBrowser: Boolean,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun launch(event: ApplicationReadyEvent) {
        if (!openBrowser || !Desktop.isDesktopSupported()) return
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.BROWSE)) return

        val context = event.applicationContext as? WebServerApplicationContext ?: return
        val address = URI.create("http://127.0.0.1:${context.webServer.port}")
        try {
            desktop.browse(address)
        } catch (exception: Exception) {
            logger.warn("Could not open the system browser. Visit {} manually.", address, exception)
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(BrowserLauncher::class.java)
    }
}
