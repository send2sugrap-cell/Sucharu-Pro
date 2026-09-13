package com.sucharu.sucharupro.domain.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Module24Step05FinancePaymentCostProfitabilityReportingTest {

    private lateinit var orderDs: FakeOrderDataSource
    private lateinit var customerDs: FakeCustomerDataSource
    private lateinit var execDs: FakeProductionExecutionDataSource
    private lateinit var qcDs: FakeProductionQcDataSource
    private lateinit var rwkDs: FakeProductionReworkDataSource
    private lateinit var challanDs: FakeDeliveryChallanDataSource
    private lateinit var shipmentDs: FakeDeliveryShipmentDataSource
    private lateinit var returnDs: FakeDeliveryReturnDataSource
    private lateinit var invoiceDs: FakeCustomerInvoiceDataSource
    private lateinit var paymentDs: FakeCustomerPaymentDataSource
    private lateinit var ledgerDs: FakeCustomerLedgerDataSource
    private lateinit var service: Module24ReportingServiceImpl

    private val tenantId = "TENANT-STEP5-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-001",
        projectId = tenantId,
        username = "admin_user",
        role = UserRole.ADMIN
    )

    private val accountsPrincipal = AuthenticatedPrincipal(
        userId = "acc-001",
        projectId = tenantId,
        username = "accounts_user",
        role = UserRole.STAFF
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cus-001",
        projectId = tenantId,
        username = "customer_user",
        role = UserRole.CUSTOMER,
        customerId = "cus-001"
    )

    @Before
    fun setUp() {
        orderDs = FakeOrderDataSource()
        customerDs = FakeCustomerDataSource()
        execDs = FakeProductionExecutionDataSource()
        qcDs = FakeProductionQcDataSource()
        rwkDs = FakeProductionReworkDataSource()
        challanDs = FakeDeliveryChallanDataSource()
        shipmentDs = FakeDeliveryShipmentDataSource()
        returnDs = FakeDeliveryReturnDataSource()
        invoiceDs = FakeCustomerInvoiceDataSource()
        paymentDs = FakeCustomerPaymentDataSource()
        ledgerDs = FakeCustomerLedgerDataSource()

        service = Module24ReportingServiceImpl(
            orderDataSource = orderDs,
            customerDataSource = customerDs,
            productionExecutionDataSource = execDs,
            productionQcDataSource = qcDs,
            productionReworkDataSource = rwkDs,
            deliveryChallanDataSource = challanDs,
            deliveryShipmentDataSource = shipmentDs,
            deliveryReturnDataSource = returnDs,
            customerInvoiceDataSource = invoiceDs,
            customerPaymentDataSource = paymentDs,
            customerLedgerDataSource = ledgerDs
        )
    }

    // =========================================================================
    // A. FINANCIAL & INVOICE REPORTING TESTS
    // =========================================================================
    @Test
    fun testFinanceSummary_reportsRevenueAndCollections() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.FINANCE,
            reportType = "FINANCE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.FINANCE, data.meta.reportCategory)
        assertEquals("FINANCE_SUMMARY", data.meta.reportType)

        val revenueMetric = data.summaryMetrics.find { it.metricId == "totalRevenue" }
        assertNotNull(revenueMetric)
        assertTrue(revenueMetric!!.value.startsWith("BDT"))
    }

    @Test
    fun testInvoiceSummary_reportsCustomerInvoicesAndDueAmount() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.FINANCE,
            reportType = "INVOICE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(accountsPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.rows.isNotEmpty())
        assertNotNull(data.rows.first().values["invoiceNumber"])
    }

    // =========================================================================
    // B. PAYMENT & PROFITABILITY REPORTING TESTS
    // =========================================================================
    @Test
    fun testPaymentSummary_reportsCollectedPayments() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.FINANCE,
            reportType = "PAYMENT_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        val collectedMetric = data.summaryMetrics.find { it.metricId == "totalCollected" }
        assertNotNull(collectedMetric)
    }

    @Test
    fun testProfitabilitySummary_calculatesRevenueCostAndGrossMargin() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.PROFITABILITY,
            reportType = "PROFITABILITY_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.PROFITABILITY, data.meta.reportCategory)

        val marginMetric = data.summaryMetrics.find { it.metricId == "grossMarginPercentage" }
        assertNotNull(marginMetric)
        assertTrue(marginMetric!!.value.endsWith("%"))
    }

    // =========================================================================
    // C. SECURITY, TENANT & IDENTITY SCOPE TESTS
    // =========================================================================
    @Test
    fun testCustomer_identityScope_restrictsToOwnInvoices() = runBlocking {
        val ownReq = ReportRequest(
            reportCategory = ReportCategory.FINANCE,
            reportType = "INVOICE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "cus-001")
        )

        val ownRes = service.queryReport(customerPrincipal, ownReq)
        assertTrue(ownRes is DomainResult.Success)

        val otherReq = ReportRequest(
            reportCategory = ReportCategory.FINANCE,
            reportType = "INVOICE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "cus-999-FOREIGN")
        )

        val otherRes = service.queryReport(customerPrincipal, otherReq)
        assertTrue(otherRes is DomainResult.Error)
    }

    @Test
    fun testCrossTenantRequest_strictlyDenied() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.FINANCE,
            reportType = "FINANCE_SUMMARY",
            tenantId = "TENANT-FOREIGN-XYZ",
            projectId = "TENANT-FOREIGN-XYZ"
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("Tenant isolation violation"))
    }

    // =========================================================================
    // D. EXPORT TESTS
    // =========================================================================
    @Test
    fun testExportFinanceSummary_generatesCsv() = runBlocking {
        val queryReq = ReportRequest(
            reportCategory = ReportCategory.FINANCE,
            reportType = "FINANCE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val exportReq = ExportReportRequest(queryRequest = queryReq, format = ReportExportFormat.CSV)
        val res = service.exportReport(adminPrincipal, exportReq)

        assertTrue(res is DomainResult.Success)
        val doc = (res as DomainResult.Success).data
        assertEquals("FINANCE_SUMMARY", doc.reportType)
        assertEquals(ReportExportFormat.CSV, doc.format)
        assertNotNull(doc.contentBase64)
    }
}
