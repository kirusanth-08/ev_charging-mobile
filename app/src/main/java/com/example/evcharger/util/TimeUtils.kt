package com.example.evcharger.util

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Time utility for ISO conversions and 12h rule checks.
 */
object TimeUtils {
    private val iso = DateTimeFormatter.ISO_DATE_TIME

    fun toIso(dt: LocalDateTime): String = dt.format(iso)

    fun canModifyOrCancel(start: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): Boolean {
        val diff = Duration.between(now, start)
        return diff.toHours() >= 12
    }
}