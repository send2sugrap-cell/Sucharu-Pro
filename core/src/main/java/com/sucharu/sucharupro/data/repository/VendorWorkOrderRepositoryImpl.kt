package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorWorkOrderDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityType
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrderAuditEvent
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrderStatus
import com.sucharu.sucharupro.domain.repository.VendorWorkOrderRepository
import kotlinx.coroutines.flow.Flow

class VendorWorkOrderRepositoryImpl(
    private val dataSource: VendorWorkOrderDataSource
) : VendorWorkOrderRepository {

    override fun observeWorkOrders(projectId: String, vendorId: String?): Flow<List<VendorWorkOrder>> {
        return dataSource.observeWorkOrders(projectId, vendorId)
    }

    override suspend fun findById(projectId: String, workOrderId: String): DomainResult<VendorWorkOrder> {
        return dataSource.findById(projectId, workOrderId)
    }

    override suspend fun findByNumber(projectId: String, workOrderNumber: String): DomainResult<VendorWorkOrder> {
        return dataSource.findByNumber(projectId, workOrderNumber)
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        status: VendorWorkOrderStatus?,
        capabilityType: CapabilityType?,
        sourceReferenceType: String?,
        sourceReferenceId: String?
    ): DomainResult<List<VendorWorkOrder>> {
        return dataSource.list(projectId, vendorId, status, capabilityType, sourceReferenceType, sourceReferenceId)
    }

    override suspend fun createWorkOrder(workOrder: VendorWorkOrder): DomainResult<VendorWorkOrder> {
        return dataSource.createWorkOrder(workOrder)
    }

    override suspend fun updateWorkOrder(workOrder: VendorWorkOrder): DomainResult<VendorWorkOrder> {
        return dataSource.updateWorkOrder(workOrder)
    }

    override suspend fun updateStatus(
        projectId: String,
        workOrderId: String,
        status: VendorWorkOrderStatus,
        updatedBy: String
    ): DomainResult<VendorWorkOrder> {
        return dataSource.updateStatus(projectId, workOrderId, status, updatedBy)
    }

    override suspend fun appendAudit(auditEvent: VendorWorkOrderAuditEvent): DomainResult<VendorWorkOrderAuditEvent> {
        return dataSource.appendAudit(auditEvent)
    }

    override suspend fun listAudits(projectId: String, workOrderId: String): DomainResult<List<VendorWorkOrderAuditEvent>> {
        return dataSource.listAudits(projectId, workOrderId)
    }
}
