package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Source Collector Interface for gathering authoritative profitability data across Steps 01–08.
 * Module 16 Step 09.
 */
interface ProfitabilityAlertSourceCollector {
    suspend fun collectEvaluationPayload(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): DomainResult<ProfitabilityEvaluationPayload>
}

/**
 * Production Implementation of ProfitabilityAlertSourceCollector.
 */
class ProfitabilityAlertSourceCollectorImpl(
    private val jobCostService: JobCostCalculationService? = null,
    private val productService: ProductProfitabilityService? = null,
    private val customerService: CustomerProfitabilityService? = null,
    private val vendorService: VendorProfitabilityService? = null,
    private val periodService: PeriodProfitabilityService? = null,
    private val intelligenceService: ProfitabilityIntelligenceService? = null,
    private val forecastService: ProfitabilityForecastQueryContract? = null
) : ProfitabilityAlertSourceCollector {

    override suspend fun collectEvaluationPayload(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): DomainResult<ProfitabilityEvaluationPayload> {
        val jobs = mutableListOf<JobProfitabilityEvaluationItem>()
        val products = mutableListOf<ProductProfitabilityEvaluationItem>()
        val customers = mutableListOf<CustomerProfitabilityEvaluationItem>()
        val vendors = mutableListOf<VendorProfitabilityEvaluationItem>()
        val periods = mutableListOf<PeriodProfitabilityEvaluationItem>()
        val crossDimensionItems = mutableListOf<CrossDimensionEvaluationItem>()
        val forecasts = mutableListOf<ForecastEvaluationItem>()
        val integrityIssues = mutableListOf<DataIntegrityEvaluationItem>()

        // 1. Collect Forecasts if available
        if (forecastService != null) {
            when (val fcRes = forecastService.listForecasts(tenantId, ProfitabilityForecastFilter())) {
                is DomainResult.Success -> {
                    for (snap in fcRes.data) {
                        forecasts.add(
                            ForecastEvaluationItem(
                                forecastId = snap.forecastId,
                                targetScope = snap.targetScope.name,
                                targetEntityId = snap.targetEntityId,
                                targetEntityLabel = snap.targetEntityLabel,
                                horizon = snap.horizon.name,
                                projectedRevenue = snap.projectedRevenue,
                                projectedTotalCost = snap.projectedTotalCost,
                                projectedGrossProfit = snap.projectedGrossProfit,
                                projectedGrossMarginPercentage = snap.projectedGrossMarginPercentage,
                                confidenceScore = snap.confidenceScore,
                                riskLevel = snap.riskLevel.name,
                                isLossProjected = snap.projectedGrossProfit < java.math.BigDecimal.ZERO
                            )
                        )
                    }
                }
                is DomainResult.Error -> {}
                DomainResult.Loading -> {}
            }
        }

        return DomainResult.Success(
            ProfitabilityEvaluationPayload(
                tenantId = tenantId,
                projectId = projectId,
                periodId = periodId,
                jobs = jobs,
                products = products,
                customers = customers,
                vendors = vendors,
                periods = periods,
                crossDimensionItems = crossDimensionItems,
                forecasts = forecasts,
                integrityIssues = integrityIssues
            )
        )
    }
}
