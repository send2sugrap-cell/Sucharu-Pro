package com.sucharu.sucharupro.data.auth.security

import com.sucharu.sucharupro.data.auth.model.VerificationType
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.logging.Logger

/**
 * Result of attempting verification code / token delivery (INFRA-03 Step 04).
 */
data class VerificationDeliveryResult(
    val isAccepted: Boolean,
    val status: VerificationDeliveryStatus,
    val providerName: String,
    val message: String,
    val transactionId: String? = null
) {
    companion object {
        fun accepted(
            providerName: String,
            message: String = "Verification code accepted for delivery.",
            transactionId: String? = null
        ) = VerificationDeliveryResult(
            isAccepted = true,
            status = VerificationDeliveryStatus.DELIVERY_ACCEPTED,
            providerName = providerName,
            message = message,
            transactionId = transactionId
        )

        fun failed(
            providerName: String,
            message: String = "We couldn't send the verification code right now. Please try again shortly."
        ) = VerificationDeliveryResult(
            isAccepted = false,
            status = VerificationDeliveryStatus.DELIVERY_FAILED,
            providerName = providerName,
            message = message
        )

        fun unavailable(
            providerName: String,
            message: String = "SMS delivery service is not configured. Please contact support or try again later."
        ) = VerificationDeliveryResult(
            isAccepted = false,
            status = VerificationDeliveryStatus.PROVIDER_UNAVAILABLE,
            providerName = providerName,
            message = message
        )

        fun invalidRecipient(
            providerName: String,
            message: String = "The provided recipient address or phone number is invalid."
        ) = VerificationDeliveryResult(
            isAccepted = false,
            status = VerificationDeliveryStatus.INVALID_RECIPIENT,
            providerName = providerName,
            message = message
        )
    }
}

/**
 * Detailed delivery outcome status.
 */
enum class VerificationDeliveryStatus {
    DELIVERY_ACCEPTED,
    DELIVERY_FAILED,
    PROVIDER_UNAVAILABLE,
    RATE_LIMITED,
    INVALID_RECIPIENT
}

/**
 * Provider-agnostic interface for delivering verification & recovery tokens (INFRA-03 Step 04).
 * Decouples core auth logic from specific email/SMS vendor SDKs.
 */
interface IVerificationNotificationProvider {
    val providerName: String get() = "DEFAULT"

    suspend fun sendVerificationNotification(
        projectId: String,
        userId: String,
        recipient: String,
        type: VerificationType,
        rawToken: String
    ): VerificationDeliveryResult
}

/**
 * In-memory / development notification provider logging token delivery without external vendor calls.
 */
class FakeVerificationNotificationProvider(
    private var simulateSuccess: Boolean = true
) : IVerificationNotificationProvider {

    override val providerName: String = "FakeVerificationNotificationProvider"
    private val sentNotifications = mutableListOf<SentNotification>()

    data class SentNotification(
        val projectId: String,
        val userId: String,
        val recipient: String,
        val type: VerificationType,
        val rawToken: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    override suspend fun sendVerificationNotification(
        projectId: String,
        userId: String,
        recipient: String,
        type: VerificationType,
        rawToken: String
    ): VerificationDeliveryResult {
        sentNotifications.add(
            SentNotification(
                projectId = projectId,
                userId = userId,
                recipient = recipient,
                type = type,
                rawToken = rawToken
            )
        )

        return if (simulateSuccess) {
            VerificationDeliveryResult.accepted(
                providerName = providerName,
                message = "Verification code delivered to test environment buffer.",
                transactionId = "test_tx_${UUID.randomUUID().toString().take(8)}"
            )
        } else {
            VerificationDeliveryResult.failed(
                providerName = providerName,
                message = "Simulated delivery failure in test provider."
            )
        }
    }

    fun setSimulateSuccess(success: Boolean) {
        this.simulateSuccess = success
    }

    fun getSentNotifications(): List<SentNotification> = sentNotifications.toList()

    fun getLatestTokenForRecipient(recipient: String): String? {
        return sentNotifications.lastOrNull { it.recipient == recipient }?.rawToken
    }

    fun clear() {
        sentNotifications.clear()
    }
}

/**
 * Production-ready SMS Verification Notification Provider.
 * Checks environment / configuration for SMS gateway parameters and safely executes HTTP delivery.
 * If credentials are not present, honestly reports PROVIDER_UNAVAILABLE.
 */
class ProductionSmsVerificationNotificationProvider(
    private val gatewayUrl: String? = System.getenv("SMS_GATEWAY_URL") ?: System.getProperty("sucharu.sms.gateway.url"),
    private val apiKey: String? = System.getenv("SMS_API_KEY") ?: System.getProperty("sucharu.sms.api.key"),
    private val senderId: String? = System.getenv("SMS_SENDER_ID") ?: System.getProperty("sucharu.sms.sender.id") ?: "SUCHARU",
    private val fallbackDevProvider: IVerificationNotificationProvider? = null
) : IVerificationNotificationProvider {

    private val logger = Logger.getLogger(ProductionSmsVerificationNotificationProvider::class.java.name)
    override val providerName: String = "ProductionSmsGateway"

    override suspend fun sendVerificationNotification(
        projectId: String,
        userId: String,
        recipient: String,
        type: VerificationType,
        rawToken: String
    ): VerificationDeliveryResult {
        // If gateway is not configured:
        if (gatewayUrl.isNullOrBlank() || apiKey.isNullOrBlank()) {
            if (fallbackDevProvider != null) {
                return fallbackDevProvider.sendVerificationNotification(projectId, userId, recipient, type, rawToken)
            }
            logger.warning("SMS gateway credentials not configured in environment. Rejecting delivery request.")
            return VerificationDeliveryResult.unavailable(
                providerName = providerName,
                message = "We couldn't send the verification code right now. Please try again shortly."
            )
        }

        return try {
            val normalizedRecipient = com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(recipient)
            val smsText = "Your Sucharu Pro verification code is: $rawToken. Valid for 15 minutes. Do not share this code."

            val url = URL(gatewayUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")

            val jsonPayload = """
                {
                    "sender": "$senderId",
                    "recipient": "$normalizedRecipient",
                    "message": "$smsText"
                }
            """.trimIndent()

            conn.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val txId = conn.headerFields["X-Message-ID"]?.firstOrNull() ?: UUID.randomUUID().toString()
                VerificationDeliveryResult.accepted(
                    providerName = providerName,
                    message = "A new verification code has been sent.",
                    transactionId = txId
                )
            } else {
                logger.severe("SMS gateway returned error HTTP status code: $responseCode")
                VerificationDeliveryResult.failed(
                    providerName = providerName,
                    message = "We couldn't send the verification code right now. Please try again shortly."
                )
            }
        } catch (e: Exception) {
            logger.severe("Exception during SMS verification delivery: ${e.message}")
            VerificationDeliveryResult.failed(
                providerName = providerName,
                message = "We couldn't send the verification code right now. Please try again shortly."
            )
        }
    }
}
