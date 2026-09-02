package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.ui.features.orders.order.OrderListUiState
import com.sucharu.sucharupro.ui.features.orders.order.OrderListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrderListViewModelTest {

    private lateinit var dataSource: FakeOrderDataSource
    private lateinit var repository: OrderRepository
    private lateinit var viewModel: OrderListViewModel

    @Before
    fun setUp() {
        dataSource = FakeOrderDataSource()
        repository = OrderRepositoryImpl(dataSource)
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        viewModel = OrderListViewModel(repository, testScope)
    }

    @Test
    fun test01_initialLoadingAndSuccessfulSampleOrders() = runBlocking {
        val state = viewModel.uiState.value
        assertTrue(state is OrderListUiState.Success)
        val success = state as OrderListUiState.Success
        assertEquals(2, success.totalCount)
        assertEquals(2, success.visibleCount)
    }

    @Test
    fun test02_searchByOrderNumber() = runBlocking {
        viewModel.onSearchQueryChange("ORD-000001")
        val state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("ORD-000001", state.visibleOrders[0].orderNumber)
    }

    @Test
    fun test03_searchByCustomerId() = runBlocking {
        viewModel.onSearchQueryChange("cus-002")
        val state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("cus-002", state.visibleOrders[0].customerId)
    }

    @Test
    fun test04_searchByQuotationId() = runBlocking {
        viewModel.onSearchQueryChange("qt-001")
        val state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("qt-001", state.visibleOrders[0].quotationId)
    }

    @Test
    fun test05_statusFilteringWorks() = runBlocking {
        viewModel.onStatusFilterChange(OrderStatusType.CONFIRMED)
        var state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals(OrderStatusType.CONFIRMED, state.visibleOrders[0].status)

        viewModel.onStatusFilterChange(OrderStatusType.IN_PRODUCTION)
        state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals(OrderStatusType.IN_PRODUCTION, state.visibleOrders[0].status)
    }

    @Test
    fun test06_priorityFilteringWorks() = runBlocking {
        viewModel.onPriorityFilterChange(OrderPriority.HIGH)
        var state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals(OrderPriority.HIGH, state.visibleOrders[0].priority)

        viewModel.onPriorityFilterChange(OrderPriority.NORMAL)
        state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals(OrderPriority.NORMAL, state.visibleOrders[0].priority)
    }

    @Test
    fun test07_combinedStatusPriorityAndSearchFiltering() = runBlocking {
        viewModel.onStatusFilterChange(OrderStatusType.IN_PRODUCTION)
        viewModel.onPriorityFilterChange(OrderPriority.HIGH)
        viewModel.onSearchQueryChange("Brochure")
        val state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("ORD-000002", state.visibleOrders[0].orderNumber)
    }

    @Test
    fun test08_clearFiltersRestoresAllOrders() = runBlocking {
        viewModel.onStatusFilterChange(OrderStatusType.PENDING)
        viewModel.onPriorityFilterChange(OrderPriority.URGENT)
        viewModel.onSearchQueryChange("NonExistent")
        var state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(0, state.visibleCount)

        viewModel.clearFilters()
        state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(2, state.visibleCount)
        assertEquals("", state.searchQuery)
        assertNull(state.selectedStatus)
        assertNull(state.selectedPriority)
    }

    @Test
    fun test09_emptySearchResultDisplaysZeroVisibleCount() = runBlocking {
        viewModel.onSearchQueryChange("NonExistentOrderXYZ")
        val state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(0, state.visibleCount)
        assertEquals(2, state.totalCount)
    }

    @Test
    fun test10_emptyDataSourceShowsEmptyState() = runBlocking {
        val emptyDataSource = FakeOrderDataSource(initialOrders = emptyList())
        val emptyRepo = OrderRepositoryImpl(emptyDataSource)
        val vm = OrderListViewModel(emptyRepo, CoroutineScope(Dispatchers.Unconfined))

        val state = vm.uiState.value
        assertTrue(state is OrderListUiState.Empty)
    }

    @Test
    fun test11_repositoryErrorShowsErrorState() = runBlocking {
        val failingRepo = object : OrderRepository {
            override fun getOrders(): Flow<List<Order>> = flow {
                throw RuntimeException("SQL timeout error")
            }
            override fun getOrderById(orderId: String): Flow<Order?> = flow { emit(null) }
            override suspend fun findOrderById(orderId: String): DomainResult<Order> = DomainResult.Error(message = "Error")
            override fun getOrdersForCustomer(customerId: String): Flow<List<Order>> = flow { emit(emptyList()) }
            override fun getOrdersForQuotation(quotationId: String): Flow<List<Order>> = flow { emit(emptyList()) }
            override suspend fun createOrder(order: Order): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun updateOrder(order: Order): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun updateOrderStatus(orderId: String, status: OrderStatusType): DomainResult<Order> =
                DomainResult.Error(message = "Error")
            override suspend fun updateOrderPriority(orderId: String, priority: OrderPriority): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun markReadyForJob(orderId: String): DomainResult<Order> = DomainResult.Error(message = "Error")
            override suspend fun updateJobHandoffStatus(orderId: String, status: com.sucharu.sucharupro.domain.model.order.JobHandoffStatus): DomainResult<Order> = DomainResult.Error(message = "Error")
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

        val vm = OrderListViewModel(failingRepo, CoroutineScope(Dispatchers.Unconfined))
        val state = vm.uiState.value
        assertTrue(state is OrderListUiState.Error)
        assertEquals("SQL timeout error", (state as OrderListUiState.Error).errorMessage)
    }

    @Test
    fun test12_orderStatusRemainsDistinctCommercialLifecycle() = runBlocking {
        val state = viewModel.uiState.value as OrderListUiState.Success
        val order = state.visibleOrders[0]
        // Verify OrderStatusType is commercial status, completely distinct from production stages
        assertEquals(OrderStatusType.CONFIRMED, order.status)
        assertEquals("Confirmed", order.status.defaultLabel)
        assertNotEquals("Printing", order.status.defaultLabel)
    }

    @Test
    fun test13_banglaUnicodeSearchInOrders() = runBlocking {
        val banglaOrder = Order(
            orderId = "ord-bn-01",
            orderNumber = "ORD-বাংলা-০১",
            customerId = "cus-003",
            status = OrderStatusType.CONFIRMED,
            priority = OrderPriority.URGENT,
            items = listOf(
                OrderItem(
                    itemId = "item-bn-1",
                    description = "ব্যানার প্রিন্টিং",
                    specification = "১০x৩ ফিট, পিভিসি ব্যানার",
                    quantity = 5,
                    unit = "পিস",
                    unitPrice = 450.toMoney(),
                    discount = Money.ZERO
                )
            ),
            notes = "মেলা উপলক্ষে জরুরি প্রিন্ট",
            createdAt = "2026-08-15T10:00:00Z",
            updatedAt = "2026-08-15T10:00:00Z"
        )
        dataSource.insertOrder(banglaOrder)

        viewModel.onSearchQueryChange("ব্যানার")
        val state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("ORD-বাংলা-০১", state.visibleOrders[0].orderNumber)
    }

    @Test
    fun test14_handoffFilteringWorks() = runBlocking {
        val notReadyOrder = Order(
            orderId = "ord-nr-01",
            orderNumber = "ORD-000003",
            customerId = "cus-003",
            status = OrderStatusType.PENDING,
            priority = OrderPriority.NORMAL,
            items = listOf(
                OrderItem(
                    itemId = "item-01",
                    description = "Leaflet Printing",
                    quantity = 500,
                    unit = "Pcs",
                    unitPrice = 5.toMoney()
                )
            ),
            jobHandoffStatus = com.sucharu.sucharupro.domain.model.order.JobHandoffStatus.NOT_READY,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
        dataSource.insertOrder(notReadyOrder)

        viewModel.onHandoffFilterChange(com.sucharu.sucharupro.domain.model.order.JobHandoffStatus.NOT_READY)
        var state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals(com.sucharu.sucharupro.domain.model.order.JobHandoffStatus.NOT_READY, state.visibleOrders[0].jobHandoffStatus)

        viewModel.onHandoffFilterChange(com.sucharu.sucharupro.domain.model.order.JobHandoffStatus.READY_FOR_JOB)
        state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(2, state.visibleCount)
        assertTrue(state.visibleOrders.all { it.jobHandoffStatus == com.sucharu.sucharupro.domain.model.order.JobHandoffStatus.READY_FOR_JOB })
    }

    @Test
    fun test15_combinedStatusPriorityHandoffAndSearchFiltering() = runBlocking {
        viewModel.onStatusFilterChange(OrderStatusType.CONFIRMED)
        viewModel.onPriorityFilterChange(OrderPriority.NORMAL)
        viewModel.onHandoffFilterChange(com.sucharu.sucharupro.domain.model.order.JobHandoffStatus.READY_FOR_JOB)
        viewModel.onSearchQueryChange("Visiting Card")

        val state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("ORD-000001", state.visibleOrders[0].orderNumber)
    }

    @Test
    fun test16_clearFiltersResetsHandoffFilter() = runBlocking {
        viewModel.onHandoffFilterChange(com.sucharu.sucharupro.domain.model.order.JobHandoffStatus.READY_FOR_JOB)
        var state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(2, state.visibleCount)

        viewModel.clearFilters()
        state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(2, state.visibleCount)
        assertNull(state.selectedHandoff)
        assertNull(state.selectedStatus)
        assertNull(state.selectedPriority)
        assertEquals("", state.searchQuery)
    }

    @Test
    fun test17_searchByRecipientAndDeliveryAddress() = runBlocking {
        val deliveryOrder = Order(
            orderId = "ord-del-01",
            orderNumber = "ORD-000099",
            customerId = "cus-004",
            status = OrderStatusType.CONFIRMED,
            priority = OrderPriority.HIGH,
            items = listOf(
                OrderItem(
                    itemId = "item-01",
                    description = "Custom Box Packaging",
                    quantity = 100,
                    unit = "Boxes",
                    unitPrice = 50.toMoney()
                )
            ),
            deliveryRequirement = DeliveryRequirement(
                deliveryType = DeliveryType.COURIER,
                contactName = "Kazi Nazrul",
                contactPhone = "01700998877",
                address = "Dhanmondi, Dhaka",
                instructions = "Handle with care"
            ),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
        dataSource.insertOrder(deliveryOrder)

        // Search by recipient name
        viewModel.onSearchQueryChange("Nazrul")
        var state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("ORD-000099", state.visibleOrders[0].orderNumber)

        // Search by address
        viewModel.onSearchQueryChange("Dhanmondi")
        state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("ORD-000099", state.visibleOrders[0].orderNumber)

        // Search by phone
        viewModel.onSearchQueryChange("01700998877")
        state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("ORD-000099", state.visibleOrders[0].orderNumber)
    }

    @Test
    fun test18_whitespaceTolerantAndCaseInsensitiveSearch() = runBlocking {
        viewModel.onSearchQueryChange("   ord-000001   ")
        val state = viewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("ORD-000001", state.visibleOrders[0].orderNumber)
    }
}
