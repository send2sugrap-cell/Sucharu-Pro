package com.sucharu.sucharupro.data.repository.businesscostcontrol

import com.sucharu.sucharupro.data.datasource.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.businesscostcontrol.*

class BusinessCostControlRepositoryImpl(
    private val dataSource: BusinessCostControlDataSource
) : BusinessCostControlRepository {

    override suspend fun createFinancialPeriod(period: BusinessFinancialPeriod): BusinessFinancialPeriod {
        return dataSource.createFinancialPeriod(period)
    }

    override suspend fun findFinancialPeriodById(id: String, tenantId: String, projectId: String): BusinessFinancialPeriod? {
        return dataSource.findFinancialPeriodById(id, tenantId, projectId)
    }

    override suspend fun findFinancialPeriodByCode(periodCode: String, tenantId: String, projectId: String): BusinessFinancialPeriod? {
        return dataSource.findFinancialPeriodByCode(periodCode, tenantId, projectId)
    }

    override suspend fun updateFinancialPeriod(period: BusinessFinancialPeriod): BusinessFinancialPeriod {
        return dataSource.updateFinancialPeriod(period)
    }

    override suspend fun listFinancialPeriods(tenantId: String, projectId: String, filter: BusinessFinancialPeriodFilter): List<BusinessFinancialPeriod> {
        return dataSource.listFinancialPeriods(tenantId, projectId, filter)
    }

    override suspend fun createCommitment(commitment: BusinessCostCommitment): BusinessCostCommitment {
        return dataSource.createCommitment(commitment)
    }

    override suspend fun findCommitmentById(id: String, tenantId: String, projectId: String): BusinessCostCommitment? {
        return dataSource.findCommitmentById(id, tenantId, projectId)
    }

    override suspend fun findCommitmentByNumber(commitmentNumber: String, tenantId: String, projectId: String): BusinessCostCommitment? {
        return dataSource.findCommitmentByNumber(commitmentNumber, tenantId, projectId)
    }

    override suspend fun updateCommitment(commitment: BusinessCostCommitment): BusinessCostCommitment {
        return dataSource.updateCommitment(commitment)
    }

    override suspend fun listCommitments(tenantId: String, projectId: String, filter: BusinessCostCommitmentFilter): List<BusinessCostCommitment> {
        return dataSource.listCommitments(tenantId, projectId, filter)
    }

    override suspend fun recordConsumption(consumption: BusinessCostCommitmentConsumption): BusinessCostCommitmentConsumption {
        return dataSource.recordConsumption(consumption)
    }

    override suspend fun listConsumptions(tenantId: String, projectId: String, commitmentId: String): List<BusinessCostCommitmentConsumption> {
        return dataSource.listConsumptions(tenantId, projectId, commitmentId)
    }

    override suspend fun createAccrual(accrual: BusinessCostAccrual): BusinessCostAccrual {
        return dataSource.createAccrual(accrual)
    }

    override suspend fun findAccrualById(id: String, tenantId: String, projectId: String): BusinessCostAccrual? {
        return dataSource.findAccrualById(id, tenantId, projectId)
    }

    override suspend fun findAccrualByNumber(accrualNumber: String, tenantId: String, projectId: String): BusinessCostAccrual? {
        return dataSource.findAccrualByNumber(accrualNumber, tenantId, projectId)
    }

    override suspend fun updateAccrual(accrual: BusinessCostAccrual): BusinessCostAccrual {
        return dataSource.updateAccrual(accrual)
    }

    override suspend fun listAccruals(tenantId: String, projectId: String, filter: BusinessCostAccrualFilter): List<BusinessCostAccrual> {
        return dataSource.listAccruals(tenantId, projectId, filter)
    }

    override suspend fun recordReversal(reversal: BusinessCostAccrualReversal): BusinessCostAccrualReversal {
        return dataSource.recordReversal(reversal)
    }

    override suspend fun listReversals(tenantId: String, projectId: String, accrualId: String): List<BusinessCostAccrualReversal> {
        return dataSource.listReversals(tenantId, projectId, accrualId)
    }

    override suspend fun recordAuditEvent(event: BusinessCostControlAuditEvent): BusinessCostControlAuditEvent {
        return dataSource.recordAuditEvent(event)
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String?, entityType: String?): List<BusinessCostControlAuditEvent> {
        return dataSource.listAuditEvents(tenantId, projectId, entityId, entityType)
    }
}
