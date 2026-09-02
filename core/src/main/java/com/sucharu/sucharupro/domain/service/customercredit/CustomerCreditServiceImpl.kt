package com.sucharu.sucharupro.domain.service.customercredit

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustment
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAllocationStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAllocation
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAuditEvent
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditEntityType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditSummary
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefund
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customercredit.CustomerCreditRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository
import com.sucharu.sucharupro.domain.repository.customerpayment.CustomerPaymentRepository
import com.sucharu.sucharupro.domain.validation.customercredit.CustomerCreditValidator
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Production implementation of [CustomerCreditService] (Module 14 Step 04).
 */
class CustomerCreditServiceImpl(
    private val creditRepository: CustomerCreditRepository,
    private val accountRepository: CustomerFinancialAccountRepository,
    private val invoiceRepository: CustomerInvoiceRepository,
    private val customerRepository: CustomerRepository,
    private val paymentRepository: CustomerPaymentRepository
) : CustomerCreditService {

    override suspend fun recordAdvance(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerFinancialAccountId: String,
        amount: BigDecimal,
        currency: String,
        paymentMethod: CustomerPaymentMethod,
        receiptDate: Long,
        referenceNumber: String?,
        externalReference: String?,
        notes: String?,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerAdvance> {
        val normalizedAmount = amount.setScale(4, RoundingMode.HALF_UP)

        // 1. Check idempotency key if provided
        if (!idempotencyKey.isNullOrBlank()) {
            val existingRes = creditRepository.findAdvanceByIdempotencyKey(tenantId, projectId, idempotencyKey)
            if (existingRes is DomainResult.Success && existingRes.data != null) {
                val existing = existingRes.data!!
                if (existing.customerId != customerId || existing.amount.compareTo(normalizedAmount) != 0) {
                    return DomainResult.Error(
                        IllegalStateException("Idempotency key '$idempotencyKey' was already used with conflicting parameters.")
                    )
                }
                return DomainResult.Success(existing)
            }
        }

        // 2. Validate customer
        val customerRes = customerRepository.findCustomerById(customerId)
        if (customerRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Customer '$customerId' not found: ${customerRes.message}"))
        }

        // 3. Validate financial account
        val accountRes = accountRepository.getAccountById(tenantId, projectId, customerFinancialAccountId)
        if (accountRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("CustomerFinancialAccount '$customerFinancialAccountId' not found"))
        }
        val account = (accountRes as DomainResult.Success).data

        // 4. Domain validation
        val valRes = CustomerCreditValidator.validateAdvanceRecording(
            tenantId, projectId, customerId, customerFinancialAccountId,
            normalizedAmount, currency, account
        )
        if (valRes is DomainResult.Error) return valRes

        // 5. Create Advance
        val advanceId = "ADV-${UUID.randomUUID().toString().take(8).uppercase()}"
        val advanceNumber = "ADV-${System.currentTimeMillis().toString().takeLast(6)}-${UUID.randomUUID().toString().take(4).uppercase()}"
        val now = System.currentTimeMillis()

        val advance = CustomerAdvance(
            advanceId = advanceId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = customerFinancialAccountId,
            advanceNumber = advanceNumber,
            amount = normalizedAmount,
            allocatedAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            availableAmount = normalizedAmount,
            currency = currency.uppercase(),
            paymentMethod = paymentMethod,
            receiptDate = receiptDate,
            referenceNumber = referenceNumber,
            externalReference = externalReference,
            notes = notes,
            status = CustomerAdvanceStatus.AVAILABLE,
            idempotencyKey = idempotencyKey,
            createdAt = now,
            createdBy = actorId,
            updatedAt = now,
            updatedBy = actorId,
            version = 1L
        )

        val createRes = creditRepository.createAdvance(advance)
        if (createRes is DomainResult.Success) {
            creditRepository.recordAuditEvent(
                CustomerCreditAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    entityType = CustomerCreditEntityType.ADVANCE,
                    entityId = advanceId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "ADVANCE_RECORDED",
                    previousStatus = null,
                    newStatus = CustomerAdvanceStatus.AVAILABLE.name,
                    amount = normalizedAmount,
                    reason = "Customer advance deposit recorded",
                    occurredAt = now,
                    metadataJson = """{"amount":"$normalizedAmount","method":"${paymentMethod.name}"}"""
                )
            )
        }
        return createRes
    }

    override suspend fun cancelAdvance(
        tenantId: String,
        projectId: String,
        advanceId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerAdvance> {
        val existingRes = creditRepository.getAdvanceById(tenantId, projectId, advanceId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        if (existing.allocatedAmount > BigDecimal.ZERO) {
            return DomainResult.Error(
                IllegalStateException("Cannot cancel advance '${existing.advanceId}' that has active allocations. Reverse allocations first.")
            )
        }
        if (existing.status.isCancelled) {
            return DomainResult.Error(IllegalStateException("Advance '${existing.advanceId}' is already cancelled."))
        }

        val cancelRes = creditRepository.cancelAdvance(tenantId, projectId, advanceId, reason, actorId, expectedVersion)
        if (cancelRes is DomainResult.Success) {
            creditRepository.recordAuditEvent(
                CustomerCreditAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = existing.customerId,
                    entityType = CustomerCreditEntityType.ADVANCE,
                    entityId = advanceId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "ADVANCE_CANCELLED",
                    previousStatus = existing.status.name,
                    newStatus = CustomerAdvanceStatus.CANCELLED.name,
                    amount = existing.amount,
                    reason = reason,
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return cancelRes
    }

    override suspend fun allocateCreditToInvoice(
        tenantId: String,
        projectId: String,
        customerId: String,
        invoiceId: String,
        advanceId: String?,
        amount: BigDecimal,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCreditAllocation> {
        val normalizedAmount = amount.setScale(4, RoundingMode.HALF_UP)

        // 1. Idempotency check
        if (!idempotencyKey.isNullOrBlank()) {
            val existingRes = creditRepository.findAllocationByIdempotencyKey(tenantId, projectId, idempotencyKey)
            if (existingRes is DomainResult.Success && existingRes.data != null) {
                val existing = existingRes.data!!
                if (existing.customerId != customerId || existing.invoiceId != invoiceId || existing.allocatedAmount.compareTo(normalizedAmount) != 0) {
                    return DomainResult.Error(
                        IllegalStateException("Idempotency key '$idempotencyKey' was already used with conflicting allocation parameters.")
                    )
                }
                return DomainResult.Success(existing)
            }
        }

        // 2. Invoice lookup
        val invoiceRes = invoiceRepository.getInvoiceById(tenantId, projectId, invoiceId)
        if (invoiceRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Invoice '$invoiceId' not found: ${invoiceRes.message}"))
        }
        val invoice = (invoiceRes as DomainResult.Success).data

        // 3. Advance lookup (if specific advance specified) or get summary available credit
        var advance: CustomerAdvance? = null
        if (!advanceId.isNullOrBlank()) {
            val advRes = creditRepository.getAdvanceById(tenantId, projectId, advanceId)
            if (advRes is DomainResult.Error) {
                return DomainResult.Error(IllegalArgumentException("Advance '$advanceId' not found: ${advRes.message}"))
            }
            advance = (advRes as DomainResult.Success).data
        }

        val summaryRes = creditRepository.getCustomerCreditSummary(tenantId, projectId, customerId)
        val availableCredit = if (summaryRes is DomainResult.Success) summaryRes.data.totalAvailableCredit else BigDecimal.ZERO

        // 4. Validate allocation
        val valRes = CustomerCreditValidator.validateCreditAllocation(
            tenantId, projectId, customerId, advance, availableCredit, invoice, normalizedAmount
        )
        if (valRes is DomainResult.Error) return valRes

        // 5. Deduct advance available amount if advance-linked
        if (advance != null) {
            val newAllocated = advance.allocatedAmount.add(normalizedAmount).setScale(4, RoundingMode.HALF_UP)
            val newAvailable = advance.availableAmount.subtract(normalizedAmount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
            val newAdvStatus = if (newAvailable.compareTo(BigDecimal.ZERO) == 0) {
                CustomerAdvanceStatus.EXHAUSTED
            } else {
                CustomerAdvanceStatus.ALLOCATED
            }
            val updateAdvRes = creditRepository.updateAdvanceAllocation(
                tenantId, projectId, advance.advanceId,
                newAllocated, newAvailable, newAdvStatus,
                actorId, advance.version
            )
            if (updateAdvRes is DomainResult.Error) {
                return DomainResult.Error(IllegalStateException("Failed to update advance allocation: ${updateAdvRes.message}"))
            }
        }

        // 6. Update Invoice Paid and Due Balance
        val newPaid = invoice.paidAmount.add(normalizedAmount).setScale(4, RoundingMode.HALF_UP)
        val newDue = invoice.dueAmount.subtract(normalizedAmount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val newInvoiceStatus = if (newDue.compareTo(BigDecimal.ZERO) == 0) {
            CustomerInvoiceStatus.PAID
        } else {
            CustomerInvoiceStatus.PARTIALLY_PAID
        }

        val invUpdateRes = invoiceRepository.updateInvoicePayment(
            tenantId = tenantId,
            projectId = projectId,
            invoiceId = invoice.invoiceId,
            newPaidAmount = newPaid,
            newDueAmount = newDue,
            newStatus = newInvoiceStatus,
            actorId = actorId,
            expectedVersion = invoice.version
        )
        if (invUpdateRes is DomainResult.Error) {
            return DomainResult.Error(IllegalStateException("Failed to update invoice balance during allocation: ${invUpdateRes.message}"))
        }

        // Emit Invoice Audit Event
        invoiceRepository.recordAuditEvent(
            CustomerInvoiceAuditEvent(
                auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                invoiceId = invoice.invoiceId,
                customerId = customerId,
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                action = "CREDIT_ALLOCATED",
                previousStatus = invoice.status,
                newStatus = newInvoiceStatus,
                reason = "Customer credit of $normalizedAmount allocated",
                occurredAt = System.currentTimeMillis(),
                metadataJson = """{"allocatedAmount":"$normalizedAmount","advanceId":"$advanceId"}"""
            )
        )

        // 7. Create Allocation record
        val allocationId = "ALC-${UUID.randomUUID().toString().take(8).uppercase()}"
        val now = System.currentTimeMillis()

        val allocation = CustomerCreditAllocation(
            allocationId = allocationId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = invoice.customerFinancialAccountId,
            advanceId = advanceId,
            invoiceId = invoiceId,
            allocatedAmount = normalizedAmount,
            currency = invoice.currency,
            status = CustomerAllocationStatus.ALLOCATED,
            idempotencyKey = idempotencyKey,
            allocatedAt = now,
            allocatedBy = actorId,
            version = 1L
        )

        val createAllocRes = creditRepository.createAllocation(allocation)
        if (createAllocRes is DomainResult.Success) {
            creditRepository.recordAuditEvent(
                CustomerCreditAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    entityType = CustomerCreditEntityType.ALLOCATION,
                    entityId = allocationId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "CREDIT_ALLOCATED",
                    previousStatus = null,
                    newStatus = CustomerAllocationStatus.ALLOCATED.name,
                    amount = normalizedAmount,
                    reason = "Credit allocated to invoice '$invoiceId'",
                    occurredAt = now,
                    metadataJson = """{"invoiceId":"$invoiceId","advanceId":"$advanceId","amount":"$normalizedAmount"}"""
                )
            )
        }
        return createAllocRes
    }

    override suspend fun reverseCreditAllocation(
        tenantId: String,
        projectId: String,
        allocationId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerCreditAllocation> {
        val allocRes = creditRepository.getAllocationById(tenantId, projectId, allocationId)
        if (allocRes is DomainResult.Error) return allocRes
        val alloc = (allocRes as DomainResult.Success).data

        if (alloc.status.isReversed) {
            return DomainResult.Error(IllegalStateException("Allocation '$allocationId' is already reversed."))
        }
        if (reason.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("A reason must be provided for reversing a credit allocation."))
        }

        // 1. Reopen advance available amount if advance-linked
        if (!alloc.advanceId.isNullOrBlank()) {
            val advRes = creditRepository.getAdvanceById(tenantId, projectId, alloc.advanceId)
            if (advRes is DomainResult.Success) {
                val adv = advRes.data
                val newAllocated = adv.allocatedAmount.subtract(alloc.allocatedAmount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
                val newAvailable = adv.availableAmount.add(alloc.allocatedAmount).min(adv.amount).setScale(4, RoundingMode.HALF_UP)
                val newAdvStatus = CustomerAdvanceStatus.AVAILABLE
                creditRepository.updateAdvanceAllocation(
                    tenantId, projectId, adv.advanceId,
                    newAllocated, newAvailable, newAdvStatus,
                    actorId, adv.version
                )
            }
        }

        // 2. Reverse Invoice balance
        val invRes = invoiceRepository.getInvoiceById(tenantId, projectId, alloc.invoiceId)
        if (invRes is DomainResult.Success) {
            val inv = invRes.data
            val reversedPaid = inv.paidAmount.subtract(alloc.allocatedAmount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
            val reversedDue = inv.dueAmount.add(alloc.allocatedAmount).min(inv.grandTotal).setScale(4, RoundingMode.HALF_UP)
            val reversedStatus = if (reversedPaid.compareTo(BigDecimal.ZERO) == 0) {
                CustomerInvoiceStatus.ISSUED
            } else {
                CustomerInvoiceStatus.PARTIALLY_PAID
            }

            invoiceRepository.updateInvoicePayment(
                tenantId = tenantId,
                projectId = projectId,
                invoiceId = inv.invoiceId,
                newPaidAmount = reversedPaid,
                newDueAmount = reversedDue,
                newStatus = reversedStatus,
                actorId = actorId,
                expectedVersion = inv.version
            )

            invoiceRepository.recordAuditEvent(
                CustomerInvoiceAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    invoiceId = inv.invoiceId,
                    customerId = alloc.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "CREDIT_ALLOCATION_REVERSED",
                    previousStatus = inv.status,
                    newStatus = reversedStatus,
                    reason = "Allocation '$allocationId' reversed: $reason",
                    occurredAt = System.currentTimeMillis()
                )
            )
        }

        // 3. Update allocation status to REVERSED
        val updateAllocRes = creditRepository.updateAllocationStatus(
            tenantId, projectId, allocationId,
            CustomerAllocationStatus.REVERSED, reason, actorId, expectedVersion
        )
        if (updateAllocRes is DomainResult.Success) {
            creditRepository.recordAuditEvent(
                CustomerCreditAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = alloc.customerId,
                    entityType = CustomerCreditEntityType.ALLOCATION,
                    entityId = allocationId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "ALLOCATION_REVERSED",
                    previousStatus = CustomerAllocationStatus.ALLOCATED.name,
                    newStatus = CustomerAllocationStatus.REVERSED.name,
                    amount = alloc.allocatedAmount,
                    reason = reason,
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updateAllocRes
    }

    override suspend fun recordAdjustment(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerFinancialAccountId: String,
        adjustmentType: CustomerAdjustmentType,
        amount: BigDecimal,
        currency: String,
        reason: String,
        referenceNumber: String?,
        notes: String?,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerAdjustment> {
        val normalizedAmount = amount.setScale(4, RoundingMode.HALF_UP)

        // 1. Idempotency check
        if (!idempotencyKey.isNullOrBlank()) {
            val existingRes = creditRepository.findAdjustmentByIdempotencyKey(tenantId, projectId, idempotencyKey)
            if (existingRes is DomainResult.Success && existingRes.data != null) {
                val existing = existingRes.data!!
                if (existing.customerId != customerId || existing.adjustmentType != adjustmentType || existing.amount.compareTo(normalizedAmount) != 0) {
                    return DomainResult.Error(
                        IllegalStateException("Idempotency key '$idempotencyKey' was already used with conflicting adjustment parameters.")
                    )
                }
                return DomainResult.Success(existing)
            }
        }

        // 2. Validate financial account
        val accountRes = accountRepository.getAccountById(tenantId, projectId, customerFinancialAccountId)
        if (accountRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("CustomerFinancialAccount '$customerFinancialAccountId' not found"))
        }
        val account = (accountRes as DomainResult.Success).data

        // 3. Current available credit
        val summaryRes = creditRepository.getCustomerCreditSummary(tenantId, projectId, customerId)
        val currentAvail = if (summaryRes is DomainResult.Success) summaryRes.data.totalAvailableCredit else BigDecimal.ZERO

        // 4. Validate adjustment
        val valRes = CustomerCreditValidator.validateAdjustment(
            tenantId, projectId, customerId, customerFinancialAccountId,
            normalizedAmount, adjustmentType, reason, account, currentAvail
        )
        if (valRes is DomainResult.Error) return valRes

        // 5. Create Adjustment
        val adjustmentId = "ADJ-${UUID.randomUUID().toString().take(8).uppercase()}"
        val adjustmentNumber = "ADJ-${System.currentTimeMillis().toString().takeLast(6)}-${UUID.randomUUID().toString().take(4).uppercase()}"
        val now = System.currentTimeMillis()

        val adjustment = CustomerAdjustment(
            adjustmentId = adjustmentId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = customerFinancialAccountId,
            adjustmentNumber = adjustmentNumber,
            adjustmentType = adjustmentType,
            amount = normalizedAmount,
            currency = currency.uppercase(),
            reason = reason,
            referenceNumber = referenceNumber,
            notes = notes,
            status = CustomerAdjustmentStatus.APPLIED,
            idempotencyKey = idempotencyKey,
            createdAt = now,
            createdBy = actorId,
            updatedAt = now,
            updatedBy = actorId,
            version = 1L
        )

        val createRes = creditRepository.createAdjustment(adjustment)
        if (createRes is DomainResult.Success) {
            creditRepository.recordAuditEvent(
                CustomerCreditAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    entityType = CustomerCreditEntityType.ADJUSTMENT,
                    entityId = adjustmentId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "ADJUSTMENT_APPLIED",
                    previousStatus = null,
                    newStatus = CustomerAdjustmentStatus.APPLIED.name,
                    amount = normalizedAmount,
                    reason = reason,
                    occurredAt = now,
                    metadataJson = """{"type":"${adjustmentType.name}","amount":"$normalizedAmount"}"""
                )
            )
        }
        return createRes
    }

    override suspend fun requestRefund(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerFinancialAccountId: String,
        paymentId: String?,
        advanceId: String?,
        amount: BigDecimal,
        currency: String,
        refundMethod: CustomerPaymentMethod,
        reason: String,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerRefund> {
        val normalizedAmount = amount.setScale(4, RoundingMode.HALF_UP)

        // 1. Idempotency check
        if (!idempotencyKey.isNullOrBlank()) {
            val existingRes = creditRepository.findRefundByIdempotencyKey(tenantId, projectId, idempotencyKey)
            if (existingRes is DomainResult.Success && existingRes.data != null) {
                val existing = existingRes.data!!
                if (existing.customerId != customerId || existing.amount.compareTo(normalizedAmount) != 0) {
                    return DomainResult.Error(
                        IllegalStateException("Idempotency key '$idempotencyKey' was already used with conflicting refund parameters.")
                    )
                }
                return DomainResult.Success(existing)
            }
        }

        // 2. Fetch source payment or advance if given
        var payment: CustomerPayment? = null
        if (!paymentId.isNullOrBlank()) {
            val payRes = paymentRepository.getPaymentById(tenantId, projectId, paymentId)
            if (payRes is DomainResult.Error) {
                return DomainResult.Error(IllegalArgumentException("Payment '$paymentId' not found: ${payRes.message}"))
            }
            payment = (payRes as DomainResult.Success).data
        }

        var advance: CustomerAdvance? = null
        if (!advanceId.isNullOrBlank()) {
            val advRes = creditRepository.getAdvanceById(tenantId, projectId, advanceId)
            if (advRes is DomainResult.Error) {
                return DomainResult.Error(IllegalArgumentException("Advance '$advanceId' not found: ${advRes.message}"))
            }
            advance = (advRes as DomainResult.Success).data
        }

        val summaryRes = creditRepository.getCustomerCreditSummary(tenantId, projectId, customerId)
        val currentAvail = if (summaryRes is DomainResult.Success) summaryRes.data.totalAvailableCredit else BigDecimal.ZERO

        // 3. Domain validation
        val valRes = CustomerCreditValidator.validateRefundRequest(
            tenantId, projectId, customerId, normalizedAmount, reason, payment, advance, currentAvail
        )
        if (valRes is DomainResult.Error) return valRes

        // 4. Create Refund
        val refundId = "REF-${UUID.randomUUID().toString().take(8).uppercase()}"
        val refundNumber = "REF-${System.currentTimeMillis().toString().takeLast(6)}-${UUID.randomUUID().toString().take(4).uppercase()}"
        val now = System.currentTimeMillis()

        val refund = CustomerRefund(
            refundId = refundId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = customerFinancialAccountId,
            paymentId = paymentId,
            advanceId = advanceId,
            refundNumber = refundNumber,
            amount = normalizedAmount,
            currency = currency.uppercase(),
            refundMethod = refundMethod,
            reason = reason,
            status = CustomerRefundStatus.REQUESTED,
            idempotencyKey = idempotencyKey,
            createdAt = now,
            createdBy = actorId,
            updatedAt = now,
            updatedBy = actorId,
            version = 1L
        )

        val createRes = creditRepository.createRefund(refund)
        if (createRes is DomainResult.Success) {
            creditRepository.recordAuditEvent(
                CustomerCreditAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    entityType = CustomerCreditEntityType.REFUND,
                    entityId = refundId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "REFUND_REQUESTED",
                    previousStatus = null,
                    newStatus = CustomerRefundStatus.REQUESTED.name,
                    amount = normalizedAmount,
                    reason = reason,
                    occurredAt = now,
                    metadataJson = """{"amount":"$normalizedAmount","paymentId":"$paymentId","advanceId":"$advanceId"}"""
                )
            )
        }
        return createRes
    }

    override suspend fun approveRefund(
        tenantId: String,
        projectId: String,
        refundId: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund> {
        val existingRes = creditRepository.getRefundById(tenantId, projectId, refundId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerCreditValidator.validateRefundTransition(
            existing, CustomerRefundStatus.APPROVED, null
        )
        if (valRes is DomainResult.Error) return valRes

        val updateRes = creditRepository.updateRefundStatus(
            tenantId, projectId, refundId,
            CustomerRefundStatus.APPROVED, null, actorId, expectedVersion
        )
        if (updateRes is DomainResult.Success) {
            creditRepository.recordAuditEvent(
                CustomerCreditAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = existing.customerId,
                    entityType = CustomerCreditEntityType.REFUND,
                    entityId = refundId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "REFUND_APPROVED",
                    previousStatus = existing.status.name,
                    newStatus = CustomerRefundStatus.APPROVED.name,
                    amount = existing.amount,
                    reason = "Refund approved by $actorRole",
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updateRes
    }

    override suspend fun processRefund(
        tenantId: String,
        projectId: String,
        refundId: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund> {
        val existingRes = creditRepository.getRefundById(tenantId, projectId, refundId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerCreditValidator.validateRefundTransition(
            existing, CustomerRefundStatus.PROCESSED, null
        )
        if (valRes is DomainResult.Error) return valRes

        val updateRes = creditRepository.updateRefundStatus(
            tenantId, projectId, refundId,
            CustomerRefundStatus.PROCESSED, null, actorId, expectedVersion
        )
        if (updateRes is DomainResult.Success) {
            creditRepository.recordAuditEvent(
                CustomerCreditAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = existing.customerId,
                    entityType = CustomerCreditEntityType.REFUND,
                    entityId = refundId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "REFUND_PROCESSED",
                    previousStatus = existing.status.name,
                    newStatus = CustomerRefundStatus.PROCESSED.name,
                    amount = existing.amount,
                    reason = "Refund marked as processed",
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updateRes
    }

    override suspend fun completeRefund(
        tenantId: String,
        projectId: String,
        refundId: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund> {
        val existingRes = creditRepository.getRefundById(tenantId, projectId, refundId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerCreditValidator.validateRefundTransition(
            existing, CustomerRefundStatus.COMPLETED, null
        )
        if (valRes is DomainResult.Error) return valRes

        // If refund was based on advance, deduct advance available amount
        if (!existing.advanceId.isNullOrBlank()) {
            val advRes = creditRepository.getAdvanceById(tenantId, projectId, existing.advanceId)
            if (advRes is DomainResult.Success) {
                val adv = advRes.data
                val newAvailable = adv.availableAmount.subtract(existing.amount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
                val newAdvStatus = if (newAvailable.compareTo(BigDecimal.ZERO) == 0 && adv.allocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
                    CustomerAdvanceStatus.EXHAUSTED
                } else {
                    adv.status
                }
                creditRepository.updateAdvanceAllocation(
                    tenantId, projectId, adv.advanceId,
                    adv.allocatedAmount, newAvailable, newAdvStatus,
                    actorId, adv.version
                )
            }
        }

        val updateRes = creditRepository.updateRefundStatus(
            tenantId, projectId, refundId,
            CustomerRefundStatus.COMPLETED, null, actorId, expectedVersion
        )
        if (updateRes is DomainResult.Success) {
            creditRepository.recordAuditEvent(
                CustomerCreditAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = existing.customerId,
                    entityType = CustomerCreditEntityType.REFUND,
                    entityId = refundId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "REFUND_COMPLETED",
                    previousStatus = existing.status.name,
                    newStatus = CustomerRefundStatus.COMPLETED.name,
                    amount = existing.amount,
                    reason = "Refund completed successfully",
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updateRes
    }

    override suspend fun cancelRefund(
        tenantId: String,
        projectId: String,
        refundId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund> {
        val existingRes = creditRepository.getRefundById(tenantId, projectId, refundId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerCreditValidator.validateRefundTransition(
            existing, CustomerRefundStatus.CANCELLED, reason
        )
        if (valRes is DomainResult.Error) return valRes

        val updateRes = creditRepository.updateRefundStatus(
            tenantId, projectId, refundId,
            CustomerRefundStatus.CANCELLED, reason, actorId, expectedVersion
        )
        if (updateRes is DomainResult.Success) {
            creditRepository.recordAuditEvent(
                CustomerCreditAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = existing.customerId,
                    entityType = CustomerCreditEntityType.REFUND,
                    entityId = refundId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "REFUND_CANCELLED",
                    previousStatus = existing.status.name,
                    newStatus = CustomerRefundStatus.CANCELLED.name,
                    amount = existing.amount,
                    reason = reason,
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updateRes
    }

    override suspend fun getCustomerCreditSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditSummary> {
        return creditRepository.getCustomerCreditSummary(tenantId, projectId, customerId)
    }

    override suspend fun listAdvances(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerAdvanceStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerAdvance>> {
        return creditRepository.listAdvances(tenantId, projectId, customerId, status, limit, offset)
    }

    override suspend fun listAllocations(
        tenantId: String,
        projectId: String,
        customerId: String?,
        invoiceId: String?,
        advanceId: String?,
        status: CustomerAllocationStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerCreditAllocation>> {
        return creditRepository.listAllocations(tenantId, projectId, customerId, invoiceId, advanceId, status, limit, offset)
    }

    override suspend fun listAdjustments(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerAdjustment>> {
        return creditRepository.listAdjustments(tenantId, projectId, customerId, limit, offset)
    }

    override suspend fun listRefunds(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerRefundStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerRefund>> {
        return creditRepository.listRefunds(tenantId, projectId, customerId, status, limit, offset)
    }

    override suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        entityId: String
    ): DomainResult<List<CustomerCreditAuditEvent>> {
        return creditRepository.getAuditEvents(tenantId, projectId, entityId)
    }
}
