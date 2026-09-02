package com.sucharu.sucharupro.data.repository.jobclosure

import com.sucharu.sucharupro.data.datasource.jobclosure.ProductionJobClosureDataSource
import com.sucharu.sucharupro.domain.model.jobclosure.*
import com.sucharu.sucharupro.domain.repository.jobclosure.ProductionJobClosureRepository

class ProductionJobClosureRepositoryImpl(
    private val dataSource: ProductionJobClosureDataSource
) : ProductionJobClosureRepository {

    override suspend fun saveClosureRecord(tenantId: String, record: ProductionJobClosureRecord) {
        dataSource.saveClosureRecord(tenantId, record)
    }

    override suspend fun getClosureRecordByJob(tenantId: String, executionJobId: String): ProductionJobClosureRecord? {
        return dataSource.getClosureRecordByJob(tenantId, executionJobId)
    }

    override suspend fun saveScorecard(tenantId: String, scorecard: ManufacturingPerformanceScorecard) {
        dataSource.saveScorecard(tenantId, scorecard)
    }

    override suspend fun getScorecardByJob(tenantId: String, executionJobId: String): ManufacturingPerformanceScorecard? {
        return dataSource.getScorecardByJob(tenantId, executionJobId)
    }

    override suspend fun saveEvent(tenantId: String, event: ProductionJobClosureEvent) {
        dataSource.saveEvent(tenantId, event)
    }

    override suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<ProductionJobClosureEvent> {
        return dataSource.listEventsByJob(tenantId, executionJobId)
    }
}
