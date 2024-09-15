package org.mattshoe.shoebox.org.mattsho.shoebox.devtools.common

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeStamper {

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    private val prettyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.getDefault())

    // Get the current timestamp in ISO 8601 format
    fun now(): String {
        return Instant.now().toString()
    }

    // Parse an ISO 8601 timestamp and return LocalDateTime object
    fun parse(text: String): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.parse(text), ZoneId.systemDefault())
    }

    // Convert the ISO 8601 timestamp to a human-readable format
    fun pretty(text: String): String {
        val dateTime = parse(text)
        return dateTime.format(prettyFormatter)
    }
}