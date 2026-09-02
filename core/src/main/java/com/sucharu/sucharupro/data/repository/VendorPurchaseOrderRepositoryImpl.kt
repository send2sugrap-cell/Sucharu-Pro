package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPurchaseOrderDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderAuditEvent
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderRevision
import com.sucharu.sucharupro.domain.model.vendor.VendorPurchaseOrderStatus
import com.sucharu.sucharupro.domain.repository.VendorPurchaseOrderRepository
import kotlinx.coroutines.flow.Flow

class VendorPurchaseOrderRepositoryImpl(
    private val dataSource: VendorPurchaseOrderDataSource
) : VendorPurchaseOrderRepository {

    override fun observePurchaseOrders(projectId: String, vendorId: String?): Flow<List<VendorPurchaseOrder>> {
        return dataSource.observePurchaseOrders(projectId, vendorId)
    }

    override suspend fun findById(projectId: String, purchaseOrderId: String): DomainResult<VendorPurchaseOrder> {
        return dataSource.findById(projectId, purchaseOrderId)
    }

    override suspend fun findByOrderNumber(projectId: String, orderNumber: String): DomainResult<VendorPurchaseOrder> {
        return dataSource.findByOrderNumber(projectId, orderNumber)
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        status: VendorPurchaseOrderStatus?,
        sourceReferenceType: String?,
        sourceReferenceId: String?
    ): DomainResult<List<VendorPurchaseOrder>> {
        return dataSource.list(projectId, vendorId, status, sourceReferenceType, sourceReferenceId)
    }

    override suspend fun createOrder(order: VendorPurchaseOrder): DomainResult<VendorPurchaseOrder> {
        return dataSource.createOrder(order)
    }

    override suspend fun updateOrder(order: VendorPurchaseOrder): DomainResult<VendorPurchaseOrder> {
        return dataSource.updateOrder(order)
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
        return dataSource.updateStatus(projectId, purchaseOrderId, status, updatedBy, approvedBy, approvedAt, issuedBy, issuedAt)
    }

    override suspend fun recordRevision(revision: VendorPurchaseOrderRevision): DomainResult<VendorPurchaseOrderRevision> {
        return dataSource.recordRevision(revision)
    }

    override suspend fun listRevisions(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderRevision>> {
        return dataSource.listRevisions(projectId, purchaseOrderId)
    }

    override suspend fun appendAudit(auditEvent: VendorPurchaseOrderAuditEvent): DomainResult<VendorPurchaseOrderAuditEvent> {
        return dataSource.appendAudit(auditEvent)
    }

    override suspend fun listAudits(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderAuditEvent>> {
        return dataSource.listAudits(projectId, purchaseOrderId)
    }
}
