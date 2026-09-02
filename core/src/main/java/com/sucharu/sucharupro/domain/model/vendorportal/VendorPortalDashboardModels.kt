package com.sucharu.sucharupro.domain.model.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Domain representations for Vendor Portal Dashboard & Workspace (Module 13 Step 02).
 */

/**
 * Key Performance Indicator metric representation for Vendor Portal.
 */
data class VendorPortalKpi(
    val key: String,
    val label: String,
    val value: String,
    val numericValue: Double? = null,
    val unit: String? = null,
    val trend: String? = null, // e.g. "UP", "DOWN", "STABLE"
    val status: String = "NORMAL", // "GOOD", "NORMAL", "WARNING", "CRITICAL"
    val category: String = "OPERATIONAL" // "OPERATIONAL", "FINANCIAL", "QUALITY", "COMPLIANCE"
)

/**
 * Profile summary projected from Module 12 Vendor Master and Capabilities.
 */
data class VendorPortalProfileSummary(
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val legalName: String? = null,
    val vendorType: String,
    val category: String,
    val status: String,
    val primaryContactName: String? = null,
    val primaryContactEmail: String? = null,
    val primaryContactPhone: String? = null,
    val address: String? = null,
    val portalAccountStatus: String,
    val portalRole: String,
    val projectScope: String,
    val serviceCount: Int = 0,
    val capabilityCount: Int = 0,
    val activeRatesCount: Int = 0
)

/**
 * Operational summary aggregating Work Orders, Purchase Orders, and Delivery Receipts.
 */
data class VendorPortalOperationalSummary(
    val totalPurchaseOrders: Int = 0,
    val activePurchaseOrders: Int = 0,
    val completedPurchaseOrders: Int = 0,
    val totalWorkOrders: Int = 0,
    val openWorkOrders: Int = 0,
    val completedWorkOrders: Int = 0,
    val totalDeliveries: Int = 0,
    val pendingDeliveries: Int = 0,
    val acceptedDeliveries: Int = 0,
    val onTimeDeliveryRatePercent: Double = 0.0,
    val poFulfillmentRatePercent: Double = 0.0
)

/**
 * Financial summary aggregating Invoices, 3-Way Matches, and Settlements.
 */
data class VendorPortalFinancialSummary(
    val totalInvoices: Int = 0,
    val pendingInvoices: Int = 0,
    val approvedInvoices: Int = 0,
    val paidInvoices: Int = 0,
    val disputedInvoices: Int = 0,
    val totalInvoicedAmount: Money = Money.ZERO,
    val totalPaidAmount: Money = Money.ZERO,
    val totalOutstandingPayables: Money = Money.ZERO,
    val totalSettlements: Int = 0,
    val pendingSettlementsCount: Int = 0,
    val lastSettlementDate: Long? = null
)

/**
 * Quality summary aggregating Inspections, Defect Rates, Rejections, and Disputes.
 */
data class VendorPortalQualitySummary(
    val totalInspections: Int = 0,
    val passedInspections: Int = 0,
    val failedInspections: Int = 0,
    val overallDefectRatePercent: Double = 0.0,
    val totalRejections: Int = 0,
    val openRejections: Int = 0,
    val openDisputes: Int = 0,
    val qualityRating: String = "GOOD" // "EXCELLENT", "GOOD", "FAIR", "POOR"
)

/**
 * Performance summary aggregating Vendor Evaluations and Scorecards.
 */
data class VendorPortalPerformanceSummary(
    val overallScore: Double = 100.0,
    val qualityScore: Double = 100.0,
    val deliveryScore: Double = 100.0,
    val pricingScore: Double = 100.0,
    val serviceScore: Double = 100.0,
    val tier: String = "TIER_1", // "TIER_1", "TIER_2", "TIER_3", "PROBATIONARY"
    val evaluationPeriod: String? = null,
    val lastEvaluatedAt: Long? = null
)

/**
 * Compliance summary aggregating document statuses and compliance audits.
 */
data class VendorPortalComplianceSummary(
    val complianceRiskLevel: String = "LOW", // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val activeCertificationsCount: Int = 0,
    val expiringCertificationsCount: Int = 0,
    val expiredCertificationsCount: Int = 0,
    val taxComplianceStatus: String = "VERIFIED",
    val tradeLicenseStatus: String = "ACTIVE"
)

/**
 * Recent activity item representation.
 */
data class VendorPortalActivitySummary(
    val activityId: String,
    val eventType: String,
    val title: String,
    val description: String,
    val referenceId: String? = null,
    val timestamp: Long,
    val actor: String? = null,
    val category: String = "GENERAL" // "ORDER", "INVOICE", "QUALITY", "SETTLEMENT", "SECURITY"
)

/**
 * Feature visibility flags evaluated per role, membership, and access policy.
 */
data class VendorPortalFeatureVisibility(
    val canViewProfile: Boolean = true,
    val canViewServices: Boolean = true,
    val canViewCapabilities: Boolean = true,
    val canViewRates: Boolean = false,
    val canViewPurchaseOrders: Boolean = false,
    val canViewWorkOrders: Boolean = false,
    val canViewDeliveries: Boolean = false,
    val canViewInvoices: Boolean = false,
    val canViewFinancials: Boolean = false,
    val canViewQuality: Boolean = false,
    val canViewDisputes: Boolean = false,
    val canViewSettlements: Boolean = false,
    val canViewPerformance: Boolean = false,
    val canViewCompliance: Boolean = false,
    val canViewAuditTrail: Boolean = false,
    val canManagePortalUsers: Boolean = false
)

/**
 * Navigation item structure for Vendor Portal sidebar and tabs.
 */
data class VendorPortalNavigationItem(
    val id: String,
    val label: String,
    val route: String,
    val icon: String,
    val badgeCount: Int = 0,
    val isEnabled: Boolean = true,
    val category: String = "MAIN", // "MAIN", "OPERATIONS", "FINANCE", "QUALITY", "SETTINGS"
    val sortOrder: Int = 0
)

/**
 * Root aggregate dashboard representation for the authenticated Vendor Portal user.
 */
data class VendorPortalDashboard(
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val portalRole: VendorPortalRole,
    val membershipStatus: VendorPortalMembershipStatus,
    val accountStatus: VendorPortalAccountStatus,
    val kpis: List<VendorPortalKpi> = emptyList(),
    val profile: VendorPortalProfileSummary,
    val operations: VendorPortalOperationalSummary? = null,
    val financials: VendorPortalFinancialSummary? = null,
    val quality: VendorPortalQualitySummary? = null,
    val performance: VendorPortalPerformanceSummary? = null,
    val compliance: VendorPortalComplianceSummary? = null,
    val recentActivities: List<VendorPortalActivitySummary> = emptyList(),
    val featureVisibility: VendorPortalFeatureVisibility,
    val navigationItems: List<VendorPortalNavigationItem> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Full Workspace representation combining navigation, feature visibility, and user profile.
 */
data class VendorPortalWorkspace(
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val userId: String,
    val portalRole: VendorPortalRole,
    val profile: VendorPortalProfileSummary,
    val featureVisibility: VendorPortalFeatureVisibility,
    val navigationItems: List<VendorPortalNavigationItem>
)
