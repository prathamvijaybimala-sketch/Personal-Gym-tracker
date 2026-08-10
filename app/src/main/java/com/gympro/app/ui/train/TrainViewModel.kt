package com.gympro.app.ui.train

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gympro.app.GymProApp
import com.gympro.app.data.AppContainer
import com.gympro.app.data.db.CardioEntity
import com.gympro.app.data.db.DaysCodec.DayData
import com.gympro.app.domain.AIPromptBuilder
import com.gympro.app.domain.Dates
import com.gympro.app.domain.PrDetector
import com.gympro.app.domain.WorkoutJsonParser
import com.gympro.app.ui.components.ToastType
import com.gympro.app.ui.modals.GraphData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.min

class TrainViewModel(application: Application) : AndroidViewModel(application) {

    private val container: AppContainer = (application as GymProApp).container
    private val routineRepository = container.routineRepository
    private val workoutRepository = container.workoutRepository
    private val bodyRepository = container.bodyRepository
    private val exerciseDao = container.database.exerciseDao()

    // ------------------------------------------------------------------
    // Main state
    // ------------------------------------------------------------------

    data class ExCard(
        val id: String,
        val name: String,
        val target: String,
        val link: String,
        val draftW: String,
        val draftR: String,
        val lastText: String,
        val est1rm: Int?,
        val isPr: Boolean,
        val hasData: Boolean,
        val volumeWidth: Float,
    )

    data class TrainState(
        val routineName: String? = null,
        val day: Int = Dates.jsDayOfWeek(LocalDate.now()),
        val dayTitle: String = "",
        val isRestDay: Boolean = false,
        val cards: List<ExCard> = emptyList(),
        val hasExercises: Boolean = false,
        val dayLogs: Map<Int, Boolean> = emptyMap(),
        val todayKey: String = Dates.todayKey(),
    )

    private val selectedDay = MutableStateFlow(Dates.jsDayOfWeek(LocalDate.now()))

    val state: StateFlow<TrainState> = combine(
        routineRepository.observeActiveRoutine(),
        selectedDay,
        workoutRepository.observeDrafts(Dates.todayKey()),
        workoutRepository.observeAttendance(),
        exerciseDao.observeAllHistory(),
    ) { routine, day, drafts, attendance, allHistory ->
        val dayData = routine?.days?.get(day) ?: DayData("", emptyList())
        val histByEx = allHistory.groupBy { it.exerciseId }
        val dayName = Dates.DAY_NAMES[day]

        val title = if (day == 0) {
            "REST DAY"
        } else {
            val n = dayData.name
            val rest = dayName.substring(min(n.length, dayName.length))
            "$n $rest".trim()
        }

        val cards = dayData.exercises.map { ex ->
            val hist = histByEx[ex.id].orEmpty()
            val last = hist.lastOrNull()
            val draft = drafts[ex.id]
            val w = draft?.weight?.toDoubleOrNull()
            val r = draft?.reps?.toIntOrNull()
            val est = if (w != null && r != null) PrDetector.est1rm(w, r) else null
            val max = PrDetector.maxEst1rm(hist)
            val width = if (est != null) {
                if (max <= 0) 100f else min(est.toFloat() / max * 100f, 100f)
            } else 0f
            ExCard(
                id = ex.id,
                name = ex.name,
                target = ex.target,
                link = ex.link,
                draftW = draft?.weight ?: "",
                draftR = draft?.reps ?: "",
                lastText = last?.let { "Last: ${trimNumber(it.weight)}kg×${it.reps}" } ?: "New",
                est1rm = est,
                isPr = est != null && PrDetector.isNewPr(est, max, hist.isNotEmpty()),
                hasData = est != null,
                volumeWidth = width,
            )
        }

        TrainState(
            routineName = routine?.name,
            day = day,
            dayTitle = title,
            isRestDay = day == 0,
            cards = cards,
            hasExercises = dayData.exercises.isNotEmpty(),
            dayLogs = (0..6).associateWith { i -> Dates.thisWeekDate(i) in attendance },
            todayKey = Dates.todayKey(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrainState())

    private fun trimNumber(v: Double): String {
        val s = v.toString()
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    fun selectDay(day: Int) {
        selectedDay.value = day
    }

    fun updateDraft(exerciseId: String, weight: String, reps: String) {
        viewModelScope.launch {
            workoutRepository.saveDraft(exerciseId, weight, reps)
        }
    }

    fun copyLast(exerciseId: String) {
        viewModelScope.launch {
            val copied = workoutRepository.copyLast(exerciseId)
            if (copied == null) {
                toastSink?.invoke("No history found", ToastType.INFO)
            } else {
                toastSink?.invoke("Last session copied", ToastType.INFO)
            }
        }
    }

    fun finishWorkout() {
        val current = state.value
        viewModelScope.launch {
            val routine = routineRepository.getActiveRoutine() ?: return@launch
            val day = routine.days[current.day] ?: return@launch
            val result = workoutRepository.finishWorkout(day)
            if (result.prCount > 0) {
                val msg = "${result.prCount} new PR${if (result.prCount > 1) "s" else ""}! 🏆"
                toastSink?.invoke(msg, ToastType.PR)
                confettiSink?.invoke()
            }
            toastSink?.invoke("Workout saved! ${result.logged} exercises logged 💪", ToastType.SUCCESS)
        }
    }

    // ------------------------------------------------------------------
    // Exercise graph
    // ------------------------------------------------------------------

    private val _graph = MutableStateFlow<GraphData?>(null)
    val graph: StateFlow<GraphData?> = _graph

    fun openExerciseGraph(exerciseId: String, title: String) {
        viewModelScope.launch {
            val hist = exerciseDao.history(exerciseId)
            val labels = hist.map { it.date.slice(5) }
            val data = hist.map { PrDetector.est1rm(it.weight, it.reps).toDouble() }
            val latest = if (data.isNotEmpty()) trimNumber(data.last()) else "--"
            val peak = if (data.isNotEmpty()) trimNumber(data.maxOrNull() ?: 0.0) else "--"
            var change = "--"
            var changeUp = false
            if (data.size >= 2) {
                val d = data.last() - data[data.size - 2]
                change = "${if (d > 0) "+" else ""}${String.format("%.1f", d)}"
                changeUp = d >= 0
            }
            _graph.value = GraphData(title.uppercase(), labels, data, latest, peak, change, changeUp)
        }
    }

    fun closeGraph() {
        _graph.value = null
    }

    // ------------------------------------------------------------------
    // Routine manager
    // ------------------------------------------------------------------

    private val _routineManagerOpen = MutableStateFlow(false)
    val routineManagerOpen: StateFlow<Boolean> = _routineManagerOpen

    val routineSummaries: StateFlow<List<com.gympro.app.data.repo.RoutineRepository.RoutineSummary>> =
        routineRepository.observeSummaries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _newRoutineName = MutableStateFlow("")
    val newRoutineName: StateFlow<String> = _newRoutineName

    private val _renameTarget = MutableStateFlow<Pair<String, String>?>(null) // id to current name
    val renameTarget: StateFlow<Pair<String, String>?> = _renameTarget

    private val _deleteTarget = MutableStateFlow<String?>(null)
    val deleteTarget: StateFlow<String?> = _deleteTarget

    fun openRoutineManager() {
        _routineManagerOpen.value = true
    }

    fun closeRoutineManager() {
        _routineManagerOpen.value = false
    }

    fun setNewRoutineName(v: String) {
        _newRoutineName.value = v
    }

    fun createRoutine() {
        val name = _newRoutineName.value.trim()
        if (name.isEmpty()) {
            toastSink?.invoke("Enter a routine name", ToastType.INFO)
            return
        }
        viewModelScope.launch {
            routineRepository.createRoutine(name)
            _newRoutineName.value = ""
            selectedDay.value = 1
            toastSink?.invoke("\"$name\" created & activated", ToastType.SUCCESS)
        }
    }

    fun switchRoutine(id: String) {
        viewModelScope.launch {
            routineRepository.switchRoutine(id)
            toastSink?.invoke("Routine switched", ToastType.SUCCESS)
        }
    }

    fun requestRename(id: String, currentName: String) {
        _renameTarget.value = id to currentName
    }

    fun setRenameName(v: String) {
        _renameTarget.value = _renameTarget.value?.let { it.first to v }
    }

    fun confirmRename() {
        val target = _renameTarget.value ?: return
        val newName = target.second.trim()
        _renameTarget.value = null
        if (newName.isEmpty()) return
        viewModelScope.launch {
            routineRepository.renameRoutine(target.first, newName)
        }
    }

    fun cancelRename() {
        _renameTarget.value = null
    }

    fun requestDelete(id: String) {
        _deleteTarget.value = id
    }

    fun confirmDelete() {
        val id = _deleteTarget.value ?: return
        _deleteTarget.value = null
        viewModelScope.launch {
            val ok = routineRepository.deleteRoutine(id)
            if (!ok) {
                toastSink?.invoke("Can't delete the only routine", ToastType.INFO)
            } else {
                toastSink?.invoke("Routine deleted", ToastType.INFO)
            }
        }
    }

    fun cancelDelete() {
        _deleteTarget.value = null
    }

    // ------------------------------------------------------------------
    // Exercise manager
    // ------------------------------------------------------------------

    private val _exManagerOpen = MutableStateFlow(false)
    val exManagerOpen: StateFlow<Boolean> = _exManagerOpen

    private val _exManagerDay = MutableStateFlow(0)
    val exManagerDay: StateFlow<Int> = _exManagerDay

    private val _exManagerList = MutableStateFlow<List<ExManagerRow>>(emptyList())
    val exManagerList: StateFlow<List<ExManagerRow>> = _exManagerList

    private val _exManagerDayName = MutableStateFlow("")
    val exManagerDayName: StateFlow<String> = _exManagerDayName

    private val _exFormName = MutableStateFlow("")
    val exFormName: StateFlow<String> = _exFormName
    private val _exFormTarget = MutableStateFlow("")
    val exFormTarget: StateFlow<String> = _exFormTarget
    private val _exFormLink = MutableStateFlow("")
    val exFormLink: StateFlow<String> = _exFormLink
    private val _editingIndex = MutableStateFlow(-1)
    val editingIndex: StateFlow<Int> = _editingIndex

    data class ExManagerRow(val index: Int, val id: String, val name: String, val target: String, val link: String)

    fun openExerciseManager(day: Int) {
        if (day == 0) {
            toastSink?.invoke("Sunday is rest day", ToastType.INFO)
            return
        }
        _exManagerDay.value = day
        _exManagerOpen.value = true
        refreshExManager()
        resetExForm()
    }

    fun closeExerciseManager() {
        _exManagerOpen.value = false
    }

    private fun refreshExManager() {
        viewModelScope.launch {
            val routine = routineRepository.getActiveRoutine() ?: return@launch
            val day = routine.days[_exManagerDay.value] ?: DayData("", emptyList())
            _exManagerList.value = day.exercises.mapIndexed { i, ex ->
                ExManagerRow(i, ex.id, ex.name, ex.target, ex.link)
            }
            _exManagerDayName.value = day.name
        }
    }

    fun setExDayName(v: String) {
        _exManagerDayName.value = v
    }

    fun setExFormName(v: String) {
        _exFormName.value = v
    }

    fun setExFormTarget(v: String) {
        _exFormTarget.value = v
    }

    fun setExFormLink(v: String) {
        _exFormLink.value = v
    }

    fun startEditExercise(index: Int) {
        val row = _exManagerList.value.getOrNull(index) ?: return
        _editingIndex.value = index
        _exFormName.value = row.name
        _exFormTarget.value = row.target
        _exFormLink.value = row.link
    }

    fun cancelExerciseEdit() {
        resetExForm()
    }

    private fun resetExForm() {
        _editingIndex.value = -1
        _exFormName.value = ""
        _exFormTarget.value = ""
        _exFormLink.value = ""
    }

    fun saveExerciseFromManager() {
        val name = _exFormName.value.trim()
        if (name.isEmpty()) {
            toastSink?.invoke("Enter an exercise name", ToastType.INFO)
            return
        }
        val day = _exManagerDay.value
        val index = _editingIndex.value
        viewModelScope.launch {
            if (index >= 0) {
                routineRepository.updateExercise(
                    day, index,
                    com.gympro.app.data.db.DaysCodec.ExerciseData(
                        id = _exManagerList.value[index].id,
                        name = name,
                        target = _exFormTarget.value.trim(),
                        link = _exFormLink.value.trim(),
                    )
                )
                toastSink?.invoke("Exercise updated", ToastType.SUCCESS)
            } else {
                routineRepository.addExercise(
                    day,
                    com.gympro.app.data.db.DaysCodec.ExerciseData(
                        id = "ex" + System.currentTimeMillis(),
                        name = name,
                        target = _exFormTarget.value.trim(),
                        link = _exFormLink.value.trim(),
                    )
                )
                toastSink?.invoke("$name added", ToastType.SUCCESS)
            }
            // Persist day rename if changed
            val routine = routineRepository.getActiveRoutine() ?: return@launch
            val current = routine.days[day] ?: return@launch
            if (current.name != _exManagerDayName.value) {
                routineRepository.updateDay(day, current.copy(name = _exManagerDayName.value))
            }
            resetExForm()
            refreshExManager()
        }
    }

    fun removeExercise(index: Int) {
        val day = _exManagerDay.value
        val name = _exManagerList.value.getOrNull(index)?.name ?: ""
        viewModelScope.launch {
            routineRepository.removeExercise(day, index)
            refreshExManager()
            toastSink?.invoke("$name removed", ToastType.INFO)
        }
    }

    // ------------------------------------------------------------------
    // Import via JSON
    // ------------------------------------------------------------------

    private val _importOpen = MutableStateFlow(false)
    val importOpen: StateFlow<Boolean> = _importOpen

    private val _importText = MutableStateFlow("")
    val importText: StateFlow<String> = _importText

    fun openImportModal() {
        _routineManagerOpen.value = false
        _importText.value = ""
        _importOpen.value = true
    }

    fun closeImportModal() {
        _importOpen.value = false
    }

    fun setImportText(v: String) {
        _importText.value = v
    }

    fun importWorkoutJson() {
        val raw = _importText.value.trim()
        if (raw.isEmpty()) {
            toastSink?.invoke("Paste JSON first", ToastType.INFO)
            return
        }
        val parsed = WorkoutJsonParser.parse(raw)
        if (parsed == null) {
            toastSink?.invoke("Invalid JSON — check format", ToastType.INFO)
            return
        }
        viewModelScope.launch {
            val days = parsed.days.mapValues { (_, d) ->
                DayData(d.name, d.exercises.map { ex ->
                    com.gympro.app.data.db.DaysCodec.ExerciseData(ex.id, ex.name, ex.target, ex.link)
                })
            }
            routineRepository.importRoutine(parsed.name, days)
            _importOpen.value = false
            selectedDay.value = 1
            toastSink?.invoke("\"${parsed.name}\" imported!", ToastType.SUCCESS)
        }
    }

    // ------------------------------------------------------------------
    // AI prompt generator
    // ------------------------------------------------------------------

    private val _aiOpen = MutableStateFlow(false)
    val aiOpen: StateFlow<Boolean> = _aiOpen

    private val _aiType = MutableStateFlow("")
    val aiType: StateFlow<String> = _aiType

    private val _aiPrompt = MutableStateFlow<String?>(null)
    val aiPrompt: StateFlow<String?> = _aiPrompt

    fun openAiPromptModal() {
        _routineManagerOpen.value = false
        _aiType.value = ""
        _aiPrompt.value = null
        _aiOpen.value = true
    }

    fun closeAiPromptModal() {
        _aiOpen.value = false
    }

    fun setAiType(v: String) {
        _aiType.value = v
    }

    fun generatePrompt() {
        val type = _aiType.value.trim()
        if (type.isEmpty()) {
            toastSink?.invoke("Describe your workout first", ToastType.INFO)
            return
        }
        _aiPrompt.value = AIPromptBuilder.build(type)
    }

    fun copyPrompt() {
        val text = _aiPrompt.value ?: return
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AI Prompt", text))
        toastSink?.invoke("Prompt copied!", ToastType.SUCCESS)
    }

    // ------------------------------------------------------------------
    // Cardio log
    // ------------------------------------------------------------------

    private val _cardioOpen = MutableStateFlow(false)
    val cardioOpen: StateFlow<Boolean> = _cardioOpen

    private val _cardioType = MutableStateFlow("Treadmill")
    val cardioType: StateFlow<String> = _cardioType
    private val _cardioDuration = MutableStateFlow("30")
    val cardioDuration: StateFlow<String> = _cardioDuration
    private val _cardioCalories = MutableStateFlow("")
    val cardioCalories: StateFlow<String> = _cardioCalories
    private val _cardioDistance = MutableStateFlow("")
    val cardioDistance: StateFlow<String> = _cardioDistance
    private val _cardioNotes = MutableStateFlow("")
    val cardioNotes: StateFlow<String> = _cardioNotes

    private val _cardioHistory = MutableStateFlow<List<CardioEntity>>(emptyList())
    val cardioHistory: StateFlow<List<CardioEntity>> = _cardioHistory

    val cardioTypes = listOf(
        "Treadmill", "Bike", "Elliptical", "Rowing", "Stairmaster", "Jump Rope",
        "Swimming", "Running", "Walking", "HIIT", "Other",
    )

    fun openCardioLog() {
        _cardioOpen.value = true
        refreshCardioHistory()
    }

    fun closeCardioLog() {
        _cardioOpen.value = false
    }

    fun setCardioType(v: String) {
        _cardioType.value = v
    }

    fun adjustCardioDuration(delta: Int) {
        val v = (_cardioDuration.value.toIntOrNull() ?: 0) + delta
        _cardioDuration.value = v.coerceAtLeast(0).toString()
    }

    fun setCardioDuration(v: String) {
        _cardioDuration.value = v
    }

    fun adjustCardioCalories(delta: Int) {
        val v = (_cardioCalories.value.toIntOrNull() ?: 0) + delta
        _cardioCalories.value = v.coerceAtLeast(0).toString()
    }

    fun setCardioCalories(v: String) {
        _cardioCalories.value = v
    }

    fun setCardioDistance(v: String) {
        _cardioDistance.value = v
    }

    fun setCardioNotes(v: String) {
        _cardioNotes.value = v
    }

    fun logCardio() {
        val duration = _cardioDuration.value.toIntOrNull()
        if (duration == null || duration < 1) {
            toastSink?.invoke("Enter duration", ToastType.INFO)
            return
        }
        val entry = CardioEntity(
            id = "c" + System.currentTimeMillis(),
            date = Dates.todayKey(),
            type = _cardioType.value,
            duration = duration,
            calories = _cardioCalories.value.toIntOrNull(),
            distance = _cardioDistance.value.toDoubleOrNull(),
            notes = _cardioNotes.value.trim(),
        )
        viewModelScope.launch {
            bodyRepository.logCardio(entry)
            _cardioCalories.value = ""
            _cardioDistance.value = ""
            _cardioNotes.value = ""
            refreshCardioHistory()
            toastSink?.invoke("${entry.type} · ${entry.duration} min logged 🏃", ToastType.SUCCESS)
        }
    }

    fun deleteCardio(id: String) {
        viewModelScope.launch {
            bodyRepository.deleteCardio(id)
            refreshCardioHistory()
            toastSink?.invoke("Entry removed", ToastType.INFO)
        }
    }

    private fun refreshCardioHistory() {
        viewModelScope.launch {
            _cardioHistory.value = bodyRepository.allCardio().reversed().take(15)
        }
    }

    /** Wired by the UI to the app-level toast/confetti sinks. */
    var toastSink: ((String, ToastType) -> Unit)? = null
    var confettiSink: (() -> Unit)? = null

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymProApp
                TrainViewModel(app)
            }
        }
    }
}
