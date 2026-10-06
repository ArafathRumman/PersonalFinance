package com.taka.personalfinance.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Bar chart with one or more bars per group. Tap a group to select it.
 * groups[i][s] = value of series s in group i.
 */
@Composable
fun GroupedBarChart(
    groups: List<List<Long>>,
    colors: List<Color>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val selectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val maxValue = max(1L, groups.maxOfOrNull { g -> g.maxOrNull() ?: 0L } ?: 0L)
    val currentGroups = rememberUpdatedState(groups)
    val currentOnSelect = rememberUpdatedState(onSelect)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val n = currentGroups.value.size
                    if (n > 0) {
                        val idx = (offset.x / (size.width.toFloat() / n)).toInt().coerceIn(0, n - 1)
                        currentOnSelect.value(idx)
                    }
                }
            },
    ) {
        val n = groups.size
        if (n == 0) return@Canvas
        val groupWidth = size.width / n
        for (i in 0..3) {
            val y = size.height * i / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val seriesCount = max(1, groups.first().size)
        val gap = 2.dp.toPx()
        val barWidth = min(groupWidth * 0.7f / seriesCount, 28.dp.toPx())
        val totalWidth = barWidth * seriesCount + gap * (seriesCount - 1)
        groups.forEachIndexed { gi, g ->
            val startX = gi * groupWidth + (groupWidth - totalWidth) / 2f
            if (gi == selected) {
                drawRoundRect(
                    selectionColor,
                    topLeft = Offset(gi * groupWidth, 0f),
                    size = Size(groupWidth, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
            }
            g.forEachIndexed { si, v ->
                if (v > 0L) {
                    val h = max(size.height * (v.toFloat() / maxValue.toFloat()), 3.dp.toPx())
                    drawRoundRect(
                        colors[si % colors.size],
                        topLeft = Offset(startX + si * (barWidth + gap), size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(min(barWidth / 3f, 6.dp.toPx())),
                    )
                }
            }
        }
    }
}

/** Donut chart. Tap a slice to select it. [center] is drawn in the hole. */
@Composable
fun DonutChart(
    values: List<Long>,
    colors: List<Color>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit = {},
) {
    val total = values.sum()
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val currentValues = rememberUpdatedState(values)
    val currentOnSelect = rememberUpdatedState(onSelect)

    Box(modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { off ->
                        val vals = currentValues.value
                        val sum = vals.sum()
                        if (sum <= 0L) return@detectTapGestures
                        val dx = off.x - size.width / 2f
                        val dy = off.y - size.height / 2f
                        val dist = sqrt(dx * dx + dy * dy)
                        val outer = min(size.width, size.height) / 2f
                        if (dist < outer * 0.45f || dist > outer) return@detectTapGestures
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (angle < 0f) angle += 360f
                        var acc = 0f
                        for ((i, v) in vals.withIndex()) {
                            acc += 360f * v.toFloat() / sum.toFloat()
                            if (angle <= acc) {
                                currentOnSelect.value(i)
                                break
                            }
                        }
                    }
                },
        ) {
            val stroke = size.minDimension * 0.17f
            val radius = (size.minDimension - stroke * 1.2f) / 2f
            val topLeft = Offset((size.width - 2 * radius) / 2f, (size.height - 2 * radius) / 2f)
            val arcSize = Size(2 * radius, 2 * radius)
            if (total <= 0L) {
                drawArc(emptyColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = stroke))
            } else {
                var start = -90f
                val gapDeg = if (values.size > 1) 1.5f else 0f
                values.forEachIndexed { i, v ->
                    val sweep = 360f * v.toFloat() / total.toFloat()
                    val w = if (i == selected) stroke * 1.2f else stroke
                    drawArc(
                        colors[i % colors.size],
                        start + gapDeg / 2f,
                        max(sweep - gapDeg, 0.5f),
                        false,
                        topLeft,
                        arcSize,
                        style = Stroke(width = w, cap = StrokeCap.Butt),
                    )
                    start += sweep
                }
            }
        }
        center()
    }
}
