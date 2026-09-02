package com.sucharu.sucharupro.domain.model.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Period options for Vendor Portal Analytics (Module 13 Step 10).
 */
enum class VendorPortalPeriod {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    YEAR_TO_DATE,
    CURRENT_PERIOD,
    CUSTOM
}

/**
 * Trend direction indicator for analytics metrics.
 */
enum class VendorPortalTrendDirection {
    IMPROVING,
    STABLE,
    DECLINING
}

/**
 * Deterministic trend metric projection.
 */
data class VendorPortalTrendMetric(
    val metricKey: String,
    val label: String,
    val currentValue: Double,
    val previousValue: Double,
    val delta: Double,
    val percentageDelta: Double,
    val direction: VendorPortalTrendDirection,
    val unit: String = ""
) {
    companion object {
        fun calculate(
            metricKey: String,
            label: String,
            current: Double,
            previous: Double,
            higherIsBetter: Boolean = true,
            unit: String = ""
        ): VendorPortalTrendMetric {
            val delta = BigDecimal.valueOf(current - previous).setScale(2, RoundingMode.HALF_UP).toDouble()
            val pctDelta = if (previous != 0.0) {
                BigDecimal.valueOf(((current - previous) / previous) * 100.0).setScale(2, RoundingMode.HALF_UP).toDouble()
            } else if (current > 0.0) {
                100.0
            } else {
                0.0
            }

            val direction = when {
                delta > 0.01 -> if (higherIsBetter) VendorPortalTrendDirection.IMPROVING else VendorPortalTrendDirection.DECLINING
                delta < -0.01 -> if (higherIsBetter) VendorPortalTrendDirection.DECLINING else VendorPortalTrendDirection.IMPROVING
                else -> VendorPortalTrendDirection.STABLE
            }

            return VendorPortalTrendMetric(
                metricKey = metricKey,
                label = label,
                currentValue = current,
                previousValue = previous,
                delta = delta,
                percentageDelta = pctDelta,
                direction = direction,
                unit = unit
            )
        }
    }
}

/**
 * Unified Operational Analytics Projection.
 */
data class VendorPortalOperationalAnalytics(
    val activePurchaseOrders: Int,
    val openWorkOrders: Int,
    val completedWorkOrders: Int,
    val pendingDeliveryNotices: Int,
    val onTimeDeliveryRate: Double,
    val poFulfillmentRate: Double,
    val recentActivityCount: Int
)

/**
 * Unified Financial Analytics Projection.
 */
data class VendorPortalFinancialAnalytics(
    val submittedInvoicesCount: Int,
    val approvedInvoicesCount: Int,
    val paidInvoicesCount: Int,
    val totalOutstandingAmount: Money,
    val totalDisputedAmount: Money,
    val totalSettledAmount: Money,
    val currency: String = "BDT",
    val paymentTrend: String = "ON_TRACK"
)

/**
 * Unified Quality Analytics Projection.
 */
data class VendorPortalQualityAnalytics(
    val totalInspections: Int,
    val passedQuantity: Double,
    val rejectedQuantity: Double,
    val defectRate: Double,
    val openRejectionCases: Int,
    val activeDisputes: Int,
    val openCapaCount: Int
)

/**
 * Unified Performance Analytics Projection.
 */
data class VendorPortalPerformanceAnalytics(
    val overallScore: Double,
    val qualityKpi: Double,
    val onTimeDeliveryKpi: Double,
    val fulfillmentKpi: Double,
    val totalEvaluations: Int,
    val performanceRating: String
)

/**
 * Unified Compliance Analytics Projection.
 */
data class VendorPortalComplianceAnalytics(
    val complianceStatus: String,
    val totalCertifications: Int,
    val expiringCertifications: Int,
    val pendingRequirements: Int,
    val overallRiskLevel: String
)

/**
 * Unified Collaboration Analytics Projection.
 */
data class VendorPortalCollaborationAnalytics(
    val openBlockers: Int,
    val unreadMessages: Int,
    val pendingAcknowledgements: Int,
    val openDisputes: Int,
    val unresolvedItems: Int
)

/**
 * Capstone Unified Vendor Portal Analytics Hub Projection.
 */
data class VendorPortalUnifiedAnalyticsHub(
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val period: VendorPortalPeriod,
    val operational: VendorPortalOperationalAnalytics,
    val financial: VendorPortalFinancialAnalytics,
    val quality: VendorPortalQualityAnalytics,
    val performance: VendorPortalPerformanceAnalytics,
    val compliance: VendorPortalComplianceAnalytics,
    val collaboration: VendorPortalCollaborationAnalytics,
    val trends: List<VendorPortalTrendMetric>,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Analytics snapshot for persistent caching / history.
 */
data class VendorPortalAnalyticsSnapshot(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val period: VendorPortalPeriod,
    val calculationVersion: Int = 1,
    val metricsJson: String,
    val calculatedAt: Long = System.currentTimeMillis()
)

// ============================================================================
// NOTIFICATIONS & PREFERENCES
// ============================================================================

enum class VendorPortalNotificationCategory {
    OPERATIONS,
    PURCHASE_ORDER,
    WORK_ORDER,
    DELIVERY,
    QUALITY,
    INVOICE,
    PAYMENT,
    SETTLEMENT,
    PERFORMANCE,
    COMPLIANCE,
    COLLABORATION,
    SECURITY,
    SYSTEM
}

enum class VendorPortalNotificationSeverity {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL,
    URGENT
}

enum class VendorPortalNotificationStatus {
    UNREAD,
    READ,
    ARCHIVED
}

data class VendorPortalNotification(
    val notificationId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val category: VendorPortalNotificationCategory,
    val severity: VendorPortalNotificationSeverity = VendorPortalNotificationSeverity.NORMAL,
    val status: VendorPortalNotificationStatus = VendorPortalNotificationStatus.UNREAD,
    val title: String,
    val message: String,
    val relatedEntityType: String? = null,
    val relatedEntityId: String? = null,
    val deepLinkTarget: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class VendorPortalNotificationPreference(
    val preferenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val emailEnabled: Boolean = true,
    val inAppEnabled: Boolean = true,
    val pushEnabled: Boolean = false,
    val importantOnlyMode: Boolean = false,
    val disabledCategories: Set<VendorPortalNotificationCategory> = emptySet(),
    val minSeverity: VendorPortalNotificationSeverity = VendorPortalNotificationSeverity.LOW,
    val updatedAt: Long = System.currentTimeMillis()
)

data class VendorPortalNotificationUnreadCount(
    val totalUnread: Int,
    val unreadByCategory: Map<VendorPortalNotificationCategory, Int>,
    val unreadBySeverity: Map<VendorPortalNotificationSeverity, Int>
)

// ============================================================================
// PORTAL-WIDE SEARCH
// ============================================================================

enum class VendorPortalSearchResultType {
    PURCHASE_ORDER,
    WORK_ORDER,
    DELIVERY_NOTICE,
    INVOICE,
    QUALITY_CASE,
    DISPUTE,
    EVALUATION,
    COMPLIANCE_RECORD,
    SETTLEMENT,
    COLLABORATION_THREAD,
    NOTIFICATION
}

data class VendorPortalSearchResultItem(
    val resultType: VendorPortalSearchResultType,
    val entityId: String,
    val title: String,
    val snippet: String,
    val status: String,
    val contextualMetadata: Map<String, String> = emptyMap(),
    val timestamp: Long? = null,
    val deepLinkTarget: String
)

data class VendorPortalSearchResult(
    val query: String,
    val totalMatches: Int,
    val page: Int,
    val pageSize: Int,
    val items: List<VendorPortalSearchResultItem>
)

// ============================================================================
// CROSS-MODULE ACTIVITY TIMELINE
// ============================================================================

data class VendorPortalCrossModuleActivityItem(
    val activityId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val sourceModule: String,
    val eventType: String,
    val entityType: String,
    val entityId: String,
    val title: String,
    val description: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long,
    val deepLinkTarget: String? = null
)

data class VendorPortalActivityTimeline(
    val items: List<VendorPortalCrossModuleActivityItem>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

// ============================================================================
// UNIFIED WORKSPACE NAVIGATION & SUMMARY
// ============================================================================

data class VendorPortalWorkspaceNavigationSection(
    val sectionId: String,
    val label: String,
    val route: String,
    val badgeCount: Int = 0,
    val isVisible: Boolean = true,
    val iconName: String = "dashboard",
    val order: Int = 0
)

data class VendorPortalUnifiedWorkspaceSummary(
    val vendorId: String,
    val vendorName: String,
    val activePoCount: Int,
    val pendingInvoiceCount: Int,
    val openDisputeCount: Int,
    val unreadNotificationCount: Int,
    val overallPerformanceScore: Double,
    val complianceStatus: String,
    val navigationSections: List<VendorPortalWorkspaceNavigationSection>
)
