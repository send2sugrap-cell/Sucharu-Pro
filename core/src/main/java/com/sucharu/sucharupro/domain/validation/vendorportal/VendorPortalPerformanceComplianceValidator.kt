package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.ComplianceStatus
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * Domain Validator for Vendor Portal Performance & Compliance Workspace (Module 13 Step 08).
 */
object VendorPortalPerformanceComplianceValidator {

    /**
     * Validates an Evaluation Response input.
     */
    fun validateEvaluationResponse(
        tenantId: String,
        projectId: String,
        vendorId: String,
        evaluationId: String,
        subject: String,
        remarks: String,
        actorId: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (vendorId.isBlank()) return DomainResult.Error(message = "Vendor ID cannot be blank.")
        if (evaluationId.isBlank()) return DomainResult.Error(message = "Evaluation ID cannot be blank.")
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")
        if (subject.isBlank()) return DomainResult.Error(message = "Response subject cannot be blank.")
        if (subject.length > 255) return DomainResult.Error(message = "Response subject exceeds maximum allowed length of 255 characters.")
        if (remarks.isBlank()) return DomainResult.Error(message = "Response remarks cannot be blank.")
        if (remarks.length < 5) return DomainResult.Error(message = "Response remarks must be at least 5 characters.")
        return DomainResult.Success(Unit)
    }

    /**
     * Validates Compliance Evidence Upload input.
     */
    fun validateComplianceEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        fileName: String,
        fileUrl: String,
        actorId: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (vendorId.isBlank()) return DomainResult.Error(message = "Vendor ID cannot be blank.")
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")
        if (fileName.isBlank()) return DomainResult.Error(message = "File name cannot be blank.")
        if (fileUrl.isBlank()) return DomainResult.Error(message = "File URL / reference cannot be blank.")
        return DomainResult.Success(Unit)
    }

    /**
     * Validates Corrective Action (CAPA) Response input.
     */
    fun validateCorrectiveActionResponse(
        tenantId: String,
        projectId: String,
        vendorId: String,
        actionId: String,
        remediationNotes: String,
        progressPercentage: Double,
        actorId: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (vendorId.isBlank()) return DomainResult.Error(message = "Vendor ID cannot be blank.")
        if (actionId.isBlank()) return DomainResult.Error(message = "Action ID cannot be blank.")
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")
        if (remediationNotes.isBlank()) return DomainResult.Error(message = "Remediation notes cannot be blank.")
        if (remediationNotes.length < 5) return DomainResult.Error(message = "Remediation notes must be at least 5 characters.")
        if (progressPercentage < 0.0 || progressPercentage > 100.0) {
            return DomainResult.Error(message = "Progress percentage must be between 0.0 and 100.0.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Computes percentage safely with deterministic HALF_UP rounding.
     */
    fun calculatePercentage(numerator: Number, denominator: Number): Double {
        val num = numerator.toDouble()
        val den = denominator.toDouble()
        if (den <= 0.0 || num <= 0.0) return 0.0
        val raw = (num / den) * 100.0
        return BigDecimal(raw).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Resolves Expiry Alert Level given days remaining until expiration.
     */
    fun resolveExpiryAlertLevel(expiryEpochMillis: Long?, nowEpochMillis: Long = System.currentTimeMillis()): VendorPortalExpiryAlertLevel {
        if (expiryEpochMillis == null) return VendorPortalExpiryAlertLevel.NORMAL
        val millisRemaining = expiryEpochMillis - nowEpochMillis
        val daysRemaining = millisRemaining / (1000 * 60 * 60 * 24)
        return when {
            daysRemaining < 0 -> VendorPortalExpiryAlertLevel.EXPIRED
            daysRemaining <= 7 -> VendorPortalExpiryAlertLevel.CRITICAL_7_DAYS
            daysRemaining <= 30 -> VendorPortalExpiryAlertLevel.UPCOMING_30_DAYS
            else -> VendorPortalExpiryAlertLevel.NORMAL
        }
    }

    /**
     * Resolves Days Remaining until expiration.
     */
    fun calculateDaysRemaining(expiryEpochMillis: Long?, nowEpochMillis: Long = System.currentTimeMillis()): Long? {
        if (expiryEpochMillis == null) return null
        val millisRemaining = expiryEpochMillis - nowEpochMillis
        return millisRemaining / (1000 * 60 * 60 * 24)
    }
}
