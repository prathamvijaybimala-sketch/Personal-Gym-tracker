package com.gympro.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gympro.app.ui.theme.GymColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.math.PI
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Toast host — top-center transient notifications (info / success / pr),
// styled like the web app's toasts.
// ---------------------------------------------------------------------------

enum class ToastType { INFO, SUCCESS, PR }

data class ToastEvent(val message: String, val type: ToastType = ToastType.INFO)

private data class ToastItem(val id: Int, val event: ToastEvent)

@Composable
fun ToastHost(events: Flow<ToastEvent>, modifier: Modifier = Modifier) {
    var counter by remember { mutableStateOf(0) }
    var toasts by remember { mutableStateOf(listOf<ToastItem>()) }

    LaunchedEffect(events) {
        events.collect { event ->
            counter += 1
            val id = counter
            toasts = toasts + ToastItem(id, event)
            delay(2200)
            toasts = toasts.filterNot { it.id == id }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        toasts.forEach { toast ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
            ) {
                ToastCard(toast.event)
            }
        }
    }
}

@Composable
private fun ToastCard(event: ToastEvent) {
    val borderColor = when (event.type) {
        ToastType.INFO -> MaterialTheme.colorScheme.primary
        ToastType.SUCCESS -> GymColors.Success
        ToastType.PR -> GymColors.Pr
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            event.message,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
        )
    }
}

// ---------------------------------------------------------------------------
// Confetti cannon — the PR celebration (60 particles, ~2s).
// ---------------------------------------------------------------------------

private data class Particle(
    val nx: Float,          // normalized start x 0..1
    val delay: Float,       // 0..1 fraction of the total duration
    val color: Color,
    val w: Float,           // px-ish sizes, scaled at draw time
    val h: Float,
    val drift: Float,       // horizontal drift, normalized
    val spin: Float,        // rotation in turns
    val round: Boolean,
)

@Composable
fun ConfettiOverlay(burstKey: Int, modifier: Modifier = Modifier) {
    if (burstKey == 0) return
    val particles = remember(burstKey) {
        val colors = listOf(
            GymColors.Pr, GymColors.Accent, GymColors.Success,
            GymColors.Partial, Color(0xFF7C3AED), Color(0xFFF0F0F0), GymColors.Water,
        )
        List(60) {
            Particle(
                nx = Random.nextFloat(),
                delay = Random.nextFloat() * 0.3f,
                color = colors[Random.nextInt(colors.size)],
                w = Random.nextFloat() * 8f + 4f,
                h = Random.nextFloat() * 5f + 2f,
                drift = (Random.nextFloat() - 0.5f) * 0.4f,
                spin = Random.nextFloat() * 720f,
                round = Random.nextBoolean(),
            )
        }
    }
    var progress by remember(burstKey) { mutableFloatStateOf(0f) }
    LaunchedEffect(burstKey) {
        val start = System.currentTimeMillis()
        while (progress < 1f) {
            progress = ((System.currentTimeMillis() - start) / 2200f).coerceIn(0f, 1f)
            delay(16)
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val local = ((progress - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = -20.dp.toPx() + local * (size.height * (0.6f + p.nx * 0.6f))
            val x = p.nx * size.width + p.drift * local * size.width
            val alpha = (1f - local).coerceIn(0f, 1f)
            val rotation = p.spin * local
            if (p.round) {
                drawCircle(p.color.copy(alpha = alpha), radius = p.w / 2 * (1f - local * 0.5f), center = Offset(x, y))
            } else {
                drawRect(
                    p.color.copy(alpha = alpha),
                    topLeft = Offset(x, y),
                    size = Size(p.w * 2.dp.toPx() * (1f - local * 0.5f), p.h * 2.dp.toPx()),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Rest timer overlay — floating pill with countdown ring (bottom-right).
// ---------------------------------------------------------------------------

@Composable
fun TimerOverlay(
    secondsLeft: Int,
    totalSeconds: Int,
    visible: Boolean,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        val fraction = if (totalSeconds <= 0) 0f else secondsLeft.toFloat() / totalSeconds
        val sweep by animateFloatAsState(targetValue = fraction * 360f, label = "timerSweep")
        Row(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(onClick = onStop)
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .semantics { contentDescription = "timer overlay" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                drawCircle(Color(0xFF2A2A2A), style = Stroke(width = 3.dp.toPx()))
                drawArc(
                    color = MaterialTheme.colorScheme.primary,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    size = Size(size.width, size.height),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = secondsLeft.toString(),
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.width(6.dp))
            Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}
