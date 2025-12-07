package com.reader.analytics.analytics.domain.projections

import java.time.LocalDate

data class ReadingStreak(
    val currentStreak: Int,
    val longestStreak: Int,
    val currentStreakStartDate: LocalDate?,
    val longestStreakStartDate: LocalDate?,
    val longestStreakEndDate: LocalDate?
)
