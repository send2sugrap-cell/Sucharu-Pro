package com.sucharu.sucharupro.data.datasource.imposition

import com.sucharu.sucharupro.domain.model.imposition.PrepressOrchestrationPlan
import com.sucharu.sucharupro.domain.model.imposition.PrepressPlanStatus

/**
 * Data Source Interface for Prepress Orchestration Plans.
 * Module 18 Step 06.
 */
interface PrepressOrchestrationDataSource {
    suspend fun savePlan(tenantId: String, plan: PrepressOrchestrationPlan): PrepressOrchestrationPlan
    suspend fun findById(tenantId: String, planId: String): PrepressOrchestrationPlan?
    suspend fun findByJobId(tenantId: String, jobId: String): List<PrepressOrchestrationPlan>
    suspend fun findByOrderId(tenantId: String, orderId: String): List<PrepressOrchestrationPlan>
    suspend fun listAll(tenantId: String, limit: Int = 50): List<PrepressOrchestrationPlan>
    suspend fun updateStatus(tenantId: String, planId: String, newStatus: PrepressPlanStatus, actor: String, notes: String? = null): Boolean
    suspend fun recordAudit(tenantId: String, planId: String, action: String, previousStatus: String?, newStatus: String, actor: String, reason: String?): Boolean
}
