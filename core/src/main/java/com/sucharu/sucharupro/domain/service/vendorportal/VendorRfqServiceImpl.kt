package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.repository.VendorRfqRepository
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorRfqValidator
import java.util.UUID

class VendorRfqServiceImpl(
    private val rfqRepository: VendorRfqRepository,
    private val vendorRepository: VendorRepository
) : VendorRfqService {

    override suspend fun createRfq(
        rfq: VendorRfq,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq> {
        return try {
            VendorRfqValidator.validateRfq(rfq)
            rfq.items.forEach { VendorRfqValidator.validateRfqItem(it) }

            val created = rfqRepository.createRfq(rfq.copy(status = VendorRfqStatus.DRAFT, createdBy = actorId))
            if (created is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = rfq.projectId,
                        rfqId = rfq.rfqId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.RFQ_CREATED,
                        action = "CREATE_RFQ",
                        details = "Created RFQ '${rfq.rfqNumber}'"
                    )
                )
            }
            created
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun publishRfq(
        rfqId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq> {
        return try {
            val rfq = when (val res = rfqRepository.findRfqById(rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorRfqValidator.validateRfqTransition(rfq.status, VendorRfqStatus.PUBLISHED)
            require(rfq.items.isNotEmpty()) { "Cannot publish an RFQ with no requirement items." }

            val updated = rfqRepository.updateRfq(
                rfq.copy(
                    status = VendorRfqStatus.PUBLISHED,
                    updatedBy = actorId,
                    updatedAt = System.currentTimeMillis(),
                    version = rfq.version + 1
                )
            )

            if (updated is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = rfq.projectId,
                        rfqId = rfq.rfqId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.RFQ_PUBLISHED,
                        action = "PUBLISH_RFQ",
                        details = "Published RFQ '${rfq.rfqNumber}'"
                    )
                )
            }
            updated
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun openRfq(
        rfqId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq> {
        return try {
            val rfq = when (val res = rfqRepository.findRfqById(rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorRfqValidator.validateRfqTransition(rfq.status, VendorRfqStatus.OPEN)

            val updated = rfqRepository.updateRfq(
                rfq.copy(
                    status = VendorRfqStatus.OPEN,
                    updatedBy = actorId,
                    updatedAt = System.currentTimeMillis(),
                    version = rfq.version + 1
                )
            )

            if (updated is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = rfq.projectId,
                        rfqId = rfq.rfqId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.RFQ_OPENED,
                        action = "OPEN_RFQ",
                        details = "Opened bidding for RFQ '${rfq.rfqNumber}'"
                    )
                )
            }
            updated
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun closeRfq(
        rfqId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq> {
        return try {
            val rfq = when (val res = rfqRepository.findRfqById(rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorRfqValidator.validateRfqTransition(rfq.status, VendorRfqStatus.CLOSED)

            val updated = rfqRepository.updateRfq(
                rfq.copy(
                    status = VendorRfqStatus.CLOSED,
                    updatedBy = actorId,
                    updatedAt = System.currentTimeMillis(),
                    version = rfq.version + 1
                )
            )

            if (updated is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = rfq.projectId,
                        rfqId = rfq.rfqId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.RFQ_CLOSED,
                        action = "CLOSE_RFQ",
                        details = "Closed bidding for RFQ '${rfq.rfqNumber}'"
                    )
                )
            }
            updated
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun cancelRfq(
        rfqId: String,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq> {
        return try {
            require(reason.isNotBlank()) { "Cancellation reason cannot be blank." }
            val rfq = when (val res = rfqRepository.findRfqById(rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorRfqValidator.validateRfqTransition(rfq.status, VendorRfqStatus.CANCELLED)

            val updated = rfqRepository.updateRfq(
                rfq.copy(
                    status = VendorRfqStatus.CANCELLED,
                    description = "${rfq.description ?: ""}\n[Cancelled: $reason]".trim(),
                    updatedBy = actorId,
                    updatedAt = System.currentTimeMillis(),
                    version = rfq.version + 1
                )
            )

            if (updated is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = rfq.projectId,
                        rfqId = rfq.rfqId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.RFQ_CANCELLED,
                        action = "CANCEL_RFQ",
                        details = "Cancelled RFQ '${rfq.rfqNumber}': $reason"
                    )
                )
            }
            updated
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun extendDeadline(
        rfqId: String,
        newDeadline: Long,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfq> {
        return try {
            require(reason.isNotBlank()) { "Extension reason cannot be blank." }
            val rfq = when (val res = rfqRepository.findRfqById(rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            require(newDeadline > rfq.responseDeadline) {
                "New deadline ($newDeadline) must be strictly after the current deadline (${rfq.responseDeadline})."
            }
            require(rfq.status in setOf(VendorRfqStatus.PUBLISHED, VendorRfqStatus.OPEN, VendorRfqStatus.CLOSING)) {
                "Cannot extend deadline on RFQ in status ${rfq.status}."
            }

            val updated = rfqRepository.updateRfq(
                rfq.copy(
                    responseDeadline = newDeadline,
                    updatedBy = actorId,
                    updatedAt = System.currentTimeMillis(),
                    version = rfq.version + 1
                )
            )

            if (updated is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = rfq.projectId,
                        rfqId = rfq.rfqId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.DEADLINE_EXTENDED,
                        action = "EXTEND_DEADLINE",
                        details = "Extended response deadline to $newDeadline: $reason"
                    )
                )
            }
            updated
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getRfqById(rfqId: String, tenantId: String): DomainResult<VendorRfq> =
        rfqRepository.findRfqById(rfqId, tenantId)

    override suspend fun listRfqs(projectId: String?, status: VendorRfqStatus?, tenantId: String): DomainResult<List<VendorRfq>> =
        rfqRepository.listRfqs(projectId, status, tenantId)

    override suspend fun listRfqsForVendor(vendorId: String, status: VendorRfqStatus?, tenantId: String): DomainResult<List<VendorRfq>> =
        rfqRepository.listRfqsForVendor(vendorId, status, tenantId)

    override suspend fun getRfqForVendor(rfqId: String, vendorId: String, tenantId: String): DomainResult<VendorRfq> {
        val invRes = rfqRepository.findInvitation(rfqId, vendorId, tenantId)
        if (invRes is DomainResult.Error) {
            return DomainResult.Error(SecurityException("Vendor '$vendorId' is not invited to RFQ '$rfqId'."))
        }
        val rfqRes = rfqRepository.findRfqById(rfqId, tenantId)
        if (rfqRes is DomainResult.Success) {
            // Update viewed timestamp on invitation if not viewed before
            val inv = (invRes as DomainResult.Success).data
            if (inv.viewedAt == null) {
                rfqRepository.updateInvitation(
                    inv.copy(
                        status = VendorRfqInvitationStatus.VIEWED,
                        viewedAt = System.currentTimeMillis(),
                        version = inv.version + 1
                    )
                )
            }
        }
        return rfqRes
    }

    override suspend fun inviteVendor(
        rfqId: String,
        vendorId: String,
        tenantId: String,
        projectId: String,
        actorId: String
    ): DomainResult<VendorRfqInvitation> {
        return try {
            val rfq = when (val res = rfqRepository.findRfqById(rfqId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return DomainResult.Error(res.exception)
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            // Verify vendor exists
            val vRes = vendorRepository.findById(projectId, vendorId)
            if (vRes is DomainResult.Error) {
                return DomainResult.Error(NoSuchElementException("Vendor '$vendorId' not found in project '$projectId'."))
            }

            val invitation = VendorRfqInvitation(
                invitationId = UUID.randomUUID().toString(),
                rfqId = rfqId,
                vendorId = vendorId,
                projectId = projectId,
                tenantId = tenantId,
                status = VendorRfqInvitationStatus.INVITED,
                invitedAt = System.currentTimeMillis()
            )
            VendorRfqValidator.validateInvitation(invitation)

            val created = rfqRepository.createInvitation(invitation)
            if (created is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = projectId,
                        rfqId = rfqId,
                        vendorId = vendorId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.VENDOR_INVITED,
                        action = "INVITE_VENDOR",
                        details = "Invited vendor '$vendorId' to RFQ '${rfq.rfqNumber}'"
                    )
                )
            }
            created
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun acknowledgeInvitation(
        rfqId: String,
        vendorId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfqInvitation> {
        return try {
            val inv = when (val res = rfqRepository.findInvitation(rfqId, vendorId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorRfqValidator.validateInvitationTransition(inv.status, VendorRfqInvitationStatus.ACKNOWLEDGED)

            val updated = rfqRepository.updateInvitation(
                inv.copy(
                    status = VendorRfqInvitationStatus.ACKNOWLEDGED,
                    acknowledgedAt = System.currentTimeMillis(),
                    version = inv.version + 1
                )
            )

            if (updated is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = inv.projectId,
                        rfqId = rfqId,
                        vendorId = vendorId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.INVITATION_ACKNOWLEDGED,
                        action = "ACKNOWLEDGE_INVITATION",
                        details = "Vendor '$vendorId' acknowledged RFQ invitation."
                    )
                )
            }
            updated
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun declineInvitation(
        rfqId: String,
        vendorId: String,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorRfqInvitation> {
        return try {
            require(reason.isNotBlank()) { "Decline reason cannot be blank." }
            val inv = when (val res = rfqRepository.findInvitation(rfqId, vendorId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            VendorRfqValidator.validateInvitationTransition(inv.status, VendorRfqInvitationStatus.DECLINED)

            val updated = rfqRepository.updateInvitation(
                inv.copy(
                    status = VendorRfqInvitationStatus.DECLINED,
                    declineReason = reason,
                    version = inv.version + 1
                )
            )

            if (updated is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = inv.projectId,
                        rfqId = rfqId,
                        vendorId = vendorId,
                        actorUserId = actorId,
                        eventType = VendorRfqAuditEventType.INVITATION_DECLINED,
                        action = "DECLINE_INVITATION",
                        details = "Vendor '$vendorId' declined RFQ invitation: $reason"
                    )
                )
            }
            updated
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listInvitations(rfqId: String, tenantId: String): DomainResult<List<VendorRfqInvitation>> =
        rfqRepository.listInvitationsByRfq(rfqId, tenantId)

    override suspend fun askClarification(
        rfqId: String,
        vendorId: String,
        question: String,
        askedBy: String,
        tenantId: String,
        projectId: String
    ): DomainResult<VendorRfqClarification> {
        return try {
            // Verify vendor is invited
            val invRes = rfqRepository.findInvitation(rfqId, vendorId, tenantId)
            if (invRes is DomainResult.Error) {
                return DomainResult.Error(SecurityException("Vendor '$vendorId' is not invited to RFQ '$rfqId'."))
            }

            val clarification = VendorRfqClarification(
                clarificationId = UUID.randomUUID().toString(),
                rfqId = rfqId,
                vendorId = vendorId,
                tenantId = tenantId,
                projectId = projectId,
                question = question,
                askedBy = askedBy,
                askedAt = System.currentTimeMillis()
            )
            VendorRfqValidator.validateClarification(clarification)

            val created = rfqRepository.createClarification(clarification)
            if (created is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = projectId,
                        rfqId = rfqId,
                        vendorId = vendorId,
                        actorUserId = askedBy,
                        eventType = VendorRfqAuditEventType.CLARIFICATION_CREATED,
                        action = "ASK_CLARIFICATION",
                        details = "Question: $question"
                    )
                )
            }
            created
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun answerClarification(
        clarificationId: String,
        answer: String,
        answeredBy: String,
        visibility: VendorClarificationVisibility,
        tenantId: String
    ): DomainResult<VendorRfqClarification> {
        return try {
            VendorRfqValidator.validateClarificationAnswer(answer, answeredBy)
            val clarification = when (val res = rfqRepository.findClarificationById(clarificationId, tenantId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return res
                DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Unexpected loading state"))
            }

            val updated = rfqRepository.updateClarification(
                clarification.copy(
                    answer = answer,
                    answeredBy = answeredBy,
                    answeredAt = System.currentTimeMillis(),
                    status = VendorClarificationStatus.ANSWERED,
                    visibility = visibility,
                    version = clarification.version + 1
                )
            )

            if (updated is DomainResult.Success) {
                rfqRepository.recordAuditEvent(
                    VendorRfqAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        tenantId = tenantId,
                        projectId = clarification.projectId,
                        rfqId = clarification.rfqId,
                        vendorId = clarification.vendorId,
                        actorUserId = answeredBy,
                        eventType = VendorRfqAuditEventType.CLARIFICATION_ANSWERED,
                        action = "ANSWER_CLARIFICATION",
                        details = "Answer: $answer (Visibility: $visibility)"
                    )
                )
            }
            updated
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listClarifications(
        rfqId: String,
        vendorId: String?,
        tenantId: String
    ): DomainResult<List<VendorRfqClarification>> =
        rfqRepository.listClarifications(rfqId, vendorId, tenantId)

    override suspend fun listAuditEvents(rfqId: String, tenantId: String): DomainResult<List<VendorRfqAuditEvent>> =
        rfqRepository.listAuditEvents(rfqId, tenantId)
}
