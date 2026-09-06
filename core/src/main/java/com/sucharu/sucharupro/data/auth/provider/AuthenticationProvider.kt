package com.sucharu.sucharupro.data.auth.provider

import com.sucharu.sucharupro.data.api.model.ApiResult

/**
 * Result data holder for OTP request dispatch.
 */
data class OtpRequestResult(
    val verificationId: String,
    val resendToken: Any? = null,
    val isAutoVerified: Boolean = false,
    val message: String = "OTP sent successfully."
)

/**
 * Result data holder for verified authentication token credential.
 */
data class AuthTokenResult(
    val idToken: String,
    val providerUid: String,
    val phone: String? = null,
    val isNewUser: Boolean = false,
    val message: String = "Identity verified."
)

/**
 * Provider-Agnostic Authentication & OTP Abstraction (Infrastructure Layer).
 * Decouples Android UI/ViewModels and Domain layer from specific OTP identity vendors (e.g. Firebase, Twilio, SMS Gateway).
 * Allows total provider portability without mutating ERP business or domain logic.
 */
interface AuthenticationProvider {
    /**
     * Dispatches phone OTP verification code to the target phone number.
     */
    suspend fun requestPhoneOtp(phoneNumber: String, activity: Any? = null): ApiResult<OtpRequestResult>

    /**
     * Verifies the received OTP code against the verification ID context.
     */
    suspend fun verifyPhoneOtp(verificationId: String, otpCode: String): ApiResult<AuthTokenResult>

    /**
     * Returns the current verified provider identity token if available.
     */
    fun getIdentityToken(): String?

    /**
     * Revokes or clears local provider authentication credentials.
     */
    fun signOut()
}
