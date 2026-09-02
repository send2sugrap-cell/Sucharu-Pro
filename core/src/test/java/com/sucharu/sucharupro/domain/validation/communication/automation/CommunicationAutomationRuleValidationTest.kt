package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationEventType
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationRule
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationCooldownPolicy
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationEscalationPolicy
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationAutomationRuleValidationTest {

    private fun validRule() = CommunicationAutomationRule(
        ruleId = "aut-test-01",
        ruleNo = "AUT-2026-00001",
        projectId = "proj-01",
        name = "Test Automation Rule",
        eventType = CommunicationAutomationEventType.ORDER_STATUS_CHANGED,
        titleTemplate = "Order #{sourceEntityId} Update",
        messageTemplate = "Order status has changed.",
        createdBy = "user-admin-01"
    )

    @Test
    fun validateRule_allValidFields_succeeds() {
        val result = CommunicationAutomationRuleValidator.validateRule(validRule())
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateRule_blankName_fails() {
        assertThrows(IllegalArgumentException::class.java) {
            validRule().copy(name = "   ")
        }
    }

    @Test
    fun validateRule_blankTitleTemplate_fails() {
        assertThrows(IllegalArgumentException::class.java) {
            validRule().copy(titleTemplate = "")
        }
    }

    @Test
    fun validateRule_blankMessageTemplate_fails() {
        assertThrows(IllegalArgumentException::class.java) {
            validRule().copy(messageTemplate = "   ")
        }
    }

    @Test
    fun validateRule_invalidCooldownPeriod_fails() {
        val rule = validRule().copy(
            cooldownPolicy = CommunicationCooldownPolicy(enabled = true, cooldownPeriodMs = 0L)
        )
        val result = CommunicationAutomationRuleValidator.validateRule(rule)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateRule_invalidEscalationTimeout_fails() {
        val rule = validRule().copy(
            escalationPolicy = CommunicationEscalationPolicy(enabled = true, timeoutMs = -500L)
        )
        val result = CommunicationAutomationRuleValidator.validateRule(rule)
        assertTrue(result is DomainResult.Error)
    }
}
