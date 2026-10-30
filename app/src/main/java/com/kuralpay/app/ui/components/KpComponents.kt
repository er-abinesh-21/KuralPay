package com.kuralpay.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuralpay.app.domain.model.PaymentSource
import com.kuralpay.app.ui.theme.EyebrowStyle
import com.kuralpay.app.ui.theme.Kp
import com.kuralpay.app.ui.theme.Lexend
import com.kuralpay.app.ui.theme.brandColor
import com.kuralpay.app.ui.theme.monogram
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val IndianLocale = Locale("en", "IN")

/** Room left under scrolling content for the floating bottom bar plus the system navigation bar. */
val BottomBarSpace: Dp
    @Composable get() = 104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Indian digit grouping: 124580 -> 1,24,580. */
fun formatInr(amount: Double): String {
    val nf = NumberFormat.getNumberInstance(IndianLocale)
    nf.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    nf.minimumFractionDigits = 0
    return nf.format(amount)
}

/** The payer's name, or null when the alert didn't say (blank, or the old "Unknown" placeholder). */
fun payerName(sender: String): String? =
    sender.trim().takeUnless { it.isEmpty() || it.equals("Unknown", ignoreCase = true) }

fun formatTime(timestamp: Long): String =
    SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(timestamp))

fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val mins = ((now - timestamp) / 60_000).coerceAtLeast(0)
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "$mins min ago"
        mins < 24 * 60 -> "${mins / 60} h ago"
        else -> formatTime(timestamp)
    }
}

fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

fun Modifier.kpClickable(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this
        .pressScale(source)
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** Rises and fades in once; the resting state is fully visible. */
fun Modifier.entrance(index: Int = 0, offset: Dp = 16.dp): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 55L)
        progress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { offset.toPx() }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * px
    }
}

fun Modifier.kpCard(radius: Dp = 22.dp, fill: Color? = null): Modifier = composed {
    val c = Kp.colors
    val shape = RoundedCornerShape(radius)
    this
        .clip(shape)
        .background(fill ?: c.deck)
        .border(1.dp, c.line, shape)
}

@Composable
fun AnimatedAmount(
    amount: Double,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    prefix: String = "₹",
    durationMillis: Int = 1300
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(amount) {
        anim.animateTo(amount.toFloat(), tween(durationMillis, easing = FastOutSlowInEasing))
    }
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = prefix,
            style = style.copy(fontWeight = FontWeight.Normal),
            color = color.copy(alpha = 0.75f)
        )
        Text(
            text = formatInr(Math.round(anim.value).toDouble()),
            style = style,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
fun VoiceWave(
    color: Color,
    modifier: Modifier = Modifier,
    bars: Int = 6,
    barWidth: Dp = 4.dp,
    height: Dp = 40.dp,
    active: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val shapes = listOf(0.3f, 0.7f, 1f, 0.55f, 0.8f, 0.4f, 0.65f, 0.9f)
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(barWidth),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(bars) { i ->
            val phase by transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(520 + i * 70, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$i"
            )
            val f = shapes[i % shapes.size] * (if (active) phase else 1f)
            Box(
                Modifier
                    .width(barWidth)
                    .fillMaxHeight(f)
                    .clip(RoundedCornerShape(barWidth / 2))
                    .background(color)
            )
        }
    }
}

@Composable
fun PulseRings(color: Color, modifier: Modifier = Modifier, rings: Int = 2, periodMillis: Int = 2200) {
    val transition = rememberInfiniteTransition(label = "rings")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
        label = "ringT"
    )
    Canvas(modifier) {
        val base = size.minDimension / 2f
        repeat(rings) { i ->
            val p = (t + i.toFloat() / rings) % 1f
            drawCircle(
                color = color.copy(alpha = 0.55f * (1f - p)),
                radius = base * (1f + 0.7f * p),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun LivePill(text: String, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live")
    val p by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800)), label = "liveP")
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(8.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(8.dp)) {
                drawCircle(color.copy(alpha = 0.7f * (1f - p)), radius = size.minDimension / 2f * (1f + 2f * p))
                drawCircle(color)
            }
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun SourceBadge(source: PaymentSource, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(source.brandColor()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = source.monogram(),
            color = Color.White,
            fontFamily = Lexend,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.33f).sp
        )
    }
}

@Composable
fun Eyebrow(text: String, color: Color = Kp.colors.muted, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = EyebrowStyle, color = color, modifier = modifier)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Kp.colors.text)
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = Kp.colors.royal,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(4.dp)
            )
        }
    }
}

@Composable
fun IconTile(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.31f))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.56f))
    }
}

@Composable
fun KpSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Kp.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = c.royal,
            checkedBorderColor = c.royal,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = c.deck2,
            uncheckedBorderColor = c.line
        )
    )
}

@Composable
fun SettingRow(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    iconTint: Color = Kp.colors.royal,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) IconTile(icon, iconTint)
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Kp.colors.text)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Kp.colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Kp.colors.line))
}
