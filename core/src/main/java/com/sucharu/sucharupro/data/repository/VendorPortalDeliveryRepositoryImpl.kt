package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPortalDeliveryDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalDeliveryRepository

/**
 * Implementation of [VendorPortalDeliveryRepository] delegating to [VendorPortalDeliveryDataSource].
 */
class VendorPortalDeliveryRepositoryImpl(
    private val dataSource: VendorPortalDeliveryDataSource
) : VendorPortalDeliveryRepository {

    override suspend fun saveDeliveryNotice(notice: VendorPortalDeliveryNotice): DomainResult<VendorPortalDeliveryNotice> {
        return try {
            DomainResult.Success(dataSource.saveDeliveryNotice(notice))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun updateDeliveryNotice(notice: VendorPortalDeliveryNotice): DomainResult<VendorPortalDeliveryNotice> {
        return try {
            DomainResult.Success(dataSource.updateDeliveryNotice(notice))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findDeliveryNoticeById(noticeId: String, tenantId: String): DomainResult<VendorPortalDeliveryNotice?> {
        return try {
            DomainResult.Success(dataSource.findDeliveryNoticeById(noticeId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findDeliveryNoticeByNumber(noticeNumber: String, tenantId: String): DomainResult<VendorPortalDeliveryNotice?> {
        return try {
            DomainResult.Success(dataSource.findDeliveryNoticeByNumber(noticeNumber, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listDeliveryNotices(
        tenantId: String,
        projectId: String,
        vendorId: String,
        purchaseOrderId: String?,
        status: VendorPortalDeliveryNoticeStatus?
    ): DomainResult<List<VendorPortalDeliveryNotice>> {
        return try {
            DomainResult.Success(dataSource.listDeliveryNotices(tenantId, projectId, vendorId, purchaseOrderId, status))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveDeliveryAcknowledgement(ack: VendorPortalDeliveryAcknowledgement): DomainResult<VendorPortalDeliveryAcknowledgement> {
        return try {
            DomainResult.Success(dataSource.saveDeliveryAcknowledgement(ack))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findDeliveryAcknowledgement(noticeId: String, tenantId: String): DomainResult<VendorPortalDeliveryAcknowledgement?> {
        return try {
            DomainResult.Success(dataSource.findDeliveryAcknowledgement(noticeId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveQualityResponse(response: VendorPortalQualityResponse): DomainResult<VendorPortalQualityResponse> {
        return try {
            DomainResult.Success(dataSource.saveQualityResponse(response))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findQualityResponseById(responseId: String, tenantId: String): DomainResult<VendorPortalQualityResponse?> {
        return try {
            DomainResult.Success(dataSource.findQualityResponseById(responseId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listQualityResponses(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String?,
        rejectionId: String?
    ): DomainResult<List<VendorPortalQualityResponse>> {
        return try {
            DomainResult.Success(dataSource.listQualityResponses(tenantId, projectId, vendorId, inspectionId, rejectionId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveException(exception: VendorPortalDeliveryException): DomainResult<VendorPortalDeliveryException> {
        return try {
            DomainResult.Success(dataSource.saveException(exception))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun updateException(exception: VendorPortalDeliveryException): DomainResult<VendorPortalDeliveryException> {
        return try {
            DomainResult.Success(dataSource.updateException(exception))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun findExceptionById(exceptionId: String, tenantId: String): DomainResult<VendorPortalDeliveryException?> {
        return try {
            DomainResult.Success(dataSource.findExceptionById(exceptionId, tenantId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listExceptions(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDeliveryExceptionStatus?,
        sourceType: String?
    ): DomainResult<List<VendorPortalDeliveryException>> {
        return try {
            DomainResult.Success(dataSource.listExceptions(tenantId, projectId, vendorId, status, sourceType))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun saveEvidence(evidence: VendorPortalDeliveryEvidence): DomainResult<VendorPortalDeliveryEvidence> {
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
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalDeliveryEvidence>> {
        return try {
            DomainResult.Success(dataSource.listEvidence(tenantId, projectId, vendorId, entityType, entityId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun recordAuditEvent(event: VendorPortalDeliveryAuditEvent): DomainResult<VendorPortalDeliveryAuditEvent> {
        return try {
            DomainResult.Success(dataSource.recordAuditEvent(event))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalDeliveryAuditEvent>> {
        return try {
            DomainResult.Success(dataSource.listAuditEvents(tenantId, entityType, entityId))
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }
}
