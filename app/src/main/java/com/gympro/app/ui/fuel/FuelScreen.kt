package com.gympro.app.ui.fuel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gympro.app.data.db.SuppItemEntity
import com.gympro.app.domain.GoalMath
import com.gympro.app.ui.components.BarChart
import com.gympro.app.ui.components.CardTitle
import com.gympro.app.ui.components.GymCard
import com.gympro.app.ui.components.MonoText
import com.gympro.app.ui.components.SmallLabel
import com.gympro.app.ui.components.ToastType
import com.gympro.app.ui.components.proteinBarColor
import com.gympro.app.ui.theme.GymColors
import kotlin.math.roundToInt

@Composable
fun FuelScreen(
    viewModel: FuelViewModel,
    onToast: (String, ToastType) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val managerOpen by viewModel.managerOpen.collectAsStateWithLifecycle()
    val goalOpen by viewModel.goalOpen.collectAsStateWithLifecycle()

    viewModel.toastSink = onToast

    LazyColumn(
        modifier = Modifier.fillMaxWidth().testTag("fuelList"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
    ) {
        item { ProteinCard(state, viewModel) }
        item { WaterCard(state, viewModel) }
        item { SuppsCard(state, onEdit = { viewModel.openDietManager("supp") }, onToggle = { viewModel.toggleItem(it) }) }
        item { ProteinTrendCard(state) }
        item { WaterTrendCard(state) }
    }

    if (managerOpen) {
        DietManagerModal(viewModel)
    }
    if (goalOpen) {
        GoalModal(viewModel)
    }
}

// ---------------------------------------------------------------------------
// Protein
// ---------------------------------------------------------------------------

@Composable
private fun ProteinCard(state: FuelViewModel.FuelState, viewModel: FuelViewModel) {
    val quickProtein by viewModel.quickProtein.collectAsStateWithLifecycle()
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("Protein") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { viewModel.openGoalEditor("protein") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("⚙ Goal", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { viewModel.openDietManager("menu") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("✎ Edit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Progress bar
        val ratio = GoalMath.proteinRatio(state.totalProtein, state.proteinGoal)
        val level = GoalMath.proteinBarLevel(state.totalProtein, state.proteinGoal)
        val barColor = when (level) {
            GoalMath.BarLevel.GREEN -> GymColors.Success
            GoalMath.BarLevel.AMBER -> GymColors.Partial
            GoalMath.BarLevel.RED -> GymColors.Fail
        }
        val animatedRatio by animateFloatAsState(
            targetValue = ratio,
            animationSpec = tween(durationMillis = 600),
            label = "proteinBar",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .semantics { contentDescription = "protein bar $ratio" },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedRatio.coerceIn(0f, 1f))
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(barColor),
            )
            Text(
                "${state.totalProtein.roundToInt()} / ${state.proteinGoal}g",
                modifier = Modifier.align(Alignment.Center),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
            )
        }

        // Checkable food items
        state.items.forEach { item ->
            val checked = item.id in state.checks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .clickable { viewModel.toggleItem(item.id) }
                    .semantics { contentDescription = "diet item ${item.id} ${if (checked) "checked" else "unchecked"}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .then(
                            if (checked) Modifier.background(MaterialTheme.colorScheme.primary)
                            else Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (checked) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(item.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                MonoText(text = "${item.protein.toInt()}g", color = MaterialTheme.colorScheme.primary, size = 12)
            }
        }

        // Quick add (divider above, like the web's border-top)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            SmallLabel("Quick Add Protein")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = quickProtein,
                    onValueChange = { viewModel.setQuickProtein(it) },
                    modifier = Modifier.weight(1f).testTag("quickProt"),
                    placeholder = { Text("grams", fontSize = 14.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.quickAddProtein() },
                    contentPadding = PaddingValues(horizontal = 20.dp),
                ) {
                    Text("ADD", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, fontSize = 12.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Water
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaterCard(state: FuelViewModel.FuelState, viewModel: FuelViewModel) {
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("Water") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoText(
                    text = "${state.waterCount} / ${state.waterTarget} glasses",
                    color = MaterialTheme.colorScheme.primary,
                    size = 12,
                )
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                    onClick = { viewModel.openGoalEditor("water") },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("⚙ Goal", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(state.waterTarget) { i ->
                val filled = i < state.waterCount
                Box(
                    modifier = Modifier
                        .size(width = 34.dp, height = 40.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 7.dp, bottomStart = 7.dp))
                        .then(
                            if (filled) Modifier.background(GymColors.Water.copy(alpha = 0.2f))
                            else Modifier
                        )
                        .border(
                            2.dp,
                            if (filled) GymColors.Water else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 7.dp, bottomStart = 7.dp),
                        )
                        .clickable { viewModel.toggleWater(i) }
                        .semantics { contentDescription = "water glass $i ${if (filled) "filled" else "empty"}" },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text("💧", fontSize = 12.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Supplements
// ---------------------------------------------------------------------------

@Composable
private fun SuppsCard(state: FuelViewModel.FuelState, onEdit: () -> Unit, onToggle: (String) -> Unit) {
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("Supplements") {
            OutlinedButton(
                onClick = onEdit,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text("✎ Edit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (state.supps.isEmpty()) {
            Text(
                "None scheduled today.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        } else {
            state.supps.forEach { (supp, checked) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { onToggle(supp.id) }
                        .padding(14.dp)
                        .semantics { contentDescription = "supp item ${supp.id} ${if (checked) "checked" else "unchecked"}" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(supp.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            scheduleLabel(supp.schedule),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (checked) Modifier.background(MaterialTheme.colorScheme.primary)
                                else Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (checked) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun scheduleLabel(schedule: String): String = when (schedule) {
    "daily" -> "Daily"
    "alt" -> "Alt Day"
    "0" -> "Sunday"
    "1" -> "Monday"
    "2" -> "Tuesday"
    "3" -> "Wednesday"
    "4" -> "Thursday"
    "5" -> "Friday"
    "6" -> "Saturday"
    else -> "Weekly"
}

// ---------------------------------------------------------------------------
// 7-day trends
// ---------------------------------------------------------------------------

@Composable
private fun ProteinTrendCard(state: FuelViewModel.FuelState) {
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("7-Day Protein")
        BarChart(
            labels = state.weekProtein.map { it.first },
            data = state.weekProtein.map { it.second },
            barColors = state.weekProtein.map { proteinBarColor(it.second, state.proteinGoal) },
            ySuffix = "g",
        )
    }
}

@Composable
private fun WaterTrendCard(state: FuelViewModel.FuelState) {
    GymCard(modifier = Modifier.padding(bottom = 14.dp)) {
        CardTitle("7-Day Water")
        BarChart(
            labels = state.weekWater.map { it.first },
            data = state.weekWater.map { it.second },
            barColors = List(7) { GymColors.Water.copy(alpha = 0.8f) },
            ySuffix = "",
            yMaxOverride = (state.weekWaterTarget * 1.3).toDouble(),
        )
    }
}

// ---------------------------------------------------------------------------
// Diet manager modal
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DietManagerModal(viewModel: FuelViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mode by viewModel.managerMode.collectAsStateWithLifecycle()
    val mName by viewModel.mName.collectAsStateWithLifecycle()
    val mValue by viewModel.mValue.collectAsStateWithLifecycle()
    val isMenu = mode == "menu"

    ModalBottomSheet(onDismissRequest = { viewModel.closeDietManager() }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                if (isMenu) "Food Menu" else "Supplements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )

            if (isMenu) {
                state.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "${item.protein.toInt()}g protein",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "✕",
                            color = GymColors.Fail,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.removeItem(item.id) }
                                .padding(6.dp)
                                .semantics { contentDescription = "Remove food ${item.name}" },
                        )
                    }
                }
            } else {
                state.allSupps.forEach { supp ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(supp.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                scheduleLabel(supp.schedule),
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "✕",
                            color = GymColors.Fail,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.removeItem(supp.id) }
                                .padding(6.dp)
                                .semantics { contentDescription = "Remove supp ${supp.name}" },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = mName,
                    onValueChange = { viewModel.setMName(it) },
                    modifier = Modifier.fillMaxWidth().testTag("mName"),
                    placeholder = { Text(if (isMenu) "Item name" else "Supplement name", fontSize = 13.sp) },
                    singleLine = true,
                )
                if (isMenu) {
                    OutlinedTextField(
                        value = mValue,
                        onValueChange = { viewModel.setMValue(it) },
                        modifier = Modifier.fillMaxWidth().testTag("mValue"),
                        placeholder = { Text("Protein (g)", fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Button(
                        onClick = { viewModel.addItem() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("ADD FOOD", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                    }
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = mValue,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuDefaults.MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            placeholder = { Text("Schedule", fontSize = 13.sp) },
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf(
                                "daily" to "Every Day",
                                "alt" to "Alternate Days",
                                "1" to "Monday",
                                "2" to "Tuesday",
                                "3" to "Wednesday",
                                "4" to "Thursday",
                                "5" to "Friday",
                                "6" to "Saturday",
                                "0" to "Sunday",
                            ).forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setMValue(code)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { viewModel.addItem() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("ADD SUPP", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Goal modal
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalModal(viewModel: FuelViewModel) {
    val mode by viewModel.goalMode.collectAsStateWithLifecycle()
    val input by viewModel.goalInput.collectAsStateWithLifecycle()
    val isProtein = mode == "protein"

    ModalBottomSheet(onDismissRequest = { viewModel.closeGoalEditor() }) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                if (isProtein) "Protein Goal" else "Water Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            SmallLabel(
                if (isProtein) "Daily Protein Target (g)" else "Daily Water Target (glasses)",
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
            )
            OutlinedTextField(
                value = input,
                onValueChange = { viewModel.setGoalInput(it) },
                modifier = Modifier.fillMaxWidth().testTag("goalInput"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Text(
                if (isProtein) "A common target is 0.8–1g per lb of bodyweight."
                else "Recommended: 8 glasses (~2 litres) per day.",
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { viewModel.saveGoal() },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) {
                Text("SAVE", fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
        }
    }
}
