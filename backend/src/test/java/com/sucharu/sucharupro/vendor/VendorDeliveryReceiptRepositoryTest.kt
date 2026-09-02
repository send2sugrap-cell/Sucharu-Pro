package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDeliveryReceiptDataSource
import com.sucharu.sucharupro.data.repository.VendorDeliveryReceiptRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceipt
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptItem
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorDeliveryReceiptRepositoryTest {

    private lateinit var repository: VendorDeliveryReceiptRepositoryImpl

    @Before
    fun setUp() {
        val ds = FakeVendorDeliveryReceiptDataSource()
        repository = VendorDeliveryReceiptRepositoryImpl(ds)
    }

    private fun sampleReceipt(
        receiptId: String = "vdr_001",
        projectId: String = "PRJ-01",
        receiptNumber: String = "VDR-2026-0001",
        purchaseOrderId: String = "po_001",
        vendorId: String = "vendor_001"
    ) = VendorDeliveryReceipt(
        deliveryReceiptId = receiptId,
        projectId = projectId,
        tenantId = "TENANT-001",
        receiptNumber = receiptNumber,
        purchaseOrderId = purchaseOrderId,
        vendorId = vendorId,
        receivedBy = "user_001",
        items = listOf(
            VendorDeliveryReceiptItem(
                receiptItemId = "vri_001",
                deliveryReceiptId = receiptId,
                purchaseOrderId = purchaseOrderId,
                purchaseOrderItemId = "poi_001",
                itemDescription = "Lamination Gloss",
                orderedQuantity = BigDecimal("100.00"),
                receivedQuantity = BigDecimal("50.00")
            )
        )
    )

    @Test
    fun testCreateAndFindById() {
        runBlocking {
            val receipt = sampleReceipt()
            val createRes = repository.createReceipt(receipt)
            assertTrue(createRes is DomainResult.Success)

            val findRes = repository.findById("PRJ-01", "vdr_001")
            assertTrue(findRes is DomainResult.Success)
            assertEquals("VDR-2026-0001", (findRes as DomainResult.Success).data.receiptNumber)
            assertEquals(1, findRes.data.items.size)
        }
    }

    @Test
    fun testFindByReceiptNumber() {
        runBlocking {
            repository.createReceipt(sampleReceipt())
            val findRes = repository.findByReceiptNumber("PRJ-01", "VDR-2026-0001")
            assertTrue(findRes is DomainResult.Success)
            assertEquals("vdr_001", (findRes as DomainResult.Success).data.deliveryReceiptId)
        }
    }

    @Test
    fun testListFiltering() {
        runBlocking {
            repository.createReceipt(sampleReceipt("vdr_001", "PRJ-01", "VDR-01", "po_001", "vendor_01"))
            repository.createReceipt(sampleReceipt("vdr_002", "PRJ-01", "VDR-02", "po_002", "vendor_02"))

            val listAll = repository.list("PRJ-01")
            assertTrue(listAll is DomainResult.Success)
            assertEquals(2, (listAll as DomainResult.Success).data.size)

            val listByVendor = repository.list("PRJ-01", vendorId = "vendor_01")
            assertEquals(1, (listByVendor as DomainResult.Success).data.size)

            val listByPo = repository.list("PRJ-01", purchaseOrderId = "po_002")
            assertEquals(1, (listByPo as DomainResult.Success).data.size)
        }
    }

    @Test
    fun testUpdateStatus() {
        runBlocking {
            repository.createReceipt(sampleReceipt())
            val updateRes = repository.updateStatus("PRJ-01", "vdr_001", VendorDeliveryReceiptStatus.RECEIVING, "manager_01")
            assertTrue(updateRes is DomainResult.Success)
            assertEquals(VendorDeliveryReceiptStatus.RECEIVING, (updateRes as DomainResult.Success).data.status)
        }
    }
}
