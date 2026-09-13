package com.sucharu.sucharupro.domain.service.report

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix
import com.sucharu.sucharupro.domain.model.report.*

/**
 * Authoritative Module 24 Catalogue Registry of Enterprise Reports across all Canonical Categories.
 */
object Module24ReportCatalogueRegistry {

    private val catalogue: List<CanonicalReportDefinition> = listOf(
        // =========================================================================
        // 1. SALES REPORTING (MODULE 03 CANONICAL ORDER DATA)
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "SALES_SUMMARY",
            category = ReportCategory.SALES,
            displayName = "Sales & Revenue Executive Summary",
            description = "Aggregate sales revenue, net revenue, discount total, order volume, and average order value.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_SALES,
            supportedFilters = listOf("fromDate", "toDate", "customerId", "status"),
            defaultMetrics = listOf("totalSalesAmount", "totalOrdersCount", "averageOrderValue", "totalDiscountAmount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "SALES_TREND",
            category = ReportCategory.SALES,
            displayName = "Sales Trend & Period Breakdown",
            description = "Period-wise revenue trends, daily/weekly/monthly revenue totals, and sales trajectory charts.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_SALES,
            supportedFilters = listOf("fromDate", "toDate", "period"),
            defaultMetrics = listOf("totalSalesAmount", "periodGrowthPercentage")
        ),
        CanonicalReportDefinition(
            reportTypeId = "SALES_BY_CUSTOMER",
            category = ReportCategory.SALES,
            displayName = "Sales Breakdown by Customer",
            description = "Customer-wise revenue contribution, gross sales, completed order counts, and average customer order value.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_SALES,
            supportedFilters = listOf("fromDate", "toDate", "customerId"),
            defaultMetrics = listOf("totalSalesAmount", "activeCustomerCount", "topCustomerRevenue")
        ),
        CanonicalReportDefinition(
            reportTypeId = "SALES_BY_ORDER",
            category = ReportCategory.SALES,
            displayName = "Detailed Sales Ledger by Order",
            description = "Line-item sales log per order showing order number, customer name, status, gross amount, and net amount.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_SALES,
            supportedFilters = listOf("fromDate", "toDate", "orderId", "status"),
            defaultMetrics = listOf("totalSalesAmount", "totalOrdersCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "SALES_BY_PRODUCT",
            category = ReportCategory.SALES,
            displayName = "Sales Breakdown by Product / Specification",
            description = "Product category revenue, item volume sales, and product line contribution margins.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_SALES,
            supportedFilters = listOf("fromDate", "toDate", "productCategory"),
            defaultMetrics = listOf("totalSalesAmount", "totalItemsSold")
        ),
        CanonicalReportDefinition(
            reportTypeId = "TOP_CUSTOMERS_BY_SALES",
            category = ReportCategory.SALES,
            displayName = "Top Customers Ranked by Sales Volume",
            description = "Leaderboard of top spending customers by total net sales and order frequency.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_SALES,
            supportedFilters = listOf("fromDate", "toDate", "limit"),
            defaultMetrics = listOf("topCustomerRevenue", "topCustomerOrderCount")
        ),

        // =========================================================================
        // 2. CUSTOMER REPORTING (MODULE 02 CANONICAL CUSTOMER DATA)
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "CUSTOMER_SUMMARY",
            category = ReportCategory.CUSTOMER,
            displayName = "Customer Management & Account Overview",
            description = "Total customer counts, active vs inactive vs archived status distribution, and account growth.",
            authoritativeModule = "Module 02 Customer Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_CUSTOMER,
            supportedFilters = listOf("fromDate", "toDate", "customerType", "status"),
            defaultMetrics = listOf("totalCustomersCount", "activeCustomersCount", "inactiveCustomersCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "CUSTOMER_GROWTH",
            category = ReportCategory.CUSTOMER,
            displayName = "Customer Onboarding & Growth Trend",
            description = "New customer registration rates, onboarding trends, and account activation trajectory over time.",
            authoritativeModule = "Module 02 Customer Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_CUSTOMER,
            supportedFilters = listOf("fromDate", "toDate", "period"),
            defaultMetrics = listOf("newCustomersCount", "customerGrowthRate")
        ),
        CanonicalReportDefinition(
            reportTypeId = "CUSTOMER_ACTIVITY",
            category = ReportCategory.CUSTOMER,
            displayName = "Customer Lifecycle Activity & Follow-Up Log",
            description = "Operational activity log per customer, status changes, contact history, and pending follow-ups.",
            authoritativeModule = "Module 02 Customer Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_CUSTOMER,
            supportedFilters = listOf("fromDate", "toDate", "customerId", "activityType"),
            defaultMetrics = listOf("totalActivitiesCount", "pendingFollowUpsCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "CUSTOMER_SALES",
            category = ReportCategory.CUSTOMER,
            displayName = "Customer Order & Purchase Volume Report",
            description = "Customer-wise historical order counts, lifetime total spend, average order value, and last purchase date.",
            authoritativeModule = "Module 02 Customer Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_CUSTOMER,
            supportedFilters = listOf("fromDate", "toDate", "customerId"),
            defaultMetrics = listOf("totalSpend", "orderCount", "averageSpendPerOrder")
        ),
        CanonicalReportDefinition(
            reportTypeId = "CUSTOMER_RECEIVABLE_AGING",
            category = ReportCategory.CUSTOMER,
            displayName = "Customer Receivables & Statement Report",
            description = "Customer statement breakdown, total receivables, aging buckets (0-30d, 31-60d, 61-90d, >90d), and overdue totals.",
            authoritativeModule = "Module 02 Customer Management & Module 14 Financials",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_CUSTOMER,
            supportedFilters = listOf("fromDate", "toDate", "customerId", "asOfDate"),
            defaultMetrics = listOf("totalReceivables", "currentAmount", "overdueAmount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "TOP_CUSTOMERS",
            category = ReportCategory.CUSTOMER,
            displayName = "Top Customer Accounts Leaderboard",
            description = "Ranking of key customer accounts by order volume, total revenue, and credit profile.",
            authoritativeModule = "Module 02 Customer Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_CUSTOMER,
            supportedFilters = listOf("fromDate", "toDate", "limit"),
            defaultMetrics = listOf("topCustomerRevenue", "topCustomerOrderCount")
        ),

        // =========================================================================
        // 3. ORDER REPORTING (MODULE 03 CANONICAL ORDER DATA)
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "ORDER_SUMMARY",
            category = ReportCategory.ORDER,
            displayName = "Order Fulfillment & Pipeline Summary",
            description = "Order status breakdown across PENDING, CONFIRMED, IN_PRODUCTION, READY, DELIVERED, ON_HOLD, and CANCELLED.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_ORDER,
            supportedFilters = listOf("fromDate", "toDate", "status", "priority"),
            defaultMetrics = listOf("totalOrdersCount", "openOrdersCount", "completedOrdersCount", "cancelledOrdersCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "ORDER_TREND",
            category = ReportCategory.ORDER,
            displayName = "Order Volume & Value Trend",
            description = "Period-wise order intake velocity, order counts, and cumulative order value progression.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_ORDER,
            supportedFilters = listOf("fromDate", "toDate", "period"),
            defaultMetrics = listOf("totalOrdersCount", "totalOrderValue")
        ),
        CanonicalReportDefinition(
            reportTypeId = "ORDER_BY_STATUS",
            category = ReportCategory.ORDER,
            displayName = "Order Distribution by Commercial Status",
            description = "Order status lifecycle distribution, stage conversion efficiency, and current open order status counts.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_ORDER,
            supportedFilters = listOf("fromDate", "toDate", "status"),
            defaultMetrics = listOf("pendingCount", "confirmedCount", "inProductionCount", "readyCount", "deliveredCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "ORDER_BY_CUSTOMER",
            category = ReportCategory.ORDER,
            displayName = "Customer Order Pipeline & Fulfillment Status",
            description = "Per-customer order tracking log showing active orders, delivery status, and order totals.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_ORDER,
            supportedFilters = listOf("fromDate", "toDate", "customerId", "status"),
            defaultMetrics = listOf("totalOrdersCount", "completedOrdersCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "ORDER_VALUE_QUANTITY",
            category = ReportCategory.ORDER,
            displayName = "Order Commercial Value & Item Quantity Report",
            description = "Aggregate order value, gross quantity produced, and unit economics summary.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_ORDER,
            supportedFilters = listOf("fromDate", "toDate", "orderId"),
            defaultMetrics = listOf("totalOrderValue", "totalItemQuantity", "averageOrderValue")
        ),
        CanonicalReportDefinition(
            reportTypeId = "ORDER_AGING",
            category = ReportCategory.ORDER,
            displayName = "Open Order Aging & Delay Risk Analysis",
            description = "Aging of unfulfilled open orders from confirmation date to identify delivery delay risk.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_ORDER,
            supportedFilters = listOf("fromDate", "toDate", "status"),
            defaultMetrics = listOf("averageOrderAgeDays", "overdueOpenOrdersCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "ORDER_LIFECYCLE",
            category = ReportCategory.ORDER,
            displayName = "Order Lifecycle & Lead Time Analysis",
            description = "Order lifecycle status distribution, lead times from confirmation to delivery, and fulfillment completion rates.",
            authoritativeModule = "Module 03 Quotation & Order Management",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_ORDER,
            supportedFilters = listOf("fromDate", "toDate", "orderId", "status"),
            defaultMetrics = listOf("totalOrders", "completedOrders", "fulfillmentRate")
        ),

        // =========================================================================
        // 4. PRODUCTION & JOB PERFORMANCE REPORTING (MODULE 04 CANONICAL DATA)
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "PRODUCTION_SUMMARY",
            category = ReportCategory.PRODUCTION,
            displayName = "Production Operations & Execution Summary",
            description = "Job progress tracking, active in-progress jobs, completed jobs, wastage, and job costing variances.",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate", "jobId", "stage", "status"),
            defaultMetrics = listOf("activeJobsCount", "completedJobsCount", "totalProductionHours", "plannedQuantity", "completedQuantity")
        ),
        CanonicalReportDefinition(
            reportTypeId = "PRODUCTION_TREND",
            category = ReportCategory.PRODUCTION,
            displayName = "Production Volume & Stage Throughput Trend",
            description = "Period-wise job execution velocity, stage completion throughput, and active job progression trajectory.",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate", "period"),
            defaultMetrics = listOf("activeJobsCount", "completedJobsCount", "throughputRate")
        ),
        CanonicalReportDefinition(
            reportTypeId = "PRODUCTION_BY_STATUS",
            category = ReportCategory.PRODUCTION,
            displayName = "Production Distribution by Execution Status",
            description = "Breakdown across READY, RELEASED, SCHEDULED, IN_PROGRESS, ON_HOLD, QC_PENDING, REWORK_REQUIRED, COMPLETING, COMPLETED, CANCELLED, BLOCKED.",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate", "status"),
            defaultMetrics = listOf("inProgressCount", "onHoldCount", "qcPendingCount", "completedCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "PRODUCTION_BY_CUSTOMER",
            category = ReportCategory.PRODUCTION,
            displayName = "Customer Production Workload Report",
            description = "Per-customer active job workload, completion rates, and production quantities.",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate", "customerId"),
            defaultMetrics = listOf("activeJobsCount", "completedJobsCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "PRODUCTION_BY_ORDER",
            category = ReportCategory.PRODUCTION,
            displayName = "Order-to-Production Progress Tracking",
            description = "Mapping of customer commercial orders to shop-floor execution jobs, current stages, and progress fractions.",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate", "orderId"),
            defaultMetrics = listOf("progressFraction", "completedWorkOrdersCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "PRODUCTION_QUANTITY",
            category = ReportCategory.PRODUCTION,
            displayName = "Production Quantity & Material Wastage Analysis",
            description = "Planned quantity vs started quantity vs completed good quantity vs rejected and wastage quantity.",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate", "jobId"),
            defaultMetrics = listOf("plannedQuantity", "goodQuantity", "wastageQuantity", "reworkQuantity")
        ),
        CanonicalReportDefinition(
            reportTypeId = "PRODUCTION_AGING",
            category = ReportCategory.PRODUCTION,
            displayName = "Active Production Job Aging Report",
            description = "Aging of uncompleted production jobs on the shop floor to detect delayed/stalled jobs.",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate", "status"),
            defaultMetrics = listOf("averageJobAgeHours", "delayedJobsCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "JOB_PERFORMANCE",
            category = ReportCategory.PRODUCTION,
            displayName = "Job Cycle Time & Work Order Execution Performance",
            description = "Individual job cycle time, work order setup/run minutes, active holds, and completion efficiency.",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate", "jobId"),
            defaultMetrics = listOf("averageCycleTimeHours", "completionRate")
        ),
        CanonicalReportDefinition(
            reportTypeId = "JOB_CYCLE_TIME",
            category = ReportCategory.PRODUCTION,
            displayName = "Job Cycle Time Analysis",
            description = "Time elapsed from job creation/release to final completion.",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate"),
            defaultMetrics = listOf("averageCycleTimeHours", "minCycleTimeHours", "maxCycleTimeHours")
        ),
        CanonicalReportDefinition(
            reportTypeId = "JOB_STAGE_PERFORMANCE",
            category = ReportCategory.PRODUCTION,
            displayName = "Stage Actual Duration & Setup/Run Minutes",
            description = "Detailed work order execution actuals: setup minutes, run minutes, and good vs scrap output.",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate", "stage"),
            defaultMetrics = listOf("actualSetupMinutes", "actualRunMinutes", "goodQuantity")
        ),
        CanonicalReportDefinition(
            reportTypeId = "STAGE_PERFORMANCE",
            category = ReportCategory.PRODUCTION,
            displayName = "Canonical 13-Stage Production Performance",
            description = "Workload, entry counts, completion counts, and duration across all 13 canonical stages (Design through Delivered).",
            authoritativeModule = "Module 04 Production Execution",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PRODUCTION,
            supportedFilters = listOf("fromDate", "toDate", "stage"),
            defaultMetrics = listOf("activeEntries", "completedEntries", "stageDurationHours")
        ),

        // =========================================================================
        // 5. QUALITY CONTROL & REWORK REPORTING (MODULE 06 CANONICAL DATA)
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "QC_SUMMARY",
            category = ReportCategory.QUALITY,
            displayName = "Quality Assurance & Inspection Summary",
            description = "Total QC inspections, first-time pass count, failure count, pass rate %, and defect rejection rate.",
            authoritativeModule = "Module 06 Quality Control",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate", "inspectorId", "defectCategory"),
            defaultMetrics = listOf("inspectionsCount", "passedCount", "rejectedCount", "defectRate")
        ),
        CanonicalReportDefinition(
            reportTypeId = "QC_TREND",
            category = ReportCategory.QUALITY,
            displayName = "QC Inspection & Pass/Fail Velocity Trend",
            description = "Period-wise inspection volume, pass rate progression, and quality trend trajectory.",
            authoritativeModule = "Module 06 Quality Control",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate", "period"),
            defaultMetrics = listOf("inspectionsCount", "passRatePercentage")
        ),
        CanonicalReportDefinition(
            reportTypeId = "QC_BY_STAGE",
            category = ReportCategory.QUALITY,
            displayName = "QC Inspection Results by Production Stage",
            description = "Inspection pass/fail distribution at QC checkpoint stages (especially QC and FINAL_QC).",
            authoritativeModule = "Module 06 Quality Control",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate", "stage"),
            defaultMetrics = listOf("inspectionsCount", "passRatePercentage")
        ),
        CanonicalReportDefinition(
            reportTypeId = "QC_BY_RESULT",
            category = ReportCategory.QUALITY,
            displayName = "QC Decision Breakdown",
            description = "Distribution across APPROVED, REJECTED, CONDITIONAL_APPROVAL, REWORK_REQUIRED, and PENDING.",
            authoritativeModule = "Module 06 Quality Control",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate", "decision"),
            defaultMetrics = listOf("approvedCount", "rejectedCount", "reworkRequiredCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "QC_FAILURE_REASONS",
            category = ReportCategory.QUALITY,
            displayName = "QC Defect & Failure Reason Classification",
            description = "Pareto breakdown of defect reasons (color misalignment, material defect, damage, spec mismatch).",
            authoritativeModule = "Module 06 Quality Control",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate", "defectCategory"),
            defaultMetrics = listOf("defectCount", "topDefectReason")
        ),
        CanonicalReportDefinition(
            reportTypeId = "FINAL_QC_SUMMARY",
            category = ReportCategory.QUALITY,
            displayName = "Final QC Release Authorization & Packaging Readiness",
            description = "Final QC pass rates before packaging/delivery, release authorization status, and dispatch readiness.",
            authoritativeModule = "Module 06 Quality Control",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate"),
            defaultMetrics = listOf("finalQcPassRate", "releaseAuthorizedCount")
        ),
        CanonicalReportDefinition(
            reportTypeId = "REWORK_SUMMARY",
            category = ReportCategory.QUALITY,
            displayName = "Rework Requests & Corrective Action Summary",
            description = "Total rework requests, active reworks, completed reworks, affected quantities, and rework resolution rates.",
            authoritativeModule = "Module 06 Quality Control & Rework",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate", "reason", "status"),
            defaultMetrics = listOf("totalReworksCount", "activeReworksCount", "completedReworksCount", "affectedQuantity")
        ),
        CanonicalReportDefinition(
            reportTypeId = "REWORK_BY_STAGE",
            category = ReportCategory.QUALITY,
            displayName = "Rework Occurrences by Production Stage",
            description = "Distribution of rework requests across production stages to identify high-risk manufacturing steps.",
            authoritativeModule = "Module 06 Quality Control & Rework",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate", "stage"),
            defaultMetrics = listOf("reworkCount", "affectedQuantity")
        ),
        CanonicalReportDefinition(
            reportTypeId = "REWORK_BY_REASON",
            category = ReportCategory.QUALITY,
            displayName = "Rework Distribution by Failure Reason",
            description = "Classification of reworks by root cause reason (defect, operator error, material damage, customer change).",
            authoritativeModule = "Module 06 Quality Control & Rework",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate", "reason"),
            defaultMetrics = listOf("reworkCount", "topReworkReason")
        ),
        CanonicalReportDefinition(
            reportTypeId = "REWORK_TREND",
            category = ReportCategory.QUALITY,
            displayName = "Rework Request Frequency & Resolution Velocity",
            description = "Period-wise rework request frequency and average duration to complete corrective rework.",
            authoritativeModule = "Module 06 Quality Control & Rework",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate", "period"),
            defaultMetrics = listOf("totalReworksCount", "averageReworkHours")
        ),
        CanonicalReportDefinition(
            reportTypeId = "REWORK_BY_JOB",
            category = ReportCategory.QUALITY,
            displayName = "Jobs with Repeated Reworks & Risk Analysis",
            description = "Identification of high-risk jobs experiencing multiple rework cycles and corrective action progress.",
            authoritativeModule = "Module 06 Quality Control & Rework",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_QUALITY,
            supportedFilters = listOf("fromDate", "toDate", "jobId"),
            defaultMetrics = listOf("jobsWithReworkCount", "repeatReworkCount")
        ),

        // =========================================================================
        // 6. INVENTORY
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "INVENTORY_VALUATION",
            category = ReportCategory.INVENTORY,
            displayName = "Finished Goods Inventory & Stock Valuation",
            description = "Inventory stock levels, warehouse locations, product valuation, and stock movement metrics.",
            authoritativeModule = "Module 07 Finished Goods Inventory",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_INVENTORY,
            supportedFilters = listOf("fromDate", "toDate", "warehouseId", "sku"),
            defaultMetrics = listOf("totalItemsCount", "totalInventoryValue", "lowStockAlertsCount")
        ),

        // =========================================================================
        // 7. DELIVERY
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "DELIVERY_PERFORMANCE",
            category = ReportCategory.DELIVERY,
            displayName = "Delivery Dispatch & Challan Fulfillment Report",
            description = "Delivery status tracking, proof-of-delivery verification, and driver/challan logs.",
            authoritativeModule = "Module 08 Delivery & Dispatch",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_DELIVERY,
            supportedFilters = listOf("fromDate", "toDate", "challanId", "status"),
            defaultMetrics = listOf("totalDeliveries", "deliveredCount", "pendingDeliveryCount")
        ),

        // =========================================================================
        // 8. FINANCE
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "FINANCE_EXECUTIVE_SUMMARY",
            category = ReportCategory.FINANCE,
            displayName = "General Ledger & Financial Reporting Summary",
            description = "General ledger balances, P&L summary, balance sheet metrics, and trial balance totals.",
            authoritativeModule = "Module 09 Finance & Ledger",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_FINANCE,
            supportedFilters = listOf("fromDate", "toDate", "periodId", "branchId"),
            defaultMetrics = listOf("totalRevenue", "totalExpense", "netOperatingIncome")
        ),

        // =========================================================================
        // 9. PROFITABILITY
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "PROFITABILITY_ANALYSIS",
            category = ReportCategory.PROFITABILITY,
            displayName = "Executive Profitability & Margin Analytics",
            description = "Gross and net profit margins by customer, job, product category, and operational period.",
            authoritativeModule = "Module 09/15 Profitability Engine",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PROFITABILITY,
            supportedFilters = listOf("fromDate", "toDate", "customerId", "jobId"),
            defaultMetrics = listOf("grossProfit", "netProfit", "grossMarginPercentage")
        ),

        // =========================================================================
        // 10. AFFILIATE
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "AFFILIATE_COMMISSIONS",
            category = ReportCategory.AFFILIATE,
            displayName = "Affiliate Referral & Commission Statement",
            description = "Referred customer orders, commission calculations, pending commissions, and payout ledger.",
            authoritativeModule = "Module 20 Affiliate Program",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_AFFILIATE,
            supportedFilters = listOf("fromDate", "toDate", "affiliateId", "status"),
            defaultMetrics = listOf("totalReferrals", "totalCommissionEarned", "pendingCommission")
        ),

        // =========================================================================
        // 11. WALLET / PAYOUT
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "WALLET_PAYOUT_HISTORY",
            category = ReportCategory.WALLET_PAYOUT,
            displayName = "Wallet Accounting & Payout Disbursement Log",
            description = "Wallet balance ledger, holds, payout request statuses, and disbursement audit events.",
            authoritativeModule = "Module 23 Wallet & Payout",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_WALLET_PAYOUT,
            supportedFilters = listOf("fromDate", "toDate", "walletId", "affiliateId"),
            defaultMetrics = listOf("availableBalance", "heldBalance", "totalPayoutsDisbursed")
        ),

        // =========================================================================
        // 12. MACHINE / OPERATIONS
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "MACHINE_OEE_SUMMARY",
            category = ReportCategory.MACHINE_OPERATIONS,
            displayName = "Machine OEE & Equipment Telemetry Analytics",
            description = "Overall Equipment Effectiveness (OEE), availability, performance, quality rating, and downtime logs.",
            authoritativeModule = "Module 21 Machine Registry & OEE",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_MACHINE_OPERATIONS,
            supportedFilters = listOf("fromDate", "toDate", "machineId", "status"),
            defaultMetrics = listOf("oeePercentage", "availabilityPercentage", "performancePercentage", "downtimeHours")
        ),

        // =========================================================================
        // 13. PREFLIGHT
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "PREFLIGHT_DIAGNOSTICS",
            category = ReportCategory.PREFLIGHT,
            displayName = "Preflight Inspection & Proofing Diagnostics",
            description = "Preflight run execution stats, finding resolution rates, waivers, and artwork readiness scores.",
            authoritativeModule = "Module 22 Preflight Engine",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_PREFLIGHT,
            supportedFilters = listOf("fromDate", "toDate", "artworkId", "jobId"),
            defaultMetrics = listOf("totalRuns", "passedRuns", "findingsCount", "waivedFindingsCount")
        ),

        // =========================================================================
        // 14. AUDIT
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "SYSTEM_AUDIT_LOGS",
            category = ReportCategory.AUDIT,
            displayName = "Enterprise Subsystem Audit & Security Log",
            description = "System activity audit trail, financial snapshot verifications, RLS security access logs, and workflow events.",
            authoritativeModule = "Subsystem Audit Logs",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_AUDIT,
            supportedFilters = listOf("fromDate", "toDate", "actorId", "eventType"),
            defaultMetrics = listOf("totalAuditEvents", "securityDenials", "snapshotEvents")
        ),

        // =========================================================================
        // 15. EXECUTIVE ANALYTICS
        // =========================================================================
        CanonicalReportDefinition(
            reportTypeId = "EXECUTIVE_ANALYTICS_DASHBOARD",
            category = ReportCategory.EXECUTIVE_ANALYTICS,
            displayName = "Cross-Module Executive Dashboard & Strategic KPI Report",
            description = "Top-level executive summary combining sales, production, finance, OEE, and operational health KPIs.",
            authoritativeModule = "Cross-Module Executive Aggregations",
            requiredCapability = AuthorizationCapability.REPORT_VIEW_EXECUTIVE_ANALYTICS,
            supportedFilters = listOf("fromDate", "toDate", "periodId"),
            defaultMetrics = listOf("totalRevenue", "grossProfitMargin", "oeeAverage", "orderFulfillmentRate")
        )
    )

    /**
     * Resolves a report definition by its type ID.
     */
    fun findDefinition(reportTypeId: String): CanonicalReportDefinition? {
        return catalogue.find { it.reportTypeId.equals(reportTypeId, ignoreCase = true) }
    }

    /**
     * Resolves all registered report definitions.
     */
    fun getAllDefinitions(): List<CanonicalReportDefinition> = catalogue

    /**
     * Resolves available report catalogue filtered by the authenticated user's capabilities.
     */
    fun getCatalogueForPrincipal(principal: AuthenticatedPrincipal): ReportCatalogueResponse {
        val userRole = principal.role
        val userCapabilities = RoleCapabilityMatrix.getCapabilities(userRole)

        val accessibleReports = catalogue.filter { def ->
            userRole == com.sucharu.sucharupro.data.api.model.UserRole.ADMIN ||
                    userCapabilities.contains(AuthorizationCapability.ADMIN_ALL) ||
                    userCapabilities.contains(def.requiredCapability)
        }

        val itemsByCat = accessibleReports.groupBy { it.category }

        val categorySummaries = ReportCategory.entries.mapNotNull { cat ->
            val reportsInCat = itemsByCat[cat]
            if (reportsInCat.isNullOrEmpty()) null
            else {
                ReportCategorySummary(
                    category = cat,
                    displayName = cat.displayName,
                    reports = reportsInCat.map { def ->
                        ReportCatalogueItem(
                            category = def.category,
                            reportType = def.reportTypeId,
                            displayName = def.displayName,
                            description = def.description,
                            authoritativeModule = def.authoritativeModule,
                            requiredCapability = def.requiredCapability,
                            defaultMetrics = def.defaultMetrics
                        )
                    }
                )
            }
        }

        return ReportCatalogueResponse(
            totalCategories = categorySummaries.size,
            totalReports = accessibleReports.size,
            categories = categorySummaries
        )
    }
}
