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

class VendorDeliveryReceiptTenantIsolationTest {

    private lateinit var repository: VendorDeliveryReceiptRepositoryImpl

    @Before
    fun setUp() {
        val ds = FakeVendorDeliveryReceiptDataSource()
        repository = VendorDeliveryReceiptRepositoryImpl(ds)
    }

    @Test
    fun testTenantAlphaCannotAccessTenantBetaReceipt() {
        runBlocking {
            val receiptBeta = VendorDeliveryReceipt(
                deliveryReceiptId = "vdr_beta_01",
                projectId = "TENANT_BETA",
                tenantId = "TENANT_BETA",
                receiptNumber = "VDR-BETA-001",
                purchaseOrderId = "po_beta_001",
                vendorId = "vendor_beta_001",
                receivedBy = "user_beta",
                items = listOf(
                    VendorDeliveryReceiptItem(
                        receiptItemId = "vri_beta_01",
                        deliveryReceiptId = "vdr_beta_01",
                        purchaseOrderId = "po_beta_001",
                        purchaseOrderItemId = "poi_beta_01",
                        itemDescription = "Beta Secret Material",
                        orderedQuantity = BigDecimal("100.00"),
                        receivedQuantity = BigDecimal("50.00")
                    )
                )
            )
            repository.createReceipt(receiptBeta)

            // Tenant Alpha attempts to access Tenant Beta receipt
            val findRes = repository.findById("TENANT_ALPHA", "vdr_beta_01")
            assertTrue(findRes is DomainResult.Error)
            assertTrue((findRes as DomainResult.Error).message.contains("not found in project 'TENANT_ALPHA'"))

            val listAlpha = repository.list("TENANT_ALPHA")
            assertTrue(listAlpha is DomainResult.Success)
            assertTrue((listAlpha as DomainResult.Success).data.isEmpty())
        }
    }
}
