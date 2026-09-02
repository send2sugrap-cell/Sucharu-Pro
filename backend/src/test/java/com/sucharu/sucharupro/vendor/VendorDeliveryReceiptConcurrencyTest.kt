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

class VendorDeliveryReceiptConcurrencyTest {

    private lateinit var repository: VendorDeliveryReceiptRepositoryImpl

    @Before
    fun setUp() {
        val ds = FakeVendorDeliveryReceiptDataSource()
        repository = VendorDeliveryReceiptRepositoryImpl(ds)
    }

    @Test
    fun testOptimisticConcurrencyConflictOnStaleVersion() {
        runBlocking {
            val receipt = VendorDeliveryReceipt(
                deliveryReceiptId = "vdr_conc_01",
                projectId = "PRJ-01",
                tenantId = "TENANT-001",
                receiptNumber = "VDR-2026-CONC",
                purchaseOrderId = "po_001",
                vendorId = "vendor_001",
                receivedBy = "user_001",
                items = listOf(
                    VendorDeliveryReceiptItem(
                        receiptItemId = "vri_01",
                        deliveryReceiptId = "vdr_conc_01",
                        purchaseOrderId = "po_001",
                        purchaseOrderItemId = "poi_01",
                        itemDescription = "Test Item",
                        orderedQuantity = BigDecimal("100.00"),
                        receivedQuantity = BigDecimal("10.00")
                    )
                )
            )

            val createRes = repository.createReceipt(receipt)
            assertTrue(createRes is DomainResult.Success)
            val savedReceipt = (createRes as DomainResult.Success).data
            assertEquals(1L, savedReceipt.version)

            // First valid update
            val update1 = savedReceipt.copy(remarks = "Updated by Worker 1")
            val update1Res = repository.updateReceipt(update1)
            assertTrue(update1Res is DomainResult.Success)
            assertEquals(2L, (update1Res as DomainResult.Success).data.version)

            // Second concurrent update attempting with stale version 1L
            val update2 = savedReceipt.copy(remarks = "Updated by Worker 2 with stale version")
            val update2Res = repository.updateReceipt(update2)
            assertTrue(update2Res is DomainResult.Error)
            assertTrue((update2Res as DomainResult.Error).message.contains("concurrency conflict"))
        }
    }
}
