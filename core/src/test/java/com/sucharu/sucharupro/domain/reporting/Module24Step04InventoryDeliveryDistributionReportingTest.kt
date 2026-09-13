package com.sucharu.sucharupro.domain.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Module24Step04InventoryDeliveryDistributionReportingTest {

    private lateinit var orderDs: FakeOrderDataSource
    private lateinit var customerDs: FakeCustomerDataSource
    private lateinit var execDs: FakeProductionExecutionDataSource
    private lateinit var qcDs: FakeProductionQcDataSource
    private lateinit var rwkDs: FakeProductionReworkDataSource
    private lateinit var challanDs: FakeDeliveryChallanDataSource
    private lateinit var shipmentDs: FakeDeliveryShipmentDataSource
    private lateinit var returnDs: FakeDeliveryReturnDataSource
    private lateinit var service: Module24ReportingServiceImpl

    private val tenantId = "TENANT-STEP4-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-001",
        projectId = tenantId,
        username = "admin_user",
        role = UserRole.ADMIN
    )

    private val warehousePrincipal = AuthenticatedPrincipal(
        userId = "wh-001",
        projectId = tenantId,
        username = "warehouse_user",
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

        service = Module24ReportingServiceImpl(
            orderDataSource = orderDs,
            customerDataSource = customerDs,
            productionExecutionDataSource = execDs,
            productionQcDataSource = qcDs,
            productionReworkDataSource = rwkDs,
            deliveryChallanDataSource = challanDs,
            deliveryShipmentDataSource = shipmentDs,
            deliveryReturnDataSource = returnDs
        )
    }

    // =========================================================================
    // A. FINISHED GOODS INVENTORY REPORTING TESTS
    // =========================================================================
    @Test
    fun testInventorySummary_reportsFinishedGoodsValuationAndSKUs() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.INVENTORY,
            reportType = "INVENTORY_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.INVENTORY, data.meta.reportCategory)
        assertEquals("INVENTORY_SUMMARY", data.meta.reportType)

        val totalValuationMetric = data.summaryMetrics.find { it.metricId == "totalInventoryValue" }
        assertNotNull(totalValuationMetric)
        assertTrue(totalValuationMetric!!.value.startsWith("BDT"))
    }

    @Test
    fun testStockBalance_reportsFinishedGoodsSKUGrid() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.INVENTORY,
            reportType = "STOCK_BALANCE",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(warehousePrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.rows.isNotEmpty())
        assertNotNull(data.rows.first().values["sku"])
    }

    // =========================================================================
    // B. DELIVERY & CHALLAN REPORTING TESTS
    // =========================================================================
    @Test
    fun testDeliverySummary_reportsChallansAndInTransitShipments() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.DELIVERY,
            reportType = "DELIVERY_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.DELIVERY, data.meta.reportCategory)

        val totalChallansMetric = data.summaryMetrics.find { it.metricId == "totalChallansCount" }
        assertNotNull(totalChallansMetric)
        assertTrue(totalChallansMetric!!.numericValue!! >= 1.0)
    }

    @Test
    fun testReturnSummary_reportsCustomerReturns() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.DELIVERY,
            reportType = "RETURN_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.rows.isNotEmpty())
        assertNotNull(data.rows.first().values["returnNo"])
    }

    // =========================================================================
    // C. SECURITY, TENANT & IDENTITY SCOPE TESTS
    // =========================================================================
    @Test
    fun testWarehouseRole_hasAccessToInventoryAndDelivery() = runBlocking {
        val invReq = ReportRequest(ReportCategory.INVENTORY, "INVENTORY_SUMMARY", tenantId, tenantId)
        val invRes = service.queryReport(warehousePrincipal, invReq)
        assertTrue("Warehouse user should access inventory summary", invRes is DomainResult.Success)

        val delReq = ReportRequest(ReportCategory.DELIVERY, "DELIVERY_SUMMARY", tenantId, tenantId)
        val delRes = service.queryReport(warehousePrincipal, delReq)
        assertTrue("Warehouse user should access delivery summary", delRes is DomainResult.Success)
    }

    @Test
    fun testCustomer_identityScope_restrictsToOwnDeliveries() = runBlocking {
        val ownReq = ReportRequest(
            reportCategory = ReportCategory.DELIVERY,
            reportType = "DELIVERY_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "cus-001")
        )

        val ownRes = service.queryReport(customerPrincipal, ownReq)
        assertTrue(ownRes is DomainResult.Success)

        val otherReq = ReportRequest(
            reportCategory = ReportCategory.DELIVERY,
            reportType = "DELIVERY_SUMMARY",
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
            reportCategory = ReportCategory.INVENTORY,
            reportType = "INVENTORY_SUMMARY",
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
    fun testExportInventorySummary_generatesCsv() = runBlocking {
        val queryReq = ReportRequest(
            reportCategory = ReportCategory.INVENTORY,
            reportType = "INVENTORY_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val exportReq = ExportReportRequest(queryRequest = queryReq, format = ReportExportFormat.CSV)
        val res = service.exportReport(adminPrincipal, exportReq)

        assertTrue(res is DomainResult.Success)
        val doc = (res as DomainResult.Success).data
        assertEquals("INVENTORY_SUMMARY", doc.reportType)
        assertEquals(ReportExportFormat.CSV, doc.format)
        assertNotNull(doc.contentBase64)
    }
}
