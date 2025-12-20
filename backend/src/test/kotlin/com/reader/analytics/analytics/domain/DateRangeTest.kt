package com.reader.analytics.analytics.domain

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class DateRangeTest {

    @Test
    fun `previousPeriod returns same duration immediately before current range`() {
        val current = DateRange(
            start = LocalDate.of(2024, 1, 8),
            end = LocalDate.of(2024, 1, 14)
        )

        val previous = current.previousPeriod()

        assertEquals(LocalDate.of(2024, 1, 1), previous.start)
        assertEquals(LocalDate.of(2024, 1, 7), previous.end)
    }

    @Test
    fun `previousPeriod preserves duration for 30 day range`() {
        val current = DateRange(
            start = LocalDate.of(2024, 2, 1),
            end = LocalDate.of(2024, 3, 1)
        )

        val previous = current.previousPeriod()

        assertEquals(current.dayCount(), previous.dayCount())
        assertEquals(LocalDate.of(2024, 1, 2), previous.start)
        assertEquals(LocalDate.of(2024, 1, 31), previous.end)
    }
}
