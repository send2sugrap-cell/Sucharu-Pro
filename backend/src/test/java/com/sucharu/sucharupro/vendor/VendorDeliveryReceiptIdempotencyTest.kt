package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDeliveryReceiptDataSource
import com.sucharu.sucharupro.data.repository.VendorDeliveryReceiptRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceipt
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorDeliveryReceiptIdempotencyTest {

    private lateinit var repository: VendorDeliveryReceiptRepositoryImpl

    @Before
    fun setUp() {
        val ds = FakeVendorDeliveryReceiptDataSource()
        repository = VendorDeliveryReceiptRepositoryImpl(ds)
    }

    @Test
    fun testDuplicateReceiptCreationRejected() {
        runBlocking {
            val receipt = VendorDeliveryReceipt(
                deliveryReceiptId = "vdr_dup_01",
                projectId = "PRJ-01",
                tenantId = "TENANT-001",
                receiptNumber = "VDR-2026-DUP",
                purchaseOrderId = "po_001",
                vendorId = "vendor_001",
                receivedBy = "user_001",
                items = listOf(
                    VendorDeliveryReceiptItem(
                        receiptItemId = "vri_dup_01",
                        deliveryReceiptId = "vdr_dup_01",
                        purchaseOrderId = "po_001",
                        purchaseOrderItemId = "poi_01",
                        itemDescription = "Test Item",
                        orderedQuantity = BigDecimal("100.00"),
                        receivedQuantity = BigDecimal("10.00")
                    )
                )
            )

            val first = repository.createReceipt(receipt)
            assertTrue(first is DomainResult.Success)

            // Re-attempt creating same receipt
            val duplicate = repository.createReceipt(receipt)
            assertTrue(duplicate is DomainResult.Error)
            assertTrue((duplicate as DomainResult.Error).message.contains("already exists"))
        }
    }
}
