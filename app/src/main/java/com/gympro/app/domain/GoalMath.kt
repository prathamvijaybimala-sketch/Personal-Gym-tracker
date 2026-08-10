package com.gympro.app.domain

/** Goal / adherence math — exact port of updateDietBar(), renderCalendar() and toggleWater(). */
object GoalMath {

    enum class BarLevel { GREEN, AMBER, RED }

    enum class CalendarLevel { GREEN, YELLOW, RED, PLAIN }

    /** Fraction of the protein goal reached, clamped to 0..1 for the progress bar. */
    fun proteinRatio(total: Double, goal: Int): Float =
        if (goal <= 0) 0f else (total / goal).toFloat().coerceIn(0f, 1f)

    /** Progress bar color: green >= 95%, amber >= 77%, red below. */
    fun proteinBarLevel(total: Double, goal: Int): BarLevel = when {
        total >= goal * 0.95 -> BarLevel.GREEN
        total >= goal * 0.77 -> BarLevel.AMBER
        else -> BarLevel.RED
    }

    /**
     * Calendar (diet mode) colors — the web app hard-codes 105g / 85g
     * (95% / ~77% of the default 110g goal), replicated exactly.
     */
    fun calendarDietLevel(total: Double): CalendarLevel = when {
        total >= 105.0 -> CalendarLevel.GREEN
        total >= 85.0 -> CalendarLevel.YELLOW
        else -> CalendarLevel.RED
    }

    /**
     * Water glass tap: tapping a glass at or beyond the current count fills up
     * to that glass (i+1); tapping a filled glass empties back to that index.
     */
    fun waterCountAfterTap(current: Int, tappedIndex: Int): Int =
        if (tappedIndex < current) tappedIndex else tappedIndex + 1

    /** 7-day chart bar color by protein ratio (same thresholds as the bar). */
    fun chartBarLevel(total: Double, goal: Int): BarLevel = proteinBarLevel(total, goal)
}
