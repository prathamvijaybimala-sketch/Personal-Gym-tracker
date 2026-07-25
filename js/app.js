/* APP INIT, NAV, MODALS & MISC */
function init() {
    if(!localStorage.getItem('dietConfig')) localStorage.setItem('dietConfig', JSON.stringify(defaultDiet));
    if(!localStorage.getItem('suppConfig')) localStorage.setItem('suppConfig', JSON.stringify(defaultSupps));
    // Init routines system (migrates legacy workoutsConfig if needed)
    getRoutines();
    const theme = localStorage.getItem('theme') || 'dark';
    if(theme === 'light') document.documentElement.setAttribute('data-theme', 'light');
    else document.documentElement.removeAttribute('data-theme');

    updateHeaderStats();
    if(localStorage.getItem('bfHeight')) document.getElementById('bfHeight').value = localStorage.getItem('bfHeight');

    renderCalendar();
    renderWorkout(new Date().getDay());
    renderDiet();
    renderWeekSummary();
    renderMood();
    renderWater();
    renderTimerPresets();
    updateRoutinePill();
}

function updateHeaderStats() {
    const w = localStorage.getItem('uwt') || '--';
    document.getElementById('headerWt').innerText = w;
    document.getElementById('statWt').innerText = w === '--' ? '--' : w + 'kg';

    const bfLog = JSON.parse(localStorage.getItem('bfLog')) || {};
    const dates = Object.keys(bfLog).sort();
    const latestBF = dates.length > 0 ? bfLog[dates[dates.length-1]].val : null;
    document.getElementById('headerBF').innerText = latestBF ? latestBF + '%' : '--%';
    document.getElementById('statBF').innerText = latestBF ? latestBF + '%' : '--%';

    // Weight delta
    const bwLog = JSON.parse(localStorage.getItem('bw_log')) || {};
    const bwDates = Object.keys(bwLog).sort();
    if(bwDates.length >= 2) {
        const latest = bwLog[bwDates[bwDates.length-1]];
        const prev = bwLog[bwDates[bwDates.length-2]];
        const delta = (latest - prev).toFixed(1);
        const el = document.getElementById('statWtDelta');
        el.innerText = (delta > 0 ? '▲ ' : '▼ ') + Math.abs(delta) + 'kg';
        el.className = 'stat-delta ' + (delta > 0 ? 'delta-up' : 'delta-down');
    }

    // Streak — animated
    const streak = calcStreak();
    if (streak > 0) {
        document.getElementById('headerStreak').style.display = 'flex';
        const prevStreak = parseInt(document.getElementById('streakNum').innerText) || 0;
        animateCounter(document.getElementById('streakNum'), prevStreak, streak, 600);
    } else {
        document.getElementById('headerStreak').style.display = 'none';
    }
}
function renderWeekSummary() {
    const att = JSON.parse(localStorage.getItem('gatt')) || {};
    const row = document.getElementById('weekDaysRow');
    const days = ['Su','Mo','Tu','We','Th','Fr','Sa'];
    const today = new Date(); today.setHours(0,0,0,0);
    // Get this week's Monday
    const dow = today.getDay();
    const mondayOffset = dow === 0 ? -6 : 1 - dow;
    let html = '';
    let gymDays = 0;
    for(let i=0; i<7; i++) {
        const d = new Date(today);
        d.setDate(today.getDate() + mondayOffset + i);
        d.setHours(0,0,0,0);
        const ds = d.toISOString().split('T')[0];
        const isFuture = d > today;
        const isDone = att[ds];
        let cls = 'week-day-dot';
        if(isDone) { cls += ' done'; gymDays++; }
        else if(!isFuture) cls += ' missed';
        html += `<div class="${cls}" title="${days[(i+1)%7]}"></div>`;
    }
    row.innerHTML = html;
    document.getElementById('weekVolume').innerText = gymDays + '/6 days';
}

// ============================================================
// MOOD
// ============================================================
function renderMood() {
    const m = localStorage.getItem('mood_' + todayStr());
    if(m) {
        document.querySelectorAll('.mood-btn').forEach(b => b.classList.remove('selected'));
        const el = document.getElementById('mood-' + m);
        if(el) el.classList.add('selected');
        document.getElementById('moodSaved').innerText = m;
    }
}
function saveMood(m) {
    localStorage.setItem('mood_' + todayStr(), m);
    renderMood();
    showToast('Mood logged', 'info', m);
}

// ============================================================
// WATER
// ============================================================
function renderWater() {
    const WATER_TARGET = getWaterTarget();
    const count = parseInt(localStorage.getItem('water_' + todayStr())) || 0;
    const track = document.getElementById('waterTrack');
    let html = '';
    for(let i=0; i<WATER_TARGET; i++) {
        html += `<div class="water-glass ${i < count ? 'filled' : ''}" onclick="toggleWater(${i})">💧</div>`;
    }
    track.innerHTML = html;
    document.getElementById('waterLabel').innerText = count + ' / ' + WATER_TARGET + ' glasses';
}
function toggleWater(i) {
    const WATER_TARGET = getWaterTarget();
    let count = parseInt(localStorage.getItem('water_' + todayStr())) || 0;
    count = (i < count) ? i : i + 1;
    localStorage.setItem('water_' + todayStr(), count);
    renderWater();
    if(count === WATER_TARGET) showToast('Hydration goal hit! 💧', 'success', '🎯');
}

// ============================================================
// CALENDAR
// ============================================================
    if(dayNum===0) return showToast('Sunday is rest day', 'info', '🧘');
    exManagerDay = dayNum;
    editingExIdx = null;
    document.getElementById('exManagerModal').style.display = 'flex';
    renderExManagerList();
    resetExMgrForm();
}
function renderExManagerList() {
    const wkts = getWorkouts();
    const dayData = wkts[exManagerDay]||{n:'',ex:[]};
    const dayNames = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
    document.getElementById('exManagerTitle').innerText = dayData.n||dayNames[exManagerDay];
    let html = '';
    dayData.ex.forEach((ex,idx) => {
        html += `<div class="ex-manager-item">
            <div>
                <div style="font-weight:700;font-size:14px;">${ex.n}</div>
                <div style="font-size:11px;color:var(--text-sub);margin-top:2px;">${ex.t||'—'} ${ex.l?'· <a href="'+ex.l+'" target="_blank" style="color:var(--accent);">Form ↗</a>':''}</div>
            </div>
            <div style="display:flex;gap:6px;flex-shrink:0;">
                <button class="btn-sm" onclick="editExercise(${idx})">✎</button>
                <button onclick="removeExercise(${idx})" style="background:none;border:none;color:var(--fail);font-size:18px;font-weight:700;cursor:pointer;padding:0 4px;">✕</button>
            </div>
        </div>`;
    });
    document.getElementById('exManagerList').innerHTML = html||`<div style="color:var(--text-sub);font-size:13px;padding:10px 0;">No exercises. Add below.</div>`;
}
function editExercise(idx) {
    const wkts = getWorkouts();
    const ex = wkts[exManagerDay].ex[idx];
    editingExIdx = idx;
    document.getElementById('exMgrName').value = ex.n;
    document.getElementById('exMgrTarget').value = ex.t||'';
    document.getElementById('exMgrLink').value = ex.l||'';
    document.getElementById('exMgrFormLabel').innerText = 'Edit Exercise';
    document.getElementById('exMgrSaveBtn').innerText = 'SAVE CHANGES';
    document.getElementById('exMgrCancelBtn').style.display = 'block';
    document.getElementById('exMgrName').focus();
}
function resetExMgrForm() {
    editingExIdx = null;
    ['exMgrName','exMgrTarget','exMgrLink'].forEach(id => document.getElementById(id).value = '');
    document.getElementById('exMgrFormLabel').innerText = 'Add Exercise';
    document.getElementById('exMgrSaveBtn').innerText = 'ADD EXERCISE';
    document.getElementById('exMgrCancelBtn').style.display = 'none';
}
function cancelExerciseEdit() { resetExMgrForm(); }
function saveExerciseFromManager() {
    const n = document.getElementById('exMgrName').value.trim();
    const t = document.getElementById('exMgrTarget').value.trim();
    const l = document.getElementById('exMgrLink').value.trim();
    if(!n) return showToast('Enter an exercise name', 'info', '⚠️');
    const wkts = getWorkouts();
    if(!wkts[exManagerDay]) wkts[exManagerDay] = {n:'',ex:[]};
    if(editingExIdx !== null) {
        Object.assign(wkts[exManagerDay].ex[editingExIdx], {n,t,l});
        showToast('Exercise updated', 'success', '✅');
    } else {
        wkts[exManagerDay].ex.push({id:'ex'+Date.now(),n,t,l});
        showToast(`${n} added`, 'success', '✅');
    }
    saveWorkouts(wkts);
    renderExManagerList();
    resetExMgrForm();
    renderWorkout(exManagerDay);
}
function removeExercise(idx) {
    const wkts = getWorkouts();
    const name = wkts[exManagerDay].ex[idx].n;
    wkts[exManagerDay].ex.splice(idx,1);
    saveWorkouts(wkts);
    renderExManagerList();
    renderWorkout(exManagerDay);
    showToast(`${name} removed`, 'info', '🗑');
}

// ============================================================
// UTILS
// ============================================================
// ============================================================
// GOAL EDITOR
// ============================================================
function openGoalEditor(mode) {
    goalEditorMode = mode;
    document.getElementById('goalModal').style.display = 'flex';
    if(mode === 'protein') {
        document.getElementById('goalModalTitle').innerText = 'Protein Goal';
        document.getElementById('goalModalBody').innerHTML = `
            <span class="label-sm">Daily Protein Target (g)</span>
            <input type="number" id="goalInput" value="${getGoal()}" placeholder="110" min="50" max="400">
            <div style="font-size:12px; color:var(--text-sub); margin-top:-4px;">A common target is 0.8–1g per lb of bodyweight.</div>`;
    } else {
        document.getElementById('goalModalTitle').innerText = 'Water Goal';
        document.getElementById('goalModalBody').innerHTML = `
            <span class="label-sm">Daily Water Target (glasses)</span>
            <input type="number" id="goalInput" value="${getWaterTarget()}" placeholder="8" min="1" max="20">
            <div style="font-size:12px; color:var(--text-sub); margin-top:-4px;">Recommended: 8 glasses (~2 litres) per day.</div>`;
    }
    setTimeout(() => document.getElementById('goalInput').focus(), 300);
}
function saveGoal() {
    const val = parseInt(document.getElementById('goalInput').value);
    if(!val || val < 1) return showToast('Enter a valid number', 'info', '⚠️');
    if(goalEditorMode === 'protein') {
        localStorage.setItem('proteinGoal', val);
        showToast(`Protein goal set to ${val}g`, 'success', '🥩');
    } else {
        localStorage.setItem('waterTarget', val);
        showToast(`Water goal set to ${val} glasses`, 'success', '💧');
    }
    closeModal('goalModal');
    renderDiet();
    renderWater();
    updateDietBar();
}

function exportData() {
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob([JSON.stringify(localStorage)],{type:"application/json"}));
    a.download = "gympro_backup_"+todayStr()+".json";
    a.click();
    showToast('Backup downloaded', 'success', '💾');
}
function importData(e) {
    const r = new FileReader();
    r.onload = (ev) => {
        const d = JSON.parse(ev.target.result);
        Object.keys(d).forEach(k => localStorage.setItem(k,d[k]));
        location.reload();
    };
    r.readAsText(e.target.files[0]);
}
// ============================================================
// ROUTINE MANAGER
// ============================================================
function updateRoutinePill() {
    const r = getActiveRoutine();
    if (r) document.getElementById('routinePillName').innerText = r.name;
}

function openRoutineManager() {
    document.getElementById('routineModal').style.display = 'flex';
    renderRoutineList();
}

function renderRoutineList() {
    const routines = getRoutines();
    const activeId = getActiveRoutineId();
    let html = '';
    routines.forEach(r => {
        const dayCount = Object.values(r.days||{}).filter(d => d.ex && d.ex.length > 0).length;
        const isActive = r.id === activeId;
        html += `<div class="routine-item ${isActive ? 'active-routine' : ''}" onclick="switchRoutine('${r.id}')">
            <div>
                <div class="routine-name">${r.name} ${isActive ? '<span style="color:var(--accent);font-size:9px;margin-left:6px;">● ACTIVE</span>' : ''}</div>
                <div class="routine-meta">${dayCount} training day${dayCount !== 1 ? 's' : ''}</div>
            </div>
            <div style="display:flex;gap:6px;align-items:center;">
                <button class="btn-sm" onclick="event.stopPropagation();renameRoutine('${r.id}')">✎</button>
                ${routines.length > 1 ? `<button onclick="event.stopPropagation();deleteRoutine('${r.id}')" style="background:none;border:none;color:var(--fail);font-size:18px;font-weight:700;cursor:pointer;padding:0 4px;">✕</button>` : ''}
            </div>
        </div>`;
    });
    document.getElementById('routineList').innerHTML = html || '<div style="color:var(--text-sub);font-size:13px;padding:10px 0;">No routines yet.</div>';
}

function switchRoutine(id) {
    setActiveRoutineId(id);
    updateRoutinePill();
    renderRoutineList();
    renderWorkout(currentWorkoutDay);
    showToast('Routine switched', 'success', '📋');
}

function createRoutine() {
    const name = document.getElementById('newRoutineName').value.trim();
    if (!name) return showToast('Enter a routine name', 'info', '⚠️');
    const routines = getRoutines();
    const id = 'r' + Date.now();
    // Empty days template
    const emptyDays = {0:{n:"REST",ex:[]},1:{n:"DAY 1",ex:[]},2:{n:"DAY 2",ex:[]},3:{n:"DAY 3",ex:[]},4:{n:"DAY 4",ex:[]},5:{n:"DAY 5",ex:[]},6:{n:"DAY 6",ex:[]}};
    routines.push({id, name, days: emptyDays});
    saveRoutines(routines);
    setActiveRoutineId(id);
    document.getElementById('newRoutineName').value = '';
    updateRoutinePill();
    renderRoutineList();
    renderWorkout(1);
    showToast(`"${name}" created & activated`, 'success', '✅');
}

function renameRoutine(id) {
    const routines = getRoutines();
    const r = routines.find(x => x.id === id);
    if (!r) return;
    const newName = prompt('Rename routine:', r.name);
    if (!newName || !newName.trim()) return;
    r.name = newName.trim();
    saveRoutines(routines);
    updateRoutinePill();
    renderRoutineList();
}

function deleteRoutine(id) {
    const routines = getRoutines();
    if (routines.length <= 1) return showToast("Can't delete the only routine", 'info', '⚠️');
    const idx = routines.findIndex(r => r.id === id);
    if (idx === -1) return;
    const name = routines[idx].name;
    routines.splice(idx, 1);
    saveRoutines(routines);
    if (getActiveRoutineId() === id) {
        setActiveRoutineId(routines[0].id);
        updateRoutinePill();
        renderWorkout(currentWorkoutDay);
    }
    renderRoutineList();
    showToast(`"${name}" deleted`, 'info', '🗑');
}

// ============================================================
// JSON IMPORT
// ============================================================
    closeModal('routineModal');
    document.getElementById('jsonImportArea').value = '';
    document.getElementById('importModal').style.display = 'flex';
}

function importWorkoutJSON() {
    const raw = document.getElementById('jsonImportArea').value.trim();
    if (!raw) return showToast('Paste JSON first', 'info', '⚠️');
    let parsed;
    try {
        parsed = JSON.parse(raw);
    } catch(e) {
        return showToast('Invalid JSON — check format', 'info', '❌');
    }

    // Support two formats:
    // Format A (direct days object): {"name":"X","days":{"1":{n,ex},…}}
    // Format B (AI output with days array): {"name":"X","days":[{day,name,exercises:[{name,sets,reps,link}]}]}
    let name = parsed.name || 'Imported Routine';
    let days = {};

    if (parsed.days && !Array.isArray(parsed.days)) {
        // Format A — keys are day numbers (as strings or ints)
        Object.entries(parsed.days).forEach(([k, v]) => {
            days[parseInt(k)] = {
                n: v.n || v.name || 'Day',
                ex: (v.ex || v.exercises || []).map(e => ({
                    id: e.id || 'ex' + Math.random().toString(36).slice(2,7),
                    n: e.n || e.name,
                    t: e.t || e.sets || '',
                    l: e.l || e.link || ''
                }))
            };
        });
    } else if (parsed.days && Array.isArray(parsed.days)) {
        // Format B — AI-generated array format
        parsed.days.forEach(d => {
            const dayNum = d.day !== undefined ? parseInt(d.day) : 0;
            days[dayNum] = {
                n: d.name || d.n || 'Day',
                ex: (d.exercises || d.ex || []).map(e => ({
                    id: 'ex' + Math.random().toString(36).slice(2,7),
                    n: e.name || e.n,
                    t: e.sets ? (e.sets + (e.reps ? '×' + e.reps : '')) : (e.t || ''),
                    l: e.link || e.l || ''
                }))
            };
        });
    } else {
        return showToast('Could not parse routine structure', 'info', '❌');
    }

    // Fill missing days as rest
    [0,1,2,3,4,5,6].forEach(i => { if (!days[i]) days[i] = {n: i===0?'REST':'DAY '+i, ex:[]}; });

    const routines = getRoutines();
    const id = 'r' + Date.now();
    routines.push({id, name, days});
    saveRoutines(routines);
    setActiveRoutineId(id);
    updateRoutinePill();
    renderWorkout(1);
    closeModal('importModal');
    showToast(`"${name}" imported!`, 'success', '📥');
}

// ============================================================
// AI PROMPT GENERATOR
// ============================================================
    closeModal('routineModal');
    document.getElementById('aiRoutineType').value = '';
    document.getElementById('aiPromptOutput').style.display = 'none';
    document.getElementById('aiPromptModal').style.display = 'flex';
}

function generatePromptText() {
    const type = document.getElementById('aiRoutineType').value.trim();
    if (!type) return showToast('Describe your workout first', 'info', '⚠️');

    const prompt = `Generate a ${type} gym workout routine for me.

Return ONLY a valid JSON object — no explanation, no markdown, no extra text. The JSON must follow this exact format:

{
  "name": "Routine Name",
  "days": [
    {
      "day": 1,
      "name": "PUSH",
      "exercises": [
        {
          "name": "Bench Press",
          "sets": "3",
          "reps": "10",
          "link": "https://exrx.net/..."
        }
      ]
    },
    {
      "day": 0,
      "name": "REST",
      "exercises": []
    }
  ]
}

Rules:
- "day" must be 0–6 (0 = Sunday, 1 = Monday, … 6 = Saturday)
- Include all 7 days (0–6). Use "REST" with empty exercises for rest days
- "sets" and "reps" are strings (e.g. "3" and "10")
- "link" should be a real exrx.net or strengthlog.com URL for the exercise, or an empty string ""
- Include 3–6 exercises per training day
- Routine type: ${type}`;

    document.getElementById('aiPromptText').innerText = prompt;
    document.getElementById('aiPromptOutput').style.display = 'block';
}

function copyPromptText() {
    const text = document.getElementById('aiPromptText').innerText;
    navigator.clipboard.writeText(text).then(() => {
        showToast('Prompt copied!', 'success', '📋');
    }).catch(() => {
        // fallback
        const ta = document.createElement('textarea');
        ta.value = text;
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        ta.remove();
        showToast('Prompt copied!', 'success', '📋');
    });
}

// ============================================================
// CARDIO LOG
// ============================================================
function openCardioLog() {
    document.getElementById('cardioModal').style.display = 'flex';
    renderCardioHistory();
}

function adjustCardio(field, delta) {
    const el = document.getElementById('cardio' + field.charAt(0).toUpperCase() + field.slice(1));
    let val = parseFloat(el.value) || 0;
    val = Math.max(0, val + delta);
    el.value = val;
}

function logCardio() {
    const type = document.getElementById('cardioType').value;
    const duration = parseInt(document.getElementById('cardioDuration').value);
    const calories = document.getElementById('cardioCalories').value;
    const distance = document.getElementById('cardioDistance').value;
    const notes = document.getElementById('cardioNotes').value.trim();

    if (!duration || duration < 1) return showToast('Enter duration', 'info', '⚠️');

    const entry = {
        id: 'c' + Date.now(),
        date: todayStr(),
        type, duration,
        calories: calories ? parseInt(calories) : null,
        distance: distance ? parseFloat(distance) : null,
        notes
    };

    const log = JSON.parse(localStorage.getItem('cardioLog')) || [];
    log.push(entry);
    localStorage.setItem('cardioLog', JSON.stringify(log));

    // Clear form
    document.getElementById('cardioCalories').value = '';
    document.getElementById('cardioDistance').value = '';
    document.getElementById('cardioNotes').value = '';

    renderCardioHistory();
    showToast(`${type} · ${duration} min logged 🏃`, 'success', '✅');
}

function renderCardioHistory() {
    const log = JSON.parse(localStorage.getItem('cardioLog')) || [];
    const container = document.getElementById('cardioHistory');
    if (!log.length) {
        container.innerHTML = '<div style="color:var(--text-sub);font-size:13px;">No cardio logged yet.</div>';
        return;
    }
    const recent = [...log].reverse().slice(0, 15);
    let html = '<span class="label-sm">Recent Sessions</span>';
    recent.forEach(e => {
        const extras = [];
        if (e.distance) extras.push(e.distance + 'km');
        if (e.calories) extras.push(e.calories + ' kcal');
        if (e.notes) extras.push(e.notes);
        html += `<div class="cardio-entry">
            <div>
                <div style="font-weight:700;font-size:14px;">${e.type} <span class="cardio-badge">${e.duration} min</span></div>
                <div style="font-size:11px;color:var(--text-sub);margin-top:3px;">${e.date}${extras.length ? ' · ' + extras.join(' · ') : ''}</div>
            </div>
            <button onclick="deleteCardio('${e.id}')" style="background:none;border:none;color:var(--fail);font-size:18px;font-weight:700;cursor:pointer;padding:0 6px;">✕</button>
        </div>`;
    });
    container.innerHTML = html;
}

function deleteCardio(id) {
    let log = JSON.parse(localStorage.getItem('cardioLog')) || [];
    log = log.filter(e => e.id !== id);
    localStorage.setItem('cardioLog', JSON.stringify(log));
    renderCardioHistory();
    showToast('Entry removed', 'info', '🗑');
}

init();