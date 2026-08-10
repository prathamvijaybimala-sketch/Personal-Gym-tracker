package com.gympro.app.ui

import android.app.Application
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gympro.app.GymProApp
import com.gympro.app.data.AppContainer
import com.gympro.app.domain.StreakCalculator
import com.gympro.app.ui.components.ToastEvent
import com.gympro.app.ui.components.ToastType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

/**
 * App-level state: theme, header stats (streak / weight / BF), the rest timer,
 * toast + confetti events and the shared body-fat modal.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val container: AppContainer = (application as GymProApp).container
    private val settings = container.settings
    private val workoutRepository = container.workoutRepository
    private val bodyRepository = container.bodyRepository

    data class HeaderStats(
        val streak: Int,
        val weight: String,
        val bf: String,
    )

    data class TimerState(
        val secondsLeft: Int = 90,
        val totalSeconds: Int = 90,
        val running: Boolean = false,
    )

    /** Emits a new value shortly after each local midnight so date-derived state refreshes. */
    private val _midnightTick = MutableStateFlow(0L)
    val midnightTick: StateFlow<Long> = _midnightTick

    val theme: StateFlow<String> = settings.theme.stateIn(
        viewModelScope, SharingStarted.Eagerly, "dark"
    )

    val headerStats: StateFlow<HeaderStats> = combine(
        workoutRepository.observeAttendance(),
        bodyRepository.uwt,
        bodyRepository.observeBodyFat(),
        _midnightTick,
    ) { attendance, uwt, bfLog, _ ->
        val latestBf = bfLog.maxByOrNull { it.date }?.value
        HeaderStats(
            streak = StreakCalculator.calculate(attendance),
            weight = uwt,
            bf = latestBf?.let { formatBf(it) } ?: "--%",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HeaderStats(0, "--", "--%"))

    private fun formatBf(v: Double): String =
        if (v == v.toLong().toDouble()) "${v.toLong()}%" else String.format("%.1f%%", v)

    val activeRoutineName: StateFlow<String?> = container.routineRepository.observeActiveRoutine()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ------------------------------------------------------------------
    // Toasts + confetti
    // ------------------------------------------------------------------

    private val _toastEvents = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 32)
    val toastEvents: MutableSharedFlow<ToastEvent> = _toastEvents

    private val _confettiKey = MutableStateFlow(0)
    val confettiKey: StateFlow<Int> = _confettiKey

    fun showToast(message: String, type: ToastType = ToastType.INFO) {
        _toastEvents.tryEmit(ToastEvent(message, type))
    }

    fun triggerConfetti() {
        _confettiKey.value += 1
    }

    // ------------------------------------------------------------------
    // Theme
    // ------------------------------------------------------------------

    fun toggleTheme() {
        viewModelScope.launch {
            val current = theme.value
            settings.setTheme(if (current == "light") "dark" else "light")
        }
    }

    // ------------------------------------------------------------------
    // Rest timer
    // ------------------------------------------------------------------

    private val _timer = MutableStateFlow(TimerState())
    val timer: StateFlow<TimerState> = _timer

    val timerDuration: StateFlow<Int> = settings.timerDuration.stateIn(
        viewModelScope, SharingStarted.Eagerly, 90
    )

    fun selectTimerDuration(seconds: Int) {
        viewModelScope.launch { settings.setTimerDuration(seconds) }
    }

    fun startTimer() {
        val seconds = timerDuration.value
        _timer.value = TimerState(secondsLeft = seconds, totalSeconds = seconds, running = true)
        viewModelScope.launch {
            while (isActive && _timer.value.running) {
                delay(1000)
                val current = _timer.value
                if (!current.running) break
                val next = current.secondsLeft - 1
                if (next <= 0) {
                    _timer.value = current.copy(secondsLeft = 0, running = false)
                    vibrate()
                    showToast("Time's up! Hit that set 💥", ToastType.SUCCESS)
                    break
                }
                _timer.value = current.copy(secondsLeft = next)
            }
        }
    }

    fun stopTimer() {
        _timer.value = _timer.value.copy(running = false)
    }

    private fun vibrate() {
        val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(200, 100, 200, 100, 200), -1))
    }

    // ------------------------------------------------------------------
    // Body-fat modal (shared with the header + Home tools)
    // ------------------------------------------------------------------

    data class BfModalState(
        val open: Boolean = false,
        val height: String = "",
        val neck: String = "",
        val waist: String = "",
        val result: String? = null,
    )

    private val _bfModal = MutableStateFlow(BfModalState())
    val bfModal: StateFlow<BfModalState> = _bfModal

    fun openBfModal() {
        viewModelScope.launch {
            val savedHeight = settings.bfHeight.firstOrNull() ?: ""
            _bfModal.value = BfModalState(open = true, height = savedHeight)
        }
    }

    fun closeBfModal() = _bfModal.update { it.copy(open = false) }

    fun setBfHeight(v: String) = _bfModal.update { it.copy(height = v) }
    fun setBfNeck(v: String) = _bfModal.update { it.copy(neck = v) }
    fun setBfWaist(v: String) = _bfModal.update { it.copy(waist = v) }

    fun calcAndSaveBf() {
        val state = _bfModal.value
        val h = state.height.toDoubleOrNull()
        val n = state.neck.toDoubleOrNull()
        val w = state.waist.toDoubleOrNull()
        if (h == null || n == null || w == null) {
            showToast("Enter height, neck and waist", ToastType.INFO)
            return
        }
        viewModelScope.launch {
            val result = bodyRepository.calcAndLogBodyFat(h, n, w)
            _bfModal.update { it.copy(result = "$result% Body Fat") }
            showToast("BF: $result% logged", ToastType.SUCCESS)
        }
    }

    // ------------------------------------------------------------------
    // Midnight ticker
    // ------------------------------------------------------------------

    init {
        viewModelScope.launch {
            while (isActive) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                val millis = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1000)
                delay(millis)
                _midnightTick.value += 1
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymProApp
                AppViewModel(app)
            }
        }
    }
}

private fun <T> MutableStateFlow<T>.update(block: (T) -> T) {
    value = block(value)
}
