package com.kuralpay.app.settings.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kuralpay.app.R
import com.kuralpay.app.ui.components.Eyebrow
import com.kuralpay.app.ui.components.Hairline
import com.kuralpay.app.ui.components.VoiceWave
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.Kp

@Composable
fun appVersion(): String {
    val context = LocalContext.current
    return remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit = {}, onCheckUpdates: () -> Unit = {}) {
    val c = Kp.colors
    val version = appVersion()
    val logoPop = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        logoPop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.entrance(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).kpClickable(onBack).kpCard(13.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.text, modifier = Modifier.size(20.dp))
            }
            Text("About", style = MaterialTheme.typography.headlineMedium, color = c.text)
        }

        // Brand: the full logo (it already carries the tagline), then a live voice wave.
        Column(
            Modifier.fillMaxWidth().entrance(1),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(width = 220.dp, height = 204.dp)
                    .graphicsLayer { scaleX = logoPop.value; scaleY = logoPop.value }
                    .clip(RoundedCornerShape(34.dp))
                    .background(Color.White)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.kp_logo_full),
                    contentDescription = "KuralPay, the voice of digital payments",
                    modifier = Modifier.fillMaxSize()
                )
            }
            VoiceWave(color = c.lime, bars = 9, barWidth = 4.dp, height = 30.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Version $version",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.accent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(c.accent.copy(alpha = 0.14f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
                Text(
                    "Check for updates",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.onLime,
                    modifier = Modifier
                        .kpClickable(onCheckUpdates)
                        .clip(CircleShape)
                        .background(c.lime)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Maker
        Column(
            Modifier.fillMaxWidth().entrance(2).kpCard(24.dp).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Eyebrow("Made by")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Image(
                    painterResource(R.drawable.developer_photo),
                    contentDescription = "Abinesh B",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .border(2.dp, c.lime, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Abinesh B", style = MaterialTheme.typography.titleLarge, color = c.text)
                    Text("Founder · ABP - Ventures", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                }
            }
            Hairline()
            InfoRow("Developer Name", "Abinesh B")
            InfoRow("Organization", "ABP - Ventures")
            InfoRow("Position", "Founder")
        }

        Text(
            "Made in Tamil Nadu for India's shopkeepers",
            style = MaterialTheme.typography.bodySmall,
            color = c.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().entrance(3).padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val c = Kp.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = c.text)
    }
}
