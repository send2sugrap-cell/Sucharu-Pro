package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.domain.model.common.DomainResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.SQLException

/**
 * Unit tests for [PostgresErrorTranslator] (INFRA-01 Step 03).
 */
class PostgresErrorTranslatorTest {

    @Test
    fun `translates unique violation 23505 to user-safe message`() {
        val ex = SQLException("duplicate key value violates unique constraint", "23505")
        val result = PostgresErrorTranslator.translate<Unit>(ex, "insert customer")
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertEquals("A record with this identifier or unique attribute already exists.", err.message)
    }

    @Test
    fun `translates foreign key violation 23503 to user-safe message`() {
        val ex = SQLException("violates foreign key constraint", "23503")
        val result = PostgresErrorTranslator.translate<Unit>(ex, "insert order")
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertEquals("Foreign key relationship error: Referenced record not found or cannot be deleted.", err.message)
    }

    @Test
    fun `translates check constraint violation 23514 to user-safe message`() {
        val ex = SQLException("violates check constraint", "23514")
        val result = PostgresErrorTranslator.translate<Unit>(ex, "insert lot")
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertEquals("Data validation failed: Check constraint violated.", err.message)
    }

    @Test
    fun `translates journal imbalance exception to user-safe message`() {
        val ex = SQLException("Journal imbalance for transaction TX-101: debits 1000.00 vs credits 500.00", "P0001")
        val result = PostgresErrorTranslator.translate<Unit>(ex, "post journal")
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertEquals("Financial journal is out of balance: Total debits must equal total credits.", err.message)
    }

    @Test
    fun `translates optimistic lock exception to user-safe message`() {
        val ex = OptimisticLockException("Customer", "CUST-001", 3)
        val result = PostgresErrorTranslator.translate<Unit>(ex, "update customer")
        assertTrue(result is DomainResult.Error)
        val err = result as DomainResult.Error
        assertEquals("Concurrent update detected: The record has been modified by another operation.", err.message)
    }
}
