package com.sucharu.sucharupro.domain.model.qc

import com.sucharu.sucharupro.domain.model.common.FileReference

/**
 * Supporting evidence reference attached to a [ProductionRework] (Module 06 Step 05).
 */
data class ReworkEvidence(
    val evidenceId: String,
    val reworkId: String,
    val fileReferenceId: String? = null,
    val fileReference: FileReference? = null,
    val description: String? = null,
    val createdBy: String,
    val createdAt: String
) {
    init {
        require(evidenceId.isNotBlank()) { "Evidence ID cannot be blank." }
        require(reworkId.isNotBlank()) { "Rework ID cannot be blank." }
        require(createdBy.isNotBlank()) { "CreatedBy cannot be blank." }
        require(createdAt.isNotBlank()) { "CreatedAt cannot be blank." }
    }
}
