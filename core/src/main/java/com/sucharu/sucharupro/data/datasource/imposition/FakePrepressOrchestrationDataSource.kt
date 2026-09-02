package com.sucharu.sucharupro.data.datasource.imposition

import com.sucharu.sucharupro.domain.model.imposition.PrepressOrchestrationPlan
import com.sucharu.sucharupro.domain.model.imposition.PrepressPlanStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * In-Memory Multi-Tenant Fake Data Source for Prepress Orchestration Plans.
 * Module 18 Step 06.
 */
class FakePrepressOrchestrationDataSource : PrepressOrchestrationDataSource {

    // tenantId -> (planId -> PrepressOrchestrationPlan)
    private val store = ConcurrentHashMap<String, ConcurrentHashMap<String, PrepressOrchestrationPlan>>()

    // tenantId -> list of audit records
    private val audits = ConcurrentHashMap<String, MutableList<Map<String, Any?>>>()

    override suspend fun savePlan(tenantId: String, plan: PrepressOrchestrationPlan): PrepressOrchestrationPlan {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(plan.tenantId == tenantId) { "Plan tenant ID mismatch." }

        val tenantStore = store.computeIfAbsent(tenantId) { ConcurrentHashMap() }
        tenantStore[plan.planId] = plan
        return plan
    }

    override suspend fun findById(tenantId: String, planId: String): PrepressOrchestrationPlan? {
        if (tenantId.isBlank()) return null
        return store[tenantId]?.get(planId)
    }

    override suspend fun findByJobId(tenantId: String, jobId: String): List<PrepressOrchestrationPlan> {
        if (tenantId.isBlank() || jobId.isBlank()) return emptyList()
        return store[tenantId]?.values?.filter { it.jobId == jobId }?.sortedByDescending { it.createdAt } ?: emptyList()
    }

    override suspend fun findByOrderId(tenantId: String, orderId: String): List<PrepressOrchestrationPlan> {
        if (tenantId.isBlank() || orderId.isBlank()) return emptyList()
        return store[tenantId]?.values?.filter { it.orderId == orderId }?.sortedByDescending { it.createdAt } ?: emptyList()
    }

    override suspend fun listAll(tenantId: String, limit: Int): List<PrepressOrchestrationPlan> {
        if (tenantId.isBlank()) return emptyList()
        return store[tenantId]?.values?.sortedByDescending { it.createdAt }?.take(limit) ?: emptyList()
    }

    override suspend fun updateStatus(
        tenantId: String,
        planId: String,
        newStatus: PrepressPlanStatus,
        actor: String,
        notes: String?
    ): Boolean {
        val existing = findById(tenantId, planId) ?: return false
        val updated = existing.copy(
            status = newStatus,
            approvalStatus = if (newStatus == PrepressPlanStatus.APPROVED) "APPROVED" else existing.approvalStatus,
            approvedBy = if (newStatus == PrepressPlanStatus.APPROVED) actor else existing.approvedBy,
            approvedAt = if (newStatus == PrepressPlanStatus.APPROVED) System.currentTimeMillis() else existing.approvedAt,
            notes = notes ?: existing.notes
        )
        store[tenantId]?.put(planId, updated)
        recordAudit(tenantId, planId, "STATUS_UPDATE", existing.status.name, newStatus.name, actor, notes)
        return true
    }

    override suspend fun recordAudit(
        tenantId: String,
        planId: String,
        action: String,
        previousStatus: String?,
        newStatus: String,
        actor: String,
        reason: String?
    ): Boolean {
        val list = audits.computeIfAbsent(tenantId) { mutableListOf() }
        list.add(
            mapOf(
                "auditId" to java.util.UUID.randomUUID().toString(),
                "planId" to planId,
                "tenantId" to tenantId,
                "action" to action,
                "previousStatus" to previousStatus,
                "newStatus" to newStatus,
                "actor" to actor,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis()
            )
        )
        return true
    }

    fun clear() {
        store.clear()
        audits.clear()
    }
}
