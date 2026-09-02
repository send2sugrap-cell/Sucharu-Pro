package com.sucharu.sucharupro.domain.service.productionexecution

import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.commercialcommitment.FakeCommercialCommitmentDataSource
import com.sucharu.sucharupro.data.datasource.printingquote.FakePrintingQuoteDataSource
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.data.datasource.productionplanning.FakeProductionPlanningDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.commercialcommitment.CommercialCommitmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.printingquote.PrintingQuoteRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionexecution.ProductionExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionplanning.ProductionPlanningRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecutionStatus
import com.sucharu.sucharupro.domain.model.productionplanning.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionExecutionServiceTest {

    private lateinit var executionDataSource: FakeProductionExecutionDataSource
    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var planningDataSource: FakeProductionPlanningDataSource
    private lateinit var commitmentDataSource: FakeCommercialCommitmentDataSource
    private lateinit var quoteDataSource: FakePrintingQuoteDataSource
    private lateinit var service: ProductionExecutionServiceImpl

    @Before
    fun setUp() {
        runBlocking {
            executionDataSource = FakeProductionExecutionDataSource()
            orderDataSource = FakeOrderDataSource(emptyList())
            planningDataSource = FakeProductionPlanningDataSource()
            commitmentDataSource = FakeCommercialCommitmentDataSource()
            quoteDataSource = FakePrintingQuoteDataSource()

            val orderRepo = OrderRepositoryImpl(orderDataSource)
            val planningRepo = ProductionPlanningRepositoryImpl(planningDataSource)
            val commitmentRepo = CommercialCommitmentRepositoryImpl(commitmentDataSource)
            val quoteRepo = PrintingQuoteRepositoryImpl(quoteDataSource)
            val executionRepo = ProductionExecutionRepositoryImpl(executionDataSource)

            service = ProductionExecutionServiceImpl(
                executionRepository = executionRepo,
                orderRepository = orderRepo,
                planningRepository = planningRepo,
                commitmentRepository = commitmentRepo,
                quoteRepository = quoteRepo
            )

            // Seed Order
            val order = Order(
                orderId = "ORD-201",
                orderNumber = "ORD-2026-0201",
                customerId = "CUST-001",
                status = OrderStatusType.CONFIRMED,
                items = listOf(
                    OrderItem(
                        itemId = "ITEM-001",
                        description = "Corporate Annual Report",
                        quantity = 2000,
                        unitPrice = Money(45.0),
                        unit = "PCS"
                    )
                ),
                createdAt = "2026-09-01T12:00:00Z",
                updatedAt = "2026-09-01T12:00:00Z"
            )
            orderRepo.createOrder(order)

            // Seed Planning Snapshot
            val planning = ProductionPlanningSnapshot(
                planningId = "PLAN-ORD-201-ITEM-001-V1",
                tenantId = "tenant_001",
                projectId = "tenant_001",
                orderId = "ORD-201",
                orderNumber = "ORD-2026-0201",
                orderItemId = "ITEM-001",
                commercialCommitmentId = "COMM-201",
                quotationId = "QUO-201",
                quotationVersionNumber = 1,
                customerId = "CUST-001",
                status = PlanningStatus.READY,
                version = 1,
                isCurrent = true,
                readinessScore = BigDecimal("94.0000"),
                feasibilityStatus = FeasibilityStatus.FEASIBLE,
                specification = ProductionJobSpecification(
                    specId = "SPEC-201",
                    jobTitle = "Annual Report 2026",
                    productType = "ANNUAL_REPORT",
                    orderedQuantity = 2000L,
                    plannedQuantity = 2100L,
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
                    specFingerprint = "FP-201"
                ),
                requirements = emptyList(),
                operations = listOf(
                    ProductionPlanningOperation(
                        operationId = "OP-201-1",
                        planningId = "PLAN-ORD-201-ITEM-001-V1",
                        sequenceNumber = 1,
                        stageType = ProductionStageType.CTP,
                        operationCode = "OP-CTP",
                        operationName = "CTP Plate Making",
                        targetWorkCenter = "Pre-Press"
                    ),
                    ProductionPlanningOperation(
                        operationId = "OP-201-2",
                        planningId = "PLAN-ORD-201-ITEM-001-V1",
                        sequenceNumber = 2,
                        stageType = ProductionStageType.PRINTING,
                        operationCode = "OP-PRT",
                        operationName = "Offset Printing",
                        targetWorkCenter = "Press Room"
                    ),
                    ProductionPlanningOperation(
                        operationId = "OP-201-3",
                        planningId = "PLAN-ORD-201-ITEM-001-V1",
                        sequenceNumber = 3,
                        stageType = ProductionStageType.FINAL_QC,
                        operationCode = "OP-QC",
                        operationName = "Final Quality Inspection",
                        targetWorkCenter = "QC Lab",
                        isQcCheckpoint = true
                    )
                ),
                diagnostics = emptyList(),
                planningFingerprint = "FP-201",
                integrityHash = "HASH-201",
                createdAt = System.currentTimeMillis(),
                createdBy = "planner"
            )
            planningDataSource.savePlanningSnapshot(planning)
        }
    }

    @Test
    fun `test evaluate eligibility`() {
        runBlocking {
            val elRes = service.evaluateJobEligibility("tenant_001", "ORD-201")
            assertTrue(elRes is DomainResult.Success)
            val diags = (elRes as DomainResult.Success).data
            assertTrue(diags.isEmpty())
        }
    }

    @Test
    fun `test create and release job execution`() {
        runBlocking {
            val jobRes = service.createJobExecution("tenant_001", "ORD-201", "operator_001")
            assertTrue(jobRes is DomainResult.Success)
            val job = (jobRes as DomainResult.Success).data
            assertEquals(ProductionJobExecutionStatus.READY, job.status)
            assertEquals(BigDecimal("2100.0000"), job.plannedQuantity)
            assertEquals(3, job.workOrders.size)

            val relRes = service.releaseJob("tenant_001", job.executionJobId, "manager_001")
            assertTrue(relRes is DomainResult.Success)
            assertEquals(ProductionJobExecutionStatus.RELEASED, (relRes as DomainResult.Success).data.status)
        }
    }

    @Test
    fun `test full execution and completion lifecycle`() {
        runBlocking {
            val job = (service.createJobExecution("tenant_001", "ORD-201", "operator_001") as DomainResult.Success).data
            service.releaseJob("tenant_001", job.executionJobId, "manager_001")

            val wo1 = job.workOrders[0]
            val s1 = service.startStage("tenant_001", job.executionJobId, wo1.workOrderId, "op_ctp", "mach_ctp_1", "operator_001")
            assertTrue(s1 is DomainResult.Success)

            val c1 = service.completeStage("tenant_001", job.executionJobId, wo1.workOrderId, BigDecimal("2100.0000"), BigDecimal("0.0000"), "Plates ready", "operator_001")
            assertTrue(c1 is DomainResult.Success)

            val wo2 = job.workOrders[1]
            val s2 = service.startStage("tenant_001", job.executionJobId, wo2.workOrderId, "op_print", "mach_heidelberg_1", "operator_001")
            assertTrue(s2 is DomainResult.Success)

            val p2 = service.pauseStage("tenant_001", job.executionJobId, wo2.workOrderId, "Lunch break", "operator_001")
            assertTrue(p2 is DomainResult.Success)

            val r2 = service.resumeStage("tenant_001", job.executionJobId, wo2.workOrderId, "operator_001")
            assertTrue(r2 is DomainResult.Success)

            val w2 = service.recordWastage("tenant_001", job.executionJobId, wo2.workOrderId, "MAT-ART-150", BigDecimal("50.0000"), "SHEETS", "Setup trim", "operator_001")
            assertTrue(w2 is DomainResult.Success)

            val c2 = service.completeStage("tenant_001", job.executionJobId, wo2.workOrderId, BigDecimal("2050.0000"), BigDecimal("0.0000"), "Printing done", "operator_001")
            assertTrue(c2 is DomainResult.Success)

            val wo3 = job.workOrders[2]
            service.startStage("tenant_001", job.executionJobId, wo3.workOrderId, "op_qc", null, "inspector_001")
            val c3 = service.completeStage("tenant_001", job.executionJobId, wo3.workOrderId, BigDecimal("2050.0000"), BigDecimal("0.0000"), "Passed 100%", "inspector_001")
            assertTrue(c3 is DomainResult.Success)

            val compJob = service.completeJob("tenant_001", job.executionJobId, "All stages finished cleanly", "manager_001")
            assertTrue(compJob is DomainResult.Success)
            val finalJob = (compJob as DomainResult.Success).data
            assertEquals(ProductionJobExecutionStatus.COMPLETED, finalJob.status)
            assertTrue(finalJob.isCompleted)

            val reconRes = service.reconcileJob("tenant_001", job.executionJobId)
            assertTrue(reconRes is DomainResult.Success)
            val recon = (reconRes as DomainResult.Success).data
            assertTrue(recon.orderMatch)
            assertTrue(recon.planningMatch)
            assertTrue(recon.workOrdersComplete)

            val aiRes = service.exportHandoffContract("tenant_001", job.executionJobId)
            assertTrue(aiRes is DomainResult.Success)
            val contract = (aiRes as DomainResult.Success).data
            assertEquals("COMPLETED", contract.status)
            assertEquals(3, contract.completedWorkOrdersCount)
            assertTrue(contract.integrityHash.isNotBlank())
        }
    }
}
