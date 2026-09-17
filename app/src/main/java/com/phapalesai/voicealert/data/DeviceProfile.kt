package com.phapalesai.voicealert.data

enum class DeviceType {
    PERSONAL, // Personal Earbuds / Headphones -> Spoken notifications ON, full volume
    CAR,      // Car Bluetooth System -> Sender-only in privacy mode, full volume
    SHARED,   // Home speaker / public -> Spoken, sender-only, quiet volume
    SILENT,   // Completely switched off -> no voice announcements at all
    OTHER     // Default
}

/** Playback volume (0f-1f) applied to spoken alerts for each device type. */
fun DeviceType.defaultVolume(): Float = when (this) {
    DeviceType.PERSONAL -> 1.0f
    DeviceType.CAR -> 0.85f
    DeviceType.SHARED -> 0.35f
    DeviceType.SILENT -> 0.0f
    DeviceType.OTHER -> 1.0f
}

data class DeviceProfile(
    val address: String,
    val name: String,
    val type: DeviceType = DeviceType.PERSONAL,
    val voiceEnabled: Boolean = true,
    val volume: Float = type.defaultVolume(),
    val allowCalls: Boolean = true,
    val allowNavigation: Boolean = true,
    val privacyModeEnabled: Boolean = false,
    /** 0-100, or null if the device doesn't report battery level (many A2DP devices don't). */
    val batteryLevel: Int? = null
)
