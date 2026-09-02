package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeCommercialActivityDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInquiryDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderJobHandoffDataSource
import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.data.repository.CommercialActivityRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.InquiryRepositoryImpl
import com.sucharu.sucharupro.data.repository.OrderJobHandoffRepositoryImpl
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.QuotationRepositoryImpl
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
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

/**
 * Master End-to-End Integration & Commercial Pipeline Verification Test Suite
 * for Module 03 (Quotation & Order Management) in Sucharu Pro ERP.
 *
 * Proves that the complete commercial workflow operates as one coherent, validated,
 * audit-traceable, and isolated system from Customer Inquiry to Ready for Production.
 */
class Module03CommercialPipelineIntegrationTest {

    private lateinit var customerDataSource: FakeCustomerDataSource
    private lateinit var customerRepository: CustomerRepositoryImpl

    private lateinit var inquiryDataSource: FakeInquiryDataSource
    private lateinit var inquiryRepository: InquiryRepositoryImpl

    private lateinit var quotationDataSource: FakeQuotationDataSource
    private lateinit var quotationRepository: QuotationRepositoryImpl

    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var orderRepository: OrderRepositoryImpl

    private lateinit var handoffDataSource: FakeOrderJobHandoffDataSource
    private lateinit var handoffRepository: OrderJobHandoffRepositoryImpl

    private lateinit var activityDataSource: FakeCommercialActivityDataSource
    private lateinit var activityRepository: CommercialActivityRepositoryImpl

    private val testCustomer = Customer(
        customerId = "cus-master-01",
        customerCode = "CUS-000101",
        displayName = "রংধনু পাবলিকেশনস",
        contactPersonName = "তানভীর আহমেদ",
        primaryPhone = "+8801711000000",
        email = "tanvir@rongdhonu.com",
        customerType = CustomerType.BUSINESS,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val sampleItem = OrderItem(
        itemId = "item-01",
        description = "ব্যানার প্রিন্টিং",
        quantity = 5,
        unit = "Pcs",
        unitPrice = 500.toMoney()
    )

    @Before
    fun setUp() {
        customerDataSource = FakeCustomerDataSource()
        customerRepository = CustomerRepositoryImpl(customerDataSource)

        inquiryDataSource = FakeInquiryDataSource(emptyList())
        inquiryRepository = InquiryRepositoryImpl(inquiryDataSource)

        quotationDataSource = FakeQuotationDataSource(emptyList())
        quotationRepository = QuotationRepositoryImpl(quotationDataSource)

        orderDataSource = FakeOrderDataSource(emptyList())
        orderRepository = OrderRepositoryImpl(
            dataSource = orderDataSource,
            quotationDataSource = quotationDataSource
        )

        handoffDataSource = FakeOrderJobHandoffDataSource()
        handoffRepository = OrderJobHandoffRepositoryImpl(handoffDataSource)

        activityDataSource = FakeCommercialActivityDataSource()
        activityRepository = CommercialActivityRepositoryImpl(activityDataSource)
    }

    // =========================================================================
    // 1. COMPLETE HAPPY-PATH COMMERCIAL PIPELINE
    // =========================================================================

    @Test
    fun fullCommercialPipeline_fromInquiryToReadyForProduction_succeeds() = runBlocking {
        val now = "2026-08-16T10:00:00Z"

        // ── Phase 1: Customer Inquiry ──
        val inquiryItem = InquiryRequirement(
            itemId = "inq-it-01",
            productName = "Custom Diary",
            description = "হাতে তৈরি ডায়েরি প্রিন্টিং",
            quantity = 500,
            unit = "Pcs",
            paperMaterial = "লেদার বাইন্ডিং, গোল্ড ফয়েল"
        )
        val inquiry = Inquiry(
            inquiryId = "inq-001",
            inquiryNumber = "INQ-2026-0001",
            customerId = testCustomer.customerId,
            source = InquirySource.DIRECT_VISIT,
            status = InquiryStatusType.NEW,
            items = listOf(inquiryItem),
            notes = "মেলা উপলক্ষে জরুরি প্রিন্ট",
            createdAt = now,
            updatedAt = now
        )
        val inqRes = inquiryRepository.createInquiry(inquiry)
        assertTrue(inqRes is DomainResult.Success<*>)

        // ── Phase 2: Quotation Creation (Revision 1) ──
        val qItem1 = QuotationItem(
            itemId = "q-it-01",
            description = "হাতে তৈরি ডায়েরি প্রিন্টিং (লেদার বাইন্ডিং)",
            specification = "৩০০ GSM আর্ট কার্ড, ফয়েল স্ট্যাম্পিং",
            quantity = 500,
            unit = "Pcs",
            unitPrice = 220.toMoney(),
            discount = Money.ZERO
        )
        val rev1 = QuotationRevision(
            revisionId = "rev-001-v1",
            quotationId = "qt-001",
            revisionNumber = 1,
            items = listOf(qItem1),
            discount = Money.ZERO,
            paymentTerms = PaymentTerms(PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 50),
            deliveryRequirement = DeliveryRequirement(
                deliveryType = DeliveryType.BUSINESS_DELIVERY,
                address = "৩৮/২ বাংলাবাজার, ঢাকা-১১০০",
                contactName = testCustomer.contactPersonName,
                contactPhone = testCustomer.primaryPhone
            ),
            revisionReason = "Initial commercial offer",
            createdAt = now
        )
        val quotation = Quotation(
            quotationId = "qt-001",
            quotationNumber = "QT-2026-0001",
            customerId = testCustomer.customerId,
            inquiryId = inquiry.inquiryId,
            currentRevisionNumber = 1,
            revisions = listOf(rev1),
            status = QuotationStatusType.SENT,
            createdAt = now,
            updatedAt = now
        )
        val qRes = quotationRepository.createQuotation(quotation)
        assertTrue(qRes is DomainResult.Success<*>)

        // ── Phase 3: Negotiation & Revision 2 ──
        val negotiatedItem = qItem1.copy(
            unitPrice = 200.toMoney() // Negotiated price discount
        )
        val rev2 = QuotationRevision(
            revisionId = "rev-001-v2",
            quotationId = "qt-001",
            revisionNumber = 2,
            items = listOf(negotiatedItem),
            discount = Money.ZERO,
            paymentTerms = PaymentTerms(PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 50),
            deliveryRequirement = rev1.deliveryRequirement,
            revisionReason = "Customer price negotiation agreed to ৳200/pc",
            previousRevisionId = "rev-001-v1",
            createdAt = now
        )
        val updatedQWithRev2 = quotation.copy(
            currentRevisionNumber = 2,
            revisions = listOf(rev1, rev2),
            status = QuotationStatusType.SENT
        )
        quotationRepository.updateQuotation(updatedQWithRev2)

        // ── Phase 4: Formal Quotation Approval ──
        val approveRes = quotationRepository.approveQuotationRevision(
            quotationId = "qt-001",
            revisionId = "rev-001-v2",
            approvedBy = "Sales Manager",
            timestamp = now
        )
        assertTrue(approveRes is DomainResult.Success<*>)
        val approvedQ = (approveRes as DomainResult.Success<Quotation>).data
        assertTrue(approvedQ.isApproved)
        assertEquals("rev-001-v2", approvedQ.approvedRevisionId)

        // ── Phase 5: Order Conversion ──
        val convertRes = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-001",
            orderNumber = "ORD-2026-0001",
            quotationId = "qt-001",
            approvedRevisionId = "rev-001-v2",
            priority = OrderPriority.URGENT,
            confirmedBy = "Sales Desk",
            timestamp = now
        )
        assertTrue(convertRes is DomainResult.Success<*>)
        val createdOrder = (convertRes as DomainResult.Success<Order>).data
        assertEquals("ORD-2026-0001", createdOrder.orderNumber)
        assertEquals(OrderStatusType.CONFIRMED, createdOrder.status)
        assertEquals(OrderPriority.URGENT, createdOrder.priority)
        assertEquals("৳ 100,000", createdOrder.totalAmount.formatted())
        assertEquals(JobHandoffStatus.READY_FOR_JOB, createdOrder.jobHandoffStatus)

        // ── Phase 6: Order Commercial Handoff Creation ──
        val handoffRes = handoffRepository.createHandoff(
            handoffId = "hnd-001",
            order = createdOrder,
            createdBy = "Sales Desk",
            notes = "জরুরি মেলা ডেলিভারি",
            timestamp = now
        )
        assertTrue(handoffRes is DomainResult.Success<*>)
        val sealedHandoff = (handoffRes as DomainResult.Success<OrderJobHandoff>).data
        assertEquals(OrderJobHandoffStatus.READY_FOR_HANDOFF, sealedHandoff.handoffStatus)
        assertEquals("৳ 100,000", sealedHandoff.commercialTotal.formatted())
        assertEquals(500, sealedHandoff.totalQuantity)

        // ── Phase 7: Handoff Confirmation to Production ──
        val confirmHandoffRes = handoffRepository.confirmHandoff(
            handoffId = "hnd-001",
            confirmedBy = "Production Planning",
            timestamp = now
        )
        assertTrue(confirmHandoffRes is DomainResult.Success<*>)
        val confirmedHandoff = (confirmHandoffRes as DomainResult.Success<OrderJobHandoff>).data
        assertEquals(OrderJobHandoffStatus.HANDED_OFF, confirmedHandoff.handoffStatus)

        // ── Phase 8: Ready for Production Boundary ──
        val readyProdRes = handoffRepository.markReadyForProduction(
            handoffId = "hnd-001",
            timestamp = now
        )
        assertTrue(readyProdRes is DomainResult.Success<*>)
        val finalHandoff = (readyProdRes as DomainResult.Success<OrderJobHandoff>).data
        assertEquals(OrderJobHandoffStatus.READY_FOR_PRODUCTION, finalHandoff.handoffStatus)

        // Verification: No Module 04 entities leaked; jobReferenceId remains null placeholder
        assertEquals(null, finalHandoff.jobReferenceId)
    }

    // =========================================================================
    // 2. COMMERCIAL SNAPSHOT IMMUTABILITY TESTS
    // =========================================================================

    @Test
    fun quotationModifications_afterOrderConversion_doNotMutateOrder() = runBlocking {
        val now = "2026-08-16T10:00:00Z"
        val qItem = QuotationItem(
            itemId = "q-it-1",
            description = "Calendar 2027",
            quantity = 1000,
            unit = "Pcs",
            unitPrice = 50.toMoney()
        )
        val rev1 = QuotationRevision(
            revisionId = "rev-cal-1",
            quotationId = "qt-cal",
            revisionNumber = 1,
            items = listOf(qItem),
            createdAt = now
        )
        val quotation = Quotation(
            quotationId = "qt-cal",
            quotationNumber = "QT-CAL-01",
            customerId = testCustomer.customerId,
            status = QuotationStatusType.APPROVED,
            approvedRevisionId = "rev-cal-1",
            revisions = listOf(rev1),
            createdAt = now,
            updatedAt = now
        )
        quotationDataSource.insertQuotation(quotation)

        // Convert Order
        val order = orderRepository.createOrderFromApprovedQuotation(
            orderId = "ord-cal-01",
            orderNumber = "ORD-CAL-01",
            quotationId = "qt-cal",
            approvedRevisionId = "rev-cal-1",
            timestamp = now
        )
        assertTrue(order is DomainResult.Success<*>)

        // Later: Mutate Quotation in repository (cancel, change items, add new revision)
        val newRev = rev1.copy(
            revisionId = "rev-cal-2",
            revisionNumber = 2,
            items = listOf(qItem.copy(unitPrice = 10.toMoney())) // Drastically reduced price
        )
        val mutatedQ = quotation.copy(
            status = QuotationStatusType.CANCELLED,
            currentRevisionNumber = 2,
            revisions = listOf(rev1, newRev)
        )
        quotationDataSource.updateQuotation(mutatedQ)

        // Order snapshot must remain completely unaffected
        val orderFromRepo = (orderRepository.findOrderById("ord-cal-01") as DomainResult.Success<Order>).data
        assertEquals(OrderStatusType.CONFIRMED, orderFromRepo.status)
        assertEquals("৳ 50,000", orderFromRepo.totalAmount.formatted())
        assertEquals(50.toMoney(), orderFromRepo.items[0].unitPrice)
        assertEquals(1000, orderFromRepo.items[0].quantity)
    }

    @Test
    fun orderLifecycleModifications_afterHandoffCreation_doNotMutateHandoff() = runBlocking {
        val now = "2026-08-16T10:00:00Z"
        val order = Order(
            orderId = "ord-snap-01",
            orderNumber = "ORD-SNAP-01",
            customerId = testCustomer.customerId,
            status = OrderStatusType.CONFIRMED,
            priority = OrderPriority.URGENT,
            items = listOf(sampleItem),
            createdAt = now,
            updatedAt = now
        )
        orderDataSource.insertOrder(order)

        // Create sealed handoff snapshot
        val handoff = handoffRepository.createHandoff(
            handoffId = "hnd-snap-01",
            order = order,
            notes = "Original handoff notes",
            timestamp = now
        )
        assertTrue(handoff is DomainResult.Success<*>)

        // Later: Change Order operational priority and remarks
        orderRepository.updateOrderPriority("ord-snap-01", OrderPriority.NORMAL)
        orderRepository.updateOrderNotes("ord-snap-01", "Updated operational remarks")

        // Handoff snapshot remains completely unmodified
        val handoffFromRepo = (handoffRepository.findHandoffById("hnd-snap-01") as DomainResult.Success<OrderJobHandoff>).data
        assertEquals(OrderPriority.URGENT, handoffFromRepo.priority)
        assertEquals("Original handoff notes", handoffFromRepo.notes)
        assertEquals("৳ 2,500", handoffFromRepo.commercialTotal.formatted())
    }

    // =========================================================================
    // 3. FAILED OPERATIONS & ZERO AUDIT EMISSION INVARIANTS
    // =========================================================================

    @Test
    fun failedOperations_produceNoStateMutation_andZeroAuditEvents() = runBlocking {
        val order = Order(
            orderId = "ord-fail-01",
            orderNumber = "ORD-FAIL-01",
            customerId = testCustomer.customerId,
            status = OrderStatusType.CONFIRMED,
            items = listOf(sampleItem),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
        val insertResult = orderDataSource.insertOrder(order)
        assertTrue(insertResult is DomainResult.Success<*>)

        // 1. Invalid status transition (CONFIRMED -> PENDING is illegal)
        val invalidTransition = orderRepository.updateOrderStatus("ord-fail-01", OrderStatusType.PENDING)
        assertTrue(invalidTransition is DomainResult.Error)

        // Verify order status unchanged
        val fetchedOrder = (orderRepository.findOrderById("ord-fail-01") as DomainResult.Success<Order>).data
        assertEquals(OrderStatusType.CONFIRMED, fetchedOrder.status)

        // 2. Cancellation with blank reason is illegal
        val blankCancel = orderRepository.cancelOrder("ord-fail-01", "   ")
        assertTrue(blankCancel is DomainResult.Error)

        // Verify order status still CONFIRMED
        val fetchedOrder2 = (orderRepository.findOrderById("ord-fail-01") as DomainResult.Success<Order>).data
        assertEquals(OrderStatusType.CONFIRMED, fetchedOrder2.status)

        // 3. Verify no false success audit events were created in activity repo
        val activities = activityDataSource.observeActivities().first()
        assertTrue(activities.none { it.entityId == "ord-fail-01" })
    }

    // =========================================================================
    // 4. TERMINAL STATE SAFETY INVARIANTS
    // =========================================================================

    @Test
    fun terminalDeliveredAndCancelledOrders_rejectAllMutations() = runBlocking {
        val now = "2026-08-16T10:00:00Z"
        val delivered = Order(
            orderId = "ord-term-del",
            orderNumber = "ORD-TERM-DEL",
            customerId = testCustomer.customerId,
            status = OrderStatusType.DELIVERED,
            items = listOf(sampleItem),
            createdAt = now,
            updatedAt = now
        )
        val cancelled = Order(
            orderId = "ord-term-can",
            orderNumber = "ORD-TERM-CAN",
            customerId = testCustomer.customerId,
            status = OrderStatusType.CANCELLED,
            items = listOf(sampleItem),
            createdAt = now,
            updatedAt = now
        )
        orderDataSource.insertOrder(delivered)
        orderDataSource.insertOrder(cancelled)

        // Rejects status changes
        assertTrue(orderRepository.updateOrderStatus("ord-term-del", OrderStatusType.CONFIRMED) is DomainResult.Error)
        assertTrue(orderRepository.updateOrderStatus("ord-term-can", OrderStatusType.CONFIRMED) is DomainResult.Error)

        // Rejects priority changes
        assertTrue(orderRepository.updateOrderPriority("ord-term-del", OrderPriority.URGENT) is DomainResult.Error)
        assertTrue(orderRepository.updateOrderPriority("ord-term-can", OrderPriority.URGENT) is DomainResult.Error)

        // Rejects handoff creation
        assertTrue(handoffRepository.createHandoff("hnd-del", delivered, timestamp = now) is DomainResult.Error)
        assertTrue(handoffRepository.createHandoff("hnd-can", cancelled, timestamp = now) is DomainResult.Error)
    }

    // =========================================================================
    // 5. BANGLA / UNICODE FIDELITY INVARIANT
    // =========================================================================

    @Test
    fun banglaUnicodeFidelity_acrossEntireCommercialPipeline() = runBlocking {
        val now = "2026-08-16T10:00:00Z"
        val unicodeItem = OrderItem(
            itemId = "item-bangla",
            description = "বাংলা ব্যাকরণ ও নির্মিতি বই",
            specification = "চার কালার প্রচ্ছদ, ৮০ জিএসএম অফসেট কাগজ",
            quantity = 2500,
            unit = "কপি",
            unitPrice = 180.toMoney()
        )
        val unicodeOrder = Order(
            orderId = "ord-bangla-01",
            orderNumber = "ORD-বাংলা-০১",
            customerId = testCustomer.customerId,
            status = OrderStatusType.CONFIRMED,
            items = listOf(unicodeItem),
            notes = "মেলা উপলক্ষে জরুরি মুদ্রণ ও সরবরাহ",
            createdAt = now,
            updatedAt = now
        )
        orderDataSource.insertOrder(unicodeOrder)

        // Verify handoff preserves Bengali Unicode text exactly
        val handoff = handoffRepository.createHandoff(
            handoffId = "hnd-bangla-01",
            order = unicodeOrder,
            notes = "বিশেষ নির্দেশনা: দ্রুত ডেলিভারি",
            timestamp = now
        )
        assertTrue(handoff is DomainResult.Success<*>)
        val data = (handoff as DomainResult.Success<OrderJobHandoff>).data
        assertEquals("ORD-বাংলা-০১", data.orderNumber)
        assertEquals("বাংলা ব্যাকরণ ও নির্মিতি বই", data.items[0].description)
        assertEquals("চার কালার প্রচ্ছদ, ৮০ জিএসএম অফসেট কাগজ", data.items[0].specification)
        assertEquals("কপি", data.items[0].unit)
        assertEquals("বিশেষ নির্দেশনা: দ্রুত ডেলিভারি", data.notes)
    }
}
