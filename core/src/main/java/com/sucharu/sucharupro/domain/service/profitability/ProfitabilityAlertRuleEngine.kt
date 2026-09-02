package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Early-Warning Rule Evaluation Engine Interface.
 * Module 16 Step 09.
 */
interface ProfitabilityAlertRuleEngine {
    fun evaluateRules(
        payload: ProfitabilityEvaluationPayload,
        customRules: List<ProfitabilityAlertRule>
    ): List<ProfitabilityAlert>
}

/**
 * Production Implementation of ProfitabilityAlertRuleEngine.
 */
class ProfitabilityAlertRuleEngineImpl : ProfitabilityAlertRuleEngine {

    private val ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

    override fun evaluateRules(
        payload: ProfitabilityEvaluationPayload,
        customRules: List<ProfitabilityAlertRule>
    ): List<ProfitabilityAlert> {
        val detectedAlerts = mutableListOf<ProfitabilityAlert>()
        val tenantId = payload.tenantId
        val projectId = payload.projectId
        val periodId = payload.periodId
        val now = System.currentTimeMillis()

        // 1. Evaluate Jobs
        for (job in payload.jobs) {
            // Negative Margin / Loss Making
            if (job.grossProfit < ZERO || job.grossMarginPercentage < ZERO) {
                val impact = job.grossProfit.abs()
                val rule = findMatchingRule(customRules, ProfitabilityAlertType.LOSS_MAKING, ProfitabilityAlertDimension.JOB)
                val severity = rule?.severity ?: ProfitabilityAlertSeverity.CRITICAL
                val thresh = rule?.thresholdValue ?: ZERO
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.LOSS_MAKING, ProfitabilityAlertDimension.JOB,
                    job.jobId, periodId, "grossProfit", rule?.ruleId
                )
                val alertId = "alt-job-loss-${job.jobId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.LOSS_MAKING, severity,
                    ProfitabilityAlertDimension.JOB, job.jobId, job.grossProfit, thresh, impact, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.LOSS_MAKING,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.JOB,
                        dimensionId = job.jobId,
                        dimensionLabel = "Job ${job.jobCode}",
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 02",
                        sourceEntityType = "JOB_ACTUAL_COST",
                        sourceEntityId = job.jobId,
                        triggerMetric = "grossProfit",
                        observedValue = job.grossProfit,
                        thresholdValue = thresh,
                        direction = ProfitabilityAlertDirection.BELOW_THRESHOLD,
                        financialImpact = impact,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Job ${job.jobCode} is operating at a loss with gross profit BDT ${job.grossProfit} (Margin: ${job.grossMarginPercentage}%).",
                        recommendedActionCode = ManagementActionCode.REVIEW_JOB_COST,
                        ruleId = rule?.ruleId
                    )
                )
            }

            // Job Margin Decline below threshold
            val marginRule = findMatchingRule(customRules, ProfitabilityAlertType.MARGIN_DECLINE, ProfitabilityAlertDimension.JOB)
            val marginThresh = marginRule?.thresholdValue ?: BigDecimal("10.0000")
            if (job.grossMarginPercentage < marginThresh && job.grossProfit >= ZERO) {
                val targetProfit = job.revenue.multiply(marginThresh).divide(BigDecimal("100.0000"), 4, RoundingMode.HALF_UP)
                val impact = targetProfit.subtract(job.grossProfit).max(ZERO)
                val severity = marginRule?.severity ?: ProfitabilityAlertSeverity.MEDIUM
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.MARGIN_DECLINE, ProfitabilityAlertDimension.JOB,
                    job.jobId, periodId, "grossMarginPercentage", marginRule?.ruleId
                )
                val alertId = "alt-job-margin-${job.jobId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.MARGIN_DECLINE, severity,
                    ProfitabilityAlertDimension.JOB, job.jobId, job.grossMarginPercentage, marginThresh, impact, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.MARGIN_DECLINE,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.JOB,
                        dimensionId = job.jobId,
                        dimensionLabel = "Job ${job.jobCode}",
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 02",
                        sourceEntityType = "JOB_ACTUAL_COST",
                        sourceEntityId = job.jobId,
                        triggerMetric = "grossMarginPercentage",
                        observedValue = job.grossMarginPercentage,
                        thresholdValue = marginThresh,
                        direction = ProfitabilityAlertDirection.BELOW_THRESHOLD,
                        financialImpact = impact,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Job ${job.jobCode} gross margin of ${job.grossMarginPercentage}% fell below minimum threshold of $marginThresh%.",
                        recommendedActionCode = ManagementActionCode.REVIEW_JOB_COST,
                        ruleId = marginRule?.ruleId
                    )
                )
            }
        }

        // 2. Evaluate Products
        for (prod in payload.products) {
            // Product Loss Making / Margin Negative
            if (prod.grossProfit < ZERO || prod.grossMarginPercentage < ZERO) {
                val impact = prod.grossProfit.abs()
                val rule = findMatchingRule(customRules, ProfitabilityAlertType.LOSS_MAKING, ProfitabilityAlertDimension.PRODUCT)
                val severity = rule?.severity ?: ProfitabilityAlertSeverity.HIGH
                val thresh = rule?.thresholdValue ?: ZERO
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.LOSS_MAKING, ProfitabilityAlertDimension.PRODUCT,
                    prod.productId, periodId, "grossProfit", rule?.ruleId
                )
                val alertId = "alt-prod-loss-${prod.productId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.LOSS_MAKING, severity,
                    ProfitabilityAlertDimension.PRODUCT, prod.productId, prod.grossProfit, thresh, impact, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.LOSS_MAKING,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.PRODUCT,
                        dimensionId = prod.productId,
                        dimensionLabel = prod.productName,
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 03",
                        sourceEntityType = "PRODUCT_PROFITABILITY",
                        sourceEntityId = prod.productId,
                        triggerMetric = "grossProfit",
                        observedValue = prod.grossProfit,
                        thresholdValue = thresh,
                        direction = ProfitabilityAlertDirection.BELOW_THRESHOLD,
                        financialImpact = impact,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Product '${prod.productName}' (${prod.productCode}) is loss-making with gross profit BDT ${prod.grossProfit}.",
                        recommendedActionCode = ManagementActionCode.REVIEW_PRODUCT_PRICING,
                        ruleId = rule?.ruleId
                    )
                )
            }

            // Product Margin Decline below threshold
            val marginRule = findMatchingRule(customRules, ProfitabilityAlertType.MARGIN_DECLINE, ProfitabilityAlertDimension.PRODUCT)
            val marginThresh = marginRule?.thresholdValue ?: BigDecimal("10.0000")
            if (prod.grossMarginPercentage < marginThresh && prod.grossProfit >= ZERO) {
                val targetProfit = prod.totalRevenue.multiply(marginThresh).divide(BigDecimal("100.0000"), 4, RoundingMode.HALF_UP)
                val impact = targetProfit.subtract(prod.grossProfit).max(ZERO)
                val severity = marginRule?.severity ?: ProfitabilityAlertSeverity.MEDIUM
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.MARGIN_DECLINE, ProfitabilityAlertDimension.PRODUCT,
                    prod.productId, periodId, "grossMarginPercentage", marginRule?.ruleId
                )
                val alertId = "alt-prod-margin-${prod.productId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.MARGIN_DECLINE, severity,
                    ProfitabilityAlertDimension.PRODUCT, prod.productId, prod.grossMarginPercentage, marginThresh, impact, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.MARGIN_DECLINE,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.PRODUCT,
                        dimensionId = prod.productId,
                        dimensionLabel = prod.productName,
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 03",
                        sourceEntityType = "PRODUCT_PROFITABILITY",
                        sourceEntityId = prod.productId,
                        triggerMetric = "grossMarginPercentage",
                        observedValue = prod.grossMarginPercentage,
                        thresholdValue = marginThresh,
                        direction = ProfitabilityAlertDirection.BELOW_THRESHOLD,
                        financialImpact = impact,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Product '${prod.productName}' gross margin of ${prod.grossMarginPercentage}% fell below minimum threshold of $marginThresh%.",
                        recommendedActionCode = ManagementActionCode.REVIEW_PRODUCT_PRICING,
                        ruleId = marginRule?.ruleId
                    )
                )
            }

            // Unit Cost Spike / Exceeding Target
            val unitCostRule = findMatchingRule(customRules, ProfitabilityAlertType.UNIT_COST_SPIKE, ProfitabilityAlertDimension.PRODUCT)
            val targetUnitCostRatio = unitCostRule?.thresholdValue ?: BigDecimal("80.0000") // unit cost > 80% of selling price
            if (prod.averageSellingPrice > ZERO) {
                val currentRatio = prod.unitCost.divide(prod.averageSellingPrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100.0000"))
                if (currentRatio > targetUnitCostRatio) {
                    val excessUnitCost = prod.unitCost.subtract(prod.averageSellingPrice.multiply(targetUnitCostRatio).divide(BigDecimal("100.0000"), 4, RoundingMode.HALF_UP))
                    val impact = excessUnitCost.multiply(BigDecimal(prod.totalUnits)).max(ZERO)
                    val severity = unitCostRule?.severity ?: ProfitabilityAlertSeverity.MEDIUM
                    val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                        tenantId, ProfitabilityAlertType.UNIT_COST_SPIKE, ProfitabilityAlertDimension.PRODUCT,
                        prod.productId, periodId, "unitCostRatio", unitCostRule?.ruleId
                    )
                    val alertId = "alt-prod-unitcost-${prod.productId}-$fp".take(64)
                    val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                        alertId, tenantId, projectId, ProfitabilityAlertType.UNIT_COST_SPIKE, severity,
                        ProfitabilityAlertDimension.PRODUCT, prod.productId, currentRatio, targetUnitCostRatio, impact, fp
                    )
                    detectedAlerts.add(
                        ProfitabilityAlert(
                            alertId = alertId,
                            tenantId = tenantId,
                            projectId = projectId,
                            alertType = ProfitabilityAlertType.UNIT_COST_SPIKE,
                            severity = severity,
                            status = ProfitabilityAlertStatus.DETECTED,
                            dimensionType = ProfitabilityAlertDimension.PRODUCT,
                            dimensionId = prod.productId,
                            dimensionLabel = prod.productName,
                            periodId = periodId,
                            sourceModule = "Module 16",
                            sourceStep = "Step 03",
                            sourceEntityType = "PRODUCT_PROFITABILITY",
                            sourceEntityId = prod.productId,
                            triggerMetric = "unitCostRatio",
                            observedValue = currentRatio,
                            thresholdValue = targetUnitCostRatio,
                            direction = ProfitabilityAlertDirection.ABOVE_THRESHOLD,
                            financialImpact = impact,
                            detectedAt = now,
                            fingerprint = fp,
                            integrityHash = hash,
                            explanation = "Product '${prod.productName}' unit cost of BDT ${prod.unitCost} is ${currentRatio}% of selling price (Threshold: ${targetUnitCostRatio}%).",
                            recommendedActionCode = ManagementActionCode.REVIEW_PRODUCT_PRICING,
                            ruleId = unitCostRule?.ruleId
                        )
                    )
                }
            }
        }

        // 3. Evaluate Customers
        for (cust in payload.customers) {
            // Contribution Margin Decline
            val contribRule = findMatchingRule(customRules, ProfitabilityAlertType.CONTRIBUTION_MARGIN_DECLINE, ProfitabilityAlertDimension.CUSTOMER)
            val contribThresh = contribRule?.thresholdValue ?: BigDecimal("10.0000")
            if (cust.contributionMarginPercentage < contribThresh) {
                val targetProfit = cust.totalRevenue.multiply(contribThresh).divide(BigDecimal("100.0000"), 4, RoundingMode.HALF_UP)
                val impact = targetProfit.subtract(cust.grossProfit).max(ZERO)
                val severity = contribRule?.severity ?: if (cust.grossProfit < ZERO) ProfitabilityAlertSeverity.HIGH else ProfitabilityAlertSeverity.MEDIUM
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.CONTRIBUTION_MARGIN_DECLINE, ProfitabilityAlertDimension.CUSTOMER,
                    cust.customerId, periodId, "contributionMarginPercentage", contribRule?.ruleId
                )
                val alertId = "alt-cust-contrib-${cust.customerId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.CONTRIBUTION_MARGIN_DECLINE, severity,
                    ProfitabilityAlertDimension.CUSTOMER, cust.customerId, cust.contributionMarginPercentage, contribThresh, impact, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.CONTRIBUTION_MARGIN_DECLINE,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.CUSTOMER,
                        dimensionId = cust.customerId,
                        dimensionLabel = cust.customerName,
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 04",
                        sourceEntityType = "CUSTOMER_PROFITABILITY",
                        sourceEntityId = cust.customerId,
                        triggerMetric = "contributionMarginPercentage",
                        observedValue = cust.contributionMarginPercentage,
                        thresholdValue = contribThresh,
                        direction = ProfitabilityAlertDirection.BELOW_THRESHOLD,
                        financialImpact = impact,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Customer '${cust.customerName}' contribution margin of ${cust.contributionMarginPercentage}% fell below threshold of $contribThresh%.",
                        recommendedActionCode = ManagementActionCode.REVIEW_CUSTOMER_PRICING,
                        ruleId = contribRule?.ruleId
                    )
                )
            }

            // Customer Concentration Risk
            val concRule = findMatchingRule(customRules, ProfitabilityAlertType.CUSTOMER_CONCENTRATION_RISK, ProfitabilityAlertDimension.CUSTOMER)
            val concThresh = concRule?.thresholdValue ?: BigDecimal("25.0000") // > 25% revenue concentration
            if (cust.revenueSharePercentage > concThresh) {
                val impact = cust.totalRevenue
                val severity = concRule?.severity ?: ProfitabilityAlertSeverity.HIGH
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.CUSTOMER_CONCENTRATION_RISK, ProfitabilityAlertDimension.CUSTOMER,
                    cust.customerId, periodId, "revenueSharePercentage", concRule?.ruleId
                )
                val alertId = "alt-cust-conc-${cust.customerId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.CUSTOMER_CONCENTRATION_RISK, severity,
                    ProfitabilityAlertDimension.CUSTOMER, cust.customerId, cust.revenueSharePercentage, concThresh, impact, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.CUSTOMER_CONCENTRATION_RISK,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.CUSTOMER,
                        dimensionId = cust.customerId,
                        dimensionLabel = cust.customerName,
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 04",
                        sourceEntityType = "CUSTOMER_PROFITABILITY",
                        sourceEntityId = cust.customerId,
                        triggerMetric = "revenueSharePercentage",
                        observedValue = cust.revenueSharePercentage,
                        thresholdValue = concThresh,
                        direction = ProfitabilityAlertDirection.ABOVE_THRESHOLD,
                        financialImpact = impact,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Customer '${cust.customerName}' represents ${cust.revenueSharePercentage}% of total revenue (Threshold: $concThresh%), posing concentration risk.",
                        recommendedActionCode = ManagementActionCode.REVIEW_CUSTOMER_CONCENTRATION,
                        ruleId = concRule?.ruleId
                    )
                )
            }
        }

        // 4. Evaluate Vendors
        for (v in payload.vendors) {
            // Vendor Cost Pressure
            val costPressureRule = findMatchingRule(customRules, ProfitabilityAlertType.VENDOR_COST_PRESSURE, ProfitabilityAlertDimension.VENDOR)
            val costPressureThresh = costPressureRule?.thresholdValue ?: BigDecimal("70.0000")
            if (v.costPressureScore > costPressureThresh) {
                val impact = v.totalSpend.multiply(BigDecimal("0.1000")).setScale(4, RoundingMode.HALF_UP)
                val severity = costPressureRule?.severity ?: ProfitabilityAlertSeverity.MEDIUM
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.VENDOR_COST_PRESSURE, ProfitabilityAlertDimension.VENDOR,
                    v.vendorId, periodId, "costPressureScore", costPressureRule?.ruleId
                )
                val alertId = "alt-vend-pressure-${v.vendorId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.VENDOR_COST_PRESSURE, severity,
                    ProfitabilityAlertDimension.VENDOR, v.vendorId, v.costPressureScore, costPressureThresh, impact, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.VENDOR_COST_PRESSURE,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.VENDOR,
                        dimensionId = v.vendorId,
                        dimensionLabel = v.vendorName,
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 05",
                        sourceEntityType = "VENDOR_PROFITABILITY",
                        sourceEntityId = v.vendorId,
                        triggerMetric = "costPressureScore",
                        observedValue = v.costPressureScore,
                        thresholdValue = costPressureThresh,
                        direction = ProfitabilityAlertDirection.ABOVE_THRESHOLD,
                        financialImpact = impact,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Vendor '${v.vendorName}' has elevated cost pressure score of ${v.costPressureScore} (Threshold: $costPressureThresh).",
                        recommendedActionCode = ManagementActionCode.NEGOTIATE_VENDOR_COST,
                        ruleId = costPressureRule?.ruleId
                    )
                )
            }

            // Vendor Dependency Risk
            val depRule = findMatchingRule(customRules, ProfitabilityAlertType.VENDOR_DEPENDENCY_RISK, ProfitabilityAlertDimension.VENDOR)
            val depThresh = depRule?.thresholdValue ?: BigDecimal("65.0000")
            if (v.dependencyRiskScore > depThresh) {
                val impact = v.totalSpend
                val severity = depRule?.severity ?: ProfitabilityAlertSeverity.HIGH
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.VENDOR_DEPENDENCY_RISK, ProfitabilityAlertDimension.VENDOR,
                    v.vendorId, periodId, "dependencyRiskScore", depRule?.ruleId
                )
                val alertId = "alt-vend-dep-${v.vendorId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.VENDOR_DEPENDENCY_RISK, severity,
                    ProfitabilityAlertDimension.VENDOR, v.vendorId, v.dependencyRiskScore, depThresh, impact, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.VENDOR_DEPENDENCY_RISK,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.VENDOR,
                        dimensionId = v.vendorId,
                        dimensionLabel = v.vendorName,
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 05",
                        sourceEntityType = "VENDOR_PROFITABILITY",
                        sourceEntityId = v.vendorId,
                        triggerMetric = "dependencyRiskScore",
                        observedValue = v.dependencyRiskScore,
                        thresholdValue = depThresh,
                        direction = ProfitabilityAlertDirection.ABOVE_THRESHOLD,
                        financialImpact = impact,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Vendor '${v.vendorName}' dependency risk score is ${v.dependencyRiskScore} (Threshold: $depThresh), indicating critical supply reliance.",
                        recommendedActionCode = ManagementActionCode.REVIEW_VENDOR_DEPENDENCY,
                        ruleId = depRule?.ruleId
                    )
                )
            }
        }

        // 5. Evaluate Periods
        for (per in payload.periods) {
            if (per.profitDeclinePercentage != null && per.profitDeclinePercentage > BigDecimal("10.0000")) {
                val impact = per.totalCost.subtract(per.totalRevenue).max(ZERO)
                val rule = findMatchingRule(customRules, ProfitabilityAlertType.PROFIT_DECLINE, ProfitabilityAlertDimension.PERIOD)
                val severity = rule?.severity ?: ProfitabilityAlertSeverity.MEDIUM
                val thresh = rule?.thresholdValue ?: BigDecimal("10.0000")
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.PROFIT_DECLINE, ProfitabilityAlertDimension.PERIOD,
                    per.periodId, per.periodId, "profitDeclinePercentage", rule?.ruleId
                )
                val alertId = "alt-per-pdecline-${per.periodId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.PROFIT_DECLINE, severity,
                    ProfitabilityAlertDimension.PERIOD, per.periodId, per.profitDeclinePercentage, thresh, impact, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.PROFIT_DECLINE,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.PERIOD,
                        dimensionId = per.periodId,
                        dimensionLabel = "Period ${per.periodId}",
                        periodId = per.periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 06",
                        sourceEntityType = "PERIOD_PROFITABILITY",
                        sourceEntityId = per.periodId,
                        triggerMetric = "profitDeclinePercentage",
                        observedValue = per.profitDeclinePercentage,
                        thresholdValue = thresh,
                        direction = ProfitabilityAlertDirection.ABOVE_THRESHOLD,
                        financialImpact = impact,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Period ${per.periodId} recorded profit decline of ${per.profitDeclinePercentage}% compared to preceding trend.",
                        recommendedActionCode = ManagementActionCode.REVIEW_OVERHEAD_ALLOCATION,
                        ruleId = rule?.ruleId
                    )
                )
            }
        }

        // 6. Evaluate Cross-Dimension (Step 07)
        for (cd in payload.crossDimensionItems) {
            // Profitability Leakage Alert
            val leakRule = findMatchingRule(customRules, ProfitabilityAlertType.PROFITABILITY_LEAKAGE, ProfitabilityAlertDimension.CROSS_DIMENSION)
            val leakThresh = leakRule?.thresholdValue ?: BigDecimal("10000.0000")
            if (cd.leakageAmount > leakThresh) {
                val severity = leakRule?.severity ?: ProfitabilityAlertSeverity.HIGH
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.PROFITABILITY_LEAKAGE, ProfitabilityAlertDimension.CROSS_DIMENSION,
                    cd.entityId, periodId, "leakageAmount", leakRule?.ruleId
                )
                val alertId = "alt-cd-leak-${cd.entityId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.PROFITABILITY_LEAKAGE, severity,
                    ProfitabilityAlertDimension.CROSS_DIMENSION, cd.entityId, cd.leakageAmount, leakThresh, cd.leakageAmount, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.PROFITABILITY_LEAKAGE,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.CROSS_DIMENSION,
                        dimensionId = cd.entityId,
                        dimensionLabel = cd.entityLabel,
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 07",
                        sourceEntityType = "CROSS_DIMENSION_LEAKAGE",
                        sourceEntityId = cd.entityId,
                        triggerMetric = "leakageAmount",
                        observedValue = cd.leakageAmount,
                        thresholdValue = leakThresh,
                        direction = ProfitabilityAlertDirection.ABOVE_THRESHOLD,
                        financialImpact = cd.leakageAmount,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Detected BDT ${cd.leakageAmount} profitability leakage on ${cd.entityLabel} (Primary driver: ${cd.primaryLeakageComponent ?: "UNSPECIFIED"}).",
                        recommendedActionCode = ManagementActionCode.REVIEW_PROFITABILITY_LEAKAGE,
                        ruleId = leakRule?.ruleId
                    )
                )
            }
        }

        // 7. Evaluate Forecasts (Step 08)
        for (f in payload.forecasts) {
            // Projected Loss Risk
            if (f.isLossProjected || f.projectedGrossProfit < ZERO) {
                val impact = f.projectedGrossProfit.abs()
                val rule = findMatchingRule(customRules, ProfitabilityAlertType.FORECAST_LOSS_RISK, ProfitabilityAlertDimension.FORECAST)
                val severity = rule?.severity ?: ProfitabilityAlertSeverity.CRITICAL
                val thresh = rule?.thresholdValue ?: ZERO
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.FORECAST_LOSS_RISK, ProfitabilityAlertDimension.FORECAST,
                    f.forecastId, periodId, "projectedGrossProfit", rule?.ruleId
                )
                val alertId = "alt-fc-loss-${f.forecastId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.FORECAST_LOSS_RISK, severity,
                    ProfitabilityAlertDimension.FORECAST, f.forecastId, f.projectedGrossProfit, thresh, impact, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.FORECAST_LOSS_RISK,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.FORECAST,
                        dimensionId = f.forecastId,
                        dimensionLabel = "Forecast: ${f.targetEntityLabel} (${f.horizon})",
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 08",
                        sourceEntityType = "PROFITABILITY_FORECAST",
                        sourceEntityId = f.forecastId,
                        triggerMetric = "projectedGrossProfit",
                        observedValue = f.projectedGrossProfit,
                        thresholdValue = thresh,
                        direction = ProfitabilityAlertDirection.BELOW_THRESHOLD,
                        financialImpact = impact,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Forecast for '${f.targetEntityLabel}' projects a forward loss of BDT ${f.projectedGrossProfit} (Risk: ${f.riskLevel}).",
                        recommendedActionCode = ManagementActionCode.REVIEW_FORECAST,
                        ruleId = rule?.ruleId
                    )
                )
            }

            // Low Confidence Alert
            val confRule = findMatchingRule(customRules, ProfitabilityAlertType.FORECAST_CONFIDENCE_LOW, ProfitabilityAlertDimension.FORECAST)
            val confThresh = confRule?.thresholdValue ?: BigDecimal("60.0000")
            if (f.confidenceScore < confThresh) {
                val severity = confRule?.severity ?: ProfitabilityAlertSeverity.MEDIUM
                val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                    tenantId, ProfitabilityAlertType.FORECAST_CONFIDENCE_LOW, ProfitabilityAlertDimension.FORECAST,
                    f.forecastId, periodId, "confidenceScore", confRule?.ruleId
                )
                val alertId = "alt-fc-conf-${f.forecastId}-$fp".take(64)
                val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                    alertId, tenantId, projectId, ProfitabilityAlertType.FORECAST_CONFIDENCE_LOW, severity,
                    ProfitabilityAlertDimension.FORECAST, f.forecastId, f.confidenceScore, confThresh, ZERO, fp
                )
                detectedAlerts.add(
                    ProfitabilityAlert(
                        alertId = alertId,
                        tenantId = tenantId,
                        projectId = projectId,
                        alertType = ProfitabilityAlertType.FORECAST_CONFIDENCE_LOW,
                        severity = severity,
                        status = ProfitabilityAlertStatus.DETECTED,
                        dimensionType = ProfitabilityAlertDimension.FORECAST,
                        dimensionId = f.forecastId,
                        dimensionLabel = "Forecast: ${f.targetEntityLabel}",
                        periodId = periodId,
                        sourceModule = "Module 16",
                        sourceStep = "Step 08",
                        sourceEntityType = "PROFITABILITY_FORECAST",
                        sourceEntityId = f.forecastId,
                        triggerMetric = "confidenceScore",
                        observedValue = f.confidenceScore,
                        thresholdValue = confThresh,
                        direction = ProfitabilityAlertDirection.BELOW_THRESHOLD,
                        financialImpact = ZERO,
                        detectedAt = now,
                        fingerprint = fp,
                        integrityHash = hash,
                        explanation = "Forecast for '${f.targetEntityLabel}' has low statistical confidence score of ${f.confidenceScore}/100.0000 (Threshold: $confThresh).",
                        recommendedActionCode = ManagementActionCode.REVIEW_FORECAST,
                        ruleId = confRule?.ruleId
                    )
                )
            }
        }

        // 8. Evaluate Data Integrity Issues
        for (di in payload.integrityIssues) {
            val impact = di.discrepancyAmount
            val alertType = if (di.issueType == "RECONCILIATION") ProfitabilityAlertType.RECONCILIATION_FAILURE else ProfitabilityAlertType.DATA_INTEGRITY_FAILURE
            val severity = if (di.issueType == "RECONCILIATION") ProfitabilityAlertSeverity.HIGH else ProfitabilityAlertSeverity.CRITICAL
            val fp = ProfitabilityAlertMathUtils.generateAlertFingerprint(
                tenantId, alertType, ProfitabilityAlertDimension.BUSINESS,
                di.sourceEntityId, periodId, "discrepancyAmount", null
            )
            val alertId = "alt-di-${di.sourceEntityId}-$fp".take(64)
            val hash = ProfitabilityAlertMathUtils.generateAlertIntegrityHash(
                alertId, tenantId, projectId, alertType, severity,
                ProfitabilityAlertDimension.BUSINESS, di.sourceEntityId, di.discrepancyAmount, ZERO, impact, fp
            )
            detectedAlerts.add(
                ProfitabilityAlert(
                    alertId = alertId,
                    tenantId = tenantId,
                    projectId = projectId,
                    alertType = alertType,
                    severity = severity,
                    status = ProfitabilityAlertStatus.DETECTED,
                    dimensionType = ProfitabilityAlertDimension.BUSINESS,
                    dimensionId = di.sourceEntityId,
                    dimensionLabel = "${di.sourceModule} ${di.sourceStep}",
                    periodId = periodId,
                    sourceModule = di.sourceModule,
                    sourceStep = di.sourceStep,
                    sourceEntityType = "DATA_INTEGRITY_ASSERTION",
                    sourceEntityId = di.sourceEntityId,
                    triggerMetric = "discrepancyAmount",
                    observedValue = di.discrepancyAmount,
                    thresholdValue = ZERO,
                    direction = ProfitabilityAlertDirection.ABOVE_THRESHOLD,
                    financialImpact = impact,
                    detectedAt = now,
                    fingerprint = fp,
                    integrityHash = hash,
                    explanation = di.description,
                    recommendedActionCode = ManagementActionCode.RECONCILE_FINANCIAL_DATA,
                    ruleId = null
                )
            )
        }

        return detectedAlerts
    }

    private fun findMatchingRule(
        rules: List<ProfitabilityAlertRule>,
        alertType: ProfitabilityAlertType,
        dimension: ProfitabilityAlertDimension
    ): ProfitabilityAlertRule? {
        return rules.firstOrNull { it.enabled && it.alertType == alertType && it.dimensionType == dimension }
    }
}
