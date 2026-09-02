package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationCondition
import com.sucharu.sucharupro.domain.model.communication.automation.ConditionOperator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationAutomationConditionTest {

    @Test
    fun evaluate_equalsOperator() {
        val cond = CommunicationAutomationCondition("status", ConditionOperator.EQUALS, "READY")
        assertTrue(cond.evaluate("READY"))
        assertTrue(cond.evaluate("ready")) // Case-insensitive
        assertFalse(cond.evaluate("IN_PROGRESS"))
    }

    @Test
    fun evaluate_greaterThanOperator_numeric() {
        val cond = CommunicationAutomationCondition("amount", ConditionOperator.GREATER_THAN, "1000")
        assertTrue(cond.evaluate("1500"))
        assertFalse(cond.evaluate("1000"))
        assertFalse(cond.evaluate("500"))
    }

    @Test
    fun evaluate_greaterOrEqualOperator_numeric() {
        val cond = CommunicationAutomationCondition("days", ConditionOperator.GREATER_OR_EQUAL, "15")
        assertTrue(cond.evaluate("15"))
        assertTrue(cond.evaluate("20"))
        assertFalse(cond.evaluate("14"))
    }

    @Test
    fun evaluate_inOperator() {
        val cond = CommunicationAutomationCondition("role", ConditionOperator.IN, "QC_INSPECTOR, MANAGER, ADMIN")
        assertTrue(cond.evaluate("QC_INSPECTOR"))
        assertTrue(cond.evaluate("manager"))
        assertFalse(cond.evaluate("STAFF"))
    }

    @Test
    fun evaluate_containsOperator() {
        val cond = CommunicationAutomationCondition("fileName", ConditionOperator.CONTAINS, ".pdf")
        assertTrue(cond.evaluate("invoice_01.pdf"))
        assertFalse(cond.evaluate("invoice_01.docx"))
    }
}
