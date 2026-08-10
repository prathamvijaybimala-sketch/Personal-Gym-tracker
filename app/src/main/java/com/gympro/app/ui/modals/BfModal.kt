package com.gympro.app.ui.modals

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gympro.app.ui.AppViewModel
import com.gympro.app.ui.components.SmallLabel
import com.gympro.app.ui.theme.GymColors

/** US Navy body-fat calculator modal — port of the web's BF MODAL. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BfModal(
    state: AppViewModel.BfModalState,
    onDismiss: () -> Unit,
    onHeightChange: (String) -> Unit,
    onNeckChange: (String) -> Unit,
    onWaistChange: (String) -> Unit,
    onCalculate: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                "Body Fat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(
                "US Navy Method",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    SmallLabel("Height (cm) — Saved")
                    OutlinedTextField(
                        value = state.height,
                        onValueChange = onHeightChange,
                        modifier = Modifier.fillMaxWidth().testTag("bfHeight"),
                        placeholder = { Text("175", fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                Column {
                    SmallLabel("Neck (cm)")
                    OutlinedTextField(
                        value = state.neck,
                        onValueChange = onNeckChange,
                        modifier = Modifier.fillMaxWidth().testTag("bfNeck"),
                        placeholder = { Text("38", fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                Column {
                    SmallLabel("Waist (cm) — At Navel")
                    OutlinedTextField(
                        value = state.waist,
                        onValueChange = onWaistChange,
                        modifier = Modifier.fillMaxWidth().testTag("bfWaist"),
                        placeholder = { Text("85", fontSize = 14.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                Button(
                    onClick = onCalculate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("CALCULATE & LOG", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                }
                state.result?.let {
                    Text(
                        it,
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}
