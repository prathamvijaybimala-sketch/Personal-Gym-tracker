package com.gympro.app.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gympro.app.ui.components.SectionLabel
import com.gympro.app.ui.components.SmallLabel
import com.gympro.app.ui.home.HomeViewModel
import com.gympro.app.ui.theme.GymColors
import kotlin.math.roundToLong

/** Per-day history modal — port of the web's HISTORY MODAL (notes, workout, diet, mood). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryModal(
    data: HomeViewModel.HistoryData,
    onDismiss: () -> Unit,
    onNoteChange: (String) -> Unit,
) {
    var note by remember(data.dateKey) { mutableStateOf(data.note) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                data.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Column(modifier = Modifier.padding(top = 14.dp)) {
                SmallLabel("NOTE")
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        onNoteChange(it)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("dayNoteField"),
                    minLines = 2,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }

            SectionLabel("Workout")
            if (data.workoutRows.isEmpty()) {
                Text(
                    "Rest Day / No Log",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                data.workoutRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(row.exerciseName, fontSize = 13.sp)
                        Text(
                            "${trimNumber(row.weight)}kg × ${row.reps}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            SectionLabel("Diet (${data.proteinTotal.roundToLong()}g protein)")
            if (data.checkedFoods.isEmpty() && data.checkedSupps.isEmpty()) {
                Text(
                    "Nothing logged",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    data.checkedFoods.forEach { name ->
                        TagChipPlain(name)
                    }
                    data.checkedSupps.forEach { name ->
                        TagChipPlain(name, accent = true)
                    }
                }
            }

            data.mood?.let { mood ->
                Text(
                    mood,
                    modifier = Modifier.padding(top = 12.dp),
                    fontSize = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun TagChipPlain(text: String, accent: Boolean = false) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
            .background(
                if (accent) GymColors.Accent.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                1.dp,
                if (accent) GymColors.AccentDim else MaterialTheme.colorScheme.outline,
                androidx.compose.foundation.shape.RoundedCornerShape(5.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            fontSize = 11.sp,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun trimNumber(v: Double): String {
    val s = v.toString()
    return if (s.endsWith(".0")) s.dropLast(2) else s
}
