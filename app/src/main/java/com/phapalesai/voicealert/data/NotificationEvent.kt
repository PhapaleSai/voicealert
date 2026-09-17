package com.phapalesai.voicealert.data

data class NotificationEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val spokenText: String,
    val language: String, // "en", "hi", "mr"
    val timestamp: Long = System.currentTimeMillis(),
    val priority: Priority = Priority.NORMAL,
    val isSpoken: Boolean = true
)

enum class Priority {
    CRITICAL, // Calls, Navigation
    HIGH,     // Banking, Security, OTP
    NORMAL,   // WhatsApp, SMS, Messages
    LOW       // Games, Social
}
