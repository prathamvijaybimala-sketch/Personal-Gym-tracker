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
