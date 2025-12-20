package com.reader.analytics.analytics.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DateRange(
    val start: LocalDate,
    val end: LocalDate
) {
    init {
        if (end.isBefore(start)) {
            throw IllegalArgumentException(
                "End date must not be before start date. Got start=$start, end=$end"
            )
        }
    }

    fun dayCount(): Long = ChronoUnit.DAYS.between(start, end) + 1

    fun previousPeriod(): DateRange {
        val duration = dayCount()
        val previousEnd = start.minusDays(1)
        val previousStart = previousEnd.minusDays(duration - 1)
        return DateRange(previousStart, previousEnd)
    }

    companion object {
        fun lastNDays(days: Int): DateRange {
            if (days < 1) {
                throw IllegalArgumentException("Days must be at least 1. Got $days")
            }
            val end = LocalDate.now()
            val start = end.minusDays(days.toLong() - 1)
            return DateRange(start, end)
        }

        fun lastWeek(): DateRange = lastNDays(7)
        fun lastMonth(): DateRange = lastNDays(30)
        fun lastYear(): DateRange = lastNDays(365)
    }
}
