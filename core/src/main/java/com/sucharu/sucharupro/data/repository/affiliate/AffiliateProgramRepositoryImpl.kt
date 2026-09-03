package com.sucharu.sucharupro.data.repository.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.AffiliateProgramDataSource
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollment
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateEnrollmentStatus
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgram
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramAuditRecord
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramEntityCategory
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramGovernanceSummary
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramOutboxEvent
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateProgramStatus
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateProgramRepository

/**
 * Implementation of AffiliateProgramRepository delegating to an AffiliateProgramDataSource.
 */
class AffiliateProgramRepositoryImpl(
    private val dataSource: AffiliateProgramDataSource
) : AffiliateProgramRepository {

    override suspend fun saveProgram(program: AffiliateProgram): AffiliateProgram {
        return dataSource.saveProgram(program)
    }

    override suspend fun findProgramById(tenantId: String, programId: String): AffiliateProgram? {
        return dataSource.findProgramById(tenantId, programId)
    }

    override suspend fun findProgramByCode(tenantId: String, programCode: String): AffiliateProgram? {
        return dataSource.findProgramByCode(tenantId, programCode)
    }

    override suspend fun listPrograms(tenantId: String, status: AffiliateProgramStatus?): List<AffiliateProgram> {
        return dataSource.listPrograms(tenantId, status)
    }

    override suspend fun saveEnrollment(enrollment: AffiliateEnrollment): AffiliateEnrollment {
        return dataSource.saveEnrollment(enrollment)
    }

    override suspend fun findEnrollmentById(tenantId: String, enrollmentId: String): AffiliateEnrollment? {
        return dataSource.findEnrollmentById(tenantId, enrollmentId)
    }

    override suspend fun findEnrollmentsByAffiliate(tenantId: String, affiliateId: String): List<AffiliateEnrollment> {
        return dataSource.findEnrollmentsByAffiliate(tenantId, affiliateId)
    }

    override suspend fun findEnrollmentsByProgram(
        tenantId: String,
        programId: String,
        status: AffiliateEnrollmentStatus?
    ): List<AffiliateEnrollment> {
        return dataSource.findEnrollmentsByProgram(tenantId, programId, status)
    }

    override suspend fun listEnrollments(tenantId: String, status: AffiliateEnrollmentStatus?): List<AffiliateEnrollment> {
        return dataSource.listEnrollments(tenantId, status)
    }

    override suspend fun appendAuditRecord(record: AffiliateProgramAuditRecord): AffiliateProgramAuditRecord {
        return dataSource.appendAuditRecord(record)
    }

    override suspend fun getLatestAuditRecord(
        tenantId: String,
        entityType: AffiliateProgramEntityCategory,
        entityId: String
    ): AffiliateProgramAuditRecord? {
        return dataSource.getLatestAuditRecord(tenantId, entityType, entityId)
    }

    override suspend fun listAuditRecords(
        tenantId: String,
        entityType: AffiliateProgramEntityCategory,
        entityId: String
    ): List<AffiliateProgramAuditRecord> {
        return dataSource.listAuditRecords(tenantId, entityType, entityId)
    }

    override suspend fun saveOutboxEvent(event: AffiliateProgramOutboxEvent): AffiliateProgramOutboxEvent {
        return dataSource.saveOutboxEvent(event)
    }

    override suspend fun getGovernanceSummary(tenantId: String): AffiliateProgramGovernanceSummary {
        return dataSource.getGovernanceSummary(tenantId)
    }
}
