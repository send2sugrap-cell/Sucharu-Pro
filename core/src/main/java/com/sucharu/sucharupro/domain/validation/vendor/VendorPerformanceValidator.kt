package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorComplianceRecord
import com.sucharu.sucharupro.domain.model.vendor.VendorComplianceRequirement
import com.sucharu.sucharupro.domain.model.vendor.VendorCorrectiveAction
import com.sucharu.sucharupro.domain.model.vendor.VendorEvaluation
import com.sucharu.sucharupro.domain.model.vendor.VendorPerformanceKpi
import com.sucharu.sucharupro.domain.model.vendor.VendorPerformanceMeasurement
import com.sucharu.sucharupro.domain.model.vendor.VendorPerformanceScorecard

object VendorPerformanceValidator {

    fun validateKpi(kpi: VendorPerformanceKpi): DomainResult<Unit> {
        if (kpi.kpiId.isBlank()) return DomainResult.Error(IllegalArgumentException("KPI ID cannot be blank"))
        if (kpi.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (kpi.tenantId.isBlank()) return DomainResult.Error(IllegalArgumentException("Tenant ID cannot be blank"))
        if (kpi.code.isBlank()) return DomainResult.Error(IllegalArgumentException("KPI code cannot be blank"))
        if (kpi.name.isBlank()) return DomainResult.Error(IllegalArgumentException("KPI name cannot be blank"))
        if (kpi.weight < 0.0) return DomainResult.Error(IllegalArgumentException("KPI weight cannot be negative"))
        if (kpi.targetValue < 0.0) return DomainResult.Error(IllegalArgumentException("Target value cannot be negative"))

        if (kpi.minimumAcceptableValue != null && kpi.minimumAcceptableValue < 0.0) {
            return DomainResult.Error(IllegalArgumentException("Minimum acceptable value cannot be negative"))
        }
        if (kpi.maximumAcceptableValue != null && kpi.maximumAcceptableValue < 0.0) {
            return DomainResult.Error(IllegalArgumentException("Maximum acceptable value cannot be negative"))
        }
        if (kpi.minimumAcceptableValue != null && kpi.maximumAcceptableValue != null && kpi.minimumAcceptableValue > kpi.maximumAcceptableValue) {
            return DomainResult.Error(IllegalArgumentException("Minimum acceptable value cannot exceed maximum acceptable value"))
        }
        if (kpi.effectiveTo != null && kpi.effectiveTo.isBefore(kpi.effectiveFrom)) {
            return DomainResult.Error(IllegalArgumentException("Effective to date cannot be before effective from date"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateMeasurement(measurement: VendorPerformanceMeasurement): DomainResult<Unit> {
        if (measurement.measurementId.isBlank()) return DomainResult.Error(IllegalArgumentException("Measurement ID cannot be blank"))
        if (measurement.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (measurement.vendorId.isBlank()) return DomainResult.Error(IllegalArgumentException("Vendor ID cannot be blank"))
        if (measurement.kpiId.isBlank()) return DomainResult.Error(IllegalArgumentException("KPI ID cannot be blank"))
        if (measurement.periodEnd.isBefore(measurement.periodStart)) {
            return DomainResult.Error(IllegalArgumentException("Period end date cannot be before period start date"))
        }
        if (measurement.actualValue < 0.0) {
            return DomainResult.Error(IllegalArgumentException("Actual value cannot be negative"))
        }
        if (measurement.sampleSize < 0) {
            return DomainResult.Error(IllegalArgumentException("Sample size cannot be negative"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateScorecard(scorecard: VendorPerformanceScorecard): DomainResult<Unit> {
        if (scorecard.scorecardId.isBlank()) return DomainResult.Error(IllegalArgumentException("Scorecard ID cannot be blank"))
        if (scorecard.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (scorecard.vendorId.isBlank()) return DomainResult.Error(IllegalArgumentException("Vendor ID cannot be blank"))
        if (scorecard.periodEnd.isBefore(scorecard.periodStart)) {
            return DomainResult.Error(IllegalArgumentException("Period end date cannot be before period start date"))
        }
        if (scorecard.overallScore < 0.0 || scorecard.overallScore > 100.0) {
            return DomainResult.Error(IllegalArgumentException("Overall score must be between 0.0 and 100.0"))
        }
        if (scorecard.dataCompleteness < 0.0 || scorecard.dataCompleteness > 100.0) {
            return DomainResult.Error(IllegalArgumentException("Data completeness must be between 0.0 and 100.0"))
        }
        for (item in scorecard.items) {
            if (item.normalizedScore < 0.0 || item.normalizedScore > 100.0) {
                return DomainResult.Error(IllegalArgumentException("Item normalized score must be between 0.0 and 100.0"))
            }
            if (item.weight < 0.0) {
                return DomainResult.Error(IllegalArgumentException("Item weight cannot be negative"))
            }
        }
        return DomainResult.Success(Unit)
    }

    fun validateEvaluation(evaluation: VendorEvaluation): DomainResult<Unit> {
        if (evaluation.evaluationId.isBlank()) return DomainResult.Error(IllegalArgumentException("Evaluation ID cannot be blank"))
        if (evaluation.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (evaluation.vendorId.isBlank()) return DomainResult.Error(IllegalArgumentException("Vendor ID cannot be blank"))
        if (evaluation.evaluatorId.isBlank()) return DomainResult.Error(IllegalArgumentException("Evaluator ID cannot be blank"))
        if (evaluation.periodEnd.isBefore(evaluation.periodStart)) {
            return DomainResult.Error(IllegalArgumentException("Period end date cannot be before period start date"))
        }
        if (evaluation.evaluationScore < 0.0 || evaluation.evaluationScore > 100.0) {
            return DomainResult.Error(IllegalArgumentException("Evaluation score must be between 0.0 and 100.0"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateEvaluationSeparationOfDuties(evaluation: VendorEvaluation, approverId: String): DomainResult<Unit> {
        if (evaluation.evaluatorId == approverId || evaluation.submittedBy == approverId) {
            return DomainResult.Error(IllegalStateException("Separation of duties violation: evaluator cannot approve their own evaluation"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateComplianceRequirement(requirement: VendorComplianceRequirement): DomainResult<Unit> {
        if (requirement.requirementId.isBlank()) return DomainResult.Error(IllegalArgumentException("Requirement ID cannot be blank"))
        if (requirement.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (requirement.code.isBlank()) return DomainResult.Error(IllegalArgumentException("Requirement code cannot be blank"))
        if (requirement.name.isBlank()) return DomainResult.Error(IllegalArgumentException("Requirement name cannot be blank"))
        if (requirement.validityDays != null && requirement.validityDays <= 0) {
            return DomainResult.Error(IllegalArgumentException("Validity days must be positive"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateComplianceRecord(record: VendorComplianceRecord): DomainResult<Unit> {
        if (record.recordId.isBlank()) return DomainResult.Error(IllegalArgumentException("Record ID cannot be blank"))
        if (record.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (record.vendorId.isBlank()) return DomainResult.Error(IllegalArgumentException("Vendor ID cannot be blank"))
        if (record.requirementId.isBlank()) return DomainResult.Error(IllegalArgumentException("Requirement ID cannot be blank"))
        if (record.expiryDate != null && record.expiryDate.isBefore(record.effectiveDate)) {
            return DomainResult.Error(IllegalArgumentException("Expiry date cannot be before effective date"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateCorrectiveAction(action: VendorCorrectiveAction): DomainResult<Unit> {
        if (action.actionId.isBlank()) return DomainResult.Error(IllegalArgumentException("Action ID cannot be blank"))
        if (action.projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (action.vendorId.isBlank()) return DomainResult.Error(IllegalArgumentException("Vendor ID cannot be blank"))
        if (action.issueDescription.isBlank()) return DomainResult.Error(IllegalArgumentException("Issue description cannot be blank"))
        if (action.actionPlan.isBlank()) return DomainResult.Error(IllegalArgumentException("Action plan cannot be blank"))
        if (action.assignedTo.isBlank()) return DomainResult.Error(IllegalArgumentException("Assigned to cannot be blank"))
        return DomainResult.Success(Unit)
    }
}
