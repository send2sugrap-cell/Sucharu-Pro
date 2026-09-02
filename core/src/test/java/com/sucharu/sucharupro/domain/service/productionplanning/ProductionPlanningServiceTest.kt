package com.sucharu.sucharupro.domain.service.productionplanning

import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.commercialcommitment.FakeCommercialCommitmentDataSource
import com.sucharu.sucharupro.data.datasource.printingquote.FakePrintingQuoteDataSource
import com.sucharu.sucharupro.data.datasource.productionplanning.FakeProductionPlanningDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.commercialcommitment.CommercialCommitmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.printingquote.PrintingQuoteRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionplanning.ProductionPlanningRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.productionplanning.PlanningStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionPlanningServiceTest {

    private lateinit var planningRepository: ProductionPlanningRepositoryImpl
    private lateinit var orderRepository: OrderRepositoryImpl
    private lateinit var commitmentRepository: CommercialCommitmentRepositoryImpl
    private lateinit var quoteRepository: PrintingQuoteRepositoryImpl
    private lateinit var service: ProductionPlanningServiceImpl

    private val tenantId = "tenant_test_001"

    @Before
    fun setUp() {
        val planningDs = FakeProductionPlanningDataSource()
        val orderDs = FakeOrderDataSource()
        val commDs = FakeCommercialCommitmentDataSource()
        val quoteDs = FakePrintingQuoteDataSource()

        planningRepository = ProductionPlanningRepositoryImpl(planningDs)
        orderRepository = OrderRepositoryImpl(orderDs)
        commitmentRepository = CommercialCommitmentRepositoryImpl(commDs)
        quoteRepository = PrintingQuoteRepositoryImpl(quoteDs)

        service = ProductionPlanningServiceImpl(
            planningRepository = planningRepository,
            orderRepository = orderRepository,
            commitmentRepository = commitmentRepository,
            quoteRepository = quoteRepository
        )
    }

    private fun setupConfirmedOrder(orderId: String = "ORD-TEST-001"): Order {
        val order = Order(
            orderId = orderId,
            orderNumber = "ORD-2026-901",
            customerId = "CUST-VIP-001",
            status = OrderStatusType.CONFIRMED,
            items = listOf(
                OrderItem(
                    itemId = "ITEM-001",
                    description = "Annual Report 2026",
                    quantity = 2000,
                    unitPrice = Money(45.0)
                )
            ),
            createdAt = "2026-09-01T12:00:00Z",
            updatedAt = "2026-09-01T12:00:00Z"
        )
        runBlocking {
            orderRepository.createOrder(order)
        }
        return order
    }

    @Test
    fun `full production planning lifecycle from readiness to snapshot, reconciliation and handoff`() = runBlocking {
        val orderId = "ORD-LIFECYCLE-001"
        setupConfirmedOrder(orderId)

        // 1. Evaluate Readiness
        val readRes = service.evaluateReadiness(tenantId, orderId, "ITEM-001")
        assertTrue(readRes is DomainResult.Success)
        val eval = (readRes as DomainResult.Success).data
        assertTrue(eval.overallScore >= BigDecimal("80.0000"))
        assertTrue(eval.isManufacturingReady)

        // 2. Create Planning Snapshot
        val planRes = service.createPlanningSnapshot(tenantId, orderId, "ITEM-001", "planner_user", "IDEM-PLAN-001")
        assertTrue(planRes is DomainResult.Success)
        val plan = (planRes as DomainResult.Success).data
        assertEquals(PlanningStatus.READY, plan.status)
        assertEquals(1, plan.version)
        assertTrue(plan.isCurrent)
        assertTrue(plan.requirements.isNotEmpty())
        assertTrue(plan.operations.isNotEmpty())

        // 3. Idempotent duplicate check
        val planRes2 = service.createPlanningSnapshot(tenantId, orderId, "ITEM-001", "planner_user", "IDEM-PLAN-001")
        assertTrue(planRes2 is DomainResult.Success)
        val plan2 = (planRes2 as DomainResult.Success).data
        assertEquals(plan.planningId, plan2.planningId)

        // 4. Reconcile Plan
        val reconRes = service.reconcilePlanning(tenantId, plan.planningId)
        assertTrue(reconRes is DomainResult.Success)
        val recon = (reconRes as DomainResult.Success).data
        assertTrue(recon.isFullyReconciled)
        assertTrue(recon.customerMatch)
        assertTrue(recon.quantityMatch)

        // 5. Handoff to Production
        val handoffRes = service.handoffPlanning(tenantId, plan.planningId, "plant_manager")
        assertTrue(handoffRes is DomainResult.Success)
        val handedOffPlan = (handoffRes as DomainResult.Success).data
        assertEquals(PlanningStatus.HANDED_OFF, handedOffPlan.status)

        // 6. Export AI Handoff Contract
        val contractRes = service.exportHandoffContract(tenantId, plan.planningId)
        assertTrue(contractRes is DomainResult.Success)
        val contract = (contractRes as DomainResult.Success).data
        assertEquals("HANDED_OFF", contract.planningStatus)
        assertTrue(contract.isManufacturingReady)
        assertEquals("RECONCILED", contract.reconciliationStatus)
        assertTrue(contract.integrityHash.isNotBlank())
    }
}
