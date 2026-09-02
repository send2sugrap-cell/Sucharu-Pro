package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorQualityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorQualityRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of VendorQualityRepository delegating to VendorQualityDataSource.
 */
class VendorQualityRepositoryImpl(
    private val dataSource: VendorQualityDataSource
) : VendorQualityRepository {

    override fun observeInspections(projectId: String, vendorId: String?, deliveryReceiptId: String?): Flow<List<VendorQualityInspection>> {
        return dataSource.observeInspections(projectId, vendorId, deliveryReceiptId)
    }

    override suspend fun findInspectionById(projectId: String, inspectionId: String): DomainResult<VendorQualityInspection> {
        return dataSource.findInspectionById(projectId, inspectionId)
    }

    override suspend fun findInspectionByReference(projectId: String, reference: String): DomainResult<VendorQualityInspection> {
        return dataSource.findInspectionByReference(projectId, reference)
    }

    override suspend fun listInspections(
        projectId: String,
        vendorId: String?,
        deliveryReceiptId: String?,
        status: VendorInspectionStatus?
    ): DomainResult<List<VendorQualityInspection>> {
        return dataSource.listInspections(projectId, vendorId, deliveryReceiptId, status)
    }

    override suspend fun createInspection(inspection: VendorQualityInspection): DomainResult<VendorQualityInspection> {
        return dataSource.createInspection(inspection)
    }

    override suspend fun updateInspection(inspection: VendorQualityInspection): DomainResult<VendorQualityInspection> {
        return dataSource.updateInspection(inspection)
    }

    override suspend fun updateInspectionStatus(
        projectId: String,
        inspectionId: String,
        status: VendorInspectionStatus,
        overallResult: InspectionResult?,
        updatedBy: String
    ): DomainResult<VendorQualityInspection> {
        return dataSource.updateInspectionStatus(projectId, inspectionId, status, overallResult, updatedBy)
    }

    override suspend fun createDefect(defect: VendorDefect): DomainResult<VendorDefect> {
        return dataSource.createDefect(defect)
    }

    override suspend fun listDefectsByInspection(projectId: String, inspectionId: String): DomainResult<List<VendorDefect>> {
        return dataSource.listDefectsByInspection(projectId, inspectionId)
    }

    override fun observeRejections(projectId: String, vendorId: String?, deliveryReceiptId: String?): Flow<List<VendorRejection>> {
        return dataSource.observeRejections(projectId, vendorId, deliveryReceiptId)
    }

    override suspend fun findRejectionById(projectId: String, rejectionId: String): DomainResult<VendorRejection> {
        return dataSource.findRejectionById(projectId, rejectionId)
    }

    override suspend fun findRejectionByReference(projectId: String, reference: String): DomainResult<VendorRejection> {
        return dataSource.findRejectionByReference(projectId, reference)
    }

    override suspend fun listRejections(
        projectId: String,
        vendorId: String?,
        deliveryReceiptId: String?,
        status: VendorRejectionStatus?
    ): DomainResult<List<VendorRejection>> {
        return dataSource.listRejections(projectId, vendorId, deliveryReceiptId, status)
    }

    override suspend fun createRejection(rejection: VendorRejection): DomainResult<VendorRejection> {
        return dataSource.createRejection(rejection)
    }

    override suspend fun updateRejection(rejection: VendorRejection): DomainResult<VendorRejection> {
        return dataSource.updateRejection(rejection)
    }

    override suspend fun updateRejectionStatus(
        projectId: String,
        rejectionId: String,
        status: VendorRejectionStatus,
        updatedBy: String,
        vendorResponse: String?,
        resolutionNotes: String?
    ): DomainResult<VendorRejection> {
        return dataSource.updateRejectionStatus(projectId, rejectionId, status, updatedBy, vendorResponse, resolutionNotes)
    }

    override fun observeDisputes(projectId: String, vendorId: String?, status: VendorDisputeStatus?): Flow<List<VendorDispute>> {
        return dataSource.observeDisputes(projectId, vendorId, status)
    }

    override suspend fun findDisputeById(projectId: String, disputeId: String): DomainResult<VendorDispute> {
        return dataSource.findDisputeById(projectId, disputeId)
    }

    override suspend fun findDisputeByReference(projectId: String, reference: String): DomainResult<VendorDispute> {
        return dataSource.findDisputeByReference(projectId, reference)
    }

    override suspend fun listDisputes(
        projectId: String,
        vendorId: String?,
        status: VendorDisputeStatus?,
        disputeType: VendorDisputeType?
    ): DomainResult<List<VendorDispute>> {
        return dataSource.listDisputes(projectId, vendorId, status, disputeType)
    }

    override suspend fun createDispute(dispute: VendorDispute): DomainResult<VendorDispute> {
        return dataSource.createDispute(dispute)
    }

    override suspend fun updateDispute(dispute: VendorDispute): DomainResult<VendorDispute> {
        return dataSource.updateDispute(dispute)
    }

    override suspend fun updateDisputeStatus(
        projectId: String,
        disputeId: String,
        status: VendorDisputeStatus,
        updatedBy: String,
        vendorResponse: String?,
        resolutionProposal: String?,
        resolution: String?
    ): DomainResult<VendorDispute> {
        return dataSource.updateDisputeStatus(projectId, disputeId, status, updatedBy, vendorResponse, resolutionProposal, resolution)
    }

    override suspend fun appendDisputeEvent(event: VendorDisputeEvent): DomainResult<VendorDisputeEvent> {
        return dataSource.appendDisputeEvent(event)
    }

    override suspend fun listDisputeEvents(projectId: String, disputeId: String): DomainResult<List<VendorDisputeEvent>> {
        return dataSource.listDisputeEvents(projectId, disputeId)
    }

    override suspend fun appendQualityAudit(auditEvent: VendorQualityAuditEvent): DomainResult<VendorQualityAuditEvent> {
        return dataSource.appendQualityAudit(auditEvent)
    }

    override suspend fun listQualityAudits(projectId: String, entityId: String): DomainResult<List<VendorQualityAuditEvent>> {
        return dataSource.listQualityAudits(projectId, entityId)
    }

    override suspend fun addEvidence(evidence: VendorQualityEvidence): DomainResult<VendorQualityEvidence> {
        return dataSource.addEvidence(evidence)
    }

    override suspend fun listEvidence(projectId: String, sourceType: String, sourceId: String): DomainResult<List<VendorQualityEvidence>> {
        return dataSource.listEvidence(projectId, sourceType, sourceId)
    }
}
