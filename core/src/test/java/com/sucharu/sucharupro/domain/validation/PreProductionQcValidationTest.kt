package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcCategory
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcItem
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Pre-Production QC validation logic (Module 06 Step 02).
 */
class PreProductionQcValidationTest {

    private fun createItem(status: PreProductionItemStatus, isRequired: Boolean = true): PreProductionQcItem {
        return PreProductionQcItem(
            itemId = "it-1",
            qcId = "qc-1",
            category = PreProductionQcCategory.JOB_SPECIFICATION,
            label = "Spec test",
            status = status,
            isRequired = isRequired
        )
    }

    @Test
    fun completionValidation_withPendingRequiredItems_fails() {
        val items = listOf(
            createItem(PreProductionItemStatus.PASS),
            createItem(PreProductionItemStatus.PENDING)
        )
        val res = PreProductionQcValidator.validateItemsCompletion(items, QcDecision.PASS)
        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("pending required check item"))
    }

    @Test
    fun completionValidation_passWithFailedRequiredItems_fails() {
        val items = listOf(
            createItem(PreProductionItemStatus.PASS),
            createItem(PreProductionItemStatus.FAIL)
        )
        val res = PreProductionQcValidator.validateItemsCompletion(items, QcDecision.PASS)
        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("Cannot PASS Pre-Production QC when"))
    }

    @Test
    fun completionValidation_failWithoutAnyFailedItem_fails() {
        val items = listOf(
            createItem(PreProductionItemStatus.PASS),
            createItem(PreProductionItemStatus.PASS)
        )
        val res = PreProductionQcValidator.validateItemsCompletion(items, QcDecision.FAIL)
        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("without at least one failed"))
    }
}
