package com.phapalesai.voicealert.notification

import android.service.notification.StatusBarNotification
import com.phapalesai.voicealert.data.DeviceType
import com.phapalesai.voicealert.data.NotificationEvent
import com.phapalesai.voicealert.privacy.PrivacyFilter
import com.phapalesai.voicealert.speech.LanguageDetector

object NotificationProcessor {

    fun processNotification(
        sbn: StatusBarNotification,
        userPreferredLanguage: String,
        currentDeviceType: DeviceType,
        privacyModeEnabled: Boolean
    ): NotificationEvent? {
        val packageName = sbn.packageName ?: return null

        // Ignore Android system noise notifications
        if (packageName == "android" || packageName == "com.android.systemui") {
            return null
        }

        val extras = sbn.notification?.extras ?: return null
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return null

        val appName = packageName.substringAfterLast(".").capitalize()

        // 1. Detect language
        val targetLocale = LanguageDetector.detectLanguage("$title $text", userPreferredLanguage)
        val langCode = targetLocale.language

        // 2. Filter privacy & device context
        val spokenText = PrivacyFilter.processNotificationText(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            language = langCode,
            deviceType = currentDeviceType,
            globalPrivacyMode = privacyModeEnabled
        ) ?: return null

        return NotificationEvent(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            spokenText = spokenText,
            language = langCode
        )
    }
}
