package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderAuditEvent
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderRevision
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class FakeVendorPurchaseOrderDataSource : VendorPurchaseOrderDataSource {

    private val orders = ConcurrentHashMap<String, VendorPurchaseOrder>()
    private val revisions = ConcurrentHashMap<String, MutableList<VendorPurchaseOrderRevision>>()
    private val audits = ConcurrentHashMap<String, MutableList<VendorPurchaseOrderAuditEvent>>()
    private val flows = ConcurrentHashMap<String, MutableStateFlow<List<VendorPurchaseOrder>>>()

    private fun key(projectId: String, purchaseOrderId: String): String = "$projectId:$purchaseOrderId"

    private fun updateFlow(projectId: String) {
        val list = orders.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        flows.getOrPut(projectId) { MutableStateFlow(emptyList()) }.value = list
    }

    override fun observePurchaseOrders(projectId: String, vendorId: String?): Flow<List<VendorPurchaseOrder>> {
        val key = if (vendorId != null) "$projectId:$vendorId" else projectId
        val initial = orders.values
            .filter { it.projectId == projectId && (vendorId == null || it.vendorId == vendorId) }
            .sortedByDescending { it.createdAt }
        return flows.getOrPut(key) { MutableStateFlow(initial) }.asStateFlow()
    }

    override suspend fun findById(projectId: String, purchaseOrderId: String): DomainResult<VendorPurchaseOrder> {
        val order = orders[key(projectId, purchaseOrderId)]
        return if (order != null && order.projectId == projectId) {
            DomainResult.Success(order)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor purchase order '$purchaseOrderId' not found in project '$projectId'."))
        }
    }

    override suspend fun findByOrderNumber(projectId: String, orderNumber: String): DomainResult<VendorPurchaseOrder> {
        val order = orders.values.find { it.projectId == projectId && it.orderNumber == orderNumber }
        return if (order != null) {
            DomainResult.Success(order)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor purchase order '$orderNumber' not found in project '$projectId'."))
        }
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        status: VendorPurchaseOrderStatus?,
        sourceReferenceType: String?,
        sourceReferenceId: String?
    ): DomainResult<List<VendorPurchaseOrder>> {
        val list = orders.values
            .filter { o ->
                o.projectId == projectId &&
                (vendorId == null || o.vendorId == vendorId) &&
                (status == null || o.status == status) &&
                (sourceReferenceType == null || o.sourceReferenceType == sourceReferenceType) &&
                (sourceReferenceId == null || o.sourceReferenceId == sourceReferenceId)
            }
            .sortedByDescending { it.createdAt }
        return DomainResult.Success(list)
    }

    override suspend fun createOrder(order: VendorPurchaseOrder): DomainResult<VendorPurchaseOrder> {
        val k = key(order.projectId, order.purchaseOrderId)
        if (orders.containsKey(k)) {
            return DomainResult.Error(IllegalStateException("Purchase order '${order.purchaseOrderId}' already exists."))
        }
        if (orders.values.any { it.projectId == order.projectId && it.orderNumber == order.orderNumber }) {
            return DomainResult.Error(IllegalStateException("Purchase order with number '${order.orderNumber}' already exists."))
        }
        val saved = order.copy(version = 1L)
        orders[k] = saved
        updateFlow(saved.projectId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateOrder(order: VendorPurchaseOrder): DomainResult<VendorPurchaseOrder> {
        val k = key(order.projectId, order.purchaseOrderId)
        val existing = orders[k] ?: return DomainResult.Error(NoSuchElementException("Purchase order not found."))
        if (existing.version != order.version) {
            return DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on purchase order '${order.purchaseOrderId}'."))
        }
        val saved = order.copy(version = existing.version + 1L, updatedAt = System.currentTimeMillis())
        orders[k] = saved
        updateFlow(saved.projectId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateStatus(
        projectId: String,
        purchaseOrderId: String,
        status: VendorPurchaseOrderStatus,
        updatedBy: String,
        approvedBy: String?,
        approvedAt: Long?,
        issuedBy: String?,
        issuedAt: Long?
    ): DomainResult<VendorPurchaseOrder> {
        val k = key(projectId, purchaseOrderId)
        val existing = orders[k] ?: return DomainResult.Error(NoSuchElementException("Purchase order not found."))
        val updated = existing.copy(
            status = status,
            approvedBy = approvedBy ?: existing.approvedBy,
            approvedAt = approvedAt ?: existing.approvedAt,
            issuedBy = issuedBy ?: existing.issuedBy,
            issuedAt = issuedAt ?: existing.issuedAt,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1L
        )
        orders[k] = updated
        updateFlow(updated.projectId)
        return DomainResult.Success(updated)
    }

    override suspend fun recordRevision(revision: VendorPurchaseOrderRevision): DomainResult<VendorPurchaseOrderRevision> {
        val k = key(revision.projectId, revision.purchaseOrderId)
        revisions.getOrPut(k) { mutableListOf() }.add(revision)
        return DomainResult.Success(revision)
    }

    override suspend fun listRevisions(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderRevision>> {
        val k = key(projectId, purchaseOrderId)
        val list = revisions[k]?.toList() ?: emptyList()
        return DomainResult.Success(list.sortedBy { it.revisionNumber })
    }

    override suspend fun appendAudit(auditEvent: VendorPurchaseOrderAuditEvent): DomainResult<VendorPurchaseOrderAuditEvent> {
        val k = key(auditEvent.projectId, auditEvent.purchaseOrderId)
        audits.getOrPut(k) { mutableListOf() }.add(auditEvent)
        return DomainResult.Success(auditEvent)
    }

    override suspend fun listAudits(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderAuditEvent>> {
        val k = key(projectId, purchaseOrderId)
        val list = audits[k]?.toList() ?: emptyList()
        return DomainResult.Success(list.sortedBy { it.occurredAt })
    }
}
