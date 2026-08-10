package com.gympro.app.domain

import org.json.JSONArray
import org.json.JSONObject

/**
 * Workout routine JSON import — exact port of importWorkoutJSON() in the web app.
 * Supports the two accepted formats:
 *
 * Format A (direct days object):  {"name":"X","days":{"1":{"n":..,"ex":[{name,sets,reps,link}]}}}
 * Format B (AI output, days array): {"name":"X","days":[{"day":1,"name":"PUSH","exercises":[...]}]}
 *
 * Missing days are filled with REST / "DAY n". Returns null when the payload
 * cannot be parsed or the structure is missing.
 */
object WorkoutJsonParser {

    data class ExerciseDraft(val id: String, val name: String, val target: String, val link: String)
    data class DayDraft(val name: String, val exercises: List<ExerciseDraft>)
    data class RoutineDraft(val name: String, val days: Map<Int, DayDraft>)

    private fun randomId(): String =
        java.util.Random().nextLong().toString(36).replace("-", "").take(5).ifEmpty { "abcde" }

    fun parse(raw: String): RoutineDraft? {
        val parsed = try {
            JSONObject(raw)
        } catch (e: Exception) {
            return null
        }

        val name = parsed.optString("name", "Imported Routine").ifEmpty { "Imported Routine" }
        val days = mutableMapOf<Int, DayDraft>()

        val daysHolder = parsed.opt("days")
        if (daysHolder is JSONObject) {
            // Format A — keys are day numbers (strings or ints)
            val keys = daysHolder.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = daysHolder.optJSONObject(k) ?: continue
                val dayNum = k.toIntOrNull() ?: continue
                val exArr = v.optJSONArray("ex") ?: v.optJSONArray("exercises") ?: JSONArray()
                val ex = (0 until exArr.length()).mapNotNull { i ->
                    val e = exArr.optJSONObject(i) ?: return@mapNotNull null
                    ExerciseDraft(
                        id = e.optString("id").ifEmpty { "ex$randomId" },
                        name = e.optString("n").ifEmpty { e.optString("name") },
                        target = e.optString("t").ifEmpty { e.optString("sets") },
                        link = e.optString("l").ifEmpty { e.optString("link") },
                    )
                }
                days[dayNum] = DayDraft(
                    name = v.optString("n").ifEmpty { v.optString("name") }.ifEmpty { "Day" },
                    exercises = ex,
                )
            }
        } else if (daysHolder is JSONArray) {
            // Format B — AI-generated array format
            for (i in 0 until daysHolder.length()) {
                val d = daysHolder.optJSONObject(i) ?: continue
                val dayNum = d.optString("day").toIntOrNull() ?: 0
                val exArr = d.optJSONArray("exercises") ?: d.optJSONArray("ex") ?: JSONArray()
                val ex = (0 until exArr.length()).mapNotNull { j ->
                    val e = exArr.optJSONObject(j) ?: return@mapNotNull null
                    val sets = e.optString("sets")
                    val reps = e.optString("reps")
                    ExerciseDraft(
                        id = "ex$randomId",
                        name = e.optString("name").ifEmpty { e.optString("n") },
                        target = if (sets.isNotEmpty()) sets + (if (reps.isNotEmpty()) "×$reps" else "") else e.optString("t"),
                        link = e.optString("link").ifEmpty { e.optString("l") },
                    )
                }
                days[dayNum] = DayDraft(
                    name = d.optString("name").ifEmpty { d.optString("n") }.ifEmpty { "Day" },
                    exercises = ex,
                )
            }
        } else {
            return null
        }

        // Fill missing days as rest
        (0..6).forEach { i ->
            if (days[i] == null) days[i] = DayDraft(if (i == 0) "REST" else "DAY $i", emptyList())
        }

        return RoutineDraft(name = name, days = days)
    }
}
