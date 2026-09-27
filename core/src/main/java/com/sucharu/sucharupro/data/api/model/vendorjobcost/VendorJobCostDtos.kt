package com.sucharu.sucharupro.data.api.model.vendorjobcost

import kotlinx.serialization.Serializable

@Serializable
data class JobVendorCostEntryDto(
    val costEntryId: String,
    val jobId: String,
    val orderId: String? = null,
    val workContext: String = "PAPER_PURCHASE",
    val vendorAttributionType: String = "VENDOR",
    val vendorId: String? = null,
    val vendorCode: String? = null,
    val vendorName: String? = null,
    val amount: String,
    val currency: String = "BDT",
    val invoiceBillRef: String? = null,
    val payableId: String? = null,
    val paymentStatus: String = "UNPAID",
    val notes: String? = null,
    val createdAt: String = "",
    val createdBy: String = ""
)

@Serializable
data class JobVendorCostLedgerSummaryDto(
    val jobId: String,
    val orderId: String? = null,
    val totalVendorCost: String,
    val totalNonVendorCost: String,
    val grandTotalJobCost: String,
    val costEntries: List<JobVendorCostEntryDto> = emptyList(),
    val generatedAt: String
)
