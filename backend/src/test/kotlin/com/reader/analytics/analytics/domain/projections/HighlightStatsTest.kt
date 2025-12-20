package com.reader.analytics.analytics.domain.projections

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HighlightStatsTest {

    @Test
    fun `notePercentage calculates correctly`() {
        val stats = createStats(totalHighlights = 100, highlightsWithNotes = 25)

        assertEquals(25.0, stats.notePercentage)
    }

    @Test
    fun `notePercentage returns zero when no highlights exist`() {
        val stats = createStats(totalHighlights = 0, highlightsWithNotes = 0)

        assertEquals(0.0, stats.notePercentage)
    }

    @Test
    fun `periodChange calculates difference between current and previous`() {
        val stats = createStats(highlightsThisPeriod = 30, highlightsPreviousPeriod = 20)

        assertEquals(10, stats.periodChange)
    }

    @Test
    fun `periodChangePercent calculates correctly`() {
        val stats = createStats(highlightsThisPeriod = 30, highlightsPreviousPeriod = 20)

        assertEquals(50.0, stats.periodChangePercent)
    }

    @Test
    fun `periodChangePercent returns null when previous period is zero`() {
        val stats = createStats(highlightsThisPeriod = 10, highlightsPreviousPeriod = 0)

        assertNull(stats.periodChangePercent)
    }

    @Test
    fun `periodChangePercent handles negative change`() {
        val stats = createStats(highlightsThisPeriod = 10, highlightsPreviousPeriod = 20)

        assertEquals(-50.0, stats.periodChangePercent)
    }

    private fun createStats(
        totalHighlights: Int = 100,
        highlightsWithNotes: Int = 0,
        highlightsThisPeriod: Int = 10,
        highlightsPreviousPeriod: Int = 10
    ) = HighlightStats(
        totalHighlights = totalHighlights,
        highlightsWithNotes = highlightsWithNotes,
        highlightsThisPeriod = highlightsThisPeriod,
        highlightsPreviousPeriod = highlightsPreviousPeriod,
        averageHighlightsPerDocument = 1.0,
        mostHighlightedDocuments = emptyList()
    )
}
