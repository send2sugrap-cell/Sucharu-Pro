package com.sucharu.sucharupro.data.api.client

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID

/**
 * Production-grade HTTP network client implementation of [BackendApiClient] (INFRA-05 Step 02).
 *
 * Communicates exclusively over secure HTTPS/HTTP REST API boundaries without direct database credentials.
 * Implements single-flight refresh mutex, token transport, correlation tracking, and structured error handling.
 */
class HttpBackendApiClient(
    private val baseUrl: String,
    private val tokenStorage: AuthTokenStorage = InMemoryAuthTokenStorage(),
    private val connectTimeoutMs: Int = 5000,
    private val readTimeoutMs: Int = 10000,
    private val gson: Gson = Gson()
) : BackendApiClient {

    private val refreshMutex = Mutex()
    private val normalizedBaseUrl: String = baseUrl.trimEnd('/')

    private fun buildUrl(path: String): String {
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$normalizedBaseUrl$cleanPath"
    }

    private suspend fun <T> request(
        method: String,
        path: String,
        body: Any? = null,
        idempotencyKey: String? = null,
        typeToken: Type
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        val targetUrl = buildUrl(path)
        val correlationId = UUID.randomUUID().toString()
        var connection: HttpURLConnection? = null

        try {
            val uri = URI(targetUrl)
            val url = uri.toURL()
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method.uppercase()
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                useCaches = false
                instanceFollowRedirects = false
                doInput = true

                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Correlation-ID", correlationId)

                val token = tokenStorage.getToken()
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
                if (!idempotencyKey.isNullOrBlank()) {
                    setRequestProperty("Idempotency-Key", idempotencyKey)
                }

                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }

            if (body != null) {
                val jsonInput = gson.toJson(body)
                connection.outputStream.use { os ->
                    os.write(jsonInput.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
            }

            val statusCode = connection.responseCode
            val respCorrelationId = connection.getHeaderField("X-Correlation-ID") ?: correlationId

            val inputStream: InputStream? = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseString = inputStream?.let { readStream(it) } ?: ""

            if (statusCode in 200..299) {
                val parsedData = parseSuccessBody<T>(responseString, typeToken)
                ApiResult.Success(parsedData, respCorrelationId)
            } else {
                val errorResponse = parseErrorBody(responseString, statusCode, respCorrelationId)
                ApiResult.Error(errorResponse)
            }
        } catch (t: Throwable) {
            val mappedCode = when {
                t is java.net.SocketTimeoutException -> ErrorCode.DATABASE_UNAVAILABLE
                t is java.net.ConnectException -> ErrorCode.DATABASE_UNAVAILABLE
                else -> ErrorCode.INTERNAL_ERROR
            }
            ApiResult.Error(
                ApiErrorResponse(
                    success = false,
                    errorCode = mappedCode,
                    message = "Network Transport Error [${t.javaClass.simpleName}]: ${t.message ?: "Connection failure"}",
                    correlationId = correlationId
                )
            )
        } finally {
            connection?.disconnect()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> parseSuccessBody(json: String, typeToken: Type): T {
        if (json.isBlank()) {
            if (typeToken == Map::class.java) {
                return emptyMap<String, String>() as T
            }
        }
        val element = JsonParser.parseString(json)
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("success") && obj.get("success").asBoolean && obj.has("data")) {
                val dataElem = obj.get("data")
                return gson.fromJson(dataElem, typeToken)
            }
        }
        return gson.fromJson(json, typeToken)
    }

    private fun parseErrorBody(json: String, statusCode: Int, correlationId: String): ApiErrorResponse {
        if (json.isNotBlank()) {
            try {
                val element = JsonParser.parseString(json)
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    if (obj.has("errorCode") || obj.has("message")) {
                        val parsed = gson.fromJson(obj, ApiErrorResponse::class.java)
                        if (parsed != null) {
                            return parsed.copy(correlationId = parsed.correlationId ?: correlationId)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        val mappedErrorCode = when (statusCode) {
            400 -> ErrorCode.VALIDATION_ERROR
            401 -> ErrorCode.UNAUTHENTICATED
            403 -> ErrorCode.FORBIDDEN
            404 -> ErrorCode.NOT_FOUND
            409 -> ErrorCode.CONFLICT
            422 -> ErrorCode.VALIDATION_ERROR
            429 -> ErrorCode.RATE_LIMITED
            503 -> ErrorCode.DATABASE_UNAVAILABLE
            else -> ErrorCode.INTERNAL_ERROR
        }

        return ApiErrorResponse(
            success = false,
            errorCode = mappedErrorCode,
            message = "HTTP $statusCode Request Failed: ${if (json.isNotBlank()) json else "No response body"}",
            correlationId = correlationId
        )
    }

    private fun readStream(inputStream: InputStream): String {
        return inputStream.use { stream ->
            val buffer = ByteArray(4096)
            val output = ByteArrayOutputStream()
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }

    override suspend fun register(request: RegisterRequestDto): ApiResult<RegisterResponseDto> {
        return request("POST", "/api/v1/auth/register", body = request, typeToken = object : TypeToken<RegisterResponseDto>() {}.type)
    }

    override suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto> {
        val result: ApiResult<AuthResponseDto> = request("POST", "/api/v1/auth/login", body = request, typeToken = object : TypeToken<AuthResponseDto>() {}.type)
        if (result is ApiResult.Success) {
            tokenStorage.saveToken(result.data.accessToken)
        }
        return result
    }

    override suspend fun loginWithFirebase(request: FirebaseAuthRequestDto): ApiResult<AuthResponseDto> {
        val result: ApiResult<AuthResponseDto> = request("POST", "/api/v1/auth/firebase", body = request, typeToken = object : TypeToken<AuthResponseDto>() {}.type)
        if (result is ApiResult.Success) {
            tokenStorage.saveToken(result.data.accessToken)
        }
        return result
    }

    override suspend fun refreshToken(refreshToken: String): ApiResult<AuthResponseDto> = refreshMutex.withLock {
        val result: ApiResult<AuthResponseDto> = request("POST", "/api/v1/auth/refresh", body = RefreshRequestDto(refreshToken), typeToken = object : TypeToken<AuthResponseDto>() {}.type)
        if (result is ApiResult.Success) {
            tokenStorage.saveToken(result.data.accessToken)
        }
        return result
    }

    override suspend fun logout(allDevices: Boolean): ApiResult<Map<String, String>> {
        val result: ApiResult<Map<String, String>> = request("POST", "/api/v1/auth/logout", body = LogoutRequestDto(allDevices), typeToken = object : TypeToken<Map<String, String>>() {}.type)
        tokenStorage.clearToken()
        return result
    }

    override suspend fun logoutAll(): ApiResult<Map<String, String>> {
        val result: ApiResult<Map<String, String>> = request("POST", "/api/v1/auth/logout-all", typeToken = object : TypeToken<Map<String, String>>() {}.type)
        tokenStorage.clearToken()
        return result
    }

    override suspend fun requestPasswordRecovery(request: PasswordRecoveryRequestDto): ApiResult<PasswordRecoveryResponseDto> {
        return request("POST", "/api/v1/auth/password/recovery/request", body = request, typeToken = object : TypeToken<PasswordRecoveryResponseDto>() {}.type)
    }

    override suspend fun confirmPasswordReset(request: PasswordRecoveryConfirmDto): ApiResult<Map<String, Any>> {
        return request("POST", "/api/v1/auth/password/recovery/confirm", body = request, typeToken = object : TypeToken<Map<String, Any>>() {}.type)
    }

    override suspend fun requestVerificationToken(request: RequestVerificationRequestDto): ApiResult<Map<String, Any>> {
        return request("POST", "/api/v1/auth/verification/request", body = request, typeToken = object : TypeToken<Map<String, Any>>() {}.type)
    }

    override suspend fun confirmVerificationToken(request: ConfirmVerificationRequestDto): ApiResult<Map<String, Any>> {
        return request("POST", "/api/v1/auth/verification/confirm", body = request, typeToken = object : TypeToken<Map<String, Any>>() {}.type)
    }

    override suspend fun resendVerificationToken(identifier: String): ApiResult<Map<String, Any>> {
        return request("POST", "/api/v1/auth/verification/resend", body = ResendVerificationRequestDto(identifier), typeToken = object : TypeToken<Map<String, Any>>() {}.type)
    }

    override suspend fun getPublicCompanyInfo(): ApiResult<CompanyInfoDto> {
        return request("GET", "/api/v1/public/company", typeToken = object : TypeToken<CompanyInfoDto>() {}.type)
    }

    override suspend fun getPublicProducts(): ApiResult<List<PublicProductDto>> {
        return request("GET", "/api/v1/public/products", typeToken = object : TypeToken<List<PublicProductDto>>() {}.type)
    }

    override suspend fun getMyProfile(): ApiResult<AuthenticatedPrincipal> {
        return request("GET", "/api/v1/auth/me", typeToken = object : TypeToken<AuthenticatedPrincipal>() {}.type)
    }

    override suspend fun updateProfile(request: UpdateUserProfileRequestDto): ApiResult<Map<String, Any>> {
        return request("PATCH", "/api/v1/auth/profile", body = request, typeToken = object : TypeToken<Map<String, Any>>() {}.type)
    }

    override suspend fun getCustomerProfile(): ApiResult<CustomerProfileDto> {
        return request("GET", "/api/v1/customer/profile", typeToken = object : TypeToken<CustomerProfileDto>() {}.type)
    }

    override suspend fun listCustomers(): ApiResult<List<CustomerDto>> {
        return request("GET", "/api/v1/customers", typeToken = object : TypeToken<List<CustomerDto>>() {}.type)
    }

    override suspend fun getCustomerById(customerId: String): ApiResult<CustomerDto> {
        return request("GET", "/api/v1/customers/$customerId", typeToken = object : TypeToken<CustomerDto>() {}.type)
    }

    override suspend fun createCustomer(request: CreateCustomerRequestDto): ApiResult<CustomerDto> {
        return request("POST", "/api/v1/customers", body = request, idempotencyKey = request.idempotencyKey, typeToken = object : TypeToken<CustomerDto>() {}.type)
    }

    override suspend fun updateCustomer(customerId: String, request: UpdateCustomerRequestDto): ApiResult<CustomerDto> {
        return request("PUT", "/api/v1/customers/$customerId", body = request, typeToken = object : TypeToken<CustomerDto>() {}.type)
    }

    override suspend fun setCustomerStatus(customerId: String, request: SetCustomerStatusRequestDto): ApiResult<CustomerDto> {
        return request("POST", "/api/v1/customers/$customerId/status", body = request, typeToken = object : TypeToken<CustomerDto>() {}.type)
    }

    override suspend fun getCustomerOrders(): ApiResult<List<CustomerOrderSummaryDto>> {
        return request("GET", "/api/v1/customer/orders", typeToken = object : TypeToken<List<CustomerOrderSummaryDto>>() {}.type)
    }

    override suspend fun getCustomerOrderDetail(orderId: String): ApiResult<CustomerOrderDetailDto> {
        return request("GET", "/api/v1/customer/orders/$orderId", typeToken = object : TypeToken<CustomerOrderDetailDto>() {}.type)
    }

    override suspend fun createCustomerOrder(request: CreateOrderRequestDto, idempotencyKey: String?): ApiResult<CustomerOrderDetailDto> {
        return request("POST", "/api/v1/customer/orders", body = request, idempotencyKey = idempotencyKey, typeToken = object : TypeToken<CustomerOrderDetailDto>() {}.type)
    }

    override suspend fun getAffiliateProfile(): ApiResult<LegacyAffiliateProfileDto> {
        return request("GET", "/api/v1/affiliate/profile", typeToken = object : TypeToken<LegacyAffiliateProfileDto>() {}.type)
    }

    override suspend fun getAffiliateCommission(): ApiResult<AffiliateCommissionDto> {
        return request("GET", "/api/v1/affiliate/commission", typeToken = object : TypeToken<AffiliateCommissionDto>() {}.type)
    }

    override suspend fun calculatePrintingCost(request: com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationRequestDto): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto> {
        return request("POST", "/api/v1/printing-calculator/calculations", body = request, idempotencyKey = request.idempotencyKey, typeToken = object : TypeToken<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto>() {}.type)
    }

    override suspend fun validatePrintingCalculation(request: com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationRequestDto): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.ValidationResponseDto> {
        return request("POST", "/api/v1/printing-calculator/validate", body = request, typeToken = object : TypeToken<com.sucharu.sucharupro.data.api.model.printingcalculator.ValidationResponseDto>() {}.type)
    }

    override suspend fun getPrintingCalculationById(calculationId: String): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto> {
        return request("GET", "/api/v1/printing-calculator/calculations/$calculationId", typeToken = object : TypeToken<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto>() {}.type)
    }

    override suspend fun getPrintingCalculationBreakdown(calculationId: String): ApiResult<List<com.sucharu.sucharupro.data.api.model.printingcalculator.CalculationBreakdownItemDto>> {
        return request("GET", "/api/v1/printing-calculator/calculations/$calculationId/breakdown", typeToken = object : TypeToken<List<com.sucharu.sucharupro.data.api.model.printingcalculator.CalculationBreakdownItemDto>>() {}.type)
    }

    override suspend fun getPrintingCalculatorHandoffContract(calculationId: String): ApiResult<com.sucharu.sucharupro.data.api.model.printingcalculator.Module17Step01PrintingCalculatorHandoffContractDto> {
        return request("GET", "/api/v1/printing-calculator/calculations/$calculationId/handoff", typeToken = object : TypeToken<com.sucharu.sucharupro.data.api.model.printingcalculator.Module17Step01PrintingCalculatorHandoffContractDto>() {}.type)
    }

    override suspend fun listPrintingCalculations(): ApiResult<List<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto>> {
        return request("GET", "/api/v1/printing-calculator/calculations", typeToken = object : TypeToken<List<com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto>>() {}.type)
    }

    override suspend fun checkHealthLive(): ApiResult<Map<String, String>> {
        return request("GET", "/health/live", typeToken = object : TypeToken<Map<String, String>>() {}.type)
    }

    override suspend fun checkHealthReady(): ApiResult<DatabaseHealthStatus> {
        return request("GET", "/health/ready", typeToken = object : TypeToken<DatabaseHealthStatus>() {}.type)
    }
}
