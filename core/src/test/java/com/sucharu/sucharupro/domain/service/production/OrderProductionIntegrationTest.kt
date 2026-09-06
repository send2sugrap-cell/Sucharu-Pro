package com.sucharu.sucharupro.domain.service.production

import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionExecutionDiagnostic
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.service.productionexecution.ProductionExecutionService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class OrderProductionIntegrationTest {

    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var orderRepository: OrderRepositoryImpl
    private lateinit var mockExecutionService: FakeProductionExecutionService
    private lateinit var integrationService: OrderProductionIntegrationServiceImpl

    private val tenantId = "TENANT-001"
    private val orderId = "ORD-TEST-001"

    @Before
    fun setUp() {
        runBlocking {
            orderDataSource = FakeOrderDataSource()
            orderRepository = OrderRepositoryImpl(orderDataSource)
            mockExecutionService = FakeProductionExecutionService()
            integrationService = OrderProductionIntegrationServiceImpl(
                productionExecutionService = mockExecutionService,
                orderRepository = orderRepository
            )

            val testOrder = Order(
                orderId = orderId,
                orderNumber = "ORD-2026-001",
                customerId = "CUST-100",
                status = OrderStatusType.CONFIRMED,
                items = listOf(
                    OrderItem(
                        itemId = "ITEM-101",
                        description = "A4 Deluxe Brochure",
                        quantity = 1000,
                        unitPrice = Money(BigDecimal("15.00"))
                    )
                ),
                createdAt = "2026-09-06T12:00:00Z",
                updatedAt = "2026-09-06T12:00:00Z"
            )
            orderDataSource.insertOrder(testOrder)
        }
    }

    @Test
    fun testCreateProductionJobFromValidOrderSuccess() = runBlocking {
        val result = integrationService.createProductionJobFromOrder(
            tenantId = tenantId,
            orderId = orderId,
            requestedBy = "TestAdmin"
        )

        assertTrue(result is DomainResult.Success)
        val job = (result as DomainResult.Success).data
        assertEquals(orderId, job.orderId)
        assertEquals("ORD-2026-001", job.orderNumber)
        assertEquals("CUST-100", job.customerId)
        assertEquals(13, job.workOrders.size)
    }

    @Test
    fun testDuplicateProductionJobPreventionReturnsExistingJob() = runBlocking {
        val firstResult = integrationService.createProductionJobFromOrder(
            tenantId = tenantId,
            orderId = orderId,
            requestedBy = "TestAdmin"
        )
        assertTrue(firstResult is DomainResult.Success)
        val firstJob = (firstResult as DomainResult.Success).data

        // Attempt duplicate creation for same order
        val secondResult = integrationService.createProductionJobFromOrder(
            tenantId = tenantId,
            orderId = orderId,
            requestedBy = "TestAdmin"
        )

        assertTrue(secondResult is DomainResult.Success)
        val secondJob = (secondResult as DomainResult.Success).data
        assertEquals(firstJob.executionJobId, secondJob.executionJobId)
    }

    @Test
    fun testCancelledOrderFailsProductionCreation() = runBlocking {
        val cancelledOrderId = "ORD-CANCELLED-01"
        orderDataSource.insertOrder(
            Order(
                orderId = cancelledOrderId,
                orderNumber = "ORD-2026-999",
                customerId = "CUST-100",
                status = OrderStatusType.CANCELLED,
                items = listOf(
                    OrderItem(
                        itemId = "ITEM-999",
                        description = "Cancelled Item",
                        quantity = 500,
                        unitPrice = Money(BigDecimal("10.00"))
                    )
                ),
                createdAt = "2026-09-06T12:00:00Z",
                updatedAt = "2026-09-06T12:00:00Z"
            )
        )

        val result = integrationService.createProductionJobFromOrder(
            tenantId = tenantId,
            orderId = cancelledOrderId,
            requestedBy = "TestAdmin"
        )

        assertTrue(result is DomainResult.Error)
        val errorMsg = (result as DomainResult.Error).message
        assertTrue(errorMsg.contains("CANCELLED"))
    }

    @Test
    fun testBlankTenantOrOrderIdFails() = runBlocking {
        val result1 = integrationService.createProductionJobFromOrder(
            tenantId = "",
            orderId = orderId,
            requestedBy = "TestAdmin"
        )
        assertTrue(result1 is DomainResult.Error)

        val result2 = integrationService.createProductionJobFromOrder(
            tenantId = tenantId,
            orderId = "",
            requestedBy = "TestAdmin"
        )
        assertTrue(result2 is DomainResult.Error)
    }

    // Fake in-memory service implementation for testing
    class FakeProductionExecutionService : ProductionExecutionService {
        private val jobs = mutableMapOf<String, ProductionJobExecution>()

        override suspend fun evaluateJobEligibility(
            tenantId: String,
            orderId: String
        ): DomainResult<List<ProductionExecutionDiagnostic>> {
            return DomainResult.Success(emptyList())
        }

        override suspend fun createJobExecution(
            tenantId: String,
            orderId: String,
            requestedBy: String,
            idempotencyKey: String?
        ): DomainResult<ProductionJobExecution> {
            val existing = jobs.values.find { it.orderId == orderId && !it.isCompleted }
            if (existing != null) {
                return DomainResult.Success(existing)
            }

            val executionId = "PJE-${System.currentTimeMillis()}"
            val workOrders = listOf(
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-01",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 1,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.DESIGN,
                    operationCode = "OP-DESIGN",
                    operationName = "Design Stage",
                    targetWorkCenter = "WC-DESIGN",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-02",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 2,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.APPROVAL,
                    operationCode = "OP-APPROVAL",
                    operationName = "Approval Stage",
                    targetWorkCenter = "WC-APPROVAL",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-03",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 3,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.QC,
                    operationCode = "OP-QC",
                    operationName = "QC Stage",
                    targetWorkCenter = "WC-QC",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-04",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 4,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.ITEM_APPROVAL,
                    operationCode = "OP-ITEM-APP",
                    operationName = "Item Approval Stage",
                    targetWorkCenter = "WC-ITEM-APP",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-05",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 5,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.CTP,
                    operationCode = "OP-CTP",
                    operationName = "CTP Stage",
                    targetWorkCenter = "WC-CTP",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-06",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 6,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.PRINTING,
                    operationCode = "OP-PRINT",
                    operationName = "Printing Stage",
                    targetWorkCenter = "WC-PRINT",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-07",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 7,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.LAMINATION,
                    operationCode = "OP-LAM",
                    operationName = "Lamination Stage",
                    targetWorkCenter = "WC-LAM",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-08",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 8,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.FOLDING,
                    operationCode = "OP-FOLD",
                    operationName = "Folding Stage",
                    targetWorkCenter = "WC-FOLD",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-09",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 9,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.BINDING,
                    operationCode = "OP-BIND",
                    operationName = "Binding Stage",
                    targetWorkCenter = "WC-BIND",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-10",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 10,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.FINAL_QC,
                    operationCode = "OP-FINAL-QC",
                    operationName = "Final QC Stage",
                    targetWorkCenter = "WC-FINAL-QC",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-11",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 11,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.PACKAGING,
                    operationCode = "OP-PACK",
                    operationName = "Packaging Stage",
                    targetWorkCenter = "WC-PACK",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-12",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 12,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.READY,
                    operationCode = "OP-READY",
                    operationName = "Ready Stage",
                    targetWorkCenter = "WC-READY",
                    plannedQuantity = BigDecimal("1000.0000")
                ),
                com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder(
                    workOrderId = "WO-13",
                    executionJobId = executionId,
                    tenantId = tenantId,
                    sequenceNumber = 13,
                    stageType = com.sucharu.sucharupro.domain.model.production.ProductionStageType.DELIVERED,
                    operationCode = "OP-DELIVERY",
                    operationName = "Delivery Stage",
                    targetWorkCenter = "WC-DELIVERY",
                    plannedQuantity = BigDecimal("1000.0000")
                )
            )

            val dummySpec = com.sucharu.sucharupro.domain.model.productionplanning.ProductionJobSpecification(
                specId = "SPEC-$orderId",
                jobTitle = "Job for Order ORD-2026-001",
                productType = "PRINT_COMMERCIAL",
                orderedQuantity = 1000L,
                plannedQuantity = 1000L,
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
                impositionUps = 1,
                specFingerprint = "fp-spec-$orderId"
            )

            val job = ProductionJobExecution(
                executionJobId = executionId,
                tenantId = tenantId,
                projectId = "PROJ-100",
                orderId = orderId,
                orderNumber = "ORD-2026-001",
                orderItemId = "ITEM-101",
                customerId = "CUST-100",
                quotationId = "QUO-100",
                quotationVersionNumber = 1,
                commercialCommitmentId = null,
                planningId = "PLAN-$orderId",
                planningVersion = 1,
                title = "Job for Order ORD-2026-001",
                specification = dummySpec,
                plannedQuantity = BigDecimal("1000.0000"),
                remainingQuantity = BigDecimal("1000.0000"),
                jobFingerprint = "fp-$executionId",
                integrityHash = "hash-$executionId",
                workOrders = workOrders,
                createdAt = System.currentTimeMillis(),
                createdBy = requestedBy,
                updatedAt = System.currentTimeMillis()
            )

            jobs[executionId] = job
            return DomainResult.Success(job)
        }

        override suspend fun getJobExecution(
            tenantId: String,
            executionJobId: String
        ): DomainResult<ProductionJobExecution?> {
            return DomainResult.Success(jobs[executionJobId])
        }

        override suspend fun listJobExecutionsByOrder(
            tenantId: String,
            orderId: String
        ): DomainResult<List<ProductionJobExecution>> {
            val list = jobs.values.filter { it.tenantId == tenantId && it.orderId == orderId }
            return DomainResult.Success(list)
        }

        override suspend fun listJobExecutions(
            tenantId: String,
            limit: Int
        ): DomainResult<List<ProductionJobExecution>> {
            return DomainResult.Success(jobs.values.filter { it.tenantId == tenantId }.take(limit))
        }

        override suspend fun releaseJob(tenantId: String, executionJobId: String, releasedBy: String) = TODO()
        override suspend fun scheduleJob(tenantId: String, executionJobId: String, scheduledBy: String) = TODO()
        override suspend fun startStage(tenantId: String, executionJobId: String, workOrderId: String, operatorId: String?, machineId: String?, startedBy: String) = TODO()
        override suspend fun pauseStage(tenantId: String, executionJobId: String, workOrderId: String, reason: String?, pausedBy: String) = TODO()
        override suspend fun resumeStage(tenantId: String, executionJobId: String, workOrderId: String, resumedBy: String) = TODO()
        override suspend fun completeStage(tenantId: String, executionJobId: String, workOrderId: String, goodQuantity: BigDecimal, scrapQuantity: BigDecimal, notes: String?, completedBy: String) = TODO()
        override suspend fun assignMachine(tenantId: String, executionJobId: String, workOrderId: String, machineId: String, machineName: String, assignedBy: String) = TODO()
        override suspend fun assignOperator(tenantId: String, executionJobId: String, workOrderId: String, operatorId: String, operatorName: String, assignedBy: String) = TODO()
        override suspend fun holdJob(tenantId: String, executionJobId: String, workOrderId: String?, category: com.sucharu.sucharupro.domain.model.productionexecution.HoldCategory, reason: String, heldBy: String) = TODO()
        override suspend fun releaseHold(tenantId: String, executionJobId: String, resolutionNotes: String?, releasedBy: String) = TODO()
        override suspend fun recordWastage(tenantId: String, executionJobId: String, workOrderId: String, materialCode: String, quantity: BigDecimal, unitOfMeasure: String, reason: String, recordedBy: String) = TODO()
        override suspend fun createRework(tenantId: String, executionJobId: String, sourceWorkOrderId: String, targetWorkOrderId: String, quantity: BigDecimal, defectCode: String?, reason: String, requestedBy: String) = TODO()
        override suspend fun requestQc(tenantId: String, executionJobId: String, workOrderId: String, requestedBy: String) = TODO()
        override suspend fun completeJob(tenantId: String, executionJobId: String, summary: String?, completedBy: String) = TODO()
        override suspend fun cancelJob(tenantId: String, executionJobId: String, reason: String, cancelledBy: String) = TODO()
        override suspend fun reconcileJob(tenantId: String, executionJobId: String) = TODO()
        override suspend fun exportHandoffContract(tenantId: String, executionJobId: String) = TODO()
        override suspend fun listExecutionEvents(tenantId: String, executionJobId: String) = TODO()
    }
}
