package com.sucharu.sucharupro.data.api.model.procurement

import kotlinx.serialization.Serializable

@Serializable
data class SupplierProcurementSummaryItemDto(
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val category: String,
    val primaryContactPhone: String? = null,
    val totalPurchaseOrdersCount: Int = 0,
    val totalProcurementValue: String,
    val totalPaidAmount: String,
    val totalOutstandingPayable: String,
    val obligationStatus: String = "NO_OBLIGATION",
    val activeWorkOrdersCount: Int = 0,
    val isActive: Boolean = true
)

@Serializable
data class ProcurementSupplierManagementSummaryDto(
    val totalSuppliersCount: Int,
    val activeSuppliersCount: Int,
    val totalActivePurchaseOrdersCount: Int,
    val totalProcurementCommitmentValue: String,
    val totalSupplierPayablesOutstanding: String,
    val totalSupplierPaymentsMadeYtd: String,
    val isFinishedGoodsInventoryBoundaryPreserved: Boolean = true,
    val supplierSummaries: List<SupplierProcurementSummaryItemDto> = emptyList(),
    val generatedAt: String
)
