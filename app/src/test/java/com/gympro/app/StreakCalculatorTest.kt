package com.gympro.app

import com.gympro.app.domain.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 8, 10) // a Monday

    private fun d(offset: Long): String = today.plusDays(offset).toString()

    @Test
    fun `empty attendance is zero`() {
        assertEquals(0, StreakCalculator.calculate(emptySet(), today))
    }

    @Test
    fun `only today counts as one`() {
        assertEquals(1, StreakCalculator.calculate(setOf(d(0)), today))
    }

    @Test
    fun `consecutive days including today`() {
        val attended = setOf(d(0), d(-1), d(-2), d(-3))
        assertEquals(4, StreakCalculator.calculate(attended, today))
    }

    @Test
    fun `today not yet logged counts from yesterday`() {
        // The web app anchors the streak at yesterday when today is not attended yet
        val attended = setOf(d(-1), d(-2), d(-3), d(-4))
        assertEquals(4, StreakCalculator.calculate(attended, today))
    }

    @Test
    fun `gap breaks the streak`() {
        val attended = setOf(d(-1), d(-2), d(-4), d(-5))
        assertEquals(2, StreakCalculator.calculate(attended, today))
    }

    @Test
    fun `today not attended and yesterday not attended is zero`() {
        val attended = setOf(d(-2))
        assertEquals(0, StreakCalculator.calculate(attended, today))
    }

    @Test
    fun `streak is capped at 365`() {
        val attended = (-400L..0L).map { d(it) }.toSet()
        assertEquals(365, StreakCalculator.calculate(attended, today))
    }

    @Test
    fun `weekend pattern keeps streak alive`() {
        // Fri, Sat, Sun, Mon trained
        val attended = setOf(d(0), d(-1), d(-2), d(-3))
        assertEquals(4, StreakCalculator.calculate(attended, today))
    }
}
