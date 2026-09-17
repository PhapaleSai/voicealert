package com.phapalesai.voicealert.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.phapalesai.voicealert.data.SpeakMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var isInitialized = false

    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow

    private val _lastSpokenTextFlow = MutableStateFlow("")
    val lastSpokenTextFlow: StateFlow<String> = _lastSpokenTextFlow

    private var focusRequest: AudioFocusRequest? = null

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
                    abandonAudioFocus()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeakingFlow.value = false
                    abandonAudioFocus()
                }
            })
            Log.d("TTSManager", "TTS Engine initialized successfully")
        } else {
            Log.e("TTSManager", "TTS Initialization failed with status: $status")
        }
    }

    /**
     * Speaks [text] at [volume] (0f-1f), respecting [mode]:
     * - SPEAK_OVER: ducks/interrupts any currently playing audio (music, calls) and speaks immediately.
     * - SPEAK: only speaks if nothing else is actively playing audio; otherwise skips.
     * - DISABLED: never called for disabled apps (callers should filter this out beforehand).
     */
    fun speak(text: String, targetLocale: Locale, volume: Float = 1f, mode: SpeakMode = SpeakMode.SPEAK) {
        if (!isInitialized || tts == null) {
            Log.w("TTSManager", "TTS engine not ready yet")
            return
        }

        if (mode == SpeakMode.SPEAK && audioManager.isMusicActive) {
            Log.d("TTSManager", "Other audio is playing and mode is SPEAK (not SPEAK_OVER); skipping.")
            return
        }

        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TTSManager", "Locale $targetLocale not supported, falling back to en_IN")
            tts?.language = Locale("en", "IN")
        }

        requestAudioFocus(duck = mode == SpeakMode.SPEAK_OVER)

        val clampedVolume = volume.coerceIn(0f, 1f)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, clampedVolume)
        }

        _lastSpokenTextFlow.value = text
        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, System.currentTimeMillis().toString())
    }

    private fun requestAudioFocus(duck: Boolean) {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val focusGain = if (duck) {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        } else {
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        }

        val request = AudioFocusRequest.Builder(focusGain)
            .setAudioAttributes(attributes)
            .build()

        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    fun stop() {
        tts?.stop()
        _isSpeakingFlow.value = false
        abandonAudioFocus()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        abandonAudioFocus()
    }
}
