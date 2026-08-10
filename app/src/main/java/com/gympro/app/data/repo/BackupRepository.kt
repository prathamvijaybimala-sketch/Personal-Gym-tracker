package com.gympro.app.data.repo

import androidx.room.withTransaction
import com.gympro.app.data.db.AttendanceEntity
import com.gympro.app.data.db.BodyFatEntity
import com.gympro.app.data.db.BodyWeightEntity
import com.gympro.app.data.db.CardioEntity
import com.gympro.app.data.db.ConfigDao
import com.gympro.app.data.db.DayDao
import com.gympro.app.data.db.DayStateEntity
import com.gympro.app.data.db.DaysCodec
import com.gympro.app.data.db.DietCheckEntity
import com.gympro.app.data.db.DietItemEntity
import com.gympro.app.data.db.DraftEntity
import com.gympro.app.data.db.ExerciseDao
import com.gympro.app.data.db.ExerciseLogEntity
import com.gympro.app.data.db.BodyDao
import com.gympro.app.data.db.RoutineDao
import com.gympro.app.data.db.RoutineEntity
import com.gympro.app.data.db.SuppItemEntity
import com.gympro.app.data.settings.SettingsRepository
import com.gympro.app.domain.Dates
import org.json.JSONArray
import org.json.JSONObject

/**
 * Full-data backup & restore.
 *
 * The exported file is byte-compatible with the web app's backup format:
 * a flat JSON object whose keys are the original localStorage keys and whose
 * values are JSON-encoded strings — so a backup from this app restores into
 * the web app and vice versa.
 */
class BackupRepository(
    private val db: com.gympro.app.data.db.GymDatabase,
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseDao,
    private val dayDao: DayDao,
    private val bodyDao: BodyDao,
    private val configDao: ConfigDao,
    private val settings: SettingsRepository,
) {

    data class ImportResult(val ok: Boolean, val message: String)

    // ------------------------------------------------------------------
    // EXPORT
    // ------------------------------------------------------------------

    suspend fun exportJson(): String {
        val dump = JSONObject()

        // Routines (array order preserved)
        val routines = JSONArray()
        routineDao.getAll().forEach { r ->
            val dayObj = try { JSONObject(r.daysJson) } catch (e: Exception) { JSONObject() }
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("name", r.name)
            obj.put("days", dayObj)
            routines.put(obj)
        }
        dump.put("routines", routines.toString())

        // Scalars
        putScalar(dump, "activeRoutineId") { settings.activeRoutineId.firstOrNull() }
        putScalar(dump, "proteinGoal") { settings.proteinGoal.firstOrNull()?.toString() }
        putScalar(dump, "waterTarget") { settings.waterTarget.firstOrNull()?.toString() }
        putScalar(dump, "timerDuration") { settings.timerDuration.firstOrNull()?.toString() }
        putScalar(dump, "theme") { settings.theme.firstOrNull() }
        putScalar(dump, "bfHeight") { settings.bfHeight.firstOrNull()?.takeIf { it.isNotEmpty() } }
        putScalar(dump, "uwt") { settings.uwt.firstOrNull()?.takeIf { it != "--" } }

        // dietConfig / suppConfig
        dump.put("dietConfig", JSONArray().also { arr ->
            configDao.dietItems().forEach { i ->
                arr.put(JSONObject().put("id", i.id).put("n", i.name).put("p", i.protein))
            }
        }.toString())
        dump.put("suppConfig", JSONArray().also { arr ->
            configDao.suppItems().forEach { i ->
                arr.put(JSONObject().put("id", i.id).put("n", i.name).put("s", i.schedule))
            }
        }.toString())

        // gatt (attendance)
        dump.put("gatt", JSONObject().also { obj ->
            dayDao.allAttendance().forEach { obj.put(it.date, true) }
        }.toString())

        // dlog (protein totals), water_, mood_, note_, de_, dc_
        val dayStates = dayDao.allDayStates()
        dump.put("dlog", JSONObject().also { obj ->
            dayStates.filter { it.proteinTotal > 0 }.forEach { obj.put(it.date, it.proteinTotal) }
        }.toString())
        dayStates.filter { it.waterCount > 0 }.forEach { dump.put("water_${it.date}", it.waterCount.toString()) }
        dayStates.filter { !it.mood.isNullOrEmpty() }.forEach { dump.put("mood_${it.date}", it.mood) }
        dayStates.filter { !it.note.isNullOrEmpty() }.forEach { dump.put("note_${it.date}", it.note) }
        dayStates.filter { it.extraProtein > 0 }.forEach { dump.put("de_${it.date}", it.extraProtein.toString()) }

        val checksByDate = dayDao.allChecks().groupBy { it.date }
        checksByDate.forEach { (date, checks) ->
            dump.put("dc_$date", JSONArray().also { arr -> checks.forEach { arr.put(it.itemId) } }.toString())
        }

        // bw_log
        dump.put("bw_log", JSONObject().also { obj ->
            bodyDao.allWeights().forEach { obj.put(it.date, it.weight) }
        }.toString())

        // bfLog
        dump.put("bfLog", JSONObject().also { obj ->
            bodyDao.allBodyFat().forEach {
                obj.put(it.date, JSONObject().put("val", it.value).put("w", it.waist).put("n", it.neck))
            }
        }.toString())

        // cardioLog
        dump.put("cardioLog", JSONArray().also { arr ->
            bodyDao.allCardio().forEach { c ->
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("date", c.date)
                obj.put("type", c.type)
                obj.put("duration", c.duration)
                obj.put("calories", c.calories ?: JSONObject.NULL)
                obj.put("distance", c.distance ?: JSONObject.NULL)
                obj.put("notes", c.notes)
                arr.put(obj)
            }
        }.toString())

        // h_<exerciseId>
        exerciseDao.allHistory().groupBy { it.exerciseId }.forEach { (exId, entries) ->
            val arr = JSONArray()
            entries.forEach { e ->
                arr.put(JSONObject().put("d", e.date).put("w", e.weight).put("r", e.reps))
            }
            dump.put("h_$exId", arr.toString())
        }

        // t_<exerciseId>_<date>
        exerciseDao.allDrafts().forEach { d ->
            dump.put(
                "t_${d.exerciseId}_${d.date}",
                JSONObject().put("w", d.weight).put("r", d.reps).toString()
            )
        }

        return dump.toString()
    }

    private suspend fun putScalar(dump: JSONObject, key: String, value: suspend () -> String?) {
        val v = value()
        if (v != null) dump.put(key, v)
    }

    // ------------------------------------------------------------------
    // IMPORT (full restore — wipes everything first)
    // ------------------------------------------------------------------

    suspend fun importJson(raw: String): ImportResult {
        val parsed = try {
            JSONObject(raw)
        } catch (e: Exception) {
            return ImportResult(false, "Invalid JSON — check format")
        }

        val dayAccumulator = mutableMapOf<String, DayStateEntity>()

        db.withTransaction {
            // Wipe current state (full restore)
            routineDao.deleteAll()
            exerciseDao.deleteAll()
            exerciseDao.deleteAllDrafts()
            dayDao.deleteAllAttendance()
            dayDao.deleteAllDayStates()
            dayDao.deleteAllChecks()
            bodyDao.deleteAllWeights()
            bodyDao.deleteAllBodyFat()
            bodyDao.deleteAllCardio()
            configDao.deleteAllDietItems()
            configDao.deleteAllSuppItems()
            settings.clear()

            val keys = parsed.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val rawValue = parsed.optString(key, "") ?: continue
                if (rawValue.isEmpty()) continue
                try {
                    applyKey(key, rawValue, dayAccumulator)
                } catch (e: Exception) {
                    // A single malformed key must not break the whole restore
                }
            }

            // Flush accumulated per-date state
            dayAccumulator.values.forEach { dayDao.upsertDayState(it) }

            // Re-seed defaults for anything the backup did not contain
            routineDao.run {
                if (getAll().isEmpty()) {
                    val days = com.gympro.app.domain.RoutineDefaults.defaultWorkouts.mapValues { (_, d) ->
                        DaysCodec.DayData(d.n, d.ex.map { DaysCodec.ExerciseData(it.id, it.n, it.t, it.l) })
                    }
                    upsert(RoutineEntity(id = "r1", name = "PPL", daysJson = DaysCodec.encode(days), position = 0))
                }
            }
            configDao.run {
                if (dietItems().isEmpty()) {
                    com.gympro.app.domain.RoutineDefaults.defaultDiet.forEachIndexed { i, (id, n, p) ->
                        upsertDietItem(DietItemEntity(id, n, p, i))
                    }
                }
                if (suppItems().isEmpty()) {
                    com.gympro.app.domain.RoutineDefaults.defaultSupps.forEachIndexed { i, (id, n, s) ->
                        upsertSuppItem(SuppItemEntity(id, n, s, i))
                    }
                }
            }

            // If the backup had no uwt, derive from the latest weight log
            if (settings.uwt.firstOrNull() == null || settings.uwt.firstOrNull() == "--") {
                val weights = bodyDao.allWeights()
                if (weights.isNotEmpty()) settings.setUwt(weights.last().weight.toString())
            }
        }

        return ImportResult(true, "Backup restored")
    }

    private suspend fun applyKey(
        key: String,
        rawValue: String,
        dayAccumulator: MutableMap<String, DayStateEntity>,
    ) {
        when (key) {
            "routines" -> {
                val arr = JSONArray(rawValue)
                for (i in 0 until arr.length()) {
                    val r = arr.optJSONObject(i) ?: continue
                    val id = r.optString("id").ifEmpty { "r${System.currentTimeMillis()}$i" }
                    val name = r.optString("name").ifEmpty { "Imported Routine" }
                    val daysObj = r.optJSONObject("days")
                    val daysJson = daysObj?.toString() ?: DaysCodec.encode(emptyMap())
                    routineDao.upsert(RoutineEntity(id, name, daysJson, i))
                }
            }
            "activeRoutineId" -> settings.setActiveRoutineId(rawValue)
            "proteinGoal" -> rawValue.toIntOrNull()?.let { settings.setProteinGoal(it) }
            "waterTarget" -> rawValue.toIntOrNull()?.let { settings.setWaterTarget(it) }
            "timerDuration" -> rawValue.toIntOrNull()?.let { settings.setTimerDuration(it) }
            "theme" -> if (rawValue == "light" || rawValue == "dark") settings.setTheme(rawValue)
            "bfHeight" -> settings.setBfHeight(rawValue)
            "uwt" -> settings.setUwt(rawValue)
            "dietConfig" -> {
                val arr = JSONArray(rawValue)
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    configDao.upsertDietItem(
                        DietItemEntity(
                            id = o.optString("id").ifEmpty { "c$i" },
                            name = o.optString("n", "Item"),
                            protein = o.optDouble("p", 0.0),
                            position = i,
                        )
                    )
                }
            }
            "suppConfig" -> {
                val arr = JSONArray(rawValue)
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    configDao.upsertSuppItem(
                        SuppItemEntity(
                            id = o.optString("id").ifEmpty { "s$i" },
                            name = o.optString("n", "Supp"),
                            schedule = o.optString("s", "daily"),
                            position = i,
                        )
                    )
                }
            }
            "gatt" -> {
                val obj = JSONObject(rawValue)
                val dates = obj.keys()
                while (dates.hasNext()) {
                    val date = dates.next()
                    dayDao.upsertAttendance(AttendanceEntity(date))
                }
            }
            "dlog" -> {
                val obj = JSONObject(rawValue)
                val dates = obj.keys()
                while (dates.hasNext()) {
                    val date = dates.next()
                    val total = obj.optDouble(date, 0.0)
                    dayAccumulator.getOrPut(date) { DayStateEntity(date) }.let { acc ->
                        dayAccumulator[date] = acc.copy(proteinTotal = total)
                    }
                }
            }
            "bw_log" -> {
                val obj = JSONObject(rawValue)
                val dates = obj.keys()
                while (dates.hasNext()) {
                    val date = dates.next()
                    bodyDao.upsertWeight(BodyWeightEntity(date, obj.optDouble(date, 0.0)))
                }
            }
            "bfLog" -> {
                val obj = JSONObject(rawValue)
                val dates = obj.keys()
                while (dates.hasNext()) {
                    val date = dates.next()
                    val o = obj.optJSONObject(date) ?: continue
                    bodyDao.upsertBodyFat(
                        BodyFatEntity(date, o.optDouble("val", 0.0), o.optDouble("w", 0.0), o.optDouble("n", 0.0))
                    )
                }
            }
            "cardioLog" -> {
                val arr = JSONArray(rawValue)
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    bodyDao.insertCardio(
                        CardioEntity(
                            id = o.optString("id").ifEmpty { "c${System.currentTimeMillis()}$i" },
                            date = o.optString("date", Dates.todayKey()),
                            type = o.optString("type", "Other"),
                            duration = o.optInt("duration", 30),
                            calories = if (o.isNull("calories")) null else o.optInt("calories", 0).takeIf { o.has("calories") },
                            distance = if (o.isNull("distance")) null else o.optDouble("distance", 0.0).takeIf { o.has("distance") },
                            notes = o.optString("notes", ""),
                        )
                    )
                }
            }
            else -> when {
                key.startsWith("h_") -> {
                    val exId = key.removePrefix("h_")
                    val arr = JSONArray(rawValue)
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        exerciseDao.insert(
                            ExerciseLogEntity(
                                exerciseId = exId,
                                date = o.optString("d", Dates.todayKey()),
                                weight = o.optDouble("w", 0.0),
                                reps = o.optInt("r", 0),
                            )
                        )
                    }
                }
                T_KEY.matches(key) -> {
                    val m = T_KEY.matchEntire(key)!!
                    val exId = m.groupValues[1]
                    val date = m.groupValues[2]
                    val o = JSONObject(rawValue)
                    exerciseDao.upsertDraft(
                        DraftEntity(exId, date, o.optString("w", ""), o.optString("r", ""))
                    )
                }
                DAY_KEY.matches(key) -> {
                    val m = DAY_KEY.matchEntire(key)!!
                    val kind = m.groupValues[1]
                    val date = m.groupValues[2]
                    val acc = dayAccumulator.getOrPut(date) { DayStateEntity(date) }
                    when (kind) {
                        "water" -> dayAccumulator[date] = acc.copy(waterCount = rawValue.toIntOrNull() ?: 0)
                        "mood" -> dayAccumulator[date] = acc.copy(mood = rawValue)
                        "note" -> dayAccumulator[date] = acc.copy(note = rawValue)
                        "de" -> dayAccumulator[date] = acc.copy(extraProtein = rawValue.toDoubleOrNull() ?: 0.0)
                        "dc" -> {
                            val arr = JSONArray(rawValue)
                            val ids = (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotEmpty() } }
                            dayDao.deleteChecks(date)
                            dayDao.upsertChecks(ids.map { DietCheckEntity(date, it) })
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val T_KEY = Regex("^t_(.+)_(\\d{4}-\\d{2}-\\d{2})$")
        private val DAY_KEY = Regex("^(water|mood|note|de|dc)_(\\d{4}-\\d{2}-\\d{2})$")
    }
}
