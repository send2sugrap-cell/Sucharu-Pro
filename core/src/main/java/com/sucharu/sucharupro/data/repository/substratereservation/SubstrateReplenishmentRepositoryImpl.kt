package com.sucharu.sucharupro.data.repository.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateReplenishmentDataSource
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateReplenishmentRepository

class SubstrateReplenishmentRepositoryImpl(
    private val dataSource: SubstrateReplenishmentDataSource
) : SubstrateReplenishmentRepository {

    override suspend fun saveEvaluation(evaluation: SubstrateReplenishmentEvaluation): SubstrateReplenishmentEvaluation {
        return dataSource.saveEvaluation(evaluation)
    }

    override suspend fun getEvaluationById(tenantId: String, evaluationId: String): SubstrateReplenishmentEvaluation? {
        return dataSource.findEvaluationById(tenantId, evaluationId)
    }

    override suspend fun getLatestEvaluationByFingerprint(tenantId: String, fingerprint: String): SubstrateReplenishmentEvaluation? {
        return dataSource.findLatestEvaluationByFingerprint(tenantId, fingerprint)
    }

    override suspend fun listEvaluationsBySku(tenantId: String, sku: String): List<SubstrateReplenishmentEvaluation> {
        return dataSource.listEvaluationsBySku(tenantId, sku)
    }

    override suspend fun listEvaluationsByState(tenantId: String, state: ReplenishmentTriggerState): List<SubstrateReplenishmentEvaluation> {
        return dataSource.listEvaluationsByState(tenantId, state)
    }

    override suspend fun listAllEvaluations(tenantId: String, limit: Int): List<SubstrateReplenishmentEvaluation> {
        return dataSource.listAllEvaluations(tenantId, limit)
    }

    override suspend fun updateEvaluationStatus(
        tenantId: String,
        evaluationId: String,
        newState: ReplenishmentTriggerState,
        actor: String
    ): Boolean {
        return dataSource.updateEvaluationStatus(tenantId, evaluationId, newState, actor)
    }

    override suspend fun saveSupplierAlert(alert: SupplierReorderAlert): SupplierReorderAlert {
        return dataSource.saveSupplierAlert(alert)
    }

    override suspend fun getAlertById(tenantId: String, alertId: String): SupplierReorderAlert? {
        return dataSource.findAlertById(tenantId, alertId)
    }

    override suspend fun listAlertsByEvaluation(tenantId: String, evaluationId: String): List<SupplierReorderAlert> {
        return dataSource.listAlertsByEvaluation(tenantId, evaluationId)
    }

    override suspend fun listAllAlerts(tenantId: String, limit: Int): List<SupplierReorderAlert> {
        return dataSource.listAllAlerts(tenantId, limit)
    }

    override suspend fun recordAuditEvent(event: SubstrateReplenishmentAuditEvent) {
        dataSource.saveAuditEvent(event)
    }

    override suspend fun listAuditEvents(tenantId: String, evaluationId: String): List<SubstrateReplenishmentAuditEvent> {
        return dataSource.listAuditEvents(tenantId, evaluationId)
    }
}
