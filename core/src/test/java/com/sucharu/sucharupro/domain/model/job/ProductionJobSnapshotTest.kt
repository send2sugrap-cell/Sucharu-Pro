package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffItem
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying snapshot immutability, factory creation, and Unicode fidelity for [ProductionJob].
 */
class ProductionJobSnapshotTest {

    private val sampleHandoffItem = OrderJobHandoffItem(
        itemId = "item-01",
        description = "ব্যানার প্রিন্টিং",
        specification = "১০x৩ ফিট পিভিসি ব্যানার, আইলেট সহ",
        quantity = 20,
        unit = "Pcs",
        unitPrice = 300.toMoney(),
        lineSubtotal = 6000.toMoney()
    )

    private val sampleHandoff = OrderJobHandoff(
        handoffId = "hnd-snap-01",
        orderId = "ord-snap-01",
        orderNumber = "ORD-2026-S01",
        customerId = "cus-snap-01",
        quotationId = "qt-snap-01",
        approvedRevisionId = "rev-snap-01",
        priority = OrderPriority.URGENT,
        deliveryRequirement = DeliveryRequirement(
            deliveryType = DeliveryType.BUSINESS_DELIVERY,
            address = "৩৮/২ বাংলাবাজার, ঢাকা",
            contactName = "তানভীর আহমেদ",
            contactPhone = "+8801711000000"
        ),
        items = listOf(sampleHandoffItem),
        commercialTotal = 6000.toMoney(),
        notes = "জরুরি প্রেস ডেলিভারি",
        createdAt = "2026-08-16T10:00:00Z"
    )

    @Test
    fun fromHandoff_createsExactProductionSnapshot() {
        val job = ProductionJob.fromHandoff(
            jobId = "job-snap-01",
            jobNumber = "JOB-2026-S01",
            handoff = sampleHandoff,
            createdBy = "Production Incharge",
            timestamp = "2026-08-16T10:30:00Z"
        )

        assertEquals("job-snap-01", job.jobId)
        assertEquals("JOB-2026-S01", job.jobNumber)
        assertEquals("ord-snap-01", job.orderId)
        assertEquals("ORD-2026-S01", job.orderNumber)
        assertEquals("cus-snap-01", job.customerId)
        assertEquals("hnd-snap-01", job.handoffId)
        assertEquals("ব্যানার প্রিন্টিং", job.title)
        assertEquals(OrderPriority.URGENT, job.priority)
        assertEquals(20, job.quantity)
        assertEquals("Pcs", job.unit)
        assertEquals("১০x৩ ফিট পিভিসি ব্যানার, আইলেট সহ", job.specification)
        assertEquals("জরুরি প্রেস ডেলিভারি", job.notes)
        assertEquals(ProductionJobStatus.READY_FOR_PRODUCTION, job.status)
        assertEquals(13, job.stages.size)
        assertEquals("Production Incharge", job.createdBy)
        assertNotNull(job.deliveryRequirement)
        assertEquals("৩৮/২ বাংলাবাজার, ঢাকা", job.deliveryRequirement?.address)
    }

    @Test
    fun snapshotImmutability_subsequentSourceHandoffModifications_doNotMutateJob() {
        val job = ProductionJob.fromHandoff(
            jobId = "job-snap-01",
            jobNumber = "JOB-2026-S01",
            handoff = sampleHandoff,
            timestamp = "2026-08-16T10:30:00Z"
        )

        // Mutate original source handoff representation
        val mutatedItem = sampleHandoffItem.copy(
            quantity = 999,
            description = "Mutated Description"
        )
        val mutatedHandoff = sampleHandoff.copy(
            priority = OrderPriority.NORMAL,
            items = listOf(mutatedItem),
            notes = "Mutated notes"
        )

        // ProductionJob snapshot remains completely untouched
        assertEquals(20, job.quantity)
        assertEquals("ব্যানার প্রিন্টিং", job.title)
        assertEquals(OrderPriority.URGENT, job.priority)
        assertEquals("জরুরি প্রেস ডেলিভারি", job.notes)
        assertEquals("ব্যানার প্রিন্টিং", job.items[0].description)
        assertEquals(20, job.items[0].quantity)
    }

    @Test
    fun banglaUnicodeFidelity_isPreservedInJobFields() {
        val unicodeItem = OrderJobHandoffItem(
            itemId = "item-u-01",
            description = "বাংলা ব্যাকরণ ও নির্মিতি বই",
            specification = "চার কালার প্রচ্ছদ, ৮০ জিএসএম অফসেট কাগজ",
            quantity = 5000,
            unit = "কপি",
            unitPrice = 150.toMoney(),
            lineSubtotal = 750000.toMoney()
        )
        val unicodeHandoff = sampleHandoff.copy(
            items = listOf(unicodeItem),
            notes = "বিশেষ অনুরোধ: বাইন্ডিং মজবুত হতে হবে"
        )

        val job = ProductionJob.fromHandoff(
            jobId = "job-u-01",
            jobNumber = "JOB-বাংলা-০১",
            handoff = unicodeHandoff,
            timestamp = "2026-08-16T10:30:00Z"
        )

        assertEquals("JOB-বাংলা-০১", job.jobNumber)
        assertEquals("বাংলা ব্যাকরণ ও নির্মিতি বই", job.title)
        assertEquals("বাংলা ব্যাকরণ ও নির্মিতি বই", job.items[0].description)
        assertEquals("চার কালার প্রচ্ছদ, ৮০ জিএসএম অফসেট কাগজ", job.specification)
        assertEquals("কপি", job.unit)
        assertEquals("বিশেষ অনুরোধ: বাইন্ডিং মজবুত হতে হবে", job.notes)
    }
}
