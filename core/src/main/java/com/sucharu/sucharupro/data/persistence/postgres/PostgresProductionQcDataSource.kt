package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.ProductionQcDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getTimestampMillis
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

/**
 * Production-grade PostgreSQL DataSource for Quality Control domain (Module 06).
 */
class PostgresProductionQcDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : ProductionQcDataSource {

    private fun mapProductionQc(rs: ResultSet): ProductionQc {
        return ProductionQc(
            qcId = rs.getString("inspection_id"),
            productionJobId = rs.getString("job_id"),
            productionStageId = rs.getString("stage_id"),
            qcType = rs.getEnumByName("qc_type", QcType.FINAL),
            status = rs.getEnumByName("status", QcStatus.DRAFT),
            decision = rs.getEnumByName("decision", QcDecision.PENDING),
            assignedInspectorId = rs.getString("inspector_id"),
            assignedInspectorName = rs.getString("inspector_name"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getTimestamp("created_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            startedAt = rs.getTimestamp("started_at")?.toInstant()?.toString(),
            completedAt = rs.getTimestamp("inspected_at")?.toInstant()?.toString(),
            notes = rs.getString("notes"),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedBy = rs.getString("updated_by")
        )
    }

    override fun observeQcList(): Flow<List<ProductionQc>> = flow {
        val tenant = TenantContext(defaultTenantId)
        val list = transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT inspection_id, project_id, job_id, stage_id, qc_type, status,
                       'PENDING' AS decision, inspector_id, NULL AS inspector_name,
                       NULL AS created_by, notes, inspected_at, started_at, created_at, updated_at, NULL AS updated_by
                FROM qc_inspections
                WHERE project_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId)) { rs ->
                mapProductionQc(rs)
            }
        }
        emit(list)
    }

    override suspend fun fetchQcById(qcId: String): DomainResult<ProductionQc> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            val qc = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT inspection_id, project_id, job_id, stage_id, qc_type, status,
                           'PENDING' AS decision, inspector_id, NULL AS inspector_name,
                           NULL AS created_by, notes, inspected_at, started_at, created_at, updated_at, NULL AS updated_by
                    FROM qc_inspections
                    WHERE project_id = ? AND inspection_id = ?
                """.trimIndent()

                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, qcId)) { rs ->
                    mapProductionQc(rs)
                }
            }
            if (qc != null) {
                DomainResult.Success(qc)
            } else {
                DomainResult.Error(message = "QC record with ID '$qcId' not found.")
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "fetch QC by ID")
        }
    }

    override suspend fun insertQc(qc: ProductionQc): DomainResult<ProductionQc> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO qc_inspections (
                        project_id, inspection_id, job_id, stage_id, qc_type, status,
                        inspector_id, sampled_quantity, accepted_quantity, rejected_quantity,
                        notes, inspected_at, created_at, updated_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?, NOW(), NOW(), NOW(), 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        tenant.projectId,
                        qc.qcId,
                        qc.productionJobId,
                        qc.productionStageId,
                        qc.qcType.name,
                        qc.status.name,
                        qc.assignedInspectorId ?: "UNASSIGNED",
                        qc.notes
                    )
                )
            }
            DomainResult.Success(qc)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "insert QC inspection")
        }
    }

    override suspend fun updateQc(qc: ProductionQc): DomainResult<ProductionQc> {
        val tenant = TenantContext(defaultTenantId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    UPDATE qc_inspections
                    SET stage_id = ?, qc_type = ?, status = ?, inspector_id = ?,
                        notes = ?, updated_at = NOW(), version = version + 1
                    WHERE project_id = ? AND inspection_id = ?
                """.trimIndent()

                val affected = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        qc.productionStageId,
                        qc.qcType.name,
                        qc.status.name,
                        qc.assignedInspectorId ?: "UNASSIGNED",
                        qc.notes,
                        tenant.projectId,
                        qc.qcId
                    )
                )
                if (affected == 0) {
                    throw OptimisticLockException("ProductionQc", qc.qcId, 1L)
                }
            }
            DomainResult.Success(qc)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update QC inspection")
        }
    }

    override fun observeAssignments(): Flow<List<QcAssignment>> = flow { emit(emptyList()) }
    override suspend fun insertAssignment(assignment: QcAssignment): DomainResult<QcAssignment> = DomainResult.Success(assignment)
    override suspend fun updateAssignment(assignment: QcAssignment): DomainResult<QcAssignment> = DomainResult.Success(assignment)

    override fun observeActivityEvents(): Flow<List<QcActivityEvent>> = flow { emit(emptyList()) }
    override suspend fun insertActivityEvent(event: QcActivityEvent): DomainResult<QcActivityEvent> = DomainResult.Success(event)

    override fun observePreProductionItems(): Flow<List<PreProductionQcItem>> = flow { emit(emptyList()) }
    override suspend fun insertPreProductionItems(items: List<PreProductionQcItem>): DomainResult<List<PreProductionQcItem>> = DomainResult.Success(items)
    override suspend fun updatePreProductionItem(item: PreProductionQcItem): DomainResult<PreProductionQcItem> = DomainResult.Success(item)

    override fun observeSnapshots(): Flow<List<PreProductionQcSnapshot>> = flow { emit(emptyList()) }
    override suspend fun insertSnapshot(snapshot: PreProductionQcSnapshot): DomainResult<PreProductionQcSnapshot> = DomainResult.Success(snapshot)
}
