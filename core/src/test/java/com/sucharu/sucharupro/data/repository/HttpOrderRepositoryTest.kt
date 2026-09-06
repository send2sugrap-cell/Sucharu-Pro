package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.api.client.BackendApiClient
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class HttpOrderRepositoryTest {

    private lateinit var stubClient: StubBackendApiClient
    private lateinit var repository: HttpOrderRepository

    @Before
    fun setUp() {
        stubClient = StubBackendApiClient()
        repository = HttpOrderRepository(stubClient)
    }

    @Test
    fun getOrders_fetchesFromClientAndMapsToDomain() = runBlocking {
        val orders = repository.getOrders().first()
        assertEquals(1, orders.size)
        assertEquals("ORD-101", orders[0].orderId)
        assertEquals("SO-100101", orders[0].orderNumber)
        assertEquals(BigDecimal("15500.00"), orders[0].totalAmount.amount)
    }

    @Test
    fun findOrderById_fetchesOrderDetailAndMapsItems() = runBlocking {
        val result = repository.findOrderById("ORD-101")
        assertTrue(result is DomainResult.Success)
        val order = (result as DomainResult.Success).data
        assertEquals("ORD-101", order.orderId)
        assertEquals("CUST-999", order.customerId)
        assertEquals(1, order.items.size)
        assertEquals("A4 Flyers - 150 GSM", order.items[0].description)
        assertEquals(1000, order.items[0].quantity)
        assertEquals(BigDecimal("15.50"), order.items[0].unitPrice.amount)
    }

    @Test
    fun createOrder_sendsRequestToClientAndReturnsCreatedOrder() = runBlocking {
        val orderToCreate = Order(
            orderId = "TEMP-1",
            orderNumber = "SO-TEMP",
            customerId = "CUST-999",
            status = OrderStatusType.CONFIRMED,
            items = listOf(
                OrderItem(
                    itemId = "ITEM-1",
                    description = "Custom Booklet Print",
                    quantity = 500,
                    unitPrice = Money(BigDecimal("25.00"))
                )
            ),
            notes = "Rush delivery required.",
            createdAt = "2026-09-05T00:00:00Z",
            updatedAt = "2026-09-05T00:00:00Z"
        )

        val result = repository.createOrder(orderToCreate)
        assertTrue(result is DomainResult.Success)
        val created = (result as DomainResult.Success).data
        assertEquals("ORD-NEW-777", created.orderId)
        assertEquals("CUST-999", created.customerId)
        assertEquals("Rush delivery required.", created.notes)
    }

    private class StubBackendApiClient : BackendApiClient {
        override suspend fun getCustomerOrders(): ApiResult<List<CustomerOrderSummaryDto>> {
            return ApiResult.Success(
                listOf(
                    CustomerOrderSummaryDto(
                        orderId = "ORD-101",
                        orderNumber = "SO-100101",
                        status = "CONFIRMED",
                        totalAmount = BigDecimal("15500.00"),
                        createdAt = System.currentTimeMillis()
                    )
                )
            )
        }

        override suspend fun getCustomerOrderDetail(orderId: String): ApiResult<CustomerOrderDetailDto> {
            return ApiResult.Success(
                CustomerOrderDetailDto(
                    orderId = orderId,
                    orderNumber = "SO-100101",
                    customerId = "CUST-999",
                    status = "CONFIRMED",
                    items = listOf(
                        OrderItemDto(
                            itemId = "ITEM-101-1",
                            description = "A4 Flyers - 150 GSM",
                            quantity = 1000,
                            unitPrice = BigDecimal("15.50"),
                            totalPrice = BigDecimal("15500.00")
                        )
                    ),
                    subtotal = BigDecimal("15500.00"),
                    discount = BigDecimal.ZERO,
                    totalAmount = BigDecimal("15500.00"),
                    notes = "Handle with care.",
                    version = 1L,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        override suspend fun createCustomerOrder(
            request: CreateOrderRequestDto,
            idempotencyKey: String?
        ): ApiResult<CustomerOrderDetailDto> {
            return ApiResult.Success(
                CustomerOrderDetailDto(
                    orderId = "ORD-NEW-777",
                    orderNumber = "SO-777001",
                    customerId = "CUST-999",
                    status = "CONFIRMED",
                    items = request.items.mapIndexed { idx, it ->
                        OrderItemDto(
                            itemId = "ITEM-NEW-$idx",
                            description = it.description,
                            quantity = it.quantity,
                            unitPrice = it.unitPrice,
                            totalPrice = it.unitPrice.multiply(BigDecimal(it.quantity))
                        )
                    },
                    subtotal = BigDecimal("12500.00"),
                    discount = BigDecimal.ZERO,
                    totalAmount = BigDecimal("12500.00"),
                    notes = request.notes,
                    version = 1L,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        override suspend fun listCustomers(): ApiResult<List<CustomerDto>> = TODO()
        override suspend fun getCustomerById(customerId: String): ApiResult<CustomerDto> = TODO()
        override suspend fun createCustomer(request: CreateCustomerRequestDto): ApiResult<CustomerDto> = TODO()
        override suspend fun updateCustomer(customerId: String, request: UpdateCustomerRequestDto): ApiResult<CustomerDto> = TODO()
        override suspend fun setCustomerStatus(customerId: String, request: SetCustomerStatusRequestDto): ApiResult<CustomerDto> = TODO()

        override suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto> = TODO()
        override suspend fun loginWithFirebase(request: FirebaseAuthRequestDto): ApiResult<AuthResponseDto> = TODO()
        override suspend fun register(request: RegisterRequestDto): ApiResult<RegisterResponseDto> = TODO()
        override suspend fun updateProfile(request: UpdateUserProfileRequestDto): ApiResult<Map<String, Any>> = TODO()
        override suspend fun refreshToken(refreshToken: String): ApiResult<AuthResponseDto> = TODO()
        override suspend fun logout(allDevices: Boolean): ApiResult<Map<String, String>> = TODO()
        override suspend fun logoutAll(): ApiResult<Map<String, String>> = TODO()
        override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequestDto): ApiResult<PasswordRecoveryResponseDto> = TODO()
        override suspend fun confirmPasswordReset(request: PasswordRecoveryConfirmDto): ApiResult<Map<String, Any>> = TODO()
        override suspend fun requestVerificationToken(request: RequestVerificationRequestDto): ApiResult<Map<String, Any>> = TODO()
        override suspend fun confirmVerificationToken(request: ConfirmVerificationRequestDto): ApiResult<Map<String, Any>> = TODO()
        override suspend fun resendVerificationToken(identifier: String): ApiResult<Map<String, Any>> = TODO()
        override suspend fun getPublicCompanyInfo(): ApiResult<CompanyInfoDto> = TODO()
        override suspend fun getPublicProducts(): ApiResult<List<PublicProductDto>> = TODO()
        override suspend fun getMyProfile(): ApiResult<AuthenticatedPrincipal> = TODO()
        override suspend fun getCustomerProfile(): ApiResult<CustomerProfileDto> = TODO()
        override suspend fun getAffiliateProfile(): ApiResult<LegacyAffiliateProfileDto> = TODO()
        override suspend fun getAffiliateCommission(): ApiResult<AffiliateCommissionDto> = TODO()
        override suspend fun checkHealthLive(): ApiResult<Map<String, String>> = TODO()
        override suspend fun checkHealthReady(): ApiResult<DatabaseHealthStatus> = TODO()
        override suspend fun calculatePrintingCost(request: com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationRequestDto): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto> = TODO()
        override suspend fun validatePrintingCalculation(request: com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationRequestDto): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.ValidationResponseDto> = TODO()
        override suspend fun getPrintingCalculationById(calculationId: String): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto> = TODO()
        override suspend fun getPrintingCalculationBreakdown(calculationId: String): ApiResult<List<com.sucharu.sucharupro.data.api.model.printingcalculator.CalculationBreakdownItemDto>> = TODO()
        override suspend fun getPrintingCalculatorHandoffContract(calculationId: String): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.Module17Step01PrintingCalculatorHandoffContractDto> = TODO()
        override suspend fun listPrintingCalculations(): ApiResult<List<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto>> = TODO()
    }
}
