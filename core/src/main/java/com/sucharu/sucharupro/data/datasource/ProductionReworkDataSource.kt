package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReworkAssignment
import com.sucharu.sucharupro.domain.model.qc.ReworkEvidence
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for QC Rework Management & Workflow storage (Module 06 Step 05).
 */
interface ProductionReworkDataSource {

    /** Observes the list of all rework records. */
    fun observeReworks(): Flow<List<ProductionRework>>

    /** Fetches a single rework by ID. */
    suspend fun fetchReworkById(reworkId: String): DomainResult<ProductionRework>

    /** Inserts a new rework record. */
    suspend fun insertRework(rework: ProductionRework): DomainResult<ProductionRework>

    /** Updates an existing rework record. */
    suspend fun updateRework(rework: ProductionRework): DomainResult<ProductionRework>

    /** Observes all assignment history records. */
    fun observeAssignments(): Flow<List<ReworkAssignment>>

    /** Inserts a new assignment record. */
    suspend fun insertAssignment(assignment: ReworkAssignment): DomainResult<ReworkAssignment>

    /** Updates an existing assignment record (e.g. marking inactive or unassigned). */
    suspend fun updateAssignment(assignment: ReworkAssignment): DomainResult<ReworkAssignment>

    /** Observes all audit activity events. */
    fun observeActivityEvents(): Flow<List<ReworkActivityEvent>>

    /** Inserts a new audit activity event. */
    suspend fun insertActivityEvent(event: ReworkActivityEvent): DomainResult<ReworkActivityEvent>

    /** Observes all attached evidence records. */
    fun observeEvidence(): Flow<List<ReworkEvidence>>

    /** Inserts a new evidence record. */
    suspend fun insertEvidence(evidence: ReworkEvidence): DomainResult<ReworkEvidence>
}
