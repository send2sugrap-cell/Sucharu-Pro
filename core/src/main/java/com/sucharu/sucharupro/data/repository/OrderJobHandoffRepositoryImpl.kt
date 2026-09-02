package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.OrderJobHandoffDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.repository.OrderJobHandoffRepository
import com.sucharu.sucharupro.domain.validation.OrderJobHandoffValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Production-ready implementation of [OrderJobHandoffRepository].
 * Enforces validation via [OrderJobHandoffValidator] before persisting mutations to [OrderJobHandoffDataSource].
 */
class OrderJobHandoffRepositoryImpl(
    private val dataSource: OrderJobHandoffDataSource
) : OrderJobHandoffRepository {

    override fun getHandoffs(): Flow<List<OrderJobHandoff>> = dataSource.observeHandoffs()

    override fun getHandoffById(handoffId: String): Flow<OrderJobHandoff?> {
        return dataSource.observeHandoffs().map { list ->
            list.find { it.handoffId == handoffId }
        }
    }

    override suspend fun findHandoffById(handoffId: String): DomainResult<OrderJobHandoff> {
        if (handoffId.isBlank()) {
            return DomainResult.Error(message = "Handoff ID cannot be blank.")
        }
        return dataSource.fetchHandoffById(handoffId)
    }

    override fun getHandoffForOrder(orderId: String): Flow<OrderJobHandoff?> {
        return dataSource.observeHandoffs().map { list ->
            list.find { it.orderId == orderId && it.handoffStatus != OrderJobHandoffStatus.CANCELLED }
        }
    }

    override suspend fun findHandoffForOrder(orderId: String): DomainResult<OrderJobHandoff> {
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        return dataSource.fetchHandoffForOrder(orderId)
    }

    override suspend fun createHandoff(
        handoffId: String,
        order: Order,
        createdBy: String?,
        notes: String?,
        timestamp: String
    ): DomainResult<OrderJobHandoff> {
        if (handoffId.isBlank()) {
            return DomainResult.Error(message = "Handoff ID cannot be blank.")
        }
        val currentHandoffs = dataSource.observeHandoffs().first()
        val eligibility = OrderJobHandoffValidator.validateHandoffEligibility(order, currentHandoffs)
        if (eligibility is DomainResult.Error) {
            return eligibility
        }

        val handoffSnapshot = OrderJobHandoff.fromOrder(
            handoffId = handoffId,
            order = order,
            createdBy = createdBy,
            notes = notes,
            timestamp = timestamp
        )

        return dataSource.insertHandoff(handoffSnapshot)
    }

    override suspend fun confirmHandoff(
        handoffId: String,
        confirmedBy: String?,
        timestamp: String
    ): DomainResult<OrderJobHandoff> {
        if (handoffId.isBlank()) {
            return DomainResult.Error(message = "Handoff ID cannot be blank.")
        }

        val handoff = when (val res = dataSource.fetchHandoffById(handoffId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(message = "Handoff data loading.")
        }

        val validation = OrderJobHandoffValidator.validateStatusTransition(
            handoff,
            OrderJobHandoffStatus.HANDED_OFF
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val updated = handoff.copy(
            handoffStatus = OrderJobHandoffStatus.HANDED_OFF,
            confirmedBy = confirmedBy,
            confirmedAt = timestamp
        )

        return dataSource.updateHandoff(updated)
    }

    override suspend fun markReadyForProduction(
        handoffId: String,
        timestamp: String
    ): DomainResult<OrderJobHandoff> {
        if (handoffId.isBlank()) {
            return DomainResult.Error(message = "Handoff ID cannot be blank.")
        }

        val handoff = when (val res = dataSource.fetchHandoffById(handoffId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(message = "Handoff data loading.")
        }

        val validation = OrderJobHandoffValidator.validateStatusTransition(
            handoff,
            OrderJobHandoffStatus.READY_FOR_PRODUCTION
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val updated = handoff.copy(
            handoffStatus = OrderJobHandoffStatus.READY_FOR_PRODUCTION
        )

        return dataSource.updateHandoff(updated)
    }

    override suspend fun cancelHandoff(
        handoffId: String,
        reason: String?
    ): DomainResult<OrderJobHandoff> {
        if (handoffId.isBlank()) {
            return DomainResult.Error(message = "Handoff ID cannot be blank.")
        }

        val handoff = when (val res = dataSource.fetchHandoffById(handoffId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(message = "Handoff data loading.")
        }

        val validation = OrderJobHandoffValidator.validateStatusTransition(
            handoff,
            OrderJobHandoffStatus.CANCELLED
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val trimmedReason = reason?.trim().orEmpty()
        val updatedNotes = if (trimmedReason.isNotBlank()) {
            val prefix = handoff.notes?.let { "$it\n" } ?: ""
            "${prefix}Handoff Cancellation: $trimmedReason"
        } else {
            handoff.notes
        }

        val updated = handoff.copy(
            handoffStatus = OrderJobHandoffStatus.CANCELLED,
            notes = updatedNotes
        )

        return dataSource.updateHandoff(updated)
    }
}
