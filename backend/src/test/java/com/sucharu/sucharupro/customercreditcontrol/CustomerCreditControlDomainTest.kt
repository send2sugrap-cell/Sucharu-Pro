package com.sucharu.sucharupro.customercreditcontrol

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerPaymentTermsType
import com.sucharu.sucharupro.domain.model.customercreditcontrol.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.validation.customercreditcontrol.CustomerCreditControlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditControlDomainTest {

    private val sampleCustomer = Customer(
        customerId = "CUS-001",
        customerCode = "CUS-001",
        displayName = "Valid Customer",
        primaryPhone = "+8801700000001",
        customerType = CustomerType.BUSINESS,
        status = CustomerStatusType.ACTIVE,
        createdAt = "2026-08-29T00:00:00Z",
        updatedAt = "2026-08-29T00:00:00Z"
    )

    @Test
    fun testValidProfileValidation() {
        val res = CustomerCreditControlValidator.validateProfileCreationOrUpdate(
            sampleCustomer,
            BigDecimal("50000.0000"),
            CustomerPaymentTermsType.NET_30,
            30
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testNegativeCreditLimitRejected() {
        val res = CustomerCreditControlValidator.validateProfileCreationOrUpdate(
            sampleCustomer,
            BigDecimal("-1000.0000"),
            CustomerPaymentTermsType.NET_30,
            30
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testPrepaidWithPositiveCreditLimitRejected() {
        val res = CustomerCreditControlValidator.validateProfileCreationOrUpdate(
            sampleCustomer,
            BigDecimal("5000.0000"),
            CustomerPaymentTermsType.PREPAID,
            0
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testFinancialHoldValidationRequiresReason() {
        val resBlank = CustomerCreditControlValidator.validateFinancialHold(sampleCustomer, "", "user1")
        assertTrue(resBlank is DomainResult.Error)

        val resValid = CustomerCreditControlValidator.validateFinancialHold(sampleCustomer, "Excessive overdue", "user1")
        assertTrue(resValid is DomainResult.Success)
    }

    @Test
    fun testHoldReleaseValidationRequiresHoldActive() {
        val resNotOnHold = CustomerCreditControlValidator.validateHoldRelease(sampleCustomer, false, "Dispute resolved", "user1")
        assertTrue(resNotOnHold is DomainResult.Error)

        val resOnHold = CustomerCreditControlValidator.validateHoldRelease(sampleCustomer, true, "Dispute resolved", "user1")
        assertTrue(resOnHold is DomainResult.Success)
    }

    @Test
    fun testCreditCheckNegativeExposureRejected() {
        val res = CustomerCreditControlValidator.validateCreditCheck(sampleCustomer, BigDecimal("-500.0000"))
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testAgingBucketsEnumIntegrity() {
        assertEquals("Current", ReceivableAgingBucket.CURRENT.label)
        assertEquals("1–7 Days Overdue", ReceivableAgingBucket.DAYS_1_7.label)
        assertEquals("8–30 Days Overdue", ReceivableAgingBucket.DAYS_8_30.label)
        assertEquals("31–60 Days Overdue", ReceivableAgingBucket.DAYS_31_60.label)
        assertEquals("61–90 Days Overdue", ReceivableAgingBucket.DAYS_61_90.label)
        assertEquals("90+ Days Overdue", ReceivableAgingBucket.DAYS_90_PLUS.label)
    }
}
