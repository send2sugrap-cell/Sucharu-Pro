package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

interface VendorRfqDataSource {
    suspend fun insertRfq(rfq: VendorRfq): DomainResult<VendorRfq>
    suspend fun updateRfq(rfq: VendorRfq): DomainResult<VendorRfq>
    suspend fun fetchRfqById(rfqId: String, tenantId: String): DomainResult<VendorRfq>
    suspend fun fetchRfqByNumber(rfqNumber: String, tenantId: String): DomainResult<VendorRfq>
    suspend fun listRfqs(projectId: String?, status: VendorRfqStatus?, tenantId: String): DomainResult<List<VendorRfq>>
    suspend fun listRfqsForVendor(vendorId: String, status: VendorRfqStatus?, tenantId: String): DomainResult<List<VendorRfq>>

    // Invitations
    suspend fun insertInvitation(invitation: VendorRfqInvitation): DomainResult<VendorRfqInvitation>
    suspend fun updateInvitation(invitation: VendorRfqInvitation): DomainResult<VendorRfqInvitation>
    suspend fun fetchInvitation(rfqId: String, vendorId: String, tenantId: String): DomainResult<VendorRfqInvitation>
    suspend fun listInvitationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorRfqInvitation>>
    suspend fun listInvitationsByVendor(vendorId: String, tenantId: String): DomainResult<List<VendorRfqInvitation>>

    // Clarifications
    suspend fun insertClarification(clarification: VendorRfqClarification): DomainResult<VendorRfqClarification>
    suspend fun updateClarification(clarification: VendorRfqClarification): DomainResult<VendorRfqClarification>
    suspend fun fetchClarificationById(clarificationId: String, tenantId: String): DomainResult<VendorRfqClarification>
    suspend fun listClarifications(rfqId: String, vendorId: String?, tenantId: String): DomainResult<List<VendorRfqClarification>>

    // Audit
    suspend fun insertAuditEvent(event: VendorRfqAuditEvent): DomainResult<VendorRfqAuditEvent>
    suspend fun listAuditEvents(rfqId: String, tenantId: String): DomainResult<List<VendorRfqAuditEvent>>
}
