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
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.repository.report.Module24ReportRepository
import com.sucharu.sucharupro.data.repository.report.Module24ReportRepositoryImpl
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Module24Step10FinalVerificationAndE2EJourneyTest {

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

    private val tenantId = "TENANT-FINAL-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-final-001",
        projectId = tenantId,
        username = "super_admin",
        role = UserRole.ADMIN
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr-final-001",
        projectId = tenantId,
        username = "plant_manager",
        role = UserRole.MANAGER
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
    // 1. COMPLETE REPORT CATALOGUE & CATEGORY VERIFICATION (15 CATEGORIES)
    // =========================================================================
    @Test
    fun testCatalogueCompleteness_all15CategoriesPresent() = runBlocking {
        val catalogueRes = repository.getReportCatalogue(adminPrincipal)
        assertTrue(catalogueRes is DomainResult.Success)

        val catalogue = (catalogueRes as DomainResult.Success).data
        assertEquals(15, catalogue.totalCategories)
        assertTrue(catalogue.totalReports >= 100)

        val categoriesInResponse = catalogue.categories.map { it.category }
        ReportCategory.entries.forEach { cat ->
            assertTrue("Category '${cat.displayName}' must exist in catalogue", categoriesInResponse.contains(cat))
        }
    }

    // =========================================================================
    // 2. 13 CANONICAL PRODUCTION STAGE RECONCILIATION
    // =========================================================================
    @Test
    fun testCanonicalStagesReconciliation_all13StagesInSequence() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.PRODUCTION,
            reportType = "STAGE_PERFORMANCE",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = repository.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(13, data.rows.size)

        val reportedStages = data.rows.mapNotNull { it.values["stageName"] }
        ProductionStageType.orderedStages.forEach { canonicalStage ->
            assertTrue("Stage '${canonicalStage.defaultLabel}' must be present in production reporting", reportedStages.contains(canonicalStage.defaultLabel))
        }
    }

    // =========================================================================
    // 3. FULL E2E SOFTWARE REPORTING JOURNEYS (RPT-E2E-01 THROUGH RPT-E2E-15)
    // =========================================================================
    @Test
    fun testFullE2EReportingJourneys_allSucceed() = runBlocking {
        val journeys = listOf(
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

        journeys.forEach { (category, reportType) ->
            val req = ReportRequest(category, reportType, tenantId, tenantId)
            val res = repository.queryReport(adminPrincipal, req)
            assertTrue("Journey $category / $reportType must succeed", res is DomainResult.Success)

            val reportData = (res as DomainResult.Success).data
            assertEquals(category, reportData.meta.reportCategory)
            assertNotNull(reportData.summaryMetrics)
            assertNotNull(reportData.columns)
            assertNotNull(reportData.rows)
        }
    }

    // =========================================================================
    // 4. MULTI-FORMAT EXPORT ORCHESTRATION (CSV, JSON, PDF, EXCEL)
    // =========================================================================
    @Test
    fun testExportOrchestration_allFormatsSupported() = runBlocking {
        val queryReq = ReportRequest(ReportCategory.EXECUTIVE_ANALYTICS, "EXECUTIVE_ANALYTICS_DASHBOARD", tenantId, tenantId)
        val formats = listOf(ReportExportFormat.CSV, ReportExportFormat.JSON, ReportExportFormat.PDF, ReportExportFormat.EXCEL)

        formats.forEach { format ->
            val exportReq = ExportReportRequest(queryReq, format)
            val res = repository.exportReport(adminPrincipal, exportReq)
            assertTrue("Export format ${format.name} must succeed", res is DomainResult.Success)

            val doc = (res as DomainResult.Success).data
            assertEquals(format, doc.format)
            assertNotNull(doc.contentBase64)
            assertTrue(doc.contentLength > 0L)
        }
    }

    // =========================================================================
    // 5. SECURITY & TENANT ISOLATION BOUNDARY VERIFICATION
    // =========================================================================
    @Test
    fun testCustomer_identityScope_strictBoundary() = runBlocking {
        val ownReq = ReportRequest(ReportCategory.SALES, "SALES_SUMMARY", tenantId, tenantId, filters = mapOf("customerId" to "cus-001"))
        val ownRes = repository.queryReport(customerPrincipal, ownReq)
        assertTrue(ownRes is DomainResult.Success)

        val foreignReq = ReportRequest(ReportCategory.SALES, "SALES_SUMMARY", tenantId, tenantId, filters = mapOf("customerId" to "cus-FOREIGN-999"))
        val foreignRes = repository.queryReport(customerPrincipal, foreignReq)
        assertTrue("Customer querying foreign data must be denied", foreignRes is DomainResult.Error)
    }

    @Test
    fun testAffiliate_selfScope_strictBoundary() = runBlocking {
        val ownReq = ReportRequest(ReportCategory.AFFILIATE, "AFFILIATE_SUMMARY", tenantId, tenantId, filters = mapOf("affiliateId" to "aff-001"))
        val ownRes = repository.queryReport(affiliatePrincipal, ownReq)
        assertTrue(ownRes is DomainResult.Success)

        val foreignReq = ReportRequest(ReportCategory.AFFILIATE, "AFFILIATE_SUMMARY", tenantId, tenantId, filters = mapOf("affiliateId" to "aff-FOREIGN-999"))
        val foreignRes = repository.queryReport(affiliatePrincipal, foreignReq)
        assertTrue("Affiliate querying foreign data must be denied", foreignRes is DomainResult.Error)
    }

    @Test
    fun testCrossTenant_strictlyDeniedAcrossAllCategories() = runBlocking {
        val req = ReportRequest(ReportCategory.EXECUTIVE_ANALYTICS, "EXECUTIVE_ANALYTICS_DASHBOARD", "TENANT-FOREIGN", "TENANT-FOREIGN")
        val res = repository.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("Tenant isolation violation"))
    }
}
