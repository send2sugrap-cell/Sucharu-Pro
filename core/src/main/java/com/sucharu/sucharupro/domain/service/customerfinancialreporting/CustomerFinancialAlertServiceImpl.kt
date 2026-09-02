package com.sucharu.sucharupro.domain.service.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditRiskStatus
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.repository.customerfinancialreporting.CustomerFinancialAlertRepository
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository
import com.sucharu.sucharupro.domain.repository.customerpayment.CustomerPaymentRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionService
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlService
import com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardService
import com.sucharu.sucharupro.domain.validation.customerfinancialreporting.CustomerFinancialAlertValidator
import java.math.BigDecimal
import java.util.UUID

class CustomerFinancialAlertServiceImpl(
    private val alertRepository: CustomerFinancialAlertRepository,
    private val customerRepository: CustomerRepository,
    private val accountRepository: CustomerFinancialAccountRepository,
    private val invoiceRepository: CustomerInvoiceRepository,
    private val paymentRepository: CustomerPaymentRepository,
    private val creditControlService: CustomerCreditControlService,
    private val collectionService: CustomerCollectionService,
    private val dashboardService: CustomerFinancialDashboardService,
    private val notificationRepository: NotificationRepository? = null
) : CustomerFinancialAlertService {

    override suspend fun evaluateCustomerFinancialAlerts(
        tenantId: String,
        projectId: String,
        customerId: String,
        actorId: String,
        actorRole: String,
        asOfDate: Long
    ): DomainResult<List<CustomerFinancialAlert>> {
        val custRes = customerRepository.findCustomerById(customerId)
        if (custRes !is DomainResult.Success) {
            return DomainResult.Error(IllegalArgumentException("Customer '$customerId' does not exist."))
        }
        val cust = custRes.data

        val newOrActiveAlerts = mutableListOf<CustomerFinancialAlert>()

        // 1. Evaluate Invoices (Due soon, due today, overdue)
        val invoicesRes = invoiceRepository.listInvoices(tenantId, projectId, customerId, limit = 1000)
        if (invoicesRes is DomainResult.Success) {
            val unpaidInvoices = invoicesRes.data.filter {
                it.status in setOf(CustomerInvoiceStatus.ISSUED, CustomerInvoiceStatus.PARTIALLY_PAID) && it.dueAmount > BigDecimal.ZERO
            }

            for (inv in unpaidInvoices) {
                val dueDate = inv.dueDate ?: continue
                val dueDiffDays = (dueDate - asOfDate) / (1000L * 60 * 60 * 24)
                if (dueDiffDays < 0L) {
                    // Overdue
                    val alert = createOrGetAlert(
                        tenantId = tenantId,
                        projectId = projectId,
                        customerId = customerId,
                        alertType = CustomerFinancialAlertType.INVOICE_OVERDUE,
                        severity = if (dueDiffDays < -30L) CustomerFinancialAlertSeverity.CRITICAL else CustomerFinancialAlertSeverity.HIGH,
                        title = "Invoice Overdue: ${inv.invoiceNumber}",
                        safeMessage = "Invoice #${inv.invoiceNumber} for amount ${inv.dueAmount} ${inv.currency} was due on date $dueDate and is overdue by ${-dueDiffDays} days.",
                        sourceType = "INVOICE",
                        sourceId = inv.invoiceId,
                        dueAt = dueDate,
                        actorId = actorId,
                        actorRole = actorRole
                    )
                    newOrActiveAlerts.add(alert)
                } else if (dueDiffDays == 0L) {
                    // Due Today
                    val alert = createOrGetAlert(
                        tenantId = tenantId,
                        projectId = projectId,
                        customerId = customerId,
                        alertType = CustomerFinancialAlertType.INVOICE_DUE_TODAY,
                        severity = CustomerFinancialAlertSeverity.MEDIUM,
                        title = "Invoice Due Today: ${inv.invoiceNumber}",
                        safeMessage = "Invoice #${inv.invoiceNumber} for amount ${inv.dueAmount} ${inv.currency} is due today.",
                        sourceType = "INVOICE",
                        sourceId = inv.invoiceId,
                        dueAt = dueDate,
                        actorId = actorId,
                        actorRole = actorRole
                    )
                    newOrActiveAlerts.add(alert)
                } else if (dueDiffDays in 1L..3L) {
                    // Due Soon
                    val alert = createOrGetAlert(
                        tenantId = tenantId,
                        projectId = projectId,
                        customerId = customerId,
                        alertType = CustomerFinancialAlertType.INVOICE_DUE_SOON,
                        severity = CustomerFinancialAlertSeverity.LOW,
                        title = "Invoice Due Soon: ${inv.invoiceNumber}",
                        safeMessage = "Invoice #${inv.invoiceNumber} for amount ${inv.dueAmount} ${inv.currency} is due in $dueDiffDays days.",
                        sourceType = "INVOICE",
                        sourceId = inv.invoiceId,
                        dueAt = dueDate,
                        actorId = actorId,
                        actorRole = actorRole
                    )
                    newOrActiveAlerts.add(alert)
                }
            }
        }

        // 2. Evaluate Credit Exposure & Financial Hold (Step 07)
        val creditRes = creditControlService.getReceivableRiskSummary(tenantId, projectId, customerId)
        if (creditRes is DomainResult.Success) {
            val risk = creditRes.data
            if (risk.riskStatus == CustomerCreditRiskStatus.OVER_LIMIT) {
                val alert = createOrGetAlert(
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    alertType = CustomerFinancialAlertType.CREDIT_LIMIT_EXCEEDED,
                    severity = CustomerFinancialAlertSeverity.CRITICAL,
                    title = "Credit Limit Exceeded",
                    safeMessage = "Total outstanding exposure of ${risk.netReceivableExposure} BDT exceeds credit limit of ${risk.creditLimit} BDT.",
                    sourceType = "CREDIT_PROFILE",
                    sourceId = risk.customerId,
                    actorId = actorId,
                    actorRole = actorRole
                )
                newOrActiveAlerts.add(alert)
            } else if (risk.riskStatus == CustomerCreditRiskStatus.LIMIT_REACHED || risk.riskStatus == CustomerCreditRiskStatus.WATCH) {
                val alert = createOrGetAlert(
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    alertType = CustomerFinancialAlertType.CREDIT_LIMIT_APPROACHING,
                    severity = CustomerFinancialAlertSeverity.HIGH,
                    title = "Credit Limit Approaching Threshold",
                    safeMessage = "Credit utilization is near configured threshold for customer '${cust.displayName}'.",
                    sourceType = "CREDIT_PROFILE",
                    sourceId = risk.customerId,
                    actorId = actorId,
                    actorRole = actorRole
                )
                newOrActiveAlerts.add(alert)
            }

            if (risk.financialHold) {
                val alert = createOrGetAlert(
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    alertType = CustomerFinancialAlertType.FINANCIAL_HOLD_ACTIVE,
                    severity = CustomerFinancialAlertSeverity.CRITICAL,
                    title = "Active Financial Hold",
                    safeMessage = "Customer '${cust.displayName}' has an active financial hold placed on the account: ${risk.holdReason ?: "Policy hold"}.",
                    sourceType = "CREDIT_PROFILE",
                    sourceId = risk.customerId,
                    actorId = actorId,
                    actorRole = actorRole
                )
                newOrActiveAlerts.add(alert)
            }
        }

        // 3. Evaluate Collection Summary & Due Schedule (Step 08)
        val collectionSummaryRes = collectionService.getCustomerCollectionSummary(tenantId, projectId, customerId, asOfDate)
        if (collectionSummaryRes is DomainResult.Success) {
            val summary = collectionSummaryRes.data
            if (summary.pendingActionCount > 0 && summary.nextFollowUpAt != null) {
                val followUpDiff = (summary.nextFollowUpAt - asOfDate) / (1000L * 60 * 60 * 24)
                if (followUpDiff < 0L) {
                    val alert = createOrGetAlert(
                        tenantId = tenantId,
                        projectId = projectId,
                        customerId = customerId,
                        alertType = CustomerFinancialAlertType.COLLECTION_ACTION_OVERDUE,
                        severity = CustomerFinancialAlertSeverity.HIGH,
                        title = "Collection Follow-up Overdue",
                        safeMessage = "Collection action follow-up is overdue by ${-followUpDiff} days for customer '${cust.displayName}'.",
                        sourceType = "COLLECTION",
                        sourceId = customerId,
                        dueAt = summary.nextFollowUpAt,
                        actorId = actorId,
                        actorRole = actorRole
                    )
                    newOrActiveAlerts.add(alert)
                } else if (followUpDiff <= 1L) {
                    val alert = createOrGetAlert(
                        tenantId = tenantId,
                        projectId = projectId,
                        customerId = customerId,
                        alertType = CustomerFinancialAlertType.COLLECTION_ACTION_DUE,
                        severity = CustomerFinancialAlertSeverity.MEDIUM,
                        title = "Collection Follow-up Due Soon",
                        safeMessage = "Scheduled collection follow-up is due for customer '${cust.displayName}'.",
                        sourceType = "COLLECTION",
                        sourceId = customerId,
                        dueAt = summary.nextFollowUpAt,
                        actorId = actorId,
                        actorRole = actorRole
                    )
                    newOrActiveAlerts.add(alert)
                }
            }
        }

        return DomainResult.Success(newOrActiveAlerts)
    }

    private suspend fun createOrGetAlert(
        tenantId: String,
        projectId: String,
        customerId: String,
        alertType: CustomerFinancialAlertType,
        severity: CustomerFinancialAlertSeverity,
        title: String,
        safeMessage: String,
        sourceType: String,
        sourceId: String,
        dueAt: Long? = null,
        actorId: String,
        actorRole: String
    ): CustomerFinancialAlert {
        val dedupKey = CustomerFinancialAlert.buildDeduplicationKey(
            tenantId, projectId, customerId, alertType, sourceType, sourceId
        )

        val existingRes = alertRepository.getActiveAlertByDedupKey(tenantId, projectId, dedupKey)
        if (existingRes is DomainResult.Success && existingRes.data != null) {
            return existingRes.data!!
        }

        val alert = CustomerFinancialAlert(
            alertId = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            alertType = alertType,
            severity = severity,
            status = CustomerFinancialAlertStatus.OPEN,
            title = title,
            safeMessage = safeMessage,
            sourceType = sourceType,
            sourceId = sourceId,
            detectedAt = System.currentTimeMillis(),
            dueAt = dueAt,
            deduplicationKey = dedupKey
        )

        alertRepository.saveAlert(alert)
        alertRepository.recordAuditEvent(
            CustomerFinancialAlertAuditEvent(
                tenantId = tenantId,
                projectId = projectId,
                alertId = alert.alertId,
                eventType = CustomerFinancialAlertEventType.ALERT_CREATED,
                actorId = actorId,
                actorRole = actorRole,
                detailsJson = "{\"alertType\":\"${alertType.name}\",\"severity\":\"${severity.name}\",\"sourceId\":\"$sourceId\"}"
            )
        )

        // Dispatch Notification if configured
        if (notificationRepository != null) {
            val role = try { UserRole.valueOf(actorRole) } catch (_: Exception) { UserRole.STAFF }
            val notifPriority = if (severity.isUrgent) NotificationPriority.HIGH else NotificationPriority.NORMAL
            notificationRepository.createNotification(
                projectId = projectId,
                recipientUserId = customerId,
                recipientType = "CUSTOMER",
                notificationType = NotificationType.FINANCIAL_ALERT,
                channel = NotificationChannel.IN_APP,
                priority = notifPriority,
                title = title,
                message = safeMessage,
                referenceType = sourceType,
                referenceId = sourceId,
                idempotencyKey = "NOTIF_${alert.alertId}",
                actorId = actorId,
                callerRole = role
            )
            alertRepository.recordAuditEvent(
                CustomerFinancialAlertAuditEvent(
                    tenantId = tenantId,
                    projectId = projectId,
                    alertId = alert.alertId,
                    eventType = CustomerFinancialAlertEventType.NOTIFICATION_SENT,
                    actorId = actorId,
                    actorRole = actorRole
                )
            )
        }

        return alert
    }

    override suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerFinancialAlertStatus?,
        severity: CustomerFinancialAlertSeverity?,
        alertType: CustomerFinancialAlertType?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerFinancialAlert>> {
        return alertRepository.listAlerts(tenantId, projectId, customerId, status, severity, alertType, limit, offset)
    }

    override suspend fun getAlertSummary(
        tenantId: String,
        projectId: String,
        customerId: String?
    ): DomainResult<CustomerFinancialAlertSummary> {
        val openAlertsRes = alertRepository.listAlerts(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            status = CustomerFinancialAlertStatus.OPEN,
            limit = 1000,
            offset = 0
        )
        val openAlerts = (openAlertsRes as? DomainResult.Success)?.data ?: emptyList()

        val ackCount = alertRepository.countAlerts(tenantId, projectId, customerId, CustomerFinancialAlertStatus.ACKNOWLEDGED)
        val resCount = alertRepository.countAlerts(tenantId, projectId, customerId, CustomerFinancialAlertStatus.RESOLVED)
        val disCount = alertRepository.countAlerts(tenantId, projectId, customerId, CustomerFinancialAlertStatus.DISMISSED)

        val summary = CustomerFinancialAlertSummary(
            totalOpen = openAlerts.size,
            criticalCount = openAlerts.count { it.severity == CustomerFinancialAlertSeverity.CRITICAL },
            highCount = openAlerts.count { it.severity == CustomerFinancialAlertSeverity.HIGH },
            mediumCount = openAlerts.count { it.severity == CustomerFinancialAlertSeverity.MEDIUM },
            lowCount = openAlerts.count { it.severity == CustomerFinancialAlertSeverity.LOW },
            infoCount = openAlerts.count { it.severity == CustomerFinancialAlertSeverity.INFO },
            acknowledgedCount = (ackCount as? DomainResult.Success)?.data ?: 0,
            resolvedCount = (resCount as? DomainResult.Success)?.data ?: 0,
            dismissedCount = (disCount as? DomainResult.Success)?.data ?: 0
        )

        return DomainResult.Success(summary)
    }

    override suspend fun getAlertById(
        tenantId: String,
        projectId: String,
        alertId: String
    ): DomainResult<CustomerFinancialAlert> {
        val alertRes = alertRepository.getAlertById(tenantId, projectId, alertId)
        if (alertRes is DomainResult.Error) return alertRes
        val alert = (alertRes as DomainResult.Success).data
            ?: return DomainResult.Error(IllegalArgumentException("Alert '$alertId' not found."))
        return DomainResult.Success(alert)
    }

    override suspend fun acknowledgeAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialAlert> {
        val alert = when (val res = getAlertById(tenantId, projectId, alertId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val valRes = CustomerFinancialAlertValidator.validateStatusTransition(
            alert.status, CustomerFinancialAlertStatus.ACKNOWLEDGED
        )
        if (valRes is DomainResult.Error) return valRes

        val updated = alert.copy(
            status = CustomerFinancialAlertStatus.ACKNOWLEDGED,
            acknowledgedAt = System.currentTimeMillis(),
            acknowledgedBy = actorId,
            version = alert.version + 1
        )

        val saveRes = alertRepository.saveAlert(updated)
        if (saveRes is DomainResult.Error) return saveRes

        alertRepository.recordAuditEvent(
            CustomerFinancialAlertAuditEvent(
                tenantId = tenantId,
                projectId = projectId,
                alertId = alertId,
                eventType = CustomerFinancialAlertEventType.ALERT_ACKNOWLEDGED,
                actorId = actorId,
                actorRole = actorRole
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun resolveAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialAlert> {
        val alert = when (val res = getAlertById(tenantId, projectId, alertId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val valRes = CustomerFinancialAlertValidator.validateStatusTransition(
            alert.status, CustomerFinancialAlertStatus.RESOLVED
        )
        if (valRes is DomainResult.Error) return valRes

        val updated = alert.copy(
            status = CustomerFinancialAlertStatus.RESOLVED,
            resolvedAt = System.currentTimeMillis(),
            dismissalReason = reason.ifBlank { null },
            version = alert.version + 1
        )

        val saveRes = alertRepository.saveAlert(updated)
        if (saveRes is DomainResult.Error) return saveRes

        alertRepository.recordAuditEvent(
            CustomerFinancialAlertAuditEvent(
                tenantId = tenantId,
                projectId = projectId,
                alertId = alertId,
                eventType = CustomerFinancialAlertEventType.ALERT_RESOLVED,
                actorId = actorId,
                actorRole = actorRole,
                detailsJson = "{\"reason\":\"$reason\"}"
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun dismissAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialAlert> {
        val alert = when (val res = getAlertById(tenantId, projectId, alertId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val valRes = CustomerFinancialAlertValidator.validateDismissal(alert, reason)
        if (valRes is DomainResult.Error) return valRes

        val updated = alert.copy(
            status = CustomerFinancialAlertStatus.DISMISSED,
            dismissedAt = System.currentTimeMillis(),
            dismissedBy = actorId,
            dismissalReason = reason,
            version = alert.version + 1
        )

        val saveRes = alertRepository.saveAlert(updated)
        if (saveRes is DomainResult.Error) return saveRes

        alertRepository.recordAuditEvent(
            CustomerFinancialAlertAuditEvent(
                tenantId = tenantId,
                projectId = projectId,
                alertId = alertId,
                eventType = CustomerFinancialAlertEventType.ALERT_DISMISSED,
                actorId = actorId,
                actorRole = actorRole,
                detailsJson = "{\"reason\":\"$reason\"}"
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun getAlertAuditHistory(
        tenantId: String,
        projectId: String,
        alertId: String
    ): DomainResult<List<CustomerFinancialAlertAuditEvent>> {
        return alertRepository.listAuditEvents(tenantId, projectId, alertId)
    }
}
