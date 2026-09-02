package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.communication.automation.*
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.notification.NotificationPreference
import org.junit.Assert.*
import org.junit.Test

class CommunicationAutomationRuleEngineTest {

    private fun sampleRule(cooldownMs: Long = 0) = CommunicationAutomationRule(
        ruleId = "rule-1",
        ruleNo = "AUT-001",
        projectId = "proj-1",
        name = "Test Rule",
        eventType = CommunicationAutomationEventType.ORDER_STATUS_CHANGED,
        conditions = listOf(
            CommunicationAutomationCondition("status", ConditionOperator.EQUALS, "READY")
        ),
        audienceType = CampaignAudienceType.CUSTOMER_SEGMENT,
        notificationType = NotificationType.ORDER_STATUS_CHANGED,
        defaultChannel = NotificationChannel.IN_APP,
        priority = NotificationPriority.NORMAL,
        titleTemplate = "Order #{sourceEntityId} Update",
        messageTemplate = "Status is {status}",
        enabled = true,
        cooldownPolicy = CommunicationCooldownPolicy(enabled = cooldownMs > 0, cooldownPeriodMs = cooldownMs),
        createdBy = "admin"
    )

    private fun sampleTrigger() = CommunicationTriggerEvent(
        triggerId = "trg-1",
        projectId = "proj-1",
        eventType = CommunicationAutomationEventType.ORDER_STATUS_CHANGED,
        sourceEntityType = "CUSTOMER",
        sourceEntityId = "cus-1",
        actorUserId = "sys",
        payloadMetadata = mapOf("status" to "READY")
    )

    private fun sampleCandidate() = CommunicationAutomationRecipientResolver.CandidateAutomationRecipient(
        projectId = "proj-1",
        userId = "cus-1",
        entityType = "CUSTOMER",
        entityId = "cus-1",
        isActive = true
    )

    @Test
    fun evaluate_matchingRuleAndCondition_returnsSendDecision() {
        val decisions = CommunicationAutomationRuleEngine.evaluate(
            trigger = sampleTrigger(),
            rules = listOf(sampleRule()),
            candidates = listOf(sampleCandidate()),
            recentExecutions = emptyList()
        )
        assertEquals(1, decisions.size)
        assertEquals(AutomationDecisionType.SEND, decisions.first().decisionType)
        assertEquals("cus-1", decisions.first().recipientUserId)
        assertEquals("Order #cus-1 Update", decisions.first().renderedTitle)
        assertEquals("Status is READY", decisions.first().renderedMessage)
    }

    @Test
    fun evaluate_conditionNotMet_returnsSkipDecision() {
        val trigger = sampleTrigger().copy(payloadMetadata = mapOf("status" to "IN_PROGRESS"))
        val decisions = CommunicationAutomationRuleEngine.evaluate(
            trigger = trigger,
            rules = listOf(sampleRule()),
            candidates = listOf(sampleCandidate()),
            recentExecutions = emptyList()
        )
        assertEquals(1, decisions.size)
        assertEquals(AutomationDecisionType.SKIP, decisions.first().decisionType)
    }

    @Test
    fun evaluate_coolingDown_returnsSuppressDecision() {
        val currentTime = 1000000L
        val rule = sampleRule(cooldownMs = 60000L) // 1 minute cooldown
        val recentExec = CommunicationAutomationExecution(
            executionId = "exec-1",
            executionNo = "EXE-1",
            projectId = "proj-1",
            triggerId = "trg-old",
            ruleId = rule.ruleId,
            status = AutomationExecutionStatus.COMPLETED,
            decision = CommunicationAutomationDecision(AutomationDecisionType.SEND),
            recipientUserId = "cus-1",
            evaluatedAt = currentTime - 30000L // 30 seconds ago
        )

        val decisions = CommunicationAutomationRuleEngine.evaluate(
            trigger = sampleTrigger(),
            rules = listOf(rule),
            candidates = listOf(sampleCandidate()),
            recentExecutions = listOf(recentExec),
            currentTime = currentTime
        )
        assertEquals(1, decisions.size)
        assertEquals(AutomationDecisionType.SUPPRESS, decisions.first().decisionType)
    }

    @Test
    fun evaluate_userPreferenceDisabled_returnsPreferenceBlockedDecision() {
        val prefs = listOf(
            NotificationPreference(
                preferenceId = "pref-1",
                projectId = "proj-1",
                userId = "cus-1",
                notificationType = NotificationType.ORDER_STATUS_CHANGED,
                channel = NotificationChannel.IN_APP,
                enabled = false
            )
        )
        val decisions = CommunicationAutomationRuleEngine.evaluate(
            trigger = sampleTrigger(),
            rules = listOf(sampleRule()),
            candidates = listOf(sampleCandidate()),
            recentExecutions = emptyList(),
            userPreferences = prefs
        )
        assertEquals(1, decisions.size)
        assertEquals(AutomationDecisionType.PREFERENCE_BLOCKED, decisions.first().decisionType)
    }
}
