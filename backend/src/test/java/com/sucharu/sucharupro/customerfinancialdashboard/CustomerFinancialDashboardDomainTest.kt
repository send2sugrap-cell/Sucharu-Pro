package com.sucharu.sucharupro.customerfinancialdashboard

import com.sucharu.sucharupro.domain.model.customercollection.CollectionPriority
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditRiskStatus
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerPaymentTermsType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerfinancialdashboard.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancialDashboardDomainTest {

    @Test
    fun testDashboardModelCreationAndKpiIntegrity() {
        val aging = CustomerReceivableAgingSummary(
            currentAmount = BigDecimal("10000.0000"),
            days1To7Amount = BigDecimal("5000.0000"),
            days8To30Amount = BigDecimal("0.0000"),
            days31To60Amount = BigDecimal("0.0000"),
            days61To90Amount = BigDecimal("0.0000"),
            days90PlusAmount = BigDecimal("0.0000"),
            totalAgingOutstanding = BigDecimal("15000.0000"),
            maxDaysOverdue = 5
        )

        val dueSchedule = CustomerDueScheduleSummary(
            upcomingDueAmount = BigDecimal("10000.0000"),
            dueTodayAmount = BigDecimal.ZERO,
            overdueAmount = BigDecimal("5000.0000"),
            criticalOverdueAmount = BigDecimal.ZERO,
            overdueInvoiceCount = 1
        )

        val collectionStatus = CustomerCollectionStatusSummary(
            priority = CollectionPriority.NORMAL,
            pendingActionCount = 1,
            completedActionCount = 0,
            activePromiseCount = 0,
            activePromisedAmount = BigDecimal.ZERO
        )

        val recon = CustomerReconciliationStatusSummary(
            isReconciled = true,
            discrepancyCount = 0,
            varianceAmount = BigDecimal.ZERO
        )

        val warning = CustomerFinancialWarning(
            warningType = FinancialWarningType.OVERDUE_RECEIVABLE,
            severity = CollectionPriority.NORMAL,
            title = "Overdue Invoices",
            message = "5 days overdue invoice detected"
        )

        val action = CustomerFinancialAction(
            actionType = FinancialActionType.REVIEW_COLLECTION,
            priority = CollectionPriority.NORMAL,
            title = "Initiate Collection Follow-up",
            description = "Contact customer",
            targetRoute = "/customers/CUS-01/collection"
        )

        val dashboard = CustomerFinancialDashboard(
            customerId = "CUS-01",
            tenantId = "TENANT-01",
            projectId = "PRJ-01",
            customerCode = "CUS-001",
            customerDisplayName = "ACME Corp",
            accountNumber = "CFA-001",
            accountStatus = CustomerFinancialAccountStatus.ACTIVE,
            totalInvoiced = BigDecimal("50000.0000"),
            totalPaid = BigDecimal("35000.0000"),
            totalAllocated = BigDecimal("35000.0000"),
            totalUnallocated = BigDecimal.ZERO,
            availableCreditBalance = BigDecimal.ZERO,
            outstandingReceivable = BigDecimal("15000.0000"),
            creditLimit = BigDecimal("100000.0000"),
            currentCreditExposure = BigDecimal("15000.0000"),
            availableCreditCapacity = BigDecimal("85000.0000"),
            paymentTerms = CustomerPaymentTermsType.NET_30,
            creditDays = 30,
            requiresAdvance = false,
            riskStatus = CustomerCreditRiskStatus.NORMAL,
            financialHold = false,
            holdReason = null,
            agingSummary = aging,
            dueSchedule = dueSchedule,
            collectionStatus = collectionStatus,
            reconciliationSummary = recon,
            warnings = listOf(warning),
            recommendedActions = listOf(action),
            recentActivity = emptyList()
        )

        assertEquals("CUS-01", dashboard.customerId)
        assertEquals(BigDecimal("15000.0000"), dashboard.outstandingReceivable)
        assertEquals(BigDecimal("85000.0000"), dashboard.availableCreditCapacity)
        assertEquals(1, dashboard.warnings.size)
        assertEquals(1, dashboard.recommendedActions.size)
    }

    @Test
    fun testFinancialWarningAndActionEnums() {
        assertTrue(FinancialWarningType.values().contains(FinancialWarningType.FINANCIAL_HOLD))
        assertTrue(FinancialWarningType.values().contains(FinancialWarningType.CREDIT_LIMIT_EXCEEDED))
        assertTrue(FinancialActionType.values().contains(FinancialActionType.REVIEW_HOLD))
        assertTrue(FinancialActionType.values().contains(FinancialActionType.ALLOCATE_PAYMENT))
        assertTrue(FinancialActivityType.values().contains(FinancialActivityType.INVOICE))
        assertTrue(FinancialActivityType.values().contains(FinancialActivityType.PAYMENT))
    }
}
