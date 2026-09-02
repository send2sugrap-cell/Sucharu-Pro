package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.repository.profitability.ProfitabilityForecastRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.ProfitabilityForecastValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Production-Grade Profitability Forecasting & Scenario Orchestration Service.
 * Module 16 Step 08.
 */
class ProfitabilityForecastServiceImpl(
    private val repository: ProfitabilityForecastRepository,
    private val sourceCollector: ProfitabilityForecastSourceCollector,
    private val forecastEngine: ProfitabilityForecastEngine = ProfitabilityForecastEngineImpl(),
    private val scenarioEngine: ProfitabilityScenarioEngine = ProfitabilityScenarioEngineImpl(),
    private val confidenceAndRiskEngine: ProfitabilityForecastConfidenceAndRiskEngine = ProfitabilityForecastConfidenceAndRiskEngineImpl(),
    private val insightEngine: ProfitabilityForecastInsightEngine = ProfitabilityForecastInsightEngineImpl(),
    private val reconciliationService: ProfitabilityForecastReconciliationService = ProfitabilityForecastReconciliationServiceImpl()
) : ProfitabilityForecastService {

    private val locks = ConcurrentHashMap<String, Mutex>()

    private fun getLock(tenantId: String, scope: ProfitabilityForecastScope, targetEntityId: String): Mutex {
        val key = "$tenantId:${scope.name}:$targetEntityId"
        return locks.computeIfAbsent(key) { Mutex() }
    }

    override suspend fun generateForecast(
        tenantId: String,
        projectId: String,
        targetScope: ProfitabilityForecastScope,
        targetEntityId: String,
        targetEntityLabel: String,
        historicalPeriodStart: String,
        historicalPeriodEnd: String,
        forecastPeriodStart: String,
        forecastPeriodEnd: String,
        horizon: ForecastHorizon,
        forecastMethod: ProfitabilityForecastMethod,
        scenarioType: ProfitabilityScenarioType,
        scenarioId: String?,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityForecastSnapshot> {
        val valResult = ProfitabilityForecastValidator.validateTenantAndProject(tenantId, projectId)
        if (valResult is DomainResult.Error) return valResult

        val targetVal = ProfitabilityForecastValidator.validateForecastTarget(targetScope, targetEntityId)
        if (targetVal is DomainResult.Error) return targetVal

        val rangeVal = ProfitabilityForecastValidator.validatePeriodRange(historicalPeriodStart, historicalPeriodEnd, forecastPeriodStart, forecastPeriodEnd)
        if (rangeVal is DomainResult.Error) return rangeVal

        // 1. Idempotency Check
        if (!idempotencyKey.isNullOrBlank()) {
            when (val cachedForecastId = repository.checkIdempotency(tenantId, idempotencyKey)) {
                is DomainResult.Success -> {
                    if (cachedForecastId.data != null) {
                        val existing = repository.getSnapshotById(tenantId, cachedForecastId.data)
                        if (existing is DomainResult.Success && existing.data != null) {
                            return DomainResult.Success(existing.data)
                        }
                    }
                }
                is DomainResult.Error -> {}
                DomainResult.Loading -> {}
            }
        }

        val mutex = getLock(tenantId, targetScope, targetEntityId)
        return mutex.withLock {
            // Re-check after lock
            if (!idempotencyKey.isNullOrBlank()) {
                val cached = repository.checkIdempotency(tenantId, idempotencyKey)
                if (cached is DomainResult.Success && cached.data != null) {
                    val existing = repository.getSnapshotById(tenantId, cached.data)
                    if (existing is DomainResult.Success && existing.data != null) {
                        return@withLock DomainResult.Success(existing.data)
                    }
                }
            }

            // 2. Historical Source Data Collection
            val historicalSeries = when (val res = sourceCollector.collectHistoricalSeries(
                tenantId = tenantId,
                projectId = projectId,
                scope = targetScope,
                targetEntityId = targetEntityId,
                historicalPeriodStart = historicalPeriodStart,
                historicalPeriodEnd = historicalPeriodEnd
            )) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return@withLock res
                DomainResult.Loading -> return@withLock DomainResult.Error(message = "Unexpected loading state")
            }

            // 3. Scenario Resolution
            val scenario = if (scenarioId != null) {
                when (val s = repository.getScenarioById(tenantId, scenarioId)) {
                    is DomainResult.Success -> s.data
                    else -> null
                }
            } else null

            val forecastId = "fc-snap-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"

            // 4. Computation
            val computation = forecastEngine.computeForecast(
                forecastId = forecastId,
                tenantId = tenantId,
                method = forecastMethod,
                historicalSeries = historicalSeries,
                horizon = horizon,
                scenario = scenario
            )

            // Baseline Anchor for variance
            val baseRev = historicalSeries.revenues.lastOrNull() ?: BigDecimal.ZERO
            val baseCost = historicalSeries.costs.lastOrNull() ?: BigDecimal.ZERO
            val baseProfit = historicalSeries.grossProfits.lastOrNull() ?: BigDecimal.ZERO
            val baseMargin = ProfitabilityForecastMathUtils.calculateGrossMarginPercentage(baseRev, baseCost)

            val revDelta = computation.projectedRevenue.subtract(baseRev.multiply(BigDecimal(horizon.periodCount)))
            val costDelta = computation.projectedTotalCost.subtract(baseCost.multiply(BigDecimal(horizon.periodCount)))
            val profitDelta = computation.projectedGrossProfit.subtract(baseProfit.multiply(BigDecimal(horizon.periodCount)))
            val marginDelta = if (computation.projectedGrossMarginPercentage != null && baseMargin != null) {
                computation.projectedGrossMarginPercentage.subtract(baseMargin)
            } else null

            // 5. Confidence & Risk Evaluation
            val eval = confidenceAndRiskEngine.evaluate(
                historicalSeries = historicalSeries,
                computation = computation,
                baselineRevenue = baseRev.multiply(BigDecimal(horizon.periodCount)),
                baselineCost = baseCost.multiply(BigDecimal(horizon.periodCount))
            )

            // 6. Provenance Lineage
            val provenanceList = mutableListOf<ProfitabilityForecastProvenance>()
            provenanceList.add(
                ProfitabilityForecastProvenance(
                    provenanceId = "prov-fc-${System.currentTimeMillis()}-rev",
                    forecastId = forecastId,
                    tenantId = tenantId,
                    projectId = projectId,
                    sourceModule = "MODULE_16_STEP_06",
                    sourceEntityType = targetScope.name,
                    sourceEntityId = targetEntityId,
                    metricType = "HISTORICAL_REVENUE",
                    amount = baseRev,
                    fingerprint = ProfitabilityForecastMathUtils.generateProvenanceFingerprint(
                        sourceModule = "MODULE_16_STEP_06",
                        sourceEntityType = targetScope.name,
                        sourceEntityId = targetEntityId,
                        sourceTransactionId = null,
                        metricType = "HISTORICAL_REVENUE",
                        forecastMethod = forecastMethod.name
                    )
                )
            )

            // 7. Integrity Hashing
            val integrityHash = ProfitabilityForecastMathUtils.generateSnapshotIntegrityHash(
                forecastId = forecastId,
                tenantId = tenantId,
                projectId = projectId,
                targetScope = targetScope,
                targetEntityId = targetEntityId,
                forecastMethod = forecastMethod,
                scenarioType = scenarioType,
                projectedRevenue = computation.projectedRevenue,
                projectedTotalCost = computation.projectedTotalCost,
                projectedGrossProfit = computation.projectedGrossProfit,
                components = computation.components,
                assumptions = scenario?.assumptions ?: emptyList(),
                provenanceRecords = provenanceList
            )

            // Initial Snapshot
            var snapshot = ProfitabilityForecastSnapshot(
                forecastId = forecastId,
                tenantId = tenantId,
                projectId = projectId,
                forecastVersion = 1,
                forecastMethod = forecastMethod,
                scenarioType = scenarioType,
                scenarioId = scenarioId,
                targetScope = targetScope,
                targetEntityId = targetEntityId,
                targetEntityLabel = targetEntityLabel,
                historicalPeriodStart = historicalPeriodStart,
                historicalPeriodEnd = historicalPeriodEnd,
                forecastPeriodStart = forecastPeriodStart,
                forecastPeriodEnd = forecastPeriodEnd,
                horizon = horizon,
                projectedRevenue = computation.projectedRevenue,
                projectedTotalCost = computation.projectedTotalCost,
                projectedGrossProfit = computation.projectedGrossProfit,
                projectedGrossMarginPercentage = computation.projectedGrossMarginPercentage,
                projectedContribution = computation.projectedContribution,
                projectedContributionMarginPercentage = computation.projectedContributionMarginPercentage,
                projectedUnits = computation.projectedUnits,
                projectedRevenuePerUnit = ProfitabilityForecastMathUtils.safeDivide(computation.projectedRevenue, BigDecimal(computation.projectedUnits)),
                projectedCostPerUnit = ProfitabilityForecastMathUtils.safeDivide(computation.projectedTotalCost, BigDecimal(computation.projectedUnits)),
                projectedProfitPerUnit = ProfitabilityForecastMathUtils.safeDivide(computation.projectedGrossProfit, BigDecimal(computation.projectedUnits)),
                baselineRevenue = baseRev.multiply(BigDecimal(horizon.periodCount)),
                baselineCost = baseCost.multiply(BigDecimal(horizon.periodCount)),
                baselineGrossProfit = baseProfit.multiply(BigDecimal(horizon.periodCount)),
                baselineGrossMarginPercentage = baseMargin,
                projectedRevenueDelta = revDelta,
                projectedCostDelta = costDelta,
                projectedProfitDelta = profitDelta,
                projectedMarginDeltaPercentage = marginDelta,
                breakEvenRevenue = computation.breakEvenRevenue,
                breakEvenUnits = computation.breakEvenUnits,
                marginOfSafetyPercentage = computation.marginOfSafetyPercentage,
                isBreakEvenAttainable = computation.breakEvenRevenue == null || computation.projectedRevenue.compareTo(computation.breakEvenRevenue) >= 0,
                confidenceScore = eval.confidenceScore,
                confidenceLevel = eval.confidenceLevel,
                riskLevel = eval.riskLevel,
                sourceReadiness = historicalSeries.sourceReadiness,
                components = computation.components,
                assumptions = scenario?.assumptions ?: emptyList(),
                provenanceRecords = provenanceList,
                generatedAt = System.currentTimeMillis(),
                generatedBy = actorId,
                integrityHash = integrityHash
            )

            // 8. Management Insights
            val insights = insightEngine.generateInsights(snapshot)
            snapshot = snapshot.copy(insights = insights)

            // 9. Persist
            repository.saveSnapshot(snapshot)

            if (!idempotencyKey.isNullOrBlank()) {
                repository.saveIdempotencyRecord(tenantId, idempotencyKey, forecastId)
            }

            // Audit
            repository.recordAuditEvent(
                ProfitabilityForecastAuditEvent(
                    auditId = "audit-fc-${System.currentTimeMillis()}",
                    tenantId = tenantId,
                    projectId = projectId,
                    forecastId = forecastId,
                    actionType = "FORECAST_GENERATED",
                    actorId = actorId,
                    actorRole = actorRole,
                    details = "Generated $forecastMethod forecast for $targetScope ($targetEntityId) over ${horizon.label}.",
                    newStateHash = integrityHash
                )
            )

            DomainResult.Success(snapshot)
        }
    }

    override suspend fun getForecastById(tenantId: String, forecastId: String): DomainResult<ProfitabilityForecastSnapshot?> {
        return repository.getSnapshotById(tenantId, forecastId)
    }

    override suspend fun listForecasts(tenantId: String, filter: ProfitabilityForecastFilter): DomainResult<List<ProfitabilityForecastSnapshot>> {
        return repository.listSnapshots(tenantId, filter)
    }

    override suspend fun getForecastComponents(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastComponent>> {
        val snap = when (val res = repository.getSnapshotById(tenantId, forecastId)) {
            is DomainResult.Success -> res.data
            else -> null
        } ?: return DomainResult.Error(message = "Forecast not found.")
        return DomainResult.Success(snap.components)
    }

    override suspend fun getForecastAssumptions(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityScenarioAssumption>> {
        val snap = when (val res = repository.getSnapshotById(tenantId, forecastId)) {
            is DomainResult.Success -> res.data
            else -> null
        } ?: return DomainResult.Error(message = "Forecast not found.")
        return DomainResult.Success(snap.assumptions)
    }

    override suspend fun getForecastInsights(tenantId: String, forecastId: String): DomainResult<List<ForecastManagementInsight>> {
        val snap = when (val res = repository.getSnapshotById(tenantId, forecastId)) {
            is DomainResult.Success -> res.data
            else -> null
        } ?: return DomainResult.Error(message = "Forecast not found.")
        return DomainResult.Success(snap.insights)
    }

    override suspend fun getForecastRisk(tenantId: String, forecastId: String): DomainResult<ForecastRiskLevel?> {
        val snap = when (val res = repository.getSnapshotById(tenantId, forecastId)) {
            is DomainResult.Success -> res.data
            else -> null
        } ?: return DomainResult.Error(message = "Forecast not found.")
        return DomainResult.Success(snap.riskLevel)
    }

    override suspend fun getForecastConfidence(tenantId: String, forecastId: String): DomainResult<ConfidenceAndRiskEvaluation?> {
        val snap = when (val res = repository.getSnapshotById(tenantId, forecastId)) {
            is DomainResult.Success -> res.data
            else -> null
        } ?: return DomainResult.Error(message = "Forecast not found.")
        return DomainResult.Success(
            ConfidenceAndRiskEvaluation(
                confidenceScore = snap.confidenceScore,
                confidenceLevel = snap.confidenceLevel,
                riskLevel = snap.riskLevel
            )
        )
    }

    override suspend fun getForecastProvenance(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastProvenance>> {
        val snap = when (val res = repository.getSnapshotById(tenantId, forecastId)) {
            is DomainResult.Success -> res.data
            else -> null
        } ?: return DomainResult.Error(message = "Forecast not found.")
        return DomainResult.Success(snap.provenanceRecords)
    }

    override suspend fun reconcileForecast(tenantId: String, projectId: String, forecastId: String): DomainResult<ProfitabilityForecastReconciliationEvent> {
        val snap = when (val res = repository.getSnapshotById(tenantId, forecastId)) {
            is DomainResult.Success -> res.data
            else -> null
        } ?: return DomainResult.Error(message = "Forecast not found.")

        val recon = reconciliationService.reconcileForecast(snap)
        if (recon is DomainResult.Success) {
            repository.saveReconciliationEvent(recon.data)
        }
        return recon
    }

    override suspend fun compareWithActual(
        tenantId: String,
        projectId: String,
        forecastId: String,
        actualRevenue: BigDecimal,
        actualCost: BigDecimal,
        actualUnits: Long,
        actualPeriodId: String
    ): DomainResult<ForecastActualComparison> {
        val snap = when (val res = repository.getSnapshotById(tenantId, forecastId)) {
            is DomainResult.Success -> res.data
            else -> null
        } ?: return DomainResult.Error(message = "Forecast not found.")

        val comp = reconciliationService.compareWithActual(
            snapshot = snap,
            actualRevenue = actualRevenue,
            actualCost = actualCost,
            actualUnits = actualUnits,
            actualPeriodId = actualPeriodId
        )
        if (comp is DomainResult.Success) {
            repository.saveActualComparison(comp.data)
        }
        return comp
    }

    override suspend fun createScenario(tenantId: String, projectId: String, scenario: ProfitabilityScenario): DomainResult<ProfitabilityScenario> {
        val valRes = ProfitabilityForecastValidator.validateScenario(scenario)
        if (valRes is DomainResult.Error) return valRes
        return repository.saveScenario(scenario)
    }

    override suspend fun listScenarios(tenantId: String, projectId: String, scope: ProfitabilityForecastScope?): DomainResult<List<ProfitabilityScenario>> {
        val standard = scenarioEngine.generateStandardScenarios(tenantId, projectId, scope ?: ProfitabilityForecastScope.BUSINESS)
        val saved = when (val res = repository.listScenarios(tenantId, projectId, scope)) {
            is DomainResult.Success -> res.data
            else -> emptyList()
        }
        val combined = (standard + saved).distinctBy { it.scenarioId }
        return DomainResult.Success(combined)
    }

    override suspend fun getScenarioById(tenantId: String, scenarioId: String): DomainResult<ProfitabilityScenario?> {
        val standard = scenarioEngine.generateStandardScenarios(tenantId, "PROJ", ProfitabilityForecastScope.BUSINESS)
        val found = standard.find { it.scenarioId == scenarioId }
        if (found != null) return DomainResult.Success(found)
        return repository.getScenarioById(tenantId, scenarioId)
    }

    override suspend fun compareScenarios(
        tenantId: String,
        projectId: String,
        forecastId: String,
        scenarioIds: List<String>?
    ): DomainResult<ProfitabilityScenarioComparison> {
        val snap = when (val res = repository.getSnapshotById(tenantId, forecastId)) {
            is DomainResult.Success -> res.data
            else -> null
        } ?: return DomainResult.Error(message = "Baseline forecast not found.")

        val historicalSeries = when (val res = sourceCollector.collectHistoricalSeries(
            tenantId = tenantId,
            projectId = projectId,
            scope = snap.targetScope,
            targetEntityId = snap.targetEntityId,
            historicalPeriodStart = snap.historicalPeriodStart,
            historicalPeriodEnd = snap.historicalPeriodEnd
        )) {
            is DomainResult.Success -> res.data
            else -> HistoricalProfitabilitySeries(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }

        val allScenarios = when (val res = listScenarios(tenantId, projectId, snap.targetScope)) {
            is DomainResult.Success -> res.data
            else -> emptyList()
        }

        val selected = if (!scenarioIds.isNullOrEmpty()) {
            allScenarios.filter { it.scenarioId in scenarioIds }
        } else {
            allScenarios.filter { it.scenarioType != ProfitabilityScenarioType.BASELINE }
        }

        val comparison = scenarioEngine.compareScenarios(
            tenantId = tenantId,
            projectId = projectId,
            baselineForecast = snap,
            scenarios = selected,
            historicalSeries = historicalSeries,
            forecastEngine = forecastEngine
        )

        return DomainResult.Success(comparison)
    }

    override suspend fun getForecastTrends(tenantId: String, scope: ProfitabilityForecastScope, targetEntityId: String): DomainResult<List<ProfitabilityForecastSnapshot>> {
        return repository.listSnapshots(tenantId, ProfitabilityForecastFilter(targetScope = scope, targetEntityId = targetEntityId))
    }

    override suspend fun getForecastRankings(tenantId: String, scope: ProfitabilityForecastScope, horizon: ForecastHorizon): DomainResult<List<ProfitabilityForecastSnapshot>> {
        val all = when (val res = repository.listSnapshots(tenantId, ProfitabilityForecastFilter(targetScope = scope))) {
            is DomainResult.Success -> res.data
            else -> emptyList()
        }
        val ranked = all.filter { it.horizon == horizon }.sortedByDescending { it.projectedGrossProfit }
        return DomainResult.Success(ranked)
    }

    override suspend fun listAuditEvents(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastAuditEvent>> {
        return repository.listAuditEvents(tenantId, forecastId)
    }

    override suspend fun exportHandoffContract(tenantId: String, projectId: String, forecastId: String): DomainResult<Module16Step08ProfitabilityForecastHandoffContract> {
        val snap = when (val res = repository.getSnapshotById(tenantId, forecastId)) {
            is DomainResult.Success -> res.data
            else -> null
        } ?: return DomainResult.Error(message = "Forecast snapshot not found for export handoff contract.")

        val handoff = Module16Step08ProfitabilityForecastHandoffContract(
            contractVersion = "MODULE16_STEP08_V1",
            forecastId = snap.forecastId,
            tenantId = snap.tenantId,
            projectId = snap.projectId,
            targetScope = snap.targetScope,
            targetEntityId = snap.targetEntityId,
            targetEntityLabel = snap.targetEntityLabel,
            forecastPeriod = "${snap.forecastPeriodStart} - ${snap.forecastPeriodEnd}",
            horizon = snap.horizon.label,
            forecastMethod = snap.forecastMethod.name,
            scenarioType = snap.scenarioType.name,
            projectedRevenue = snap.projectedRevenue,
            projectedTotalCost = snap.projectedTotalCost,
            projectedGrossProfit = snap.projectedGrossProfit,
            projectedGrossMarginPercentage = snap.projectedGrossMarginPercentage,
            projectedUnits = snap.projectedUnits,
            confidenceScore = snap.confidenceScore,
            confidenceLevel = snap.confidenceLevel.name,
            riskLevel = snap.riskLevel.name,
            breakEvenRevenue = snap.breakEvenRevenue,
            majorDrivers = snap.components.sortedByDescending { it.projectedAmount }.take(3).map { "${it.componentType.name}: ${it.projectedAmount}" },
            majorRisks = listOf("Risk Level: ${snap.riskLevel.name}"),
            topManagementInsights = snap.insights.map { it.title },
            scenarioSummaryDeltas = mapOf(
                "REVENUE_DELTA" to (snap.projectedRevenueDelta ?: BigDecimal.ZERO),
                "PROFIT_DELTA" to (snap.projectedProfitDelta ?: BigDecimal.ZERO)
            ),
            isReconciled = true,
            integrityHash = snap.integrityHash,
            generatedAt = snap.generatedAt
        )

        return DomainResult.Success(handoff)
    }
}
