package com.kuralpay.app.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuralpay.app.domain.model.AudioRoute
import com.kuralpay.app.domain.model.VoiceProfile
import com.kuralpay.app.ui.components.BottomBarSpace
import com.kuralpay.app.ui.components.Eyebrow
import com.kuralpay.app.ui.components.Hairline
import com.kuralpay.app.ui.components.KpSwitch
import com.kuralpay.app.ui.components.SettingRow
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.Kp
import com.kuralpay.app.voice.tts.VoiceProfileMapper
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val VoiceProfile.blurb: String
    get() = when (this) {
        VoiceProfile.SHOP -> "Loud and slow, for a busy counter"
        VoiceProfile.PROFESSIONAL -> "Clear and formal"
        VoiceProfile.FRIENDLY -> "Warm, says who paid"
        VoiceProfile.TAMIL -> "தொகை மட்டும் சொல்லும்"
        VoiceProfile.TAMIL_FRIENDLY -> "யார் அனுப்பினார் என்றும் சொல்லும்"
    }

private val VoiceProfile.title: String
    get() = when (this) {
        VoiceProfile.SHOP -> "Shop"
        VoiceProfile.PROFESSIONAL -> "Professional"
        VoiceProfile.FRIENDLY -> "Friendly"
        VoiceProfile.TAMIL -> "தமிழ்"
        VoiceProfile.TAMIL_FRIENDLY -> "தமிழ் நட்பு"
    }

private val AudioRoute.label: String
    get() = when (this) {
        AudioRoute.AUTO_DETECT -> "Auto"
        AudioRoute.BLUETOOTH_ONLY -> "Bluetooth"
        AudioRoute.WIRED_ONLY -> "Wired"
        AudioRoute.PHONE_SPEAKER_ONLY -> "Phone"
    }

@Composable
fun SettingsScreen(
    onNavigateToSummary: () -> Unit = {},
    onNavigateToForwarding: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val c = Kp.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(c.ink).statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium, color = c.text, modifier = Modifier.entrance(0).padding(bottom = 6.dp)) }

        item { Eyebrow("Alerts", modifier = Modifier.entrance(1)) }
        item {
            Column(Modifier.entrance(2).kpCard().padding(horizontal = 16.dp)) {
                SettingRow(Icons.Rounded.Layers, "Lock screen card", "Show each payment over any app") {
                    KpSwitch(state.overlayEnabled, { viewModel.toggleOverlay() })
                }
                Hairline()
                SettingRow(Icons.AutoMirrored.Rounded.VolumeUp, "Speak payments aloud", "Announce each payment as it arrives") {
                    KpSwitch(state.voiceEnabled, { viewModel.toggleVoice() })
                }
                Hairline()
                SettingRow(
                    Icons.Rounded.Campaign,
                    "Full volume for announcements",
                    "Turns media volume to maximum while speaking, then sets it back"
                ) {
                    KpSwitch(state.maxVolume, { viewModel.toggleMaxVolume() })
                }
                Hairline()
                DurationSlider(state.overlayDuration) { viewModel.setOverlayDuration(it) }
            }
        }

        item { Eyebrow("Voice", modifier = Modifier.entrance(3).padding(top = 8.dp)) }
        item {
            // Each "amount only" voice sits beside its "says who paid" twin.
            val rows = listOf(
                listOf(VoiceProfile.SHOP, VoiceProfile.FRIENDLY),
                listOf(VoiceProfile.TAMIL, VoiceProfile.TAMIL_FRIENDLY),
                listOf(VoiceProfile.PROFESSIONAL)
            )
            Column(Modifier.entrance(4), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { p ->
                            ProfileCard(p, p == state.voiceProfile, Modifier.weight(1f)) { viewModel.setVoiceProfile(p) }
                        }
                    }
                }
                VoicePreview(
                    profile = state.voiceProfile,
                    ready = viewModel.isVoiceReady(state.voiceProfile),
                    onPlay = { viewModel.playSample(state.voiceProfile) },
                    installIntent = { viewModel.installVoiceIntent() }
                )
                ReplayCountSlider(state.replayCount) { viewModel.setReplayCount(it) }
            }
        }

        item { Eyebrow("Speaker", modifier = Modifier.entrance(5).padding(top = 8.dp)) }
        item { RouteSelector(viewModel.audioRoutes, state.audioRoute, Modifier.entrance(6)) { viewModel.setAudioRoute(it) } }

        item { Eyebrow("More", modifier = Modifier.entrance(7).padding(top = 8.dp)) }
        item {
            Column(Modifier.entrance(8).kpCard().padding(horizontal = 16.dp)) {
                SettingRow(
                    Icons.Rounded.Schedule, "Daily summaries", "Hear your collection at set times",
                    modifier = Modifier.kpClickable(onNavigateToSummary)
                ) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = c.muted)
                }
                Hairline()
                SettingRow(
                    Icons.AutoMirrored.Rounded.Send, "Payment forwarding", "Announce your payments on another KuralPay phone",
                    modifier = Modifier.kpClickable(onNavigateToForwarding)
                ) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = c.muted)
                }
                Hairline()
                GroqKeyEditor(state.groqApiKey) { viewModel.setGroqApiKey(it) }
            }
        }

        item { AboutEntry(onNavigateToAbout, Modifier.entrance(9).padding(top = 8.dp)) }
    }
}

@Composable
private fun DurationSlider(durationMs: Int, onChange: (Int) -> Unit) {
    val c = Kp.colors
    Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("Card stays on screen", style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
            Text("${durationMs / 1000} s", style = MaterialTheme.typography.titleSmall, color = c.accent)
        }
        Slider(
            value = durationMs / 1000f,
            onValueChange = { onChange((it.roundToInt()) * 1000) },
            valueRange = 2f..8f,
            steps = 5,
            colors = SliderDefaults.colors(
                thumbColor = c.royal,
                activeTrackColor = c.royal,
                inactiveTrackColor = c.deck2,
                activeTickColor = c.onRoyal.copy(alpha = 0.5f),
                inactiveTickColor = c.muted.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun ReplayCountSlider(count: Int, onChange: (Int) -> Unit) {
    val c = Kp.colors
    Column(
        Modifier.fillMaxWidth().kpCard().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Replay button plays", style = MaterialTheme.typography.bodyLarge, color = c.text)
                Text("The replay on Today's collection, newest first", style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Text(
                if (count == 1) "Last 1" else "Last $count",
                style = MaterialTheme.typography.titleSmall,
                color = c.accent
            )
        }
        Slider(
            value = count.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = c.royal,
                activeTrackColor = c.royal,
                inactiveTrackColor = c.deck2,
                activeTickColor = c.onRoyal.copy(alpha = 0.5f),
                inactiveTickColor = c.muted.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun ProfileCard(profile: VoiceProfile, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Kp.colors
    val border by animateColorAsState(if (selected) c.accent else c.line, tween(250), label = "pBorder")
    val fill by animateColorAsState(if (selected) c.accent.copy(alpha = 0.10f) else c.deck, tween(250), label = "pFill")
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .kpClickable(onClick)
            .clip(shape)
            .background(c.deck)
            .background(fill)
            .border(1.5.dp, border, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(profile.title, style = MaterialTheme.typography.titleSmall, color = c.text)
        Text(profile.blurb, style = MaterialTheme.typography.bodySmall, color = c.muted, maxLines = 2)
    }
}

@Composable
private fun VoicePreview(
    profile: VoiceProfile,
    ready: Boolean,
    onPlay: () -> Unit,
    installIntent: () -> android.content.Intent
) {
    val c = Kp.colors
    val context = LocalContext.current
    val line = VoiceProfileMapper.getAnnouncementText(profile, 500.0, "Arun", "Google Pay")
    Column(
        Modifier.fillMaxWidth().kpCard(16.dp, fill = c.accent.copy(alpha = 0.08f)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("“$line”", style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .kpClickable(onPlay)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.lime)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = c.onLime, modifier = Modifier.size(18.dp))
                Text("Play sample", style = MaterialTheme.typography.labelMedium, color = c.onLime)
            }
        }
        AnimatedVisibility(!ready) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Hairline()
                Text(
                    "This phone has no ${if (profile.language.startsWith("ta")) "Tamil" else "matching"} voice installed, so announcements fall back to English. Install the voice, then come back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.muted
                )
                Text(
                    "Install voice",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.onRoyal,
                    modifier = Modifier
                        .kpClickable { runCatching { context.startActivity(installIntent()) } }
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.royal)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun RouteSelector(routes: List<AudioRoute>, selected: AudioRoute, modifier: Modifier, onSelect: (AudioRoute) -> Unit) {
    val c = Kp.colors
    val index = routes.indexOf(selected).coerceAtLeast(0)
    BoxWithConstraints(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.deck2).padding(4.dp)
    ) {
        val segment = maxWidth / routes.size
        val x by animateDpAsState(segment * index, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.deck)
                .border(1.dp, c.line, RoundedCornerShape(10.dp))
        )
        Row(Modifier.fillMaxWidth().height(38.dp)) {
            routes.forEach { r ->
                val fg by animateColorAsState(if (r == selected) c.text else c.muted, label = "segFg")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp)).kpClickable { onSelect(r) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(r.label, style = MaterialTheme.typography.labelMedium, color = fg, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun AboutEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Kp.colors
    Row(
        modifier
            .fillMaxWidth()
            .kpClickable(onClick)
            .kpCard()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(androidx.compose.ui.graphics.Color.White),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(com.kuralpay.app.R.drawable.kp_logo_mark),
                contentDescription = null,
                modifier = Modifier.size(38.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text("About KuralPay", style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = c.text)
            Text("Version ${appVersion()} · ABP - Ventures", style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = c.muted)
    }
}

private fun maskKey(key: String): String =
    if (key.length <= 8) "••••••••" else key.take(4) + "••••••••" + key.takeLast(4)

/**
 * A saved key stays masked. Viewing or changing it needs the phone's screen lock;
 * adding the first key does not. Leaving the screen locks it again.
 */
@Composable
private fun GroqKeyEditor(saved: String, onSave: (String) -> Unit) {
    val c = Kp.colors
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(false) }
    var draft by remember(saved, unlocked) { mutableStateOf(if (saved.isEmpty() || unlocked) saved else "") }
    var showKey by remember(unlocked) { mutableStateOf(unlocked) }
    var message by remember { mutableStateOf<String?>(null) }
    var justSaved by remember { mutableStateOf(false) }
    LaunchedEffect(justSaved) {
        if (justSaved) { delay(1800); justSaved = false }
    }
    val locked = saved.isNotEmpty() && !unlocked
    val fieldShape = RoundedCornerShape(12.dp)

    fun unlock() {
        message = null
        confirmScreenLock(
            context,
            title = "Unlock Groq API key",
            subtitle = "Confirm your screen lock to view or change the key",
            onSuccess = { unlocked = true },
            onFailure = { message = it }
        )
    }

    Column(Modifier.padding(top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingRow(
            Icons.Rounded.Key, "Groq API key", "Unlocks open-ended questions in Ask KuralPay",
            modifier = Modifier.padding(vertical = 0.dp), iconTint = c.accent
        ) {
            AnimatedVisibility(justSaved, enter = scaleIn() + fadeIn(), exit = fadeOut()) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Saved", tint = c.good)
            }
        }

        AnimatedContent(locked, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "keyLock") { isLocked ->
            if (isLocked) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clip(fieldShape).background(c.deck2).border(1.dp, c.line, fieldShape)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
                        Text(maskKey(saved), style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
                    }
                    Text(
                        "Unlock to view or change",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.onRoyal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .kpClickable { unlock() }
                            .clip(fieldShape)
                            .background(c.royal)
                            .padding(vertical = 12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.weight(1f).clip(fieldShape).background(c.deck2).border(1.dp, c.line, fieldShape)
                                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                if (draft.isEmpty()) Text("Paste your gsk_… key", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                                BasicTextField(
                                    value = draft,
                                    onValueChange = { draft = it },
                                    singleLine = true,
                                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.text),
                                    cursorBrush = SolidColor(c.royal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).kpClickable { showKey = !showKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showKey) "Hide key" else "Show key",
                                    tint = c.muted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            "Save",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.onRoyal,
                            modifier = Modifier
                                .kpClickable {
                                    onSave(draft.trim())
                                    unlocked = false
                                    justSaved = true
                                }
                                .clip(fieldShape)
                                .background(c.royal)
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }
                    if (unlocked) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Cancel",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.muted,
                                modifier = Modifier.kpClickable { unlocked = false }.padding(vertical = 4.dp)
                            )
                            Text(
                                "Remove key",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.accent,
                                modifier = Modifier.kpClickable { onSave(""); unlocked = false }.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.warn) }
    }
}
