package com.phapalesai.voicealert.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.ContactsContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.phapalesai.voicealert.VoiceAlertApp
import com.phapalesai.voicealert.data.DeviceProfile
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class VoiceNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var ttsManager: TTSManager? = null

    // Tracks the notification currently being spoken, so we can stop mid-sentence
    // if the user dismisses or opens it (they've already seen/heard what they need to).
    private var currentlySpokenKey: String? = null

    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var legacyPhoneStateListener: PhoneStateListener? = null

    // Caller announcement ("Hey Sai, Mom is calling") needs the incoming number, which
    // TelephonyCallback/PhoneStateListener's state callback doesn't carry — only the
    // ACTION_PHONE_STATE_CHANGED broadcast's EXTRA_INCOMING_NUMBER does (requires READ_CALL_LOG).
    private var lastAnnouncedNumber: String? = null

    // Missed-call tracking: a call that rings and goes back to IDLE without ever reaching
    // OFFHOOK was never answered.
    private var ringingNumber: String? = null
    private var ringingStartTime: Long = 0L
    private var callWasAnswered: Boolean = false

    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(receivedContext: Context, intent: Intent) {
            when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    if (!incomingNumber.isNullOrBlank()) {
                        ringingNumber = incomingNumber
                        ringingStartTime = System.currentTimeMillis()
                        callWasAnswered = false
                        if (incomingNumber != lastAnnouncedNumber) {
                            lastAnnouncedNumber = incomingNumber
                            announceIncomingCall(incomingNumber)
                        }
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    callWasAnswered = true
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    val missedNumber = ringingNumber
                    if (missedNumber != null && !callWasAnswered) {
                        recordMissedCall(missedNumber)
                    }
                    ringingNumber = null
                    lastAnnouncedNumber = null
                }
            }
        }
    }

    /** Announces any calls missed while the phone was locked, the moment the user unlocks it. */
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(receivedContext: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                announcePendingMissedCalls()
            }
        }
    }

    private var sensorManager: SensorManager? = null
    private var lastShakeTime = 0L

    @Volatile private var shakeToStopEnabled = true

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!shakeToStopEnabled) return
            val gX = event.values[0] / SensorManager.GRAVITY_EARTH
            val gY = event.values[1] / SensorManager.GRAVITY_EARTH
            val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
            if (gForce > 2.5f) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 1000) {
                    lastShakeTime = now
                    Log.d("VoiceListener", "Shake detected — stopping speech.")
                    ttsManager?.stop()
                    currentlySpokenKey = null
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate() {
        super.onCreate()
        ttsManager = TTSManager(applicationContext)
        activeService = this
        registerCallStateWatcher()

        try {
            ContextCompat.registerReceiver(this, phoneStateReceiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
            ContextCompat.registerReceiver(this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (e: SecurityException) {
            Log.w("VoiceListener", "Missing permission to observe incoming call number", e)
        }

        sensorManager = (getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.also { sm ->
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sm.registerListener(shakeListener, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }

        val repository = VoiceAlertApp.instance.preferencesRepository
        scope.launch {
            repository.shakeToStopFlow.collect { shakeToStopEnabled = it }
        }
        scope.launch {
            VoiceAlertApp.instance.bluetoothManager.lowBatteryEventFlow.collect { device ->
                announceLowBattery(device)
            }
        }

        Log.d("VoiceListener", "VoiceNotificationListenerService Created")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterCallStateWatcher()
        sensorManager?.unregisterListener(shakeListener)
        try {
            unregisterReceiver(phoneStateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was never registered (e.g. permission was missing at onCreate) — fine to ignore.
        }
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: IllegalArgumentException) {
            // Same as above.
        }
        ttsManager?.shutdown()
        activeService = null
    }

    /** "Hey Sai, Mom is calling" — or just "Mom is calling" if no name is set in Settings. */
    private fun announceIncomingCall(phoneNumber: String) {
        scope.launch {
            val repository = VoiceAlertApp.instance.preferencesRepository

            if (!repository.travelModeFlow.first() || !repository.announceCallerFlow.first()) return@launch

            val device = VoiceAlertApp.instance.bluetoothManager.connectedDeviceFlow.value
            if (device != null && !device.voiceEnabled) return@launch

            val callerName = resolveContactName(phoneNumber) ?: phoneNumber
            val userName = repository.userNameFlow.first()
            val preferredLang = repository.preferredLanguageFlow.first()

            val announcement = if (userName.isNotBlank()) {
                "Hey $userName, $callerName is calling"
            } else {
                "$callerName is calling"
            }

            Log.d("VoiceListener", "Announcing incoming call: $announcement")
            val locale = LanguageDetector.detectLanguage(announcement, preferredLang)
            // SPEAK_OVER: a ringing call is already an interruption, so the announcement should
            // always cut through, at full volume, regardless of what's currently playing.
            ttsManager?.speak(announcement, locale, volume = 1f, mode = SpeakMode.SPEAK_OVER)
        }
    }

    private fun recordMissedCall(phoneNumber: String) {
        scope.launch {
            val repository = VoiceAlertApp.instance.preferencesRepository
            val callerName = resolveContactName(phoneNumber) ?: phoneNumber
            repository.addMissedCall("$callerName|$phoneNumber|${System.currentTimeMillis()}")
            repository.incrementDigestMissedCallCount()
            Log.d("VoiceListener", "Recorded missed call from $callerName")
        }
    }

    /** Speaks "You missed a call from X, N minutes ago" for every call missed since the last unlock. */
    private fun announcePendingMissedCalls() {
        scope.launch {
            val repository = VoiceAlertApp.instance.preferencesRepository
            if (!repository.travelModeFlow.first()) return@launch

            val missed = repository.missedCallsFlow.first()
            if (missed.isEmpty()) return@launch

            val preferredLang = repository.preferredLanguageFlow.first()
            val now = System.currentTimeMillis()

            val sentences = missed.mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size != 3) return@mapNotNull null
                val (name, _, timestampStr) = parts
                val timestamp = timestampStr.toLongOrNull() ?: return@mapNotNull null
                val minutesAgo = ((now - timestamp) / 60000L).coerceAtLeast(0)
                val whenText = when {
                    minutesAgo < 1 -> "just now"
                    minutesAgo == 1L -> "1 minute ago"
                    minutesAgo < 60 -> "$minutesAgo minutes ago"
                    else -> "earlier"
                }
                "You missed a call from $name, $whenText"
            }

            if (sentences.isNotEmpty()) {
                val announcement = sentences.joinToString(". ")
                Log.d("VoiceListener", "Announcing missed calls: $announcement")
                val locale = LanguageDetector.detectLanguage(announcement, preferredLang)
                ttsManager?.speak(announcement, locale, volume = 1f, mode = SpeakMode.SPEAK_OVER)
            }
            repository.clearMissedCalls()
        }
    }

    private fun announceLowBattery(device: DeviceProfile) {
        scope.launch {
            val repository = VoiceAlertApp.instance.preferencesRepository
            if (!repository.travelModeFlow.first() || !repository.lowBatteryAlertFlow.first()) return@launch

            val level = device.batteryLevel ?: return@launch
            val announcement = "${device.name} is at $level percent battery, charge soon"
            Log.d("VoiceListener", "Low battery alert: $announcement")
            val preferredLang = repository.preferredLanguageFlow.first()
            val locale = LanguageDetector.detectLanguage(announcement, preferredLang)
            ttsManager?.speak(announcement, locale, volume = device.volume, mode = SpeakMode.SPEAK)
        }
    }

    /** If a new day has started, speaks a short summary of yesterday's activity and resets counters. */
    private suspend fun checkDailyDigest() {
        val repository = VoiceAlertApp.instance.preferencesRepository
        if (!repository.dailyDigestEnabledFlow.first()) return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastDate = repository.digestLastDateFlow.first()

        if (lastDate.isEmpty()) {
            // First run ever — nothing to summarize yet, just start tracking from today.
            repository.resetDigestForNewDay(today)
            return
        }

        if (lastDate != today) {
            val spokenCount = repository.digestSpokenCountFlow.first()
            val missedCount = repository.digestMissedCallCountFlow.first()

            if (spokenCount > 0 || missedCount > 0) {
                val parts = mutableListOf<String>()
                if (spokenCount > 0) parts.add("$spokenCount notification${if (spokenCount == 1) "" else "s"} spoken")
                if (missedCount > 0) parts.add("$missedCount missed call${if (missedCount == 1) "" else "s"}")
                val announcement = "Yesterday: " + parts.joinToString(", ")

                Log.d("VoiceListener", "Daily digest: $announcement")
                val preferredLang = repository.preferredLanguageFlow.first()
                val locale = LanguageDetector.detectLanguage(announcement, preferredLang)
                ttsManager?.speak(announcement, locale, volume = 1f, mode = SpeakMode.SPEAK)
            }

            repository.resetDigestForNewDay(today)
        }
    }

    private suspend fun resolveContactName(phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                val uri: Uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber)
                )
                val cursor: Cursor? = contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                    null,
                    null,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                    } else {
                        null
                    }
                }
            } catch (e: SecurityException) {
                null
            }
        }
    }

    /** Stops speaking the instant a call rings or is answered — a live call always wins. */
    private fun registerCallStateWatcher() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        telephonyManager = tm

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallStateChanged(state)
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
                    handleCallStateChanged(state)
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

    private fun handleCallStateChanged(state: Int) {
        // Only OFFHOOK (call answered) forcibly silences everything — once the user is actually
        // on a call, nothing should keep talking. RINGING is deliberately left alone here: the
        // caller announcement (see announceIncomingCall) needs to speak *during* the ring, and
        // stopping all TTS on RINGING would race with and potentially kill that announcement.
        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            Log.d("VoiceListener", "Call answered — stopping voice alerts.")
            ttsManager?.stop()
            currentlySpokenKey = null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val rawTitle = sbn.notification?.extras?.getCharSequence("android.title")?.toString() ?: ""

        scope.launch {
            val app = VoiceAlertApp.instance
            val repository = app.preferencesRepository

            checkDailyDigest()

            val travelModeActive = repository.travelModeFlow.first()
            if (!travelModeActive) {
                Log.d("VoiceListener", "Travel Mode is inactive. Skipping notification.")
                return@launch
            }

            // A VIP contact always gets through — bypassing their app's mode, Quiet Hours and DND —
            // matched by comparing the notification's sender/title against the VIP name list.
            val vipContacts = repository.vipContactsFlow.first()
            val isVip = vipContacts.any { vip -> rawTitle.contains(vip, ignoreCase = true) }

            val appRules = repository.appRulesFlow.first()
            var speakMode = appRules[packageName]?.let {
                try { SpeakMode.valueOf(it) } catch (e: IllegalArgumentException) { SpeakMode.SPEAK }
            } ?: SpeakMode.SPEAK

            if (isVip) {
                speakMode = SpeakMode.SPEAK_OVER
            } else if (speakMode == SpeakMode.DISABLED) {
                Log.d("VoiceListener", "App $packageName is disabled for voice alerts. Skipping.")
                return@launch
            }

            // Speak Over apps/VIP contacts bypass Quiet Hours and system DND on purpose.
            if (speakMode != SpeakMode.SPEAK_OVER) {
                if (repository.quietHoursEnabledFlow.first()) {
                    val start = repository.quietHoursStartFlow.first()
                    val end = repository.quietHoursEndFlow.first()
                    if (isWithinQuietHours(start, end)) {
                        Log.d("VoiceListener", "Quiet Hours active ($start:00-$end:00). Skipping.")
                        return@launch
                    }
                }

                if (repository.respectDndFlow.first() && currentInterruptionFilter != INTERRUPTION_FILTER_ALL) {
                    Log.d("VoiceListener", "System Do Not Disturb is active. Skipping.")
                    return@launch
                }
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
                repository.incrementDigestSpokenCount()

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

    /** Re-speaks the most recent alert — handy when it got drowned out by noise the first time. */
    fun repeatLastAlert() {
        val last = _recentEventsFlow.value.firstOrNull() ?: return
        speakTestMessage(last.spokenText, last.language)
    }

    private fun isWithinQuietHours(startHour: Int, endHour: Int): Boolean {
        if (startHour == endHour) return false
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return if (startHour < endHour) {
            currentHour in startHour until endHour
        } else {
            // Window wraps past midnight, e.g. 22 (10 PM) -> 7 (7 AM).
            currentHour >= startHour || currentHour < endHour
        }
    }

    companion object {
        var activeService: VoiceNotificationListenerService? = null
            private set

        private val _recentEventsFlow = MutableStateFlow<List<NotificationEvent>>(emptyList())
        val recentEventsFlow: StateFlow<List<NotificationEvent>> = _recentEventsFlow
    }
}
