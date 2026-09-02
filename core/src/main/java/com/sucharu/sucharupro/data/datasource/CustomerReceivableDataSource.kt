package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.CustomerReceivable
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableActivityEvent
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contract for Customer Receivable data persistence (Module 09 Step 02).
 */
interface CustomerReceivableDataSource {

    suspend fun insertReceivable(receivable: CustomerReceivable)

    suspend fun updateReceivable(receivable: CustomerReceivable)

    suspend fun getReceivableById(receivableId: String): CustomerReceivable?

    suspend fun getReceivableByNumber(projectId: String, receivableNo: String): CustomerReceivable?

    suspend fun getReceivablesByReference(projectId: String, referenceId: String): List<CustomerReceivable>

    fun observeReceivables(projectId: String): Flow<List<CustomerReceivable>>

    fun observeReceivablesByCustomer(projectId: String, customerId: String): Flow<List<CustomerReceivable>>

    suspend fun getReceivablesByStatus(
        projectId: String,
        status: CustomerReceivableStatus
    ): List<CustomerReceivable>

    suspend fun insertActivityEvent(event: CustomerReceivableActivityEvent)

    suspend fun getActivityEvents(receivableId: String): List<CustomerReceivableActivityEvent>
}
