package com.kuralpay.app.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuralpay.app.R
import com.kuralpay.app.domain.model.Transaction
import com.kuralpay.app.insights.ui.InsightStrip
import com.kuralpay.app.ui.components.AnimatedAmount
import com.kuralpay.app.ui.components.BottomBarSpace
import com.kuralpay.app.ui.components.Eyebrow
import com.kuralpay.app.ui.components.IconTile
import com.kuralpay.app.ui.components.LivePill
import com.kuralpay.app.ui.components.SectionHeader
import com.kuralpay.app.ui.components.SourceBadge
import com.kuralpay.app.ui.components.VoiceWave
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.formatInr
import com.kuralpay.app.ui.components.formatTime
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.components.payerName
import com.kuralpay.app.ui.components.relativeTime
import com.kuralpay.app.ui.theme.BrandOrange
import com.kuralpay.app.ui.theme.Kp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToSummary: () -> Unit = {},
    onNavigateToForwarding: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val c = Kp.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(c.ink).statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { TopGreeting(onNavigateToSummary, Modifier.entrance(0)) }
        item { LivePill("Listening for payments", c.good, Modifier.entrance(1)) }
        item {
            CollectionHero(
                state = state,
                onReplay = viewModel::replayLast,
                modifier = Modifier.entrance(2)
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().entrance(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction(
                    Icons.Rounded.Replay,
                    when {
                        state.missedCount > 0 -> "Replay ${state.missedCount} missed"
                        state.replayCount == 1 -> "Replay last"
                        else -> "Replay last ${state.replayCount}"
                    },
                    Modifier.weight(1f),
                    viewModel::replayTile
                )
                QuickAction(Icons.Rounded.Mic, "Ask KuralPay", Modifier.weight(1f), onNavigateToAssistant)
                QuickAction(Icons.Rounded.Schedule, "Summaries", Modifier.weight(1f), onNavigateToSummary)
                QuickAction(Icons.AutoMirrored.Rounded.Send, "Forward", Modifier.weight(1f), onNavigateToForwarding)
            }
        }
        if (state.insights.isNotEmpty()) {
            item { SectionHeader("Insights", Modifier.entrance(4)) }
            item { InsightStrip(state.insights, Modifier.entrance(5)) }
        }
        item {
            SectionHeader(
                "Recent payments",
                Modifier.entrance(6),
                action = if (state.recentTransactions.isNotEmpty()) "See all" else null,
                onAction = onNavigateToAnalytics
            )
        }
        if (state.recentTransactions.isEmpty()) {
            item { EmptyPayments(Modifier.entrance(7)) }
        } else {
            item {
                Column(Modifier.kpCard(22.dp).padding(6.dp).entrance(7)) {
                    val todayStart = remember { startOfToday() }
                    state.recentTransactions.forEachIndexed { i, tx ->
                        TransactionRow(
                            tx,
                            Modifier.entrance(8 + i, offset = 10.dp),
                            onReplay = if (tx.timestamp >= todayStart) ({ viewModel.replayPayment(tx) }) else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopGreeting(onSummary: () -> Unit, modifier: Modifier = Modifier) {
    val c = Kp.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.kp_logo_mark), contentDescription = "KuralPay", modifier = Modifier.size(34.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Vanakkam", style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(
                SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).format(Date()),
                style = MaterialTheme.typography.bodySmall,
                color = c.muted
            )
        }
        Box(
            Modifier.size(42.dp).kpClickable(onSummary).kpCard(13.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Schedule, contentDescription = "Daily summaries", tint = c.text, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CollectionHero(state: DashboardState, onReplay: () -> Unit, modifier: Modifier = Modifier) {
    val c = Kp.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(BrandOrange)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.75f
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width, 0f)
                )
            }
            .padding(20.dp)
    ) {
        VoiceWave(
            color = Color.White,
            modifier = Modifier.align(Alignment.TopEnd),
            bars = 6,
            height = 40.dp
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Eyebrow("Today's collection", Color.White.copy(alpha = 0.72f))
            AnimatedAmount(
                amount = state.todayTotal,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                HeroStat("${state.todayCount}", if (state.todayCount == 1) "payment" else "payments")
                HeroStat("₹${formatInr(state.average.roundTo0())}", "avg")
                HeroStat("₹${formatInr(state.maxAmount)}", "best")
            }
            AnimatedContent(
                targetState = state.lastTransaction,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith fadeOut()
                },
                contentKey = { it?.id },
                label = "lastPayment"
            ) { last ->
                LastPaymentStrip(last, onReplay)
            }
        }
    }
}

private fun Double.roundTo0(): Double = Math.round(this).toDouble()

@Composable
private fun HeroStat(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
    }
}

@Composable
private fun LastPaymentStrip(last: Transaction?, onReplay: () -> Unit) {
    val c = Kp.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (last == null) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Text(
                "Waiting for today's first payment. Keep KuralPay running and it will speak each one.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        } else {
            SourceBadge(last.sourceApp, 40.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    payerName(last.sender)?.let { "₹${formatInr(last.amount)} from $it" } ?: "₹${formatInr(last.amount)} received",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${last.sourceApp.displayName} · ${relativeTime(last.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Box(
                Modifier.size(42.dp).kpClickable(onReplay).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = "Replay recent payments", tint = BrandOrange, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    val c = Kp.colors
    Column(
        modifier
            .kpClickable(onClick)
            .kpCard(18.dp)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconTile(icon, c.royal)
        FittedLabel(label, MaterialTheme.typography.labelMedium, c.text)
    }
}

/**
 * Centered label of up to two lines that shrinks its text (down to 9sp) until every word fits on a line,
 * so narrow screens or large display sizes never split a word like "Summarie-s".
 */
@Composable
private fun FittedLabel(text: String, style: TextStyle, color: Color) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val measurer = rememberTextMeasurer()
        val widthPx = constraints.maxWidth
        val fitted = remember(text, style, widthPx) {
            val words = text.split(' ')
            var s = style
            fun tooWide(s: TextStyle) = words.any { measurer.measure(it, s, softWrap = false, maxLines = 1).size.width > widthPx }
            while (s.fontSize.value > 9f && tooWide(s)) {
                s = s.copy(
                    fontSize = s.fontSize * 0.92f,
                    lineHeight = if (s.lineHeight.isSpecified) s.lineHeight * 0.92f else s.lineHeight
                )
            }
            s
        }
        Text(text, style = fitted, color = color, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun startOfToday(): Long = java.util.Calendar.getInstance().apply {
    set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun TransactionRow(tx: Transaction, modifier: Modifier = Modifier, onReplay: (() -> Unit)? = null) {
    val c = Kp.colors
    Row(
        modifier.fillMaxWidth().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SourceBadge(tx.sourceApp)
        Column(Modifier.weight(1f)) {
            Text(
                payerName(tx.sender) ?: "Payment received",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${tx.sourceApp.displayName} · ${formatTime(tx.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = c.muted
            )
        }
        Text(
            "+₹${formatInr(tx.amount)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = c.good
        )
        if (onReplay != null) {
            Box(
                Modifier.size(36.dp).kpClickable(onReplay).clip(CircleShape).background(c.deck2),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Replay, contentDescription = "Replay this payment", tint = c.accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyPayments(modifier: Modifier = Modifier) {
    val c = Kp.colors
    Column(
        modifier.fillMaxWidth().kpCard(22.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VoiceWave(color = c.royal.copy(alpha = 0.6f), bars = 5, height = 28.dp, barWidth = 3.dp)
        Text("No payments yet today", style = MaterialTheme.typography.titleSmall, color = c.text)
        Text(
            "Payments from Google Pay, PhonePe, Paytm and BHIM appear here as they arrive.",
            style = MaterialTheme.typography.bodySmall,
            color = c.muted
        )
    }
}
