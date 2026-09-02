package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.automation.*
import com.sucharu.sucharupro.domain.validation.communication.automation.CommunicationAutomationRecipientResolver
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Communication Automation persistence (Module 10 Step 08).
 */
interface CommunicationAutomationDataSource {

    // Rules
    fun observeRules(projectId: String): Flow<List<CommunicationAutomationRule>>
    suspend fun getRules(projectId: String): List<CommunicationAutomationRule>
    suspend fun getRuleById(projectId: String, ruleId: String): CommunicationAutomationRule?
    suspend fun saveRule(rule: CommunicationAutomationRule): CommunicationAutomationRule
    suspend fun deleteRule(projectId: String, ruleId: String): Boolean

    // Triggers
    suspend fun saveTrigger(trigger: CommunicationTriggerEvent): CommunicationTriggerEvent
    suspend fun getTriggerById(projectId: String, triggerId: String): CommunicationTriggerEvent?
    suspend fun getTriggerByIdempotencyKey(projectId: String, idempotencyKey: String): CommunicationTriggerEvent?

    // Executions
    fun observeExecutions(projectId: String): Flow<List<CommunicationAutomationExecution>>
    suspend fun getExecutions(projectId: String): List<CommunicationAutomationExecution>
    suspend fun getExecutionById(projectId: String, executionId: String): CommunicationAutomationExecution?
    suspend fun saveExecution(execution: CommunicationAutomationExecution): CommunicationAutomationExecution
    suspend fun updateExecution(execution: CommunicationAutomationExecution): CommunicationAutomationExecution

    // Activity Events / Audit Trail
    suspend fun recordActivity(event: CommunicationAutomationActivityEvent): CommunicationAutomationActivityEvent
    suspend fun getActivityEvents(projectId: String): List<CommunicationAutomationActivityEvent>
    fun observeActivityEvents(projectId: String): Flow<List<CommunicationAutomationActivityEvent>>

    // Candidate Recipients
    suspend fun getCandidateRecipients(projectId: String): List<CommunicationAutomationRecipientResolver.CandidateAutomationRecipient>
}
