package com.kuralpay.app.account

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.Kp

@Composable
fun AuthScreen(
    onBack: () -> Unit = {},
    onSignedIn: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val c = Kp.colors
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val signedIn by viewModel.signedIn.collectAsState()
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(signedIn) { if (signedIn) onSignedIn() }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.entrance(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).kpClickable(onBack).kpCard(13.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.text, modifier = Modifier.size(20.dp))
            }
            Text(if (creating) "Create account" else "Sign in", style = MaterialTheme.typography.headlineMedium, color = c.text)
        }

        Text(
            "An account lets your payments be announced on another KuralPay phone. KuralPay still works without one.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted,
            modifier = Modifier.entrance(1)
        )

        Column(Modifier.fillMaxWidth().entrance(2).kpCard(24.dp).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedVisibility(creating) {
                Field("Your name", name, { name = it })
            }
            Field("Email", email, { email = it }, keyboard = KeyboardType.Email)
            Field("Password", password, { password = it }, secret = true, keyboard = KeyboardType.Password)

            AnimatedContent(state.error ?: state.notice, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "authMessage") { msg ->
                if (msg != null) {
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = if (state.error != null) c.warn else c.good)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .kpClickable {
                        if (state.busy) return@kpClickable
                        if (creating) viewModel.signUp(name, email, password) else viewModel.signIn(email, password)
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.lime)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), color = c.onLime, strokeWidth = 2.dp)
                else Text(if (creating) "Create account" else "Sign in", style = MaterialTheme.typography.labelLarge, color = c.onLime)
            }

            if (!creating) {
                Text(
                    "Forgot password?",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.accent,
                    modifier = Modifier.align(Alignment.CenterHorizontally).kpClickable { viewModel.resetPassword(email) }.padding(4.dp)
                )
            }
        }

        Row(Modifier.fillMaxWidth().entrance(3), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).size(1.dp).background(c.line))
            Text("or", style = MaterialTheme.typography.bodySmall, color = c.muted)
            Box(Modifier.weight(1f).size(1.dp).background(c.line))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .entrance(4)
                .kpClickable { if (!state.busy) viewModel.google(context) }
                .clip(RoundedCornerShape(14.dp))
                .background(c.deck)
                .border(1.dp, c.line, RoundedCornerShape(14.dp))
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("G", style = MaterialTheme.typography.titleMedium, color = c.accent, modifier = Modifier.padding(end = 10.dp))
            Text("Continue with Google", style = MaterialTheme.typography.labelLarge, color = c.text)
        }

        Text(
            if (creating) "Already have an account? Sign in" else "New here? Create an account",
            style = MaterialTheme.typography.labelLarge,
            color = c.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .entrance(5)
                .kpClickable { creating = !creating; viewModel.clearMessages() }
                .padding(12.dp)
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    secret: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text
) {
    val c = Kp.colors
    val shape = RoundedCornerShape(12.dp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.muted)
        Box(Modifier.fillMaxWidth().clip(shape).background(c.deck2).border(1.dp, c.line, shape).padding(horizontal = 12.dp, vertical = 12.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.text),
                cursorBrush = SolidColor(c.royal),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
