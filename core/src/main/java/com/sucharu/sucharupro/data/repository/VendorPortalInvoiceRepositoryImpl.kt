package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPortalInvoiceDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalInvoiceRepository

/**
 * Implementation of VendorPortalInvoiceRepository delegating to datasource (Module 13 Step 06).
 */
class VendorPortalInvoiceRepositoryImpl(
    private val dataSource: VendorPortalInvoiceDataSource
) : VendorPortalInvoiceRepository {

    override suspend fun saveSubmission(submission: VendorPortalInvoiceSubmission): DomainResult<VendorPortalInvoiceSubmission> {
        return try {
            DomainResult.Success(dataSource.saveSubmission(submission))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findSubmissionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        submissionId: String
    ): DomainResult<VendorPortalInvoiceSubmission?> {
        return try {
            DomainResult.Success(dataSource.findSubmissionById(tenantId, projectId, vendorId, submissionId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listSubmissions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String?,
        status: VendorPortalInvoiceSubmissionStatus?
    ): DomainResult<List<VendorPortalInvoiceSubmission>> {
        return try {
            DomainResult.Success(dataSource.listSubmissions(tenantId, projectId, vendorId, purchaseOrderId, status))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveResponse(response: VendorPortalInvoiceResponse): DomainResult<VendorPortalInvoiceResponse> {
        return try {
            DomainResult.Success(dataSource.saveResponse(response))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        invoiceId: String
    ): DomainResult<List<VendorPortalInvoiceResponse>> {
        return try {
            DomainResult.Success(dataSource.listResponses(tenantId, projectId, vendorId, invoiceId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveEvidence(evidence: VendorPortalFinancialEvidence): DomainResult<VendorPortalFinancialEvidence> {
        return try {
            DomainResult.Success(dataSource.saveEvidence(evidence))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String?,
        entityId: String?
    ): DomainResult<List<VendorPortalFinancialEvidence>> {
        return try {
            DomainResult.Success(dataSource.listEvidence(tenantId, projectId, vendorId, entityType, entityId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun recordAuditEvent(event: VendorPortalInvoiceAuditEvent): DomainResult<Unit> {
        return try {
            dataSource.recordAuditEvent(event)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        vendorId: String,
        targetType: String?,
        targetId: String?
    ): DomainResult<List<VendorPortalInvoiceAuditEvent>> {
        return try {
            DomainResult.Success(dataSource.listAuditEvents(tenantId, projectId, vendorId, targetType, targetId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }
}
