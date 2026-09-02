package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorPurchaseOrderDataSource
import com.sucharu.sucharupro.data.repository.VendorPurchaseOrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderItem
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPurchaseOrderRepositoryTest {

    private lateinit var repository: VendorPurchaseOrderRepositoryImpl

    @Before
    fun setUp() {
        val ds = FakeVendorPurchaseOrderDataSource()
        repository = VendorPurchaseOrderRepositoryImpl(ds)
    }

    private fun buildOrder(
        id: String = "vpo_1",
        num: String = "PO-2026-0001",
        vendorId: String = "v_1",
        projectId: String = "proj_1"
    ): VendorPurchaseOrder {
        val item = VendorPurchaseOrderItem(
            itemId = "poi_1",
            purchaseOrderId = id,
            itemDescription = "Gloss Lamination Film",
            quantity = BigDecimal("10.00"),
            unitRate = Money(300.0),
            lineTotal = Money(3000.0)
        )
        return VendorPurchaseOrder(
            purchaseOrderId = id,
            projectId = projectId,
            orderNumber = num,
            vendorId = vendorId,
            requestedBy = "usr_creator",
            subtotal = Money(3000.0),
            totalAmount = Money(3000.0),
            items = listOf(item)
        )
    }

    @Test
    fun `test create and findById`() = runBlocking {
        val order = buildOrder()
        val created = repository.createOrder(order)
        assertTrue(created is DomainResult.Success)

        val fetched = repository.findById("proj_1", "vpo_1")
        assertTrue(fetched is DomainResult.Success)
        assertEquals("PO-2026-0001", (fetched as DomainResult.Success).data.orderNumber)
        assertEquals(1, fetched.data.items.size)
    }

    @Test
    fun `test findByOrderNumber`() = runBlocking {
        val order = buildOrder()
        repository.createOrder(order)

        val fetched = repository.findByOrderNumber("proj_1", "PO-2026-0001")
        assertTrue(fetched is DomainResult.Success)
        assertEquals("vpo_1", (fetched as DomainResult.Success).data.purchaseOrderId)
    }

    @Test
    fun `test unique orderNumber enforcement`() = runBlocking {
        val order1 = buildOrder("vpo_1", "PO-2026-0001")
        val order2 = buildOrder("vpo_2", "PO-2026-0001") // duplicate number

        repository.createOrder(order1)
        val res = repository.createOrder(order2)
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun `test list filtering by vendor and status`() = runBlocking {
        val o1 = buildOrder("vpo_1", "PO-2026-0001", vendorId = "v_1")
        val o2 = buildOrder("vpo_2", "PO-2026-0002", vendorId = "v_2")
        repository.createOrder(o1)
        repository.createOrder(o2)

        val v1List = repository.list("proj_1", vendorId = "v_1")
        assertTrue(v1List is DomainResult.Success)
        assertEquals(1, (v1List as DomainResult.Success).data.size)
        assertEquals("vpo_1", v1List.data[0].purchaseOrderId)
    }

    @Test
    fun `test updateStatus changes status and increments version`() = runBlocking {
        val order = buildOrder()
        repository.createOrder(order)

        val updated = repository.updateStatus("proj_1", "vpo_1", VendorPurchaseOrderStatus.PENDING_APPROVAL, "usr_1")
        assertTrue(updated is DomainResult.Success)
        assertEquals(VendorPurchaseOrderStatus.PENDING_APPROVAL, (updated as DomainResult.Success).data.status)
        assertEquals(2L, updated.data.version)
    }
}
