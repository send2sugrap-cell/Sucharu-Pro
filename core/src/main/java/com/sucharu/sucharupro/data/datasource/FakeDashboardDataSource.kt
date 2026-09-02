package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.dashboard.DashboardInventoryAlert
import com.sucharu.sucharupro.domain.model.dashboard.DashboardJobSummary
import com.sucharu.sucharupro.domain.model.dashboard.DashboardKpis
import com.sucharu.sucharupro.domain.model.dashboard.DashboardOperationalAlerts
import com.sucharu.sucharupro.domain.model.dashboard.DashboardSummary
import com.sucharu.sucharupro.domain.model.dashboard.PaymentBreakdown
import com.sucharu.sucharupro.domain.model.dashboard.ShopHeaderInfo
import com.sucharu.sucharupro.domain.model.dashboard.StageCount
import com.sucharu.sucharupro.domain.model.dashboard.WorkloadSummary
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.inventory.StockStatusType
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.payment.PaymentStatusType
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * ⚠️ TEMPORARY: In-memory fake implementation of [DashboardDataSource].
 *
 * Provides realistic sample data for development and Dashboard UI prototyping.
 * All data represents a real-world commercial printing press operation day.
 * Financial figures are in BDT (Bangladeshi Taka).
 *
 * Replace this with:
 *  - `RoomDashboardDataSource` — after Room database module is established (Module 07+)
 *  - `RemoteDashboardDataSource` — after backend API integration
 *
 * Data integrity rules enforced here:
 *  1. Production pipeline uses canonical [ProductionStageType] (13 stages) only.
 *  2. Commercial order states use canonical [OrderStatusType] (7 values) only.
 *  3. [DashboardJobSummary] keeps `jobStatus` and `currentProductionStage` SEPARATE.
 *  4. Inventory alerts contain FINISHED PRODUCTS ONLY (no raw materials).
 *  5. All monetary values use [Money] (BigDecimal-backed, no floating-point).
 *
 * Data consistency rules:
 *  - PENDING order       → currentProductionStage = null
 *  - CONFIRMED order     → may be null or early stage (DESIGN/APPROVAL)
 *  - IN_PRODUCTION order → should have a non-null production stage
 *  - READY order         → currentProductionStage = READY
 *  - DELIVERED order     → currentProductionStage = DELIVERED
 *  - ON_HOLD order       → currentProductionStage may vary
 *  - CANCELLED order     → currentProductionStage = null
 */
class FakeDashboardDataSource : DashboardDataSource {

    override suspend fun fetchDashboardSummary(): DomainResult<DashboardSummary> =
        DomainResult.of { buildSampleDashboardSummary() }

    override suspend fun fetchKpis(): DomainResult<DashboardKpis> =
        DomainResult.of { buildSampleKpis() }

    override suspend fun fetchStageCounts(): DomainResult<List<StageCount>> =
        DomainResult.of { buildSampleStageCounts() }

    override suspend fun fetchRecentOrders(limit: Int): DomainResult<List<DashboardJobSummary>> =
        DomainResult.of { buildSampleRecentOrders().take(limit) }

    override suspend fun fetchInventoryAlerts(): DomainResult<List<DashboardInventoryAlert>> =
        DomainResult.of { buildSampleInventoryAlerts() }

    override suspend fun fetchOperationalAlerts(): DomainResult<DashboardOperationalAlerts> =
        DomainResult.of { buildSampleOperationalAlerts() }

    // =========================================================================
    // Private Sample Data Builders
    // =========================================================================

    private fun buildSampleDashboardSummary(): DashboardSummary {
        val recentOrders = buildSampleRecentOrders()
        return DashboardSummary(
            shopHeader = ShopHeaderInfo(
                shopName      = "Sucharu Printing & Packaging Press",
                ownerName     = "Shafiqul Islam (Manager)",
                formattedDate = "Saturday, 15 August 2026",
                activeShift   = "Shift A (8:00 AM – 4:00 PM)"
            ),
            kpis              = buildSampleKpis(),
            stageCounts       = buildSampleStageCounts(),
            paymentBreakdown  = buildSamplePaymentBreakdown(),
            workloadSummary   = WorkloadSummary(
                dueTodayJobs  = recentOrders.filter { it.deliveryDueTime.startsWith("Today") },
                priorityJobs  = recentOrders.filter { it.isPriority },
                delayedJobs   = recentOrders.filter { it.isDelayed }
            ),
            recentOrders      = recentOrders,
            inventoryAlerts   = buildSampleInventoryAlerts(),
            operationalAlerts = buildSampleOperationalAlerts()
        )
    }

    private fun buildSampleKpis(): DashboardKpis = DashboardKpis(
        // ---- Volume ----
        todayOrdersCount          = 18,
        activeJobsCount           = 14,
        readyJobsCount            = 7,
        deliveredJobsCount        = 12,

        // ---- Sales ----
        todaySales                = 84_500.toMoney(),
        weeklySales               = 342_000.toMoney(),
        monthlySales              = 1_185_000.toMoney(),

        // ---- Receivables / Payables ----
        customerDue               = 328_500.toMoney(),
        vendorPayable             = 95_000.toMoney(),
        amountReceived            = 52_000.toMoney(),   // today's collected
        amountDue                 = 32_500.toMoney(),   // today's outstanding

        // ---- Expense & Profit ----
        expense                   = 410_000.toMoney(),
        profit                    = 775_000.toMoney(),  // simplified: monthlySales - expense

        // ---- Stock & Replacement ----
        finishedProductStockItems = 24,                 // 24 finished product SKUs tracked
        replacementCount          = 1,

        // ---- Affiliate ----
        affiliateCommission       = 12_500.toMoney()
    )

    private fun buildSampleStageCounts(): List<StageCount> = listOf(
        // ------------------------------------------------------------------
        // All 13 canonical ProductionStageType entries.
        // Do NOT use OrderStatusType here.
        // ------------------------------------------------------------------
        StageCount(ProductionStageType.DESIGN,        count = 4,  totalEstimatedValue = 48_000.toMoney()),
        StageCount(ProductionStageType.APPROVAL,      count = 3,  totalEstimatedValue = 26_500.toMoney()),
        StageCount(ProductionStageType.QC,            count = 2,  totalEstimatedValue = 18_000.toMoney()),
        StageCount(ProductionStageType.ITEM_APPROVAL, count = 1,  totalEstimatedValue = 8_500.toMoney()),
        StageCount(ProductionStageType.CTP,           count = 3,  totalEstimatedValue = 32_000.toMoney()),
        StageCount(ProductionStageType.PRINTING,      count = 6,  totalEstimatedValue = 68_000.toMoney()),
        StageCount(ProductionStageType.LAMINATION,    count = 5,  totalEstimatedValue = 41_200.toMoney()),
        StageCount(ProductionStageType.FOLDING,       count = 4,  totalEstimatedValue = 28_000.toMoney()),
        StageCount(ProductionStageType.BINDING,       count = 2,  totalEstimatedValue = 16_000.toMoney()),
        StageCount(ProductionStageType.FINAL_QC,      count = 3,  totalEstimatedValue = 24_000.toMoney()),
        StageCount(ProductionStageType.PACKAGING,     count = 4,  totalEstimatedValue = 38_000.toMoney()),
        StageCount(ProductionStageType.READY,         count = 7,  totalEstimatedValue = 94_000.toMoney()),
        StageCount(ProductionStageType.DELIVERED,     count = 12, totalEstimatedValue = 118_000.toMoney())
    )

    private fun buildSamplePaymentBreakdown(): PaymentBreakdown = PaymentBreakdown(
        paidCount           = 15,
        partialCount        = 8,
        dueCount            = 5,
        overdueCount        = 2,
        totalInvoicedToday  = 84_500.toMoney(),
        totalCollectedToday = 52_000.toMoney(),
        totalOutstandingDue = 32_500.toMoney(),
        collectionRate      = 0.615f   // 61.5%
    )

    private fun buildSampleOperationalAlerts(): DashboardOperationalAlerts =
        DashboardOperationalAlerts(
            pendingApprovalCount    = 3,   // 3 jobs at APPROVAL stage, customer not yet responded
            qcPendingCount          = 2,   // 2 jobs at QC / FINAL_QC stage, not yet inspected
            deliveryPendingCount    = 5,   // 5 READY jobs with delivery scheduled for today
            lowStockAlertCount      = 4,   // 4 finished product SKUs below minimum stock threshold
            outstandingPaymentCount = 7,   // 7 invoices overdue or unpaid past delivery date
            vendorDueCount          = 2,   // 2 vendor bills currently due or overdue
            replacementPendingCount = 1,   // 1 replacement/rework request awaiting completion
            delayedJobsCount        = 3    // 3 production jobs past their scheduled delivery date
        )

    /**
     * Builds a list of sample recent orders/jobs.
     *
     * Each entry demonstrates the distinction between:
     *  - [OrderStatusType]     : commercial lifecycle (PENDING/CONFIRMED/IN_PRODUCTION/READY/DELIVERED/ON_HOLD/CANCELLED)
     *  - [ProductionStageType] : production position in 13-stage pipeline (null if not yet in production)
     *
     * Data consistency is maintained:
     *  - PENDING      → currentProductionStage = null
     *  - CONFIRMED    → currentProductionStage = DESIGN (design started, order not yet in production)
     *  - IN_PRODUCTION → currentProductionStage is a mid-pipeline stage
     *  - ON_HOLD      → currentProductionStage reflects where the job was paused
     *  - READY        → currentProductionStage = READY
     *  - DELIVERED    → currentProductionStage = DELIVERED
     *  - CANCELLED    → currentProductionStage = null
     */
    private fun buildSampleRecentOrders(): List<DashboardJobSummary> = listOf(

        // ---- PENDING: no production stage yet ----
        DashboardJobSummary(
            orderId                = "JOB-2026-110",
            customerName           = "Dhaka City College",
            customerPhone          = "+880 1711-001122",
            jobTitle               = "10,000 Pcs Admission Brochure (A4, 4-Color, 130 GSM Art Paper)",
            quantity               = 10_000,
            itemType               = "Brochure",
            totalAmount            = 32_000.toMoney(),
            dueAmount              = 32_000.toMoney(),
            jobStatus              = OrderStatusType.PENDING,
            currentProductionStage = null,                   // not yet confirmed — no stage assigned
            paymentStatus          = PaymentStatusType.UNPAID,
            deliveryDueTime        = "Aug 22, 12:00 PM",
            isPriority             = false,
            isDelayed              = false
        ),

        // ---- CONFIRMED: design stage just started ----
        DashboardJobSummary(
            orderId                = "JOB-2026-103",
            customerName           = "Standard Pharma Distributors",
            customerPhone          = "+880 1912-345678",
            jobTitle               = "2,000 Pcs Multi-Color Cash Memo (3-Part Carbonless 55 GSM)",
            quantity               = 2_000,
            itemType               = "Cash Memo",
            totalAmount            = 14_200.toMoney(),
            dueAmount              = 14_200.toMoney(),
            jobStatus              = OrderStatusType.CONFIRMED,
            currentProductionStage = ProductionStageType.APPROVAL, // design done, waiting customer approval
            paymentStatus          = PaymentStatusType.UNPAID,
            deliveryDueTime        = "Tomorrow, 11:00 AM",
            isPriority             = false,
            isDelayed              = false
        ),

        // ---- IN_PRODUCTION: PRINTING stage ----
        DashboardJobSummary(
            orderId                = "JOB-2026-101",
            customerName           = "Bengal Software Ltd.",
            customerPhone          = "+880 1711-234567",
            jobTitle               = "5,000 Pcs Tri-fold Corporate Brochure (170 GSM Art Paper)",
            quantity               = 5_000,
            itemType               = "Brochure",
            totalAmount            = 24_500.toMoney(),
            dueAmount              = Money.ZERO,
            jobStatus              = OrderStatusType.IN_PRODUCTION,
            currentProductionStage = ProductionStageType.PRINTING,
            paymentStatus          = PaymentStatusType.PAID,
            deliveryDueTime        = "Today, 3:00 PM",
            isPriority             = true,
            isDelayed              = false
        ),

        // ---- IN_PRODUCTION: LAMINATION stage ----
        DashboardJobSummary(
            orderId                = "JOB-2026-102",
            customerName           = "Aura Fashion House",
            customerPhone          = "+880 1819-876543",
            jobTitle               = "10,000 Pcs Premium Visiting Cards (300 GSM Art Card, Spot UV)",
            quantity               = 10_000,
            itemType               = "Visiting Card",
            totalAmount            = 6_800.toMoney(),
            dueAmount              = 2_800.toMoney(),
            jobStatus              = OrderStatusType.IN_PRODUCTION,
            currentProductionStage = ProductionStageType.LAMINATION,
            paymentStatus          = PaymentStatusType.PARTIAL,
            deliveryDueTime        = "Today, 5:30 PM",
            isPriority             = true,
            isDelayed              = false
        ),

        // ---- IN_PRODUCTION: DESIGN stage ----
        DashboardJobSummary(
            orderId                = "JOB-2026-104",
            customerName           = "Crestview Holdings",
            customerPhone          = "+880 1610-987654",
            jobTitle               = "1,000 Pcs Hardcover Annual Report (100 GSM Cream Paper, Gold Foil)",
            quantity               = 1_000,
            itemType               = "Book / Annual Report",
            totalAmount            = 38_000.toMoney(),
            dueAmount              = 15_000.toMoney(),
            jobStatus              = OrderStatusType.IN_PRODUCTION,
            currentProductionStage = ProductionStageType.DESIGN,
            paymentStatus          = PaymentStatusType.PARTIAL,
            deliveryDueTime        = "Aug 18, 4:00 PM",
            isPriority             = false,
            isDelayed              = false
        ),

        // ---- IN_PRODUCTION: BINDING stage ----
        DashboardJobSummary(
            orderId                = "JOB-2026-109",
            customerName           = "Noor Academy Trust",
            customerPhone          = "+880 1811-667788",
            jobTitle               = "3,000 Pcs Noorani Qaida (Standard 36-Page, Offset Print)",
            quantity               = 3_000,
            itemType               = "Religious Book",
            totalAmount            = 31_500.toMoney(),
            dueAmount              = 12_000.toMoney(),
            jobStatus              = OrderStatusType.IN_PRODUCTION,
            currentProductionStage = ProductionStageType.BINDING,
            paymentStatus          = PaymentStatusType.PARTIAL,
            deliveryDueTime        = "Aug 17, 2:00 PM",
            isPriority             = false,
            isDelayed              = false
        ),

        // ---- IN_PRODUCTION: PRINTING — DELAYED ----
        DashboardJobSummary(
            orderId                = "JOB-2026-098",
            customerName           = "Green Leaf Tea Co.",
            customerPhone          = "+880 1733-445566",
            jobTitle               = "15,000 Pcs Tea Pouch Sticker Labels (Mirror Coat Chromo, Die-Cut)",
            quantity               = 15_000,
            itemType               = "Sticker Label",
            totalAmount            = 12_500.toMoney(),
            dueAmount              = 4_500.toMoney(),
            jobStatus              = OrderStatusType.IN_PRODUCTION,
            currentProductionStage = ProductionStageType.PRINTING,
            paymentStatus          = PaymentStatusType.PARTIAL,
            deliveryDueTime        = "Today, 11:30 AM (Delayed ~1h)",
            isPriority             = true,
            isDelayed              = true
        ),

        // ---- ON_HOLD: paused at QC ----
        DashboardJobSummary(
            orderId                = "JOB-2026-107",
            customerName           = "Prime Textiles Ltd.",
            customerPhone          = "+880 1614-778899",
            jobTitle               = "50,000 Pcs Hang Tags (350 GSM Board, 2-Color, Die-Cut, String)",
            quantity               = 50_000,
            itemType               = "Hang Tag",
            totalAmount            = 28_000.toMoney(),
            dueAmount              = 10_000.toMoney(),
            jobStatus              = OrderStatusType.ON_HOLD,
            currentProductionStage = ProductionStageType.QC,    // paused at QC — customer requested color change
            paymentStatus          = PaymentStatusType.PARTIAL,
            deliveryDueTime        = "Aug 20, 5:00 PM",
            isPriority             = false,
            isDelayed              = false
        ),

        // ---- READY: production complete, awaiting dispatch ----
        DashboardJobSummary(
            orderId                = "JOB-2026-105",
            customerName           = "Apex BioCare Ltd.",
            customerPhone          = "+880 1715-554433",
            jobTitle               = "25,000 Pcs Medicine Packaging Cartons (350 GSM Duplex, Die-Cut)",
            quantity               = 25_000,
            itemType               = "Packaging Box",
            totalAmount            = 72_000.toMoney(),
            dueAmount              = Money.ZERO,
            jobStatus              = OrderStatusType.READY,
            currentProductionStage = ProductionStageType.READY,
            paymentStatus          = PaymentStatusType.PAID,
            deliveryDueTime        = "Today, 1:00 PM",
            isPriority             = true,
            isDelayed              = false
        ),

        // ---- DELIVERED: fully complete ----
        DashboardJobSummary(
            orderId                = "JOB-2026-106",
            customerName           = "Spice Garden Lounge",
            customerPhone          = "+880 1822-112233",
            jobTitle               = "500 Pcs Restaurant Menu Book (Synthetic Waterproof 250 Micron)",
            quantity               = 500,
            itemType               = "Menu Book",
            totalAmount            = 18_500.toMoney(),
            dueAmount              = Money.ZERO,
            jobStatus              = OrderStatusType.DELIVERED,
            currentProductionStage = ProductionStageType.DELIVERED,
            paymentStatus          = PaymentStatusType.PAID,
            deliveryDueTime        = "Delivered (10:15 AM)",
            isPriority             = false,
            isDelayed              = false
        ),

        // ---- CANCELLED: terminated before production ----
        DashboardJobSummary(
            orderId                = "JOB-2026-099",
            customerName           = "Horizon Events Co.",
            customerPhone          = "+880 1917-334455",
            jobTitle               = "2,000 Pcs Event Invitation Cards (300 GSM Pearlescent, Foil)",
            quantity               = 2_000,
            itemType               = "Invitation Card",
            totalAmount            = 9_500.toMoney(),
            dueAmount              = Money.ZERO,
            jobStatus              = OrderStatusType.CANCELLED,
            currentProductionStage = null,    // cancelled before production began
            paymentStatus          = PaymentStatusType.PAID,   // advance refunded
            deliveryDueTime        = "Cancelled",
            isPriority             = false,
            isDelayed              = false
        )
    )

    /**
     * Builds sample finished product stock alerts.
     *
     * ⚠️ FINISHED PRODUCTS ONLY — no raw materials.
     *
     * Products tracked: Quran Sharif, Qaida, Ampara, Calendars, Gift Items, Diaries.
     * NOT tracked: Paper, Ink, Plates, Chemicals, Lamination Film.
     *
     * [StockStatusType] values used: IN_STOCK (excluded from alerts), LOW_STOCK, OUT_OF_STOCK.
     */
    private fun buildSampleInventoryAlerts(): List<DashboardInventoryAlert> = listOf(
        DashboardInventoryAlert(
            itemId       = "FP-QRN-001",
            itemName     = "Quran Sharif — Large (Hardcover, A4)",
            category     = "Religious Book",
            currentStock = 15.0,
            minThreshold = 50.0,
            unit         = "Pcs",
            stockStatus  = StockStatusType.LOW_STOCK
        ),
        DashboardInventoryAlert(
            itemId       = "FP-QDA-002",
            itemName     = "Noorani Qaida (Standard Edition)",
            category     = "Religious Book",
            currentStock = 8.0,
            minThreshold = 30.0,
            unit         = "Pcs",
            stockStatus  = StockStatusType.LOW_STOCK
        ),
        DashboardInventoryAlert(
            itemId       = "FP-AMP-003",
            itemName     = "Ampara (30-Para, Pocket Size)",
            category     = "Religious Book",
            currentStock = 0.0,
            minThreshold = 25.0,
            unit         = "Pcs",
            stockStatus  = StockStatusType.OUT_OF_STOCK
        ),
        DashboardInventoryAlert(
            itemId       = "FP-CAL-2027",
            itemName     = "Wall Calendar 2027 (12-Sheet, Full Color)",
            category     = "Calendar",
            currentStock = 0.0,
            minThreshold = 20.0,
            unit         = "Pcs",
            stockStatus  = StockStatusType.OUT_OF_STOCK
        ),
        DashboardInventoryAlert(
            itemId       = "FP-DRY-2027",
            itemName     = "Desk Diary 2027 (A5, Spiral Bound)",
            category     = "Diary",
            currentStock = 12.0,
            minThreshold = 30.0,
            unit         = "Pcs",
            stockStatus  = StockStatusType.LOW_STOCK
        ),
        DashboardInventoryAlert(
            itemId       = "FP-GFT-PKG-01",
            itemName     = "Corporate Gift Box Set (Premium Branded)",
            category     = "Corporate Gift",
            currentStock = 5.0,
            minThreshold = 25.0,
            unit         = "Sets",
            stockStatus  = StockStatusType.LOW_STOCK
        )
    )
}
