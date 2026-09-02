package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow

interface VendorInvoiceDataSource {
    fun observeInvoices(projectId: String, vendorId: String? = null, purchaseOrderId: String? = null): Flow<List<VendorInvoice>>
    suspend fun findById(projectId: String, invoiceId: String): DomainResult<VendorInvoice>
    suspend fun findByInvoiceNumber(projectId: String, invoiceNumber: String): DomainResult<VendorInvoice>
    suspend fun findByVendorInvoiceNumber(projectId: String, vendorId: String, vendorInvoiceNumber: String): DomainResult<VendorInvoice>
    suspend fun list(
        projectId: String,
        vendorId: String? = null,
        purchaseOrderId: String? = null,
        status: VendorInvoiceStatus? = null,
        matchStatus: VendorInvoiceMatchStatus? = null
    ): DomainResult<List<VendorInvoice>>
    suspend fun createInvoice(invoice: VendorInvoice): DomainResult<VendorInvoice>
    suspend fun updateInvoice(invoice: VendorInvoice): DomainResult<VendorInvoice>
    suspend fun updateStatus(
        projectId: String,
        invoiceId: String,
        status: VendorInvoiceStatus,
        matchStatus: VendorInvoiceMatchStatus? = null,
        updatedBy: String
    ): DomainResult<VendorInvoice>

    suspend fun saveMatch(match: VendorInvoiceMatch): DomainResult<VendorInvoiceMatch>
    suspend fun findMatchByInvoiceId(projectId: String, invoiceId: String): DomainResult<VendorInvoiceMatch>

    suspend fun saveException(exception: VendorInvoiceException): DomainResult<VendorInvoiceException>
    suspend fun listExceptions(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceException>>
    suspend fun resolveException(
        projectId: String,
        exceptionId: String,
        resolvedBy: String,
        resolutionNotes: String
    ): DomainResult<VendorInvoiceException>

    suspend fun appendAudit(auditEvent: VendorInvoiceAuditEvent): DomainResult<VendorInvoiceAuditEvent>
    suspend fun listAudits(projectId: String, invoiceId: String): DomainResult<List<VendorInvoiceAuditEvent>>
}
