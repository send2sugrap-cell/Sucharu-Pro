package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.postgres.IntegrationDeliveryRecord
import com.sucharu.sucharupro.data.event.postgres.PostgresIntegrationDeliveryRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.orchestration.ConsumerSubscription
import com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerRegistry
import com.sucharu.sucharupro.domain.event.consumer.orchestration.IntegrationType
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IntegrationSecurityTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var deliveryRepo: PostgresIntegrationDeliveryRepository
    private val tenantA = TenantContext("tenant_alpha")
    private val tenantB = TenantContext("tenant_beta")

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        deliveryRepo = PostgresIntegrationDeliveryRepository(mockDb)
    }

    @Test
    fun test01_crossTenantDeliveryRecord_isStrictlyIsolated() {
        runBlocking {
            val recordA = IntegrationDeliveryRecord(
                projectId = "tenant_alpha",
                eventId = "EVT-100",
                consumerId = "n8n.order_created",
                integrationType = IntegrationType.N8N,
                destination = "https://automation.sucharu.internal",
                correlationId = "CORR-100"
            )

            deliveryRepo.recordDeliveryAttempt(recordA, tenantA)

            // Tenant A finds the record
            val fetchedA = deliveryRepo.getByConsumerAndEvent("n8n.order_created", "EVT-100", tenantA)
            assertNotNull(fetchedA)
            assertEquals("tenant_alpha", fetchedA?.projectId)

            // Tenant B cannot access Tenant A's record
            val fetchedB = deliveryRepo.getByConsumerAndEvent("n8n.order_created", "EVT-100", tenantB)
            assertNull(fetchedB)
        }
    }

    @Test
    fun test02_crossTenantRecordInsert_throwsException() {
        runBlocking {
            val recordA = IntegrationDeliveryRecord(
                projectId = "tenant_alpha",
                eventId = "EVT-100",
                consumerId = "n8n.order_created",
                integrationType = IntegrationType.N8N,
                destination = "https://automation.sucharu.internal",
                correlationId = "CORR-100"
            )

            // Attempting to record Tenant A's delivery in Tenant B context fails
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    deliveryRepo.recordDeliveryAttempt(recordA, tenantB)
                }
            }
        }
    }

    @Test
    fun test03_blankConsumerIdOrVersion_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ConsumerSubscription(
                consumerId = "",
                supportedEventType = DomainEventType.ORDER_CREATED,
                supportedVersion = "v1"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ConsumerSubscription(
                consumerId = "valid.consumer",
                supportedEventType = DomainEventType.ORDER_CREATED,
                supportedVersion = ""
            )
        }
    }
}
