package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcSnapshot
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection

/**
 * Domain validator enforcing creation, model integrity, and cross-job/cross-project isolation for Final QC (Module 06 Step 07).
 */
object FinalQcValidator {

    /**
     * Validates initial creation parameters for [FinalQcInspection].
     */
    fun validateCreationParams(
        projectId: String,
        productionJobId: String,
        totalQuantity: Int,
        quantityUnit: String?,
        timestamp: String
    ): DomainResult<Unit> {
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (totalQuantity <= 0) {
            return DomainResult.Error(message = "Total quantity must be greater than zero. Provided: $totalQuantity")
        }
        if (quantityUnit != null && quantityUnit.isBlank()) {
            return DomainResult.Error(message = "Quantity unit cannot be blank if specified.")
        }
        if (timestamp.isBlank()) {
            return DomainResult.Error(message = "Timestamp cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates full domain entity integrity of a [FinalQcInspection].
     */
    fun validateInspectionModel(inspection: FinalQcInspection): DomainResult<Unit> {
        if (inspection.finalQcId.isBlank()) {
            return DomainResult.Error(message = "Final QC ID cannot be blank.")
        }
        if (inspection.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (inspection.productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (inspection.totalQuantity <= 0) {
            return DomainResult.Error(message = "Total quantity must be greater than zero. Provided: ${inspection.totalQuantity}")
        }
        if (inspection.inspectedQuantity < 0) {
            return DomainResult.Error(message = "Inspected quantity cannot be negative. Provided: ${inspection.inspectedQuantity}")
        }
        if (inspection.acceptedQuantity < 0) {
            return DomainResult.Error(message = "Accepted quantity cannot be negative. Provided: ${inspection.acceptedQuantity}")
        }
        if (inspection.rejectedQuantity < 0) {
            return DomainResult.Error(message = "Rejected quantity cannot be negative. Provided: ${inspection.rejectedQuantity}")
        }
        if (inspection.inspectedQuantity > inspection.totalQuantity) {
            return DomainResult.Error(message = "Inspected quantity (${inspection.inspectedQuantity}) cannot exceed total quantity (${inspection.totalQuantity}).")
        }
        if (inspection.acceptedQuantity > inspection.totalQuantity) {
            return DomainResult.Error(message = "Accepted quantity (${inspection.acceptedQuantity}) cannot exceed total quantity (${inspection.totalQuantity}).")
        }

        // When in PASSED or FAILED status, inspected must equal accepted + rejected
        if (inspection.status in setOf(FinalQcStatus.PASSED, FinalQcStatus.FAILED, FinalQcStatus.RELEASED)) {
            if (inspection.inspectedQuantity != inspection.acceptedQuantity + inspection.rejectedQuantity) {
                return DomainResult.Error(message = 
                    "Inspected quantity (${inspection.inspectedQuantity}) must equal the sum of accepted (${inspection.acceptedQuantity}) and rejected (${inspection.rejectedQuantity}) quantities."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for marking a Final QC inspection as PASSED.
     */
    fun validatePassPrerequisites(
        acceptedQuantity: Int,
        rejectedQuantity: Int,
        inspectorId: String,
        timestamp: String
    ): DomainResult<Unit> {
        if (inspectorId.isBlank()) {
            return DomainResult.Error(message = "Inspector ID cannot be blank when completing inspection.")
        }
        if (timestamp.isBlank()) {
            return DomainResult.Error(message = "Timestamp cannot be blank.")
        }
        if (acceptedQuantity <= 0) {
            return DomainResult.Error(message = "Accepted quantity must be greater than zero for a PASS decision. Provided: $acceptedQuantity")
        }
        if (rejectedQuantity > 0) {
            return DomainResult.Error(message = "Cannot mark Final QC as PASS when rejected quantity is greater than zero ($rejectedQuantity). Must record FAIL or rework.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for marking a Final QC inspection as FAILED.
     */
    fun validateFailPrerequisites(
        failureReason: String,
        rejectedQuantity: Int,
        inspectorId: String,
        timestamp: String
    ): DomainResult<Unit> {
        if (inspectorId.isBlank()) {
            return DomainResult.Error(message = "Inspector ID cannot be blank when completing inspection.")
        }
        if (timestamp.isBlank()) {
            return DomainResult.Error(message = "Timestamp cannot be blank.")
        }
        if (failureReason.isBlank()) {
            return DomainResult.Error(message = "Failure reason/notes cannot be blank for a FAIL decision.")
        }
        if (rejectedQuantity < 0) {
            return DomainResult.Error(message = "Rejected quantity cannot be negative. Provided: $rejectedQuantity")
        }
        return DomainResult.Success(Unit)
    }

    // ==========================================
    // Cross-Job & Cross-Project Isolation
    // ==========================================

    fun validateDefectCrossJobIsolation(
        productionJobId: String,
        defect: ProductionDefect
    ): DomainResult<Unit> {
        if (defect.productionJobId != productionJobId) {
            return DomainResult.Error(message = 
                "Cross-job reference violation: Defect ${defect.defectId} belongs to Job ${defect.productionJobId}, but Final QC is for Job $productionJobId."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateReworkCrossJobIsolation(
        productionJobId: String,
        rework: ProductionRework
    ): DomainResult<Unit> {
        if (rework.productionJobId != productionJobId) {
            return DomainResult.Error(message = 
                "Cross-job reference violation: Rework ${rework.reworkId} belongs to Job ${rework.productionJobId}, but Final QC is for Job $productionJobId."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateReQcCrossJobIsolation(
        productionJobId: String,
        reQc: ReQcInspection
    ): DomainResult<Unit> {
        if (reQc.productionJobId != productionJobId) {
            return DomainResult.Error(message = 
                "Cross-job reference violation: Re-QC ${reQc.reQcId} belongs to Job ${reQc.productionJobId}, but Final QC is for Job $productionJobId."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateChecklistCrossJobIsolation(
        productionJobId: String,
        checklist: QcInspectionChecklist
    ): DomainResult<Unit> {
        if (checklist.productionJobId != productionJobId) {
            return DomainResult.Error(message = 
                "Cross-job reference violation: Checklist ${checklist.inspectionChecklistId} belongs to Job ${checklist.productionJobId}, but Final QC is for Job $productionJobId."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validatePreProductionQcCrossJobIsolation(
        productionJobId: String,
        qc: ProductionQc
    ): DomainResult<Unit> {
        if (qc.productionJobId != productionJobId) {
            return DomainResult.Error(message = 
                "Cross-job reference violation: Pre-Production QC ${qc.qcId} belongs to Job ${qc.productionJobId}, but Final QC is for Job $productionJobId."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateCrossProjectIsolation(
        expectedProjectId: String,
        actualProjectId: String
    ): DomainResult<Unit> {
        if (expectedProjectId != actualProjectId) {
            return DomainResult.Error(message = 
                "Cross-project reference violation: Expected Project $expectedProjectId but received $actualProjectId."
            )
        }
        return DomainResult.Success(Unit)
    }
}
