package com.sucharu.sucharupro.ui.navigation

import com.sucharu.sucharupro.data.api.client.BackendApiClient
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.session.AuthenticationSessionManager
import com.sucharu.sucharupro.data.auth.session.InMemorySecureSessionStore
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionExpiryNavigationTest {

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
            override suspend fun createCustomerOrder(request: CreateOrderRequestDto, idempotencyKey: String?): ApiResult<CustomerOrderDetailDto> = err
            override suspend fun getAffiliateProfile(): ApiResult<AffiliateProfileDto> = err
            override suspend fun getAffiliateCommission(): ApiResult<AffiliateCommissionDto> = err
            override suspend fun checkHealthLive(): ApiResult<Map<String, String>> = ApiResult.Success(mapOf("status" to "UP"))
            override suspend fun checkHealthReady(): ApiResult<DatabaseHealthStatus> = ApiResult.Success(DatabaseHealthStatus(isLive = true, isReady = true))
        }

        sessionManager = AuthenticationSessionManager(client = stubClient, sessionStore = store)
        navigationManager = AppNavigationManager(sessionManager)
    }

    @Test
    fun testSessionExpirationResetsBackStackAndRoutesToSessionExpired() {
        val principal = AuthenticatedPrincipal(
            userId = "USR-CUST-1",
            projectId = "PRJ-01",
            username = "customer1",
            role = UserRole.CUSTOMER,
            accountStatus = AccountStatus.ACTIVE
        )

        navigationManager.navigateTo(AppDestination.Customer.Orders, principal)
        navigationManager.navigateTo(AppDestination.Customer.Invoices, principal)
        assertEquals(3, navigationManager.getBackStackDepth())

        // Expire session
        navigationManager.handleSessionExpiration()

        assertEquals(AppDestination.Security.SessionExpired, navigationManager.currentDestination.value)
        assertEquals(2, navigationManager.getBackStackDepth()) // Public Home + SessionExpired
    }
}
