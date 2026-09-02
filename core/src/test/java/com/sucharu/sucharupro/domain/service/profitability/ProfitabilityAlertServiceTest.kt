package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.profitability.FakeProfitabilityAlertDataSource
import com.sucharu.sucharupro.data.repository.profitability.ProfitabilityAlertRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Comprehensive Unit and Integration Test Suite for Profitability Alerts & Management Action Engine.
 * Module 16 Step 09.
 */
class ProfitabilityAlertServiceTest {

    private val tenantId = "tenant-001"
    private val projectId = "tenant-001"

    private lateinit var fakeDataSource: FakeProfitabilityAlertDataSource
    private lateinit var repository: ProfitabilityAlertRepositoryImpl
    private lateinit var sourceCollector: FakeProfitabilityAlertSourceCollector
    private lateinit var service: ProfitabilityAlertServiceImpl

    @Before
    fun setUp() {
        fakeDataSource = FakeProfitabilityAlertDataSource()
        repository = ProfitabilityAlertRepositoryImpl(fakeDataSource)
        sourceCollector = FakeProfitabilityAlertSourceCollector()
        service = ProfitabilityAlertServiceImpl(repository, sourceCollector)
    }

    @Test
    fun testEvaluateAlerts_detectsLossMakingJobAndCalculatesFinancialImpact() = runBlocking {
        // Prepare mock payload with a loss-making job
        sourceCollector.payloadToReturn = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            jobs = listOf(
                JobProfitabilityEvaluationItem(
                    jobId = "JOB-001",
                    jobCode = "JB-101",
                    customerId = "CUST-01",
                    revenue = BigDecimal("10000.0000"),
                    actualCost = BigDecimal("12500.0000"),
                    grossProfit = BigDecimal("-2500.0000"),
                    grossMarginPercentage = BigDecimal("-25.0000"),
                    materialCost = BigDecimal("8000.0000"),
                    labourCost = BigDecimal("2000.0000"),
                    machineCost = BigDecimal("1000.0000"),
                    vendorCost = BigDecimal("1500.0000")
                )
            )
        )

        val result = service.evaluateAlerts(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            idempotencyKey = null,
            actorId = "admin-1",
            actorRole = "ADMIN"
        )

        assertTrue(result is DomainResult.Success)
        val snapshot = (result as DomainResult.Success).data
        assertTrue(snapshot.totalActiveAlerts >= 1)
        assertEquals(BigDecimal("2500.0000"), snapshot.totalUnresolvedFinancialImpact)

        val alertsRes = service.listAlerts(tenantId, projectId, dimension = ProfitabilityAlertDimension.JOB)
        assertTrue(alertsRes is DomainResult.Success)
        val alerts = (alertsRes as DomainResult.Success).data
        assertEquals(1, alerts.size)
        val alert = alerts.first()
        assertEquals(ProfitabilityAlertType.LOSS_MAKING, alert.alertType)
        assertEquals(ProfitabilityAlertSeverity.CRITICAL, alert.severity)
        assertEquals(ProfitabilityAlertStatus.DETECTED, alert.status)
        assertEquals(BigDecimal("2500.0000"), alert.financialImpact)
    }

    @Test
    fun testEvaluateAlerts_detectsProductMarginDeclineAndCostSpikes() = runBlocking {
        sourceCollector.payloadToReturn = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            products = listOf(
                ProductProfitabilityEvaluationItem(
                    productId = "PROD-10",
                    productCode = "PKG-BOX",
                    productName = "Corrugated Carton",
                    totalRevenue = BigDecimal("50000.0000"),
                    totalCost = BigDecimal("48000.0000"),
                    grossProfit = BigDecimal("2000.0000"),
                    grossMarginPercentage = BigDecimal("4.0000"),
                    unitCost = BigDecimal("48.0000"),
                    averageSellingPrice = BigDecimal("50.0000"),
                    totalUnits = 1000L
                )
            )
        )

        val result = service.evaluateAlerts(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            idempotencyKey = null,
            actorId = "admin-1",
            actorRole = "ADMIN"
        )
        assertTrue(result is DomainResult.Success)

        val alertsRes = service.listAlerts(tenantId, projectId, dimension = ProfitabilityAlertDimension.PRODUCT)
        assertTrue(alertsRes is DomainResult.Success)
        val alerts = (alertsRes as DomainResult.Success).data
        assertTrue(alerts.isNotEmpty())
        assertTrue(alerts.any { it.alertType == ProfitabilityAlertType.MARGIN_DECLINE })
    }

    @Test
    fun testEvaluateAlerts_detectsCustomerConcentrationAndVendorDependency() = runBlocking {
        sourceCollector.payloadToReturn = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            customers = listOf(
                CustomerProfitabilityEvaluationItem(
                    customerId = "CUST-99",
                    customerCode = "C-99",
                    customerName = "Dominant Client Corp",
                    totalRevenue = BigDecimal("100000.0000"),
                    totalCost = BigDecimal("95000.0000"),
                    grossProfit = BigDecimal("5000.0000"),
                    grossMarginPercentage = BigDecimal("5.0000"),
                    contributionMarginPercentage = BigDecimal("15.0000"),
                    revenueSharePercentage = BigDecimal("45.0000")
                )
            ),
            vendors = listOf(
                VendorProfitabilityEvaluationItem(
                    vendorId = "VEND-88",
                    vendorCode = "V-88",
                    vendorName = "Sole Paper Supplier Ltd",
                    totalSpend = BigDecimal("80000.0000"),
                    spendSharePercentage = BigDecimal("55.0000"),
                    costPressureScore = BigDecimal("75.0000"),
                    dependencyRiskScore = BigDecimal("85.0000")
                )
            )
        )

        val result = service.evaluateAlerts(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            idempotencyKey = null,
            actorId = "admin-1",
            actorRole = "ADMIN"
        )
        assertTrue(result is DomainResult.Success)

        val alertsRes = service.listAlerts(tenantId, projectId)
        assertTrue(alertsRes is DomainResult.Success)
        val alerts = (alertsRes as DomainResult.Success).data
        assertTrue(alerts.any { it.alertType == ProfitabilityAlertType.CUSTOMER_CONCENTRATION_RISK })
        assertTrue(alerts.any { it.alertType == ProfitabilityAlertType.VENDOR_DEPENDENCY_RISK })
    }

    @Test
    fun testDeduplication_incrementsOccurrenceCountOnRepeatedEvaluation() = runBlocking {
        val payload = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            jobs = listOf(
                JobProfitabilityEvaluationItem(
                    jobId = "JOB-002",
                    jobCode = "JB-102",
                    customerId = "CUST-02",
                    revenue = BigDecimal("5000.0000"),
                    actualCost = BigDecimal("7000.0000"),
                    grossProfit = BigDecimal("-2000.0000"),
                    grossMarginPercentage = BigDecimal("-40.0000")
                )
            )
        )
        sourceCollector.payloadToReturn = payload

        // Run 1
        service.evaluateAlerts(tenantId, projectId, "2026-M09", null, "admin-1", "ADMIN")
        var alerts = (service.listAlerts(tenantId, projectId) as DomainResult.Success).data
        assertEquals(1, alerts.size)
        assertEquals(1, alerts.first().occurrenceCount)
        assertFalse(alerts.first().isRecurring)

        // Run 2 (Same condition detected)
        service.evaluateAlerts(tenantId, projectId, "2026-M09", null, "admin-1", "ADMIN")
        alerts = (service.listAlerts(tenantId, projectId) as DomainResult.Success).data
        assertEquals(1, alerts.size) // No duplicate row
        val alert = alerts.first()
        assertEquals(2, alert.occurrenceCount) // Count incremented
        assertTrue(alert.isRecurring)

        val occurrences = (repository.listOccurrences(tenantId, alert.alertId) as DomainResult.Success).data
        assertEquals(2, occurrences.size)
    }

    @Test
    fun testLifecycleTransitions_acknowledge_resolve_and_reopen() = runBlocking {
        sourceCollector.payloadToReturn = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            jobs = listOf(
                JobProfitabilityEvaluationItem(
                    jobId = "JOB-003",
                    jobCode = "JB-103",
                    customerId = "CUST-03",
                    revenue = BigDecimal("2000.0000"),
                    actualCost = BigDecimal("3000.0000"),
                    grossProfit = BigDecimal("-1000.0000"),
                    grossMarginPercentage = BigDecimal("-50.0000")
                )
            )
        )
        service.evaluateAlerts(tenantId, projectId, "2026-M09", null, "admin-1", "ADMIN")
        val alertId = (service.listAlerts(tenantId, projectId) as DomainResult.Success).data.first().alertId

        // Acknowledge
        val ackRes = service.acknowledgeAlert(tenantId, projectId, alertId, "staff-1", "STAFF")
        assertTrue(ackRes is DomainResult.Success)
        assertEquals(ProfitabilityAlertStatus.ACKNOWLEDGED, (ackRes as DomainResult.Success).data.status)

        // Update to IN_REVIEW
        val inReviewRes = service.updateAlertStatus(tenantId, projectId, alertId, ProfitabilityAlertStatus.IN_REVIEW, null, "manager-1", "MANAGER")
        assertTrue(inReviewRes is DomainResult.Success)
        assertEquals(ProfitabilityAlertStatus.IN_REVIEW, (inReviewRes as DomainResult.Success).data.status)

        // Resolve
        val resRes = service.resolveAlert(tenantId, projectId, alertId, "Price adjusted for future orders", "manager-1", "MANAGER")
        assertTrue(resRes is DomainResult.Success)
        assertEquals(ProfitabilityAlertStatus.RESOLVED, (resRes as DomainResult.Success).data.status)

        // Reopen
        val reopenRes = service.reopenAlert(tenantId, projectId, alertId, "Customer still receiving discounted rates", "manager-1", "MANAGER")
        assertTrue(reopenRes is DomainResult.Success)
        assertEquals(ProfitabilityAlertStatus.REOPENED, (reopenRes as DomainResult.Success).data.status)

        // Audit Trail Check
        val audits = (service.listAuditEvents(tenantId, alertId) as DomainResult.Success).data
        assertTrue(audits.size >= 4)
    }

    @Test
    fun testIllegalLifecycleTransition_isRejected() = runBlocking {
        sourceCollector.payloadToReturn = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            jobs = listOf(
                JobProfitabilityEvaluationItem(
                    jobId = "JOB-004",
                    jobCode = "JB-104",
                    customerId = "CUST-04",
                    revenue = BigDecimal("1000.0000"),
                    actualCost = BigDecimal("2000.0000"),
                    grossProfit = BigDecimal("-1000.0000"),
                    grossMarginPercentage = BigDecimal("-100.0000")
                )
            )
        )
        service.evaluateAlerts(tenantId, projectId, "2026-M09", null, "admin-1", "ADMIN")
        val alertId = (service.listAlerts(tenantId, projectId) as DomainResult.Success).data.first().alertId

        // Directly resolve
        service.resolveAlert(tenantId, projectId, alertId, "Resolved", "manager-1", "MANAGER")

        // Try illegal transition: RESOLVED -> ACTION_IN_PROGRESS (Should fail)
        val illegalRes = service.updateAlertStatus(tenantId, projectId, alertId, ProfitabilityAlertStatus.ACTION_IN_PROGRESS, null, "manager-1", "MANAGER")
        assertTrue(illegalRes is DomainResult.Error)
    }

    @Test
    fun testManagementAction_priorityScoringAndOutcomeTracking() = runBlocking {
        val actionId = "act-101"
        val action = ProfitabilityManagementAction(
            actionId = actionId,
            alertId = "alert-99",
            tenantId = tenantId,
            projectId = projectId,
            actionCode = ManagementActionCode.REVIEW_CUSTOMER_PRICING,
            actionTitle = "Review Tier 1 Customer Pricing",
            actionDescription = "Increase base price by 15%",
            priorityScore = BigDecimal("82.5000"),
            status = ManagementActionStatus.PROPOSED,
            assignedTo = "sales-mgr",
            assignedBy = "admin-1",
            expectedFinancialImpact = BigDecimal("15000.0000"),
            integrityHash = "hash-101"
        )

        val createRes = service.createManagementAction(tenantId, projectId, action, "admin-1", "ADMIN")
        assertTrue(createRes is DomainResult.Success)

        // PROPOSED -> APPROVED
        val approveRes = service.updateActionStatus(
            tenantId = tenantId,
            projectId = projectId,
            actionId = actionId,
            newStatus = ManagementActionStatus.APPROVED,
            actorId = "admin-1",
            actorRole = "ADMIN"
        )
        assertTrue(approveRes is DomainResult.Success)

        // APPROVED -> IN_PROGRESS
        val progressRes = service.updateActionStatus(
            tenantId = tenantId,
            projectId = projectId,
            actionId = actionId,
            newStatus = ManagementActionStatus.IN_PROGRESS,
            actorId = "admin-1",
            actorRole = "ADMIN"
        )
        assertTrue(progressRes is DomainResult.Success)

        // IN_PROGRESS -> COMPLETED
        val completeRes = service.updateActionStatus(
            tenantId = tenantId,
            projectId = projectId,
            actionId = actionId,
            newStatus = ManagementActionStatus.COMPLETED,
            realizedImpact = BigDecimal("16500.0000"),
            outcomeNotes = "Negotiated 16.5k improvement",
            actorId = "admin-1",
            actorRole = "ADMIN"
        )
        assertTrue(completeRes is DomainResult.Success)
        val updated = (completeRes as DomainResult.Success).data
        assertEquals(ManagementActionStatus.COMPLETED, updated.status)
        assertEquals(BigDecimal("16500.0000"), updated.realizedFinancialImpact)
    }

    @Test
    fun testReconciliationAndCryptographicHandoffContract() = runBlocking {
        sourceCollector.payloadToReturn = ProfitabilityEvaluationPayload(
            tenantId = tenantId,
            projectId = projectId,
            periodId = "2026-M09",
            jobs = listOf(
                JobProfitabilityEvaluationItem(
                    jobId = "JOB-005",
                    jobCode = "JB-105",
                    customerId = "CUST-05",
                    revenue = BigDecimal("5000.0000"),
                    actualCost = BigDecimal("6000.0000"),
                    grossProfit = BigDecimal("-1000.0000"),
                    grossMarginPercentage = BigDecimal("-20.0000")
                )
            )
        )
        service.evaluateAlerts(tenantId, projectId, "2026-M09", null, "admin-1", "ADMIN")

        // Reconcile
        val reconRes = service.reconcileAlerts(tenantId, projectId)
        assertTrue(reconRes is DomainResult.Success)
        val assertion = (reconRes as DomainResult.Success).data
        assertTrue(assertion.isBalanced)
        assertTrue(assertion.openAlertsCountMatches)
        assertEquals(BigDecimal.ZERO.setScale(4), assertion.discrepancyAmount)

        // Handoff Contract
        val handoffRes = service.exportHandoffContract(tenantId, projectId)
        assertTrue(handoffRes is DomainResult.Success)
        val contract = (handoffRes as DomainResult.Success).data
        assertEquals(tenantId, contract.tenantId)
        assertEquals(1, contract.totalActiveAlerts)
        assertTrue(contract.handoffIntegrityHash.isNotBlank())
        assertEquals("1.0.0", contract.contractVersion)
    }
}

/**
 * Fake Source Collector for Unit Tests.
 */
class FakeProfitabilityAlertSourceCollector : ProfitabilityAlertSourceCollector {
    var payloadToReturn = ProfitabilityEvaluationPayload(
        tenantId = "tenant-001",
        projectId = "tenant-001",
        periodId = "2026-M09"
    )

    override suspend fun collectEvaluationPayload(tenantId: String, projectId: String, periodId: String?): DomainResult<ProfitabilityEvaluationPayload> {
        return DomainResult.Success(payloadToReturn)
    }
}
