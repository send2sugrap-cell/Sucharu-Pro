package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorDeliveryReceiptRepository
import com.sucharu.sucharupro.domain.repository.VendorPurchaseOrderRepository
import com.sucharu.sucharupro.domain.repository.VendorQualityRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorQualityValidator
import java.math.BigDecimal
import java.util.UUID

interface VendorQualityService {
    // Inspections
    suspend fun createInspection(inspection: VendorQualityInspection, actorId: String): DomainResult<VendorQualityInspection>
    suspend fun startInspection(projectId: String, inspectionId: String, actorId: String): DomainResult<VendorQualityInspection>
    suspend fun completeInspection(
        projectId: String,
        inspectionId: String,
        status: VendorInspectionStatus,
        overallResult: InspectionResult,
        acceptedQty: BigDecimal,
        rejectedQty: BigDecimal,
        conditionalQty: BigDecimal,
        actorId: String
    ): DomainResult<VendorQualityInspection>
    suspend fun updateInspection(inspection: VendorQualityInspection, actorId: String): DomainResult<VendorQualityInspection>
    suspend fun getInspection(projectId: String, inspectionId: String): DomainResult<VendorQualityInspection>
    suspend fun listInspections(projectId: String, vendorId: String? = null, deliveryReceiptId: String? = null, status: VendorInspectionStatus? = null): DomainResult<List<VendorQualityInspection>>

    // Defects
    suspend fun addDefect(defect: VendorDefect, actorId: String): DomainResult<VendorDefect>
    suspend fun listDefects(projectId: String, inspectionId: String): DomainResult<List<VendorDefect>>

    // Rejections
    suspend fun createRejection(rejection: VendorRejection, actorId: String): DomainResult<VendorRejection>
    suspend fun submitRejection(projectId: String, rejectionId: String, actorId: String): DomainResult<VendorRejection>
    suspend fun acceptRejection(projectId: String, rejectionId: String, vendorResponse: String, actorId: String): DomainResult<VendorRejection>
    suspend fun disputeRejection(projectId: String, rejectionId: String, vendorResponse: String, actorId: String): DomainResult<VendorRejection>
    suspend fun resolveRejection(projectId: String, rejectionId: String, resolutionNotes: String, actorId: String): DomainResult<VendorRejection>
    suspend fun closeRejection(projectId: String, rejectionId: String, actorId: String): DomainResult<VendorRejection>
    suspend fun getRejection(projectId: String, rejectionId: String): DomainResult<VendorRejection>
    suspend fun listRejections(projectId: String, vendorId: String? = null, deliveryReceiptId: String? = null, status: VendorRejectionStatus? = null): DomainResult<List<VendorRejection>>

    // Disputes
    suspend fun createDispute(dispute: VendorDispute, actorId: String): DomainResult<VendorDispute>
    suspend fun assignDispute(projectId: String, disputeId: String, assignedTo: String, actorId: String): DomainResult<VendorDispute>
    suspend fun submitVendorResponse(projectId: String, disputeId: String, vendorResponse: String, actorId: String): DomainResult<VendorDispute>
    suspend fun escalateDispute(projectId: String, disputeId: String, reason: String, actorId: String): DomainResult<VendorDispute>
    suspend fun proposeDisputeResolution(projectId: String, disputeId: String, proposal: String, actorId: String): DomainResult<VendorDispute>
    suspend fun resolveDispute(projectId: String, disputeId: String, resolution: String, actorId: String): DomainResult<VendorDispute>
    suspend fun closeDispute(projectId: String, disputeId: String, actorId: String): DomainResult<VendorDispute>
    suspend fun getDispute(projectId: String, disputeId: String): DomainResult<VendorDispute>
    suspend fun listDisputes(projectId: String, vendorId: String? = null, status: VendorDisputeStatus? = null, disputeType: VendorDisputeType? = null): DomainResult<List<VendorDispute>>

    // Events & Evidence
    suspend fun listDisputeEvents(projectId: String, disputeId: String): DomainResult<List<VendorDisputeEvent>>
    suspend fun addEvidence(evidence: VendorQualityEvidence, actorId: String): DomainResult<VendorQualityEvidence>
    suspend fun listEvidence(projectId: String, sourceType: String, sourceId: String): DomainResult<List<VendorQualityEvidence>>
    suspend fun listQualityAudits(projectId: String, entityId: String): DomainResult<List<VendorQualityAuditEvent>>
}

class VendorQualityServiceImpl(
    private val vendorRepository: VendorRepository,
    private val purchaseOrderRepository: VendorPurchaseOrderRepository? = null,
    private val receiptRepository: VendorDeliveryReceiptRepository? = null,
    private val qualityRepository: VendorQualityRepository
) : VendorQualityService {

    override suspend fun createInspection(inspection: VendorQualityInspection, actorId: String): DomainResult<VendorQualityInspection> {
        val valErr = VendorQualityValidator.validateInspection(inspection)
        if (valErr is DomainResult.Error) return valErr

        val vendor = vendorRepository.findById(inspection.projectId, inspection.vendorId)
        if (vendor is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Vendor '${inspection.vendorId}' not found"))
        }
        if ((vendor as DomainResult.Success).data.status != VendorStatus.ACTIVE) {
            return DomainResult.Error(IllegalStateException("Vendor '${inspection.vendorId}' is not ACTIVE"))
        }

        // Validate Delivery Receipt if provided
        if (inspection.deliveryReceiptId != null && receiptRepository != null) {
            val receipt = receiptRepository.findById(inspection.projectId, inspection.deliveryReceiptId)
            if (receipt is DomainResult.Error) {
                return DomainResult.Error(IllegalArgumentException("Delivery receipt '${inspection.deliveryReceiptId}' not found"))
            }
            if ((receipt as DomainResult.Success).data.vendorId != inspection.vendorId) {
                return DomainResult.Error(IllegalArgumentException("Delivery receipt vendor does not match inspection vendor"))
            }
        }

        // Check duplicate reference
        val existing = qualityRepository.findInspectionByReference(inspection.projectId, inspection.inspectionReference)
        if (existing is DomainResult.Success) {
            return DomainResult.Error(IllegalArgumentException("Inspection reference '${inspection.inspectionReference}' already exists"))
        }

        val created = qualityRepository.createInspection(inspection)
        if (created is DomainResult.Success) {
            qualityRepository.appendQualityAudit(
                VendorQualityAuditEvent(
                    auditId = "AUD-${UUID.randomUUID()}",
                    projectId = inspection.projectId,
                    tenantId = inspection.tenantId,
                    entityType = "INSPECTION",
                    entityId = created.data.inspectionId,
                    eventType = "CREATED",
                    actorId = actorId,
                    details = "Quality inspection '${inspection.inspectionReference}' created in DRAFT status"
                )
            )
        }
        return created
    }

    override suspend fun startInspection(projectId: String, inspectionId: String, actorId: String): DomainResult<VendorQualityInspection> {
        val existing = qualityRepository.findInspectionById(projectId, inspectionId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateInspectionTransition(current.inspectionStatus, VendorInspectionStatus.IN_PROGRESS)
        if (transErr is DomainResult.Error) return transErr

        val updated = qualityRepository.updateInspection(
            current.copy(
                inspectionStatus = VendorInspectionStatus.IN_PROGRESS,
                inspectedBy = actorId,
                inspectionStartedAt = System.currentTimeMillis(),
                updatedBy = actorId,
                updatedAt = System.currentTimeMillis()
            )
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendQualityAudit(
                VendorQualityAuditEvent(
                    auditId = "AUD-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "INSPECTION",
                    entityId = inspectionId,
                    eventType = "STARTED",
                    actorId = actorId,
                    details = "Quality inspection started by $actorId"
                )
            )
        }
        return updated
    }

    override suspend fun completeInspection(
        projectId: String,
        inspectionId: String,
        status: VendorInspectionStatus,
        overallResult: InspectionResult,
        acceptedQty: BigDecimal,
        rejectedQty: BigDecimal,
        conditionalQty: BigDecimal,
        actorId: String
    ): DomainResult<VendorQualityInspection> {
        val existing = qualityRepository.findInspectionById(projectId, inspectionId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateInspectionTransition(current.inspectionStatus, status)
        if (transErr is DomainResult.Error) return transErr

        val sum = acceptedQty + rejectedQty + conditionalQty
        if (sum > current.receivedQuantity) {
            return DomainResult.Error(IllegalArgumentException("Inspected quantity sum ($sum) cannot exceed received quantity (${current.receivedQuantity})"))
        }

        val completed = current.copy(
            inspectionStatus = status,
            overallResult = overallResult,
            acceptedQuantity = acceptedQty,
            rejectedQuantity = rejectedQty,
            conditionalQuantity = conditionalQty,
            inspectionCompletedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )

        val valErr = VendorQualityValidator.validateInspection(completed)
        if (valErr is DomainResult.Error) return valErr

        val result = qualityRepository.updateInspection(completed)
        if (result is DomainResult.Success) {
            qualityRepository.appendQualityAudit(
                VendorQualityAuditEvent(
                    auditId = "AUD-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "INSPECTION",
                    entityId = inspectionId,
                    eventType = status.name,
                    actorId = actorId,
                    details = "Quality inspection completed with status $status, result $overallResult"
                )
            )
        }
        return result
    }

    override suspend fun updateInspection(inspection: VendorQualityInspection, actorId: String): DomainResult<VendorQualityInspection> {
        val valErr = VendorQualityValidator.validateInspection(inspection)
        if (valErr is DomainResult.Error) return valErr
        return qualityRepository.updateInspection(inspection.copy(updatedBy = actorId, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun getInspection(projectId: String, inspectionId: String): DomainResult<VendorQualityInspection> {
        return qualityRepository.findInspectionById(projectId, inspectionId)
    }

    override suspend fun listInspections(projectId: String, vendorId: String?, deliveryReceiptId: String?, status: VendorInspectionStatus?): DomainResult<List<VendorQualityInspection>> {
        return qualityRepository.listInspections(projectId, vendorId, deliveryReceiptId, status)
    }

    override suspend fun addDefect(defect: VendorDefect, actorId: String): DomainResult<VendorDefect> {
        val valErr = VendorQualityValidator.validateDefect(defect)
        if (valErr is DomainResult.Error) return valErr

        val inspection = qualityRepository.findInspectionById(defect.projectId, defect.inspectionId)
        if (inspection is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Inspection '${defect.inspectionId}' not found"))
        }

        return qualityRepository.createDefect(defect.copy(detectedBy = actorId))
    }

    override suspend fun listDefects(projectId: String, inspectionId: String): DomainResult<List<VendorDefect>> {
        return qualityRepository.listDefectsByInspection(projectId, inspectionId)
    }

    override suspend fun createRejection(rejection: VendorRejection, actorId: String): DomainResult<VendorRejection> {
        val valErr = VendorQualityValidator.validateRejection(rejection)
        if (valErr is DomainResult.Error) return valErr

        val vendor = vendorRepository.findById(rejection.projectId, rejection.vendorId)
        if (vendor is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Vendor '${rejection.vendorId}' not found"))
        }

        // Check duplicate reference
        val existing = qualityRepository.findRejectionByReference(rejection.projectId, rejection.rejectionReference)
        if (existing is DomainResult.Success) {
            return DomainResult.Error(IllegalArgumentException("Rejection reference '${rejection.rejectionReference}' already exists"))
        }

        // Validate against receipt item if supplied
        if (rejection.deliveryReceiptId != null && rejection.deliveryReceiptItemId != null && receiptRepository != null) {
            val receipt = receiptRepository.findById(rejection.projectId, rejection.deliveryReceiptId)
            if (receipt is DomainResult.Success) {
                val receiptItem = receipt.data.items.find { it.receiptItemId == rejection.deliveryReceiptItemId }
                if (receiptItem != null && rejection.rejectedQuantity > receiptItem.receivedQuantity) {
                    return DomainResult.Error(IllegalArgumentException("Rejected quantity (${rejection.rejectedQuantity}) cannot exceed received quantity (${receiptItem.receivedQuantity})"))
                }
            }
        }

        val created = qualityRepository.createRejection(rejection)
        if (created is DomainResult.Success) {
            qualityRepository.appendQualityAudit(
                VendorQualityAuditEvent(
                    auditId = "AUD-${UUID.randomUUID()}",
                    projectId = rejection.projectId,
                    tenantId = rejection.tenantId,
                    entityType = "REJECTION",
                    entityId = created.data.rejectionId,
                    eventType = "CREATED",
                    actorId = actorId,
                    details = "Vendor rejection '${rejection.rejectionReference}' created with disposition ${rejection.disposition}"
                )
            )
        }
        return created
    }

    override suspend fun submitRejection(projectId: String, rejectionId: String, actorId: String): DomainResult<VendorRejection> {
        val existing = qualityRepository.findRejectionById(projectId, rejectionId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateRejectionTransition(current.status, VendorRejectionStatus.PENDING_VENDOR_RESPONSE)
        if (transErr is DomainResult.Error) return transErr

        val updated = qualityRepository.updateRejectionStatus(
            projectId = projectId,
            rejectionId = rejectionId,
            status = VendorRejectionStatus.PENDING_VENDOR_RESPONSE,
            updatedBy = actorId
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendQualityAudit(
                VendorQualityAuditEvent(
                    auditId = "AUD-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "REJECTION",
                    entityId = rejectionId,
                    eventType = "SUBMITTED",
                    actorId = actorId,
                    details = "Rejection submitted to vendor for response"
                )
            )
        }
        return updated
    }

    override suspend fun acceptRejection(projectId: String, rejectionId: String, vendorResponse: String, actorId: String): DomainResult<VendorRejection> {
        val existing = qualityRepository.findRejectionById(projectId, rejectionId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateRejectionTransition(current.status, VendorRejectionStatus.ACCEPTED)
        if (transErr is DomainResult.Error) return transErr

        val updated = qualityRepository.updateRejectionStatus(
            projectId = projectId,
            rejectionId = rejectionId,
            status = VendorRejectionStatus.ACCEPTED,
            updatedBy = actorId,
            vendorResponse = vendorResponse
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendQualityAudit(
                VendorQualityAuditEvent(
                    auditId = "AUD-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "REJECTION",
                    entityId = rejectionId,
                    eventType = "ACCEPTED",
                    actorId = actorId,
                    details = "Rejection accepted by vendor: $vendorResponse"
                )
            )
        }
        return updated
    }

    override suspend fun disputeRejection(projectId: String, rejectionId: String, vendorResponse: String, actorId: String): DomainResult<VendorRejection> {
        val existing = qualityRepository.findRejectionById(projectId, rejectionId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateRejectionTransition(current.status, VendorRejectionStatus.DISPUTED)
        if (transErr is DomainResult.Error) return transErr

        val updated = qualityRepository.updateRejectionStatus(
            projectId = projectId,
            rejectionId = rejectionId,
            status = VendorRejectionStatus.DISPUTED,
            updatedBy = actorId,
            vendorResponse = vendorResponse
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendQualityAudit(
                VendorQualityAuditEvent(
                    auditId = "AUD-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "REJECTION",
                    entityId = rejectionId,
                    eventType = "DISPUTED",
                    actorId = actorId,
                    details = "Rejection disputed by vendor: $vendorResponse"
                )
            )
        }
        return updated
    }

    override suspend fun resolveRejection(projectId: String, rejectionId: String, resolutionNotes: String, actorId: String): DomainResult<VendorRejection> {
        val existing = qualityRepository.findRejectionById(projectId, rejectionId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateRejectionTransition(current.status, VendorRejectionStatus.RESOLVED)
        if (transErr is DomainResult.Error) return transErr

        val updated = qualityRepository.updateRejectionStatus(
            projectId = projectId,
            rejectionId = rejectionId,
            status = VendorRejectionStatus.RESOLVED,
            updatedBy = actorId,
            resolutionNotes = resolutionNotes
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendQualityAudit(
                VendorQualityAuditEvent(
                    auditId = "AUD-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "REJECTION",
                    entityId = rejectionId,
                    eventType = "RESOLVED",
                    actorId = actorId,
                    details = "Rejection resolved: $resolutionNotes"
                )
            )
        }
        return updated
    }

    override suspend fun closeRejection(projectId: String, rejectionId: String, actorId: String): DomainResult<VendorRejection> {
        val existing = qualityRepository.findRejectionById(projectId, rejectionId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateRejectionTransition(current.status, VendorRejectionStatus.CLOSED)
        if (transErr is DomainResult.Error) return transErr

        val updated = qualityRepository.updateRejectionStatus(
            projectId = projectId,
            rejectionId = rejectionId,
            status = VendorRejectionStatus.CLOSED,
            updatedBy = actorId
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendQualityAudit(
                VendorQualityAuditEvent(
                    auditId = "AUD-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    entityType = "REJECTION",
                    entityId = rejectionId,
                    eventType = "CLOSED",
                    actorId = actorId,
                    details = "Rejection closed"
                )
            )
        }
        return updated
    }

    override suspend fun getRejection(projectId: String, rejectionId: String): DomainResult<VendorRejection> {
        return qualityRepository.findRejectionById(projectId, rejectionId)
    }

    override suspend fun listRejections(projectId: String, vendorId: String?, deliveryReceiptId: String?, status: VendorRejectionStatus?): DomainResult<List<VendorRejection>> {
        return qualityRepository.listRejections(projectId, vendorId, deliveryReceiptId, status)
    }

    override suspend fun createDispute(dispute: VendorDispute, actorId: String): DomainResult<VendorDispute> {
        val valErr = VendorQualityValidator.validateDispute(dispute)
        if (valErr is DomainResult.Error) return valErr

        val vendor = vendorRepository.findById(dispute.projectId, dispute.vendorId)
        if (vendor is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Vendor '${dispute.vendorId}' not found"))
        }

        // Check duplicate reference
        val existing = qualityRepository.findDisputeByReference(dispute.projectId, dispute.disputeReference)
        if (existing is DomainResult.Success) {
            return DomainResult.Error(IllegalArgumentException("Dispute reference '${dispute.disputeReference}' already exists"))
        }

        val created = qualityRepository.createDispute(dispute)
        if (created is DomainResult.Success) {
            qualityRepository.appendDisputeEvent(
                VendorDisputeEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    projectId = dispute.projectId,
                    tenantId = dispute.tenantId,
                    disputeId = created.data.disputeId,
                    eventType = VendorDisputeEventType.CREATED,
                    actorId = actorId,
                    notes = "Dispute '${dispute.disputeReference}' created with subject: ${dispute.subject}"
                )
            )
        }
        return created
    }

    override suspend fun assignDispute(projectId: String, disputeId: String, assignedTo: String, actorId: String): DomainResult<VendorDispute> {
        val existing = qualityRepository.findDisputeById(projectId, disputeId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        if (current.status == VendorDisputeStatus.CLOSED || current.status == VendorDisputeStatus.CANCELLED) {
            return DomainResult.Error(IllegalStateException("Cannot assign dispute in status ${current.status}"))
        }

        val updated = qualityRepository.updateDispute(
            current.copy(
                assignedTo = assignedTo,
                status = if (current.status == VendorDisputeStatus.OPEN) VendorDisputeStatus.UNDER_REVIEW else current.status,
                updatedBy = actorId,
                updatedAt = System.currentTimeMillis()
            )
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendDisputeEvent(
                VendorDisputeEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    disputeId = disputeId,
                    eventType = VendorDisputeEventType.ASSIGNED,
                    actorId = actorId,
                    notes = "Dispute assigned to $assignedTo"
                )
            )
        }
        return updated
    }

    override suspend fun submitVendorResponse(projectId: String, disputeId: String, vendorResponse: String, actorId: String): DomainResult<VendorDispute> {
        val existing = qualityRepository.findDisputeById(projectId, disputeId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        if (current.status == VendorDisputeStatus.CLOSED || current.status == VendorDisputeStatus.CANCELLED) {
            return DomainResult.Error(IllegalStateException("Cannot submit response for dispute in status ${current.status}"))
        }

        val updated = qualityRepository.updateDispute(
            current.copy(
                vendorResponse = vendorResponse,
                vendorResponseAt = System.currentTimeMillis(),
                status = VendorDisputeStatus.UNDER_REVIEW,
                updatedBy = actorId,
                updatedAt = System.currentTimeMillis()
            )
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendDisputeEvent(
                VendorDisputeEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    disputeId = disputeId,
                    eventType = VendorDisputeEventType.VENDOR_RESPONDED,
                    actorId = actorId,
                    notes = "Vendor responded: $vendorResponse"
                )
            )
        }
        return updated
    }

    override suspend fun escalateDispute(projectId: String, disputeId: String, reason: String, actorId: String): DomainResult<VendorDispute> {
        val existing = qualityRepository.findDisputeById(projectId, disputeId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateDisputeTransition(current.status, VendorDisputeStatus.ESCALATED)
        if (transErr is DomainResult.Error) return transErr

        val updated = qualityRepository.updateDisputeStatus(
            projectId = projectId,
            disputeId = disputeId,
            status = VendorDisputeStatus.ESCALATED,
            updatedBy = actorId
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendDisputeEvent(
                VendorDisputeEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    disputeId = disputeId,
                    eventType = VendorDisputeEventType.ESCALATED,
                    actorId = actorId,
                    notes = "Dispute escalated: $reason"
                )
            )
        }
        return updated
    }

    override suspend fun proposeDisputeResolution(projectId: String, disputeId: String, proposal: String, actorId: String): DomainResult<VendorDispute> {
        val existing = qualityRepository.findDisputeById(projectId, disputeId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateDisputeTransition(current.status, VendorDisputeStatus.RESOLUTION_PROPOSED)
        if (transErr is DomainResult.Error) return transErr

        val updated = qualityRepository.updateDisputeStatus(
            projectId = projectId,
            disputeId = disputeId,
            status = VendorDisputeStatus.RESOLUTION_PROPOSED,
            updatedBy = actorId,
            resolutionProposal = proposal
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendDisputeEvent(
                VendorDisputeEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    disputeId = disputeId,
                    eventType = VendorDisputeEventType.RESOLUTION_PROPOSED,
                    actorId = actorId,
                    notes = "Resolution proposed: $proposal"
                )
            )
        }
        return updated
    }

    override suspend fun resolveDispute(projectId: String, disputeId: String, resolution: String, actorId: String): DomainResult<VendorDispute> {
        val existing = qualityRepository.findDisputeById(projectId, disputeId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateDisputeTransition(current.status, VendorDisputeStatus.RESOLVED)
        if (transErr is DomainResult.Error) return transErr

        val updated = qualityRepository.updateDisputeStatus(
            projectId = projectId,
            disputeId = disputeId,
            status = VendorDisputeStatus.RESOLVED,
            updatedBy = actorId,
            resolution = resolution
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendDisputeEvent(
                VendorDisputeEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    disputeId = disputeId,
                    eventType = VendorDisputeEventType.RESOLVED,
                    actorId = actorId,
                    notes = "Dispute resolved: $resolution"
                )
            )
        }
        return updated
    }

    override suspend fun closeDispute(projectId: String, disputeId: String, actorId: String): DomainResult<VendorDispute> {
        val existing = qualityRepository.findDisputeById(projectId, disputeId)
        if (existing is DomainResult.Error) return existing
        val current = (existing as DomainResult.Success).data

        val transErr = VendorQualityValidator.validateDisputeTransition(current.status, VendorDisputeStatus.CLOSED)
        if (transErr is DomainResult.Error) return transErr

        val updated = qualityRepository.updateDisputeStatus(
            projectId = projectId,
            disputeId = disputeId,
            status = VendorDisputeStatus.CLOSED,
            updatedBy = actorId
        )
        if (updated is DomainResult.Success) {
            qualityRepository.appendDisputeEvent(
                VendorDisputeEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    projectId = projectId,
                    tenantId = current.tenantId,
                    disputeId = disputeId,
                    eventType = VendorDisputeEventType.CLOSED,
                    actorId = actorId,
                    notes = "Dispute closed by $actorId"
                )
            )
        }
        return updated
    }

    override suspend fun getDispute(projectId: String, disputeId: String): DomainResult<VendorDispute> {
        return qualityRepository.findDisputeById(projectId, disputeId)
    }

    override suspend fun listDisputes(projectId: String, vendorId: String?, status: VendorDisputeStatus?, disputeType: VendorDisputeType?): DomainResult<List<VendorDispute>> {
        return qualityRepository.listDisputes(projectId, vendorId, status, disputeType)
    }

    override suspend fun listDisputeEvents(projectId: String, disputeId: String): DomainResult<List<VendorDisputeEvent>> {
        return qualityRepository.listDisputeEvents(projectId, disputeId)
    }

    override suspend fun addEvidence(evidence: VendorQualityEvidence, actorId: String): DomainResult<VendorQualityEvidence> {
        if (evidence.evidenceId.isBlank() || evidence.sourceId.isBlank() || evidence.fileReference.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Invalid evidence parameters"))
        }
        val created = qualityRepository.addEvidence(evidence.copy(uploadedBy = actorId))
        if (created is DomainResult.Success && evidence.sourceType == "DISPUTE") {
            qualityRepository.appendDisputeEvent(
                VendorDisputeEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    projectId = evidence.projectId,
                    tenantId = evidence.tenantId,
                    disputeId = evidence.sourceId,
                    eventType = VendorDisputeEventType.EVIDENCE_ADDED,
                    actorId = actorId,
                    notes = "Evidence added: ${evidence.fileName} (${evidence.fileType})"
                )
            )
        }
        return created
    }

    override suspend fun listEvidence(projectId: String, sourceType: String, sourceId: String): DomainResult<List<VendorQualityEvidence>> {
        return qualityRepository.listEvidence(projectId, sourceType, sourceId)
    }

    override suspend fun listQualityAudits(projectId: String, entityId: String): DomainResult<List<VendorQualityAuditEvent>> {
        return qualityRepository.listQualityAudits(projectId, entityId)
    }
}
