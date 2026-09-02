package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcItem
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcSnapshot
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcAssignment
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for Quality Control persistence in Sucharu Pro ERP.
 */
interface ProductionQcDataSource {
    fun observeQcList(): Flow<List<ProductionQc>>
    suspend fun fetchQcById(qcId: String): DomainResult<ProductionQc>
    suspend fun insertQc(qc: ProductionQc): DomainResult<ProductionQc>
    suspend fun updateQc(qc: ProductionQc): DomainResult<ProductionQc>

    fun observeAssignments(): Flow<List<QcAssignment>>
    suspend fun insertAssignment(assignment: QcAssignment): DomainResult<QcAssignment>
    suspend fun updateAssignment(assignment: QcAssignment): DomainResult<QcAssignment>

    fun observeActivityEvents(): Flow<List<QcActivityEvent>>
    suspend fun insertActivityEvent(event: QcActivityEvent): DomainResult<QcActivityEvent>

    // Pre-Production QC extensions (Module 06 Step 02)
    fun observePreProductionItems(): Flow<List<PreProductionQcItem>>
    suspend fun insertPreProductionItems(items: List<PreProductionQcItem>): DomainResult<List<PreProductionQcItem>>
    suspend fun updatePreProductionItem(item: PreProductionQcItem): DomainResult<PreProductionQcItem>

    fun observeSnapshots(): Flow<List<PreProductionQcSnapshot>>
    suspend fun insertSnapshot(snapshot: PreProductionQcSnapshot): DomainResult<PreProductionQcSnapshot>
}
