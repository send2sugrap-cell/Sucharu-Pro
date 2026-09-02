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

class VendorInvoiceWorkflowTest {

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
    fun testFullLifecycleFromDraftToPosted() = runBlocking {
        val invoiceItem = VendorInvoiceItem(
            itemId = "vii_01",
            invoiceId = "",
            purchaseOrderItemId = "poi_01",
            description = "Offset Paper 100gsm",
            quantity = BigDecimal("100"),
            unitPrice = Money(50.0),
            lineTotal = Money(5000.0)
        )

        // 1. Create Draft
        val invRes = invoiceService.createInvoice(
            projectId = "PRJ-01",
            vendorId = "VND-01",
            purchaseOrderId = "PO-01",
            vendorInvoiceNumber = "VINV-FLOW-01",
            items = listOf(invoiceItem),
            actorId = "staff_creator"
        )
        assertTrue(invRes is DomainResult.Success)
        val invoice = (invRes as DomainResult.Success).data
        assertEquals(VendorInvoiceStatus.DRAFT, invoice.status)

        // 2. Submit
        val submitRes = invoiceService.submitInvoice("PRJ-01", invoice.invoiceId, "staff_creator")
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(VendorInvoiceStatus.SUBMITTED, (submitRes as DomainResult.Success).data.status)

        // 3. Match
        val matchRes = invoiceService.executeThreeWayMatch("PRJ-01", invoice.invoiceId, "system")
        assertTrue(matchRes is DomainResult.Success)
        assertEquals(VendorInvoiceMatchStatus.MATCHED, (matchRes as DomainResult.Success).data.matchStatus)

        val matchedInv = (invoiceService.getInvoiceById("PRJ-01", invoice.invoiceId) as DomainResult.Success).data
        assertEquals(VendorInvoiceStatus.MATCHED, matchedInv.status)

        // 4. Separation of Duties on Approval: Creator cannot approve
        val selfApproveRes = invoiceService.approveInvoice("PRJ-01", invoice.invoiceId, "staff_creator", allowSelfApproval = false)
        assertTrue(selfApproveRes is DomainResult.Error)

        // Manager approves
        val approveRes = invoiceService.approveInvoice("PRJ-01", invoice.invoiceId, "manager_approver")
        assertTrue(approveRes is DomainResult.Success)
        assertEquals(VendorInvoiceStatus.APPROVED, (approveRes as DomainResult.Success).data.status)

        // 5. Post (Ready for Payable)
        val postRes = invoiceService.postInvoice("PRJ-01", invoice.invoiceId, "manager_approver")
        assertTrue(postRes is DomainResult.Success)
        assertEquals(VendorInvoiceStatus.POSTED, (postRes as DomainResult.Success).data.status)
        assertTrue(postRes.data.status.isTerminal)

        // Verify audit log
        val audits = (invoiceService.listAudits("PRJ-01", invoice.invoiceId) as DomainResult.Success).data
        assertTrue(audits.any { it.eventType == "INVOICE_CREATED" })
        assertTrue(audits.any { it.eventType == "INVOICE_SUBMITTED" })
        assertTrue(audits.any { it.eventType == "MATCH_EXECUTED" })
        assertTrue(audits.any { it.eventType == "INVOICE_APPROVED" })
        assertTrue(audits.any { it.eventType == "INVOICE_POSTED" })
    }

    @Test
    fun testCancelDraftInvoice() = runBlocking {
        val invoiceItem = VendorInvoiceItem(
            itemId = "vii_02",
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
            vendorInvoiceNumber = "VINV-CANCEL-01",
            items = listOf(invoiceItem)
        )
        val invoice = (invRes as DomainResult.Success).data

        val cancelRes = invoiceService.cancelInvoice("PRJ-01", invoice.invoiceId, "Duplicate entry error", "manager_01")
        assertTrue(cancelRes is DomainResult.Success)
        assertEquals(VendorInvoiceStatus.CANCELLED, (cancelRes as DomainResult.Success).data.status)
        assertTrue(cancelRes.data.status.isTerminal)
    }
}
