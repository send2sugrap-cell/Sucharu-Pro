package com.sucharu.sucharupro.domain.validation.productionexecution

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.productionexecution.WorkOrderStatus
import com.sucharu.sucharupro.domain.model.productionplanning.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProductionJobValidatorTest {

    private fun createTestOrder(status: OrderStatusType = OrderStatusType.CONFIRMED): Order {
        return Order(
            orderId = "ORD-001",
            orderNumber = "ORD-2026-0001",
            customerId = "CUST-001",
            status = status,
            items = listOf(
                OrderItem(
                    itemId = "ITEM-001",
                    description = "Magazine Print",
                    quantity = 1000,
                    unitPrice = Money(50.0)
                )
            ),
            createdAt = "2026-09-01T12:00:00Z",
            updatedAt = "2026-09-01T12:00:00Z"
        )
    }

    private fun createTestPlanningSnapshot(
        status: PlanningStatus = PlanningStatus.READY,
        readinessScore: BigDecimal = BigDecimal("92.5000"),
        diagnostics: List<PlanningDiagnostic> = emptyList(),
        plannedQty: Long = 1050L
    ): ProductionPlanningSnapshot {
        return ProductionPlanningSnapshot(
            planningId = "PLAN-ORD-001-ITEM-001-V1",
            tenantId = "tenant_001",
            projectId = "tenant_001",
            orderId = "ORD-001",
            orderNumber = "ORD-2026-0001",
            orderItemId = "ITEM-001",
            commercialCommitmentId = "COMM-001",
            quotationId = "QUO-001",
            quotationVersionNumber = 1,
            customerId = "CUST-001",
            status = status,
            version = 1,
            isCurrent = true,
            readinessScore = readinessScore,
            feasibilityStatus = FeasibilityStatus.FEASIBLE,
            specification = ProductionJobSpecification(
                specId = "SPEC-001",
                jobTitle = "Magazine Print Job",
                productType = "MAGAZINE",
                orderedQuantity = 1000L,
                plannedQuantity = plannedQty,
                finishedWidthMm = BigDecimal("210.0000"),
                finishedHeightMm = BigDecimal("297.0000"),
                substrateType = "ART_PAPER",
                substrateGsm = 150,
                parentSheetWidthMm = BigDecimal("640.0000"),
                parentSheetHeightMm = BigDecimal("900.0000"),
                pressSheetWidthMm = BigDecimal("640.0000"),
                pressSheetHeightMm = BigDecimal("450.0000"),
                printingMethod = "OFFSET",
                colorsFront = 4,
                colorsBack = 4,
                impositionUps = 4,
                specFingerprint = "FP-001"
            ),
            requirements = emptyList(),
            operations = emptyList(),
            diagnostics = diagnostics,
            planningFingerprint = "FP-001",
            integrityHash = "HASH-001",
            createdAt = System.currentTimeMillis(),
            createdBy = "planner"
        )
    }

    @Test
    fun `valid ready planning snapshot and order pass validation`() {
        val order = createTestOrder()
        val plan = createTestPlanningSnapshot()

        val diags = ProductionExecutionValidator.validateJobCreationEligibility(order, plan)
        assertTrue(diags.isEmpty())
    }

    @Test
    fun `cancelled order produces critical blocking diagnostic`() {
        val order = createTestOrder(status = OrderStatusType.CANCELLED)
        val plan = createTestPlanningSnapshot()

        val diags = ProductionExecutionValidator.validateJobCreationEligibility(order, plan)
        assertTrue(diags.any { it.isBlocking && it.code == "ORDER_CANCELLED" })
    }

    @Test
    fun `planning snapshot with insufficient score produces blocking diagnostic`() {
        val order = createTestOrder()
        val plan = createTestPlanningSnapshot(readinessScore = BigDecimal("65.0000"))

        val diags = ProductionExecutionValidator.validateJobCreationEligibility(order, plan)
        assertTrue(diags.any { it.isBlocking && it.code == "INSUFFICIENT_READINESS_SCORE" })
    }

    @Test
    fun `planning snapshot with critical blockers blocks job creation`() {
        val order = createTestOrder()
        val plan = createTestPlanningSnapshot(
            diagnostics = listOf(
                PlanningDiagnostic(
                    diagnosticId = "DIAG-001",
                    planningId = "PLAN-ORD-001-ITEM-001-V1",
                    code = "MISSING_ARTWORK",
                    severity = DiagnosticSeverity.CRITICAL_BLOCKING,
                    category = "SPECIFICATION",
                    message = "Artwork proof is not yet approved by customer.",
                    isBlocking = true
                )
            )
        )

        val diags = ProductionExecutionValidator.validateJobCreationEligibility(order, plan)
        assertTrue(diags.any { it.isBlocking && it.code == "ACTIVE_PLANNING_BLOCKERS" })
    }
}
