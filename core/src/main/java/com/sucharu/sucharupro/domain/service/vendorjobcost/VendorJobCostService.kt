package com.sucharu.sucharupro.domain.service.vendorjobcost

import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType
import com.sucharu.sucharupro.domain.model.vendorjobcost.*
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

/**
 * Domain Service for Vendor-Based Job Cost & Traceability.
 *
 * Enforces subject/capability-based vendor filtering, multi-vendor job cost entries,
 * NO_VENDOR attribution, and multi-layer domain validation.
 */
class VendorJobCostService {

    private val vendorsStore = ConcurrentHashMap<String, Vendor>()
    private val entriesStore = ConcurrentHashMap<String, JobVendorCostEntry>()

    init {
        // Default Active Master Vendors with specific VendorCategories
        val v1 = Vendor(
            vendorId = "VND-2026-PAPER",
            projectId = "TENANT-001",
            vendorCode = "VND-001",
            vendorName = "Bengal Paper & Substrate Suppliers",
            vendorType = VendorType.MATERIAL_SUPPLIER,
            vendorCategory = VendorCategory.PAPER_SUPPLIER,
            status = VendorStatus.ACTIVE,
            primaryPhone = "01812000000"
        )
        val v2 = Vendor(
            vendorId = "VND-2026-CTP",
            projectId = "TENANT-001",
            vendorCode = "VND-002",
            vendorName = "Creative CTP & Plate House",
            vendorType = VendorType.SERVICE_PROVIDER,
            vendorCategory = VendorCategory.CTP_PREPRESS,
            status = VendorStatus.ACTIVE,
            primaryPhone = "01712000000"
        )
        val v3 = Vendor(
            vendorId = "VND-2026-BINDING",
            projectId = "TENANT-001",
            vendorCode = "VND-003",
            vendorName = "Karim Binding & Finishing Works",
            vendorType = VendorType.PRODUCTION_VENDOR,
            vendorCategory = VendorCategory.FINISHING,
            status = VendorStatus.ACTIVE,
            primaryPhone = "01912000000"
        )
        val v4 = Vendor(
            vendorId = "VND-2026-INACTIVE",
            projectId = "TENANT-001",
            vendorCode = "VND-004",
            vendorName = "Inactive Transport Service",
            vendorType = VendorType.SERVICE_PROVIDER,
            vendorCategory = VendorCategory.LOGISTICS_TRANSPORT,
            status = VendorStatus.SUSPENDED,
            primaryPhone = "01612000000"
        )

        vendorsStore[v1.vendorId] = v1
        vendorsStore[v2.vendorId] = v2
        vendorsStore[v3.vendorId] = v3
        vendorsStore[v4.vendorId] = v4
    }

    /**
     * Slices active vendors and filters ONLY vendors matching the required category for [workContext].
     */
    fun getEligibleVendorsForCostContext(workContext: WorkCostContext): List<Vendor> {
        val requiredCategory = workContext.requiredVendorCategory ?: return vendorsStore.values.filter { it.status == VendorStatus.ACTIVE }
        return vendorsStore.values.filter { it.status == VendorStatus.ACTIVE && it.vendorCategory == requiredCategory }
    }

    /**
     * Adds a Job Vendor Cost Entry with strict domain/capability validation.
     */
    fun addJobVendorCostEntry(entry: JobVendorCostEntry): JobVendorCostEntry {
        if (entry.vendorAttributionType == VendorCostAttributionType.VENDOR) {
            val vendorId = entry.vendorId ?: throw IllegalArgumentException("Vendor ID is required.")
            val vendor = vendorsStore[vendorId] ?: throw IllegalArgumentException("Vendor '$vendorId' not found.")

            require(vendor.status == VendorStatus.ACTIVE) { "Vendor '${vendor.vendorName}' is suspended or inactive." }

            val requiredCategory = entry.workContext.requiredVendorCategory
            if (requiredCategory != null && vendor.vendorCategory != requiredCategory && vendor.vendorCategory != VendorCategory.OTHER) {
                throw IllegalArgumentException("Vendor '${vendor.vendorName}' (${vendor.vendorCategory}) is not eligible for work context '${entry.workContext.name}' (requires $requiredCategory).")
            }
        }

        entriesStore[entry.costEntryId] = entry
        return entry
    }

    /**
     * Calculates the Multi-Vendor Job Cost Accounting Summary for [jobId].
     */
    fun getJobVendorCostSummary(jobId: String): JobVendorCostLedgerSummary {
        val timestamp = "2026-09-27T13:30:00Z"
        val entries = entriesStore.values.filter { it.jobId == jobId }

        val vendorTotal = entries
            .filter { it.vendorAttributionType == VendorCostAttributionType.VENDOR }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }

        val nonVendorTotal = entries
            .filter { it.vendorAttributionType == VendorCostAttributionType.NO_VENDOR }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }

        return JobVendorCostLedgerSummary(
            jobId = jobId,
            orderId = entries.firstOrNull()?.orderId ?: "ORD-1001",
            totalVendorCost = vendorTotal,
            totalNonVendorCost = nonVendorTotal,
            grandTotalJobCost = vendorTotal + nonVendorTotal,
            costEntries = entries,
            generatedAt = timestamp
        )
    }
}
