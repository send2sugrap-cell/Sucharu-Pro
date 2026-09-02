package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorCapabilityDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPurchaseOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorServiceRateDataSource
import com.sucharu.sucharupro.data.repository.VendorCapabilityRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPurchaseOrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorServiceRateRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderServiceImpl
import com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPurchaseOrderServiceTest {

    private lateinit var poService: VendorPurchaseOrderServiceImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var capRepo: VendorCapabilityRepositoryImpl
    private lateinit var rateRepo: VendorServiceRateRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl

    @Before
    fun setUp() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            capRepo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())
            rateRepo = VendorServiceRateRepositoryImpl(FakeVendorServiceRateDataSource())
            poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())

            val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
            poService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)

            // Seed Active Vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "v_1",
                    projectId = "proj_1",
                    vendorCode = "VND-001",
                    vendorName = "Apex Print Solutions",
                    vendorType = VendorType.SERVICE_PROVIDER,
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            // Seed Active Capability
            capRepo.createCapability(
                VendorCapability(
                    capabilityId = "cap_1",
                    projectId = "proj_1",
                    vendorId = "v_1",
                    capabilityType = CapabilityType.PRINTING,
                    status = CapabilityStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `test create order successfully calculates totals and creates audit`() = runBlocking {
        val item = VendorPurchaseOrderItem(
            itemId = "poi_1",
            purchaseOrderId = "",
            capabilityType = CapabilityType.PRINTING,
            itemDescription = "Offset Plate Making",
            quantity = BigDecimal("10.00"),
            unitRate = Money(250.0),
            lineTotal = Money(2500.0)
        )

        val res = poService.createOrder(
            projectId = "proj_1",
            vendorId = "v_1",
            requestedBy = "usr_requester",
            items = listOf(item),
            taxAmount = Money(150.0),
            discountAmount = Money(50.0),
            actorId = "usr_requester"
        )

        assertTrue(res is DomainResult.Success)
        val order = (res as DomainResult.Success).data
        assertEquals(Money(2500.0), order.subtotal)
        assertEquals(Money(2600.0), order.totalAmount) // 2500 + 150 - 50 = 2600
        assertEquals(VendorPurchaseOrderStatus.DRAFT, order.status)

        val audits = poService.listAudits("proj_1", order.purchaseOrderId)
        assertTrue(audits is DomainResult.Success)
        assertEquals(1, (audits as DomainResult.Success).data.size)
        assertEquals("CREATED", audits.data[0].eventType)
    }

    @Test
    fun `test creating order for suspended vendor is rejected`() = runBlocking {
        vendorRepo.createVendor(
            Vendor(
                vendorId = "v_suspended",
                projectId = "proj_1",
                vendorCode = "VND-002",
                vendorName = "Suspended Vendor",
                vendorType = VendorType.SERVICE_PROVIDER,
                vendorCategory = VendorCategory.PRINTING,
                status = VendorStatus.SUSPENDED
            )
        )

        val item = VendorPurchaseOrderItem(
            itemId = "poi_1",
            purchaseOrderId = "",
            itemDescription = "Service",
            quantity = BigDecimal("5.00"),
            unitRate = Money(100.0),
            lineTotal = Money(500.0)
        )

        val res = poService.createOrder(
            projectId = "proj_1",
            vendorId = "v_suspended",
            requestedBy = "usr_requester",
            items = listOf(item)
        )

        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("vendor status is 'SUSPENDED'"))
    }
}
