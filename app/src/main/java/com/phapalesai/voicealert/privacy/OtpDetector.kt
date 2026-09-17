package com.phapalesai.voicealert.privacy

object OtpDetector {

    private val otpKeywords = listOf(
        "otp", "one time password", "verification code", "auth code",
        "ओटीपी", "वन टाइम पासवर्ड", "सत्यापन कोड", "ओ.टी.पी"
    )

    fun containsOtp(text: String): Boolean {
        val lower = text.lowercase()
        return otpKeywords.any { keyword -> lower.contains(keyword) } ||
               text.contains(Regex("""\b\d{4,8}\b""")) && (lower.contains("code") || lower.contains("login") || lower.contains("पासवर्ड"))
    }

    fun sanitizeOtpMessage(appName: String, language: String): String {
        return when (language) {
            "hi" -> "$appName से सुरक्षा कोड प्राप्त हुआ।"
            "mr" -> "$appName कडून सुरक्षा कोड प्राप्त झाला."
            else -> "Security OTP received from $appName."
        }
    }
}
