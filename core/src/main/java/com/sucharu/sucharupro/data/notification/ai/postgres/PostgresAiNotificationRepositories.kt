package com.sucharu.sucharupro.data.notification.ai.postgres

import com.sucharu.sucharupro.data.notification.ai.*
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.notification.ai.*
import java.sql.Timestamp

/**
 * PostgreSQL persistent implementation for AI Notification Confirmation records (INFRA-04 Step 08).
 */
class PostgresAiNotificationConfirmationRepository(
    private val transactionManager: TransactionManager
) : AiNotificationConfirmationRepository {

    override suspend fun saveConfirmation(
        request: AiNotificationConfirmationRequest,
        tenantContext: TenantContext
    ) = transactionManager.inTransaction(tenantContext) { tx ->
        val sql = """
            INSERT INTO ai_notification_confirmations (
                confirmation_id, project_id, action_type, requested_by_agent_id,
                payload_summary, target_recipient_id, status, approved_by_human_id,
                approver_role, approved_at, rejection_reason, created_at, expires_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (project_id, confirmation_id) DO NOTHING
        """.trimIndent()

        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, request.confirmationId)
            stmt.setString(2, request.projectId)
            stmt.setString(3, request.actionType.name)
            stmt.setString(4, request.requestedByAgentId)
            stmt.setString(5, request.payloadSummary)
            stmt.setString(6, request.targetRecipientId)
            stmt.setString(7, request.status.name)
            stmt.setString(8, request.approvedByHumanId)
            stmt.setString(9, request.approverRole)
            stmt.setObject(10, request.approvedAt?.let { Timestamp(it) })
            stmt.setString(11, request.rejectionReason)
            stmt.setTimestamp(12, Timestamp(request.createdAt))
            stmt.setTimestamp(13, Timestamp(request.expiresAt))
            stmt.executeUpdate()
        }
        Unit
    }

    override suspend fun getConfirmation(
        confirmationId: String,
        tenantContext: TenantContext
    ): AiNotificationConfirmationRequest? = transactionManager.inReadOnly(tenantContext) { tx ->
        val sql = """
            SELECT confirmation_id, project_id, action_type, requested_by_agent_id,
                   payload_summary, target_recipient_id, status, approved_by_human_id,
                   approver_role, approved_at, rejection_reason, created_at, expires_at
            FROM ai_notification_confirmations
            WHERE project_id = ? AND confirmation_id = ?
        """.trimIndent()

        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, tenantContext.projectId)
            stmt.setString(2, confirmationId)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                AiNotificationConfirmationRequest(
                    confirmationId = rs.getString("confirmation_id"),
                    projectId = rs.getString("project_id"),
                    actionType = AiNotificationActionType.valueOf(rs.getString("action_type")),
                    requestedByAgentId = rs.getString("requested_by_agent_id"),
                    payloadSummary = rs.getString("payload_summary"),
                    targetRecipientId = rs.getString("target_recipient_id"),
                    status = AiConfirmationStatus.valueOf(rs.getString("status")),
                    approvedByHumanId = rs.getString("approved_by_human_id"),
                    approverRole = rs.getString("approver_role"),
                    approvedAt = rs.getTimestamp("approved_at")?.time,
                    rejectionReason = rs.getString("rejection_reason"),
                    createdAt = rs.getTimestamp("created_at").time,
                    expiresAt = rs.getTimestamp("expires_at").time
                )
            } else null
        }
    }

    override suspend fun updateConfirmationStatus(
        confirmationId: String,
        status: AiConfirmationStatus,
        approverId: String?,
        approverRole: String?,
        rejectionReason: String?,
        tenantContext: TenantContext
    ): Boolean = transactionManager.inTransaction(tenantContext) { tx ->
        val sql = """
            UPDATE ai_notification_confirmations
            SET status = ?, approved_by_human_id = ?, approver_role = ?,
                approved_at = ?, rejection_reason = ?
            WHERE project_id = ? AND confirmation_id = ? AND status = 'PENDING'
        """.trimIndent()

        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, status.name)
            stmt.setString(2, approverId)
            stmt.setString(3, approverRole)
            stmt.setObject(4, if (status == AiConfirmationStatus.APPROVED) Timestamp(System.currentTimeMillis()) else null)
            stmt.setString(5, rejectionReason)
            stmt.setString(6, tenantContext.projectId)
            stmt.setString(7, confirmationId)
            stmt.executeUpdate() > 0
        }
    }

    override suspend fun listPendingConfirmations(
        tenantContext: TenantContext
    ): List<AiNotificationConfirmationRequest> = transactionManager.inReadOnly(tenantContext) { tx ->
        val sql = """
            SELECT confirmation_id, project_id, action_type, requested_by_agent_id,
                   payload_summary, target_recipient_id, status, approved_by_human_id,
                   approver_role, approved_at, rejection_reason, created_at, expires_at
            FROM ai_notification_confirmations
            WHERE project_id = ? AND status = 'PENDING' AND expires_at > NOW()
            ORDER BY created_at ASC
        """.trimIndent()

        val list = mutableListOf<AiNotificationConfirmationRequest>()
        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, tenantContext.projectId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(
                    AiNotificationConfirmationRequest(
                        confirmationId = rs.getString("confirmation_id"),
                        projectId = rs.getString("project_id"),
                        actionType = AiNotificationActionType.valueOf(rs.getString("action_type")),
                        requestedByAgentId = rs.getString("requested_by_agent_id"),
                        payloadSummary = rs.getString("payload_summary"),
                        targetRecipientId = rs.getString("target_recipient_id"),
                        status = AiConfirmationStatus.valueOf(rs.getString("status")),
                        approvedByHumanId = rs.getString("approved_by_human_id"),
                        approverRole = rs.getString("approver_role"),
                        approvedAt = rs.getTimestamp("approved_at")?.time,
                        rejectionReason = rs.getString("rejection_reason"),
                        createdAt = rs.getTimestamp("created_at").time,
                        expiresAt = rs.getTimestamp("expires_at").time
                    )
                )
            }
        }
        list
    }
}

/**
 * PostgreSQL persistent implementation for AI Notification Action Records (INFRA-04 Step 08).
 */
class PostgresAiNotificationActionRecordRepository(
    private val transactionManager: TransactionManager
) : AiNotificationActionRecordRepository {

    override suspend fun getActionRecord(
        projectId: String,
        agentId: String,
        actionType: String,
        idempotencyKey: String,
        tenantContext: TenantContext
    ): AiNotificationActionRecord? = transactionManager.inReadOnly(tenantContext) { tx ->
        val sql = """
            SELECT action_id, project_id, agent_id, action_type, idempotency_key,
                   status, response_summary, correlation_id, executed_at
            FROM ai_notification_action_records
            WHERE project_id = ? AND agent_id = ? AND action_type = ? AND idempotency_key = ?
        """.trimIndent()

        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, tenantContext.projectId)
            stmt.setString(2, agentId)
            stmt.setString(3, actionType)
            stmt.setString(4, idempotencyKey)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                AiNotificationActionRecord(
                    actionId = rs.getString("action_id"),
                    projectId = rs.getString("project_id"),
                    agentId = rs.getString("agent_id"),
                    actionType = rs.getString("action_type"),
                    idempotencyKey = rs.getString("idempotency_key"),
                    status = rs.getString("status"),
                    responseSummary = rs.getString("response_summary"),
                    correlationId = rs.getString("correlation_id"),
                    executedAt = rs.getTimestamp("executed_at").time
                )
            } else null
        }
    }

    override suspend fun saveActionRecord(
        record: AiNotificationActionRecord,
        tenantContext: TenantContext
    ) = transactionManager.inTransaction(tenantContext) { tx ->
        val sql = """
            INSERT INTO ai_notification_action_records (
                action_id, project_id, agent_id, action_type, idempotency_key,
                status, response_summary, correlation_id, executed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (project_id, agent_id, action_type, idempotency_key) DO NOTHING
        """.trimIndent()

        tx.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, record.actionId)
            stmt.setString(2, record.projectId)
            stmt.setString(3, record.agentId)
            stmt.setString(4, record.actionType)
            stmt.setString(5, record.idempotencyKey)
            stmt.setString(6, record.status)
            stmt.setString(7, record.responseSummary)
            stmt.setString(8, record.correlationId)
            stmt.setTimestamp(9, Timestamp(record.executedAt))
            stmt.executeUpdate()
        }
        Unit
    }
}
