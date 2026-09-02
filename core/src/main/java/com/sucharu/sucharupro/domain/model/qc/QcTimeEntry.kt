package com.sucharu.sucharupro.domain.model.qc

/**
 * Entity representing an individual operational QC time tracking entry (Module 06 Step 08).
 *
 * Tracks duration in minutes for specific quality tasks performed by inspectors or technicians.
 */
data class QcTimeEntry(
    val id: String,
    val productionJobId: String,
    val projectId: String,
    val qcId: String? = null,
    val inspectionChecklistId: String? = null,
    val productionDefectId: String? = null,
    val productionReworkId: String? = null,
    val reQcId: String? = null,
    val finalQcId: String? = null,
    val entryType: QcTimeEntryType,
    val actorId: String,
    val actorName: String? = null,
    val startedAt: String,
    val endedAt: String? = null,
    val durationMinutes: Long,
    val status: QcTimeStatus = QcTimeStatus.RECORDED,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    /** Whether this time entry is immutable and permanently locked. */
    val isLocked: Boolean get() = status.isLocked

    /** Whether this time entry is in a terminal state. */
    val isTerminal: Boolean get() = status.isTerminal

    /** Whether this time entry is active and included in reconciliation. */
    val isActive: Boolean get() = status == QcTimeStatus.RECORDED || status == QcTimeStatus.RECONCILED || status == QcTimeStatus.LOCKED
}
