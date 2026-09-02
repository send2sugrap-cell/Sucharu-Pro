package com.sucharu.sucharupro.domain.model.notification

/**
 * Priority levels for business notifications (Module 10 Step 01).
 */
enum class NotificationPriority(val weight: Int, val defaultLabel: String) {
    LOW(1, "Low Priority"),
    NORMAL(2, "Normal"),
    HIGH(3, "High Priority"),
    URGENT(4, "Urgent Alert")
}
