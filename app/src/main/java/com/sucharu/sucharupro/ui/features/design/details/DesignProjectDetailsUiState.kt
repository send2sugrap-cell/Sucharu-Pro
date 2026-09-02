package com.sucharu.sucharupro.ui.features.design.details

import com.sucharu.sucharupro.domain.model.design.DesignActivityEvent
import com.sucharu.sucharupro.domain.model.design.DesignAssignment
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.job.ProductionOperator

/**
 * UI State for Design Project Details & Assignment control.
 */
sealed interface DesignProjectDetailsUiState {
    data object Loading : DesignProjectDetailsUiState

    data class Success(
        val project: DesignProject,
        val assignments: List<DesignAssignment>,
        val activityEvents: List<DesignActivityEvent>,
        val availableDesigners: List<ProductionOperator>,
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : DesignProjectDetailsUiState

    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : DesignProjectDetailsUiState
}
