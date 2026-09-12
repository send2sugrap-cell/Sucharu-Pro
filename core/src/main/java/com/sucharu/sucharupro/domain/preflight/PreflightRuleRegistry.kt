package com.sucharu.sucharupro.domain.preflight

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe Registry for Preflight Rules ensuring deterministic ordering and no duplicates.
 */
class PreflightRuleRegistry {

    private val rulesByCode = ConcurrentHashMap<String, PreflightRule>()

    fun registerRule(rule: PreflightRule) {
        val existing = rulesByCode.putIfAbsent(rule.ruleCode, rule)
        if (existing != null) {
            throw IllegalArgumentException("Duplicate preflight rule code '${rule.ruleCode}' is already registered.")
        }
    }

    fun getRuleByCode(ruleCode: String): PreflightRule? {
        return rulesByCode[ruleCode]
    }

    fun getApplicableRules(context: PreflightExecutionContext): List<PreflightRule> {
        return rulesByCode.values
            .filter { it.isApplicable(context) }
            .sortedBy { it.ruleCode }
    }

    fun getAllRules(): List<PreflightRule> {
        return rulesByCode.values.sortedBy { it.ruleCode }
    }

    fun clear() {
        rulesByCode.clear()
    }
}
