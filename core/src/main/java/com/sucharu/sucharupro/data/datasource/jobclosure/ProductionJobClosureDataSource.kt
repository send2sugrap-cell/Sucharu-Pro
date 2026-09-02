package com.sucharu.sucharupro.data.datasource.jobclosure

import com.sucharu.sucharupro.domain.model.jobclosure.*

interface ProductionJobClosureDataSource {
    suspend fun saveClosureRecord(tenantId: String, record: ProductionJobClosureRecord)
    suspend fun getClosureRecordByJob(tenantId: String, executionJobId: String): ProductionJobClosureRecord?

    suspend fun saveScorecard(tenantId: String, scorecard: ManufacturingPerformanceScorecard)
    suspend fun getScorecardByJob(tenantId: String, executionJobId: String): ManufacturingPerformanceScorecard?

    suspend fun saveEvent(tenantId: String, event: ProductionJobClosureEvent)
    suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<ProductionJobClosureEvent>
}
