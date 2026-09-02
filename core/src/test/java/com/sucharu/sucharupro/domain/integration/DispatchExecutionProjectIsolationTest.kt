package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeDeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.FakeDispatchExecutionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.DispatchExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryStockOutRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DispatchExecutionProjectIsolationTest {

    private lateinit var dispatchDataSource: FakeDispatchExecutionDataSource
    private lateinit var challanDataSource: FakeDeliveryChallanDataSource
    private lateinit var dispatchRepository: DispatchExecutionRepository

    @Before
    fun setUp() {
        runBlocking {
            dispatchDataSource = FakeDispatchExecutionDataSource()
            challanDataSource = FakeDeliveryChallanDataSource()

            val stockOutDataSource = FakeInventoryStockOutDataSource()
            val receivingDataSource = FakeInventoryReceivingDataSource()
            val productDataSource = FakeInventoryProductDataSource()
            val warehouseDataSource = FakeInventoryWarehouseDataSource()
            val locationDataSource = FakeInventoryLocationDataSource()

            val stockOutRepo = InventoryStockOutRepositoryImpl(
                stockOutDataSource, receivingDataSource, productDataSource, warehouseDataSource, locationDataSource
            )

            dispatchRepository = DispatchExecutionRepositoryImpl(
                dispatchDataSource = dispatchDataSource,
                challanDataSource = challanDataSource,
                stockOutRepository = stockOutRepo
            )

            // Challan A
            val chA = DeliveryChallan("CH-A", "PRJ-A", "CH-A", "DO-A", null, null, null, DeliveryChallanType.STANDARD, DeliveryChallanStatus.APPROVED, 1000L, null, "user-1", 1000L, 1000L)
            val lineA = DeliveryChallanLine("CLA", "CH-A", "PRJ-A", "DOLA", "PROD-A", 10.0)
            challanDataSource.insertChallan(chA, listOf(lineA))

            // Challan B
            val chB = DeliveryChallan("CH-B", "PRJ-B", "CH-B", "DO-B", null, null, null, DeliveryChallanType.STANDARD, DeliveryChallanStatus.APPROVED, 1000L, null, "user-2", 1000L, 1000L)
            val lineB = DeliveryChallanLine("CLB", "CH-B", "PRJ-B", "DOLB", "PROD-B", 20.0)
            challanDataSource.insertChallan(chB, listOf(lineB))
        }
    }

    @Test
    fun `observeDispatches returns strictly project scoped dispatch executions`() = runBlocking {
        val dispA = DispatchExecution("DISP-A", "PRJ-A", "DN-001", "DO-A", "CH-A", null, "WH-A", "LOC-A", DispatchExecutionType.STANDARD, DispatchExecutionStatus.DRAFT, null, 1000L, null, "user-1", 1000L, 1000L)
        val dlineA = DispatchExecutionLine("DLA", "PRJ-A", "DISP-A", "CLA", "DOLA", "PROD-A", 10.0, 10.0, null, null, "LOC-A", 1000L)
        dispatchRepository.createDispatch(dispA, listOf(dlineA), UserRole.ADMIN, "PRJ-A")

        val dispB = DispatchExecution("DISP-B", "PRJ-B", "DN-001", "DO-B", "CH-B", null, "WH-B", "LOC-B", DispatchExecutionType.STANDARD, DispatchExecutionStatus.DRAFT, null, 1000L, null, "user-2", 1000L, 1000L)
        val dlineB = DispatchExecutionLine("DLB", "PRJ-B", "DISP-B", "CLB", "DOLB", "PROD-B", 20.0, 20.0, null, null, "LOC-B", 1000L)
        dispatchRepository.createDispatch(dispB, listOf(dlineB), UserRole.ADMIN, "PRJ-B")

        val listA = dispatchRepository.observeDispatches("PRJ-A").first()
        val listB = dispatchRepository.observeDispatches("PRJ-B").first()

        assertEquals(1, listA.size)
        assertEquals("DISP-A", listA[0].dispatchExecutionId)

        assertEquals(1, listB.size)
        assertEquals("DISP-B", listB[0].dispatchExecutionId)
    }

    @Test
    fun `cross project getDispatch is blocked`() = runBlocking {
        val dispA = DispatchExecution("DISP-A", "PRJ-A", "DN-001", "DO-A", "CH-A", null, "WH-A", "LOC-A", DispatchExecutionType.STANDARD, DispatchExecutionStatus.DRAFT, null, 1000L, null, "user-1", 1000L, 1000L)
        val dlineA = DispatchExecutionLine("DLA", "PRJ-A", "DISP-A", "CLA", "DOLA", "PROD-A", 10.0, 10.0, null, null, "LOC-A", 1000L)
        dispatchRepository.createDispatch(dispA, listOf(dlineA), UserRole.ADMIN, "PRJ-A")

        val result = dispatchRepository.getDispatch("DISP-A", UserRole.ADMIN, "PRJ-B")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Access denied"))
    }
}
