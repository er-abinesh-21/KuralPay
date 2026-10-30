package com.kuralpay.app.lockscreenoverlay.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kuralpay.app.domain.model.ParsedPayment
import com.kuralpay.app.ui.components.AnimatedAmount
import com.kuralpay.app.ui.components.Eyebrow
import com.kuralpay.app.ui.components.PulseRings
import com.kuralpay.app.ui.components.SourceBadge
import com.kuralpay.app.ui.components.VoiceWave
import com.kuralpay.app.ui.components.formatTime
import com.kuralpay.app.ui.components.payerName
import com.kuralpay.app.ui.theme.Kp
import com.kuralpay.app.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun PaymentOverlayContent(
    payment: ParsedPayment,
    onDismiss: () -> Unit = {}
) {
    val c = Kp.colors
    val pop = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)) }
        launch { tick.animateTo(1f, tween(420, delayMillis = 180)) }
    }
    val shape = RoundedCornerShape(30.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(horizontal = 24.dp, vertical = 40.dp)
            .graphicsLayer {
                alpha = pop.value.coerceIn(0f, 1f)
                translationY = (1f - pop.value) * 120f
                scaleX = 0.9f + 0.1f * pop.value
                scaleY = 0.9f + 0.1f * pop.value
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .padding(top = 32.dp)
                .fillMaxWidth()
                .shadow(28.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(c.deck)
                .border(1.dp, c.line, shape)
                .padding(start = 20.dp, end = 20.dp, top = 44.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Eyebrow(if (payment.forwardedFrom != null) "Payment forwarded" else "Payment received", if (payment.forwardedFrom != null) c.accent else c.good)
            AnimatedAmount(
                amount = payment.amount,
                style = MaterialTheme.typography.displayLarge,
                color = c.text,
                durationMillis = 700
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceWave(color = c.accent, bars = 5, barWidth = 3.dp, height = 16.dp)
                Text("Speaking now", style = MaterialTheme.typography.labelMedium, color = c.accent)
            }
            Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(1.dp).background(c.line))
            Row(
                Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (payment.forwardedFrom != null) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Forwarded payment", tint = c.accent, modifier = Modifier.size(18.dp))
                    }
                } else {
                    SourceBadge(payment.source, 34.dp)
                }
                Column {
                    Text(
                        payerName(payment.sender) ?: "Payment received",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = c.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${payment.forwardedFrom?.let { "Forwarded by $it" } ?: payment.source.displayName} · ${formatTime(System.currentTimeMillis())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted
                    )
                }
            }
        }

        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            PulseRings(SuccessGreen, Modifier.size(64.dp), periodMillis = 2000)
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer { scaleX = tick.value; scaleY = tick.value }
                    .clip(CircleShape)
                    .background(SuccessGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
    }
}
