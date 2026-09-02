package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorInvoiceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorInvoiceServiceTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var receiptRepo: VendorDeliveryReceiptRepositoryImpl
    private lateinit var invoiceRepo: VendorInvoiceRepositoryImpl
    private lateinit var invoiceService: VendorInvoiceServiceImpl

    @Before
    fun setUp() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())
            receiptRepo = VendorDeliveryReceiptRepositoryImpl(FakeVendorDeliveryReceiptDataSource())
            invoiceRepo = VendorInvoiceRepositoryImpl(FakeVendorInvoiceDataSource())

            invoiceService = VendorInvoiceServiceImpl(
                vendorRepository = vendorRepo,
                purchaseOrderRepository = poRepo,
                receiptRepository = receiptRepo,
                invoiceRepository = invoiceRepo
            )

            // Seed active vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-01",
                    projectId = "PRJ-01",
                    vendorCode = "V001",
                    vendorName = "Apex Paper Mills",
                    status = VendorStatus.ACTIVE
                )
            )

            // Seed PO
            val poItem = VendorPurchaseOrderItem(
                itemId = "poi_01",
                purchaseOrderId = "PO-01",
                itemDescription = "Offset Paper 100gsm",
                quantity = BigDecimal("100"),
                unitRate = Money(50.0),
                lineTotal = Money(5000.0)
            )
            poRepo.createOrder(
                VendorPurchaseOrder(
                    purchaseOrderId = "PO-01",
                    projectId = "PRJ-01",
                    vendorId = "VND-01",
                    orderNumber = "PO-2026-0001",
                    requestedBy = "system",
                    status = VendorPurchaseOrderStatus.APPROVED,
                    subtotal = Money(5000.0),
                    totalAmount = Money(5000.0),
                    items = listOf(poItem)
                )
            )
        }
    }

    @Test
    fun testCreateInvoiceSuccess() = runBlocking {
        val item = VendorInvoiceItem(
            itemId = "vii_01",
            invoiceId = "",
            purchaseOrderItemId = "poi_01",
            description = "Offset Paper 100gsm",
            quantity = BigDecimal("100"),
            unitPrice = Money(50.0),
            lineTotal = Money(5000.0)
        )

        val res = invoiceService.createInvoice(
            projectId = "PRJ-01",
            vendorId = "VND-01",
            purchaseOrderId = "PO-01",
            vendorInvoiceNumber = "APEX-INV-101",
            items = listOf(item),
            actorId = "staff_01"
        )

        assertTrue(res is DomainResult.Success)
        val created = (res as DomainResult.Success).data
        assertEquals("VND-01", created.vendorId)
        assertEquals("PO-01", created.purchaseOrderId)
        assertEquals("APEX-INV-101", created.vendorInvoiceNumber)
        assertEquals(Money(5000.0), created.totalAmount)
        assertEquals(VendorInvoiceStatus.DRAFT, created.status)
        assertEquals(VendorInvoiceMatchStatus.NOT_MATCHED, created.matchStatus)
    }

    @Test
    fun testCreateInvoiceAgainstInactiveVendorFails() = runBlocking {
        vendorRepo.createVendor(
            Vendor(
                vendorId = "VND-SUSPENDED",
                projectId = "PRJ-01",
                vendorCode = "V002",
                vendorName = "Suspended Vendor",
                status = VendorStatus.SUSPENDED
            )
        )

        val item = VendorInvoiceItem(
            itemId = "vii_02",
            invoiceId = "",
            purchaseOrderItemId = "poi_01",
            description = "Test Item",
            quantity = BigDecimal("10"),
            unitPrice = Money(50.0),
            lineTotal = Money(500.0)
        )

        val res = invoiceService.createInvoice(
            projectId = "PRJ-01",
            vendorId = "VND-SUSPENDED",
            purchaseOrderId = "PO-01",
            vendorInvoiceNumber = "SUS-INV-01",
            items = listOf(item)
        )

        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("SUSPENDED"))
    }
}
