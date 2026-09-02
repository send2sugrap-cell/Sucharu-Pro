package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffItem
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffStatus
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit and integration test suite for Production Job Creation from OrderJobHandoff (Module 04 Step 03).
 */
class ProductionJobCreationTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleItem = OrderJobHandoffItem(
        itemId = "item-01",
        description = "বাংলা ব্যাকরণ ও নির্মিতি বই",
        specification = "চার কালার প্রচ্ছদ, ৮০ জিএসএম অফসেট কাগজ",
        quantity = 5000,
        unit = "কপি",
        unitPrice = 120.toMoney(),
        lineSubtotal = 600000.toMoney()
    )

    private val validHandoff = OrderJobHandoff(
        handoffId = "hnd-2026-001",
        orderId = "ord-2026-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-2026-001",
        quotationId = "qt-2026-001",
        approvedRevisionId = "rev-2026-001",
        priority = OrderPriority.URGENT,
        deliveryRequirement = DeliveryRequirement(
            deliveryType = DeliveryType.BUSINESS_DELIVERY,
            address = "৩৮/২ বাংলাবাজার, ঢাকা",
            contactName = "আহমেদ হাসান",
            contactPhone = "+8801711223344"
        ),
        items = listOf(sampleItem),
        commercialTotal = 600000.toMoney(),
        notes = "জরুরি প্রেস ডেলিভারি প্রয়োজন",
        createdAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
    }

    @Test
    fun createJobFromHandoff_succeedsWithAll13StagesPending() = runBlocking {
        val result = repository.createJobFromHandoff(
            handoff = validHandoff,
            createdBy = "Head of Production",
            timestamp = "2026-08-16T10:30:00Z"
        )

        assertTrue("Expected success, got $result", result is DomainResult.Success<*>)
        val job = (result as DomainResult.Success<ProductionJob>).data

        assertEquals("ORD-2026-0001", job.orderNumber)
        assertEquals("cus-2026-001", job.customerId)
        assertEquals("hnd-2026-001", job.handoffId)
        assertEquals("বাংলা ব্যাকরণ ও নির্মিতি বই", job.title)
        assertEquals(5000, job.quantity)
        assertEquals("কপি", job.unit)
        assertEquals(OrderPriority.URGENT, job.priority)
        assertEquals(ProductionJobStatus.READY_FOR_PRODUCTION, job.status)
        assertEquals("Head of Production", job.createdBy)
        assertEquals("৩৮/২ বাংলাবাজার, ঢাকা", job.deliveryRequirement?.address)

        // All 13 canonical stages must be initialized to PENDING
        assertEquals(13, job.stages.size)
        assertTrue(job.stages.all { it.status == ProductionStageStatus.PENDING })
        assertEquals(ProductionStageType.DESIGN, job.stages[0].stageType)
        assertEquals(ProductionStageType.DELIVERED, job.stages[12].stageType)
        assertEquals(0, job.completedStagesCount)
        assertEquals(0f, job.progressFraction, 0.001f)
    }

    @Test
    fun createJobFromHandoff_generatesSequentialJobNumber() = runBlocking {
        val job1 = (repository.createJobFromHandoff(validHandoff, timestamp = "2026-08-16T10:30:00Z") as DomainResult.Success).data
        assertEquals("JOB-2026-0001", job1.jobNumber)

        val handoff2 = validHandoff.copy(
            handoffId = "hnd-2026-002",
            orderId = "ord-2026-002",
            orderNumber = "ORD-2026-0002"
        )
        val job2 = (repository.createJobFromHandoff(handoff2, timestamp = "2026-08-16T10:35:00Z") as DomainResult.Success).data
        assertEquals("JOB-2026-0002", job2.jobNumber)
    }

    @Test
    fun duplicateActiveHandoff_isRejected() = runBlocking {
        repository.createJobFromHandoff(validHandoff, timestamp = "2026-08-16T10:30:00Z")

        val duplicateResult = repository.createJobFromHandoff(validHandoff, timestamp = "2026-08-16T10:35:00Z")
        assertTrue(duplicateResult is DomainResult.Error)
        assertTrue((duplicateResult as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun cancelledHandoff_isRejected() = runBlocking {
        val cancelledHandoff = validHandoff.copy(handoffStatus = OrderJobHandoffStatus.CANCELLED)

        val result = repository.createJobFromHandoff(cancelledHandoff, timestamp = "2026-08-16T10:30:00Z")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cancelled"))
    }

    @Test
    fun handoffWithNoItems_isRejected() = runBlocking {
        val emptyHandoff = validHandoff.copy(items = emptyList())

        val result = repository.createJobFromHandoff(emptyHandoff, timestamp = "2026-08-16T10:30:00Z")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("at least one item"))
    }

    @Test
    fun snapshotImmutability_subsequentHandoffMutations_doNotAlterJobSnapshot() = runBlocking {
        val job = (repository.createJobFromHandoff(validHandoff, timestamp = "2026-08-16T10:30:00Z") as DomainResult.Success).data

        // Mutate original handoff representation
        val mutatedHandoff = validHandoff.copy(
            priority = OrderPriority.NORMAL,
            notes = "Mutated notes",
            items = listOf(sampleItem.copy(quantity = 1))
        )

        // Job record in repository is unaffected
        val fetched = (repository.findJobById(job.jobId) as DomainResult.Success).data
        assertEquals(OrderPriority.URGENT, fetched.priority)
        assertEquals(5000, fetched.quantity)
        assertEquals("জরুরি প্রেস ডেলিভারি প্রয়োজন", fetched.notes)
    }

    @Test
    fun concurrentJobCreationForSameHandoff_onlyOneWins() = runBlocking {
        // Concurrently attempt to create Job for the same handoff 10 times
        val results = (1..10).map {
            async {
                repository.createJobFromHandoff(validHandoff, timestamp = "2026-08-16T10:30:00Z")
            }
        }.awaitAll()

        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(9, errorCount)

        val allJobs = repository.observeJobs().first()
        assertEquals(1, allJobs.size)
    }
}
