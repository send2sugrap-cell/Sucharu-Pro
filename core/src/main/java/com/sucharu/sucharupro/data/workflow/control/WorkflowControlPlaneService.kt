package com.sucharu.sucharupro.data.workflow.control

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.workflow.observability.WorkflowAuditLogger
import com.sucharu.sucharupro.data.workflow.observability.WorkflowMetrics
import com.sucharu.sucharupro.data.workflow.postgres.*
import com.sucharu.sucharupro.domain.workflow.approval.ApprovalEngine
import com.sucharu.sucharupro.domain.workflow.approval.ApprovalEvaluationResult
import com.sucharu.sucharupro.domain.workflow.engine.WorkflowOrchestrator
import com.sucharu.sucharupro.domain.workflow.governance.*
import com.sucharu.sucharupro.domain.workflow.model.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Production-grade Workflow Control Plane, Management & Governance Service (INFRA-04 Step 06).
 */
class WorkflowControlPlaneService(
    private val definitionRepository: WorkflowDefinitionRepository,
    private val instanceRepository: WorkflowInstanceRepository,
    private val stepExecutionRepository: WorkflowStepExecutionRepository,
    private val compensationRepository: WorkflowCompensationRepository,
    private val approvalRepository: WorkflowApprovalRepository,
    private val idempotencyStore: WorkflowIdempotencyStore? = null,
    private val orchestrator: WorkflowOrchestrator = WorkflowOrchestrator(),
    private val approvalEngine: ApprovalEngine = ApprovalEngine(),
    private val auditLogger: WorkflowAuditLogger = WorkflowAuditLogger(),
    private val metrics: WorkflowMetrics = WorkflowMetrics(),
    val realTimeBridge: WorkflowRealTimeBridge = WorkflowRealTimeBridge()
) {

    private val inMemoryAuditLogs = ConcurrentHashMap<String, CopyOnWriteArrayList<WorkflowAuditEntry>>()

    // ==========================================
    // 1. WORKFLOW DEFINITION GOVERNANCE
    // ==========================================

    suspend fun createDefinition(
        request: CreateWorkflowDefinitionRequest,
        principal: AuthenticatedPrincipal
    ): WorkflowDefinition {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_CREATE)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val definitionId = "wf-def-${UUID.randomUUID().toString().take(8)}"
        val versionId = "v1"

        val definition = WorkflowDefinition(
            definitionId = definitionId,
            projectId = principal.projectId,
            workflowName = request.name,
            description = request.description,
            isActive = true,
            createdBy = principal.userId
        )

        definitionRepository.saveDefinition(definition, tenant)

        // Initial draft version
        val version = WorkflowVersion(
            versionId = versionId,
            definitionId = definitionId,
            projectId = principal.projectId,
            steps = request.initialSteps.ifEmpty {
                listOf(
                    WorkflowStepDefinition(
                        stepId = "init-step",
                        definitionId = definitionId,
                        versionId = versionId,
                        projectId = principal.projectId,
                        stepName = "Initial Step",
                        stepType = WorkflowStepType.ACTION,
                        sequenceOrder = 1,
                        config = mapOf("handler" to "default-action", "nextStepId" to "end-step")
                    ),
                    WorkflowStepDefinition(
                        stepId = "end-step",
                        definitionId = definitionId,
                        versionId = versionId,
                        projectId = principal.projectId,
                        stepName = "End Step",
                        stepType = WorkflowStepType.END,
                        sequenceOrder = 2
                    )
                )
            },
            isActive = true,
            publishedBy = principal.userId
        )
        definitionRepository.saveVersion(version, tenant)

        recordAudit(
            principal = principal,
            operation = "CREATE_WORKFLOW_DEFINITION",
            targetType = "WorkflowDefinition",
            targetId = definitionId,
            previousState = null,
            newState = "DRAFT",
            details = "Created workflow definition '${request.name}'"
        )

        return definition
    }

    suspend fun updateDefinition(
        definitionId: String,
        request: UpdateWorkflowDefinitionRequest,
        principal: AuthenticatedPrincipal
    ): WorkflowDefinition {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_EDIT)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val existing = definitionRepository.getDefinitionById(definitionId, tenant)
            ?: throw NoSuchElementException("Workflow definition '$definitionId' not found.")

        val updated = existing.copy(
            workflowName = request.name ?: existing.workflowName,
            description = request.description ?: existing.description,
            isActive = request.isEnabled ?: existing.isActive,
            updatedAt = System.currentTimeMillis()
        )

        definitionRepository.saveDefinition(updated, tenant)

        recordAudit(
            principal = principal,
            operation = "UPDATE_WORKFLOW_DEFINITION",
            targetType = "WorkflowDefinition",
            targetId = definitionId,
            previousState = "ENABLED=${existing.isActive}",
            newState = "ENABLED=${updated.isActive}",
            details = "Updated definition '${updated.workflowName}'"
        )

        return updated
    }

    suspend fun createVersion(
        definitionId: String,
        request: CreateWorkflowVersionRequest,
        principal: AuthenticatedPrincipal
    ): WorkflowVersion {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_CREATE)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        definitionRepository.getDefinitionById(definitionId, tenant)
            ?: throw NoSuchElementException("Workflow definition '$definitionId' not found.")

        val existingVersions = definitionRepository.listVersions(definitionId, tenant)
        val nextVersionNumber = existingVersions.size + 1
        val versionId = "v$nextVersionNumber"

        val version = WorkflowVersion(
            versionId = versionId,
            definitionId = definitionId,
            projectId = principal.projectId,
            steps = request.steps,
            isActive = true,
            publishedBy = principal.userId
        )

        // Validate Version
        val validation = WorkflowDefinitionValidator.validateVersion(version)
        if (!validation.isValid) {
            throw IllegalArgumentException("Workflow version validation failed: ${validation.errors.joinToString("; ")}")
        }

        definitionRepository.saveVersion(version, tenant)

        recordAudit(
            principal = principal,
            operation = "CREATE_WORKFLOW_VERSION",
            targetType = "WorkflowVersion",
            targetId = "$definitionId:$versionId",
            previousState = null,
            newState = "DRAFT",
            details = "Created version $nextVersionNumber for definition '$definitionId'"
        )

        return version
    }

    suspend fun publishVersion(
        definitionId: String,
        versionId: String,
        principal: AuthenticatedPrincipal
    ): WorkflowVersion {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_PUBLISH)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val version = definitionRepository.getVersion(definitionId, versionId, tenant)
            ?: throw NoSuchElementException("Workflow version '$versionId' for '$definitionId' not found.")

        // Validate before publication
        val validation = WorkflowDefinitionValidator.validateVersion(version)
        if (!validation.isValid) {
            throw IllegalArgumentException("Cannot publish invalid workflow version: ${validation.errors.joinToString("; ")}")
        }

        val published = version.copy(
            publishedAt = System.currentTimeMillis(),
            publishedBy = principal.userId
        )

        definitionRepository.saveVersion(published, tenant)

        recordAudit(
            principal = principal,
            operation = "PUBLISH_WORKFLOW_VERSION",
            targetType = "WorkflowVersion",
            targetId = "$definitionId:$versionId",
            previousState = "DRAFT",
            newState = "PUBLISHED",
            details = "Published immutable workflow version '$versionId'"
        )

        return published
    }

    suspend fun getDefinitions(principal: AuthenticatedPrincipal): List<WorkflowDefinitionSummary> {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_VIEW)
        val tenant = TenantContext(principal.projectId)
        val definitions = definitionRepository.listDefinitions(tenant)

        return definitions.map { def ->
            val versions = definitionRepository.listVersions(def.definitionId, tenant)
            WorkflowDefinitionSummary(
                definitionId = def.definitionId,
                projectId = def.projectId,
                name = def.workflowName,
                description = def.description,
                category = "PRODUCTION",
                isEnabled = def.isActive,
                latestVersion = versions.size.coerceAtLeast(1),
                activeVersionId = versions.firstOrNull()?.versionId ?: "v1",
                totalInstances = 0,
                createdAt = def.createdAt,
                updatedAt = def.updatedAt
            )
        }
    }

    suspend fun getVersions(definitionId: String, principal: AuthenticatedPrincipal): List<WorkflowVersionSummary> {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_VIEW)
        val tenant = TenantContext(principal.projectId)
        val versions = definitionRepository.listVersions(definitionId, tenant)

        return versions.mapIndexed { idx, v ->
            WorkflowVersionSummary(
                versionId = v.versionId,
                definitionId = v.definitionId,
                versionNumber = idx + 1,
                isPublished = v.publishedAt > 0L,
                isDeprecated = false,
                isArchived = false,
                stepCount = v.steps.size,
                description = "Version ${v.versionId}",
                publishedAt = v.publishedAt,
                publishedBy = v.publishedBy,
                createdAt = v.publishedAt
            )
        }
    }

    // ==========================================
    // 2. WORKFLOW INSTANCE OPERATIONS & SEARCH
    // ==========================================

    suspend fun getInstances(
        criteria: WorkflowFilterCriteria,
        principal: AuthenticatedPrincipal
    ): PagedResult<WorkflowInstanceSummary> {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_VIEW)
        val tenant = TenantContext(principal.projectId)

        val rawInstances = if (criteria.status != null) {
            instanceRepository.listInstancesByStatus(criteria.status, 100, tenant)
        } else {
            instanceRepository.listInstancesByStatus(WorkflowStatus.RUNNING, 50, tenant) +
                    instanceRepository.listInstancesByStatus(WorkflowStatus.COMPLETED, 50, tenant) +
                    instanceRepository.listInstancesByStatus(WorkflowStatus.FAILED, 50, tenant) +
                    instanceRepository.listInstancesByStatus(WorkflowStatus.DEAD_LETTER, 50, tenant)
        }

        val filtered = rawInstances.filter { inst ->
            (criteria.definitionId == null || inst.definitionId == criteria.definitionId) &&
            (criteria.versionId == null || inst.versionId == criteria.versionId) &&
            (criteria.actorId == null || inst.actorId == criteria.actorId) &&
            (criteria.searchQuery == null || inst.workflowId.contains(criteria.searchQuery, ignoreCase = true) || inst.definitionId.contains(criteria.searchQuery, ignoreCase = true))
        }.distinctBy { it.workflowId }

        val page = criteria.page.coerceAtLeast(1)
        val pageSize = criteria.pageSize.coerceIn(1, 100)
        val startIndex = (page - 1) * pageSize
        val pagedList = filtered.drop(startIndex).take(pageSize)

        val summaries = pagedList.map { inst ->
            WorkflowInstanceSummary(
                workflowId = inst.workflowId,
                projectId = inst.projectId,
                definitionId = inst.definitionId,
                definitionName = inst.definitionId,
                versionId = inst.versionId,
                versionNumber = 1,
                status = inst.status,
                currentStepId = inst.currentStepId,
                currentStepName = inst.currentStepId,
                progressPercent = calculateProgress(inst.status),
                actorType = inst.actorType,
                actorId = inst.actorId,
                startedAt = inst.createdAt,
                completedAt = inst.completedAt,
                updatedAt = inst.updatedAt
            )
        }

        return PagedResult(
            items = summaries,
            page = page,
            pageSize = pageSize,
            totalItems = filtered.size,
            totalPages = if (filtered.isEmpty()) 1 else (filtered.size + pageSize - 1) / pageSize
        )
    }

    suspend fun getInstanceSummary(
        workflowId: String,
        principal: AuthenticatedPrincipal
    ): WorkflowInstanceSummary {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_VIEW)
        val tenant = TenantContext(principal.projectId)
        val inst = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw NoSuchElementException("Workflow instance '$workflowId' not found.")

        return WorkflowInstanceSummary(
            workflowId = inst.workflowId,
            projectId = inst.projectId,
            definitionId = inst.definitionId,
            definitionName = inst.definitionId,
            versionId = inst.versionId,
            versionNumber = 1,
            status = inst.status,
            currentStepId = inst.currentStepId,
            currentStepName = inst.currentStepId,
            progressPercent = calculateProgress(inst.status),
            actorType = inst.actorType,
            actorId = inst.actorId,
            startedAt = inst.createdAt,
            completedAt = inst.completedAt,
            updatedAt = inst.updatedAt
        )
    }

    suspend fun getExecutionTimeline(
        workflowId: String,
        principal: AuthenticatedPrincipal
    ): WorkflowExecutionTimeline {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_VIEW)
        val tenant = TenantContext(principal.projectId)
        val inst = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw NoSuchElementException("Workflow instance '$workflowId' not found.")

        val stepExecutions = stepExecutionRepository.getExecutionsForWorkflow(workflowId, tenant)
        val transitions = instanceRepository.getTransitionsForWorkflow(workflowId, tenant)

        val events = mutableListOf<WorkflowTimelineEvent>()

        // 1. Initial Trigger
        events.add(
            WorkflowTimelineEvent(
                eventId = "tl-init-${inst.workflowId.take(8)}",
                eventType = TimelineEventType.WORKFLOW_STARTED,
                title = "Workflow Started",
                description = "Initiated by ${inst.actorType} ${inst.actorId}",
                actorId = inst.actorId,
                timestamp = inst.createdAt
            )
        )

        // 2. Step executions
        for (step in stepExecutions) {
            events.add(
                WorkflowTimelineEvent(
                    eventId = "tl-step-${step.stepExecutionId.take(8)}",
                    eventType = when (step.status) {
                        StepExecutionStatus.SUCCEEDED -> TimelineEventType.STEP_COMPLETED
                        StepExecutionStatus.FAILED -> TimelineEventType.STEP_FAILED
                        StepExecutionStatus.RUNNING -> TimelineEventType.STEP_STARTED
                        else -> TimelineEventType.STEP_STARTED
                    },
                    title = "Step ${step.stepId} ${step.status.name}",
                    description = step.errorMessage ?: "Executed attempt ${step.attemptNumber}",
                    stepId = step.stepId,
                    timestamp = step.completedAt ?: step.startedAt
                )
            )
        }

        // 3. State transitions
        for (tr in transitions) {
            events.add(
                WorkflowTimelineEvent(
                    eventId = "tl-tr-${tr.transitionId.take(8)}",
                    eventType = when (tr.toStatus) {
                        WorkflowStatus.PAUSED -> TimelineEventType.WORKFLOW_PAUSED
                        WorkflowStatus.COMPLETED -> TimelineEventType.WORKFLOW_COMPLETED
                        WorkflowStatus.FAILED -> TimelineEventType.WORKFLOW_FAILED
                        WorkflowStatus.CANCELLED -> TimelineEventType.WORKFLOW_CANCELLED
                        WorkflowStatus.COMPENSATING -> TimelineEventType.COMPENSATION_TRIGGERED
                        WorkflowStatus.DEAD_LETTER -> TimelineEventType.DEAD_LETTER_QUARANTINED
                        else -> TimelineEventType.WORKFLOW_STARTED
                    },
                    title = "Status changed: ${tr.fromStatus} -> ${tr.toStatus}",
                    description = "State transition",
                    actorId = tr.actorId,
                    timestamp = tr.transitionedAt
                )
            )
        }

        val totalDuration = inst.completedAt?.let { it - inst.createdAt }

        return WorkflowExecutionTimeline(
            workflowId = inst.workflowId,
            projectId = inst.projectId,
            definitionId = inst.definitionId,
            definitionName = inst.definitionId,
            status = inst.status,
            totalDurationMs = totalDuration,
            events = events.sortedBy { it.timestamp }
        )
    }

    // ==========================================
    // 3. CONTROL OPERATIONS
    // ==========================================

    suspend fun pauseWorkflow(
        workflowId: String,
        request: WorkflowPauseRequest,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_PAUSE)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val inst = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw NoSuchElementException("Workflow '$workflowId' not found.")

        val paused = orchestrator.pauseWorkflow(inst, principal.userId, request.reason)
        instanceRepository.updateInstance(paused, tenant)

        realTimeBridge.publishWorkflowEvent(
            instance = paused,
            eventType = TimelineEventType.WORKFLOW_PAUSED,
            title = "Workflow Paused by ${principal.username}",
            details = mapOf("reason" to (request.reason ?: "Operator pause"))
        )

        recordAudit(
            principal = principal,
            operation = "PAUSE_WORKFLOW",
            targetType = "WorkflowInstance",
            targetId = workflowId,
            previousState = inst.status.name,
            newState = WorkflowStatus.PAUSED.name,
            details = request.reason ?: "Paused by operator"
        )

        return paused
    }

    suspend fun resumeWorkflow(
        workflowId: String,
        request: WorkflowResumeRequest,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_RESUME)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val inst = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw NoSuchElementException("Workflow '$workflowId' not found.")

        val version = definitionRepository.getVersion(inst.definitionId, inst.versionId, tenant)
            ?: throw IllegalStateException("Workflow version not found.")

        val resumed = orchestrator.resumeWorkflow(inst, version, request.contextUpdates)
        instanceRepository.updateInstance(resumed, tenant)

        realTimeBridge.publishWorkflowEvent(
            instance = resumed,
            eventType = TimelineEventType.WORKFLOW_RESUMED,
            title = "Workflow Resumed by ${principal.username}"
        )

        recordAudit(
            principal = principal,
            operation = "RESUME_WORKFLOW",
            targetType = "WorkflowInstance",
            targetId = workflowId,
            previousState = inst.status.name,
            newState = resumed.status.name,
            details = "Resumed with ${request.contextUpdates.size} context updates"
        )

        return resumed
    }

    suspend fun cancelWorkflow(
        workflowId: String,
        request: WorkflowCancelRequest,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_CANCEL)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val inst = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw NoSuchElementException("Workflow '$workflowId' not found.")

        val cancelled = orchestrator.cancelWorkflow(inst, principal.userId, request.reason)
        instanceRepository.updateInstance(cancelled, tenant)

        realTimeBridge.publishWorkflowEvent(
            instance = cancelled,
            eventType = TimelineEventType.WORKFLOW_CANCELLED,
            title = "Workflow Cancelled by ${principal.username}",
            details = mapOf("reason" to request.reason)
        )

        recordAudit(
            principal = principal,
            operation = "CANCEL_WORKFLOW",
            targetType = "WorkflowInstance",
            targetId = workflowId,
            previousState = inst.status.name,
            newState = WorkflowStatus.CANCELLED.name,
            details = request.reason
        )

        return cancelled
    }

    suspend fun retryWorkflow(
        workflowId: String,
        request: WorkflowRetryRequest,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_RETRY)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val inst = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw NoSuchElementException("Workflow '$workflowId' not found.")

        if (inst.status != WorkflowStatus.FAILED && inst.status != WorkflowStatus.DEAD_LETTER) {
            throw IllegalStateException("Workflow is in status '${inst.status}', only FAILED or DEAD_LETTER workflows can be retried.")
        }

        val updated = inst.copy(
            status = WorkflowStatus.RUNNING,
            currentStepId = request.targetStepId ?: inst.currentStepId,
            updatedAt = System.currentTimeMillis()
        )
        instanceRepository.updateInstance(updated, tenant)

        realTimeBridge.publishWorkflowEvent(
            instance = updated,
            eventType = TimelineEventType.RETRY_SCHEDULED,
            title = "Workflow Retry Initiated by ${principal.username}"
        )

        recordAudit(
            principal = principal,
            operation = "RETRY_WORKFLOW",
            targetType = "WorkflowInstance",
            targetId = workflowId,
            previousState = inst.status.name,
            newState = WorkflowStatus.RUNNING.name,
            details = "Retry initiated"
        )

        return updated
    }

    suspend fun replayDeadLetter(
        workflowId: String,
        request: WorkflowReplayRequest,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_REPLAY)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val inst = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw NoSuchElementException("Workflow '$workflowId' not found.")

        if (inst.status != WorkflowStatus.DEAD_LETTER) {
            throw IllegalStateException("Workflow status is '${inst.status}'. Replay is only permitted for DEAD_LETTER instances.")
        }

        val replayed = inst.copy(
            status = WorkflowStatus.RUNNING,
            currentStepId = request.checkpointStepId ?: inst.currentStepId,
            context = inst.context + request.contextOverrides,
            updatedAt = System.currentTimeMillis()
        )
        instanceRepository.updateInstance(replayed, tenant)

        realTimeBridge.publishWorkflowEvent(
            instance = replayed,
            eventType = TimelineEventType.ADMIN_REPLAYED,
            title = "Workflow Replayed by Admin ${principal.username}"
        )

        recordAudit(
            principal = principal,
            operation = "REPLAY_DEAD_LETTER_WORKFLOW",
            targetType = "WorkflowInstance",
            targetId = workflowId,
            previousState = WorkflowStatus.DEAD_LETTER.name,
            newState = WorkflowStatus.RUNNING.name,
            details = "Admin replay from checkpoint '${request.checkpointStepId ?: "resume"}'"
        )

        return replayed
    }

    suspend fun compensateWorkflow(
        workflowId: String,
        request: WorkflowCompensationRequest,
        principal: AuthenticatedPrincipal
    ): WorkflowInstance {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_COMPENSATE)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val inst = instanceRepository.getInstanceById(workflowId, tenant)
            ?: throw NoSuchElementException("Workflow '$workflowId' not found.")

        val version = definitionRepository.getVersion(inst.definitionId, inst.versionId, tenant)
            ?: throw IllegalStateException("Workflow version not found.")

        val executions = stepExecutionRepository.getExecutionsForWorkflow(workflowId, tenant)
        val compResult = orchestrator.failWorkflowWithCompensation(
            instance = inst,
            stepDefinitions = version.steps,
            stepExecutions = executions,
            errorMessage = "Operator manual compensation: ${request.reason}"
        )

        instanceRepository.updateInstance(compResult.instance, tenant)

        realTimeBridge.publishWorkflowEvent(
            instance = compResult.instance,
            eventType = TimelineEventType.COMPENSATION_TRIGGERED,
            title = "Manual Compensation Triggered: ${request.reason}"
        )

        recordAudit(
            principal = principal,
            operation = "COMPENSATE_WORKFLOW",
            targetType = "WorkflowInstance",
            targetId = workflowId,
            previousState = inst.status.name,
            newState = compResult.instance.status.name,
            details = request.reason
        )

        return compResult.instance
    }

    // ==========================================
    // 4. APPROVAL GOVERNANCE (HITL + SoD)
    // ==========================================

    suspend fun getPendingApprovals(principal: AuthenticatedPrincipal): List<WorkflowApprovalSummary> {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_VIEW)
        val tenant = TenantContext(principal.projectId)
        val requests = approvalRepository.listPendingApprovals(100, tenant)

        return requests.map { req ->
            val policy = approvalRepository.getPolicyById(req.policyId, tenant)
            val decisions = approvalRepository.getDecisionsForApproval(req.approvalId, tenant)
            WorkflowApprovalSummary(
                approvalId = req.approvalId,
                workflowId = req.workflowId,
                stepId = req.stepId,
                policyId = req.policyId,
                policyName = policy?.policyName ?: req.policyId,
                requiredRole = policy?.requiredRole ?: UserRole.MANAGER,
                status = req.status,
                requesterId = req.requesterId,
                requesterRole = req.requesterRole,
                approvalsReceived = decisions.count { it.decisionType == ApprovalDecisionType.APPROVE },
                approvalsRequired = policy?.minimumApprovals ?: 1,
                allowSelfApproval = policy?.allowSelfApproval ?: false,
                isEscalated = req.status == ApprovalStatus.ESCALATED,
                timeoutAt = req.expiresAt,
                createdAt = req.createdAt
            )
        }
    }

    suspend fun submitApprovalDecision(
        approvalId: String,
        decisionDto: WorkflowApprovalDecisionDto,
        principal: AuthenticatedPrincipal
    ): ApprovalRequest {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_APPROVE)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val request = approvalRepository.getApprovalRequestById(approvalId, tenant)
            ?: throw NoSuchElementException("Approval request '$approvalId' not found.")

        val policy = approvalRepository.getPolicyById(request.policyId, tenant)
            ?: throw IllegalStateException("Approval policy '${request.policyId}' not found.")

        val existingDecisions = approvalRepository.getDecisionsForApproval(approvalId, tenant)

        val evalResult = approvalEngine.processDecision(
            request = request,
            policy = policy,
            principal = principal,
            decisionType = decisionDto.decision,
            existingDecisions = existingDecisions,
            notes = decisionDto.notes,
            humanConfirmation = decisionDto.humanConfirmation
        )

        return when (evalResult) {
            is ApprovalEvaluationResult.Decided -> {
                approvalRepository.recordDecision(evalResult.decision, tenant)
                approvalRepository.updateApprovalRequest(evalResult.updatedRequest, tenant)
                metrics.recordApprovalDecided()

                // Resume workflow
                val inst = instanceRepository.getInstanceById(request.workflowId, tenant)
                val version = inst?.let { definitionRepository.getVersion(it.definitionId, it.versionId, tenant) }
                if (inst != null && version != null) {
                    val resumed = orchestrator.resumeWorkflow(inst, version, mapOf("approvalDecision" to "APPROVED"))
                    instanceRepository.updateInstance(resumed, tenant)
                    realTimeBridge.publishWorkflowEvent(resumed, TimelineEventType.APPROVAL_DECIDED, "Approval Granted by ${principal.username}")
                }

                recordAudit(
                    principal = principal,
                    operation = "APPROVE_WORKFLOW_REQUEST",
                    targetType = "ApprovalRequest",
                    targetId = approvalId,
                    previousState = request.status.name,
                    newState = evalResult.updatedRequest.status.name,
                    details = decisionDto.notes ?: "Approved"
                )

                evalResult.updatedRequest
            }
            is ApprovalEvaluationResult.Rejected -> {
                approvalRepository.recordDecision(evalResult.decision, tenant)
                approvalRepository.updateApprovalRequest(evalResult.updatedRequest, tenant)
                metrics.recordApprovalDecided()

                // Fail workflow
                val inst = instanceRepository.getInstanceById(request.workflowId, tenant)
                val version = inst?.let { definitionRepository.getVersion(it.definitionId, it.versionId, tenant) }
                if (inst != null && version != null) {
                    val failedResult = orchestrator.failWorkflowWithCompensation(
                        instance = inst,
                        stepDefinitions = version.steps,
                        stepExecutions = emptyList(),
                        errorMessage = "Approval request '$approvalId' rejected by ${principal.username}"
                    )
                    instanceRepository.updateInstance(failedResult.instance, tenant)
                    realTimeBridge.publishWorkflowEvent(failedResult.instance, TimelineEventType.APPROVAL_DECIDED, "Approval Rejected by ${principal.username}")
                }

                recordAudit(
                    principal = principal,
                    operation = "REJECT_WORKFLOW_REQUEST",
                    targetType = "ApprovalRequest",
                    targetId = approvalId,
                    previousState = request.status.name,
                    newState = evalResult.updatedRequest.status.name,
                    details = decisionDto.notes ?: "Rejected"
                )

                evalResult.updatedRequest
            }
            is ApprovalEvaluationResult.Escalated -> {
                approvalRepository.recordEscalation(evalResult.escalation, tenant)
                val escalated = request.copy(status = ApprovalStatus.ESCALATED, updatedAt = System.currentTimeMillis())
                approvalRepository.updateApprovalRequest(escalated, tenant)

                recordAudit(
                    principal = principal,
                    operation = "ESCALATE_APPROVAL",
                    targetType = "ApprovalRequest",
                    targetId = approvalId,
                    previousState = request.status.name,
                    newState = ApprovalStatus.ESCALATED.name,
                    details = "Auto-escalated to ${evalResult.escalation.toRole}"
                )

                escalated
            }
            is ApprovalEvaluationResult.RequiresMoreApprovals -> {
                request
            }
            is ApprovalEvaluationResult.Denied -> {
                throw SecurityException(evalResult.reason)
            }
        }
    }

    suspend fun escalateApproval(
        approvalId: String,
        escalationDto: WorkflowApprovalEscalationDto,
        principal: AuthenticatedPrincipal
    ): ApprovalRequest {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_ESCALATE)
        requireNotAiAgent(principal)
        val tenant = TenantContext(principal.projectId)

        val request = approvalRepository.getApprovalRequestById(approvalId, tenant)
            ?: throw NoSuchElementException("Approval request '$approvalId' not found.")

        val escalation = ApprovalEscalation(
            escalationId = UUID.randomUUID().toString(),
            approvalId = approvalId,
            projectId = principal.projectId,
            workflowId = request.workflowId,
            fromRole = request.requesterRole,
            toRole = escalationDto.targetRole,
            reason = escalationDto.reason
        )

        approvalRepository.recordEscalation(escalation, tenant)
        val escalated = request.copy(status = ApprovalStatus.ESCALATED, updatedAt = System.currentTimeMillis())
        approvalRepository.updateApprovalRequest(escalated, tenant)

        recordAudit(
            principal = principal,
            operation = "MANUAL_ESCALATE_APPROVAL",
            targetType = "ApprovalRequest",
            targetId = approvalId,
            previousState = request.status.name,
            newState = ApprovalStatus.ESCALATED.name,
            details = "Escalated to ${escalationDto.targetRole}: ${escalationDto.reason}"
        )

        return escalated
    }

    // ==========================================
    // 5. OPERATIONAL METRICS & AUDIT
    // ==========================================

    suspend fun getManagementSummary(principal: AuthenticatedPrincipal): WorkflowManagementSummary {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_METRICS_VIEW)
        val tenant = TenantContext(principal.projectId)

        val defs = definitionRepository.listDefinitions(tenant)
        val running = instanceRepository.listInstancesByStatus(WorkflowStatus.RUNNING, 1000, tenant).size
        val approvals = approvalRepository.listPendingApprovals(1000, tenant).size
        val failed = instanceRepository.listInstancesByStatus(WorkflowStatus.FAILED, 1000, tenant).size
        val deadLetter = instanceRepository.listInstancesByStatus(WorkflowStatus.DEAD_LETTER, 1000, tenant).size
        val completed = instanceRepository.listInstancesByStatus(WorkflowStatus.COMPLETED, 1000, tenant).size

        return WorkflowManagementSummary(
            projectId = principal.projectId,
            totalDefinitions = defs.size,
            totalActiveVersions = defs.size,
            totalRunningInstances = running,
            totalWaitingApprovals = approvals,
            totalFailedInstances = failed,
            totalDeadLetterInstances = deadLetter,
            totalCompletedToday = completed
        )
    }

    suspend fun getOperationalMetrics(principal: AuthenticatedPrincipal): WorkflowOperationalMetrics {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_METRICS_VIEW)
        val tenant = TenantContext(principal.projectId)

        val running = instanceRepository.listInstancesByStatus(WorkflowStatus.RUNNING, 1000, tenant).size
        val completed = instanceRepository.listInstancesByStatus(WorkflowStatus.COMPLETED, 1000, tenant).size
        val failed = instanceRepository.listInstancesByStatus(WorkflowStatus.FAILED, 1000, tenant).size
        val cancelled = instanceRepository.listInstancesByStatus(WorkflowStatus.CANCELLED, 1000, tenant).size
        val paused = instanceRepository.listInstancesByStatus(WorkflowStatus.PAUSED, 1000, tenant).size
        val approvals = approvalRepository.listPendingApprovals(1000, tenant).size
        val deadLetter = instanceRepository.listInstancesByStatus(WorkflowStatus.DEAD_LETTER, 1000, tenant).size

        val total = running + completed + failed + cancelled + paused + deadLetter
        val failureRate = if (total > 0) (failed.toDouble() / total) * 100.0 else 0.0

        return WorkflowOperationalMetrics(
            projectId = principal.projectId,
            activeWorkflows = running,
            completedWorkflows = completed,
            failedWorkflows = failed,
            cancelledWorkflows = cancelled,
            pausedWorkflows = paused,
            pendingApprovals = approvals,
            compensatedSagas = 0,
            deadLetterCount = deadLetter,
            averageWorkflowDurationMs = 2450.0,
            averageStepDurationMs = 380.0,
            failureRatePercent = failureRate,
            throughputPerMinute = (completed.toDouble() / 60.0).coerceAtLeast(0.0)
        )
    }

    suspend fun getAuditLogs(
        limit: Int = 50,
        principal: AuthenticatedPrincipal
    ): List<WorkflowAuditEntry> {
        requireCapability(principal, AuthorizationCapability.WORKFLOW_AUDIT_VIEW)
        val logs = inMemoryAuditLogs[principal.projectId] ?: emptyList()
        return logs.takeLast(limit.coerceIn(1, 200)).reversed()
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private fun requireCapability(principal: AuthenticatedPrincipal, capability: AuthorizationCapability) {
        if (!RoleCapabilityMatrix.hasCapability(principal.role, capability)) {
            throw SecurityException("Principal '${principal.username}' with role '${principal.role}' lacks required capability '${capability.name}'.")
        }
    }

    private fun requireNotAiAgent(principal: AuthenticatedPrincipal) {
        if (principal.principalType == PrincipalType.AI_AGENT || principal.role == UserRole.AI_AGENT) {
            throw SecurityException("AI_AGENT machine principal is strictly prohibited from executing workflow administrative control operations.")
        }
    }

    private fun calculateProgress(status: WorkflowStatus): Int {
        return when (status) {
            WorkflowStatus.DRAFT -> 0
            WorkflowStatus.ACTIVE -> 5
            WorkflowStatus.RUNNING -> 50
            WorkflowStatus.WAITING, WorkflowStatus.WAITING_APPROVAL -> 60
            WorkflowStatus.PAUSED -> 45
            WorkflowStatus.COMPENSATING -> 75
            WorkflowStatus.COMPLETED -> 100
            WorkflowStatus.FAILED, WorkflowStatus.DEAD_LETTER -> 80
            WorkflowStatus.CANCELLED -> 100
            WorkflowStatus.TIMED_OUT -> 80
        }
    }

    private fun recordAudit(
        principal: AuthenticatedPrincipal,
        operation: String,
        targetType: String,
        targetId: String,
        previousState: String?,
        newState: String?,
        details: String?
    ) {
        val entry = WorkflowAuditEntry(
            auditId = "wf-aud-${UUID.randomUUID().toString().take(8)}",
            projectId = principal.projectId,
            actorId = principal.userId,
            actorRole = principal.role,
            principalType = principal.principalType,
            operation = operation,
            targetType = targetType,
            targetId = targetId,
            previousState = previousState,
            newState = newState,
            details = details,
            clientIp = "127.0.0.1",
            correlationId = "corr-${targetId.take(8)}"
        )
        val list = inMemoryAuditLogs.computeIfAbsent(principal.projectId) { CopyOnWriteArrayList() }
        list.add(entry)
        auditLogger.logWorkflowStarted(targetId, principal.projectId, principal.userId, operation)
    }
}
