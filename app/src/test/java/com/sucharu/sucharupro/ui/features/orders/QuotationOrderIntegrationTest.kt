package com.sucharu.sucharupro.ui.features.orders

import com.sucharu.sucharupro.data.datasource.FakeInquiryDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.data.repository.InquiryRepositoryImpl
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.QuotationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.ui.features.orders.inquiry.InquiryListUiState
import com.sucharu.sucharupro.ui.features.orders.inquiry.InquiryListViewModel
import com.sucharu.sucharupro.ui.features.orders.order.OrderListUiState
import com.sucharu.sucharupro.ui.features.orders.order.OrderListViewModel
import com.sucharu.sucharupro.ui.features.orders.quotation.QuotationListUiState
import com.sucharu.sucharupro.ui.features.orders.quotation.QuotationListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuotationOrderIntegrationTest {

    private lateinit var inquiryDataSource: FakeInquiryDataSource
    private lateinit var inquiryRepository: InquiryRepositoryImpl
    private lateinit var inquiryViewModel: InquiryListViewModel

    private lateinit var quotationDataSource: FakeQuotationDataSource
    private lateinit var quotationRepository: QuotationRepositoryImpl
    private lateinit var quotationViewModel: QuotationListViewModel

    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var orderRepository: OrderRepositoryImpl
    private lateinit var orderViewModel: OrderListViewModel

    @Before
    fun setUp() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        inquiryDataSource = FakeInquiryDataSource()
        inquiryRepository = InquiryRepositoryImpl(inquiryDataSource)
        inquiryViewModel = InquiryListViewModel(inquiryRepository, testScope)

        quotationDataSource = FakeQuotationDataSource()
        quotationRepository = QuotationRepositoryImpl(quotationDataSource)
        quotationViewModel = QuotationListViewModel(quotationRepository, testScope)

        orderDataSource = FakeOrderDataSource()
        orderRepository = OrderRepositoryImpl(orderDataSource, quotationDataSource)
        orderViewModel = OrderListViewModel(orderRepository, testScope)
    }

    @Test
    fun test01_inquiryReactiveInsertionReflectsInUiState() = runBlocking {
        var state = inquiryViewModel.uiState.value as InquiryListUiState.Success
        val initialCount = state.totalCount

        val newInquiry = Inquiry(
            inquiryId = "inq-new-001",
            inquiryNumber = "INQ-999999",
            customerId = "cus-005",
            status = InquiryStatusType.NEW,
            source = InquirySource.PHONE_CALL,
            items = listOf(
                InquiryRequirement(
                    itemId = "item-01",
                    productName = "Product Catalog 2026",
                    description = "A4 32 Pages, 150 GSM Art Paper",
                    quantity = 500,
                    unit = "Copies"
                )
            ),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
        inquiryRepository.createInquiry(newInquiry)

        state = inquiryViewModel.uiState.value as InquiryListUiState.Success
        assertEquals(initialCount + 1, state.totalCount)
        assertTrue(state.allInquiries.any { it.inquiryNumber == "INQ-999999" })
    }

    @Test
    fun test02_quotationRevisionAndApprovalReflectsInUiState() = runBlocking {
        var state = quotationViewModel.uiState.value as QuotationListUiState.Success
        assertEquals(1, state.totalCount)

        val newRev = QuotationRevision(
            revisionId = "rev-001-v3",
            quotationId = "qt-001",
            revisionNumber = 3,
            items = listOf(
                QuotationItem(
                    itemId = "item-01",
                    description = "Visiting Card Special Edition",
                    specification = "350 GSM Velvet Matte",
                    quantity = 2000,
                    unit = "Pcs",
                    unitPrice = 1.50.toMoney(),
                    discount = Money.ZERO
                )
            ),
            discount = Money.ZERO,
            paymentTerms = PaymentTerms.DEFAULT,
            deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
            revisionReason = "Upgraded to 350 GSM velvet matte",
            createdAt = "2026-08-16T11:00:00Z"
        )
        quotationRepository.createQuotationRevision("qt-001", newRev)

        state = quotationViewModel.uiState.value as QuotationListUiState.Success
        val updatedQ = state.allQuotations.find { it.quotationId == "qt-001" }!!
        assertEquals(3, updatedQ.currentRevisionNumber)
        assertEquals(3, updatedQ.revisionCount)
        assertEquals(3000.toMoney(), updatedQ.totalAmount)
    }

    @Test
    fun test03_orderConversionAndLifecycleReflectsInUiState() = runBlocking {
        var state = orderViewModel.uiState.value as OrderListUiState.Success
        val initialCount = state.totalCount

        val newOrder = Order(
            orderId = "ord-new-001",
            orderNumber = "ORD-000099",
            customerId = "cus-004",
            status = OrderStatusType.CONFIRMED,
            priority = OrderPriority.URGENT,
            items = listOf(
                OrderItem(
                    itemId = "item-01",
                    description = "Custom Packaging Box",
                    specification = "3 ply corrugated, 4 color offset",
                    quantity = 5000,
                    unit = "Pcs",
                    unitPrice = 18.0.toMoney(),
                    discount = Money.ZERO
                )
            ),
            createdAt = "2026-08-16T12:00:00Z",
            updatedAt = "2026-08-16T12:00:00Z"
        )
        orderRepository.createOrder(newOrder)

        state = orderViewModel.uiState.value as OrderListUiState.Success
        assertEquals(initialCount + 1, state.totalCount)
        assertTrue(state.allOrders.any { it.orderNumber == "ORD-000099" })
    }

    @Test
    fun test04_orderCancellationReflectsInFilteredUiState() = runBlocking {
        orderRepository.cancelOrder("ord-001", "Customer requested cancellation")

        orderViewModel.onStatusFilterChange(OrderStatusType.CANCELLED)
        val state = orderViewModel.uiState.value as OrderListUiState.Success
        assertEquals(1, state.visibleCount)
        assertEquals("ORD-000001", state.visibleOrders[0].orderNumber)
        assertEquals(OrderStatusType.CANCELLED, state.visibleOrders[0].status)
    }
}
