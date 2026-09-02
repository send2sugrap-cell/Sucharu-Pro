package com.sucharu.sucharupro.data.repository.imposition

import com.sucharu.sucharupro.data.datasource.imposition.PrepressOrchestrationDataSource
import com.sucharu.sucharupro.domain.model.imposition.PrepressOrchestrationPlan
import com.sucharu.sucharupro.domain.model.imposition.PrepressPlanStatus
import com.sucharu.sucharupro.domain.repository.imposition.PrepressOrchestrationRepository

/**
 * Repository Implementation for Prepress Orchestration Plans.
 * Module 18 Step 06.
 */
class PrepressOrchestrationRepositoryImpl(
    private val dataSource: PrepressOrchestrationDataSource
) : PrepressOrchestrationRepository {

    override suspend fun savePlan(plan: PrepressOrchestrationPlan): PrepressOrchestrationPlan {
        return dataSource.savePlan(plan.tenantId, plan)
    }

    override suspend fun getPlanById(tenantId: String, planId: String): PrepressOrchestrationPlan? {
        return dataSource.findById(tenantId, planId)
    }

    override suspend fun listPlansByJob(tenantId: String, jobId: String): List<PrepressOrchestrationPlan> {
        return dataSource.findByJobId(tenantId, jobId)
    }

    override suspend fun listPlansByOrder(tenantId: String, orderId: String): List<PrepressOrchestrationPlan> {
        return dataSource.findByOrderId(tenantId, orderId)
    }

    override suspend fun listAllPlans(tenantId: String, limit: Int): List<PrepressOrchestrationPlan> {
        return dataSource.listAll(tenantId, limit)
    }

    override suspend fun updatePlanStatus(
        tenantId: String,
        planId: String,
        newStatus: PrepressPlanStatus,
        actor: String,
        notes: String?
    ): Boolean {
        return dataSource.updateStatus(tenantId, planId, newStatus, actor, notes)
    }
}
