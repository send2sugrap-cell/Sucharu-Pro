package com.sucharu.sucharupro.ui.features.customerportal

import com.sucharu.sucharupro.data.api.model.*
import java.math.BigDecimal

sealed interface CustomerPortalUiState<out T> {
    data object Loading : CustomerPortalUiState<Nothing>
    data class Success<out T>(val data: T) : CustomerPortalUiState<T>
    data class Error(val message: String, val canRetry: Boolean = true) : CustomerPortalUiState<Nothing>
    data object Empty : CustomerPortalUiState<Nothing>
}

data class CustomerDashboardSummary(
    val profile: CustomerProfileDto,
    val activeOrderCount: Int,
    val inProductionCount: Int,
    val readyForDeliveryCount: Int,
    val pendingPaymentCount: Int,
    val totalOutstandingBalance: BigDecimal,
    val recentOrders: List<CustomerOrderSummaryDto>
)

data class CustomerProductionStageProgress(
    val stageNumber: Int,
    val stageName: String,
    val stageLabel: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
    val completedAt: String? = null
)

data class CustomerProductionStatus(
    val orderId: String,
    val orderNumber: String,
    val jobId: String,
    val currentStage: String,
    val currentStageLabel: String,
    val stages: List<CustomerProductionStageProgress>,
    val updatedAt: Long
)

data class CustomerReturnItem(
    val returnId: String,
    val orderId: String,
    val orderNumber: String,
    val reason: String,
    val status: String,
    val requestedAt: Long,
    val resolvedAt: Long? = null
)

data class CustomerSupportTicket(
    val ticketId: String,
    val subject: String,
    val category: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class CustomerNotificationItem(
    val notificationId: String,
    val title: String,
    val message: String,
    val category: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
