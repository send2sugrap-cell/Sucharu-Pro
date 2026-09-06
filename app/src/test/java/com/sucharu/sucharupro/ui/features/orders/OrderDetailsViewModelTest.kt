package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderDetailsViewModelTest {

    private fun sampleOrder(id: String = "ord-101") = Order(
        orderId = id,
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        quotationId = "quo-101",
        approvedQuotationRevisionId = "rev-002",
        status = OrderStatusType.CONFIRMED,
        priority = OrderPriority.URGENT,
        items = listOf(
            OrderItem(
                itemId = "ord-item-01",
                description = "ম্যাগাজিন প্রিন্টিং (Official Snapshot)",
                specification = "100 pages, 120 GSM Inner, 300 GSM Cover, Perfect Binding",
                quantity = 2500,
                unit = "copies",
                unitPrice = Money(80.0),
                discount = Money(5000.0)
            )
        ),
        discount = Money.ZERO,
        paymentTerms = PaymentTerms(PaymentTermType.PARTIAL_ADVANCE, advancePercentage = 50),
        deliveryRequirement = DeliveryRequirement(
            deliveryType = DeliveryType.BUSINESS_DELIVERY,
            requiredDate = "2026-09-01T00:00:00Z",
            address = "Motijheel C/A, Dhaka"
        ),
        jobHandoffStatus = JobHandoffStatus.READY_FOR_JOB,
        notes = "Bangla instruction: ডেলিভারি মতিঝিল অফিসে দিতে হবে।",
        confirmedBy = "Finance Team",
        confirmedAt = "2026-08-16T12:00:00Z",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T12:00:00Z"
    )

    private fun createFakeRepo(order: Order?): OrderRepository = object : OrderRepository {
        override fun getOrders(): Flow<List<Order>> = flowOf(listOfNotNull(order))
        override fun getOrderById(orderId: String): Flow<Order?> = flowOf(if (order?.orderId == orderId) order else null)
        override suspend fun findOrderById(orderId: String): DomainResult<Order> =
            if (order?.orderId == orderId) DomainResult.Success(order) else DomainResult.Error(message = "Not found")
        override fun getOrdersForCustomer(customerId: String): Flow<List<Order>> = flowOf(listOfNotNull(order))
        override fun getOrdersForQuotation(quotationId: String): Flow<List<Order>> = flowOf(listOfNotNull(order))
        override suspend fun createOrder(order: Order): DomainResult<Order> = DomainResult.Success(order)
        override suspend fun updateOrder(order: Order): DomainResult<Order> = DomainResult.Success(order)
        override suspend fun updateOrderStatus(orderId: String, status: OrderStatusType): DomainResult<Order> =
            order?.let { DomainResult.Success(it.copy(status = status)) } ?: DomainResult.Error(message = "Not found")
        override suspend fun updateOrderPriority(orderId: String, priority: OrderPriority): DomainResult<Order> =
            order?.let { DomainResult.Success(it.copy(priority = priority)) } ?: DomainResult.Error(message = "Not found")
        override suspend fun markReadyForJob(orderId: String): DomainResult<Order> =
            order?.let { DomainResult.Success(it.copy(jobHandoffStatus = JobHandoffStatus.READY_FOR_JOB)) } ?: DomainResult.Error(message = "Not found")
        override suspend fun updateJobHandoffStatus(orderId: String, status: JobHandoffStatus): DomainResult<Order> =
            order?.let { DomainResult.Success(it.copy(jobHandoffStatus = status)) } ?: DomainResult.Error(message = "Not found")
        override suspend fun updateOrderNotes(orderId: String, notes: String?): DomainResult<Order> =
            order?.let { DomainResult.Success(it.copy(notes = notes)) } ?: DomainResult.Error(message = "Not found")
        override suspend fun cancelOrder(orderId: String, reason: String?): DomainResult<Order> =
            order?.let { DomainResult.Success(it.copy(status = OrderStatusType.CANCELLED)) } ?: DomainResult.Error(message = "Not found")
        override suspend fun createOrderFromApprovedQuotation(
            orderId: String,
            orderNumber: String,
            quotationId: String,
            approvedRevisionId: String,
            priority: OrderPriority,
            confirmedBy: String?,
            timestamp: String
        ): DomainResult<Order> = order?.let { DomainResult.Success(it) } ?: DomainResult.Error(message = "Not found")
    }

    @Test
    fun loadOrder_successfulLoad_emitsSuccessWithSnapshotData() {
        val order = sampleOrder("ord-101")
        val repo = createFakeRepo(order)
        val vm = OrderDetailsViewModel(
            orderId = "ord-101",
            repository = repo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertTrue("State should be Success, got $state", state is OrderDetailsUiState.Success)
        val successState = state as OrderDetailsUiState.Success
        assertEquals("ord-101", successState.order.orderId)
        assertEquals("ORD-2026-0001", successState.order.orderNumber)
        assertEquals("cus-001", successState.order.customerId)
        assertEquals("quo-101", successState.order.quotationId)
        assertEquals("rev-002", successState.order.approvedQuotationRevisionId)
        assertEquals(OrderStatusType.CONFIRMED, successState.order.status)
        assertEquals(OrderPriority.URGENT, successState.order.priority)
        assertEquals(Money(195000.0), successState.order.totalAmount)
        assertEquals(JobHandoffStatus.READY_FOR_JOB, successState.order.jobHandoffStatus)
        assertEquals("Finance Team", successState.order.confirmedBy)
    }

    @Test
    fun snapshotIntegrity_verifiesOrderMaintainsIndependentSnapshot() {
        val order = sampleOrder("ord-101")
        val repo = createFakeRepo(order)
        val vm = OrderDetailsViewModel(
            orderId = "ord-101",
            repository = repo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value as OrderDetailsUiState.Success
        assertEquals(1, state.order.items.size)
        assertEquals("ম্যাগাজিন প্রিন্টিং (Official Snapshot)", state.order.items[0].description)
        assertEquals(Money(195000.0), state.order.items[0].lineSubtotal)
        assertEquals(Money(195000.0), state.order.subtotal)
    }

    @Test
    fun loadOrder_notFound_emitsNotFoundState() {
        val repo = createFakeRepo(null)
        val vm = OrderDetailsViewModel(
            orderId = "ord-missing",
            repository = repo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertTrue("State should be NotFound, got $state", state is OrderDetailsUiState.NotFound)
        assertEquals("ord-missing", (state as OrderDetailsUiState.NotFound).orderId)
    }

    @Test
    fun loadOrder_error_emitsErrorState() {
        val failingRepo = object : OrderRepository {
            override fun getOrders(): Flow<List<Order>> = flow { emit(emptyList()) }
            override fun getOrderById(orderId: String): Flow<Order?> = flow {
                throw RuntimeException("SQL timeout")
            }
            override suspend fun findOrderById(orderId: String): DomainResult<Order> = DomainResult.Error(message = "Error")
            override fun getOrdersForCustomer(customerId: String): Flow<List<Order>> = flow { emit(emptyList()) }
            override fun getOrdersForQuotation(quotationId: String): Flow<List<Order>> = flow { emit(emptyList()) }
            override suspend fun createOrder(order: Order): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun updateOrder(order: Order): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun updateOrderStatus(orderId: String, status: OrderStatusType): DomainResult<Order> =
                DomainResult.Error(message = "Error")
            override suspend fun updateOrderPriority(orderId: String, priority: OrderPriority): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun markReadyForJob(orderId: String): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun updateJobHandoffStatus(orderId: String, status: JobHandoffStatus): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun updateOrderNotes(orderId: String, notes: String?): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun cancelOrder(orderId: String, reason: String?): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun createOrderFromApprovedQuotation(
                orderId: String,
                orderNumber: String,
                quotationId: String,
                approvedRevisionId: String,
                priority: OrderPriority,
                confirmedBy: String?,
                timestamp: String
            ): DomainResult<Order> = DomainResult.Error(message = "Error")
        }

        val vm = OrderDetailsViewModel(
            orderId = "ord-101",
            repository = failingRepo,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = vm.uiState.value
        assertTrue("State should be Error, got $state", state is OrderDetailsUiState.Error)
        assertEquals("SQL timeout", (state as OrderDetailsUiState.Error).errorMessage)
    }

    @Test
    fun createProductionJob_successfulCreation_updatesProductionJobInState() {
        val order = sampleOrder("ord-101")
        val repo = createFakeRepo(order)
        val sampleExecution = com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution(
            executionJobId = "job-999",
            tenantId = "tenant-test",
            projectId = "cus-001",
            orderId = "ord-101",
            orderNumber = "ORD-2026-0001",
            orderItemId = "ord-item-01",
            customerId = "cus-001",
            quotationId = "quo-101",
            quotationVersionNumber = 1,
            commercialCommitmentId = null,
            planningId = "plan-101",
            planningVersion = 1,
            title = "Commercial Job for ORD-2026-0001",
            priority = com.sucharu.sucharupro.domain.model.order.OrderPriority.URGENT,
            status = com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecutionStatus.IN_PROGRESS,
            specification = com.sucharu.sucharupro.domain.model.productionplanning.ProductionJobSpecification(
                specId = "spec-101",
                jobTitle = "Commercial Job",
                productType = "PRINT_COMMERCIAL",
                orderedQuantity = 2500L,
                plannedQuantity = 2600L,
                finishedWidthMm = java.math.BigDecimal("210"),
                finishedHeightMm = java.math.BigDecimal("297"),
                substrateType = "ART_PAPER",
                substrateGsm = 150,
                parentSheetWidthMm = java.math.BigDecimal("640"),
                parentSheetHeightMm = java.math.BigDecimal("900"),
                pressSheetWidthMm = java.math.BigDecimal("640"),
                pressSheetHeightMm = java.math.BigDecimal("450"),
                printingMethod = "OFFSET",
                colorsFront = 4,
                colorsBack = 4,
                impositionUps = 1,
                specFingerprint = "fp-101"
            ),
            plannedQuantity = java.math.BigDecimal("2500"),
            jobFingerprint = "fp-101",
            integrityHash = "hash-101",
            createdAt = System.currentTimeMillis(),
            createdBy = "Finance Team",
            updatedAt = System.currentTimeMillis()
        )

        val integrationService = object : com.sucharu.sucharupro.domain.service.production.OrderProductionIntegrationService {
            override suspend fun evaluateOrderEligibility(
                tenantId: String,
                orderId: String
            ): DomainResult<List<com.sucharu.sucharupro.domain.model.productionexecution.ProductionExecutionDiagnostic>> = DomainResult.Success(emptyList())

            override suspend fun createProductionJobFromOrder(
                tenantId: String,
                orderId: String,
                requestedBy: String,
                idempotencyKey: String?
            ): DomainResult<com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution> {
                return DomainResult.Success(sampleExecution)
            }

            override suspend fun getProductionJobForOrder(
                tenantId: String,
                orderId: String
            ): DomainResult<com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution?> {
                return DomainResult.Success(null)
            }
        }

        val vm = OrderDetailsViewModel(
            orderId = "ord-101",
            repository = repo,
            productionService = integrationService,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        var state = vm.uiState.value as OrderDetailsUiState.Success
        assertEquals(null, state.productionJob)

        vm.createProductionJob()

        state = vm.uiState.value as OrderDetailsUiState.Success
        assertNotNull(state.productionJob)
        assertEquals("job-999", state.productionJob?.jobId)
        assertEquals("JOB-ORD-2026-0001", state.productionJob?.jobNumber)
        assertEquals(false, state.isActionInProgress)
    }

    @Test
    fun createProductionJob_error_setsErrorMessageInState() {
        val order = sampleOrder("ord-101")
        val repo = createFakeRepo(order)
        val failingService = object : com.sucharu.sucharupro.domain.service.production.OrderProductionIntegrationService {
            override suspend fun evaluateOrderEligibility(
                tenantId: String,
                orderId: String
            ): DomainResult<List<com.sucharu.sucharupro.domain.model.productionexecution.ProductionExecutionDiagnostic>> = DomainResult.Success(emptyList())

            override suspend fun createProductionJobFromOrder(
                tenantId: String,
                orderId: String,
                requestedBy: String,
                idempotencyKey: String?
            ): DomainResult<com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution> {
                return DomainResult.Error(message = "Order must be confirmed before production handoff")
            }

            override suspend fun getProductionJobForOrder(
                tenantId: String,
                orderId: String
            ): DomainResult<com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution?> {
                return DomainResult.Success(null)
            }
        }

        val vm = OrderDetailsViewModel(
            orderId = "ord-101",
            repository = repo,
            productionService = failingService,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )

        vm.createProductionJob()

        val state = vm.uiState.value as OrderDetailsUiState.Success
        assertEquals("Order must be confirmed before production handoff", state.actionError)
        assertEquals(false, state.isActionInProgress)
    }
}
