package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.erp.ErpActionLog
import com.sucharu.sucharupro.domain.model.erp.ErpWorkflowOrchestration

/**
 * Domain Repository Interface for Form 05 — ERP Workflow Orchestration.
 */
interface ErpWorkflowRepository {
    suspend fun saveOrchestration(orchestration: ErpWorkflowOrchestration): ErpWorkflowOrchestration
    suspend fun getOrchestrationById(orchestrationId: String): ErpWorkflowOrchestration?
    suspend fun getOrchestrationByOrder(orderId: String): ErpWorkflowOrchestration?
    suspend fun getAllOrchestrations(): List<ErpWorkflowOrchestration>
    suspend fun addActionLog(log: ErpActionLog): ErpActionLog
}
