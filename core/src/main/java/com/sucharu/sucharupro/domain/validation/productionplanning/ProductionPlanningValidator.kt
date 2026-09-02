package com.sucharu.sucharupro.domain.validation.productionplanning

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommitmentStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.productionplanning.*
import java.math.BigDecimal
import java.util.UUID

object ProductionPlanningValidator {

    fun validateOrderAndItem(
        tenantId: String,
        order: Order,
        item: OrderItem,
        commitment: CommercialCommitment?
    ): List<PlanningDiagnostic> {
        val diagnostics = mutableListOf<PlanningDiagnostic>()
        val planningDummyId = "PLAN-EVAL"

        // 1. Order Status check
        if (order.status == OrderStatusType.CANCELLED) {
            diagnostics.add(
                PlanningDiagnostic(
                    diagnosticId = UUID.randomUUID().toString(),
                    planningId = planningDummyId,
                    code = "ORDER_CANCELLED",
                    severity = DiagnosticSeverity.CRITICAL_BLOCKING,
                    category = "COMMERCIAL",
                    message = "The order '${order.orderNumber}' is CANCELLED and cannot enter production planning.",
                    isBlocking = true,
                    recommendedAction = "Verify order lifecycle state with sales management."
                )
            )
        }

        // 2. Quantity check
        if (item.quantity <= 0) {
            diagnostics.add(
                PlanningDiagnostic(
                    diagnosticId = UUID.randomUUID().toString(),
                    planningId = planningDummyId,
                    code = "INVALID_ORDER_QUANTITY",
                    severity = DiagnosticSeverity.CRITICAL_BLOCKING,
                    category = "SPECIFICATION",
                    message = "Order item '${item.itemId}' has invalid quantity ${item.quantity}.",
                    isBlocking = true,
                    recommendedAction = "Correct the ordered item quantity in canonical order."
                )
            )
        }

        // 3. Commercial Commitment check
        if (commitment == null) {
            diagnostics.add(
                PlanningDiagnostic(
                    diagnosticId = UUID.randomUUID().toString(),
                    planningId = planningDummyId,
                    code = "COMMERCIAL_COMMITMENT_MISSING",
                    severity = DiagnosticSeverity.WARNING,
                    category = "COMMERCIAL",
                    message = "Order was created without an explicit commercial commitment snapshot (direct order).",
                    isBlocking = false,
                    recommendedAction = "Verify pricing and customer terms manually before production handoff."
                )
            )
        } else {
            if (commitment.status == CommitmentStatus.CANCELLED || commitment.status == CommitmentStatus.EXPIRED || commitment.status == CommitmentStatus.BLOCKED) {
                diagnostics.add(
                    PlanningDiagnostic(
                        diagnosticId = UUID.randomUUID().toString(),
                        planningId = planningDummyId,
                        code = "COMMERCIAL_COMMITMENT_INVALID_STATE",
                        severity = DiagnosticSeverity.CRITICAL_BLOCKING,
                        category = "COMMERCIAL",
                        message = "The commercial commitment '${commitment.commitmentId}' is in state ${commitment.status}.",
                        isBlocking = true,
                        recommendedAction = "Re-evaluate commercial terms with customer."
                    )
                )
            }
        }

        return diagnostics
    }

    fun validateJobSpecification(
        spec: ProductionJobSpecification
    ): List<PlanningDiagnostic> {
        val diagnostics = mutableListOf<PlanningDiagnostic>()
        val planningDummyId = "PLAN-SPEC-EVAL"

        // 1. Dimensions check
        if (spec.finishedWidthMm <= BigDecimal.ZERO || spec.finishedHeightMm <= BigDecimal.ZERO) {
            diagnostics.add(
                PlanningDiagnostic(
                    diagnosticId = UUID.randomUUID().toString(),
                    planningId = planningDummyId,
                    code = "MISSING_DIMENSIONS",
                    severity = DiagnosticSeverity.CRITICAL_BLOCKING,
                    category = "SPECIFICATION",
                    message = "Finished width (${spec.finishedWidthMm}) and height (${spec.finishedHeightMm}) must be positive.",
                    isBlocking = true,
                    recommendedAction = "Define exact finished dimensions in millimetres."
                )
            )
        }

        // 2. Substrate check
        if (spec.substrateType.isBlank() || spec.substrateGsm <= 0) {
            diagnostics.add(
                PlanningDiagnostic(
                    diagnosticId = UUID.randomUUID().toString(),
                    planningId = planningDummyId,
                    code = "MISSING_SUBSTRATE",
                    severity = DiagnosticSeverity.CRITICAL_BLOCKING,
                    category = "SPECIFICATION",
                    message = "Substrate type and positive GSM (${spec.substrateGsm}) are mandatory for planning.",
                    isBlocking = true,
                    recommendedAction = "Specify paper/substrate type and weight (GSM)."
                )
            )
        }

        // 3. Printing Method check
        if (spec.printingMethod.isBlank()) {
            diagnostics.add(
                PlanningDiagnostic(
                    diagnosticId = UUID.randomUUID().toString(),
                    planningId = planningDummyId,
                    code = "MISSING_PRINT_METHOD",
                    severity = DiagnosticSeverity.CRITICAL_BLOCKING,
                    category = "SPECIFICATION",
                    message = "Printing method (e.g. OFFSET, DIGITAL) is unspecified.",
                    isBlocking = true,
                    recommendedAction = "Select an authoritative printing method."
                )
            )
        }

        // 4. Imposition Ups
        if (spec.impositionUps <= 0) {
            diagnostics.add(
                PlanningDiagnostic(
                    diagnosticId = UUID.randomUUID().toString(),
                    planningId = planningDummyId,
                    code = "INVALID_IMPOSITION_UPS",
                    severity = DiagnosticSeverity.WARNING,
                    category = "SPECIFICATION",
                    message = "Imposition ups count is ${spec.impositionUps}; defaulting to 1 up.",
                    isBlocking = false,
                    recommendedAction = "Verify imposition layout."
                )
            )
        }

        // 5. Artwork check
        if (spec.artworkUrl.isNullOrBlank()) {
            diagnostics.add(
                PlanningDiagnostic(
                    diagnosticId = UUID.randomUUID().toString(),
                    planningId = planningDummyId,
                    code = "MISSING_ARTWORK",
                    severity = DiagnosticSeverity.WARNING,
                    category = "SPECIFICATION",
                    message = "No approved artwork file attached. Prepress plate-making will require artwork approval.",
                    isBlocking = false,
                    recommendedAction = "Upload approved artwork before CTP/Printing stage."
                )
            )
        }

        return diagnostics
    }
}
