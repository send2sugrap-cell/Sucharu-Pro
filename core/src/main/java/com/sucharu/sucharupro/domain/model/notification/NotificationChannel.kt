package com.sucharu.sucharupro.domain.model.notification

/**
 * Supported communication delivery channels (Module 10 Step 01).
 */
enum class NotificationChannel(val defaultLabel: String, val isDirectUserChannel: Boolean) {
    IN_APP("In-App Notification", true),
    PUSH("Push Notification", true),
    EMAIL("Email", true),
    SMS("SMS Text", true),
    WHATSAPP("WhatsApp Message", true),
    SYSTEM("Internal System Alert", false)
}
