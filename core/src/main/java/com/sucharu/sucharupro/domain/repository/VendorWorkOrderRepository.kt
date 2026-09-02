package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityType
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrderAuditEvent
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrderStatus
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for VendorWorkOrder persistence, state changes, and audit trails (Module 12 Step 04).
 */
interface VendorWorkOrderRepository {
    fun observeWorkOrders(projectId: String, vendorId: String? = null): Flow<List<VendorWorkOrder>>
    suspend fun findById(projectId: String, workOrderId: String): DomainResult<VendorWorkOrder>
    suspend fun findByNumber(projectId: String, workOrderNumber: String): DomainResult<VendorWorkOrder>
    suspend fun list(
        projectId: String,
        vendorId: String? = null,
        status: VendorWorkOrderStatus? = null,
        capabilityType: CapabilityType? = null,
        sourceReferenceType: String? = null,
        sourceReferenceId: String? = null
    ): DomainResult<List<VendorWorkOrder>>
    suspend fun createWorkOrder(workOrder: VendorWorkOrder): DomainResult<VendorWorkOrder>
    suspend fun updateWorkOrder(workOrder: VendorWorkOrder): DomainResult<VendorWorkOrder>
    suspend fun updateStatus(
        projectId: String,
        workOrderId: String,
        status: VendorWorkOrderStatus,
        updatedBy: String
    ): DomainResult<VendorWorkOrder>
    suspend fun appendAudit(auditEvent: VendorWorkOrderAuditEvent): DomainResult<VendorWorkOrderAuditEvent>
    suspend fun listAudits(projectId: String, workOrderId: String): DomainResult<List<VendorWorkOrderAuditEvent>>
}
