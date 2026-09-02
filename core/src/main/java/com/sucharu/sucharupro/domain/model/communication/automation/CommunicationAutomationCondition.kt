package com.sucharu.sucharupro.domain.model.communication.automation

/**
 * Safe, typed condition specification for Automation Rules (Module 10 Step 08).
 *
 * Security: Prevents raw script or SQL injection by using explicit comparison operators.
 */
data class CommunicationAutomationCondition(
    val field: String,
    val operator: ConditionOperator = ConditionOperator.EQUALS,
    val expectedValue: String = ""
) {
    init {
        require(field.isNotBlank()) { "Condition field cannot be blank." }
    }

    fun evaluate(actualValue: String?): Boolean {
        if (actualValue == null) {
            return operator == ConditionOperator.IS_NULL
        }
        if (operator == ConditionOperator.IS_NOT_NULL) {
            return true
        }

        return when (operator) {
            ConditionOperator.EQUALS -> actualValue.equals(expectedValue, ignoreCase = true)
            ConditionOperator.NOT_EQUALS -> !actualValue.equals(expectedValue, ignoreCase = true)
            ConditionOperator.GREATER_THAN -> {
                val act = actualValue.toDoubleOrNull()
                val exp = expectedValue.toDoubleOrNull()
                if (act != null && exp != null) act > exp else actualValue > expectedValue
            }
            ConditionOperator.GREATER_OR_EQUAL -> {
                val act = actualValue.toDoubleOrNull()
                val exp = expectedValue.toDoubleOrNull()
                if (act != null && exp != null) act >= exp else actualValue >= expectedValue
            }
            ConditionOperator.LESS_THAN -> {
                val act = actualValue.toDoubleOrNull()
                val exp = expectedValue.toDoubleOrNull()
                if (act != null && exp != null) act < exp else actualValue < expectedValue
            }
            ConditionOperator.LESS_OR_EQUAL -> {
                val act = actualValue.toDoubleOrNull()
                val exp = expectedValue.toDoubleOrNull()
                if (act != null && exp != null) act <= exp else actualValue <= expectedValue
            }
            ConditionOperator.IN -> {
                val set = expectedValue.split(",").map { it.trim().lowercase() }
                actualValue.lowercase() in set
            }
            ConditionOperator.NOT_IN -> {
                val set = expectedValue.split(",").map { it.trim().lowercase() }
                actualValue.lowercase() !in set
            }
            ConditionOperator.CONTAINS -> actualValue.contains(expectedValue, ignoreCase = true)
            ConditionOperator.NOT_CONTAINS -> !actualValue.contains(expectedValue, ignoreCase = true)
            ConditionOperator.IS_NULL -> false
            ConditionOperator.IS_NOT_NULL -> true
        }
    }
}

enum class ConditionOperator(val defaultLabel: String) {
    EQUALS("Equals (=)"),
    NOT_EQUALS("Not Equals (!=)"),
    GREATER_THAN("Greater Than (>)"),
    GREATER_OR_EQUAL("Greater Or Equal (>=)"),
    LESS_THAN("Less Than (<)"),
    LESS_OR_EQUAL("Less Or Equal (<=)"),
    IN("In Set"),
    NOT_IN("Not In Set"),
    CONTAINS("Contains Substring"),
    NOT_CONTAINS("Does Not Contain"),
    IS_NULL("Is Null / Empty"),
    IS_NOT_NULL("Is Not Null")
}
