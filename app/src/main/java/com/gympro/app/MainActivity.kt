package com.gympro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Sunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gympro.app.ui.AppViewModel
import com.gympro.app.ui.components.ConfettiOverlay
import com.gympro.app.ui.components.TimerOverlay
import com.gympro.app.ui.components.ToastHost
import com.gympro.app.ui.components.ToastType
import com.gympro.app.ui.fuel.FuelScreen
import com.gympro.app.ui.fuel.FuelViewModel
import com.gympro.app.ui.home.HomeScreen
import com.gympro.app.ui.home.HomeViewModel
import com.gympro.app.ui.modals.BfModal
import com.gympro.app.ui.theme.GymColors
import com.gympro.app.ui.theme.GymProTheme
import com.gympro.app.ui.train.TrainScreen
import com.gympro.app.ui.train.TrainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
            val theme by appViewModel.theme.collectAsStateWithLifecycle()

            GymProTheme(darkTheme = theme != "light") {
                MainScreen(appViewModel)
            }
        }
    }
}

@Composable
private fun MainScreen(appViewModel: AppViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val trainViewModel: TrainViewModel = viewModel(factory = TrainViewModel.Factory)
    val fuelViewModel: FuelViewModel = viewModel(factory = FuelViewModel.Factory)

    val timer by appViewModel.timer.collectAsStateWithLifecycle()
    val confettiKey by appViewModel.confettiKey.collectAsStateWithLifecycle()
    val bfModal by appViewModel.bfModal.collectAsStateWithLifecycle()

    val onToast: (String, ToastType) -> Unit = { message, type ->
        appViewModel.showToast(message, type)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { GymHeader(appViewModel, onToast) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home", fontWeight = FontWeight.Bold) },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = "Train") },
                    label = { Text("Train", fontWeight = FontWeight.Bold) },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.RestaurantMenu, contentDescription = "Fuel") },
                    label = { Text("Fuel", fontWeight = FontWeight.Bold) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            when (selectedTab) {
                0 -> HomeScreen(homeViewModel, onToast = onToast, onOpenBfModal = { appViewModel.openBfModal() })
                1 -> TrainScreen(trainViewModel, appViewModel, onToast = onToast)
                2 -> FuelScreen(fuelViewModel, onToast = onToast)
            }

            // Root overlays
            ToastHost(
                events = appViewModel.toastEvents,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 90.dp),
            ) {
                TimerOverlay(
                    secondsLeft = timer.secondsLeft,
                    totalSeconds = timer.totalSeconds,
                    visible = timer.running,
                    onStop = { appViewModel.stopTimer() },
                )
            }
            ConfettiOverlay(burstKey = confettiKey)
        }
    }

    if (bfModal.open) {
        BfModal(
            state = bfModal,
            onDismiss = { appViewModel.closeBfModal() },
            onHeightChange = { appViewModel.setBfHeight(it) },
            onNeckChange = { appViewModel.setBfNeck(it) },
            onWaistChange = { appViewModel.setBfWaist(it) },
            onCalculate = { appViewModel.calcAndSaveBf() },
        )
    }
}

// ---------------------------------------------------------------------------
// Header — logo, streak, weight/BF stats, theme toggle (port of the web header)
// ---------------------------------------------------------------------------

@Composable
private fun GymHeader(appViewModel: AppViewModel, onToast: (String, ToastType) -> Unit) {
    val stats by appViewModel.headerStats.collectAsStateWithLifecycle()
    val theme by appViewModel.theme.collectAsStateWithLifecycle()
    val isDark = theme != "light"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "GYM",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            letterSpacing = 3.sp,
        )
        Text(
            " PRO",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.weight(1f))

        // Streak pill
        if (stats.streak > 0) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GymColors.Partial.copy(alpha = 0.15f))
                    .border(1.dp, GymColors.Partial.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .semantics { contentDescription = "streak ${stats.streak}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔥", fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    stats.streak.toString(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = GymColors.Partial,
                )
            }
        }

        // Weight + BF stats pill (opens BF modal)
        Row(
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable { appViewModel.openBfModal() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .semantics { contentDescription = "header stats" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${stats.weight}kg",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "${stats.bf} BF",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Theme toggle
        IconButton(
            onClick = { appViewModel.toggleTheme() },
            modifier = Modifier
                .padding(start = 4.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .semantics { contentDescription = "theme toggle" },
        ) {
            Icon(
                if (isDark) Icons.Filled.Sunny else Icons.Filled.DarkMode,
                contentDescription = "Toggle theme",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
