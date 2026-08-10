package com.gympro.app.data.repo

import com.gympro.app.data.db.DaysCodec
import com.gympro.app.data.db.DaysCodec.DayData
import com.gympro.app.data.db.DaysCodec.ExerciseData
import com.gympro.app.data.db.RoutineDao
import com.gympro.app.data.db.RoutineEntity
import com.gympro.app.data.settings.SettingsRepository
import com.gympro.app.domain.RoutineDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Multi-routine system — port of getRoutines()/saveWorkouts() etc.
 * On first run seeds the default PPL routine (the web app's legacy migration
 * path creates the same default).
 */
class RoutineRepository(
    private val dao: RoutineDao,
    private val settings: SettingsRepository,
) {

    data class RoutineSummary(
        val id: String,
        val name: String,
        val trainingDays: Int,
        val isActive: Boolean,
    )

    data class ActiveRoutine(
        val id: String,
        val name: String,
        val days: Map<Int, DayData>,
    )

    suspend fun ensureSeeded() {
        if (dao.getAll().isEmpty()) {
            val days = RoutineDefaults.defaultWorkouts.mapValues { (_, d) ->
                DayData(d.n, d.ex.map { ExerciseData(it.id, it.n, it.t, it.l) })
            }
            dao.upsert(RoutineEntity(id = "r1", name = "PPL", daysJson = DaysCodec.encode(days), position = 0))
        }
    }

    fun observeSummaries(): Flow<List<RoutineSummary>> =
        combine(dao.observeAll(), settings.activeRoutineId) { routines, activeId ->
            routines.map { r ->
                val days = DaysCodec.decode(r.daysJson)
                RoutineSummary(
                    id = r.id,
                    name = r.name,
                    trainingDays = days.values.count { it.exercises.isNotEmpty() },
                    isActive = r.id == (activeId ?: routines.firstOrNull()?.id),
                )
            }
        }

    fun observeActiveRoutine(): Flow<ActiveRoutine?> =
        settings.activeRoutineId.flatMapLatest { activeId ->
            dao.observeAll().map { routines ->
                if (routines.isEmpty()) return@map null
                val chosen = routines.firstOrNull { it.id == activeId } ?: routines.first()
                ActiveRoutine(chosen.id, chosen.name, DaysCodec.decode(chosen.daysJson))
            }
        }

    suspend fun getActiveRoutine(): ActiveRoutine? {
        val routines = dao.getAll()
        if (routines.isEmpty()) return null
        val activeId = settings.activeRoutineId.firstOrNull()
        val chosen = routines.firstOrNull { it.id == activeId } ?: routines.first()
        return ActiveRoutine(chosen.id, chosen.name, DaysCodec.decode(chosen.daysJson))
    }

    suspend fun createRoutine(name: String) {
        val id = "r" + System.currentTimeMillis()
        val days = RoutineDefaults.emptyDays().mapValues { (_, d) ->
            DayData(d.n, d.ex.map { ExerciseData(it.id, it.n, it.t, it.l) })
        }
        dao.upsert(RoutineEntity(id = id, name = name, daysJson = DaysCodec.encode(days), position = dao.maxPosition() + 1))
        settings.setActiveRoutineId(id)
    }

    suspend fun renameRoutine(id: String, newName: String) {
        val r = dao.getById(id) ?: return
        dao.upsert(r.copy(name = newName))
    }

    suspend fun deleteRoutine(id: String): Boolean {
        val routines = dao.getAll()
        if (routines.size <= 1) return false
        dao.deleteById(id)
        val remaining = dao.getAll()
        val active = settings.activeRoutineId.firstOrNull()
        if (active == id) {
            settings.setActiveRoutineId(remaining.first().id)
        }
        return true
    }

    suspend fun switchRoutine(id: String) = settings.setActiveRoutineId(id)

    suspend fun importRoutine(name: String, days: Map<Int, DayData>) {
        val id = "r" + System.currentTimeMillis()
        dao.upsert(RoutineEntity(id = id, name = name, daysJson = DaysCodec.encode(days), position = dao.maxPosition() + 1))
        settings.setActiveRoutineId(id)
    }

    suspend fun updateDay(dayNum: Int, day: DayData) {
        val active = getActiveRoutine() ?: return
        val days = active.days.toMutableMap()
        days[dayNum] = day
        saveDays(active.id, days)
    }

    suspend fun addExercise(dayNum: Int, exercise: ExerciseData) {
        val active = getActiveRoutine() ?: return
        val days = active.days.toMutableMap()
        val day = days[dayNum] ?: DayData("DAY $dayNum", emptyList())
        days[dayNum] = day.copy(exercises = day.exercises + exercise)
        saveDays(active.id, days)
    }

    suspend fun updateExercise(dayNum: Int, index: Int, exercise: ExerciseData) {
        val active = getActiveRoutine() ?: return
        val days = active.days.toMutableMap()
        val day = days[dayNum] ?: return
        if (index in day.exercises.indices) {
            days[dayNum] = day.copy(exercises = day.exercises.toMutableList().also { it[index] = exercise })
            saveDays(active.id, days)
        }
    }

    suspend fun removeExercise(dayNum: Int, index: Int) {
        val active = getActiveRoutine() ?: return
        val days = active.days.toMutableMap()
        val day = days[dayNum] ?: return
        if (index in day.exercises.indices) {
            days[dayNum] = day.copy(exercises = day.exercises.toMutableList().also { it.removeAt(index) })
            saveDays(active.id, days)
        }
    }

    private suspend fun saveDays(routineId: String, days: Map<Int, DayData>) {
        val r = dao.getById(routineId) ?: return
        dao.upsert(r.copy(daysJson = DaysCodec.encode(days)))
    }

    private suspend fun <T> Flow<T>.firstOrNull(): T? {
        var result: T? = null
        var done = false
        collect { value ->
            if (!done) {
                result = value
                done = true
            }
        }
        return result
    }
}
