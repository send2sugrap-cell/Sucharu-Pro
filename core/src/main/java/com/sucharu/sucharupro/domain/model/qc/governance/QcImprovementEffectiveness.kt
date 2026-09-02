package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Deterministic effectiveness outcome evaluation for completed quality improvement actions.
 */
enum class QcImprovementEffectiveness(
    val defaultLabel: String,
    val score: Int
) {
    NOT_EVALUATED("Not Evaluated", 0),
    INEFFECTIVE("Ineffective (No observed improvement)", 1),
    PARTIALLY_EFFECTIVE("Partially Effective", 2),
    EFFECTIVE("Effective (Observed target improvement)", 3),
    HIGHLY_EFFECTIVE("Highly Effective (Exceeded target improvement)", 4);

    companion object {
        fun fromString(value: String?): QcImprovementEffectiveness? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
