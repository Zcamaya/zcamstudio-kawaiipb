package com.zcamstudio.kawaiipb.services.logging

import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService
import java.io.File
import java.time.Instant

class SessionLogService(
    private val storageService: KawaiiStorageService
) {
    fun logSessionStart(sessionId: String) {
        appendLine(sessionId, "session_started")
    }

    fun logEvent(sessionId: String, event: String) {
        appendLine(sessionId, event)
    }

    fun logError(sessionId: String, event: String, message: String? = null) {
        val suffix = message?.let { " | $it" } ?: ""
        appendLine(sessionId, "error:$event$suffix")
    }

    private fun appendLine(sessionId: String, line: String) {
        val file = sessionLogFile(sessionId)
        file.appendText("${Instant.now()} | $line\n")
    }

    private fun sessionLogFile(sessionId: String): File {
        val logsDir = storageService.logsDirectory()
        return File(logsDir, "session_$sessionId.log")
    }
}
