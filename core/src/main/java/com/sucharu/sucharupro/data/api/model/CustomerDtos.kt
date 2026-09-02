package com.sucharu.sucharupro.data.api.model

import java.math.BigDecimal

/**
 * Customer profile presentation DTO (INFRA-02 Step 04).
 */
data class CustomerProfileDto(
    val customerId: String,
    val customerCode: String,
    val name: String,
    val companyName: String?,
    val email: String?,
    val phone: String?,
    val creditLimit: BigDecimal,
    val currentBalance: BigDecimal,
    val status: String
)

/**
 * Customer order summary DTO.
 */
data class CustomerOrderSummaryDto(
    val orderId: String,
    val orderNumber: String,
    val status: String,
    val totalAmount: BigDecimal,
    val createdAt: Long
)

/**
 * Order item detail DTO.
 */
data class OrderItemDto(
    val itemId: String,
    val description: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal
)

/**
 * Full order detail DTO.
 */
data class CustomerOrderDetailDto(
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val status: String,
    val items: List<OrderItemDto>,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val totalAmount: BigDecimal,
    val notes: String?,
    val version: Long,
    val createdAt: Long
)

/**
 * Customer delivery status DTO.
 */
data class CustomerDeliveryStatusDto(
    val challanId: String,
    val challanNumber: String,
    val orderId: String,
    val deliveryStatus: String,
    val recipientName: String?,
    val dispatchedAt: Long?,
    val deliveredAt: Long?
)

/**
 * Order creation request DTO.
 */
data class OrderItemRequestDto(
    val description: String,
    val quantity: Int,
    val unitPrice: BigDecimal
)

data class CreateOrderRequestDto(
    val items: List<OrderItemRequestDto>,
    val notes: String? = null,
    val idempotencyKey: String? = null
)
