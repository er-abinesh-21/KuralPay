package com.kuralpay.app.account

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuralpay.app.R
import com.kuralpay.app.ui.components.PulseRings
import com.kuralpay.app.ui.components.VoiceWave
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.BrandIndigo
import com.kuralpay.app.ui.theme.BrandOrange
import com.kuralpay.app.ui.theme.Kp

@Composable
fun AuthScreen(
    onBack: () -> Unit = {},
    onSignedIn: () -> Unit = {},
    /** False on first launch, where signing in is required and there is nowhere to go back to. */
    showBack: Boolean = true,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val c = Kp.colors
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val state by viewModel.state.collectAsState()
    val signedIn by viewModel.signedIn.collectAsState()
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(signedIn) { if (signedIn) onSignedIn() }

    val submit = {
        focus.clearFocus()
        if (!state.busy) {
            if (creating) viewModel.signUp(name, email, password) else viewModel.signIn(email, password)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.ink)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        BrandHeader(showBack = showBack, onBack = onBack)

        // The form card rises over the bottom edge of the header.
        Column(
            Modifier
                .offset(y = (-36).dp)
                .padding(horizontal = 18.dp)
                .entrance(1, offset = 28.dp)
                .shadow(24.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(30.dp))
                .background(c.deck)
                .border(1.dp, c.line, RoundedCornerShape(30.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModeSwitch(creating) { creating = it; viewModel.clearMessages() }

            AnimatedContent(creating, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "authTitle") { isNew ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (isNew) "Create your account" else "Welcome back",
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.text
                    )
                    Text(
                        if (isNew) "Your payment history and settings follow you to any phone."
                        else "Sign in to keep your payments safe in your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.muted
                    )
                }
            }

            AnimatedVisibility(creating, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                PremiumField(
                    icon = Icons.Rounded.Person,
                    placeholder = "Your name",
                    value = name,
                    onChange = { name = it },
                    imeAction = ImeAction.Next,
                    onIme = { focus.moveFocus(FocusDirection.Down) }
                )
            }
            PremiumField(
                icon = Icons.Rounded.Email,
                placeholder = "Email address",
                value = email,
                onChange = { email = it.trim() },
                keyboard = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onIme = { focus.moveFocus(FocusDirection.Down) }
            )
            PremiumField(
                icon = Icons.Rounded.Lock,
                placeholder = if (creating) "Password (6+ characters)" else "Password",
                value = password,
                onChange = { password = it },
                keyboard = KeyboardType.Password,
                secret = true,
                imeAction = ImeAction.Done,
                onIme = { submit() }
            )

            if (!creating) {
                Text(
                    "Forgot password?",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.accent,
                    modifier = Modifier
                        .align(Alignment.End)
                        .offset(y = (-6).dp)
                        .kpClickable { viewModel.resetPassword(email) }
                        .padding(4.dp)
                )
            }

            // Only takes space when there is something to say, so the button sits right under the fields.
            val message = state.error ?: state.notice
            AnimatedVisibility(message != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                if (message != null) {
                    val isError = state.error != null
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) c.warn else c.good,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background((if (isError) c.warn else c.good).copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            PrimaryButton(
                label = if (creating) "Create account" else "Sign in",
                busy = state.busy,
                onClick = submit
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).height(1.dp).background(c.line))
                Text("or continue with", style = MaterialTheme.typography.labelMedium, color = c.muted)
                Box(Modifier.weight(1f).height(1.dp).background(c.line))
            }

            GoogleButton(enabled = !state.busy) { viewModel.google(context) }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .padding(horizontal = 28.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .entrance(3),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = c.muted, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            Text(
                "  Secured by Google Firebase. After you sign in, KuralPay works offline.",
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Orange brand panel: logo with pulse rings, name, tagline and the live voice wave. */
@Composable
private fun BrandHeader(showBack: Boolean, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(BrandOrange)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = Offset(size.width * 0.85f, 0f), radius = size.width * 0.8f),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.85f, 0f)
                )
                drawCircle(
                    Brush.radialGradient(listOf(BrandIndigo.copy(alpha = 0.25f), Color.Transparent), center = Offset(0f, size.height), radius = size.width * 0.7f),
                    radius = size.width * 0.7f,
                    center = Offset(0f, size.height)
                )
            }
            .statusBarsPadding()
            .padding(top = 12.dp, bottom = 60.dp)
    ) {
        if (showBack) {
            Box(
                Modifier
                    .padding(start = 18.dp)
                    .size(42.dp)
                    .kpClickable(onBack)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(top = if (showBack) 8.dp else 20.dp).entrance(0),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                PulseRings(Color.White, Modifier.size(88.dp), periodMillis = 2600)
                Box(
                    Modifier
                        .size(88.dp)
                        .shadow(18.dp, RoundedCornerShape(26.dp))
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(R.drawable.kp_logo_mark), contentDescription = "KuralPay", modifier = Modifier.size(76.dp))
                }
            }
            Text("KuralPay", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VoiceWave(color = Color.White, bars = 5, barWidth = 3.dp, height = 16.dp)
                Text("The voice of digital payments", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.9f))
                VoiceWave(color = Color.White, bars = 5, barWidth = 3.dp, height = 16.dp)
            }
        }
    }
}

/** Two-way switch with a sliding pill. */
@Composable
private fun ModeSwitch(creating: Boolean, onChange: (Boolean) -> Unit) {
    val c = Kp.colors
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(c.deck2).padding(4.dp)
    ) {
        val half = maxWidth / 2
        val x by animateDpAsState(if (creating) half else 0.dp, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "modePill")
        Box(
            Modifier
                .offset(x = x)
                .width(half)
                .fillMaxHeight()
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(BrandOrange)
        )
        Row(Modifier.fillMaxSize()) {
            listOf(false to "Sign in", true to "Create account").forEach { (isNew, label) ->
                val selected = isNew == creating
                val fg by animateColorAsState(if (selected) Color.White else c.muted, label = "modeFg")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).kpClickable { onChange(isNew) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
                }
            }
        }
    }
}

/** Text field with a leading icon, an orange glow while focused and an optional show/hide eye. */
@Composable
private fun PremiumField(
    icon: ImageVector,
    placeholder: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onIme: () -> Unit = {}
) {
    val c = Kp.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var reveal by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) BrandOrange else c.line, tween(200), label = "fieldBorder")
    val tint by animateColorAsState(if (focused) BrandOrange else c.muted, tween(200), label = "fieldIcon")
    val shape = RoundedCornerShape(16.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(c.deck2)
            .border(if (focused) 1.5.dp else 1.dp, border, shape)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.muted)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                interactionSource = interaction,
                visualTransformation = if (secret && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
                keyboardActions = KeyboardActions(onNext = { onIme() }, onDone = { onIme() }),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.text),
                cursorBrush = SolidColor(BrandOrange),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (secret) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).kpClickable { reveal = !reveal },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (reveal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (reveal) "Hide password" else "Show password",
                    tint = c.muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, busy: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .kpClickable(onClick)
            .shadow(14.dp, RoundedCornerShape(16.dp), ambientColor = BrandOrange, spotColor = BrandOrange)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandOrange),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(busy, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "primaryBusy") { isBusy ->
            if (isBusy) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** White button with the real Google mark, per Google's sign-in branding. */
@Composable
private fun GoogleButton(enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .kpClickable { if (enabled) onClick() }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(16.dp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(20.dp))
        Text("  Continue with Google", style = MaterialTheme.typography.titleSmall, color = Color(0xFF1F1F1F))
    }
}
