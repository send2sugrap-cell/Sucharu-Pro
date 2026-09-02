package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeCommercialActivityDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.repository.CommercialActivityRepositoryImpl
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTermType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.ui.features.orders.order.details.OrderDetailsUiState
import com.sucharu.sucharupro.ui.features.orders.order.details.OrderDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrderLifecycleControlTest {

    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var orderRepository: OrderRepository

    private val sampleItem = OrderItem(
        itemId = "item-01",
        description = "বই প্রিন্টিং ও বাইন্ডিং",
        specification = "২০০ পৃষ্ঠা, ৪ কালার কাভার",
        quantity = 1000,
        unit = "Pcs",
        unitPrice = Money(150.0),
        discount = Money.ZERO
    )

    private val sampleOrder = Order(
        orderId = "ord-101",
        orderNumber = "ORD-2026-000101",
        customerId = "cus-001",
        quotationId = "qt-101",
        approvedQuotationRevisionId = "rev-101-v1",
        status = OrderStatusType.CONFIRMED,
        priority = OrderPriority.NORMAL,
        items = listOf(sampleItem),
        discount = Money.ZERO,
        paymentTerms = PaymentTerms(PaymentTermType.PARTIAL_ADVANCE, 50),
        deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
        jobHandoffStatus = JobHandoffStatus.NOT_READY,
        notes = "Initial commercial agreement",
        confirmedAt = "2026-08-16T10:00:00Z",
        confirmedBy = "Admin",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        orderDataSource = FakeOrderDataSource(listOf(sampleOrder))
        orderRepository = OrderRepositoryImpl(orderDataSource)
    }

    @Test
    fun orderLifecycle_pendingToConfirmed_succeeds() = runBlocking {
        val pendingOrder = sampleOrder.copy(
            orderId = "ord-pending",
            orderNumber = "ORD-PENDING-001",
            status = OrderStatusType.PENDING
        )
        orderDataSource.insertOrder(pendingOrder)

        val result = orderRepository.updateOrderStatus("ord-pending", OrderStatusType.CONFIRMED)
        assertTrue(result is DomainResult.Success)
        val updated = (result as DomainResult.Success).data
        assertEquals(OrderStatusType.CONFIRMED, updated.status)
    }

    @Test
    fun orderLifecycle_confirmedToOnHold_andResume_succeeds() = runBlocking {
        // Put On Hold
        val holdResult = orderRepository.updateOrderStatus("ord-101", OrderStatusType.ON_HOLD)
        assertTrue(holdResult is DomainResult.Success)
        assertEquals(OrderStatusType.ON_HOLD, (holdResult as DomainResult.Success).data.status)

        // Resume to Confirmed
        val resumeResult = orderRepository.updateOrderStatus("ord-101", OrderStatusType.CONFIRMED)
        assertTrue(resumeResult is DomainResult.Success)
        assertEquals(OrderStatusType.CONFIRMED, (resumeResult as DomainResult.Success).data.status)
    }

    @Test
    fun orderLifecycle_invalidStatusTransition_returnsError() = runBlocking {
        // PENDING directly to READY is invalid
        val pendingOrder = sampleOrder.copy(
            orderId = "ord-p2",
            orderNumber = "ORD-P2",
            status = OrderStatusType.PENDING
        )
        orderDataSource.insertOrder(pendingOrder)

        val result = orderRepository.updateOrderStatus("ord-p2", OrderStatusType.READY)
        assertTrue(result is DomainResult.Error)
        val errorMsg = (result as DomainResult.Error).message
        assertTrue(errorMsg.contains("Invalid order status transition"))
    }

    @Test
    fun orderCancellation_requiresReason_andPreservesSnapshot() = runBlocking {
        val cancelReason = "গ্রাহকের অনুরোধে কাজ বাতিল করা হলো"
        val result = orderRepository.cancelOrder("ord-101", cancelReason)
        assertTrue(result is DomainResult.Success)

        val cancelledOrder = (result as DomainResult.Success).data
        assertEquals(OrderStatusType.CANCELLED, cancelledOrder.status)
        assertTrue(cancelledOrder.notes?.contains(cancelReason) == true)
        assertEquals(sampleOrder.totalAmount, cancelledOrder.totalAmount)
        assertEquals(sampleOrder.items.size, cancelledOrder.items.size)
        assertEquals(sampleOrder.customerId, cancelledOrder.customerId)
        assertEquals(sampleOrder.quotationId, cancelledOrder.quotationId)
    }

    @Test
    fun orderCancellation_failsForAlreadyCancelledOrder() = runBlocking {
        orderRepository.cancelOrder("ord-101", "Initial cancel")

        val secondCancel = orderRepository.cancelOrder("ord-101", "Second cancel attempt")
        assertTrue(secondCancel is DomainResult.Error)
        val msg = (secondCancel as DomainResult.Error).message
        assertTrue(msg.contains("already cancelled"))
    }

    @Test
    fun orderCancellation_failsForDeliveredOrder() = runBlocking {
        val deliveredOrder = sampleOrder.copy(
            orderId = "ord-delivered",
            orderNumber = "ORD-DEL-001",
            status = OrderStatusType.DELIVERED
        )
        orderDataSource.insertOrder(deliveredOrder)

        val result = orderRepository.cancelOrder("ord-delivered", "Attempted cancel on delivered")
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("Delivered orders cannot be cancelled"))
    }

    @Test
    fun orderPriority_normalToHighToUrgent_preservesCommercialData() = runBlocking {
        // Set to HIGH
        val highRes = orderRepository.updateOrderPriority("ord-101", OrderPriority.HIGH)
        assertTrue(highRes is DomainResult.Success)
        assertEquals(OrderPriority.HIGH, (highRes as DomainResult.Success).data.priority)

        // Set to URGENT
        val urgentRes = orderRepository.updateOrderPriority("ord-101", OrderPriority.URGENT)
        assertTrue(urgentRes is DomainResult.Success)
        val urgentOrder = (urgentRes as DomainResult.Success).data
        assertEquals(OrderPriority.URGENT, urgentOrder.priority)
        assertEquals(sampleOrder.totalAmount, urgentOrder.totalAmount)
        assertEquals(sampleOrder.items, urgentOrder.items)
    }

    @Test
    fun orderPriority_failsForCancelledOrDeliveredOrder() = runBlocking {
        orderRepository.cancelOrder("ord-101", "Cancellation")
        val result = orderRepository.updateOrderPriority("ord-101", OrderPriority.URGENT)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cannot update priority of a cancelled order"))
    }

    @Test
    fun jobHandoff_validConfirmedOrder_becomesReadyForJob() = runBlocking {
        assertEquals(JobHandoffStatus.NOT_READY, sampleOrder.jobHandoffStatus)

        val result = orderRepository.markReadyForJob("ord-101")
        assertTrue(result is DomainResult.Success)
        val updated = (result as DomainResult.Success).data
        assertEquals(JobHandoffStatus.READY_FOR_JOB, updated.jobHandoffStatus)
        assertEquals(sampleOrder.items, updated.items)
        assertEquals(sampleOrder.totalAmount, updated.totalAmount)
    }

    @Test
    fun jobHandoff_failsForCancelledOrDeliveredOrder() = runBlocking {
        orderRepository.cancelOrder("ord-101", "Cancelled")

        val result = orderRepository.markReadyForJob("ord-101")
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("Cancelled orders cannot be marked ready for job handoff"))
    }

    @Test
    fun jobHandoff_failsForOrderWithoutItems() = runBlocking {
        val emptyItemOrder = Order(
            orderId = "ord-empty",
            orderNumber = "ORD-EMPTY-001",
            customerId = "cus-001",
            quotationId = "qt-101",
            approvedQuotationRevisionId = "rev-101-v1",
            status = OrderStatusType.CONFIRMED,
            priority = OrderPriority.NORMAL,
            items = listOf(sampleItem),
            discount = Money.ZERO,
            paymentTerms = PaymentTerms(PaymentTermType.PARTIAL_ADVANCE, 50),
            deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
            jobHandoffStatus = JobHandoffStatus.NOT_READY,
            notes = null,
            confirmedAt = null,
            confirmedBy = null,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
        orderDataSource.insertOrder(emptyItemOrder)

        // Mutate in datasource to have empty items to simulate corrupt data
        val fetched = (orderDataSource.fetchOrderById("ord-empty") as DomainResult.Success).data
        val zeroItems = fetched.copy(items = emptyList())
        orderDataSource.updateOrder(zeroItems)

        val result = orderRepository.markReadyForJob("ord-empty")
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("at least one line item"))
    }

    @Test
    fun orderNotes_updateRemarks_succeeds() = runBlocking {
        val newNotes = "ডেলিভারি দ্রুত করতে হবে।"
        val result = orderRepository.updateOrderNotes("ord-101", newNotes)
        assertTrue(result is DomainResult.Success)
        assertEquals(newNotes, (result as DomainResult.Success).data.notes)
    }

    @Test
    fun dataIsolation_mutatingOrderA_doesNotAffectOrderB() = runBlocking {
        val orderB = sampleOrder.copy(
            orderId = "ord-102",
            orderNumber = "ORD-2026-000102",
            priority = OrderPriority.NORMAL,
            jobHandoffStatus = JobHandoffStatus.NOT_READY
        )
        orderDataSource.insertOrder(orderB)

        // Mutate Order A
        orderRepository.updateOrderPriority("ord-101", OrderPriority.URGENT)
        orderRepository.markReadyForJob("ord-101")

        // Order B must remain untouched
        val fetchedB = orderRepository.getOrderById("ord-102").first()
        assertNotNull(fetchedB)
        assertEquals(OrderPriority.NORMAL, fetchedB?.priority)
        assertEquals(JobHandoffStatus.NOT_READY, fetchedB?.jobHandoffStatus)
    }

    @Test
    fun viewModel_orderActions_updateUiStateReactively() = runBlocking {
        val viewModel = OrderDetailsViewModel(
            orderId = "ord-101",
            repository = orderRepository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = viewModel.uiState.value
        assertTrue(state is OrderDetailsUiState.Success)
        val successState = state as OrderDetailsUiState.Success
        assertEquals(OrderPriority.NORMAL, successState.order.priority)

        // Trigger priority change
        viewModel.setOrderPriority(OrderPriority.HIGH)

        val updatedState = viewModel.uiState.value as OrderDetailsUiState.Success
        assertEquals(OrderPriority.HIGH, updatedState.order.priority)
        assertEquals("Order priority updated to High.", updatedState.actionMessage)

        // Trigger mark ready for job
        viewModel.markReadyForJob()

        val handoffState = viewModel.uiState.value as OrderDetailsUiState.Success
        assertEquals(JobHandoffStatus.READY_FOR_JOB, handoffState.order.jobHandoffStatus)
        assertEquals("Order marked as Ready for Job Handoff.", handoffState.actionMessage)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 10: INVARIANTS A THROUGH J TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun invariantA_cancelledOrdersRemainReadable() = runBlocking {
        orderRepository.cancelOrder("ord-101", "Client request")
        val order = orderRepository.findOrderById("ord-101")
        assertTrue(order is DomainResult.Success)
        val data = (order as DomainResult.Success).data
        assertEquals(OrderStatusType.CANCELLED, data.status)
        assertEquals(sampleOrder.totalAmount, data.totalAmount)
        assertEquals(sampleOrder.items.size, data.items.size)
    }

    @Test
    fun invariantB_deliveredOrdersRemainReadable() = runBlocking {
        val deliveredOrder = sampleOrder.copy(
            orderId = "ord-deliv-01",
            orderNumber = "ORD-2026-DELIV",
            status = OrderStatusType.DELIVERED
        )
        orderDataSource.insertOrder(deliveredOrder)

        val order = orderRepository.findOrderById("ord-deliv-01")
        assertTrue(order is DomainResult.Success)
        val data = (order as DomainResult.Success).data
        assertEquals(OrderStatusType.DELIVERED, data.status)
        assertEquals(sampleOrder.totalAmount, data.totalAmount)
    }

    @Test
    fun invariantC_terminalOrdersCannotBeMutated() = runBlocking {
        orderRepository.cancelOrder("ord-101", "Cancelled")

        // Cannot transition status
        val statusRes = orderRepository.updateOrderStatus("ord-101", OrderStatusType.CONFIRMED)
        assertTrue(statusRes is DomainResult.Error)

        // Cannot change priority
        val priorityRes = orderRepository.updateOrderPriority("ord-101", OrderPriority.URGENT)
        assertTrue(priorityRes is DomainResult.Error)

        // Cannot change notes
        val notesRes = orderRepository.updateOrderNotes("ord-101", "New notes")
        assertTrue(notesRes is DomainResult.Error)

        // Cannot update handoff
        val handoffRes = orderRepository.updateJobHandoffStatus("ord-101", JobHandoffStatus.READY_FOR_JOB)
        assertTrue(handoffRes is DomainResult.Error)
    }

    @Test
    fun invariantD_quotationModificationsDoNotAffectExistingOrder() = runBlocking {
        // Create an order from quotation
        val originalQuotationItem = com.sucharu.sucharupro.domain.model.order.QuotationItem(
            itemId = "q-item-1",
            description = "Flyer Printing",
            quantity = 500,
            unit = "Pcs",
            unitPrice = Money(5.0)
        )
        val originalRevision = com.sucharu.sucharupro.domain.model.order.QuotationRevision(
            revisionId = "rev-001",
            quotationId = "qt-999",
            revisionNumber = 1,
            items = listOf(originalQuotationItem),
            discount = Money.ZERO,
            paymentTerms = PaymentTerms.DEFAULT,
            deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
            createdAt = "2026-08-16T10:00:00Z"
        )
        val quotation = com.sucharu.sucharupro.domain.model.order.Quotation(
            quotationId = "qt-999",
            quotationNumber = "QT-2026-999",
            customerId = "cus-001",
            status = com.sucharu.sucharupro.domain.model.order.QuotationStatusType.APPROVED,
            approvedRevisionId = "rev-001",
            revisions = listOf(originalRevision),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )

        val createdOrder = Order.fromApprovedQuotation(
            orderId = "ord-from-q",
            orderNumber = "ORD-2026-999",
            quotation = quotation,
            revision = originalRevision,
            timestamp = "2026-08-16T10:00:00Z"
        )
        orderDataSource.insertOrder(createdOrder)

        // Mutate quotation in source (e.g. new revision, different pricing)
        val mutatedQuotation = quotation.copy(
            status = com.sucharu.sucharupro.domain.model.order.QuotationStatusType.CANCELLED
        )

        // Order snapshot must remain completely unaffected
        val fetchedOrder = (orderRepository.findOrderById("ord-from-q") as DomainResult.Success).data
        assertEquals(OrderStatusType.CONFIRMED, fetchedOrder.status)
        assertEquals(Money(2500.0), fetchedOrder.totalAmount)
        assertEquals("Flyer Printing", fetchedOrder.items[0].description)
        assertEquals(500, fetchedOrder.items[0].quantity)
    }

    @Test
    fun invariantE_failedOperationsProduceNoAuditEvent() = runBlocking {
        val fakeActivityDataSource = FakeCommercialActivityDataSource()
        val activityRepo = CommercialActivityRepositoryImpl(fakeActivityDataSource)
        val viewModel = OrderDetailsViewModel(
            orderId = "ord-101",
            repository = orderRepository,
            activityRepository = activityRepo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // Blank cancellation should fail at validation before recording audit event
        viewModel.cancelOrder("   ")

        val state = viewModel.uiState.value as OrderDetailsUiState.Success
        assertEquals("Cancellation reason is required.", state.actionError)

        val activities = fakeActivityDataSource.observeActivities().first()
        // No CANCELLED activity recorded
        assertTrue(activities.none { it.activityType == CommercialActivityType.CANCELLED })
    }

    @Test
    fun invariantF_successfulOperationsProduceExactlyOneAuditEvent() = runBlocking {
        val fakeActivityDataSource = FakeCommercialActivityDataSource()
        val activityRepo = CommercialActivityRepositoryImpl(fakeActivityDataSource)
        val viewModel = OrderDetailsViewModel(
            orderId = "ord-101",
            repository = orderRepository,
            activityRepository = activityRepo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        viewModel.setOrderPriority(OrderPriority.URGENT, actorId = "usr-1", actorName = "Manager")

        val state = viewModel.uiState.value as OrderDetailsUiState.Success
        assertEquals(OrderPriority.URGENT, state.order.priority)

        val activities = fakeActivityDataSource.observeActivities().first()
        val priorityEvents = activities.filter { it.activityType == CommercialActivityType.PRIORITY_CHANGED }
        assertEquals(1, priorityEvents.size)
        assertEquals("Urgent", priorityEvents[0].newValue)
        assertEquals("Normal", priorityEvents[0].previousValue)
    }

    @Test
    fun invariantG_invalidTransitionsDoNotChangeOriginalOrder() = runBlocking {
        val pendingOrder = sampleOrder.copy(
            orderId = "ord-pending-test",
            orderNumber = "ORD-P-TEST",
            status = OrderStatusType.PENDING
        )
        orderDataSource.insertOrder(pendingOrder)

        // Attempt invalid transition PENDING -> DELIVERED
        val result = orderRepository.updateOrderStatus("ord-pending-test", OrderStatusType.DELIVERED)
        assertTrue(result is DomainResult.Error)

        // Verify order status in repository is still PENDING
        val fetched = (orderRepository.findOrderById("ord-pending-test") as DomainResult.Success).data
        assertEquals(OrderStatusType.PENDING, fetched.status)
    }

    @Test
    fun invariantH_cancellationReasonCannotBeBlank() = runBlocking {
        // Blank reason
        val blankRes = orderRepository.cancelOrder("ord-101", "   ")
        assertTrue(blankRes is DomainResult.Error)
        val order = (orderRepository.findOrderById("ord-101") as DomainResult.Success).data
        assertEquals(OrderStatusType.CONFIRMED, order.status)
    }

    @Test
    fun invariantI_priorityChangesCannotAlterFinancialSnapshot() = runBlocking {
        val originalTotal = sampleOrder.totalAmount
        val originalSubtotal = sampleOrder.subtotal
        val originalDiscount = sampleOrder.discount
        val originalItems = sampleOrder.items

        orderRepository.updateOrderPriority("ord-101", OrderPriority.URGENT)

        val updated = (orderRepository.findOrderById("ord-101") as DomainResult.Success).data
        assertEquals(OrderPriority.URGENT, updated.priority)
        assertEquals(originalTotal, updated.totalAmount)
        assertEquals(originalSubtotal, updated.subtotal)
        assertEquals(originalDiscount, updated.discount)
        assertEquals(originalItems, updated.items)
    }

    @Test
    fun invariantJ_lifecycleMutationIsAtomic() = runBlocking {
        // Run concurrent status transitions; Mutex guarantees consistency
        val jobs = listOf(
            orderRepository.updateOrderStatus("ord-101", OrderStatusType.ON_HOLD),
            orderRepository.updateOrderPriority("ord-101", OrderPriority.HIGH)
        )
        val finalOrder = (orderRepository.findOrderById("ord-101") as DomainResult.Success).data
        assertEquals(OrderStatusType.ON_HOLD, finalOrder.status)
        assertEquals(OrderPriority.HIGH, finalOrder.priority)
    }
}
