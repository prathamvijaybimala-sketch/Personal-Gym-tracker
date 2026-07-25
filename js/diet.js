/* DIET, WATER & SUPPLEMENTS */
    const container = document.getElementById('dietList');
    const sContainer = document.getElementById('suppList');
    const config = JSON.parse(localStorage.getItem('dietConfig'));
    const supps = JSON.parse(localStorage.getItem('suppConfig'));
    const checks = JSON.parse(localStorage.getItem('dc_'+todayStr()))||[];
    const todayDay = new Date().getDay();
    const date = new Date().getDate();

    let html = '';
    config.forEach(i => {
        const checked = checks.includes(i.id);
        html += `<div class="diet-item">
            <div style="display:flex;align-items:center;">
                <div class="diet-check ${checked?'checked':''}" onclick="toggleDiet('${i.id}')"></div>
                <span style="font-size:14px;">${i.n}</span>
            </div>
            <span style="font-family:var(--font-mono);font-size:12px;font-weight:700;color:var(--accent);">${i.p}g</span>
        </div>`;
    });
    container.innerHTML = html;

    let sHtml = '';
    supps.forEach(i => {
        let show = i.s==='daily' || (i.s==='alt' && date%2!==0) || i.s==todayDay;
        if(show) {
            const checked = checks.includes(i.id);
            sHtml += `<div class="supp-item">
                <div>
                    <div class="supp-name">${i.n}</div>
                    <div class="supp-sched">${i.s==='daily'?'Daily':i.s==='alt'?'Alt Day':'Weekly'}</div>
                </div>
                <div class="diet-check ${checked?'checked':''}" onclick="toggleDiet('${i.id}')"></div>
            </div>`;
        }
    });
    sContainer.innerHTML = sHtml || `<div style="font-size:12px;color:var(--text-sub);padding:10px 0;">None scheduled today.</div>`;
    updateDietBar();
}

function toggleDiet(id) {
    let checks = JSON.parse(localStorage.getItem('dc_'+todayStr()))||[];
    if(checks.includes(id)) checks = checks.filter(x=>x!==id); else checks.push(id);
    localStorage.setItem('dc_'+todayStr(), JSON.stringify(checks));
    renderDiet();
}

function updateDietBar() {
    const GOAL = getGoal();
    const checks = JSON.parse(localStorage.getItem('dc_'+todayStr()))||[];
    const config = JSON.parse(localStorage.getItem('dietConfig'));
    const extra = parseFloat(localStorage.getItem('de_'+todayStr()))||0;
    let total = extra;
    config.forEach(i => { if(checks.includes(i.id)) total += i.p; });
    const pct = Math.min((total/GOAL)*100, 100);
    const bar = document.getElementById('proteinBar');
    bar.style.width = pct + '%';
    bar.style.background = total>=(GOAL*0.95) ? 'var(--success)' : (total>=(GOAL*0.77)?'var(--partial)':'var(--fail)');
    document.getElementById('proteinLabel').innerText = Math.round(total) + ' / ' + GOAL + 'g';
    const log = JSON.parse(localStorage.getItem('dlog'))||{};
    const prev = log[todayStr()]||0;
    log[todayStr()] = total;
    localStorage.setItem('dlog', JSON.stringify(log));
    if(calendarMode==='diet') renderCalendar();
    // Only toast when crossing the goal threshold (not on every render)
    if(total >= GOAL && prev < GOAL) showToast('Protein goal reached! 🎯', 'success', '✅');
}

function addQuickProtein() {
    const val = parseFloat(document.getElementById('quickProt').value);
    if(val) {
        const cur = parseFloat(localStorage.getItem('de_'+todayStr()))||0;
        localStorage.setItem('de_'+todayStr(), cur+val);
        document.getElementById('quickProt').value = '';
        renderDiet();
        showToast(`+${val}g protein added`, 'info', '🥩');
    }
}

// ============================================================
// DIET TREND CHART
// ============================================================
    managerMode = mode;
    document.getElementById('managerModal').style.display = 'flex';
    document.getElementById('managerTitle').innerText = mode==='menu' ? 'Food Menu' : 'Supplements';
    const data = mode==='menu' ? JSON.parse(localStorage.getItem('dietConfig')) : JSON.parse(localStorage.getItem('suppConfig'));
    let html = '';
    data.forEach((item,idx) => {
        const detail = mode==='menu' ? item.p+'g protein' : (item.s==='daily'?'Daily':item.s==='alt'?'Alt Day':'Day '+item.s);
        html += `<div style="display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-bottom:1px solid var(--border);">
            <div><div style="font-weight:600;font-size:14px;">${item.n}</div>
            <div style="font-family:var(--font-body);font-size:10px;letter-spacing:1px;text-transform:uppercase;color:var(--text-sub);margin-top:2px;">${detail}</div></div>
            <button onclick="removeItem(${idx})" style="background:none;border:none;color:var(--fail);font-size:18px;font-weight:700;cursor:pointer;padding:0 6px;">✕</button>
        </div>`;
    });
    document.getElementById('managerList').innerHTML = html || `<div style="color:var(--text-sub);font-size:13px;padding:10px 0;">Empty. Add some below.</div>`;
    if(mode==='menu') {
        document.getElementById('managerInputs').innerHTML = `
            <input id="mName" placeholder="Item name">
            <input id="mVal" type="number" placeholder="Protein (g)">
            <button class="btn-main" onclick="addItem()">ADD FOOD</button>`;
    } else {
        document.getElementById('managerInputs').innerHTML = `
            <input id="mName" placeholder="Supplement name">
            <select id="mVal">
                <option value="daily">Every Day</option>
                <option value="alt">Alternate Days</option>
                <option value="1">Monday</option><option value="2">Tuesday</option>
                <option value="3">Wednesday</option><option value="4">Thursday</option>
                <option value="5">Friday</option><option value="6">Saturday</option>
                <option value="0">Sunday</option>
            </select>
            <button class="btn-main" onclick="addItem()">ADD SUPP</button>`;
    }
}
function addItem() {
    const n = document.getElementById('mName').value.trim();
    const v = document.getElementById('mVal').value;
    if(!n||!v) return;
    const key = managerMode==='menu' ? 'dietConfig' : 'suppConfig';
    const arr = JSON.parse(localStorage.getItem(key));
    if(managerMode==='menu') arr.push({id:'c'+Date.now(),n,p:parseFloat(v)});
    else arr.push({id:'s'+Date.now(),n,s:v});
    localStorage.setItem(key, JSON.stringify(arr));
    openDietManager(managerMode);
    renderDiet();
}
function removeItem(idx) {
    const key = managerMode==='menu' ? 'dietConfig' : 'suppConfig';
    const arr = JSON.parse(localStorage.getItem(key));
    arr.splice(idx,1);
    localStorage.setItem(key, JSON.stringify(arr));
    openDietManager(managerMode);
    renderDiet();
}

// ============================================================
// EXERCISE MANAGER
// ============================================================