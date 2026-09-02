package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.jobcosting.ProductionJobCostingDataSource
import com.sucharu.sucharupro.domain.model.jobcosting.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.sql.ResultSet

class PostgresProductionJobCostingDataSource(
    private val transactionManager: TransactionManager
) : ProductionJobCostingDataSource {

    override suspend fun saveActualJobCost(tenantId: String, costRecord: ProductionActualJobCostRecord) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_actual_job_cost_records (
                    cost_record_id, tenant_id, execution_job_id, order_id,
                    manufactured_good_quantity, total_material_cost, total_labor_cost,
                    total_machine_cost, total_quality_scrap_cost, total_rework_cost,
                    total_packaging_cost, total_overhead_allocated_cost, grand_total_actual_cost,
                    actual_unit_cost, material_breakdown_json, labor_breakdown_json,
                    machine_breakdown_json, scrap_rework_breakdown_json, packaging_breakdown_json,
                    cost_status, calculated_at, calculated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (cost_record_id) DO UPDATE SET
                    manufactured_good_quantity = EXCLUDED.manufactured_good_quantity,
                    total_material_cost = EXCLUDED.total_material_cost,
                    total_labor_cost = EXCLUDED.total_labor_cost,
                    total_machine_cost = EXCLUDED.total_machine_cost,
                    total_quality_scrap_cost = EXCLUDED.total_quality_scrap_cost,
                    total_rework_cost = EXCLUDED.total_rework_cost,
                    total_packaging_cost = EXCLUDED.total_packaging_cost,
                    total_overhead_allocated_cost = EXCLUDED.total_overhead_allocated_cost,
                    grand_total_actual_cost = EXCLUDED.grand_total_actual_cost,
                    actual_unit_cost = EXCLUDED.actual_unit_cost,
                    material_breakdown_json = EXCLUDED.material_breakdown_json,
                    labor_breakdown_json = EXCLUDED.labor_breakdown_json,
                    machine_breakdown_json = EXCLUDED.machine_breakdown_json,
                    scrap_rework_breakdown_json = EXCLUDED.scrap_rework_breakdown_json,
                    packaging_breakdown_json = EXCLUDED.packaging_breakdown_json,
                    cost_status = EXCLUDED.cost_status,
                    calculated_at = EXCLUDED.calculated_at,
                    calculated_by = EXCLUDED.calculated_by
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, costRecord.costRecordId)
                ps.setString(2, tenantId)
                ps.setString(3, costRecord.executionJobId)
                ps.setString(4, costRecord.orderId)
                ps.setBigDecimal(5, costRecord.manufacturedGoodQuantity)
                ps.setBigDecimal(6, costRecord.totalMaterialCost)
                ps.setBigDecimal(7, costRecord.totalLaborCost)
                ps.setBigDecimal(8, costRecord.totalMachineCost)
                ps.setBigDecimal(9, costRecord.totalQualityScrapCost)
                ps.setBigDecimal(10, costRecord.totalReworkCost)
                ps.setBigDecimal(11, costRecord.totalPackagingCost)
                ps.setBigDecimal(12, costRecord.totalOverheadAllocatedCost)
                ps.setBigDecimal(13, costRecord.grandTotalActualCost)
                ps.setBigDecimal(14, costRecord.actualUnitCost)
                ps.setString(15, serializeMaterialBreakdown(costRecord.materialBreakdown))
                ps.setString(16, serializeLaborBreakdown(costRecord.laborBreakdown))
                ps.setString(17, serializeMachineBreakdown(costRecord.machineBreakdown))
                ps.setString(18, serializeScrapReworkBreakdown(costRecord.scrapReworkBreakdown))
                ps.setString(19, serializePackagingBreakdown(costRecord.packagingBreakdown))
                ps.setString(20, costRecord.costStatus.name)
                ps.setLong(21, costRecord.calculatedAt)
                ps.setString(22, costRecord.calculatedBy)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getActualJobCost(tenantId: String, costRecordId: String): ProductionActualJobCostRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_actual_job_cost_records WHERE tenant_id = ? AND cost_record_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, costRecordId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapActualJobCost(rs) else null
                }
            }
        }
    }

    override suspend fun getActualJobCostByJob(tenantId: String, executionJobId: String): ProductionActualJobCostRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_actual_job_cost_records WHERE tenant_id = ? AND execution_job_id = ? ORDER BY calculated_at DESC LIMIT 1"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapActualJobCost(rs) else null
                }
            }
        }
    }

    override suspend fun saveVarianceSummary(tenantId: String, variance: ProductionJobCostVarianceSummary) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_job_cost_variance_records (
                    variance_record_id, tenant_id, execution_job_id, order_id,
                    order_quantity, actual_good_output_quantity, quoted_selling_price,
                    estimated_total_cost, actual_total_cost, total_cost_variance,
                    total_cost_variance_percentage, overall_cost_classification,
                    estimated_material_cost, actual_material_cost, material_variance,
                    material_variance_percentage, material_cost_classification,
                    estimated_labor_cost, actual_labor_cost, labor_variance,
                    labor_variance_percentage, labor_cost_classification,
                    estimated_machine_cost, actual_machine_cost, machine_variance,
                    machine_variance_percentage, machine_cost_classification,
                    total_quality_scrap_rework_cost, estimated_unit_cost, actual_unit_cost,
                    unit_cost_variance, estimated_gross_profit, actual_gross_profit,
                    gross_profit_variance, estimated_gross_margin_percentage,
                    actual_gross_margin_percentage, gross_margin_percentage_delta,
                    generated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (variance_record_id) DO UPDATE SET
                    actual_good_output_quantity = EXCLUDED.actual_good_output_quantity,
                    actual_total_cost = EXCLUDED.actual_total_cost,
                    total_cost_variance = EXCLUDED.total_cost_variance,
                    total_cost_variance_percentage = EXCLUDED.total_cost_variance_percentage,
                    overall_cost_classification = EXCLUDED.overall_cost_classification,
                    actual_material_cost = EXCLUDED.actual_material_cost,
                    material_variance = EXCLUDED.material_variance,
                    actual_labor_cost = EXCLUDED.actual_labor_cost,
                    labor_variance = EXCLUDED.labor_variance,
                    actual_machine_cost = EXCLUDED.actual_machine_cost,
                    machine_variance = EXCLUDED.machine_variance,
                    actual_unit_cost = EXCLUDED.actual_unit_cost,
                    unit_cost_variance = EXCLUDED.unit_cost_variance,
                    actual_gross_profit = EXCLUDED.actual_gross_profit,
                    gross_profit_variance = EXCLUDED.gross_profit_variance,
                    actual_gross_margin_percentage = EXCLUDED.actual_gross_margin_percentage,
                    gross_margin_percentage_delta = EXCLUDED.gross_margin_percentage_delta,
                    generated_at = EXCLUDED.generated_at
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, "VAR-" + variance.executionJobId)
                ps.setString(2, tenantId)
                ps.setString(3, variance.executionJobId)
                ps.setString(4, variance.orderId)
                ps.setBigDecimal(5, variance.orderQuantity)
                ps.setBigDecimal(6, variance.actualGoodOutputQuantity)
                ps.setBigDecimal(7, variance.quotedSellingPrice)
                ps.setBigDecimal(8, variance.estimatedTotalCost)
                ps.setBigDecimal(9, variance.actualTotalCost)
                ps.setBigDecimal(10, variance.totalCostVariance)
                ps.setBigDecimal(11, variance.totalCostVariancePercentage)
                ps.setString(12, variance.overallCostClassification.name)
                ps.setBigDecimal(13, variance.estimatedMaterialCost)
                ps.setBigDecimal(14, variance.actualMaterialCost)
                ps.setBigDecimal(15, variance.materialVariance)
                ps.setBigDecimal(16, variance.materialVariancePercentage)
                ps.setString(17, variance.materialCostClassification.name)
                ps.setBigDecimal(18, variance.estimatedLaborCost)
                ps.setBigDecimal(19, variance.actualLaborCost)
                ps.setBigDecimal(20, variance.laborVariance)
                ps.setBigDecimal(21, variance.laborVariancePercentage)
                ps.setString(22, variance.laborCostClassification.name)
                ps.setBigDecimal(23, variance.estimatedMachineCost)
                ps.setBigDecimal(24, variance.actualMachineCost)
                ps.setBigDecimal(25, variance.machineVariance)
                ps.setBigDecimal(26, variance.machineVariancePercentage)
                ps.setString(27, variance.machineCostClassification.name)
                ps.setBigDecimal(28, variance.totalQualityScrapReworkCost)
                ps.setBigDecimal(29, variance.estimatedUnitCost)
                ps.setBigDecimal(30, variance.actualUnitCost)
                ps.setBigDecimal(31, variance.unitCostVariance)
                ps.setBigDecimal(32, variance.estimatedGrossProfit)
                ps.setBigDecimal(33, variance.actualGrossProfit)
                ps.setBigDecimal(34, variance.grossProfitVariance)
                ps.setBigDecimal(35, variance.estimatedGrossMarginPercentage)
                ps.setBigDecimal(36, variance.actualGrossMarginPercentage)
                ps.setBigDecimal(37, variance.grossMarginPercentageDelta)
                ps.setLong(38, variance.generatedAt)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getVarianceSummaryByJob(tenantId: String, executionJobId: String): ProductionJobCostVarianceSummary? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_job_cost_variance_records WHERE tenant_id = ? AND execution_job_id = ? ORDER BY generated_at DESC LIMIT 1"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapVarianceSummary(rs) else null
                }
            }
        }
    }

    override suspend fun saveReconciliationResult(tenantId: String, reconciliation: ProductionJobCostingReconciliationResult) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_job_cost_reconciliation_records (
                    reconciliation_id, tenant_id, execution_job_id,
                    bom_quantities_reconciled, labor_hours_reconciled, machine_hours_reconciled,
                    scrap_rework_valuation_consistent, packaging_cost_balanced, actual_cost_math_balanced,
                    variance_integrity_hash_valid, multi_tenant_isolation_verified, is_fully_reconciled,
                    certificate_hash, discrepancies_json, reconciled_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (reconciliation_id) DO UPDATE SET
                    bom_quantities_reconciled = EXCLUDED.bom_quantities_reconciled,
                    labor_hours_reconciled = EXCLUDED.labor_hours_reconciled,
                    machine_hours_reconciled = EXCLUDED.machine_hours_reconciled,
                    scrap_rework_valuation_consistent = EXCLUDED.scrap_rework_valuation_consistent,
                    packaging_cost_balanced = EXCLUDED.packaging_cost_balanced,
                    actual_cost_math_balanced = EXCLUDED.actual_cost_math_balanced,
                    variance_integrity_hash_valid = EXCLUDED.variance_integrity_hash_valid,
                    multi_tenant_isolation_verified = EXCLUDED.multi_tenant_isolation_verified,
                    is_fully_reconciled = EXCLUDED.is_fully_reconciled,
                    certificate_hash = EXCLUDED.certificate_hash,
                    discrepancies_json = EXCLUDED.discrepancies_json,
                    reconciled_at = EXCLUDED.reconciled_at
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, "REC-" + reconciliation.executionJobId)
                ps.setString(2, tenantId)
                ps.setString(3, reconciliation.executionJobId)
                ps.setBoolean(4, reconciliation.bomQuantitiesReconciled)
                ps.setBoolean(5, reconciliation.laborHoursReconciled)
                ps.setBoolean(6, reconciliation.machineHoursReconciled)
                ps.setBoolean(7, reconciliation.scrapReworkValuationConsistent)
                ps.setBoolean(8, reconciliation.packagingCostBalanced)
                ps.setBoolean(9, reconciliation.actualCostMathBalanced)
                ps.setBoolean(10, reconciliation.varianceIntegrityHashValid)
                ps.setBoolean(11, reconciliation.multiTenantIsolationVerified)
                ps.setBoolean(12, reconciliation.isFullyReconciled)
                ps.setString(13, reconciliation.certificateHash)
                ps.setString(14, reconciliation.discrepancies.joinToString("||"))
                ps.setLong(15, reconciliation.reconciledAt)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getReconciliationResultByJob(tenantId: String, executionJobId: String): ProductionJobCostingReconciliationResult? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_job_cost_reconciliation_records WHERE tenant_id = ? AND execution_job_id = ? ORDER BY reconciled_at DESC LIMIT 1"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapReconciliationResult(rs) else null
                }
            }
        }
    }

    override suspend fun saveEvent(tenantId: String, event: ProductionJobCostingEvent) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_job_costing_audit_events (
                    event_id, tenant_id, execution_job_id, event_type, actor, payload, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, tenantId)
                ps.setString(3, event.executionJobId)
                ps.setString(4, event.eventType)
                ps.setString(5, event.actor)
                ps.setString(6, event.payload)
                ps.setLong(7, event.timestamp)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<ProductionJobCostingEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_job_costing_audit_events WHERE tenant_id = ? AND execution_job_id = ? ORDER BY timestamp ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionJobCostingEvent>()
                    while (rs.next()) {
                        list.add(
                            ProductionJobCostingEvent(
                                eventId = rs.getString("event_id"),
                                tenantId = rs.getString("tenant_id"),
                                executionJobId = rs.getString("execution_job_id"),
                                eventType = rs.getString("event_type"),
                                actor = rs.getString("actor"),
                                payload = rs.getString("payload"),
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    private fun mapActualJobCost(rs: ResultSet): ProductionActualJobCostRecord {
        return ProductionActualJobCostRecord(
            costRecordId = rs.getString("cost_record_id"),
            tenantId = rs.getString("tenant_id"),
            executionJobId = rs.getString("execution_job_id"),
            orderId = rs.getString("order_id"),
            manufacturedGoodQuantity = rs.getBigDecimal("manufactured_good_quantity"),
            totalMaterialCost = rs.getBigDecimal("total_material_cost"),
            totalLaborCost = rs.getBigDecimal("total_labor_cost"),
            totalMachineCost = rs.getBigDecimal("total_machine_cost"),
            totalQualityScrapCost = rs.getBigDecimal("total_quality_scrap_cost"),
            totalReworkCost = rs.getBigDecimal("total_rework_cost"),
            totalPackagingCost = rs.getBigDecimal("total_packaging_cost"),
            totalOverheadAllocatedCost = rs.getBigDecimal("total_overhead_allocated_cost"),
            grandTotalActualCost = rs.getBigDecimal("grand_total_actual_cost"),
            actualUnitCost = rs.getBigDecimal("actual_unit_cost"),
            materialBreakdown = deserializeMaterialBreakdown(rs.getString("material_breakdown_json")),
            laborBreakdown = deserializeLaborBreakdown(rs.getString("labor_breakdown_json")),
            machineBreakdown = deserializeMachineBreakdown(rs.getString("machine_breakdown_json")),
            scrapReworkBreakdown = deserializeScrapReworkBreakdown(rs.getString("scrap_rework_breakdown_json")),
            packagingBreakdown = deserializePackagingBreakdown(rs.getString("packaging_breakdown_json")),
            costStatus = JobCostStatus.valueOf(rs.getString("cost_status")),
            calculatedAt = rs.getLong("calculated_at"),
            calculatedBy = rs.getString("calculated_by")
        )
    }

    private fun mapVarianceSummary(rs: ResultSet): ProductionJobCostVarianceSummary {
        return ProductionJobCostVarianceSummary(
            executionJobId = rs.getString("execution_job_id"),
            tenantId = rs.getString("tenant_id"),
            orderId = rs.getString("order_id"),
            orderQuantity = rs.getBigDecimal("order_quantity"),
            actualGoodOutputQuantity = rs.getBigDecimal("actual_good_output_quantity"),
            quotedSellingPrice = rs.getBigDecimal("quoted_selling_price"),
            estimatedTotalCost = rs.getBigDecimal("estimated_total_cost"),
            actualTotalCost = rs.getBigDecimal("actual_total_cost"),
            totalCostVariance = rs.getBigDecimal("total_cost_variance"),
            totalCostVariancePercentage = rs.getBigDecimal("total_cost_variance_percentage"),
            overallCostClassification = VarianceClassification.valueOf(rs.getString("overall_cost_classification")),
            estimatedMaterialCost = rs.getBigDecimal("estimated_material_cost"),
            actualMaterialCost = rs.getBigDecimal("actual_material_cost"),
            materialVariance = rs.getBigDecimal("material_variance"),
            materialVariancePercentage = rs.getBigDecimal("material_variance_percentage"),
            materialCostClassification = VarianceClassification.valueOf(rs.getString("material_cost_classification")),
            estimatedLaborCost = rs.getBigDecimal("estimated_labor_cost"),
            actualLaborCost = rs.getBigDecimal("actual_labor_cost"),
            laborVariance = rs.getBigDecimal("labor_variance"),
            laborVariancePercentage = rs.getBigDecimal("labor_variance_percentage"),
            laborCostClassification = VarianceClassification.valueOf(rs.getString("labor_cost_classification")),
            estimatedMachineCost = rs.getBigDecimal("estimated_machine_cost"),
            actualMachineCost = rs.getBigDecimal("actual_machine_cost"),
            machineVariance = rs.getBigDecimal("machine_variance"),
            machineVariancePercentage = rs.getBigDecimal("machine_variance_percentage"),
            machineCostClassification = VarianceClassification.valueOf(rs.getString("machine_cost_classification")),
            totalQualityScrapReworkCost = rs.getBigDecimal("total_quality_scrap_rework_cost"),
            estimatedUnitCost = rs.getBigDecimal("estimated_unit_cost"),
            actualUnitCost = rs.getBigDecimal("actual_unit_cost"),
            unitCostVariance = rs.getBigDecimal("unit_cost_variance"),
            estimatedGrossProfit = rs.getBigDecimal("estimated_gross_profit"),
            actualGrossProfit = rs.getBigDecimal("actual_gross_profit"),
            grossProfitVariance = rs.getBigDecimal("gross_profit_variance"),
            estimatedGrossMarginPercentage = rs.getBigDecimal("estimated_gross_margin_percentage"),
            actualGrossMarginPercentage = rs.getBigDecimal("actual_gross_margin_percentage"),
            grossMarginPercentageDelta = rs.getBigDecimal("gross_margin_percentage_delta"),
            generatedAt = rs.getLong("generated_at")
        )
    }

    private fun mapReconciliationResult(rs: ResultSet): ProductionJobCostingReconciliationResult {
        val discStr = rs.getString("discrepancies_json") ?: ""
        val discList = if (discStr.isNotBlank()) discStr.split("||") else emptyList()
        return ProductionJobCostingReconciliationResult(
            executionJobId = rs.getString("execution_job_id"),
            tenantId = rs.getString("tenant_id"),
            bomQuantitiesReconciled = rs.getBoolean("bom_quantities_reconciled"),
            laborHoursReconciled = rs.getBoolean("labor_hours_reconciled"),
            machineHoursReconciled = rs.getBoolean("machine_hours_reconciled"),
            scrapReworkValuationConsistent = rs.getBoolean("scrap_rework_valuation_consistent"),
            packagingCostBalanced = rs.getBoolean("packaging_cost_balanced"),
            actualCostMathBalanced = rs.getBoolean("actual_cost_math_balanced"),
            varianceIntegrityHashValid = rs.getBoolean("variance_integrity_hash_valid"),
            multiTenantIsolationVerified = rs.getBoolean("multi_tenant_isolation_verified"),
            isFullyReconciled = rs.getBoolean("is_fully_reconciled"),
            certificateHash = rs.getString("certificate_hash"),
            discrepancies = discList,
            reconciledAt = rs.getLong("reconciled_at")
        )
    }

    private fun serializeMaterialBreakdown(items: List<ActualMaterialCostItem>): String {
        return items.joinToString(";;") {
            "${it.materialCode}::${it.materialName}::${it.unitOfMeasure}::${it.plannedQuantity}::${it.actualQuantity}::${it.quantityVariance}::${it.standardUnitPrice}::${it.actualUnitPrice}::${it.priceVariance}::${it.plannedCost}::${it.actualCost}::${it.totalVariance}::${it.varianceClassification.name}::${it.batchLotNumber ?: ""}"
        }
    }

    private fun deserializeMaterialBreakdown(str: String?): List<ActualMaterialCostItem> {
        if (str.isNullOrBlank()) return emptyList()
        return str.split(";;").filter { it.isNotBlank() }.mapNotNull {
            val p = it.split("::")
            if (p.size >= 13) {
                ActualMaterialCostItem(
                    materialCode = p[0],
                    materialName = p[1],
                    unitOfMeasure = p[2],
                    plannedQuantity = p[3].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    actualQuantity = p[4].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    quantityVariance = p[5].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    standardUnitPrice = p[6].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    actualUnitPrice = p[7].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    priceVariance = p[8].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    plannedCost = p[9].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    actualCost = p[10].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    totalVariance = p[11].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    varianceClassification = VarianceClassification.valueOf(p[12]),
                    batchLotNumber = if (p.size > 13 && p[13].isNotBlank()) p[13] else null
                )
            } else null
        }
    }

    private fun serializeLaborBreakdown(items: List<ActualLaborCostItem>): String {
        return items.joinToString(";;") {
            "${it.stageType.name}::${it.stageName}::${it.plannedSetupHours}::${it.actualSetupHours}::${it.plannedRunHours}::${it.actualRunHours}::${it.standardHourlyRate}::${it.actualHourlyRate}::${it.plannedLaborCost}::${it.actualLaborCost}::${it.efficiencyVariance}::${it.rateVariance}::${it.totalVariance}::${it.varianceClassification.name}"
        }
    }

    private fun deserializeLaborBreakdown(str: String?): List<ActualLaborCostItem> {
        if (str.isNullOrBlank()) return emptyList()
        return str.split(";;").filter { it.isNotBlank() }.mapNotNull {
            val p = it.split("::")
            if (p.size >= 14) {
                ActualLaborCostItem(
                    stageType = ProductionStageType.valueOf(p[0]),
                    stageName = p[1],
                    plannedSetupHours = p[2].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    actualSetupHours = p[3].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    plannedRunHours = p[4].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    actualRunHours = p[5].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    standardHourlyRate = p[6].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    actualHourlyRate = p[7].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    plannedLaborCost = p[8].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    actualLaborCost = p[9].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    efficiencyVariance = p[10].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    rateVariance = p[11].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    totalVariance = p[12].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    varianceClassification = VarianceClassification.valueOf(p[13])
                )
            } else null
        }
    }

    private fun serializeMachineBreakdown(items: List<ActualMachineCostItem>): String {
        return items.joinToString(";;") {
            "${it.machineId}::${it.machineName}::${it.stageType.name}::${it.plannedMachineHours}::${it.actualMachineHours}::${it.recordedDowntimeHours}::${it.machineHourlyRate}::${it.plannedMachineCost}::${it.actualMachineCost}::${it.downtimeCostImpact}::${it.utilizationVariance}::${it.varianceClassification.name}"
        }
    }

    private fun deserializeMachineBreakdown(str: String?): List<ActualMachineCostItem> {
        if (str.isNullOrBlank()) return emptyList()
        return str.split(";;").filter { it.isNotBlank() }.mapNotNull {
            val p = it.split("::")
            if (p.size >= 12) {
                ActualMachineCostItem(
                    machineId = p[0],
                    machineName = p[1],
                    stageType = ProductionStageType.valueOf(p[2]),
                    plannedMachineHours = p[3].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    actualMachineHours = p[4].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    recordedDowntimeHours = p[5].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    machineHourlyRate = p[6].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    plannedMachineCost = p[7].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    actualMachineCost = p[8].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    downtimeCostImpact = p[9].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    utilizationVariance = p[10].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    varianceClassification = VarianceClassification.valueOf(p[11])
                )
            } else null
        }
    }

    private fun serializeScrapReworkBreakdown(items: List<ScrapReworkValuationItem>): String {
        return items.joinToString(";;") {
            "${it.defectRecordId}::${it.stageType.name}::${it.defectType}::${it.scrappedQuantity}::${it.unitMaterialCost}::${it.scrapMaterialLoss}::${it.reworkWorkOrderId ?: ""}::${it.reworkAdditionalHours}::${it.reworkHourlyRate}::${it.reworkLaborCost}::${it.scrapSalvageRecoveryValue}::${it.netQualityCost}"
        }
    }

    private fun deserializeScrapReworkBreakdown(str: String?): List<ScrapReworkValuationItem> {
        if (str.isNullOrBlank()) return emptyList()
        return str.split(";;").filter { it.isNotBlank() }.mapNotNull {
            val p = it.split("::")
            if (p.size >= 12) {
                ScrapReworkValuationItem(
                    defectRecordId = p[0],
                    stageType = ProductionStageType.valueOf(p[1]),
                    defectType = p[2],
                    scrappedQuantity = p[3].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    unitMaterialCost = p[4].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    scrapMaterialLoss = p[5].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    reworkWorkOrderId = if (p[6].isNotBlank()) p[6] else null,
                    reworkAdditionalHours = p[7].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    reworkHourlyRate = p[8].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    reworkLaborCost = p[9].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    scrapSalvageRecoveryValue = p[10].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    netQualityCost = p[11].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                )
            } else null
        }
    }

    private fun serializePackagingBreakdown(items: List<ActualPackagingCostItem>): String {
        return items.joinToString(";;") {
            "${it.packagingRecordId}::${it.packagingType}::${it.cartonCount}::${it.unitsPerCarton}::${it.totalPackagedUnits}::${it.standardUnitPackagingCost}::${it.actualTotalPackagingCost}"
        }
    }

    private fun deserializePackagingBreakdown(str: String?): List<ActualPackagingCostItem> {
        if (str.isNullOrBlank()) return emptyList()
        return str.split(";;").filter { it.isNotBlank() }.mapNotNull {
            val p = it.split("::")
            if (p.size >= 7) {
                ActualPackagingCostItem(
                    packagingRecordId = p[0],
                    packagingType = p[1],
                    cartonCount = p[2].toIntOrNull() ?: 0,
                    unitsPerCarton = p[3].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    totalPackagedUnits = p[4].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    standardUnitPackagingCost = p[5].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO,
                    actualTotalPackagingCost = p[6].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                )
            } else null
        }
    }
}
