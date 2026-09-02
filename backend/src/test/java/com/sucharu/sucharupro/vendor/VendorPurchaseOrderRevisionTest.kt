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

class VendorPurchaseOrderRevisionTest {

    private lateinit var poService: VendorPurchaseOrderServiceImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl

    @Before
    fun setUp() {
        runBlocking {
            val vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            val capRepo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())
            val rateRepo = VendorServiceRateRepositoryImpl(FakeVendorServiceRateDataSource())
            poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())

            val rateService = VendorServiceRateServiceImpl(vendorRepo, capRepo, rateRepo)
            poService = VendorPurchaseOrderServiceImpl(vendorRepo, capRepo, rateService, poRepo)

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "v_1",
                    projectId = "proj_1",
                    vendorCode = "VND-001",
                    vendorName = "Apex Print",
                    vendorType = VendorType.SERVICE_PROVIDER,
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `test revision tracks historical changes and increments revision number`() = runBlocking {
        val item1 = VendorPurchaseOrderItem(
            itemId = "poi_1",
            purchaseOrderId = "",
            itemDescription = "Gloss Lamination Film",
            quantity = BigDecimal("10.00"),
            unitRate = Money(200.0),
            lineTotal = Money(2000.0)
        )

        val createRes = poService.createOrder(
            projectId = "proj_1",
            vendorId = "v_1",
            requestedBy = "usr_alice",
            items = listOf(item1),
            actorId = "usr_alice"
        )
        val poId = (createRes as DomainResult.Success).data.purchaseOrderId

        // Submit & Approve
        poService.submitForApproval("proj_1", poId, "usr_alice")
        poService.approveOrder("proj_1", poId, "usr_bob")

        // Revise order: increase quantity to 20
        val updatedItem = item1.copy(
            quantity = BigDecimal("20.00"),
            lineTotal = Money(4000.0)
        )

        val reviseRes = poService.reviseOrder(
            projectId = "proj_1",
            purchaseOrderId = poId,
            updatedItems = listOf(updatedItem),
            reason = "Client doubled order batch size",
            actorId = "usr_bob"
        )

        assertTrue(reviseRes is DomainResult.Success)
        val revisedOrder = (reviseRes as DomainResult.Success).data
        assertEquals(Money(4000.0), revisedOrder.totalAmount)

        val revisions = poService.listRevisions("proj_1", poId)
        assertTrue(revisions is DomainResult.Success)
        val revList = (revisions as DomainResult.Success).data
        assertEquals(1, revList.size)
        assertEquals(1, revList[0].revisionNumber)
        assertEquals(Money(2000.0), revList[0].previousTotalAmount)
        assertEquals(Money(4000.0), revList[0].newTotalAmount)
        assertEquals("Client doubled order batch size", revList[0].changeSummary)
    }
}
