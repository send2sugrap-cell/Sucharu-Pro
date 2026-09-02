package com.sucharu.sucharupro.data.datasource.jobclosure

import com.sucharu.sucharupro.domain.model.jobclosure.*
import java.util.concurrent.ConcurrentHashMap

class FakeProductionJobClosureDataSource : ProductionJobClosureDataSource {

    private val closureRecords = ConcurrentHashMap<String, ProductionJobClosureRecord>()
    private val scorecards = ConcurrentHashMap<String, ManufacturingPerformanceScorecard>()
    private val events = ConcurrentHashMap<String, MutableList<ProductionJobClosureEvent>>()

    override suspend fun saveClosureRecord(tenantId: String, record: ProductionJobClosureRecord) {
        closureRecords["$tenantId:${record.executionJobId}"] = record
    }

    override suspend fun getClosureRecordByJob(tenantId: String, executionJobId: String): ProductionJobClosureRecord? {
        return closureRecords["$tenantId:$executionJobId"]
    }

    override suspend fun saveScorecard(tenantId: String, scorecard: ManufacturingPerformanceScorecard) {
        scorecards["$tenantId:${scorecard.executionJobId}"] = scorecard
    }

    override suspend fun getScorecardByJob(tenantId: String, executionJobId: String): ManufacturingPerformanceScorecard? {
        return scorecards["$tenantId:$executionJobId"]
    }

    override suspend fun saveEvent(tenantId: String, event: ProductionJobClosureEvent) {
        val list = events.computeIfAbsent("$tenantId:${event.executionJobId}") { mutableListOf() }
        synchronized(list) {
            list.add(event)
        }
    }

    override suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<ProductionJobClosureEvent> {
        return events["$tenantId:$executionJobId"]?.toList() ?: emptyList()
    }
}
