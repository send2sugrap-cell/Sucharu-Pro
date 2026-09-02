package com.sucharu.sucharupro.domain.service.customerpayment

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentAuditEvent
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository
import com.sucharu.sucharupro.domain.repository.customerpayment.CustomerPaymentRepository
import com.sucharu.sucharupro.domain.validation.customerpayment.CustomerPaymentValidator
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Production implementation of [CustomerPaymentService] (Module 14 Step 03).
 */
class CustomerPaymentServiceImpl(
    private val paymentRepository: CustomerPaymentRepository,
    private val invoiceRepository: CustomerInvoiceRepository,
    private val customerRepository: CustomerRepository,
    private val accountRepository: CustomerFinancialAccountRepository
) : CustomerPaymentService {

    override suspend fun recordPayment(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerFinancialAccountId: String,
        invoiceId: String?,
        amount: BigDecimal,
        currency: String,
        paymentMethod: CustomerPaymentMethod,
        paymentDate: Long,
        referenceNumber: String?,
        externalReference: String?,
        notes: String?,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerPayment> {
        val normalizedAmount = amount.setScale(4, RoundingMode.HALF_UP)

        // 1. Check idempotency key if provided
        if (!idempotencyKey.isNullOrBlank()) {
            val existingRes = paymentRepository.findByIdempotencyKey(tenantId, projectId, idempotencyKey)
            if (existingRes is DomainResult.Success && existingRes.data != null) {
                val existing = existingRes.data!!
                // Check if key is used with conflicting parameters
                if (existing.customerId != customerId ||
                    existing.invoiceId != invoiceId ||
                    existing.amount.compareTo(normalizedAmount) != 0
                ) {
                    return DomainResult.Error(
                        IllegalStateException("Idempotency key '$idempotencyKey' was already used with different financial parameters.")
                    )
                }
                return DomainResult.Success(existing)
            }
        }

        // 2. Customer validation
        val customerRes = customerRepository.findCustomerById(customerId)
        if (customerRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Customer '$customerId' not found: ${customerRes.message}"))
        }

        // 3. CustomerFinancialAccount validation
        val accountRes = accountRepository.getAccountById(tenantId, projectId, customerFinancialAccountId)
        if (accountRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("CustomerFinancialAccount '$customerFinancialAccountId' not found"))
        }
        val account = (accountRes as DomainResult.Success).data

        // 4. Invoice validation if invoiceId provided
        var invoice: CustomerInvoice? = null
        if (!invoiceId.isNullOrBlank()) {
            val invoiceRes = invoiceRepository.getInvoiceById(tenantId, projectId, invoiceId)
            if (invoiceRes is DomainResult.Error) {
                return DomainResult.Error(IllegalArgumentException("Invoice '$invoiceId' not found: ${invoiceRes.message}"))
            }
            invoice = (invoiceRes as DomainResult.Success).data
        }

        // 5. Domain validation
        val valRes = CustomerPaymentValidator.validatePaymentRecording(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            financialAccountId = customerFinancialAccountId,
            amount = normalizedAmount,
            currency = currency,
            paymentMethod = paymentMethod,
            referenceNumber = referenceNumber,
            account = account,
            invoice = invoice
        )
        if (valRes is DomainResult.Error) return valRes

        // 6. Update invoice balance and status if linked
        if (invoice != null) {
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
                return DomainResult.Error(
                    IllegalStateException("Failed to update invoice balance: ${invUpdateRes.message}")
                )
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
                    action = "PAYMENT_APPLIED",
                    previousStatus = invoice.status,
                    newStatus = newInvoiceStatus,
                    reason = "Payment of $normalizedAmount applied",
                    occurredAt = System.currentTimeMillis(),
                    metadataJson = """{"paidAmount":"$newPaid","dueAmount":"$newDue","paymentMethod":"${paymentMethod.name}"}"""
                )
            )
        }

        // 7. Create Payment record
        val paymentId = "PAY-${UUID.randomUUID().toString().take(8).uppercase()}"
        val paymentNumber = "PAY-${System.currentTimeMillis().toString().takeLast(6)}-${UUID.randomUUID().toString().take(4).uppercase()}"
        val now = System.currentTimeMillis()

        val payment = CustomerPayment(
            paymentId = paymentId,
            tenantId = tenantId,
            projectId = projectId,
            paymentNumber = paymentNumber,
            customerId = customerId,
            customerFinancialAccountId = customerFinancialAccountId,
            invoiceId = invoiceId,
            amount = normalizedAmount,
            currency = currency.uppercase(),
            paymentMethod = paymentMethod,
            paymentDate = paymentDate,
            referenceNumber = referenceNumber,
            externalReference = externalReference,
            notes = notes,
            status = CustomerPaymentStatus.RECORDED,
            idempotencyKey = idempotencyKey,
            createdAt = now,
            createdBy = actorId,
            updatedAt = now,
            updatedBy = actorId,
            version = 1L
        )

        val createRes = paymentRepository.createPayment(payment)
        if (createRes is DomainResult.Success) {
            paymentRepository.recordAuditEvent(
                CustomerPaymentAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    paymentId = paymentId,
                    customerId = customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "PAYMENT_RECORDED",
                    previousStatus = null,
                    newStatus = CustomerPaymentStatus.RECORDED,
                    reason = "Customer payment recorded",
                    occurredAt = now,
                    metadataJson = """{"amount":"$normalizedAmount","method":"${paymentMethod.name}","invoiceId":"$invoiceId"}"""
                )
            )
        }
        return createRes
    }

    override suspend fun confirmPayment(
        tenantId: String,
        projectId: String,
        paymentId: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerPayment> {
        val existingRes = paymentRepository.getPaymentById(tenantId, projectId, paymentId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerPaymentValidator.validateStatusTransition(
            existing,
            CustomerPaymentStatus.CONFIRMED,
            null
        )
        if (valRes is DomainResult.Error) return valRes

        val updateRes = paymentRepository.updatePaymentStatus(
            tenantId, projectId, paymentId,
            CustomerPaymentStatus.CONFIRMED, null, actorId, expectedVersion
        )
        if (updateRes is DomainResult.Success) {
            paymentRepository.recordAuditEvent(
                CustomerPaymentAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    paymentId = paymentId,
                    customerId = existing.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "PAYMENT_CONFIRMED",
                    previousStatus = existing.status,
                    newStatus = CustomerPaymentStatus.CONFIRMED,
                    reason = "Payment confirmed",
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updateRes
    }

    override suspend fun cancelPayment(
        tenantId: String,
        projectId: String,
        paymentId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerPayment> {
        val existingRes = paymentRepository.getPaymentById(tenantId, projectId, paymentId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerPaymentValidator.validateStatusTransition(
            existing,
            CustomerPaymentStatus.CANCELLED,
            reason
        )
        if (valRes is DomainResult.Error) return valRes

        // If linked to an invoice, reverse invoice balance
        if (!existing.invoiceId.isNullOrBlank()) {
            val invRes = invoiceRepository.getInvoiceById(tenantId, projectId, existing.invoiceId)
            if (invRes is DomainResult.Success) {
                val inv = invRes.data
                val reversedPaid = inv.paidAmount.subtract(existing.amount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
                val reversedDue = inv.dueAmount.add(existing.amount).min(inv.grandTotal).setScale(4, RoundingMode.HALF_UP)
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
                        customerId = existing.customerId,
                        tenantId = tenantId,
                        projectId = projectId,
                        actorId = actorId,
                        actorRole = actorRole,
                        action = "PAYMENT_REVERSED",
                        previousStatus = inv.status,
                        newStatus = reversedStatus,
                        reason = "Payment '$paymentId' cancelled: $reason",
                        occurredAt = System.currentTimeMillis()
                    )
                )
            }
        }

        val updateRes = paymentRepository.updatePaymentStatus(
            tenantId, projectId, paymentId,
            CustomerPaymentStatus.CANCELLED, reason, actorId, expectedVersion
        )
        if (updateRes is DomainResult.Success) {
            paymentRepository.recordAuditEvent(
                CustomerPaymentAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    paymentId = paymentId,
                    customerId = existing.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "PAYMENT_CANCELLED",
                    previousStatus = existing.status,
                    newStatus = CustomerPaymentStatus.CANCELLED,
                    reason = reason,
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updateRes
    }

    override suspend fun getPaymentById(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<CustomerPayment> {
        return paymentRepository.getPaymentById(tenantId, projectId, paymentId)
    }

    override suspend fun getPaymentByNumber(
        tenantId: String,
        paymentNumber: String
    ): DomainResult<CustomerPayment> {
        return paymentRepository.getPaymentByNumber(tenantId, paymentNumber)
    }

    override suspend fun listPayments(
        tenantId: String,
        projectId: String,
        customerId: String?,
        invoiceId: String?,
        status: CustomerPaymentStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerPayment>> {
        return paymentRepository.listPayments(tenantId, projectId, customerId, invoiceId, status, limit, offset)
    }

    override suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<List<CustomerPaymentAuditEvent>> {
        return paymentRepository.getAuditEvents(tenantId, projectId, paymentId)
    }
}
