package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorRfqDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorRfqRepository

class VendorRfqRepositoryImpl(
    private val dataSource: VendorRfqDataSource
) : VendorRfqRepository {

    override suspend fun createRfq(rfq: VendorRfq): DomainResult<VendorRfq> =
        dataSource.insertRfq(rfq)

    override suspend fun updateRfq(rfq: VendorRfq): DomainResult<VendorRfq> =
        dataSource.updateRfq(rfq)

    override suspend fun findRfqById(rfqId: String, tenantId: String): DomainResult<VendorRfq> =
        dataSource.fetchRfqById(rfqId, tenantId)

    override suspend fun findRfqByNumber(rfqNumber: String, tenantId: String): DomainResult<VendorRfq> =
        dataSource.fetchRfqByNumber(rfqNumber, tenantId)

    override suspend fun listRfqs(projectId: String?, status: VendorRfqStatus?, tenantId: String): DomainResult<List<VendorRfq>> =
        dataSource.listRfqs(projectId, status, tenantId)

    override suspend fun listRfqsForVendor(vendorId: String, status: VendorRfqStatus?, tenantId: String): DomainResult<List<VendorRfq>> =
        dataSource.listRfqsForVendor(vendorId, status, tenantId)

    override suspend fun createInvitation(invitation: VendorRfqInvitation): DomainResult<VendorRfqInvitation> =
        dataSource.insertInvitation(invitation)

    override suspend fun updateInvitation(invitation: VendorRfqInvitation): DomainResult<VendorRfqInvitation> =
        dataSource.updateInvitation(invitation)

    override suspend fun findInvitation(rfqId: String, vendorId: String, tenantId: String): DomainResult<VendorRfqInvitation> =
        dataSource.fetchInvitation(rfqId, vendorId, tenantId)

    override suspend fun listInvitationsByRfq(rfqId: String, tenantId: String): DomainResult<List<VendorRfqInvitation>> =
        dataSource.listInvitationsByRfq(rfqId, tenantId)

    override suspend fun listInvitationsByVendor(vendorId: String, tenantId: String): DomainResult<List<VendorRfqInvitation>> =
        dataSource.listInvitationsByVendor(vendorId, tenantId)

    override suspend fun createClarification(clarification: VendorRfqClarification): DomainResult<VendorRfqClarification> =
        dataSource.insertClarification(clarification)

    override suspend fun updateClarification(clarification: VendorRfqClarification): DomainResult<VendorRfqClarification> =
        dataSource.updateClarification(clarification)

    override suspend fun findClarificationById(clarificationId: String, tenantId: String): DomainResult<VendorRfqClarification> =
        dataSource.fetchClarificationById(clarificationId, tenantId)

    override suspend fun listClarifications(rfqId: String, vendorId: String?, tenantId: String): DomainResult<List<VendorRfqClarification>> =
        dataSource.listClarifications(rfqId, vendorId, tenantId)

    override suspend fun recordAuditEvent(event: VendorRfqAuditEvent): DomainResult<VendorRfqAuditEvent> =
        dataSource.insertAuditEvent(event)

    override suspend fun listAuditEvents(rfqId: String, tenantId: String): DomainResult<List<VendorRfqAuditEvent>> =
        dataSource.listAuditEvents(rfqId, tenantId)
}
