package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignApprovalDecision
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for Approval Workflow storage in Sucharu Pro ERP.
 */
interface DesignApprovalDataSource {
    fun observeApprovals(): Flow<List<DesignApproval>>
    suspend fun fetchApprovalById(approvalId: String): DomainResult<DesignApproval>
    suspend fun insertApproval(approval: DesignApproval): DomainResult<DesignApproval>
    suspend fun updateApproval(approval: DesignApproval): DomainResult<DesignApproval>

    fun observeDecisions(): Flow<List<DesignApprovalDecision>>
    suspend fun insertDecision(decision: DesignApprovalDecision): DomainResult<DesignApprovalDecision>
}
