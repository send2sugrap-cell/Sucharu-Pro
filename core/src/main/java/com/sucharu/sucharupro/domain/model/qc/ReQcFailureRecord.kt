package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable historical failure record captured whenever a Re-QC inspection fails (Module 06 Step 06).
 *
 * This record must NEVER be overwritten and provides permanent auditability
 * across multi-cycle failure loops.
 */
data class ReQcFailureRecord(
    val failureRecordId: String,
    val reQcId: String,
    val cycleNumber: Int,
    val productionJobId: String,
    val projectId: String,
    val defectId: String? = null,
    val checklistId: String? = null,
    val failedItemIds: List<String> = emptyList(),
    val failureReason: ReQcFailureReason,
    val failureNotes: String,
    val affectedQuantity: Int,
    val quantityUnit: String = "pcs",
    val detectedBy: String,
    val detectedByName: String? = null,
    val detectedAt: String,
    val nextAction: String? = null,
    val linkedReworkId: String? = null
)
