package com.sucharu.sucharupro.data.repository.productionplanning

import com.sucharu.sucharupro.data.datasource.productionplanning.ProductionPlanningDataSource
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningEvent
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningSnapshot
import com.sucharu.sucharupro.domain.repository.productionplanning.ProductionPlanningRepository

class ProductionPlanningRepositoryImpl(
    private val dataSource: ProductionPlanningDataSource
) : ProductionPlanningRepository {

    override suspend fun savePlanningSnapshot(
        snapshot: ProductionPlanningSnapshot,
        idempotencyKey: String?
    ): ProductionPlanningSnapshot {
        return dataSource.savePlanningSnapshot(snapshot, idempotencyKey)
    }

    override suspend fun getPlanningSnapshotById(
        tenantId: String,
        planningId: String
    ): ProductionPlanningSnapshot? {
        return dataSource.getPlanningSnapshotById(tenantId, planningId)
    }

    override suspend fun getLatestPlanningSnapshotByOrder(
        tenantId: String,
        orderId: String
    ): ProductionPlanningSnapshot? {
        return dataSource.getLatestPlanningSnapshotByOrder(tenantId, orderId)
    }

    override suspend fun getPlanningSnapshotByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): ProductionPlanningSnapshot? {
        return dataSource.getPlanningSnapshotByIdempotencyKey(tenantId, idempotencyKey)
    }

    override suspend fun listPlanningSnapshotsByOrder(
        tenantId: String,
        orderId: String
    ): List<ProductionPlanningSnapshot> {
        return dataSource.listPlanningSnapshotsByOrder(tenantId, orderId)
    }

    override suspend fun savePlanningEvent(
        event: ProductionPlanningEvent
    ): ProductionPlanningEvent {
        return dataSource.savePlanningEvent(event)
    }

    override suspend fun listPlanningEvents(
        tenantId: String,
        planningId: String
    ): List<ProductionPlanningEvent> {
        return dataSource.listPlanningEvents(tenantId, planningId)
    }
}
