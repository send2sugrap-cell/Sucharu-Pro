package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Minimum domain model representing a production operator/staff identity eligible for stage assignment.
 */
data class ProductionOperator(
    val operatorId: String,
    val operatorName: String,
    val role: UserRole = UserRole.STAFF,
    val phone: String? = null
) {
    init {
        require(operatorId.isNotBlank()) { "Operator ID cannot be blank." }
        require(operatorName.isNotBlank()) { "Operator Name cannot be blank." }
    }

    companion object {
        fun getSampleOperators(): List<ProductionOperator> = listOf(
            ProductionOperator("op-01", "রহিম আহমেদ (Rahim Ahmed)", UserRole.STAFF, "+8801711001122"),
            ProductionOperator("op-02", "করিম চৌধুরী (Karim Chowdhury)", UserRole.STAFF, "+8801711002233"),
            ProductionOperator("op-03", "তানভীর হাসান (Tanveer Hassan)", UserRole.DESIGNER, "+8801711003344"),
            ProductionOperator("op-04", "মাহমুদ আলম (Mahmud Alam)", UserRole.QC_INSPECTOR, "+8801711004455"),
            ProductionOperator("op-05", "রফিকুল ইসলাম (Rafiqul Islam)", UserRole.STAFF, "+8801711005566")
        )
    }
}
