package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeCommercialActivityDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderJobHandoffDataSource
import com.sucharu.sucharupro.data.repository.CommercialActivityRepositoryImpl
import com.sucharu.sucharupro.data.repository.OrderJobHandoffRepositoryImpl
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
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

/**
 * End-to-end integration tests for Order → Job Handoff Foundation (Module 03 Step 11).
 */
class OrderJobHandoffIntegrationTest {

    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var orderRepository: OrderRepositoryImpl
    private lateinit var handoffDataSource: FakeOrderJobHandoffDataSource
    private lateinit var handoffRepository: OrderJobHandoffRepositoryImpl
    private lateinit var activityDataSource: FakeCommercialActivityDataSource
    private lateinit var activityRepository: CommercialActivityRepositoryImpl

    private val sampleItem = OrderItem(
        itemId = "item-01",
        description = "কাস্টম ডায়েরি প্রিন্টিং",
        specification = "হার্ড কভার, ফয়েল প্রিন্ট",
        quantity = 300,
        unit = "Pcs",
        unitPrice = 250.toMoney()
    )

    private val confirmedOrder = Order(
        orderId = "ord-integ-01",
        orderNumber = "ORD-2026-INT01",
        customerId = "cus-001",
        quotationId = "qt-001",
        approvedQuotationRevisionId = "rev-001",
        status = OrderStatusType.CONFIRMED,
        priority = OrderPriority.URGENT,
        items = listOf(sampleItem),
        discount = Money.ZERO,
        paymentTerms = PaymentTerms.DEFAULT,
        deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
        jobHandoffStatus = JobHandoffStatus.READY_FOR_JOB,
        notes = "Original order notes",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        orderDataSource = FakeOrderDataSource(listOf(confirmedOrder))
        orderRepository = OrderRepositoryImpl(orderDataSource)
        handoffDataSource = FakeOrderJobHandoffDataSource()
        handoffRepository = OrderJobHandoffRepositoryImpl(handoffDataSource)
        activityDataSource = FakeCommercialActivityDataSource()
        activityRepository = CommercialActivityRepositoryImpl(activityDataSource)
    }

    @Test
    fun endToEnd_handoffLifecycleFlow_andViewModelIntegration() = runBlocking {
        val viewModel = OrderDetailsViewModel(
            orderId = "ord-integ-01",
            repository = orderRepository,
            activityRepository = activityRepository,
            handoffRepository = handoffRepository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // 1. Initial state: confirmed order, no handoff record yet
        var state = viewModel.uiState.value as OrderDetailsUiState.Success
        assertEquals("ORD-2026-INT01", state.order.orderNumber)
        assertEquals(null, state.handoff)

        // 2. Initiate Handoff snapshot
        viewModel.createHandoff(notes = "গ্রাহকের বিশেষ ডেলিভারি নির্দেশনা", actorName = "Sales Desk")

        state = viewModel.uiState.value as OrderDetailsUiState.Success
        assertNotNull(state.handoff)
        val createdHandoff = state.handoff!!
        assertEquals(OrderJobHandoffStatus.READY_FOR_HANDOFF, createdHandoff.handoffStatus)
        assertEquals("৳ 75,000", createdHandoff.commercialTotal.formatted())
        assertEquals(300, createdHandoff.totalQuantity)
        assertEquals("গ্রাহকের বিশেষ ডেলিভারি নির্দেশনা", createdHandoff.notes)

        // 3. Confirm handoff to production
        viewModel.confirmHandoff(createdHandoff.handoffId, actorName = "Production Head")

        state = viewModel.uiState.value as OrderDetailsUiState.Success
        val confirmedHandoff = state.handoff!!
        assertEquals(OrderJobHandoffStatus.HANDED_OFF, confirmedHandoff.handoffStatus)
        assertEquals("Production Head", confirmedHandoff.confirmedBy)

        // 4. Mark ready for production intake
        viewModel.markHandoffReadyForProduction(confirmedHandoff.handoffId, actorName = "Production Head")

        state = viewModel.uiState.value as OrderDetailsUiState.Success
        val readyProdHandoff = state.handoff!!
        assertEquals(OrderJobHandoffStatus.READY_FOR_PRODUCTION, readyProdHandoff.handoffStatus)

        // 5. Verify audit history records corresponding events
        val activities = activityDataSource.observeActivities().first()
        assertTrue(activities.any { it.activityType == CommercialActivityType.HANDOFF_READY })
        assertTrue(activities.any { it.newStatus == "Handed Off" })
        assertTrue(activities.any { it.newStatus == "Ready for Production" })
    }

    @Test
    fun snapshotIsolation_orderMutationDoesNotAffectExistingHandoff() = runBlocking {
        // Create handoff
        val handoffResult = handoffRepository.createHandoff(
            handoffId = "hnd-iso-01",
            order = confirmedOrder,
            timestamp = "2026-08-16T10:00:00Z"
        )
        assertTrue(handoffResult is DomainResult.Success)

        // Later update operational order priority or remarks on Order
        orderRepository.updateOrderPriority("ord-integ-01", OrderPriority.NORMAL)
        orderRepository.updateOrderNotes("ord-integ-01", "Updated remarks after handoff")

        // Handoff snapshot remains completely unmodified
        val fetchedHandoff = (handoffRepository.findHandoffById("hnd-iso-01") as DomainResult.Success).data
        assertEquals(OrderPriority.URGENT, fetchedHandoff.priority)
        assertEquals("Original order notes", fetchedHandoff.notes)
        assertEquals("৳ 75,000", fetchedHandoff.commercialTotal.formatted())
        assertEquals(1, fetchedHandoff.itemCount)
    }
}
