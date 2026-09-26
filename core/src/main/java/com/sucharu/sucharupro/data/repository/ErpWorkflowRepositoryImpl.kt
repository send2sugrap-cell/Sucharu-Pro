package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.domain.model.erp.ErpActionLog
import com.sucharu.sucharupro.domain.model.erp.ErpWorkflowOrchestration
import com.sucharu.sucharupro.domain.repository.ErpWorkflowRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe Repository Implementation for Form 05 ERP Workflow Orchestration.
 */
class ErpWorkflowRepositoryImpl : ErpWorkflowRepository {
    private val orchestrationsStore = ConcurrentHashMap<String, ErpWorkflowOrchestration>()
    private val logsStore = ConcurrentHashMap<String, ErpActionLog>()

    override suspend fun saveOrchestration(orchestration: ErpWorkflowOrchestration): ErpWorkflowOrchestration {
        orchestrationsStore[orchestration.orchestrationId] = orchestration
        return orchestration
    }

    override suspend fun getOrchestrationById(orchestrationId: String): ErpWorkflowOrchestration? {
        return orchestrationsStore[orchestrationId]
    }

    override suspend fun getOrchestrationByOrder(orderId: String): ErpWorkflowOrchestration? {
        return orchestrationsStore.values.firstOrNull { it.orderId == orderId }
    }

    override suspend fun getAllOrchestrations(): List<ErpWorkflowOrchestration> {
        return orchestrationsStore.values.toList()
    }

    override suspend fun addActionLog(log: ErpActionLog): ErpActionLog {
        logsStore[log.logId] = log
        return log
    }
}
