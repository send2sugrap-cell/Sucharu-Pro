package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.affiliate.AffiliateProfileDataSource
import com.sucharu.sucharupro.domain.model.affiliate.*
import java.sql.ResultSet

/**
 * PostgreSQL implementation of AffiliateProfileDataSource with RLS and Multi-Tenant Isolation (Module 20 Step 03).
 */
class PostgresAffiliateProfileDataSource(
    private val transactionManager: TransactionManager
) : AffiliateProfileDataSource {

    override suspend fun saveProfile(profile: AffiliateOperationalProfile): AffiliateOperationalProfile {
        return transactionManager.inTransaction(TenantContext(profile.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_profiles (
                    tenant_id, affiliate_id, display_name, legal_name, business_type, business_description,
                    contact_email, contact_phone, website, address_line1, address_line2, city, region,
                    country, postal_code, tax_id_or_gst, tax_information_reference, profile_status,
                    completeness_score, completeness_details_json, submitted_at, verified_at, suspended_at,
                    created_at, updated_at, version, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, affiliate_id) DO UPDATE SET
                    display_name = EXCLUDED.display_name,
                    legal_name = EXCLUDED.legal_name,
                    business_type = EXCLUDED.business_type,
                    business_description = EXCLUDED.business_description,
                    contact_email = EXCLUDED.contact_email,
                    contact_phone = EXCLUDED.contact_phone,
                    website = EXCLUDED.website,
                    address_line1 = EXCLUDED.address_line1,
                    address_line2 = EXCLUDED.address_line2,
                    city = EXCLUDED.city,
                    region = EXCLUDED.region,
                    country = EXCLUDED.country,
                    postal_code = EXCLUDED.postal_code,
                    tax_id_or_gst = EXCLUDED.tax_id_or_gst,
                    tax_information_reference = EXCLUDED.tax_information_reference,
                    profile_status = EXCLUDED.profile_status,
                    completeness_score = EXCLUDED.completeness_score,
                    completeness_details_json = EXCLUDED.completeness_details_json,
                    submitted_at = EXCLUDED.submitted_at,
                    verified_at = EXCLUDED.verified_at,
                    suspended_at = EXCLUDED.suspended_at,
                    updated_at = EXCLUDED.updated_at,
                    version = affiliate_profiles.version + 1,
                    metadata_json = EXCLUDED.metadata_json
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, profile.tenantId)
                stmt.setString(2, profile.affiliateId)
                stmt.setString(3, profile.displayName)
                stmt.setString(4, profile.legalName)
                stmt.setString(5, profile.businessType.name)
                stmt.setString(6, profile.businessDescription)
                stmt.setString(7, profile.contactEmail)
                stmt.setString(8, profile.contactPhone)
                stmt.setString(9, profile.website)
                stmt.setString(10, profile.addressLine1)
                stmt.setString(11, profile.addressLine2)
                stmt.setString(12, profile.city)
                stmt.setString(13, profile.region)
                stmt.setString(14, profile.country)
                stmt.setString(15, profile.postalCode)
                stmt.setString(16, profile.taxIdOrGst)
                stmt.setString(17, profile.taxInformationReference)
                stmt.setString(18, profile.profileStatus.name)
                stmt.setInt(19, profile.completenessScore)
                stmt.setString(20, profile.completenessDetailsJson)
                stmt.setObject(21, profile.submittedAt)
                stmt.setObject(22, profile.verifiedAt)
                stmt.setObject(23, profile.suspendedAt)
                stmt.setLong(24, profile.createdAt)
                stmt.setLong(25, profile.updatedAt)
                stmt.setLong(26, profile.version)
                stmt.setString(27, profile.metadataJson)
                stmt.executeUpdate()
            }
            profile
        }
    }

    override suspend fun findProfileByAffiliateId(tenantId: String, affiliateId: String): AffiliateOperationalProfile? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_profiles WHERE tenant_id = ? AND affiliate_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapProfile(rs) else null
                }
            }
        }
    }

    override suspend fun listProfiles(tenantId: String, status: AffiliateProfileStatus?, query: String?): List<AffiliateOperationalProfile> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = StringBuilder("SELECT * FROM affiliate_profiles WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)

            if (status != null) {
                sql.append(" AND profile_status = ?")
                params.add(status.name)
            }
            if (!query.isNullOrBlank()) {
                sql.append(" AND (LOWER(display_name) LIKE ? OR LOWER(legal_name) LIKE ? OR LOWER(contact_email) LIKE ?)")
                val q = "%${query.lowercase()}%"
                params.add(q)
                params.add(q)
                params.add(q)
            }
            sql.append(" ORDER BY updated_at DESC")

            conn.prepareStatement(sql.toString()).use { stmt ->
                params.forEachIndexed { index, param -> stmt.setObject(index + 1, param) }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<AffiliateOperationalProfile>()
                    while (rs.next()) {
                        list.add(mapProfile(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveVerification(record: AffiliateVerificationRecord): AffiliateVerificationRecord {
        return transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_verification_records (
                    tenant_id, verification_id, affiliate_id, verification_type, status,
                    submitted_at, reviewed_at, reviewer_user_id, reason, change_request_notes,
                    metadata_reference, previous_verification_id, expires_at, created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, verification_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    submitted_at = EXCLUDED.submitted_at,
                    reviewed_at = EXCLUDED.reviewed_at,
                    reviewer_user_id = EXCLUDED.reviewer_user_id,
                    reason = EXCLUDED.reason,
                    change_request_notes = EXCLUDED.change_request_notes,
                    metadata_reference = EXCLUDED.metadata_reference,
                    expires_at = EXCLUDED.expires_at,
                    updated_at = EXCLUDED.updated_at,
                    version = affiliate_verification_records.version + 1
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, record.tenantId)
                stmt.setString(2, record.verificationId)
                stmt.setString(3, record.affiliateId)
                stmt.setString(4, record.verificationType.name)
                stmt.setString(5, record.status.name)
                stmt.setObject(6, record.submittedAt)
                stmt.setObject(7, record.reviewedAt)
                stmt.setString(8, record.reviewerUserId)
                stmt.setString(9, record.reason)
                stmt.setString(10, record.changeRequestNotes)
                stmt.setString(11, record.metadataReference)
                stmt.setString(12, record.previousVerificationId)
                stmt.setObject(13, record.expiresAt)
                stmt.setLong(14, record.createdAt)
                stmt.setLong(15, record.updatedAt)
                stmt.setLong(16, record.version)
                stmt.executeUpdate()
            }
            record
        }
    }

    override suspend fun findVerificationById(tenantId: String, verificationId: String): AffiliateVerificationRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_verification_records WHERE tenant_id = ? AND verification_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, verificationId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapVerification(rs) else null
                }
            }
        }
    }

    override suspend fun listVerificationsByAffiliateId(tenantId: String, affiliateId: String): List<AffiliateVerificationRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_verification_records WHERE tenant_id = ? AND affiliate_id = ? ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<AffiliateVerificationRecord>()
                    while (rs.next()) {
                        list.add(mapVerification(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveDocument(doc: AffiliateDocumentReference): AffiliateDocumentReference {
        return transactionManager.inTransaction(TenantContext(doc.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_document_references (
                    tenant_id, document_id, affiliate_id, verification_id, document_type,
                    storage_reference, file_name, file_size_bytes, mime_type, status,
                    rejection_reason, uploaded_at, expires_at, verified_at, created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, document_id) DO UPDATE SET
                    verification_id = EXCLUDED.verification_id,
                    status = EXCLUDED.status,
                    rejection_reason = EXCLUDED.rejection_reason,
                    expires_at = EXCLUDED.expires_at,
                    verified_at = EXCLUDED.verified_at,
                    updated_at = EXCLUDED.updated_at,
                    version = affiliate_document_references.version + 1
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, doc.tenantId)
                stmt.setString(2, doc.documentId)
                stmt.setString(3, doc.affiliateId)
                stmt.setString(4, doc.verificationId)
                stmt.setString(5, doc.documentType.name)
                stmt.setString(6, doc.storageReference)
                stmt.setString(7, doc.fileName)
                stmt.setObject(8, doc.fileSizeBytes)
                stmt.setString(9, doc.mimeType)
                stmt.setString(10, doc.status.name)
                stmt.setString(11, doc.rejectionReason)
                stmt.setLong(12, doc.uploadedAt)
                stmt.setObject(13, doc.expiresAt)
                stmt.setObject(14, doc.verifiedAt)
                stmt.setLong(15, doc.createdAt)
                stmt.setLong(16, doc.updatedAt)
                stmt.setLong(17, doc.version)
                stmt.executeUpdate()
            }
            doc
        }
    }

    override suspend fun findDocumentById(tenantId: String, documentId: String): AffiliateDocumentReference? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_document_references WHERE tenant_id = ? AND document_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, documentId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapDocument(rs) else null
                }
            }
        }
    }

    override suspend fun listDocumentsByAffiliateId(tenantId: String, affiliateId: String): List<AffiliateDocumentReference> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_document_references WHERE tenant_id = ? AND affiliate_id = ? ORDER BY uploaded_at DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<AffiliateDocumentReference>()
                    while (rs.next()) {
                        list.add(mapDocument(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveAuditRecord(record: AffiliateProfileAuditRecord): AffiliateProfileAuditRecord {
        return transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_profile_audit_records (
                    tenant_id, audit_id, affiliate_id, actor_user_id, actor_role, actor_type,
                    action, entity_reference, previous_state, new_state, reason, correlation_id,
                    idempotency_key, record_hash, previous_audit_hash, chain_hash, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, record.tenantId)
                stmt.setString(2, record.auditId)
                stmt.setString(3, record.affiliateId)
                stmt.setString(4, record.actorUserId)
                stmt.setString(5, record.actorRole)
                stmt.setString(6, record.actorType.name)
                stmt.setString(7, record.action)
                stmt.setString(8, record.entityReference)
                stmt.setString(9, record.previousState)
                stmt.setString(10, record.newState)
                stmt.setString(11, record.reason)
                stmt.setString(12, record.correlationId)
                stmt.setString(13, record.idempotencyKey)
                stmt.setString(14, record.recordHash)
                stmt.setString(15, record.previousAuditHash)
                stmt.setString(16, record.chainHash)
                stmt.setLong(17, record.timestamp)
                stmt.executeUpdate()
            }
            record
        }
    }

    override suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateProfileAuditRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_profile_audit_records WHERE tenant_id = ? AND affiliate_id = ? ORDER BY timestamp ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<AffiliateProfileAuditRecord>()
                    while (rs.next()) {
                        list.add(mapAuditRecord(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun getLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateProfileAuditRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_profile_audit_records WHERE tenant_id = ? AND affiliate_id = ? ORDER BY timestamp DESC LIMIT 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAuditRecord(rs) else null
                }
            }
        }
    }

    override suspend fun saveOutboxEvent(event: AffiliateProfileOutboxEvent): AffiliateProfileOutboxEvent {
        return transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_profile_outbox_events (
                    tenant_id, outbox_id, aggregate_id, event_type, payload_json, status, correlation_id, version, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.tenantId)
                stmt.setString(2, event.outboxId)
                stmt.setString(3, event.aggregateId)
                stmt.setString(4, event.eventType)
                stmt.setString(5, event.payloadJson)
                stmt.setString(6, event.status)
                stmt.setString(7, event.correlationId)
                stmt.setLong(8, event.version)
                stmt.setLong(9, event.createdAt)
                stmt.executeUpdate()
            }
            event
        }
    }

    override suspend fun listPendingOutboxEvents(tenantId: String): List<AffiliateProfileOutboxEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_profile_outbox_events WHERE tenant_id = ? AND status = 'PENDING' ORDER BY created_at ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<AffiliateProfileOutboxEvent>()
                    while (rs.next()) {
                        list.add(
                            AffiliateProfileOutboxEvent(
                                tenantId = rs.getString("tenant_id"),
                                outboxId = rs.getString("outbox_id"),
                                aggregateId = rs.getString("aggregate_id"),
                                eventType = rs.getString("event_type"),
                                payloadJson = rs.getString("payload_json"),
                                status = rs.getString("status"),
                                correlationId = rs.getString("correlation_id"),
                                version = rs.getLong("version"),
                                createdAt = rs.getLong("created_at")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateProfileGovernanceSummary {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val pSql = """
                SELECT
                    COUNT(*) as total_profiles,
                    COUNT(*) FILTER (WHERE profile_status = 'VERIFIED') as verified_profiles,
                    COUNT(*) FILTER (WHERE profile_status IN ('SUBMITTED', 'UNDER_REVIEW')) as pending_profiles,
                    COUNT(*) FILTER (WHERE profile_status = 'INCOMPLETE') as incomplete_profiles,
                    COUNT(*) FILTER (WHERE profile_status = 'CHANGES_REQUIRED') as changes_required_profiles,
                    COUNT(*) FILTER (WHERE profile_status = 'SUSPENDED') as suspended_profiles
                FROM affiliate_profiles WHERE tenant_id = ?
            """.trimIndent()

            var totalProfiles = 0L
            var verifiedProfiles = 0L
            var pendingProfiles = 0L
            var incompleteProfiles = 0L
            var changesRequiredProfiles = 0L
            var suspendedProfiles = 0L

            conn.prepareStatement(pSql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        totalProfiles = rs.getLong("total_profiles")
                        verifiedProfiles = rs.getLong("verified_profiles")
                        pendingProfiles = rs.getLong("pending_profiles")
                        incompleteProfiles = rs.getLong("incomplete_profiles")
                        changesRequiredProfiles = rs.getLong("changes_required_profiles")
                        suspendedProfiles = rs.getLong("suspended_profiles")
                    }
                }
            }

            val vSql = """
                SELECT
                    COUNT(*) as total_verifs,
                    COUNT(*) FILTER (WHERE status = 'VERIFIED') as verified_verifs,
                    COUNT(*) FILTER (WHERE status IN ('SUBMITTED', 'UNDER_REVIEW')) as pending_verifs,
                    COUNT(*) FILTER (WHERE status = 'REJECTED') as rejected_verifs
                FROM affiliate_verification_records WHERE tenant_id = ?
            """.trimIndent()

            var totalVerifs = 0L
            var verifiedVerifs = 0L
            var pendingVerifs = 0L
            var rejectedVerifs = 0L

            conn.prepareStatement(vSql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        totalVerifs = rs.getLong("total_verifs")
                        verifiedVerifs = rs.getLong("verified_verifs")
                        pendingVerifs = rs.getLong("pending_verifs")
                        rejectedVerifs = rs.getLong("rejected_verifs")
                    }
                }
            }

            val dSql = """
                SELECT
                    COUNT(*) as total_docs,
                    COUNT(*) FILTER (WHERE status = 'VERIFIED') as verified_docs
                FROM affiliate_document_references WHERE tenant_id = ?
            """.trimIndent()

            var totalDocs = 0L
            var verifiedDocs = 0L

            conn.prepareStatement(dSql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        totalDocs = rs.getLong("total_docs")
                        verifiedDocs = rs.getLong("verified_docs")
                    }
                }
            }

            AffiliateProfileGovernanceSummary(
                tenantId = tenantId,
                totalProfiles = totalProfiles,
                verifiedProfiles = verifiedProfiles,
                pendingReviewProfiles = pendingProfiles,
                incompleteProfiles = incompleteProfiles,
                changesRequiredProfiles = changesRequiredProfiles,
                suspendedProfiles = suspendedProfiles,
                totalVerifications = totalVerifs,
                verifiedVerifications = verifiedVerifs,
                pendingVerifications = pendingVerifs,
                rejectedVerifications = rejectedVerifs,
                totalDocuments = totalDocs,
                verifiedDocuments = verifiedDocs
            )
        }
    }

    private fun mapProfile(rs: ResultSet): AffiliateOperationalProfile {
        return AffiliateOperationalProfile(
            tenantId = rs.getString("tenant_id"),
            affiliateId = rs.getString("affiliate_id"),
            displayName = rs.getString("display_name"),
            legalName = rs.getString("legal_name"),
            businessType = runCatching { AffiliateBusinessType.valueOf(rs.getString("business_type")) }.getOrDefault(AffiliateBusinessType.INDIVIDUAL),
            businessDescription = rs.getString("business_description"),
            contactEmail = rs.getString("contact_email"),
            contactPhone = rs.getString("contact_phone"),
            website = rs.getString("website"),
            addressLine1 = rs.getString("address_line1"),
            addressLine2 = rs.getString("address_line2"),
            city = rs.getString("city"),
            region = rs.getString("region"),
            country = rs.getString("country"),
            postalCode = rs.getString("postal_code"),
            taxIdOrGst = rs.getString("tax_id_or_gst"),
            taxInformationReference = rs.getString("tax_information_reference"),
            profileStatus = runCatching { AffiliateProfileStatus.valueOf(rs.getString("profile_status")) }.getOrDefault(AffiliateProfileStatus.INCOMPLETE),
            completenessScore = rs.getInt("completeness_score"),
            completenessDetailsJson = rs.getString("completeness_details_json"),
            submittedAt = rs.getObject("submitted_at") as? Long,
            verifiedAt = rs.getObject("verified_at") as? Long,
            suspendedAt = rs.getObject("suspended_at") as? Long,
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    private fun mapVerification(rs: ResultSet): AffiliateVerificationRecord {
        return AffiliateVerificationRecord(
            tenantId = rs.getString("tenant_id"),
            verificationId = rs.getString("verification_id"),
            affiliateId = rs.getString("affiliate_id"),
            verificationType = runCatching { AffiliateVerificationType.valueOf(rs.getString("verification_type")) }.getOrDefault(AffiliateVerificationType.OTHER),
            status = runCatching { AffiliateVerificationStatus.valueOf(rs.getString("status")) }.getOrDefault(AffiliateVerificationStatus.NOT_SUBMITTED),
            submittedAt = rs.getObject("submitted_at") as? Long,
            reviewedAt = rs.getObject("reviewed_at") as? Long,
            reviewerUserId = rs.getString("reviewer_user_id"),
            reason = rs.getString("reason"),
            changeRequestNotes = rs.getString("change_request_notes"),
            metadataReference = rs.getString("metadata_reference"),
            previousVerificationId = rs.getString("previous_verification_id"),
            expiresAt = rs.getObject("expires_at") as? Long,
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version")
        )
    }

    private fun mapDocument(rs: ResultSet): AffiliateDocumentReference {
        return AffiliateDocumentReference(
            tenantId = rs.getString("tenant_id"),
            documentId = rs.getString("document_id"),
            affiliateId = rs.getString("affiliate_id"),
            verificationId = rs.getString("verification_id"),
            documentType = runCatching { AffiliateDocumentType.valueOf(rs.getString("document_type")) }.getOrDefault(AffiliateDocumentType.OTHER),
            storageReference = rs.getString("storage_reference"),
            fileName = rs.getString("file_name"),
            fileSizeBytes = rs.getObject("file_size_bytes") as? Long,
            mimeType = rs.getString("mime_type"),
            status = runCatching { AffiliateDocumentStatus.valueOf(rs.getString("status")) }.getOrDefault(AffiliateDocumentStatus.UPLOADED),
            rejectionReason = rs.getString("rejection_reason"),
            uploadedAt = rs.getLong("uploaded_at"),
            expiresAt = rs.getObject("expires_at") as? Long,
            verifiedAt = rs.getObject("verified_at") as? Long,
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version")
        )
    }

    private fun mapAuditRecord(rs: ResultSet): AffiliateProfileAuditRecord {
        return AffiliateProfileAuditRecord(
            tenantId = rs.getString("tenant_id"),
            auditId = rs.getString("audit_id"),
            affiliateId = rs.getString("affiliate_id"),
            actorUserId = rs.getString("actor_user_id"),
            actorRole = rs.getString("actor_role"),
            actorType = runCatching { AffiliateActorType.valueOf(rs.getString("actor_type")) }.getOrDefault(AffiliateActorType.HUMAN),
            action = rs.getString("action"),
            entityReference = rs.getString("entity_reference"),
            previousState = rs.getString("previous_state"),
            newState = rs.getString("new_state"),
            reason = rs.getString("reason"),
            correlationId = rs.getString("correlation_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            recordHash = rs.getString("record_hash"),
            previousAuditHash = rs.getString("previous_audit_hash"),
            chainHash = rs.getString("chain_hash"),
            timestamp = rs.getLong("timestamp")
        )
    }
}
