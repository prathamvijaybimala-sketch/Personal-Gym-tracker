/* THEME & NAVIGATION */
function toggleTheme() {
    const root = document.documentElement;
    if(root.getAttribute('data-theme') === 'light') {
        root.removeAttribute('data-theme');
        localStorage.setItem('theme', 'dark');
    } else {
        root.setAttribute('data-theme', 'light');
        localStorage.setItem('theme', 'light');
    }
    // Re-render charts if open
    if(dietChartInstance) renderDietTrend();
}

function switchTab(tab, btn) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.getElementById(tab + 'Page').classList.add('active');
    btn.classList.add('active');
    if(tab === 'diet') renderDietTrend();
}

// ============================================================
// WEEK SUMMARY