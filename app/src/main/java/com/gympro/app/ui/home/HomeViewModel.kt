package com.gympro.app.ui.home

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gympro.app.GymProApp
import com.gympro.app.data.AppContainer
import com.gympro.app.data.db.BodyWeightEntity
import com.gympro.app.data.db.DaysCodec
import com.gympro.app.data.db.DayStateEntity
import com.gympro.app.domain.Dates
import com.gympro.app.domain.PrDetector
import com.gympro.app.ui.components.ToastType
import com.gympro.app.ui.modals.GraphData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container: AppContainer = (application as GymProApp).container
    private val settings = container.settings
    private val workoutRepository = container.workoutRepository
    private val bodyRepository = container.bodyRepository
    private val dietRepository = container.dietRepository
    private val routineRepository = container.routineRepository
    private val backupRepository = container.backupRepository

    private val dayDao = container.database.dayDao()
    private val exerciseDao = container.database.exerciseDao()
    private val configDao = container.database.configDao()

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    enum class CellStatus { GREEN, YELLOW, RED, PLAIN }

    data class WeekDot(val dateKey: String, val label: String, val done: Boolean, val isFuture: Boolean)

    data class CalendarCell(
        val day: Int,
        val dateKey: String,
        val status: CellStatus,
        val isToday: Boolean,
        val hasNote: Boolean,
    )

    data class HomeState(
        val weekDots: List<WeekDot> = emptyList(),
        val weekVolume: Int = 0,
        val calendarMode: String = "gym",
        val monthLabel: String = "",
        val calendarCells: List<CalendarCell?> = emptyList(),
        val latestWeight: String = "--",
        val weightDelta: String? = null,
        val weightDeltaIsUp: Boolean = false,
        val latestBf: String = "--%",
        val mood: String? = null,
        val weightInput: String = "",
        val exportUri: Uri? = null,
    )

    data class HistoryWorkoutRow(val exerciseName: String, val weight: Double, val reps: Int)

    data class HistoryData(
        val dateKey: String,
        val title: String,
        val note: String,
        val workoutRows: List<HistoryWorkoutRow>,
        val proteinTotal: Double,
        val checkedFoods: List<String>,
        val checkedSupps: List<String>,
        val mood: String?,
    )

    private val monthFlow = MutableStateFlow(YearMonth.now())
    private val modeFlow = MutableStateFlow("gym")
    private val midnightTick = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            while (true) {
                val now = java.time.LocalDateTime.now()
                val next = now.toLocalDate().plusDays(1).atStartOfDay()
                kotlinx.coroutines.delay(
                    java.time.Duration.between(now, next).toMillis().coerceAtLeast(1000)
                )
                midnightTick.value += 1
            }
        }
    }

    // combine() supports at most 5 typed flows, so bundle the data sources first.
    private data class DataBundle(
        val attendance: Set<String>,
        val dayStates: List<DayStateEntity>,
        val weights: List<BodyWeightEntity>,
        val bfLog: List<com.gympro.app.data.db.BodyFatEntity>,
        val uwt: String,
    )

    private val dataFlow = combine(
        workoutRepository.observeAttendance(),
        dayDao.observeDayStates(),
        bodyRepository.observeWeights(),
        bodyRepository.observeBodyFat(),
        bodyRepository.uwt,
    ) { attendance, dayStates, weights, bfLog, uwt ->
        DataBundle(attendance, dayStates, weights, bfLog, uwt)
    }

    private data class ViewBundle(val month: YearMonth, val mode: String, val tick: Long)

    private val viewFlow = combine(monthFlow, modeFlow, midnightTick) { month, mode, tick ->
        ViewBundle(month, mode, tick)
    }

    private val _history = MutableStateFlow<HistoryData?>(null)
    val history: StateFlow<HistoryData?> = _history

    private val _graph = MutableStateFlow<GraphData?>(null)
    val graph: StateFlow<GraphData?> = _graph

    val state: StateFlow<HomeState> = combine(dataFlow, viewFlow) { data, view ->
        val attendance = data.attendance
        val dayStates = data.dayStates
        val weights = data.weights
        val bfLog = data.bfLog
        val uwt = data.uwt
        val month = view.month
        val mode = view.mode
        val todayKey = Dates.todayKey()
        val today = Dates.today()
        val dayStateMap = dayStates.associateBy { it.date }
        val noteDates = dayStates.filter { !it.note.isNullOrEmpty() }.map { it.date }.toSet()

        // This week
        val monday = Dates.mondayOfWeek(today)
        val dots = (0..6).map { i ->
            val d = monday.plusDays(i.toLong())
            WeekDot(
                dateKey = Dates.key(d),
                label = Dates.DAY_SHORT[(i + 1) % 7],
                done = Dates.key(d) in attendance,
                isFuture = d.isAfter(today),
            )
        }
        val volume = dots.count { it.done }

        // Calendar
        val firstDow = Dates.jsDayOfWeek(month.atDay(1))
        val daysInMonth = month.lengthOfMonth()
        val cells: List<CalendarCell?> = List(firstDow) { null } + (1..daysInMonth).map { day ->
            val date = LocalDate.of(month.year, month.monthValue, day)
            val ds = Dates.key(date)
            val isPast = date.isBefore(today)
            val status = if (mode == "gym") {
                when {
                    ds in attendance -> CellStatus.GREEN
                    isPast -> CellStatus.RED
                    else -> CellStatus.PLAIN
                }
            } else {
                val total = dayStateMap[ds]?.proteinTotal ?: 0.0
                when {
                    total >= 105.0 -> CellStatus.GREEN
                    total >= 85.0 -> CellStatus.YELLOW
                    isPast -> CellStatus.RED
                    else -> CellStatus.PLAIN
                }
            }
            CalendarCell(day, ds, status, ds == todayKey, ds in noteDates)
        }

        // Weight delta vs previous log
        val sortedWeights = weights.sortedBy { it.date }
        var delta: String? = null
        var deltaUp = false
        if (sortedWeights.size >= 2) {
            val latest = sortedWeights.last().weight
            val prev = sortedWeights[sortedWeights.size - 2].weight
            val d = latest - prev
            val formatted = String.format("%.1f", kotlin.math.abs(d))
            delta = (if (d > 0) "▲ " else "▼ ") + formatted + "kg"
            deltaUp = d > 0
        }

        val latestBf = bfLog.maxByOrNull { it.date }?.value
        val bfText = latestBf?.let { formatBf(it) } ?: "--%"
        val mood = dayStateMap[todayKey]?.mood

        HomeState(
            weekDots = dots,
            weekVolume = volume,
            calendarMode = mode,
            monthLabel = Dates.monthYearLabel(month.year, month.monthValue),
            calendarCells = cells,
            latestWeight = uwt,
            weightDelta = delta,
            weightDeltaIsUp = deltaUp,
            latestBf = bfText,
            mood = mood,
            weightInput = _weightInput.value,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    private fun formatBf(v: Double): String =
        if (v == v.toLong().toDouble()) "${v.toLong()}%" else String.format("%.1f%%", v)

    private val _weightInput = MutableStateFlow("")

    // ------------------------------------------------------------------
    // Calendar / week
    // ------------------------------------------------------------------

    fun changeMonth(delta: Int) {
        monthFlow.value = monthFlow.value.plusMonths(delta.toLong())
    }

    fun jumpToToday() {
        monthFlow.value = YearMonth.now()
    }

    fun setCalendarMode(mode: String) {
        modeFlow.value = mode
    }

    fun setWeightInput(v: String) {
        _weightInput.value = v
    }

    fun logWeight() {
        val w = _weightInput.value.toDoubleOrNull() ?: return
        viewModelScope.launch {
            bodyRepository.logWeight(w)
            _weightInput.value = ""
            showToast("${trimNumber(w)}kg logged", ToastType.SUCCESS)
        }
    }

    private fun trimNumber(v: Double): String {
        val s = v.toString()
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    fun saveMood(mood: String) {
        viewModelScope.launch {
            dietRepository.saveMood(Dates.todayKey(), mood)
            showToast("Mood logged $mood", ToastType.INFO)
        }
    }

    // ------------------------------------------------------------------
    // History modal
    // ------------------------------------------------------------------

    fun openHistory(dateKey: String) {
        viewModelScope.launch {
            val routine = routineRepository.getActiveRoutine()
            val rows = mutableListOf<HistoryWorkoutRow>()
            if (routine != null) {
                (0..6).forEach { dayNum ->
                    val day = routine.days[dayNum] ?: return@forEach
                    day.exercises.forEach { ex ->
                        val hit = exerciseDao.history(ex.id).firstOrNull { it.date == dateKey }
                        if (hit != null) rows.add(HistoryWorkoutRow(ex.name, hit.weight, hit.reps))
                    }
                }
            }
            val state = dayDao.dayState(dateKey)
            val checks = dayDao.checks(dateKey).map { it.itemId }.toSet()
            val foods = configDao.dietItems().filter { it.id in checks }.map { it.name }
            val supps = configDao.suppItems().filter { it.id in checks }.map { it.name }
            _history.value = HistoryData(
                dateKey = dateKey,
                title = Dates.displayDate(dateKey),
                note = state?.note ?: "",
                workoutRows = rows,
                proteinTotal = state?.proteinTotal ?: 0.0,
                checkedFoods = foods,
                checkedSupps = supps,
                mood = state?.mood,
            )
        }
    }

    fun closeHistory() {
        _history.value = null
    }

    fun saveNote(dateKey: String, note: String) {
        viewModelScope.launch {
            dietRepository.saveNote(dateKey, note)
        }
    }

    // ------------------------------------------------------------------
    // Graph modal
    // ------------------------------------------------------------------

    fun openGraph(type: String, title: String, exerciseId: String? = null) {
        viewModelScope.launch {
            val labels: MutableList<String> = mutableListOf()
            val data: MutableList<Double> = mutableListOf()
            when (type) {
                "wt" -> {
                    bodyRepository.allWeights().sortedBy { it.date }.forEach {
                        labels.add(it.date.slice(5))
                        data.add(it.weight)
                    }
                }
                "bf" -> {
                    bodyRepository.allBodyFat().sortedBy { it.date }.forEach {
                        labels.add(it.date.slice(5))
                        data.add(it.value)
                    }
                }
                else -> {
                    val exId = exerciseId ?: return@launch
                    exerciseDao.history(exId).forEach {
                        labels.add(it.date.slice(5))
                        data.add(PrDetector.est1rm(it.weight, it.reps).toDouble())
                    }
                }
            }
            val latest = if (data.isNotEmpty()) trimNumber(data.last()) else "--"
            val peak = if (data.isNotEmpty()) trimNumber(data.maxOrNull() ?: 0.0) else "--"
            var change = "--"
            var changeUp = false
            if (data.size >= 2) {
                val d = data.last() - data[data.size - 2]
                val sign = if (d > 0) "+" else ""
                change = "$sign${String.format("%.1f", d)}"
                changeUp = d >= 0
            }
            _graph.value = com.gympro.app.ui.modals.GraphData(title.uppercase(), labels, data, latest, peak, change, changeUp)
        }
    }

    fun closeGraph() {
        _graph.value = null
    }

    // ------------------------------------------------------------------
    // Backup / restore
    // ------------------------------------------------------------------

    fun exportData() {
        viewModelScope.launch {
            val json = backupRepository.exportJson()
            val dir = File(getApplication<Application>().cacheDir, "backup").apply { mkdirs() }
            val file = File(dir, "gympro_backup_${Dates.todayKey()}.json")
            file.writeText(json)
            val uri = FileProvider.getUriForFile(
                getApplication(),
                "${getApplication<Application>().packageName}.fileprovider",
                file
            )
            _exportUri.value = uri
            showToast("Backup exported", ToastType.SUCCESS)
        }
    }

    private val _exportUri = MutableStateFlow<Uri?>(null)
    val exportUri: StateFlow<Uri?> = _exportUri

    fun consumeExportUri() {
        _exportUri.value = null
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = getApplication<Application>().contentResolver
                    .openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@launch
                val result = backupRepository.importJson(text)
                if (result.ok) {
                    showToast("Backup restored 💾", ToastType.SUCCESS)
                } else {
                    showToast(result.message, ToastType.INFO)
                }
            } catch (e: Exception) {
                showToast("Invalid backup file", ToastType.INFO)
            }
        }
    }

    private fun showToast(message: String, type: ToastType) {
        // Route through a callback the UI wires to the app-level toast host.
        toastSink?.invoke(message, type)
    }

    /** Wired by MainActivity to AppViewModel.showToast. */
    var toastSink: ((String, ToastType) -> Unit)? = null

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymProApp
                HomeViewModel(app)
            }
        }
    }
}
