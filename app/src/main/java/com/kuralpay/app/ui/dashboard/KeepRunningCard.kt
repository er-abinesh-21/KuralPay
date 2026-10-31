package com.kuralpay.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kuralpay.app.paymentdetection.BackgroundHealth
import com.kuralpay.app.ui.components.IconTile
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.Kp

/** True while the phone has recently been stopping KuralPay in the background; re-checked whenever Home is shown. */
@Composable
fun rememberKeepRunningNeeded(): MutableState<Boolean> {
    val context = LocalContext.current
    val needed = remember { mutableStateOf(BackgroundHealth.stoppedBySystemRecently(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) needed.value = BackgroundHealth.stoppedBySystemRecently(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return needed
}

/** Shown when the phone's power manager stopped KuralPay: payments go unheard until it is opened again. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeepRunningCard(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val c = Kp.colors
    val context = LocalContext.current
    val exempt = remember { BackgroundHealth.ignoringBatteryOptimizations(context) }
    Column(
        modifier.fillMaxWidth().kpCard(22.dp, fill = c.warn.copy(alpha = 0.10f)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(Icons.Rounded.BatteryAlert, c.warn)
            Column(Modifier.weight(1f)) {
                Text("This phone keeps stopping KuralPay", style = MaterialTheme.typography.titleSmall, color = c.text)
                Text(
                    "Its battery saver closed KuralPay in the background, so payments weren't announced or forwarded until it was opened.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted
                )
            }
        }
        BackgroundHealth.makerSteps().forEachIndexed { i, step ->
            Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium, color = c.text)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!exempt) {
                Action("Allow always running", filled = true) {
                    runCatching { context.startActivity(BackgroundHealth.batteryExemptionIntent(context)) }
                }
            }
            Action("App settings", filled = exempt) {
                runCatching { context.startActivity(BackgroundHealth.appSettingsIntent(context)) }
            }
            Action("Done", filled = false) {
                BackgroundHealth.acknowledge(context)
                onDone()
            }
        }
    }
}

@Composable
private fun Action(label: String, filled: Boolean, onClick: () -> Unit) {
    val c = Kp.colors
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (filled) c.onRoyal else c.text,
        modifier = Modifier
            .kpClickable(onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) c.royal else c.deck2)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    )
}
