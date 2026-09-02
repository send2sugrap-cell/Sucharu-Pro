package com.sucharu.sucharupro.domain.model.communication.vendor.document

import java.util.concurrent.TimeUnit

/**
 * Centralized, configurable policy engine for Vendor Document Expiry and Renewal detection (Module 10 Step 06).
 */
data class VendorDocumentExpiryPolicy(
    val reminderThresholdsInDays: List<Int> = listOf(90, 60, 30, 7),
    val expiringSoonThresholdDays: Int = 30
) {
    /**
     * Determines whether a document is a candidate for renewal reminder at [now].
     */
    fun isEligibleForReminder(
        document: VendorDocument,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val exp = document.expiryDate ?: return false
        if (document.status == VendorDocumentStatus.CANCELLED ||
            document.status == VendorDocumentStatus.REJECTED ||
            document.status == VendorDocumentStatus.REQUESTED
        ) {
            return false
        }

        val diffMillis = exp - now
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

        // Expired or matching one of the notification windows
        return daysRemaining <= expiringSoonThresholdDays
    }

    /**
     * Generates standard idempotency key for renewal reminders to prevent duplicate notifications.
     */
    fun generateReminderIdempotencyKey(
        projectId: String,
        documentId: String,
        daysRemainingWindow: Int
    ): String = "VREM-$projectId-$documentId-$daysRemainingWindow"

    /**
     * Categorizes document expiry status.
     */
    fun evaluateStatus(
        document: VendorDocument,
        now: Long = System.currentTimeMillis()
    ): VendorDocumentExpiryStatus {
        val exp = document.expiryDate ?: return VendorDocumentExpiryStatus.NO_EXPIRY
        val diff = exp - now
        return when {
            diff <= 0 -> VendorDocumentExpiryStatus.EXPIRED
            TimeUnit.MILLISECONDS.toDays(diff) <= expiringSoonThresholdDays -> VendorDocumentExpiryStatus.EXPIRING_SOON
            else -> VendorDocumentExpiryStatus.VALID
        }
    }
}
