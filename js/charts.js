/* CHARTS, GRAPHS & BODY STATS */
function renderDietTrend() {
    const GOAL = getGoal();
    const ctx = document.getElementById('dietChart').getContext('2d');
    const log = JSON.parse(localStorage.getItem('dlog'))||{};
    let labels = [], data = [];
    for(let i=6;i>=0;i--) {
        const d = new Date(); d.setDate(d.getDate()-i);
        const ds = d.toISOString().split('T')[0];
        labels.push(d.toLocaleDateString('en',{weekday:'short'}));
        data.push(Math.round(log[ds]||0));
    }
    if(dietChartInstance) dietChartInstance.destroy();
    const accent = getComputedStyle(document.body).getPropertyValue('--accent').trim();
    const text = getComputedStyle(document.body).getPropertyValue('--text').trim();
    const border = getComputedStyle(document.body).getPropertyValue('--border').trim();
    const bg2 = getComputedStyle(document.body).getPropertyValue('--bg2').trim();
    const yMax = Math.max(GOAL * 1.3, Math.max(...data) * 1.1, 50);

    // Build gradient bars
    const bgColors = data.map((v,i) => {
        const ratio = GOAL > 0 ? v / GOAL : 0;
        if (ratio >= 0.95) {
            const g = ctx.createLinearGradient(0,0,0,200);
            g.addColorStop(0,'#22c55e'); g.addColorStop(1,'#16a34a'); return g;
        } else if (ratio >= 0.77) {
            const g = ctx.createLinearGradient(0,0,0,200);
            g.addColorStop(0,'#f59e0b'); g.addColorStop(1,'#d97706'); return g;
        } else {
            const g = ctx.createLinearGradient(0,0,0,200);
            g.addColorStop(0,'#ef4444'); g.addColorStop(1,'#dc2626'); return g;
        }
    });

    dietChartInstance = new Chart(ctx, {
        type:'bar',
        data:{labels, datasets:[{
            label:'Protein (g)', data,
            backgroundColor: bgColors,
            borderRadius: 6,
            borderSkipped: false,
            hoverBackgroundColor: bgColors.map(() => accent + 'cc')
        }]},
        options:{
            responsive:true,
            maintainAspectRatio:false,
            animation: { duration: 800, easing: 'easeOutQuart' },
            scales:{
                x:{ticks:{color:text, font:{size:11,weight:'700',family:'JetBrains Mono'}}, grid:{color:border,drawBorder:false}},
                y:{beginAtZero:true, max:Math.ceil(yMax),
                   ticks:{color:text, font:{size:11,family:'JetBrains Mono'}, callback:v=>v+'g'},
                   grid:{color:border,drawBorder:false},
                   afterBuildTicks: axis => { axis.ticks = axis.ticks.filter(t => t.value <= Math.ceil(yMax)); }}
            },
            plugins:{
                legend:{display:false},
                tooltip:{
                    backgroundColor: bg2 + 'f2',
                    titleColor: text,
                    bodyColor: accent,
                    borderColor: border,
                    borderWidth: 1,
                    cornerRadius: 8,
                    padding: 12,
                    callbacks:{label: ctx => ctx.parsed.y + 'g protein'}
                }
            }
        }
    });
}

// ============================================================
// BODY STATS
// ============================================================
function saveWeight() {
    const w = document.getElementById('wtInput').value;
    if(w) {
        localStorage.setItem('uwt', w);
        const history = JSON.parse(localStorage.getItem('bw_log'))||{};
        history[todayStr()] = parseFloat(w);
        localStorage.setItem('bw_log', JSON.stringify(history));
        document.getElementById('wtInput').value = '';
        updateHeaderStats();
        showToast(`${w}kg logged`, 'success', '⚖️');
    }
}

function openBFModal() { document.getElementById('bfModal').style.display = 'flex'; }

function calcAndSaveBF() {
    const h = parseFloat(document.getElementById('bfHeight').value);
    const n = parseFloat(document.getElementById('bfNeck').value);
    const w = parseFloat(document.getElementById('bfWaist').value);
    if(h && n && w) {
        localStorage.setItem('bfHeight', h);
        const bf = 495/(1.0324-0.19077*Math.log10(w-n)+0.15456*Math.log10(h))-450;
        const res = Math.round(bf*10)/10;
        document.getElementById('bfResult').innerText = res + '% Body Fat';
        const log = JSON.parse(localStorage.getItem('bfLog'))||{};
        log[todayStr()] = {val:res, w, n};
        localStorage.setItem('bfLog', JSON.stringify(log));
        updateHeaderStats();
        showToast(`BF: ${res}% logged`, 'success', '📏');
    }
}

// ============================================================
// GRAPHS
// ============================================================
function openGraph(type, title) {
    document.getElementById('graphModal').style.display = 'flex';
    document.getElementById('graphTitle').innerText = title.toUpperCase();
    let labels=[], data=[], yLabel='';
    if(type==='bf') {
        const log = JSON.parse(localStorage.getItem('bfLog'))||{};
        const dates = Object.keys(log).sort();
        labels = dates.map(d=>d.slice(5)); data = dates.map(d=>log[d].val); yLabel='Body Fat %';
    } else if(type==='wt') {
        const log = JSON.parse(localStorage.getItem('bw_log'))||{};
        const dates = Object.keys(log).sort();
        labels = dates.map(d=>d.slice(5)); data = dates.map(d=>log[d]); yLabel='Weight (kg)';
    } else {
        const hist = JSON.parse(localStorage.getItem('h_'+type))||[];
        labels = hist.map(h=>h.d.slice(5));
        data = hist.map(h=>Math.round(h.w*(1+h.r/30)));
        yLabel='Est 1RM (kg)';
    }

    // Graph stats
    const statsEl = document.getElementById('graphStats');
    if(data.length > 0) {
        const max = Math.max(...data), min = Math.min(...data);
        const latest = data[data.length-1];
        const delta = data.length>1 ? (latest - data[data.length-2]).toFixed(1) : '--';
        statsEl.innerHTML = `
            <div class="graph-stat"><div class="gs-val">${latest}</div><div class="gs-label">Latest</div></div>
            <div class="graph-stat"><div class="gs-val">${max}</div><div class="gs-label">Peak</div></div>
            <div class="graph-stat"><div class="gs-val" style="color:${parseFloat(delta)>=0?'var(--fail)':'var(--success)'}">${delta>0?'+':''}${delta}</div><div class="gs-label">Change</div></div>`;
    } else {
        statsEl.innerHTML = '';
    }

    const canvas = document.getElementById('exChart');
    const ctx = canvas.getContext('2d');
    if(chartInstance) chartInstance.destroy();
    const accent = getComputedStyle(document.body).getPropertyValue('--accent').trim();
    const accentDim = getComputedStyle(document.body).getPropertyValue('--accent-dim').trim();
    const textColor = getComputedStyle(document.body).getPropertyValue('--text').trim();
    const borderColor = getComputedStyle(document.body).getPropertyValue('--border').trim();
    const bg2 = getComputedStyle(document.body).getPropertyValue('--bg2').trim();

    // Build gradient fill
    const gradient = ctx.createLinearGradient(0, 0, 0, 280);
    gradient.addColorStop(0, accent + '44');
    gradient.addColorStop(0.5, accent + '15');
    gradient.addColorStop(1, accent + '02');

    chartInstance = new Chart(ctx, {
        type:'line',
        data:{labels, datasets:[{
            label:yLabel, data,
            borderColor: accent,
            backgroundColor: gradient,
            fill: true,
            tension: 0.45,
            pointBackgroundColor: data.map((_,i) => i === data.length-1 ? accent : accentDim),
            pointBorderColor: data.map((_,i) => i === data.length-1 ? accent : 'transparent'),
            pointBorderWidth: 2,
            pointRadius: data.map((_,i) => i === data.length-1 ? 6 : 3),
            pointHoverRadius: 8,
            borderWidth: 2.5,
            hoverBorderWidth: 3
        }]},
        options:{
            responsive:true,
            maintainAspectRatio:false,
            animation: { duration: 1000, easing: 'easeOutQuart' },
            interaction: { intersect: false, mode: 'index' },
            scales:{
                x:{
                    ticks:{color:textColor, font:{size:10,family:'JetBrains Mono'}, maxTicksLimit:8},
                    grid:{color:borderColor, drawBorder:false}
                },
                y:{
                    ticks:{color:textColor, font:{size:10,family:'JetBrains Mono'}, callback:v => v + (yLabel.includes('%') ? '%' : '')},
                    grid:{color:borderColor, drawBorder:false}
                }
            },
            plugins:{
                legend:{display:false},
                tooltip:{
                    backgroundColor: bg2 + 'f2',
                    titleColor: textColor,
                    bodyColor: accent,
                    borderColor: borderColor,
                    borderWidth: 1,
                    cornerRadius: 8,
                    padding: 12,
                    displayColors: false,
                    callbacks:{label: ctx => ctx.dataset.label + ': ' + ctx.parsed.y}
                }
            }
        }
    });
}

// ============================================================
// DIET & SUPP MANAGER
// ============================================================