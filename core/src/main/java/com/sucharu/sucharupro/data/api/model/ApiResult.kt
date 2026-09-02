package com.sucharu.sucharupro.data.api.model

/**
 * Standard machine-readable error codes for Sucharu Pro API (INFRA-02 Step 04).
 */
enum class ErrorCode {
    VALIDATION_ERROR,
    UNAUTHENTICATED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    OPTIMISTIC_CONCURRENCY_CONFLICT,
    IDEMPOTENCY_CONFLICT,
    TENANT_ACCESS_DENIED,
    RATE_LIMITED,
    DATABASE_UNAVAILABLE,
    INTERNAL_ERROR
}

/**
 * Standard API error detail payload.
 */
data class ApiErrorDetail(
    val field: String? = null,
    val message: String
)

/**
 * Standardized API error response payload.
 */
data class ApiErrorResponse(
    val success: Boolean = false,
    val errorCode: ErrorCode,
    val message: String,
    val details: List<ApiErrorDetail> = emptyList(),
    val correlationId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Standardized API success response payload.
 */
data class ApiSuccessResponse<T>(
    val success: Boolean = true,
    val data: T,
    val correlationId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sealed result type for backend and client API execution.
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T, val correlationId: String? = null) : ApiResult<T>()
    data class Error(val errorResponse: ApiErrorResponse) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw ApiException(errorResponse)
    }
}

/**
 * Typed domain API exceptions mapped to HTTP status codes.
 */
open class ApiException(val errorResponse: ApiErrorResponse) : RuntimeException(errorResponse.message)

class UnauthenticatedException(message: String = "Authentication credentials missing or invalid") :
    ApiException(ApiErrorResponse(errorCode = ErrorCode.UNAUTHENTICATED, message = message))

class ForbiddenException(message: String = "Access to the requested resource is forbidden") :
    ApiException(ApiErrorResponse(errorCode = ErrorCode.FORBIDDEN, message = message))

class NotFoundException(message: String = "Requested resource not found") :
    ApiException(ApiErrorResponse(errorCode = ErrorCode.NOT_FOUND, message = message))

class ConflictException(
    val errorCode: ErrorCode = ErrorCode.CONFLICT,
    message: String = "Resource state conflict detected"
) : ApiException(ApiErrorResponse(errorCode = errorCode, message = message))

class ValidationException(message: String, details: List<ApiErrorDetail> = emptyList()) :
    ApiException(ApiErrorResponse(errorCode = ErrorCode.VALIDATION_ERROR, message = message, details = details))

class DatabaseUnavailableException(message: String = "Database service is temporarily unavailable") :
    ApiException(ApiErrorResponse(errorCode = ErrorCode.DATABASE_UNAVAILABLE, message = message))
