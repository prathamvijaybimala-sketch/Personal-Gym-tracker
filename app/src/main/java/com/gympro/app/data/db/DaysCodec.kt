package com.gympro.app.data.db

import org.json.JSONArray
import org.json.JSONObject

/**
 * Codec between the in-memory day map and the routines[].days JSON blob.
 * The JSON shape is identical to the web app's localStorage format:
 * {"0":{"n":"REST","ex":[]},"1":{"n":"PUSH","ex":[{"id","n","t","l"}]},...}
 */
object DaysCodec {

    data class ExerciseData(val id: String, val name: String, val target: String, val link: String)
    data class DayData(val name: String, val exercises: List<ExerciseData>)

    fun encode(days: Map<Int, DayData>): String {
        val root = JSONObject()
        days.forEach { (dayNum, day) ->
            val dayObj = JSONObject()
            dayObj.put("n", day.name)
            val exArr = JSONArray()
            day.exercises.forEach { ex ->
                val exObj = JSONObject()
                exObj.put("id", ex.id)
                exObj.put("n", ex.name)
                exObj.put("t", ex.target)
                exObj.put("l", ex.link)
                exArr.put(exObj)
            }
            dayObj.put("ex", exArr)
            root.put(dayNum.toString(), dayObj)
        }
        return root.toString()
    }

    fun decode(json: String): Map<Int, DayData> {
        val result = mutableMapOf<Int, DayData>()
        try {
            val root = JSONObject(json)
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val dayObj = root.optJSONObject(k) ?: continue
                val dayNum = k.toIntOrNull() ?: continue
                val exArr = dayObj.optJSONArray("ex") ?: JSONArray()
                val exercises = (0 until exArr.length()).mapNotNull { i ->
                    val e = exArr.optJSONObject(i) ?: return@mapNotNull null
                    ExerciseData(
                        id = e.optString("id"),
                        name = e.optString("n"),
                        target = e.optString("t"),
                        link = e.optString("l"),
                    )
                }
                result[dayNum] = DayData(dayObj.optString("n"), exercises)
            }
        } catch (e: Exception) {
            // Malformed JSON — fall back to empty days
        }
        if (result.isEmpty()) {
            (0..6).forEach { result[it] = DayData(if (it == 0) "REST" else "DAY $it", emptyList()) }
        }
        return result
    }
}
