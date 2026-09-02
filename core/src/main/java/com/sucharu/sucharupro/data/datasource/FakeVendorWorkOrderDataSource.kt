package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityType
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrder
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrderAuditEvent
import com.sucharu.sucharupro.domain.model.vendor.VendorWorkOrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class FakeVendorWorkOrderDataSource : VendorWorkOrderDataSource {

    private val workOrders = ConcurrentHashMap<String, VendorWorkOrder>()
    private val audits = ConcurrentHashMap<String, MutableList<VendorWorkOrderAuditEvent>>()
    private val flows = ConcurrentHashMap<String, MutableStateFlow<List<VendorWorkOrder>>>()

    private fun key(projectId: String, workOrderId: String): String = "$projectId:$workOrderId"

    private fun updateFlow(projectId: String) {
        val list = workOrders.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        flows.getOrPut(projectId) { MutableStateFlow(emptyList()) }.value = list
    }

    override fun observeWorkOrders(projectId: String, vendorId: String?): Flow<List<VendorWorkOrder>> {
        val key = if (vendorId != null) "$projectId:$vendorId" else projectId
        val initial = workOrders.values
            .filter { it.projectId == projectId && (vendorId == null || it.vendorId == vendorId) }
            .sortedByDescending { it.createdAt }
        return flows.getOrPut(key) { MutableStateFlow(initial) }.asStateFlow()
    }

    override suspend fun findById(projectId: String, workOrderId: String): DomainResult<VendorWorkOrder> {
        val order = workOrders[key(projectId, workOrderId)]
        return if (order != null && order.projectId == projectId) {
            DomainResult.Success(order)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor work order '$workOrderId' not found in project '$projectId'."))
        }
    }

    override suspend fun findByNumber(projectId: String, workOrderNumber: String): DomainResult<VendorWorkOrder> {
        val order = workOrders.values.find { it.projectId == projectId && it.workOrderNumber == workOrderNumber }
        return if (order != null) {
            DomainResult.Success(order)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor work order '$workOrderNumber' not found in project '$projectId'."))
        }
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        status: VendorWorkOrderStatus?,
        capabilityType: CapabilityType?,
        sourceReferenceType: String?,
        sourceReferenceId: String?
    ): DomainResult<List<VendorWorkOrder>> {
        val list = workOrders.values
            .filter { o ->
                o.projectId == projectId &&
                (vendorId == null || o.vendorId == vendorId) &&
                (status == null || o.status == status) &&
                (capabilityType == null || o.capabilityType == capabilityType) &&
                (sourceReferenceType == null || o.sourceReferenceType == sourceReferenceType) &&
                (sourceReferenceId == null || o.sourceReferenceId == sourceReferenceId)
            }
            .sortedByDescending { it.createdAt }
        return DomainResult.Success(list)
    }

    override suspend fun createWorkOrder(workOrder: VendorWorkOrder): DomainResult<VendorWorkOrder> {
        val k = key(workOrder.projectId, workOrder.workOrderId)
        if (workOrders.containsKey(k)) {
            return DomainResult.Error(IllegalStateException("Work order '${workOrder.workOrderId}' already exists."))
        }
        if (workOrders.values.any { it.projectId == workOrder.projectId && it.workOrderNumber == workOrder.workOrderNumber }) {
            return DomainResult.Error(IllegalStateException("Work order with number '${workOrder.workOrderNumber}' already exists."))
        }
        val saved = workOrder.copy(version = 1L)
        workOrders[k] = saved
        updateFlow(saved.projectId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateWorkOrder(workOrder: VendorWorkOrder): DomainResult<VendorWorkOrder> {
        val k = key(workOrder.projectId, workOrder.workOrderId)
        val existing = workOrders[k] ?: return DomainResult.Error(NoSuchElementException("Work order not found."))
        if (existing.version != workOrder.version) {
            return DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on work order '${workOrder.workOrderId}'."))
        }
        val saved = workOrder.copy(version = existing.version + 1L, updatedAt = System.currentTimeMillis())
        workOrders[k] = saved
        updateFlow(saved.projectId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateStatus(
        projectId: String,
        workOrderId: String,
        status: VendorWorkOrderStatus,
        updatedBy: String
    ): DomainResult<VendorWorkOrder> {
        val k = key(projectId, workOrderId)
        val existing = workOrders[k] ?: return DomainResult.Error(NoSuchElementException("Work order not found."))
        val updated = existing.copy(
            status = status,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1L
        )
        workOrders[k] = updated
        updateFlow(updated.projectId)
        return DomainResult.Success(updated)
    }

    override suspend fun appendAudit(auditEvent: VendorWorkOrderAuditEvent): DomainResult<VendorWorkOrderAuditEvent> {
        val k = key(auditEvent.projectId, auditEvent.workOrderId)
        audits.getOrPut(k) { mutableListOf() }.add(auditEvent)
        return DomainResult.Success(auditEvent)
    }

    override suspend fun listAudits(projectId: String, workOrderId: String): DomainResult<List<VendorWorkOrderAuditEvent>> {
        val k = key(projectId, workOrderId)
        val list = audits[k]?.toList() ?: emptyList()
        return DomainResult.Success(list.sortedBy { it.occurredAt })
    }
}
