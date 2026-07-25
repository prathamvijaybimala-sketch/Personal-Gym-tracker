/* CALENDAR & HISTORY */
    calendarMode = mode;
    document.querySelectorAll('.cal-mode-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('btn-' + mode).classList.add('active');
    renderCalendar();
}
function renderCalendar() {
    const year = currentDt.getFullYear(), month = currentDt.getMonth();
    document.getElementById('monthLabel').innerText = currentDt.toLocaleString('default',{month:'long',year:'numeric'});
    const grid = document.getElementById('calendarGrid');
    grid.innerHTML = ['Su','Mo','Tu','We','Th','Fr','Sa'].map(d => `<div class="cal-day-label">${d}</div>`).join('');
    const firstDay = new Date(year,month,1).getDay();
    const daysInMonth = new Date(year,month+1,0).getDate();
    const todayObj = new Date(); todayObj.setHours(0,0,0,0);
    for(let i=0;i<firstDay;i++) grid.innerHTML += `<div></div>`;
    for(let i=1;i<=daysInMonth;i++) {
        const ds = `${year}-${String(month+1).padStart(2,'0')}-${String(i).padStart(2,'0')}`;
        const isPast = new Date(year,month,i) < todayObj;
        let cls = 'cal-day';
        if(ds === todayStr()) cls += ' today';
        if(localStorage.getItem('note_'+ds)) cls += ' has-note';
        if(calendarMode === 'gym') {
            const done = (JSON.parse(localStorage.getItem('gatt'))||{})[ds];
            if(done) cls += ' green'; else if(isPast) cls += ' red';
        } else {
            const tot = (JSON.parse(localStorage.getItem('dlog'))||{})[ds]||0;
            if(tot>=105) cls += ' green'; else if(tot>=85) cls += ' yellow'; else if(isPast) cls += ' red';
        }
        grid.innerHTML += `<div class="${cls}" onclick="openHistory('${ds}')">${i}</div>`;
    }
}
function changeMonth(d) { currentDt.setMonth(currentDt.getMonth()+d); renderCalendar(); }
function jumpToday() { currentDt = new Date(); renderCalendar(); }

// ============================================================
// HISTORY MODAL
// ============================================================
function openHistory(ds) {
    document.getElementById('histDateTitle').innerText = new Date(ds+'T12:00:00').toLocaleDateString('en',{weekday:'long',month:'long',day:'numeric'});
    document.getElementById('dayNote').value = localStorage.getItem('note_'+ds)||'';
    document.getElementById('historyModal').style.display = 'flex';
    const content = document.getElementById('histContent');
    const wkts = getWorkouts();
    let html = `<div class="section-label">Workout</div>`;
    let found = false;
    Object.values(wkts).forEach(day => {
        day.ex.forEach(ex => {
            const h = JSON.parse(localStorage.getItem('h_'+ex.id))||[];
            const e = h.find(x=>x.d===ds);
            if(e){found=true; html+=`<div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--border);font-size:13px;"><span>${ex.n}</span><b style="font-family:var(--font-mono)">${e.w}kg × ${e.r}</b></div>`;}
        });
    });
    if(!found) html += `<div style="font-size:12px;color:var(--text-sub);padding:8px 0;">Rest Day / No Log</div>`;
    const dTotal = (JSON.parse(localStorage.getItem('dlog'))||{})[ds]||0;
    html += `<div class="section-label" style="margin-top:16px;">Diet (${Math.round(dTotal)}g protein)</div>`;
    const checks = JSON.parse(localStorage.getItem('dc_'+ds))||[];
    const config = JSON.parse(localStorage.getItem('dietConfig'));
    const suppConfig = JSON.parse(localStorage.getItem('suppConfig'));
    let tags = '';
    config.forEach(i=>{if(checks.includes(i.id)) tags+=`<span style="background:var(--bg2);border:1px solid var(--border);padding:3px 8px;border-radius:5px;font-size:11px;margin:2px;display:inline-block;">${i.n}</span>`;});
    suppConfig.forEach(i=>{if(checks.includes(i.id)) tags+=`<span style="background:var(--primary-container);border:1px solid var(--accent-dim);color:var(--accent);padding:3px 8px;border-radius:5px;font-size:11px;margin:2px;display:inline-block;">${i.n}</span>`;});
    html += `<div style="display:flex;flex-wrap:wrap;gap:2px;">${tags||'<span style="font-size:12px;color:var(--text-sub)">Nothing logged</span>'}</div>`;
    const mood = localStorage.getItem('mood_'+ds);
    if(mood) html += `<div style="margin-top:12px;font-size:20px;">${mood}</div>`;
    content.innerHTML = html;
    localStorage.setItem('tempHistDate', ds);
}
function saveNote() {
    const date = localStorage.getItem('tempHistDate');
    localStorage.setItem('note_'+date, document.getElementById('dayNote').value);
    renderCalendar();
}

// ============================================================
// WORKOUT
// ============================================================