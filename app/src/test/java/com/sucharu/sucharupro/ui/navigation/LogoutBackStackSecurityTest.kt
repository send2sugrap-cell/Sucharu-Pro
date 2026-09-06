package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.client.BackendApiClient
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.session.AuthenticationSessionManager
import com.sucharu.sucharupro.data.auth.session.InMemorySecureSessionStore
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LogoutBackStackSecurityTest {

    private lateinit var navigationManager: AppNavigationManager
    private lateinit var sessionManager: AuthenticationSessionManager

    @Before
    fun setUp() {
        val store = InMemorySecureSessionStore()
        val err = ApiResult.Error(ApiErrorResponse(errorCode = ErrorCode.INTERNAL_ERROR, message = "test"))
        val stubClient = object : BackendApiClient {
            override suspend fun register(request: RegisterRequestDto): ApiResult<RegisterResponseDto> = err
            override suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto> = err
            override suspend fun refreshToken(refreshToken: String): ApiResult<AuthResponseDto> = err
            override suspend fun logout(allDevices: Boolean): ApiResult<Map<String, String>> = ApiResult.Success(mapOf("status" to "ok"))
            override suspend fun logoutAll(): ApiResult<Map<String, String>> = ApiResult.Success(mapOf("status" to "ok"))
            override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequestDto): ApiResult<PasswordRecoveryResponseDto> = err
            override suspend fun confirmPasswordReset(request: PasswordRecoveryConfirmDto): ApiResult<Map<String, Any>> = err
            override suspend fun requestVerificationToken(request: RequestVerificationRequestDto): ApiResult<Map<String, Any>> = err
            override suspend fun confirmVerificationToken(request: ConfirmVerificationRequestDto): ApiResult<Map<String, Any>> = err
            override suspend fun resendVerificationToken(identifier: String): ApiResult<Map<String, Any>> = err
            override suspend fun getPublicCompanyInfo(): ApiResult<CompanyInfoDto> = err
            override suspend fun getPublicProducts(): ApiResult<List<PublicProductDto>> = ApiResult.Success(emptyList())
            override suspend fun getMyProfile(): ApiResult<AuthenticatedPrincipal> = err
            override suspend fun getCustomerProfile(): ApiResult<CustomerProfileDto> = err
            override suspend fun getCustomerOrders(): ApiResult<List<CustomerOrderSummaryDto>> = ApiResult.Success(emptyList())
            override suspend fun getCustomerOrderDetail(orderId: String): ApiResult<CustomerOrderDetailDto> = err
            override suspend fun updateProfile(request: UpdateUserProfileRequestDto): ApiResult<Map<String, Any>> = err
            override suspend fun createCustomerOrder(request: CreateOrderRequestDto, idempotencyKey: String?): ApiResult<CustomerOrderDetailDto> = err
            override suspend fun getAffiliateProfile(): ApiResult<LegacyAffiliateProfileDto> = err
            override suspend fun getAffiliateCommission(): ApiResult<AffiliateCommissionDto> = err
            override suspend fun checkHealthLive(): ApiResult<Map<String, String>> = ApiResult.Success(mapOf("status" to "UP"))
            override suspend fun checkHealthReady(): ApiResult<DatabaseHealthStatus> = ApiResult.Success(DatabaseHealthStatus(isLive = true, isReady = true))
            override suspend fun calculatePrintingCost(request: com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationRequestDto): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto> = err
            override suspend fun validatePrintingCalculation(request: com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationRequestDto): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.ValidationResponseDto> = err
            override suspend fun getPrintingCalculationById(calculationId: String): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto> = err
            override suspend fun getPrintingCalculationBreakdown(calculationId: String): ApiResult<List<com.sucharu.sucharupro.data.api.model.printingcalculator.CalculationBreakdownItemDto>> = err
            override suspend fun getPrintingCalculatorHandoffContract(calculationId: String): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.Module17Step01PrintingCalculatorHandoffContractDto> = err
            override suspend fun listCustomers(): ApiResult<List<CustomerDto>> = err
            override suspend fun getCustomerById(customerId: String): ApiResult<CustomerDto> = err
            override suspend fun createCustomer(request: CreateCustomerRequestDto): ApiResult<CustomerDto> = err
            override suspend fun updateCustomer(customerId: String, request: UpdateCustomerRequestDto): ApiResult<CustomerDto> = err
            override suspend fun setCustomerStatus(customerId: String, request: SetCustomerStatusRequestDto): ApiResult<CustomerDto> = err
            override suspend fun loginWithFirebase(request: FirebaseAuthRequestDto): ApiResult<AuthResponseDto> = err
            override suspend fun listPrintingCalculations(): ApiResult<List<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto>> = err
        }

        sessionManager = AuthenticationSessionManager(client = stubClient, sessionStore = store)
        navigationManager = AppNavigationManager(sessionManager)
    }

    @Test
    fun testLogoutClearsProtectedBackStackAndResetsToPublicHome() = runBlocking {
        val principal = AuthenticatedPrincipal(
            userId = "USR-ADM-1",
            projectId = "PRJ-01",
            username = "admin",
            role = UserRole.ADMIN,
            accountStatus = AccountStatus.ACTIVE
        )

        navigationManager.navigateTo(AppDestination.Admin.FullAdministration, principal)
        navigationManager.navigateTo(AppDestination.Admin.Security, principal)
        assertEquals(3, navigationManager.getBackStackDepth())

        // Perform secure logout
        navigationManager.performSecureLogout()

        assertEquals(AppDestination.Public.Home, navigationManager.currentDestination.value)
        assertEquals(1, navigationManager.getBackStackDepth())

        // Back navigation must return false as backstack is at root
        val popped = navigationManager.navigateBack()
        assertFalse("Popping backstack after logout must return false", popped)
    }
}
