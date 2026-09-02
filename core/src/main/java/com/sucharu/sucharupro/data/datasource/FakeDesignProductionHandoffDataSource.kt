package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProductionHandoff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory reactive implementation of [DesignProductionHandoffDataSource] with [Mutex] atomicity.
 */
class FakeDesignProductionHandoffDataSource(
    initialHandoffs: List<DesignProductionHandoff> = emptyList()
) : DesignProductionHandoffDataSource {

    private val mutex = Mutex()
    private val _handoffs = MutableStateFlow<List<DesignProductionHandoff>>(initialHandoffs)

    override fun observeHandoffs(): Flow<List<DesignProductionHandoff>> = _handoffs.asStateFlow()

    override suspend fun fetchHandoffByApprovalId(approvalId: String): DomainResult<DesignProductionHandoff> = mutex.withLock {
        val handoff = _handoffs.value.find { it.approvalId == approvalId }
        return if (handoff != null) {
            DomainResult.Success(handoff)
        } else {
            DomainResult.Error(message = "Handoff authorization not found for Approval ID: $approvalId")
        }
    }

    override suspend fun insertHandoff(handoff: DesignProductionHandoff): DomainResult<DesignProductionHandoff> = mutex.withLock {
        if (_handoffs.value.any { it.approvalId == handoff.approvalId }) {
            return DomainResult.Error(message = "Handoff authorization for Approval '${handoff.approvalId}' already exists.")
        }
        _handoffs.value = _handoffs.value + handoff
        DomainResult.Success(handoff)
    }
}
