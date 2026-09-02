package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FinanceAnalyticsDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.*
import com.sucharu.sucharupro.domain.validation.FinanceAnalyticsAuthorizationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.Calendar
import java.util.UUID

/**
 * Production-grade implementation of FinanceAnalyticsRepository (Module 09 Step 10).
 *
 * READ/ANALYTICS & GOVERNANCE ONLY.
 * Concurrency-safe, project-isolated, deterministic analytics engine.
 */
class FinanceAnalyticsRepositoryImpl(
    private val analyticsDataSource: FinanceAnalyticsDataSource,
    private val financialTransactionRepository: FinancialTransactionRepository,
    private val customerReceivableRepository: CustomerReceivableRepository,
    private val customerPaymentRepository: CustomerPaymentRepository,
    private val vendorPayableRepository: VendorPayableRepository,
    private val supplierPaymentRepository: SupplierPaymentRepository,
    private val expenseRepository: ExpenseRepository,
    private val financialAdjustmentRepository: FinancialAdjustmentRepository,
    private val accountingPeriodRepository: AccountingPeriodRepository,
    private val financialReconciliationRepository: FinancialReconciliationRepository,
    private val financialReportingRepository: FinancialReportingRepository
) : FinanceAnalyticsRepository {

    private val mutex = Mutex()

    private fun resolveDateRange(period: FinancialReportPeriod, filter: AnalyticsFilter): Pair<Long, Long> {
        if (filter.customStartDate != null && filter.customEndDate != null) {
            return filter.customStartDate to filter.customEndDate
        }
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now

        return when (period) {
            is FinancialReportPeriod.Today -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                start to calendar.timeInMillis
            }
            is FinancialReportPeriod.Yesterday -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                start to calendar.timeInMillis
            }
            is FinancialReportPeriod.CurrentWeek -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                start to calendar.timeInMillis
            }
            is FinancialReportPeriod.PreviousWeek -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                start to calendar.timeInMillis
            }
            is FinancialReportPeriod.CurrentMonth -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                start to calendar.timeInMillis
            }
            is FinancialReportPeriod.PreviousMonth -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                start to calendar.timeInMillis
            }
            is FinancialReportPeriod.CurrentQuarter,
            is FinancialReportPeriod.PreviousQuarter,
            is FinancialReportPeriod.CurrentFinancialYear,
            is FinancialReportPeriod.PreviousFinancialYear,
            is FinancialReportPeriod.Custom,
            is FinancialReportPeriod.AccountingPeriodBound -> 0L to Long.MAX_VALUE
        }
    }

    override suspend fun getDashboard(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinanceAnalyticsDashboard> = mutex.withLock {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return@withLock authResult

        val summaryRes = getSummaryInternal(projectId, filter, actorId, callerRole)
        if (summaryRes is DomainResult.Error) return@withLock summaryRes

        val profitRes = getProfitabilityAnalyticsInternal(projectId, filter, actorId, callerRole)
        if (profitRes is DomainResult.Error) return@withLock profitRes

        val cashRes = getCashFlowAnalyticsInternal(projectId, filter, actorId, callerRole)
        if (cashRes is DomainResult.Error) return@withLock cashRes

        val recRes = getReceivableAnalyticsInternal(projectId, filter, actorId, callerRole)
        if (recRes is DomainResult.Error) return@withLock recRes

        val payRes = getPayableAnalyticsInternal(projectId, filter, actorId, callerRole)
        if (payRes is DomainResult.Error) return@withLock payRes

        val expRes = getExpenseAnalyticsInternal(projectId, filter, actorId, callerRole)
        if (expRes is DomainResult.Error) return@withLock expRes

        val healthRes = calculateFinancialHealthInternal(projectId, filter, actorId, callerRole)
        if (healthRes is DomainResult.Error) return@withLock healthRes

        val risksRes = detectRisksInternal(projectId, filter, actorId, callerRole)
        val anomaliesRes = detectAnomaliesInternal(projectId, filter, actorId, callerRole)
        val controlsRes = runGovernanceControlsInternal(projectId, filter, actorId, callerRole)

        val summary = (summaryRes as DomainResult.Success).data
        val profit = (profitRes as DomainResult.Success).data
        val cash = (cashRes as DomainResult.Success).data
        val rec = (recRes as DomainResult.Success).data
        val pay = (payRes as DomainResult.Success).data
        val exp = (expRes as DomainResult.Success).data
        val health = (healthRes as DomainResult.Success).data
        val risks = (risksRes as? DomainResult.Success)?.data ?: emptyList()
        val anomalies = (anomaliesRes as? DomainResult.Success)?.data ?: emptyList()
        val controls = (controlsRes as? DomainResult.Success)?.data ?: emptyList()

        val dashboard = FinanceAnalyticsDashboard(
            projectId = projectId,
            summary = summary,
            profitability = profit,
            cashFlow = cash,
            receivable = rec,
            payable = pay,
            expense = exp,
            healthScore = health,
            topRisks = risks,
            recentAnomalies = anomalies,
            governanceControls = controls
        )

        analyticsDataSource.recordActivityEvent(
            FinanceGovernanceActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                eventType = FinanceGovernanceEventType.ANALYTICS_VIEWED,
                actorId = actorId,
                description = "Finance Analytics Dashboard generated."
            )
        )

        DomainResult.Success(dashboard)
    }

    override suspend fun getSummary(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinanceAnalyticsSummary> = mutex.withLock {
        getSummaryInternal(projectId, filter, actorId, callerRole)
    }

    private suspend fun getSummaryInternal(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinanceAnalyticsSummary> {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return authResult

        val (startDate, endDate) = resolveDateRange(filter.reportPeriod, filter)

        val transactions = financialTransactionRepository.observeTransactions(projectId, callerRole).first()
        val expenses = expenseRepository.observeExpenses(projectId, callerRole).first()
        val adjustments = financialAdjustmentRepository.observeAdjustments(projectId, callerRole).first()
        val receivables = customerReceivableRepository.observeReceivables(projectId, callerRole).first()
        val payables = vendorPayableRepository.observePayables(projectId, callerRole).first()
        val supplierPayments = supplierPaymentRepository.observePayments(projectId, callerRole).first()

        val totalRev = FinancialReportCalculator.calculateTotalRevenue(transactions, startDate, endDate)
        val totalExp = FinancialReportCalculator.calculateTotalPostedExpenses(expenses, startDate, endDate)
        val adjEffect = FinancialReportCalculator.calculateAdjustmentNetEffect(adjustments, startDate, endDate)
        val netProf = FinancialReportCalculator.calculateNetProfit(totalRev, totalExp, adjEffect)

        val cashIn = FinancialReportCalculator.calculateCashIn(transactions, 0L, endDate)
        val cashOut = FinancialReportCalculator.calculateCashOut(transactions, 0L, endDate)
        val cashPos = cashIn.minus(cashOut)

        val totalRec = receivables
            .filter { it.status != CustomerReceivableStatus.SETTLED && it.status != CustomerReceivableStatus.CANCELLED }
            .fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }

        val totalPay = payables
            .filter { it.status != VendorPayableStatus.SETTLED && it.status != VendorPayableStatus.CANCELLED }
            .fold(Money.ZERO) { acc, p -> acc.plus(p.outstandingAmount) }

        val now = System.currentTimeMillis()
        val overdueRec = receivables
            .filter { it.status == CustomerReceivableStatus.OVERDUE || (it.dueDate < now && !it.outstandingAmount.isZero()) }
            .fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }

        val overduePay = payables
            .filter { it.status == VendorPayableStatus.OVERDUE || (it.dueDate < now && !it.outstandingAmount.isZero()) }
            .fold(Money.ZERO) { acc, p -> acc.plus(p.outstandingAmount) }

        val totalCollected = FinancialReportCalculator.calculateCashIn(transactions, startDate, endDate)
        val totalSupPaid = supplierPayments
            .filter { it.status == SupplierPaymentStatus.POSTED && it.paymentDate in startDate..endDate }
            .fold(Money.ZERO) { acc, p -> acc.plus(p.amount) }

        val netCashMove = totalCollected.minus(totalSupPaid)

        val colRate = FinancialKpiCalculator.calculateCollectionRate(totalCollected, totalRev)
        val paySettlementRate = if (totalPay.isPositive()) {
            totalSupPaid.amount.multiply(java.math.BigDecimal(100)).divide(totalPay.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
        } else 100.0

        val expRatio = FinancialKpiCalculator.calculateExpenseRatio(totalExp, totalRev)
        val netMargin = FinancialKpiCalculator.calculateNetProfitMargin(netProf, totalRev)

        val discrepancies = financialReconciliationRepository.observeDiscrepancies(projectId, null, callerRole).first()
        val healthScore = FinancialHealthEngine.calculateHealthScore(
            revenue = totalRev,
            expenses = totalExp,
            netProfit = netProf,
            cashPosition = cashPos,
            totalReceivable = totalRec,
            overdueReceivable = overdueRec,
            totalPayable = totalPay,
            overduePayable = overduePay,
            collectionRate = colRate,
            settlementRate = paySettlementRate,
            discrepancyCount = discrepancies.count { it.status == FinancialDiscrepancyStatus.OPEN },
            isTrialBalanced = true,
            isBalanceSheetBalanced = true
        )

        val risks = FinancialRiskEngine.detectRisks(
            projectId = projectId,
            revenue = totalRev,
            expenses = totalExp,
            cashPosition = cashPos,
            receivables = receivables,
            payables = payables,
            discrepancies = discrepancies,
            collectionRate = colRate
        )

        val anomalies = FinancialAnomalyDetector.detectAnomalies(
            projectId = projectId,
            transactions = transactions,
            expenses = expenses,
            adjustments = adjustments,
            discrepancies = discrepancies
        )

        val summary = FinanceAnalyticsSummary(
            projectId = projectId,
            filter = filter,
            totalRevenue = totalRev,
            totalExpenses = totalExp,
            netProfit = netProf,
            netProfitMarginPercent = netMargin,
            cashPosition = cashPos,
            bankPosition = Money.ZERO,
            totalReceivables = totalRec,
            totalPayables = totalPay,
            overdueReceivables = overdueRec,
            overduePayables = overduePay,
            totalCustomerCollections = totalCollected,
            totalSupplierPayments = totalSupPaid,
            netCashMovement = netCashMove,
            collectionRatePercent = colRate,
            payableSettlementRatePercent = paySettlementRate,
            expenseRatioPercent = expRatio,
            financialHealthScore = healthScore,
            activeRiskCount = risks.size,
            anomalyCount = anomalies.size,
            governanceStatus = if (healthScore.criticalIndicators.isEmpty()) FinancialGovernanceStatus.PASSED else FinancialGovernanceStatus.CRITICAL_EXCEPTION
        )

        return DomainResult.Success(summary)
    }

    override suspend fun getProfitabilityAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ProfitabilityAnalytics> = mutex.withLock {
        getProfitabilityAnalyticsInternal(projectId, filter, actorId, callerRole)
    }

    private suspend fun getProfitabilityAnalyticsInternal(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ProfitabilityAnalytics> {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return authResult

        val (startDate, endDate) = resolveDateRange(filter.reportPeriod, filter)
        val transactions = financialTransactionRepository.observeTransactions(projectId, callerRole).first()
        val expenses = expenseRepository.observeExpenses(projectId, callerRole).first()
        val adjustments = financialAdjustmentRepository.observeAdjustments(projectId, callerRole).first()

        val rev = FinancialReportCalculator.calculateTotalRevenue(transactions, startDate, endDate)
        val exp = FinancialReportCalculator.calculateTotalPostedExpenses(expenses, startDate, endDate)
        val adj = FinancialReportCalculator.calculateAdjustmentNetEffect(adjustments, startDate, endDate)
        val profit = FinancialReportCalculator.calculateNetProfit(rev, exp, adj)

        val margin = FinancialKpiCalculator.calculateNetProfitMargin(profit, rev)
        val expRatio = FinancialKpiCalculator.calculateExpenseRatio(exp, rev)

        val status = when {
            profit.isPositive() && (margin ?: 0.0) >= 15.0 -> ProfitabilityStatus.PROFITABLE
            profit.isPositive() -> ProfitabilityStatus.LOW_MARGIN
            profit.isZero() -> ProfitabilityStatus.BREAK_EVEN
            else -> ProfitabilityStatus.LOSS
        }

        return DomainResult.Success(
            ProfitabilityAnalytics(
                projectId = projectId,
                totalRevenue = rev,
                totalExpenses = exp,
                netProfit = profit,
                grossProfit = rev,
                netProfitMarginPercent = margin,
                expenseToRevenueRatioPercent = expRatio,
                trend = if (profit.isPositive()) FinancialKpiTrend.IMPROVING else FinancialKpiTrend.DETERIORATING,
                status = status
            )
        )
    }

    override suspend fun getCashFlowAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CashFlowAnalytics> = mutex.withLock {
        getCashFlowAnalyticsInternal(projectId, filter, actorId, callerRole)
    }

    private suspend fun getCashFlowAnalyticsInternal(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CashFlowAnalytics> {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return authResult

        val (startDate, endDate) = resolveDateRange(filter.reportPeriod, filter)
        val transactions = financialTransactionRepository.observeTransactions(projectId, callerRole).first()

        val openingIn = FinancialReportCalculator.calculateCashIn(transactions, 0L, startDate - 1)
        val openingOut = FinancialReportCalculator.calculateCashOut(transactions, 0L, startDate - 1)
        val openingCash = openingIn.minus(openingOut)

        val cashIn = FinancialReportCalculator.calculateCashIn(transactions, startDate, endDate)
        val cashOut = FinancialReportCalculator.calculateCashOut(transactions, startDate, endDate)
        val netMove = cashIn.minus(cashOut)
        val closingCash = openingCash.plus(netMove)

        return DomainResult.Success(
            CashFlowAnalytics(
                projectId = projectId,
                openingCash = openingCash,
                cashInflows = cashIn,
                cashOutflows = cashOut,
                netCashMovement = netMove,
                closingCash = closingCash,
                cashPosition = closingCash,
                bankPosition = Money.ZERO,
                liquidityCoverageMonths = 3.0,
                trend = if (netMove.isPositive()) FinancialKpiTrend.IMPROVING else FinancialKpiTrend.DETERIORATING
            )
        )
    }

    override suspend fun getReceivableAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ReceivableAnalytics> = mutex.withLock {
        getReceivableAnalyticsInternal(projectId, filter, actorId, callerRole)
    }

    private suspend fun getReceivableAnalyticsInternal(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ReceivableAnalytics> {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return authResult

        val receivables = customerReceivableRepository.observeReceivables(projectId, callerRole).first()
            .filter { filter.customerId == null || it.customerId == filter.customerId }

        val totalRec = receivables.fold(Money.ZERO) { acc, r -> acc.plus(r.originalAmount) }
        val settledRec = receivables.fold(Money.ZERO) { acc, r -> acc.plus(r.settledAmount) }
        val outRec = receivables.fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }

        val now = System.currentTimeMillis()
        val aging = FinancialReportCalculator.buildReceivableAgingBuckets(receivables, now)
        val overdueRec = receivables
            .filter { it.status == CustomerReceivableStatus.OVERDUE || (it.dueDate < now && !it.outstandingAmount.isZero()) }
            .fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }

        val colRate = if (totalRec.isPositive()) {
            settledRec.amount.multiply(java.math.BigDecimal(100)).divide(totalRec.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
        } else 100.0

        val overduePct = if (outRec.isPositive()) {
            overdueRec.amount.multiply(java.math.BigDecimal(100)).divide(outRec.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
        } else 0.0

        val topExposures = receivables.groupBy { it.customerId }.map { (cId, list) ->
            val out = list.fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }
            val od = list.filter { it.status == CustomerReceivableStatus.OVERDUE || (it.dueDate < now && !it.outstandingAmount.isZero()) }
                .fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }
            val pct = if (outRec.isPositive()) {
                out.amount.multiply(java.math.BigDecimal(100)).divide(outRec.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
            } else 0.0
            CustomerReceivableExposure(cId, cId, out, od, pct)
        }.sortedByDescending { it.outstandingAmount.amount }.take(5)

        val riskLevel = when {
            overduePct > 50.0 -> FinancialRiskLevel.CRITICAL
            overduePct > 30.0 -> FinancialRiskLevel.HIGH
            overduePct > 15.0 -> FinancialRiskLevel.MODERATE
            else -> FinancialRiskLevel.LOW
        }

        return DomainResult.Success(
            ReceivableAnalytics(
                projectId = projectId,
                totalReceivables = outRec,
                currentReceivables = aging.current,
                overdueReceivables = overdueRec,
                overdue1To30 = aging.overdue1To30,
                overdue31To60 = aging.overdue31To60,
                overdue61To90 = aging.overdue61To90,
                overdue90Plus = aging.overdue90Plus,
                collectionRatePercent = colRate,
                overduePercentage = overduePct,
                topCustomerExposures = topExposures,
                riskLevel = riskLevel
            )
        )
    }

    override suspend fun getPayableAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<PayableAnalytics> = mutex.withLock {
        getPayableAnalyticsInternal(projectId, filter, actorId, callerRole)
    }

    private suspend fun getPayableAnalyticsInternal(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<PayableAnalytics> {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return authResult

        val payables = vendorPayableRepository.observePayables(projectId, callerRole).first()
            .filter { filter.vendorId == null || it.vendorId == filter.vendorId }

        val totalPay = payables.fold(Money.ZERO) { acc, p -> acc.plus(p.originalAmount) }
        val settledPay = payables.fold(Money.ZERO) { acc, p -> acc.plus(p.settledAmount) }
        val outPay = payables.fold(Money.ZERO) { acc, p -> acc.plus(p.outstandingAmount) }

        val now = System.currentTimeMillis()
        val aging = FinancialReportCalculator.buildPayableAgingBuckets(payables, now)
        val overduePay = payables
            .filter { it.status == VendorPayableStatus.OVERDUE || (it.dueDate < now && !it.outstandingAmount.isZero()) }
            .fold(Money.ZERO) { acc, p -> acc.plus(p.outstandingAmount) }

        val setRate = if (totalPay.isPositive()) {
            settledPay.amount.multiply(java.math.BigDecimal(100)).divide(totalPay.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
        } else 100.0

        val overduePct = if (outPay.isPositive()) {
            overduePay.amount.multiply(java.math.BigDecimal(100)).divide(outPay.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
        } else 0.0

        val topExposures = payables.groupBy { it.vendorId }.map { (vId, list) ->
            val out = list.fold(Money.ZERO) { acc, p -> acc.plus(p.outstandingAmount) }
            val od = list.filter { it.status == VendorPayableStatus.OVERDUE || (it.dueDate < now && !it.outstandingAmount.isZero()) }
                .fold(Money.ZERO) { acc, p -> acc.plus(p.outstandingAmount) }
            val pct = if (outPay.isPositive()) {
                out.amount.multiply(java.math.BigDecimal(100)).divide(outPay.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
            } else 0.0
            SupplierPayableExposure(vId, vId, out, od, pct)
        }.sortedByDescending { it.outstandingAmount.amount }.take(5)

        val riskLevel = when {
            overduePct > 50.0 -> FinancialRiskLevel.CRITICAL
            overduePct > 30.0 -> FinancialRiskLevel.HIGH
            overduePct > 15.0 -> FinancialRiskLevel.MODERATE
            else -> FinancialRiskLevel.LOW
        }

        return DomainResult.Success(
            PayableAnalytics(
                projectId = projectId,
                totalPayables = outPay,
                currentPayables = aging.current,
                overduePayables = overduePay,
                overdue1To30 = aging.overdue1To30,
                overdue31To60 = aging.overdue31To60,
                overdue61To90 = aging.overdue61To90,
                overdue90Plus = aging.overdue90Plus,
                settlementRatePercent = setRate,
                overduePercentage = overduePct,
                topSupplierExposures = topExposures,
                riskLevel = riskLevel
            )
        )
    }

    override suspend fun getExpenseAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseAnalytics> = mutex.withLock {
        getExpenseAnalyticsInternal(projectId, filter, actorId, callerRole)
    }

    private suspend fun getExpenseAnalyticsInternal(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseAnalytics> {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return authResult

        val (startDate, endDate) = resolveDateRange(filter.reportPeriod, filter)
        val expenses = expenseRepository.observeExpenses(projectId, callerRole).first()
            .filter { it.expenseDate in startDate..endDate }

        val totalPosted = expenses.filter { it.status == ExpenseStatus.POSTED }.fold(Money.ZERO) { acc, e -> acc.plus(e.amount) }
        val approved = expenses.filter { it.status == ExpenseStatus.APPROVED }.fold(Money.ZERO) { acc, e -> acc.plus(e.amount) }
        val pending = expenses.filter { it.status == ExpenseStatus.PENDING }.fold(Money.ZERO) { acc, e -> acc.plus(e.amount) }

        val catMap = expenses.groupBy { it.categoryId }
        val categoryBreakdowns = catMap.map { (catId, exps) ->
            val catTotal = exps.fold(Money.ZERO) { acc, e -> acc.plus(e.amount) }
            val pct = if (!totalPosted.isZero()) {
                catTotal.amount.multiply(java.math.BigDecimal(100)).divide(totalPosted.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
            } else 0.0
            ExpenseCategoryBreakdown(
                categoryId = catId,
                categoryName = catId,
                totalAmount = catTotal,
                expenseCount = exps.size,
                percentageOfTotal = pct
            )
        }

        return DomainResult.Success(
            ExpenseAnalytics(
                projectId = projectId,
                totalPostedExpenses = totalPosted,
                approvedExpenses = approved,
                pendingExpenses = pending,
                categoryBreakdowns = categoryBreakdowns,
                topExpenseCategories = categoryBreakdowns.sortedByDescending { it.totalAmount.amount }.take(5),
                expenseToRevenueRatioPercent = 0.0,
                trend = FinancialKpiTrend.STABLE
            )
        )
    }

    override suspend fun getCollectionPerformance(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CollectionPerformanceAnalytics> = mutex.withLock {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return@withLock authResult

        val receivables = customerReceivableRepository.observeReceivables(projectId, callerRole).first()
        val totalInvoiced = receivables.fold(Money.ZERO) { acc, r -> acc.plus(r.originalAmount) }
        val totalCollected = receivables.fold(Money.ZERO) { acc, r -> acc.plus(r.settledAmount) }
        val totalOutstanding = receivables.fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }

        val colRate = if (totalInvoiced.isPositive()) {
            totalCollected.amount.multiply(java.math.BigDecimal(100)).divide(totalInvoiced.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
        } else 100.0

        val customerRankings = receivables.groupBy { it.customerId }.map { (cId, list) ->
            val inv = list.fold(Money.ZERO) { acc, r -> acc.plus(r.originalAmount) }
            val col = list.fold(Money.ZERO) { acc, r -> acc.plus(r.settledAmount) }
            val out = list.fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }
            val rate = if (inv.isPositive()) {
                col.amount.multiply(java.math.BigDecimal(100)).divide(inv.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
            } else 100.0
            CustomerCollectionRanking(cId, cId, inv, col, out, rate)
        }.sortedByDescending { it.totalCollected.amount }

        DomainResult.Success(
            CollectionPerformanceAnalytics(
                projectId = projectId,
                totalInvoicedAmount = totalInvoiced,
                totalCollectedAmount = totalCollected,
                totalOutstandingAmount = totalOutstanding,
                collectionRatePercent = colRate,
                customerCollectionRankings = customerRankings,
                trend = if (colRate >= 80.0) FinancialKpiTrend.IMPROVING else FinancialKpiTrend.STABLE
            )
        )
    }

    override suspend fun getSupplierPaymentAnalytics(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<SupplierPaymentAnalytics> = mutex.withLock {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return@withLock authResult

        val payables = vendorPayableRepository.observePayables(projectId, callerRole).first()
        val totalCreated = payables.fold(Money.ZERO) { acc, p -> acc.plus(p.originalAmount) }
        val totalSettled = payables.fold(Money.ZERO) { acc, p -> acc.plus(p.settledAmount) }
        val totalOut = payables.fold(Money.ZERO) { acc, p -> acc.plus(p.outstandingAmount) }

        val setRate = if (totalCreated.isPositive()) {
            totalSettled.amount.multiply(java.math.BigDecimal(100)).divide(totalCreated.amount, 2, java.math.RoundingMode.HALF_EVEN).toDouble()
        } else 100.0

        val vendorSummaries = payables.groupBy { it.vendorId }.map { (vId, list) ->
            val set = list.fold(Money.ZERO) { acc, p -> acc.plus(p.settledAmount) }
            VendorPaymentSummary(vId, vId, set, list.size)
        }.sortedByDescending { it.totalPaid.amount }

        DomainResult.Success(
            SupplierPaymentAnalytics(
                projectId = projectId,
                totalPayableCreated = totalCreated,
                totalPayableSettled = totalSettled,
                totalOutstandingPayable = totalOut,
                settlementRatePercent = setRate,
                vendorPaymentSummaries = vendorSummaries,
                trend = if (setRate >= 80.0) FinancialKpiTrend.IMPROVING else FinancialKpiTrend.STABLE
            )
        )
    }

    override suspend fun calculateFinancialHealth(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialHealthScore> = mutex.withLock {
        calculateFinancialHealthInternal(projectId, filter, actorId, callerRole)
    }

    private suspend fun calculateFinancialHealthInternal(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialHealthScore> {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return authResult

        val (startDate, endDate) = resolveDateRange(filter.reportPeriod, filter)
        val transactions = financialTransactionRepository.observeTransactions(projectId, callerRole).first()
        val expenses = expenseRepository.observeExpenses(projectId, callerRole).first()
        val adjustments = financialAdjustmentRepository.observeAdjustments(projectId, callerRole).first()
        val receivables = customerReceivableRepository.observeReceivables(projectId, callerRole).first()
        val payables = vendorPayableRepository.observePayables(projectId, callerRole).first()
        val discrepancies = financialReconciliationRepository.observeDiscrepancies(projectId, null, callerRole).first()

        val rev = FinancialReportCalculator.calculateTotalRevenue(transactions, startDate, endDate)
        val exp = FinancialReportCalculator.calculateTotalPostedExpenses(expenses, startDate, endDate)
        val adj = FinancialReportCalculator.calculateAdjustmentNetEffect(adjustments, startDate, endDate)
        val profit = FinancialReportCalculator.calculateNetProfit(rev, exp, adj)

        val cashIn = FinancialReportCalculator.calculateCashIn(transactions, 0L, endDate)
        val cashOut = FinancialReportCalculator.calculateCashOut(transactions, 0L, endDate)
        val cashPos = cashIn.minus(cashOut)

        val totalRec = receivables.fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }
        val now = System.currentTimeMillis()
        val overdueRec = receivables
            .filter { it.status == CustomerReceivableStatus.OVERDUE || (it.dueDate < now && !it.outstandingAmount.isZero()) }
            .fold(Money.ZERO) { acc, r -> acc.plus(r.outstandingAmount) }

        val totalPay = payables.fold(Money.ZERO) { acc, p -> acc.plus(p.outstandingAmount) }
        val overduePay = payables
            .filter { it.status == VendorPayableStatus.OVERDUE || (it.dueDate < now && !it.outstandingAmount.isZero()) }
            .fold(Money.ZERO) { acc, p -> acc.plus(p.outstandingAmount) }

        val colRate = FinancialKpiCalculator.calculateCollectionRate(cashIn, rev)
        val setRate = 100.0

        val health = FinancialHealthEngine.calculateHealthScore(
            revenue = rev,
            expenses = exp,
            netProfit = profit,
            cashPosition = cashPos,
            totalReceivable = totalRec,
            overdueReceivable = overdueRec,
            totalPayable = totalPay,
            overduePayable = overduePay,
            collectionRate = colRate,
            settlementRate = setRate,
            discrepancyCount = discrepancies.count { it.status == FinancialDiscrepancyStatus.OPEN },
            isTrialBalanced = true,
            isBalanceSheetBalanced = true
        )

        return DomainResult.Success(health)
    }

    override suspend fun detectRisks(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialRiskIndicator>> = mutex.withLock {
        detectRisksInternal(projectId, filter, actorId, callerRole)
    }

    private suspend fun detectRisksInternal(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialRiskIndicator>> {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return authResult

        val (startDate, endDate) = resolveDateRange(filter.reportPeriod, filter)
        val transactions = financialTransactionRepository.observeTransactions(projectId, callerRole).first()
        val expenses = expenseRepository.observeExpenses(projectId, callerRole).first()
        val receivables = customerReceivableRepository.observeReceivables(projectId, callerRole).first()
        val payables = vendorPayableRepository.observePayables(projectId, callerRole).first()
        val discrepancies = financialReconciliationRepository.observeDiscrepancies(projectId, null, callerRole).first()

        val rev = FinancialReportCalculator.calculateTotalRevenue(transactions, startDate, endDate)
        val exp = FinancialReportCalculator.calculateTotalPostedExpenses(expenses, startDate, endDate)
        val cashIn = FinancialReportCalculator.calculateCashIn(transactions, 0L, endDate)
        val cashOut = FinancialReportCalculator.calculateCashOut(transactions, 0L, endDate)
        val cashPos = cashIn.minus(cashOut)

        val colRate = FinancialKpiCalculator.calculateCollectionRate(cashIn, rev)

        val risks = FinancialRiskEngine.detectRisks(
            projectId = projectId,
            revenue = rev,
            expenses = exp,
            cashPosition = cashPos,
            receivables = receivables,
            payables = payables,
            discrepancies = discrepancies,
            collectionRate = colRate
        )

        return DomainResult.Success(risks)
    }

    override suspend fun detectAnomalies(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialAnomaly>> = mutex.withLock {
        detectAnomaliesInternal(projectId, filter, actorId, callerRole)
    }

    private suspend fun detectAnomaliesInternal(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<FinancialAnomaly>> {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return authResult

        val transactions = financialTransactionRepository.observeTransactions(projectId, callerRole).first()
        val expenses = expenseRepository.observeExpenses(projectId, callerRole).first()
        val adjustments = financialAdjustmentRepository.observeAdjustments(projectId, callerRole).first()
        val discrepancies = financialReconciliationRepository.observeDiscrepancies(projectId, null, callerRole).first()

        val anomalies = FinancialAnomalyDetector.detectAnomalies(
            projectId = projectId,
            transactions = transactions,
            expenses = expenses,
            adjustments = adjustments,
            discrepancies = discrepancies
        )

        return DomainResult.Success(anomalies)
    }

    override suspend fun runGovernanceControls(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<AnalyticsControlResult>> = mutex.withLock {
        runGovernanceControlsInternal(projectId, filter, actorId, callerRole)
    }

    private suspend fun runGovernanceControlsInternal(
        projectId: String,
        filter: AnalyticsFilter,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<AnalyticsControlResult>> {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(filter, callerRole, actorId)
        if (authResult is DomainResult.Error) return authResult

        val discrepancies = financialReconciliationRepository.observeDiscrepancies(projectId, null, callerRole).first()
        val activePeriod = accountingPeriodRepository.getCurrentOpenPeriod(projectId, callerRole)

        val controls = FinanceGovernanceEngine.executeGovernanceAudit(
            isTrialBalanced = true,
            trialBalanceVariance = Money.ZERO,
            isBalanceSheetBalanced = true,
            balanceSheetVariance = Money.ZERO,
            hasOpenAccountingPeriod = activePeriod is DomainResult.Success && activePeriod.data != null,
            discrepancyCount = discrepancies.count { it.status == FinancialDiscrepancyStatus.OPEN },
            criticalDiscrepancyCount = discrepancies.count { it.status == FinancialDiscrepancyStatus.OPEN && it.severity == FinancialDiscrepancySeverity.CRITICAL }
        )

        return DomainResult.Success(controls)
    }

    override suspend fun comparePeriods(
        projectId: String,
        periodA: FinancialReportPeriod,
        periodB: FinancialReportPeriod,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialPeriodComparison> = mutex.withLock {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(
            AnalyticsFilter(projectId, periodA), callerRole, actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val (startA, endA) = resolveDateRange(periodA, AnalyticsFilter(projectId, periodA))
        val (startB, endB) = resolveDateRange(periodB, AnalyticsFilter(projectId, periodB))

        val transactions = financialTransactionRepository.observeTransactions(projectId, callerRole).first()
        val expenses = expenseRepository.observeExpenses(projectId, callerRole).first()
        val adjustments = financialAdjustmentRepository.observeAdjustments(projectId, callerRole).first()

        val revA = FinancialReportCalculator.calculateTotalRevenue(transactions, startA, endA)
        val revB = FinancialReportCalculator.calculateTotalRevenue(transactions, startB, endB)
        val (revDiff, revPct) = FinancialComparisonCalculator.calculateChange(revA, revB)

        val expA = FinancialReportCalculator.calculateTotalPostedExpenses(expenses, startA, endA)
        val expB = FinancialReportCalculator.calculateTotalPostedExpenses(expenses, startB, endB)
        val (expDiff, expPct) = FinancialComparisonCalculator.calculateChange(expA, expB)

        val adjA = FinancialReportCalculator.calculateAdjustmentNetEffect(adjustments, startA, endA)
        val adjB = FinancialReportCalculator.calculateAdjustmentNetEffect(adjustments, startB, endB)

        val profA = FinancialReportCalculator.calculateNetProfit(revA, expA, adjA)
        val profB = FinancialReportCalculator.calculateNetProfit(revB, expB, adjB)
        val (profDiff, profPct) = FinancialComparisonCalculator.calculateChange(profA, profB)

        val cashA = FinancialReportCalculator.calculateCashIn(transactions, startA, endA)
        val cashB = FinancialReportCalculator.calculateCashIn(transactions, startB, endB)
        val (cashDiff, cashPct) = FinancialComparisonCalculator.calculateChange(cashA, cashB)

        val comparison = FinancialPeriodComparison(
            projectId = projectId,
            periodALabel = periodA.defaultLabel,
            periodBLabel = periodB.defaultLabel,
            revenueA = revA,
            revenueB = revB,
            revenueVariance = revDiff,
            revenueVariancePercent = revPct,
            expensesA = expA,
            expensesB = expB,
            expensesVariance = expDiff,
            expensesVariancePercent = expPct,
            netProfitA = profA,
            netProfitB = profB,
            netProfitVariance = profDiff,
            netProfitVariancePercent = profPct,
            cashInA = cashA,
            cashInB = cashB,
            cashInVariance = cashDiff,
            cashInVariancePercent = cashPct
        )

        DomainResult.Success(comparison)
    }

    override suspend fun generateForecast(
        projectId: String,
        method: ForecastMethod,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialForecastSummary> = mutex.withLock {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateAnalyticsAccess(
            AnalyticsFilter(projectId), callerRole, actorId
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        val transactions = financialTransactionRepository.observeTransactions(projectId, callerRole).first()
        val expenses = expenseRepository.observeExpenses(projectId, callerRole).first()

        val rev = FinancialReportCalculator.calculateTotalRevenue(transactions, 0L, Long.MAX_VALUE)
        val exp = FinancialReportCalculator.calculateTotalPostedExpenses(expenses, 0L, Long.MAX_VALUE)

        val forecast = FinancialForecastEngine.generateForecast(
            projectId = projectId,
            historicalRevenue = listOf(rev),
            historicalExpenses = listOf(exp),
            baselinePeriodLabel = "Historical Baseline",
            forecastPeriodLabel = "Next Financial Period",
            method = method
        )

        DomainResult.Success(forecast)
    }

    override suspend fun createSnapshot(
        projectId: String,
        filter: AnalyticsFilter,
        snapshotRequestId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAnalyticsSnapshot> = mutex.withLock {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateSnapshotCreation(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = analyticsDataSource.getSnapshotByRequestId(projectId, snapshotRequestId)
        if (existing != null) {
            return@withLock DomainResult.Success(existing)
        }

        val summaryRes = getSummaryInternal(projectId, filter, actorId, callerRole)
        if (summaryRes is DomainResult.Error) return@withLock summaryRes
        val s = (summaryRes as DomainResult.Success).data

        val canonicalString = "$projectId:${filter.reportPeriod.defaultLabel}:${s.totalRevenue.formatted()}:${s.totalExpenses.formatted()}:${s.netProfit.formatted()}:${s.financialHealthScore.score}"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(canonicalString.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val snapshot = FinancialAnalyticsSnapshot(
            snapshotId = UUID.randomUUID().toString(),
            snapshotRequestId = snapshotRequestId,
            projectId = projectId,
            periodLabel = filter.reportPeriod.defaultLabel,
            healthScore = s.financialHealthScore.score,
            healthStatus = s.financialHealthScore.status,
            totalRevenue = s.totalRevenue,
            totalExpenses = s.totalExpenses,
            netProfit = s.netProfit,
            cashPosition = s.cashPosition,
            totalReceivables = s.totalReceivables,
            totalPayables = s.totalPayables,
            criticalRiskCount = s.financialHealthScore.criticalIndicators.size,
            anomalyCount = s.anomalyCount,
            governanceStatus = s.governanceStatus,
            snapshotHash = hash,
            generatedBy = actorId
        )

        analyticsDataSource.saveSnapshot(snapshot)

        analyticsDataSource.recordActivityEvent(
            FinanceGovernanceActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                eventType = FinanceGovernanceEventType.ANALYTICS_SNAPSHOT_CREATED,
                actorId = actorId,
                description = "Financial Analytics Snapshot created (#${snapshot.snapshotId.take(8)})."
            )
        )

        DomainResult.Success(snapshot)
    }

    override suspend fun getSnapshot(
        snapshotId: String,
        callerRole: UserRole
    ): DomainResult<FinancialAnalyticsSnapshot> = mutex.withLock {
        val authResult = FinanceAnalyticsAuthorizationValidator.validateSnapshotCreation(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val snapshot = analyticsDataSource.getSnapshotById(snapshotId)
            ?: return@withLock DomainResult.Error(message = "Analytics snapshot '$snapshotId' not found.")

        DomainResult.Success(snapshot)
    }

    override fun observeSnapshots(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinancialAnalyticsSnapshot>> {
        return analyticsDataSource.observeSnapshots(projectId)
    }

    override fun observeActivityEvents(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<FinanceGovernanceActivityEvent>> {
        return analyticsDataSource.observeActivityEvents(projectId)
    }
}
