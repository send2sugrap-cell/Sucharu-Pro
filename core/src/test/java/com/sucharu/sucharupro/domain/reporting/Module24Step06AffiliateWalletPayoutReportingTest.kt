package com.sucharu.sucharupro.domain.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.datasource.affiliate.*
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.*
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Module24Step06AffiliateWalletPayoutReportingTest {

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
    private lateinit var affiliateDs: FakeAffiliateDataSource
    private lateinit var walletDs: FakeAffiliateWalletDataSource
    private lateinit var walletLedgerDs: FakeAffiliateWalletLedgerDataSource
    private lateinit var payoutReqDs: FakeAffiliatePayoutRequestDataSource
    private lateinit var service: Module24ReportingServiceImpl

    private val tenantId = "TENANT-STEP6-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-001",
        projectId = tenantId,
        username = "admin_user",
        role = UserRole.ADMIN
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "stf-001",
        projectId = tenantId,
        username = "staff_user",
        role = UserRole.STAFF
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "aff-user-001",
        projectId = tenantId,
        username = "affiliate_user",
        role = UserRole.AFFILIATE,
        affiliateId = "aff-001"
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
        affiliateDs = FakeAffiliateDataSource()
        walletDs = FakeAffiliateWalletDataSource()
        walletLedgerDs = FakeAffiliateWalletLedgerDataSource()
        payoutReqDs = FakeAffiliatePayoutRequestDataSource()

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
            customerLedgerDataSource = ledgerDs,
            affiliateDataSource = affiliateDs,
            affiliateWalletDataSource = walletDs,
            affiliateWalletLedgerDataSource = walletLedgerDs,
            affiliatePayoutRequestDataSource = payoutReqDs
        )
    }

    // =========================================================================
    // A. AFFILIATE & EARNING REPORTING TESTS
    // =========================================================================
    @Test
    fun testAffiliateSummary_reportsManagedAffiliatesAndEarnings() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.AFFILIATE,
            reportType = "AFFILIATE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.AFFILIATE, data.meta.reportCategory)
        assertEquals("AFFILIATE_SUMMARY", data.meta.reportType)

        val totalAffiliatesMetric = data.summaryMetrics.find { it.metricId == "totalAffiliatesCount" }
        assertNotNull(totalAffiliatesMetric)
        assertTrue(totalAffiliatesMetric!!.numericValue!! >= 1.0)
    }

    @Test
    fun testAffiliateByStatus_reportsStatusGrid() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.AFFILIATE,
            reportType = "AFFILIATE_BY_STATUS",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(staffPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.rows.isNotEmpty())
        assertNotNull(data.rows.first().values["affiliateCode"])
    }

    // =========================================================================
    // B. WALLET & PAYOUT REPORTING TESTS
    // =========================================================================
    @Test
    fun testWalletSummary_reportsAvailableBalanceAndDisbursements() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.WALLET_PAYOUT,
            reportType = "WALLET_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.WALLET_PAYOUT, data.meta.reportCategory)

        val availableBalanceMetric = data.summaryMetrics.find { it.metricId == "availableBalance" }
        assertNotNull(availableBalanceMetric)
        assertTrue(availableBalanceMetric!!.value.startsWith("BDT"))
    }

    @Test
    fun testPayoutByStatus_reportsDisbursementStatusGrid() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.WALLET_PAYOUT,
            reportType = "PAYOUT_BY_STATUS",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.rows.isNotEmpty())
        assertNotNull(data.rows.first().values["requestId"])
    }

    // =========================================================================
    // C. SECURITY, TENANT & AFFILIATE SELF-SCOPE TESTS
    // =========================================================================
    @Test
    fun testAffiliate_selfScope_restrictsToOwnRecord() = runBlocking {
        val ownReq = ReportRequest(
            reportCategory = ReportCategory.AFFILIATE,
            reportType = "AFFILIATE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("affiliateId" to "aff-001")
        )

        val ownRes = service.queryReport(affiliatePrincipal, ownReq)
        assertTrue(ownRes is DomainResult.Success)

        val otherReq = ReportRequest(
            reportCategory = ReportCategory.AFFILIATE,
            reportType = "AFFILIATE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("affiliateId" to "aff-999-FOREIGN")
        )

        val otherRes = service.queryReport(affiliatePrincipal, otherReq)
        assertTrue(otherRes is DomainResult.Error)
    }

    @Test
    fun testCrossTenantRequest_strictlyDenied() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.AFFILIATE,
            reportType = "AFFILIATE_SUMMARY",
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
    fun testExportAffiliateSummary_generatesCsv() = runBlocking {
        val queryReq = ReportRequest(
            reportCategory = ReportCategory.AFFILIATE,
            reportType = "AFFILIATE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val exportReq = ExportReportRequest(queryRequest = queryReq, format = ReportExportFormat.CSV)
        val res = service.exportReport(adminPrincipal, exportReq)

        assertTrue(res is DomainResult.Success)
        val doc = (res as DomainResult.Success).data
        assertEquals("AFFILIATE_SUMMARY", doc.reportType)
        assertEquals(ReportExportFormat.CSV, doc.format)
        assertNotNull(doc.contentBase64)
    }
}
