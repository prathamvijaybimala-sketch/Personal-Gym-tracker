package com.gympro.app

import com.gympro.app.domain.Est1RmEntry
import com.gympro.app.domain.PrDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrDetectorTest {

    @Test
    fun `est 1rm formula matches the web app`() {
        // round(80 * (1 + 10/30)) = round(106.67) = 107
        assertEquals(107, PrDetector.est1rm(80.0, 10))
        // round(75.5 * (1 + 12/30)) = round(105.7) = 106
        assertEquals(106, PrDetector.est1rm(75.5, 12))
        // round(70.5 * (1 + 3/30)) = round(77.55) = 78
        assertEquals(78, PrDetector.est1rm(70.5, 3))
        // round(100 * (1 + 0/30)) = 100
        assertEquals(100, PrDetector.est1rm(100.0, 0))
    }

    @Test
    fun `half-up rounding matches JavaScript Math round`() {
        // JS Math.round(106.5) = 107; weight 106.5 @ 0 reps
        assertEquals(107, PrDetector.est1rm(106.5, 0))
    }

    @Test
    fun `history max uses est 1rm of every entry`() {
        val entries = listOf(
            Est1RmEntry(60.0, 10),  // 80
            Est1RmEntry(75.0, 10),  // 100
            Est1RmEntry(70.0, 12),  // 98
        )
        assertEquals(100, PrDetector.maxEst1rm(entries))
    }

    @Test
    fun `empty history max is zero`() {
        assertEquals(0, PrDetector.maxEst1rm(emptyList()))
    }

    @Test
    fun `new PR when estimate strictly exceeds history max`() {
        assertTrue(PrDetector.isNewPr(est = 101, historyMax = 100, hasHistory = true))
    }

    @Test
    fun `equal estimate is not a PR`() {
        assertFalse(PrDetector.isNewPr(est = 100, historyMax = 100, hasHistory = true))
    }

    @Test
    fun `lower estimate is not a PR`() {
        assertFalse(PrDetector.isNewPr(est = 99, historyMax = 100, hasHistory = true))
    }

    @Test
    fun `first ever session is not a PR (web requires history)`() {
        assertFalse(PrDetector.isNewPr(est = 200, historyMax = 0, hasHistory = false))
    }
}
