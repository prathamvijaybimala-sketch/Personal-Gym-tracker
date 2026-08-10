package com.gympro.app.ui.fuel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gympro.app.GymProApp
import com.gympro.app.data.AppContainer
import com.gympro.app.data.db.DietItemEntity
import com.gympro.app.data.db.SuppItemEntity
import com.gympro.app.domain.Dates
import com.gympro.app.ui.components.ToastType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FuelViewModel(application: Application) : AndroidViewModel(application) {

    private val container: AppContainer = (application as GymProApp).container
    private val dietRepository = container.dietRepository
    private val dayDao = container.database.dayDao()

    // ------------------------------------------------------------------
    // Main state
    // ------------------------------------------------------------------

    data class FuelState(
        val items: List<DietItemEntity> = emptyList(),
        val supps: List<Pair<SuppItemEntity, Boolean>> = emptyList(), // scheduled today, item to checked
        val allSupps: List<SuppItemEntity> = emptyList(),
        val checks: Set<String> = emptySet(),
        val extraProtein: Double = 0.0,
        val totalProtein: Double = 0.0,
        val proteinGoal: Int = 110,
        val waterTarget: Int = 8,
        val waterCount: Int = 0,
        val weekProtein: List<Pair<String, Double>> = emptyList(),
        val weekWater: List<Pair<String, Double>> = emptyList(),
        val weekWaterTarget: Int = 8,
    )

    // combine() supports at most 5 typed flows, so bundle the sources first.
    private data class DataBundle(
        val items: List<DietItemEntity>,
        val supps: List<SuppItemEntity>,
        val checks: List<com.gympro.app.data.db.DietCheckEntity>,
        val dayStates: List<com.gympro.app.data.db.DayStateEntity>,
    )

    private val dataFlow = combine(
        dietRepository.observeDietItems(),
        dietRepository.observeSuppItems(),
        dayDao.observeChecks(),
        dayDao.observeDayStates(),
    ) { items, supps, checks, dayStates ->
        DataBundle(items, supps, checks, dayStates)
    }

    val state: StateFlow<FuelState> = combine(
        dataFlow,
        dietRepository.proteinGoal,
        dietRepository.waterTarget,
    ) { data, goal, waterTarget ->
        val items = data.items
        val supps = data.supps
        val checks = data.checks
        val dayStates = data.dayStates
        val today = Dates.todayKey()
        val todayState = dayStates.firstOrNull { it.date == today }
        val checksToday = checks.filter { it.date == today }.map { it.itemId }.toSet()
        val suppRows = supps.filter { dietRepository.isSuppScheduledToday(it.schedule) }
            .map { it to (it.id in checksToday) }

        val weekProtein = (6 downTo 0).map { i ->
            val key = Dates.key(Dates.today().minusDays(i.toLong()))
            Dates.shortWeekday(key) to (dayStates.firstOrNull { it.date == key }?.proteinTotal ?: 0.0)
        }
        val weekWater = (6 downTo 0).map { i ->
            val key = Dates.key(Dates.today().minusDays(i.toLong()))
            Dates.shortWeekday(key) to (dayStates.firstOrNull { it.date == key }?.waterCount?.toDouble() ?: 0.0)
        }

        FuelState(
            items = items,
            supps = suppRows,
            allSupps = supps,
            checks = checksToday,
            extraProtein = todayState?.extraProtein ?: 0.0,
            totalProtein = todayState?.proteinTotal ?: 0.0,
            proteinGoal = goal,
            waterTarget = waterTarget,
            waterCount = todayState?.waterCount ?: 0,
            weekProtein = weekProtein,
            weekWater = weekWater,
            weekWaterTarget = waterTarget,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FuelState())

    // ------------------------------------------------------------------
    // Diet
    // ------------------------------------------------------------------

    fun toggleItem(itemId: String) {
        viewModelScope.launch {
            val crossed = dietRepository.toggleDietItem(itemId)
            if (crossed) {
                toastSink?.invoke("Protein goal reached! 🎯", ToastType.SUCCESS)
            }
        }
    }

    private val _quickProtein = MutableStateFlow("")
    val quickProtein: StateFlow<String> = _quickProtein

    fun setQuickProtein(v: String) {
        _quickProtein.value = v
    }

    fun quickAddProtein() {
        val grams = _quickProtein.value.toDoubleOrNull()
        if (grams == null || grams <= 0) return
        viewModelScope.launch {
            dietRepository.addQuickProtein(grams)
            _quickProtein.value = ""
            toastSink?.invoke("+${trimNumber(grams)}g protein added", ToastType.INFO)
        }
    }

    private fun trimNumber(v: Double): String {
        val s = v.toString()
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    // ------------------------------------------------------------------
    // Water
    // ------------------------------------------------------------------

    fun toggleWater(index: Int) {
        viewModelScope.launch {
            val (count, goalHit) = dietRepository.toggleWater(Dates.todayKey(), index)
            if (goalHit && count == state.value.waterTarget) {
                toastSink?.invoke("Hydration goal hit! 💧", ToastType.SUCCESS)
            }
        }
    }

    // ------------------------------------------------------------------
    // Goals
    // ------------------------------------------------------------------

    private val _goalOpen = MutableStateFlow(false)
    val goalOpen: StateFlow<Boolean> = _goalOpen
    private val _goalMode = MutableStateFlow("protein")
    val goalMode: StateFlow<String> = _goalMode
    private val _goalInput = MutableStateFlow("")
    val goalInput: StateFlow<String> = _goalInput

    fun openGoalEditor(mode: String) {
        _goalMode.value = mode
        val current = if (mode == "protein") state.value.proteinGoal else state.value.waterTarget
        _goalInput.value = current.toString()
        _goalOpen.value = true
    }

    fun closeGoalEditor() {
        _goalOpen.value = false
    }

    fun setGoalInput(v: String) {
        _goalInput.value = v
    }

    fun saveGoal() {
        val value = _goalInput.value.toIntOrNull()
        if (value == null || value < 1) {
            toastSink?.invoke("Enter a valid number", ToastType.INFO)
            return
        }
        viewModelScope.launch {
            if (_goalMode.value == "protein") {
                dietRepository.setProteinGoal(value)
                toastSink?.invoke("Protein goal set to ${value}g", ToastType.SUCCESS)
            } else {
                dietRepository.setWaterTarget(value)
                toastSink?.invoke("Water goal set to $value glasses", ToastType.SUCCESS)
            }
            _goalOpen.value = false
        }
    }

    // ------------------------------------------------------------------
    // Diet / supp manager
    // ------------------------------------------------------------------

    private val _managerOpen = MutableStateFlow(false)
    val managerOpen: StateFlow<Boolean> = _managerOpen
    private val _managerMode = MutableStateFlow("menu")
    val managerMode: StateFlow<String> = _managerMode
    private val _mName = MutableStateFlow("")
    val mName: StateFlow<String> = _mName
    private val _mValue = MutableStateFlow("")
    val mValue: StateFlow<String> = _mValue

    fun openDietManager(mode: String) {
        _managerMode.value = mode
        _mName.value = ""
        _mValue.value = ""
        _managerOpen.value = true
    }

    fun closeDietManager() {
        _managerOpen.value = false
    }

    fun setMName(v: String) {
        _mName.value = v
    }

    fun setMValue(v: String) {
        _mValue.value = v
    }

    fun addItem() {
        val name = _mName.value.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            if (_managerMode.value == "menu") {
                val protein = _mValue.value.toDoubleOrNull() ?: return@launch
                dietRepository.addDietItem(name, protein)
            } else {
                val schedule = _mValue.value.ifEmpty { "daily" }
                dietRepository.addSuppItem(name, schedule)
            }
            _mName.value = ""
            _mValue.value = ""
        }
    }

    fun removeItem(id: String) {
        viewModelScope.launch {
            if (_managerMode.value == "menu") dietRepository.removeDietItem(id)
            else dietRepository.removeSuppItem(id)
        }
    }

    /** Wired by the UI to the app-level toast sink. */
    var toastSink: ((String, ToastType) -> Unit)? = null

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymProApp
                FuelViewModel(app)
            }
        }
    }
}
