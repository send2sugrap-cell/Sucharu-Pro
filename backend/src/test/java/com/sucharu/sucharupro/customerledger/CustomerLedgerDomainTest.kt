package com.sucharu.sucharupro.customerledger

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerledger.CustomerLedgerEntry
import com.sucharu.sucharupro.domain.model.customerledger.CustomerLedgerEntryType
import com.sucharu.sucharupro.domain.validation.customerledger.CustomerLedgerValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class CustomerLedgerDomainTest {

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-LED-001"
    private val accountId = "CFA-LED-001"

    private val validAccount = CustomerFinancialAccount(
        financialAccountId = accountId,
        tenantId = tenantId,
        projectId = projectId,
        customerId = customerId,
        accountNumber = "CFA-10001",
        status = CustomerFinancialAccountStatus.ACTIVE
    )

    @Test
    fun testEntryTypeDebitCreditClassifications() {
        assertTrue(CustomerLedgerEntryType.INVOICE.isDebit)
        assertTrue(CustomerLedgerEntryType.DEBIT_ADJUSTMENT.isDebit)
        assertTrue(CustomerLedgerEntryType.REFUND.isDebit)

        assertTrue(CustomerLedgerEntryType.PAYMENT.isCredit)
        assertTrue(CustomerLedgerEntryType.ADVANCE.isCredit)
        assertTrue(CustomerLedgerEntryType.CREDIT_ADJUSTMENT.isCredit)

        assertFalse(CustomerLedgerEntryType.INVOICE.isCredit)
        assertFalse(CustomerLedgerEntryType.PAYMENT.isDebit)
    }

    @Test
    fun testRunningBalanceEquationDeterministic() {
        // Step 1: Invoice of 10,000 (Debit)
        var balance = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        balance = balance.add(BigDecimal("10000.0000")).subtract(BigDecimal.ZERO)
        assertEquals(BigDecimal("10000.0000"), balance)

        // Step 2: Payment of 4,000 (Credit)
        balance = balance.add(BigDecimal.ZERO).subtract(BigDecimal("4000.0000"))
        assertEquals(BigDecimal("6000.0000"), balance)

        // Step 3: Advance of 2,000 (Credit)
        balance = balance.add(BigDecimal.ZERO).subtract(BigDecimal("2000.0000"))
        assertEquals(BigDecimal("4000.0000"), balance)

        // Step 4: Debit Adjustment of 500 (Debit)
        balance = balance.add(BigDecimal("500.0000")).subtract(BigDecimal.ZERO)
        assertEquals(BigDecimal("4500.0000"), balance)

        // Step 5: Credit Adjustment of 1000 (Credit)
        balance = balance.add(BigDecimal.ZERO).subtract(BigDecimal("1000.0000"))
        assertEquals(BigDecimal("3500.0000"), balance)

        // Step 6: Refund of 500 (Debit)
        balance = balance.add(BigDecimal("500.0000")).subtract(BigDecimal.ZERO)
        assertEquals(BigDecimal("4000.0000"), balance)
    }

    @Test
    fun testValidatorStatementQuery_Validations() {
        // Valid query
        val validRes = CustomerLedgerValidator.validateStatementQuery(
            tenantId, projectId, customerId, 1000L, 2000L, validAccount
        )
        assertTrue(validRes is DomainResult.Success)

        // From date > To date
        val invalidDateRes = CustomerLedgerValidator.validateStatementQuery(
            tenantId, projectId, customerId, 3000L, 2000L, validAccount
        )
        assertTrue(invalidDateRes is DomainResult.Error)

        // Missing account
        val missingAccountRes = CustomerLedgerValidator.validateStatementQuery(
            tenantId, projectId, customerId, 1000L, 2000L, null
        )
        assertTrue(missingAccountRes is DomainResult.Error)
    }

    @Test
    fun testValidatorPagination_Validations() {
        val validPage = CustomerLedgerValidator.validatePagination(50, 0)
        assertTrue(validPage is DomainResult.Success)

        val invalidLimit = CustomerLedgerValidator.validatePagination(0, 0)
        assertTrue(invalidLimit is DomainResult.Error)

        val negativeOffset = CustomerLedgerValidator.validatePagination(50, -5)
        assertTrue(negativeOffset is DomainResult.Error)
    }
}
