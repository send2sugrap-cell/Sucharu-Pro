package com.sucharu.sucharupro.domain.workflow.governance

import com.sucharu.sucharupro.domain.workflow.model.*

/**
 * Result of static and semantic validation on a workflow definition/version (INFRA-04 Step 06).
 */
data class WorkflowValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Production-grade static and semantic validator for Workflow Definitions and Versions.
 */
object WorkflowDefinitionValidator {

    fun validateVersion(version: WorkflowVersion): WorkflowValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val steps = version.steps
        if (steps.isEmpty()) {
            errors.add("Workflow version must contain at least one step.")
            return WorkflowValidationResult(isValid = false, errors = errors)
        }

        // 1. Duplicate Step ID Check
        val stepIds = mutableSetOf<String>()
        for (step in steps) {
            if (step.stepId.isBlank()) {
                errors.add("Step ID cannot be blank.")
            } else if (!stepIds.add(step.stepId)) {
                errors.add("Duplicate step ID '${step.stepId}' found.")
            }
        }

        val stepMap = steps.associateBy { it.stepId }

        // 2. Initial Step Validation
        val firstStep = steps.first()
        if (firstStep.stepType == WorkflowStepType.END) {
            errors.add("Initial step '${firstStep.stepId}' cannot be of type END.")
        }

        // 3. Terminal State & Reachability Analysis
        val hasEndStep = steps.any { it.stepType == WorkflowStepType.END || it.config["nextStepId"] == null }
        if (!hasEndStep) {
            errors.add("Workflow must have at least one terminal step (type END or nextStepId == null).")
        }

        // 4. Validate Step Definitions & Transitions
        for (step in steps) {
            val nextStepId = step.config["nextStepId"]
            // Next step reference validation
            if (nextStepId != null && !stepMap.containsKey(nextStepId)) {
                errors.add("Step '${step.stepId}' references non-existent nextStepId '$nextStepId'.")
            }

            // Self-referential loop validation
            if (nextStepId == step.stepId) {
                errors.add("Step '${step.stepId}' has an illegal direct cycle to itself.")
            }

            // Step Type specific validation
            when (step.stepType) {
                WorkflowStepType.ACTION -> {
                    val handler = step.config["handler"]
                    if (handler.isNullOrBlank()) {
                        errors.add("ACTION step '${step.stepId}' must declare a non-blank handler in config.")
                    }
                }
                WorkflowStepType.JOB -> {
                    val jobType = step.config["jobType"] ?: step.config["handler"]
                    if (jobType.isNullOrBlank()) {
                        errors.add("JOB step '${step.stepId}' must declare a background jobType in config.")
                    }
                }
                WorkflowStepType.EVENT_WAIT -> {
                    val eventType = step.config["eventType"]
                    if (eventType.isNullOrBlank()) {
                        errors.add("EVENT_WAIT step '${step.stepId}' must configure 'eventType' in config.")
                    }
                }
                WorkflowStepType.APPROVAL -> {
                    val policyId = step.config["policyId"]
                    if (policyId.isNullOrBlank()) {
                        errors.add("APPROVAL step '${step.stepId}' must specify 'policyId' in config.")
                    }
                }
                WorkflowStepType.CONDITION -> {
                    val conditionExpr = step.config["condition"]
                    val falseStepId = step.config["falseStepId"]
                    if (conditionExpr.isNullOrBlank()) {
                        errors.add("CONDITION step '${step.stepId}' must define a 'condition' expression.")
                    }
                    if (falseStepId != null && !stepMap.containsKey(falseStepId)) {
                        errors.add("CONDITION step '${step.stepId}' references non-existent falseStepId '$falseStepId'.")
                    }
                }
                WorkflowStepType.DELAY -> {
                    val delayMs = step.config["delayMs"]?.toLongOrNull()
                    if (delayMs == null || delayMs <= 0) {
                        errors.add("DELAY step '${step.stepId}' must declare a positive 'delayMs' parameter.")
                    }
                }
                WorkflowStepType.NOTIFICATION -> {
                    val template = step.config["template"]
                    if (template.isNullOrBlank()) {
                        errors.add("NOTIFICATION step '${step.stepId}' must declare a 'template' in config.")
                    }
                }
                WorkflowStepType.WEBHOOK -> {
                    val webhookUrl = step.config["url"]
                    if (webhookUrl.isNullOrBlank()) {
                        errors.add("WEBHOOK step '${step.stepId}' must specify target 'url' in config.")
                    }
                }
                WorkflowStepType.COMPENSATION -> {
                    val handler = step.config["handler"]
                    if (handler.isNullOrBlank()) {
                        errors.add("COMPENSATION step '${step.stepId}' must declare a compensation handler.")
                    }
                }
                WorkflowStepType.END -> {
                    if (nextStepId != null) {
                        warnings.add("END step '${step.stepId}' has a nextStepId which will be ignored.")
                    }
                }
            }

            // Retry policy validation
            step.retryPolicy?.let { retry ->
                if (retry.maxAttempts < 1) {
                    errors.add("Step '${step.stepId}' retry maxAttempts must be at least 1.")
                }
                if (retry.initialBackoffMs <= 0) {
                    errors.add("Step '${step.stepId}' retry initialBackoffMs must be positive.")
                }
                if (retry.multiplier < 1.0) {
                    errors.add("Step '${step.stepId}' retry multiplier must be >= 1.0.")
                }
            }

            // Compensation step reference validation
            if (step.compensationStepId != null) {
                val compStep = stepMap[step.compensationStepId]
                if (compStep == null && step.compensationStepId.isBlank()) {
                    errors.add("Step '${step.stepId}' references an empty compensationStepId.")
                }
            }
        }

        // 5. Reachability and Graph Cycle Detection
        val reachableStepIds = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(firstStep.stepId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            if (reachableStepIds.add(currentId)) {
                val currentStep = stepMap[currentId]
                currentStep?.config?.get("nextStepId")?.let { nextId ->
                    if (stepMap.containsKey(nextId)) queue.add(nextId)
                }
                currentStep?.config?.get("falseStepId")?.let { falseId ->
                    if (stepMap.containsKey(falseId)) queue.add(falseId)
                }
            }
        }

        for (step in steps) {
            if (!reachableStepIds.contains(step.stepId) && step.stepType != WorkflowStepType.COMPENSATION) {
                warnings.add("Step '${step.stepId}' is unreachable from the initial step '${firstStep.stepId}'.")
            }
        }

        return WorkflowValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
