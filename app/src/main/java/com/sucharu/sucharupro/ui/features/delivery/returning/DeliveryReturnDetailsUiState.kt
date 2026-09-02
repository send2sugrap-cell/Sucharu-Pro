package com.sucharu.sucharupro.ui.features.delivery.returning

import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnSummary

data class DeliveryReturnDetailsUiState(
    val isLoading: Boolean = false,
    val returnItem: DeliveryReturn? = null,
    val summary: DeliveryReturnSummary? = null,
    val lines: List<DeliveryReturnLine> = emptyList(),
    val events: List<DeliveryReturnActivityEvent> = emptyList(),
    val isActionInProgress: Boolean = false,
    val actionSuccessMessage: String? = null,
    val errorMessage: String? = null
)
