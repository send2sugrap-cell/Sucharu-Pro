package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsThresholdConfig
import org.junit.Assert.assertTrue
import org.junit.Test

class QcAnalyticsThresholdConfigTest {

    @Test
    fun `valid threshold config passes validation`() {
        val config = QcAnalyticsThresholdConfig(
            maxAcceptableCostVariance = 100.0,
            maxAcceptableTimeVarianceMinutes = 30L,
            maxAcceptableDefectRate = 10.0,
            maxAcceptableReworkRate = 5.0,
            maxAcceptableReQcRate = 5.0,
            repeatedFailureCycleThreshold = 2
        )
        val result = QcAnalyticsValidator.validateThresholdConfig(config)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `negative cost variance threshold fails validation`() {
        val config = QcAnalyticsThresholdConfig.DEFAULT.copy(maxAcceptableCostVariance = -10.0)
        val result = QcAnalyticsValidator.validateThresholdConfig(config)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cost variance cannot be negative", ignoreCase = true))
    }

    @Test
    fun `negative time variance threshold fails validation`() {
        val config = QcAnalyticsThresholdConfig.DEFAULT.copy(maxAcceptableTimeVarianceMinutes = -5L)
        val result = QcAnalyticsValidator.validateThresholdConfig(config)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("time variance cannot be negative", ignoreCase = true))
    }

    @Test
    fun `invalid defect rate threshold fails validation`() {
        val config = QcAnalyticsThresholdConfig.DEFAULT.copy(maxAcceptableDefectRate = 150.0)
        val result = QcAnalyticsValidator.validateThresholdConfig(config)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("defect rate must be between", ignoreCase = true))
    }

    @Test
    fun `zero or negative repeated failure threshold fails validation`() {
        val config = QcAnalyticsThresholdConfig.DEFAULT.copy(repeatedFailureCycleThreshold = 0)
        val result = QcAnalyticsValidator.validateThresholdConfig(config)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("repeated failure cycle threshold must be at least 1", ignoreCase = true))
    }
}
