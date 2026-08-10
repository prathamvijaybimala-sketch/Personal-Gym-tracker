package com.gympro.app.ui.train

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gympro.app.data.db.CardioEntity
import com.gympro.app.domain.Dates
import com.gympro.app.ui.AppViewModel
import com.gympro.app.ui.components.EmptyStateCard
import com.gympro.app.ui.components.GymCard
import com.gympro.app.ui.components.MonoText
import com.gympro.app.ui.components.SmallLabel
import com.gympro.app.ui.components.TagChip
import com.gympro.app.ui.components.ToastType
import com.gympro.app.ui.modals.GraphModal
import com.gympro.app.ui.theme.GymColors

@Composable
fun TrainScreen(
    viewModel: TrainViewModel,
    appViewModel: AppViewModel,
    onToast: (String, ToastType) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val graph by viewModel.graph.collectAsStateWithLifecycle()
    val routineManagerOpen by viewModel.routineManagerOpen.collectAsStateWithLifecycle()
    val exManagerOpen by viewModel.exManagerOpen.collectAsStateWithLifecycle()
    val importOpen by viewModel.importOpen.collectAsStateWithLifecycle()
    val aiOpen by viewModel.aiOpen.collectAsStateWithLifecycle()
    val cardioOpen by viewModel.cardioOpen.collectAsStateWithLifecycle()
    val context = LocalContext.current

    viewModel.toastSink = onToast
    viewModel.confettiSink = { appViewModel.triggerConfetti() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().testTag("trainList"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.dayTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                IconButton(onClick = { viewModel.openExerciseManager(state.day) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit exercises")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoutinePill(state.routineName ?: "—") { viewModel.openRoutineManager() }
                OutlinedButton(
                    onClick = { viewModel.openCardioLog() },
                    modifier = Modifier.semantics { contentDescription = "Open cardio log" },
                ) {
                    Text("🏃 Cardio", fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }
        }

        item { DayScroller(state, onSelect = { viewModel.selectDay(it) }) }

        item {
            TimerPresetsCard(
                selected = appViewModel.timerDuration.collectAsStateWithLifecycle().value,
                onSelect = { appViewModel.selectTimerDuration(it) },
                onStart = { appViewModel.startTimer() },
            )
        }

        if (state.isRestDay) {
            item { RestDayCard() }
        } else {
            items(state.cards.size) { index ->
                val card = state.cards[index]
                ExerciseCard(
                    card = card,
                    onWeightChange = { v -> viewModel.updateDraft(card.id, v, card.draftR) },
                    onRepsChange = { v -> viewModel.updateDraft(card.id, card.draftW, v) },
                    onCopyLast = { viewModel.copyLast(card.id) },
                    onOpenTimer = { appViewModel.startTimer() },
                    onOpenGraph = { viewModel.openExerciseGraph(card.id, card.name) },
                    onOpenLink = {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(card.link))
                        )
                    },
                )
            }
            if (state.hasExercises) {
                item {
                    Button(
                        onClick = { viewModel.finishWorkout() },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text("✓ LOG COMPLETE WORKOUT", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }

    // Modals
    graph?.let { g -> GraphModal(data = g, onDismiss = { viewModel.closeGraph() }) }

    if (routineManagerOpen) {
        RoutineManagerModal(viewModel)
    }
    if (exManagerOpen) {
        ExerciseManagerModal(viewModel)
    }
    if (importOpen) {
        ImportModal(viewModel)
    }
    if (aiOpen) {
        AiPromptModal(viewModel)
    }
    if (cardioOpen) {
        CardioModal(viewModel)
    }
}

// ---------------------------------------------------------------------------
// Routine pill + day scroller + timer presets
// ---------------------------------------------------------------------------

@Composable
private fun RoutinePill(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .semantics { contentDescription = "routine pill" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("📋", fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(" ▾", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
    }
}

@Composable
private fun DayScroller(state: TrainViewModel.TrainState, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Dates.DAY_SHORT.forEachIndexed { i, label ->
            val isActive = i == state.day
            val hasLog = state.dayLogs[i] == true && !isActive
            val isToday = i == Dates.jsDayOfWeek(java.time.LocalDate.now())
            val bg = when {
                isActive -> MaterialTheme.colorScheme.primary
                hasLog -> GymColors.Success.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val fg = when {
                isActive -> MaterialTheme.colorScheme.onPrimary
                hasLog -> GymColors.Success
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .then(
                        if (hasLog) Modifier.border(1.dp, GymColors.Success, RoundedCornerShape(999.dp))
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .semantics { contentDescription = "day chip $i" },
            ) {
                Text(
                    label + if (isToday) " ●" else "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = fg,
                )
            }
        }
    }
}

@Composable
private fun TimerPresetsCard(selected: Int, onSelect: (Int) -> Unit, onStart: () -> Unit) {
    GymCard(modifier = Modifier.padding(bottom = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallLabel("REST TIMER")
            MonoText(text = "${selected}s selected", color = MaterialTheme.colorScheme.onSurfaceVariant, size = 11)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(60, 90, 120, 180).forEach { s ->
                val active = s == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .then(if (active) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) else Modifier)
                        .border(
                            1.dp,
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(999.dp),
                        )
                        .clickable { onSelect(s) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        "${s}s",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onStart,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            ) {
                Icon(Icons.Filled.Timer, contentDescription = "Start rest timer", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// REST day + exercise cards
// ---------------------------------------------------------------------------

@Composable
private fun RestDayCard() {
    val transition = rememberInfiniteTransition(label = "breathe")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "breatheScale",
    )
    GymCard(modifier = Modifier.padding(top = 4.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🧘", fontSize = 48.sp, modifier = Modifier.scale(scale))
            Spacer(Modifier.height(12.dp))
            Text(
                "Active Recovery",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Walk · Stretch · Sleep · Eat",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("🚶 Walk 20 min", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("💧 Hydrate well", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("😴 7-9 hrs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    card: TrainViewModel.ExCard,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onCopyLast: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenGraph: () -> Unit,
    onOpenLink: () -> Unit,
) {
    val isPr = card.isPr
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isPr) GymColors.Pr.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline,
                MaterialTheme.shapes.medium,
            )
            .padding(16.dp)
            .semantics { contentDescription = "exercise card ${card.id}" },
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            card.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                        )
                        if (isPr) {
                            PrBadge()
                        }
                    }
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (card.target.isNotEmpty()) {
                            TagChip("🎯 ${card.target}", color = MaterialTheme.colorScheme.primary)
                        }
                        TagChip(card.lastText)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = onCopyLast, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    if (card.link.isNotEmpty()) {
                        Text(
                            "FORM ↗",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onOpenLink)
                                .padding(4.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                StepperField(
                    label = "Weight (kg)",
                    value = card.draftW,
                    onValueChange = onWeightChange,
                    onMinus = { onWeightChange(decrement(card.draftW, 2.5)) },
                    onPlus = { onWeightChange(increment(card.draftW, 2.5)) },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Decimal,
                    fieldTag = "w_${card.id}",
                )
                StepperField(
                    label = "Reps",
                    value = card.draftR,
                    onValueChange = onRepsChange,
                    onMinus = { onRepsChange(decrementInt(card.draftR, 1)) },
                    onPlus = { onRepsChange(incrementInt(card.draftR, 1)) },
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                    fieldTag = "r_${card.id}",
                )
                Column(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onOpenTimer, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Timer, contentDescription = "Start rest timer")
                    }
                    IconButton(onClick = onOpenGraph, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Progress graph")
                    }
                }
            }

            Text(
                if (card.est1rm != null) {
                    "Est 1RM: ${card.est1rm}kg"
                } else {
                    "Enter weight + reps"
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (card.est1rm != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (card.hasData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(card.volumeWidth.coerceIn(0f, 100f) / 100f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrBadge() {
    val transition = rememberInfiniteTransition(label = "prPulse")
    val glow by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "prGlow",
    )
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(
                if (glow > 0.75f) Color(0xFF7C3AED) else Color(0xFFA855F7)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = "PR badge" },
    ) {
        Text(
            "🏆 PR",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
        )
    }
}

private fun increment(value: String, delta: Double): String {
    val v = (value.toDoubleOrNull() ?: 0.0) + delta
    return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
}

private fun decrement(value: String, delta: Double): String {
    val v = ((value.toDoubleOrNull() ?: 0.0) - delta).coerceAtLeast(0.0)
    return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
}

private fun incrementInt(value: String, delta: Int): String =
    ((value.toIntOrNull() ?: 0) + delta).toString()

private fun decrementInt(value: String, delta: Int): String =
    ((value.toIntOrNull() ?: 0) - delta).coerceAtLeast(0).toString()

@Composable
private fun StepperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType,
    fieldTag: String? = null,
) {
    Column(modifier = modifier) {
        SmallLabel(label)
        Row(
            modifier = Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMinus, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease $label", modifier = Modifier.size(18.dp))
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).then(if (fieldTag != null) Modifier.testTag(fieldTag) else Modifier),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
            )
            IconButton(onClick = onPlus, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Increase $label", modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Routine manager modal
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineManagerModal(viewModel: TrainViewModel) {
    val summaries by viewModel.routineSummaries.collectAsStateWithLifecycle()
    val newName by viewModel.newRoutineName.collectAsStateWithLifecycle()
    val renameTarget by viewModel.renameTarget.collectAsStateWithLifecycle()
    val deleteTarget by viewModel.deleteTarget.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = { viewModel.closeRoutineManager() }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                "Routines",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                if (summaries.isEmpty()) {
                    Text("No routines yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                summaries.forEach { r ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (r.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (r.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                MaterialTheme.shapes.medium,
                            )
                            .clickable { viewModel.switchRoutine(r.id) }
                            .padding(14.dp)
                            .semantics { contentDescription = "routine ${r.name}" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.name + if (r.isActive) "  ● ACTIVE" else "",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = if (r.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${r.trainingDays} training day${if (r.trainingDays != 1) "s" else ""}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.requestRename(r.id, r.name) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename ${r.name}", modifier = Modifier.size(18.dp))
                        }
                        if (summaries.size > 1) {
                            Text(
                                "✕",
                                color = GymColors.Fail,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.requestDelete(r.id) }
                                    .padding(6.dp)
                                    .semantics { contentDescription = "Delete ${r.name}" },
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { viewModel.setNewRoutineName(it) },
                    modifier = Modifier.weight(1f).testTag("newRoutineName"),
                    placeholder = { Text("e.g. Full Body, Isolation, Bro Split", fontSize = 13.sp) },
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { viewModel.createRoutine() }) {
                    Text("+ CREATE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = { viewModel.openImportModal() },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Text("📥 Import via JSON", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            OutlinedButton(
                onClick = { viewModel.openAiPromptModal() },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) {
                Text("🤖 AI Prompt Generator", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }

    renameTarget?.let { (id, currentName) ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelRename() },
            title = { Text("Rename routine") },
            text = {
                OutlinedTextField(
                    value = currentName,
                    onValueChange = { viewModel.setRenameName(it) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRename() }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRename() }) { Text("CANCEL") }
            },
        )
    }

    deleteTarget?.let { id ->
        val name = summaries.firstOrNull { it.id == id }?.name ?: ""
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete routine?") },
            text = { Text("\"$name\" will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) { Text("DELETE", color = GymColors.Fail) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text("CANCEL") }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Exercise manager modal
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseManagerModal(viewModel: TrainViewModel) {
    val list by viewModel.exManagerList.collectAsStateWithLifecycle()
    val dayName by viewModel.exManagerDayName.collectAsStateWithLifecycle()
    val formName by viewModel.exFormName.collectAsStateWithLifecycle()
    val formTarget by viewModel.exFormTarget.collectAsStateWithLifecycle()
    val formLink by viewModel.exFormLink.collectAsStateWithLifecycle()
    val editing by viewModel.editingIndex.collectAsStateWithLifecycle()
    val dayTitle = Dates.DAY_NAMES[viewModel.exManagerDay.collectAsStateWithLifecycle().value]

    ModalBottomSheet(onDismissRequest = { viewModel.closeExerciseManager() }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                dayName.ifEmpty { dayTitle },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )

            list.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(row.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            (row.target.ifEmpty { "—" } + if (row.link.isNotEmpty()) " · Form ↗" else ""),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { viewModel.startEditExercise(row.index) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit ${row.name}", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "✕",
                        color = GymColors.Fail,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.removeExercise(row.index) }
                            .padding(6.dp)
                            .semantics { contentDescription = "Remove ${row.name}" },
                    )
                }
            }
            if (list.isEmpty()) {
                Text(
                    "No exercises. Add below.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }

            Column(
                modifier = Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallLabel("DAY NAME", modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = dayName,
                    onValueChange = { viewModel.setExDayName(it) },
                    modifier = Modifier.fillMaxWidth().testTag("exMgrDayName"),
                    placeholder = { Text("e.g. PUSH", fontSize = 13.sp) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = formName,
                    onValueChange = { viewModel.setExFormName(it) },
                    modifier = Modifier.fillMaxWidth().testTag("exMgrName"),
                    placeholder = { Text("Exercise name", fontSize = 13.sp) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = formTarget,
                    onValueChange = { viewModel.setExFormTarget(it) },
                    modifier = Modifier.fillMaxWidth().testTag("exMgrTarget"),
                    placeholder = { Text("Target (e.g. 3×10)", fontSize = 13.sp) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = formLink,
                    onValueChange = { viewModel.setExFormLink(it) },
                    modifier = Modifier.fillMaxWidth().testTag("exMgrLink"),
                    placeholder = { Text("Form guide URL (optional)", fontSize = 13.sp) },
                    singleLine = true,
                )
                Button(
                    onClick = { viewModel.saveExerciseFromManager() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (editing >= 0) "SAVE CHANGES" else "ADD EXERCISE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                }
                if (editing >= 0) {
                    OutlinedButton(onClick = { viewModel.cancelExerciseEdit() }, modifier = Modifier.fillMaxWidth()) {
                        Text("CANCEL", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Import modal
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportModal(viewModel: TrainViewModel) {
    val text by viewModel.importText.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = { viewModel.closeImportModal() }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                "Import Workout JSON",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            SmallLabel("Paste your workout JSON below", modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { viewModel.setImportText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("jsonImportArea"),
                placeholder = { Text("{\"name\":\"My Routine\",\"days\":{...}}", fontSize = 12.sp) },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
            Text(
                "EXPECTED FORMAT\nUse the AI Prompt Generator to get a JSON that fits this format automatically.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { viewModel.importWorkoutJson() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("IMPORT ROUTINE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AI prompt modal
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiPromptModal(viewModel: TrainViewModel) {
    val type by viewModel.aiType.collectAsStateWithLifecycle()
    val prompt by viewModel.aiPrompt.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = { viewModel.closeAiPromptModal() }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                "🤖 AI Prompt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            SmallLabel("What kind of workout do you want?", modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            OutlinedTextField(
                value = type,
                onValueChange = { viewModel.setAiType(it) },
                modifier = Modifier.fillMaxWidth().testTag("aiRoutineType"),
                placeholder = { Text("e.g. 6-day PPL, 4-day Upper Lower, Isolation...", fontSize = 13.sp) },
                singleLine = true,
            )
            Button(
                onClick = { viewModel.generatePrompt() },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Text("GENERATE PROMPT", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            }
            if (prompt != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallLabel("Copy this → paste in Claude / ChatGPT")
                    TextButton(onClick = { viewModel.copyPrompt() }) {
                        Text("📋 Copy", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                        .testTag("aiPromptText"),
                ) {
                    Text(
                        prompt ?: "",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "After the AI replies with JSON, come back here → Routines → Import via JSON → paste it.",
                    modifier = Modifier.padding(top = 10.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Cardio modal
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardioModal(viewModel: TrainViewModel) {
    val type by viewModel.cardioType.collectAsStateWithLifecycle()
    val duration by viewModel.cardioDuration.collectAsStateWithLifecycle()
    val calories by viewModel.cardioCalories.collectAsStateWithLifecycle()
    val distance by viewModel.cardioDistance.collectAsStateWithLifecycle()
    val notes by viewModel.cardioNotes.collectAsStateWithLifecycle()
    val history by viewModel.cardioHistory.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = { viewModel.closeCardioLog() }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                "🏃 Cardio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuDefaults.MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        label = { Text("Type") },
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        viewModel.cardioTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    viewModel.setCardioType(t)
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StepperField(
                        label = "Duration (min)",
                        value = duration,
                        onValueChange = { viewModel.setCardioDuration(it) },
                        onMinus = { viewModel.adjustCardioDuration(-5) },
                        onPlus = { viewModel.adjustCardioDuration(5) },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                        fieldTag = "cardioDuration",
                    )
                    StepperField(
                        label = "Calories",
                        value = calories,
                        onValueChange = { viewModel.setCardioCalories(it) },
                        onMinus = { viewModel.adjustCardioCalories(-50) },
                        onPlus = { viewModel.adjustCardioCalories(50) },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                        fieldTag = "cardioCalories",
                    )
                }

                OutlinedTextField(
                    value = distance,
                    onValueChange = { viewModel.setCardioDistance(it) },
                    modifier = Modifier.fillMaxWidth().testTag("cardioDistance"),
                    placeholder = { Text("Distance (optional, km) e.g. 3.5", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { viewModel.setCardioNotes(it) },
                    modifier = Modifier.fillMaxWidth().testTag("cardioNotes"),
                    placeholder = { Text("Notes — e.g. Zone 2, intervals, easy pace", fontSize = 13.sp) },
                    singleLine = true,
                )
                Button(
                    onClick = { viewModel.logCardio() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("LOG CARDIO", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                }

                SmallLabel("Recent Sessions", modifier = Modifier.padding(top = 8.dp))
                if (history.isEmpty()) {
                    Text("No cardio logged yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    history.forEach { entry ->
                        CardioRow(entry, onDelete = { viewModel.deleteCardio(entry.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CardioRow(entry: CardioEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.type, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(GymColors.Cardio.copy(alpha = 0.15f))
                        .border(1.dp, GymColors.Cardio, RoundedCornerShape(5.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Text(
                        "${entry.duration} min",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF818CF8),
                    )
                }
            }
            val extras = buildList {
                entry.distance?.let { add(trimNum(it) + "km") }
                entry.calories?.let { add("$it kcal") }
                if (entry.notes.isNotEmpty()) add(entry.notes)
            }
            Text(
                (entry.date + if (extras.isNotEmpty()) " · " + extras.joinToString(" · ") else ""),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Text(
            "✕",
            color = GymColors.Fail,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(onClick = onDelete)
                .padding(6.dp)
                .semantics { contentDescription = "Delete cardio ${entry.id}" },
        )
    }
}

private fun trimNum(v: Double): String {
    val s = v.toString()
    return if (s.endsWith(".0")) s.dropLast(2) else s
}
