package com.gympro.app

import com.gympro.app.domain.Dates
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DatesTest {

    private val wednesday = LocalDate.of(2026, 8, 12)

    @Test
    fun `js day of week is zero-based from Sunday`() {
        assertEquals(0, Dates.jsDayOfWeek(LocalDate.of(2026, 8, 9)))  // Sunday
        assertEquals(1, Dates.jsDayOfWeek(LocalDate.of(2026, 8, 10))) // Monday
        assertEquals(6, Dates.jsDayOfWeek(LocalDate.of(2026, 8, 15))) // Saturday
    }

    @Test
    fun `monday of week for a mid-week day`() {
        assertEquals("2026-08-10", Dates.key(Dates.mondayOfWeek(wednesday)))
    }

    @Test
    fun `monday of week for a Sunday is the previous Monday`() {
        val sunday = LocalDate.of(2026, 8, 16)
        assertEquals("2026-08-10", Dates.key(Dates.mondayOfWeek(sunday)))
    }

    @Test
    fun `this week date maps js day-of-week to the current week`() {
        // Wednesday 2026-08-12: same week's Monday = 08-10
        assertEquals("2026-08-10", Dates.thisWeekDate(1, wednesday))
        // same week's Sunday = 08-16
        assertEquals("2026-08-16", Dates.thisWeekDate(0, wednesday))
    }

    @Test
    fun `date keys round trip`() {
        val key = Dates.key(wednesday)
        assertEquals(wednesday, Dates.parseKey(key))
    }

    @Test
    fun `display date matches toLocaleDateString english format`() {
        assertEquals("Wednesday, August 12", Dates.displayDate("2026-08-12"))
    }
}
