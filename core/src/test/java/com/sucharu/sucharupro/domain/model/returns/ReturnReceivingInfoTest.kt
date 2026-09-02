package com.sucharu.sucharupro.domain.model.returns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnReceivingInfoTest {

    @Test
    fun `instantiates ReturnReceivingInfo with correct attributes`() {
        val receiving = ReturnReceivingInfo(
            receivingEventId = "RCV-EVT-01",
            returnId = "RET-001",
            projectId = "PRJ-01",
            receiverId = "user-warehouse-01",
            receivedAt = 1700000000000L,
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 8,
            rejectedQty = 1,
            damagedQty = 1,
            mismatchFlag = false,
            condition = "Slight outer box wear",
            packaging = "Original box intact",
            damageNotes = "1 unit scratched",
            version = 1L,
            idempotencyKey = "IDEMP-KEY-001"
        )

        assertEquals("RCV-EVT-01", receiving.receivingEventId)
        assertEquals("RET-001", receiving.returnId)
        assertEquals("PRJ-01", receiving.projectId)
        assertEquals("user-warehouse-01", receiving.receiverId)
        assertEquals(1700000000000L, receiving.receivedAt)
        assertEquals(10, receiving.approvedQty)
        assertEquals(10, receiving.actualQty)
        assertEquals(8, receiving.acceptedQty)
        assertEquals(1, receiving.rejectedQty)
        assertEquals(1, receiving.damagedQty)
        assertFalse(receiving.mismatchFlag)
        assertEquals("Slight outer box wear", receiving.condition)
        assertEquals("Original box intact", receiving.packaging)
        assertEquals("1 unit scratched", receiving.damageNotes)
        assertEquals(1L, receiving.version)
        assertEquals("IDEMP-KEY-001", receiving.idempotencyKey)
    }

    @Test
    fun `copy method creates updated copy preserving other properties`() {
        val original = ReturnReceivingInfo(
            receivingEventId = "RCV-EVT-01",
            returnId = "RET-001",
            projectId = "PRJ-01",
            receiverId = "user-warehouse-01",
            approvedQty = 10,
            actualQty = 8,
            acceptedQty = 8,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = true,
            version = 1L,
            idempotencyKey = "IDEMP-KEY-001"
        )

        val updated = original.copy(
            version = 2L,
            condition = "Verified with manager"
        )

        assertEquals(2L, updated.version)
        assertEquals("Verified with manager", updated.condition)
        assertEquals(original.receivingEventId, updated.receivingEventId)
        assertEquals(original.returnId, updated.returnId)
        assertEquals(original.projectId, updated.projectId)
        assertEquals(original.receiverId, updated.receiverId)
        assertEquals(original.approvedQty, updated.approvedQty)
        assertEquals(original.actualQty, updated.actualQty)
        assertEquals(original.acceptedQty, updated.acceptedQty)
        assertEquals(original.rejectedQty, updated.rejectedQty)
        assertEquals(original.damagedQty, updated.damagedQty)
        assertTrue(updated.mismatchFlag)
        assertEquals(original.idempotencyKey, updated.idempotencyKey)
    }
}
