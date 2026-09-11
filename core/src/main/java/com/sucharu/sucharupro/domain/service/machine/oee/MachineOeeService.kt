package com.sucharu.sucharupro.domain.service.machine.oee

import com.sucharu.sucharupro.domain.machine.oee.MachineOeeCalculationRequest
import com.sucharu.sucharupro.domain.machine.oee.MachineOeeMetrics
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Service interface for Machine Performance & OEE calculations.
 */
interface MachineOeeService {
    suspend fun calculateAndSaveOee(request: MachineOeeCalculationRequest, actorId: String? = null): DomainResult<MachineOeeMetrics>
    suspend fun getLatestOeeSummary(tenantId: String, machineId: String): DomainResult<MachineOeeMetrics?>
    suspend fun listOeeHistory(tenantId: String, machineId: String, limit: Int = 100): DomainResult<List<MachineOeeMetrics>>
}
