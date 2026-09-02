package com.sucharu.sucharupro.domain.repository.imposition

import com.sucharu.sucharupro.domain.model.imposition.PrepressOrchestrationPlan
import com.sucharu.sucharupro.domain.model.imposition.PrepressPlanStatus

/**
 * Domain Repository Interface for Prepress Orchestration Master Plans.
 * Module 18 Step 06.
 */
interface PrepressOrchestrationRepository {
    suspend fun savePlan(plan: PrepressOrchestrationPlan): PrepressOrchestrationPlan
    suspend fun getPlanById(tenantId: String, planId: String): PrepressOrchestrationPlan?
    suspend fun listPlansByJob(tenantId: String, jobId: String): List<PrepressOrchestrationPlan>
    suspend fun listPlansByOrder(tenantId: String, orderId: String): List<PrepressOrchestrationPlan>
    suspend fun listAllPlans(tenantId: String, limit: Int = 50): List<PrepressOrchestrationPlan>
    suspend fun updatePlanStatus(tenantId: String, planId: String, newStatus: PrepressPlanStatus, actor: String, notes: String? = null): Boolean
}
