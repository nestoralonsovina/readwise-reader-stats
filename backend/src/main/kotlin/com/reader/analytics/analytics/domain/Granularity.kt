package com.reader.analytics.analytics.domain

enum class Granularity {
    DAILY,
    WEEKLY,
    MONTHLY;

    fun toPostgresTrunc(): String = when (this) {
        DAILY -> "day"
        WEEKLY -> "week"
        MONTHLY -> "month"
    }
}
