package com.phapalesai.voicealert.data

enum class DeviceType {
    PERSONAL, // Personal Earbuds / Headphones -> Spoken notifications ON
    CAR,      // Car Bluetooth System -> Private notifications OFF
    SHARED,   // Speaker / Public -> Spoken notifications OFF
    OTHER     // Default
}

data class DeviceProfile(
    val address: String,
    val name: String,
    val type: DeviceType = DeviceType.PERSONAL,
    val voiceEnabled: Boolean = true,
    val allowCalls: Boolean = true,
    val allowNavigation: Boolean = true,
    val privacyModeEnabled: Boolean = false
)
