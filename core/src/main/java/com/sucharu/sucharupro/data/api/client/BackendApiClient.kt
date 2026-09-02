package com.sucharu.sucharupro.data.api.client

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendApiServer
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Client-side interface for communicating with Sucharu Pro Backend API (INFRA-02 Step 04, INFRA-03 Step 01 & Step 04).
 *
 * Guarantees that the Android application never bundles direct PostgreSQL credentials
 * and interacts exclusively via the secure REST API boundary.
 */
interface BackendApiClient {
    suspend fun register(request: RegisterRequestDto): ApiResult<RegisterResponseDto>
    suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto>
    suspend fun refreshToken(refreshToken: String): ApiResult<AuthResponseDto>
    suspend fun logout(allDevices: Boolean = false): ApiResult<Map<String, String>>
    suspend fun logoutAll(): ApiResult<Map<String, String>>
    suspend fun requestPasswordRecovery(request: PasswordRecoveryRequestDto): ApiResult<PasswordRecoveryResponseDto>
    suspend fun confirmPasswordReset(request: PasswordRecoveryConfirmDto): ApiResult<Map<String, Any>>
    suspend fun requestVerificationToken(request: RequestVerificationRequestDto): ApiResult<Map<String, Any>>
    suspend fun confirmVerificationToken(request: ConfirmVerificationRequestDto): ApiResult<Map<String, Any>>
    suspend fun resendVerificationToken(identifier: String): ApiResult<Map<String, Any>>
    suspend fun getPublicCompanyInfo(): ApiResult<CompanyInfoDto>
    suspend fun getPublicProducts(): ApiResult<List<PublicProductDto>>
    suspend fun getMyProfile(): ApiResult<AuthenticatedPrincipal>
    suspend fun getCustomerProfile(): ApiResult<CustomerProfileDto>
    suspend fun getCustomerOrders(): ApiResult<List<CustomerOrderSummaryDto>>
    suspend fun getCustomerOrderDetail(orderId: String): ApiResult<CustomerOrderDetailDto>
    suspend fun createCustomerOrder(request: CreateOrderRequestDto, idempotencyKey: String? = null): ApiResult<CustomerOrderDetailDto>
    suspend fun getAffiliateProfile(): ApiResult<AffiliateProfileDto>
    suspend fun getAffiliateCommission(): ApiResult<AffiliateCommissionDto>
    suspend fun checkHealthLive(): ApiResult<Map<String, String>>
    suspend fun checkHealthReady(): ApiResult<DatabaseHealthStatus>
}

typealias DevelopmentDirectBackendApiClient = DirectBackendApiClient
typealias InProcessBackendTestClient = DirectBackendApiClient

/**
 * In-process direct-dispatch implementation of [BackendApiClient] connecting client to [BackendApiServer].
 * Features single-flight mutex protection to prevent race conditions during token refresh.
 *
 * NOTE: For local development / offline demo mode and deterministic testing only.
 * Production network transport is provided by HttpBackendApiClient (INFRA-05 Step 02).
 */
class DirectBackendApiClient(
    private val server: BackendApiServer,
    private val tokenStorage: AuthTokenStorage = InMemoryAuthTokenStorage()
) : BackendApiClient {

    private val refreshMutex = Mutex()

    private fun buildHeaders(idempotencyKey: String? = null): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val token = tokenStorage.getToken()
        if (token != null) {
            headers["Authorization"] = "Bearer $token"
        }
        if (idempotencyKey != null) {
            headers["Idempotency-Key"] = idempotencyKey
        }
        return headers
    }

    override suspend fun register(request: RegisterRequestDto): ApiResult<RegisterResponseDto> {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/auth/register", body = request))
        return if (res.statusCode == 201 || res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as RegisterResponseDto, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto> {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/auth/login", body = request))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            val authResp = success.data as AuthResponseDto
            tokenStorage.saveToken(authResp.accessToken)
            ApiResult.Success(authResp, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun refreshToken(refreshToken: String): ApiResult<AuthResponseDto> = refreshMutex.withLock {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/auth/refresh", body = RefreshRequestDto(refreshToken)))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            val authResp = success.data as AuthResponseDto
            tokenStorage.saveToken(authResp.accessToken)
            ApiResult.Success(authResp, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun logout(allDevices: Boolean): ApiResult<Map<String, String>> {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/auth/logout", headers = buildHeaders(), body = LogoutRequestDto(allDevices)))
        tokenStorage.clearToken()
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as Map<String, String>, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun logoutAll(): ApiResult<Map<String, String>> {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/auth/logout-all", headers = buildHeaders()))
        tokenStorage.clearToken()
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as Map<String, String>, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequestDto): ApiResult<PasswordRecoveryResponseDto> {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/auth/password/recovery/request", body = request))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as PasswordRecoveryResponseDto, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun confirmPasswordReset(request: PasswordRecoveryConfirmDto): ApiResult<Map<String, Any>> {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/auth/password/recovery/confirm", body = request))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as Map<String, Any>, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun requestVerificationToken(request: RequestVerificationRequestDto): ApiResult<Map<String, Any>> {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/auth/verification/request", headers = buildHeaders(), body = request))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as Map<String, Any>, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun confirmVerificationToken(request: ConfirmVerificationRequestDto): ApiResult<Map<String, Any>> {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/auth/verification/confirm", headers = buildHeaders(), body = request))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as Map<String, Any>, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun resendVerificationToken(identifier: String): ApiResult<Map<String, Any>> {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/auth/verification/resend", headers = buildHeaders(), body = ResendVerificationRequestDto(identifier)))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as Map<String, Any>, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun getPublicCompanyInfo(): ApiResult<CompanyInfoDto> {
        val res = server.handle(HttpRequest(method = "GET", path = "/api/v1/public/company", headers = buildHeaders()))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as CompanyInfoDto, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun getPublicProducts(): ApiResult<List<PublicProductDto>> {
        val res = server.handle(HttpRequest(method = "GET", path = "/api/v1/public/products", headers = buildHeaders()))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as List<PublicProductDto>, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun getMyProfile(): ApiResult<AuthenticatedPrincipal> {
        val res = server.handle(HttpRequest(method = "GET", path = "/api/v1/auth/me", headers = buildHeaders()))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            val data = success.data
            val principal = when (data) {
                is AuthenticatedPrincipal -> data
                is UserProfileDto -> AuthenticatedPrincipal(
                    userId = data.userId,
                    projectId = data.projectId,
                    username = data.username,
                    role = data.role,
                    permissions = emptySet(),
                    email = data.email,
                    accountStatus = data.accountStatus,
                    tokenExpiresAt = System.currentTimeMillis() + 3600000L
                )
                is Map<*, *> -> AuthenticatedPrincipal(
                    userId = data["userId"] as? String ?: "",
                    projectId = data["projectId"] as? String ?: "",
                    username = data["username"] as? String ?: "",
                    role = UserRole.valueOf(data["role"] as? String ?: "CUSTOMER"),
                    permissions = emptySet(),
                    email = data["email"] as? String,
                    accountStatus = AccountStatus.valueOf(data["accountStatus"] as? String ?: "ACTIVE"),
                    tokenExpiresAt = System.currentTimeMillis() + 3600000L
                )
                else -> throw IllegalStateException("Unexpected identity response type: ${data?.javaClass}")
            }
            ApiResult.Success(principal, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun getCustomerProfile(): ApiResult<CustomerProfileDto> {
        val res = server.handle(HttpRequest(method = "GET", path = "/api/v1/customer/profile", headers = buildHeaders()))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as CustomerProfileDto, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun getCustomerOrders(): ApiResult<List<CustomerOrderSummaryDto>> {
        val res = server.handle(HttpRequest(method = "GET", path = "/api/v1/customer/orders", headers = buildHeaders()))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as List<CustomerOrderSummaryDto>, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun getCustomerOrderDetail(orderId: String): ApiResult<CustomerOrderDetailDto> {
        val res = server.handle(HttpRequest(method = "GET", path = "/api/v1/customer/orders/$orderId", headers = buildHeaders()))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as CustomerOrderDetailDto, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun createCustomerOrder(request: CreateOrderRequestDto, idempotencyKey: String?): ApiResult<CustomerOrderDetailDto> {
        val res = server.handle(HttpRequest(method = "POST", path = "/api/v1/customer/orders", headers = buildHeaders(idempotencyKey), body = request))
        return if (res.statusCode == 201) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as CustomerOrderDetailDto, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun getAffiliateProfile(): ApiResult<AffiliateProfileDto> {
        val res = server.handle(HttpRequest(method = "GET", path = "/api/v1/affiliate/profile", headers = buildHeaders()))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as AffiliateProfileDto, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun getAffiliateCommission(): ApiResult<AffiliateCommissionDto> {
        val res = server.handle(HttpRequest(method = "GET", path = "/api/v1/affiliate/commission", headers = buildHeaders()))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as AffiliateCommissionDto, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun checkHealthLive(): ApiResult<Map<String, String>> {
        val res = server.handle(HttpRequest(method = "GET", path = "/health/live"))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as Map<String, String>, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }

    override suspend fun checkHealthReady(): ApiResult<DatabaseHealthStatus> {
        val res = server.handle(HttpRequest(method = "GET", path = "/health/ready"))
        return if (res.statusCode == 200) {
            val success = res.body as ApiSuccessResponse<*>
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(success.data as DatabaseHealthStatus, res.correlationId)
        } else {
            ApiResult.Error(res.body as ApiErrorResponse)
        }
    }
}
