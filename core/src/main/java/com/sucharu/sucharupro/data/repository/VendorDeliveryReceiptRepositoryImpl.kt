package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorDeliveryReceiptDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceipt
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptAuditEvent
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptStatus
import com.sucharu.sucharupro.domain.repository.VendorDeliveryReceiptRepository
import kotlinx.coroutines.flow.Flow

class VendorDeliveryReceiptRepositoryImpl(
    private val dataSource: VendorDeliveryReceiptDataSource
) : VendorDeliveryReceiptRepository {

    override fun observeDeliveryReceipts(projectId: String, vendorId: String?, purchaseOrderId: String?): Flow<List<VendorDeliveryReceipt>> {
        return dataSource.observeDeliveryReceipts(projectId, vendorId, purchaseOrderId)
    }

    override suspend fun findById(projectId: String, deliveryReceiptId: String): DomainResult<VendorDeliveryReceipt> {
        return dataSource.findById(projectId, deliveryReceiptId)
    }

    override suspend fun findByReceiptNumber(projectId: String, receiptNumber: String): DomainResult<VendorDeliveryReceipt> {
        return dataSource.findByReceiptNumber(projectId, receiptNumber)
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        purchaseOrderId: String?,
        status: VendorDeliveryReceiptStatus?
    ): DomainResult<List<VendorDeliveryReceipt>> {
        return dataSource.list(projectId, vendorId, purchaseOrderId, status)
    }

    override suspend fun createReceipt(receipt: VendorDeliveryReceipt): DomainResult<VendorDeliveryReceipt> {
        return dataSource.createReceipt(receipt)
    }

    override suspend fun updateReceipt(receipt: VendorDeliveryReceipt): DomainResult<VendorDeliveryReceipt> {
        return dataSource.updateReceipt(receipt)
    }

    override suspend fun updateStatus(
        projectId: String,
        deliveryReceiptId: String,
        status: VendorDeliveryReceiptStatus,
        updatedBy: String
    ): DomainResult<VendorDeliveryReceipt> {
        return dataSource.updateStatus(projectId, deliveryReceiptId, status, updatedBy)
    }

    override suspend fun appendAudit(auditEvent: VendorDeliveryReceiptAuditEvent): DomainResult<VendorDeliveryReceiptAuditEvent> {
        return dataSource.appendAudit(auditEvent)
    }

    override suspend fun listAudits(projectId: String, deliveryReceiptId: String): DomainResult<List<VendorDeliveryReceiptAuditEvent>> {
        return dataSource.listAudits(projectId, deliveryReceiptId)
    }
}
