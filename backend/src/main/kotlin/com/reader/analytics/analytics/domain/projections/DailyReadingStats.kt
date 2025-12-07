package com.reader.analytics.analytics.domain.projections

import java.time.LocalDate

data class DailyReadingStats(
    val date: LocalDate,
    val wordsRead: Long,
    val articlesProgressed: Int,
    val articlesCompleted: Int
)
