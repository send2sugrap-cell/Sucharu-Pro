package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Deterministic, zero-safe KPI and metrics calculator for Vendor Portal (Module 13 Step 02).
 */
object VendorPortalDashboardCalculator {

    /**
     * Calculates a percentage safely with deterministic rounding (2 decimal places).
     * Guarantees never throwing ArithmeticException or returning NaN/Infinity.
     */
    fun calculatePercentage(numerator: Number, denominator: Number): Double {
        val num = numerator.toDouble()
        val den = denominator.toDouble()
        if (den <= 0.0 || num <= 0.0) return 0.0
        val raw = (num / den) * 100.0
        return BigDecimal(raw).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Computes on-time delivery percentage safely.
     */
    fun calculateOnTimeDeliveryRate(onTimeCount: Int, totalDeliveredCount: Int): Double {
        return calculatePercentage(onTimeCount, totalDeliveredCount)
    }

    /**
     * Computes PO fulfillment percentage safely.
     */
    fun calculatePoFulfillmentRate(fulfilledCount: Int, totalPoCount: Int): Double {
        return calculatePercentage(fulfilledCount, totalPoCount)
    }

    /**
     * Computes defect rate percentage safely.
     */
    fun calculateDefectRate(defectiveUnits: Number, totalInspectedUnits: Number): Double {
        return calculatePercentage(defectiveUnits, totalInspectedUnits)
    }

    /**
     * Computes outstanding payables from total invoiced and total paid.
     */
    fun calculateOutstandingPayables(totalInvoiced: Money, totalPaid: Money): Money {
        val outstanding = totalInvoiced - totalPaid
        return if (outstanding.amount < BigDecimal.ZERO) Money.ZERO else outstanding
    }

    /**
     * Resolves quality rating based on defect rate.
     */
    fun resolveQualityRating(defectRatePercent: Double): String {
        return when {
            defectRatePercent <= 1.0 -> "EXCELLENT"
            defectRatePercent <= 3.0 -> "GOOD"
            defectRatePercent <= 6.0 -> "FAIR"
            else -> "POOR"
        }
    }

    /**
     * Evaluates Feature Visibility based on VendorPortalRole and AccessPolicy.
     */
    fun resolveFeatureVisibility(
        role: VendorPortalRole,
        policy: VendorPortalAccessPolicy
    ): VendorPortalFeatureVisibility {
        return when (role) {
            VendorPortalRole.VENDOR_ADMIN -> VendorPortalFeatureVisibility(
                canViewProfile = true,
                canViewServices = true,
                canViewCapabilities = true,
                canViewRates = true,
                canViewPurchaseOrders = policy.allowPoAcknowledgement,
                canViewWorkOrders = true,
                canViewDeliveries = true,
                canViewInvoices = policy.allowInvoiceSubmission,
                canViewFinancials = true,
                canViewQuality = policy.allowQualityDispute,
                canViewDisputes = policy.allowQualityDispute,
                canViewSettlements = true,
                canViewPerformance = true,
                canViewCompliance = true,
                canViewAuditTrail = true,
                canManagePortalUsers = true
            )
            VendorPortalRole.VENDOR_OPERATOR -> VendorPortalFeatureVisibility(
                canViewProfile = true,
                canViewServices = true,
                canViewCapabilities = true,
                canViewRates = false,
                canViewPurchaseOrders = policy.allowPoAcknowledgement,
                canViewWorkOrders = true,
                canViewDeliveries = true,
                canViewInvoices = false,
                canViewFinancials = false,
                canViewQuality = policy.allowQualityDispute,
                canViewDisputes = policy.allowQualityDispute,
                canViewSettlements = false,
                canViewPerformance = true,
                canViewCompliance = false,
                canViewAuditTrail = false,
                canManagePortalUsers = false
            )
            VendorPortalRole.VENDOR_FINANCE -> VendorPortalFeatureVisibility(
                canViewProfile = true,
                canViewServices = true,
                canViewCapabilities = false,
                canViewRates = true,
                canViewPurchaseOrders = true,
                canViewWorkOrders = false,
                canViewDeliveries = false,
                canViewInvoices = policy.allowInvoiceSubmission,
                canViewFinancials = true,
                canViewQuality = false,
                canViewDisputes = false,
                canViewSettlements = true,
                canViewPerformance = false,
                canViewCompliance = true,
                canViewAuditTrail = false,
                canManagePortalUsers = false
            )
            VendorPortalRole.VENDOR_LOGISTICS -> VendorPortalFeatureVisibility(
                canViewProfile = true,
                canViewServices = true,
                canViewCapabilities = false,
                canViewRates = false,
                canViewPurchaseOrders = policy.allowPoAcknowledgement,
                canViewWorkOrders = false,
                canViewDeliveries = true,
                canViewInvoices = false,
                canViewFinancials = false,
                canViewQuality = false,
                canViewDisputes = false,
                canViewSettlements = false,
                canViewPerformance = false,
                canViewCompliance = false,
                canViewAuditTrail = false,
                canManagePortalUsers = false
            )
            VendorPortalRole.VENDOR_QC -> VendorPortalFeatureVisibility(
                canViewProfile = true,
                canViewServices = true,
                canViewCapabilities = true,
                canViewRates = false,
                canViewPurchaseOrders = false,
                canViewWorkOrders = true,
                canViewDeliveries = true,
                canViewInvoices = false,
                canViewFinancials = false,
                canViewQuality = true,
                canViewDisputes = policy.allowQualityDispute,
                canViewSettlements = false,
                canViewPerformance = true,
                canViewCompliance = true,
                canViewAuditTrail = false,
                canManagePortalUsers = false
            )
            VendorPortalRole.VENDOR_VIEWER -> VendorPortalFeatureVisibility(
                canViewProfile = true,
                canViewServices = true,
                canViewCapabilities = true,
                canViewRates = false,
                canViewPurchaseOrders = false,
                canViewWorkOrders = false,
                canViewDeliveries = false,
                canViewInvoices = false,
                canViewFinancials = false,
                canViewQuality = false,
                canViewDisputes = false,
                canViewSettlements = false,
                canViewPerformance = true,
                canViewCompliance = false,
                canViewAuditTrail = false,
                canManagePortalUsers = false
            )
        }
    }

    /**
     * Builds navigation items list based on feature visibility and badge counts.
     */
    fun buildNavigationItems(
        visibility: VendorPortalFeatureVisibility,
        openPoCount: Int = 0,
        openWoCount: Int = 0,
        pendingDeliveryCount: Int = 0,
        pendingInvoiceCount: Int = 0,
        openDisputeCount: Int = 0
    ): List<VendorPortalNavigationItem> {
        val items = mutableListOf<VendorPortalNavigationItem>()

        // 1. Main Dashboard
        items.add(
            VendorPortalNavigationItem(
                id = "nav_dashboard",
                label = "Dashboard",
                route = "/vendor-portal/dashboard",
                icon = "dashboard",
                category = "MAIN",
                sortOrder = 10
            )
        )

        // 2. Profile
        if (visibility.canViewProfile) {
            items.add(
                VendorPortalNavigationItem(
                    id = "nav_profile",
                    label = "Vendor Profile",
                    route = "/vendor-portal/profile",
                    icon = "business",
                    category = "MAIN",
                    sortOrder = 20
                )
            )
        }

        // 3. Services & Capabilities
        if (visibility.canViewServices || visibility.canViewCapabilities) {
            items.add(
                VendorPortalNavigationItem(
                    id = "nav_services",
                    label = "Services & Capabilities",
                    route = "/vendor-portal/services",
                    icon = "build",
                    category = "MAIN",
                    sortOrder = 30
                )
            )
        }

        // 4. Operations (Purchase Orders, Work Orders, Delivery)
        if (visibility.canViewPurchaseOrders) {
            items.add(
                VendorPortalNavigationItem(
                    id = "nav_po",
                    label = "Purchase Orders",
                    route = "/vendor-portal/purchase-orders",
                    icon = "shopping_cart",
                    badgeCount = openPoCount,
                    category = "OPERATIONS",
                    sortOrder = 40
                )
            )
        }

        if (visibility.canViewWorkOrders) {
            items.add(
                VendorPortalNavigationItem(
                    id = "nav_wo",
                    label = "Work Orders",
                    route = "/vendor-portal/work-orders",
                    icon = "assignment",
                    badgeCount = openWoCount,
                    category = "OPERATIONS",
                    sortOrder = 50
                )
            )
        }

        if (visibility.canViewDeliveries) {
            items.add(
                VendorPortalNavigationItem(
                    id = "nav_deliveries",
                    label = "Deliveries & Shipments",
                    route = "/vendor-portal/deliveries",
                    icon = "local_shipping",
                    badgeCount = pendingDeliveryCount,
                    category = "OPERATIONS",
                    sortOrder = 60
                )
            )
        }

        // 5. Finance (Invoices & Settlements)
        if (visibility.canViewInvoices || visibility.canViewFinancials) {
            items.add(
                VendorPortalNavigationItem(
                    id = "nav_invoices",
                    label = "Invoices & Payments",
                    route = "/vendor-portal/invoices",
                    icon = "receipt",
                    badgeCount = pendingInvoiceCount,
                    category = "FINANCE",
                    sortOrder = 70
                )
            )
        }

        if (visibility.canViewSettlements) {
            items.add(
                VendorPortalNavigationItem(
                    id = "nav_settlements",
                    label = "Settlements & Statements",
                    route = "/vendor-portal/settlements",
                    icon = "account_balance",
                    category = "FINANCE",
                    sortOrder = 80
                )
            )
        }

        // 6. Quality & Disputes
        if (visibility.canViewQuality || visibility.canViewDisputes) {
            items.add(
                VendorPortalNavigationItem(
                    id = "nav_quality",
                    label = "Quality & Disputes",
                    route = "/vendor-portal/quality",
                    icon = "verified",
                    badgeCount = openDisputeCount,
                    category = "QUALITY",
                    sortOrder = 90
                )
            )
        }

        // 7. Performance & Compliance
        if (visibility.canViewPerformance) {
            items.add(
                VendorPortalNavigationItem(
                    id = "nav_performance",
                    label = "Performance & Scorecard",
                    route = "/vendor-portal/performance",
                    icon = "trending_up",
                    category = "QUALITY",
                    sortOrder = 100
                )
            )
        }

        // 8. Settings & Administration
        if (visibility.canManagePortalUsers) {
            items.add(
                VendorPortalNavigationItem(
                    id = "nav_settings",
                    label = "Portal Settings",
                    route = "/vendor-portal/settings",
                    icon = "settings",
                    category = "SETTINGS",
                    sortOrder = 110
                )
            )
        }

        return items.sortedBy { it.sortOrder }
    }

    /**
     * Builds role-tailored KPIs list for the dashboard header.
     */
    fun buildDashboardKpis(
        visibility: VendorPortalFeatureVisibility,
        operations: VendorPortalOperationalSummary?,
        financials: VendorPortalFinancialSummary?,
        quality: VendorPortalQualitySummary?,
        performance: VendorPortalPerformanceSummary?
    ): List<VendorPortalKpi> {
        val kpis = mutableListOf<VendorPortalKpi>()

        if (visibility.canViewPurchaseOrders && operations != null) {
            kpis.add(
                VendorPortalKpi(
                    key = "ACTIVE_PO",
                    label = "Active Purchase Orders",
                    value = operations.activePurchaseOrders.toString(),
                    numericValue = operations.activePurchaseOrders.toDouble(),
                    status = if (operations.activePurchaseOrders > 0) "NORMAL" else "GOOD",
                    category = "OPERATIONAL"
                )
            )
        }

        if (visibility.canViewWorkOrders && operations != null) {
            kpis.add(
                VendorPortalKpi(
                    key = "OPEN_WO",
                    label = "Open Work Orders",
                    value = operations.openWorkOrders.toString(),
                    numericValue = operations.openWorkOrders.toDouble(),
                    status = if (operations.openWorkOrders > 0) "NORMAL" else "GOOD",
                    category = "OPERATIONAL"
                )
            )
        }

        if (visibility.canViewDeliveries && operations != null) {
            kpis.add(
                VendorPortalKpi(
                    key = "OTD_RATE",
                    label = "On-Time Delivery Rate",
                    value = "${operations.onTimeDeliveryRatePercent}%",
                    numericValue = operations.onTimeDeliveryRatePercent,
                    unit = "%",
                    status = if (operations.onTimeDeliveryRatePercent >= 95.0) "GOOD" else if (operations.onTimeDeliveryRatePercent >= 85.0) "NORMAL" else "WARNING",
                    category = "OPERATIONAL"
                )
            )
        }

        if (visibility.canViewFinancials && financials != null) {
            kpis.add(
                VendorPortalKpi(
                    key = "OUTSTANDING_PAYABLES",
                    label = "Outstanding Payables",
                    value = financials.totalOutstandingPayables.formatted("৳"),
                    numericValue = financials.totalOutstandingPayables.amount.toDouble(),
                    unit = "BDT",
                    status = "NORMAL",
                    category = "FINANCIAL"
                )
            )
        }

        if (visibility.canViewQuality && quality != null) {
            kpis.add(
                VendorPortalKpi(
                    key = "DEFECT_RATE",
                    label = "Overall Defect Rate",
                    value = "${quality.overallDefectRatePercent}%",
                    numericValue = quality.overallDefectRatePercent,
                    unit = "%",
                    status = if (quality.overallDefectRatePercent <= 1.0) "GOOD" else if (quality.overallDefectRatePercent <= 3.0) "NORMAL" else "WARNING",
                    category = "QUALITY"
                )
            )
            kpis.add(
                VendorPortalKpi(
                    key = "OPEN_DISPUTES",
                    label = "Open Disputes",
                    value = quality.openDisputes.toString(),
                    numericValue = quality.openDisputes.toDouble(),
                    status = if (quality.openDisputes == 0) "GOOD" else "WARNING",
                    category = "QUALITY"
                )
            )
        }

        if (visibility.canViewPerformance && performance != null) {
            kpis.add(
                VendorPortalKpi(
                    key = "PERFORMANCE_SCORE",
                    label = "Vendor Performance Score",
                    value = "${performance.overallScore}/100",
                    numericValue = performance.overallScore,
                    unit = "/100",
                    status = if (performance.overallScore >= 90.0) "GOOD" else if (performance.overallScore >= 75.0) "NORMAL" else "WARNING",
                    category = "QUALITY"
                )
            )
        }

        return kpis
    }
}
