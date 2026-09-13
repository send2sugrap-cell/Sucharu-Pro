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
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Module24Step08PreflightProofingReadinessReportingTest {

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
    private lateinit var service: Module24ReportingServiceImpl

    private val tenantId = "TENANT-STEP8-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-001",
        projectId = tenantId,
        username = "admin_user",
        role = UserRole.ADMIN
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "stf-001",
        projectId = tenantId,
        username = "prepress_operator",
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
            affiliatePayoutRequestDataSource = payoutReqDs,
            machineRegistryDataSource = machineRegistryDs,
            machineTelemetryDataSource = machineTelemetryDs,
            machineEventDataSource = machineEventDs,
            machineMaintenanceDataSource = machineMaintenanceDs,
            machineAlertDataSource = machineAlertDs,
            machineOeeDataSource = machineOeeDs,
            preflightDataSource = preflightDs
        )
    }

    // =========================================================================
    // A. PREFLIGHT DIAGNOSTICS & READINESS REPORTING TESTS
    // =========================================================================
    @Test
    fun testPreflightDiagnostics_calculatesPassRateAndFindingCounts() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.PREFLIGHT,
            reportType = "PREFLIGHT_DIAGNOSTICS",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.PREFLIGHT, data.meta.reportCategory)
        assertEquals("PREFLIGHT_DIAGNOSTICS", data.meta.reportType)

        val passRateMetric = data.summaryMetrics.find { it.metricId == "passRatePercentage" }
        assertNotNull(passRateMetric)
        assertTrue(passRateMetric!!.value.endsWith("%"))
    }

    @Test
    fun testPreflightSummary_reportsRunExecutionGrid() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.PREFLIGHT,
            reportType = "PREFLIGHT_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(staffPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.rows.isNotEmpty())
        assertNotNull(data.rows.first().values["preflightRunId"])
    }

    // =========================================================================
    // B. SECURITY, TENANT & IDENTITY ISOLATION TESTS
    // =========================================================================
    @Test
    fun testExternalCustomer_deniedPreflightReportAccess() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.PREFLIGHT,
            reportType = "PREFLIGHT_DIAGNOSTICS",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(customerPrincipal, req)
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testCrossTenantRequest_strictlyDenied() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.PREFLIGHT,
            reportType = "PREFLIGHT_DIAGNOSTICS",
            tenantId = "TENANT-FOREIGN-XYZ",
            projectId = "TENANT-FOREIGN-XYZ"
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("Tenant isolation violation"))
    }

    // =========================================================================
    // C. EXPORT TESTS
    // =========================================================================
    @Test
    fun testExportPreflightDiagnostics_generatesCsv() = runBlocking {
        val queryReq = ReportRequest(
            reportCategory = ReportCategory.PREFLIGHT,
            reportType = "PREFLIGHT_DIAGNOSTICS",
            tenantId = tenantId,
            projectId = tenantId
        )

        val exportReq = ExportReportRequest(queryRequest = queryReq, format = ReportExportFormat.CSV)
        val res = service.exportReport(adminPrincipal, exportReq)

        assertTrue(res is DomainResult.Success)
        val doc = (res as DomainResult.Success).data
        assertEquals("PREFLIGHT_DIAGNOSTICS", doc.reportType)
        assertEquals(ReportExportFormat.CSV, doc.format)
        assertNotNull(doc.contentBase64)
    }
}
