package com.sucharu.sucharupro.data.datasource.productionplanning

import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningEvent
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeProductionPlanningDataSource : ProductionPlanningDataSource {

    private val mutex = Mutex()
    private val snapshots = mutableListOf<ProductionPlanningSnapshot>()
    private val events = mutableListOf<ProductionPlanningEvent>()
    private val idempotencyMap = mutableMapOf<String, String>() // "tenant:key" -> planningId

    override suspend fun savePlanningSnapshot(
        snapshot: ProductionPlanningSnapshot,
        idempotencyKey: String?
    ): ProductionPlanningSnapshot = mutex.withLock {
        // Mark previous versions for this order as not current if new is current
        if (snapshot.isCurrent) {
            val updated = snapshots.map {
                if (it.tenantId == snapshot.tenantId && it.orderId == snapshot.orderId && it.planningId != snapshot.planningId) {
                    it.copy(isCurrent = false)
                } else {
                    it
                }
            }
            snapshots.clear()
            snapshots.addAll(updated)
        }

        val idx = snapshots.indexOfFirst { it.planningId == snapshot.planningId }
        if (idx >= 0) {
            snapshots[idx] = snapshot
        } else {
            snapshots.add(snapshot)
        }

        if (idempotencyKey != null) {
            idempotencyMap["${snapshot.tenantId}:$idempotencyKey"] = snapshot.planningId
        }

        snapshot
    }

    override suspend fun getPlanningSnapshotById(
        tenantId: String,
        planningId: String
    ): ProductionPlanningSnapshot? = mutex.withLock {
        snapshots.find { it.tenantId == tenantId && it.planningId == planningId }
    }

    override suspend fun getLatestPlanningSnapshotByOrder(
        tenantId: String,
        orderId: String
    ): ProductionPlanningSnapshot? = mutex.withLock {
        snapshots.filter { it.tenantId == tenantId && it.orderId == orderId }
            .maxByOrNull { it.version }
    }

    override suspend fun getPlanningSnapshotByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): ProductionPlanningSnapshot? = mutex.withLock {
        val planningId = idempotencyMap["$tenantId:$idempotencyKey"] ?: return@withLock null
        snapshots.find { it.tenantId == tenantId && it.planningId == planningId }
    }

    override suspend fun listPlanningSnapshotsByOrder(
        tenantId: String,
        orderId: String
    ): List<ProductionPlanningSnapshot> = mutex.withLock {
        snapshots.filter { it.tenantId == tenantId && it.orderId == orderId }
            .sortedByDescending { it.version }
    }

    override suspend fun savePlanningEvent(
        event: ProductionPlanningEvent
    ): ProductionPlanningEvent = mutex.withLock {
        events.add(event)
        event
    }

    override suspend fun listPlanningEvents(
        tenantId: String,
        planningId: String
    ): List<ProductionPlanningEvent> = mutex.withLock {
        events.filter { it.tenantId == tenantId && it.planningId == planningId }
            .sortedBy { it.performedAt }
    }
}
