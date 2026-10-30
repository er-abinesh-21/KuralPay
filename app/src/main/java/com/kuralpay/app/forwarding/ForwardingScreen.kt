package com.kuralpay.app.forwarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuralpay.app.ui.components.BottomBarSpace
import com.kuralpay.app.ui.components.Eyebrow
import com.kuralpay.app.ui.components.Hairline
import com.kuralpay.app.ui.components.IconTile
import com.kuralpay.app.ui.components.KpSwitch
import com.kuralpay.app.ui.components.SettingRow
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.Kp

@Composable
fun ForwardingScreen(
    onBack: () -> Unit = {},
    onSignIn: () -> Unit = {},
    viewModel: ForwardingViewModel = hiltViewModel()
) {
    val c = Kp.colors
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(c.ink).statusBarsPadding().imePadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.entrance(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).kpClickable(onBack).kpCard(13.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.text, modifier = Modifier.size(20.dp))
                }
                Text("Payment forwarding", style = MaterialTheme.typography.headlineMedium, color = c.text)
            }
        }

        if (!state.configured) {
            item { InfoCard("Firebase isn't set up in this build", "Add google-services.json to the app folder and rebuild. Everything else in KuralPay keeps working.") }
            return@LazyColumn
        }

        if (!state.signedIn) {
            item {
                Column(Modifier.fillMaxWidth().entrance(1).kpCard(24.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Announce your payments on another phone", style = MaterialTheme.typography.titleLarge, color = c.text)
                    Text(
                        "Link a partner's KuralPay by mobile number. When you receive a payment, their phone announces the amount and who paid. Nothing else is shared.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.muted
                    )
                    Box(
                        Modifier.fillMaxWidth().kpClickable(onSignIn).clip(RoundedCornerShape(14.dp)).background(c.lime).padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Sign in or create account", style = MaterialTheme.typography.labelLarge, color = c.onLime) }
                }
            }
            return@LazyColumn
        }

        item { Messages(state, viewModel::dismissMessage) }

        item {
            Row(
                Modifier.fillMaxWidth().entrance(1).kpCard().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Signed in", style = MaterialTheme.typography.bodySmall, color = c.muted)
                    Text(state.email, style = MaterialTheme.typography.bodyMedium, color = c.text, maxLines = 1)
                }
                Text("Sign out", style = MaterialTheme.typography.labelLarge, color = c.accent, modifier = Modifier.kpClickable(viewModel::signOut).padding(8.dp))
            }
        }

        item { MyNumberCard(state.myPhone, state.myName, state.busy, viewModel::saveMyPhone, Modifier.entrance(2)) }

        item {
            Column(Modifier.entrance(3).kpCard().padding(horizontal = 16.dp)) {
                SettingRow(Icons.AutoMirrored.Rounded.Send, "Forward my payments", "Send each payment I receive to the people below") {
                    KpSwitch(state.forwardingEnabled, viewModel::setForwardingEnabled)
                }
                Hairline()
                SettingRow(Icons.Rounded.Phone, "Announce payments sent to me", "Speak payments that linked people forward to this phone") {
                    KpSwitch(state.acceptForwarded, viewModel::setAcceptForwarded)
                }
            }
        }

        if (state.requestsToMe.isNotEmpty()) {
            item { Eyebrow("Requests", modifier = Modifier.padding(top = 4.dp)) }
            items(state.requestsToMe, key = { it.id }) { link ->
                Column(Modifier.fillMaxWidth().kpCard(20.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${link.fromName.ifBlank { "Someone" }} wants to forward payments to you", style = MaterialTheme.typography.bodyLarge, color = c.text)
                    link.fromPhone?.let { Text(PhoneNumber.pretty(it), style = MaterialTheme.typography.bodySmall, color = c.muted) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pill("Accept", filled = true) { viewModel.respond(link, true) }
                        Pill("Decline", filled = false) { viewModel.respond(link, false) }
                    }
                }
            }
        }

        item { Eyebrow("I forward to", modifier = Modifier.padding(top = 4.dp)) }
        if (state.sendingTo.isEmpty()) {
            item { EmptyHint("Nobody yet. Add a mobile number below.") }
        } else {
            item {
                Column(Modifier.fillMaxWidth().kpCard().padding(horizontal = 16.dp)) {
                    state.sendingTo.forEachIndexed { i, link ->
                        if (i > 0) Hairline()
                        LinkedPerson(
                            key = link.id,
                            name = link.toName.ifBlank { "KuralPay user" },
                            phone = link.toPhone,
                            detail = when {
                                link.status == LinkStatus.PENDING -> "Waiting for them to accept"
                                link.muted -> "Accepted, but they muted you"
                                else -> "Hears your payments"
                            },
                            removeQuestion = "Stop forwarding your payments to them?",
                            onRemove = { viewModel.unlink(link) }
                        )
                    }
                }
            }
        }

        item { Eyebrow("Forwarding to me", modifier = Modifier.padding(top = 4.dp)) }
        if (state.receivingFrom.isEmpty()) {
            item { EmptyHint("No one forwards to you yet.") }
        } else {
            item {
                Column(Modifier.fillMaxWidth().kpCard().padding(horizontal = 16.dp)) {
                    state.receivingFrom.forEachIndexed { i, link ->
                        if (i > 0) Hairline()
                        LinkedPerson(
                            key = link.id,
                            name = link.fromName.ifBlank { "KuralPay user" },
                            phone = link.fromPhone,
                            detail = if (link.muted) "Muted" else "Announcing their payments",
                            removeQuestion = "Stop hearing their payments on this phone?",
                            onRemove = { viewModel.unlink(link) },
                            extra = { KpSwitch(!link.muted, { on -> viewModel.setMuted(link, !on) }) }
                        )
                    }
                }
            }
        }

        item { AddByNumber(state.busy, viewModel::requestLink, Modifier.entrance(4)) }
    }
}

@Composable
private fun Messages(state: ForwardingState, onDismiss: () -> Unit) {
    val c = Kp.colors
    val text = state.error ?: state.message
    LaunchedEffect(text) { if (text != null) { kotlinx.coroutines.delay(4000); onDismiss() } }
    AnimatedVisibility(text != null) {
        Text(
            text.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.error != null) c.warn else c.good,
            modifier = Modifier.fillMaxWidth().kpCard(16.dp, fill = (if (state.error != null) c.warn else c.good).copy(alpha = 0.10f)).padding(14.dp)
        )
    }
}

@Composable
private fun MyNumberCard(saved: String?, savedName: String?, busy: Boolean, onSave: (String, String) -> Unit, modifier: Modifier = Modifier) {
    val c = Kp.colors
    // Prefilled with what is saved, so a wrong number or name can simply be edited.
    var draft by rememberSaveable(saved) { mutableStateOf(saved?.removePrefix("+91").orEmpty()) }
    var name by rememberSaveable(savedName) { mutableStateOf(savedName.orEmpty()) }
    Column(modifier.fillMaxWidth().kpCard(22.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("My name and number", style = MaterialTheme.typography.titleMedium, color = c.text)
        Text(
            if (saved != null) "Linked people see you as ${savedName ?: "you"}, ${PhoneNumber.pretty(saved)}. Edit and save to change."
            else "Add the name and number others will use to find you.",
            style = MaterialTheme.typography.bodySmall,
            color = c.muted
        )
        NameField(name, { name = it }, "Your name or shop name")
        NumberRow(draft, { draft = it }, if (saved != null) "Save changes" else "Save", busy) { onSave(draft, name) }
    }
}

@Composable
private fun AddByNumber(busy: Boolean, onRequest: (String, String) -> Unit, modifier: Modifier = Modifier) {
    val c = Kp.colors
    var draft by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    Column(modifier.fillMaxWidth().kpCard(22.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Add a person", style = MaterialTheme.typography.titleMedium, color = c.text)
        Text("Enter their name and KuralPay mobile number. They must accept before anything is forwarded.", style = MaterialTheme.typography.bodySmall, color = c.muted)
        NameField(name, { name = it }, "Their name (optional)")
        NumberRow(draft, { draft = it }, "Send request", busy) { onRequest(draft, name); draft = ""; name = "" }
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit, hint: String) {
    val c = Kp.colors
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.fillMaxWidth().clip(shape).background(c.deck2).border(1.dp, c.line, shape).padding(horizontal = 12.dp, vertical = 12.dp)) {
        if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyMedium, color = c.muted)
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(40)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.text),
            cursorBrush = SolidColor(c.royal),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NumberRow(value: String, onChange: (String) -> Unit, action: String, busy: Boolean, onAction: () -> Unit) {
    val c = Kp.colors
    val shape = RoundedCornerShape(12.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clip(shape).background(c.deck2).border(1.dp, c.line, shape).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("+91", style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.padding(end = 8.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text("98765 43210", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                BasicTextField(
                    value = value,
                    onValueChange = { onChange(it.filter { ch -> ch.isDigit() || ch == ' ' || ch == '+' }.take(16)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.text),
                    cursorBrush = SolidColor(c.royal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Text(
            action,
            style = MaterialTheme.typography.labelLarge,
            color = c.onRoyal,
            modifier = Modifier
                .kpClickable { if (!busy && value.isNotBlank()) onAction() }
                .clip(shape)
                .background(if (value.isBlank() || busy) c.royal.copy(alpha = 0.4f) else c.royal)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

/** A linked person with name, number and status, and a Remove that asks before unlinking. */
@Composable
private fun LinkedPerson(
    key: String,
    name: String,
    phone: String?,
    detail: String,
    removeQuestion: String,
    onRemove: () -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    val c = Kp.colors
    var confirming by rememberSaveable(key) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.deck2), contentAlignment = Alignment.Center) {
                Text(name.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium, color = c.accent)
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                phone?.let { Text(PhoneNumber.pretty(it), style = MaterialTheme.typography.bodySmall, color = c.muted) }
                Text(detail, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            extra?.invoke()
            if (!confirming) {
                Text("Remove", style = MaterialTheme.typography.labelMedium, color = c.warn, modifier = Modifier.kpClickable { confirming = true }.padding(8.dp))
            }
        }
        AnimatedVisibility(confirming) {
            Column(
                Modifier.fillMaxWidth().kpCard(14.dp, fill = c.warn.copy(alpha = 0.10f)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Remove $name? $removeQuestion", style = MaterialTheme.typography.bodyMedium, color = c.text)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Remove",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.onRoyal,
                        modifier = Modifier.kpClickable { confirming = false; onRemove() }.clip(RoundedCornerShape(12.dp)).background(c.warn).padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                    Pill("Cancel", filled = false) { confirming = false }
                }
            }
        }
    }
}

@Composable
private fun Pill(label: String, filled: Boolean, onClick: () -> Unit) {
    val c = Kp.colors
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (filled) c.onLime else c.text,
        modifier = Modifier
            .kpClickable(onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) c.lime else c.deck2)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun InfoCard(title: String, body: String) {
    val c = Kp.colors
    Column(Modifier.fillMaxWidth().entrance(1).kpCard(22.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.muted)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Kp.colors.muted, modifier = Modifier.padding(horizontal = 4.dp))
}
