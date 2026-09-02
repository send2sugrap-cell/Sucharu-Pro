package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.communication.automation.*
import com.sucharu.sucharupro.domain.model.notification.NotificationPreference

/**
 * Deterministic Rule Engine for evaluating domain triggers and generating automation decisions (Module 10 Step 08).
 */
object CommunicationAutomationRuleEngine {

    fun evaluate(
        trigger: CommunicationTriggerEvent,
        rules: List<CommunicationAutomationRule>,
        candidates: List<CommunicationAutomationRecipientResolver.CandidateAutomationRecipient>,
        recentExecutions: List<CommunicationAutomationExecution>,
        userPreferences: List<NotificationPreference> = emptyList(),
        currentTime: Long = System.currentTimeMillis()
    ): List<CommunicationAutomationDecision> {
        // 1. Filter enabled rules matching projectId and eventType
        val matchingRules = rules.filter {
            it.projectId == trigger.projectId &&
            it.enabled &&
            it.eventType == trigger.eventType
        }

        if (matchingRules.isEmpty()) {
            return listOf(
                CommunicationAutomationDecision(
                    decisionType = AutomationDecisionType.NO_MATCH,
                    suppressionReason = "No active automation rule matched event '${trigger.eventType.name}'."
                )
            )
        }

        val decisions = mutableListOf<CommunicationAutomationDecision>()

        for (rule in matchingRules) {
            // 2. Evaluate all conditions
            val allConditionsMet = rule.conditions.all { cond ->
                val actualValue = trigger.payloadMetadata[cond.field]
                cond.evaluate(actualValue)
            }

            if (!allConditionsMet) {
                decisions.add(
                    CommunicationAutomationDecision(
                        decisionType = AutomationDecisionType.SKIP,
                        matchedRuleId = rule.ruleId,
                        suppressionReason = "Trigger payload did not satisfy rule conditions."
                    )
                )
                continue
            }

            // 3. Resolve target recipients
            val recipients = CommunicationAutomationRecipientResolver.resolveRecipients(trigger, rule, candidates)
            if (recipients.isEmpty()) {
                decisions.add(
                    CommunicationAutomationDecision(
                        decisionType = AutomationDecisionType.INVALID_RECIPIENT,
                        matchedRuleId = rule.ruleId,
                        suppressionReason = "No eligible recipients resolved for rule audience '${rule.audienceType}'."
                    )
                )
                continue
            }

            for (recipientUserId in recipients) {
                // 4. Evaluate Cooldown / Anti-spam policy
                if (rule.cooldownPolicy.enabled) {
                    val cooldownMs = rule.cooldownPolicy.cooldownPeriodMs
                    val isCoolingDown = recentExecutions.any { exec ->
                        exec.ruleId == rule.ruleId &&
                        exec.recipientUserId == recipientUserId &&
                        (currentTime - exec.evaluatedAt) < cooldownMs
                    }

                    if (isCoolingDown) {
                        decisions.add(
                            CommunicationAutomationDecision(
                                decisionType = AutomationDecisionType.SUPPRESS,
                                matchedRuleId = rule.ruleId,
                                recipientUserId = recipientUserId,
                                suppressionReason = "Suppressed by cooldown policy (${cooldownMs / 1000}s window)."
                            )
                        )
                        continue
                    }
                }

                // 5. Evaluate User Preference (Mandatory alerts bypass preference)
                val channel = rule.defaultChannel
                val isMandatory = rule.notificationType.isMandatory
                val pref = userPreferences.firstOrNull {
                    it.userId == recipientUserId &&
                    it.notificationType == rule.notificationType &&
                    it.channel == channel
                }

                if (pref != null && !pref.enabled && !isMandatory) {
                    decisions.add(
                        CommunicationAutomationDecision(
                            decisionType = AutomationDecisionType.PREFERENCE_BLOCKED,
                            matchedRuleId = rule.ruleId,
                            recipientUserId = recipientUserId,
                            selectedChannel = channel,
                            suppressionReason = "User disabled notifications for '${rule.notificationType.defaultLabel}' on channel '$channel'."
                        )
                    )
                    continue
                }

                // 6. Template Rendering
                val renderedTitle = renderTemplate(rule.titleTemplate, trigger)
                val renderedMessage = renderTemplate(rule.messageTemplate, trigger)

                // 7. Scheduling policy
                val scheduleDecision = when (rule.schedulePolicy.type) {
                    SchedulePolicyType.IMMEDIATE -> AutomationDecisionType.SEND
                    SchedulePolicyType.DELAYED -> AutomationDecisionType.SCHEDULE
                    SchedulePolicyType.SCHEDULED -> AutomationDecisionType.SCHEDULE
                    SchedulePolicyType.BUSINESS_HOURS -> AutomationDecisionType.SCHEDULE
                    SchedulePolicyType.NEXT_WORKING_DAY -> AutomationDecisionType.SCHEDULE
                }

                val scheduledTime = if (scheduleDecision == AutomationDecisionType.SCHEDULE) {
                    rule.schedulePolicy.targetEpochTime ?: (currentTime + rule.schedulePolicy.delayMs)
                } else null

                decisions.add(
                    CommunicationAutomationDecision(
                        decisionType = scheduleDecision,
                        matchedRuleId = rule.ruleId,
                        recipientUserId = recipientUserId,
                        selectedChannel = channel,
                        calculatedPriority = rule.priority,
                        renderedTitle = renderedTitle,
                        renderedMessage = renderedMessage,
                        scheduledAt = scheduledTime
                    )
                )
            }
        }

        return decisions
    }

    private fun renderTemplate(template: String, trigger: CommunicationTriggerEvent): String {
        var result = template
            .replace("{sourceEntityType}", trigger.sourceEntityType)
            .replace("{sourceEntityId}", trigger.sourceEntityId)
            .replace("{eventType}", trigger.eventType.defaultLabel)

        for ((k, v) in trigger.payloadMetadata) {
            result = result.replace("{$k}", v)
        }
        return result
    }
}
