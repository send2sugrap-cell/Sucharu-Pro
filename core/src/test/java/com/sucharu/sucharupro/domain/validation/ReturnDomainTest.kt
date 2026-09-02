package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.validation.returns.ReturnDomainValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ReturnRequest domain model and [ReturnDomainValidator] (Module 11 Step 01).
 *
 * Covers: valid creation, invalid field cases, and project isolation validation.
 */
class ReturnDomainTest {

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun validRequest(
        returnId: String = "RET-001",
        projectId: String = "PRJ-A",
        returnNo: String = "RN-2024-001",
        customerId: String = "CUST-001",
        requestedBy: String = "USER-01",
        reason: ReturnReason = ReturnReason.PRINTING_DEFECT
    ) = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = returnNo,
        customerId = customerId,
        originalChallanId = "CHAL-001",
        status = ReturnStatus.REQUESTED,
        reason = reason,
        description = "Colour misprint on 50 copies.",
        requestedAt = System.currentTimeMillis(),
        requestedBy = requestedBy,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = 1L
    )

    // =========================================================================
    // Return creation — valid cases
    // =========================================================================

    @Test
    fun `valid return request passes domain validation`() {
        val request = validRequest()
        val result = ReturnDomainValidator.validateReturnRequest(request)
        assertTrue("Valid request should pass", result is DomainResult.Success)
    }

    @Test
    fun `all return reasons are accepted`() {
        for (reason in ReturnReason.entries) {
            val request = validRequest(reason = reason)
            val result = ReturnDomainValidator.validateReturnRequest(request)
            assertTrue("Reason $reason should be accepted", result is DomainResult.Success)
        }
    }

    @Test
    fun `optional challan id may be null`() {
        val request = validRequest().copy(originalChallanId = null)
        val result = ReturnDomainValidator.validateReturnRequest(request)
        assertTrue(result is DomainResult.Success)
    }

    // =========================================================================
    // Return creation — invalid cases
    // =========================================================================

    @Test
    fun `blank returnId is rejected by data class init`() {
        try {
            validRequest(returnId = "  ")
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Return ID"))
        }
    }

    @Test
    fun `blank projectId is rejected by data class init`() {
        try {
            validRequest(projectId = "")
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Project ID"))
        }
    }

    @Test
    fun `blank customerId is rejected by data class init`() {
        try {
            validRequest(customerId = "")
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Customer ID"))
        }
    }

    @Test
    fun `blank requestedBy is rejected by data class init`() {
        try {
            validRequest(requestedBy = "")
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Requested By"))
        }
    }

    @Test
    fun `version zero is rejected by data class init`() {
        try {
            validRequest().copy(version = 0L)
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Version"))
        }
    }

    @Test
    fun `updatedAt before createdAt is rejected by data class init`() {
        val now = System.currentTimeMillis()
        try {
            ReturnRequest(
                returnId = "RET-001",
                projectId = "PRJ-A",
                returnNo = "RN-001",
                customerId = "CUST-001",
                originalChallanId = null,
                status = ReturnStatus.REQUESTED,
                reason = ReturnReason.DAMAGED,
                requestedBy = "USER-01",
                requestedAt = now,
                createdAt = now,
                updatedAt = now - 1000L, // before createdAt
                version = 1L
            )
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Updated At"))
        }
    }

    // =========================================================================
    // Project isolation
    // =========================================================================

    @Test
    fun `project isolation passes when project ids match`() {
        val request = validRequest(projectId = "PRJ-A")
        val result = ReturnDomainValidator.validateProjectIsolation(request, "PRJ-A")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `project isolation fails when project ids differ`() {
        val request = validRequest(projectId = "PRJ-A")
        val result = ReturnDomainValidator.validateProjectIsolation(request, "PRJ-B")
        assertTrue("Cross-project access should be denied", result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("isolation"))
    }
}
