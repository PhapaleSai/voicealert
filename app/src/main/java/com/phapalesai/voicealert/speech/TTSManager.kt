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

    // Remembers the media stream's volume from before we ducked it, so it can be restored
    // to exactly where the user left it once every queued utterance has finished.
    private var preDuckMusicVolume: Int? = null
    private val pendingUtteranceIds = mutableSetOf<String>()

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
                    onUtteranceFinished(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeakingFlow.value = false
                    onUtteranceFinished(utteranceId)
                }
            })
            Log.d("TTSManager", "TTS Engine initialized successfully")
        } else {
            Log.e("TTSManager", "TTS Initialization failed with status: $status")
        }
    }

    private fun onUtteranceFinished(utteranceId: String?) {
        pendingUtteranceIds.remove(utteranceId)
        if (pendingUtteranceIds.isEmpty()) {
            abandonAudioFocus()
            restoreMediaVolume()
        }
    }

    /**
     * Speaks [text] at [volume] (0f-1f), respecting [mode]:
     * - SPEAK_OVER: ducks any currently playing media and speaks immediately, even breaking
     *   through Quiet Hours / system DND (enforced by the caller).
     * - SPEAK: ducks any currently playing media (music, a YouTube video, etc.), speaks, then
     *   restores it to exactly the volume it was at.
     * - DISABLED: never called for disabled apps (callers should filter this out beforehand).
     */
    fun speak(text: String, targetLocale: Locale, volume: Float = 1f, mode: SpeakMode = SpeakMode.SPEAK) {
        if (!isInitialized || tts == null) {
            Log.w("TTSManager", "TTS engine not ready yet")
            return
        }

        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TTSManager", "Locale $targetLocale not supported, falling back to en_IN")
            tts?.language = Locale("en", "IN")
        }

        requestAudioFocus(duck = mode == SpeakMode.SPEAK_OVER)
        duckMediaVolume()

        val clampedVolume = volume.coerceIn(0f, 1f)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, clampedVolume)
        }

        _lastSpokenTextFlow.value = text
        val utteranceId = System.currentTimeMillis().toString()
        pendingUtteranceIds.add(utteranceId)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
    }

    /** Lowers the media (music/video) stream to ~30% of its current level, remembering the original. */
    private fun duckMediaVolume() {
        if (preDuckMusicVolume != null) return // already ducked for an earlier queued utterance

        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (current <= 0) return // nothing playing at audible volume — leave it alone

        val duckedLevel = (current * 0.3f).toInt().coerceIn(0, current)
        if (duckedLevel < current) {
            preDuckMusicVolume = current
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, duckedLevel, 0)
            } catch (e: SecurityException) {
                Log.w("TTSManager", "Could not adjust media volume", e)
                preDuckMusicVolume = null
            }
        }
    }

    /** Restores the media stream to its pre-duck level, once nothing is left queued to speak. */
    private fun restoreMediaVolume() {
        val original = preDuckMusicVolume ?: return
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, original, 0)
        } catch (e: SecurityException) {
            Log.w("TTSManager", "Could not restore media volume", e)
        }
        preDuckMusicVolume = null
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
        pendingUtteranceIds.clear()
        abandonAudioFocus()
        restoreMediaVolume()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        pendingUtteranceIds.clear()
        abandonAudioFocus()
        restoreMediaVolume()
    }
}
