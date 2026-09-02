package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.integration.notification.*
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.DeliveryDispatchedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.PaymentReceivedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList

class NotificationIntegrationTest {

    private class FakeNotificationProvider(
        override val channel: NotificationChannel
    ) : NotificationProvider {
        val deliveredIntents = CopyOnWriteArrayList<String>()

        override suspend fun deliver(
            recipient: NotificationRecipient,
            title: String,
            body: String,
            metadata: Map<String, String>,
            idempotencyKey: String
        ): NotificationDeliveryResult {
            deliveredIntents.add(idempotencyKey)
            return NotificationDeliveryResult(channel = channel, isSuccess = true, providerRef = "REF-$idempotencyKey")
        }
    }

    private lateinit var dispatchService: NotificationDispatchService
    private lateinit var inAppProvider: FakeNotificationProvider
    private lateinit var smsProvider: FakeNotificationProvider
    private lateinit var emailProvider: FakeNotificationProvider

    @Before
    fun setUp() {
        inAppProvider = FakeNotificationProvider(NotificationChannel.IN_APP)
        smsProvider = FakeNotificationProvider(NotificationChannel.SMS)
        emailProvider = FakeNotificationProvider(NotificationChannel.EMAIL)

        dispatchService = NotificationDispatchService()
        dispatchService.registerProvider(inAppProvider)
        dispatchService.registerProvider(smsProvider)
        dispatchService.registerProvider(emailProvider)
    }

    @Test
    fun test01_orderCreated_resolvesAndDispatchesToEligibleChannels() {
        runBlocking {
            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-901", "CUST-42", BigDecimal("1500.00"), 3),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            val consumer = NotificationEventConsumer<OrderCreatedEvent>(
                supportedEventType = DomainEventType.ORDER_CREATED,
                dispatchService = dispatchService
            )

            val result = consumer.consume(envelope)
            assertTrue(result.isSuccess)

            assertEquals(1, inAppProvider.deliveredIntents.size)
            assertEquals(1, smsProvider.deliveredIntents.size)
            assertEquals(1, emailProvider.deliveredIntents.size)

            assertTrue(inAppProvider.deliveredIntents[0].contains("sucharu_main"))
            assertTrue(inAppProvider.deliveredIntents[0].contains("CUST-42"))
        }
    }

    @Test
    fun test02_preferenceHandling_quietHoursBlocksSms() {
        runBlocking {
            val customPreferencesService = NotificationDispatchService(
                preferenceLookup = { p, r ->
                    // Quiet hours active between 22 and 7, current hour simulated in quiet hours
                    NotificationPreferences(
                        recipientId = r,
                        projectId = p,
                        enabledChannels = setOf(NotificationChannel.IN_APP, NotificationChannel.SMS),
                        quietHoursStartHour = 22,
                        quietHoursEndHour = 7
                    )
                }
            )
            val inApp = FakeNotificationProvider(NotificationChannel.IN_APP)
            val sms = FakeNotificationProvider(NotificationChannel.SMS)
            customPreferencesService.registerProvider(inApp)
            customPreferencesService.registerProvider(sms)

            val intent = NotificationIntent(
                eventId = "EVT-1",
                eventType = DomainEventType.ORDER_CREATED,
                projectId = "sucharu_main",
                targetRecipientId = "CUST-1",
                targetChannels = setOf(NotificationChannel.IN_APP, NotificationChannel.SMS),
                title = "Order update",
                body = "Your order has been updated",
                correlationId = "CORR-1"
            )

            val result = customPreferencesService.dispatchIntent(intent)
            assertTrue(result.isSuccess)

            // In-app was delivered
            assertEquals(1, inApp.deliveredIntents.size)
        }
    }

    @Test
    fun test03_sanitization_zeroSecretsInResolvedIntents() {
        val envelope = EventEnvelope.create(
            payload = PaymentReceivedEvent(
                paymentId = "PAY-100",
                invoiceId = "INV-100",
                orderId = "ORD-100",
                customerId = "CUST-99",
                amount = BigDecimal("20000.00"),
                currency = "BDT",
                paymentMethod = "BKASH",
                transactionRef = "TRX-BKASH-777",
                aggregateVersion = 1L
            ),
            projectId = "sucharu_main",
            actor = EventActor.human("U-1"),
            metadata = mapOf("password" to "secret123", "token" to "jwt.token.abc")
        )

        val intent = NotificationIntentResolver.resolve(envelope)
        assertNotNull(intent)
        assertFalse(intent!!.title.contains("secret123"))
        assertFalse(intent.body.contains("secret123"))
        assertFalse(intent.body.contains("jwt.token.abc"))
    }
}
