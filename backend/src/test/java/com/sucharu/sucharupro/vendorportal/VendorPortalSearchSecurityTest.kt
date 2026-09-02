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
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalAnalyticsNotificationSearchServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalSearchSecurityTest {

    private lateinit var searchDataSource: FakeVendorPortalAnalyticsNotificationSearchDataSource
    private lateinit var poDataSource: FakeVendorPurchaseOrderDataSource
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var service: VendorPortalAnalyticsNotificationSearchServiceImpl

    private val testVendor1 = Vendor(
        vendorId = "VND-001",
        projectId = "PRJ-001",
        vendorCode = "VN-001",
        vendorName = "Apex Steel Ltd",
        vendorCategory = VendorCategory.RAW_MATERIALS,
        status = VendorStatus.ACTIVE
    )

    private val testVendor2 = Vendor(
        vendorId = "VND-002",
        projectId = "PRJ-001",
        vendorCode = "VN-002",
        vendorName = "Bashundhara Cement Ltd",
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
            vendorRepo.createVendor(testVendor1)
            vendorRepo.createVendor(testVendor2)
        }

        service = VendorPortalAnalyticsNotificationSearchServiceImpl(
            repository = searchRepo,
            vendorRepository = vendorRepo,
            purchaseOrderRepository = poRepo
        )
    }

    @Test
    fun testServerSideVendorIsolationInSearch() = runBlocking {
        // Seed PO for Vendor 1
        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "PO-101",
                projectId = "PRJ-001",
                vendorId = "VND-001",
                orderNumber = "PO-APEX-001",
                orderDate = 1756291200000L,
                requestedBy = "buyer_01",
                subtotal = Money(BigDecimal("50000.00")),
                totalAmount = Money(BigDecimal("50000.00")),
                status = VendorPurchaseOrderStatus.ISSUED,
                currency = "BDT"
            )
        )

        // Seed PO for Vendor 2 with same keyword
        poRepo.createOrder(
            VendorPurchaseOrder(
                purchaseOrderId = "PO-202",
                projectId = "PRJ-001",
                vendorId = "VND-002",
                orderNumber = "PO-BASH-001",
                orderDate = 1756291200000L,
                requestedBy = "buyer_01",
                subtotal = Money(BigDecimal("90000.00")),
                totalAmount = Money(BigDecimal("90000.00")),
                status = VendorPurchaseOrderStatus.ISSUED,
                currency = "BDT"
            )
        )

        // Vendor 1 searches for "PO"
        val search1Res = service.search("TENANT-001", "PRJ-001", "VND-001", "PO")
        assertTrue(search1Res is DomainResult.Success)
        val items1 = (search1Res as DomainResult.Success).data.items
        assertEquals(1, items1.size)
        assertEquals("PO-101", items1[0].entityId)

        // Vendor 2 searches for "PO"
        val search2Res = service.search("TENANT-001", "PRJ-001", "VND-002", "PO")
        assertTrue(search2Res is DomainResult.Success)
        val items2 = (search2Res as DomainResult.Success).data.items
        assertEquals(1, items2.size)
        assertEquals("PO-202", items2[0].entityId)
    }
}
