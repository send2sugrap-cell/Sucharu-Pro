package com.sucharu.sucharupro.domain.model.dashboard

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.inventory.StockStatusType
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.payment.PaymentStatusType
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

// =============================================================================
// TOP-LEVEL DASHBOARD AGGREGATE
// =============================================================================

/**
 * Top-level dashboard data container for Sucharu Pro.
 *
 * Aggregates all data required for the Admin/Manager operational dashboard:
 * shop context, KPIs, production pipeline, financial snapshot, workload,
 * inventory alerts, and operational alert counts.
 *
 * All financial values use [Money] (BigDecimal-backed) for precision.
 * All production pipeline data uses canonical [ProductionStageType].
 * All commercial order states use canonical [OrderStatusType].
 */
data class DashboardSummary(
    /** Shop identity and active shift context. */
    val shopHeader: ShopHeaderInfo,

    /** Executive KPI metrics (financial + operational counts). */
    val kpis: DashboardKpis,

    /** Active job counts per production stage (canonical 13-stage pipeline). */
    val stageCounts: List<StageCount>,

    /** Financial payment and receivables breakdown. */
    val paymentBreakdown: PaymentBreakdown,

    /** Today's workload: due, priority, and delayed job lists. */
    val workloadSummary: WorkloadSummary,

    /** Most recent orders/jobs list for quick review. */
    val recentOrders: List<DashboardJobSummary>,

    /** Finished product stock alert items (NOT raw material). */
    val inventoryAlerts: List<DashboardInventoryAlert>,

    /**
     * Operational alert counts: approval pending, QC, delivery, payment, etc.
     * Defaults to [DashboardOperationalAlerts.EMPTY] if not yet loaded.
     */
    val operationalAlerts: DashboardOperationalAlerts = DashboardOperationalAlerts.EMPTY
)

// =============================================================================
// SHOP CONTEXT
// =============================================================================

data class ShopHeaderInfo(
    val shopName: String,
    val ownerName: String,
    val formattedDate: String,
    val activeShift: String = "Morning Shift"
)

// =============================================================================
// KPI MODEL — Executive Dashboard Metrics
// =============================================================================

/**
 * Full suite of executive KPI metrics for the Sucharu Pro Dashboard.
 *
 * Covers three categories:
 *  1. Sales/Financial KPIs: today, weekly, monthly sales; profit; expense; payables
 *  2. Job Volume KPIs: active, ready, delivered, replacement counts
 *  3. Operational KPIs: affiliate commission, finished product stock count
 *
 * All monetary values use [Money] (BigDecimal-backed — no floating-point precision issues).
 * All counts use [Int].
 *
 * This model is presentation-agnostic and does NOT depend on any UI/Compose type.
 */
data class DashboardKpis(

    // ---- Today's Volume Metrics ----

    /** Number of new orders placed today. */
    val todayOrdersCount: Int,

    /** Number of jobs currently in any active production stage. */
    val activeJobsCount: Int,

    // ---- Job Status Counts ----

    /** Number of jobs with [OrderStatusType.READY] status (completed, awaiting dispatch). */
    val readyJobsCount: Int,

    /** Number of jobs with [OrderStatusType.DELIVERED] status (delivered today or all-time YTD). */
    val deliveredJobsCount: Int,

    // ---- Financial: Sales ----

    /** Gross sales value for today (sum of all order totals booked today). */
    val todaySales: Money,

    /** Gross sales value for the current week (Mon–today). */
    val weeklySales: Money,

    /** Gross sales value for the current month (1st–today). */
    val monthlySales: Money,

    // ---- Financial: Receivables & Payables ----

    /**
     * Total amount owed by customers across all unpaid/partial invoices.
     * Commercial accounts receivable balance.
     */
    val customerDue: Money,

    /**
     * Total amount owed to vendors/suppliers for goods/services received.
     * Commercial accounts payable balance.
     * Full vendor payment workflow is implemented in a future module.
     */
    val vendorPayable: Money,

    // ---- Financial: Expense & Profit ----

    /**
     * Total operational expense for the current period (current month).
     * Includes production cost, overheads, salaries (summary only).
     * Full expense tracking is a later module.
     */
    val expense: Money,

    /**
     * Estimated gross profit for the current period.
     * Typically: monthlySales - expense - vendorPayable (approximation).
     * Full P&L calculation is a later module.
     */
    val profit: Money,

    // ---- Operational: Stock & Replacement ----

    /**
     * Total number of finished product SKUs currently tracked in inventory
     * (regardless of stock level). Useful for stock overview widget.
     */
    val finishedProductStockItems: Int,

    /**
     * Number of active replacement/rework requests.
     * A replacement occurs when a delivered order is returned for defect correction.
     * Full replacement workflow is a later module.
     */
    val replacementCount: Int,

    // ---- Operational: Affiliate ----

    /**
     * Total affiliate commission earned in the current period (month).
     * Commission is owed to affiliate partners who referred sales.
     * Full affiliate workflow is a later module.
     */
    val affiliateCommission: Money,

    // ---- Legacy / Backward-Compat Fields ----
    // These were the original minimal KPI fields. Retained for existing UI compatibility.

    /**
     * Amount received (cash + bank) today from customer payments.
     * Part of the payment breakdown; summarized here for quick reference.
     */
    val amountReceived: Money,

    /**
     * Total outstanding amount due from customers for today's invoiced orders.
     */
    val amountDue: Money
)

// =============================================================================
// PRODUCTION PIPELINE
// =============================================================================

/**
 * Job count (and optional estimated value) for a specific production stage.
 *
 * Uses [ProductionStageType] — the canonical 13-stage production workflow.
 * Do NOT use [OrderStatusType] for production stage counts.
 *
 * Example: PRINTING → count=12 means 12 jobs are currently in the Printing stage.
 */
data class StageCount(
    /** The production stage. Must be a [ProductionStageType] entry. */
    val stage: ProductionStageType,

    /** Number of active jobs currently at this stage. */
    val count: Int,

    /**
     * Sum of estimated job values for all jobs at this stage.
     * Zero if not applicable or not computed.
     */
    val totalEstimatedValue: Money
)

// =============================================================================
// PAYMENT SNAPSHOT
// =============================================================================

/**
 * Financial payment and receivables snapshot for the dashboard.
 * Used for the "Payment & Receivables" dashboard section.
 */
data class PaymentBreakdown(
    val paidCount: Int,
    val partialCount: Int,
    val dueCount: Int,
    val overdueCount: Int,
    val totalInvoicedToday: Money,
    val totalCollectedToday: Money,
    val totalOutstandingDue: Money,
    /** Collection efficiency ratio: totalCollectedToday / totalInvoicedToday (0.0 to 1.0). */
    val collectionRate: Float
)

// =============================================================================
// WORKLOAD SUMMARY
// =============================================================================

/**
 * Today's operational workload grouped by urgency.
 * Contains job summary lists — not counts (see [DashboardOperationalAlerts] for counts).
 */
data class WorkloadSummary(
    /** Jobs with a delivery deadline of today (any status). */
    val dueTodayJobs: List<DashboardJobSummary>,

    /** Jobs flagged as priority/rush. */
    val priorityJobs: List<DashboardJobSummary>,

    /** Jobs that have missed their scheduled delivery time. */
    val delayedJobs: List<DashboardJobSummary>
)

// =============================================================================
// JOB / ORDER SUMMARY
// =============================================================================

/**
 * Compact job/order summary card model for dashboard lists.
 *
 * Used in: recent orders list, workload section (due today, priority, delayed).
 *
 * CRITICAL: Two separate concepts are maintained here:
 *
 * - [jobStatus]: Commercial order lifecycle state ([OrderStatusType]).
 *   Tracks the commercial/business state (PENDING → CONFIRMED → IN_PRODUCTION → READY → DELIVERED).
 *   Do NOT use [ProductionStageType] for this field.
 *
 * - [currentProductionStage]: Current position in the 13-stage production pipeline ([ProductionStageType]).
 *   Null when the order is PENDING/CONFIRMED (not yet in production).
 *   Do NOT use [OrderStatusType] for this field.
 *
 * These two fields MUST remain separate and MUST NOT be merged.
 */
data class DashboardJobSummary(
    val orderId: String,
    val customerName: String,
    val customerPhone: String,
    val jobTitle: String,
    val quantity: Int,
    val itemType: String,
    val totalAmount: Money,
    val dueAmount: Money,

    /**
     * Commercial order lifecycle state.
     * Example: PENDING, CONFIRMED, IN_PRODUCTION, READY, DELIVERED, ON_HOLD, CANCELLED.
     */
    val jobStatus: OrderStatusType,

    /**
     * Current production stage in the 13-stage canonical pipeline.
     * Null if not yet in production (PENDING/CONFIRMED) or if already delivered/cancelled.
     */
    val currentProductionStage: ProductionStageType? = null,

    val paymentStatus: PaymentStatusType,

    /** Human-readable delivery due time/date string (e.g. "Today, 3:00 PM", "Aug 18, 4:00 PM"). */
    val deliveryDueTime: String,

    val isPriority: Boolean = false,
    val isDelayed: Boolean = false
)

// =============================================================================
// INVENTORY ALERT
// =============================================================================

/**
 * Finished product stock alert for the dashboard inventory summary.
 *
 * Represents LOW_STOCK or OUT_OF_STOCK conditions for FINISHED/SALEABLE products ONLY.
 *
 * Examples of products tracked here:
 *   - Quran Sharif, Qaida, Ampara, Tajwid, Information Books
 *   - Calendars, Diaries
 *   - Corporate Gift Items, Promotional Merchandise
 *
 * ⚠️ Raw materials (paper, ink, plates, chemicals, lamination film) are NOT tracked here.
 * Sucharu Pro does not maintain raw material inventory — only finished product stock.
 *
 * [stockStatus] uses canonical [StockStatusType]: IN_STOCK, LOW_STOCK, OUT_OF_STOCK.
 */
data class DashboardInventoryAlert(
    val itemId: String,
    val itemName: String,
    /** Product category (e.g. "Religious Book", "Calendar", "Corporate Gift", "Diary"). */
    val category: String,
    val currentStock: Double,
    val minThreshold: Double,
    /** Unit of measurement (e.g. "Pcs", "Sets", "Boxes"). */
    val unit: String,
    val stockStatus: StockStatusType
)
