package com.sucharu.sucharupro.domain.service.customersettlement

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditSummary
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocation
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementAuditEvent
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementResult
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementSummary
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerUnallocatedPayment
import com.sucharu.sucharupro.domain.model.customersettlement.InvoiceAllocationRequestItem
import com.sucharu.sucharupro.domain.repository.customercredit.CustomerCreditRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository
import com.sucharu.sucharupro.domain.repository.customerpayment.CustomerPaymentRepository
import com.sucharu.sucharupro.domain.repository.customersettlement.CustomerPaymentAllocationRepository
import com.sucharu.sucharupro.domain.validation.customersettlement.CustomerSettlementValidator
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Production implementation of [CustomerSettlementService] (Module 14 Step 06).
 */
class CustomerSettlementServiceImpl(
    private val allocationRepository: CustomerPaymentAllocationRepository,
    private val paymentRepository: CustomerPaymentRepository,
    private val invoiceRepository: CustomerInvoiceRepository,
    private val accountRepository: CustomerFinancialAccountRepository,
    private val creditRepository: CustomerCreditRepository
) : CustomerSettlementService {

    override suspend fun allocatePayment(
        tenantId: String,
        projectId: String,
        paymentId: String,
        invoiceId: String,
        amount: BigDecimal,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerPaymentAllocation> {
        val normalizedAmount = amount.setScale(4, RoundingMode.HALF_UP)

        // 1. Idempotency Check
        if (!idempotencyKey.isNullOrBlank()) {
            val existingRes = allocationRepository.findByIdempotencyKey(tenantId, projectId, idempotencyKey)
            if (existingRes is DomainResult.Success && existingRes.data != null) {
                val existing = existingRes.data!!
                if (existing.paymentId != paymentId ||
                    existing.invoiceId != invoiceId ||
                    existing.allocatedAmount.compareTo(normalizedAmount) != 0
                ) {
                    return DomainResult.Error(
                        IllegalStateException("Idempotency key '$idempotencyKey' was already used with different allocation parameters.")
                    )
                }
                return DomainResult.Success(existing)
            }
        }

        // 2. Load Payment
        val paymentRes = paymentRepository.getPaymentById(tenantId, projectId, paymentId)
        if (paymentRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Payment '$paymentId' not found: ${paymentRes.message}"))
        }
        val payment = (paymentRes as DomainResult.Success).data

        // 3. Load Invoice
        val invoiceRes = invoiceRepository.getInvoiceById(tenantId, projectId, invoiceId)
        if (invoiceRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Invoice '$invoiceId' not found: ${invoiceRes.message}"))
        }
        val invoice = (invoiceRes as DomainResult.Success).data

        // 4. Load Account
        val accountRes = accountRepository.getAccountById(tenantId, projectId, payment.customerFinancialAccountId)
        if (accountRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Account '${payment.customerFinancialAccountId}' not found: ${accountRes.message}"))
        }
        val account = (accountRes as DomainResult.Success).data

        // 5. Calculate current allocated amount for this payment
        val existingAllocationsRes = allocationRepository.listAllocations(
            tenantId = tenantId,
            projectId = projectId,
            paymentId = paymentId,
            status = CustomerPaymentAllocationStatus.ALLOCATED,
            limit = 1000
        )
        val currentAllocated = if (existingAllocationsRes is DomainResult.Success) {
            existingAllocationsRes.data.map { it.allocatedAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
        } else BigDecimal.ZERO

        // 6. Domain Validation
        val valRes = CustomerSettlementValidator.validateAllocation(
            tenantId = tenantId,
            projectId = projectId,
            payment = payment,
            invoice = invoice,
            account = account,
            amount = normalizedAmount,
            currentPaymentAllocatedAmount = currentAllocated
        )
        if (valRes is DomainResult.Error) return valRes

        // 7. Update Invoice Paid/Due & Status
        val newPaid = invoice.paidAmount.add(normalizedAmount).setScale(4, RoundingMode.HALF_UP)
        val newDue = invoice.dueAmount.subtract(normalizedAmount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val newStatus = if (newDue.compareTo(BigDecimal.ZERO) == 0) {
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
            newStatus = newStatus,
            actorId = actorId,
            expectedVersion = invoice.version
        )
        if (invUpdateRes is DomainResult.Error) {
            return DomainResult.Error(IllegalStateException("Failed to update invoice balance: ${invUpdateRes.message}"))
        }

        // 8. Emit Invoice Audit Event
        invoiceRepository.recordAuditEvent(
            CustomerInvoiceAuditEvent(
                auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                invoiceId = invoice.invoiceId,
                customerId = payment.customerId,
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                action = "PAYMENT_ALLOCATED",
                previousStatus = invoice.status,
                newStatus = newStatus,
                reason = "Allocated $normalizedAmount from payment '$paymentId'",
                occurredAt = System.currentTimeMillis(),
                metadataJson = """{"paymentId":"$paymentId","allocatedAmount":"$normalizedAmount"}"""
            )
        )

        // 9. Create Allocation Record
        val allocationId = "ALC-${UUID.randomUUID().toString().take(8).uppercase()}"
        val now = System.currentTimeMillis()
        val allocation = CustomerPaymentAllocation(
            allocationId = allocationId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = payment.customerId,
            customerFinancialAccountId = payment.customerFinancialAccountId,
            paymentId = paymentId,
            invoiceId = invoiceId,
            allocatedAmount = normalizedAmount,
            currency = payment.currency,
            status = CustomerPaymentAllocationStatus.ALLOCATED,
            idempotencyKey = idempotencyKey,
            allocatedAt = now,
            allocatedBy = actorId,
            version = 1L
        )

        val createRes = allocationRepository.createAllocation(allocation)
        if (createRes is DomainResult.Success) {
            allocationRepository.recordAuditEvent(
                CustomerSettlementAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = payment.customerId,
                    allocationId = allocationId,
                    paymentId = paymentId,
                    invoiceId = invoiceId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "PAYMENT_ALLOCATED",
                    previousStatus = null,
                    newStatus = CustomerPaymentAllocationStatus.ALLOCATED.name,
                    amount = normalizedAmount,
                    reason = "Payment allocated to invoice '$invoiceId'",
                    occurredAt = now
                )
            )
        }
        return createRes
    }

    override suspend fun allocatePaymentMulti(
        tenantId: String,
        projectId: String,
        paymentId: String,
        allocations: List<InvoiceAllocationRequestItem>,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerSettlementResult> {
        // 1. Load Payment
        val paymentRes = paymentRepository.getPaymentById(tenantId, projectId, paymentId)
        if (paymentRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Payment '$paymentId' not found: ${paymentRes.message}"))
        }
        val payment = (paymentRes as DomainResult.Success).data

        // 2. Load Account
        val accountRes = accountRepository.getAccountById(tenantId, projectId, payment.customerFinancialAccountId)
        if (accountRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Account '${payment.customerFinancialAccountId}' not found"))
        }
        val account = (accountRes as DomainResult.Success).data

        // 3. Current allocated amount
        val existingAllocationsRes = allocationRepository.listAllocations(
            tenantId = tenantId,
            projectId = projectId,
            paymentId = paymentId,
            status = CustomerPaymentAllocationStatus.ALLOCATED,
            limit = 1000
        )
        val currentAllocated = if (existingAllocationsRes is DomainResult.Success) {
            existingAllocationsRes.data.map { it.allocatedAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
        } else BigDecimal.ZERO

        // 4. Validate Multi Allocation
        val valRes = CustomerSettlementValidator.validateMultiAllocation(
            tenantId = tenantId,
            projectId = projectId,
            payment = payment,
            account = account,
            items = allocations,
            currentPaymentAllocatedAmount = currentAllocated
        )
        if (valRes is DomainResult.Error) return valRes

        // 5. Pre-validate all target invoices before executing any updates
        val invoicesToUpdate = mutableListOf<Triple<CustomerInvoice, BigDecimal, BigDecimal>>()
        for (item in allocations) {
            val invRes = invoiceRepository.getInvoiceById(tenantId, projectId, item.invoiceId)
            if (invRes is DomainResult.Error) {
                return DomainResult.Error(IllegalArgumentException("Invoice '${item.invoiceId}' not found"))
            }
            val inv = (invRes as DomainResult.Success).data
            if (inv.status !in setOf(CustomerInvoiceStatus.ISSUED, CustomerInvoiceStatus.PARTIALLY_PAID)) {
                return DomainResult.Error(IllegalStateException("Invoice '${inv.invoiceId}' is not eligible (status: ${inv.status})"))
            }
            if (inv.customerId != payment.customerId) {
                return DomainResult.Error(IllegalArgumentException("Invoice '${inv.invoiceId}' customer mismatch"))
            }
            val normAmount = item.amount.setScale(4, RoundingMode.HALF_UP)
            if (normAmount > inv.dueAmount) {
                return DomainResult.Error(IllegalArgumentException("Allocation amount ($normAmount) exceeds invoice '${inv.invoiceId}' due amount (${inv.dueAmount})"))
            }
            invoicesToUpdate.add(Triple(inv, normAmount, inv.dueAmount))
        }

        // 6. Execute atomic allocations
        val createdAllocations = mutableListOf<CustomerPaymentAllocation>()
        var totalAllocatedSoFar = BigDecimal.ZERO

        for ((inv, normAmount, _) in invoicesToUpdate) {
            val newPaid = inv.paidAmount.add(normAmount).setScale(4, RoundingMode.HALF_UP)
            val newDue = inv.dueAmount.subtract(normAmount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
            val newStatus = if (newDue.compareTo(BigDecimal.ZERO) == 0) CustomerInvoiceStatus.PAID else CustomerInvoiceStatus.PARTIALLY_PAID

            val invUpdRes = invoiceRepository.updateInvoicePayment(
                tenantId = tenantId,
                projectId = projectId,
                invoiceId = inv.invoiceId,
                newPaidAmount = newPaid,
                newDueAmount = newDue,
                newStatus = newStatus,
                actorId = actorId,
                expectedVersion = inv.version
            )
            if (invUpdRes is DomainResult.Error) {
                return DomainResult.Error(IllegalStateException("Failed updating invoice '${inv.invoiceId}' balance"))
            }

            val allocationId = "ALC-${UUID.randomUUID().toString().take(8).uppercase()}"
            val now = System.currentTimeMillis()
            val allocation = CustomerPaymentAllocation(
                allocationId = allocationId,
                tenantId = tenantId,
                projectId = projectId,
                customerId = payment.customerId,
                customerFinancialAccountId = payment.customerFinancialAccountId,
                paymentId = paymentId,
                invoiceId = inv.invoiceId,
                allocatedAmount = normAmount,
                currency = payment.currency,
                status = CustomerPaymentAllocationStatus.ALLOCATED,
                allocatedAt = now,
                allocatedBy = actorId,
                version = 1L
            )
            allocationRepository.createAllocation(allocation)
            allocationRepository.recordAuditEvent(
                CustomerSettlementAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = payment.customerId,
                    allocationId = allocationId,
                    paymentId = paymentId,
                    invoiceId = inv.invoiceId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "PAYMENT_ALLOCATED",
                    previousStatus = null,
                    newStatus = CustomerPaymentAllocationStatus.ALLOCATED.name,
                    amount = normAmount,
                    reason = "Multi-allocation of $normAmount to invoice '${inv.invoiceId}'",
                    occurredAt = now
                )
            )

            createdAllocations.add(allocation)
            totalAllocatedSoFar = totalAllocatedSoFar.add(normAmount)
        }

        val remainingUnallocated = payment.amount.subtract(currentAllocated.add(totalAllocatedSoFar)).setScale(4, RoundingMode.HALF_UP)
        return DomainResult.Success(
            CustomerSettlementResult(
                paymentId = paymentId,
                totalAllocated = totalAllocatedSoFar,
                remainingUnallocated = remainingUnallocated,
                allocations = createdAllocations
            )
        )
    }

    override suspend fun reverseAllocation(
        tenantId: String,
        projectId: String,
        allocationId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerPaymentAllocation> {
        // 1. Load Allocation
        val allocRes = allocationRepository.getAllocationById(tenantId, projectId, allocationId)
        if (allocRes is DomainResult.Error) return allocRes
        val allocation = (allocRes as DomainResult.Success).data

        // 2. Validate Reversal
        val valRes = CustomerSettlementValidator.validateReversal(allocation, reason)
        if (valRes is DomainResult.Error) return valRes

        // 3. Load Invoice
        val invRes = invoiceRepository.getInvoiceById(tenantId, projectId, allocation.invoiceId)
        if (invRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Invoice '${allocation.invoiceId}' not found: ${invRes.message}"))
        }
        val invoice = (invRes as DomainResult.Success).data

        // 4. Restore Invoice Balance
        val restoredPaid = invoice.paidAmount.subtract(allocation.allocatedAmount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val restoredDue = invoice.dueAmount.add(allocation.allocatedAmount).min(invoice.grandTotal).setScale(4, RoundingMode.HALF_UP)
        val restoredStatus = if (restoredPaid.compareTo(BigDecimal.ZERO) == 0) {
            CustomerInvoiceStatus.ISSUED
        } else {
            CustomerInvoiceStatus.PARTIALLY_PAID
        }

        val invUpdRes = invoiceRepository.updateInvoicePayment(
            tenantId = tenantId,
            projectId = projectId,
            invoiceId = invoice.invoiceId,
            newPaidAmount = restoredPaid,
            newDueAmount = restoredDue,
            newStatus = restoredStatus,
            actorId = actorId,
            expectedVersion = invoice.version
        )
        if (invUpdRes is DomainResult.Error) {
            return DomainResult.Error(IllegalStateException("Failed restoring invoice balance: ${invUpdRes.message}"))
        }

        // 5. Emit Invoice Audit Event
        invoiceRepository.recordAuditEvent(
            CustomerInvoiceAuditEvent(
                auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                invoiceId = invoice.invoiceId,
                customerId = allocation.customerId,
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                action = "ALLOCATION_REVERSED",
                previousStatus = invoice.status,
                newStatus = restoredStatus,
                reason = "Allocation '$allocationId' reversed: $reason",
                occurredAt = System.currentTimeMillis(),
                metadataJson = """{"allocationId":"$allocationId","restoredAmount":"${allocation.allocatedAmount}"}"""
            )
        )

        // 6. Update Allocation Status to REVERSED
        val updateAllocRes = allocationRepository.updateAllocationStatus(
            tenantId = tenantId,
            projectId = projectId,
            allocationId = allocationId,
            newStatus = CustomerPaymentAllocationStatus.REVERSED,
            reversalReason = reason,
            actorId = actorId,
            expectedVersion = expectedVersion
        )
        if (updateAllocRes is DomainResult.Success) {
            allocationRepository.recordAuditEvent(
                CustomerSettlementAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = allocation.customerId,
                    allocationId = allocationId,
                    paymentId = allocation.paymentId,
                    invoiceId = allocation.invoiceId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "ALLOCATION_REVERSED",
                    previousStatus = CustomerPaymentAllocationStatus.ALLOCATED.name,
                    newStatus = CustomerPaymentAllocationStatus.REVERSED.name,
                    amount = allocation.allocatedAmount,
                    reason = reason,
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updateAllocRes
    }

    override suspend fun getAllocationById(
        tenantId: String,
        projectId: String,
        allocationId: String
    ): DomainResult<CustomerPaymentAllocation> {
        return allocationRepository.getAllocationById(tenantId, projectId, allocationId)
    }

    override suspend fun listAllocations(
        tenantId: String,
        projectId: String,
        paymentId: String?,
        invoiceId: String?,
        customerId: String?,
        status: CustomerPaymentAllocationStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerPaymentAllocation>> {
        return allocationRepository.listAllocations(tenantId, projectId, paymentId, invoiceId, customerId, status, limit, offset)
    }

    override suspend fun getUnallocatedPayments(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<List<CustomerUnallocatedPayment>> {
        val paymentsRes = paymentRepository.listPayments(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            status = CustomerPaymentStatus.CONFIRMED,
            limit = 1000
        )
        if (paymentsRes is DomainResult.Error) return DomainResult.Error(paymentsRes.exception)
        val payments = (paymentsRes as DomainResult.Success).data

        val unallocatedList = mutableListOf<CustomerUnallocatedPayment>()
        for (payment in payments) {
            val allocationsRes = allocationRepository.listAllocations(
                tenantId = tenantId,
                projectId = projectId,
                paymentId = payment.paymentId,
                status = CustomerPaymentAllocationStatus.ALLOCATED,
                limit = 1000
            )
            val allocated = if (allocationsRes is DomainResult.Success) {
                allocationsRes.data.map { it.allocatedAmount }.fold(BigDecimal.ZERO, BigDecimal::add)
            } else BigDecimal.ZERO

            val unallocated = payment.amount.subtract(allocated).setScale(4, RoundingMode.HALF_UP)
            if (unallocated > BigDecimal.ZERO) {
                unallocatedList.add(
                    CustomerUnallocatedPayment(
                        paymentId = payment.paymentId,
                        paymentNumber = payment.paymentNumber,
                        customerId = payment.customerId,
                        customerFinancialAccountId = payment.customerFinancialAccountId,
                        totalAmount = payment.amount,
                        allocatedAmount = allocated,
                        unallocatedAmount = unallocated,
                        currency = payment.currency,
                        paymentDate = payment.paymentDate,
                        status = payment.status.name
                    )
                )
            }
        }
        return DomainResult.Success(unallocatedList)
    }

    override suspend fun getCustomerSettlementSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerSettlementSummary> {
        val invoicesRes = invoiceRepository.listInvoices(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            limit = 5000
        )
        if (invoicesRes is DomainResult.Error) return DomainResult.Error(invoicesRes.exception)
        val invoices = (invoicesRes as DomainResult.Success).data
            .filter { it.status != CustomerInvoiceStatus.VOID && it.status != CustomerInvoiceStatus.CANCELLED }

        val paymentsRes = paymentRepository.listPayments(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            status = CustomerPaymentStatus.CONFIRMED,
            limit = 5000
        )
        if (paymentsRes is DomainResult.Error) return DomainResult.Error(paymentsRes.exception)
        val payments = (paymentsRes as DomainResult.Success).data

        val allocationsRes = allocationRepository.listAllocations(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            status = CustomerPaymentAllocationStatus.ALLOCATED,
            limit = 5000
        )
        val allocations = if (allocationsRes is DomainResult.Success) allocationsRes.data else emptyList()

        val creditSummaryRes = creditRepository.getCustomerCreditSummary(tenantId, projectId, customerId)
        val totalAvailableCredit = if (creditSummaryRes is DomainResult.Success) {
            creditSummaryRes.data.totalAvailableCredit
        } else BigDecimal.ZERO

        val totalInvoiced = invoices.map { it.grandTotal }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)
        val totalPaid = payments.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)
        val totalAllocated = allocations.map { it.allocatedAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)
        val totalUnallocated = totalPaid.subtract(totalAllocated).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val totalOutstanding = invoices.map { it.dueAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)

        val accountId = invoices.firstOrNull()?.customerFinancialAccountId
            ?: payments.firstOrNull()?.customerFinancialAccountId
            ?: ""

        val now = System.currentTimeMillis()
        val summary = CustomerSettlementSummary(
            customerId = customerId,
            projectId = projectId,
            customerFinancialAccountId = accountId,
            totalInvoiced = totalInvoiced,
            totalPaid = totalPaid,
            totalAllocated = totalAllocated,
            totalUnallocated = totalUnallocated,
            totalAvailableCredit = totalAvailableCredit,
            totalOutstanding = totalOutstanding,
            invoiceCount = invoices.size,
            partiallyPaidInvoiceCount = invoices.count { it.status == CustomerInvoiceStatus.PARTIALLY_PAID },
            paidInvoiceCount = invoices.count { it.status == CustomerInvoiceStatus.PAID },
            overdueInvoiceCount = invoices.count { it.dueDate?.let { due -> due < now } == true && it.status != CustomerInvoiceStatus.PAID }
        )
        return DomainResult.Success(summary)
    }

    override suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        allocationId: String?,
        paymentId: String?,
        invoiceId: String?
    ): DomainResult<List<CustomerSettlementAuditEvent>> {
        return allocationRepository.getAuditEvents(tenantId, projectId, allocationId, paymentId, invoiceId)
    }
}
