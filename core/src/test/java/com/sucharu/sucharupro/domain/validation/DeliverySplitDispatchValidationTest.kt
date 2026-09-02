package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatch
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliverySplitDispatchValidationTest {

    @Test
    fun `valid split dispatch passes validation`() {
        val split = DeliverySplitDispatch(
            splitDispatchId = "SD-1",
            projectId = "PRJ-01",
            deliveryOrderId = "DO-01",
            splitSequence = 1,
            status = DeliverySplitDispatchStatus.APPROVED,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val lines = listOf(
            DeliverySplitDispatchLine("SDL-1", "PRJ-01", "SD-1", "DOL-1", "PROD-1", 100.0, createdAt = 1000L)
        )
        val result = DeliverySplitDispatchValidator.validateSplitDispatch(split, lines, emptyList())
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `duplicate split sequence is rejected`() {
        val existing = DeliverySplitDispatch(
            splitDispatchId = "SD-1",
            projectId = "PRJ-01",
            deliveryOrderId = "DO-01",
            splitSequence = 1,
            status = DeliverySplitDispatchStatus.APPROVED,
            createdBy = "user-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val newSplit = DeliverySplitDispatch(
            splitDispatchId = "SD-2",
            projectId = "PRJ-01",
            deliveryOrderId = "DO-01",
            splitSequence = 1,
            status = DeliverySplitDispatchStatus.APPROVED,
            createdBy = "user-2",
            createdAt = 2000L,
            updatedAt = 2000L
        )
        val lines = listOf(
            DeliverySplitDispatchLine("SDL-2", "PRJ-01", "SD-2", "DOL-1", "PROD-1", 50.0, createdAt = 2000L)
        )
        val result = DeliverySplitDispatchValidator.validateSplitDispatch(newSplit, lines, listOf(existing))
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }
}
