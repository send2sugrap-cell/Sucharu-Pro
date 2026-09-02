package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.QuotationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.ui.features.orders.quotation.details.QuotationDetailsUiState
import com.sucharu.sucharupro.ui.features.orders.quotation.details.QuotationDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuotationDetailsViewModelLifecycleTest {

    private lateinit var quotationDataSource: FakeQuotationDataSource
    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var quotationRepository: QuotationRepositoryImpl
    private lateinit var orderRepository: OrderRepositoryImpl

    private val sampleRev = QuotationRevision(
        revisionId = "rev-001",
        quotationId = "qt-101",
        revisionNumber = 1,
        items = listOf(
            QuotationItem(
                itemId = "i-1",
                description = "লিফলেট",
                quantity = 5000,
                unitPrice = Money(2.5)
            )
        ),
        createdAt = "2026-08-16T10:00:00Z"
    )

    private val sampleQuotation = Quotation(
        quotationId = "qt-101",
        quotationNumber = "QT-2026-000101",
        customerId = "cus-001",
        currentRevisionNumber = 1,
        revisions = listOf(sampleRev),
        status = QuotationStatusType.DRAFT,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        quotationDataSource = FakeQuotationDataSource(listOf(sampleQuotation))
        orderDataSource = FakeOrderDataSource(emptyList())
        quotationRepository = QuotationRepositoryImpl(quotationDataSource)
        orderRepository = OrderRepositoryImpl(orderDataSource, quotationDataSource)
    }

    @Test
    fun viewModel_sendQuotation_transitionsStateToSent() {
        val vm = QuotationDetailsViewModel(
            quotationId = "qt-101",
            repository = quotationRepository,
            orderRepository = orderRepository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        var successCalled = false
        vm.sendQuotation { successCalled = true }

        assertTrue(successCalled)
        val state = vm.uiState.value as QuotationDetailsUiState.Success
        assertEquals(QuotationStatusType.SENT, state.quotation.status)
        assertEquals("Quotation marked as SENT to customer.", state.actionMessage)
    }

    @Test
    fun viewModel_startNegotiation_transitionsStateToNegotiation() {
        val vm = QuotationDetailsViewModel(
            quotationId = "qt-101",
            repository = quotationRepository,
            orderRepository = orderRepository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        vm.sendQuotation()
        var negotiationCalled = false
        vm.startNegotiation { negotiationCalled = true }

        assertTrue(negotiationCalled)
        val state = vm.uiState.value as QuotationDetailsUiState.Success
        assertEquals(QuotationStatusType.NEGOTIATION, state.quotation.status)
    }

    @Test
    fun viewModel_approveQuotation_approvesAndUpdatesState() {
        val vm = QuotationDetailsViewModel(
            quotationId = "qt-101",
            repository = quotationRepository,
            orderRepository = orderRepository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        vm.sendQuotation()
        var approvedCalled = false
        vm.approveQuotation(
            revisionId = "rev-001",
            approvedBy = "Executive Lead",
            notes = "Client approved over call",
            onSuccess = { approvedCalled = true }
        )

        assertTrue(approvedCalled)
        val state = vm.uiState.value as QuotationDetailsUiState.Success
        assertEquals(QuotationStatusType.APPROVED, state.quotation.status)
        assertEquals("rev-001", state.quotation.approvedRevisionId)
        assertEquals("Executive Lead", state.quotation.approvedBy)
    }

    @Test
    fun viewModel_createRevision_addsRevisionAndSetsCurrent() {
        val vm = QuotationDetailsViewModel(
            quotationId = "qt-101",
            repository = quotationRepository,
            orderRepository = orderRepository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val rev2 = QuotationRevision(
            revisionId = "rev-002",
            quotationId = "qt-101",
            revisionNumber = 2,
            items = listOf(
                QuotationItem(
                    itemId = "i-1",
                    description = "লিফলেট (সংশোধিত)",
                    quantity = 10000,
                    unitPrice = Money(2.0)
                )
            ),
            revisionReason = "পরিমাণ বৃদ্ধি ও স্পেশাল ছাড়",
            createdAt = "2026-08-16T12:00:00Z"
        )

        var createdRevId: String? = null
        vm.createRevision(rev2) { createdRevId = it }

        assertEquals("rev-002", createdRevId)
        val state = vm.uiState.value as QuotationDetailsUiState.Success
        assertEquals(2, state.quotation.currentRevisionNumber)
        assertEquals(2, state.revisions.size)
        assertEquals("rev-002", state.selectedRevisionId)
    }

    @Test
    fun viewModel_convertQuotationToOrder_createsOrderAndTriggersCallback() {
        val vm = QuotationDetailsViewModel(
            quotationId = "qt-101",
            repository = quotationRepository,
            orderRepository = orderRepository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // Must approve first
        vm.sendQuotation()
        vm.approveQuotation("rev-001", "Sales Lead")

        var generatedOrderId: String? = null
        vm.convertQuotationToOrder(
            priority = OrderPriority.HIGH,
            confirmedBy = "Account Manager",
            onSuccess = { generatedOrderId = it }
        )

        assertNotNull(generatedOrderId)
        val state = vm.uiState.value as QuotationDetailsUiState.Success
        assertTrue(state.linkedOrders.isNotEmpty())
        assertEquals(generatedOrderId, state.linkedOrders.first().orderId)
        assertEquals("cus-001", state.linkedOrders.first().customerId)
    }

    @Test
    fun viewModel_rejectQuotation_updatesStatus() {
        val vm = QuotationDetailsViewModel(
            quotationId = "qt-101",
            repository = quotationRepository,
            orderRepository = orderRepository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        vm.sendQuotation()
        vm.rejectQuotation("Customer decided to postpone")

        val state = vm.uiState.value as QuotationDetailsUiState.Success
        assertEquals(QuotationStatusType.REJECTED, state.quotation.status)
    }

    @Test
    fun viewModel_cancelQuotation_updatesStatus() {
        val vm = QuotationDetailsViewModel(
            quotationId = "qt-101",
            repository = quotationRepository,
            orderRepository = orderRepository,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        vm.cancelQuotation("Entry duplicate")

        val state = vm.uiState.value as QuotationDetailsUiState.Success
        assertEquals(QuotationStatusType.CANCELLED, state.quotation.status)
    }
}
