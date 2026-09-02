package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectAssignment
import com.sucharu.sucharupro.domain.model.qc.DefectEvidence
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.QcDefectActivityEvent
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for QC Defects, Assignments, Evidence, and Audit Activity (Module 06 Step 04).
 */
interface ProductionDefectDataSource {

    fun observeDefects(): Flow<List<ProductionDefect>>

    suspend fun fetchDefectById(defectId: String): DomainResult<ProductionDefect>

    suspend fun insertDefect(defect: ProductionDefect): DomainResult<ProductionDefect>

    suspend fun updateDefect(defect: ProductionDefect): DomainResult<ProductionDefect>

    fun observeAssignments(): Flow<List<DefectAssignment>>

    suspend fun insertAssignment(assignment: DefectAssignment): DomainResult<DefectAssignment>

    suspend fun updateAssignment(assignment: DefectAssignment): DomainResult<DefectAssignment>

    fun observeActivityEvents(): Flow<List<QcDefectActivityEvent>>

    suspend fun insertActivityEvent(event: QcDefectActivityEvent): DomainResult<QcDefectActivityEvent>

    fun observeEvidence(): Flow<List<DefectEvidence>>

    suspend fun insertEvidence(evidence: DefectEvidence): DomainResult<DefectEvidence>
}
