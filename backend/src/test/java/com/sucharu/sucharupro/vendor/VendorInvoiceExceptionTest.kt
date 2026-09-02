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

class VendorInvoiceExceptionTest {

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

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-01",
                    projectId = "PRJ-01",
                    vendorCode = "V001",
                    vendorName = "Apex Paper Mills",
                    status = VendorStatus.ACTIVE
                )
            )

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
    fun testExceptionGenerationAndResolution() = runBlocking {
        // Invoice without delivery receipt -> triggers RECEIPT_MISSING exception
        val invoiceItem = VendorInvoiceItem(
            itemId = "vii_01",
            invoiceId = "",
            purchaseOrderItemId = "poi_01",
            description = "Offset Paper 100gsm",
            quantity = BigDecimal("100"),
            unitPrice = Money(50.0),
            lineTotal = Money(5000.0)
        )

        val invRes = invoiceService.createInvoice(
            projectId = "PRJ-01",
            vendorId = "VND-01",
            purchaseOrderId = "PO-01",
            vendorInvoiceNumber = "VINV-EX-01",
            items = listOf(invoiceItem)
        )
        val invoice = (invRes as DomainResult.Success).data
        invoiceService.submitInvoice("PRJ-01", invoice.invoiceId)

        invoiceService.executeThreeWayMatch("PRJ-01", invoice.invoiceId)

        val exceptions = (invoiceService.listExceptions("PRJ-01", invoice.invoiceId) as DomainResult.Success).data
        assertEquals(1, exceptions.size)
        val ex = exceptions.first()
        assertEquals(VendorInvoiceExceptionType.RECEIPT_MISSING, ex.exceptionType)
        assertFalse(ex.resolved)

        // Resolve exception with authorization note
        val resolveRes = invoiceService.resolveException(
            projectId = "PRJ-01",
            exceptionId = ex.exceptionId,
            resolutionNotes = "Approved by Procurement Director due to urgent paper delivery waiver.",
            actorId = "procurement_director"
        )
        assertTrue(resolveRes is DomainResult.Success)
        val resolved = (resolveRes as DomainResult.Success).data
        assertTrue(resolved.resolved)
        assertEquals("procurement_director", resolved.resolvedBy)

        val updatedExceptions = (invoiceService.listExceptions("PRJ-01", invoice.invoiceId) as DomainResult.Success).data
        assertTrue(updatedExceptions.first().resolved)
    }
}
