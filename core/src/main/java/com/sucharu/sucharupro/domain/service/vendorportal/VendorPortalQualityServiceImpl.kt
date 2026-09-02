package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalQualityRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.service.vendor.VendorDeliveryReceiptService
import com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderService
import com.sucharu.sucharupro.domain.service.vendor.VendorQualityService
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalQualityValidator
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Enterprise Production Implementation of VendorPortalQualityService.
 */
class VendorPortalQualityServiceImpl(
    private val qualityRepository: VendorPortalQualityRepository,
    private val canonicalQualityService: VendorQualityService,
    private val vendorPurchaseOrderService: VendorPurchaseOrderService,
    private val vendorDeliveryReceiptService: VendorDeliveryReceiptService,
    private val vendorRepository: VendorRepository
) : VendorPortalQualityService {

    private suspend fun validateVendorActive(projectId: String, vendorId: String): DomainResult<Vendor> {
        val vRes = vendorRepository.findById(projectId, vendorId)
        if (vRes is DomainResult.Error) return vRes
        val vendor = (vRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(NoSuchElementException("Vendor '$vendorId' not found."))
        if (vendor.status != VendorStatus.ACTIVE) {
            return DomainResult.Error(IllegalStateException("Vendor '$vendorId' is not ACTIVE."))
        }
        return DomainResult.Success(vendor)
    }

    override suspend fun listQualityCases(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalQualityCaseStatus?
    ): DomainResult<List<VendorPortalQualityCase>> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        return qualityRepository.listQualityCases(tenantId, projectId, vendorId, status)
    }

    override suspend fun getQualityCaseById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String
    ): DomainResult<VendorPortalQualityCase> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val found = qualityRepository.findQualityCaseById(tenantId, projectId, vendorId, caseId)
        if (found is DomainResult.Error) return found
        val case = (found as DomainResult.Success).data
            ?: return DomainResult.Error(NoSuchElementException("Quality case '$caseId' not found."))

        return DomainResult.Success(case)
    }

    override suspend fun acknowledgeQualityCase(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String,
        actorId: String
    ): DomainResult<VendorPortalQualityCase> {
        val caseRes = getQualityCaseById(tenantId, projectId, vendorId, caseId)
        if (caseRes is DomainResult.Error) return caseRes
        val case = (caseRes as DomainResult.Success).data

        VendorPortalQualityValidator.validateQualityCaseStatusTransition(
            case.status,
            VendorPortalQualityCaseStatus.ACKNOWLEDGED
        )

        val updated = case.copy(
            status = VendorPortalQualityCaseStatus.ACKNOWLEDGED,
            acknowledgedAt = System.currentTimeMillis(),
            acknowledgedBy = actorId,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val saveRes = qualityRepository.saveQualityCase(updated)
        if (saveRes is DomainResult.Success) {
            qualityRepository.recordAudit(
                VendorPortalQualityActivity(
                    activityId = "ACT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    entityType = "QUALITY_CASE",
                    entityId = caseId,
                    action = "CASE_ACKNOWLEDGED",
                    actorId = actorId,
                    details = "Vendor acknowledged quality case ${case.caseNumber}."
                )
            )
        }
        return saveRes
    }

    override suspend fun listInspections(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorInspectionStatus?
    ): DomainResult<List<VendorPortalQualityInspectionSummary>> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val canonicalRes = canonicalQualityService.listInspections(
            projectId = projectId,
            vendorId = vendorId,
            status = status
        )
        if (canonicalRes is DomainResult.Error) return canonicalRes
        val canonicalList = (canonicalRes as DomainResult.Success).data

        val summaries = canonicalList.map { insp ->
            val defectsRes = canonicalQualityService.listDefects(projectId, insp.inspectionId)
            val defects = (defectsRes as? DomainResult.Success)?.data?.map { d ->
                VendorPortalDefectSummary(
                    defectId = d.defectId,
                    defectCode = d.defectType.name,
                    defectCategory = d.defectType.name,
                    severity = d.severity.name,
                    affectedQuantity = d.quantityAffected,
                    description = d.description
                )
            } ?: emptyList()

            val rejectionsRes = canonicalQualityService.listRejections(projectId, vendorId = vendorId, deliveryReceiptId = insp.deliveryReceiptId)
            val rejection = (rejectionsRes as? DomainResult.Success)?.data?.firstOrNull { it.deliveryReceiptId == insp.deliveryReceiptId }
            val disputesRes = canonicalQualityService.listDisputes(projectId, vendorId = vendorId)
            val dispute = (disputesRes as? DomainResult.Success)?.data?.firstOrNull { it.inspectionId == insp.inspectionId }

            VendorPortalQualityInspectionSummary(
                inspectionId = insp.inspectionId,
                inspectionNumber = insp.inspectionReference,
                deliveryReceiptId = insp.deliveryReceiptId ?: "",
                purchaseOrderId = insp.purchaseOrderId ?: "",
                vendorId = insp.vendorId,
                inspectionDate = insp.inspectionStartedAt ?: insp.createdAt,
                status = insp.inspectionStatus.name,
                overallResult = insp.overallResult?.name ?: "PENDING",
                inspectedQuantity = insp.receivedQuantity,
                acceptedQuantity = insp.acceptedQuantity,
                rejectedQuantity = insp.rejectedQuantity,
                conditionalQuantity = insp.conditionalQuantity,
                rejectionId = rejection?.rejectionId,
                rejectionReason = rejection?.rejectionReason,
                disposition = rejection?.disposition?.name,
                replacementRequired = rejection?.replacementRequired ?: false,
                creditRequired = rejection?.creditRequired ?: false,
                correctiveActionRequired = rejection?.resolutionNotes != null,
                disputeId = dispute?.disputeId,
                disputeStatus = dispute?.status?.name,
                items = insp.items.map { item ->
                    VendorPortalQualityItemSummary(
                        inspectionItemId = item.inspectionItemId,
                        purchaseOrderItemId = item.purchaseOrderItemId,
                        itemName = item.itemDescription,
                        inspectedQuantity = item.receivedQuantity,
                        acceptedQuantity = item.acceptedQuantity,
                        rejectedQuantity = item.rejectedQuantity,
                        conditionalQuantity = item.conditionalQuantity,
                        defectCount = item.defectCount,
                        remarks = item.notes
                    )
                },
                defects = defects
            )
        }

        return DomainResult.Success(summaries)
    }

    override suspend fun getInspectionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        inspectionId: String
    ): DomainResult<VendorPortalQualityInspectionSummary> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val inspRes = canonicalQualityService.getInspection(projectId, inspectionId)
        if (inspRes is DomainResult.Error) return inspRes
        val insp = (inspRes as DomainResult.Success).data
            ?: return DomainResult.Error(NoSuchElementException("Inspection '$inspectionId' not found."))

        if (insp.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Access denied: Inspection does not belong to vendor '$vendorId'."))
        }

        val defectsRes = canonicalQualityService.listDefects(projectId, insp.inspectionId)
        val defects = (defectsRes as? DomainResult.Success)?.data?.map { d ->
            VendorPortalDefectSummary(
                defectId = d.defectId,
                defectCode = d.defectType.name,
                defectCategory = d.defectType.name,
                severity = d.severity.name,
                affectedQuantity = d.quantityAffected,
                description = d.description
            )
        } ?: emptyList()

        val rejectionsRes = canonicalQualityService.listRejections(projectId, vendorId = vendorId, deliveryReceiptId = insp.deliveryReceiptId)
        val rejection = (rejectionsRes as? DomainResult.Success)?.data?.firstOrNull { it.deliveryReceiptId == insp.deliveryReceiptId }
        val disputesRes = canonicalQualityService.listDisputes(projectId, vendorId = vendorId)
        val dispute = (disputesRes as? DomainResult.Success)?.data?.firstOrNull { it.inspectionId == insp.inspectionId }

        return DomainResult.Success(
            VendorPortalQualityInspectionSummary(
                inspectionId = insp.inspectionId,
                inspectionNumber = insp.inspectionReference,
                deliveryReceiptId = insp.deliveryReceiptId ?: "",
                purchaseOrderId = insp.purchaseOrderId ?: "",
                vendorId = insp.vendorId,
                inspectionDate = insp.inspectionStartedAt ?: insp.createdAt,
                status = insp.inspectionStatus.name,
                overallResult = insp.overallResult?.name ?: "PENDING",
                inspectedQuantity = insp.receivedQuantity,
                acceptedQuantity = insp.acceptedQuantity,
                rejectedQuantity = insp.rejectedQuantity,
                conditionalQuantity = insp.conditionalQuantity,
                rejectionId = rejection?.rejectionId,
                rejectionReason = rejection?.rejectionReason,
                disposition = rejection?.disposition?.name,
                replacementRequired = rejection?.replacementRequired ?: false,
                creditRequired = rejection?.creditRequired ?: false,
                correctiveActionRequired = rejection?.resolutionNotes != null,
                disputeId = dispute?.disputeId,
                disputeStatus = dispute?.status?.name,
                items = insp.items.map { item ->
                    VendorPortalQualityItemSummary(
                        inspectionItemId = item.inspectionItemId,
                        purchaseOrderItemId = item.purchaseOrderItemId,
                        itemName = item.itemDescription,
                        inspectedQuantity = item.receivedQuantity,
                        acceptedQuantity = item.acceptedQuantity,
                        rejectedQuantity = item.rejectedQuantity,
                        conditionalQuantity = item.conditionalQuantity,
                        defectCount = item.defectCount,
                        remarks = item.notes
                    )
                },
                defects = defects
            )
        )
    }

    override suspend fun listRejections(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorRejectionStatus?
    ): DomainResult<List<VendorPortalRejectionSummary>> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val canonicalRes = canonicalQualityService.listRejections(
            projectId = projectId,
            vendorId = vendorId,
            status = status
        )
        if (canonicalRes is DomainResult.Error) return canonicalRes
        val canonicalList = (canonicalRes as DomainResult.Success).data

        val summaries = canonicalList.map { rej ->
            val poRes = rej.purchaseOrderId?.let { vendorPurchaseOrderService.getOrderById(projectId, it) }
            val orderNumber = (poRes as? DomainResult.Success)?.data?.orderNumber ?: rej.purchaseOrderId

            val drRes = rej.deliveryReceiptId?.let { vendorDeliveryReceiptService.getReceiptById(projectId, it) }
            val receiptNumber = (drRes as? DomainResult.Success)?.data?.receiptNumber ?: rej.deliveryReceiptId

            VendorPortalRejectionSummary(
                rejectionId = rej.rejectionId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                rejectionReference = rej.rejectionReference,
                purchaseOrderId = rej.purchaseOrderId,
                orderNumber = orderNumber,
                deliveryReceiptId = rej.deliveryReceiptId,
                receiptNumber = receiptNumber,
                inspectionId = rej.inspectionId,
                rejectionType = rej.rejectionType,
                rejectionReason = rej.rejectionReason,
                rejectedQuantity = rej.rejectedQuantity,
                rejectedValue = rej.rejectedValue,
                status = rej.status,
                disposition = rej.disposition,
                replacementRequired = rej.replacementRequired,
                returnRequired = rej.returnRequired,
                creditRequired = rej.creditRequired,
                vendorResponse = rej.vendorResponse,
                vendorResponseAt = rej.vendorResponseAt,
                resolutionNotes = rej.resolutionNotes,
                resolvedAt = rej.resolvedAt,
                createdAt = rej.createdAt
            )
        }

        return DomainResult.Success(summaries)
    }

    override suspend fun getRejectionById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        rejectionId: String
    ): DomainResult<VendorPortalRejectionSummary> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val rejRes = canonicalQualityService.getRejection(projectId, rejectionId)
        if (rejRes is DomainResult.Error) return rejRes
        val rej = (rejRes as DomainResult.Success).data
            ?: return DomainResult.Error(NoSuchElementException("Rejection '$rejectionId' not found."))

        if (rej.vendorId != vendorId) {
            return DomainResult.Error(SecurityException("Access denied: Rejection does not belong to vendor '$vendorId'."))
        }

        val poRes = rej.purchaseOrderId?.let { vendorPurchaseOrderService.getOrderById(projectId, it) }
        val orderNumber = (poRes as? DomainResult.Success)?.data?.orderNumber ?: rej.purchaseOrderId

        val drRes = rej.deliveryReceiptId?.let { vendorDeliveryReceiptService.getReceiptById(projectId, it) }
        val receiptNumber = (drRes as? DomainResult.Success)?.data?.receiptNumber ?: rej.deliveryReceiptId

        return DomainResult.Success(
            VendorPortalRejectionSummary(
                rejectionId = rej.rejectionId,
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                rejectionReference = rej.rejectionReference,
                purchaseOrderId = rej.purchaseOrderId,
                orderNumber = orderNumber,
                deliveryReceiptId = rej.deliveryReceiptId,
                receiptNumber = receiptNumber,
                inspectionId = rej.inspectionId,
                rejectionType = rej.rejectionType,
                rejectionReason = rej.rejectionReason,
                rejectedQuantity = rej.rejectedQuantity,
                rejectedValue = rej.rejectedValue,
                status = rej.status,
                disposition = rej.disposition,
                replacementRequired = rej.replacementRequired,
                returnRequired = rej.returnRequired,
                creditRequired = rej.creditRequired,
                vendorResponse = rej.vendorResponse,
                vendorResponseAt = rej.vendorResponseAt,
                resolutionNotes = rej.resolutionNotes,
                resolvedAt = rej.resolvedAt,
                createdAt = rej.createdAt
            )
        )
    }

    override suspend fun respondToQualityCase(
        tenantId: String,
        projectId: String,
        vendorId: String,
        caseId: String,
        comment: String,
        correctiveActionPlan: String?,
        promisedReplacementDate: Long?,
        evidenceReferences: List<String>,
        actorId: String
    ): DomainResult<VendorPortalQualityCase> {
        val caseRes = getQualityCaseById(tenantId, projectId, vendorId, caseId)
        if (caseRes is DomainResult.Error) return caseRes
        val case = (caseRes as DomainResult.Success).data

        VendorPortalQualityValidator.validateQualityCaseStatusTransition(
            case.status,
            VendorPortalQualityCaseStatus.RESPONSE_SUBMITTED
        )

        val updated = case.copy(
            status = VendorPortalQualityCaseStatus.RESPONSE_SUBMITTED,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val saveRes = qualityRepository.saveQualityCase(updated)
        if (saveRes is DomainResult.Success) {
            qualityRepository.recordAudit(
                VendorPortalQualityActivity(
                    activityId = "ACT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    entityType = "QUALITY_CASE",
                    entityId = caseId,
                    action = "RESPONSE_SUBMITTED",
                    actorId = actorId,
                    details = "Vendor submitted quality response: $comment"
                )
            )
        }
        return saveRes
    }

    override suspend fun createCapaPlan(
        tenantId: String,
        projectId: String,
        vendorId: String,
        input: VendorPortalCapaPlanInput,
        actorId: String
    ): DomainResult<VendorPortalCapaPlan> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val capaId = "CAPA-${UUID.randomUUID()}"
        val capaNumber = "CAPA-${System.currentTimeMillis() % 1000000}"

        val plan = VendorPortalCapaPlan(
            capaId = capaId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            caseId = input.caseId,
            inspectionId = input.inspectionId,
            rejectionId = input.rejectionId,
            capaNumber = capaNumber,
            status = VendorPortalCapaStatus.DRAFT,
            priority = input.priority,
            title = input.title,
            rootCause = input.rootCause,
            correctiveAction = input.correctiveAction,
            preventiveAction = input.preventiveAction,
            responsiblePerson = input.responsiblePerson,
            targetCompletionDate = input.targetCompletionDate,
            affectedQuantity = input.affectedQuantity,
            affectedUnit = input.affectedUnit,
            createdBy = actorId,
            updatedBy = actorId
        )

        VendorPortalQualityValidator.validateCapaPlan(plan)

        val saveRes = qualityRepository.saveCapaPlan(plan)
        if (saveRes is DomainResult.Success) {
            qualityRepository.recordAudit(
                VendorPortalQualityActivity(
                    activityId = "ACT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    entityType = "CAPA",
                    entityId = capaId,
                    action = "CAPA_DRAFT_CREATED",
                    actorId = actorId,
                    details = "Draft CAPA created for case ${input.caseId ?: "direct"}."
                )
            )
        }
        return saveRes
    }

    override suspend fun getCapaPlanById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        capaId: String
    ): DomainResult<VendorPortalCapaPlan> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val found = qualityRepository.findCapaPlanById(tenantId, projectId, vendorId, capaId)
        if (found is DomainResult.Error) return found
        val capa = (found as DomainResult.Success).data
            ?: return DomainResult.Error(NoSuchElementException("CAPA plan '$capaId' not found."))

        return DomainResult.Success(capa)
    }

    override suspend fun listCapaPlans(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalCapaStatus?,
        caseId: String?
    ): DomainResult<List<VendorPortalCapaPlan>> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        return qualityRepository.listCapaPlans(tenantId, projectId, vendorId, status, caseId)
    }

    override suspend fun submitCapaPlan(
        tenantId: String,
        projectId: String,
        vendorId: String,
        capaId: String,
        actorId: String
    ): DomainResult<VendorPortalCapaPlan> {
        val capaRes = getCapaPlanById(tenantId, projectId, vendorId, capaId)
        if (capaRes is DomainResult.Error) return capaRes
        val capa = (capaRes as DomainResult.Success).data

        VendorPortalQualityValidator.validateCapaStatusTransition(
            capa.status,
            VendorPortalCapaStatus.SUBMITTED
        )

        val updated = capa.copy(
            status = VendorPortalCapaStatus.SUBMITTED,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val saveRes = qualityRepository.saveCapaPlan(updated)
        if (saveRes is DomainResult.Success) {
            qualityRepository.recordAudit(
                VendorPortalQualityActivity(
                    activityId = "ACT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    entityType = "CAPA",
                    entityId = capaId,
                    action = "CAPA_SUBMITTED",
                    actorId = actorId,
                    details = "Vendor submitted CAPA plan ${capa.capaNumber} for review."
                )
            )
        }
        return saveRes
    }

    override suspend fun completeCapaPlan(
        tenantId: String,
        projectId: String,
        vendorId: String,
        capaId: String,
        actorId: String
    ): DomainResult<VendorPortalCapaPlan> {
        val capaRes = getCapaPlanById(tenantId, projectId, vendorId, capaId)
        if (capaRes is DomainResult.Error) return capaRes
        val capa = (capaRes as DomainResult.Success).data

        VendorPortalQualityValidator.validateCapaStatusTransition(
            capa.status,
            VendorPortalCapaStatus.COMPLETED
        )

        val updated = capa.copy(
            status = VendorPortalCapaStatus.COMPLETED,
            actualCompletionDate = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val saveRes = qualityRepository.saveCapaPlan(updated)
        if (saveRes is DomainResult.Success) {
            qualityRepository.recordAudit(
                VendorPortalQualityActivity(
                    activityId = "ACT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    entityType = "CAPA",
                    entityId = capaId,
                    action = "CAPA_COMPLETED",
                    actorId = actorId,
                    details = "Vendor marked CAPA plan ${capa.capaNumber} as completed."
                )
            )
        }
        return saveRes
    }

    override suspend fun addCapaAction(
        tenantId: String,
        projectId: String,
        vendorId: String,
        capaId: String,
        actionInput: VendorPortalCapaActionInput,
        actorId: String
    ): DomainResult<VendorPortalCapaAction> {
        val capaRes = getCapaPlanById(tenantId, projectId, vendorId, capaId)
        if (capaRes is DomainResult.Error) return capaRes
        val capa = (capaRes as DomainResult.Success).data

        val existingActionsRes = qualityRepository.listCapaActions(tenantId, projectId, capaId)
        val count = (existingActionsRes as? DomainResult.Success)?.data?.size ?: 0

        val action = VendorPortalCapaAction(
            actionId = "ACTION-${UUID.randomUUID()}",
            capaId = capaId,
            tenantId = tenantId,
            projectId = projectId,
            actionNumber = count + 1,
            actionType = actionInput.actionType,
            description = actionInput.description,
            owner = actionInput.owner,
            targetDate = actionInput.targetDate,
            status = VendorPortalCapaActionStatus.OPEN,
            notes = actionInput.notes
        )

        return qualityRepository.saveCapaAction(action)
    }

    override suspend fun completeCapaAction(
        tenantId: String,
        projectId: String,
        vendorId: String,
        capaId: String,
        actionId: String,
        evidenceRefs: List<String>,
        actorId: String
    ): DomainResult<VendorPortalCapaAction> {
        val actionsRes = qualityRepository.listCapaActions(tenantId, projectId, capaId)
        if (actionsRes is DomainResult.Error) return actionsRes
        val actions = (actionsRes as DomainResult.Success).data
        val target = actions.find { it.actionId == actionId }
            ?: return DomainResult.Error(NoSuchElementException("Action '$actionId' not found in CAPA '$capaId'."))

        val updated = target.copy(
            status = VendorPortalCapaActionStatus.COMPLETED,
            completedAt = System.currentTimeMillis(),
            evidenceReferences = evidenceRefs
        )

        return qualityRepository.saveCapaAction(updated)
    }

    override suspend fun createDispute(
        tenantId: String,
        projectId: String,
        vendorId: String,
        input: VendorPortalDisputeInput,
        actorId: String
    ): DomainResult<VendorPortalDisputeSummary> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val disputeId = "DISP-${UUID.randomUUID()}"
        val disputeReference = "DISP-${System.currentTimeMillis() % 1000000}"

        val dispute = VendorPortalDisputeSummary(
            disputeId = disputeId,
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            disputeReference = disputeReference,
            sourceType = input.sourceType,
            sourceId = input.sourceId,
            disputeType = input.disputeType,
            priority = input.priority,
            status = VendorPortalDisputeStatus.OPEN,
            subject = input.subject,
            description = input.description,
            requestedResolution = input.requestedResolution,
            disputedQuantity = input.disputedQuantity,
            disputedAmount = input.disputedAmount,
            raisedBy = actorId
        )

        VendorPortalQualityValidator.validateDisputeSubmission(dispute)

        val saveRes = qualityRepository.saveDisputeSubmission(dispute)
        if (saveRes is DomainResult.Success) {
            qualityRepository.recordAudit(
                VendorPortalQualityActivity(
                    activityId = "ACT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    entityType = "DISPUTE",
                    entityId = disputeId,
                    action = "DISPUTE_RAISED",
                    actorId = actorId,
                    details = "Vendor raised formal dispute ${dispute.disputeReference}: ${input.subject}."
                )
            )
        }
        return saveRes
    }

    override suspend fun getDisputeById(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String
    ): DomainResult<VendorPortalDisputeSummary> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val found = qualityRepository.findDisputeSubmissionById(tenantId, projectId, vendorId, disputeId)
        if (found is DomainResult.Error) return found
        val disp = (found as DomainResult.Success).data
            ?: return DomainResult.Error(NoSuchElementException("Dispute '$disputeId' not found."))

        return DomainResult.Success(disp)
    }

    override suspend fun listDisputes(
        tenantId: String,
        projectId: String,
        vendorId: String,
        status: VendorPortalDisputeStatus?
    ): DomainResult<List<VendorPortalDisputeSummary>> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        return qualityRepository.listDisputeSubmissions(tenantId, projectId, vendorId, status)
    }

    override suspend fun respondToDispute(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String,
        response: String,
        actorId: String
    ): DomainResult<VendorPortalDisputeSummary> {
        val dispRes = getDisputeById(tenantId, projectId, vendorId, disputeId)
        if (dispRes is DomainResult.Error) return dispRes
        val disp = (dispRes as DomainResult.Success).data

        val updated = disp.copy(
            status = VendorPortalDisputeStatus.VENDOR_RESPONDED,
            vendorResponse = response,
            vendorResponseAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val saveRes = qualityRepository.saveDisputeSubmission(updated)
        if (saveRes is DomainResult.Success) {
            qualityRepository.recordAudit(
                VendorPortalQualityActivity(
                    activityId = "ACT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    entityType = "DISPUTE",
                    entityId = disputeId,
                    action = "DISPUTE_RESPONDED",
                    actorId = actorId,
                    details = "Vendor submitted dispute clarification."
                )
            )
        }
        return saveRes
    }

    override suspend fun respondToResolutionProposal(
        tenantId: String,
        projectId: String,
        vendorId: String,
        disputeId: String,
        action: VendorPortalProposalAction,
        rationale: String,
        actorId: String
    ): DomainResult<VendorPortalResolutionResponse> {
        val dispRes = getDisputeById(tenantId, projectId, vendorId, disputeId)
        if (dispRes is DomainResult.Error) return DomainResult.Error((dispRes as DomainResult.Error).exception)
        val disp = (dispRes as DomainResult.Success).data

        val resp = VendorPortalResolutionResponse(
            responseId = "RESP-${UUID.randomUUID()}",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            disputeId = disputeId,
            proposalAction = action,
            rationale = rationale,
            respondedBy = actorId
        )

        VendorPortalQualityValidator.validateResolutionResponse(resp)

        val saveRes = qualityRepository.saveResolutionResponse(resp)
        if (saveRes is DomainResult.Success) {
            val newStatus = when (action) {
                VendorPortalProposalAction.ACCEPT -> VendorPortalDisputeStatus.RESOLUTION_ACCEPTED
                VendorPortalProposalAction.REJECT -> VendorPortalDisputeStatus.RESOLUTION_REJECTED
                VendorPortalProposalAction.REQUEST_CLARIFICATION -> VendorPortalDisputeStatus.VENDOR_RESPONDED
            }
            qualityRepository.saveDisputeSubmission(disp.copy(status = newStatus, updatedAt = System.currentTimeMillis()))

            qualityRepository.recordAudit(
                VendorPortalQualityActivity(
                    activityId = "ACT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    entityType = "DISPUTE",
                    entityId = disputeId,
                    action = "RESOLUTION_PROPOSAL_${action.name}",
                    actorId = actorId,
                    details = "Vendor $action resolution proposal for dispute ${disp.disputeReference}."
                )
            )
        }
        return saveRes
    }

    override suspend fun uploadEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        input: VendorPortalQualityEvidenceInput,
        actorId: String
    ): DomainResult<VendorPortalQualityEvidence> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val evidence = VendorPortalQualityEvidence(
            evidenceId = "EVD-${UUID.randomUUID()}",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            entityType = input.entityType,
            entityId = input.entityId,
            evidenceType = input.evidenceType,
            filename = input.filename,
            fileReference = input.fileReference,
            sizeBytes = input.sizeBytes,
            checksum = input.checksum,
            description = input.description,
            uploadedBy = actorId
        )

        VendorPortalQualityValidator.validateQualityEvidence(evidence)

        val saveRes = qualityRepository.saveEvidence(evidence)
        if (saveRes is DomainResult.Success) {
            qualityRepository.recordAudit(
                VendorPortalQualityActivity(
                    activityId = "ACT-${UUID.randomUUID()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    entityType = input.entityType,
                    entityId = input.entityId,
                    action = "EVIDENCE_UPLOADED",
                    actorId = actorId,
                    details = "Uploaded ${input.filename} (${input.evidenceType.name})."
                )
            )
        }
        return saveRes
    }

    override suspend fun listEvidence(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalQualityEvidence>> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        return qualityRepository.listEvidence(tenantId, projectId, vendorId, entityType, entityId)
    }

    override suspend fun listQualityActivityTimeline(
        tenantId: String,
        projectId: String,
        vendorId: String,
        entityType: String,
        entityId: String
    ): DomainResult<List<VendorPortalQualityActivity>> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        return qualityRepository.listAuditEvents(tenantId, projectId, vendorId, entityType, entityId)
    }

    override suspend fun getQualityKpiSummary(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalQualityKpiSummary> {
        val vCheck = validateVendorActive(projectId, vendorId)
        if (vCheck is DomainResult.Error) return vCheck

        val casesRes = listQualityCases(tenantId, projectId, vendorId)
        val cases = (casesRes as? DomainResult.Success)?.data ?: emptyList()

        val inspRes = listInspections(tenantId, projectId, vendorId)
        val inspections = (inspRes as? DomainResult.Success)?.data ?: emptyList()

        val rejRes = listRejections(tenantId, projectId, vendorId)
        val rejections = (rejRes as? DomainResult.Success)?.data ?: emptyList()

        val capaRes = listCapaPlans(tenantId, projectId, vendorId)
        val capas = (capaRes as? DomainResult.Success)?.data ?: emptyList()

        val dispRes = listDisputes(tenantId, projectId, vendorId)
        val disputes = (dispRes as? DomainResult.Success)?.data ?: emptyList()

        val openCases = cases.count { it.status != VendorPortalQualityCaseStatus.CLOSED && it.status != VendorPortalQualityCaseStatus.RESOLVED }
        val pendingResp = cases.count { it.status == VendorPortalQualityCaseStatus.RESPONSE_REQUIRED }
        val activeCapas = capas.count { it.status in setOf(VendorPortalCapaStatus.SUBMITTED, VendorPortalCapaStatus.UNDER_REVIEW, VendorPortalCapaStatus.APPROVED, VendorPortalCapaStatus.IN_PROGRESS) }
        val overdueCapas = capas.count { it.status == VendorPortalCapaStatus.OVERDUE || (it.status != VendorPortalCapaStatus.COMPLETED && it.status != VendorPortalCapaStatus.CLOSED && it.targetCompletionDate < System.currentTimeMillis()) }
        val openDisputes = disputes.count { it.status != VendorPortalDisputeStatus.CLOSED && it.status != VendorPortalDisputeStatus.RESOLVED }

        var totalAccepted = BigDecimal.ZERO
        var totalRejected = BigDecimal.ZERO

        for (insp in inspections) {
            totalAccepted = totalAccepted.add(insp.acceptedQuantity)
            totalRejected = totalRejected.add(insp.rejectedQuantity)
        }

        val totalEvaluated = totalAccepted.add(totalRejected)
        val passRate = if (totalEvaluated > BigDecimal.ZERO) {
            totalAccepted.multiply(BigDecimal(100)).divide(totalEvaluated, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal("100.00")
        }

        return DomainResult.Success(
            VendorPortalQualityKpiSummary(
                vendorId = vendorId,
                openQualityCases = openCases,
                pendingVendorResponses = pendingResp,
                activeCapaCount = activeCapas,
                overdueCapaCount = overdueCapas,
                openDisputesCount = openDisputes,
                totalInspectionsCount = inspections.size,
                totalRejectionsCount = rejections.size,
                totalRejectedQuantity = totalRejected,
                totalAcceptedQuantity = totalAccepted,
                qualityPassRate = passRate
            )
        )
    }

    override suspend fun getQualityWorkspace(
        tenantId: String,
        projectId: String,
        vendorId: String
    ): DomainResult<VendorPortalQualityWorkspace> {
        val kpiRes = getQualityKpiSummary(tenantId, projectId, vendorId)
        if (kpiRes is DomainResult.Error) return kpiRes
        val kpi = (kpiRes as DomainResult.Success).data

        val cases = (listQualityCases(tenantId, projectId, vendorId) as? DomainResult.Success)?.data?.take(5) ?: emptyList()
        val inspections = (listInspections(tenantId, projectId, vendorId) as? DomainResult.Success)?.data?.take(5) ?: emptyList()
        val rejections = (listRejections(tenantId, projectId, vendorId) as? DomainResult.Success)?.data?.take(5) ?: emptyList()
        val capas = (listCapaPlans(tenantId, projectId, vendorId) as? DomainResult.Success)?.data?.take(5) ?: emptyList()
        val disputes = (listDisputes(tenantId, projectId, vendorId) as? DomainResult.Success)?.data?.take(5) ?: emptyList()

        return DomainResult.Success(
            VendorPortalQualityWorkspace(
                kpiSummary = kpi,
                recentCases = cases,
                recentInspections = inspections,
                recentRejections = rejections,
                activeCapas = capas,
                activeDisputes = disputes
            )
        )
    }
}
