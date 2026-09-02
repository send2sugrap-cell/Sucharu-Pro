package com.sucharu.sucharupro.domain.service.productionexecution

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecutionStatus
import com.sucharu.sucharupro.domain.model.productionexecution.WorkOrderStatus
import com.sucharu.sucharupro.domain.model.productionplanning.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProductionJobEngineTest {

    private fun createTestOrder(): Order {
        return Order(
            orderId = "ORD-101",
            orderNumber = "ORD-2026-0101",
            customerId = "CUST-001",
            status = OrderStatusType.CONFIRMED,
            items = listOf(
                OrderItem(
                    itemId = "ITEM-001",
                    description = "Brochure Print",
                    quantity = 5000,
                    unitPrice = Money(15.0)
                )
            ),
            createdAt = "2026-09-01T12:00:00Z",
            updatedAt = "2026-09-01T12:00:00Z"
        )
    }

    private fun createTestPlanningSnapshot(): ProductionPlanningSnapshot {
        val operations = listOf(
            ProductionPlanningOperation(
                operationId = "OP-101-1",
                planningId = "PLAN-ORD-101-ITEM-001-V1",
                sequenceNumber = 1,
                stageType = ProductionStageType.DESIGN,
                operationCode = "OP-DSN",
                operationName = "Artwork Prep",
                targetWorkCenter = "Design Studio"
            ),
            ProductionPlanningOperation(
                operationId = "OP-101-2",
                planningId = "PLAN-ORD-101-ITEM-001-V1",
                sequenceNumber = 2,
                stageType = ProductionStageType.PRINTING,
                operationCode = "OP-PRT",
                operationName = "Offset Printing 4-Color",
                targetWorkCenter = "Press Hall"
            ),
            ProductionPlanningOperation(
                operationId = "OP-101-3",
                planningId = "PLAN-ORD-101-ITEM-001-V1",
                sequenceNumber = 3,
                stageType = ProductionStageType.FINAL_QC,
                operationCode = "OP-QC",
                operationName = "Final Quality Inspection",
                targetWorkCenter = "QC Lab",
                isQcCheckpoint = true
            )
        )

        return ProductionPlanningSnapshot(
            planningId = "PLAN-ORD-101-ITEM-001-V1",
            tenantId = "tenant_001",
            projectId = "tenant_001",
            orderId = "ORD-101",
            orderNumber = "ORD-2026-0101",
            orderItemId = "ITEM-001",
            commercialCommitmentId = "COMM-101",
            quotationId = "QUO-101",
            quotationVersionNumber = 1,
            customerId = "CUST-001",
            status = PlanningStatus.READY,
            version = 1,
            isCurrent = true,
            readinessScore = BigDecimal("95.0000"),
            feasibilityStatus = FeasibilityStatus.FEASIBLE,
            specification = ProductionJobSpecification(
                specId = "SPEC-101",
                jobTitle = "Tri-fold Brochure",
                productType = "BROCHURE",
                orderedQuantity = 5000L,
                plannedQuantity = 5250L,
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
                impositionUps = 2,
                specFingerprint = "FP-101"
            ),
            requirements = emptyList(),
            operations = operations,
            diagnostics = emptyList(),
            planningFingerprint = "FP-101",
            integrityHash = "HASH-101",
            createdAt = System.currentTimeMillis(),
            createdBy = "planner"
        )
    }

    @Test
    fun `deriveWorkOrders creates sequential work orders with predecessor links`() {
        val order = createTestOrder()
        val plan = createTestPlanningSnapshot()

        val job = ProductionJobEngine.createJobExecution(order, plan, "supervisor")

        assertEquals(ProductionJobExecutionStatus.READY, job.status)
        assertEquals(BigDecimal("5250.0000"), job.plannedQuantity)
        assertEquals(3, job.workOrders.size)

        // First work order is READY with no predecessors
        val wo1 = job.workOrders[0]
        assertEquals(WorkOrderStatus.READY, wo1.status)
        assertTrue(wo1.predecessorWorkOrderIds.isEmpty())

        // Second work order is PENDING and depends on first
        val wo2 = job.workOrders[1]
        assertEquals(WorkOrderStatus.PENDING, wo2.status)
        assertEquals(listOf(wo1.workOrderId), wo2.predecessorWorkOrderIds)

        // Third work order is PENDING and depends on second
        val wo3 = job.workOrders[2]
        assertEquals(WorkOrderStatus.PENDING, wo3.status)
        assertEquals(listOf(wo2.workOrderId), wo3.predecessorWorkOrderIds)
    }

    @Test
    fun `advanceWorkOrders sets successor work order to READY once predecessor completes`() {
        val order = createTestOrder()
        val plan = createTestPlanningSnapshot()
        val job = ProductionJobEngine.createJobExecution(order, plan, "supervisor")

        val completedFirst = job.workOrders[0].copy(status = WorkOrderStatus.COMPLETED)
        val list = listOf(completedFirst, job.workOrders[1], job.workOrders[2])

        val advanced = ProductionJobEngine.advanceWorkOrders(list)
        assertEquals(WorkOrderStatus.COMPLETED, advanced[0].status)
        assertEquals(WorkOrderStatus.READY, advanced[1].status) // Transitioned from PENDING to READY
        assertEquals(WorkOrderStatus.PENDING, advanced[2].status) // Still PENDING
    }
}
