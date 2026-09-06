package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.*
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.ui.features.orders.order.wizard.OrderPlacementWizardViewModel
import com.sucharu.sucharupro.ui.features.orders.order.wizard.WizardStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class OrderPlacementWizardViewModelTest {

    private lateinit var mockRepository: FakeTestOrderRepository
    private lateinit var viewModel: OrderPlacementWizardViewModel

    @Before
    fun setUp() {
        mockRepository = FakeTestOrderRepository()
        viewModel = OrderPlacementWizardViewModel(mockRepository, Dispatchers.Unconfined)
    }

    @Test
    fun wizardStepNavigation_advancesThroughAllStepsCorrectly() {
        assertEquals(WizardStep.CUSTOMER, viewModel.uiState.value.currentStep)

        viewModel.nextStep() // CUSTOMER -> PRODUCT
        assertEquals(WizardStep.PRODUCT, viewModel.uiState.value.currentStep)

        viewModel.nextStep() // PRODUCT -> SPECIFICATIONS
        assertEquals(WizardStep.SPECIFICATIONS, viewModel.uiState.value.currentStep)

        viewModel.nextStep() // SPECIFICATIONS -> QUANTITY
        assertEquals(WizardStep.QUANTITY, viewModel.uiState.value.currentStep)

        viewModel.onQuantityChange(1000)
        viewModel.nextStep() // QUANTITY -> ESTIMATION
        assertEquals(WizardStep.ESTIMATION, viewModel.uiState.value.currentStep)

        viewModel.nextStep() // ESTIMATION -> REVIEW
        assertEquals(WizardStep.REVIEW, viewModel.uiState.value.currentStep)
    }

    @Test
    fun quantityValidation_rejectsZeroAndNegativeQuantities() {
        viewModel.onQuantityChange(0)
        assertNotNull(viewModel.uiState.value.quantityError)
        assertEquals("Quantity must be greater than zero.", viewModel.uiState.value.quantityError)

        viewModel.onQuantityChange(-50)
        assertNotNull(viewModel.uiState.value.quantityError)

        viewModel.onQuantityChange(500)
        assertNull(viewModel.uiState.value.quantityError)
    }

    @Test
    fun calculateCommercialEstimate_calculatesValidSubtotalAndTotal() {
        viewModel.onSpecificationsChange(
            description = "Test A4 Brochure",
            width = 210,
            height = 297,
            gsm = 150,
            paperType = "Art Paper",
            sides = "Double-Sided 4/4",
            colorMode = "CMYK",
            lamination = "Matte",
            binding = "Saddle Stitch"
        )
        viewModel.onQuantityChange(1000)
        viewModel.calculateCommercialEstimate()

        val state = viewModel.uiState.value
        assertTrue(state.totalAmount > BigDecimal.ZERO)
        assertEquals(1000, state.quantity)
    }

    @Test
    fun submitOrder_callsRepositoryCreateOrderAndSetsCreatedOrder() = runBlocking {
        viewModel.onCustomerInfoChange("CUST-WIZ-1", "Test Company", "test@company.com")
        viewModel.onQuantityChange(1000)
        viewModel.calculateCommercialEstimate()

        var callbackOrder: Order? = null
        viewModel.submitOrder { created ->
            callbackOrder = created
        }

        val state = viewModel.uiState.value
        assertNotNull(state.createdOrder)
        assertEquals("CUST-WIZ-1", state.createdOrder?.customerId)
        assertNotNull(callbackOrder)
    }

    private class FakeTestOrderRepository : OrderRepository {
        var createdOrder: Order? = null

        override fun getOrders(): Flow<List<Order>> = flowOf(emptyList())
        override fun getOrderById(orderId: String): Flow<Order?> = flowOf(null)
        override suspend fun findOrderById(orderId: String): DomainResult<Order> = DomainResult.Error(message = "Not found")
        override fun getOrdersForCustomer(customerId: String): Flow<List<Order>> = flowOf(emptyList())
        override fun getOrdersForQuotation(quotationId: String): Flow<List<Order>> = flowOf(emptyList())

        override suspend fun createOrder(order: Order): DomainResult<Order> {
            createdOrder = order
            return DomainResult.Success(order)
        }

        override suspend fun updateOrder(order: Order): DomainResult<Order> = DomainResult.Success(order)
        override suspend fun updateOrderStatus(orderId: String, status: OrderStatusType): DomainResult<Order> = DomainResult.Error(message = "N/A")
        override suspend fun updateOrderPriority(orderId: String, priority: OrderPriority): DomainResult<Order> = DomainResult.Error(message = "N/A")
        override suspend fun markReadyForJob(orderId: String): DomainResult<Order> = DomainResult.Error(message = "N/A")
        override suspend fun updateJobHandoffStatus(orderId: String, status: JobHandoffStatus): DomainResult<Order> = DomainResult.Error(message = "N/A")
        override suspend fun updateOrderNotes(orderId: String, notes: String?): DomainResult<Order> = DomainResult.Error(message = "N/A")
        override suspend fun cancelOrder(orderId: String, reason: String?): DomainResult<Order> = DomainResult.Error(message = "N/A")
        override suspend fun createOrderFromApprovedQuotation(
            orderId: String,
            orderNumber: String,
            quotationId: String,
            approvedRevisionId: String,
            priority: OrderPriority,
            confirmedBy: String?,
            timestamp: String
        ): DomainResult<Order> = DomainResult.Error(message = "N/A")
    }
}

