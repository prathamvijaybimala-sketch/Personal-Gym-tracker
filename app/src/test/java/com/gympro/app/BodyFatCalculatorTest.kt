package com.gympro.app

import com.gympro.app.domain.BodyFatCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class BodyFatCalculatorTest {

    @Test
    fun `us navy formula matches the web implementation`() {
        // 495 / (1.0324 - 0.19077*log10(85-38) + 0.15456*log10(175)) - 450 ≈ 16.9
        val result = BodyFatCalculator.navy(heightCm = 175.0, neckCm = 38.0, waistCm = 85.0)
        assertEquals(16.9, result, 0.05)
    }

    @Test
    fun `rounds to one decimal place`() {
        val result = BodyFatCalculator.navy(heightCm = 180.0, neckCm = 40.0, waistCm = 90.0)
        val tenths = (result * 10) % 1.0
        assertEquals(0.0, tenths, 1e-9)
    }

    @Test
    fun `larger waist yields higher body fat`() {
        val lean = BodyFatCalculator.navy(175.0, 38.0, 80.0)
        val heavier = BodyFatCalculator.navy(175.0, 38.0, 95.0)
        assertEquals(true, heavier > lean)
    }
}
