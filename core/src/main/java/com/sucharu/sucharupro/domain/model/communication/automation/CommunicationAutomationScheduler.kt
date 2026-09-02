package com.sucharu.sucharupro.domain.model.communication.automation

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Provider-neutral interface for scheduling and processing automated communication triggers (Module 10 Step 08).
 */
interface CommunicationAutomationScheduler {
    suspend fun scheduleExecution(projectId: String, executionId: String, scheduledAt: Long): DomainResult<Unit>
    suspend fun cancelScheduledExecution(projectId: String, executionId: String): DomainResult<Unit>
    suspend fun processDueExecutions(projectId: String): DomainResult<List<String>>
}
