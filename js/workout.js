/* WORKOUT LOGGING */
    currentWorkoutDay = dayNum;
    const nav = document.getElementById('workoutNav');
    const att = JSON.parse(localStorage.getItem('gatt'))||{};
    const wkts = getWorkouts();
    nav.innerHTML = '';
    ['Su','Mo','Tu','We','Th','Fr','Sa'].forEach((d,i) => {
        const isToday = i === new Date().getDay();
        // Check if this day has a log this week
        const thisWeekDate = getThisWeekDate(i);
        const hasLog = att[thisWeekDate];
        nav.innerHTML += `<button class="day-btn ${i===dayNum?'active':''} ${hasLog&&i!==dayNum?'has-log':''}" onclick="renderWorkout(${i})">${d}${isToday?' ●':''}</button>`;
    });

    const dayData = wkts[dayNum] || {n:'',ex:[]};
    const dayNames = ['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'];
    const title = document.getElementById('workoutDayTitle');
    if(dayNum === 0) {
        title.innerHTML = `<span>REST</span> DAY`;
        document.getElementById('workoutList').innerHTML = `
            <div class="card" style="text-align:center;padding:44px 20px;border:1px dashed var(--border2);">
                <div style="font-size:3.5em;margin-bottom:12px;animation:breathe 3s ease-in-out infinite;">🧘</div>
                <div style="font-family:var(--font-display);font-size:1.4em;font-weight:800;letter-spacing:3px;text-transform:uppercase;">Active Recovery</div>
                <div style="color:var(--text-sub);margin-top:10px;font-size:13px;line-height:1.7;">Walk · Stretch · Sleep · Eat</div>
                <div style="margin-top:18px;display:flex;justify-content:center;gap:20px;">
                    <div style="color:var(--text-sub);font-size:11px;font-family:var(--font-display);letter-spacing:1px;">🚶 Walk 20 min</div>
                    <div style="color:var(--text-sub);font-size:11px;font-family:var(--font-display);letter-spacing:1px;">💧 Hydrate well</div>
                    <div style="color:var(--text-sub);font-size:11px;font-family:var(--font-display);letter-spacing:1px;">😴 7-9 hrs</div>
                </div>
            </div>`;
        return;
    }
    title.innerHTML = `<span>${dayData.n}</span> ${dayNames[dayNum].slice(dayData.n.length)}`;

    let html = '';
    dayData.ex.forEach(ex => {
        const hist = JSON.parse(localStorage.getItem('h_'+ex.id))||[];
        const last = hist.length>0 ? hist[hist.length-1] : null;
        const todayData = JSON.parse(localStorage.getItem(`t_${ex.id}_${todayStr()}`))||{w:'',r:''};
        const est1rm = (todayData.w && todayData.r) ? Math.round(todayData.w*(1+todayData.r/30)) : null;
        // Check PR
        const allMax = hist.length>0 ? Math.max(...hist.map(h=>Math.round(h.w*(1+h.r/30)))) : 0;
        const isPR = est1rm && hist.length>0 && est1rm > allMax;
        const hasData = !!(todayData.w && todayData.r);

        html += `<div class="ex-card ${hasData?'has-data':''} ${isPR?'is-pr':''}">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:6px;">
                <div>
                    <span class="ex-name">${ex.n}</span>${isPR?'<span class="ex-pr-badge">🏆 PR</span>':''}
                    <div class="ex-meta">
                        ${ex.t?`<span class="ex-tag target">🎯 ${ex.t}</span>`:''}
                        ${last?`<span class="ex-tag">Last: ${last.w}kg×${last.r}</span>`:'<span class="ex-tag">New</span>'}
                    </div>
                </div>
                <div style="display:flex;flex-direction:column;align-items:flex-end;gap:4px;">
                    <button class="btn-sm" onclick="copyLast('${ex.id}')">📋 Copy</button>
                    ${ex.l?`<a href="${ex.l}" target="_blank" style="font-family:var(--font-display);font-size:9px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:var(--accent);text-decoration:none;">FORM ↗</a>`:''}
                </div>
            </div>
            <div class="input-row">
                <div>
                    <span class="label-sm">Weight (kg)</span>
                    <div class="stepper">
                        <button onclick="adjustVal('${ex.id}','w',-2.5)">−</button>
                        <input type="number" id="w_${ex.id}" value="${todayData.w}" oninput="saveW('${ex.id}')">
                        <button onclick="adjustVal('${ex.id}','w',2.5)">+</button>
                    </div>
                </div>
                <div>
                    <span class="label-sm">Reps</span>
                    <div class="stepper">
                        <button onclick="adjustVal('${ex.id}','r',-1)">−</button>
                        <input type="number" id="r_${ex.id}" value="${todayData.r}" oninput="saveW('${ex.id}')">
                        <button onclick="adjustVal('${ex.id}','r',1)">+</button>
                    </div>
                </div>
                <div style="display:flex;flex-direction:column;gap:4px;">
                    <button class="btn-icon" onclick="startTimer()" title="Rest Timer">⏱</button>
                    <button class="btn-icon" onclick="openGraph('${ex.id}','${ex.n.replace(/'/g,'')}')" title="Progress">📊</button>
                </div>
            </div>
            <div class="est-1rm">${est1rm ? `Est 1RM: <span>${est1rm}kg</span>` : 'Enter weight + reps'}</div>
            ${hasData ? `<div class="vol-bar-wrap"><div class="vol-bar-fill" style="width:${Math.min((est1rm||0)/allMax*100||50,100)}%"></div></div>` : ''}
        </div>`;
    });

    if(dayData.ex.length === 0) {
        html = `<div class="card" style="text-align:center;padding:36px 20px;color:var(--text-sub);border:1px dashed var(--border2);">
            <div style="font-size:2.5em;margin-bottom:10px;opacity:0.6;">➕</div>
            <div style="font-family:var(--font-display);font-size:1.1em;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;">No exercises yet</div>
            <div style="font-size:12px;margin-top:8px;line-height:1.6;color:var(--text-sub);">Tap <b style="color:var(--text-mid);">Edit</b> to build your workout for this day</div>
        </div>`;
    } else {
        html += `<button class="btn-main" style="margin-top:4px;" onclick="finishWorkout(${dayNum})">✓ LOG COMPLETE WORKOUT</button>`;
    }
    document.getElementById('workoutList').innerHTML = html;
}

function getThisWeekDate(dayOfWeek) {
    const today = new Date(); today.setHours(0,0,0,0);
    const diff = dayOfWeek - today.getDay();
    const d = new Date(today); d.setDate(today.getDate() + diff);
    return d.toISOString().split('T')[0];
}

function adjustVal(id, field, delta) {
    const el = document.getElementById(field+'_'+id);
    let val = parseFloat(el.value)||0;
    val = Math.max(0, parseFloat((val+delta).toFixed(1)));
    el.value = val;
    saveW(id);
}

function saveW(id) {
    const w = document.getElementById('w_'+id)?.value;
    const r = document.getElementById('r_'+id)?.value;
    localStorage.setItem(`t_${id}_${todayStr()}`, JSON.stringify({w,r}));
    // Check for PR before re-render
    const est = (w && r) ? Math.round(parseFloat(w)*(1+parseInt(r)/30)) : null;
    const hist = JSON.parse(localStorage.getItem('h_'+id))||[];
    const allMax = hist.length>0 ? Math.max(...hist.map(h=>Math.round(h.w*(1+h.r/30)))) : 0;
    if (est && hist.length > 0 && est > allMax) {
        const exCard = document.querySelector(`#w_${CSS.escape(id)}`)?.closest('.ex-card');
        if (exCard) { exCard.classList.add('is-pr'); }
    }
    renderWorkout(currentWorkoutDay);
}

function copyLast(id) {
    const hist = JSON.parse(localStorage.getItem('h_'+id))||[];
    if(!hist.length) return showToast('No history found', 'info', '📋');
    const last = hist[hist.length-1];
    localStorage.setItem(`t_${id}_${todayStr()}`, JSON.stringify({w:last.w, r:last.r}));
    renderWorkout(currentWorkoutDay);
    showToast('Last session copied', 'info', '📋');
}

function finishWorkout(dayNum) {
    const wkts = getWorkouts();
    const exList = wkts[dayNum]?.ex || [];
    let logged = 0;
    let prCount = 0;
    exList.forEach(ex => {
        const t = JSON.parse(localStorage.getItem(`t_${ex.id}_${todayStr()}`));
        if(t && t.w>0 && t.r>0) {
            const hist = JSON.parse(localStorage.getItem('h_'+ex.id))||[];
            const est = Math.round(parseFloat(t.w)*(1+parseInt(t.r)/30));
            const allMax = hist.length>0 ? Math.max(...hist.map(h=>Math.round(h.w*(1+h.r/30)))) : -1;
            if (est > allMax && hist.length > 0) prCount++;
            hist.push({d:todayStr(), w:parseFloat(t.w), r:parseInt(t.r)});
            localStorage.setItem('h_'+ex.id, JSON.stringify(hist));
            logged++;
        }
    });
    const att = JSON.parse(localStorage.getItem('gatt'))||{};
    att[todayStr()] = true;
    localStorage.setItem('gatt', JSON.stringify(att));
    if (prCount > 0) {
        showToast(`${prCount} new PR${prCount>1?'s':''}! 🏆`, 'pr', '🏆');
    }
    showToast(`Workout saved! ${logged} exercises logged 💪`, 'success', '✅');
    updateHeaderStats();
    renderWeekSummary();
    renderCalendar();
}

// ============================================================
// TIMER
// ============================================================