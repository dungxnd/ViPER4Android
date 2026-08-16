package com.dxnd.viper4android.ui.screens.debug

internal enum class LogSource {
    APP,
    DRIVER,
}

internal enum class LogLevel {
    INFO,
    DEBUG,
    WARN,
    ERROR,
    UNKNOWN,
}

internal data class LogEntry(
    val source: LogSource,
    val level: LogLevel,
    val category: String?,
    val tag: String?,
    val message: String,
    val raw: String,
)

private val APP_HEADER = Regex("""^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} \[([^]]+)]\[([^]]+)] (.*)$""")
private val DRIVER_HEADER = Regex("""^\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}\s+([VDIWEF])/([^(]+?)\s*\(\s*\d+\):\s?(.*)$""")

internal object LogParser {
    fun parseApp(line: String): LogEntry {
        val match =
            APP_HEADER.find(line) ?: return LogEntry(
                source = LogSource.APP,
                level = LogLevel.UNKNOWN,
                category = null,
                tag = null,
                message = line,
                raw = line,
            )
        val (categoryToken, levelToken, message) = match.destructured
        return LogEntry(
            source = LogSource.APP,
            level = mapAppLevel(levelToken),
            category = categoryToken,
            tag = null,
            message = message,
            raw = line,
        )
    }

    fun parseDriver(line: String): LogEntry? {
        val match = DRIVER_HEADER.find(line) ?: return null
        val (levelToken, tagToken, message) = match.destructured
        return LogEntry(
            source = LogSource.DRIVER,
            level = mapDriverLevel(levelToken),
            category = null,
            tag = tagToken.trim(),
            message = message,
            raw = line,
        )
    }

    private fun mapAppLevel(token: String): LogLevel =
        when (token.uppercase()) {
            "ERROR" -> LogLevel.ERROR
            "WARN" -> LogLevel.WARN
            "INFO" -> LogLevel.INFO
            "DEBUG" -> LogLevel.DEBUG
            else -> LogLevel.UNKNOWN
        }

    private fun mapDriverLevel(token: String): LogLevel =
        when (token) {
            "E", "F" -> LogLevel.ERROR
            "W" -> LogLevel.WARN
            "I" -> LogLevel.INFO
            "D", "V" -> LogLevel.DEBUG
            else -> LogLevel.UNKNOWN
        }
}
