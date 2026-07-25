# CHANGES.md — GYM PRO UI Redesign v2

## 2026-07-25 — TASK 1: Multi-file Split (Structural Refactor)

### New file structure
- `/index.html` — Markup only + link/script includes
- `/css/tokens.css` — Design tokens, CSS variables, reset, scrollbar
- `/css/layout.css` — Header, page layout, cards, bottom nav
- `/css/components.css` — Calendar, inputs, buttons, modals
- `/css/pages.css` — Workout, diet, home, timer, toasts, misc
- `/js/state.js` — State vars, routines, default data, helpers, PR confetti
- `/js/theme.js` — Theme toggle, tab switching
- `/js/calendar.js` — Calendar render, history modal
- `/js/workout.js` — renderWorkout, finishWorkout, saveW, copyLast
- `/js/diet.js` — renderDiet, toggleDiet, updateDietBar, diet manager
- `/js/charts.js` — Charts, graphs, body stats
- `/js/timer.js` — Rest timer
- `/js/app.js` — init, updateHeaderStats, week/mood/water, modals, routines, cardio

### What changed
- index.html: Removed all inline style/script blocks. Now references 4 CSS + 8 JS files.
- All CSS extracted and split by section into 4 files
- All JS extracted and split by feature into 8 files
- 0 data-breaking changes: All localStorage keys, function names, HTML IDs unchanged
- All 10 modals and 3 pages preserved exactly

### Verified
- 66 functions across split files
- 10 modals, 15+ key element IDs preserved
- 23 localStorage keys unchanged

## 2026-07-25 — TASK 2: Google Health / Material 3 Visual Redesign

### Design language
- **Typography**: Replaced Barlow Condensed + Barlow with **Inter** (400/500/600/700 weights) across all surfaces. JetBrains Mono retained for numeric data.
- **Accent**: Changed from neon lime (#c8f500) to a soft green-teal (#459b88), matching Google Health's calming aesthetic.
- **Surfaces**: Full Material 3 tonal surface system — 8 surface layers from `surface-dim` through `surface-container-highest`. Cards use elevation shadows instead of borders.
- **No more glassmorphism** — removed all backdrop-filter blur effects. Header, nav, and modals now use solid elevated surfaces.
- **No more glow effects** — removed text-shadows, box-shadow glows, and neon borders.

### Files changed
- **css/tokens.css** (rewritten): 184 lines. New design tokens including Material 3 surface/color system, Inter typography scale, elevation shadows, semantic colors (softer success/partial/fail/pr tones), large rounded corners (16-28px), Material-style transitions.
- **css/layout.css** (rewritten): 144 lines. Simplified header (solid surface, no glass), shared-axis page transitions, cards with elevation shadows (no borders), Material 3 Navigation Bar style with filled state for active nav item.
- **css/components.css** (rewritten): 250 lines. Calendar cells are now circular with tonal backgrounds, inputs use outline-variant borders, all buttons are pill-shaped (border-radius: 9999px) with normal-case text, modals use Material 3 Bottom Sheet style (28px top radius, solid surface, no blur).
- **css/pages.css** (rewritten): 570 lines. Exercise cards use elevation, tags are round pills, protein bar is pill-shaped (no shimmer), diet checkboxes are circular, water glasses use tonal fills, mood buttons have elevated states, week dots use solid color fills, toasts are solid colored chips.

### JS updates (hardcoded values only)
- **js/state.js**: Confetti colors updated to match new palette
- **js/charts.js**: Chart font references simplified
- **js/calendar.js**: Diet tag background changed from `#1a2000` to `var(--primary-container)`
- **js/workout.js**, **js/diet.js**: Template string font references updated from `font-display` to `font-body`

### index.html changes
- Font link: Inter + JetBrains Mono (replacing Barlow Condensed + Barlow)
- Theme color meta: `#11161b` (matches new surface-color)
- Various inline styles updated to remove uppercase/letter-spacing and use new CSS variables
- Timer ring SVG stroke now references `var(--accent)` instead of hardcoded `#c8f500`

### What's preserved
- All 23 localStorage keys unchanged
- All 66 JS function names and logic identical
- All HTML IDs and onclick handlers identical
- All 10 modals with same IDs
- All Chart.js configurations preserved
- Capacitor compatibility maintained (vanilla HTML/CSS/JS, no framework)

## 2026-07-25 — TASK 3: Dynamic Accent Color System (Material You–style)

### What was added
- **`/js/colors.js`** (new, ~160 lines): Complete tonal palette generator from a single seed hex color. Computes `--primary`, `--on-primary`, `--primary-container`, `--on-primary-container`, `--accent`, `--accent-dim`, `--accent-glow`, `--accent-glow-strong`, `--accent-gradient`, and `--accent-gradient-subtle` using HSL color math. Supports both dark and light modes with mode-appropriate container tones. Applied in real time by setting CSS custom properties on `document.documentElement`.

### Preset accent colors (7 + custom)
| Name   | Hex       | Description |
|--------|-----------|-------------|
| Teal   | `#459b88` | Default — soft green-teal |
| Blue   | `#4a8fd4` | Calm sky blue |
| Purple | `#7b6ef0` | Rich violet |
| Pink   | `#e0558b` | Warm rose |
| Orange | `#e07b39` | Energetic amber |
| Green  | `#4caf50` | Fresh green |
| Slate  | `#64748b` | Neutral gray-blue |
| Custom | (picker) | Native `<input type="color">` |

### UI changes
- **Palette button** (🎨) added to the header, between the BF stats chip and the theme toggle.
- **Accent picker modal** (`#accentModal`): Material 3 bottom sheet with 7 circular color swatches + a rainbow custom picker. Active swatch gets a ring highlight. A live preview strip shows primary, container, and dim tones.
- **Swatch styles** in `css/pages.css`: 48px circular buttons with elevation shadows, active ring using `var(--accent)`, scale-on-press feedback.

### How it works
1. User taps 🎨 → modal opens → `renderAccentPicker()` draws swatches
2. Tapping a preset calls `selectAccent(color, name)` → `applyPalette(seedHex, name)`
3. `applyPalette` calls `generatePalette` to compute all 10 CSS variables, sets them on `:root` via `root.style.setProperty()`, saves to `localStorage` (`accentSeed`, `accentName`), and re-renders charts
4. On app init, `restoreAccent()` reads `localStorage.accentSeed` and re-applies the saved palette
5. The custom button opens a native `<input type="color">` — changing it live-updates the theme

### Files changed
- **`/js/colors.js`** — new file (all palette logic)
- **`/css/pages.css`** — added `.accent-swatch` and `.palette-btn` styles
- **`/index.html`** — added palette button in header, `#accentModal` bottom sheet, `<script src="js/colors.js">`
- **`/js/app.js`** — `init()` now calls `restoreAccent()` after setting theme

### Backward compatibility
- If no `accentSeed` exists in localStorage, defaults to Teal (`#459b88`) — identical to the hardcoded values before this change.
- All 23 existing localStorage keys unchanged.
- All existing functions untouched.
