package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.repository.profitability.ExecutiveProfitabilityRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.ExecutiveProfitabilityValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode

class ExecutiveProfitabilityServiceImpl(
    private val repository: ExecutiveProfitabilityRepository,
    private val sourceCollector: ExecutiveProfitabilitySourceCollector,
    private val kpiEngine: ExecutiveKpiEngine = ExecutiveKpiEngineImpl(),
    private val scorecardEngine: ExecutiveScorecardEngine = ExecutiveScorecardEngineImpl(),
    private val rankingEngine: ExecutiveRankingEngine = ExecutiveRankingEngineImpl(),
    private val priorityEngine: ExecutivePriorityEngine = ExecutivePriorityEngineImpl(),
    private val reportEngine: ExecutiveReportEngine = ExecutiveReportEngineImpl(),
    private val reconciliationService: ExecutiveProfitabilityReconciliationService = ExecutiveProfitabilityReconciliationServiceImpl()
) : ExecutiveProfitabilityService {

    private val calculationMutex = Mutex()
    private val ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
    private val ONE_HUNDRED = BigDecimal("100.0000").setScale(4, RoundingMode.HALF_UP)

    override suspend fun calculateSnapshot(
        tenantId: String,
        projectId: String,
        periodId: String?,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<ExecutiveProfitabilitySnapshot> = calculationMutex.withLock {
        val tenantVal = ExecutiveProfitabilityValidator.validateTenantContext(tenantId, projectId)
        if (tenantVal is DomainResult.Error) return tenantVal
        val periodVal = ExecutiveProfitabilityValidator.validatePeriodId(periodId)
        if (periodVal is DomainResult.Error) return periodVal

        // 1. Collect Canonical Upstream Payloads
        val currentPayloadRes = sourceCollector.collectCurrentPayload(tenantId, projectId, periodId)
        if (currentPayloadRes is DomainResult.Error) return currentPayloadRes
        val currentPayload = (currentPayloadRes as DomainResult.Success).data

        val prevPayloadRes = sourceCollector.collectPreviousPayload(tenantId, projectId, periodId)
        val prevPayload = if (prevPayloadRes is DomainResult.Success) prevPayloadRes.data else null

        val forecastRes = sourceCollector.collectForecastSnapshot(tenantId, projectId, periodId)
        val forecastSnapshot = if (forecastRes is DomainResult.Success) forecastRes.data else null

        val alertSnapshotRes = sourceCollector.collectAlertSnapshot(tenantId, projectId, periodId)
        val alertSnapshot = if (alertSnapshotRes is DomainResult.Success) alertSnapshotRes.data else null

        val activeAlertsRes = sourceCollector.collectActiveAlerts(tenantId, projectId)
        val activeAlerts = if (activeAlertsRes is DomainResult.Success) activeAlertsRes.data else emptyList()

        val actionsRes = sourceCollector.collectManagementActions(tenantId, projectId)
        val actions = if (actionsRes is DomainResult.Success) actionsRes.data else emptyList()

        val leakageRes = sourceCollector.collectLeakageSummary(tenantId, projectId, periodId)
        val leakageSummary = if (leakageRes is DomainResult.Success) leakageRes.data else ExecutiveLeakageSummary(
            totalLeakageAmount = ZERO,
            leakagePercentageOfRevenue = ZERO,
            directMaterialWastageLeakage = ZERO,
            reworkCostLeakage = ZERO,
            unallocatedOverheadLeakage = ZERO,
            pricingErosionLeakage = ZERO,
            vendorCostSurgeLeakage = ZERO,
            topLeakageItems = emptyList(),
            primaryMitigationRecommendation = "Zero detected cost leakage."
        )

        val driversRes = sourceCollector.collectProfitabilityDrivers(tenantId, projectId, periodId)
        val drivers = if (driversRes is DomainResult.Success) driversRes.data else emptyList()

        // 2. Compute Analytics Engines
        val kpis = kpiEngine.computeKpis(currentPayload, prevPayload, forecastSnapshot, alertSnapshot, leakageSummary)
        val rankings = rankingEngine.computeRankings(currentPayload)
        val concentration = rankingEngine.computeConcentration(currentPayload)
        val scorecard = scorecardEngine.computeScorecard(kpis, currentPayload, alertSnapshot, forecastSnapshot, concentration)
        val priorities = priorityEngine.computePriorities(activeAlerts, actions, forecastSnapshot, leakageSummary.topLeakageItems, concentration)

        // 3. Financial Totals
        val totalGrossRev = currentPayload.jobs.sumOf { it.revenue }.setScale(4, RoundingMode.HALF_UP)
        val totalNetRev = totalGrossRev // Net revenue equals gross in analytical projection
        val totalActualCost = currentPayload.jobs.sumOf { it.actualCost }.setScale(4, RoundingMode.HALF_UP)
        val totalGp = totalGrossRev.subtract(totalActualCost).setScale(4, RoundingMode.HALF_UP)
        val gpMargin = ExecutiveProfitabilityMathUtils.calculateMargin(totalGp, totalGrossRev)

        val totalContrib = currentPayload.customers.sumOf { it.totalRevenue.multiply(it.contributionMarginPercentage).divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP) }.setScale(4, RoundingMode.HALF_UP)
        val contribMargin = if (totalGrossRev > ZERO) totalContrib.multiply(ONE_HUNDRED).divide(totalGrossRev, 4, RoundingMode.HALF_UP) else ZERO

        val activeAlertsCount = alertSnapshot?.totalActiveAlerts ?: 0
        val critAlertsCount = alertSnapshot?.criticalAlertCount ?: 0
        val pendingActionsCount = actions.count { it.status != ManagementActionStatus.COMPLETED && it.status != ManagementActionStatus.VERIFIED && it.status != ManagementActionStatus.CANCELLED }

        // 4. Deterministic Fingerprint & Idempotency Check
        val fp = ExecutiveProfitabilityMathUtils.generateExecutiveSnapshotFingerprint(tenantId, projectId, periodId, totalGrossRev, totalActualCost, totalGp)
        val existingRes = repository.findSnapshotByFingerprint(tenantId, fp)
        if (existingRes is DomainResult.Success && existingRes.data != null) {
            return DomainResult.Success(existingRes.data!!)
        }

        val now = System.currentTimeMillis()
        val snapshotId = "snp-exec-$tenantId-${periodId ?: "ALL"}-$fp".take(64)
        val hash = ExecutiveProfitabilityMathUtils.generateExecutiveSnapshotIntegrityHash(
            snapshotId, tenantId, projectId, periodId, totalGrossRev, totalActualCost, totalGp, scorecard.overallScore, scorecard.classification, fp
        )

        // 5. Reconcile
        val reconRes = reconciliationService.reconcile(tenantId, projectId, periodId, snapshotId, kpis, currentPayload, forecastSnapshot, alertSnapshot)
        if (reconRes is DomainResult.Success) {
            repository.saveReconciliation(reconRes.data)
        }

        val snapshot = ExecutiveProfitabilitySnapshot(
            snapshotId = snapshotId,
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            generatedAt = now,
            totalGrossRevenue = totalGrossRev,
            totalNetRevenue = totalNetRev,
            totalActualCost = totalActualCost,
            totalGrossProfit = totalGp,
            grossMarginPercentage = gpMargin,
            totalContributionAmount = totalContrib,
            contributionMarginPercentage = contribMargin,
            forecastRevenue = forecastSnapshot?.projectedRevenue,
            forecastGrossProfit = forecastSnapshot?.projectedGrossProfit,
            forecastGrossMargin = forecastSnapshot?.projectedGrossMarginPercentage,
            activeAlertsCount = activeAlertsCount,
            criticalAlertsCount = critAlertsCount,
            pendingActionsCount = pendingActionsCount,
            overallHealth = scorecard.classification,
            overallScore = scorecard.overallScore,
            scorecardJson = "{}",
            kpisJson = "[]",
            rankingsJson = "{}",
            prioritiesJson = "[]",
            concentrationJson = "{}",
            driversJson = "[]",
            leakageJson = "{}",
            reconciliationJson = "{}",
            sourceFingerprint = fp,
            integrityHash = hash,
            calculationVersion = "1.0.0"
        )

        // Save Snapshot
        val saveRes = repository.saveSnapshot(snapshot)
        if (saveRes is DomainResult.Error) return saveRes

        // Save Provenance Records for Key Metrics
        kpis.forEach { kpi ->
            val provId = "prv-exec-$snapshotId-${kpi.kpiKey}".take(64)
            val provHash = ExecutiveProfitabilityMathUtils.sha256("$provId:$tenantId:${kpi.kpiKey}:${kpi.currentValue}:$now")
            repository.saveProvenance(
                ExecutiveProvenanceRecord(
                    provenanceId = provId,
                    snapshotId = snapshotId,
                    tenantId = tenantId,
                    kpiOrSectionKey = kpi.kpiKey,
                    sourceModule = "Module 16",
                    sourceStep = "Step 10",
                    sourceEntityType = "EXECUTIVE_KPI",
                    sourceEntityId = kpi.kpiKey,
                    sourceSnapshotId = snapshotId,
                    metricKey = kpi.kpiKey,
                    metricValue = kpi.currentValue,
                    calculationTimestamp = now,
                    provenanceHash = provHash
                )
            )
        }

        return DomainResult.Success(snapshot)
    }

    override suspend fun getLatestSnapshot(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveProfitabilitySnapshot> {
        val existing = repository.findLatestSnapshot(tenantId, projectId, periodId)
        if (existing is DomainResult.Success && existing.data != null) {
            return DomainResult.Success(existing.data!!)
        }
        // Auto-calculate if not present
        return calculateSnapshot(tenantId, projectId, periodId)
    }

    override suspend fun getSnapshotById(tenantId: String, snapshotId: String): DomainResult<ExecutiveProfitabilitySnapshot> {
        return repository.findSnapshotById(tenantId, snapshotId)
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, limit: Int): DomainResult<List<ExecutiveProfitabilitySnapshot>> {
        return repository.listSnapshots(tenantId, projectId, limit)
    }

    override suspend fun getKpis(tenantId: String, projectId: String, periodId: String?): DomainResult<List<ExecutiveKpi>> {
        val currentPayload = when (val res = sourceCollector.collectCurrentPayload(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Loading
        }
        val prevPayload = when (val res = sourceCollector.collectPreviousPayload(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> null
            DomainResult.Loading -> null
        }
        val fc = when (val res = sourceCollector.collectForecastSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> null
            DomainResult.Loading -> null
        }
        val alt = when (val res = sourceCollector.collectAlertSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> null
            DomainResult.Loading -> null
        }
        val lk = when (val res = sourceCollector.collectLeakageSummary(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> null
            DomainResult.Loading -> null
        }
        return DomainResult.Success(kpiEngine.computeKpis(currentPayload, prevPayload, fc, alt, lk))
    }

    override suspend fun getScorecard(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveManagementScorecard> {
        val kpis = when (val res = getKpis(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Loading
        }
        val payload = when (val res = sourceCollector.collectCurrentPayload(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Loading
        }
        val alt = when (val res = sourceCollector.collectAlertSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> null
            DomainResult.Loading -> null
        }
        val fc = when (val res = sourceCollector.collectForecastSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> null
            DomainResult.Loading -> null
        }
        val conc = rankingEngine.computeConcentration(payload)
        return DomainResult.Success(scorecardEngine.computeScorecard(kpis, payload, alt, fc, conc))
    }

    override suspend fun getRankings(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveRankingsPayload> {
        val payload = when (val res = sourceCollector.collectCurrentPayload(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Loading
        }
        return DomainResult.Success(rankingEngine.computeRankings(payload))
    }

    override suspend fun getPriorities(tenantId: String, projectId: String, periodId: String?): DomainResult<List<ExecutivePriorityItem>> {
        val activeAlerts = when (val res = sourceCollector.collectActiveAlerts(tenantId, projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> emptyList()
            DomainResult.Loading -> emptyList()
        }
        val actions = when (val res = sourceCollector.collectManagementActions(tenantId, projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> emptyList()
            DomainResult.Loading -> emptyList()
        }
        val fc = when (val res = sourceCollector.collectForecastSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> null
            DomainResult.Loading -> null
        }
        val lk = when (val res = sourceCollector.collectLeakageSummary(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> null
            DomainResult.Loading -> null
        }
        val payload = when (val res = sourceCollector.collectCurrentPayload(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Loading
        }
        val conc = rankingEngine.computeConcentration(payload)
        return DomainResult.Success(priorityEngine.computePriorities(activeAlerts, actions, fc, lk?.topLeakageItems ?: emptyList(), conc))
    }

    override suspend fun getDrivers(tenantId: String, projectId: String, periodId: String?): DomainResult<List<ExecutiveProfitabilityDriver>> {
        return sourceCollector.collectProfitabilityDrivers(tenantId, projectId, periodId)
    }

    override suspend fun getLeakages(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveLeakageSummary> {
        return sourceCollector.collectLeakageSummary(tenantId, projectId, periodId)
    }

    override suspend fun getConcentration(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveConcentrationSummary> {
        val payload = when (val res = sourceCollector.collectCurrentPayload(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Loading
        }
        return DomainResult.Success(rankingEngine.computeConcentration(payload))
    }

    override suspend fun getReconciliation(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveReconciliationResult> {
        val latest = repository.findLatestReconciliation(tenantId, projectId, periodId)
        if (latest is DomainResult.Success && latest.data != null) {
            return DomainResult.Success(latest.data!!)
        }
        // If not found, trigger calculateSnapshot to generate and save reconciliation
        val snapRes = getLatestSnapshot(tenantId, projectId, periodId)
        if (snapRes is DomainResult.Error) return snapRes
        val snap = (snapRes as DomainResult.Success).data
        val kpis = (getKpis(tenantId, projectId, periodId) as DomainResult.Success).data
        val payload = (sourceCollector.collectCurrentPayload(tenantId, projectId, periodId) as DomainResult.Success).data
        val fc = when (val res = sourceCollector.collectForecastSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            else -> null
        }
        val alt = when (val res = sourceCollector.collectAlertSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            else -> null
        }
        return reconciliationService.reconcile(tenantId, projectId, periodId, snap.snapshotId, kpis, payload, fc, alt)
    }

    override suspend fun getProvenance(tenantId: String, snapshotId: String): DomainResult<List<ExecutiveProvenanceRecord>> {
        return repository.listProvenance(tenantId, snapshotId)
    }

    override suspend fun getFullReport(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveProfitabilityReport> {
        val kpis = when (val res = getKpis(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Loading
        }
        val payload = when (val res = sourceCollector.collectCurrentPayload(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Loading
        }
        val rankings = rankingEngine.computeRankings(payload)
        val conc = rankingEngine.computeConcentration(payload)
        val fc = when (val res = sourceCollector.collectForecastSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            else -> null
        }
        val alt = when (val res = sourceCollector.collectAlertSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            else -> null
        }
        val scorecard = scorecardEngine.computeScorecard(kpis, payload, alt, fc, conc)
        val priorities = (getPriorities(tenantId, projectId, periodId) as DomainResult.Success).data
        val leakage = (getLeakages(tenantId, projectId, periodId) as DomainResult.Success).data
        val drivers = (getDrivers(tenantId, projectId, periodId) as DomainResult.Success).data
        val recon = (getReconciliation(tenantId, projectId, periodId) as DomainResult.Success).data

        return DomainResult.Success(
            reportEngine.buildReport(
                tenantId, projectId, periodId, kpis, scorecard, rankings, priorities, conc, leakage, drivers, fc, alt, recon
            )
        )
    }

    override suspend fun exportHandoffContract(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): DomainResult<Module16Step10ExecutiveProfitabilityHandoffContract> {
        val kpis = when (val res = getKpis(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Loading
        }
        val payload = when (val res = sourceCollector.collectCurrentPayload(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Loading
        }
        val conc = rankingEngine.computeConcentration(payload)
        val fc = when (val res = sourceCollector.collectForecastSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            else -> null
        }
        val alt = when (val res = sourceCollector.collectAlertSnapshot(tenantId, projectId, periodId)) {
            is DomainResult.Success -> res.data
            else -> null
        }
        val scorecard = scorecardEngine.computeScorecard(kpis, payload, alt, fc, conc)
        val priorities = (getPriorities(tenantId, projectId, periodId) as DomainResult.Success).data
        val leakage = (getLeakages(tenantId, projectId, periodId) as DomainResult.Success).data
        val drivers = (getDrivers(tenantId, projectId, periodId) as DomainResult.Success).data
        val recon = (getReconciliation(tenantId, projectId, periodId) as DomainResult.Success).data

        val now = System.currentTimeMillis()
        val handoffId = "hnd-exec-$tenantId-${periodId ?: "ALL"}-$now".take(64)
        val hash = ExecutiveProfitabilityMathUtils.generateHandoffIntegrityHash(
            handoffId, tenantId, projectId, periodId, scorecard.overallScore, scorecard.classification, now
        )

        return DomainResult.Success(
            Module16Step10ExecutiveProfitabilityHandoffContract(
                handoffId = handoffId,
                tenantId = tenantId,
                projectId = projectId,
                periodId = periodId,
                generatedAt = now,
                contractVersion = "1.0.0",
                overallHealth = scorecard.classification,
                overallScorecardScore = scorecard.overallScore,
                keyExecutiveKpis = kpis,
                topProfitabilityDrivers = drivers,
                leakageSummary = leakage,
                concentrationRisks = conc,
                topPriorityDecisions = priorities,
                forecastSummary = fc,
                alertMonitoringSummary = alt,
                reconciliationStatus = recon,
                sourceSnapshotReferences = listOf("STEP_01", "STEP_02", "STEP_03", "STEP_04", "STEP_05", "STEP_06", "STEP_07", "STEP_08", "STEP_09"),
                isReadOnly = true,
                handoffIntegrityHash = hash
            )
        )
    }
}
