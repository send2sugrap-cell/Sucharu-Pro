package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.*
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalDashboardCalculator

/**
 * Production implementation of [VendorPortalDashboardRepository] (Module 13 Step 02).
 * Aggregates read-only projections from canonical Module 12 domains and Module 13 Step 01 security context.
 */
class VendorPortalDashboardRepositoryImpl(
    private val vendorRepository: VendorRepository,
    private val capabilityRepository: VendorCapabilityRepository? = null,
    private val rateRepository: VendorServiceRateRepository? = null,
    private val workOrderRepository: VendorWorkOrderRepository? = null,
    private val purchaseOrderRepository: VendorPurchaseOrderRepository? = null,
    private val deliveryReceiptRepository: VendorDeliveryReceiptRepository? = null,
    private val invoiceRepository: VendorInvoiceRepository? = null,
    private val qualityRepository: VendorQualityRepository? = null,
    private val performanceRepository: VendorPerformanceRepository? = null,
    private val settlementRepository: VendorSettlementRepository? = null,
    private val portalRepository: VendorPortalRepository? = null
) : VendorPortalDashboardRepository {

    private suspend fun resolveEffectiveProjectId(vendorId: String, tenantId: String, projectId: String): String {
        if (projectId.isNotBlank() && projectId != "*") return projectId
        val accRes = portalRepository?.getAccountByVendorId(vendorId, tenantId)
        val account = (accRes as? DomainResult.Success)?.data
        return account?.projectId ?: tenantId
    }

    override suspend fun getProfileSummary(
        vendorId: String,
        tenantId: String,
        projectId: String
    ): DomainResult<VendorPortalProfileSummary> {
        return try {
            val effectiveProjectId = resolveEffectiveProjectId(vendorId, tenantId, projectId)
            val vRes = vendorRepository.findById(effectiveProjectId, vendorId)
            val vendor = if (vRes is DomainResult.Success) vRes.data else null
                ?: return DomainResult.Error(NoSuchElementException("Vendor '$vendorId' not found"))

            val accRes = portalRepository?.getAccountByVendorId(vendorId, tenantId)
            val account = (accRes as? DomainResult.Success)?.data

            val capRes = capabilityRepository?.listByVendor(effectiveProjectId, vendorId)
            val capabilities = (capRes as? DomainResult.Success)?.data ?: emptyList()

            val ratesRes = rateRepository?.listByVendor(vendorId, tenantId)
            val rates = (ratesRes as? DomainResult.Success)?.data ?: emptyList()
            val activeRates = rates.count { it.status == RateStatus.ACTIVE }

            val profileSummary = VendorPortalProfileSummary(
                vendorId = vendor.vendorId,
                vendorCode = vendor.vendorCode,
                vendorName = vendor.vendorName,
                legalName = vendor.legalName,
                vendorType = vendor.vendorType.name,
                category = vendor.vendorCategory.name,
                status = vendor.status.name,
                primaryContactName = vendor.primaryContactName,
                primaryContactEmail = vendor.primaryEmail,
                primaryContactPhone = vendor.primaryPhone,
                address = vendor.notes,
                portalAccountStatus = account?.status?.name ?: VendorPortalAccountStatus.INVITED.name,
                portalRole = "VENDOR_USER",
                projectScope = vendor.projectId,
                serviceCount = capabilities.map { it.capabilityType }.distinct().size,
                capabilityCount = capabilities.size,
                activeRatesCount = activeRates
            )
            DomainResult.Success(profileSummary)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getOperationalSummary(
        vendorId: String,
        tenantId: String,
        projectId: String
    ): DomainResult<VendorPortalOperationalSummary> {
        return try {
            val effectiveProjectId = resolveEffectiveProjectId(vendorId, tenantId, projectId)
            val poRes = purchaseOrderRepository?.list(projectId = effectiveProjectId, vendorId = vendorId)
            val pos = (poRes as? DomainResult.Success)?.data ?: emptyList()
            val totalPos = pos.size
            val activePos = pos.count { it.status in setOf(VendorPurchaseOrderStatus.APPROVED, VendorPurchaseOrderStatus.ISSUED, VendorPurchaseOrderStatus.ACKNOWLEDGED, VendorPurchaseOrderStatus.PARTIALLY_FULFILLED) }
            val completedPos = pos.count { it.status in setOf(VendorPurchaseOrderStatus.FULFILLED, VendorPurchaseOrderStatus.CLOSED) }

            val woRes = workOrderRepository?.list(projectId = effectiveProjectId, vendorId = vendorId)
            val wos = (woRes as? DomainResult.Success)?.data ?: emptyList()
            val totalWos = wos.size
            val openWos = wos.count { it.status in setOf(VendorWorkOrderStatus.ASSIGNED, VendorWorkOrderStatus.RELEASED, VendorWorkOrderStatus.IN_PROGRESS) }
            val completedWos = wos.count { it.status == VendorWorkOrderStatus.COMPLETED }

            val drRes = deliveryReceiptRepository?.list(projectId = effectiveProjectId, vendorId = vendorId)
            val deliveries = (drRes as? DomainResult.Success)?.data ?: emptyList()
            val totalDeliveries = deliveries.size
            val pendingDeliveries = deliveries.count { it.status in setOf(VendorDeliveryReceiptStatus.DRAFT, VendorDeliveryReceiptStatus.RECEIVING, VendorDeliveryReceiptStatus.RECEIVED, VendorDeliveryReceiptStatus.INSPECTED) }
            val acceptedDeliveries = deliveries.count { it.status in setOf(VendorDeliveryReceiptStatus.ACCEPTED, VendorDeliveryReceiptStatus.PARTIALLY_ACCEPTED) }

            val onTimeRate = VendorPortalDashboardCalculator.calculateOnTimeDeliveryRate(acceptedDeliveries, totalDeliveries)
            val poFulfillmentRate = VendorPortalDashboardCalculator.calculatePoFulfillmentRate(completedPos, totalPos)

            val summary = VendorPortalOperationalSummary(
                totalPurchaseOrders = totalPos,
                activePurchaseOrders = activePos,
                completedPurchaseOrders = completedPos,
                totalWorkOrders = totalWos,
                openWorkOrders = openWos,
                completedWorkOrders = completedWos,
                totalDeliveries = totalDeliveries,
                pendingDeliveries = pendingDeliveries,
                acceptedDeliveries = acceptedDeliveries,
                onTimeDeliveryRatePercent = onTimeRate,
                poFulfillmentRatePercent = poFulfillmentRate
            )
            DomainResult.Success(summary)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getFinancialSummary(
        vendorId: String,
        tenantId: String,
        projectId: String
    ): DomainResult<VendorPortalFinancialSummary> {
        return try {
            val effectiveProjectId = resolveEffectiveProjectId(vendorId, tenantId, projectId)
            val invRes = invoiceRepository?.list(projectId = effectiveProjectId, vendorId = vendorId)
            val invoices = (invRes as? DomainResult.Success)?.data ?: emptyList()
            val totalInvoices = invoices.size
            val pendingInvoices = invoices.count { it.status in setOf(VendorInvoiceStatus.DRAFT, VendorInvoiceStatus.SUBMITTED, VendorInvoiceStatus.UNDER_REVIEW) }
            val approvedInvoices = invoices.count { it.status in setOf(VendorInvoiceStatus.MATCHED, VendorInvoiceStatus.APPROVED) }
            val paidInvoices = invoices.count { it.status == VendorInvoiceStatus.POSTED }
            val disputedInvoices = invoices.count { it.status == VendorInvoiceStatus.REJECTED }

            var totalInvoiced = Money.ZERO
            var totalPaid = Money.ZERO
            for (inv in invoices) {
                totalInvoiced = totalInvoiced + inv.totalAmount
                if (inv.status == VendorInvoiceStatus.POSTED) {
                    totalPaid = totalPaid + inv.totalAmount
                }
            }
            val outstanding = VendorPortalDashboardCalculator.calculateOutstandingPayables(totalInvoiced, totalPaid)

            val setRes = settlementRepository?.listSettlements(vendorId = vendorId, status = null, projectId = effectiveProjectId, tenantId = tenantId)
            val settlements = (setRes as? DomainResult.Success)?.data ?: emptyList()
            val totalSettlements = settlements.size
            val pendingSettlements = settlements.count { it.status in setOf(VendorSettlementStatus.DRAFT, VendorSettlementStatus.ELIGIBLE, VendorSettlementStatus.PROCESSING) }
            val lastSettlementDate = settlements.maxByOrNull { it.createdAt }?.createdAt

            val summary = VendorPortalFinancialSummary(
                totalInvoices = totalInvoices,
                pendingInvoices = pendingInvoices,
                approvedInvoices = approvedInvoices,
                paidInvoices = paidInvoices,
                disputedInvoices = disputedInvoices,
                totalInvoicedAmount = totalInvoiced,
                totalPaidAmount = totalPaid,
                totalOutstandingPayables = outstanding,
                totalSettlements = totalSettlements,
                pendingSettlementsCount = pendingSettlements,
                lastSettlementDate = lastSettlementDate
            )
            DomainResult.Success(summary)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getQualitySummary(
        vendorId: String,
        tenantId: String,
        projectId: String
    ): DomainResult<VendorPortalQualitySummary> {
        return try {
            val effectiveProjectId = resolveEffectiveProjectId(vendorId, tenantId, projectId)
            val insRes = qualityRepository?.listInspections(projectId = effectiveProjectId, vendorId = vendorId)
            val inspections = (insRes as? DomainResult.Success)?.data ?: emptyList()
            val totalInspections = inspections.size
            val passedInspections = inspections.count { it.overallResult == InspectionResult.ACCEPTED }
            val failedInspections = inspections.count { it.overallResult == InspectionResult.REJECTED }

            var totalInspectedUnits = 0
            var totalDefectiveUnits = 0
            for (ins in inspections) {
                totalInspectedUnits += ins.receivedQuantity.toInt()
                totalDefectiveUnits += ins.rejectedQuantity.toInt()
            }
            val defectRate = VendorPortalDashboardCalculator.calculateDefectRate(totalDefectiveUnits, totalInspectedUnits)
            val rating = VendorPortalDashboardCalculator.resolveQualityRating(defectRate)

            val rejRes = qualityRepository?.listRejections(projectId = effectiveProjectId, vendorId = vendorId)
            val rejections = (rejRes as? DomainResult.Success)?.data ?: emptyList()
            val totalRejections = rejections.size
            val openRejections = rejections.count { it.status in setOf(VendorRejectionStatus.DRAFT, VendorRejectionStatus.PENDING_VENDOR_RESPONSE) }
            val openDisputes = rejections.count { it.status == VendorRejectionStatus.DISPUTED }

            val summary = VendorPortalQualitySummary(
                totalInspections = totalInspections,
                passedInspections = passedInspections,
                failedInspections = failedInspections,
                overallDefectRatePercent = defectRate,
                totalRejections = totalRejections,
                openRejections = openRejections,
                openDisputes = openDisputes,
                qualityRating = rating
            )
            DomainResult.Success(summary)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getPerformanceSummary(
        vendorId: String,
        tenantId: String,
        projectId: String
    ): DomainResult<VendorPortalPerformanceSummary> {
        return try {
            val effectiveProjectId = resolveEffectiveProjectId(vendorId, tenantId, projectId)
            val evalRes = performanceRepository?.listEvaluations(projectId = effectiveProjectId, vendorId = vendorId)
            val evaluations = (evalRes as? DomainResult.Success)?.data ?: emptyList()
            val latestEval: VendorEvaluation? = evaluations.maxByOrNull { it.createdAt.toEpochMilli() }

            val summary = if (latestEval != null) {
                VendorPortalPerformanceSummary(
                    overallScore = latestEval.evaluationScore,
                    qualityScore = latestEval.evaluationScore,
                    deliveryScore = latestEval.evaluationScore,
                    pricingScore = latestEval.evaluationScore,
                    serviceScore = latestEval.evaluationScore,
                    tier = latestEval.rating.name,
                    evaluationPeriod = latestEval.periodType.name,
                    lastEvaluatedAt = latestEval.createdAt.toEpochMilli()
                )
            } else {
                VendorPortalPerformanceSummary()
            }
            DomainResult.Success(summary)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getComplianceSummary(
        vendorId: String,
        tenantId: String,
        projectId: String
    ): DomainResult<VendorPortalComplianceSummary> {
        return try {
            val effectiveProjectId = resolveEffectiveProjectId(vendorId, tenantId, projectId)
            val compRes = performanceRepository?.listComplianceRecords(projectId = effectiveProjectId, vendorId = vendorId)
            val records = (compRes as? DomainResult.Success)?.data ?: emptyList()

            val active = records.count { it.status in setOf(ComplianceStatus.VERIFIED, ComplianceStatus.EXPIRING_SOON) }
            val expiring = records.count { it.status == ComplianceStatus.EXPIRING_SOON }
            val expired = records.count { it.status == ComplianceStatus.EXPIRED }
            val riskLevel = when {
                expired > 0 -> "HIGH"
                records.isEmpty() -> "MEDIUM"
                else -> "LOW"
            }

            val summary = VendorPortalComplianceSummary(
                complianceRiskLevel = riskLevel,
                activeCertificationsCount = active,
                expiringCertificationsCount = expiring,
                expiredCertificationsCount = expired,
                taxComplianceStatus = "VERIFIED",
                tradeLicenseStatus = "ACTIVE"
            )
            DomainResult.Success(summary)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getRecentActivities(
        vendorId: String,
        tenantId: String,
        limit: Int
    ): DomainResult<List<VendorPortalActivitySummary>> {
        return try {
            val auditRes = portalRepository?.listAuditEvents(vendorId = vendorId, actorUserId = null, tenantId = tenantId)
            val audits = (auditRes as? DomainResult.Success)?.data ?: emptyList()

            val activities = audits.take(limit).map { audit ->
                VendorPortalActivitySummary(
                    activityId = audit.eventId,
                    eventType = audit.eventType.name,
                    title = audit.action.replace("_", " "),
                    description = audit.details,
                    referenceId = audit.targetId,
                    timestamp = audit.timestamp,
                    actor = audit.actorUserId,
                    category = "SECURITY"
                )
            }
            DomainResult.Success(activities)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }
}
