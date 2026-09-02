package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.automation.*
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.validation.communication.automation.CommunicationAutomationRecipientResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production-grade in-memory thread-safe implementation of [CommunicationAutomationDataSource] (Module 10 Step 08).
 */
class FakeCommunicationAutomationDataSource : CommunicationAutomationDataSource {

    private val mutex = Mutex()

    private val rulesState = MutableStateFlow<Map<String, CommunicationAutomationRule>>(emptyMap())
    private val triggersState = MutableStateFlow<Map<String, CommunicationTriggerEvent>>(emptyMap())
    private val executionsState = MutableStateFlow<Map<String, CommunicationAutomationExecution>>(emptyMap())
    private val activityEventsState = MutableStateFlow<List<CommunicationAutomationActivityEvent>>(emptyList())

    init {
        seedInitialRules()
    }

    private fun seedInitialRules() {
        val now = System.currentTimeMillis()

        // Rule 1: Order Ready Notification
        val rule1 = CommunicationAutomationRule(
            ruleId = "aut-001",
            ruleNo = "AUT-2026-00001",
            projectId = "default-project",
            name = "Order Ready Customer Alert",
            description = "Automatically sends an In-App & SMS alert to customer when an order stage becomes READY.",
            eventType = CommunicationAutomationEventType.ORDER_STATUS_CHANGED,
            conditions = listOf(
                CommunicationAutomationCondition(field = "newStatus", operator = ConditionOperator.EQUALS, expectedValue = "READY")
            ),
            audienceType = CampaignAudienceType.CUSTOMER_SEGMENT,
            notificationType = NotificationType.ORDER_STATUS_CHANGED,
            defaultChannel = NotificationChannel.IN_APP,
            priority = NotificationPriority.HIGH,
            titleTemplate = "Your Order #{sourceEntityId} is Ready for Pickup",
            messageTemplate = "Great news! Your print order #{sourceEntityId} has completed production and is ready for delivery/pickup.",
            enabled = true,
            cooldownPolicy = CommunicationCooldownPolicy(enabled = true, cooldownPeriodMs = 1800000L),
            createdBy = "user-admin-01",
            createdAt = now - 86400000L * 5,
            updatedAt = now - 86400000L * 5
        )

        // Rule 2: Overdue Payment Escalation
        val rule2 = CommunicationAutomationRule(
            ruleId = "aut-002",
            ruleNo = "AUT-2026-00002",
            projectId = "default-project",
            name = "Overdue Invoice Escalation",
            description = "Alerts accounts and manager when an invoice remains unpaid for more than 15 days.",
            eventType = CommunicationAutomationEventType.PAYMENT_OVERDUE,
            conditions = listOf(
                CommunicationAutomationCondition(field = "overdueDays", operator = ConditionOperator.GREATER_OR_EQUAL, expectedValue = "15")
            ),
            audienceType = CampaignAudienceType.ROLE,
            notificationType = NotificationType.PAYMENT_OVERDUE,
            defaultChannel = NotificationChannel.IN_APP,
            priority = NotificationPriority.URGENT,
            titleTemplate = "Overdue Payment Alert: Invoice #{sourceEntityId}",
            messageTemplate = "Invoice #{sourceEntityId} is overdue by {overdueDays} days. Total amount pending: BDT {amount}.",
            enabled = true,
            escalationPolicy = CommunicationEscalationPolicy(enabled = true, timeoutMs = 43200000L),
            createdBy = "user-admin-01",
            createdAt = now - 86400000L * 3,
            updatedAt = now - 86400000L * 3
        )

        // Rule 3: Vendor Document Expiring
        val rule3 = CommunicationAutomationRule(
            ruleId = "aut-003",
            ruleNo = "AUT-2026-00003",
            projectId = "default-project",
            name = "Vendor Compliance Expiry Warning",
            description = "Warns vendor and procurement team 7 days prior to trade license or tax certificate expiry.",
            eventType = CommunicationAutomationEventType.VENDOR_DOCUMENT_EXPIRING,
            conditions = listOf(
                CommunicationAutomationCondition(field = "daysToExpiry", operator = ConditionOperator.LESS_OR_EQUAL, expectedValue = "7")
            ),
            audienceType = CampaignAudienceType.VENDOR_SEGMENT,
            notificationType = NotificationType.GENERAL,
            defaultChannel = NotificationChannel.IN_APP,
            priority = NotificationPriority.NORMAL,
            titleTemplate = "Compliance Document Expiring: {documentType}",
            messageTemplate = "Your vendor compliance document '{documentType}' will expire in {daysToExpiry} days. Please upload a renewed copy.",
            enabled = true,
            createdBy = "user-admin-01",
            createdAt = now - 86400000L,
            updatedAt = now - 86400000L
        )

        rulesState.value = mapOf(
            rule1.ruleId to rule1,
            rule2.ruleId to rule2,
            rule3.ruleId to rule3
        )
    }

    override fun observeRules(projectId: String): Flow<List<CommunicationAutomationRule>> {
        return rulesState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getRules(projectId: String): List<CommunicationAutomationRule> = mutex.withLock {
        rulesState.value.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
    }

    override suspend fun getRuleById(projectId: String, ruleId: String): CommunicationAutomationRule? = mutex.withLock {
        val r = rulesState.value[ruleId]
        if (r?.projectId == projectId) r else null
    }

    override suspend fun saveRule(rule: CommunicationAutomationRule): CommunicationAutomationRule = mutex.withLock {
        val current = rulesState.value.toMutableMap()
        current[rule.ruleId] = rule
        rulesState.value = current
        rule
    }

    override suspend fun deleteRule(projectId: String, ruleId: String): Boolean = mutex.withLock {
        val current = rulesState.value.toMutableMap()
        val existing = current[ruleId]
        if (existing != null && existing.projectId == projectId) {
            current.remove(ruleId)
            rulesState.value = current
            true
        } else {
            false
        }
    }

    override suspend fun saveTrigger(trigger: CommunicationTriggerEvent): CommunicationTriggerEvent = mutex.withLock {
        val current = triggersState.value.toMutableMap()
        current[trigger.triggerId] = trigger
        triggersState.value = current
        trigger
    }

    override suspend fun getTriggerById(projectId: String, triggerId: String): CommunicationTriggerEvent? = mutex.withLock {
        val t = triggersState.value[triggerId]
        if (t?.projectId == projectId) t else null
    }

    override suspend fun getTriggerByIdempotencyKey(projectId: String, idempotencyKey: String): CommunicationTriggerEvent? = mutex.withLock {
        triggersState.value.values.firstOrNull { it.projectId == projectId && it.idempotencyKey == idempotencyKey }
    }

    override fun observeExecutions(projectId: String): Flow<List<CommunicationAutomationExecution>> {
        return executionsState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getExecutions(projectId: String): List<CommunicationAutomationExecution> = mutex.withLock {
        executionsState.value.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
    }

    override suspend fun getExecutionById(projectId: String, executionId: String): CommunicationAutomationExecution? = mutex.withLock {
        val e = executionsState.value[executionId]
        if (e?.projectId == projectId) e else null
    }

    override suspend fun saveExecution(execution: CommunicationAutomationExecution): CommunicationAutomationExecution = mutex.withLock {
        val current = executionsState.value.toMutableMap()
        current[execution.executionId] = execution
        executionsState.value = current
        execution
    }

    override suspend fun updateExecution(execution: CommunicationAutomationExecution): CommunicationAutomationExecution = mutex.withLock {
        val current = executionsState.value.toMutableMap()
        current[execution.executionId] = execution
        executionsState.value = current
        execution
    }

    override suspend fun recordActivity(event: CommunicationAutomationActivityEvent): CommunicationAutomationActivityEvent = mutex.withLock {
        val current = activityEventsState.value.toMutableList()
        current.add(event)
        activityEventsState.value = current
        event
    }

    override suspend fun getActivityEvents(projectId: String): List<CommunicationAutomationActivityEvent> = mutex.withLock {
        activityEventsState.value.filter { it.projectId == projectId }.sortedByDescending { it.timestamp }
    }

    override fun observeActivityEvents(projectId: String): Flow<List<CommunicationAutomationActivityEvent>> {
        return activityEventsState.map { list ->
            list.filter { it.projectId == projectId }.sortedByDescending { it.timestamp }
        }
    }

    override suspend fun getCandidateRecipients(projectId: String): List<CommunicationAutomationRecipientResolver.CandidateAutomationRecipient> {
        return listOf(
            CommunicationAutomationRecipientResolver.CandidateAutomationRecipient(
                projectId = projectId,
                userId = "user-cus-001",
                entityType = "CUSTOMER",
                entityId = "cus-001",
                role = "CUSTOMER",
                isActive = true
            ),
            CommunicationAutomationRecipientResolver.CandidateAutomationRecipient(
                projectId = projectId,
                userId = "user-ven-001",
                entityType = "VENDOR",
                entityId = "ven-001",
                role = "VENDOR",
                isActive = true
            ),
            CommunicationAutomationRecipientResolver.CandidateAutomationRecipient(
                projectId = projectId,
                userId = "user-staff-01",
                entityType = "STAFF",
                entityId = "user-staff-01",
                role = "STAFF",
                departmentId = "dept-prod",
                teamId = "team-press-01",
                isActive = true
            ),
            CommunicationAutomationRecipientResolver.CandidateAutomationRecipient(
                projectId = projectId,
                userId = "user-qc-01",
                entityType = "STAFF",
                entityId = "user-qc-01",
                role = "QC_INSPECTOR",
                departmentId = "dept-qc",
                teamId = "team-qc-01",
                isActive = true
            ),
            CommunicationAutomationRecipientResolver.CandidateAutomationRecipient(
                projectId = projectId,
                userId = "user-accounts-01",
                entityType = "STAFF",
                entityId = "user-accounts-01",
                role = "ACCOUNTS",
                departmentId = "dept-accounts",
                isActive = true
            ),
            CommunicationAutomationRecipientResolver.CandidateAutomationRecipient(
                projectId = projectId,
                userId = "user-manager-01",
                entityType = "USER",
                entityId = "user-manager-01",
                role = "MANAGER",
                isActive = true
            ),
            CommunicationAutomationRecipientResolver.CandidateAutomationRecipient(
                projectId = projectId,
                userId = "user-admin-01",
                entityType = "USER",
                entityId = "user-admin-01",
                role = "ADMIN",
                isActive = true
            )
        )
    }
}
