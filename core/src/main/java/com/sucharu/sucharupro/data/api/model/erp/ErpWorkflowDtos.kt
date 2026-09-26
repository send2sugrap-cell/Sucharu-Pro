package com.sucharu.sucharupro.data.api.model.erp

import kotlinx.serialization.Serializable

@Serializable
data class OrchestrateOrderActionRequestDto(
    val customerAction: String = "ORDER_NOW",
    val customerId: String,
    val productId: String,
    val offerId: String? = null,
    val orderQuantity: Int = 1000,
    val deliveryZone: String = "INSIDE_DHAKA",
    val specialInstructions: String? = null
)

@Serializable
data class OrchestratedOrderResultDto(
    val orchestrationId: String,
    val workflowStatus: String,
    val customerId: String,
    val productId: String,
    val quotationId: String? = null,
    val orderId: String? = null,
    val priceSnapshotId: String? = null,
    val grandTotalAmount: Double,
    val currency: String = "BDT",
    val message: String
)

@Serializable
data class ProductionHandoffRequestDto(
    val orderId: String,
    val plannedQuantity: Int = 1000
)

@Serializable
data class GenerateChallanRequestDto(
    val orderId: String,
    val receiverName: String? = null
)

@Serializable
data class IssueInvoiceRequestDto(
    val orderId: String
)
