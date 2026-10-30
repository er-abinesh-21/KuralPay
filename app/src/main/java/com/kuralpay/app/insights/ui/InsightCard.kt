package com.kuralpay.app.insights.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kuralpay.app.domain.model.Insight
import com.kuralpay.app.domain.model.InsightType
import com.kuralpay.app.ui.components.Eyebrow
import com.kuralpay.app.ui.components.IconTile
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.theme.Kp

private fun InsightType.label(): String = when (this) {
    InsightType.DAILY_TREND -> "Daily trend"
    InsightType.WEEKLY_TREND -> "This week"
    InsightType.GROWTH_INDICATOR -> "Growth"
    InsightType.PEAK_HOUR -> "Peak hour"
    InsightType.SOURCE_PERFORMANCE -> "Top app"
}

private fun InsightType.icon(): ImageVector = when (this) {
    InsightType.DAILY_TREND -> Icons.AutoMirrored.Rounded.ShowChart
    InsightType.WEEKLY_TREND -> Icons.Rounded.CalendarMonth
    InsightType.GROWTH_INDICATOR -> Icons.AutoMirrored.Rounded.TrendingUp
    InsightType.PEAK_HOUR -> Icons.Rounded.AccessTime
    InsightType.SOURCE_PERFORMANCE -> Icons.Rounded.CreditCard
}

@Composable
fun InsightStrip(insights: List<Insight>, modifier: Modifier = Modifier) {
    if (insights.isEmpty()) return
    val c = Kp.colors
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(insights) { i, insight ->
            Column(
                Modifier
                    .fillParentMaxWidth(if (insights.size == 1) 1f else 0.8f)
                    .entrance(i)
                    .kpCard(20.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconTile(insight.type.icon(), c.accent, size = 30.dp)
                    Eyebrow(insight.type.label(), c.accent)
                }
                Text(insight.text, style = MaterialTheme.typography.bodyMedium, color = c.text)
            }
        }
    }
}
