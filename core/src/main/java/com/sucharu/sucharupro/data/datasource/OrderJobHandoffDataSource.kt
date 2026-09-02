package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for OrderJobHandoff records.
 */
interface OrderJobHandoffDataSource {
    fun observeHandoffs(): Flow<List<OrderJobHandoff>>
    suspend fun fetchHandoffById(handoffId: String): DomainResult<OrderJobHandoff>
    suspend fun fetchHandoffForOrder(orderId: String): DomainResult<OrderJobHandoff>
    suspend fun insertHandoff(handoff: OrderJobHandoff): DomainResult<OrderJobHandoff>
    suspend fun updateHandoff(handoff: OrderJobHandoff): DomainResult<OrderJobHandoff>
}
