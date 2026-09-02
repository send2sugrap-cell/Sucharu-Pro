package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorInvoiceDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorInvoiceRepository
import kotlinx.coroutines.flow.Flow

class VendorInvoiceRepositoryImpl(
    private val dataSource: VendorInvoiceDataSource
) : VendorInvoiceRepository {

    override fun observeInvoices(projectId: String, vendorId: String?, purchaseOrderId: String?): Flow<List<VendorInvoice>> {
        return dataSource.observeInvoices(projectId, vendorId, purchaseOrderId)
    }

    override suspend fun findById(projectId: String, invoiceId: String): DomainResult<VendorInvoice> {
        return dataSource.findById(projectId, invoiceId)
    }

    override suspend fun findByInvoiceNumber(projectId: String, invoiceNumber: String): DomainResult<VendorInvoice> {
        return dataSource.findByInvoiceNumber(projectId, invoiceNumber)
    }

    override suspend fun findByVendorInvoiceNumber(projectId: String, vendorId: String, vendorInvoiceNumber: String): DomainResult<VendorInvoice> {
        return dataSource.findByVendorInvoiceNumber(projectId, vendorId, vendorInvoiceNumber)
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        purchaseOrderId: String?,
        status: VendorInvoiceStatus?,
        matchStatus: VendorInvoiceMatchStatus?
    ): DomainResult<List<VendorInvoice>> {
        return dataSource.list(projectId, vendorId, purchaseOrderId, status, matchStatus)
    }

    override suspend fun createInvoice(invoice: VendorInvoice): DomainResult<VendorInvoice> {
        return dataSource.createInvoice(invoice)
    }

    override suspend fun updateInvoice(invoice: VendorInvoice): DomainResult<VendorInvoice> {
        return dataSource.updateInvoice(invoice)
    }

    override suspend fun updateStatus(
        projectId: String,
        invoiceId: String,
        status: VendorInvoiceStatus,
        matchStatus: VendorInvoiceMatchStatus?,
        updatedBy: String
    ): DomainResult<VendorInvoice> {
        return dataSource.updateStatus(projectId, invoiceId, status, matchStatus, updatedBy)
    }

    override suspend fun saveMatch(match: VendorInvoiceMatch): DomainResult<VendorInvoiceMatch> {
        return dataSource.saveMatch(match)
    }

    override suspend fun findMatchByInvoiceId(projectId: String, invoiceId: String): DomainResult<VendorInvoiceMatch> {
        return dataSource.findMatchByInvoiceId(projectId, invoiceId)
    }

    override suspend fun saveException(exception: VendorInvoiceException): DomainResult<VendorInvoiceException> {
        return dataSource.saveException(exception)
    }

    override suspend fun listExceptions(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceException>> {
        return dataSource.listExceptions(projectId, invoiceId)
    }

    override suspend fun resolveException(
        projectId: String,
        exceptionId: String,
        resolvedBy: String,
        resolutionNotes: String
    ): DomainResult<VendorInvoiceException> {
        return dataSource.resolveException(projectId, exceptionId, resolvedBy, resolutionNotes)
    }

    override suspend fun appendAudit(auditEvent: VendorInvoiceAuditEvent): DomainResult<VendorInvoiceAuditEvent> {
        return dataSource.appendAudit(auditEvent)
    }

    override suspend fun listAudits(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceAuditEvent>> {
        return dataSource.listAudits(projectId, invoiceId)
    }
}
