package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignApprovalDecision
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory reactive implementation of [DesignApprovalDataSource] with [Mutex] atomicity.
 */
class FakeDesignApprovalDataSource(
    initialApprovals: List<DesignApproval> = emptyList(),
    initialDecisions: List<DesignApprovalDecision> = emptyList()
) : DesignApprovalDataSource {

    private val mutex = Mutex()
    private val _approvals = MutableStateFlow<List<DesignApproval>>(initialApprovals)
    private val _decisions = MutableStateFlow<List<DesignApprovalDecision>>(initialDecisions)

    override fun observeApprovals(): Flow<List<DesignApproval>> = _approvals.asStateFlow()

    override suspend fun fetchApprovalById(approvalId: String): DomainResult<DesignApproval> = mutex.withLock {
        val approval = _approvals.value.find { it.approvalId == approvalId }
        return if (approval != null) {
            DomainResult.Success(approval)
        } else {
            DomainResult.Error(message = "Approval not found with ID: $approvalId")
        }
    }

    override suspend fun insertApproval(approval: DesignApproval): DomainResult<DesignApproval> = mutex.withLock {
        if (_approvals.value.any { it.approvalId == approval.approvalId }) {
            return DomainResult.Error(message = "Approval with ID '${approval.approvalId}' already exists.")
        }
        _approvals.value = _approvals.value + approval
        DomainResult.Success(approval)
    }

    override suspend fun updateApproval(approval: DesignApproval): DomainResult<DesignApproval> = mutex.withLock {
        val index = _approvals.value.indexOfFirst { it.approvalId == approval.approvalId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent Approval: ${approval.approvalId}")
        }

        val currentList = _approvals.value.toMutableList()
        currentList[index] = approval
        _approvals.value = currentList.toList()
        DomainResult.Success(approval)
    }

    override fun observeDecisions(): Flow<List<DesignApprovalDecision>> = _decisions.asStateFlow()

    override suspend fun insertDecision(decision: DesignApprovalDecision): DomainResult<DesignApprovalDecision> = mutex.withLock {
        if (_decisions.value.any { it.decisionId == decision.decisionId }) {
            return DomainResult.Error(message = "Decision with ID '${decision.decisionId}' already exists.")
        }
        _decisions.value = _decisions.value + decision

        // Synchronize parent approval decision list
        val approvalIndex = _approvals.value.indexOfFirst { it.approvalId == decision.approvalId }
        if (approvalIndex != -1) {
            val parent = _approvals.value[approvalIndex]
            val updatedDecisions = parent.decisions.filterNot { it.decisionId == decision.decisionId } + decision
            val currentApprovals = _approvals.value.toMutableList()
            currentApprovals[approvalIndex] = parent.copy(decisions = updatedDecisions)
            _approvals.value = currentApprovals.toList()
        }

        DomainResult.Success(decision)
    }
}
