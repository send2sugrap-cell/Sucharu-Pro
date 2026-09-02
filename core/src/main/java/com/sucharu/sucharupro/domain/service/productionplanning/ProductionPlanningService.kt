package com.sucharu.sucharupro.domain.service.productionplanning

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.productionplanning.*

interface ProductionPlanningService {
    suspend fun evaluateReadiness(tenantId: String, orderId: String, orderItemId: String? = null): DomainResult<ManufacturingReadinessEvaluation>
    suspend fun createPlanningSnapshot(tenantId: String, orderId: String, orderItemId: String? = null, requestedBy: String, idempotencyKey: String? = null): DomainResult<ProductionPlanningSnapshot>
    suspend fun getPlanningSnapshot(tenantId: String, planningId: String): DomainResult<ProductionPlanningSnapshot?>
    suspend fun getLatestPlanningSnapshotByOrder(tenantId: String, orderId: String): DomainResult<ProductionPlanningSnapshot?>
    suspend fun listPlanningSnapshots(tenantId: String, orderId: String): DomainResult<List<ProductionPlanningSnapshot>>
    suspend fun reconcilePlanning(tenantId: String, planningId: String): DomainResult<ProductionPlanningReconciliationResult>
    suspend fun supersedePlanning(tenantId: String, planningId: String, reason: String, supersededBy: String): DomainResult<ProductionPlanningSnapshot>
    suspend fun handoffPlanning(tenantId: String, planningId: String, handedOffBy: String): DomainResult<ProductionPlanningSnapshot>
    suspend fun exportHandoffContract(tenantId: String, planningId: String): DomainResult<Module17Step04ProductionPlanningHandoffContract>
    suspend fun listPlanningEvents(tenantId: String, planningId: String): DomainResult<List<ProductionPlanningEvent>>
}
