package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class FakeVendorInvoiceDataSource : VendorInvoiceDataSource {

    private val invoices = ConcurrentHashMap<String, MutableMap<String, VendorInvoice>>()
    private val matches = ConcurrentHashMap<String, MutableMap<String, VendorInvoiceMatch>>()
    private val exceptions = ConcurrentHashMap<String, MutableList<VendorInvoiceException>>()
    private val audits = ConcurrentHashMap<String, MutableList<VendorInvoiceAuditEvent>>()

    private val invoiceFlows = ConcurrentHashMap<String, MutableStateFlow<List<VendorInvoice>>>()

    private fun getProjectInvoices(projectId: String): MutableMap<String, VendorInvoice> {
        return invoices.getOrPut(projectId) { ConcurrentHashMap() }
    }

    private fun getProjectMatches(projectId: String): MutableMap<String, VendorInvoiceMatch> {
        return matches.getOrPut(projectId) { ConcurrentHashMap() }
    }

    private fun getProjectExceptions(projectId: String): MutableList<VendorInvoiceException> {
        return exceptions.getOrPut(projectId) { mutableListOf() }
    }

    private fun getProjectAudits(projectId: String): MutableList<VendorInvoiceAuditEvent> {
        return audits.getOrPut(projectId) { mutableListOf() }
    }

    private fun notifyFlow(projectId: String) {
        val list = getProjectInvoices(projectId).values.toList()
        invoiceFlows.keys.filter { it.startsWith("$projectId:") }.forEach { key ->
            invoiceFlows[key]?.value = list
        }
    }

    override fun observeInvoices(projectId: String, vendorId: String?, purchaseOrderId: String?): Flow<List<VendorInvoice>> {
        val key = "$projectId:$vendorId:$purchaseOrderId"
        val flow = invoiceFlows.getOrPut(key) {
            MutableStateFlow(getProjectInvoices(projectId).values.toList())
        }
        return flow.asStateFlow()
    }

    override suspend fun findById(projectId: String, invoiceId: String): DomainResult<VendorInvoice> {
        val inv = getProjectInvoices(projectId)[invoiceId]
        return if (inv != null) DomainResult.Success(inv) else DomainResult.Error(NoSuchElementException("Invoice '$invoiceId' not found in project '$projectId'"))
    }

    override suspend fun findByInvoiceNumber(projectId: String, invoiceNumber: String): DomainResult<VendorInvoice> {
        val inv = getProjectInvoices(projectId).values.find { it.invoiceNumber == invoiceNumber }
        return if (inv != null) DomainResult.Success(inv) else DomainResult.Error(NoSuchElementException("Invoice '$invoiceNumber' not found in project '$projectId'"))
    }

    override suspend fun findByVendorInvoiceNumber(projectId: String, vendorId: String, vendorInvoiceNumber: String): DomainResult<VendorInvoice> {
        val inv = getProjectInvoices(projectId).values.find { it.vendorId == vendorId && it.vendorInvoiceNumber == vendorInvoiceNumber }
        return if (inv != null) DomainResult.Success(inv) else DomainResult.Error(NoSuchElementException("Vendor invoice '$vendorInvoiceNumber' not found in project '$projectId'"))
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        purchaseOrderId: String?,
        status: VendorInvoiceStatus?,
        matchStatus: VendorInvoiceMatchStatus?
    ): DomainResult<List<VendorInvoice>> {
        var res = getProjectInvoices(projectId).values.toList()
        if (vendorId != null) res = res.filter { it.vendorId == vendorId }
        if (purchaseOrderId != null) res = res.filter { it.purchaseOrderId == purchaseOrderId }
        if (status != null) res = res.filter { it.status == status }
        if (matchStatus != null) res = res.filter { it.matchStatus == matchStatus }
        return DomainResult.Success(res.sortedByDescending { it.createdAt })
    }

    override suspend fun createInvoice(invoice: VendorInvoice): DomainResult<VendorInvoice> {
        val projectMap = getProjectInvoices(invoice.projectId)
        if (projectMap.containsKey(invoice.invoiceId)) {
            return DomainResult.Error(IllegalStateException("Invoice '${invoice.invoiceId}' already exists"))
        }
        if (projectMap.values.any { it.invoiceNumber == invoice.invoiceNumber }) {
            return DomainResult.Error(IllegalStateException("Invoice number '${invoice.invoiceNumber}' already exists"))
        }
        if (projectMap.values.any { it.vendorId == invoice.vendorId && it.vendorInvoiceNumber == invoice.vendorInvoiceNumber }) {
            return DomainResult.Error(IllegalStateException("Vendor invoice number '${invoice.vendorInvoiceNumber}' already exists for vendor '${invoice.vendorId}'"))
        }
        projectMap[invoice.invoiceId] = invoice
        notifyFlow(invoice.projectId)
        return DomainResult.Success(invoice)
    }

    override suspend fun updateInvoice(invoice: VendorInvoice): DomainResult<VendorInvoice> {
        val projectMap = getProjectInvoices(invoice.projectId)
        val existing = projectMap[invoice.invoiceId]
            ?: return DomainResult.Error(NoSuchElementException("Invoice '${invoice.invoiceId}' not found"))

        if (existing.version != invoice.version) {
            return DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on invoice '${invoice.invoiceId}'"))
        }
        val updated = invoice.copy(version = invoice.version + 1, updatedAt = System.currentTimeMillis())
        projectMap[invoice.invoiceId] = updated
        notifyFlow(invoice.projectId)
        return DomainResult.Success(updated)
    }

    override suspend fun updateStatus(
        projectId: String,
        invoiceId: String,
        status: VendorInvoiceStatus,
        matchStatus: VendorInvoiceMatchStatus?,
        updatedBy: String
    ): DomainResult<VendorInvoice> {
        val projectMap = getProjectInvoices(projectId)
        val existing = projectMap[invoiceId]
            ?: return DomainResult.Error(NoSuchElementException("Invoice '$invoiceId' not found"))

        val updated = existing.copy(
            status = status,
            matchStatus = matchStatus ?: existing.matchStatus,
            updatedAt = System.currentTimeMillis(),
            updatedBy = updatedBy,
            version = existing.version + 1
        )
        projectMap[invoiceId] = updated
        notifyFlow(projectId)
        return DomainResult.Success(updated)
    }

    override suspend fun saveMatch(match: VendorInvoiceMatch): DomainResult<VendorInvoiceMatch> {
        getProjectMatches(match.projectId)[match.invoiceId] = match
        return DomainResult.Success(match)
    }

    override suspend fun findMatchByInvoiceId(projectId: String, invoiceId: String): DomainResult<VendorInvoiceMatch> {
        val match = getProjectMatches(projectId)[invoiceId]
        return if (match != null) DomainResult.Success(match) else DomainResult.Error(NoSuchElementException("Match not found for invoice '$invoiceId'"))
    }

    override suspend fun saveException(exception: VendorInvoiceException): DomainResult<VendorInvoiceException> {
        getProjectExceptions(exception.projectId).add(exception)
        return DomainResult.Success(exception)
    }

    override suspend fun listExceptions(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceException>> {
        val list = getProjectExceptions(projectId).filter { it.invoiceId == invoiceId }
        return DomainResult.Success(list)
    }

    override suspend fun resolveException(
        projectId: String,
        exceptionId: String,
        resolvedBy: String,
        resolutionNotes: String
    ): DomainResult<VendorInvoiceException> {
        val list = getProjectExceptions(projectId)
        val idx = list.indexOfFirst { it.exceptionId == exceptionId }
        if (idx == -1) {
            return DomainResult.Error(NoSuchElementException("Exception '$exceptionId' not found"))
        }
        val existing = list[idx]
        val updated = existing.copy(
            resolved = true,
            resolvedBy = resolvedBy,
            resolvedAt = System.currentTimeMillis(),
            resolutionNotes = resolutionNotes
        )
        list[idx] = updated
        return DomainResult.Success(updated)
    }

    override suspend fun appendAudit(auditEvent: VendorInvoiceAuditEvent): DomainResult<VendorInvoiceAuditEvent> {
        getProjectAudits(auditEvent.projectId).add(auditEvent)
        return DomainResult.Success(auditEvent)
    }

    override suspend fun listAudits(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceAuditEvent>> {
        val list = getProjectAudits(projectId).filter { it.invoiceId == invoiceId }
        return DomainResult.Success(list)
    }
}
