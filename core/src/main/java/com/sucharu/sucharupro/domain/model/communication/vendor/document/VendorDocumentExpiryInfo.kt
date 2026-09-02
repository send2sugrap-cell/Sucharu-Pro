package com.sucharu.sucharupro.domain.model.communication.vendor.document

import java.util.concurrent.TimeUnit

/**
 * Calculated expiry evaluation model for a vendor document (Module 10 Step 06).
 */
data class VendorDocumentExpiryInfo(
    val documentId: String,
    val documentNo: String,
    val projectId: String,
    val vendorId: String,
    val documentType: VendorDocumentType,
    val title: String,
    val issueDate: Long?,
    val expiryDate: Long?,
    val daysUntilExpiry: Long?,
    val expiryStatus: VendorDocumentExpiryStatus
) {
    companion object {
        fun calculate(
            document: VendorDocument,
            now: Long = System.currentTimeMillis(),
            expiringSoonThresholdDays: Long = 30L
        ): VendorDocumentExpiryInfo {
            val exp = document.expiryDate
            if (exp == null) {
                return VendorDocumentExpiryInfo(
                    documentId = document.documentId,
                    documentNo = document.documentNo,
                    projectId = document.projectId,
                    vendorId = document.vendorId,
                    documentType = document.documentType,
                    title = document.title,
                    issueDate = document.issueDate,
                    expiryDate = null,
                    daysUntilExpiry = null,
                    expiryStatus = VendorDocumentExpiryStatus.NO_EXPIRY
                )
            }

            val diffMillis = exp - now
            val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

            val status = when {
                diffMillis <= 0 -> VendorDocumentExpiryStatus.EXPIRED
                days <= expiringSoonThresholdDays -> VendorDocumentExpiryStatus.EXPIRING_SOON
                else -> VendorDocumentExpiryStatus.VALID
            }

            return VendorDocumentExpiryInfo(
                documentId = document.documentId,
                documentNo = document.documentNo,
                projectId = document.projectId,
                vendorId = document.vendorId,
                documentType = document.documentType,
                title = document.title,
                issueDate = document.issueDate,
                expiryDate = exp,
                daysUntilExpiry = days,
                expiryStatus = status
            )
        }
    }
}
