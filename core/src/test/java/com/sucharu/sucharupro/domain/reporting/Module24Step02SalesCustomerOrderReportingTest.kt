package com.sucharu.sucharupro.domain.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Module24Step02SalesCustomerOrderReportingTest {

    private lateinit var orderDs: FakeOrderDataSource
    private lateinit var customerDs: FakeCustomerDataSource
    private lateinit var service: Module24ReportingServiceImpl

    private val tenantId = "TENANT-STEP2-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-001",
        projectId = tenantId,
        username = "admin_user",
        role = UserRole.ADMIN
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr-001",
        projectId = tenantId,
        username = "manager_user",
        role = UserRole.MANAGER
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
        service = Module24ReportingServiceImpl(
            orderDataSource = orderDs,
            customerDataSource = customerDs
        )
    }

    // =========================================================================
    // A. SALES REPORTING TESTS
    // =========================================================================
    @Test
    fun testSalesSummary_calculatesRevenueAndOrderTotals() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.SALES,
            reportType = "SALES_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.SALES, data.meta.reportCategory)
        assertEquals("SALES_SUMMARY", data.meta.reportType)

        val totalSalesMetric = data.summaryMetrics.find { it.metricId == "totalSalesAmount" }
        assertNotNull(totalSalesMetric)
        assertTrue(totalSalesMetric!!.value.startsWith("৳"))

        val totalOrdersMetric = data.summaryMetrics.find { it.metricId == "totalOrdersCount" }
        assertNotNull(totalOrdersMetric)
        assertTrue(totalOrdersMetric!!.numericValue!! > 0)
    }

    @Test
    fun testSalesByCustomer_groupsRevenueByCustomer() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.SALES,
            reportType = "SALES_BY_CUSTOMER",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.rows.isNotEmpty())
        val firstRow = data.rows.first()
        assertNotNull(firstRow.values["customerName"])
        assertNotNull(firstRow.values["totalSales"])
    }

    @Test
    fun testTopCustomersBySales_ranksByNetSpend() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.SALES,
            reportType = "TOP_CUSTOMERS_BY_SALES",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.rows.isNotEmpty())
        assertEquals("#1", data.rows.first().values["rank"])
    }

    // =========================================================================
    // B. CUSTOMER REPORTING TESTS
    // =========================================================================
    @Test
    fun testCustomerSummary_countsActiveAndTotalCustomers() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.CUSTOMER,
            reportType = "CUSTOMER_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.CUSTOMER, data.meta.reportCategory)

        val totalCustMetric = data.summaryMetrics.find { it.metricId == "totalCustomersCount" }
        assertNotNull(totalCustMetric)
        assertTrue(totalCustMetric!!.numericValue!! >= 1.0)
    }

    @Test
    fun testCustomerSales_joinsOrderHistoryPerCustomer() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.CUSTOMER,
            reportType = "CUSTOMER_SALES",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.rows.isNotEmpty())
        assertNotNull(data.rows.first().values["totalSpend"])
    }

    @Test
    fun testCustomerIdentityScope_restrictsCustomerToOwnData() = runBlocking {
        val ownReq = ReportRequest(
            reportCategory = ReportCategory.CUSTOMER,
            reportType = "CUSTOMER_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "cus-001")
        )

        val ownRes = service.queryReport(customerPrincipal, ownReq)
        assertTrue(ownRes is DomainResult.Success)

        val foreignReq = ReportRequest(
            reportCategory = ReportCategory.CUSTOMER,
            reportType = "CUSTOMER_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "cus-999-FOREIGN")
        )

        val foreignRes = service.queryReport(customerPrincipal, foreignReq)
        assertTrue(foreignRes is DomainResult.Error)
        val err = foreignRes as DomainResult.Error
        assertTrue(err.message.contains("Customer accounts can only access"))
    }

    // =========================================================================
    // C. ORDER REPORTING TESTS
    // =========================================================================
    @Test
    fun testOrderSummary_tracksOpenAndCompletedOrders() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.ORDER,
            reportType = "ORDER_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.ORDER, data.meta.reportCategory)

        val openMetric = data.summaryMetrics.find { it.metricId == "openOrdersCount" }
        assertNotNull(openMetric)

        val completedMetric = data.summaryMetrics.find { it.metricId == "completedOrdersCount" }
        assertNotNull(completedMetric)
    }

    @Test
    fun testOrderByStatus_groupsOrdersByCanonicalStatus() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.ORDER,
            reportType = "ORDER_BY_STATUS",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.chartSeries.isNotEmpty())
    }

    // =========================================================================
    // D. TENANT ISOLATION TESTS
    // =========================================================================
    @Test
    fun testCrossTenantQuery_strictlyRejected() = runBlocking {
        val crossTenantReq = ReportRequest(
            reportCategory = ReportCategory.SALES,
            reportType = "SALES_SUMMARY",
            tenantId = "TENANT-MALICIOUS-FOREIGN",
            projectId = "TENANT-MALICIOUS-FOREIGN"
        )

        val res = service.queryReport(managerPrincipal, crossTenantReq)
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("Tenant isolation violation"))
    }

    // =========================================================================
    // E. EXPORT TESTS
    // =========================================================================
    @Test
    fun testExportSalesSummary_generatesCsvDocument() = runBlocking {
        val queryReq = ReportRequest(
            reportCategory = ReportCategory.SALES,
            reportType = "SALES_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val exportReq = ExportReportRequest(
            queryRequest = queryReq,
            format = ReportExportFormat.CSV
        )

        val res = service.exportReport(adminPrincipal, exportReq)
        assertTrue(res is DomainResult.Success)

        val doc = (res as DomainResult.Success).data
        assertEquals("SALES_SUMMARY", doc.reportType)
        assertEquals(ReportExportFormat.CSV, doc.format)
        assertNotNull(doc.contentBase64)
        assertTrue(doc.contentLength > 0L)
    }
}
