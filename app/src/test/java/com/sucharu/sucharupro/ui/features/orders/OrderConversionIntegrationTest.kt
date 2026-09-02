package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.QuotationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrderConversionIntegrationTest {

    private lateinit var quotationDataSource: FakeQuotationDataSource
    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var quotationRepository: QuotationRepositoryImpl
    private lateinit var orderRepository: OrderRepositoryImpl

    private val rev1 = QuotationRevision(
        revisionId = "rev-001-v1",
        quotationId = "qt-200",
        revisionNumber = 1,
        items = listOf(
            QuotationItem(
                itemId = "item-01",
                description = "ম্যাগাজিন প্রিন্টিং (১২৮ পৃষ্ঠা)",
                specification = "A4, কভার ৪ কালার, ইনার ৮০ জিএসএম অফসেট",
                quantity = 1500,
                unit = "Copies",
                unitPrice = Money(180.0),
                discount = Money(10000.0)
            )
        ),
        discount = Money(5000.0),
        paymentTerms = PaymentTerms(type = PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 50),
        deliveryRequirement = DeliveryRequirement(
            deliveryType = DeliveryType.BUSINESS_DELIVERY,
            address = "১২ মতিঝিল বা/এ, ঢাকা",
            contactName = "তানভীর আহমেদ",
            contactPhone = "01711223344"
        ),
        notes = "নির্ধারিত সময়ে ডেলিভারি নিশ্চিত করতে হবে।",
        revisionReason = "Initial Quote",
        createdAt = "2026-08-16T10:00:00Z",
        createdBy = "Commercial Desk"
    )

    private val approvedQuotation = Quotation(
        quotationId = "qt-200",
        quotationNumber = "QT-2026-000200",
        customerId = "cus-002",
        inquiryId = "inq-002",
        currentRevisionNumber = 1,
        revisions = listOf(rev1),
        status = QuotationStatusType.APPROVED,
        validUntil = "2026-09-30",
        termsAndConditions = "নিয়মিত বাণিজ্যিক শর্ত প্রযোজ্য।",
        approvedAt = "2026-08-16T12:00:00Z",
        approvedBy = "Commercial Head",
        approvedRevisionId = "rev-001-v1",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T12:00:00Z"
    )

    @Before
    fun setUp() {
        quotationDataSource = FakeQuotationDataSource(listOf(approvedQuotation))
        orderDataSource = FakeOrderDataSource(emptyList())
        quotationRepository = QuotationRepositoryImpl(quotationDataSource)
        orderRepository = OrderRepositoryImpl(orderDataSource, quotationDataSource)
    }

    @Test
    fun orderConversion_fromApprovedQuotation_createsConfirmedOrderSnapshot() = runBlocking {
        val timestamp = "2026-08-16T14:00:00Z"
        val result = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-500",
            orderNumber = "ORD-2026-000500",
            quotationId = "qt-200",
            approvedRevisionId = "rev-001-v1",
            priority = OrderPriority.URGENT,
            confirmedBy = "Sales Coordinator",
            timestamp = timestamp
        )

        assertTrue(result is DomainResult.Success)
        val order = (result as DomainResult.Success).data
        assertEquals("ord-500", order.orderId)
        assertEquals("ORD-2026-000500", order.orderNumber)
        assertEquals("cus-002", order.customerId)
        assertEquals("qt-200", order.quotationId)
        assertEquals("rev-001-v1", order.approvedQuotationRevisionId)
        assertEquals(OrderStatusType.CONFIRMED, order.status)
        assertEquals(OrderPriority.URGENT, order.priority)
        assertEquals(JobHandoffStatus.READY_FOR_JOB, order.jobHandoffStatus)

        // Verify Commercial Snapshot
        assertEquals(1, order.items.size)
        val orderItem = order.items.first()
        assertEquals("ম্যাগাজিন প্রিন্টিং (১২৮ পৃষ্ঠা)", orderItem.description)
        assertEquals(1500, orderItem.quantity)
        assertEquals(Money(180.0), orderItem.unitPrice)
        assertEquals(Money(10000.0), orderItem.discount)
        // Subtotal = 1500 * 180 - 10000 = 260,000; Total = 260,000 - 5000 = 255,000
        assertEquals(Money(255000.0), order.totalAmount)

        // Verify Payment & Delivery Terms Snapshot
        assertEquals(PaymentTermType.PARTIAL_ADVANCE, order.paymentTerms.type)
        assertEquals(50, order.paymentTerms.advancePercentage)
        assertEquals(DeliveryType.BUSINESS_DELIVERY, order.deliveryRequirement?.deliveryType)
        assertEquals("১২ মতিঝিল বা/এ, ঢাকা", order.deliveryRequirement?.address)
    }

    @Test
    fun snapshotIsolation_quotationChangesAfterConversion_doNotAffectOrder() = runBlocking {
        // 1. Create Order from Rev #1
        val orderResult = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-600",
            orderNumber = "ORD-2026-000600",
            quotationId = "qt-200",
            approvedRevisionId = "rev-001-v1",
            timestamp = "2026-08-16T14:00:00Z"
        )
        assertTrue(orderResult is DomainResult.Success)

        // 2. Add a completely different Revision #2 to the quotation
        val rev2 = QuotationRevision(
            revisionId = "rev-001-v2",
            quotationId = "qt-200",
            revisionNumber = 2,
            items = listOf(
                QuotationItem(
                    itemId = "item-01",
                    description = "সংশোধিত ম্যাগাজিন প্রিন্টিং (২০০ পৃষ্ঠা)",
                    quantity = 5000, // Changed from 1500 to 5000
                    unitPrice = Money(300.0) // Changed from 180 to 300
                )
            ),
            createdAt = "2026-08-16T16:00:00Z"
        )
        quotationRepository.createQuotationRevision("qt-200", rev2)

        // 3. Verify Order remains completely unchanged with Rev #1 snapshot
        val fetchedOrder = orderRepository.getOrderById("ord-600").first()
        assertNotNull(fetchedOrder)
        assertEquals("rev-001-v1", fetchedOrder?.approvedQuotationRevisionId)
        assertEquals(1500, fetchedOrder?.items?.first()?.quantity)
        assertEquals(Money(180.0), fetchedOrder?.items?.first()?.unitPrice)
        assertEquals(Money(255000.0), fetchedOrder?.totalAmount)
    }

    @Test
    fun orderConversion_failsForUnapprovedQuotation() = runBlocking {
        // Create draft quotation
        val draftQuotation = approvedQuotation.copy(
            quotationId = "qt-draft",
            quotationNumber = "QT-2026-DRAFT-001",
            status = QuotationStatusType.DRAFT,
            approvedRevisionId = null,
            revisions = listOf(rev1.copy(quotationId = "qt-draft"))
        )
        val insertRes = quotationDataSource.insertQuotation(draftQuotation)
        assertTrue(insertRes is DomainResult.Success)

        val result = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-fail-1",
            orderNumber = "ORD-FAIL-1",
            quotationId = "qt-draft",
            approvedRevisionId = "rev-001-v1",
            timestamp = "2026-08-16T14:00:00Z"
        )

        assertTrue(result is DomainResult.Error)
        val errorMsg = (result as DomainResult.Error).message
        assertTrue(errorMsg.contains("APPROVED") || errorMsg.contains("unapproved"))
    }

    @Test
    fun orderConversion_failsWhenApprovedRevisionIdMismatches() = runBlocking {
        val result = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-fail-2",
            orderNumber = "ORD-FAIL-2",
            quotationId = "qt-200",
            approvedRevisionId = "rev-wrong-id",
            timestamp = "2026-08-16T14:00:00Z"
        )

        assertTrue(result is DomainResult.Error)
    }
}
