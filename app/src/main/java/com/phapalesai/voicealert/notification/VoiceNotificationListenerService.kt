package com.phapalesai.voicealert.notification

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.phapalesai.voicealert.VoiceAlertApp
import com.phapalesai.voicealert.data.DeviceType
import com.phapalesai.voicealert.data.NotificationEvent
import com.phapalesai.voicealert.speech.LanguageDetector
import com.phapalesai.voicealert.speech.TTSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VoiceNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var ttsManager: TTSManager? = null

    override fun onCreate() {
        super.onCreate()
        ttsManager = TTSManager(applicationContext)
        activeService = this
        Log.d("VoiceListener", "VoiceNotificationListenerService Created")
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager?.shutdown()
        activeService = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        scope.launch {
            val repository = VoiceAlertApp.instance.preferencesRepository
            val travelModeActive = repository.travelModeFlow.first()
            if (!travelModeActive) {
                Log.d("VoiceListener", "Travel Mode is inactive. Skipping notification.")
                return@launch
            }

            val preferredLang = repository.preferredLanguageFlow.first()
            val privacyMode = repository.privacyModeFlow.first()

            val event = NotificationProcessor.processNotification(
                sbn = sbn,
                userPreferredLanguage = preferredLang,
                currentDeviceType = DeviceType.PERSONAL,
                privacyModeEnabled = privacyMode
            )

            if (event != null) {
                Log.d("VoiceListener", "Speaking alert: ${event.spokenText}")
                val locale = LanguageDetector.detectLanguage(event.spokenText, preferredLang)
                ttsManager?.speak(event.spokenText, locale)
                
                // Post to global recent alerts list
                _recentEventsFlow.value = listOf(event) + _recentEventsFlow.value.take(20)
            }
        }
    }

    fun speakTestMessage(text: String, langCode: String) {
        val locale = LanguageDetector.detectLanguage(text, langCode)
        ttsManager?.speak(text, locale)
    }

    companion object {
        var activeService: VoiceNotificationListenerService? = null
            private set

        private val _recentEventsFlow = MutableStateFlow<List<NotificationEvent>>(emptyList())
        val recentEventsFlow: StateFlow<List<NotificationEvent>> = _recentEventsFlow
    }
}
