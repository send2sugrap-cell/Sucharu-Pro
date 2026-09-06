package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.api.client.BackendApiClient
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpCustomerRepositoryTest {

    private lateinit var stubClient: StubBackendApiClient
    private lateinit var repository: HttpCustomerRepository

    @Before
    fun setUp() {
        stubClient = StubBackendApiClient()
        repository = HttpCustomerRepository(stubClient)
    }

    @Test
    fun addCustomer_callsRegisterAndReturnsSuccess() = runBlocking {
        val newCustomer = Customer(
            customerId = "temp-id",
            customerCode = "CUS-000000",
            displayName = "New Corporate Client",
            customerType = CustomerType.BUSINESS,
            status = CustomerStatusType.ACTIVE,
            primaryPhone = "+8801711223344",
            email = "client@sucharu.com",
            createdAt = "2026-09-05T00:00:00Z",
            updatedAt = "2026-09-05T00:00:00Z"
        )

        val result = repository.addCustomer(newCustomer)
        assertTrue(result is DomainResult.Success)
        val created = (result as DomainResult.Success).data
        assertEquals("user-created-999", created.customerId)
        assertEquals("New Corporate Client", created.displayName)
    }

    @Test
    fun updateCustomer_callsUpdateProfileAndReturnsSuccess() = runBlocking {
        val customerToUpdate = Customer(
            customerId = "user-created-999",
            customerCode = "CUS-USER999",
            displayName = "Updated Corporate Client Name",
            customerType = CustomerType.BUSINESS,
            status = CustomerStatusType.ACTIVE,
            primaryPhone = "+8801711223344",
            email = "client@sucharu.com",
            createdAt = "2026-09-05T00:00:00Z",
            updatedAt = "2026-09-05T00:00:00Z"
        )

        val result = repository.updateCustomer(customerToUpdate)
        assertTrue(result is DomainResult.Success)
        val updated = (result as DomainResult.Success).data
        assertEquals("Updated Corporate Client Name", updated.displayName)
    }

    @Test
    fun getCustomers_emitsListOfCustomers() = runBlocking {
        val list = repository.getCustomers().first()
        assertEquals(1, list.size)
        assertEquals("user-created-999", list[0].customerId)
        assertEquals("New Corporate Client", list[0].displayName)
    }

    @Test
    fun getCustomerById_emitsCustomer() = runBlocking {
        val customer = repository.getCustomerById("user-created-999").first()
        assertTrue(customer != null)
        assertEquals("user-created-999", customer?.customerId)
        assertEquals("New Corporate Client", customer?.displayName)
    }

    @Test
    fun findCustomerById_returnsSuccess() = runBlocking {
        val result = repository.findCustomerById("user-created-999")
        assertTrue(result is DomainResult.Success)
        val customer = (result as DomainResult.Success).data
        assertEquals("user-created-999", customer.customerId)
    }

    @Test
    fun setCustomerStatus_deactivate_reactivate_archive() = runBlocking {
        val deactivateRes = repository.deactivateCustomer("user-created-999")
        assertTrue(deactivateRes is DomainResult.Success)
        assertEquals(CustomerStatusType.INACTIVE, (deactivateRes as DomainResult.Success).data.status)

        val reactivateRes = repository.reactivateCustomer("user-created-999")
        assertTrue(reactivateRes is DomainResult.Success)
        assertEquals(CustomerStatusType.ACTIVE, (reactivateRes as DomainResult.Success).data.status)

        val archiveRes = repository.archiveCustomer("user-created-999")
        assertTrue(archiveRes is DomainResult.Success)
        assertEquals(CustomerStatusType.ARCHIVED, (archiveRes as DomainResult.Success).data.status)
    }

    private class StubBackendApiClient : BackendApiClient {
        override suspend fun loginWithFirebase(request: FirebaseAuthRequestDto): ApiResult<AuthResponseDto> = TODO()
        override suspend fun register(request: RegisterRequestDto): ApiResult<RegisterResponseDto> {
            return ApiResult.Success(
                RegisterResponseDto(
                    userId = "user-created-999",
                    username = request.displayName,
                    email = request.email,
                    phone = request.phone,
                    accountStatus = AccountStatus.ACTIVE,
                    role = UserRole.CUSTOMER,
                    verificationRequired = false,
                    message = "Registration successful"
                )
            )
        }

        override suspend fun updateProfile(request: UpdateUserProfileRequestDto): ApiResult<Map<String, Any>> {
            return ApiResult.Success(mapOf("message" to "Profile updated"))
        }

        override suspend fun listCustomers(): ApiResult<List<CustomerDto>> {
            return ApiResult.Success(
                listOf(
                    CustomerDto(
                        customerId = "user-created-999",
                        customerCode = "CUS-USER999",
                        displayName = "New Corporate Client",
                        customerType = "BUSINESS",
                        status = "ACTIVE",
                        primaryPhone = "+8801711223344",
                        email = "client@sucharu.com"
                    )
                )
            )
        }

        override suspend fun getCustomerById(customerId: String): ApiResult<CustomerDto> {
            return ApiResult.Success(
                CustomerDto(
                    customerId = customerId,
                    customerCode = "CUS-999",
                    displayName = "New Corporate Client",
                    customerType = "BUSINESS",
                    status = "ACTIVE",
                    primaryPhone = "+8801711223344",
                    email = "client@sucharu.com"
                )
            )
        }

        override suspend fun createCustomer(request: CreateCustomerRequestDto): ApiResult<CustomerDto> {
            return ApiResult.Success(
                CustomerDto(
                    customerId = "user-created-999",
                    customerCode = "CUS-USER999",
                    displayName = request.displayName,
                    customerType = request.customerType,
                    status = "ACTIVE",
                    primaryPhone = request.primaryPhone,
                    email = request.email
                )
            )
        }

        override suspend fun updateCustomer(customerId: String, request: UpdateCustomerRequestDto): ApiResult<CustomerDto> {
            return ApiResult.Success(
                CustomerDto(
                    customerId = customerId,
                    customerCode = "CUS-USER999",
                    displayName = request.displayName ?: "Updated Corporate Client Name",
                    customerType = request.customerType ?: "BUSINESS",
                    status = request.status ?: "ACTIVE",
                    primaryPhone = request.primaryPhone,
                    email = request.email
                )
            )
        }

        override suspend fun setCustomerStatus(customerId: String, request: SetCustomerStatusRequestDto): ApiResult<CustomerDto> {
            return ApiResult.Success(
                CustomerDto(
                    customerId = customerId,
                    customerCode = "CUS-USER999",
                    displayName = "Corporate Client",
                    customerType = "BUSINESS",
                    status = request.status,
                    primaryPhone = "+8801711223344",
                    email = "client@sucharu.com"
                )
            )
        }

        override suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto> = TODO()
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
        override suspend fun getCustomerOrders(): ApiResult<List<CustomerOrderSummaryDto>> = TODO()
        override suspend fun getCustomerOrderDetail(orderId: String): ApiResult<CustomerOrderDetailDto> = TODO()
        override suspend fun createCustomerOrder(request: CreateOrderRequestDto, idempotencyKey: String?): ApiResult<CustomerOrderDetailDto> = TODO()
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
