package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceipt
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptAuditEvent
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptStatus
import kotlinx.coroutines.flow.Flow

interface VendorDeliveryReceiptDataSource {
    fun observeDeliveryReceipts(projectId: String, vendorId: String? = null, purchaseOrderId: String? = null): Flow<List<VendorDeliveryReceipt>>
    suspend fun findById(projectId: String, deliveryReceiptId: String): DomainResult<VendorDeliveryReceipt>
    suspend fun findByReceiptNumber(projectId: String, receiptNumber: String): DomainResult<VendorDeliveryReceipt>
    suspend fun list(
        projectId: String,
        vendorId: String? = null,
        purchaseOrderId: String? = null,
        status: VendorDeliveryReceiptStatus? = null
    ): DomainResult<List<VendorDeliveryReceipt>>
    suspend fun createReceipt(receipt: VendorDeliveryReceipt): DomainResult<VendorDeliveryReceipt>
    suspend fun updateReceipt(receipt: VendorDeliveryReceipt): DomainResult<VendorDeliveryReceipt>
    suspend fun updateStatus(
        projectId: String,
        deliveryReceiptId: String,
        status: VendorDeliveryReceiptStatus,
        updatedBy: String
    ): DomainResult<VendorDeliveryReceipt>
    suspend fun appendAudit(auditEvent: VendorDeliveryReceiptAuditEvent): DomainResult<VendorDeliveryReceiptAuditEvent>
    suspend fun listAudits(projectId: String, deliveryReceiptId: String): DomainResult<List<VendorDeliveryReceiptAuditEvent>>
}
