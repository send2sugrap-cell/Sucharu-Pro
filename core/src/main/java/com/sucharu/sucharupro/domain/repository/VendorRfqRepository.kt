package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

interface VendorRfqRepository {
    suspend fun createRfq(rfq: VendorRfq): DomainResult<VendorRfq>
    suspend fun updateRfq(rfq: VendorRfq): DomainResult<VendorRfq>
    suspend fun findRfqById(rfqId: String, tenantId: String): DomainResult<VendorRfq>
    suspend fun findRfqByNumber(rfqNumber: String, tenantId: String): DomainResult<VendorRfq>
    suspend fun listRfqs(projectId: String?, status: VendorRfqStatus?, tenantId: String): DomainResult<List<VendorRfq>>
    suspend fun listRfqsForVendor(vendorId: String, status: VendorRfqStatus?, tenantId: String): DomainResult<List<VendorRfq>>

    // Invitations
    suspend fun createInvitation(invitation: VendorRfqInvitation): DomainResult<VendorRfqInvitation>
    suspend fun updateInvitation(invitation: VendorRfqInvitation): DomainResult<VendorRfqInvitation>
    suspend fun findInvitation(rfqId: String, vendorId: String, tenantId: String): DomainResult<VendorRfqInvitation>
    suspend fun listInvitationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorRfqInvitation>>
    suspend fun listInvitationsByVendor(vendorId: String, tenantId: String): DomainResult<List<VendorRfqInvitation>>

    // Clarifications
    suspend fun createClarification(clarification: VendorRfqClarification): DomainResult<VendorRfqClarification>
    suspend fun updateClarification(clarification: VendorRfqClarification): DomainResult<VendorRfqClarification>
    suspend fun findClarificationById(clarificationId: String, tenantId: String): DomainResult<VendorRfqClarification>
    suspend fun listClarifications(rfqId: String, vendorId: String?, tenantId: String): DomainResult<List<VendorRfqClarification>>

    // Audit
    suspend fun recordAuditEvent(event: VendorRfqAuditEvent): DomainResult<VendorRfqAuditEvent>
    suspend fun listAuditEvents(rfqId: String, tenantId: String): DomainResult<List<VendorRfqAuditEvent>>
}
