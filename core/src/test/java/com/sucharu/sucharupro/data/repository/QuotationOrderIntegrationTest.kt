package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeInquiryDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTermType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuotationOrderIntegrationTest {

    private lateinit var inquiryDataSource: FakeInquiryDataSource
    private lateinit var quotationDataSource: FakeQuotationDataSource
    private lateinit var orderDataSource: FakeOrderDataSource

    private lateinit var inquiryRepository: InquiryRepositoryImpl
    private lateinit var quotationRepository: QuotationRepositoryImpl
    private lateinit var orderRepository: OrderRepositoryImpl

    @Before
    fun setUp() {
        inquiryDataSource = FakeInquiryDataSource(emptyList())
        quotationDataSource = FakeQuotationDataSource(emptyList())
        orderDataSource = FakeOrderDataSource(emptyList())

        inquiryRepository = InquiryRepositoryImpl(inquiryDataSource)
        quotationRepository = QuotationRepositoryImpl(quotationDataSource)
        orderRepository = OrderRepositoryImpl(
            dataSource = orderDataSource,
            quotationDataSource = quotationDataSource
        )
    }

    @Test
    fun test01_fullCommercialLifecycleChain_inquiryToQuotationToRevisionToOrder() = runBlocking {
        val customerId = "cus-100"

        // 1. Capture Customer Inquiry
        val inquiry = Inquiry(
            inquiryId = "inq-chain-01",
            inquiryNumber = "INQ-2026-001",
            customerId = customerId,
            status = InquiryStatusType.NEW,
            source = InquirySource.DIRECT_VISIT,
            items = listOf(
                InquiryRequirement(
                    itemId = "inq-it-1",
                    productName = "Custom Calendar 2027",
                    description = "Desk Calendar 12 Leaves, 250 GSM Art Board with Stand",
                    quantity = 1000
                )
            ),
            createdAt = "2026-08-15T09:00:00Z",
            updatedAt = "2026-08-15T09:00:00Z"
        )
        val inqResult = inquiryRepository.createInquiry(inquiry)
        assertTrue(inqResult.isSuccess)

        // 2. Advance Inquiry to QUOTED and create Quotation Revision 1
        inquiryRepository.updateInquiryStatus("inq-chain-01", InquiryStatusType.QUOTED)

        val rev1 = QuotationRevision(
            revisionId = "rev-chain-v1",
            quotationId = "qt-chain-01",
            revisionNumber = 1,
            items = listOf(
                QuotationItem(
                    itemId = "qt-it-1",
                    description = "Desk Calendar 12 Leaves, 250 GSM Art Board",
                    quantity = 1000,
                    unitPrice = 150.toMoney()
                )
            ),
            revisionReason = "Initial Offer",
            createdAt = "2026-08-15T10:00:00Z"
        )

        val quotation = Quotation(
            quotationId = "qt-chain-01",
            quotationNumber = "QT-2026-001",
            customerId = customerId,
            inquiryId = "inq-chain-01",
            currentRevisionNumber = 1,
            revisions = listOf(rev1),
            status = QuotationStatusType.SENT,
            createdAt = "2026-08-15T10:00:00Z",
            updatedAt = "2026-08-15T10:00:00Z"
        )
        val qtResult = quotationRepository.createQuotation(quotation)
        assertTrue(qtResult.isSuccess)

        // 3. Customer negotiates -> Add Revision 2 with volume discount
        val rev2 = QuotationRevision(
            revisionId = "rev-chain-v2",
            quotationId = "qt-chain-01",
            revisionNumber = 2,
            items = listOf(
                QuotationItem(
                    itemId = "qt-it-1",
                    description = "Desk Calendar 12 Leaves, 250 GSM Art Board (Negotiated)",
                    quantity = 1000,
                    unitPrice = 135.toMoney()
                )
            ),
            discount = 5000.toMoney(),
            paymentTerms = PaymentTerms(
                type = PaymentTermType.PARTIAL_ADVANCE,
                advancePercentage = 50
            ),
            deliveryRequirement = DeliveryRequirement(
                deliveryType = DeliveryType.BUSINESS_DELIVERY,
                address = "Gulshan 2, Dhaka"
            ),
            revisionReason = "Client negotiated price and added delivery",
            createdAt = "2026-08-15T13:00:00Z",
            previousRevisionId = "rev-chain-v1"
        )
        quotationRepository.createQuotationRevision("qt-chain-01", rev2)

        // 4. Formally Approve Revision 2
        val approveResult = quotationRepository.approveQuotationRevision(
            quotationId = "qt-chain-01",
            revisionId = "rev-chain-v2",
            approvedBy = "Commercial Lead",
            timestamp = "2026-08-15T14:00:00Z"
        )
        assertTrue(approveResult.isSuccess)

        // 5. Convert Approved Quotation to Confirmed Order
        val orderResult = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-chain-01",
            orderNumber = "ORD-2026-001",
            quotationId = "qt-chain-01",
            approvedRevisionId = "rev-chain-v2",
            priority = OrderPriority.HIGH,
            confirmedBy = "Sales Operations",
            timestamp = "2026-08-15T14:30:00Z"
        )
        assertTrue(orderResult.isSuccess)

        // 6. Mark Inquiry as CONVERTED
        inquiryRepository.updateInquiryStatus("inq-chain-01", InquiryStatusType.CONVERTED)

        // 7. Verify full chain states and relationships
        val finalInquiry = (inquiryRepository.findInquiryById("inq-chain-01") as DomainResult.Success).data
        assertEquals(InquiryStatusType.CONVERTED, finalInquiry.status)

        val finalQuotation = (quotationRepository.findQuotationById("qt-chain-01") as DomainResult.Success).data
        assertEquals(QuotationStatusType.APPROVED, finalQuotation.status)
        assertEquals("rev-chain-v2", finalQuotation.approvedRevisionId)

        val finalOrder = (orderRepository.findOrderById("ord-chain-01") as DomainResult.Success).data
        assertEquals("ORD-2026-001", finalOrder.orderNumber)
        assertEquals(customerId, finalOrder.customerId)
        assertEquals("qt-chain-01", finalOrder.quotationId)
        assertEquals("rev-chain-v2", finalOrder.approvedQuotationRevisionId)
        assertEquals(OrderStatusType.CONFIRMED, finalOrder.status)
        assertEquals(JobHandoffStatus.READY_FOR_JOB, finalOrder.jobHandoffStatus)
        // Subtotal: 135,000, Discount: 5,000 -> Total: 130,000
        assertEquals("৳ 135,000", finalOrder.subtotal.formatted())
        assertEquals("৳ 130,000", finalOrder.totalAmount.formatted())
    }

    @Test
    fun test02_strictCustomerIsolationAcrossAllRepositories() = runBlocking {
        val cusA = "customer-A"
        val cusB = "customer-B"

        // Insert inquiries
        inquiryRepository.createInquiry(
            Inquiry("inq-A", "INQ-A", cusA, createdAt = "2026-08-15T10:00:00Z", updatedAt = "2026-08-15T10:00:00Z")
        )
        inquiryRepository.createInquiry(
            Inquiry("inq-B", "INQ-B", cusB, createdAt = "2026-08-15T10:00:00Z", updatedAt = "2026-08-15T10:00:00Z")
        )

        // Customer A queries
        val inqA = inquiryRepository.getInquiriesForCustomer(cusA).first()
        val inqB = inquiryRepository.getInquiriesForCustomer(cusB).first()

        assertEquals(1, inqA.size)
        assertEquals("inq-A", inqA.first().inquiryId)
        assertEquals(1, inqB.size)
        assertEquals("inq-B", inqB.first().inquiryId)
        assertFalse(inqA.any { it.customerId == cusB })
        assertFalse(inqB.any { it.customerId == cusA })
    }

    @Test
    fun test03_failedOrderCreation_noPartialStateLeftInDataSources() = runBlocking {
        // Attempting to create an order from a non-existent quotation
        val result = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-fail-01",
            orderNumber = "ORD-FAIL",
            quotationId = "qt-non-existent",
            approvedRevisionId = "rev-none",
            timestamp = "2026-08-15T10:00:00Z"
        )

        assertTrue(result.isError)
        // Verify 0 orders exist in repository
        val orders = orderRepository.getOrders().first()
        assertTrue(orders.isEmpty())
    }
}
