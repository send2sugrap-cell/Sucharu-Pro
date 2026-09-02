package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProductionHandoff
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for Production Handoff persistence in Sucharu Pro ERP.
 */
interface DesignProductionHandoffDataSource {
    fun observeHandoffs(): Flow<List<DesignProductionHandoff>>
    suspend fun fetchHandoffByApprovalId(approvalId: String): DomainResult<DesignProductionHandoff>
    suspend fun insertHandoff(handoff: DesignProductionHandoff): DomainResult<DesignProductionHandoff>
}
