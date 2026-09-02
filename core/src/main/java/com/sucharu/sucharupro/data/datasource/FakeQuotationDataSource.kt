package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.PaymentTermType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory reactive implementation of [QuotationDataSource] for development and testing.
 */
class FakeQuotationDataSource(
    initialQuotations: List<Quotation> = defaultSampleQuotations()
) : QuotationDataSource {

    private val mutex = Mutex()
    private val _quotations = MutableStateFlow<List<Quotation>>(initialQuotations)

    override fun observeQuotations(): Flow<List<Quotation>> = _quotations.asStateFlow()

    override suspend fun fetchQuotationById(quotationId: String): DomainResult<Quotation> = mutex.withLock {
        val quotation = _quotations.value.find { it.quotationId == quotationId }
        return if (quotation != null) {
            DomainResult.Success(quotation)
        } else {
            DomainResult.Error(message = "Quotation not found with ID: $quotationId")
        }
    }

    override suspend fun insertQuotation(quotation: Quotation): DomainResult<Quotation> = mutex.withLock {
        if (_quotations.value.any { it.quotationId == quotation.quotationId }) {
            return DomainResult.Error(message = "Quotation with ID '${quotation.quotationId}' already exists.")
        }
        if (_quotations.value.any { it.quotationNumber.equals(quotation.quotationNumber, ignoreCase = true) }) {
            return DomainResult.Error(message = "Quotation with Number '${quotation.quotationNumber}' already exists.")
        }
        if (quotation.revisions.isEmpty()) {
            return DomainResult.Error(message = "Quotation must contain at least one revision.")
        }

        _quotations.value = _quotations.value + quotation
        DomainResult.Success(quotation)
    }

    override suspend fun updateQuotation(quotation: Quotation): DomainResult<Quotation> = mutex.withLock {
        val index = _quotations.value.indexOfFirst { it.quotationId == quotation.quotationId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent quotation: ${quotation.quotationId}")
        }

        val existing = _quotations.value[index]
        val updated = quotation.copy(
            quotationId = existing.quotationId,
            createdAt = existing.createdAt
        )

        val currentList = _quotations.value.toMutableList()
        currentList[index] = updated
        _quotations.value = currentList.toList()
        DomainResult.Success(updated)
    }

    override suspend fun updateQuotationStatus(
        quotationId: String,
        status: QuotationStatusType
    ): DomainResult<Quotation> = mutex.withLock {
        val index = _quotations.value.indexOfFirst { it.quotationId == quotationId }
        if (index == -1) {
            return DomainResult.Error(message = "Quotation not found: $quotationId")
        }

        val existing = _quotations.value[index]
        if (!existing.status.canTransitionTo(status)) {
            return DomainResult.Error(
                message = "Invalid quotation status transition from '${existing.status.defaultLabel}' to '${status.defaultLabel}'."
            )
        }

        val updated = existing.copy(status = status)
        val currentList = _quotations.value.toMutableList()
        currentList[index] = updated
        _quotations.value = currentList.toList()
        DomainResult.Success(updated)
    }

    override suspend fun deleteQuotation(quotationId: String): DomainResult<Unit> = mutex.withLock {
        val quotation = _quotations.value.find { it.quotationId == quotationId }
            ?: return DomainResult.Error(message = "Quotation not found: $quotationId")

        if (quotation.isApproved) {
            return DomainResult.Error(message = "Cannot delete an approved quotation: $quotationId")
        }

        _quotations.value = _quotations.value.filterNot { it.quotationId == quotationId }
        DomainResult.Success(Unit)
    }

    override suspend fun insertQuotationRevision(
        quotationId: String,
        revision: QuotationRevision
    ): DomainResult<QuotationRevision> = mutex.withLock {
        val index = _quotations.value.indexOfFirst { it.quotationId == quotationId }
        if (index == -1) {
            return DomainResult.Error(message = "Quotation not found: $quotationId")
        }

        val quotation = _quotations.value[index]
        if (quotation.revisions.any { it.revisionId == revision.revisionId }) {
            return DomainResult.Error(message = "Revision with ID '${revision.revisionId}' already exists.")
        }
        if (quotation.revisions.any { it.revisionNumber == revision.revisionNumber }) {
            return DomainResult.Error(message = "Revision with Number '${revision.revisionNumber}' already exists for quotation.")
        }

        val updatedRevisions = quotation.revisions + revision
        val updatedQuotation = quotation.copy(
            revisions = updatedRevisions,
            currentRevisionNumber = revision.revisionNumber
        )

        val currentList = _quotations.value.toMutableList()
        currentList[index] = updatedQuotation
        _quotations.value = currentList.toList()
        DomainResult.Success(revision)
    }

    override suspend fun approveQuotationRevision(
        quotationId: String,
        revisionId: String,
        approvedBy: String,
        timestamp: String
    ): DomainResult<Quotation> = mutex.withLock {
        val index = _quotations.value.indexOfFirst { it.quotationId == quotationId }
        if (index == -1) {
            return DomainResult.Error(message = "Quotation not found: $quotationId")
        }

        val quotation = _quotations.value[index]
        val revision = quotation.revisions.find { it.revisionId == revisionId }
            ?: return DomainResult.Error(message = "Revision '$revisionId' does not belong to quotation '$quotationId'.")

        val updatedQuotation = quotation.copy(
            status = QuotationStatusType.APPROVED,
            currentRevisionNumber = revision.revisionNumber,
            approvedRevisionId = revision.revisionId,
            approvedBy = approvedBy,
            approvedAt = timestamp,
            updatedAt = timestamp
        )

        val currentList = _quotations.value.toMutableList()
        currentList[index] = updatedQuotation
        _quotations.value = currentList.toList()
        DomainResult.Success(updatedQuotation)
    }

    companion object {
        fun defaultSampleQuotations(): List<Quotation> {
            val rev1 = QuotationRevision(
                revisionId = "rev-001-v1",
                quotationId = "qt-001",
                revisionNumber = 1,
                items = listOf(
                    QuotationItem(
                        itemId = "qt-item-01",
                        description = "Visiting Card (300 GSM Art Card, Matte, Both Sides)",
                        specification = "3.25x2.0 in, 4/4 Color, 1000 Pcs",
                        quantity = 1000,
                        unit = "Pcs",
                        unitPrice = 0.85.toMoney(),
                        discount = Money.ZERO
                    )
                ),
                discount = Money.ZERO,
                paymentTerms = PaymentTerms.DEFAULT,
                deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
                revisionReason = "Initial Commercial Quotation",
                createdAt = "2026-08-11T10:00:00Z"
            )

            val rev2 = QuotationRevision(
                revisionId = "rev-001-v2",
                quotationId = "qt-001",
                revisionNumber = 2,
                items = listOf(
                    QuotationItem(
                        itemId = "qt-item-01",
                        description = "Visiting Card (300 GSM Art Card, Matte + Spot UV)",
                        specification = "3.25x2.0 in, 4/4 Color + Spot UV, 1000 Pcs",
                        quantity = 1000,
                        unit = "Pcs",
                        unitPrice = 1.20.toMoney(),
                        discount = 100.toMoney()
                    )
                ),
                discount = Money.ZERO,
                paymentTerms = PaymentTerms(
                    type = PaymentTermType.PARTIAL_ADVANCE,
                    advancePercentage = 50
                ),
                deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
                revisionReason = "Customer added Spot UV effect to logo",
                createdAt = "2026-08-12T14:30:00Z",
                previousRevisionId = "rev-001-v1"
            )

            return listOf(
                Quotation(
                    quotationId = "qt-001",
                    quotationNumber = "QT-000001",
                    customerId = "cus-001",
                    inquiryId = "inq-001",
                    currentRevisionNumber = 2,
                    revisions = listOf(rev1, rev2),
                    status = QuotationStatusType.APPROVED,
                    validUntil = "2026-09-12T23:59:59Z",
                    termsAndConditions = "Delivery within 3 working days from advance deposit confirmation.",
                    approvedAt = "2026-08-13T11:00:00Z",
                    approvedBy = "Sales Manager",
                    approvedRevisionId = "rev-001-v2",
                    createdAt = "2026-08-11T10:00:00Z",
                    updatedAt = "2026-08-13T11:00:00Z"
                )
            )
        }
    }
}
