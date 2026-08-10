package com.gympro.app.domain

import kotlin.math.round

/**
 * PR detection — exact port of the web app:
 * est 1RM = round(weight * (1 + reps/30)), and a new PR is declared when the
 * estimate strictly exceeds the all-time max of previously logged sessions.
 * A PR is only declared once at least one previous session exists.
 */
object PrDetector {

    fun est1rm(weight: Double, reps: Int): Int = round(weight * (1.0 + reps / 30.0)).toInt()

    fun maxEst1rm(entries: List<Est1RmEntry>): Int =
        entries.maxOfOrNull { est1rm(it.weight, it.reps) } ?: 0

    fun isNewPr(est: Int, historyMax: Int, hasHistory: Boolean): Boolean =
        hasHistory && est > historyMax
}

data class Est1RmEntry(val weight: Double, val reps: Int)
