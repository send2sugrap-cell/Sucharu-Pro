package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderAuditEvent
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderRevision
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderStatus
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for VendorPurchaseOrder persistence, items, revisions, and audits (Module 12 Step 05).
 */
interface VendorPurchaseOrderRepository {
    fun observePurchaseOrders(projectId: String, vendorId: String? = null): Flow<List<VendorPurchaseOrder>>
    suspend fun findById(projectId: String, purchaseOrderId: String): DomainResult<VendorPurchaseOrder>
    suspend fun findByOrderNumber(projectId: String, orderNumber: String): DomainResult<VendorPurchaseOrder>
    suspend fun list(
        projectId: String,
        vendorId: String? = null,
        status: VendorPurchaseOrderStatus? = null,
        sourceReferenceType: String? = null,
        sourceReferenceId: String? = null
    ): DomainResult<List<VendorPurchaseOrder>>
    suspend fun createOrder(order: VendorPurchaseOrder): DomainResult<VendorPurchaseOrder>
    suspend fun updateOrder(order: VendorPurchaseOrder): DomainResult<VendorPurchaseOrder>
    suspend fun updateStatus(
        projectId: String,
        purchaseOrderId: String,
        status: VendorPurchaseOrderStatus,
        updatedBy: String,
        approvedBy: String? = null,
        approvedAt: Long? = null,
        issuedBy: String? = null,
        issuedAt: Long? = null
    ): DomainResult<VendorPurchaseOrder>
    suspend fun recordRevision(revision: VendorPurchaseOrderRevision): DomainResult<VendorPurchaseOrderRevision>
    suspend fun listRevisions(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderRevision>>
    suspend fun appendAudit(auditEvent: VendorPurchaseOrderAuditEvent): DomainResult<VendorPurchaseOrderAuditEvent>
    suspend fun listAudits(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderAuditEvent>>
}
