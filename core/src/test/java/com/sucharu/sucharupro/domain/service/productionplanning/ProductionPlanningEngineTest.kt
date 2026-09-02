package com.sucharu.sucharupro.domain.service.productionplanning

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionplanning.FeasibilityStatus
import com.sucharu.sucharupro.domain.model.productionplanning.MachineCompatibilityStatus
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProductionPlanningEngineTest {

    private fun createTestOrder() = Order(
        orderId = "ORD-001",
        orderNumber = "ORD-2026-001",
        customerId = "CUST-001",
        status = OrderStatusType.CONFIRMED,
        items = listOf(
            OrderItem(
                itemId = "ITEM-001",
                description = "Tri-fold Brochure",
                quantity = 5000,
                unitPrice = Money(12.50)
            )
        ),
        createdAt = "2026-09-01T12:00:00Z",
        updatedAt = "2026-09-01T12:00:00Z"
    )

    @Test
    fun `specification normalization and routing generation map correctly to canonical stages`() {
        val order = createTestOrder()
        val item = order.items[0]

        val spec = ProductionPlanningEngine.normalizeSpecification(order, item, null, null, null)
        assertEquals("Tri-fold Brochure", spec.jobTitle)
        assertEquals(5000L, spec.orderedQuantity)
        assertTrue(spec.plannedQuantity >= 5000L)

        val reqs = ProductionPlanningEngine.deriveRequirements("PLAN-001", spec)
        assertTrue(reqs.any { it.category == "SUBSTRATE" })
        assertTrue(reqs.any { it.category == "PLATE" })
        assertTrue(reqs.any { it.category == "INK" })

        val machines = ProductionPlanningEngine.evaluateMachineCompatibility(spec)
        assertTrue(machines.any { it.status == MachineCompatibilityStatus.COMPATIBLE })

        val ops = ProductionPlanningEngine.deriveRouting("PLAN-001", spec)
        assertTrue(ops.any { it.stageType == ProductionStageType.DESIGN })
        assertTrue(ops.any { it.stageType == ProductionStageType.CTP })
        assertTrue(ops.any { it.stageType == ProductionStageType.PRINTING })
        assertTrue(ops.any { it.stageType == ProductionStageType.FINAL_QC && it.isQcCheckpoint })
        assertTrue(ops.any { it.stageType == ProductionStageType.PACKAGING })

        val eval = ProductionPlanningEngine.evaluateManufacturingReadiness(
            order = order,
            item = item,
            commitment = null,
            spec = spec,
            machines = machines,
            operations = ops,
            feasibility = FeasibilityStatus.FEASIBLE
        )

        assertTrue(eval.overallScore >= BigDecimal("80.0000"))
        assertTrue(eval.isManufacturingReady)
        assertEquals(0, eval.blockingIssuesCount)
    }
}
