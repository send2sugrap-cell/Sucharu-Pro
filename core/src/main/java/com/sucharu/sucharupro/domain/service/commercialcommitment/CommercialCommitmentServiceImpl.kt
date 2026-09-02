package com.sucharu.sucharupro.domain.service.commercialcommitment

import com.sucharu.sucharupro.domain.model.commercialcommitment.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuote
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuoteVersion
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.domain.repository.commercialcommitment.CommercialCommitmentRepository
import com.sucharu.sucharupro.domain.repository.printingquote.PrintingQuoteRepository
import com.sucharu.sucharupro.domain.validation.commercialcommitment.CommercialCommitmentValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.UUID

/**
 * Production implementation of [CommercialCommitmentService].
 * Handles commercial commitment preparation, idempotency checks, atomic quotation -> order conversion,
 * reconciliation, and AI agent handoff contracts.
 * Module 17 Step 03.
 */
class CommercialCommitmentServiceImpl(
    private val commitmentRepository: CommercialCommitmentRepository,
    private val quoteRepository: PrintingQuoteRepository,
    private val orderRepository: OrderRepository
) : CommercialCommitmentService {

    private val conversionMutex = Mutex()

    override suspend fun evaluateEligibility(tenantId: String, quotationId: String): DomainResult<ConversionEligibility> {
        val quoteRes = quoteRepository.findQuoteById(tenantId, quotationId)
        val quote = (quoteRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Quotation '$quotationId' not found.")

        val versionsRes = quoteRepository.listVersionsByQuoteId(tenantId, quotationId)
        val versions = (versionsRes as? DomainResult.Success)?.data ?: emptyList()
        val version = versions.find { it.versionNumber == quote.currentVersion }

        val commitmentRes = commitmentRepository.findCommitmentByQuotation(tenantId, quotationId)
        val existingCommitment = (commitmentRes as? DomainResult.Success)?.data

        val eligibility = CommercialCommitmentValidator.evaluateEligibility(
            tenantId = tenantId,
            quote = quote,
            version = version,
            existingCommitment = existingCommitment
        )

        return DomainResult.Success(eligibility)
    }

    override suspend fun prepareCommitment(
        tenantId: String,
        quotationId: String,
        targetVersionNumber: Int?,
        request: ConvertQuotationToOrderRequest?,
        actor: String
    ): DomainResult<CommercialCommitment> {
        val quoteRes = quoteRepository.findQuoteById(tenantId, quotationId)
        val quote = (quoteRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Quotation '$quotationId' not found.")

        val vNum = targetVersionNumber ?: quote.currentVersion
        val versionsRes = quoteRepository.listVersionsByQuoteId(tenantId, quotationId)
        val versions = (versionsRes as? DomainResult.Success)?.data ?: emptyList()
        val version = versions.find { it.versionNumber == vNum }
            ?: return DomainResult.Error(message = "Quotation version '$vNum' not found for quote '$quotationId'.")

        val commitmentRes = commitmentRepository.findCommitmentByQuotation(tenantId, quotationId)
        val existingCommitment = (commitmentRes as? DomainResult.Success)?.data

        val eligibility = CommercialCommitmentValidator.evaluateEligibility(
            tenantId = tenantId,
            quote = quote,
            version = version,
            existingCommitment = existingCommitment
        )
        if (!eligibility.isEligible) {
            return DomainResult.Error(message = "Quotation not eligible for commitment: ${eligibility.reasons.joinToString("; ")}")
        }

        val now = System.currentTimeMillis()
        val commitmentId = existingCommitment?.commitmentId ?: "COMM-${UUID.randomUUID().toString().take(12).uppercase()}"
        val rawFingerprint = "COMM:$tenantId:$quotationId:$vNum:${version.pricing.finalQuoteTotal}:$now"
        val integrityHash = sha256(rawFingerprint)

        val commitment = CommercialCommitmentConversionEngine.buildCommitment(
            commitmentId = commitmentId,
            quote = quote,
            version = version,
            request = request,
            actor = actor,
            idempotencyKey = request?.idempotencyKey,
            integrityHash = integrityHash,
            timestamp = now
        )

        val saveRes = if (existingCommitment != null) {
            commitmentRepository.updateCommitment(commitment)
        } else {
            commitmentRepository.saveCommitment(commitment)
        }

        if (saveRes is DomainResult.Success) {
            recordEvent(
                commitmentId = commitmentId,
                tenantId = tenantId,
                projectId = quote.projectId,
                eventType = CommercialCommitmentEventType.COMMITMENT_PREPARED,
                actor = actor,
                details = "Commercial commitment prepared for Quote #${quote.quoteNumber} v$vNum"
            )
        }

        return saveRes
    }

    override suspend fun convertQuotationToOrder(
        tenantId: String,
        request: ConvertQuotationToOrderRequest
    ): DomainResult<ConversionResult> = conversionMutex.withLock {
        val now = System.currentTimeMillis()

        // 1. Idempotency Check
        if (!request.idempotencyKey.isNullOrBlank()) {
            val existing = commitmentRepository.findCommitmentByIdempotencyKey(tenantId, request.idempotencyKey)
            if (existing is DomainResult.Success && existing.data != null && existing.data.isConverted) {
                val comm = existing.data
                return@withLock DomainResult.Success(
                    ConversionResult(
                        isSuccess = true,
                        commitment = comm,
                        orderId = comm.orderId ?: "",
                        orderNumber = comm.orderNumber ?: "",
                        customerId = comm.customerId,
                        committedAmount = comm.approvedGrandTotal,
                        currency = comm.currency,
                        message = "Idempotent duplicate request: order already exists #${comm.orderNumber}."
                    )
                )
            }
        }

        // 2. Fetch Quotation & Snapshot Version
        val quoteRes = quoteRepository.findQuoteById(tenantId, request.quotationId)
        val quote = (quoteRes as? DomainResult.Success)?.data
            ?: return@withLock DomainResult.Error(message = "Quotation '${request.quotationId}' not found.")

        val vNum = request.targetVersionNumber ?: quote.currentVersion
        val versionsRes = quoteRepository.listVersionsByQuoteId(tenantId, request.quotationId)
        val versions = (versionsRes as? DomainResult.Success)?.data ?: emptyList()
        val version = versions.find { it.versionNumber == vNum }
            ?: return@withLock DomainResult.Error(message = "Quotation version '$vNum' not found for quote '${request.quotationId}'.")

        // 3. Check Existing Commitment
        val commitmentRes = commitmentRepository.findCommitmentByQuotation(tenantId, request.quotationId)
        val existingCommitment = (commitmentRes as? DomainResult.Success)?.data

        // 4. Validate Eligibility
        val eligibility = CommercialCommitmentValidator.evaluateEligibility(
            tenantId = tenantId,
            quote = quote,
            version = version,
            existingCommitment = existingCommitment,
            now = now
        )
        if (!eligibility.isEligible) {
            return@withLock DomainResult.Error(message = "Quotation conversion blocked: ${eligibility.reasons.joinToString("; ")}")
        }

        // 5. Prepare or Reuse Commitment
        val commitmentId = existingCommitment?.commitmentId ?: "COMM-${UUID.randomUUID().toString().take(12).uppercase()}"
        val orderId = "ORD-${UUID.randomUUID().toString().take(12).uppercase()}"
        val orderNumber = request.customOrderNumber ?: "ORD-${System.currentTimeMillis() % 1000000}"

        // 6. Build Canonical Order using Conversion Engine
        val order = CommercialCommitmentConversionEngine.createOrderFromQuotation(
            orderId = orderId,
            orderNumber = orderNumber,
            quote = quote,
            version = version,
            commitmentId = commitmentId,
            request = request,
            timestamp = now
        )

        // 7. Persist Order in OrderRepository
        val orderCreateRes = orderRepository.createOrder(order)
        if (orderCreateRes !is DomainResult.Success) {
            recordEvent(
                commitmentId = commitmentId,
                tenantId = tenantId,
                projectId = quote.projectId,
                eventType = CommercialCommitmentEventType.CONVERSION_FAILED,
                actor = request.requestedBy,
                details = "Order creation failed: ${(orderCreateRes as? DomainResult.Error)?.message}"
            )
            return@withLock DomainResult.Error(message = "Failed to create Order: ${(orderCreateRes as? DomainResult.Error)?.message}")
        }

        // 8. Update Commitment to CONVERTED
        val rawFingerprint = "COMM_CONVERTED:$tenantId:$commitmentId:$orderId:$now"
        val convertedCommitment = CommercialCommitmentConversionEngine.buildCommitment(
            commitmentId = commitmentId,
            quote = quote,
            version = version,
            request = request,
            actor = request.requestedBy,
            idempotencyKey = request.idempotencyKey,
            integrityHash = sha256(rawFingerprint),
            timestamp = now
        ).copy(
            orderId = orderId,
            orderNumber = orderNumber,
            status = CommitmentStatus.CONVERTED,
            convertedAt = now,
            convertedBy = request.requestedBy
        )

        if (existingCommitment != null) {
            commitmentRepository.updateCommitment(convertedCommitment)
        } else {
            commitmentRepository.saveCommitment(convertedCommitment)
        }

        // 9. Audit Event
        recordEvent(
            commitmentId = commitmentId,
            tenantId = tenantId,
            projectId = quote.projectId,
            eventType = CommercialCommitmentEventType.CONVERSION_COMPLETED,
            actor = request.requestedBy,
            details = "Quotation #${quote.quoteNumber} successfully converted to Order #$orderNumber ($orderId)"
        )

        return@withLock DomainResult.Success(
            ConversionResult(
                isSuccess = true,
                commitment = convertedCommitment,
                orderId = orderId,
                orderNumber = orderNumber,
                customerId = convertedCommitment.customerId,
                committedAmount = convertedCommitment.approvedGrandTotal,
                currency = convertedCommitment.currency,
                message = "Quotation successfully converted to Order #$orderNumber."
            )
        )
    }

    override suspend fun getCommitment(tenantId: String, commitmentId: String): DomainResult<CommercialCommitment?> =
        commitmentRepository.findCommitmentById(tenantId, commitmentId)

    override suspend fun getCommitmentByQuotation(tenantId: String, quotationId: String): DomainResult<CommercialCommitment?> =
        commitmentRepository.findCommitmentByQuotation(tenantId, quotationId)

    override suspend fun listCommitments(tenantId: String, limit: Int): DomainResult<List<CommercialCommitment>> =
        commitmentRepository.listCommitments(tenantId, limit)

    override suspend fun listCommitmentEvents(tenantId: String, commitmentId: String): DomainResult<List<CommercialCommitmentEvent>> =
        commitmentRepository.listEvents(tenantId, commitmentId)

    override suspend fun reconcileCommitment(tenantId: String, commitmentId: String): DomainResult<CommercialCommitmentReconciliationResult> {
        val commRes = commitmentRepository.findCommitmentById(tenantId, commitmentId)
        val commitment = (commRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Commitment '$commitmentId' not found.")

        val quoteRes = quoteRepository.findQuoteById(tenantId, commitment.quotationId)
        val quote = (quoteRes as? DomainResult.Success)?.data

        val order: Order? = if (!commitment.orderId.isNullOrBlank()) {
            val oRes = orderRepository.findOrderById(commitment.orderId)
            (oRes as? DomainResult.Success)?.data
        } else null

        val discrepancies = mutableListOf<String>()

        val customerMatch = quote != null && (commitment.customerId == (quote.customerRef ?: "DEFAULT_CUSTOMER"))
        if (!customerMatch) discrepancies += "Customer ID mismatch between quote and commitment."

        val currencyMatch = quote != null && (commitment.currency == quote.currency)
        if (!currencyMatch) discrepancies += "Currency mismatch between quote and commitment."

        val quantityMatch = quote != null && (commitment.committedQuantity == quote.orderedQuantity)
        if (!quantityMatch) discrepancies += "Quantity mismatch between quote and commitment."

        val amountMatch = commitment.approvedSubtotal.subtract(commitment.approvedDiscount)
            .add(commitment.approvedTax).compareTo(commitment.approvedGrandTotal) == 0
        if (!amountMatch) discrepancies += "Mathematical total inconsistency in commercial commitment."

        val statusMatch = if (commitment.status == CommitmentStatus.CONVERTED) {
            commitment.orderId != null && order != null
        } else true
        if (!statusMatch) discrepancies += "Commitment marked CONVERTED but target order missing in Order repository."

        val isFullyReconciled = discrepancies.isEmpty()

        recordEvent(
            commitmentId = commitmentId,
            tenantId = tenantId,
            projectId = commitment.projectId,
            eventType = CommercialCommitmentEventType.COMMITMENT_RECONCILED,
            actor = "SYSTEM_RECONCILER",
            details = "Reconciliation completed: isFullyReconciled=$isFullyReconciled (Discrepancies: ${discrepancies.size})"
        )

        return DomainResult.Success(
            CommercialCommitmentReconciliationResult(
                commitmentId = commitmentId,
                quotationId = commitment.quotationId,
                orderId = commitment.orderId,
                isFullyReconciled = isFullyReconciled,
                customerMatch = customerMatch,
                currencyMatch = currencyMatch,
                quantityMatch = quantityMatch,
                amountMatch = amountMatch,
                statusIntegrityMatch = statusMatch,
                discrepancies = discrepancies
            )
        )
    }

    override suspend fun exportHandoffContract(tenantId: String, commitmentId: String): DomainResult<Module17Step03CommercialCommitmentHandoffContract> {
        val commRes = commitmentRepository.findCommitmentById(tenantId, commitmentId)
        val commitment = (commRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Commitment '$commitmentId' not found.")

        val quoteRes = quoteRepository.findQuoteById(tenantId, commitment.quotationId)
        val quote = (quoteRes as? DomainResult.Success)?.data
        val quoteNumber = quote?.quoteNumber ?: commitment.quotationId

        val reconRes = reconcileCommitment(tenantId, commitmentId)
        val reconStatus = if (reconRes is DomainResult.Success && reconRes.data.isFullyReconciled) "RECONCILED" else "UNRECONCILED"

        val now = System.currentTimeMillis()
        val rawPayload = "HANDOFF_V3:${commitment.commitmentId}:${commitment.quotationId}:${commitment.orderId}:$reconStatus:$now"
        val hash = sha256(rawPayload)

        val handoff = Module17Step03CommercialCommitmentHandoffContract(
            handoffId = "HANDOFF-${UUID.randomUUID().toString().take(8).uppercase()}",
            commitmentId = commitment.commitmentId,
            tenantId = commitment.tenantId,
            projectId = commitment.projectId,
            quotationId = commitment.quotationId,
            quotationVersion = commitment.quotationVersion,
            quotationNumber = quoteNumber,
            customerId = commitment.customerId,
            orderId = commitment.orderId,
            orderNumber = commitment.orderNumber,
            commitmentStatus = commitment.status.name,
            committedQuantity = commitment.committedQuantity,
            approvedUnitPrice = commitment.approvedUnitPrice.toPlainString(),
            approvedSubtotal = commitment.approvedSubtotal.toPlainString(),
            approvedDiscount = commitment.approvedDiscount.toPlainString(),
            approvedTax = commitment.approvedTax.toPlainString(),
            approvedGrandTotal = commitment.approvedGrandTotal.toPlainString(),
            currency = commitment.currency,
            paymentTerms = commitment.paymentTerms,
            deliveryTerms = commitment.deliveryTerms,
            conversionTimestamp = commitment.convertedAt,
            reconciliationStatus = reconStatus,
            integrityHash = hash,
            generatedAt = now
        )

        recordEvent(
            commitmentId = commitmentId,
            tenantId = tenantId,
            projectId = commitment.projectId,
            eventType = CommercialCommitmentEventType.HANDOFF_EXPORTED,
            actor = "AI_AGENT_HANDOFF",
            details = "AI Handoff contract exported with fingerprint ${hash.take(12)}..."
        )

        return DomainResult.Success(handoff)
    }

    private suspend fun recordEvent(
        commitmentId: String,
        tenantId: String,
        projectId: String,
        eventType: CommercialCommitmentEventType,
        actor: String,
        details: String
    ) {
        val event = CommercialCommitmentEvent(
            eventId = UUID.randomUUID().toString(),
            commitmentId = commitmentId,
            tenantId = tenantId,
            projectId = projectId,
            eventType = eventType,
            actor = actor,
            detailsJson = details,
            occurredAt = System.currentTimeMillis()
        )
        commitmentRepository.saveEvent(event)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
