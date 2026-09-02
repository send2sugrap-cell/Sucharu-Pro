package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationExecution
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationRule

/**
 * Evaluates whether an unhandled or critical execution requires escalation (Module 10 Step 08).
 */
object CommunicationEscalationEngine {

    fun shouldEscalate(
        execution: CommunicationAutomationExecution,
        rule: CommunicationAutomationRule,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        if (!rule.escalationPolicy.enabled) return false

        val elapsed = currentTime - execution.evaluatedAt
        return elapsed >= rule.escalationPolicy.timeoutMs
    }
}
