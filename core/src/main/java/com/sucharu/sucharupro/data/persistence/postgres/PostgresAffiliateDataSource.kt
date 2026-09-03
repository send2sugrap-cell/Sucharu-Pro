package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.affiliate.AffiliateDataSource
import com.sucharu.sucharupro.domain.model.affiliate.*
import java.sql.ResultSet

/**
 * PostgreSQL implementation of AffiliateDataSource with RLS and Multi-Tenant Isolation (Module 20 Step 01).
 */
class PostgresAffiliateDataSource(
    private val transactionManager: TransactionManager
) : AffiliateDataSource {

    override suspend fun saveAffiliate(profile: AffiliateProfile): AffiliateProfile {
        return transactionManager.inTransaction(TenantContext(profile.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliates (
                    affiliate_id, tenant_id, user_id, customer_id, display_name, affiliate_code,
                    status, affiliate_type, contact_phone, contact_email, tax_id_or_gst,
                    onboarding_state, verification_state, agreement_reference, agreement_version,
                    agreement_accepted_at, agreement_accepted_by, joined_at, activated_at,
                    suspended_at, terminated_at, created_at, updated_at, version, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (affiliate_id) DO UPDATE SET
                    display_name = EXCLUDED.display_name,
                    status = EXCLUDED.status,
                    affiliate_type = EXCLUDED.affiliate_type,
                    contact_phone = EXCLUDED.contact_phone,
                    contact_email = EXCLUDED.contact_email,
                    tax_id_or_gst = EXCLUDED.tax_id_or_gst,
                    onboarding_state = EXCLUDED.onboarding_state,
                    verification_state = EXCLUDED.verification_state,
                    agreement_reference = EXCLUDED.agreement_reference,
                    agreement_version = EXCLUDED.agreement_version,
                    agreement_accepted_at = EXCLUDED.agreement_accepted_at,
                    agreement_accepted_by = EXCLUDED.agreement_accepted_by,
                    activated_at = EXCLUDED.activated_at,
                    suspended_at = EXCLUDED.suspended_at,
                    terminated_at = EXCLUDED.terminated_at,
                    updated_at = EXCLUDED.updated_at,
                    version = affiliates.version + 1,
                    metadata_json = EXCLUDED.metadata_json
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, profile.affiliateId)
                stmt.setString(2, profile.tenantId)
                stmt.setString(3, profile.userId)
                stmt.setString(4, profile.customerId)
                stmt.setString(5, profile.displayName)
                stmt.setString(6, profile.affiliateCode)
                stmt.setString(7, profile.status.name)
                stmt.setString(8, profile.affiliateType.name)
                stmt.setString(9, profile.contactPhone)
                stmt.setString(10, profile.contactEmail)
                stmt.setString(11, profile.taxIdOrGst)
                stmt.setString(12, profile.onboardingState.name)
                stmt.setString(13, profile.verificationState.name)
                stmt.setString(14, profile.agreementReference)
                stmt.setString(15, profile.agreementVersion)
                stmt.setObject(16, profile.agreementAcceptedAt)
                stmt.setString(17, profile.agreementAcceptedBy)
                stmt.setLong(18, profile.joinedAt)
                stmt.setObject(19, profile.activatedAt)
                stmt.setObject(20, profile.suspendedAt)
                stmt.setObject(21, profile.terminatedAt)
                stmt.setLong(22, profile.createdAt)
                stmt.setLong(23, profile.updatedAt)
                stmt.setLong(24, profile.version)
                stmt.setString(25, profile.metadataJson)
                stmt.executeUpdate()
            }
            profile
        }
    }

    override suspend fun findById(tenantId: String, affiliateId: String): AffiliateProfile? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliates WHERE tenant_id = ? AND affiliate_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAffiliateProfile(rs) else null
                }
            }
        }
    }

    override suspend fun findByUserId(tenantId: String, userId: String): AffiliateProfile? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliates WHERE tenant_id = ? AND user_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, userId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAffiliateProfile(rs) else null
                }
            }
        }
    }

    override suspend fun findByAffiliateCode(tenantId: String, affiliateCode: String): AffiliateProfile? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliates WHERE tenant_id = ? AND UPPER(affiliate_code) = UPPER(?)"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateCode)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAffiliateProfile(rs) else null
                }
            }
        }
    }

    override suspend fun listAffiliates(
        tenantId: String,
        status: AffiliateStatus?,
        affiliateType: AffiliateType?
    ): List<AffiliateProfile> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sb = StringBuilder("SELECT * FROM affiliates WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)

            if (status != null) {
                sb.append(" AND status = ?")
                params.add(status.name)
            }
            if (affiliateType != null) {
                sb.append(" AND affiliate_type = ?")
                params.add(affiliateType.name)
            }
            sb.append(" ORDER BY created_at DESC")

            conn.prepareStatement(sb.toString()).use { stmt ->
                params.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }
                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<AffiliateProfile>()
                    while (rs.next()) {
                        results.add(mapAffiliateProfile(rs))
                    }
                    results
                }
            }
        }
    }

    override suspend fun saveEligibility(eligibility: AffiliateEligibility): AffiliateEligibility {
        return transactionManager.inTransaction(TenantContext(eligibility.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_eligibility_records (
                    eligibility_id, tenant_id, affiliate_id, is_eligible, identity_verified,
                    agreement_accepted, account_active, tax_compliant, business_verified,
                    rejection_reasons, evaluated_at, evaluated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (eligibility_id) DO UPDATE SET
                    is_eligible = EXCLUDED.is_eligible,
                    identity_verified = EXCLUDED.identity_verified,
                    agreement_accepted = EXCLUDED.agreement_accepted,
                    account_active = EXCLUDED.account_active,
                    tax_compliant = EXCLUDED.tax_compliant,
                    business_verified = EXCLUDED.business_verified,
                    rejection_reasons = EXCLUDED.rejection_reasons,
                    evaluated_at = EXCLUDED.evaluated_at,
                    evaluated_by = EXCLUDED.evaluated_by
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, eligibility.eligibilityId)
                stmt.setString(2, eligibility.tenantId)
                stmt.setString(3, eligibility.affiliateId)
                stmt.setBoolean(4, eligibility.isEligible)
                stmt.setBoolean(5, eligibility.identityVerified)
                stmt.setBoolean(6, eligibility.agreementAccepted)
                stmt.setBoolean(7, eligibility.accountActive)
                stmt.setBoolean(8, eligibility.taxCompliant)
                stmt.setBoolean(9, eligibility.businessVerified)
                stmt.setString(10, eligibility.rejectionReasons.joinToString(";;"))
                stmt.setLong(11, eligibility.evaluatedAt)
                stmt.setString(12, eligibility.evaluatedBy)
                stmt.executeUpdate()
            }
            eligibility
        }
    }

    override suspend fun findLatestEligibility(tenantId: String, affiliateId: String): AffiliateEligibility? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_eligibility_records WHERE tenant_id = ? AND affiliate_id = ? ORDER BY evaluated_at DESC LIMIT 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAffiliateEligibility(rs) else null
                }
            }
        }
    }

    override suspend fun appendAuditRecord(record: AffiliateAuditRecord): AffiliateAuditRecord {
        return transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_audit_records (
                    audit_id, tenant_id, affiliate_id, event_type, previous_status, new_status,
                    actor_type, actor_id, actor_role, reason, correlation_id, record_hash,
                    previous_audit_hash, chain_hash, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (audit_id) DO NOTHING
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, record.auditId)
                stmt.setString(2, record.tenantId)
                stmt.setString(3, record.affiliateId)
                stmt.setString(4, record.eventType.name)
                stmt.setString(5, record.previousStatus?.name)
                stmt.setString(6, record.newStatus.name)
                stmt.setString(7, record.actorType.name)
                stmt.setString(8, record.actorId)
                stmt.setString(9, record.actorRole)
                stmt.setString(10, record.reason)
                stmt.setString(11, record.correlationId)
                stmt.setString(12, record.recordHash)
                stmt.setString(13, record.previousAuditHash)
                stmt.setString(14, record.chainHash)
                stmt.setLong(15, record.timestamp)
                stmt.executeUpdate()
            }
            record
        }
    }

    override suspend fun listAuditRecords(tenantId: String, affiliateId: String): List<AffiliateAuditRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_audit_records WHERE tenant_id = ? AND affiliate_id = ? ORDER BY timestamp ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<AffiliateAuditRecord>()
                    while (rs.next()) {
                        list.add(mapAffiliateAuditRecord(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun findLatestAuditRecord(tenantId: String, affiliateId: String): AffiliateAuditRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_audit_records WHERE tenant_id = ? AND affiliate_id = ? ORDER BY timestamp DESC LIMIT 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, affiliateId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAffiliateAuditRecord(rs) else null
                }
            }
        }
    }

    override suspend fun appendOutboxEvent(event: AffiliateOutboxEvent): AffiliateOutboxEvent {
        return transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO affiliate_outbox_events (
                    outbox_id, tenant_id, aggregate_id, event_type, payload_json, status,
                    correlation_id, version, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (outbox_id) DO NOTHING
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.outboxId)
                stmt.setString(2, event.tenantId)
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

    override suspend fun listPendingOutboxEvents(tenantId: String): List<AffiliateOutboxEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM affiliate_outbox_events WHERE tenant_id = ? AND status = 'PENDING' ORDER BY created_at ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<AffiliateOutboxEvent>()
                    while (rs.next()) {
                        list.add(
                            AffiliateOutboxEvent(
                                outboxId = rs.getString("outbox_id"),
                                tenantId = rs.getString("tenant_id"),
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

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateGovernanceSummary {
        val all = listAffiliates(tenantId)
        val active = all.count { it.status == AffiliateStatus.ACTIVE }.toLong()
        val pending = all.count { it.status == AffiliateStatus.PENDING }.toLong()
        val suspended = all.count { it.status == AffiliateStatus.SUSPENDED }.toLong()
        val terminated = all.count { it.status == AffiliateStatus.TERMINATED }.toLong()
        val verified = all.count { it.verificationState == VerificationState.VERIFIED }.toLong()

        val eligible = all.count { profile ->
            findLatestEligibility(tenantId, profile.affiliateId)?.isEligible == true
        }.toLong()

        return AffiliateGovernanceSummary(
            tenantId = tenantId,
            totalAffiliates = all.size.toLong(),
            activeAffiliates = active,
            pendingAffiliates = pending,
            suspendedAffiliates = suspended,
            terminatedAffiliates = terminated,
            verifiedCount = verified,
            eligibleCount = eligible,
            individualCount = all.count { it.affiliateType == AffiliateType.INDIVIDUAL }.toLong(),
            businessCount = all.count { it.affiliateType == AffiliateType.BUSINESS }.toLong(),
            partnerCount = all.count { it.affiliateType == AffiliateType.PARTNER }.toLong(),
            creatorCount = all.count { it.affiliateType == AffiliateType.CREATOR }.toLong(),
            referralPartnerCount = all.count { it.affiliateType == AffiliateType.REFERRAL_PARTNER }.toLong()
        )
    }

    private fun mapAffiliateProfile(rs: ResultSet): AffiliateProfile {
        return AffiliateProfile(
            affiliateId = rs.getString("affiliate_id"),
            tenantId = rs.getString("tenant_id"),
            userId = rs.getString("user_id"),
            customerId = rs.getString("customer_id"),
            displayName = rs.getString("display_name"),
            affiliateCode = rs.getString("affiliate_code"),
            status = AffiliateStatus.valueOf(rs.getString("status")),
            affiliateType = AffiliateType.valueOf(rs.getString("affiliate_type")),
            contactPhone = rs.getString("contact_phone"),
            contactEmail = rs.getString("contact_email"),
            taxIdOrGst = rs.getString("tax_id_or_gst"),
            onboardingState = OnboardingState.valueOf(rs.getString("onboarding_state")),
            verificationState = VerificationState.valueOf(rs.getString("verification_state")),
            agreementReference = rs.getString("agreement_reference"),
            agreementVersion = rs.getString("agreement_version"),
            agreementAcceptedAt = rs.getObject("agreement_accepted_at") as? Long,
            agreementAcceptedBy = rs.getString("agreement_accepted_by"),
            joinedAt = rs.getLong("joined_at"),
            activatedAt = rs.getObject("activated_at") as? Long,
            suspendedAt = rs.getObject("suspended_at") as? Long,
            terminatedAt = rs.getObject("terminated_at") as? Long,
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    private fun mapAffiliateEligibility(rs: ResultSet): AffiliateEligibility {
        val rawReasons = rs.getString("rejection_reasons") ?: ""
        val reasonsList = if (rawReasons.isBlank()) emptyList() else rawReasons.split(";;")
        return AffiliateEligibility(
            eligibilityId = rs.getString("eligibility_id"),
            tenantId = rs.getString("tenant_id"),
            affiliateId = rs.getString("affiliate_id"),
            isEligible = rs.getBoolean("is_eligible"),
            identityVerified = rs.getBoolean("identity_verified"),
            agreementAccepted = rs.getBoolean("agreement_accepted"),
            accountActive = rs.getBoolean("account_active"),
            taxCompliant = rs.getBoolean("tax_compliant"),
            businessVerified = rs.getBoolean("business_verified"),
            rejectionReasons = reasonsList,
            evaluatedAt = rs.getLong("evaluated_at"),
            evaluatedBy = rs.getString("evaluated_by")
        )
    }

    private fun mapAffiliateAuditRecord(rs: ResultSet): AffiliateAuditRecord {
        val prevStatusRaw = rs.getString("previous_status")
        val prevStatus = if (prevStatusRaw != null) AffiliateStatus.valueOf(prevStatusRaw) else null
        return AffiliateAuditRecord(
            auditId = rs.getString("audit_id"),
            tenantId = rs.getString("tenant_id"),
            affiliateId = rs.getString("affiliate_id"),
            eventType = AffiliateAuditEventType.valueOf(rs.getString("event_type")),
            previousStatus = prevStatus,
            newStatus = AffiliateStatus.valueOf(rs.getString("new_status")),
            actorType = AffiliateActorType.valueOf(rs.getString("actor_type")),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            reason = rs.getString("reason"),
            correlationId = rs.getString("correlation_id"),
            recordHash = rs.getString("record_hash"),
            previousAuditHash = rs.getString("previous_audit_hash"),
            chainHash = rs.getString("chain_hash"),
            timestamp = rs.getLong("timestamp")
        )
    }
}
