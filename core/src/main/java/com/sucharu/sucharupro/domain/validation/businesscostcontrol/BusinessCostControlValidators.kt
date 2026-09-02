package com.sucharu.sucharupro.domain.validation.businesscostcontrol

import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal
import java.math.RoundingMode

object BusinessCostControlValidators {

    private val SUPPORTED_CURRENCIES = setOf("BDT", "USD", "EUR", "GBP", "INR")

    fun validatePrecision(amount: BigDecimal, fieldName: String = "Amount"): DomainResult<Unit> {
        if (amount.scale() > 4) {
            return DomainResult.Error(message = "$fieldName has scale ${amount.scale()}, which exceeds maximum allowed precision of 4 decimal places.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateCurrency(currency: String): DomainResult<Unit> {
        val upper = currency.trim().uppercase()
        if (upper !in SUPPORTED_CURRENCIES) {
            return DomainResult.Error(message = "Unsupported currency '$currency'. Supported currencies are: ${SUPPORTED_CURRENCIES.joinToString()}")
        }
        return DomainResult.Success(Unit)
    }

    fun validateFinancialPeriod(
        periodCode: String,
        periodName: String,
        startDate: Long,
        endDate: Long,
        tenantId: String,
        projectId: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (periodCode.trim().length < 2) return DomainResult.Error(message = "Period code must be at least 2 characters.")
        if (periodName.trim().length < 2) return DomainResult.Error(message = "Period name must be at least 2 characters.")
        if (endDate < startDate) return DomainResult.Error(message = "Period end date cannot be earlier than start date.")
        return DomainResult.Success(Unit)
    }

    fun validateCommitment(
        commitmentNumber: String,
        committedAmount: BigDecimal,
        currency: String,
        costCategoryId: String,
        description: String,
        tenantId: String,
        projectId: String,
        createdBy: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (commitmentNumber.trim().length < 2) return DomainResult.Error(message = "Commitment number must be at least 2 characters.")
        if (costCategoryId.isBlank()) return DomainResult.Error(message = "Cost category ID is mandatory for cost commitments.")
        if (description.trim().length < 3) return DomainResult.Error(message = "Commitment description must be at least 3 characters.")
        if (createdBy.isBlank()) return DomainResult.Error(message = "Created by actor ID cannot be blank.")

        if (committedAmount <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Committed amount must be strictly greater than zero.")
        }

        val precRes = validatePrecision(committedAmount, "Committed amount")
        if (precRes is DomainResult.Error) return precRes

        val currRes = validateCurrency(currency)
        if (currRes is DomainResult.Error) return currRes

        return DomainResult.Success(Unit)
    }

    fun validateConsumption(
        commitment: BusinessCostCommitment,
        consumptionAmount: BigDecimal,
        currency: String,
        actorId: String
    ): DomainResult<Unit> {
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")
        if (!commitment.status.canBeConsumed) {
            return DomainResult.Error(message = "Cannot consume commitment '${commitment.commitmentNumber}' in status '${commitment.status}'. Commitment must be APPROVED or ACTIVE.")
        }
        if (consumptionAmount <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Consumption amount must be strictly greater than zero.")
        }

        val precRes = validatePrecision(consumptionAmount, "Consumption amount")
        if (precRes is DomainResult.Error) return precRes

        if (!commitment.currency.equals(currency, ignoreCase = true)) {
            return DomainResult.Error(message = "Consumption currency '$currency' does not match commitment currency '${commitment.currency}'.")
        }

        val scaledConsumption = consumptionAmount.setScale(4, RoundingMode.HALF_UP)
        if (scaledConsumption > commitment.remainingAmount) {
            return DomainResult.Error(message = "Consumption amount $scaledConsumption exceeds remaining commitment of ${commitment.remainingAmount}.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateAccrual(
        accrualNumber: String,
        accrualAmount: BigDecimal,
        currency: String,
        accountingPeriod: BusinessFinancialPeriod?,
        costCategoryId: String,
        description: String,
        tenantId: String,
        projectId: String,
        createdBy: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (accrualNumber.isBlank()) return DomainResult.Error(message = "Accrual number cannot be blank.")
        if (costCategoryId.isBlank()) return DomainResult.Error(message = "Cost category ID is mandatory for cost accruals.")
        if (description.trim().length < 3) return DomainResult.Error(message = "Accrual description must be at least 3 characters.")
        if (createdBy.isBlank()) return DomainResult.Error(message = "Created by actor ID cannot be blank.")

        if (accrualAmount <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Accrual amount must be strictly greater than zero.")
        }

        val precRes = validatePrecision(accrualAmount, "Accrual amount")
        if (precRes is DomainResult.Error) return precRes

        val currRes = validateCurrency(currency)
        if (currRes is DomainResult.Error) return currRes

        if (accountingPeriod != null && accountingPeriod.status.isClosed) {
            return DomainResult.Error(message = "Cannot create accrual in closed accounting period '${accountingPeriod.periodCode}'.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateReversal(
        accrual: BusinessCostAccrual,
        reversalAmount: BigDecimal,
        currency: String,
        reason: String,
        accountingPeriod: BusinessFinancialPeriod?,
        actorId: String
    ): DomainResult<Unit> {
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")
        if (reason.trim().length < 3) return DomainResult.Error(message = "A mandatory reversal reason (at least 3 characters) must be provided.")
        if (!accrual.status.canBeReversed) {
            return DomainResult.Error(message = "Only POSTED accruals can be reversed. Current status is '${accrual.status}'.")
        }
        if (reversalAmount <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Reversal amount must be strictly greater than zero.")
        }

        val precRes = validatePrecision(reversalAmount, "Reversal amount")
        if (precRes is DomainResult.Error) return precRes

        if (!accrual.currency.equals(currency, ignoreCase = true)) {
            return DomainResult.Error(message = "Reversal currency '$currency' does not match accrual currency '${accrual.currency}'.")
        }

        val scaledReversal = reversalAmount.setScale(4, RoundingMode.HALF_UP)
        val availableToReverse = accrual.netAccrualAmount
        if (scaledReversal > availableToReverse) {
            return DomainResult.Error(message = "Reversal amount $scaledReversal exceeds net posted accrual amount of $availableToReverse.")
        }

        if (accountingPeriod != null && accountingPeriod.status.isClosed) {
            return DomainResult.Error(message = "Cannot post reversal into closed accounting period '${accountingPeriod.periodCode}'.")
        }

        return DomainResult.Success(Unit)
    }
}
