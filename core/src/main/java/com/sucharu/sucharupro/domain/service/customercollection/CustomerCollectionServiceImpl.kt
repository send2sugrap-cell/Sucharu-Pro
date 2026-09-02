package com.sucharu.sucharupro.domain.service.customercollection

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercollection.*
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditRiskStatus
import com.sucharu.sucharupro.domain.model.customercreditcontrol.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customercollection.CustomerCollectionRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlService
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementService
import com.sucharu.sucharupro.domain.validation.customercollection.CustomerCollectionValidator
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class CustomerCollectionServiceImpl(
    private val collectionRepository: CustomerCollectionRepository,
    private val customerRepository: CustomerRepository,
    private val accountRepository: CustomerFinancialAccountRepository,
    private val invoiceRepository: CustomerInvoiceRepository,
    private val settlementService: CustomerSettlementService,
    private val creditControlService: CustomerCreditControlService
) : CustomerCollectionService {

    private val millisInDay = 86_400_000L

    private suspend fun validateTenantCustomer(tenantId: String, projectId: String, customerId: String): DomainResult<Unit> {
        val custRes = customerRepository.findCustomerById(customerId)
        if (custRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Customer '$customerId' not found."))
        }
        val accountRes = accountRepository.getAccountByCustomerId(tenantId, projectId, customerId)
        if (accountRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Customer '$customerId' does not belong to tenant '$tenantId' / project '$projectId'."))
        }
        return DomainResult.Success(Unit)
    }

    override suspend fun createCollectionAction(
        tenantId: String,
        projectId: String,
        customerId: String,
        invoiceId: String?,
        actionType: CollectionActionType,
        priority: CollectionPriority?,
        scheduledAt: Long,
        nextFollowUpAt: Long?,
        assignedUserId: String?,
        notes: String?,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCollectionAction> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        // Idempotency check
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = collectionRepository.getActionByIdempotencyKey(tenantId, projectId, idempotencyKey)
            if (existing is DomainResult.Success && existing.data != null) {
                return DomainResult.Success(existing.data)
            }
        }

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = if (custRes is DomainResult.Success) custRes.data else null

        val invoice = if (!invoiceId.isNullOrBlank()) {
            val invRes = invoiceRepository.getInvoiceById(tenantId, projectId, invoiceId)
            if (invRes is DomainResult.Success) invRes.data else null
        } else null

        val valRes = CustomerCollectionValidator.validateCreateAction(customer, invoice, scheduledAt, assignedUserId, actorId)
        if (valRes is DomainResult.Error) return DomainResult.Error(valRes.exception, valRes.message)

        // Determine priority if not specified
        val resolvedPriority = if (priority != null) {
            priority
        } else {
            val riskRes = creditControlService.getReceivableRiskSummary(tenantId, projectId, customerId)
            val risk = if (riskRes is DomainResult.Success) riskRes.data else null
            calculateCollectionPriority(
                overdueAmount = risk?.overdueAmount ?: BigDecimal.ZERO,
                maxDaysOverdue = 0,
                riskStatus = risk?.riskStatus ?: CustomerCreditRiskStatus.NORMAL,
                financialHold = risk?.financialHold ?: false
            )
        }

        val now = System.currentTimeMillis()
        val actionId = UUID.randomUUID().toString()
        val action = CustomerCollectionAction(
            actionId = actionId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            invoiceId = invoiceId,
            actionType = actionType,
            priority = resolvedPriority,
            status = CollectionActionStatus.SCHEDULED,
            scheduledAt = scheduledAt,
            nextFollowUpAt = nextFollowUpAt,
            assignedUserId = assignedUserId,
            notes = notes,
            idempotencyKey = idempotencyKey,
            createdAt = now,
            createdBy = actorId,
            updatedAt = now,
            updatedBy = actorId,
            version = 1
        )

        val saveRes = collectionRepository.saveAction(action)
        if (saveRes is DomainResult.Success) {
            val auditEvent = CustomerCollectionAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                actionId = actionId,
                actorId = actorId,
                actorRole = actorRole,
                action = "CREATE_COLLECTION_ACTION",
                newValueJson = "{\"actionType\": \"${action.actionType}\", \"priority\": \"${action.priority}\", \"scheduledAt\": $scheduledAt, \"assignedUserId\": \"$assignedUserId\"}",
                reason = "Created collection action",
                occurredAt = now
            )
            collectionRepository.recordAuditEvent(auditEvent)
        }
        return saveRes
    }

    override suspend fun rescheduleAction(
        tenantId: String,
        projectId: String,
        actionId: String,
        newScheduledAt: Long,
        newNextFollowUpAt: Long?,
        notes: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCollectionAction> {
        val existingRes = collectionRepository.getActionById(tenantId, projectId, actionId)
        if (existingRes is DomainResult.Error) return DomainResult.Error(existingRes.exception, existingRes.message)
        val existing = (existingRes as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Collection action '$actionId' not found."))

        val valRes = CustomerCollectionValidator.validateRescheduleAction(existing, newScheduledAt, actorId)
        if (valRes is DomainResult.Error) return DomainResult.Error(valRes.exception, valRes.message)

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            scheduledAt = newScheduledAt,
            nextFollowUpAt = newNextFollowUpAt ?: existing.nextFollowUpAt,
            notes = notes ?: existing.notes,
            updatedAt = now,
            updatedBy = actorId,
            version = existing.version + 1
        )

        val saveRes = collectionRepository.saveAction(updated)
        if (saveRes is DomainResult.Success) {
            val auditEvent = CustomerCollectionAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                customerId = existing.customerId,
                actionId = actionId,
                actorId = actorId,
                actorRole = actorRole,
                action = "RESCHEDULE_ACTION",
                previousValueJson = "{\"scheduledAt\": ${existing.scheduledAt}}",
                newValueJson = "{\"scheduledAt\": $newScheduledAt}",
                reason = notes ?: "Rescheduled collection action",
                occurredAt = now
            )
            collectionRepository.recordAuditEvent(auditEvent)
        }
        return saveRes
    }

    override suspend fun assignAction(
        tenantId: String,
        projectId: String,
        actionId: String,
        assignedUserId: String?,
        notes: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCollectionAction> {
        val existingRes = collectionRepository.getActionById(tenantId, projectId, actionId)
        if (existingRes is DomainResult.Error) return DomainResult.Error(existingRes.exception, existingRes.message)
        val existing = (existingRes as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Collection action '$actionId' not found."))

        if (existing.status in setOf(CollectionActionStatus.COMPLETED, CollectionActionStatus.CANCELLED)) {
            return DomainResult.Error(IllegalStateException("Cannot reassign a ${existing.status} action."))
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            assignedUserId = assignedUserId,
            notes = notes ?: existing.notes,
            updatedAt = now,
            updatedBy = actorId,
            version = existing.version + 1
        )

        val saveRes = collectionRepository.saveAction(updated)
        if (saveRes is DomainResult.Success) {
            val auditEvent = CustomerCollectionAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                customerId = existing.customerId,
                actionId = actionId,
                actorId = actorId,
                actorRole = actorRole,
                action = "ASSIGN_ACTION",
                previousValueJson = "{\"assignedUserId\": \"${existing.assignedUserId}\"}",
                newValueJson = "{\"assignedUserId\": \"$assignedUserId\"}",
                reason = notes ?: "Reassigned collection action",
                occurredAt = now
            )
            collectionRepository.recordAuditEvent(auditEvent)
        }
        return saveRes
    }

    override suspend fun completeAction(
        tenantId: String,
        projectId: String,
        actionId: String,
        outcome: CollectionOutcomeType,
        outcomeNotes: String?,
        nextFollowUpAt: Long?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCollectionAction> {
        val existingRes = collectionRepository.getActionById(tenantId, projectId, actionId)
        if (existingRes is DomainResult.Error) return DomainResult.Error(existingRes.exception, existingRes.message)
        val existing = (existingRes as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Collection action '$actionId' not found."))

        val valRes = CustomerCollectionValidator.validateCompleteAction(existing, outcome, actorId)
        if (valRes is DomainResult.Error) return DomainResult.Error(valRes.exception, valRes.message)

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = CollectionActionStatus.COMPLETED,
            performedAt = now,
            outcome = outcome,
            outcomeNotes = outcomeNotes,
            nextFollowUpAt = nextFollowUpAt,
            updatedAt = now,
            updatedBy = actorId,
            version = existing.version + 1
        )

        val saveRes = collectionRepository.saveAction(updated)
        if (saveRes is DomainResult.Success) {
            val auditEvent = CustomerCollectionAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                customerId = existing.customerId,
                actionId = actionId,
                actorId = actorId,
                actorRole = actorRole,
                action = "COMPLETE_ACTION",
                previousValueJson = "{\"status\": \"${existing.status}\"}",
                newValueJson = "{\"status\": \"COMPLETED\", \"outcome\": \"$outcome\", \"notes\": \"${outcomeNotes ?: ""}\"}",
                reason = outcomeNotes ?: "Completed collection action",
                occurredAt = now
            )
            collectionRepository.recordAuditEvent(auditEvent)
        }
        return saveRes
    }

    override suspend fun cancelAction(
        tenantId: String,
        projectId: String,
        actionId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCollectionAction> {
        val existingRes = collectionRepository.getActionById(tenantId, projectId, actionId)
        if (existingRes is DomainResult.Error) return DomainResult.Error(existingRes.exception, existingRes.message)
        val existing = (existingRes as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Collection action '$actionId' not found."))

        val valRes = CustomerCollectionValidator.validateCancelAction(existing, reason, actorId)
        if (valRes is DomainResult.Error) return DomainResult.Error(valRes.exception, valRes.message)

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = CollectionActionStatus.CANCELLED,
            cancellationReason = reason,
            updatedAt = now,
            updatedBy = actorId,
            version = existing.version + 1
        )

        val saveRes = collectionRepository.saveAction(updated)
        if (saveRes is DomainResult.Success) {
            val auditEvent = CustomerCollectionAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                customerId = existing.customerId,
                actionId = actionId,
                actorId = actorId,
                actorRole = actorRole,
                action = "CANCEL_ACTION",
                previousValueJson = "{\"status\": \"${existing.status}\"}",
                newValueJson = "{\"status\": \"CANCELLED\", \"cancellationReason\": \"$reason\"}",
                reason = reason,
                occurredAt = now
            )
            collectionRepository.recordAuditEvent(auditEvent)
        }
        return saveRes
    }

    override suspend fun createPaymentPromise(
        tenantId: String,
        projectId: String,
        customerId: String,
        invoiceId: String?,
        actionId: String?,
        promisedAmount: BigDecimal,
        promisedDate: Long,
        notes: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerPaymentPromise> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val custRes = customerRepository.findCustomerById(customerId)
        val customer = if (custRes is DomainResult.Success) custRes.data else null

        val invoice = if (!invoiceId.isNullOrBlank()) {
            val invRes = invoiceRepository.getInvoiceById(tenantId, projectId, invoiceId)
            if (invRes is DomainResult.Success) invRes.data else null
        } else null

        val summaryRes = settlementService.getCustomerSettlementSummary(tenantId, projectId, customerId)
        val totalOutstanding = if (summaryRes is DomainResult.Success) summaryRes.data.totalOutstanding else BigDecimal.ZERO

        val valRes = CustomerCollectionValidator.validatePaymentPromise(
            customer, invoice, promisedAmount, promisedDate, totalOutstanding, actorId
        )
        if (valRes is DomainResult.Error) return DomainResult.Error(valRes.exception, valRes.message)

        val now = System.currentTimeMillis()
        val promiseId = UUID.randomUUID().toString()
        val promise = CustomerPaymentPromise(
            promiseId = promiseId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            invoiceId = invoiceId,
            actionId = actionId,
            promisedAmount = promisedAmount.setScale(4, RoundingMode.HALF_UP),
            promisedDate = promisedDate,
            status = PaymentPromiseStatus.PENDING,
            notes = notes,
            createdAt = now,
            createdBy = actorId,
            updatedAt = now,
            updatedBy = actorId,
            version = 1
        )

        val saveRes = collectionRepository.savePaymentPromise(promise)
        if (saveRes is DomainResult.Success) {
            val auditEvent = CustomerCollectionAuditEvent(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                actionId = actionId,
                actorId = actorId,
                actorRole = actorRole,
                action = "CREATE_PAYMENT_PROMISE",
                newValueJson = "{\"promiseId\": \"$promiseId\", \"promisedAmount\": $promisedAmount, \"promisedDate\": $promisedDate}",
                reason = notes ?: "Recorded customer payment promise",
                occurredAt = now
            )
            collectionRepository.recordAuditEvent(auditEvent)
        }
        return saveRes
    }

    override suspend fun getReceivableDueSchedule(
        tenantId: String,
        projectId: String,
        customerId: String?,
        asOfDate: Long
    ): DomainResult<List<ReceivableDueScheduleItem>> {
        if (!customerId.isNullOrBlank()) {
            val check = validateTenantCustomer(tenantId, projectId, customerId)
            if (check is DomainResult.Error) return check
        }

        val invoicesRes = invoiceRepository.listInvoices(tenantId, projectId, customerId = customerId, limit = 5000)
        if (invoicesRes is DomainResult.Error) return DomainResult.Error(invoicesRes.exception, invoicesRes.message)
        val invoices = (invoicesRes as DomainResult.Success).data
            .filter { it.status in setOf(CustomerInvoiceStatus.ISSUED, CustomerInvoiceStatus.PARTIALLY_PAID) && it.dueAmount > BigDecimal.ZERO }

        val scheduleItems = invoices.map { inv ->
            val due = inv.dueDate ?: asOfDate
            val daysOverdue = if (asOfDate > due) {
                ((asOfDate - due) / millisInDay).toInt()
            } else 0

            val agingBucket = when {
                daysOverdue <= 0 -> ReceivableAgingBucket.CURRENT
                daysOverdue in 1..7 -> ReceivableAgingBucket.DAYS_1_7
                daysOverdue in 8..30 -> ReceivableAgingBucket.DAYS_8_30
                daysOverdue in 31..60 -> ReceivableAgingBucket.DAYS_31_60
                daysOverdue in 61..90 -> ReceivableAgingBucket.DAYS_61_90
                else -> ReceivableAgingBucket.DAYS_90_PLUS
            }

            val priority = when {
                daysOverdue > 60 || inv.dueAmount >= BigDecimal("100000.0000") -> CollectionPriority.CRITICAL
                daysOverdue > 30 || inv.dueAmount >= BigDecimal("50000.0000") -> CollectionPriority.HIGH
                daysOverdue > 0 -> CollectionPriority.NORMAL
                else -> CollectionPriority.LOW
            }

            ReceivableDueScheduleItem(
                invoiceId = inv.invoiceId,
                invoiceNumber = inv.invoiceNumber,
                customerId = inv.customerId,
                dueDate = due,
                dueAmount = inv.dueAmount.setScale(4, RoundingMode.HALF_UP),
                totalAmount = inv.grandTotal.setScale(4, RoundingMode.HALF_UP),
                daysOverdue = daysOverdue,
                agingBucket = agingBucket,
                priority = priority
            )
        }.sortedWith(compareByDescending<ReceivableDueScheduleItem> { it.daysOverdue }.thenByDescending { it.dueAmount })

        return DomainResult.Success(scheduleItems)
    }

    override suspend fun getCollectionQueue(
        tenantId: String,
        projectId: String,
        customerId: String?,
        priority: CollectionPriority?,
        agingBucket: ReceivableAgingBucket?,
        status: CollectionActionStatus?,
        assignedUserId: String?,
        asOfDate: Long,
        limit: Int,
        offset: Int
    ): DomainResult<List<CollectionQueueItem>> {
        val customersFlow = customerRepository.getCustomers()
        val allCustomers = customersFlow.firstOrNull() ?: emptyList()
        val targetCustomers = if (!customerId.isNullOrBlank()) {
            allCustomers.filter { it.customerId == customerId }
        } else allCustomers

        val queueItems = mutableListOf<CollectionQueueItem>()

        for (cust in targetCustomers) {
            // Check account in tenant/project
            val accountRes = accountRepository.getAccountByCustomerId(tenantId, projectId, cust.customerId)
            if (accountRes is DomainResult.Error) continue

            val riskRes = creditControlService.getReceivableRiskSummary(tenantId, projectId, cust.customerId)
            if (riskRes is DomainResult.Error) continue
            val risk = (riskRes as DomainResult.Success).data

            if (risk.currentOutstanding <= BigDecimal.ZERO) continue

            val agingRes = creditControlService.getReceivableAgingReport(tenantId, projectId, cust.customerId, asOfDate)
            val aging = if (agingRes is DomainResult.Success) agingRes.data else null

            val primaryAgingBucket = when {
                (aging?.maxDaysOverdue ?: 0) <= 0 -> ReceivableAgingBucket.CURRENT
                (aging?.maxDaysOverdue ?: 0) in 1..7 -> ReceivableAgingBucket.DAYS_1_7
                (aging?.maxDaysOverdue ?: 0) in 8..30 -> ReceivableAgingBucket.DAYS_8_30
                (aging?.maxDaysOverdue ?: 0) in 31..60 -> ReceivableAgingBucket.DAYS_31_60
                (aging?.maxDaysOverdue ?: 0) in 61..90 -> ReceivableAgingBucket.DAYS_61_90
                else -> ReceivableAgingBucket.DAYS_90_PLUS
            }

            val queuePriority = calculateCollectionPriority(
                overdueAmount = risk.overdueAmount,
                maxDaysOverdue = aging?.maxDaysOverdue ?: 0,
                riskStatus = risk.riskStatus,
                financialHold = risk.financialHold
            )

            // Collection actions
            val actionsRes = collectionRepository.listActions(tenantId, projectId, customerId = cust.customerId, limit = 10)
            val actions = if (actionsRes is DomainResult.Success) actionsRes.data else emptyList()
            val latestAction = actions.firstOrNull()

            // Promises
            val promisesRes = collectionRepository.listPaymentPromises(tenantId, projectId, customerId = cust.customerId, status = PaymentPromiseStatus.PENDING, limit = 10)
            val promises = if (promisesRes is DomainResult.Success) promisesRes.data else emptyList()
            val activePromisesAmount = promises.map { it.promisedAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)

            // Filtering
            if (priority != null && queuePriority != priority) continue
            if (agingBucket != null && primaryAgingBucket != agingBucket) continue
            if (status != null && latestAction?.status != status) continue
            if (assignedUserId != null && latestAction?.assignedUserId != assignedUserId) continue

            val item = CollectionQueueItem(
                customerId = cust.customerId,
                customerCode = cust.customerCode,
                customerDisplayName = cust.displayName,
                totalOutstanding = risk.currentOutstanding,
                overdueAmount = risk.overdueAmount,
                oldestDueInvoiceDate = risk.oldestDueInvoiceDate,
                maxDaysOverdue = aging?.maxDaysOverdue ?: 0,
                agingBucket = primaryAgingBucket,
                creditRiskStatus = risk.riskStatus,
                financialHold = risk.financialHold,
                priority = queuePriority,
                latestActionId = latestAction?.actionId,
                latestActionType = latestAction?.actionType,
                latestActionStatus = latestAction?.status,
                nextFollowUpAt = latestAction?.nextFollowUpAt,
                assignedUserId = latestAction?.assignedUserId,
                activePromiseCount = promises.size,
                activePromisedAmount = activePromisesAmount
            )
            queueItems.add(item)
        }

        val sorted = queueItems.sortedWith(
            compareByDescending<CollectionQueueItem> { it.priority == CollectionPriority.CRITICAL }
                .thenByDescending { it.priority == CollectionPriority.HIGH }
                .thenByDescending { it.overdueAmount }
                .thenByDescending { it.maxDaysOverdue }
        ).drop(offset).take(limit)

        return DomainResult.Success(sorted)
    }

    override suspend fun getCustomerCollectionSummary(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long
    ): DomainResult<CustomerReceivableCollectionSummary> {
        val check = validateTenantCustomer(tenantId, projectId, customerId)
        if (check is DomainResult.Error) return check

        val riskRes = creditControlService.getReceivableRiskSummary(tenantId, projectId, customerId)
        if (riskRes is DomainResult.Error) return DomainResult.Error(riskRes.exception, riskRes.message)
        val risk = (riskRes as DomainResult.Success).data

        val scheduleRes = getReceivableDueSchedule(tenantId, projectId, customerId, asOfDate)
        val schedule = if (scheduleRes is DomainResult.Success) scheduleRes.data else emptyList()

        val dueTodayAmount = schedule.filter {
            it.daysOverdue == 0 && isSameDay(it.dueDate, asOfDate)
        }.map { it.dueAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)

        val upcomingDueAmount = schedule.filter {
            it.dueDate > asOfDate
        }.map { it.dueAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)

        val overdueAmount = schedule.filter {
            it.daysOverdue > 0
        }.map { it.dueAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)

        val criticalOverdueAmount = schedule.filter {
            it.daysOverdue > 60
        }.map { it.dueAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)

        val actionsRes = collectionRepository.listActions(tenantId, projectId, customerId = customerId, limit = 500)
        val actions = if (actionsRes is DomainResult.Success) actionsRes.data else emptyList()
        val pendingCount = actions.count { it.status in setOf(CollectionActionStatus.SCHEDULED, CollectionActionStatus.IN_PROGRESS) }
        val completedCount = actions.count { it.status == CollectionActionStatus.COMPLETED }
        val latestCompleted = actions.filter { it.status == CollectionActionStatus.COMPLETED }.maxByOrNull { it.performedAt ?: 0L }
        val latestPending = actions.filter { it.status in setOf(CollectionActionStatus.SCHEDULED, CollectionActionStatus.IN_PROGRESS) }.minByOrNull { it.scheduledAt }

        val promisesRes = collectionRepository.listPaymentPromises(tenantId, projectId, customerId = customerId, status = PaymentPromiseStatus.PENDING, limit = 100)
        val promises = if (promisesRes is DomainResult.Success) promisesRes.data else emptyList()
        val promisedAmount = promises.map { it.promisedAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)

        val maxDaysOverdue = schedule.maxOfOrNull { it.daysOverdue } ?: 0
        val priority = calculateCollectionPriority(
            overdueAmount = overdueAmount,
            maxDaysOverdue = maxDaysOverdue,
            riskStatus = risk.riskStatus,
            financialHold = risk.financialHold
        )

        val summary = CustomerReceivableCollectionSummary(
            customerId = customerId,
            totalOutstanding = risk.currentOutstanding,
            dueTodayAmount = dueTodayAmount,
            upcomingDueAmount = upcomingDueAmount,
            overdueAmount = overdueAmount,
            criticalOverdueAmount = criticalOverdueAmount,
            overdueInvoiceCount = risk.overdueInvoiceCount,
            pendingActionCount = pendingCount,
            completedActionCount = completedCount,
            activePromiseCount = promises.size,
            activePromisedAmount = promisedAmount,
            creditRiskStatus = risk.riskStatus,
            financialHold = risk.financialHold,
            priority = priority,
            nextFollowUpAt = latestPending?.scheduledAt,
            latestOutcome = latestCompleted?.outcome
        )
        return DomainResult.Success(summary)
    }

    override suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        customerId: String?,
        actionId: String?
    ): DomainResult<List<CustomerCollectionAuditEvent>> {
        return collectionRepository.getAuditEvents(tenantId, projectId, customerId, actionId)
    }

    private fun calculateCollectionPriority(
        overdueAmount: BigDecimal,
        maxDaysOverdue: Int,
        riskStatus: CustomerCreditRiskStatus,
        financialHold: Boolean
    ): CollectionPriority {
        return when {
            financialHold || maxDaysOverdue > 60 || overdueAmount >= BigDecimal("100000.0000") || riskStatus == CustomerCreditRiskStatus.FINANCIAL_HOLD -> CollectionPriority.CRITICAL
            maxDaysOverdue > 30 || overdueAmount >= BigDecimal("50000.0000") || riskStatus in setOf(CustomerCreditRiskStatus.OVER_LIMIT, CustomerCreditRiskStatus.OVERDUE) -> CollectionPriority.HIGH
            maxDaysOverdue > 0 || overdueAmount > BigDecimal.ZERO -> CollectionPriority.NORMAL
            else -> CollectionPriority.LOW
        }
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val d1 = date1 / millisInDay
        val d2 = date2 / millisInDay
        return d1 == d2
    }
}
