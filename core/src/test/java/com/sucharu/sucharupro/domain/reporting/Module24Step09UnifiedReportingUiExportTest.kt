package com.sucharu.sucharupro.domain.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.datasource.affiliate.*
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.*
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.machine.*
import com.sucharu.sucharupro.data.datasource.machine.alerts.FakeMachineAlertDataSource
import com.sucharu.sucharupro.data.datasource.machine.events.FakeMachineEventDataSource
import com.sucharu.sucharupro.data.datasource.machine.maintenance.FakeMachineMaintenanceDataSource
import com.sucharu.sucharupro.data.datasource.machine.oee.FakeMachineOeeDataSource
import com.sucharu.sucharupro.data.datasource.machine.telemetry.FakeMachineTelemetryDataSource
import com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.repository.report.Module24ReportRepository
import com.sucharu.sucharupro.data.repository.report.Module24ReportRepositoryImpl
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Module24Step09UnifiedReportingUiExportTest {

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
    private lateinit var machineRegistryDs: FakeMachineRegistryDataSource
    private lateinit var machineTelemetryDs: FakeMachineTelemetryDataSource
    private lateinit var machineEventDs: FakeMachineEventDataSource
    private lateinit var machineMaintenanceDs: FakeMachineMaintenanceDataSource
    private lateinit var machineAlertDs: FakeMachineAlertDataSource
    private lateinit var machineOeeDs: FakeMachineOeeDataSource
    private lateinit var preflightDs: FakePreflightDataSource
    private lateinit var repository: Module24ReportRepository

    private val tenantId = "TENANT-STEP9-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-001",
        projectId = tenantId,
        username = "admin_user",
        role = UserRole.ADMIN
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cus-001",
        projectId = tenantId,
        username = "customer_user",
        role = UserRole.CUSTOMER,
        customerId = "cus-001"
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
        machineRegistryDs = FakeMachineRegistryDataSource()
        machineTelemetryDs = FakeMachineTelemetryDataSource()
        machineEventDs = FakeMachineEventDataSource()
        machineMaintenanceDs = FakeMachineMaintenanceDataSource()
        machineAlertDs = FakeMachineAlertDataSource()
        machineOeeDs = FakeMachineOeeDataSource()
        preflightDs = FakePreflightDataSource()

        val service = Module24ReportingServiceImpl(
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
            affiliatePayoutRequestDataSource = payoutReqDs,
            machineRegistryDataSource = machineRegistryDs,
            machineTelemetryDataSource = machineTelemetryDs,
            machineEventDataSource = machineEventDs,
            machineMaintenanceDataSource = machineMaintenanceDs,
            machineAlertDataSource = machineAlertDs,
            machineOeeDataSource = machineOeeDs,
            preflightDataSource = preflightDs
        )

        repository = Module24ReportRepositoryImpl(service)
    }

    // =========================================================================
    // A. UNIFIED CATALOGUE RESOLUTION TESTS (ALL 15 CATEGORIES)
    // =========================================================================
    @Test
    fun testReportCatalogue_returnsAll15CategoriesForAdmin() = runBlocking {
        val catalogueRes = repository.getReportCatalogue(adminPrincipal)
        assertTrue(catalogueRes is DomainResult.Success)

        val catalogue = (catalogueRes as DomainResult.Success).data
        assertEquals(15, catalogue.totalCategories)
        assertTrue(catalogue.totalReports >= 100)

        val categoriesInResponse = catalogue.categories.map { it.category }
        ReportCategory.entries.forEach { cat ->
            assertTrue("Category '${cat.displayName}' must be present in catalogue", categoriesInResponse.contains(cat))
        }
    }

    // =========================================================================
    // B. UNIFIED QUERY EXECUTION ACROSS ALL 15 CATEGORIES
    // =========================================================================
    @Test
    fun testUnifiedQuery_executesSuccessfullyAcrossAll15Categories() = runBlocking {
        val testQueries = listOf(
            ReportCategory.SALES to "SALES_SUMMARY",
            ReportCategory.CUSTOMER to "CUSTOMER_SUMMARY",
            ReportCategory.ORDER to "ORDER_SUMMARY",
            ReportCategory.PRODUCTION to "PRODUCTION_SUMMARY",
            ReportCategory.QUALITY to "QC_SUMMARY",
            ReportCategory.INVENTORY to "INVENTORY_SUMMARY",
            ReportCategory.DELIVERY to "DELIVERY_SUMMARY",
            ReportCategory.FINANCE to "FINANCE_SUMMARY",
            ReportCategory.PROFITABILITY to "PROFITABILITY_SUMMARY",
            ReportCategory.AFFILIATE to "AFFILIATE_SUMMARY",
            ReportCategory.WALLET_PAYOUT to "WALLET_SUMMARY",
            ReportCategory.MACHINE_OPERATIONS to "MACHINE_OEE_SUMMARY",
            ReportCategory.PREFLIGHT to "PREFLIGHT_DIAGNOSTICS",
            ReportCategory.AUDIT to "SYSTEM_AUDIT_LOGS",
            ReportCategory.EXECUTIVE_ANALYTICS to "EXECUTIVE_ANALYTICS_DASHBOARD"
        )

        testQueries.forEach { (cat, type) ->
            val req = ReportRequest(
                reportCategory = cat,
                reportType = type,
                tenantId = tenantId,
                projectId = tenantId
            )
            val res = repository.queryReport(adminPrincipal, req)
            assertTrue("Query for category '${cat.displayName}' ($type) must succeed", res is DomainResult.Success)

            val data = (res as DomainResult.Success).data
            assertEquals(cat, data.meta.reportCategory)
            assertEquals(type, data.meta.reportType)
            assertNotNull(data.summaryMetrics)
            assertNotNull(data.columns)
            assertNotNull(data.rows)
        }
    }

    // =========================================================================
    // C. UNIFIED EXPORT ORCHESTRATION TESTS (CSV, JSON, PDF, EXCEL)
    // =========================================================================
    @Test
    fun testUnifiedExport_supportsCsvJsonPdfExcelFormats() = runBlocking {
        val queryReq = ReportRequest(
            reportCategory = ReportCategory.EXECUTIVE_ANALYTICS,
            reportType = "EXECUTIVE_ANALYTICS_DASHBOARD",
            tenantId = tenantId,
            projectId = tenantId
        )

        val formatsToTest = listOf(
            ReportExportFormat.CSV,
            ReportExportFormat.JSON,
            ReportExportFormat.PDF,
            ReportExportFormat.EXCEL
        )

        formatsToTest.forEach { format ->
            val exportReq = ExportReportRequest(queryRequest = queryReq, format = format)
            val res = repository.exportReport(adminPrincipal, exportReq)

            assertTrue("Export for format '${format.name}' must succeed", res is DomainResult.Success)
            val doc = (res as DomainResult.Success).data
            assertEquals("EXECUTIVE_ANALYTICS_DASHBOARD", doc.reportType)
            assertEquals(format, doc.format)
            assertEquals(format.mimeType, doc.mimeType)
            assertNotNull(doc.contentBase64)
            assertTrue(doc.contentLength > 0L)
        }
    }

    // =========================================================================
    // D. ROLE-AWARE CAPABILITY & TENANT ISOLATION TESTS
    // =========================================================================
    @Test
    fun testCustomer_identityScope_restrictsToOwnContext() = runBlocking {
        val ownSalesReq = ReportRequest(
            reportCategory = ReportCategory.SALES,
            reportType = "SALES_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "cus-001")
        )
        val ownRes = repository.queryReport(customerPrincipal, ownSalesReq)
        assertTrue(ownRes is DomainResult.Success)

        val otherSalesReq = ReportRequest(
            reportCategory = ReportCategory.SALES,
            reportType = "SALES_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "cus-999-FOREIGN")
        )
        val otherRes = repository.queryReport(customerPrincipal, otherSalesReq)
        assertTrue("Customer querying another customer must be rejected", otherRes is DomainResult.Error)
    }

    @Test
    fun testAffiliate_selfScope_restrictsToOwnRecord() = runBlocking {
        val ownAffReq = ReportRequest(
            reportCategory = ReportCategory.AFFILIATE,
            reportType = "AFFILIATE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("affiliateId" to "aff-001")
        )
        val ownRes = repository.queryReport(affiliatePrincipal, ownAffReq)
        assertTrue(ownRes is DomainResult.Success)

        val otherAffReq = ReportRequest(
            reportCategory = ReportCategory.AFFILIATE,
            reportType = "AFFILIATE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("affiliateId" to "aff-999-FOREIGN")
        )
        val otherRes = repository.queryReport(affiliatePrincipal, otherAffReq)
        assertTrue("Affiliate querying another affiliate must be rejected", otherRes is DomainResult.Error)
    }

    @Test
    fun testCrossTenantRequest_strictlyDeniedAcrossCategories() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.EXECUTIVE_ANALYTICS,
            reportType = "EXECUTIVE_ANALYTICS_DASHBOARD",
            tenantId = "TENANT-FOREIGN-XYZ",
            projectId = "TENANT-FOREIGN-XYZ"
        )

        val res = repository.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("Tenant isolation violation"))
    }
}
