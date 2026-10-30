package com.kuralpay.app.assistant

import android.content.Intent
import android.speech.RecognizerIntent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandHandler @Inject constructor() {

    fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask about your payments")
        }
    }
}
