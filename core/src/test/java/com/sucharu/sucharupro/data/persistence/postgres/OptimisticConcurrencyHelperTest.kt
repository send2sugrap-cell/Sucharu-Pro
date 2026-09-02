package com.sucharu.sucharupro.data.persistence.postgres

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement

/**
 * Unit tests for [OptimisticConcurrencyHelper] (INFRA-01 Step 03).
 */
class OptimisticConcurrencyHelperTest {

    @Test
    fun `optimistic lock exception formats message correctly`() {
        val ex = OptimisticLockException("Order", "ORD-12345", 2)
        assertEquals("Order", ex.entityType)
        assertEquals("ORD-12345", ex.entityId)
        assertEquals(2L, ex.expectedVersion)
        assertTrue(ex.message!!.contains("ORD-12345"))
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
