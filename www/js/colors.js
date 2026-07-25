/* ============================================================
   MATERIAL-YOU DYNAMIC ACCENT COLOR SYSTEM
   ============================================================ */

// ── Preset accent colors ──
const ACCENT_PRESETS = [
    { name: 'Teal',     color: '#459b88', darkContainer: '#1e3d36', darkOnContainer: '#a7e0cf' },
    { name: 'Blue',     color: '#4a8fd4', darkContainer: '#1a2d44', darkOnContainer: '#abcbf0' },
    { name: 'Purple',   color: '#7b6ef0', darkContainer: '#241e44', darkOnContainer: '#c8c2ff' },
    { name: 'Pink',     color: '#e0558b', darkContainer: '#3d1a2a', darkOnContainer: '#ffc4da' },
    { name: 'Orange',   color: '#e07b39', darkContainer: '#3d2410', darkOnContainer: '#ffcfb0' },
    { name: 'Green',    color: '#4caf50', darkContainer: '#1a381a', darkOnContainer: '#a5d6a7' },
    { name: 'Slate',    color: '#64748b', darkContainer: '#1e2630', darkOnContainer: '#cbd5e1' },
    { name: 'Custom',   color: null }, /* opens native color picker */
];

// ── HSL manipulation helpers ──
function hexToHSL(hex) {
    let r = 0, g = 0, b = 0;
    const hx = hex.replace('#', '');
    if (hx.length === 3) {
        r = parseInt(hx[0]+hx[0], 16) / 255;
        g = parseInt(hx[1]+hx[1], 16) / 255;
        b = parseInt(hx[2]+hx[2], 16) / 255;
    } else {
        r = parseInt(hx.slice(0,2), 16) / 255;
        g = parseInt(hx.slice(2,4), 16) / 255;
        b = parseInt(hx.slice(4,6), 16) / 255;
    }
    const max = Math.max(r,g,b), min = Math.min(r,g,b);
    let h = 0, s = 0, l = (max + min) / 2;
    if (max !== min) {
        const d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch (max) {
            case r: h = ((g - b) / d + (g < b ? 6 : 0)) / 6; break;
            case g: h = ((b - r) / d + 2) / 6; break;
            case b: h = ((r - g) / d + 4) / 6; break;
        }
    }
    return { h: Math.round(h * 360), s: Math.round(s * 100), l: Math.round(l * 100) };
}

function hslToHex(h, s, l) {
    s /= 100; l /= 100;
    const a = s * Math.min(l, 1 - l);
    const f = n => {
        const k = (n + h / 30) % 12;
        const color = l - a * Math.max(Math.min(k - 3, 9 - k, 1), -1);
        return Math.round(255 * color).toString(16).padStart(2, '0');
    };
    return `#${f(0)}${f(8)}${f(4)}`;
}

function blendColor(hex1, hex2, ratio) {
    const c1 = parseInt(hex1.replace('#',''), 16);
    const c2 = parseInt(hex2.replace('#',''), 16);
    const r = Math.round(((c1 >> 16) * (1-ratio) + (c2 >> 16) * ratio));
    const g = Math.round((((c1 >> 8) & 0xff) * (1-ratio) + ((c2 >> 8) & 0xff) * ratio));
    const b = Math.round(((c1 & 0xff) * (1-ratio) + (c2 & 0xff) * ratio));
    return '#' + [r,g,b].map(v => v.toString(16).padStart(2,'0')).join('');
}

function isLightMode() {
    return document.documentElement.getAttribute('data-theme') === 'light';
}

// ── Generate full tonal palette from a seed hex ──
function generatePalette(seedHex) {
    const hsl = hexToHSL(seedHex);
    const isLight = isLightMode();

    // Determine if the seed is light or dark
    const seedIsDark = hsl.l < 40;

    // Primary = seed color itself
    const primary = seedHex;

    // On-primary: white if seed is dark, dark if seed is light
    const onPrimary = seedIsDark ? '#ffffff' : '#1a1a1a';

    // Primary-container: tint tone for dark mode, pastel for light
    const container = isLight
        ? hslToHex(hsl.h, Math.min(hsl.s, 40), 90)
        : hslToHex(hsl.h, hsl.s, 25);

    // On-primary-container: readable text on container
    const onContainer = isLight
        ? hslToHex(hsl.h, Math.max(hsl.s, 50), 10)
        : hslToHex(hsl.h, Math.min(hsl.s, 60), 85);

    // Darker variant for active/selected states
    const dim = hslToHex(hsl.h, Math.min(hsl.s, 80), Math.max(hsl.l - 12, 8));

    // Glow/RGBA variants
    const rgbValues = [
        parseInt(primary.slice(1,3), 16),
        parseInt(primary.slice(3,5), 16),
        parseInt(primary.slice(5,7), 16)
    ];

    const glow = `rgba(${rgbValues.join(',')}, 0.12)`;
    const glowStrong = `rgba(${rgbValues.join(',')}, 0.22)`;
    const gradient = primary;
    const gradientSubtle = `linear-gradient(135deg, rgba(${rgbValues.join(',')},0.08), transparent)`;

    return {
        '--primary': primary,
        '--on-primary': onPrimary,
        '--primary-container': container,
        '--on-primary-container': onContainer,
        '--accent': primary,
        '--accent-dim': dim,
        '--accent-glow': glow,
        '--accent-glow-strong': glowStrong,
        '--accent-gradient': gradient,
        '--accent-gradient-subtle': gradientSubtle,
    };
}

// ── Apply a palette to the document ──
function applyPalette(seedHex, presetName) {
    const palette = generatePalette(seedHex);
    const root = document.documentElement;
    Object.entries(palette).forEach(([key, value]) => {
        root.style.setProperty(key, value);
    });
    // Persist
    localStorage.setItem('accentSeed', seedHex);
    localStorage.setItem('accentName', presetName || '');

    // Re-render charts to pick up new accent colors
    if (typeof dietChartInstance !== 'undefined' && dietChartInstance) {
        dietChartInstance.destroy();
        if (typeof renderDietTrend === 'function') renderDietTrend();
    }
    if (typeof chartInstance !== 'undefined' && chartInstance) {
        chartInstance.destroy();
        chartInstance = null;
    }
}

// ── Restore saved accent on load ──
function restoreAccent() {
    const saved = localStorage.getItem('accentSeed');
    if (saved) {
        applyPalette(saved, localStorage.getItem('accentName') || '');
        return true;
    }
    return false;
}

// ── Open native color picker ──
function pickCustomColor() {
    const input = document.createElement('input');
    input.type = 'color';
    input.value = localStorage.getItem('accentSeed') || '#459b88';
    input.addEventListener('input', () => {
        applyPalette(input.value, 'Custom');
        renderAccentPicker(); // update checkmarks
    });
    input.addEventListener('change', () => {
        applyPalette(input.value, 'Custom');
        renderAccentPicker();
    });
    input.click();
}

// ── Open accent picker modal ──
function openAccentPicker() {
    document.getElementById('accentModal').style.display = 'flex';
    renderAccentPicker();
}

// ── Render accent swatches ──
function renderAccentPicker() {
    const container = document.getElementById('accentSwatches');
    const currentSeed = localStorage.getItem('accentSeed') || '#459b88';
    let html = '<div style="display:flex;flex-wrap:wrap;gap:12px;justify-content:center;">';
    ACCENT_PRESETS.forEach((preset, idx) => {
        const isCustom = preset.color === null;
        const displayColor = isCustom ? (localStorage.getItem('accentSeed') || '#459b88') : preset.color;
        const isActive = !isCustom && displayColor.toLowerCase() === currentSeed.toLowerCase();
        // For custom, also check if name is 'Custom'
        const isActiveCustom = isCustom && localStorage.getItem('accentName') === 'Custom';

        if (isCustom) {
            html += `
                <div style="display:flex;flex-direction:column;align-items:center;gap:6px;">
                    <button class="accent-swatch ${isActiveCustom ? 'active' : ''}"
                        onclick="pickCustomColor()"
                        style="background:conic-gradient(red,yellow,lime,cyan,blue,magenta,red);"
                        title="Custom color">
                        <span style="font-size:18px;">🎨</span>
                    </button>
                    <span style="font-size:0.625rem;color:var(--text-sub);">Custom</span>
                </div>`;
        } else {
            html += `
                <div style="display:flex;flex-direction:column;align-items:center;gap:6px;">
                    <button class="accent-swatch ${isActive ? 'active' : ''}"
                        onclick="selectAccent('${displayColor}','${preset.name}')"
                        style="background:${displayColor};" title="${preset.name}">
                        ${isActive ? '<span style="color:white;font-size:14px;">✓</span>' : ''}
                    </button>
                    <span style="font-size:0.625rem;color:${isActive ? 'var(--accent)' : 'var(--text-sub)'};font-weight:${isActive ? 600 : 400};">${preset.name}</span>
                </div>`;
        }
    });
    html += '</div>';

    // Add live preview strip
    html += `
        <div style="margin-top:20px;display:flex;gap:8px;align-items:center;justify-content:center;flex-wrap:wrap;">
            <div style="width:40px;height:40px;border-radius:50%;background:var(--primary);" title="Primary"></div>
            <div style="width:40px;height:40px;border-radius:50%;background:var(--primary-container);border:1px solid var(--outline-variant);" title="Container"></div>
            <div style="width:40px;height:40px;border-radius:50%;background:var(--accent-dim);" title="Dim"></div>
            <span style="font-size:0.75rem;color:var(--text-sub);margin-left:4px;">Preview</span>
        </div>`;

    container.innerHTML = html;
}

// ── Select a preset accent ──
function selectAccent(color, name) {
    applyPalette(color, name);
    renderAccentPicker();
    if (typeof showToast === 'function') {
        showToast(`${name} accent applied`, 'info', '🎨');
    }
}
