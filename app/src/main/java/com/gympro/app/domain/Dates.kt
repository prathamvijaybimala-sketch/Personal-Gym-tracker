package com.gympro.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Date helpers. All dates are stored as ISO "yyyy-MM-dd" strings in the device's
 * local timezone (the web app used UTC via toISOString(); we deliberately use
 * local dates so "today" matches the user's calendar).
 */
object Dates {
    val FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()

    fun key(date: LocalDate): String = date.format(FORMATTER)

    fun todayKey(): String = key(today())

    fun parseKey(s: String): LocalDate = LocalDate.parse(s, FORMATTER)

    /** JS-style day-of-week: 0 = Sunday … 6 = Saturday (matches Date.getDay()). */
    fun jsDayOfWeek(date: LocalDate): Int = date.dayOfWeek.value % 7

    val DAY_NAMES = listOf("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
    val DAY_SHORT = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    /** e.g. "Monday, August 11" — matches toLocaleDateString('en', {weekday:'long',month:'long',day:'numeric'}) */
    fun displayDate(dateKey: String): String =
        parseKey(dateKey).format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    /** e.g. "August 2026" */
    fun monthYearLabel(year: Int, month: Int): String =
        LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    /** Short weekday label e.g. "Mon" (en-US style). */
    fun shortWeekday(dateKey: String): String =
        parseKey(dateKey).format(DateTimeFormatter.ofPattern("EEE"))

    /**
     * Monday-start week helper (mirrors renderWeekSummary in the web app).
     * Returns the date of the Monday of the week containing [today].
     */
    fun mondayOfWeek(today: LocalDate): LocalDate {
        val dow = jsDayOfWeek(today)
        val mondayOffset = if (dow == 0) -6 else 1 - dow
        return today.plusDays(mondayOffset.toLong())
    }

    /**
     * The date of the given JS day-of-week (0=Sunday..6=Saturday) within the
     * current week (mirrors getThisWeekDate in the web app).
     */
    fun thisWeekDate(dayOfWeek: Int, today: LocalDate = today()): String {
        val diff = dayOfWeek - jsDayOfWeek(today)
        return key(today.plusDays(diff.toLong()))
    }
}
