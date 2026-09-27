package com.sucharu.sucharupro.domain.model.vendorjobcost

import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import java.math.BigDecimal

/**
 * Vendor Cost Attribution Type.
 */
enum class VendorCostAttributionType {
    VENDOR,
    NO_VENDOR
}

/**
 * Work or Cost Context for Printing Production Jobs.
 */
enum class WorkCostContext(val requiredVendorCategory: VendorCategory?) {
    PAPER_PURCHASE(VendorCategory.PAPER_SUPPLIER),
    CTP_PREPRESS(VendorCategory.CTP_PREPRESS),
    PRINTING(VendorCategory.PRINTING),
    FINISHING_LAMINATION(VendorCategory.FINISHING),
    FINISHING_FOLDING(VendorCategory.FINISHING),
    FINISHING_BINDING(VendorCategory.FINISHING),
    PACKAGING(VendorCategory.PACKAGING),
    LOGISTICS_TRANSPORT(VendorCategory.LOGISTICS_TRANSPORT),
    MAINTENANCE(VendorCategory.MAINTENANCE),
    OTHER(VendorCategory.OTHER)
}

/**
 * Job Vendor Cost Entry Entity.
 */
data class JobVendorCostEntry(
    val costEntryId: String,
    val jobId: String,
    val orderId: String? = null,
    val workContext: WorkCostContext,
    val vendorAttributionType: VendorCostAttributionType = VendorCostAttributionType.VENDOR,
    val vendorId: String? = null,
    val vendorCode: String? = null,
    val vendorName: String? = null,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val invoiceBillRef: String? = null,
    val payableId: String? = null,
    val paymentStatus: String = "UNPAID",
    val notes: String? = null,
    val createdAt: String,
    val createdBy: String,
    val updatedAt: String? = null,
    val updatedBy: String? = null
) {
    init {
        require(costEntryId.isNotBlank()) { "Cost Entry ID cannot be blank." }
        require(jobId.isNotBlank()) { "Job ID cannot be blank." }
        require(amount >= BigDecimal.ZERO) { "Amount cannot be negative." }

        if (vendorAttributionType == VendorCostAttributionType.VENDOR) {
            require(!vendorId.isNullOrBlank()) { "Vendor ID is required when Vendor Attribution Type is VENDOR." }
        } else {
            require(vendorId == null) { "Vendor ID must be null when Vendor Attribution Type is NO_VENDOR." }
        }
    }
}

/**
 * Multi-Vendor Job Cost Accounting Summary Model.
 */
data class JobVendorCostLedgerSummary(
    val jobId: String,
    val orderId: String? = null,
    val totalVendorCost: BigDecimal,
    val totalNonVendorCost: BigDecimal,
    val grandTotalJobCost: BigDecimal,
    val costEntries: List<JobVendorCostEntry> = emptyList(),
    val generatedAt: String
)
