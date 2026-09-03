package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateActorType
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollment
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollmentStatus
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgram
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditEventType
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditRecord
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramEntityCategory
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramGovernanceSummary
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramOutboxEvent
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramStatus
import com.sucharu.sucharupro.domain.model.affiliate.Module20Step02ProgramHandoffContract
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateProgramRepository
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository
import java.util.UUID

/**
 * Implementation of AffiliateProgramService handling multi-tenant programs and enrollment lifecycle.
 */
class AffiliateProgramServiceImpl(
    private val programRepository: AffiliateProgramRepository,
    private val affiliateRepository: AffiliateRepository
) : AffiliateProgramService {

    override suspend fun createProgram(
        tenantId: String,
        programCode: String,
        programName: String,
        description: String?,
        startDate: Long,
        endDate: Long?,
        eligibilityPolicy: String,
        termsReference: String?,
        termsVersion: String?,
        maxParticipants: Int?,
        actorId: String,
        actorRole: String,
        metadataJson: String?
    ): AffiliateProgram {
        AffiliateProgramValidationEngine.validateProgramCreation(
            programCode = programCode,
            programName = programName,
            startDate = startDate,
            endDate = endDate
        ).getOrThrow()

        val normalizedCode = AffiliateProgramValidationEngine.validateProgramCode(programCode).getOrThrow()

        // Check unique code per tenant
        val existing = programRepository.findProgramByCode(tenantId, normalizedCode)
        if (existing != null) {
            throw IllegalArgumentException("Program code '$normalizedCode' already exists in tenant '$tenantId'.")
        }

        val programId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val program = AffiliateProgram(
            programId = programId,
            tenantId = tenantId,
            programCode = normalizedCode,
            programName = programName.trim(),
            description = description?.trim(),
            status = AffiliateProgramStatus.DRAFT,
            startDate = startDate,
            endDate = endDate,
            eligibilityPolicy = eligibilityPolicy.trim(),
            termsReference = termsReference?.trim(),
            termsVersion = termsVersion?.trim(),
            maxParticipants = maxParticipants,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now,
            version = 1L,
            metadataJson = metadataJson
        )

        val saved = programRepository.saveProgram(program)

        // Append audit
        appendProgramAudit(
            tenantId = tenantId,
            entityType = AffiliateProgramEntityCategory.PROGRAM,
            entityId = saved.programId,
            eventType = AffiliateProgramAuditEventType.PROGRAM_CREATED,
            previousStatus = null,
            newStatus = saved.status.name,
            actorType = resolveActorType(actorRole),
            actorId = actorId,
            actorRole = actorRole,
            reason = "Program created in DRAFT status",
            correlationId = UUID.randomUUID().toString(),
            timestamp = now
        )

        // Outbox event
        programRepository.saveOutboxEvent(
            AffiliateProgramOutboxEvent(
                outboxId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                aggregateType = "PROGRAM",
                aggregateId = saved.programId,
                eventType = "AffiliateProgramCreated",
                payloadJson = """{"programId":"${saved.programId}","programCode":"${saved.programCode}","status":"${saved.status}"}""",
                correlationId = UUID.randomUUID().toString()
            )
        )

        return saved
    }

    override suspend fun getProgramById(tenantId: String, programId: String): AffiliateProgram? {
        return programRepository.findProgramById(tenantId, programId)
    }

    override suspend fun getProgramByCode(tenantId: String, programCode: String): AffiliateProgram? {
        return programRepository.findProgramByCode(tenantId, programCode)
    }

    override suspend fun listPrograms(tenantId: String, status: AffiliateProgramStatus?): List<AffiliateProgram> {
        return programRepository.listPrograms(tenantId, status)
    }

    override suspend fun updateProgram(
        tenantId: String,
        programId: String,
        programName: String?,
        description: String?,
        startDate: Long?,
        endDate: Long?,
        eligibilityPolicy: String?,
        termsReference: String?,
        termsVersion: String?,
        maxParticipants: Int?,
        actorId: String,
        actorRole: String,
        metadataJson: String?
    ): AffiliateProgram {
        val existing = programRepository.findProgramById(tenantId, programId)
            ?: throw IllegalArgumentException("Program '$programId' not found in tenant '$tenantId'.")

        val effectiveStart = startDate ?: existing.startDate
        val effectiveEnd = if (endDate != null) endDate else existing.endDate

        if (effectiveEnd != null && effectiveEnd < effectiveStart) {
            throw IllegalArgumentException("Program end date ($effectiveEnd) cannot be before start date ($effectiveStart).")
        }

        val updated = existing.copy(
            programName = programName?.trim() ?: existing.programName,
            description = description?.trim() ?: existing.description,
            startDate = effectiveStart,
            endDate = effectiveEnd,
            eligibilityPolicy = eligibilityPolicy?.trim() ?: existing.eligibilityPolicy,
            termsReference = termsReference?.trim() ?: existing.termsReference,
            termsVersion = termsVersion?.trim() ?: existing.termsVersion,
            maxParticipants = maxParticipants ?: existing.maxParticipants,
            updatedAt = System.currentTimeMillis(),
            metadataJson = metadataJson ?: existing.metadataJson
        )

        val saved = programRepository.saveProgram(updated)

        appendProgramAudit(
            tenantId = tenantId,
            entityType = AffiliateProgramEntityCategory.PROGRAM,
            entityId = saved.programId,
            eventType = AffiliateProgramAuditEventType.PROGRAM_UPDATED,
            previousStatus = existing.status.name,
            newStatus = saved.status.name,
            actorType = resolveActorType(actorRole),
            actorId = actorId,
            actorRole = actorRole,
            reason = "Program details updated",
            correlationId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis()
        )

        return saved
    }

    override suspend fun activateProgram(
        tenantId: String,
        programId: String,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateProgram {
        return transitionProgramStatus(
            tenantId = tenantId,
            programId = programId,
            targetStatus = AffiliateProgramStatus.ACTIVE,
            eventType = AffiliateProgramAuditEventType.PROGRAM_ACTIVATED,
            actorId = actorId,
            actorRole = actorRole,
            reason = reason
        )
    }

    override suspend fun pauseProgram(
        tenantId: String,
        programId: String,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateProgram {
        return transitionProgramStatus(
            tenantId = tenantId,
            programId = programId,
            targetStatus = AffiliateProgramStatus.PAUSED,
            eventType = AffiliateProgramAuditEventType.PROGRAM_PAUSED,
            actorId = actorId,
            actorRole = actorRole,
            reason = reason
        )
    }

    override suspend fun closeProgram(
        tenantId: String,
        programId: String,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateProgram {
        return transitionProgramStatus(
            tenantId = tenantId,
            programId = programId,
            targetStatus = AffiliateProgramStatus.CLOSED,
            eventType = AffiliateProgramAuditEventType.PROGRAM_CLOSED,
            actorId = actorId,
            actorRole = actorRole,
            reason = reason
        )
    }

    override suspend fun archiveProgram(
        tenantId: String,
        programId: String,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateProgram {
        return transitionProgramStatus(
            tenantId = tenantId,
            programId = programId,
            targetStatus = AffiliateProgramStatus.ARCHIVED,
            eventType = AffiliateProgramAuditEventType.PROGRAM_ARCHIVED,
            actorId = actorId,
            actorRole = actorRole,
            reason = reason
        )
    }

    private suspend fun transitionProgramStatus(
        tenantId: String,
        programId: String,
        targetStatus: AffiliateProgramStatus,
        eventType: AffiliateProgramAuditEventType,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateProgram {
        val existing = programRepository.findProgramById(tenantId, programId)
            ?: throw IllegalArgumentException("Program '$programId' not found in tenant '$tenantId'.")

        AffiliateProgramValidationEngine.validateProgramStatusTransition(
            currentStatus = existing.status,
            newStatus = targetStatus
        ).getOrThrow()

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = targetStatus,
            updatedAt = now
        )

        val saved = programRepository.saveProgram(updated)

        appendProgramAudit(
            tenantId = tenantId,
            entityType = AffiliateProgramEntityCategory.PROGRAM,
            entityId = saved.programId,
            eventType = eventType,
            previousStatus = existing.status.name,
            newStatus = saved.status.name,
            actorType = resolveActorType(actorRole),
            actorId = actorId,
            actorRole = actorRole,
            reason = reason,
            correlationId = UUID.randomUUID().toString(),
            timestamp = now
        )

        programRepository.saveOutboxEvent(
            AffiliateProgramOutboxEvent(
                outboxId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                aggregateType = "PROGRAM",
                aggregateId = saved.programId,
                eventType = eventType.name,
                payloadJson = """{"programId":"${saved.programId}","previousStatus":"${existing.status}","newStatus":"${saved.status}"}""",
                correlationId = UUID.randomUUID().toString()
            )
        )

        return saved
    }

    override suspend fun enrollAffiliate(
        tenantId: String,
        affiliateId: String,
        programId: String,
        enrollmentReason: String?,
        effectiveFrom: Long?,
        effectiveTo: Long?,
        actorId: String,
        actorRole: String,
        metadataJson: String?
    ): AffiliateEnrollment {
        val affiliate = affiliateRepository.findById(tenantId, affiliateId)
            ?: throw IllegalArgumentException("Affiliate '$affiliateId' not found in tenant '$tenantId'.")

        val program = programRepository.findProgramById(tenantId, programId)
            ?: throw IllegalArgumentException("Program '$programId' not found in tenant '$tenantId'.")

        val eligibility = affiliateRepository.findLatestEligibility(tenantId, affiliateId)
        val existingEnrollments = programRepository.findEnrollmentsByProgram(tenantId, programId)

        AffiliateProgramValidationEngine.validateEnrollmentEligibility(
            affiliate = affiliate,
            affiliateEligibility = eligibility,
            program = program,
            existingEnrollments = existingEnrollments
        ).getOrThrow()

        val enrollmentId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val enrollment = AffiliateEnrollment(
            enrollmentId = enrollmentId,
            tenantId = tenantId,
            affiliateId = affiliateId,
            programId = programId,
            enrollmentStatus = AffiliateEnrollmentStatus.PENDING,
            effectiveFrom = effectiveFrom ?: now,
            effectiveTo = effectiveTo,
            enrollmentReason = enrollmentReason?.trim(),
            requestedAt = now,
            createdAt = now,
            updatedAt = now,
            version = 1L,
            metadataJson = metadataJson
        )

        val saved = programRepository.saveEnrollment(enrollment)

        appendProgramAudit(
            tenantId = tenantId,
            entityType = AffiliateProgramEntityCategory.ENROLLMENT,
            entityId = saved.enrollmentId,
            eventType = AffiliateProgramAuditEventType.ENROLLMENT_REQUESTED,
            previousStatus = null,
            newStatus = saved.enrollmentStatus.name,
            actorType = resolveActorType(actorRole),
            actorId = actorId,
            actorRole = actorRole,
            reason = enrollmentReason ?: "Affiliate enrollment submitted",
            correlationId = UUID.randomUUID().toString(),
            timestamp = now
        )

        programRepository.saveOutboxEvent(
            AffiliateProgramOutboxEvent(
                outboxId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                aggregateType = "ENROLLMENT",
                aggregateId = saved.enrollmentId,
                eventType = "AffiliateEnrollmentRequested",
                payloadJson = """{"enrollmentId":"${saved.enrollmentId}","affiliateId":"$affiliateId","programId":"$programId"}""",
                correlationId = UUID.randomUUID().toString()
            )
        )

        return saved
    }

    override suspend fun getEnrollmentById(tenantId: String, enrollmentId: String): AffiliateEnrollment? {
        return programRepository.findEnrollmentById(tenantId, enrollmentId)
    }

    override suspend fun listEnrollments(tenantId: String, status: AffiliateEnrollmentStatus?): List<AffiliateEnrollment> {
        return programRepository.listEnrollments(tenantId, status)
    }

    override suspend fun findEnrollmentsByAffiliate(tenantId: String, affiliateId: String): List<AffiliateEnrollment> {
        return programRepository.findEnrollmentsByAffiliate(tenantId, affiliateId)
    }

    override suspend fun findEnrollmentsByProgram(
        tenantId: String,
        programId: String,
        status: AffiliateEnrollmentStatus?
    ): List<AffiliateEnrollment> {
        return programRepository.findEnrollmentsByProgram(tenantId, programId, status)
    }

    override suspend fun approveEnrollment(
        tenantId: String,
        enrollmentId: String,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateEnrollment {
        val existing = programRepository.findEnrollmentById(tenantId, enrollmentId)
            ?: throw IllegalArgumentException("Enrollment '$enrollmentId' not found in tenant '$tenantId'.")

        AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(
            currentStatus = existing.enrollmentStatus,
            newStatus = AffiliateEnrollmentStatus.APPROVED
        ).getOrThrow()

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            enrollmentStatus = AffiliateEnrollmentStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = now,
            updatedAt = now
        )

        val saved = programRepository.saveEnrollment(updated)

        appendProgramAudit(
            tenantId = tenantId,
            entityType = AffiliateProgramEntityCategory.ENROLLMENT,
            entityId = saved.enrollmentId,
            eventType = AffiliateProgramAuditEventType.ENROLLMENT_APPROVED,
            previousStatus = existing.enrollmentStatus.name,
            newStatus = saved.enrollmentStatus.name,
            actorType = resolveActorType(actorRole),
            actorId = actorId,
            actorRole = actorRole,
            reason = reason,
            correlationId = UUID.randomUUID().toString(),
            timestamp = now
        )

        return saved
    }

    override suspend fun rejectEnrollment(
        tenantId: String,
        enrollmentId: String,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateEnrollment {
        val existing = programRepository.findEnrollmentById(tenantId, enrollmentId)
            ?: throw IllegalArgumentException("Enrollment '$enrollmentId' not found in tenant '$tenantId'.")

        AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(
            currentStatus = existing.enrollmentStatus,
            newStatus = AffiliateEnrollmentStatus.REJECTED
        ).getOrThrow()

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            enrollmentStatus = AffiliateEnrollmentStatus.REJECTED,
            rejectedBy = actorId,
            rejectedAt = now,
            rejectionReason = reason,
            updatedAt = now
        )

        val saved = programRepository.saveEnrollment(updated)

        appendProgramAudit(
            tenantId = tenantId,
            entityType = AffiliateProgramEntityCategory.ENROLLMENT,
            entityId = saved.enrollmentId,
            eventType = AffiliateProgramAuditEventType.ENROLLMENT_REJECTED,
            previousStatus = existing.enrollmentStatus.name,
            newStatus = saved.enrollmentStatus.name,
            actorType = resolveActorType(actorRole),
            actorId = actorId,
            actorRole = actorRole,
            reason = reason,
            correlationId = UUID.randomUUID().toString(),
            timestamp = now
        )

        return saved
    }

    override suspend fun activateEnrollment(
        tenantId: String,
        enrollmentId: String,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateEnrollment {
        val existing = programRepository.findEnrollmentById(tenantId, enrollmentId)
            ?: throw IllegalArgumentException("Enrollment '$enrollmentId' not found in tenant '$tenantId'.")

        AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(
            currentStatus = existing.enrollmentStatus,
            newStatus = AffiliateEnrollmentStatus.ACTIVE
        ).getOrThrow()

        // Verify program is active
        val program = programRepository.findProgramById(tenantId, existing.programId)
            ?: throw IllegalStateException("Program '${existing.programId}' not found.")
        if (!program.isActive) {
            throw IllegalStateException("Cannot activate enrollment: program '${program.programCode}' is not ACTIVE (status: ${program.status}).")
        }

        // Verify affiliate is active
        val affiliate = affiliateRepository.findById(tenantId, existing.affiliateId)
            ?: throw IllegalStateException("Affiliate '${existing.affiliateId}' not found.")
        if (!affiliate.isActive) {
            throw IllegalStateException("Cannot activate enrollment: affiliate '${affiliate.affiliateCode}' is not ACTIVE (status: ${affiliate.status}).")
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            enrollmentStatus = AffiliateEnrollmentStatus.ACTIVE,
            effectiveFrom = existing.effectiveFrom ?: now,
            updatedAt = now
        )

        val saved = programRepository.saveEnrollment(updated)

        appendProgramAudit(
            tenantId = tenantId,
            entityType = AffiliateProgramEntityCategory.ENROLLMENT,
            entityId = saved.enrollmentId,
            eventType = AffiliateProgramAuditEventType.ENROLLMENT_ACTIVATED,
            previousStatus = existing.enrollmentStatus.name,
            newStatus = saved.enrollmentStatus.name,
            actorType = resolveActorType(actorRole),
            actorId = actorId,
            actorRole = actorRole,
            reason = reason,
            correlationId = UUID.randomUUID().toString(),
            timestamp = now
        )

        programRepository.saveOutboxEvent(
            AffiliateProgramOutboxEvent(
                outboxId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                aggregateType = "ENROLLMENT",
                aggregateId = saved.enrollmentId,
                eventType = "AffiliateEnrollmentActivated",
                payloadJson = """{"enrollmentId":"${saved.enrollmentId}","affiliateId":"${existing.affiliateId}","programId":"${existing.programId}"}""",
                correlationId = UUID.randomUUID().toString()
            )
        )

        return saved
    }

    override suspend fun suspendEnrollment(
        tenantId: String,
        enrollmentId: String,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateEnrollment {
        val existing = programRepository.findEnrollmentById(tenantId, enrollmentId)
            ?: throw IllegalArgumentException("Enrollment '$enrollmentId' not found in tenant '$tenantId'.")

        AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(
            currentStatus = existing.enrollmentStatus,
            newStatus = AffiliateEnrollmentStatus.SUSPENDED
        ).getOrThrow()

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            enrollmentStatus = AffiliateEnrollmentStatus.SUSPENDED,
            suspendedBy = actorId,
            suspendedAt = now,
            suspensionReason = reason,
            updatedAt = now
        )

        val saved = programRepository.saveEnrollment(updated)

        appendProgramAudit(
            tenantId = tenantId,
            entityType = AffiliateProgramEntityCategory.ENROLLMENT,
            entityId = saved.enrollmentId,
            eventType = AffiliateProgramAuditEventType.ENROLLMENT_SUSPENDED,
            previousStatus = existing.enrollmentStatus.name,
            newStatus = saved.enrollmentStatus.name,
            actorType = resolveActorType(actorRole),
            actorId = actorId,
            actorRole = actorRole,
            reason = reason,
            correlationId = UUID.randomUUID().toString(),
            timestamp = now
        )

        return saved
    }

    override suspend fun resumeEnrollment(
        tenantId: String,
        enrollmentId: String,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateEnrollment {
        val existing = programRepository.findEnrollmentById(tenantId, enrollmentId)
            ?: throw IllegalArgumentException("Enrollment '$enrollmentId' not found in tenant '$tenantId'.")

        AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(
            currentStatus = existing.enrollmentStatus,
            newStatus = AffiliateEnrollmentStatus.ACTIVE
        ).getOrThrow()

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            enrollmentStatus = AffiliateEnrollmentStatus.ACTIVE,
            suspendedBy = null,
            suspendedAt = null,
            suspensionReason = null,
            updatedAt = now
        )

        val saved = programRepository.saveEnrollment(updated)

        appendProgramAudit(
            tenantId = tenantId,
            entityType = AffiliateProgramEntityCategory.ENROLLMENT,
            entityId = saved.enrollmentId,
            eventType = AffiliateProgramAuditEventType.ENROLLMENT_RESUMED,
            previousStatus = existing.enrollmentStatus.name,
            newStatus = saved.enrollmentStatus.name,
            actorType = resolveActorType(actorRole),
            actorId = actorId,
            actorRole = actorRole,
            reason = reason,
            correlationId = UUID.randomUUID().toString(),
            timestamp = now
        )

        return saved
    }

    override suspend fun terminateEnrollment(
        tenantId: String,
        enrollmentId: String,
        actorId: String,
        actorRole: String,
        reason: String
    ): AffiliateEnrollment {
        val existing = programRepository.findEnrollmentById(tenantId, enrollmentId)
            ?: throw IllegalArgumentException("Enrollment '$enrollmentId' not found in tenant '$tenantId'.")

        AffiliateProgramValidationEngine.validateEnrollmentStatusTransition(
            currentStatus = existing.enrollmentStatus,
            newStatus = AffiliateEnrollmentStatus.TERMINATED
        ).getOrThrow()

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            enrollmentStatus = AffiliateEnrollmentStatus.TERMINATED,
            terminatedBy = actorId,
            terminatedAt = now,
            terminationReason = reason,
            updatedAt = now
        )

        val saved = programRepository.saveEnrollment(updated)

        appendProgramAudit(
            tenantId = tenantId,
            entityType = AffiliateProgramEntityCategory.ENROLLMENT,
            entityId = saved.enrollmentId,
            eventType = AffiliateProgramAuditEventType.ENROLLMENT_TERMINATED,
            previousStatus = existing.enrollmentStatus.name,
            newStatus = saved.enrollmentStatus.name,
            actorType = resolveActorType(actorRole),
            actorId = actorId,
            actorRole = actorRole,
            reason = reason,
            correlationId = UUID.randomUUID().toString(),
            timestamp = now
        )

        return saved
    }

    override suspend fun listProgramAuditRecords(tenantId: String, programId: String): List<AffiliateProgramAuditRecord> {
        return programRepository.listAuditRecords(tenantId, AffiliateProgramEntityCategory.PROGRAM, programId)
    }

    override suspend fun listEnrollmentAuditRecords(tenantId: String, enrollmentId: String): List<AffiliateProgramAuditRecord> {
        return programRepository.listAuditRecords(tenantId, AffiliateProgramEntityCategory.ENROLLMENT, enrollmentId)
    }

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateProgramGovernanceSummary {
        return programRepository.getGovernanceSummary(tenantId)
    }

    override suspend fun getHandoffContract(tenantId: String, enrollmentId: String): Module20Step02ProgramHandoffContract {
        val enrollment = programRepository.findEnrollmentById(tenantId, enrollmentId)
            ?: throw IllegalArgumentException("Enrollment '$enrollmentId' not found in tenant '$tenantId'.")

        val affiliate = affiliateRepository.findById(tenantId, enrollment.affiliateId)
            ?: throw IllegalArgumentException("Affiliate '${enrollment.affiliateId}' not found in tenant '$tenantId'.")

        val program = programRepository.findProgramById(tenantId, enrollment.programId)
            ?: throw IllegalArgumentException("Program '${enrollment.programId}' not found in tenant '$tenantId'.")

        return AffiliateProgramValidationEngine.buildHandoffContract(
            tenantId = tenantId,
            enrollment = enrollment,
            affiliate = affiliate,
            program = program
        )
    }

    private suspend fun appendProgramAudit(
        tenantId: String,
        entityType: AffiliateProgramEntityCategory,
        entityId: String,
        eventType: AffiliateProgramAuditEventType,
        previousStatus: String?,
        newStatus: String,
        actorType: AffiliateActorType,
        actorId: String,
        actorRole: String,
        reason: String,
        correlationId: String,
        timestamp: Long
    ) {
        val latestAudit = programRepository.getLatestAuditRecord(tenantId, entityType, entityId)
        val recordHash = AffiliateProgramValidationEngine.computeRecordHash(
            tenantId = tenantId,
            entityType = entityType,
            entityId = entityId,
            eventType = eventType,
            previousStatus = previousStatus,
            newStatus = newStatus,
            actorType = actorType,
            actorId = actorId,
            actorRole = actorRole,
            timestamp = timestamp,
            correlationId = correlationId,
            reason = reason
        )
        val chainHash = AffiliateProgramValidationEngine.computeChainHash(
            previousAuditHash = latestAudit?.chainHash,
            recordHash = recordHash
        )

        programRepository.appendAuditRecord(
            AffiliateProgramAuditRecord(
                auditId = UUID.randomUUID().toString(),
                tenantId = tenantId,
                entityType = entityType,
                entityId = entityId,
                eventType = eventType,
                previousStatus = previousStatus,
                newStatus = newStatus,
                actorType = actorType,
                actorId = actorId,
                actorRole = actorRole,
                reason = reason,
                correlationId = correlationId,
                recordHash = recordHash,
                previousAuditHash = latestAudit?.chainHash,
                chainHash = chainHash,
                timestamp = timestamp
            )
        )
    }

    private fun resolveActorType(actorRole: String): AffiliateActorType {
        return when {
            actorRole.equals("AI_AGENT", ignoreCase = true) -> AffiliateActorType.AI_AGENT
            actorRole.equals("SYSTEM", ignoreCase = true) -> AffiliateActorType.SYSTEM
            else -> AffiliateActorType.HUMAN
        }
    }
}
