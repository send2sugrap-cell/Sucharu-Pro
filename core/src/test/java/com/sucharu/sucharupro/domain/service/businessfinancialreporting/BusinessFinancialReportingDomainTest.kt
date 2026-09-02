package com.sucharu.sucharupro.domain.service.businessfinancialreporting

import com.sucharu.sucharupro.data.datasource.businesscost.FakeBusinessCostManagementDataSource
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessfinancialreporting.FakeBusinessFinancialReportingDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.businesscost.BusinessCostManagementRepositoryImpl
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessfinancialreporting.BusinessFinancialReportingRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.businessexpense.*
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.*
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialReportingDomainTest {

    private lateinit var reportingRepository: BusinessFinancialReportingRepositoryImpl
    private lateinit var expenseRepo: BusinessExpenseRepositoryImpl
    private lateinit var payableRepo: VendorPayableRepositoryImpl
    private lateinit var ledgerRepo: BusinessLedgerRepositoryImpl
    private lateinit var costRepo: BusinessCostManagementRepositoryImpl
    private lateinit var costControlRepo: BusinessCostControlRepositoryImpl
    private lateinit var reconciliationRepo: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var adjustmentRepo: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var service: BusinessFinancialReportingServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-001"

    @Before
    fun setup() {
        val fakeReportingDs = FakeBusinessFinancialReportingDataSource()
        reportingRepository = BusinessFinancialReportingRepositoryImpl(fakeReportingDs)
        expenseRepo = BusinessExpenseRepositoryImpl(FakeBusinessExpenseDataSource())
        payableRepo = VendorPayableRepositoryImpl(FakeVendorPayableDataSource())
        ledgerRepo = BusinessLedgerRepositoryImpl(FakeBusinessLedgerDataSource())
        costRepo = BusinessCostManagementRepositoryImpl(FakeBusinessCostManagementDataSource())
        costControlRepo = BusinessCostControlRepositoryImpl(FakeBusinessCostControlDataSource())
        reconciliationRepo = BusinessFinancialReconciliationRepositoryImpl(FakeBusinessFinancialReconciliationDataSource())
        adjustmentRepo = BusinessFinancialAdjustmentRepositoryImpl(FakeBusinessFinancialAdjustmentDataSource())

        service = BusinessFinancialReportingServiceImpl(
            reportingRepository = reportingRepository,
            expenseRepository = expenseRepo,
            payableRepository = payableRepo,
            ledgerRepository = ledgerRepo,
            costManagementRepository = costRepo,
            costControlRepository = costControlRepo,
            reconciliationRepository = reconciliationRepo,
            adjustmentRepository = adjustmentRepo,
            orderRepository = null,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testExecutiveSummaryCalculation() = runBlocking {
        // Seed Expense
        expenseRepo.createExpense(
            BusinessExpense(
                expenseId = "EXP-1",
                tenantId = tenantId,
                projectId = projectId,
                expenseNumber = "EXP-2026-001",
                description = "Office Supplies",
                expenseCategoryId = "CAT-OFFICE",
                amount = BigDecimal("1500.0000"),
                currency = "BDT",
                expenseDate = System.currentTimeMillis(),
                status = BusinessExpenseStatus.APPROVED,
                paymentMethod = BusinessExpensePaymentMethod.BANK,
                createdBy = "user1"
            )
        )

        // Seed Payable
        payableRepo.createPayable(
            VendorPayable(
                payableId = "PAY-1",
                tenantId = tenantId,
                projectId = projectId,
                payableNumber = "PAY-2026-001",
                vendorId = "VEND-1",
                description = "Paper Supplier Ltd",
                originalAmount = BigDecimal("5000.0000"),
                paidAmount = BigDecimal("2000.0000"),
                currency = "BDT",
                issueDate = System.currentTimeMillis(),
                dueDate = System.currentTimeMillis() + 86400000L,
                status = VendorPayableStatus.PARTIALLY_PAID,
                createdBy = "user1"
            )
        )

        val filter = BusinessFinancialReportFilter(tenantId = tenantId, projectId = projectId, currency = "BDT")
        val summary = service.generateExecutiveSummary(filter)

        assertEquals(BigDecimal("1500.0000"), summary.totalExpenseAmount)
        assertEquals(BigDecimal("1500.0000"), summary.approvedExpenseAmount)
        assertEquals(1, summary.expenseCount)
        assertEquals(BigDecimal("5000.0000"), summary.totalPayableAmount)
        assertEquals(BigDecimal("3000.0000"), summary.outstandingPayableAmount)
        assertEquals(1, summary.payableCount)
    }

    @Test
    fun testExpenseAnalyticsBreakdown() = runBlocking {
        expenseRepo.createExpense(
            BusinessExpense(
                expenseId = "EXP-1",
                tenantId = tenantId,
                projectId = projectId,
                expenseNumber = "EXP-2026-001",
                description = "Raw Paper Stock",
                expenseCategoryId = "RAW_MATERIALS",
                amount = BigDecimal("10000.0000"),
                currency = "BDT",
                expenseDate = System.currentTimeMillis(),
                status = BusinessExpenseStatus.APPROVED,
                paymentMethod = BusinessExpensePaymentMethod.BANK,
                createdBy = "user1"
            )
        )
        expenseRepo.createExpense(
            BusinessExpense(
                expenseId = "EXP-2",
                tenantId = tenantId,
                projectId = projectId,
                expenseNumber = "EXP-2026-002",
                description = "Printing Ink",
                expenseCategoryId = "CONSUMABLES",
                amount = BigDecimal("5000.0000"),
                currency = "BDT",
                expenseDate = System.currentTimeMillis(),
                status = BusinessExpenseStatus.APPROVED,
                paymentMethod = BusinessExpensePaymentMethod.CASH,
                createdBy = "user1"
            )
        )

        val filter = BusinessFinancialReportFilter(tenantId = tenantId, projectId = projectId, currency = "BDT")
        val report = service.generateExpenseAnalytics(filter)

        assertEquals(BigDecimal("15000.0000"), report.totalAmount)
        assertEquals(BigDecimal("7500.0000"), report.averageAmount)
        assertEquals(2, report.totalCount)
        assertEquals(2, report.categoryBreakdown.size)
        assertEquals("RAW_MATERIALS", report.categoryBreakdown[0].category)
        assertEquals(BigDecimal("10000.0000"), report.categoryBreakdown[0].totalAmount)
        assertEquals(BigDecimal("66.67"), report.categoryBreakdown[0].percentage)
    }

    @Test
    fun testVendorPayableAgingBuckets() = runBlocking {
        val now = System.currentTimeMillis()
        val oneDayMs = 86400000L

        // Current payable
        payableRepo.createPayable(
            VendorPayable(
                payableId = "PAY-1",
                tenantId = tenantId,
                projectId = projectId,
                payableNumber = "PAY-2026-001",
                vendorId = "VEND-1",
                description = "Vendor One",
                originalAmount = BigDecimal("1000.0000"),
                paidAmount = BigDecimal.ZERO,
                currency = "BDT",
                issueDate = now - 5 * oneDayMs,
                dueDate = now + 5 * oneDayMs,
                status = VendorPayableStatus.APPROVED,
                createdBy = "user1"
            )
        )

        // 10 days overdue
        payableRepo.createPayable(
            VendorPayable(
                payableId = "PAY-2",
                tenantId = tenantId,
                projectId = projectId,
                payableNumber = "PAY-2026-002",
                vendorId = "VEND-2",
                description = "Vendor Two",
                originalAmount = BigDecimal("2000.0000"),
                paidAmount = BigDecimal.ZERO,
                currency = "BDT",
                issueDate = now - 20 * oneDayMs,
                dueDate = now - 10 * oneDayMs,
                status = VendorPayableStatus.APPROVED,
                createdBy = "user1"
            )
        )

        val filter = BusinessFinancialReportFilter(tenantId = tenantId, projectId = projectId, currency = "BDT")
        val report = service.generateVendorPayableAnalytics(filter)

        assertEquals(BigDecimal("3000.0000"), report.totalPayable)
        assertEquals(BigDecimal("3000.0000"), report.outstandingAmount)
        assertEquals(BigDecimal("2000.0000"), report.overdueAmount)
        assertEquals(BigDecimal("1000.0000"), report.currentAmount)

        val currentBucket = report.agingBuckets.find { it.bucketType == PayableAgingBucketType.CURRENT }
        val d1To30Bucket = report.agingBuckets.find { it.bucketType == PayableAgingBucketType.DAYS_1_TO_30 }

        assertNotNull(currentBucket)
        assertEquals(BigDecimal("1000.0000"), currentBucket!!.amount)
        assertNotNull(d1To30Bucket)
        assertEquals(BigDecimal("2000.0000"), d1To30Bucket!!.amount)
    }

    @Test
    fun testPeriodEndReadinessDiagnostics() = runBlocking {
        // Create period
        val period = costControlRepo.createFinancialPeriod(
            BusinessFinancialPeriod(
                id = "PER-2026-08",
                tenantId = tenantId,
                projectId = projectId,
                periodCode = "2026-08",
                periodName = "August 2026",
                startDate = System.currentTimeMillis() - 864000000L,
                endDate = System.currentTimeMillis() + 864000000L,
                status = BusinessFinancialPeriodStatus.OPEN,
                createdBy = "admin"
            )
        )

        // Without reconciliation, readiness must be NOT_READY with blocker
        val readinessReport = service.generatePeriodEndReadinessReport(tenantId, projectId, period.id)
        assertEquals(PeriodReadinessStatus.NOT_READY, readinessReport.readinessStatus)
        assertTrue(readinessReport.blockerCount > 0)
        assertTrue(readinessReport.blockers.any { it.code == "MISSING_RECONCILIATION" })
    }

    @Test
    fun testSnapshotCreationAndIntegrityVerification() = runBlocking {
        val filter = BusinessFinancialReportFilter(
            tenantId = tenantId,
            projectId = projectId,
            reportType = BusinessFinancialReportType.EXECUTIVE_SUMMARY
        )
        val metricsJson = "{\"totalExpenseAmount\": 1500.0000, \"totalPayableAmount\": 5000.0000}"

        val snapshot = service.createReportSnapshot(filter, metricsJson, "admin")

        assertNotNull(snapshot.snapshotId)
        assertTrue(snapshot.integrityHash.isNotBlank())
        assertTrue(snapshot.isImmutable)

        val retrieved = service.getReportSnapshot(tenantId, snapshot.snapshotId)
        assertNotNull(retrieved)
        assertEquals(snapshot.integrityHash, retrieved!!.integrityHash)

        // Verify SHA-256 hash recalculation
        val recomputedHash = BusinessFinancialReportSnapshot.calculateIntegrityHash(
            tenantId = snapshot.tenantId,
            projectId = snapshot.projectId,
            reportType = snapshot.reportType,
            metricsPayloadJson = metricsJson,
            generatedAt = snapshot.generatedAt
        )
        assertEquals(snapshot.integrityHash, recomputedHash)
    }

    @Test
    fun testExportReportCsvAndTextGeneration() = runBlocking {
        val filterCsv = BusinessFinancialReportFilter(
            tenantId = tenantId,
            projectId = projectId,
            reportType = BusinessFinancialReportType.EXECUTIVE_SUMMARY,
            format = BusinessFinancialReportFormat.CSV
        )
        val csvDoc = service.exportReport(filterCsv)
        assertNotNull(csvDoc.documentId)
        assertEquals(BusinessFinancialReportFormat.CSV, csvDoc.format)
        assertTrue(csvDoc.fileName.endsWith(".csv"))
        assertTrue(csvDoc.contentString.contains("Metric,Value,Currency"))

        val filterText = BusinessFinancialReportFilter(
            tenantId = tenantId,
            projectId = projectId,
            reportType = BusinessFinancialReportType.EXECUTIVE_SUMMARY,
            format = BusinessFinancialReportFormat.PDF_TEXT
        )
        val textDoc = service.exportReport(filterText)
        assertNotNull(textDoc.documentId)
        assertEquals(BusinessFinancialReportFormat.PDF_TEXT, textDoc.format)
        assertTrue(textDoc.fileName.endsWith(".txt"))
        assertTrue(textDoc.contentString.contains("SUCHARU PRO ERP — BUSINESS FINANCIAL REPORT"))
    }
}
