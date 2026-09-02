package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.printingcalculator.PrintingCalculatorDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import com.sucharu.sucharupro.domain.model.product.ProductType
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of PrintingCalculatorDataSource with TransactionManager & RLS.
 * Module 17 Step 01.
 */
class PostgresPrintingCalculatorDataSource(
    private val transactionManager: TransactionManager
) : PrintingCalculatorDataSource {

    override suspend fun saveCalculation(result: PrintingCalculationResult): DomainResult<PrintingCalculationResult> {
        return try {
            transactionManager.inTransaction(TenantContext(result.projectId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO printing_calculations (
                        calculation_id, tenant_id, project_id, request_fingerprint,
                        job_title, product_type, quantity, quantity_unit,
                        finished_width_mm, finished_height_mm,
                        material_name, stock_type, gsm,
                        sheet_width_mm, sheet_height_mm, material_unit_price,
                        process_type, sides, color_mode,
                        front_colors, back_colors, spot_colors,
                        setup_sheets, running_waste_pct, finishing_waste_pct,
                        productive_sheets, waste_sheets, total_sheets, total_reams, total_weight_kg,
                        total_impressions, plate_count,
                        material_cost, printing_cost, plate_cost, finishing_cost,
                        total_estimated_cost, estimated_unit_cost, currency,
                        calculation_status, classification,
                        diagnostics_json, breakdown_json,
                        requested_at, calculated_at, integrity_hash, calculation_version
                    ) VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                    )
                    ON CONFLICT (calculation_id) DO UPDATE SET
                        calculated_at = EXCLUDED.calculated_at,
                        total_estimated_cost = EXCLUDED.total_estimated_cost,
                        estimated_unit_cost = EXCLUDED.estimated_unit_cost,
                        calculation_status = EXCLUDED.calculation_status,
                        integrity_hash = EXCLUDED.integrity_hash
                """.trimIndent()

                conn.prepareStatement(sql).use { stmt ->
                    val spec = result.normalizedSpecification
                    val mat = result.materialRequirement
                    val prt = result.printingRequirement
                    val fin = result.finishingRequirement

                    stmt.setString(1, result.calculationId)
                    stmt.setString(2, result.tenantId)
                    stmt.setString(3, result.projectId)
                    stmt.setString(4, result.requestFingerprint)
                    stmt.setString(5, spec.jobTitle)
                    stmt.setString(6, spec.productType.name)
                    stmt.setLong(7, spec.quantity.normalizedQuantity)
                    stmt.setString(8, spec.quantity.unit.name)
                    stmt.setBigDecimal(9, spec.normalizedDimensionMm.width)
                    stmt.setBigDecimal(10, spec.normalizedDimensionMm.height)
                    stmt.setString(11, spec.material.materialName)
                    stmt.setString(12, spec.material.stockType.name)
                    stmt.setBigDecimal(13, spec.material.gsm)
                    stmt.setBigDecimal(14, spec.material.sheetDimension?.width)
                    stmt.setBigDecimal(15, spec.material.sheetDimension?.height)
                    stmt.setBigDecimal(16, spec.material.unitPricePerSheet)
                    stmt.setString(17, spec.processType.name)
                    stmt.setString(18, spec.sides.name)
                    stmt.setString(19, spec.color.colorMode.name)
                    stmt.setInt(20, spec.color.frontColorsCount)
                    stmt.setInt(21, spec.color.backColorsCount)
                    stmt.setInt(22, spec.color.spotColorsCount)
                    stmt.setLong(23, spec.waste.setupSheets)
                    stmt.setBigDecimal(24, spec.waste.runningWastePercentage)
                    stmt.setBigDecimal(25, spec.waste.finishingWastePercentage)
                    stmt.setLong(26, mat.productiveSheetsRequired)
                    stmt.setLong(27, mat.wasteSheetsRequired)
                    stmt.setLong(28, mat.totalSheetsRequired)
                    stmt.setBigDecimal(29, mat.totalReamsRequired)
                    stmt.setBigDecimal(30, mat.totalWeightKg)
                    stmt.setLong(31, prt.totalImpressions)
                    stmt.setInt(32, prt.plateCount)
                    stmt.setBigDecimal(33, mat.estimatedMaterialCost)
                    stmt.setBigDecimal(34, prt.estimatedPrintingCost)
                    stmt.setBigDecimal(35, prt.estimatedPlateCost)
                    stmt.setBigDecimal(36, fin.totalEstimatedFinishingCost)
                    stmt.setBigDecimal(37, result.totalEstimatedCost)
                    stmt.setBigDecimal(38, result.estimatedUnitCost)
                    stmt.setString(39, result.currency)
                    stmt.setString(40, result.status.name)
                    stmt.setString(41, result.classification.name)
                    stmt.setString(42, "[]") // diagnostics json placeholder
                    stmt.setString(43, "[]") // breakdown json placeholder
                    stmt.setLong(44, result.requestedAt)
                    stmt.setLong(45, result.calculatedAt)
                    stmt.setString(46, result.integrityHash)
                    stmt.setString(47, result.calculationVersion)

                    stmt.executeUpdate()
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to persist printing calculation: ${e.message}")
        }
    }

    override suspend fun findCalculationById(tenantId: String, calculationId: String): DomainResult<PrintingCalculationResult?> {
        return try {
            val calc = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM printing_calculations WHERE calculation_id = ? AND tenant_id = ?"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, calculationId)
                    stmt.setString(2, tenantId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) mapCalculation(rs) else null
                    }
                }
            }
            DomainResult.Success(calc)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to find calculation: ${e.message}")
        }
    }

    override suspend fun findCalculationByFingerprint(tenantId: String, fingerprint: String): DomainResult<PrintingCalculationResult?> {
        return try {
            val calc = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM printing_calculations WHERE request_fingerprint = ? AND tenant_id = ? ORDER BY calculated_at DESC LIMIT 1"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, fingerprint)
                    stmt.setString(2, tenantId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) mapCalculation(rs) else null
                    }
                }
            }
            DomainResult.Success(calc)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to find calculation by fingerprint: ${e.message}")
        }
    }

    override suspend fun listCalculations(tenantId: String, limit: Int): DomainResult<List<PrintingCalculationResult>> {
        return try {
            val list = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM printing_calculations WHERE tenant_id = ? ORDER BY calculated_at DESC LIMIT ?"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setInt(2, limit)
                    stmt.executeQuery().use { rs ->
                        val res = mutableListOf<PrintingCalculationResult>()
                        while (rs.next()) {
                            res.add(mapCalculation(rs))
                        }
                        res
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to list calculations: ${e.message}")
        }
    }

    private fun mapCalculation(rs: ResultSet): PrintingCalculationResult {
        val calculationId = rs.getString("calculation_id")
        val tenantId = rs.getString("tenant_id")
        val projectId = rs.getString("project_id")
        val requestFingerprint = rs.getString("request_fingerprint")
        val jobTitle = rs.getString("job_title")
        val productType = try { ProductType.valueOf(rs.getString("product_type")) } catch (_: Exception) { ProductType.PRINTING_JOB }
        val quantity = rs.getLong("quantity")
        val quantityUnit = try { QuantityUnit.valueOf(rs.getString("quantity_unit")) } catch (_: Exception) { QuantityUnit.PIECES }

        val fW = rs.getBigDecimal("finished_width_mm") ?: BigDecimal.ZERO
        val fH = rs.getBigDecimal("finished_height_mm") ?: BigDecimal.ZERO
        val finDim = PrintingDimension(fW, fH, MeasurementUnit.MILLIMETERS)

        val matName = rs.getString("material_name") ?: "Paper"
        val stockType = try { PaperStockType.valueOf(rs.getString("stock_type")) } catch (_: Exception) { PaperStockType.ART_PAPER }
        val gsm = rs.getBigDecimal("gsm")
        val sW = rs.getBigDecimal("sheet_width_mm")
        val sH = rs.getBigDecimal("sheet_height_mm")
        val sheetDim = if (sW != null && sH != null) PrintingDimension(sW, sH, MeasurementUnit.MILLIMETERS) else null
        val unitPrice = rs.getBigDecimal("material_unit_price")

        val processType = try { PrintingProcessType.valueOf(rs.getString("process_type")) } catch (_: Exception) { PrintingProcessType.OFFSET }
        val sides = try { PrintingSideOption.valueOf(rs.getString("sides")) } catch (_: Exception) { PrintingSideOption.SINGLE_SIDED }
        val colorMode = try { ColorMode.valueOf(rs.getString("color_mode")) } catch (_: Exception) { ColorMode.CMYK_FOUR_COLOR }
        val frontColors = rs.getInt("front_colors")
        val backColors = rs.getInt("back_colors")
        val spotColors = rs.getInt("spot_colors")

        val setupSheets = rs.getLong("setup_sheets")
        val runningWastePct = rs.getBigDecimal("running_waste_pct") ?: BigDecimal.ZERO
        val finishingWastePct = rs.getBigDecimal("finishing_waste_pct") ?: BigDecimal.ZERO

        val prodSheets = rs.getLong("productive_sheets")
        val wasteSheets = rs.getLong("waste_sheets")
        val totalSheets = rs.getLong("total_sheets")
        val totalReams = rs.getBigDecimal("total_reams") ?: BigDecimal.ZERO
        val totalWeightKg = rs.getBigDecimal("total_weight_kg")

        val impressions = rs.getLong("total_impressions")
        val plates = rs.getInt("plate_count")

        val matCost = rs.getBigDecimal("material_cost")
        val prtCost = rs.getBigDecimal("printing_cost")
        val pltCost = rs.getBigDecimal("plate_cost")
        val finCost = rs.getBigDecimal("finishing_cost")

        val totalEstCost = rs.getBigDecimal("total_estimated_cost")
        val estUnitCost = rs.getBigDecimal("estimated_unit_cost")
        val currency = rs.getString("currency") ?: "BDT"
        val status = try { CalculationStatus.valueOf(rs.getString("calculation_status")) } catch (_: Exception) { CalculationStatus.SUCCESSFUL }
        val classification = try { EstimateActualClassification.valueOf(rs.getString("classification")) } catch (_: Exception) { EstimateActualClassification.ESTIMATED }
        val requestedAt = rs.getLong("requested_at")
        val calculatedAt = rs.getLong("calculated_at")
        val integrityHash = rs.getString("integrity_hash") ?: ""
        val version = rs.getString("calculation_version") ?: "1.0.0"

        val spec = NormalizedPrintingSpecification(
            jobTitle = jobTitle,
            productType = productType,
            finishedDimension = finDim,
            normalizedDimensionMm = finDim,
            quantity = QuantitySpecification(quantity, quantityUnit, quantity),
            material = PaperMaterialSpecification(
                materialName = matName,
                stockType = stockType,
                gsm = gsm,
                sheetDimension = sheetDim,
                unitPricePerSheet = unitPrice,
                currency = currency
            ),
            processType = processType,
            sides = sides,
            color = ColorSpecification(colorMode, frontColors, backColors, spotColors),
            waste = WasteAllowanceSpecification(setupSheets, runningWastePct, finishingWastePct),
            currency = currency
        )

        val matRes = MaterialRequirementResult(
            finishedItemsPerSheet = 1,
            cutDirection = "CALCULATED",
            productiveSheetsRequired = prodSheets,
            wasteSheetsRequired = wasteSheets,
            totalSheetsRequired = totalSheets,
            totalReamsRequired = totalReams,
            totalWeightKg = totalWeightKg,
            estimatedMaterialCost = matCost,
            costStatus = status
        )

        val prtRes = PrintingRequirementResult(
            totalImpressions = impressions,
            totalPasses = if (sides.isDoubleSided) 2 else 1,
            plateCount = plates,
            estimatedPrintingCost = prtCost,
            estimatedPlateCost = pltCost,
            costStatus = status
        )

        val finRes = FinishingRequirementResult(
            operations = emptyList(),
            totalEstimatedFinishingCost = finCost,
            costStatus = status
        )

        return PrintingCalculationResult(
            calculationId = calculationId,
            tenantId = tenantId,
            projectId = projectId,
            requestFingerprint = requestFingerprint,
            requestedAt = requestedAt,
            calculatedAt = calculatedAt,
            status = status,
            classification = classification,
            normalizedSpecification = spec,
            materialRequirement = matRes,
            printingRequirement = prtRes,
            finishingRequirement = finRes,
            breakdownItems = emptyList(),
            totalEstimatedCost = totalEstCost,
            estimatedUnitCost = estUnitCost,
            currency = currency,
            diagnostics = emptyList(),
            integrityHash = integrityHash,
            calculationVersion = version
        )
    }
}
