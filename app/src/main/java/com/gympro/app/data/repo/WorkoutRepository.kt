package com.gympro.app.data.repo

import androidx.room.withTransaction
import com.gympro.app.data.db.AttendanceEntity
import com.gympro.app.data.db.DayDao
import com.gympro.app.data.db.DaysCodec.DayData
import com.gympro.app.data.db.DraftEntity
import com.gympro.app.data.db.ExerciseDao
import com.gympro.app.data.db.ExerciseLogEntity
import com.gympro.app.data.db.GymDatabase
import com.gympro.app.domain.Dates
import com.gympro.app.domain.PrDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Workout logging — drafts, history and attendance. Port of saveW/copyLast/finishWorkout. */
class WorkoutRepository(
    private val db: GymDatabase,
    private val exerciseDao: ExerciseDao,
    private val dayDao: DayDao,
) {

    data class Draft(val weight: String, val reps: String)

    data class WorkoutResult(val logged: Int, val prCount: Int)

    fun observeAttendance(): Flow<Set<String>> =
        dayDao.observeAttendance().map { list -> list.map { it.date }.toSet() }

    fun observeDrafts(date: String): Flow<Map<String, Draft>> =
        exerciseDao.observeDraftsForDate(date).map { list ->
            list.associate { it.exerciseId to Draft(it.weight, it.reps) }
        }

    suspend fun draftsForDate(date: String): Map<String, Draft> =
        exerciseDao.draftsForDate(date).associate { it.exerciseId to Draft(it.weight, it.reps) }

    suspend fun saveDraft(exerciseId: String, weight: String, reps: String) {
        exerciseDao.upsertDraft(
            DraftEntity(exerciseId = exerciseId, date = Dates.todayKey(), weight = weight, reps = reps)
        )
    }

    /** Copy the most recent logged session into today's draft; null if no history. */
    suspend fun copyLast(exerciseId: String): Draft? {
        val hist = exerciseDao.history(exerciseId)
        if (hist.isEmpty()) return null
        val last = hist.last()
        val draft = Draft(weight = trimNumber(last.weight), reps = last.reps.toString())
        exerciseDao.upsertDraft(
            DraftEntity(exerciseId = exerciseId, date = Dates.todayKey(), weight = draft.weight, reps = draft.reps)
        )
        return draft
    }

    private fun trimNumber(value: Double): String {
        val s = value.toString()
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    suspend fun history(exerciseId: String): List<ExerciseLogEntity> = exerciseDao.history(exerciseId)

    suspend fun historyForDate(date: String): List<ExerciseLogEntity> = exerciseDao.historyForDate(date)

    /**
     * Finish-workout flow: every exercise with weight>0 and reps>0 is appended
     * to its history (PRs counted against the history BEFORE this save), and
     * today is marked attended — exactly like the web app, including marking
     * attendance even when nothing was logged.
     */
    suspend fun finishWorkout(day: DayData): WorkoutResult {
        val today = Dates.todayKey()
        var logged = 0
        var prCount = 0
        db.withTransaction {
            day.exercises.forEach { ex ->
                val draft = exerciseDao.draft(ex.id, today) ?: return@forEach
                val w = draft.weight.toDoubleOrNull() ?: return@forEach
                val r = draft.reps.toIntOrNull() ?: return@forEach
                if (w > 0 && r > 0) {
                    val hist = exerciseDao.history(ex.id)
                    val est = PrDetector.est1rm(w, r)
                    val max = PrDetector.maxEst1rm(hist)
                    if (est > max && hist.isNotEmpty()) prCount++
                    exerciseDao.insert(ExerciseLogEntity(exerciseId = ex.id, date = today, weight = w, reps = r))
                    logged++
                }
            }
            dayDao.upsertAttendance(AttendanceEntity(date = today, done = true))
        }
        return WorkoutResult(logged = logged, prCount = prCount)
    }
}
