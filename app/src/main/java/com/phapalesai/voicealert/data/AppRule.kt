package com.phapalesai.voicealert.data

enum class SpeakMode {
    SPEAK_OVER, // Duck/interrupt other audio (music, calls-in-background) and speak immediately
    SPEAK,      // Queue normally, waiting for other audio to finish
    DISABLED    // Never speak notifications from this app
}

data class AppRule(
    val packageName: String,
    val displayName: String,
    val mode: SpeakMode = SpeakMode.SPEAK
)
