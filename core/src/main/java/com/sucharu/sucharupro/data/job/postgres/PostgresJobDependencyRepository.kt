package com.sucharu.sucharupro.data.job.postgres

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.job.model.DependencyRequirement
import com.sucharu.sucharupro.domain.job.model.JobDependencyLink
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for workflow DAG dependency persistence (INFRA-04 Step 04).
 */
interface JobDependencyRepository {
    suspend fun addDependency(link: JobDependencyLink, tenantContext: TenantContext)
    suspend fun getDependenciesForJob(jobId: String, tenantContext: TenantContext): List<JobDependencyLink>
    suspend fun getDependentsOfJob(parentJobId: String, tenantContext: TenantContext): List<JobDependencyLink>
    suspend fun markDependencySatisfied(dependencyId: String, tenantContext: TenantContext)
}

/**
 * PostgreSQL implementation of [JobDependencyRepository] with multi-tenant RLS.
 */
class PostgresJobDependencyRepository(
    private val transactionManager: TransactionManager
) : JobDependencyRepository {

    private fun mapRowToDependency(rs: ResultSet): JobDependencyLink {
        return JobDependencyLink(
            dependencyId = rs.getString("dependency_id"),
            projectId = rs.getString("project_id"),
            jobId = rs.getString("job_id"),
            dependsOnJobId = rs.getString("depends_on_job_id"),
            requirement = DependencyRequirement.valueOf(rs.getString("required_status")),
            isSatisfied = rs.getBoolean("is_satisfied"),
            createdAt = rs.getTimestamp("created_at").time
        )
    }

    override suspend fun addDependency(link: JobDependencyLink, tenantContext: TenantContext) {
        require(link.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: link projectId '${link.projectId}' != tenant '${tenantContext.projectId}'"
        }

        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO job_dependencies (
                    dependency_id, project_id, job_id, depends_on_job_id,
                    required_status, is_satisfied, created_at
                ) VALUES (
                    ?, ?, ?, ?,
                    ?, ?, ?
                )
                ON CONFLICT (project_id, job_id, depends_on_job_id) DO UPDATE
                SET required_status = EXCLUDED.required_status
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    link.dependencyId,
                    tenantContext.projectId,
                    link.jobId,
                    link.dependsOnJobId,
                    link.requirement.name,
                    link.isSatisfied,
                    Timestamp(link.createdAt)
                )
            )
        }
    }

    override suspend fun getDependenciesForJob(jobId: String, tenantContext: TenantContext): List<JobDependencyLink> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM job_dependencies WHERE project_id = ? AND job_id = ?"
            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, jobId)) { rs ->
                mapRowToDependency(rs)
            }
        }
    }

    override suspend fun getDependentsOfJob(parentJobId: String, tenantContext: TenantContext): List<JobDependencyLink> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM job_dependencies WHERE project_id = ? AND depends_on_job_id = ?"
            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, parentJobId)) { rs ->
                mapRowToDependency(rs)
            }
        }
    }

    override suspend fun markDependencySatisfied(dependencyId: String, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE job_dependencies
                SET is_satisfied = TRUE
                WHERE project_id = ? AND dependency_id = ?
            """.trimIndent()
            txContext.sqlExecutor.executeUpdate(sql, listOf(tenantContext.projectId, dependencyId))
        }
    }
}
