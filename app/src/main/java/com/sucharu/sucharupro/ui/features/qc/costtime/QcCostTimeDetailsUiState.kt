package com.sucharu.sucharupro.ui.features.qc.costtime

import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeReconciliation
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeSnapshot
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * UI state for QC Cost & Time Reconciliation Details Screen (Module 06 Step 08).
 */
data class QcCostTimeDetailsUiState(
    val isLoading: Boolean = false,
    val productionJobId: String = "",
    val reconciliation: QcCostTimeReconciliation? = null,
    val snapshot: QcCostTimeSnapshot? = null,
    val costEntries: List<QcCostEntry> = emptyList(),
    val timeEntries: List<QcTimeEntry> = emptyList(),
    val activityEvents: List<QcCostTimeActivityEvent> = emptyList(),
    val currentUserRole: UserRole? = null,
    val showRecordCostDialog: Boolean = false,
    val showRecordTimeDialog: Boolean = false,
    val showReconcileDialog: Boolean = false,
    val showAdjustDialog: Boolean = false,
    val showLockDialog: Boolean = false,
    val isActionInProgress: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val isLocked: Boolean get() = reconciliation?.isLocked == true
    val canReconcile: Boolean get() = currentUserRole in setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR) && !isLocked
    val canAdjust: Boolean get() = currentUserRole in setOf(UserRole.ADMIN, UserRole.MANAGER) && !isLocked && reconciliation != null
    val canLock: Boolean get() = currentUserRole in setOf(UserRole.ADMIN, UserRole.MANAGER) && !isLocked && reconciliation != null
    val canRecordCostOrTime: Boolean get() = currentUserRole in setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR) && !isLocked
}
