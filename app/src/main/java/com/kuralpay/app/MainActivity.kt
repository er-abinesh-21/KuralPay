package com.kuralpay.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.kuralpay.app.lockscreenoverlay.OverlayService
import com.kuralpay.app.navigation.KuralPayNavGraph
import com.kuralpay.app.ui.components.IconTile
import com.kuralpay.app.ui.components.VoiceWave
import com.kuralpay.app.ui.components.entrance
import com.kuralpay.app.ui.components.kpCard
import com.kuralpay.app.ui.components.kpClickable
import com.kuralpay.app.ui.theme.Kp
import com.kuralpay.app.ui.theme.KuralPayTheme
import com.kuralpay.app.voice.tts.AnnouncementService
import com.kuralpay.app.widgets.WidgetUpdateWorker
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuralpay.app.account.AuthScreen
import com.kuralpay.app.account.AuthViewModel
import com.kuralpay.app.summary.DailySummaryScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private enum class Stage { SIGN_IN, PERMISSIONS, APP }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var dailySummaryScheduler: DailySummaryScheduler

    private var overlayGranted by mutableStateOf(false)
    private var listenerGranted by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        overlayGranted = Settings.canDrawOverlays(this)
        listenerGranted = isNotificationListenerEnabled()
        NotificationHelper.createChannels(this)
        requestRuntimePermissions()
        WidgetUpdateWorker.schedule(this)
        dailySummaryScheduler.syncWithSettings()

        setContent {
            KuralPayTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val signedIn by authViewModel.signedIn.collectAsState()
                // First launch needs an account. The login is remembered, so later launches work offline.
                // Builds without google-services.json skip this so the app still runs.
                val needsSignIn = authViewModel.configured && !signedIn

                val stage = when {
                    needsSignIn -> Stage.SIGN_IN
                    overlayGranted && listenerGranted -> Stage.APP
                    else -> Stage.PERMISSIONS
                }
                AnimatedContent(
                    targetState = stage,
                    transitionSpec = { (fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.97f)) togetherWith fadeOut(tween(200)) },
                    label = "stage"
                ) { current ->
                    when (current) {
                        Stage.SIGN_IN -> AuthScreen(showBack = false)
                        Stage.APP -> KuralPayNavGraph(navController = rememberNavController())
                        Stage.PERMISSIONS -> Onboarding(
                            listenerGranted = listenerGranted,
                            overlayGranted = overlayGranted,
                            onAllowListener = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                            onAllowOverlay = {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = Settings.canDrawOverlays(this)
        listenerGranted = isNotificationListenerEnabled()
        startServices()
    }

    private fun requestRuntimePermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (permissions.isNotEmpty()) requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startServices() {
        if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, OverlayService::class.java))
        }
        startService(Intent(this, AnnouncementService::class.java))
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(packageName) == true
    }
}

@Composable
private fun Onboarding(
    listenerGranted: Boolean,
    overlayGranted: Boolean,
    onAllowListener: () -> Unit,
    onAllowOverlay: () -> Unit
) {
    val c = Kp.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.size(8.dp))
        Column(
            Modifier.fillMaxWidth().entrance(0),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier
                    .size(width = 220.dp, height = 204.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.kp_logo_full), contentDescription = "KuralPay, the voice of digital payments", modifier = Modifier.fillMaxSize())
            }
            Text(
                "Hear every UPI payment the moment it lands. Two permissions and you're ready.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.muted,
                textAlign = TextAlign.Center
            )
            VoiceWave(color = c.accent, bars = 7, barWidth = 4.dp, height = 36.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PermissionStep(
                Icons.Rounded.NotificationsActive,
                "Read payment alerts",
                "So KuralPay can hear Google Pay, PhonePe, Paytm and BHIM",
                listenerGranted,
                onAllowListener,
                Modifier.entrance(1)
            )
            PermissionStep(
                Icons.Rounded.Layers,
                "Show over other apps",
                "For the payment card on your lock screen",
                overlayGranted,
                onAllowOverlay,
                Modifier.entrance(2)
            )
            Text(
                "Turn both on in the screen that opens, then come back here.",
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).entrance(3)
            )
        }
    }
}

@Composable
private fun PermissionStep(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Kp.colors
    val chipBg by animateColorAsState(if (granted) c.good.copy(alpha = 0.16f) else c.royal, label = "stepBg")
    val chipFg by animateColorAsState(if (granted) c.good else c.onRoyal, label = "stepFg")
    Row(
        modifier.fillMaxWidth().kpCard(20.dp).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconTile(icon, c.royal)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
        Text(
            if (granted) "Allowed" else "Allow",
            style = MaterialTheme.typography.labelMedium,
            color = chipFg,
            modifier = Modifier
                .then(if (granted) Modifier else Modifier.kpClickable(onAllow))
                .clip(CircleShape)
                .background(chipBg)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
