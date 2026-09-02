package com.sucharu.sucharupro.domain.model.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Value class representing monetary values for Sucharu Pro.
 * Eliminates floating-point precision issues in financial calculations (paper cost, printing rates, invoices).
 */
@JvmInline
value class Money(val amount: BigDecimal) : Comparable<Money> {

    constructor(valueDouble: Double) : this(BigDecimal.valueOf(valueDouble).setScale(2, RoundingMode.HALF_UP))
    constructor(valueLong: Long) : this(BigDecimal.valueOf(valueLong).setScale(2, RoundingMode.HALF_UP))
    constructor(valueInt: Int) : this(BigDecimal.valueOf(valueInt.toLong()).setScale(2, RoundingMode.HALF_UP))
    constructor(valueString: String) : this(BigDecimal(valueString).setScale(2, RoundingMode.HALF_UP))

    operator fun plus(other: Money): Money = Money(amount.add(other.amount).setScale(2, RoundingMode.HALF_UP))
    operator fun minus(other: Money): Money = Money(amount.subtract(other.amount).setScale(2, RoundingMode.HALF_UP))
    operator fun times(multiplier: Int): Money = Money(amount.multiply(BigDecimal.valueOf(multiplier.toLong())).setScale(2, RoundingMode.HALF_UP))
    operator fun times(multiplier: Long): Money = Money(amount.multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP))
    operator fun times(multiplier: BigDecimal): Money = Money(amount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP))
    operator fun times(multiplier: Double): Money = Money(amount.multiply(BigDecimal.valueOf(multiplier)).setScale(2, RoundingMode.HALF_UP))
    
    operator fun div(divisor: Int): Money = Money(amount.divide(BigDecimal.valueOf(divisor.toLong()), 2, RoundingMode.HALF_UP))
    operator fun div(divisor: BigDecimal): Money = Money(amount.divide(divisor, 2, RoundingMode.HALF_UP))

    override fun compareTo(other: Money): Int = amount.compareTo(other.amount)

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0
    fun isPositive(): Boolean = amount > BigDecimal.ZERO
    fun isNegative(): Boolean = amount < BigDecimal.ZERO
    fun abs(): Money = Money(amount.abs().setScale(2, RoundingMode.HALF_UP))

    /**
     * Formats the amount with currency symbol and locale-aware number formatting.
     * Example: "৳ 45,000" or "৳ 1,250.50"
     */
    fun formatted(currencySymbol: String = "৳", showDecimalsIfZero: Boolean = false): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            if (showDecimalsIfZero || amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            } else {
                minimumFractionDigits = 0
                maximumFractionDigits = 0
            }
        }
        return "$currencySymbol ${numberFormat.format(amount)}"
    }

    companion object {
        val ZERO = Money(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
    }
}

// Extension helpers
fun Double.toMoney(): Money = Money(this)
fun Long.toMoney(): Money = Money(this)
fun Int.toMoney(): Money = Money(this)
fun String.toMoney(): Money = Money(this)
