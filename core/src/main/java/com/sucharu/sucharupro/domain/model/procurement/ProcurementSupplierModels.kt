package com.sucharu.sucharupro.domain.model.procurement

import java.math.BigDecimal

/**
 * BI-06 Supplier Category Classification.
 */
enum class ProcurementSupplierCategory {
    PAPER_SUBSTRATE_SUPPLIER,
    INK_CHEMICAL_SUPPLIER,
    CTP_PLATE_SUPPLIER,
    OUTSOURCED_FINISHING_SERVICE,
    PACKAGING_SUPPLIER,
    EQUIPMENT_SPARE_SUPPLIER
}

/**
 * BI-06 Supplier Obligation & Payable Status.
 */
enum class SupplierObligationStatus {
    NO_OBLIGATION,
    OBLIGATION_PENDING,
    PARTIALLY_PAID,
    FULLY_PAID
}

/**
 * Single Supplier Procurement & Obligation Summary Item.
 */
data class SupplierProcurementSummaryItem(
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val category: ProcurementSupplierCategory,
    val primaryContactPhone: String? = null,
    val totalPurchaseOrdersCount: Int = 0,
    val totalProcurementValue: BigDecimal = BigDecimal.ZERO,
    val totalPaidAmount: BigDecimal = BigDecimal.ZERO,
    val totalOutstandingPayable: BigDecimal = BigDecimal.ZERO,
    val obligationStatus: SupplierObligationStatus = SupplierObligationStatus.NO_OBLIGATION,
    val activeWorkOrdersCount: Int = 0,
    val isActive: Boolean = true
)

/**
 * BI-06 Master Procurement & Supplier Management Intelligence Summary.
 */
data class ProcurementSupplierManagementSummary(
    val totalSuppliersCount: Int,
    val activeSuppliersCount: Int,
    val totalActivePurchaseOrdersCount: Int,
    val totalProcurementCommitmentValue: BigDecimal,
    val totalSupplierPayablesOutstanding: BigDecimal,
    val totalSupplierPaymentsMadeYtd: BigDecimal,
    val isFinishedGoodsInventoryBoundaryPreserved: Boolean = true, // Critical Invariant: Always true!
    val supplierSummaries: List<SupplierProcurementSummaryItem> = emptyList(),
    val generatedAt: String
)
