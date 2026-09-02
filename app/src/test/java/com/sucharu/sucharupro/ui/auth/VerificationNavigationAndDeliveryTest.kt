package com.sucharu.sucharupro.ui.auth

import com.sucharu.sucharupro.data.auth.model.VerificationType
import com.sucharu.sucharupro.data.auth.security.FakeVerificationNotificationProvider
import com.sucharu.sucharupro.data.auth.security.ProductionSmsVerificationNotificationProvider
import com.sucharu.sucharupro.data.auth.security.TokenGenerator
import com.sucharu.sucharupro.data.auth.security.VerificationDeliveryStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying Authentication Verification Delivery Pipeline & Back Navigation semantics.
 */
class VerificationNavigationAndDeliveryTest {

    @Test
    fun test01_authNavigationBackTransitions() {
        var activeAuthScreenOverride: String? = "verification"
        var errorMessage: String? = "Some error"
        var successMessage: String? = "Some success"

        // Simulating BackHandler on "verification" screen
        fun handleBack() {
            when (activeAuthScreenOverride) {
                "verification" -> {
                    errorMessage = null
                    successMessage = null
                    activeAuthScreenOverride = "register"
                }
                "register" -> {
                    errorMessage = null
                    successMessage = null
                    activeAuthScreenOverride = "login"
                }
                "forgot_password" -> {
                    errorMessage = null
                    successMessage = null
                    activeAuthScreenOverride = "login"
                }
                "reset_password" -> {
                    errorMessage = null
                    successMessage = null
                    activeAuthScreenOverride = "login"
                }
                "login" -> {
                    errorMessage = null
                    successMessage = null
                    activeAuthScreenOverride = null
                }
                else -> {
                    activeAuthScreenOverride = null
                }
            }
        }

        // 1. Back from verification -> register
        handleBack()
        assertEquals("register", activeAuthScreenOverride)
        assertNull(errorMessage)
        assertNull(successMessage)

        // 2. Back from register -> login
        handleBack()
        assertEquals("login", activeAuthScreenOverride)

        // 3. Back from login -> public home
        handleBack()
        assertNull(activeAuthScreenOverride)
    }

    @Test
    fun test02_tokenGeneratorProduces6DigitNumericOtp() {
        for (i in 1..100) {
            val otp = TokenGenerator.generateNumericOtp(6)
            assertEquals(6, otp.length)
            assertTrue("OTP $otp must be numeric", otp.all { it.isDigit() })
            val numericVal = otp.toInt()
            assertTrue("OTP $otp must be >= 100000 and <= 999999", numericVal in 100000..999999)
        }
    }

    @Test
    fun test03_fakeProviderCapturesRecipientAndToken() = runBlocking {
        val provider = FakeVerificationNotificationProvider()
        val result = provider.sendVerificationNotification(
            projectId = "TENANT-001",
            userId = "usr-123",
            recipient = "01712553809",
            type = VerificationType.PHONE,
            rawToken = "489201"
        )

        assertTrue(result.isAccepted)
        assertEquals(VerificationDeliveryStatus.DELIVERY_ACCEPTED, result.status)
        assertEquals("489201", provider.getLatestTokenForRecipient("01712553809"))
    }

    @Test
    fun test04_fakeProviderSimulatesDeliveryFailureHonesty() = runBlocking {
        val provider = FakeVerificationNotificationProvider(simulateSuccess = false)
        val result = provider.sendVerificationNotification(
            projectId = "TENANT-001",
            userId = "usr-123",
            recipient = "01712553809",
            type = VerificationType.PHONE,
            rawToken = "489201"
        )

        assertFalse(result.isAccepted)
        assertEquals(VerificationDeliveryStatus.DELIVERY_FAILED, result.status)
    }

    @Test
    fun test05_productionSmsProviderWithoutCredentialsReportsUnavailable() = runBlocking {
        val prodProvider = ProductionSmsVerificationNotificationProvider(
            gatewayUrl = null,
            apiKey = null
        )
        val result = prodProvider.sendVerificationNotification(
            projectId = "TENANT-001",
            userId = "usr-123",
            recipient = "01712553809",
            type = VerificationType.PHONE,
            rawToken = "489201"
        )

        assertFalse(result.isAccepted)
        assertEquals(VerificationDeliveryStatus.PROVIDER_UNAVAILABLE, result.status)
        assertTrue(result.message.contains("We couldn't send the verification code right now"))
    }
}
