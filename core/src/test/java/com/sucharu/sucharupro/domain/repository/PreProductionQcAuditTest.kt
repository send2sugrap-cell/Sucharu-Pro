package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.qc.QcActivityType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Pre-Production QC activity event types (Module 06 Step 02).
 */
class PreProductionQcAuditTest {

    @Test
    fun preProductionActivityTypes_definedCorrectly() {
        val expected = listOf(
            QcActivityType.PRE_PRODUCTION_QC_ITEMS_INITIALIZED,
            QcActivityType.PRE_PRODUCTION_QC_ITEM_UPDATED,
            QcActivityType.PRE_PRODUCTION_QC_SUBMITTED
        )

        expected.forEach { type ->
            assertTrue(type.name.isNotBlank())
            assertTrue(type.defaultLabel.isNotBlank())
        }
    }
}
