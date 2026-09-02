package com.sucharu.sucharupro.domain.repository.communication.automation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.automation.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Communication Automation, Event Triggers & Orchestration (Module 10 Step 08).
 */
interface CommunicationAutomationRepository {

    // ─── Rules ───
    fun observeRules(projectId: String, callerRole: UserRole): Flow<List<CommunicationAutomationRule>>

    suspend fun getRules(
        projectId: String,
        eventType: CommunicationAutomationEventType? = null,
        enabledOnly: Boolean = false,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CommunicationAutomationRule>>

    suspend fun getRuleById(
        projectId: String,
        ruleId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CommunicationAutomationRule>

    suspend fun createRule(
        rule: CommunicationAutomationRule,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CommunicationAutomationRule>

    suspend fun updateRule(
        rule: CommunicationAutomationRule,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CommunicationAutomationRule>

    suspend fun toggleRuleStatus(
        projectId: String,
        ruleId: String,
        enabled: Boolean,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CommunicationAutomationRule>

    suspend fun deleteRule(
        projectId: String,
        ruleId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Boolean>

    // ─── Trigger Processing & Execution ───
    suspend fun processTrigger(
        trigger: CommunicationTriggerEvent,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CommunicationAutomationExecution>>

    fun observeExecutions(projectId: String, callerRole: UserRole): Flow<List<CommunicationAutomationExecution>>

    suspend fun getExecutions(
        projectId: String,
        ruleId: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CommunicationAutomationExecution>>

    // ─── Summary & Analytics ───
    suspend fun getProjectSummary(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CommunicationAutomationSummary>

    // ─── Audit Trail ───
    fun observeActivityEvents(projectId: String, callerRole: UserRole): Flow<List<CommunicationAutomationActivityEvent>>

    suspend fun getActivityEvents(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CommunicationAutomationActivityEvent>>
}
