package com.phapalesai.voicealert.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow

    private val _lastSpokenTextFlow = MutableStateFlow("")
    val lastSpokenTextFlow: StateFlow<String> = _lastSpokenTextFlow

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale("en", "IN")
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeakingFlow.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeakingFlow.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeakingFlow.value = false
                }
            })
            Log.d("TTSManager", "TTS Engine initialized successfully")
        } else {
            Log.e("TTSManager", "TTS Initialization failed with status: $status")
        }
    }

    fun speak(text: String, targetLocale: Locale) {
        if (!isInitialized || tts == null) {
            Log.w("TTSManager", "TTS engine not ready yet")
            return
        }

        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TTSManager", "Locale $targetLocale not supported, falling back to en_IN")
            tts?.language = Locale("en", "IN")
        }

        _lastSpokenTextFlow.value = text
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, System.currentTimeMillis().toString())
    }

    fun stop() {
        tts?.stop()
        _isSpeakingFlow.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
