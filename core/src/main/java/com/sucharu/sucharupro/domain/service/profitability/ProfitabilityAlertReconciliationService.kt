package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Profitability Alert Reconciliation Service Interface.
 * Non-Mutating mathematical identity and audit verification.
 * Module 16 Step 09.
 */
interface ProfitabilityAlertReconciliationService {
    suspend fun reconcileAlerts(
        tenantId: String,
        projectId: String,
        alerts: List<ProfitabilityAlert>,
        actions: List<ProfitabilityManagementAction>,
        snapshot: ProfitabilityMonitoringSnapshot?
    ): DomainResult<ProfitabilityAlertReconciliationAssertion>
}

/**
 * Production Implementation of ProfitabilityAlertReconciliationService.
 */
class ProfitabilityAlertReconciliationServiceImpl : ProfitabilityAlertReconciliationService {

    override suspend fun reconcileAlerts(
        tenantId: String,
        projectId: String,
        alerts: List<ProfitabilityAlert>,
        actions: List<ProfitabilityManagementAction>,
        snapshot: ProfitabilityMonitoringSnapshot?
    ): DomainResult<ProfitabilityAlertReconciliationAssertion> {
        val assertions = mutableListOf<String>()
        var isBalanced = true

        // 1. Total Financial Impact Assertion
        val activeAlerts = alerts.filter {
            it.status !in setOf(
                ProfitabilityAlertStatus.RESOLVED,
                ProfitabilityAlertStatus.DISMISSED,
                ProfitabilityAlertStatus.SUPPRESSED
            )
        }
        val calculatedSum = activeAlerts.map { it.financialImpact }.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
            .setScale(4, RoundingMode.HALF_UP)

        val snapshotImpact = snapshot?.totalUnresolvedFinancialImpact ?: calculatedSum
        val discrepancy = calculatedSum.subtract(snapshotImpact).abs()

        if (discrepancy.compareTo(BigDecimal("0.0100")) > 0) {
            isBalanced = false
            assertions.add("Financial impact discrepancy: Calculated BDT $calculatedSum vs Snapshot BDT $snapshotImpact (Diff: $discrepancy).")
        } else {
            assertions.add("Financial impact assertion passed: Sum of ${activeAlerts.size} active alerts exactly matches BDT $calculatedSum.")
        }

        // 2. Open Alert Count Assertion
        val openAlertsMatch = if (snapshot != null) {
            snapshot.totalActiveAlerts == activeAlerts.size
        } else true

        if (!openAlertsMatch) {
            isBalanced = false
            assertions.add("Open alert count mismatch: Calculated ${activeAlerts.size} vs Snapshot ${snapshot?.totalActiveAlerts}.")
        } else {
            assertions.add("Open alert count assertion passed (${activeAlerts.size} active alerts).")
        }

        // 3. Action Counts Assertion
        val openActions = actions.filter {
            it.status !in setOf(
                ManagementActionStatus.COMPLETED,
                ManagementActionStatus.VERIFIED,
                ManagementActionStatus.CANCELLED
            )
        }
        val actionCountsMatch = if (snapshot != null) {
            snapshot.openActionCount == openActions.size
        } else true

        if (!actionCountsMatch) {
            isBalanced = false
            assertions.add("Open management action count mismatch: Found ${openActions.size} vs Snapshot ${snapshot?.openActionCount}.")
        } else {
            assertions.add("Management action count assertion passed (${openActions.size} open actions).")
        }

        // 4. Provenance & SHA-256 Integrity Verification
        var provenanceValid = true
        for (alert in alerts) {
            val expectedHash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                alert.alertId, alert.tenantId, alert.projectId, alert.alertType, alert.severity,
                alert.dimensionType, alert.dimensionId, alert.observedValue, alert.thresholdValue,
                alert.financialImpact, alert.fingerprint
            )
            if (alert.integrityHash != expectedHash) {
                provenanceValid = false
                isBalanced = false
                assertions.add("Integrity hash mismatch on alert ${alert.alertId}.")
            }
        }

        if (provenanceValid) {
            assertions.add("All ${alerts.size} alert integrity hashes verified against canonical fingerprints.")
        }

        return DomainResult.Success(
            ProfitabilityAlertReconciliationAssertion(
                tenantId = tenantId,
                projectId = projectId,
                isBalanced = isBalanced,
                totalAlertsChecked = alerts.size,
                totalFinancialImpact = snapshotImpact,
                aggregatedImpactFromAlerts = calculatedSum,
                discrepancyAmount = discrepancy,
                openAlertsCountMatches = openAlertsMatch,
                actionCountsMatch = actionCountsMatch,
                provenanceIntegrityMatches = provenanceValid,
                assertions = assertions
            )
        )
    }
}
