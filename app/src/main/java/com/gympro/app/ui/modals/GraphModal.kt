package com.gympro.app.ui.modals

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gympro.app.ui.components.LineChart
import com.gympro.app.ui.components.MonoText
import com.gympro.app.ui.home.HomeViewModel
import com.gympro.app.ui.theme.GymColors

/** Shared graph payload (used by Home for weight/BF and Train for exercises). */
data class GraphData(
    val title: String,
    val labels: List<String>,
    val data: List<Double>,
    val latest: String,
    val peak: String,
    val change: String,
    val changeUp: Boolean,
)

/** Progress graph modal — port of the web's GRAPH MODAL. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphModal(data: GraphData, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                data.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )

            // Graph stats: Latest / Peak / Change
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GraphStat(label = "Latest", value = data.latest)
                GraphStat(label = "Peak", value = data.peak)
                GraphStat(
                    label = "Change",
                    value = data.change,
                    valueColor = if (data.changeUp) GymColors.Fail else GymColors.Success,
                )
            }

            LineChart(labels = data.labels, data = data.data)

            Text(
                "Consistency is the only magic pill.",
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GraphStat(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MonoText(text = value, color = valueColor, size = 15)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
