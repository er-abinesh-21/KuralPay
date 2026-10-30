package com.kuralpay.app.update.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import com.kuralpay.app.R
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.BrandOrange
import com.kuralpay.app.ui.theme.Kp
import com.kuralpay.app.ui.theme.SuccessGreen
import com.kuralpay.app.update.AvailableUpdate
import com.kuralpay.app.update.UpdateManager
import com.kuralpay.app.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(val manager: UpdateManager) : ViewModel() {
    fun checkOnLaunch() = manager.checkOnLaunch()
    fun checkNow() = manager.checkNow()
}

/** Shows the update sheet when asked, and a pending automatic prompt only on Home with no announcement playing. */
@Composable
fun UpdateHost(viewModel: UpdateViewModel, onHome: Boolean) {
    val manager = viewModel.manager
    val visible by manager.sheetVisible.collectAsState()
    val pending by manager.autoPromptPending.collectAsState()

    LaunchedEffect(pending, onHome) {
        delay(1_500) // let Home settle before interrupting
        while (pending && onHome) {
            manager.showAutoPrompt()
            delay(3_000)
        }
    }

    if (visible) UpdateSheet(manager)
}

@Composable
private fun UpdateSheet(manager: UpdateManager) {
    val c = Kp.colors
    val context = LocalContext.current
    val state by manager.state.collectAsState()
    val dismissible = state !is UpdateState.Verifying

    // Coming back from "Install unknown apps": pick up the new permission without another tap.
    LaunchedEffect(state) {
        while (state is UpdateState.NeedsInstallPermission) {
            manager.refreshInstallPermission()
            delay(1_000)
        }
    }

    Dialog(
        onDismissRequest = { if (dismissible) manager.hideSheet() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = dismissible)
    ) {
        Column(
            Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(c.deck)
                .border(1.dp, c.line, RoundedCornerShape(30.dp))
        ) {
            Header(state, manager.installedVersion)
            AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "updateBody"
            ) { s ->
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (s) {
                        UpdateState.Idle, UpdateState.Checking -> Busy("Checking Release Orbit for a newer KuralPay…")
                        UpdateState.UpToDate -> {
                            Centered(Icons.Rounded.CheckCircle, SuccessGreen, "You're on the latest version", "KuralPay ${manager.installedVersion} is up to date.")
                            Buttons(primary = "Done" to { manager.hideSheet() })
                        }
                        is UpdateState.Available -> {
                            WhatsNew(s.update)
                            Meta("Download size", formatSize(s.update.apk.size))
                            Buttons(primary = "Update now" to { manager.download() }, secondary = "Later" to { manager.later() })
                        }
                        is UpdateState.Downloading -> {
                            Progress(s.bytes, s.total)
                            Buttons(secondary = "Cancel" to { manager.cancelDownload() })
                        }
                        is UpdateState.Verifying -> Busy("Checking the update is genuine and signed by KuralPay…")
                        is UpdateState.NeedsInstallPermission -> {
                            Note("To install updates, allow KuralPay to install apps. You only need to do this once.")
                            Buttons(
                                primary = "Allow installs" to { manager.openInstallPermission(context) },
                                secondary = "Later" to { manager.hideSheet() }
                            )
                        }
                        is UpdateState.ReadyToInstall -> {
                            SafetyNote()
                            if (s.waitingForAnnouncement) Busy("Waiting for the current payment announcement to finish…")
                            else Buttons(primary = "Install" to { manager.install(context) }, secondary = "Later" to { manager.hideSheet() })
                        }
                        is UpdateState.Failed -> {
                            Centered(Icons.Rounded.ErrorOutline, c.warn, "Update not completed", s.message)
                            Buttons(
                                primary = "Retry" to { if (s.update != null) manager.download() else manager.checkNow() },
                                secondary = "Close" to { manager.hideSheet() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: UpdateState, installed: String) {
    val update: AvailableUpdate? = when (state) {
        is UpdateState.Available -> state.update
        is UpdateState.Downloading -> state.update
        is UpdateState.Verifying -> state.update
        is UpdateState.NeedsInstallPermission -> state.update
        is UpdateState.ReadyToInstall -> state.update
        is UpdateState.Failed -> state.update
        else -> null
    }
    Row(
        Modifier.fillMaxWidth().background(BrandOrange).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.kp_logo_mark), contentDescription = null, modifier = Modifier.size(44.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (update != null) "Update available" else "Software update", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VersionPill("v$installed", filled = false)
                if (update != null) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "to", tint = Color.White, modifier = Modifier.size(16.dp))
                    VersionPill("v${update.version}", filled = true)
                }
            }
        }
    }
}

@Composable
private fun VersionPill(text: String, filled: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (filled) BrandOrange else Color.White,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (filled) Color.White else Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun WhatsNew(update: AvailableUpdate) {
    val c = Kp.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("What's new", style = MaterialTheme.typography.titleMedium, color = c.text)
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.deck2)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            releaseNoteLines(update.notes).forEach { (kind, text) ->
                when (kind) {
                    NoteLine.Heading -> Text(text, style = MaterialTheme.typography.titleSmall, color = c.text)
                    NoteLine.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", style = MaterialTheme.typography.bodyMedium, color = c.accent)
                        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.text)
                    }
                    NoteLine.Text -> Text(text, style = MaterialTheme.typography.bodyMedium, color = c.text)
                }
            }
        }
    }
}

private enum class NoteLine { Heading, Bullet, Text }

/** Release notes are untrusted public text: shown as plain text, links not followed, length capped. */
private fun releaseNoteLines(markdown: String): List<Pair<NoteLine, String>> {
    val lines = markdown.take(4_000).lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("```") }
        .map { line ->
            val clean = line.replace(Regex("""\*\*|__|`"""), "").replace(Regex("""\[([^\]]+)]\([^)]*\)"""), "$1")
            when {
                clean.startsWith("#") -> NoteLine.Heading to clean.trimStart('#').trim()
                clean.startsWith("- ") || clean.startsWith("* ") -> NoteLine.Bullet to clean.drop(2).trim()
                else -> NoteLine.Text to clean
            }
        }
        .filter { it.second.isNotEmpty() }
    return lines.ifEmpty { listOf(NoteLine.Text to "Improvements and fixes.") }
}

@Composable
private fun Meta(label: String, value: String) {
    val c = Kp.colors
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = c.text)
    }
}

@Composable
private fun Progress(bytes: Long, total: Long) {
    val c = Kp.colors
    val fraction = if (total > 0) (bytes.toFloat() / total).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(fraction, tween(250), label = "dl")
    val percent = (fraction * 100).toInt()
    Column(
        Modifier.semantics { contentDescription = "Downloading update, $percent percent" },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("Downloading", style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
            Text("$percent%", style = MaterialTheme.typography.titleSmall, color = c.accent)
        }
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = BrandOrange,
            trackColor = c.deck2
        )
        Text("${formatSize(bytes)} of ${formatSize(total)}", style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}

@Composable
private fun SafetyNote() {
    val c = Kp.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SuccessGreen.copy(alpha = 0.12f)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.Shield, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
        Text(
            "Verified: signed by KuralPay. Android will ask you to confirm. Your payments, history, settings and sign-in stay exactly as they are.",
            style = MaterialTheme.typography.bodySmall,
            color = c.text
        )
    }
}

@Composable
private fun Busy(text: String) {
    val c = Kp.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        CircularProgressIndicator(Modifier.size(22.dp), color = BrandOrange, strokeWidth = 2.5.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.text)
    }
}

@Composable
private fun Centered(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, body: String) {
    val c = Kp.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(44.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = c.text, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Kp.colors.text)
}

@Composable
private fun Buttons(primary: Pair<String, () -> Unit>? = null, secondary: Pair<String, () -> Unit>? = null) {
    val c = Kp.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (secondary != null) {
            Box(
                Modifier.weight(1f).height(52.dp).kpClickable(secondary.second).clip(RoundedCornerShape(16.dp)).background(c.deck2),
                contentAlignment = Alignment.Center
            ) { Text(secondary.first, style = MaterialTheme.typography.titleSmall, color = c.text) }
        }
        if (primary != null) {
            Box(
                Modifier.weight(1f).height(52.dp).kpClickable(primary.second).clip(RoundedCornerShape(16.dp)).background(BrandOrange),
                contentAlignment = Alignment.Center
            ) { Text(primary.first, style = MaterialTheme.typography.titleSmall, color = Color.White) }
        }
    }
}

private fun formatSize(bytes: Long): String =
    if (bytes >= 1024 * 1024) String.format(Locale.ENGLISH, "%.1f MB", bytes / (1024.0 * 1024.0))
    else String.format(Locale.ENGLISH, "%d KB", (bytes / 1024).coerceAtLeast(1))
