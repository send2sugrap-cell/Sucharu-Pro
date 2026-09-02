package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationRule

/**
 * Validates invariant rules and structural requirements for automation rule configurations (Module 10 Step 08).
 */
object CommunicationAutomationRuleValidator {

    data class RuleValidationError(val field: String, val message: String)

    fun validateRule(rule: CommunicationAutomationRule): DomainResult<Unit> {
        val errors = mutableListOf<RuleValidationError>()

        if (rule.ruleId.isBlank()) errors.add(RuleValidationError("ruleId", "Rule ID cannot be blank."))
        if (rule.ruleNo.isBlank()) errors.add(RuleValidationError("ruleNo", "Rule Number cannot be blank."))
        if (rule.projectId.isBlank()) errors.add(RuleValidationError("projectId", "Project ID cannot be blank."))
        if (rule.name.isBlank()) errors.add(RuleValidationError("name", "Rule name cannot be blank."))
        if (rule.name.length > 150) errors.add(RuleValidationError("name", "Rule name cannot exceed 150 characters."))
        if (rule.titleTemplate.isBlank()) errors.add(RuleValidationError("titleTemplate", "Title template cannot be blank."))
        if (rule.messageTemplate.isBlank()) errors.add(RuleValidationError("messageTemplate", "Message template cannot be blank."))
        if (rule.createdBy.isBlank()) errors.add(RuleValidationError("createdBy", "Creator User ID cannot be blank."))

        if (rule.cooldownPolicy.enabled && rule.cooldownPolicy.cooldownPeriodMs <= 0) {
            errors.add(RuleValidationError("cooldownPolicy", "Cooldown period must be positive when enabled."))
        }

        if (rule.escalationPolicy.enabled && rule.escalationPolicy.timeoutMs <= 0) {
            errors.add(RuleValidationError("escalationPolicy", "Escalation timeout must be positive when enabled."))
        }

        return if (errors.isEmpty()) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Automation rule validation failed: ${errors.joinToString { "${it.field}: ${it.message}" }}")
        }
    }
}
