package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for Vendor Quality Inspections, Defects, Rejections, Disputes, and Audits.
 */
interface VendorQualityDataSource {
    // Inspections
    fun observeInspections(projectId: String, vendorId: String? = null, deliveryReceiptId: String? = null): Flow<List<VendorQualityInspection>>
    suspend fun findInspectionById(projectId: String, inspectionId: String): DomainResult<VendorQualityInspection>
    suspend fun findInspectionByReference(projectId: String, reference: String): DomainResult<VendorQualityInspection>
    suspend fun listInspections(
        projectId: String,
        vendorId: String? = null,
        deliveryReceiptId: String? = null,
        status: VendorInspectionStatus? = null
    ): DomainResult<List<VendorQualityInspection>>
    suspend fun createInspection(inspection: VendorQualityInspection): DomainResult<VendorQualityInspection>
    suspend fun updateInspection(inspection: VendorQualityInspection): DomainResult<VendorQualityInspection>
    suspend fun updateInspectionStatus(
        projectId: String,
        inspectionId: String,
        status: VendorInspectionStatus,
        overallResult: InspectionResult?,
        updatedBy: String
    ): DomainResult<VendorQualityInspection>

    // Defects
    suspend fun createDefect(defect: VendorDefect): DomainResult<VendorDefect>
    suspend fun listDefectsByInspection(projectId: String, inspectionId: String): DomainResult<List<VendorDefect>>

    // Rejections
    fun observeRejections(projectId: String, vendorId: String? = null, deliveryReceiptId: String? = null): Flow<List<VendorRejection>>
    suspend fun findRejectionById(projectId: String, rejectionId: String): DomainResult<VendorRejection>
    suspend fun findRejectionByReference(projectId: String, reference: String): DomainResult<VendorRejection>
    suspend fun listRejections(
        projectId: String,
        vendorId: String? = null,
        deliveryReceiptId: String? = null,
        status: VendorRejectionStatus? = null
    ): DomainResult<List<VendorRejection>>
    suspend fun createRejection(rejection: VendorRejection): DomainResult<VendorRejection>
    suspend fun updateRejection(rejection: VendorRejection): DomainResult<VendorRejection>
    suspend fun updateRejectionStatus(
        projectId: String,
        rejectionId: String,
        status: VendorRejectionStatus,
        updatedBy: String,
        vendorResponse: String? = null,
        resolutionNotes: String? = null
    ): DomainResult<VendorRejection>

    // Disputes
    fun observeDisputes(projectId: String, vendorId: String? = null, status: VendorDisputeStatus? = null): Flow<List<VendorDispute>>
    suspend fun findDisputeById(projectId: String, disputeId: String): DomainResult<VendorDispute>
    suspend fun findDisputeByReference(projectId: String, reference: String): DomainResult<VendorDispute>
    suspend fun listDisputes(
        projectId: String,
        vendorId: String? = null,
        status: VendorDisputeStatus? = null,
        disputeType: VendorDisputeType? = null
    ): DomainResult<List<VendorDispute>>
    suspend fun createDispute(dispute: VendorDispute): DomainResult<VendorDispute>
    suspend fun updateDispute(dispute: VendorDispute): DomainResult<VendorDispute>
    suspend fun updateDisputeStatus(
        projectId: String,
        disputeId: String,
        status: VendorDisputeStatus,
        updatedBy: String,
        vendorResponse: String? = null,
        resolutionProposal: String? = null,
        resolution: String? = null
    ): DomainResult<VendorDispute>

    // Dispute Events
    suspend fun appendDisputeEvent(event: VendorDisputeEvent): DomainResult<VendorDisputeEvent>
    suspend fun listDisputeEvents(projectId: String, disputeId: String): DomainResult<List<VendorDisputeEvent>>

    // Quality Audits
    suspend fun appendQualityAudit(auditEvent: VendorQualityAuditEvent): DomainResult<VendorQualityAuditEvent>
    suspend fun listQualityAudits(projectId: String, entityId: String): DomainResult<List<VendorQualityAuditEvent>>

    // Evidence
    suspend fun addEvidence(evidence: VendorQualityEvidence): DomainResult<VendorQualityEvidence>
    suspend fun listEvidence(projectId: String, sourceType: String, sourceId: String): DomainResult<List<VendorQualityEvidence>>
}
