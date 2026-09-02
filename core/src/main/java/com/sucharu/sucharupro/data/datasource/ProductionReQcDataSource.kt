package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureRecord
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import kotlinx.coroutines.flow.Flow

/**
 * Data Source abstraction interface for Re-QC & Failure Loops (Module 06 Step 06).
 */
interface ProductionReQcDataSource {

    /** Observes all Re-QC inspection records reactively. */
    fun observeReQcList(): Flow<List<ReQcInspection>>

    /** Fetches a single Re-QC inspection record by [reQcId]. */
    suspend fun fetchReQcById(reQcId: String): DomainResult<ReQcInspection>

    /** Inserts a new Re-QC inspection record. */
    suspend fun insertReQc(reQc: ReQcInspection): DomainResult<ReQcInspection>

    /** Updates an existing Re-QC inspection record. */
    suspend fun updateReQc(reQc: ReQcInspection): DomainResult<ReQcInspection>

    /** Observes all immutable failure records reactively. */
    fun observeFailureRecords(): Flow<List<ReQcFailureRecord>>

    /** Fetches a specific failure record by [recordId]. */
    suspend fun fetchFailureRecordById(recordId: String): DomainResult<ReQcFailureRecord>

    /** Inserts an immutable failure record. */
    suspend fun insertFailureRecord(record: ReQcFailureRecord): DomainResult<ReQcFailureRecord>

    /** Observes all audit activity events reactively. */
    fun observeActivityEvents(): Flow<List<ReQcActivityEvent>>

    /** Inserts an audit activity event. */
    suspend fun insertActivityEvent(event: ReQcActivityEvent): DomainResult<ReQcActivityEvent>
}
