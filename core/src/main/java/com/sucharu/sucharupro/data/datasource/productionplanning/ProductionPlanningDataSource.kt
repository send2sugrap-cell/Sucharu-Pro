package com.sucharu.sucharupro.data.datasource.productionplanning

import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningEvent
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningSnapshot

interface ProductionPlanningDataSource {
    suspend fun savePlanningSnapshot(snapshot: ProductionPlanningSnapshot, idempotencyKey: String? = null): ProductionPlanningSnapshot
    suspend fun getPlanningSnapshotById(tenantId: String, planningId: String): ProductionPlanningSnapshot?
    suspend fun getLatestPlanningSnapshotByOrder(tenantId: String, orderId: String): ProductionPlanningSnapshot?
    suspend fun getPlanningSnapshotByIdempotencyKey(tenantId: String, idempotencyKey: String): ProductionPlanningSnapshot?
    suspend fun listPlanningSnapshotsByOrder(tenantId: String, orderId: String): List<ProductionPlanningSnapshot>
    suspend fun savePlanningEvent(event: ProductionPlanningEvent): ProductionPlanningEvent
    suspend fun listPlanningEvents(tenantId: String, planningId: String): List<ProductionPlanningEvent>
}
