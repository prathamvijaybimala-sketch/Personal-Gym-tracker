package com.gympro.app.data.repo

import com.gympro.app.data.db.ConfigDao
import com.gympro.app.data.db.DayDao
import com.gympro.app.data.db.DayStateEntity
import com.gympro.app.data.db.DietCheckEntity
import com.gympro.app.data.db.DietItemEntity
import com.gympro.app.data.db.SuppItemEntity
import com.gympro.app.data.settings.SettingsRepository
import com.gympro.app.domain.Dates
import com.gympro.app.domain.GoalMath
import kotlinx.coroutines.flow.Flow

/**
 * Diet / water / mood / notes — port of renderDiet, toggleDiet, updateDietBar,
 * addQuickProtein, renderWater, toggleWater, saveMood, saveNote.
 */
class DietRepository(
    private val dayDao: DayDao,
    private val configDao: ConfigDao,
    private val settings: SettingsRepository,
) {

    fun observeDietItems(): Flow<List<DietItemEntity>> = configDao.observeDietItems()
    fun observeSuppItems(): Flow<List<SuppItemEntity>> = configDao.observeSuppItems()
    fun observeDayStates(): Flow<List<DayStateEntity>> = dayDao.observeDayStates()
    fun observeChecks(): Flow<List<DietCheckEntity>> = dayDao.observeChecks()
    val proteinGoal: Flow<Int> = settings.proteinGoal
    val waterTarget: Flow<Int> = settings.waterTarget

    suspend fun ensureSeeded() {
        if (configDao.dietItems().isEmpty()) {
            com.gympro.app.domain.RoutineDefaults.defaultDiet.forEachIndexed { i, (id, n, p) ->
                configDao.upsertDietItem(DietItemEntity(id, n, p, i))
            }
        }
        if (configDao.suppItems().isEmpty()) {
            com.gympro.app.domain.RoutineDefaults.defaultSupps.forEachIndexed { i, (id, n, s) ->
                configDao.upsertSuppItem(SuppItemEntity(id, n, s, i))
            }
        }
    }

    suspend fun checksForDate(date: String): Set<String> = dayDao.checks(date).map { it.itemId }.toSet()

    /**
     * Toggle a diet/supp item for today, recompute the day's protein total and
     * persist dlog. Returns true when the protein goal was crossed by this toggle
     * (used for the "goal reached" toast, matching updateDietBar).
     */
    suspend fun toggleDietItem(itemId: String): Boolean {
        val today = Dates.todayKey()
        val checks = dayDao.checks(today).map { it.itemId }.toMutableSet()
        val prevTotal = dayDao.dayState(today)?.proteinTotal ?: 0.0
        if (!checks.remove(itemId)) checks.add(itemId)
        dayDao.deleteChecks(today)
        dayDao.upsertChecks(checks.map { DietCheckEntity(today, it) })
        val newTotal = recomputeProteinTotal(today)
        val goal = settings.proteinGoal.firstOrNull() ?: 110
        return newTotal >= goal && prevTotal < goal
    }

    suspend fun addQuickProtein(grams: Double): Double {
        val today = Dates.todayKey()
        val state = dayDao.dayState(today) ?: DayStateEntity(today)
        val extra = state.extraProtein + grams
        dayDao.upsertDayState(state.copy(extraProtein = extra))
        recomputeProteinTotal(today)
        return extra
    }

    /** Sum of quick-add protein plus checked food items (supplements carry no protein). */
    suspend fun recomputeProteinTotal(date: String): Double {
        val state = dayDao.dayState(date) ?: DayStateEntity(date)
        val checks = dayDao.checks(date).map { it.itemId }.toSet()
        val items = configDao.dietItems()
        val total = state.extraProtein + items.filter { it.id in checks }.sumOf { it.protein }
        dayDao.upsertDayState(state.copy(proteinTotal = total))
        return total
    }

    suspend fun waterCount(date: String): Int = dayDao.dayState(date)?.waterCount ?: 0

    /** Tap glass i. Returns Pair(count, goalHit) mirroring toggleWater. */
    suspend fun toggleWater(date: String, tappedIndex: Int): Pair<Int, Boolean> {
        val state = dayDao.dayState(date) ?: DayStateEntity(date)
        val count = GoalMath.waterCountAfterTap(state.waterCount, tappedIndex)
        dayDao.upsertDayState(state.copy(waterCount = count))
        val target = settings.waterTarget.firstOrNull() ?: 8
        return count to (count == target)
    }

    suspend fun saveMood(date: String, mood: String) {
        val state = dayDao.dayState(date) ?: DayStateEntity(date)
        dayDao.upsertDayState(state.copy(mood = mood))
    }

    suspend fun saveNote(date: String, note: String) {
        val state = dayDao.dayState(date) ?: DayStateEntity(date)
        dayDao.upsertDayState(state.copy(note = note))
    }

    suspend fun dayState(date: String): DayStateEntity? = dayDao.dayState(date)

    suspend fun addDietItem(name: String, protein: Double) {
        val items = configDao.dietItems()
        val position = (items.maxOfOrNull { it.position } ?: -1) + 1
        configDao.upsertDietItem(DietItemEntity(id = "c" + System.currentTimeMillis(), name = name, protein = protein, position = position))
    }

    suspend fun removeDietItem(id: String) = configDao.deleteDietItem(id)

    suspend fun addSuppItem(name: String, schedule: String) {
        val items = configDao.suppItems()
        val position = (items.maxOfOrNull { it.position } ?: -1) + 1
        configDao.upsertSuppItem(SuppItemEntity(id = "s" + System.currentTimeMillis(), name = name, schedule = schedule, position = position))
    }

    suspend fun removeSuppItem(id: String) = configDao.deleteSuppItem(id)

    suspend fun setProteinGoal(goal: Int) {
        settings.setProteinGoal(goal)
        recomputeProteinTotal(Dates.todayKey())
    }

    suspend fun setWaterTarget(target: Int) = settings.setWaterTarget(target)

    /** Supplement schedule check — port of renderDiet's show logic. */
    fun isSuppScheduledToday(schedule: String): Boolean {
        val today = Dates.today()
        return when {
            schedule == "daily" -> true
            schedule == "alt" -> today.dayOfMonth % 2 != 0
            else -> schedule.toIntOrNull() == Dates.jsDayOfWeek(today)
        }
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
