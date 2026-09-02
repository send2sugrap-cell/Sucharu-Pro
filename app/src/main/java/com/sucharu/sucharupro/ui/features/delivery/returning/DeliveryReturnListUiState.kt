package com.sucharu.sucharupro.ui.features.delivery.returning

import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnType

data class DeliveryReturnListUiState(
    val isLoading: Boolean = false,
    val returns: List<DeliveryReturn> = emptyList(),
    val filteredReturns: List<DeliveryReturn> = emptyList(),
    val searchQuery: String = "",
    val selectedStatusFilter: DeliveryReturnStatus? = null,
    val selectedTypeFilter: DeliveryReturnType? = null,
    val errorMessage: String? = null
)
