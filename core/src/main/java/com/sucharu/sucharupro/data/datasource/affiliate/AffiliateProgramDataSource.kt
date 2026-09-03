package com.sucharu.sucharupro.data.datasource.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollment
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollmentStatus
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgram
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditRecord
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramEntityCategory
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramGovernanceSummary
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramOutboxEvent
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramStatus

/**
 * Data Source Contract for Affiliate Program and Enrollment persistence.
 */
interface AffiliateProgramDataSource {
    suspend fun saveProgram(program: AffiliateProgram): AffiliateProgram
    suspend fun findProgramById(tenantId: String, programId: String): AffiliateProgram?
    suspend fun findProgramByCode(tenantId: String, programCode: String): AffiliateProgram?
    suspend fun listPrograms(tenantId: String, status: AffiliateProgramStatus? = null): List<AffiliateProgram>

    suspend fun saveEnrollment(enrollment: AffiliateEnrollment): AffiliateEnrollment
    suspend fun findEnrollmentById(tenantId: String, enrollmentId: String): AffiliateEnrollment?
    suspend fun findEnrollmentsByAffiliate(tenantId: String, affiliateId: String): List<AffiliateEnrollment>
    suspend fun findEnrollmentsByProgram(tenantId: String, programId: String, status: AffiliateEnrollmentStatus? = null): List<AffiliateEnrollment>
    suspend fun listEnrollments(tenantId: String, status: AffiliateEnrollmentStatus? = null): List<AffiliateEnrollment>

    suspend fun appendAuditRecord(record: AffiliateProgramAuditRecord): AffiliateProgramAuditRecord
    suspend fun getLatestAuditRecord(tenantId: String, entityType: AffiliateProgramEntityCategory, entityId: String): AffiliateProgramAuditRecord?
    suspend fun listAuditRecords(tenantId: String, entityType: AffiliateProgramEntityCategory, entityId: String): List<AffiliateProgramAuditRecord>

    suspend fun saveOutboxEvent(event: AffiliateProgramOutboxEvent): AffiliateProgramOutboxEvent
    suspend fun getGovernanceSummary(tenantId: String): AffiliateProgramGovernanceSummary
}
