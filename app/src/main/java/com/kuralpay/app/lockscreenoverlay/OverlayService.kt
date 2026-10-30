package com.kuralpay.app.lockscreenoverlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kuralpay.app.NotificationHelper
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.domain.model.ParsedPayment
import com.kuralpay.app.lockscreenoverlay.ui.PaymentOverlayContent
import com.kuralpay.app.paymentdetection.PaymentEventBus
import com.kuralpay.app.ui.theme.KuralPayTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject lateinit var settingsRepository: SettingsRepository

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var hideJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        observePayments()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        startForeground(
            1,
            NotificationHelper.buildForegroundNotification(
                this,
                NotificationHelper.OVERLAY_CHANNEL_ID,
                "KuralPay Overlay",
                "Payment overlay is active"
            )
        )
        return START_STICKY
    }

    private fun observePayments() {
        scope.launch {
            PaymentEventBus.events.collect { payment ->
                if (settingsRepository.overlayEnabled.first()) {
                    showOverlay(payment)
                }
            }
        }
    }

    private fun showOverlay(payment: ParsedPayment) {
        hideOverlay()
        val overlay = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                KuralPayTheme {
                    PaymentOverlayContent(payment, onDismiss = { hideOverlay() })
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            // Not FLAG_NOT_TOUCHABLE: Android 12+ caps pass-through overlays at 80% opacity.
            // NOT_TOUCH_MODAL keeps taps outside the card reaching the app underneath.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(overlay, params)
        overlayView = overlay

        hideJob?.cancel()
        hideJob = scope.launch {
            val duration = settingsRepository.overlayDuration.first().toLong()
            delay(duration)
            hideOverlay()
        }
    }

    private fun hideOverlay() {
        hideJob?.cancel()
        hideJob = null
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        hideOverlay()
        super.onDestroy()
    }
}
