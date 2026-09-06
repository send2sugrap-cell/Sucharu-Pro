package com.sucharu.sucharupro.data.auth.provider

import com.sucharu.sucharupro.data.api.model.ApiErrorResponse
import com.sucharu.sucharupro.data.api.model.ApiResult
import com.sucharu.sucharupro.data.api.model.ErrorCode
import java.util.UUID

/**
 * Isolated In-Memory Authentication Provider for non-Firebase test/demo environments.
 * Dispatches dynamic 6-digit verification sessions and strictly rejects hardcoded '123456'.
 * Strictly isolated from production Firebase and production server endpoints.
 */
class DemoAuthenticationProvider(
    private val mockOtpCode: String? = null
) : AuthenticationProvider {

    private var currentVerificationId: String? = null
    private var currentPhone: String? = null
    private var currentToken: String? = null
    private var activeOtpSessionCode: String? = null

    override suspend fun requestPhoneOtp(phoneNumber: String, activity: Any?): ApiResult<OtpRequestResult> {
        val normalized = com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(phoneNumber)
        if (normalized.isBlank() || normalized.length < 8) {
            return ApiResult.Error(
                errorResponse = ApiErrorResponse(
                    errorCode = ErrorCode.VALIDATION_ERROR,
                    message = "Invalid phone number format."
                )
            )
        }
        val verId = "VER-SESSION-${UUID.randomUUID().toString().take(8)}"
        currentVerificationId = verId
        currentPhone = normalized
        activeOtpSessionCode = mockOtpCode ?: (100000..999999).random().toString()
        return ApiResult.Success(
            OtpRequestResult(
                verificationId = verId,
                message = "Verification code dispatched to $normalized."
            )
        )
    }

    override suspend fun verifyPhoneOtp(verificationId: String, otpCode: String): ApiResult<AuthTokenResult> {
        if (currentVerificationId == null || currentVerificationId != verificationId) {
            return ApiResult.Error(
                errorResponse = ApiErrorResponse(
                    errorCode = ErrorCode.UNAUTHENTICATED,
                    message = "Invalid or expired OTP session. Please request a new code."
                )
            )
        }

        val trimmed = otpCode.trim()
        if (trimmed == "123456") {
            return ApiResult.Error(
                errorResponse = ApiErrorResponse(
                    errorCode = ErrorCode.UNAUTHENTICATED,
                    message = "Hardcoded Demo OTP 123456 is strictly prohibited. Real SMS OTP is required."
                )
            )
        }

        if (activeOtpSessionCode != null && trimmed != activeOtpSessionCode) {
            return ApiResult.Error(
                errorResponse = ApiErrorResponse(
                    errorCode = ErrorCode.UNAUTHENTICATED,
                    message = "Incorrect verification code."
                )
            )
        }

        val sessionToken = "session-id-token-${UUID.randomUUID()}"
        currentToken = sessionToken

        return ApiResult.Success(
            AuthTokenResult(
                idToken = sessionToken,
                providerUid = "UID-${currentPhone?.takeLast(6) ?: "000000"}",
                phone = currentPhone,
                isNewUser = false,
                message = "Phone identity verified successfully."
            )
        )
    }

    override fun getIdentityToken(): String? = currentToken

    override fun signOut() {
        currentVerificationId = null
        currentPhone = null
        currentToken = null
        activeOtpSessionCode = null
    }
}

