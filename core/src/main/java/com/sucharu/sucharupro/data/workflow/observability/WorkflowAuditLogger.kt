package com.sucharu.sucharupro.data.workflow.observability

import com.sucharu.sucharupro.domain.workflow.model.WorkflowTransition

/**
 * Structured workflow audit logger with automatic credential scrubbing (INFRA-04 Step 05).
 */
class WorkflowAuditLogger {

    private val sensitiveKeys = setOf(
        "password", "token", "accesstoken", "refreshtoken",
        "secret", "signingsecret", "authorization", "apikey"
    )

    fun logTransition(transition: WorkflowTransition) {
        val sanitizedMetadata = scrubMap(transition.metadata)
        println(
            "[WORKFLOW-AUDIT] tenant=${transition.projectId} wf=${transition.workflowId} exec=${transition.executionId} " +
                    "from=${transition.fromStatus} to=${transition.toStatus} trigger=${transition.triggerType} " +
                    "actor=${transition.actorId}(${transition.actorType}) meta=$sanitizedMetadata"
        )
    }

    fun logApproval(approvalId: String, projectId: String, actorId: String, action: String, notes: String?) {
        val sanitizedNotes = if (notes != null) scrubString(notes) else null
        println(
            "[APPROVAL-AUDIT] tenant=$projectId approval=$approvalId actor=$actorId action=$action notes=$sanitizedNotes"
        )
    }

    fun logWorkflowStarted(workflowId: String, projectId: String, actorId: String, operation: String) {
        println(
            "[WORKFLOW-AUDIT] tenant=$projectId wf=$workflowId actor=$actorId operation=$operation"
        )
    }

    fun logWorkflowOperation(operation: String, projectId: String, targetId: String, actorId: String, details: String?) {
        val sanitized = if (details != null) scrubString(details) else null
        println(
            "[WORKFLOW-AUDIT] tenant=$projectId target=$targetId actor=$actorId op=$operation details=$sanitized"
        )
    }

    private fun scrubMap(map: Map<String, String>): Map<String, String> {
        return map.mapValues { (k, v) ->
            if (sensitiveKeys.contains(k.lowercase())) "******" else v
        }
    }

    private fun scrubString(content: String): String {
        var result = content
        for (key in sensitiveKeys) {
            val regex = Regex("(?i)($key\\s*[:=]\\s*)([^,\\s\\}]+)")
            result = result.replace(regex, "$1******")
        }
        return result
    }
}
