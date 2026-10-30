package com.kuralpay.app.voice.tts

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.kuralpay.app.data.repository.SettingsRepository
import com.kuralpay.app.domain.model.VoiceProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    settingsRepository: SettingsRepository
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var forceMaxVolume = false
    /** Media volume before we raised it; null when we haven't touched it. */
    private var volumeBeforeBoost: Int? = null

    init {
        scope.launch { settingsRepository.maxVolume.collect { forceMaxVolume = it } }
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initializing = false
    private val pendingInit = mutableListOf<() -> Unit>()
    private val utteranceQueue = mutableListOf<Utterance>()
    private var isSpeaking = false
    private val activeUtterances = mutableMapOf<String, Utterance>()

    data class Utterance(
        val text: String,
        val profile: VoiceProfile,
        val utteranceId: String = System.nanoTime().toString(),
        val onDone: (() -> Unit)? = null
    )

    /** Safe to call many times; the engine is created once and later callers run as soon as it is ready. */
    fun init(callback: () -> Unit) {
        if (isInitialized) { callback(); return }
        pendingInit += callback
        if (initializing) return
        initializing = true
        // Samsung's default engine ships without Tamil, so prefer Google's engine, which has en-IN and ta-IN voices.
        val engine = GOOGLE_TTS.takeIf { isEngineInstalled(it) }
        tts = TextToSpeech(context, { status ->
            initializing = false
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                // Speak on the media stream so the volume we raise is the one the voice uses.
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onDone(utteranceId: String?) {
                        val cb = synchronized(this@PaymentTtsEngine) {
                            isSpeaking = false
                            utteranceId?.let { activeUtterances.remove(it) }?.onDone
                        }
                        cb?.invoke()
                        processQueue()
                    }
                    override fun onError(utteranceId: String?) {
                        Log.w(TAG, "Speech failed for $utteranceId")
                        synchronized(this@PaymentTtsEngine) {
                            isSpeaking = false
                            utteranceId?.let { activeUtterances.remove(it) }
                        }
                        processQueue()
                    }
                    override fun onStart(utteranceId: String?) {}
                })
                Log.d(TAG, "TTS ready (requested engine: ${engine ?: "system default"})")
                val callbacks = pendingInit.toList()
                pendingInit.clear()
                callbacks.forEach { it() }
            } else {
                Log.w(TAG, "TTS init failed: $status")
            }
        }, engine)
    }

    /** True when the current engine can speak this profile's language right now. Unknown before init counts as ready. */
    fun isVoiceReady(profile: VoiceProfile): Boolean {
        val engine = tts ?: return true
        if (!isInitialized) return true
        val result = engine.isLanguageAvailable(Locale.forLanguageTag(profile.language))
        return result >= TextToSpeech.LANG_AVAILABLE
    }

    /** Opens the installer for missing voice data, in Google's engine when it is present. */
    fun installVoiceIntent(): Intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
        if (isEngineInstalled(GOOGLE_TTS)) setPackage(GOOGLE_TTS)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    // speak() is called from coroutine threads while onDone arrives on the TTS binder thread,
    // so queue access is synchronized; otherwise two utterances can start together and cut each other off.
    /** True while a payment is being announced or waiting to be; the updater never installs during this. */
    val isBusy: Boolean get() = synchronized(this) { isSpeaking || utteranceQueue.isNotEmpty() }

    fun speak(text: String, profile: VoiceProfile, onDone: (() -> Unit)? = null) {
        synchronized(this) { utteranceQueue.add(Utterance(text, profile, onDone = onDone)) }
        processQueue()
    }

    /** The lock only guards the queue; calls into the TTS service happen outside it to avoid deadlocking its callbacks. */
    private fun processQueue() {
        val next: Utterance? = synchronized(this) {
            if (isSpeaking) return
            if (utteranceQueue.isEmpty()) null
            else utteranceQueue.removeFirst().also {
                isSpeaking = true
                activeUtterances[it.utteranceId] = it
            }
        }
        if (next == null) restoreVolume() else speakNow(next)
    }

    private fun boostVolume() {
        if (!forceMaxVolume || volumeBeforeBoost != null) return
        runCatching {
            val stream = AudioManager.STREAM_MUSIC
            volumeBeforeBoost = audioManager.getStreamVolume(stream)
            audioManager.setStreamVolume(stream, audioManager.getStreamMaxVolume(stream), 0)
        }.onFailure {
            volumeBeforeBoost = null
            Log.w(TAG, "Could not raise volume", it)
        }
    }

    private fun restoreVolume() {
        val previous = volumeBeforeBoost ?: return
        volumeBeforeBoost = null
        runCatching { audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previous, 0) }
    }

    private fun speakNow(utterance: Utterance) {
        val engine = tts ?: run {
            synchronized(this) { isSpeaking = false; activeUtterances.remove(utterance.utteranceId) }
            return
        }
        boostVolume()
        val result = engine.setLanguage(Locale.forLanguageTag(utterance.profile.language))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "No ${utterance.profile.language} voice installed (result $result)")
        } else {
            Log.d(TAG, "Speaking in ${engine.voice?.locale} (${engine.voice?.name}): ${utterance.text}")
        }
        engine.setPitch(utterance.profile.pitch)
        engine.setSpeechRate(utterance.profile.speechRate)
        engine.speak(utterance.text, TextToSpeech.QUEUE_FLUSH, null, utterance.utteranceId)
    }

    fun stop() {
        synchronized(this) {
            utteranceQueue.clear()
            activeUtterances.clear()
            isSpeaking = false
        }
        tts?.stop()
        restoreVolume()
    }

    fun shutdown() {
        restoreVolume()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        initializing = false
    }

    private fun isEngineInstalled(pkg: String): Boolean =
        context.packageManager
            .queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
            .any { it.serviceInfo.packageName == pkg }

    private companion object {
        const val TAG = "PaymentTtsEngine"
        const val GOOGLE_TTS = "com.google.android.tts"
    }
}
