package com.sucharu.sucharupro.domain.validation.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.ProfitabilityForecastScope
import com.sucharu.sucharupro.domain.model.profitability.ProfitabilityScenario
import java.math.BigDecimal

/**
 * Domain Validation Rules for Profitability Forecasting & Scenario Modelling.
 * Module 16 Step 08.
 */
object ProfitabilityForecastValidator {

    fun validateTenantAndProject(tenantId: String?, projectId: String?): DomainResult<Unit> {
        if (tenantId.isNullOrBlank()) {
            return DomainResult.Error(message = "tenantId must not be blank.")
        }
        if (projectId.isNullOrBlank()) {
            return DomainResult.Error(message = "projectId must not be blank.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateForecastTarget(targetScope: ProfitabilityForecastScope?, targetEntityId: String?): DomainResult<Unit> {
        if (targetScope == null) {
            return DomainResult.Error(message = "targetScope is required.")
        }
        if (targetEntityId.isNullOrBlank()) {
            return DomainResult.Error(message = "targetEntityId must not be blank.")
        }
        return DomainResult.Success(Unit)
    }

    fun validatePeriodRange(historicalStart: String?, historicalEnd: String?, forecastStart: String?, forecastEnd: String?): DomainResult<Unit> {
        if (historicalStart.isNullOrBlank() || historicalEnd.isNullOrBlank()) {
            return DomainResult.Error(message = "Historical period range (start and end) must be specified.")
        }
        if (forecastStart.isNullOrBlank() || forecastEnd.isNullOrBlank()) {
            return DomainResult.Error(message = "Forecast period range (start and end) must be specified.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateScenario(scenario: ProfitabilityScenario): DomainResult<Unit> {
        val baseVal = validateTenantAndProject(scenario.tenantId, scenario.projectId)
        if (baseVal is DomainResult.Error) return baseVal

        if (scenario.scenarioName.isBlank()) {
            return DomainResult.Error(message = "scenarioName must not be blank.")
        }

        for (a in scenario.assumptions) {
            if (a.parameterKey.isBlank()) {
                return DomainResult.Error(message = "Assumption parameterKey must not be blank.")
            }
            if (a.adjustmentPercentage.compareTo(BigDecimal("-100.0000")) < 0) {
                return DomainResult.Error(message = "Adjustment percentage cannot be lower than -100.0000% on parameter ${a.parameterKey}.")
            }
        }
        return DomainResult.Success(Unit)
    }
}
