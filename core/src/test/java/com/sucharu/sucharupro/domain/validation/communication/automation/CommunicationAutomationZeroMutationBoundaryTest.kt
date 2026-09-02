package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.data.datasource.FakeCommunicationAutomationDataSource
import com.sucharu.sucharupro.data.repository.CommunicationAutomationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationEventType
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationTriggerEvent
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

class CommunicationAutomationZeroMutationBoundaryTest {

    @Test
    fun boundaryCheck_noMutatingMethodsOnBusinessEntities() {
        val repoClass = CommunicationAutomationRepositoryImpl::class.java

        val methods = repoClass.declaredMethods

        // Ensure the automation repository does NOT have any methods capable of mutating Orders, Payments, Users, etc.
        val forbiddenKeywords = listOf("updateOrder", "saveCustomer", "deleteVendor", "modifyPayment")

        for (method in methods) {
            val name = method.name
            val hasForbiddenName = forbiddenKeywords.any { name.contains(it, ignoreCase = true) }
            assertTrue(
                "Violation of Zero-Mutation Boundary: Method $name suggests business entity mutation.",
                !hasForbiddenName
            )
        }
    }

    @Test
    fun boundaryCheck_triggerProcessing_doesNotMutateSourceEntity() = runBlocking {
        val dataSource = FakeCommunicationAutomationDataSource()
        val repo = CommunicationAutomationRepositoryImpl(dataSource)

        val trigger = CommunicationTriggerEvent(
            triggerId = "trg-1",
            projectId = "default-project",
            eventType = CommunicationAutomationEventType.ORDER_STATUS_CHANGED,
            sourceEntityType = "ORDER",
            sourceEntityId = "ord-001",
            actorUserId = "sys",
            payloadMetadata = mapOf("status" to "READY")
        )

        // The repository should only process the trigger and return executions.
        // It does not accept any source business entities as parameters to update them.
        val result = repo.processTrigger(trigger, "sys", UserRole.ADMIN)

        assertTrue(result is DomainResult.Success)

        // Reflection validation to ensure no hidden dependencies to business repositories
        val fields = repo.javaClass.declaredFields
        val hasOrderRepo = fields.any { it.type.simpleName.contains("OrderRepository") }
        val hasCustomerRepo = fields.any { it.type.simpleName.contains("CustomerRepository") }

        assertTrue("Automation Repository MUST NOT hold reference to OrderRepository", !hasOrderRepo)
        assertTrue("Automation Repository MUST NOT hold reference to CustomerRepository", !hasCustomerRepo)
    }
}
