package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory thread-safe fake data source for Delivery Challans (Module 08 Step 02).
 */
class FakeDeliveryChallanDataSource : DeliveryChallanDataSource {

    private val mutex = Mutex()

    private val challansFlow = MutableStateFlow<List<DeliveryChallan>>(emptyList())
    private val linesFlow = MutableStateFlow<List<DeliveryChallanLine>>(emptyList())
    private val activityEventsFlow = MutableStateFlow<List<DeliveryChallanActivityEvent>>(emptyList())

    // ──────────────────────────────────────────────────────────────
    // Challan Headers
    // ──────────────────────────────────────────────────────────────

    override fun observeChallans(projectId: String): Flow<List<DeliveryChallan>> {
        return challansFlow.map { list -> list.filter { it.projectId == projectId } }
    }

    override fun observeChallansForDeliveryOrder(deliveryOrderId: String): Flow<List<DeliveryChallan>> {
        return challansFlow.map { list -> list.filter { it.deliveryOrderId == deliveryOrderId } }
    }

    override fun observeChallan(challanId: String): Flow<DeliveryChallan?> {
        return challansFlow.map { list -> list.find { it.challanId == challanId } }
    }

    override suspend fun getChallan(challanId: String): DeliveryChallan? = mutex.withLock {
        challansFlow.value.find { it.challanId == challanId }
    }

    override suspend fun getChallanByNo(projectId: String, challanNo: String): DeliveryChallan? = mutex.withLock {
        challansFlow.value.find { it.projectId == projectId && it.challanNo.equals(challanNo, ignoreCase = true) }
    }

    override suspend fun getChallansForDeliveryOrder(deliveryOrderId: String): List<DeliveryChallan> = mutex.withLock {
        challansFlow.value.filter { it.deliveryOrderId == deliveryOrderId }
    }

    override suspend fun insertChallan(challan: DeliveryChallan, lines: List<DeliveryChallanLine>): Unit = mutex.withLock {
        val currentChallans = challansFlow.value.toMutableList()
        currentChallans.add(challan)
        challansFlow.value = currentChallans

        val currentLines = linesFlow.value.toMutableList()
        currentLines.addAll(lines)
        linesFlow.value = currentLines
    }

    override suspend fun updateChallan(challan: DeliveryChallan): Unit = mutex.withLock {
        val current = challansFlow.value.toMutableList()
        val index = current.indexOfFirst { it.challanId == challan.challanId }
        if (index != -1) {
            current[index] = challan
            challansFlow.value = current
        }
    }

    override suspend fun updateChallanWithLines(
        challan: DeliveryChallan,
        lines: List<DeliveryChallanLine>
    ): Unit = mutex.withLock {
        val currentChallans = challansFlow.value.toMutableList()
        val index = currentChallans.indexOfFirst { it.challanId == challan.challanId }
        if (index != -1) {
            currentChallans[index] = challan
            challansFlow.value = currentChallans
        }

        val currentLines = linesFlow.value.toMutableList()
        currentLines.removeAll { it.challanId == challan.challanId }
        currentLines.addAll(lines)
        linesFlow.value = currentLines
    }

    // ──────────────────────────────────────────────────────────────
    // Challan Lines
    // ──────────────────────────────────────────────────────────────

    override fun observeChallanLines(challanId: String): Flow<List<DeliveryChallanLine>> {
        return linesFlow.map { list -> list.filter { it.challanId == challanId } }
    }

    override suspend fun getChallanLines(challanId: String): List<DeliveryChallanLine> = mutex.withLock {
        linesFlow.value.filter { it.challanId == challanId }
    }

    override suspend fun getChallanLine(lineId: String): DeliveryChallanLine? = mutex.withLock {
        linesFlow.value.find { it.lineId == lineId }
    }

    override suspend fun getLinesForChallans(challanIds: List<String>): List<DeliveryChallanLine> = mutex.withLock {
        linesFlow.value.filter { it.challanId in challanIds }
    }

    // ──────────────────────────────────────────────────────────────
    // Activity Events
    // ──────────────────────────────────────────────────────────────

    override fun observeActivityEvents(challanId: String): Flow<List<DeliveryChallanActivityEvent>> {
        return activityEventsFlow.map { list ->
            list.filter { it.challanId == challanId }.sortedByDescending { it.performedAt }
        }
    }

    override suspend fun getActivityEvents(challanId: String): List<DeliveryChallanActivityEvent> = mutex.withLock {
        activityEventsFlow.value.filter { it.challanId == challanId }.sortedByDescending { it.performedAt }
    }

    override suspend fun insertActivityEvent(event: DeliveryChallanActivityEvent): Unit = mutex.withLock {
        val current = activityEventsFlow.value.toMutableList()
        current.add(event)
        activityEventsFlow.value = current
    }
}
