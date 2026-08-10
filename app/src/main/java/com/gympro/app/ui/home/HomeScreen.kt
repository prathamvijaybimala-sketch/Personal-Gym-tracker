package com.gympro.app.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gympro.app.domain.Dates
import com.gympro.app.ui.components.AnimatedCounter
import com.gympro.app.ui.components.CardTitle
import com.gympro.app.ui.components.EmptyStateCard
import com.gympro.app.ui.components.GymCard
import com.gympro.app.ui.components.MonoText
import com.gympro.app.ui.components.SmallLabel
import com.gympro.app.ui.components.ToastType
import com.gympro.app.ui.modals.GraphModal
import com.gympro.app.ui.modals.HistoryModal
import com.gympro.app.ui.theme.GymColors

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onToast: (String, ToastType) -> Unit,
    onOpenBfModal: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val graph by viewModel.graph.collectAsStateWithLifecycle()
    val exportUri by viewModel.exportUri.collectAsStateWithLifecycle()
    val context = LocalContext.current

    viewModel.toastSink = onToast

    // Share the exported backup via the system share sheet
    LaunchedEffect(exportUri) {
        val uri = exportUri ?: return@LaunchedEffect
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share backup"))
        viewModel.consumeExportUri()
    }

    // Restore via the system file picker
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFromUri(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().testTag("homeList"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp
        ),
    ) {
        item { WeekCard(state) }
        item {
            CalendarCard(
                state = state,
                onPrevMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) },
                onJumpToday = { viewModel.jumpToToday() },
                onModeChange = { viewModel.setCalendarMode(it) },
                onDayClick = { viewModel.openHistory(it) },
            )
        }
        item {
            BodyStatsCard(
                state = state,
                onWeightInput = { viewModel.setWeightInput(it) },
                onLogWeight = { viewModel.logWeight() },
                onOpenWeightGraph = { viewModel.openGraph("wt", "Body Weight") },
            )
        }
        item {
            ToolsCard(
                onOpenBf = onOpenBfModal,
                onOpenBfGraph = { viewModel.openGraph("bf", "Body Fat %") },
                onOpenWeightGraph = { viewModel.openGraph("wt", "Body Weight") },
            )
        }
        item { MoodCard(state.mood, onMood = { viewModel.saveMood(it) }) }
        item {
            DataCard(
                onExport = { viewModel.exportData() },
                onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/*")) },
            )
        }
    }

    history?.let { h ->
        HistoryModal(
            data = h,
            onDismiss = { viewModel.closeHistory() },
            onNoteChange = { viewModel.saveNote(h.dateKey, it) },
        )
    }

    graph?.let { g ->
        GraphModal(data = g, onDismiss = { viewModel.closeGraph() })
    }
}

// ---------------------------------------------------------------------------
// This Week
// ---------------------------------------------------------------------------

@Composable
private fun WeekCard(state: HomeViewModel.HomeState) {
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("This Week") {
            MonoText(text = "${state.weekVolume}/6 days", color = MaterialTheme.colorScheme.onSurfaceVariant, size = 11)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.weekDots.forEach { dot ->
                val color = when {
                    dot.done -> GymColors.Accent
                    dot.isFuture -> MaterialTheme.colorScheme.surfaceVariant
                    else -> GymColors.Fail.copy(alpha = 0.35f)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                        .border(
                            1.dp,
                            if (dot.done) GymColors.Accent else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(3.dp),
                        )
                        .semantics {
                            contentDescription = "week dot ${dot.dateKey} ${if (dot.done) "done" else "missed"}"
                        },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Calendar
// ---------------------------------------------------------------------------

@Composable
private fun CalendarCard(
    state: HomeViewModel.HomeState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onModeChange: (String) -> Unit,
    onDayClick: (String) -> Unit,
) {
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("Consistency") {
            Text(
                "TODAY",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onJumpToday() }
                    .padding(4.dp),
            )
        }

        // Gym / Diet mode toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
        ) {
            CalendarModeButton("🏋️ Gym", state.calendarMode == "gym") { onModeChange("gym") }
            CalendarModeButton("🥗 Diet", state.calendarMode == "diet") { onModeChange("diet") }
        }

        // Month header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                state.monthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        // Day-of-week header
        Row {
            Dates.DAY_SHORT.forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Grid: 7 columns
        val weeks = state.calendarCells.chunked(7)
        weeks.forEach { weekCells ->
            Row {
                weekCells.forEach { cell ->
                    if (cell == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        CalendarDayCell(cell, onDayClick)
                    }
                }
                repeat(7 - weekCells.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CalendarModeButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .then(if (active) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CalendarDayCell(cell: HomeViewModel.CalendarCell, onDayClick: (String) -> Unit) {
    val bg = when (cell.status) {
        HomeViewModel.CellStatus.GREEN -> GymColors.Success.copy(alpha = 0.15f)
        HomeViewModel.CellStatus.YELLOW -> GymColors.Partial.copy(alpha = 0.15f)
        HomeViewModel.CellStatus.RED -> GymColors.Fail.copy(alpha = 0.12f)
        HomeViewModel.CellStatus.PLAIN -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when (cell.status) {
        HomeViewModel.CellStatus.GREEN -> GymColors.Success
        HomeViewModel.CellStatus.YELLOW -> GymColors.Partial
        HomeViewModel.CellStatus.RED -> GymColors.Fail
        HomeViewModel.CellStatus.PLAIN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .then(
                if (cell.isToday) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp))
                } else {
                    Modifier.border(1.dp, Color.Transparent, RoundedCornerShape(5.dp))
                }
            )
            .semantics {
                contentDescription = "calendar day ${cell.dateKey} ${cell.status.name.lowercase()}"
            }
            .clickable { onDayClick(cell.dateKey) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            cell.day.toString(),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
        if (cell.hasNote) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Body Stats
// ---------------------------------------------------------------------------

@Composable
private fun BodyStatsCard(
    state: HomeViewModel.HomeState,
    onWeightInput: (String) -> Unit,
    onLogWeight: () -> Unit,
    onOpenWeightGraph: () -> Unit,
) {
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("Body Stats")

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "Weight",
                value = if (state.latestWeight == "--") "--" else state.latestWeight + "kg",
                modifier = Modifier.weight(1f),
            ) {
                state.weightDelta?.let { delta ->
                    Text(
                        delta,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (state.weightDeltaIsUp) GymColors.Fail else GymColors.Success,
                    )
                }
            }
            StatTile(
                label = "Body Fat",
                value = state.latestBf,
                modifier = Modifier.weight(1f),
            )
        }

        SmallLabel("Log Today's Weight (kg)", modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.weightInput,
                onValueChange = onWeightInput,
                modifier = Modifier.weight(1f).testTag("wtInput"),
                placeholder = { Text("e.g. 75.5", fontSize = 14.sp) },
                singleLine = true,
            )
            IconButton(onClick = onOpenWeightGraph) {
                Icon(Icons.Filled.ShowChart, contentDescription = "Weight graph")
            }
            Button(
                onClick = onLogWeight,
                modifier = Modifier.padding(top = 8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text("LOG", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    extra: @Composable (() -> Unit)? = null,
) {
    val numeric = value.filter { it.isDigit() || it == '.' || it == '-' }.toDoubleOrNull()
    val suffix = value.substringAfter(numeric?.let { value.indexOfFirst { c -> !c.isDigit() && c != '.' && c != '-' } } ?: value.length)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        if (numeric != null) {
            val animated by androidx.compose.animation.core.animateFloatAsState(
                targetValue = numeric.toFloat(),
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 600),
                label = "stat",
            )
            Text(
                text = formatAnimated(animated, numeric) + suffix,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
        extra?.invoke()
    }
}

private fun formatAnimated(animated: Float, target: Double): String {
    val shown = animated.toDouble()
    return if (target == target.toLong().toDouble()) shown.toLong().toString() else String.format("%.1f", shown)
}

// ---------------------------------------------------------------------------
// Tools
// ---------------------------------------------------------------------------

@Composable
private fun ToolsCard(
    onOpenBf: () -> Unit,
    onOpenBfGraph: () -> Unit,
    onOpenWeightGraph: () -> Unit,
) {
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("Tools")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolButton(icon = Icons.Filled.Straighten, label = "Navy BF") { onOpenBf() }
            ToolButton(icon = Icons.Filled.BarChart, label = "BF Graph") { onOpenBfGraph() }
            ToolButton(icon = Icons.Filled.ShowChart, label = "Wt Graph") { onOpenWeightGraph() }
        }
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Mood
// ---------------------------------------------------------------------------

@Composable
private fun MoodCard(mood: String?, onMood: (String) -> Unit) {
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("Today's Mood") {
            mood?.let {
                MonoText(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant, size = 11)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "💀" to "Dead", "😤" to "Tired", "😐" to "OK",
                "💪" to "Strong", "🔥" to "Beast",
            ).forEach { (emoji, label) ->
                val selected = mood == emoji
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .then(
                            if (selected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        .border(
                            2.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            MaterialTheme.shapes.medium,
                        )
                        .clickable { onMood(emoji) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(emoji, fontSize = 20.sp)
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Data (backup / restore)
// ---------------------------------------------------------------------------

@Composable
private fun DataCard(onExport: () -> Unit, onRestore: () -> Unit) {
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("Data")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.weight(1f).semantics { contentDescription = "Export backup" },
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Backup", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onRestore,
                modifier = Modifier.weight(1f).semantics { contentDescription = "Restore backup" },
            ) {
                Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
            }
        }
    }
}
