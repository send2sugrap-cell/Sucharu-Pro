package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.util.concurrent.ConcurrentHashMap

class FakeVendorRfqDataSource : VendorRfqDataSource {

    private val rfqs = ConcurrentHashMap<String, VendorRfq>()
    private val invitations = ConcurrentHashMap<String, VendorRfqInvitation>()
    private val clarifications = ConcurrentHashMap<String, VendorRfqClarification>()
    private val auditEvents = ConcurrentHashMap<String, VendorRfqAuditEvent>()

    override suspend fun insertRfq(rfq: VendorRfq): DomainResult<VendorRfq> {
        val existing = rfqs.values.firstOrNull { it.tenantId == rfq.tenantId && it.rfqNumber.equals(rfq.rfqNumber, ignoreCase = true) }
        if (existing != null) {
            return DomainResult.Error(IllegalStateException("RFQ number '${rfq.rfqNumber}' already exists."))
        }
        rfqs[rfq.rfqId] = rfq
        return DomainResult.Success(rfq)
    }

    override suspend fun updateRfq(rfq: VendorRfq): DomainResult<VendorRfq> {
        val current = rfqs[rfq.rfqId]
            ?: return DomainResult.Error(NoSuchElementException("RFQ '${rfq.rfqId}' not found."))
        if (current.version != rfq.version - 1 && current.version != rfq.version) {
            return DomainResult.Error(IllegalStateException("Optimistic lock conflict on RFQ '${rfq.rfqId}'."))
        }
        rfqs[rfq.rfqId] = rfq
        return DomainResult.Success(rfq)
    }

    override suspend fun fetchRfqById(rfqId: String, tenantId: String): DomainResult<VendorRfq> {
        val rfq = rfqs[rfqId]
        return if (rfq != null && rfq.tenantId == tenantId) {
            DomainResult.Success(rfq)
        } else {
            DomainResult.Error(NoSuchElementException("RFQ '$rfqId' not found."))
        }
    }

    override suspend fun fetchRfqByNumber(rfqNumber: String, tenantId: String): DomainResult<VendorRfq> {
        val rfq = rfqs.values.firstOrNull { it.tenantId == tenantId && it.rfqNumber.equals(rfqNumber, ignoreCase = true) }
        return if (rfq != null) {
            DomainResult.Success(rfq)
        } else {
            DomainResult.Error(NoSuchElementException("RFQ '$rfqNumber' not found."))
        }
    }

    override suspend fun listRfqs(projectId: String?, status: VendorRfqStatus?, tenantId: String): DomainResult<List<VendorRfq>> {
        val list = rfqs.values.filter {
            it.tenantId == tenantId &&
            (projectId == null || it.projectId == projectId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.createdAt }
        return DomainResult.Success(list)
    }

    override suspend fun listRfqsForVendor(vendorId: String, status: VendorRfqStatus?, tenantId: String): DomainResult<List<VendorRfq>> {
        val invitedRfqIds = invitations.values.filter { it.tenantId == tenantId && it.vendorId == vendorId }.map { it.rfqId }.toSet()
        val list = rfqs.values.filter {
            it.tenantId == tenantId &&
            it.rfqId in invitedRfqIds &&
            (status == null || it.status == status)
        }.sortedByDescending { it.createdAt }
        return DomainResult.Success(list)
    }

    override suspend fun insertInvitation(invitation: VendorRfqInvitation): DomainResult<VendorRfqInvitation> {
        val key = "${invitation.rfqId}_${invitation.vendorId}"
        if (invitations.containsKey(key)) {
            return DomainResult.Error(IllegalStateException("Vendor '${invitation.vendorId}' is already invited to RFQ '${invitation.rfqId}'."))
        }
        invitations[key] = invitation
        return DomainResult.Success(invitation)
    }

    override suspend fun updateInvitation(invitation: VendorRfqInvitation): DomainResult<VendorRfqInvitation> {
        val key = "${invitation.rfqId}_${invitation.vendorId}"
        if (!invitations.containsKey(key)) {
            return DomainResult.Error(NoSuchElementException("Invitation for vendor '${invitation.vendorId}' on RFQ '${invitation.rfqId}' not found."))
        }
        invitations[key] = invitation
        return DomainResult.Success(invitation)
    }

    override suspend fun fetchInvitation(rfqId: String, vendorId: String, tenantId: String): DomainResult<VendorRfqInvitation> {
        val key = "${rfqId}_${vendorId}"
        val inv = invitations[key]
        return if (inv != null && inv.tenantId == tenantId) {
            DomainResult.Success(inv)
        } else {
            DomainResult.Error(NoSuchElementException("Invitation for vendor '$vendorId' on RFQ '$rfqId' not found."))
        }
    }

    override suspend fun listInvitationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorRfqInvitation>> {
        val list = invitations.values.filter { it.tenantId == tenantId && it.rfqId == rfqId }.sortedBy { it.invitedAt }
        return DomainResult.Success(list)
    }

    override suspend fun listInvitationsByVendor(vendorId: String, tenantId: String): DomainResult<List<VendorRfqInvitation>> {
        val list = invitations.values.filter { it.tenantId == tenantId && it.vendorId == vendorId }.sortedByDescending { it.invitedAt }
        return DomainResult.Success(list)
    }

    override suspend fun insertClarification(clarification: VendorRfqClarification): DomainResult<VendorRfqClarification> {
        clarifications[clarification.clarificationId] = clarification
        return DomainResult.Success(clarification)
    }

    override suspend fun updateClarification(clarification: VendorRfqClarification): DomainResult<VendorRfqClarification> {
        if (!clarifications.containsKey(clarification.clarificationId)) {
            return DomainResult.Error(NoSuchElementException("Clarification '${clarification.clarificationId}' not found."))
        }
        clarifications[clarification.clarificationId] = clarification
        return DomainResult.Success(clarification)
    }

    override suspend fun fetchClarificationById(clarificationId: String, tenantId: String): DomainResult<VendorRfqClarification> {
        val c = clarifications[clarificationId]
        return if (c != null && c.tenantId == tenantId) {
            DomainResult.Success(c)
        } else {
            DomainResult.Error(NoSuchElementException("Clarification '$clarificationId' not found."))
        }
    }

    override suspend fun listClarifications(rfqId: String, vendorId: String?, tenantId: String): DomainResult<List<VendorRfqClarification>> {
        val list = clarifications.values.filter {
            it.tenantId == tenantId &&
            it.rfqId == rfqId &&
            (vendorId == null || it.visibility == VendorClarificationVisibility.PUBLIC_TO_ALL_INVITED || it.vendorId == vendorId)
        }.sortedBy { it.askedAt }
        return DomainResult.Success(list)
    }

    override suspend fun insertAuditEvent(event: VendorRfqAuditEvent): DomainResult<VendorRfqAuditEvent> {
        auditEvents[event.eventId] = event
        return DomainResult.Success(event)
    }

    override suspend fun listAuditEvents(rfqId: String, tenantId: String): DomainResult<List<VendorRfqAuditEvent>> {
        val list = auditEvents.values.filter { it.tenantId == tenantId && it.rfqId == rfqId }.sortedBy { it.timestamp }
        return DomainResult.Success(list)
    }
}
