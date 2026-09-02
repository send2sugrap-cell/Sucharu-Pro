package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Fake Data Source for Vendor Quality testing.
 */
class FakeVendorQualityDataSource : VendorQualityDataSource {

    private val inspections = ConcurrentHashMap<String, VendorQualityInspection>()
    private val defects = ConcurrentHashMap<String, VendorDefect>()
    private val rejections = ConcurrentHashMap<String, VendorRejection>()
    private val disputes = ConcurrentHashMap<String, VendorDispute>()
    private val disputeEvents = ConcurrentHashMap<String, MutableList<VendorDisputeEvent>>()
    private val qualityAudits = ConcurrentHashMap<String, MutableList<VendorQualityAuditEvent>>()
    private val evidenceList = ConcurrentHashMap<String, MutableList<VendorQualityEvidence>>()

    private val inspectionFlows = ConcurrentHashMap<String, MutableStateFlow<List<VendorQualityInspection>>>()
    private val rejectionFlows = ConcurrentHashMap<String, MutableStateFlow<List<VendorRejection>>>()
    private val disputeFlows = ConcurrentHashMap<String, MutableStateFlow<List<VendorDispute>>>()

    private fun compositeKey(projectId: String, id: String): String = "$projectId:$id"

    override fun observeInspections(projectId: String, vendorId: String?, deliveryReceiptId: String?): Flow<List<VendorQualityInspection>> {
        val key = "$projectId:$vendorId:$deliveryReceiptId"
        return inspectionFlows.getOrPut(key) {
            val initial = inspections.values.filter {
                it.projectId == projectId &&
                        (vendorId == null || it.vendorId == vendorId) &&
                        (deliveryReceiptId == null || it.deliveryReceiptId == deliveryReceiptId)
            }
            MutableStateFlow(initial)
        }.asStateFlow()
    }

    override suspend fun findInspectionById(projectId: String, inspectionId: String): DomainResult<VendorQualityInspection> {
        val insp = inspections[compositeKey(projectId, inspectionId)]
        return if (insp != null) DomainResult.Success(insp)
        else DomainResult.Error(NoSuchElementException("Quality inspection '$inspectionId' not found"))
    }

    override suspend fun findInspectionByReference(projectId: String, reference: String): DomainResult<VendorQualityInspection> {
        val insp = inspections.values.find { it.projectId == projectId && it.inspectionReference.equals(reference, ignoreCase = true) }
        return if (insp != null) DomainResult.Success(insp)
        else DomainResult.Error(NoSuchElementException("Quality inspection '$reference' not found"))
    }

    override suspend fun listInspections(
        projectId: String,
        vendorId: String?,
        deliveryReceiptId: String?,
        status: VendorInspectionStatus?
    ): DomainResult<List<VendorQualityInspection>> {
        val filtered = inspections.values.filter {
            it.projectId == projectId &&
                    (vendorId == null || it.vendorId == vendorId) &&
                    (deliveryReceiptId == null || it.deliveryReceiptId == deliveryReceiptId) &&
                    (status == null || it.inspectionStatus == status)
        }.sortedByDescending { it.createdAt }
        return DomainResult.Success(filtered)
    }

    override suspend fun createInspection(inspection: VendorQualityInspection): DomainResult<VendorQualityInspection> = synchronized(inspections) {
        val key = compositeKey(inspection.projectId, inspection.inspectionId)
        if (inspections.containsKey(key)) {
            return@synchronized DomainResult.Error(IllegalStateException("Quality inspection '${inspection.inspectionId}' already exists"))
        }
        val duplicateRef = inspections.values.any { it.projectId == inspection.projectId && it.inspectionReference == inspection.inspectionReference }
        if (duplicateRef) {
            return@synchronized DomainResult.Error(IllegalArgumentException("Inspection reference '${inspection.inspectionReference}' already exists"))
        }
        inspections[key] = inspection
        DomainResult.Success(inspection)
    }

    override suspend fun updateInspection(inspection: VendorQualityInspection): DomainResult<VendorQualityInspection> = synchronized(inspections) {
        val key = compositeKey(inspection.projectId, inspection.inspectionId)
        val existing = inspections[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("Quality inspection '${inspection.inspectionId}' not found"))
        if (existing.version != inspection.version) {
            return@synchronized DomainResult.Error(IllegalStateException("Optimistic lock conflict on inspection '${inspection.inspectionId}'"))
        }
        val updated = inspection.copy(version = inspection.version + 1, updatedAt = System.currentTimeMillis())
        inspections[key] = updated
        DomainResult.Success(updated)
    }

    override suspend fun updateInspectionStatus(
        projectId: String,
        inspectionId: String,
        status: VendorInspectionStatus,
        overallResult: InspectionResult?,
        updatedBy: String
    ): DomainResult<VendorQualityInspection> = synchronized(inspections) {
        val key = compositeKey(projectId, inspectionId)
        val existing = inspections[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("Quality inspection '$inspectionId' not found"))
        val updated = existing.copy(
            inspectionStatus = status,
            overallResult = overallResult ?: existing.overallResult,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        inspections[key] = updated
        DomainResult.Success(updated)
    }

    override suspend fun createDefect(defect: VendorDefect): DomainResult<VendorDefect> = synchronized(defects) {
        val key = compositeKey(defect.projectId, defect.defectId)
        defects[key] = defect
        DomainResult.Success(defect)
    }

    override suspend fun listDefectsByInspection(projectId: String, inspectionId: String): DomainResult<List<VendorDefect>> {
        val list = defects.values.filter { it.projectId == projectId && it.inspectionId == inspectionId }.sortedBy { it.createdAt }
        return DomainResult.Success(list)
    }

    override fun observeRejections(projectId: String, vendorId: String?, deliveryReceiptId: String?): Flow<List<VendorRejection>> {
        val key = "$projectId:$vendorId:$deliveryReceiptId"
        return rejectionFlows.getOrPut(key) {
            val initial = rejections.values.filter {
                it.projectId == projectId &&
                        (vendorId == null || it.vendorId == vendorId) &&
                        (deliveryReceiptId == null || it.deliveryReceiptId == deliveryReceiptId)
            }
            MutableStateFlow(initial)
        }.asStateFlow()
    }

    override suspend fun findRejectionById(projectId: String, rejectionId: String): DomainResult<VendorRejection> {
        val rej = rejections[compositeKey(projectId, rejectionId)]
        return if (rej != null) DomainResult.Success(rej)
        else DomainResult.Error(NoSuchElementException("Vendor rejection '$rejectionId' not found"))
    }

    override suspend fun findRejectionByReference(projectId: String, reference: String): DomainResult<VendorRejection> {
        val rej = rejections.values.find { it.projectId == projectId && it.rejectionReference.equals(reference, ignoreCase = true) }
        return if (rej != null) DomainResult.Success(rej)
        else DomainResult.Error(NoSuchElementException("Vendor rejection '$reference' not found"))
    }

    override suspend fun listRejections(
        projectId: String,
        vendorId: String?,
        deliveryReceiptId: String?,
        status: VendorRejectionStatus?
    ): DomainResult<List<VendorRejection>> {
        val filtered = rejections.values.filter {
            it.projectId == projectId &&
                    (vendorId == null || it.vendorId == vendorId) &&
                    (deliveryReceiptId == null || it.deliveryReceiptId == deliveryReceiptId) &&
                    (status == null || it.status == status)
        }.sortedByDescending { it.createdAt }
        return DomainResult.Success(filtered)
    }

    override suspend fun createRejection(rejection: VendorRejection): DomainResult<VendorRejection> = synchronized(rejections) {
        val key = compositeKey(rejection.projectId, rejection.rejectionId)
        if (rejections.containsKey(key)) {
            return@synchronized DomainResult.Error(IllegalStateException("Vendor rejection '${rejection.rejectionId}' already exists"))
        }
        val duplicateRef = rejections.values.any { it.projectId == rejection.projectId && it.rejectionReference == rejection.rejectionReference }
        if (duplicateRef) {
            return@synchronized DomainResult.Error(IllegalArgumentException("Rejection reference '${rejection.rejectionReference}' already exists"))
        }
        rejections[key] = rejection
        DomainResult.Success(rejection)
    }

    override suspend fun updateRejection(rejection: VendorRejection): DomainResult<VendorRejection> = synchronized(rejections) {
        val key = compositeKey(rejection.projectId, rejection.rejectionId)
        val existing = rejections[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("Vendor rejection '${rejection.rejectionId}' not found"))
        if (existing.version != rejection.version) {
            return@synchronized DomainResult.Error(IllegalStateException("Optimistic lock conflict on rejection '${rejection.rejectionId}'"))
        }
        val updated = rejection.copy(version = rejection.version + 1, updatedAt = System.currentTimeMillis())
        rejections[key] = updated
        DomainResult.Success(updated)
    }

    override suspend fun updateRejectionStatus(
        projectId: String,
        rejectionId: String,
        status: VendorRejectionStatus,
        updatedBy: String,
        vendorResponse: String?,
        resolutionNotes: String?
    ): DomainResult<VendorRejection> = synchronized(rejections) {
        val key = compositeKey(projectId, rejectionId)
        val existing = rejections[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("Vendor rejection '$rejectionId' not found"))
        val updated = existing.copy(
            status = status,
            vendorResponse = vendorResponse ?: existing.vendorResponse,
            vendorResponseAt = if (vendorResponse != null) System.currentTimeMillis() else existing.vendorResponseAt,
            resolutionNotes = resolutionNotes ?: existing.resolutionNotes,
            resolvedAt = if (status == VendorRejectionStatus.RESOLVED) System.currentTimeMillis() else existing.resolvedAt,
            resolvedBy = if (status == VendorRejectionStatus.RESOLVED) updatedBy else existing.resolvedBy,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        rejections[key] = updated
        DomainResult.Success(updated)
    }

    override fun observeDisputes(projectId: String, vendorId: String?, status: VendorDisputeStatus?): Flow<List<VendorDispute>> {
        val key = "$projectId:$vendorId:$status"
        return disputeFlows.getOrPut(key) {
            val initial = disputes.values.filter {
                it.projectId == projectId &&
                        (vendorId == null || it.vendorId == vendorId) &&
                        (status == null || it.status == status)
            }
            MutableStateFlow(initial)
        }.asStateFlow()
    }

    override suspend fun findDisputeById(projectId: String, disputeId: String): DomainResult<VendorDispute> {
        val disp = disputes[compositeKey(projectId, disputeId)]
        return if (disp != null) DomainResult.Success(disp)
        else DomainResult.Error(NoSuchElementException("Vendor dispute '$disputeId' not found"))
    }

    override suspend fun findDisputeByReference(projectId: String, reference: String): DomainResult<VendorDispute> {
        val disp = disputes.values.find { it.projectId == projectId && it.disputeReference.equals(reference, ignoreCase = true) }
        return if (disp != null) DomainResult.Success(disp)
        else DomainResult.Error(NoSuchElementException("Vendor dispute '$reference' not found"))
    }

    override suspend fun listDisputes(
        projectId: String,
        vendorId: String?,
        status: VendorDisputeStatus?,
        disputeType: VendorDisputeType?
    ): DomainResult<List<VendorDispute>> {
        val filtered = disputes.values.filter {
            it.projectId == projectId &&
                    (vendorId == null || it.vendorId == vendorId) &&
                    (status == null || it.status == status) &&
                    (disputeType == null || it.disputeType == disputeType)
        }.sortedByDescending { it.createdAt }
        return DomainResult.Success(filtered)
    }

    override suspend fun createDispute(dispute: VendorDispute): DomainResult<VendorDispute> = synchronized(disputes) {
        val key = compositeKey(dispute.projectId, dispute.disputeId)
        if (disputes.containsKey(key)) {
            return@synchronized DomainResult.Error(IllegalStateException("Vendor dispute '${dispute.disputeId}' already exists"))
        }
        val duplicateRef = disputes.values.any { it.projectId == dispute.projectId && it.disputeReference == dispute.disputeReference }
        if (duplicateRef) {
            return@synchronized DomainResult.Error(IllegalArgumentException("Dispute reference '${dispute.disputeReference}' already exists"))
        }
        disputes[key] = dispute
        DomainResult.Success(dispute)
    }

    override suspend fun updateDispute(dispute: VendorDispute): DomainResult<VendorDispute> = synchronized(disputes) {
        val key = compositeKey(dispute.projectId, dispute.disputeId)
        val existing = disputes[key] ?: return@synchronized DomainResult.Error(NoSuchElementException("Vendor dispute '${dispute.disputeId}' not found"))
        if (existing.version != dispute.version) {
            return@synchronized DomainResult.Error(IllegalStateException("Optimistic lock conflict on dispute '${dispute.disputeId}'"))
        }
        val updated = dispute.copy(version = dispute.version + 1, updatedAt = System.currentTimeMillis())
        disputes[key] = updated
        DomainResult.Success(updated)
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
        val key = compositeKey(projectId, disputeId)
        val existing = disputes[key] ?: return DomainResult.Error(NoSuchElementException("Vendor dispute '$disputeId' not found"))
        val updated = existing.copy(
            status = status,
            vendorResponse = vendorResponse ?: existing.vendorResponse,
            vendorResponseAt = if (vendorResponse != null) System.currentTimeMillis() else existing.vendorResponseAt,
            resolutionProposal = resolutionProposal ?: existing.resolutionProposal,
            resolution = resolution ?: existing.resolution,
            resolvedAt = if (status == VendorDisputeStatus.RESOLVED) System.currentTimeMillis() else existing.resolvedAt,
            resolvedBy = if (status == VendorDisputeStatus.RESOLVED) updatedBy else existing.resolvedBy,
            closedAt = if (status == VendorDisputeStatus.CLOSED) System.currentTimeMillis() else existing.closedAt,
            closedBy = if (status == VendorDisputeStatus.CLOSED) updatedBy else existing.closedBy,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        disputes[key] = updated
        return DomainResult.Success(updated)
    }

    override suspend fun appendDisputeEvent(event: VendorDisputeEvent): DomainResult<VendorDisputeEvent> {
        val key = compositeKey(event.projectId, event.disputeId)
        disputeEvents.getOrPut(key) { mutableListOf() }.add(event)
        return DomainResult.Success(event)
    }

    override suspend fun listDisputeEvents(projectId: String, disputeId: String): DomainResult<List<VendorDisputeEvent>> {
        val key = compositeKey(projectId, disputeId)
        val list = disputeEvents[key]?.toList() ?: emptyList()
        return DomainResult.Success(list)
    }

    override suspend fun appendQualityAudit(auditEvent: VendorQualityAuditEvent): DomainResult<VendorQualityAuditEvent> {
        val key = compositeKey(auditEvent.projectId, auditEvent.entityId)
        qualityAudits.getOrPut(key) { mutableListOf() }.add(auditEvent)
        return DomainResult.Success(auditEvent)
    }

    override suspend fun listQualityAudits(projectId: String, entityId: String): DomainResult<List<VendorQualityAuditEvent>> {
        val key = compositeKey(projectId, entityId)
        val list = qualityAudits[key]?.toList() ?: emptyList()
        return DomainResult.Success(list)
    }

    override suspend fun addEvidence(evidence: VendorQualityEvidence): DomainResult<VendorQualityEvidence> {
        val key = "${evidence.projectId}:${evidence.sourceType}:${evidence.sourceId}"
        evidenceList.getOrPut(key) { mutableListOf() }.add(evidence)
        return DomainResult.Success(evidence)
    }

    override suspend fun listEvidence(projectId: String, sourceType: String, sourceId: String): DomainResult<List<VendorQualityEvidence>> {
        val key = "$projectId:$sourceType:$sourceId"
        val list = evidenceList[key]?.toList() ?: emptyList()
        return DomainResult.Success(list)
    }
}
