/* STATE, DATA MODELS & HELPERS */

// ============================================================
// STATE & CONFIG
// ============================================================
const getGoal = () => parseInt(localStorage.getItem('proteinGoal')) || 110;
const getWaterTarget = () => parseInt(localStorage.getItem('waterTarget')) || 8;
const todayStr = () => new Date().toISOString().split('T')[0];
let currentDt = new Date();
let calendarMode = 'gym';
let timerInterval = null;
let timerTotal = 90;
let timerSelected = 90;
let chartInstance = null;
let dietChartInstance = null;
let currentWorkoutDay = new Date().getDay();
let exManagerDay = 0;
let editingExIdx = null;
let managerMode = '';

// ============================================================
// MULTI-ROUTINE SYSTEM
// ============================================================
function getRoutines() {
    const s = localStorage.getItem('routines');
    if (s) return JSON.parse(s);
    // Migrate legacy single workout
    const legacy = localStorage.getItem('workoutsConfig');
    const days = legacy ? JSON.parse(legacy) : JSON.parse(JSON.stringify(defaultWorkouts));
    const routines = [{id: 'r1', name: 'PPL', days}];
    localStorage.setItem('routines', JSON.stringify(routines));
    return routines;
}
function saveRoutines(r) { localStorage.setItem('routines', JSON.stringify(r)); }
function getActiveRoutineId() { return localStorage.getItem('activeRoutineId') || getRoutines()[0]?.id || 'r1'; }
function setActiveRoutineId(id) { localStorage.setItem('activeRoutineId', id); }
function getActiveRoutine() {
    const routines = getRoutines();
    return routines.find(r => r.id === getActiveRoutineId()) || routines[0];
}
function getWorkouts() {
    return getActiveRoutine()?.days || JSON.parse(JSON.stringify(defaultWorkouts));
}
function saveWorkouts(d) {
    const routines = getRoutines();
    const idx = routines.findIndex(r => r.id === getActiveRoutineId());
    if (idx !== -1) { routines[idx].days = d; saveRoutines(routines); }
}

const defaultDiet = [
    {id:'e1',n:'2 Eggs',p:12},{id:'w1',n:'Whey Scoop',p:24},
    {id:'d1',n:'Dal Bowl',p:7},{id:'c1',n:'Curd Bowl',p:5},
    {id:'rt',n:'3 Roti',p:9},{id:'ch',n:'Chicken Breast',p:25},
    {id:'pn',n:'Paneer 100g',p:18},{id:'sy',n:'Soya 50g',p:26}
];
const defaultSupps = [
    {id:'cr',n:'Creatine',s:'daily'},
    {id:'mv',n:'Multivitamin',s:'alt'},
    {id:'d3',n:'Vit D3',s:'6'}
];
const defaultWorkouts = {
    1:{n:"PUSH",ex:[
        {id:"cp",n:"Chest Press",t:"3×10",l:"https://exrx.net/WeightExercises/PectoralSternal/LVChestPressS"},
        {id:"idp",n:"Incline DB Press",t:"2×8",l:"https://exrx.net/WeightExercises/PectoralClavicular/DBInclineBenchPress"},
        {id:"lr",n:"Lateral Raises",t:"3×12",l:"https://exrx.net/WeightExercises/DeltoidLateral/DBLateralRaise"},
        {id:"tp",n:"Tri Pushdown",t:"2×12",l:"https://exrx.net/WeightExercises/Triceps/CBPushdown"}
    ]},
    2:{n:"PULL",ex:[
        {id:"lp",n:"Lat Pulldown",t:"3×10",l:"https://exrx.net/WeightExercises/LatissimusDorsi/CBFrontPulldown"},
        {id:"sr",n:"Cable Row",t:"3×10",l:"https://exrx.net/WeightExercises/BackGeneral/CBSeatedRow"},
        {id:"sh",n:"DB Shrugs",t:"2×12",l:"https://exrx.net/WeightExercises/TrapeziusUpper/DBShrug"},
        {id:"bc",n:"Bicep Curl",t:"2×12",l:"https://exrx.net/WeightExercises/Biceps/CBCurl"}
    ]},
    3:{n:"LEGS",ex:[
        {id:"lpr",n:"Leg Press",t:"3×12",l:"https://www.strengthlog.com/leg-press/"},
        {id:"lc",n:"Leg Curl",t:"3×10",l:"https://exrx.net/WeightExercises/Hamstrings/LVLyingLegCurl"},
        {id:"cr",n:"Calf Raise",t:"3×15",l:"https://exrx.net/WeightExercises/Gastrocnemius/BWStandingCalfRaise"},
        {id:"bd",n:"Bird Dog",t:"2×10",l:"https://exrx.net/WeightExercises/ErectorSpinae/BWBirdDog"}
    ]},
    4:{n:"PUSH B",ex:[
        {id:"pd",n:"Pec Deck",t:"3×12",l:"https://exrx.net/WeightExercises/PectoralSternal/LVPecDeckFly"},
        {id:"sp",n:"Seated Press",t:"2×8",l:"https://exrx.net/WeightExercises/DeltoidAnterior/DBShoulderPress"},
        {id:"te",n:"Tri Extension",t:"2×10",l:"https://exrx.net/WeightExercises/Triceps/DBTriExt"}
    ]},
    5:{n:"PULL B",ex:[
        {id:"ulp",n:"Underhand Pull",t:"3×10",l:"https://exrx.net/WeightExercises/LatissimusDorsi/CBUnderhandPulldown"},
        {id:"dr",n:"DB Row",t:"2×10",l:"https://exrx.net/WeightExercises/BackGeneral/DBBentOverRow"},
        {id:"hc",n:"Hammer Curl",t:"2×10",l:"https://exrx.net/WeightExercises/Brachioradialis/DBHammerCurl"}
    ]},
    6:{n:"LEGS B",ex:[
        {id:"gs",n:"Goblet Squat",t:"3×10",l:"https://www.strengthlog.com/goblet-squat/"},
        {id:"rdl",n:"DB RDL",t:"2×10",l:"https://www.strengthlog.com/dumbbell-romanian-deadlift/"},
        {id:"pk",n:"Plank",t:"2×45s",l:"https://exrx.net/WeightExercises/RectusAbdominis/BWFrontPlank"}
    ]},
    0:{n:"REST",ex:[]}
};

// ============================================================
// HELPERS
// ============================================================
function showToast(msg, type='info', icon='') {
    const c = document.getElementById('toast-container');
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.innerHTML = `${icon ? '<span>'+icon+'</span>' : ''}<span>${msg}</span>`;
    c.appendChild(t);
    setTimeout(() => { t.style.animation = 'toastOut 0.3s ease forwards'; setTimeout(() => t.remove(), 300); }, 2200);
    if (type === 'pr') celebratePR();
}

// ── Confetti cannon for PR celebrations ──
function celebratePR() {
    const colors = ['#9575cd','#459b88','#43a371','#c88a20','#7c4dff','#e4e7e8','#5b9bd5'];
    for (let i = 0; i < 60; i++) {
        const particle = document.createElement('div');
        const size = Math.random() * 8 + 4;
        particle.style.cssText = `
            position:fixed; z-index:10000; pointer-events:none;
            width:${size}px; height:${size * (Math.random() * 0.6 + 0.4)}px;
            background:${colors[Math.floor(Math.random() * colors.length)]};
            border-radius:${Math.random() > 0.5 ? '50%' : '2px'};
            left:${Math.random() * 100}vw; top:-20px;
            opacity:0.95;
            transition: all ${0.8 + Math.random() * 1.2}s cubic-bezier(0.25, 0.46, 0.45, 0.94);
        `;
        document.body.appendChild(particle);
        requestAnimationFrame(() => {
            particle.style.transform = `translateY(${60 + Math.random() * 60}vh) translateX(${(Math.random()-0.5)*200}px) rotate(${Math.random()*720}deg)`;
            particle.style.opacity = '0';
        });
        setTimeout(() => particle.remove(), 2200);
    }
}

// ── Animated number counter ──
function animateCounter(el, from, to, duration) {
    if (!el) return;
    const start = performance.now();
    const step = (now) => {
        const progress = Math.min((now - start) / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3); // ease-out cubic
        const current = Math.round(from + (to - from) * eased);
        el.innerText = current;
        if (progress < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
}

function closeModal(id) { document.getElementById(id).style.display = 'none'; }

function calcStreak() {
    const att = JSON.parse(localStorage.getItem('gatt')) || {};
    let streak = 0;
    const d = new Date(); d.setHours(0,0,0,0);
    if (!att[d.toISOString().split('T')[0]]) d.setDate(d.getDate() - 1);
    while (true) {
        const s = d.toISOString().split('T')[0];
        if (att[s]) { streak++; d.setDate(d.getDate() - 1); }
        else break;
        if (streak > 365) break;
    }
    return streak;
}

// ============================================================
// INIT
// ============================================================