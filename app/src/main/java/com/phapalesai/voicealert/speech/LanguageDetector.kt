package com.phapalesai.voicealert.speech

import java.util.Locale

object LanguageDetector {

    /**
     * Inspects text content for Unicode Devanagari range (\u0900 - \u097F).
     * Returns "mr" for Marathi indicators, "hi" for Hindi indicators, or "en" as fallback.
     */
    fun detectLanguage(text: String, userPreference: String): Locale {
        if (userPreference != "auto") {
            return when (userPreference) {
                "hi" -> Locale("hi", "IN")
                "mr" -> Locale("mr", "IN")
                else -> Locale("en", "IN")
            }
        }

        // Devanagari script detection
        val devanagariCount = text.count { c -> c.code in 0x0900..0x097F }
        val isDevanagari = devanagariCount > 2 || (text.isNotEmpty() && devanagariCount.toDouble() / text.length > 0.2)

        if (!isDevanagari) {
            return Locale("en", "IN")
        }

        // Specific Marathi keywords check vs Hindi
        val marathiKeywords = listOf("आहेस", "झाले", "करा", "नाही", "आलो", "गेलो", "खात्यात", "मिळाले", "नमस्कार")
        val isMarathi = marathiKeywords.any { keyword -> text.contains(keyword, ignoreCase = true) }

        return if (isMarathi) {
            Locale("mr", "IN")
        } else {
            Locale("hi", "IN")
        }
    }
}
