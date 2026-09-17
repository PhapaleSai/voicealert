package com.phapalesai.voicealert.notification

import android.content.Context
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import com.phapalesai.voicealert.VoiceAlertApp
import com.phapalesai.voicealert.data.DeviceType
import com.phapalesai.voicealert.data.NotificationEvent
import com.phapalesai.voicealert.data.SpeakMode
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

    // Tracks the notification currently being spoken, so we can stop mid-sentence
    // if the user dismisses or opens it (they've already seen/heard what they need to).
    private var currentlySpokenKey: String? = null

    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var legacyPhoneStateListener: PhoneStateListener? = null

    override fun onCreate() {
        super.onCreate()
        ttsManager = TTSManager(applicationContext)
        activeService = this
        registerCallStateWatcher()
        Log.d("VoiceListener", "VoiceNotificationListenerService Created")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterCallStateWatcher()
        ttsManager?.shutdown()
        activeService = null
    }

    /** Stops speaking the instant a call rings or is answered — a live call always wins. */
    private fun registerCallStateWatcher() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        telephonyManager = tm

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    onCallStateChanged(state)
                }
            }
            try {
                tm.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallback = callback
            } catch (e: SecurityException) {
                Log.w("VoiceListener", "Missing permission to observe call state", e)
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    this@VoiceNotificationListenerService.onCallStateChanged(state)
                }
            }
            try {
                @Suppress("DEPRECATION")
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                legacyPhoneStateListener = listener
            } catch (e: SecurityException) {
                Log.w("VoiceListener", "Missing permission to observe call state", e)
            }
        }
    }

    private fun unregisterCallStateWatcher() {
        val tm = telephonyManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { tm.unregisterTelephonyCallback(it) }
        } else {
            @Suppress("DEPRECATION")
            legacyPhoneStateListener?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
        }
    }

    private fun onCallStateChanged(state: Int) {
        if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK) {
            Log.d("VoiceListener", "Call ringing/answered — stopping voice alerts.")
            ttsManager?.stop()
            currentlySpokenKey = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName

        scope.launch {
            val app = VoiceAlertApp.instance
            val repository = app.preferencesRepository

            val travelModeActive = repository.travelModeFlow.first()
            if (!travelModeActive) {
                Log.d("VoiceListener", "Travel Mode is inactive. Skipping notification.")
                return@launch
            }

            val appRules = repository.appRulesFlow.first()
            val speakMode = appRules[packageName]?.let {
                try { SpeakMode.valueOf(it) } catch (e: IllegalArgumentException) { SpeakMode.SPEAK }
            } ?: SpeakMode.SPEAK

            if (speakMode == SpeakMode.DISABLED) {
                Log.d("VoiceListener", "App $packageName is disabled for voice alerts. Skipping.")
                return@launch
            }

            val device = app.bluetoothManager.connectedDeviceFlow.value
            if (device != null && !device.voiceEnabled) {
                Log.d("VoiceListener", "Current device (${device.name}) has voice fully disabled. Skipping.")
                return@launch
            }

            val preferredLang = repository.preferredLanguageFlow.first()
            val privacyMode = repository.privacyModeFlow.first()

            val event = NotificationProcessor.processNotification(
                sbn = sbn,
                userPreferredLanguage = preferredLang,
                currentDeviceType = device?.type ?: DeviceType.PERSONAL,
                privacyModeEnabled = privacyMode
            )

            if (event != null) {
                Log.d("VoiceListener", "Speaking alert: ${event.spokenText}")
                val locale = LanguageDetector.detectLanguage(event.spokenText, preferredLang)
                val volume = device?.volume ?: 1f
                currentlySpokenKey = sbn.key
                ttsManager?.speak(event.spokenText, locale, volume, speakMode)

                // Post to global recent alerts list
                _recentEventsFlow.value = listOf(event) + _recentEventsFlow.value.take(20)
            }
        }
    }

    /**
     * Fires when a notification leaves the shade — the user swiped it away, opened the app
     * from it, or the posting app cancelled it. Any of those means they've already seen it,
     * so if we're still reading it aloud, cut the speech short instead of finishing the sentence.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn != null && sbn.key == currentlySpokenKey) {
            Log.d("VoiceListener", "Notification ${sbn.key} was read/dismissed — stopping speech.")
            ttsManager?.stop()
            currentlySpokenKey = null
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
