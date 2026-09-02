package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.printingquote.PrintingQuoteDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.CalculationStatus
import com.sucharu.sucharupro.domain.model.printingquote.*
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * PostgreSQL implementation of [PrintingQuoteDataSource] with TransactionManager + RLS.
 * All queries are scoped to the tenant via TenantContext (which sets app.current_tenant).
 * Module 17 Step 02.
 */
class PostgresPrintingQuoteDataSource(
    private val transactionManager: TransactionManager
) : PrintingQuoteDataSource {

    // ─────────────────────────────────────────────────────────────
    // Quote Header
    // ─────────────────────────────────────────────────────────────

    override suspend fun insertQuote(quote: PrintingQuote): DomainResult<PrintingQuote> {
        return try {
            transactionManager.inTransaction(TenantContext(quote.projectId)) { ctx ->
                val sql = """
                    INSERT INTO printing_quotes (
                        quote_id, tenant_id, project_id, quote_number, job_title,
                        calculation_id, request_fingerprint, status, current_version, currency,
                        ordered_quantity, customer_ref, customer_note, internal_note,
                        idempotency_key, created_by, created_at, updated_at,
                        approved_at, approved_by, expires_at, integrity_hash
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT (quote_id) DO NOTHING
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, quote.quoteId)
                    s.setString(2, quote.tenantId)
                    s.setString(3, quote.projectId)
                    s.setString(4, quote.quoteNumber)
                    s.setString(5, quote.jobTitle)
                    s.setString(6, quote.calculationId)
                    s.setString(7, quote.requestFingerprint)
                    s.setString(8, quote.status.name)
                    s.setInt(9, quote.currentVersion)
                    s.setString(10, quote.currency)
                    s.setLong(11, quote.orderedQuantity)
                    s.setString(12, quote.customerRef)
                    s.setString(13, quote.customerNote)
                    s.setString(14, quote.internalNote)
                    s.setString(15, quote.idempotencyKey)
                    s.setString(16, quote.createdBy)
                    s.setLong(17, quote.createdAt)
                    s.setLong(18, quote.updatedAt)
                    s.setObject(19, quote.approvedAt)
                    s.setString(20, quote.approvedBy)
                    s.setObject(21, quote.expiresAt)
                    s.setString(22, quote.integrityHash)
                    s.executeUpdate()
                }
            }
            DomainResult.Success(quote)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to insert quote: ${e.message}")
        }
    }

    override suspend fun updateQuote(quote: PrintingQuote): DomainResult<PrintingQuote> {
        return try {
            transactionManager.inTransaction(TenantContext(quote.projectId)) { ctx ->
                val sql = """
                    UPDATE printing_quotes SET
                        status = ?, current_version = ?, ordered_quantity = ?,
                        request_fingerprint = ?, customer_note = ?, internal_note = ?,
                        updated_at = ?, approved_at = ?, approved_by = ?,
                        expires_at = ?, integrity_hash = ?
                    WHERE quote_id = ? AND tenant_id = ?
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, quote.status.name)
                    s.setInt(2, quote.currentVersion)
                    s.setLong(3, quote.orderedQuantity)
                    s.setString(4, quote.requestFingerprint)
                    s.setString(5, quote.customerNote)
                    s.setString(6, quote.internalNote)
                    s.setLong(7, quote.updatedAt)
                    s.setObject(8, quote.approvedAt)
                    s.setString(9, quote.approvedBy)
                    s.setObject(10, quote.expiresAt)
                    s.setString(11, quote.integrityHash)
                    s.setString(12, quote.quoteId)
                    s.setString(13, quote.tenantId)
                    s.executeUpdate()
                }
            }
            DomainResult.Success(quote)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to update quote: ${e.message}")
        }
    }

    override suspend fun selectQuoteById(tenantId: String, quoteId: String): DomainResult<PrintingQuote?> {
        return try {
            val quote = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quotes WHERE quote_id = ? AND tenant_id = ?"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, quoteId); s.setString(2, tenantId)
                    s.executeQuery().use { rs -> if (rs.next()) mapQuote(rs) else null }
                }
            }
            DomainResult.Success(quote)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to select quote: ${e.message}")
        }
    }

    override suspend fun selectQuoteByIdempotencyKey(tenantId: String, key: String): DomainResult<PrintingQuote?> {
        return try {
            val quote = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quotes WHERE idempotency_key = ? AND tenant_id = ? LIMIT 1"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, key); s.setString(2, tenantId)
                    s.executeQuery().use { rs -> if (rs.next()) mapQuote(rs) else null }
                }
            }
            DomainResult.Success(quote)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to select quote by idempotency key: ${e.message}")
        }
    }

    override suspend fun selectQuotes(tenantId: String, limit: Int): DomainResult<List<PrintingQuote>> {
        return try {
            val list = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quotes WHERE tenant_id = ? ORDER BY created_at DESC LIMIT ?"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, tenantId); s.setInt(2, limit)
                    s.executeQuery().use { rs ->
                        val res = mutableListOf<PrintingQuote>()
                        while (rs.next()) res.add(mapQuote(rs))
                        res
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to list quotes: ${e.message}")
        }
    }

    override suspend fun selectQuotesByCalculationId(
        tenantId: String, calculationId: String
    ): DomainResult<List<PrintingQuote>> {
        return try {
            val list = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quotes WHERE tenant_id = ? AND calculation_id = ? ORDER BY created_at DESC"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, tenantId); s.setString(2, calculationId)
                    s.executeQuery().use { rs ->
                        val res = mutableListOf<PrintingQuote>()
                        while (rs.next()) res.add(mapQuote(rs))
                        res
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to list quotes by calculationId: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Quote Versions
    // ─────────────────────────────────────────────────────────────

    override suspend fun insertQuoteVersion(version: PrintingQuoteVersion): DomainResult<PrintingQuoteVersion> {
        return try {
            transactionManager.inTransaction(TenantContext(version.projectId)) { ctx ->
                val qb = version.quantityBreakdown
                val pr = version.pricing
                val sql = """
                    INSERT INTO printing_quote_versions (
                        version_id, quote_id, tenant_id, project_id, version_number, status, currency,
                        calculation_id, spec_fingerprint, calc_fingerprint,
                        ordered_quantity, produced_quantity, sellable_quantity, wastage_quantity,
                        wastage_percentage, imposition_ups,
                        costing_assumptions_json, pricing_assumptions_json,
                        total_cost, unit_cost,
                        base_selling_price, discount_type, discount_value, discount_amount,
                        tax_percentage, tax_amount, final_quote_total,
                        markup_amount, markup_percentage, gross_profit, gross_margin_percentage,
                        contribution_amount, contribution_margin_pct,
                        break_even_price, break_even_quantity,
                        target_margin_price, target_margin_percentage,
                        integrity_hash, created_by, created_at, is_approved
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT (version_id) DO NOTHING
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, version.versionId)
                    s.setString(2, version.quoteId)
                    s.setString(3, version.tenantId)
                    s.setString(4, version.projectId)
                    s.setInt(5, version.versionNumber)
                    s.setString(6, version.status.name)
                    s.setString(7, version.currency)
                    s.setString(8, version.calculationId)
                    s.setString(9, version.specFingerprint)
                    s.setString(10, version.calcFingerprint)
                    s.setLong(11, qb.orderedQuantity)
                    s.setLong(12, qb.producedQuantity)
                    s.setLong(13, qb.sellableQuantity)
                    s.setLong(14, qb.wastageQuantity)
                    s.setBigDecimal(15, qb.wastagePercentage)
                    s.setInt(16, qb.impositionUps)
                    s.setString(17, """{"engineVersion":"${version.costingAssumptions.engineVersion}","overheadPct":"${version.costingAssumptions.overheadAllocationPct}"}""")
                    s.setString(18, """{"method":"${version.pricingAssumptions.pricingMethod}","markupPct":"${version.pricingAssumptions.markupPercentage}","targetMarginPct":"${version.pricingAssumptions.targetMarginPercentage}"}""")
                    s.setBigDecimal(19, version.totalCost)
                    s.setBigDecimal(20, version.unitCost)
                    s.setBigDecimal(21, pr.baseSellingPrice)
                    s.setString(22, pr.discountType.name)
                    s.setBigDecimal(23, pr.discountValue)
                    s.setBigDecimal(24, pr.discountAmount)
                    s.setBigDecimal(25, pr.taxPercentage)
                    s.setBigDecimal(26, pr.taxAmount)
                    s.setBigDecimal(27, pr.finalQuoteTotal)
                    s.setBigDecimal(28, pr.markupAmount)
                    s.setBigDecimal(29, pr.markupPercentage)
                    s.setBigDecimal(30, pr.grossProfit)
                    s.setBigDecimal(31, pr.grossMarginPercentage)
                    s.setBigDecimal(32, pr.contributionAmount)
                    s.setBigDecimal(33, pr.contributionMarginPercentage)
                    s.setBigDecimal(34, pr.breakEvenPrice)
                    s.setLong(35, pr.breakEvenQuantity)
                    s.setBigDecimal(36, pr.targetMarginPrice)
                    s.setBigDecimal(37, pr.targetMarginPercentage)
                    s.setString(38, version.integrityHash)
                    s.setString(39, version.createdBy)
                    s.setLong(40, version.createdAt)
                    s.setBoolean(41, version.isApproved)
                    s.executeUpdate()
                }
            }
            DomainResult.Success(version)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to insert quote version: ${e.message}")
        }
    }

    override suspend fun updateQuoteVersion(version: PrintingQuoteVersion): DomainResult<PrintingQuoteVersion> {
        return try {
            transactionManager.inTransaction(TenantContext(version.projectId)) { ctx ->
                val sql = "UPDATE printing_quote_versions SET is_approved = ? WHERE version_id = ? AND tenant_id = ?"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setBoolean(1, version.isApproved)
                    s.setString(2, version.versionId)
                    s.setString(3, version.tenantId)
                    s.executeUpdate()
                }
            }
            DomainResult.Success(version)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to update quote version: ${e.message}")
        }
    }

    override suspend fun selectVersionById(tenantId: String, versionId: String): DomainResult<PrintingQuoteVersion?> {
        return try {
            val v = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quote_versions WHERE version_id = ? AND tenant_id = ?"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, versionId); s.setString(2, tenantId)
                    s.executeQuery().use { rs -> if (rs.next()) mapVersion(rs) else null }
                }
            }
            DomainResult.Success(v)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to select version: ${e.message}")
        }
    }

    override suspend fun selectVersionsByQuoteId(tenantId: String, quoteId: String): DomainResult<List<PrintingQuoteVersion>> {
        return try {
            val list = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quote_versions WHERE quote_id = ? AND tenant_id = ? ORDER BY version_number ASC"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, quoteId); s.setString(2, tenantId)
                    s.executeQuery().use { rs ->
                        val res = mutableListOf<PrintingQuoteVersion>()
                        while (rs.next()) res.add(mapVersion(rs))
                        res
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to list versions: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Cost Components
    // ─────────────────────────────────────────────────────────────

    override suspend fun insertCostComponents(components: List<PrintingCostComponent>): DomainResult<List<PrintingCostComponent>> {
        if (components.isEmpty()) return DomainResult.Success(emptyList())
        return try {
            val projectId = components.first().quoteId.let { components.first().tenantId }
            transactionManager.inTransaction(TenantContext(components.first().tenantId)) { ctx ->
                val sql = """
                    INSERT INTO printing_quote_cost_components (
                        component_id, version_id, quote_id, tenant_id, project_id,
                        component_type, component_code, description, quantity, unit,
                        unit_rate, amount, formula_reference, source_ref, is_applicable, sort_order
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (component_id) DO NOTHING
                """.trimIndent()
                components.forEach { c ->
                    ctx.connection.prepareStatement(sql).use { s ->
                        s.setString(1, c.componentId); s.setString(2, c.versionId)
                        s.setString(3, c.quoteId); s.setString(4, c.tenantId)
                        s.setString(5, c.tenantId)  // project_id maps to tenantId context
                        s.setString(6, c.componentType.name); s.setString(7, c.componentCode)
                        s.setString(8, c.description); s.setBigDecimal(9, c.quantity)
                        s.setString(10, c.unit); s.setBigDecimal(11, c.unitRate)
                        s.setBigDecimal(12, c.amount); s.setString(13, c.formulaReference)
                        s.setString(14, c.sourceRef); s.setBoolean(15, c.isApplicable)
                        s.setInt(16, c.sortOrder)
                        s.executeUpdate()
                    }
                }
            }
            DomainResult.Success(components)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to insert cost components: ${e.message}")
        }
    }

    override suspend fun selectCostComponents(tenantId: String, versionId: String): DomainResult<List<PrintingCostComponent>> {
        return try {
            val list = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quote_cost_components WHERE version_id = ? AND tenant_id = ? ORDER BY sort_order"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, versionId); s.setString(2, tenantId)
                    s.executeQuery().use { rs ->
                        val res = mutableListOf<PrintingCostComponent>()
                        while (rs.next()) res.add(mapCostComponent(rs))
                        res
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to select cost components: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Quantity Tiers
    // ─────────────────────────────────────────────────────────────

    override suspend fun insertQuantityTiers(tiers: List<PrintingQuantityTier>): DomainResult<List<PrintingQuantityTier>> {
        if (tiers.isEmpty()) return DomainResult.Success(emptyList())
        return try {
            transactionManager.inTransaction(TenantContext(tiers.first().tenantId)) { ctx ->
                val sql = """
                    INSERT INTO printing_quote_quantity_tiers (
                        tier_id, version_id, quote_id, tenant_id, project_id,
                        tier_quantity, unit_cost, total_cost, selling_price_unit,
                        final_total, gross_margin_pct, is_base_tier
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (tier_id) DO NOTHING
                """.trimIndent()
                tiers.forEach { t ->
                    ctx.connection.prepareStatement(sql).use { s ->
                        s.setString(1, t.tierId); s.setString(2, t.versionId)
                        s.setString(3, t.quoteId); s.setString(4, t.tenantId)
                        s.setString(5, t.tenantId)
                        s.setLong(6, t.tierQuantity); s.setBigDecimal(7, t.unitCost)
                        s.setBigDecimal(8, t.totalCost); s.setBigDecimal(9, t.sellingPricePerUnit)
                        s.setBigDecimal(10, t.finalTotal); s.setBigDecimal(11, t.grossMarginPercentage)
                        s.setBoolean(12, t.isBaseTier)
                        s.executeUpdate()
                    }
                }
            }
            DomainResult.Success(tiers)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to insert quantity tiers: ${e.message}")
        }
    }

    override suspend fun selectQuantityTiers(tenantId: String, versionId: String): DomainResult<List<PrintingQuantityTier>> {
        return try {
            val list = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quote_quantity_tiers WHERE version_id = ? AND tenant_id = ? ORDER BY tier_quantity"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, versionId); s.setString(2, tenantId)
                    s.executeQuery().use { rs ->
                        val res = mutableListOf<PrintingQuantityTier>()
                        while (rs.next()) res.add(mapTier(rs))
                        res
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to select quantity tiers: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Audit Events
    // ─────────────────────────────────────────────────────────────

    override suspend fun insertAuditEvent(event: QuoteAuditEvent): DomainResult<QuoteAuditEvent> {
        return try {
            transactionManager.inTransaction(TenantContext(event.projectId)) { ctx ->
                val sql = """
                    INSERT INTO printing_quote_audit_events (
                        audit_id, quote_id, version_id, tenant_id, project_id,
                        event_type, actor, description, before_status, after_status,
                        metadata_json, occurred_at
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (audit_id) DO NOTHING
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, event.auditId); s.setString(2, event.quoteId)
                    s.setString(3, event.versionId); s.setString(4, event.tenantId)
                    s.setString(5, event.projectId); s.setString(6, event.eventType.name)
                    s.setString(7, event.actor); s.setString(8, event.description)
                    s.setString(9, event.beforeStatus?.name); s.setString(10, event.afterStatus?.name)
                    s.setString(11, event.metadataJson); s.setLong(12, event.occurredAt)
                    s.executeUpdate()
                }
            }
            DomainResult.Success(event)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to insert audit event: ${e.message}")
        }
    }

    override suspend fun selectAuditEvents(tenantId: String, quoteId: String): DomainResult<List<QuoteAuditEvent>> {
        return try {
            val list = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quote_audit_events WHERE quote_id = ? AND tenant_id = ? ORDER BY occurred_at ASC"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, quoteId); s.setString(2, tenantId)
                    s.executeQuery().use { rs ->
                        val res = mutableListOf<QuoteAuditEvent>()
                        while (rs.next()) res.add(mapAuditEvent(rs))
                        res
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to select audit events: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Provenance
    // ─────────────────────────────────────────────────────────────

    override suspend fun insertProvenance(provenance: QuoteProvenance): DomainResult<QuoteProvenance> {
        return try {
            transactionManager.inTransaction(TenantContext(provenance.projectId)) { ctx ->
                val sql = """
                    INSERT INTO printing_quote_provenance (
                        provenance_id, quote_id, version_id, tenant_id, project_id,
                        calculation_id, calculation_version, calculation_status,
                        spec_fingerprint, calc_fingerprint,
                        costing_engine_version, pricing_engine_version,
                        assumptions_json, step01_breakdown_json,
                        captured_at, captured_by
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (provenance_id) DO NOTHING
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, provenance.provenanceId); s.setString(2, provenance.quoteId)
                    s.setString(3, provenance.versionId); s.setString(4, provenance.tenantId)
                    s.setString(5, provenance.projectId); s.setString(6, provenance.calculationId)
                    s.setString(7, provenance.calculationVersion); s.setString(8, provenance.calculationStatus.name)
                    s.setString(9, provenance.specFingerprint); s.setString(10, provenance.calcFingerprint)
                    s.setString(11, provenance.costingEngineVersion); s.setString(12, provenance.pricingEngineVersion)
                    s.setString(13, provenance.assumptionsJson); s.setString(14, provenance.step01BreakdownJson)
                    s.setLong(15, provenance.capturedAt); s.setString(16, provenance.capturedBy)
                    s.executeUpdate()
                }
            }
            DomainResult.Success(provenance)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to insert provenance: ${e.message}")
        }
    }

    override suspend fun selectProvenance(tenantId: String, quoteId: String, versionId: String): DomainResult<QuoteProvenance?> {
        return try {
            val p = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quote_provenance WHERE quote_id = ? AND version_id = ? AND tenant_id = ? LIMIT 1"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, quoteId); s.setString(2, versionId); s.setString(3, tenantId)
                    s.executeQuery().use { rs -> if (rs.next()) mapProvenance(rs) else null }
                }
            }
            DomainResult.Success(p)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to select provenance: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Reconciliation
    // ─────────────────────────────────────────────────────────────

    override suspend fun insertReconciliationEvent(event: QuoteReconciliationEvent): DomainResult<QuoteReconciliationEvent> {
        return try {
            transactionManager.inTransaction(TenantContext(event.projectId)) { ctx ->
                val sql = """
                    INSERT INTO printing_quote_reconciliation_events (
                        reconciliation_id, quote_id, version_id, tenant_id, project_id,
                        is_reconciled, total_cost_check, revenue_identity_check,
                        gross_profit_check, margin_check, markup_check, breakeven_check,
                        discrepancies_json, reconciled_at, reconciled_by, integrity_hash
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (reconciliation_id) DO NOTHING
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, event.reconciliationId); s.setString(2, event.quoteId)
                    s.setString(3, event.versionId); s.setString(4, event.tenantId)
                    s.setString(5, event.projectId); s.setBoolean(6, event.isReconciled)
                    s.setBoolean(7, event.totalCostCheck); s.setBoolean(8, event.revenueIdentityCheck)
                    s.setBoolean(9, event.grossProfitCheck); s.setBoolean(10, event.marginCheck)
                    s.setBoolean(11, event.markupCheck); s.setBoolean(12, event.breakevenCheck)
                    s.setString(13, event.discrepanciesJson); s.setLong(14, event.reconciledAt)
                    s.setString(15, event.reconciledBy); s.setString(16, event.integrityHash)
                    s.executeUpdate()
                }
            }
            DomainResult.Success(event)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to insert reconciliation event: ${e.message}")
        }
    }

    override suspend fun selectReconciliationEvents(tenantId: String, quoteId: String): DomainResult<List<QuoteReconciliationEvent>> {
        return try {
            val list = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM printing_quote_reconciliation_events WHERE quote_id = ? AND tenant_id = ? ORDER BY reconciled_at ASC"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, quoteId); s.setString(2, tenantId)
                    s.executeQuery().use { rs ->
                        val res = mutableListOf<QuoteReconciliationEvent>()
                        while (rs.next()) res.add(mapReconciliation(rs))
                        res
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to select reconciliation events: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ResultSet Mappers
    // ─────────────────────────────────────────────────────────────

    private fun mapQuote(rs: ResultSet): PrintingQuote = PrintingQuote(
        quoteId = rs.getString("quote_id"),
        tenantId = rs.getString("tenant_id"),
        projectId = rs.getString("project_id"),
        quoteNumber = rs.getString("quote_number"),
        jobTitle = rs.getString("job_title"),
        calculationId = rs.getString("calculation_id"),
        requestFingerprint = rs.getString("request_fingerprint") ?: "",
        status = safeEnum<QuoteStatus>(rs.getString("status"), QuoteStatus.DRAFT),
        currentVersion = rs.getInt("current_version"),
        currency = rs.getString("currency") ?: "BDT",
        orderedQuantity = rs.getLong("ordered_quantity"),
        customerRef = rs.getString("customer_ref"),
        customerNote = rs.getString("customer_note"),
        internalNote = rs.getString("internal_note"),
        idempotencyKey = rs.getString("idempotency_key"),
        createdBy = rs.getString("created_by"),
        createdAt = rs.getLong("created_at"),
        updatedAt = rs.getLong("updated_at"),
        approvedAt = rs.getObject("approved_at") as? Long,
        approvedBy = rs.getString("approved_by"),
        expiresAt = rs.getObject("expires_at") as? Long,
        integrityHash = rs.getString("integrity_hash") ?: ""
    )

    private fun mapVersion(rs: ResultSet): PrintingQuoteVersion {
        val qb = QuoteQuantityBreakdown(
            orderedQuantity = rs.getLong("ordered_quantity"),
            producedQuantity = rs.getLong("produced_quantity"),
            sellableQuantity = rs.getLong("sellable_quantity"),
            wastageQuantity = rs.getLong("wastage_quantity"),
            wastagePercentage = rs.getBigDecimal("wastage_percentage") ?: QUOTE_ZERO,
            impositionUps = rs.getInt("imposition_ups")
        )
        val pricing = PrintingPricingSnapshot(
            baseSellingPrice = rs.getBigDecimal("base_selling_price") ?: QUOTE_ZERO,
            discountType = safeEnum<DiscountType>(rs.getString("discount_type"), DiscountType.NONE),
            discountValue = rs.getBigDecimal("discount_value") ?: QUOTE_ZERO,
            discountAmount = rs.getBigDecimal("discount_amount") ?: QUOTE_ZERO,
            taxPercentage = rs.getBigDecimal("tax_percentage") ?: QUOTE_ZERO,
            taxAmount = rs.getBigDecimal("tax_amount") ?: QUOTE_ZERO,
            finalQuoteTotal = rs.getBigDecimal("final_quote_total") ?: QUOTE_ZERO,
            markupAmount = rs.getBigDecimal("markup_amount") ?: QUOTE_ZERO,
            markupPercentage = rs.getBigDecimal("markup_percentage") ?: QUOTE_ZERO,
            grossProfit = rs.getBigDecimal("gross_profit") ?: QUOTE_ZERO,
            grossMarginPercentage = rs.getBigDecimal("gross_margin_percentage") ?: QUOTE_ZERO,
            contributionAmount = rs.getBigDecimal("contribution_amount") ?: QUOTE_ZERO,
            contributionMarginPercentage = rs.getBigDecimal("contribution_margin_pct") ?: QUOTE_ZERO,
            breakEvenPrice = rs.getBigDecimal("break_even_price") ?: QUOTE_ZERO,
            breakEvenQuantity = rs.getLong("break_even_quantity"),
            targetMarginPrice = rs.getBigDecimal("target_margin_price"),
            targetMarginPercentage = rs.getBigDecimal("target_margin_percentage")
        )
        return PrintingQuoteVersion(
            versionId = rs.getString("version_id"),
            quoteId = rs.getString("quote_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            versionNumber = rs.getInt("version_number"),
            status = safeEnum<QuoteStatus>(rs.getString("status"), QuoteStatus.CALCULATED),
            currency = rs.getString("currency") ?: "BDT",
            calculationId = rs.getString("calculation_id"),
            specFingerprint = rs.getString("spec_fingerprint") ?: "",
            calcFingerprint = rs.getString("calc_fingerprint") ?: "",
            quantityBreakdown = qb,
            costingAssumptions = CostingAssumptions(),
            pricingAssumptions = PricingAssumptions(),
            totalCost = rs.getBigDecimal("total_cost") ?: QUOTE_ZERO,
            unitCost = rs.getBigDecimal("unit_cost") ?: QUOTE_ZERO,
            pricing = pricing,
            integrityHash = rs.getString("integrity_hash") ?: "",
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            isApproved = rs.getBoolean("is_approved")
        )
    }

    private fun mapCostComponent(rs: ResultSet): PrintingCostComponent = PrintingCostComponent(
        componentId = rs.getString("component_id"),
        versionId = rs.getString("version_id"),
        quoteId = rs.getString("quote_id"),
        tenantId = rs.getString("tenant_id"),
        componentType = safeEnum<CostComponentType>(rs.getString("component_type"), CostComponentType.OTHER),
        componentCode = rs.getString("component_code"),
        description = rs.getString("description"),
        quantity = rs.getBigDecimal("quantity") ?: QUOTE_ZERO,
        unit = rs.getString("unit"),
        unitRate = rs.getBigDecimal("unit_rate"),
        amount = rs.getBigDecimal("amount") ?: QUOTE_ZERO,
        formulaReference = rs.getString("formula_reference") ?: "",
        sourceRef = rs.getString("source_ref"),
        isApplicable = rs.getBoolean("is_applicable"),
        sortOrder = rs.getInt("sort_order")
    )

    private fun mapTier(rs: ResultSet): PrintingQuantityTier = PrintingQuantityTier(
        tierId = rs.getString("tier_id"),
        versionId = rs.getString("version_id"),
        quoteId = rs.getString("quote_id"),
        tenantId = rs.getString("tenant_id"),
        tierQuantity = rs.getLong("tier_quantity"),
        unitCost = rs.getBigDecimal("unit_cost") ?: QUOTE_ZERO,
        totalCost = rs.getBigDecimal("total_cost") ?: QUOTE_ZERO,
        sellingPricePerUnit = rs.getBigDecimal("selling_price_unit") ?: QUOTE_ZERO,
        finalTotal = rs.getBigDecimal("final_total") ?: QUOTE_ZERO,
        grossMarginPercentage = rs.getBigDecimal("gross_margin_pct") ?: QUOTE_ZERO,
        isBaseTier = rs.getBoolean("is_base_tier")
    )

    private fun mapAuditEvent(rs: ResultSet): QuoteAuditEvent = QuoteAuditEvent(
        auditId = rs.getString("audit_id"),
        quoteId = rs.getString("quote_id"),
        versionId = rs.getString("version_id"),
        tenantId = rs.getString("tenant_id"),
        projectId = rs.getString("project_id"),
        eventType = safeEnum<QuoteAuditEventType>(rs.getString("event_type"), QuoteAuditEventType.QUOTE_CREATED),
        actor = rs.getString("actor"),
        description = rs.getString("description"),
        beforeStatus = rs.getString("before_status")?.let { safeEnum<QuoteStatus>(it, QuoteStatus.DRAFT) },
        afterStatus = rs.getString("after_status")?.let { safeEnum<QuoteStatus>(it, QuoteStatus.DRAFT) },
        metadataJson = rs.getString("metadata_json"),
        occurredAt = rs.getLong("occurred_at")
    )

    private fun mapProvenance(rs: ResultSet): QuoteProvenance = QuoteProvenance(
        provenanceId = rs.getString("provenance_id"),
        quoteId = rs.getString("quote_id"),
        versionId = rs.getString("version_id"),
        tenantId = rs.getString("tenant_id"),
        projectId = rs.getString("project_id"),
        calculationId = rs.getString("calculation_id"),
        calculationVersion = rs.getString("calculation_version") ?: "1.0.0",
        calculationStatus = safeEnum<CalculationStatus>(rs.getString("calculation_status"), CalculationStatus.SUCCESSFUL),
        specFingerprint = rs.getString("spec_fingerprint") ?: "",
        calcFingerprint = rs.getString("calc_fingerprint") ?: "",
        costingEngineVersion = rs.getString("costing_engine_version") ?: "2.0.0",
        pricingEngineVersion = rs.getString("pricing_engine_version") ?: "2.0.0",
        assumptionsJson = rs.getString("assumptions_json") ?: "{}",
        step01BreakdownJson = rs.getString("step01_breakdown_json"),
        capturedAt = rs.getLong("captured_at"),
        capturedBy = rs.getString("captured_by")
    )

    private fun mapReconciliation(rs: ResultSet): QuoteReconciliationEvent = QuoteReconciliationEvent(
        reconciliationId = rs.getString("reconciliation_id"),
        quoteId = rs.getString("quote_id"),
        versionId = rs.getString("version_id"),
        tenantId = rs.getString("tenant_id"),
        projectId = rs.getString("project_id"),
        isReconciled = rs.getBoolean("is_reconciled"),
        totalCostCheck = rs.getBoolean("total_cost_check"),
        revenueIdentityCheck = rs.getBoolean("revenue_identity_check"),
        grossProfitCheck = rs.getBoolean("gross_profit_check"),
        marginCheck = rs.getBoolean("margin_check"),
        markupCheck = rs.getBoolean("markup_check"),
        breakevenCheck = rs.getBoolean("breakeven_check"),
        discrepanciesJson = rs.getString("discrepancies_json"),
        reconciledAt = rs.getLong("reconciled_at"),
        reconciledBy = rs.getString("reconciled_by"),
        integrityHash = rs.getString("integrity_hash") ?: ""
    )

    private inline fun <reified T : Enum<T>> safeEnum(value: String?, default: T): T =
        try { if (value != null) enumValueOf<T>(value) else default } catch (_: Exception) { default }
}
