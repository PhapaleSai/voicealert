package com.phapalesai.voicealert.privacy

import com.phapalesai.voicealert.data.DeviceType

object PrivacyFilter {

    fun processNotificationText(
        packageName: String,
        appName: String,
        title: String,
        text: String,
        language: String,
        deviceType: DeviceType,
        globalPrivacyMode: Boolean
    ): String? {

        // Fully switched off: no voice announcements at all.
        if (deviceType == DeviceType.SILENT) {
            return null
        }

        // Shared/public speakers (e.g. a home speaker) still speak, but quieter and
        // sender-only, to avoid broadcasting private message content out loud.
        if (deviceType == DeviceType.SHARED) {
            return when (language) {
                "hi" -> "$appName से संदेश।"
                "mr" -> "$appName कडून संदेश."
                else -> "New message from $appName."
            }
        }

        if (deviceType == DeviceType.CAR && globalPrivacyMode) {
            // Read sender only, no text content in car
            return when (language) {
                "hi" -> "$appName से संदेश।"
                "mr" -> "$appName कडून संदेश."
                else -> "New message from $title."
            }
        }

        // 1. Check for OTP protection
        if (OtpDetector.containsOtp(text) || OtpDetector.containsOtp(title)) {
            return OtpDetector.sanitizeOtpMessage(appName, language)
        }

        // 2. Check for Banking privacy
        if (MultilingualBankParser.isBankNotification(packageName, text)) {
            return MultilingualBankParser.parseBankMessage(appName, text, language)
        }

        // 3. Privacy Mode (Sender Only)
        if (globalPrivacyMode) {
            return when (language) {
                "hi" -> "$appName संदेश $title से।"
                "mr" -> "$appName वरून $title चा संदेश."
                else -> "$appName message from $title."
            }
        }

        // 4. Default Clean Announcement
        val cleanedText = text.take(150) // Limit text length for comfortable listening
        return if (title.isNotBlank()) {
            when (language) {
                "hi" -> "$appName: $title कहते हैं, $cleanedText"
                "mr" -> "$appName: $title म्हणतात, $cleanedText"
                else -> "$appName from $title: $cleanedText"
            }
        } else {
            "$appName: $cleanedText"
        }
    }
}
