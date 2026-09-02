package com.sucharu.sucharupro.domain.validation.returns

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnReceivingValidatorTest {

    private fun validReturnRequest(): ReturnRequest = ReturnRequest(
        returnId = "r123",
        projectId = "p1",
        returnNo = "RN-001",
        customerId = "c1",
        originalChallanId = null,
        status = ReturnStatus.APPROVED,
        reason = com.sucharu.sucharupro.domain.model.returns.ReturnReason.OTHER,
        description = null,
        requestedBy = "user1",
        // timestamps and version are auto‑filled with defaults in the data class init
    )

    private fun validReceivingInfo(request: ReturnRequest): ReturnReceivingInfo = ReturnReceivingInfo(
        receivingEventId = "ev123",
        returnId = request.returnId,
        projectId = request.projectId,
        receiverId = "receiver1",
        receivedAt = System.currentTimeMillis(),
        approvedQty = 10,
        actualQty = 10,
        acceptedQty = 8,
        rejectedQty = 1,
        damagedQty = 1,
        mismatchFlag = false,
        condition = null,
        packaging = null,
        damageNotes = null,
        version = 1,
        idempotencyKey = "idem-key"
    )

    @Test
    fun `valid approved return receiving passes`() {
        val request = validReturnRequest()
        val receiving = validReceivingInfo(request)
        val result = ReturnReceivingValidator.validate(request, receiving)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `non‑APPROVED return is rejected`() {
        val request = validReturnRequest().copy(status = ReturnStatus.REQUESTED)
        val receiving = validReceivingInfo(request)
        val result = ReturnReceivingValidator.validate(request, receiving)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `actual quantity greater than approved fails`() {
        val request = validReturnRequest()
        val receiving = validReceivingInfo(request).copy(
            approvedQty = 5,
            actualQty = 6,
            acceptedQty = 6,
            rejectedQty = 0,
            damagedQty = 0
        )
        val result = ReturnReceivingValidator.validate(request, receiving)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `quantity breakdown mismatch fails`() {
        val request = validReturnRequest()
        val receiving = validReceivingInfo(request).copy(
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 5,
            rejectedQty = 2,
            damagedQty = 2 // sum = 9, not 10
        )
        val result = ReturnReceivingValidator.validate(request, receiving)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `negative approved quantity fails`() {
        val request = validReturnRequest()
        val receiving = validReceivingInfo(request).copy(approvedQty = -1)
        val result = ReturnReceivingValidator.validate(request, receiving)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `blank receiver id fails`() {
        val request = validReturnRequest()
        val receiving = validReceivingInfo(request).copy(receiverId = "")
        val result = ReturnReceivingValidator.validate(request, receiving)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `invalid receivedAt timestamp fails`() {
        val request = validReturnRequest()
        val receiving = validReceivingInfo(request).copy(receivedAt = 0L)
        val result = ReturnReceivingValidator.validate(request, receiving)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `blank idempotency key fails`() {
        val request = validReturnRequest()
        val receiving = validReceivingInfo(request).copy(idempotencyKey = "")
        val result = ReturnReceivingValidator.validate(request, receiving)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `project isolation mismatch fails`() {
        val request = validReturnRequest()
        val receiving = validReceivingInfo(request).copy(projectId = "differentProject")
        val result = ReturnReceivingValidator.validate(request, receiving)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `customer ownership violation fails`() {
        val request = validReturnRequest()
        val receiving = validReceivingInfo(request)
        // Use a callerCustomerId that does not match the request.customerId
        val result = ReturnReceivingValidator.validate(
            request,
            receiving,
            callerCustomerId = "otherCustomer"
        )
        assertTrue(result is DomainResult.Error)
    }
}
