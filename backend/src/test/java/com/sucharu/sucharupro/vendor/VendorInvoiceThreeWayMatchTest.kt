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

class VendorInvoiceThreeWayMatchTest {

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

            // Seed delivery receipt with 100 accepted quantity
            val receiptItem = VendorDeliveryReceiptItem(
                receiptItemId = "vri_01",
                deliveryReceiptId = "vdr_01",
                purchaseOrderId = "PO-01",
                purchaseOrderItemId = "poi_01",
                itemDescription = "Offset Paper 100gsm",
                orderedQuantity = BigDecimal("100"),
                receivedQuantity = BigDecimal("100"),
                acceptedQuantity = BigDecimal("100"),
                unitRate = Money(50.0),
                lineTotal = Money(5000.0)
            )
            receiptRepo.createReceipt(
                VendorDeliveryReceipt(
                    deliveryReceiptId = "vdr_01",
                    projectId = "PRJ-01",
                    receiptNumber = "VDR-2026-0001",
                    purchaseOrderId = "PO-01",
                    vendorId = "VND-01",
                    receivedBy = "system",
                    status = VendorDeliveryReceiptStatus.ACCEPTED,
                    items = listOf(receiptItem)
                )
            )
        }
    }

    @Test
    fun testExactMatchPasses() = runBlocking {
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
            vendorInvoiceNumber = "VINV-EXACT",
            items = listOf(invoiceItem)
        )
        assertTrue(invRes is DomainResult.Success)
        val invoice = (invRes as DomainResult.Success).data

        invoiceService.submitInvoice("PRJ-01", invoice.invoiceId)

        val matchRes = invoiceService.executeThreeWayMatch("PRJ-01", invoice.invoiceId)
        assertTrue(matchRes is DomainResult.Success)
        val match = (matchRes as DomainResult.Success).data
        assertEquals(VendorInvoiceMatchStatus.MATCHED, match.matchStatus)
        assertEquals(0, match.exceptionCount)
        assertEquals(Money.ZERO, match.priceVariance)
        assertEquals(BigDecimal.ZERO, match.quantityVariance)

        val loadedInvoice = (invoiceService.getInvoiceById("PRJ-01", invoice.invoiceId) as DomainResult.Success).data
        assertEquals(VendorInvoiceStatus.MATCHED, loadedInvoice.status)
        assertEquals(VendorInvoiceMatchStatus.MATCHED, loadedInvoice.matchStatus)
    }

    @Test
    fun testPriceVarianceTriggersMismatchException() = runBlocking {
        val invoiceItem = VendorInvoiceItem(
            itemId = "vii_02",
            invoiceId = "",
            purchaseOrderItemId = "poi_01",
            description = "Offset Paper 100gsm",
            quantity = BigDecimal("100"),
            unitPrice = Money(55.0), // PO is 50.0 -> Price Variance!
            lineTotal = Money(5500.0)
        )

        val invRes = invoiceService.createInvoice(
            projectId = "PRJ-01",
            vendorId = "VND-01",
            purchaseOrderId = "PO-01",
            vendorInvoiceNumber = "VINV-PRICE-VAR",
            items = listOf(invoiceItem)
        )
        val invoice = (invRes as DomainResult.Success).data
        invoiceService.submitInvoice("PRJ-01", invoice.invoiceId)

        val matchRes = invoiceService.executeThreeWayMatch("PRJ-01", invoice.invoiceId)
        assertTrue(matchRes is DomainResult.Success)
        val match = (matchRes as DomainResult.Success).data
        assertTrue(match.matchStatus in setOf(VendorInvoiceMatchStatus.MISMATCH, VendorInvoiceMatchStatus.EXCEPTION))
        assertTrue(match.exceptionCount > 0)
        assertEquals(Money(5.0), match.priceVariance)

        val exList = (invoiceService.listExceptions("PRJ-01", invoice.invoiceId) as DomainResult.Success).data
        assertTrue(exList.any { it.exceptionType == VendorInvoiceExceptionType.PRICE_VARIANCE })
    }

    @Test
    fun testUnreceivedQuantityTriggersException() = runBlocking {
        val invoiceItem = VendorInvoiceItem(
            itemId = "vii_03",
            invoiceId = "",
            purchaseOrderItemId = "poi_01",
            description = "Offset Paper 100gsm",
            quantity = BigDecimal("120"), // Receipt has only 100 accepted!
            unitPrice = Money(50.0),
            lineTotal = Money(6000.0)
        )

        val invRes = invoiceService.createInvoice(
            projectId = "PRJ-01",
            vendorId = "VND-01",
            purchaseOrderId = "PO-01",
            vendorInvoiceNumber = "VINV-QTY-VAR",
            items = listOf(invoiceItem)
        )
        val invoice = (invRes as DomainResult.Success).data
        invoiceService.submitInvoice("PRJ-01", invoice.invoiceId)

        val matchRes = invoiceService.executeThreeWayMatch("PRJ-01", invoice.invoiceId)
        assertTrue(matchRes is DomainResult.Success)
        val match = (matchRes as DomainResult.Success).data
        assertTrue(match.exceptionCount > 0)

        val exList = (invoiceService.listExceptions("PRJ-01", invoice.invoiceId) as DomainResult.Success).data
        assertTrue(exList.any { it.exceptionType == VendorInvoiceExceptionType.UNRECEIVED_QUANTITY })
    }
}
