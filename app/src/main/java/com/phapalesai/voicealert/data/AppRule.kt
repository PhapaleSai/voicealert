package com.phapalesai.voicealert.data

data class AppRule(
    val packageName: String,
    val displayName: String,
    val category: String, // "Messaging", "Banking", "Navigation", "Social", "Other"
    val enabled: Boolean = true,
    val priority: Priority = Priority.NORMAL,
    val readSender: Boolean = true,
    val readContent: Boolean = true
)
