package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceipt
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptAuditEvent
import com.sucharu.sucharupro.domain.model.vendor.VendorDeliveryReceiptStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class FakeVendorDeliveryReceiptDataSource : VendorDeliveryReceiptDataSource {

    private val receipts = ConcurrentHashMap<String, VendorDeliveryReceipt>()
    private val audits = ConcurrentHashMap<String, MutableList<VendorDeliveryReceiptAuditEvent>>()
    private val flows = ConcurrentHashMap<String, MutableStateFlow<List<VendorDeliveryReceipt>>>()

    private fun key(projectId: String, deliveryReceiptId: String): String = "$projectId:$deliveryReceiptId"

    private fun updateFlow(projectId: String) {
        val list = receipts.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        flows.getOrPut(projectId) { MutableStateFlow(emptyList()) }.value = list
    }

    override fun observeDeliveryReceipts(projectId: String, vendorId: String?, purchaseOrderId: String?): Flow<List<VendorDeliveryReceipt>> {
        val key = "$projectId:$vendorId:$purchaseOrderId"
        val initial = receipts.values
            .filter {
                it.projectId == projectId &&
                (vendorId == null || it.vendorId == vendorId) &&
                (purchaseOrderId == null || it.purchaseOrderId == purchaseOrderId)
            }
            .sortedByDescending { it.createdAt }
        return flows.getOrPut(key) { MutableStateFlow(initial) }.asStateFlow()
    }

    override suspend fun findById(projectId: String, deliveryReceiptId: String): DomainResult<VendorDeliveryReceipt> {
        val r = receipts[key(projectId, deliveryReceiptId)]
        return if (r != null && r.projectId == projectId) {
            DomainResult.Success(r)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor delivery receipt '$deliveryReceiptId' not found in project '$projectId'."))
        }
    }

    override suspend fun findByReceiptNumber(projectId: String, receiptNumber: String): DomainResult<VendorDeliveryReceipt> {
        val r = receipts.values.find { it.projectId == projectId && it.receiptNumber == receiptNumber }
        return if (r != null) {
            DomainResult.Success(r)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor delivery receipt '$receiptNumber' not found in project '$projectId'."))
        }
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        purchaseOrderId: String?,
        status: VendorDeliveryReceiptStatus?
    ): DomainResult<List<VendorDeliveryReceipt>> {
        val list = receipts.values
            .filter { r ->
                r.projectId == projectId &&
                (vendorId == null || r.vendorId == vendorId) &&
                (purchaseOrderId == null || r.purchaseOrderId == purchaseOrderId) &&
                (status == null || r.status == status)
            }
            .sortedByDescending { it.createdAt }
        return DomainResult.Success(list)
    }

    override suspend fun createReceipt(receipt: VendorDeliveryReceipt): DomainResult<VendorDeliveryReceipt> {
        val k = key(receipt.projectId, receipt.deliveryReceiptId)
        if (receipts.containsKey(k)) {
            return DomainResult.Error(IllegalStateException("Delivery receipt '${receipt.deliveryReceiptId}' already exists."))
        }
        if (receipts.values.any { it.projectId == receipt.projectId && it.receiptNumber == receipt.receiptNumber }) {
            return DomainResult.Error(IllegalStateException("Delivery receipt with number '${receipt.receiptNumber}' already exists."))
        }
        val saved = receipt.copy(version = 1L)
        receipts[k] = saved
        updateFlow(saved.projectId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateReceipt(receipt: VendorDeliveryReceipt): DomainResult<VendorDeliveryReceipt> {
        val k = key(receipt.projectId, receipt.deliveryReceiptId)
        val existing = receipts[k] ?: return DomainResult.Error(NoSuchElementException("Delivery receipt not found."))
        if (existing.version != receipt.version) {
            return DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on delivery receipt '${receipt.deliveryReceiptId}'."))
        }
        val saved = receipt.copy(version = existing.version + 1L, updatedAt = System.currentTimeMillis())
        receipts[k] = saved
        updateFlow(saved.projectId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateStatus(
        projectId: String,
        deliveryReceiptId: String,
        status: VendorDeliveryReceiptStatus,
        updatedBy: String
    ): DomainResult<VendorDeliveryReceipt> {
        val k = key(projectId, deliveryReceiptId)
        val existing = receipts[k] ?: return DomainResult.Error(NoSuchElementException("Delivery receipt not found."))
        val updated = existing.copy(
            status = status,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1L
        )
        receipts[k] = updated
        updateFlow(updated.projectId)
        return DomainResult.Success(updated)
    }

    override suspend fun appendAudit(auditEvent: VendorDeliveryReceiptAuditEvent): DomainResult<VendorDeliveryReceiptAuditEvent> {
        val k = key(auditEvent.projectId, auditEvent.deliveryReceiptId)
        audits.getOrPut(k) { mutableListOf() }.add(auditEvent)
        return DomainResult.Success(auditEvent)
    }

    override suspend fun listAudits(projectId: String, deliveryReceiptId: String): DomainResult<List<VendorDeliveryReceiptAuditEvent>> {
        val k = key(projectId, deliveryReceiptId)
        val list = audits[k]?.toList() ?: emptyList()
        return DomainResult.Success(list.sortedBy { it.occurredAt })
    }
}
