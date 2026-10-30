package com.kuralpay.app.analytics.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuralpay.app.ui.components.AnimatedAmount
import com.kuralpay.app.ui.components.BottomBarSpace
import com.kuralpay.app.ui.components.Eyebrow
import com.kuralpay.app.ui.components.SectionHeader
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.formatInr
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.Kp

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val hourly by viewModel.hourly.collectAsState()
    val day by viewModel.dayOffset.collectAsState()
    val c = Kp.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(c.ink).statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Analytics", style = MaterialTheme.typography.headlineMedium, color = c.text, modifier = Modifier.entrance(0))
        }
        item {
            Row(Modifier.entrance(1), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayChip("Today", day == 0) { viewModel.selectDay(0) }
                DayChip("Yesterday", day == -1) { viewModel.selectDay(-1) }
            }
        }
        item {
            Column(Modifier.entrance(2)) {
                Eyebrow("Collected")
                AnimatedAmount(stats.total, MaterialTheme.typography.displayMedium, c.text)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().entrance(3), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${stats.count}", "Payments", Modifier.weight(1f))
                StatTile("₹${formatInr(Math.round(stats.average).toDouble())}", "Average", Modifier.weight(1f))
                StatTile("₹${formatInr(stats.maxAmount)}", "Largest", Modifier.weight(1f))
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().entrance(4).kpCard(24.dp).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SectionHeader("By payment app")
                if (stats.sourceDistribution.isEmpty()) {
                    Text(
                        "No payments for this day yet. The split by app appears after the first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.muted
                    )
                } else {
                    SourceDonut(stats.sourceDistribution, Modifier.align(Alignment.CenterHorizontally))
                    SourceLegend(stats.sourceDistribution)
                }
            }
        }
        item { HourlyCard(hourly, Modifier.entrance(5)) }
    }
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Kp.colors
    val bg by animateColorAsState(if (selected) c.royal else c.deck, tween(250), label = "chipBg")
    val fg by animateColorAsState(if (selected) c.onRoyal else c.muted, tween(250), label = "chipFg")
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        modifier = Modifier
            .kpClickable(onClick)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) c.royal else c.line, CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    val c = Kp.colors
    Column(modifier.kpCard(18.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

@Composable
private fun HourlyCard(hourly: List<Int>, modifier: Modifier = Modifier) {
    val c = Kp.colors
    val max = (hourly.maxOrNull() ?: 0).coerceAtLeast(1)
    val peakIndex = hourly.indexOf(hourly.maxOrNull() ?: 0).takeIf { (hourly.maxOrNull() ?: 0) > 0 }
    val grow = remember { Animatable(0f) }
    LaunchedEffect(hourly) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    Column(
        modifier.fillMaxWidth().kpCard(24.dp).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Payments by hour", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
            if (peakIndex != null) {
                Text("peak ${hourLabel(ChartHours.first + peakIndex)}", style = MaterialTheme.typography.labelMedium, color = c.accent)
            }
        }
        Row(
            Modifier.fillMaxWidth().height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            hourly.forEachIndexed { i, count ->
                val f = if (count == 0) 0.04f else count / max.toFloat()
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(f)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            scaleY = grow.value
                        }
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(if (i == peakIndex) c.lime else c.muted.copy(alpha = if (count == 0) 0.15f else 0.45f))
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            hourly.indices.forEach { i ->
                Text(
                    if (i % 2 == 0) hourLabel(ChartHours.first + i, short = true) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun hourLabel(hour24: Int, short: Boolean = false): String {
    val h = if (hour24 % 12 == 0) 12 else hour24 % 12
    return if (short) "$h" else "$h ${if (hour24 < 12) "AM" else "PM"}"
}
