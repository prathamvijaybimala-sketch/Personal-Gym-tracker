package com.gympro.app.domain

import kotlin.math.log10
import kotlin.math.round

/**
 * US Navy body-fat method — exact port of calcAndSaveBF():
 * bf = 495 / (1.0324 - 0.19077*log10(waist - neck) + 0.15456*log10(height)) - 450
 * rounded to 1 decimal place.
 */
object BodyFatCalculator {

    fun navy(heightCm: Double, neckCm: Double, waistCm: Double): Double {
        val bf = 495.0 / (1.0324 - 0.19077 * log10(waistCm - neckCm) + 0.15456 * log10(heightCm)) - 450.0
        return round(bf * 10.0) / 10.0
    }
}
