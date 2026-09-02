package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

interface VendorRfqService {

    suspend fun createRfq(
        rfq: VendorRfq,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq>

    suspend fun publishRfq(
        rfqId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq>

    suspend fun openRfq(
        rfqId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq>

    suspend fun closeRfq(
        rfqId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq>

    suspend fun cancelRfq(
        rfqId: String,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq>

    suspend fun extendDeadline(
        rfqId: String,
        newDeadline: Long,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq>

    suspend fun getRfqById(
        rfqId: String,
        tenantId: String
    ): DomainResult<VendorRfq>

    suspend fun listRfqs(
        projectId: String?,
        status: VendorRfqStatus?,
        tenantId: String
    ): DomainResult<List<VendorRfq>>

    // Vendor Portal Scoped Views
    suspend fun listRfqsForVendor(
        vendorId: String,
        status: VendorRfqStatus?,
        tenantId: String
    ): DomainResult<List<VendorRfq>>

    suspend fun getRfqForVendor(
        rfqId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorRfq>

    // Vendor Invitations
    suspend fun inviteVendor(
        rfqId: String,
        vendorId: String,
        tenantId: String,
        projectId: String,
        actorId: String
    ): DomainResult<VendorRfqInvitation>

    suspend fun acknowledgeInvitation(
        rfqId: String,
        vendorId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfqInvitation>

    suspend fun declineInvitation(
        rfqId: String,
        vendorId: String,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfqInvitation>

    suspend fun listInvitations(
        rfqId: String,
        tenantId: String
    ): DomainResult<List<VendorRfqInvitation>>

    // Clarifications
    suspend fun askClarification(
        rfqId: String,
        vendorId: String,
        question: String,
        askedBy: String,
        tenantId: String,
        projectId: String
    ): DomainResult<VendorRfqClarification>

    suspend fun answerClarification(
        clarificationId: String,
        answer: String,
        answeredBy: String,
        visibility: VendorClarificationVisibility,
        tenantId: String
    ): DomainResult<VendorRfqClarification>

    suspend fun listClarifications(
        rfqId: String,
        vendorId: String?,
        tenantId: String
    ): DomainResult<List<VendorRfqClarification>>

    // Audit
    suspend fun listAuditEvents(
        rfqId: String,
        tenantId: String
    ): DomainResult<List<VendorRfqAuditEvent>>
}
