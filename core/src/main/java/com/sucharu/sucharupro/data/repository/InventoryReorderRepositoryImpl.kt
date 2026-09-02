package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentType
import com.sucharu.sucharupro.domain.model.inventory.reorder.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryReorderRepository
import com.sucharu.sucharupro.domain.validation.InventoryReorderAuthorizationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.*

/**
 * Thread-safe production-grade repository implementation for Reorder Alert & Stock Level Management
 * (Module 07 Step 08).
 *
 * Implements automated stock level evaluation using the unified movement formula:
 * Stock = StockIn - StockOut - TransferOut + TransferIn + AdjustmentIn - AdjustmentOut.
 */
class InventoryReorderRepositoryImpl(
    private val reorderDataSource: InventoryReorderDataSource,
    private val receivingDataSource: InventoryReceivingDataSource,
    private val stockOutDataSource: InventoryStockOutDataSource,
    private val transferDataSource: InventoryStockTransferDataSource,
    private val adjustmentDataSource: InventoryStockAdjustmentDataSource,
    private val productDataSource: InventoryProductDataSource,
    private val warehouseDataSource: InventoryWarehouseDataSource,
    private val locationDataSource: InventoryLocationDataSource
) : InventoryReorderRepository {

    private val repositoryMutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Stock Level Policies (CRUD with RBAC)
    // ──────────────────────────────────────────────────────────────

    override fun observePolicies(projectId: String): Flow<List<InventoryStockLevelPolicy>> {
        return reorderDataSource.observePolicies().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun getPolicy(policyId: String, callerRole: UserRole?): DomainResult<InventoryStockLevelPolicy> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReorderAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val policy = reorderDataSource.observePolicies().first().find { it.policyId == policyId }
            ?: return DomainResult.Error(message = "Policy with ID '$policyId' not found.")
        DomainResult.Success(policy)
    }

    override suspend fun createPolicy(policy: InventoryStockLevelPolicy, callerRole: UserRole?): DomainResult<InventoryStockLevelPolicy> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReorderAuthorizationValidator.validateConfigurePolicyPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        reorderDataSource.insertPolicy(policy)
    }

    override suspend fun updatePolicy(policy: InventoryStockLevelPolicy, callerRole: UserRole?): DomainResult<InventoryStockLevelPolicy> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReorderAuthorizationValidator.validateConfigurePolicyPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        reorderDataSource.updatePolicy(policy)
    }

    override suspend fun deletePolicy(policyId: String, callerRole: UserRole?): DomainResult<Unit> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReorderAuthorizationValidator.validateConfigurePolicyPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        reorderDataSource.deletePolicy(policyId)
    }

    // ──────────────────────────────────────────────────────────────
    // Reorder Alerts
    // ──────────────────────────────────────────────────────────────

    override fun observeAlerts(projectId: String): Flow<List<InventoryReorderAlert>> {
        return reorderDataSource.observeAlerts().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun getAlert(alertId: String, callerRole: UserRole?): DomainResult<InventoryReorderAlert> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReorderAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val alert = reorderDataSource.observeAlerts().first().find { it.alertId == alertId }
            ?: return DomainResult.Error(message = "Alert with ID '$alertId' not found.")
        DomainResult.Success(alert)
    }

    override suspend fun acknowledgeAlert(alertId: String, userId: String, callerRole: UserRole?): DomainResult<InventoryReorderAlert> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReorderAuthorizationValidator.validateAcknowledgePermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val alert = reorderDataSource.observeAlerts().first().find { it.alertId == alertId }
            ?: return DomainResult.Error(message = "Alert with ID '$alertId' not found.")
        
        if (alert.status != InventoryReorderAlertStatus.OPEN) {
            return DomainResult.Error(message = "Only OPEN alerts can be acknowledged.")
        }

        val updated = alert.copy(
            status = InventoryReorderAlertStatus.ACKNOWLEDGED,
            acknowledgedAt = Instant.now().toString(),
            acknowledgedBy = userId
        )
        reorderDataSource.updateAlert(updated)
    }

    override suspend fun resolveAlert(alertId: String, userId: String, callerRole: UserRole?): DomainResult<InventoryReorderAlert> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReorderAuthorizationValidator.validateManageAlertsPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val alert = reorderDataSource.observeAlerts().first().find { it.alertId == alertId }
            ?: return DomainResult.Error(message = "Alert with ID '$alertId' not found.")

        val updated = alert.copy(
            status = InventoryReorderAlertStatus.RESOLVED,
            resolvedAt = Instant.now().toString(),
            resolvedBy = userId
        )
        reorderDataSource.updateAlert(updated)
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Level Evaluation Engine
    // ──────────────────────────────────────────────────────────────

    override suspend fun evaluatePolicies(projectId: String): DomainResult<Unit> = repositoryMutex.withLock {
        val policies = reorderDataSource.observePolicies().first().filter { it.projectId == projectId && it.enabled }
        val openAlerts = reorderDataSource.observeAlerts().first().filter { 
            it.projectId == projectId && it.status != InventoryReorderAlertStatus.RESOLVED 
        }

        for (policy in policies) {
            val currentStock = calculateStock(projectId, policy.productId, policy.locationId)
            val alertType = determineAlertType(currentStock, policy)

            if (alertType != null) {
                // Prevent duplicate open alerts for the same policy and alert type
                val alreadyAlerted = openAlerts.any { 
                    it.policyId == policy.policyId && it.alertType == alertType 
                }

                if (!alreadyAlerted) {
                    val alert = InventoryReorderAlert(
                        alertId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        productId = policy.productId,
                        locationId = policy.locationId ?: "GLOBAL",
                        policyId = policy.policyId,
                        alertType = alertType,
                        availableQuantity = currentStock,
                        thresholdQuantity = when(alertType) {
                            InventoryReorderAlertType.OUT_OF_STOCK -> 0.0
                            InventoryReorderAlertType.CRITICAL -> policy.criticalStockLevel
                            InventoryReorderAlertType.REORDER_REQUIRED -> policy.reorderPoint
                            InventoryReorderAlertType.LOW_STOCK -> policy.minimumStockLevel
                        },
                        detectedAt = Instant.now().toString()
                    )
                    reorderDataSource.insertAlert(alert)
                }
            } else {
                // Auto-resolve open alerts for this policy if stock is now healthy
                val alertsToResolve = openAlerts.filter { it.policyId == policy.policyId }
                for (alert in alertsToResolve) {
                    reorderDataSource.updateAlert(alert.copy(
                        status = InventoryReorderAlertStatus.RESOLVED,
                        resolvedAt = Instant.now().toString(),
                        resolvedBy = "SYSTEM_AUTO_RESOLVE"
                    ))
                }
            }
        }
        DomainResult.Success(Unit)
    }

    private fun determineAlertType(stock: Double, policy: InventoryStockLevelPolicy): InventoryReorderAlertType? {
        return when {
            stock <= 0.0 -> InventoryReorderAlertType.OUT_OF_STOCK
            stock <= policy.criticalStockLevel -> InventoryReorderAlertType.CRITICAL
            stock <= policy.reorderPoint -> InventoryReorderAlertType.REORDER_REQUIRED
            stock <= policy.minimumStockLevel -> InventoryReorderAlertType.LOW_STOCK
            else -> null
        }
    }

    private suspend fun calculateStock(projectId: String, productId: String, locationId: String?): Double {
        val stockIn = receivingDataSource.observeStockInRecords().first()
            .filter { it.projectId == projectId && it.inventoryProductId == productId && (locationId == null || it.locationId == locationId) }
            .sumOf { it.quantity.toDouble() }

        val stockOut = stockOutDataSource.observeStockOutRecords().first()
            .filter { it.projectId == projectId && it.inventoryProductId == productId && (locationId == null || it.locationId == locationId) }
            .sumOf { it.quantity.toDouble() }

        val transfers = transferDataSource.observeStockTransferRecords().first()
            .filter { it.projectId == projectId && it.inventoryProductId == productId }
        
        val transferIn = transfers.filter { locationId == null || it.toLocationId == locationId }.sumOf { it.quantity.toDouble() }
        val transferOut = transfers.filter { locationId == null || it.fromLocationId == locationId }.sumOf { it.quantity.toDouble() }

        val adjustments = adjustmentDataSource.observeStockAdjustmentRecords().first()
            .filter { it.projectId == projectId && it.inventoryProductId == productId && (locationId == null || it.locationId == locationId) }
        
        val adjustmentIn = adjustments.filter { it.adjustmentType == InventoryAdjustmentType.INCREASE }.sumOf { it.quantity.toDouble() }
        val adjustmentOut = adjustments.filter { it.adjustmentType == InventoryAdjustmentType.DECREASE }.sumOf { it.quantity.toDouble() }

        return stockIn - stockOut - transferOut + transferIn + adjustmentIn - adjustmentOut
    }
}
