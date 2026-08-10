package com.gympro.app.domain

import java.time.LocalDate

/**
 * Workout streak — exact port of calcStreak() in the web app:
 * counts consecutive attended days ending at [today]; if today is not yet
 * attended, the streak is anchored at yesterday (so it does not break before
 * the user has trained today). Capped at 365.
 */
object StreakCalculator {

    fun calculate(attendedDates: Set<String>, today: LocalDate = Dates.today()): Int {
        var streak = 0
        var d = today
        if (Dates.key(d) !in attendedDates) {
            d = d.minusDays(1)
        }
        while (true) {
            if (Dates.key(d) in attendedDates) {
                streak++
                d = d.minusDays(1)
            } else {
                break
            }
            if (streak > 365) break
        }
        return streak
    }
}
