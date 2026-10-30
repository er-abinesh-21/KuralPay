package com.kuralpay.app.summary.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuralpay.app.domain.model.SummarySlot
import com.kuralpay.app.ui.components.Eyebrow
import com.kuralpay.app.ui.components.KpSwitch
import com.kuralpay.app.ui.components.SettingRow
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.Kp

private val SummarySlot.title: String
    get() = when (this) {
        SummarySlot.MORNING -> "Morning"
        SummarySlot.AFTERNOON -> "Afternoon"
        SummarySlot.EVENING -> "Evening"
        SummarySlot.CLOSING -> "Closing time"
    }

private val SummarySlot.detail: String
    get() = if (this == SummarySlot.CLOSING) "Full day total" else "Collection so far"

private fun clock(minutes: Int): Pair<String, String> {
    val h24 = minutes / 60
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    return "$h12:${(minutes % 60).toString().padStart(2, '0')}" to if (h24 < 12) "AM" else "PM"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SummarySettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SummarySettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val preview by viewModel.previewText.collectAsState()
    val c = Kp.colors
    var editing by remember { mutableStateOf<SummarySlot?>(null) }
    val ordered = SummarySlot.entries.sortedBy { state.minutes[it] ?: it.defaultMinutes }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(c.ink).statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.entrance(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).kpClickable(onBack).kpCard(13.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.text, modifier = Modifier.size(20.dp))
                }
                Text("Daily summaries", style = MaterialTheme.typography.headlineMedium, color = c.text)
            }
        }
        item {
            Text(
                "KuralPay reads your collection aloud at these times every day, even when the phone is locked. Tap a time to change it.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.muted,
                modifier = Modifier.entrance(1)
            )
        }
        items(ordered, key = { it.name }) { slot ->
            TimelineSlot(
                slot = slot,
                minutes = state.minutes[slot] ?: slot.defaultMinutes,
                on = state.enabled[slot] ?: true,
                isFirst = slot == ordered.first(),
                isLast = slot == ordered.last(),
                onToggle = { viewModel.toggle(slot) },
                onEditTime = { editing = slot },
                modifier = Modifier.animateItemPlacement()
            )
        }
        item {
            Column(Modifier.entrance(6).kpCard().padding(horizontal = 16.dp)) {
                SettingRow(null, "Add details", "Top payment app and its share of payments") {
                    KpSwitch(state.summaryDetails, { viewModel.toggleDetails() })
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().entrance(7).kpCard(20.dp, fill = c.accent.copy(alpha = 0.09f)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Eyebrow("You will hear", c.accent)
                AnimatedContent(preview, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "preview") { text ->
                    Text(
                        if (text.isBlank()) "Your collection total and payment count for the day." else "“$text”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.text
                    )
                }
            }
        }
    }

    editing?.let { slot ->
        SummaryTimeDialog(
            slot = slot,
            minutes = state.minutes[slot] ?: slot.defaultMinutes,
            onDismiss = { editing = null },
            onConfirm = { m ->
                viewModel.setTime(slot, m)
                editing = null
            }
        )
    }
}

@Composable
private fun TimelineSlot(
    slot: SummarySlot,
    minutes: Int,
    on: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit,
    onEditTime: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Kp.colors
    val dot by animateColorAsState(if (on) c.accent else c.muted, label = "dot")
    val (time, period) = clock(minutes)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(26.dp).height(76.dp)) {
            val x = 9.dp.toPx()
            val lineW = 2.dp.toPx()
            if (!isFirst) drawLine(c.line, Offset(x, 0f), Offset(x, size.height / 2), lineW)
            if (!isLast) drawLine(c.line, Offset(x, size.height / 2), Offset(x, size.height), lineW)
            val r = 6.dp.toPx()
            val center = Offset(x, size.height / 2)
            if (on) drawCircle(dot, r, center) else {
                drawCircle(c.ink, r, center)
                drawCircle(dot, r, center, style = Stroke(2.dp.toPx()))
            }
        }
        Row(
            Modifier.weight(1f).kpCard(20.dp).padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier
                    .kpClickable(onEditTime)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.deck2)
                    .border(1.dp, c.line, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(time, style = MaterialTheme.typography.titleLarge, color = c.text)
                Text(period, style = MaterialTheme.typography.labelSmall, color = c.muted)
                Icon(Icons.Rounded.Edit, contentDescription = "Change ${slot.title} time", tint = c.accent, modifier = Modifier.size(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(slot.title, style = MaterialTheme.typography.bodyMedium, color = c.text, maxLines = 1)
                Text(slot.detail, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 1)
            }
            KpSwitch(on, { onToggle() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryTimeDialog(
    slot: SummarySlot,
    minutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val c = Kp.colors
    val picker = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.deck,
        title = { Text("${slot.title} summary", style = MaterialTheme.typography.titleLarge, color = c.text) },
        text = {
            TimePicker(
                state = picker,
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.deck2,
                    clockDialSelectedContentColor = c.onLime,
                    clockDialUnselectedContentColor = c.text,
                    selectorColor = c.lime,
                    containerColor = c.deck,
                    periodSelectorBorderColor = c.line,
                    periodSelectorSelectedContainerColor = c.lime,
                    periodSelectorUnselectedContainerColor = c.deck,
                    periodSelectorSelectedContentColor = c.onLime,
                    periodSelectorUnselectedContentColor = c.muted,
                    timeSelectorSelectedContainerColor = c.lime,
                    timeSelectorUnselectedContainerColor = c.deck2,
                    timeSelectorSelectedContentColor = c.onLime,
                    timeSelectorUnselectedContentColor = c.text
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picker.hour * 60 + picker.minute) }) {
                Text("Save", style = MaterialTheme.typography.labelLarge, color = c.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, color = c.muted)
            }
        }
    )
}
