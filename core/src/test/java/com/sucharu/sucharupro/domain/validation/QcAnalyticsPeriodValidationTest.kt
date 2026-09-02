package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import org.junit.Assert.assertTrue
import org.junit.Test

class QcAnalyticsPeriodValidationTest {

    @Test
    fun `valid period passes validation`() {
        val period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z")
        val result = QcAnalyticsValidator.validatePeriod(period)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `blank start timestamp fails validation`() {
        val period = QcAnalyticsPeriod.custom("", "2026-08-31T23:59:59Z")
        val result = QcAnalyticsValidator.validatePeriod(period)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Start timestamp cannot be blank"))
    }

    @Test
    fun `blank end timestamp fails validation`() {
        val period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "")
        val result = QcAnalyticsValidator.validatePeriod(period)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("End timestamp cannot be blank"))
    }

    @Test
    fun `start after end fails validation`() {
        val period = QcAnalyticsPeriod.custom("2026-08-31T23:59:59Z", "2026-08-01T00:00:00Z")
        val result = QcAnalyticsValidator.validatePeriod(period)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cannot be after end timestamp"))
    }

    @Test
    fun `invalid ISO timestamp format fails validation`() {
        val period = QcAnalyticsPeriod.custom("invalid-date", "2026-08-31T23:59:59Z")
        val result = QcAnalyticsValidator.validatePeriod(period)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Invalid timestamp format"))
    }
}
