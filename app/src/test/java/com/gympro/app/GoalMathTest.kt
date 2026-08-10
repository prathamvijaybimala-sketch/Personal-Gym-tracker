package com.gympro.app

import com.gympro.app.domain.GoalMath
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalMathTest {

    @Test
    fun `protein ratio is clamped between zero and one`() {
        assertEquals(0.5f, GoalMath.proteinRatio(55.0, 110), 0.001f)
        assertEquals(1f, GoalMath.proteinRatio(110.0, 110), 0.001f)
        assertEquals(1f, GoalMath.proteinRatio(250.0, 110), 0.001f)
        assertEquals(0f, GoalMath.proteinRatio(0.0, 110), 0.001f)
        assertEquals(0f, GoalMath.proteinRatio(50.0, 0), 0.001f)
    }

    @Test
    fun `bar is green at 95 percent of goal and above`() {
        assertEquals(GoalMath.BarLevel.GREEN, GoalMath.proteinBarLevel(104.5, 110))
        assertEquals(GoalMath.BarLevel.GREEN, GoalMath.proteinBarLevel(110.0, 110))
        assertEquals(GoalMath.BarLevel.GREEN, GoalMath.proteinBarLevel(200.0, 110))
    }

    @Test
    fun `bar is amber between 77 and 95 percent`() {
        assertEquals(GoalMath.BarLevel.AMBER, GoalMath.proteinBarLevel(104.4, 110))
        assertEquals(GoalMath.BarLevel.AMBER, GoalMath.proteinBarLevel(84.7, 110))
    }

    @Test
    fun `bar is red below 77 percent`() {
        assertEquals(GoalMath.BarLevel.RED, GoalMath.proteinBarLevel(84.6, 110))
        assertEquals(GoalMath.BarLevel.RED, GoalMath.proteinBarLevel(0.0, 110))
    }

    @Test
    fun `calendar diet thresholds are hard-coded 105 and 85 like the web`() {
        assertEquals(GoalMath.CalendarLevel.GREEN, GoalMath.calendarDietLevel(105.0))
        assertEquals(GoalMath.CalendarLevel.YELLOW, GoalMath.calendarDietLevel(104.9))
        assertEquals(GoalMath.CalendarLevel.YELLOW, GoalMath.calendarDietLevel(85.0))
        assertEquals(GoalMath.CalendarLevel.RED, GoalMath.calendarDietLevel(84.9))
        assertEquals(GoalMath.CalendarLevel.RED, GoalMath.calendarDietLevel(0.0))
    }

    @Test
    fun `water tap fills up to the tapped glass`() {
        assertEquals(6, GoalMath.waterCountAfterTap(current = 3, tappedIndex = 5))
        assertEquals(1, GoalMath.waterCountAfterTap(current = 0, tappedIndex = 0))
    }

    @Test
    fun `water tap on a filled glass empties back to that index`() {
        assertEquals(2, GoalMath.waterCountAfterTap(current = 5, tappedIndex = 2))
        assertEquals(0, GoalMath.waterCountAfterTap(current = 3, tappedIndex = 0))
    }
}
