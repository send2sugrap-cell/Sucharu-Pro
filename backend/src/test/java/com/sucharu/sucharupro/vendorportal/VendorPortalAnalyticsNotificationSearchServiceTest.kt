package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalAnalyticsNotificationSearchDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPurchaseOrderDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalAnalyticsNotificationSearchRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPurchaseOrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalAnalyticsNotificationSearchServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalAnalyticsNotificationSearchServiceTest {

    private lateinit var searchDataSource: FakeVendorPortalAnalyticsNotificationSearchDataSource
    private lateinit var poDataSource: FakeVendorPurchaseOrderDataSource
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var service: VendorPortalAnalyticsNotificationSearchServiceImpl

    private val testVendor = Vendor(
        vendorId = "VND-001",
        projectId = "PRJ-001",
        vendorCode = "VN-001",
        vendorName = "Apex Steel Ltd",
        vendorCategory = VendorCategory.RAW_MATERIALS,
        status = VendorStatus.ACTIVE
    )

    @Before
    fun setup() {
        searchDataSource = FakeVendorPortalAnalyticsNotificationSearchDataSource()
        poDataSource = FakeVendorPurchaseOrderDataSource()
        val vendorDs = FakeVendorDataSource()

        val searchRepo = VendorPortalAnalyticsNotificationSearchRepositoryImpl(searchDataSource)
        poRepo = VendorPurchaseOrderRepositoryImpl(poDataSource)
        vendorRepo = VendorRepositoryImpl(vendorDs)

        runBlocking {
            vendorRepo.createVendor(testVendor)
        }

        service = VendorPortalAnalyticsNotificationSearchServiceImpl(
            repository = searchRepo,
            vendorRepository = vendorRepo,
            purchaseOrderRepository = poRepo
        )
    }

    @Test
    fun testUnifiedAnalyticsHubAggregation() = runBlocking {
        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "PO-001",
                projectId = "PRJ-001",
                vendorId = "VND-001",
                orderNumber = "PO-2026-001",
                orderDate = 1756291200000L,
                requestedBy = "buyer_01",
                subtotal = Money(BigDecimal("100000.00")),
                totalAmount = Money(BigDecimal("100000.00")),
                status = VendorPurchaseOrderStatus.ISSUED,
                currency = "BDT"
            )
        )

        val hubRes = service.getUnifiedAnalyticsHub("TENANT-001", "PRJ-001", "VND-001", VendorPortalPeriod.LAST_30_DAYS)
        assertTrue(hubRes is DomainResult.Success)
        val hub = (hubRes as DomainResult.Success).data
        assertEquals("VND-001", hub.vendorId)
        assertEquals(1, hub.operational.activePurchaseOrders)
        assertTrue(hub.trends.isNotEmpty())
    }

    @Test
    fun testEmitNotificationWithIdempotency() = runBlocking {
        val emit1 = service.emitNotification(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            category = VendorPortalNotificationCategory.PURCHASE_ORDER,
            title = "PO Ready",
            message = "Your PO is ready for review",
            idempotencyKey = "PO-READY-001"
        )
        assertTrue(emit1 is DomainResult.Success)
        val notif1 = (emit1 as DomainResult.Success).data

        // Re-emit with same idempotency key
        val emit2 = service.emitNotification(
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            category = VendorPortalNotificationCategory.PURCHASE_ORDER,
            title = "PO Ready",
            message = "Your PO is ready for review",
            idempotencyKey = "PO-READY-001"
        )
        assertTrue(emit2 is DomainResult.Success)
        val notif2 = (emit2 as DomainResult.Success).data

        assertEquals(notif1.notificationId, notif2.notificationId)
    }

    @Test
    fun testUnifiedWorkspaceSummary() = runBlocking {
        val summaryRes = service.getUnifiedWorkspaceSummary("TENANT-001", "PRJ-001", "VND-001")
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals("Apex Steel Ltd", summary.vendorName)
        assertTrue(summary.navigationSections.isNotEmpty())
    }
}
