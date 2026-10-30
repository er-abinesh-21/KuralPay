package com.kuralpay.app.assistant.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuralpay.app.ui.components.BottomBarSpace
import com.kuralpay.app.ui.components.PulseRings
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.Kp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val thinking by viewModel.thinking.collectAsState()
    val c = Kp.colors
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }

    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(viewModel::processQuery)
        }
    }
    val startVoice = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask about your payments")
        }
        runCatching { speech.launch(intent) }
    }

    LaunchedEffect(messages.size, thinking) {
        val last = messages.size + (if (thinking) 1 else 0) + 1
        listState.animateScrollToItem(last.coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize().background(c.ink).statusBarsPadding().imePadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = BottomBarSpace + 150.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().entrance(0), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ask KuralPay", style = MaterialTheme.typography.headlineMedium, color = c.text, modifier = Modifier.weight(1f))
                    Text(
                        "AI · Premium",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.accent,
                        modifier = Modifier.clip(CircleShape).background(c.accent.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            items(messages, key = { it.timestamp }) { msg -> Bubble(msg) }
            if (thinking) item(key = "thinking") { TypingBubble() }
            item(key = "suggest") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    viewModel.suggestions.forEach { s ->
                        Text(
                            s,
                            style = MaterialTheme.typography.labelLarge,
                            color = c.text,
                            modifier = Modifier
                                .kpClickable { viewModel.processQuery(s) }
                                .clip(CircleShape)
                                .background(c.deck)
                                .border(1.dp, c.line, CircleShape)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(c.ink.copy(alpha = 0f), c.ink, c.ink)))
                .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = BottomBarSpace - 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MicOrb(onClick = { startVoice() })
            Row(
                Modifier.fillMaxWidth().kpCard(18.dp).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val send = { viewModel.processQuery(draft); draft = "" }
                Box(Modifier.weight(1f)) {
                    if (draft.isEmpty()) Text("Or type a question", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.text),
                        cursorBrush = SolidColor(c.royal),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    Modifier.size(40.dp).kpClickable { send() }.clip(RoundedCornerShape(13.dp)).background(c.royal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = c.onRoyal, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMessage) {
    val c = Kp.colors
    val shape = if (msg.isUser) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            msg.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (msg.isUser) c.onRoyal else c.text,
            modifier = Modifier
                .entrance(offset = 12.dp)
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(if (msg.isUser) c.royal else c.deck)
                .then(if (msg.isUser) Modifier else Modifier.border(1.dp, c.line, shape))
                .padding(horizontal = 14.dp, vertical = 11.dp)
        )
    }
}

@Composable
private fun TypingBubble() {
    val c = Kp.colors
    val t = rememberInfiniteTransition(label = "typing")
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
            .background(c.deck)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(3) { i ->
            val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(450, delayMillis = i * 150), RepeatMode.Reverse), label = "dot$i")
            Box(Modifier.size(7.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(c.muted))
        }
    }
}

@Composable
private fun MicOrb(onClick: () -> Unit) {
    val c = Kp.colors
    var pressedOnce by remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        PulseRings(c.accent, Modifier.size(76.dp))
        Box(
            Modifier
                .size(76.dp)
                .kpClickable { pressedOnce = true; onClick() }
                .clip(CircleShape)
                .background(c.lime),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = "Speak a question", tint = c.onLime, modifier = Modifier.size(32.dp))
        }
    }
    AnimatedVisibility(!pressedOnce, enter = fadeIn(), exit = fadeOut()) {
        Text("Tap the mic and ask in English", style = MaterialTheme.typography.bodySmall, color = c.muted)
    }
}
