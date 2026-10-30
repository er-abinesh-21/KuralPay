package com.kuralpay.app.analytics.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kuralpay.app.domain.model.PaymentSource
import com.kuralpay.app.ui.theme.Kp
import com.kuralpay.app.ui.theme.brandColor

/** Donut whose arcs sweep in one after another; the centre names the total count. */
@Composable
fun SourceDonut(
    distribution: List<Pair<PaymentSource, Int>>,
    modifier: Modifier = Modifier
) {
    val c = Kp.colors
    val total = distribution.sumOf { it.second }.coerceAtLeast(1).toFloat()
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(distribution) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
    }
    Box(modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(190.dp)) {
            val stroke = 18.dp.toPx()
            val r = size.minDimension / 2 - stroke / 2
            val topLeft = Offset(center.x - r, center.y - r)
            val arcSize = Size(r * 2, r * 2)
            drawArc(c.deck2, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val gap = if (distribution.size > 1) 1.5f else 0f
            var start = -90f
            val visible = 360f * sweep.value
            distribution.forEach { (source, count) ->
                val full = count / total * 360f
                val drawn = (visible - (start + 90f)).coerceIn(0f, full)
                if (drawn > gap) {
                    drawArc(source.brandColor(), start, drawn - gap, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                }
                start += full
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${distribution.sumOf { it.second }}",
                style = MaterialTheme.typography.displaySmall,
                color = c.text
            )
            Text("payments", style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

@Composable
fun SourceLegend(distribution: List<Pair<PaymentSource, Int>>, modifier: Modifier = Modifier) {
    val c = Kp.colors
    val total = distribution.sumOf { it.second }.coerceAtLeast(1).toFloat()
    val grow = remember { Animatable(0f) }
    LaunchedEffect(distribution) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(900, delayMillis = 200, easing = FastOutSlowInEasing))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        distribution.sortedByDescending { it.second }.forEach { (source, count) ->
            val share = count / total
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(source.brandColor()))
                    Text(source.displayName, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                    Text(
                        "$count · ${(share * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.muted
                    )
                }
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.deck2)) {
                    Box(
                        Modifier
                            .fillMaxWidth(share * grow.value)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(source.brandColor())
                    )
                }
            }
        }
    }
}
