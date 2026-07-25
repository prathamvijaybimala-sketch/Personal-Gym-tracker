/* REST TIMER */
function renderTimerPresets() {
    const saved = parseInt(localStorage.getItem('timerDuration'))||90;
    timerSelected = saved;
    const c = document.getElementById('timerPresets');
    c.innerHTML = TIMER_PRESETS.map(s => `<button class="timer-preset ${s===saved?'active-preset':''}" onclick="selectTimer(${s})">${s}s</button>`).join('');
    document.getElementById('timerDurationLabel').innerText = saved + 's selected';
}
function selectTimer(s) {
    timerSelected = s;
    localStorage.setItem('timerDuration', s);
    renderTimerPresets();
}
function startTimer() {
    let s = timerSelected;
    timerTotal = s;
    const overlay = document.getElementById('timerOverlay');
    const val = document.getElementById('timerVal');
    const arc = document.getElementById('timerRingArc');
    const circumference = 81.68;
    overlay.style.display = 'block';
    if(timerInterval) clearInterval(timerInterval);
    val.innerText = s;
    arc.style.strokeDashoffset = 0;
    timerInterval = setInterval(() => {
        s--;
        val.innerText = s;
        arc.style.strokeDashoffset = circumference * (1 - s/timerTotal);
        if(s <= 0) {
            stopTimer();
            try { navigator.vibrate([200,100,200,100,200]); } catch(e){}
            showToast("Time's up! Hit that set 💥", 'success', '⏱');
        }
    }, 1000);
}
function stopTimer() {
    clearInterval(timerInterval);
    timerInterval = null;
    document.getElementById('timerOverlay').style.display = 'none';
}

// ============================================================