package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalRole

/**
 * Granular feature visibility flags for Vendor Portal Performance & Compliance workspace.
 */
data class VendorPortalPerformanceComplianceVisibility(
    val canViewOverview: Boolean = true,
    val canViewScorecards: Boolean = true,
    val canViewDetailedKpis: Boolean = true,
    val canViewEvaluations: Boolean = true,
    val canRespondToEvaluations: Boolean = false,
    val canViewComplianceRecords: Boolean = true,
    val canUploadComplianceEvidence: Boolean = false,
    val canViewCorrectiveActions: Boolean = true,
    val canRespondToCorrectiveActions: Boolean = false,
    val canViewAuditTrail: Boolean = false
)

/**
 * Security policy determining role-specific visibility in Performance & Compliance workspace.
 */
object VendorPortalPerformanceComplianceVisibilityPolicy {

    fun resolveVisibility(role: VendorPortalRole): VendorPortalPerformanceComplianceVisibility {
        return when (role) {
            VendorPortalRole.VENDOR_ADMIN -> VendorPortalPerformanceComplianceVisibility(
                canViewOverview = true,
                canViewScorecards = true,
                canViewDetailedKpis = true,
                canViewEvaluations = true,
                canRespondToEvaluations = true,
                canViewComplianceRecords = true,
                canUploadComplianceEvidence = true,
                canViewCorrectiveActions = true,
                canRespondToCorrectiveActions = true,
                canViewAuditTrail = true
            )
            VendorPortalRole.VENDOR_QC -> VendorPortalPerformanceComplianceVisibility(
                canViewOverview = true,
                canViewScorecards = true,
                canViewDetailedKpis = true,
                canViewEvaluations = true,
                canRespondToEvaluations = true,
                canViewComplianceRecords = true,
                canUploadComplianceEvidence = true,
                canViewCorrectiveActions = true,
                canRespondToCorrectiveActions = true,
                canViewAuditTrail = false
            )
            VendorPortalRole.VENDOR_LOGISTICS -> VendorPortalPerformanceComplianceVisibility(
                canViewOverview = true,
                canViewScorecards = true,
                canViewDetailedKpis = true,
                canViewEvaluations = true,
                canRespondToEvaluations = false,
                canViewComplianceRecords = true,
                canUploadComplianceEvidence = false,
                canViewCorrectiveActions = true,
                canRespondToCorrectiveActions = true,
                canViewAuditTrail = false
            )
            VendorPortalRole.VENDOR_FINANCE -> VendorPortalPerformanceComplianceVisibility(
                canViewOverview = true,
                canViewScorecards = true,
                canViewDetailedKpis = false,
                canViewEvaluations = true,
                canRespondToEvaluations = false,
                canViewComplianceRecords = true,
                canUploadComplianceEvidence = true,
                canViewCorrectiveActions = false,
                canRespondToCorrectiveActions = false,
                canViewAuditTrail = false
            )
            VendorPortalRole.VENDOR_OPERATOR -> VendorPortalPerformanceComplianceVisibility(
                canViewOverview = true,
                canViewScorecards = true,
                canViewDetailedKpis = true,
                canViewEvaluations = true,
                canRespondToEvaluations = true,
                canViewComplianceRecords = true,
                canUploadComplianceEvidence = false,
                canViewCorrectiveActions = true,
                canRespondToCorrectiveActions = true,
                canViewAuditTrail = false
            )
            VendorPortalRole.VENDOR_VIEWER -> VendorPortalPerformanceComplianceVisibility(
                canViewOverview = true,
                canViewScorecards = true,
                canViewDetailedKpis = false,
                canViewEvaluations = true,
                canRespondToEvaluations = false,
                canViewComplianceRecords = true,
                canUploadComplianceEvidence = false,
                canViewCorrectiveActions = true,
                canRespondToCorrectiveActions = false,
                canViewAuditTrail = false
            )
        }
    }
}
