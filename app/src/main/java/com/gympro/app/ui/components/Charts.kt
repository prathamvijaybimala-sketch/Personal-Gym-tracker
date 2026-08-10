package com.gympro.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gympro.app.ui.theme.GymColors
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Hand-rolled chart components (line + bars) — same visual language as the
 * web app's Chart.js renders: gradient fills, accent line, emphasized last
 * point, monospace labels.
 */

@Composable
fun LineChart(
    labels: List<String>,
    data: List<Double>,
    modifier: Modifier = Modifier,
    ySuffix: String = "",
    heightDp: Int = 220,
) {
    val accent = MaterialTheme.colorScheme.primary
    val accentDim = MaterialTheme.colorScheme.secondary
    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outline

    if (data.isEmpty()) {
        Text(
            "No data yet — log something to see it here",
            modifier = modifier.padding(vertical = 60.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = textColor,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
    )
    val yMax = (data.maxOrNull() ?: 1.0) * 1.15
    val yMin = (data.minOrNull() ?: 0.0).coerceAtLeast(0.0)
    val span = max(yMax - yMin, 1.0)
    val n = data.size
    val xStride = ceil(n / 8.0).toInt().coerceAtLeast(1)

    Canvas(modifier = modifier.height(heightDp.dp).fillMaxWidth()) {
        val leftPad = 34.dp.toPx()
        val rightPad = 10.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 22.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        fun x(i: Int): Float = leftPad + if (n == 1) chartW / 2 else chartW * i / (n - 1)
        fun y(v: Double): Float = topPad + chartH * (1f - ((v - yMin) / span).toFloat())

        // Horizontal grid (4 lines) + y labels
        for (g in 0..3) {
            val gy = topPad + chartH * g / 3
            drawLine(gridColor.copy(alpha = 0.5f), Offset(leftPad, gy), Offset(size.width - rightPad, gy), strokeWidth = 1f)
            val value = yMax - span * g / 3
            val label = if (value >= 100) value.roundToInt().toString() else String.format("%.1f", value)
            drawText(
                textMeasurer.measure(label + ySuffix, labelStyle),
                topLeft = Offset(2.dp.toPx(), gy - 6.dp.toPx()),
            )
        }

        // X labels (max ~8)
        for (i in data.indices) {
            if (i % xStride != 0 && i != n - 1) continue
            val measured = textMeasurer.measure(labels[i], labelStyle)
            drawText(
                measured,
                topLeft = Offset(x(i) - measured.size.width / 2, size.height - measured.size.height),
            )
        }

        // Gradient fill under the line
        val linePath = Path()
        data.forEachIndexed { i, v ->
            if (i == 0) linePath.moveTo(x(i), y(v)) else linePath.lineTo(x(i), y(v))
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(x(n - 1), topPad + chartH)
            lineTo(x(0), topPad + chartH)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.27f), accent.copy(alpha = 0.02f)),
                startY = topPad,
                endY = topPad + chartH,
            ),
        )
        drawPath(linePath, color = accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        // Points: small for history, emphasized for the latest
        data.forEachIndexed { i, v ->
            val isLast = i == n - 1
            drawCircle(
                color = if (isLast) accent else accentDim,
                radius = if (isLast) 6.dp.toPx() else 3.dp.toPx(),
                center = Offset(x(i), y(v)),
            )
            if (isLast) {
                drawCircle(color = accent, radius = 9.dp.toPx(), center = Offset(x(i), y(v)), style = Stroke(2.dp.toPx()))
            }
        }
    }
}

@Composable
fun BarChart(
    labels: List<String>,
    data: List<Double>,
    barColors: List<Color>,
    modifier: Modifier = Modifier,
    ySuffix: String = "",
    heightDp: Int = 200,
    yMaxOverride: Double? = null,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outline

    if (data.isEmpty()) {
        Text(
            "No data yet",
            modifier = modifier.padding(vertical = 60.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = textColor,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
    )
    val maxData = data.maxOrNull() ?: 0.0
    val yMax = yMaxOverride ?: max(maxData * 1.1, 50.0)

    Canvas(modifier = modifier.height(heightDp.dp).fillMaxWidth()) {
        val leftPad = 30.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 10.dp.toPx()
        val bottomPad = 22.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad
        val slot = chartW / data.size
        val barW = slot * 0.55f

        for (g in 0..3) {
            val gy = topPad + chartH * g / 3
            drawLine(gridColor.copy(alpha = 0.5f), Offset(leftPad, gy), Offset(size.width - rightPad, gy), strokeWidth = 1f)
            val value = yMax * (1 - g / 3f)
            val label = if (value >= 100) value.roundToInt().toString() else String.format("%.0f", value)
            drawText(
                textMeasurer.measure(label + ySuffix, labelStyle),
                topLeft = Offset(2.dp.toPx(), gy - 6.dp.toPx()),
            )
        }

        data.forEachIndexed { i, v ->
            val barH = chartH * (v / yMax).toFloat().coerceIn(0f, 1f)
            val left = leftPad + slot * i + (slot - barW) / 2
            val top = topPad + chartH - barH
            drawRoundRect(
                color = barColors[i % barColors.size],
                topLeft = Offset(left, top),
                size = Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            )
            val measured = textMeasurer.measure(labels[i], labelStyle)
            drawText(
                measured,
                topLeft = Offset(left + barW / 2 - measured.size.width / 2, size.height - measured.size.height),
            )
        }
    }
}

/** Color mapping for the 7-day protein chart (green/amber/red by ratio). */
fun proteinBarColor(total: Double, goal: Int): Color {
    val ratio = if (goal <= 0) 0f else (total / goal).toFloat()
    return when {
        ratio >= 0.95f -> GymColors.Success
        ratio >= 0.77f -> GymColors.Partial
        else -> GymColors.Fail
    }
}
