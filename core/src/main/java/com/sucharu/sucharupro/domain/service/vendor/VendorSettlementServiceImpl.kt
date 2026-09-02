package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.*
import com.sucharu.sucharupro.domain.validation.vendor.VendorSettlementValidator
import java.util.UUID

/**
 * Enterprise implementation of VendorSettlementService (Module 12 Step 10).
 */
class VendorSettlementServiceImpl(
    private val settlementRepository: VendorSettlementRepository,
    private val analyticsRepository: VendorAnalyticsRepository,
    private val vendorRepository: VendorRepository,
    private val invoiceRepository: VendorInvoiceRepository,
    private val payableRepository: VendorPayableRepository? = null,
    private val paymentRepository: SupplierPaymentRepository? = null
) : VendorSettlementService {

    override suspend fun evaluateEligibility(
        vendorId: String,
        payableId: String?,
        tenantId: String
    ): DomainResult<SettlementEligibilityResult> {
        val vRes = vendorRepository.findById(tenantId, vendorId)
        val vendor = if (vRes is DomainResult.Success<Vendor>) vRes.data else null
        if (vendor == null) {
            return DomainResult.Success(
                SettlementEligibilityResult(
                    vendorId = vendorId,
                    payableId = payableId,
                    status = SettlementEligibility.INELIGIBLE_VENDOR_SUSPENDED,
                    isEligible = false,
                    reasons = listOf("Vendor '$vendorId' does not exist")
                )
            )
        }
        if (vendor.status == VendorStatus.SUSPENDED || vendor.status == VendorStatus.INACTIVE || vendor.status == VendorStatus.ARCHIVED) {
            return DomainResult.Success(
                SettlementEligibilityResult(
                    vendorId = vendorId,
                    payableId = payableId,
                    status = SettlementEligibility.INELIGIBLE_VENDOR_SUSPENDED,
                    isEligible = false,
                    reasons = listOf("Vendor '$vendorId' is ${vendor.status}")
                )
            )
        }

        val finRes = analyticsRepository.getFinancialSummary(vendorId, tenantId)
        val fin = if (finRes is DomainResult.Success<VendorFinancialSummary>) finRes.data else null

        val gross = fin?.totalApprovedPayable ?: Money.ZERO
        val settled = fin?.totalSettledAmount ?: Money.ZERO
        val outstanding = fin?.totalOutstandingPayable ?: Money.ZERO

        if (outstanding.isZero() || outstanding.isNegative()) {
            return DomainResult.Success(
                SettlementEligibilityResult(
                    vendorId = vendorId,
                    payableId = payableId,
                    status = SettlementEligibility.INELIGIBLE_ZERO_BALANCE,
                    isEligible = false,
                    reasons = listOf("No outstanding payable balance for vendor '$vendorId'"),
                    grossPayable = gross,
                    approvedAmount = gross,
                    previouslySettledAmount = settled,
                    outstandingAmount = outstanding
                )
            )
        }

        return DomainResult.Success(
            SettlementEligibilityResult(
                vendorId = vendorId,
                payableId = payableId,
                status = SettlementEligibility.ELIGIBLE,
                isEligible = true,
                reasons = emptyList(),
                payableReferences = if (payableId != null) listOf(payableId) else emptyList(),
                grossPayable = gross,
                approvedAmount = gross,
                previouslySettledAmount = settled,
                creditsAmount = Money.ZERO,
                outstandingAmount = outstanding
            )
        )
    }

    override suspend fun createSettlement(
        vendorId: String,
        settlementNumber: String,
        totalAmount: Money,
        settlementMethod: SettlementMethod,
        referenceNumber: String?,
        notes: String?,
        allocations: List<VendorSettlementAllocation>,
        tenantId: String,
        projectId: String,
        actorId: String
    ): DomainResult<VendorSettlement> {
        val valRes = VendorSettlementValidator.validateSettlementCreation(
            vendorId = vendorId,
            settlementNumber = settlementNumber,
            totalAmount = totalAmount,
            allocations = allocations
        )
        if (valRes is DomainResult.Error) return valRes

        val settlementId = "VSET-${UUID.randomUUID().toString().take(12).uppercase()}"
        val preparedAllocs = allocations.map {
            it.copy(
                allocationId = if (it.allocationId.isBlank()) "VSA-${UUID.randomUUID().toString().take(8).uppercase()}" else it.allocationId,
                settlementId = settlementId,
                createdBy = actorId
            )
        }

        val settlement = VendorSettlement(
            settlementId = settlementId,
            projectId = projectId,
            tenantId = tenantId,
            vendorId = vendorId,
            settlementNumber = settlementNumber,
            settlementDate = System.currentTimeMillis(),
            currency = "BDT",
            totalAmount = totalAmount,
            status = VendorSettlementStatus.DRAFT,
            settlementMethod = settlementMethod,
            referenceNumber = referenceNumber,
            notes = notes,
            allocations = preparedAllocs,
            createdAt = System.currentTimeMillis(),
            createdBy = actorId,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = 1L
        )

        val createRes = settlementRepository.createSettlement(settlement)
        if (createRes is DomainResult.Success<VendorSettlement>) {
            settlementRepository.appendAuditEvent(
                VendorSettlementAuditEvent(
                    eventId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    settlementId = settlementId,
                    vendorId = vendorId,
                    projectId = projectId,
                    tenantId = tenantId,
                    eventType = VendorSettlementAuditEventType.SETTLEMENT_CREATED,
                    details = "Created settlement '$settlementNumber' for total ${totalAmount.amount.toPlainString()}",
                    actor = actorId
                )
            )
        }
        return createRes
    }

    override suspend fun approveSettlement(
        settlementId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorSettlement> {
        val sRes = settlementRepository.getSettlementById(settlementId, tenantId)
        val settlement = (sRes as? DomainResult.Success<VendorSettlement?>)?.data
            ?: return DomainResult.Error(IllegalArgumentException("Settlement '$settlementId' not found"))

        val sodRes = VendorSettlementValidator.validateSeparationOfDuties(settlement, actorId)
        if (sodRes is DomainResult.Error) return sodRes

        val transRes = VendorSettlementValidator.validateStatusTransition(settlement.status, VendorSettlementStatus.APPROVED)
        if (transRes is DomainResult.Error) return transRes

        val updated = settlement.copy(
            status = VendorSettlementStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = settlement.version + 1
        )

        val updateRes = settlementRepository.updateSettlement(updated)
        if (updateRes is DomainResult.Success<VendorSettlement>) {
            settlementRepository.appendAuditEvent(
                VendorSettlementAuditEvent(
                    eventId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    settlementId = settlementId,
                    vendorId = settlement.vendorId,
                    projectId = settlement.projectId,
                    tenantId = tenantId,
                    eventType = VendorSettlementAuditEventType.SETTLEMENT_APPROVED,
                    details = "Approved settlement '$settlementId' by actor '$actorId'",
                    actor = actorId
                )
            )
        }
        return updateRes
    }

    override suspend fun processSettlement(
        settlementId: String,
        tenantId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorSettlement> {
        val sRes = settlementRepository.getSettlementById(settlementId, tenantId)
        val settlement = (sRes as? DomainResult.Success<VendorSettlement?>)?.data
            ?: return DomainResult.Error(IllegalArgumentException("Settlement '$settlementId' not found"))

        if (settlement.status != VendorSettlementStatus.APPROVED && settlement.status != VendorSettlementStatus.PROCESSING) {
            return DomainResult.Error(IllegalStateException("Settlement '$settlementId' is in status '${settlement.status}' and cannot be processed (must be APPROVED)"))
        }

        var paymentId: String? = settlement.paymentId
        if (paymentRepository != null && paymentId == null && settlement.allocations.isNotEmpty()) {
            val firstAlloc = settlement.allocations.first()
            val payMethod = when (settlement.settlementMethod) {
                SettlementMethod.BANK_TRANSFER -> SupplierPaymentMethod.BANK_TRANSFER
                SettlementMethod.CHEQUE -> SupplierPaymentMethod.CHEQUE
                SettlementMethod.CASH -> SupplierPaymentMethod.CASH
                SettlementMethod.MOBILE_MONEY -> SupplierPaymentMethod.MOBILE_BANKING
                SettlementMethod.ELECTRONIC_FUNDS_TRANSFER -> SupplierPaymentMethod.BANK_TRANSFER
                else -> SupplierPaymentMethod.OTHER
            }
            val payRes = paymentRepository.createPayment(
                projectId = settlement.projectId,
                vendorId = settlement.vendorId,
                payableId = firstAlloc.payableId,
                amount = settlement.totalAmount,
                currency = settlement.currency,
                paymentMethod = payMethod,
                paymentReference = settlement.referenceNumber,
                actorId = actorId,
                callerRole = callerRole
            )
            if (payRes is DomainResult.Success) {
                paymentId = payRes.data.paymentId
            }
        }

        val settled = settlement.copy(
            status = VendorSettlementStatus.SETTLED,
            paymentId = paymentId ?: settlement.paymentId ?: "PMT-${UUID.randomUUID().toString().take(8).uppercase()}",
            settledAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = settlement.version + 1
        )

        val updateRes = settlementRepository.updateSettlement(settled)
        if (updateRes is DomainResult.Success<VendorSettlement>) {
            settlementRepository.appendAuditEvent(
                VendorSettlementAuditEvent(
                    eventId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    settlementId = settlementId,
                    vendorId = settlement.vendorId,
                    projectId = settlement.projectId,
                    tenantId = tenantId,
                    eventType = VendorSettlementAuditEventType.SETTLEMENT_PROCESSED,
                    details = "Processed settlement '$settlementId' with payment reference '${settled.paymentId}'",
                    actor = actorId
                )
            )
        }
        return updateRes
    }

    override suspend fun reconcileSettlement(
        settlementId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorReconciliationResult> {
        val sRes = settlementRepository.getSettlementById(settlementId, tenantId)
        val settlement = (sRes as? DomainResult.Success<VendorSettlement?>)?.data
            ?: return DomainResult.Error(IllegalArgumentException("Settlement '$settlementId' not found"))

        val expected = settlement.totalAmount
        val settledAmount = if (settlement.status == VendorSettlementStatus.SETTLED) settlement.totalAmount else Money.ZERO
        val variance = VendorSettlementCalculator.calculateVariance(expected, settledAmount)

        val status = if (variance.isZero() && settlement.status == VendorSettlementStatus.SETTLED) {
            ReconciliationStatus.MATCHED
        } else {
            ReconciliationStatus.DISCREPANCY_DETECTED
        }

        val reasons = if (status == ReconciliationStatus.MATCHED) {
            listOf("Settlement and Payment match fully")
        } else {
            listOf("Status is ${settlement.status}, variance is ${variance.amount.toPlainString()}")
        }

        val result = VendorReconciliationResult(
            reconciliationId = "REC-${UUID.randomUUID().toString().take(10).uppercase()}",
            vendorId = settlement.vendorId,
            projectId = settlement.projectId,
            tenantId = tenantId,
            settlementId = settlementId,
            payableId = settlement.allocations.firstOrNull()?.payableId,
            paymentId = settlement.paymentId,
            status = status,
            expectedAmount = expected,
            settledAmount = settledAmount,
            paidAmount = settledAmount,
            ledgerAmount = settledAmount,
            variance = variance,
            reasons = reasons,
            reconciledAt = System.currentTimeMillis(),
            reconciledBy = actorId
        )

        settlementRepository.recordReconciliationResult(result)
        settlementRepository.appendAuditEvent(
            VendorSettlementAuditEvent(
                eventId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                settlementId = settlementId,
                vendorId = settlement.vendorId,
                projectId = settlement.projectId,
                tenantId = tenantId,
                eventType = if (status == ReconciliationStatus.MATCHED) VendorSettlementAuditEventType.RECONCILIATION_PERFORMED else VendorSettlementAuditEventType.RECONCILIATION_MISMATCH_DETECTED,
                details = "Reconciliation completed with status '$status', variance: ${variance.amount.toPlainString()}",
                actor = actorId
            )
        )
        return DomainResult.Success(result)
    }

    override suspend fun getSettlementById(settlementId: String, tenantId: String): DomainResult<VendorSettlement?> {
        return settlementRepository.getSettlementById(settlementId, tenantId)
    }

    override suspend fun listSettlements(
        vendorId: String?,
        status: VendorSettlementStatus?,
        projectId: String?,
        tenantId: String
    ): DomainResult<List<VendorSettlement>> {
        return settlementRepository.listSettlements(vendorId, status, projectId, tenantId)
    }

    override suspend fun getFinancialSummary(
        vendorId: String,
        tenantId: String,
        projectId: String?
    ): DomainResult<VendorFinancialSummary> {
        return analyticsRepository.getFinancialSummary(vendorId, tenantId, projectId)
    }

    override suspend fun getOperationalSummary(
        vendorId: String,
        tenantId: String,
        projectId: String?
    ): DomainResult<VendorOperationalSummary> {
        return analyticsRepository.getOperationalSummary(vendorId, tenantId, projectId)
    }

    override suspend fun getQualitySummary(vendorId: String, tenantId: String): DomainResult<VendorQualitySummary> {
        return analyticsRepository.getQualitySummary(vendorId, tenantId)
    }

    override suspend fun getDeliverySummary(vendorId: String, tenantId: String): DomainResult<VendorDeliverySummary> {
        return analyticsRepository.getDeliverySummary(vendorId, tenantId)
    }

    override suspend fun getInvoiceSummary(vendorId: String, tenantId: String): DomainResult<VendorInvoiceSummary> {
        return analyticsRepository.getInvoiceSummary(vendorId, tenantId)
    }

    override suspend fun getPerformanceSummary(vendorId: String, tenantId: String): DomainResult<VendorPerformanceSummary> {
        return analyticsRepository.getPerformanceSummary(vendorId, tenantId)
    }

    override suspend fun getComplianceSummary(vendorId: String, tenantId: String): DomainResult<VendorComplianceSummary> {
        return analyticsRepository.getComplianceSummary(vendorId, tenantId)
    }

    override suspend fun getRiskSummary(vendorId: String, tenantId: String): DomainResult<VendorRiskSummary> {
        return analyticsRepository.getRiskSummary(vendorId, tenantId)
    }

    override suspend fun getVendor360Summary(vendorId: String, tenantId: String): DomainResult<Vendor360Summary> {
        return analyticsRepository.getVendor360Summary(vendorId, tenantId)
    }

    override suspend fun getAnalyticsTrends(
        vendorId: String,
        period: AnalyticsPeriod,
        tenantId: String
    ): DomainResult<List<VendorAnalyticsTrendPoint>> {
        return analyticsRepository.getAnalyticsTrends(vendorId, period, tenantId)
    }

    override suspend fun saveAnalyticsSnapshot(
        vendorId: String,
        projectId: String,
        period: AnalyticsPeriod,
        startDate: Long,
        endDate: Long,
        metricsJson: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorAnalyticsSnapshot> {
        val snapshot = VendorAnalyticsSnapshot(
            snapshotId = "VSNAP-${UUID.randomUUID().toString().take(10).uppercase()}",
            vendorId = vendorId,
            projectId = projectId,
            tenantId = tenantId,
            period = period,
            startDate = startDate,
            endDate = endDate,
            generatedAt = System.currentTimeMillis(),
            generatedBy = actorId,
            calculationVersion = "1.0.0",
            metricsJson = metricsJson
        )
        return settlementRepository.saveAnalyticsSnapshot(snapshot)
    }

    override suspend fun listAnalyticsSnapshots(
        vendorId: String,
        period: AnalyticsPeriod?,
        tenantId: String
    ): DomainResult<List<VendorAnalyticsSnapshot>> {
        return settlementRepository.listAnalyticsSnapshots(vendorId, period, tenantId)
    }

    override suspend fun listAuditEvents(
        settlementId: String?,
        vendorId: String?,
        tenantId: String
    ): DomainResult<List<VendorSettlementAuditEvent>> {
        return settlementRepository.listAuditEvents(settlementId, vendorId, tenantId)
    }
}
