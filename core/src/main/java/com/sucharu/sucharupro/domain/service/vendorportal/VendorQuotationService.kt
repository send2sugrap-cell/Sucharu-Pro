package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

interface VendorQuotationService {

    suspend fun createQuotationDraft(
        quotation: VendorQuotation,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation>

    suspend fun updateQuotationDraft(
        quotation: VendorQuotation,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation>

    suspend fun submitQuotation(
        quotationId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation>

    suspend fun withdrawQuotation(
        quotationId: String,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation>

    suspend fun requestRevision(
        quotationId: String,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation>

    suspend fun submitRevision(
        quotationId: String,
        revisedQuotation: VendorQuotation,
        reasonForRevision: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorQuotation>

    suspend fun getQuotationById(
        quotationId: String,
        tenantId: String
    ): DomainResult<VendorQuotation>

    suspend fun getQuotationForVendor(
        quotationId: String,
        vendorId: String,
        tenantId: String
    ): DomainResult<VendorQuotation>

    suspend fun listQuotationsByRfq(
        rfqId: String,
        tenantId: String
    ): DomainResult<List<VendorQuotation>>

    suspend fun listQuotationsByVendor(
        vendorId: String,
        tenantId: String
    ): DomainResult<List<VendorQuotation>>

    suspend fun listRevisions(
        quotationId: String,
        tenantId: String
    ): DomainResult<List<VendorQuotationRevision>>
}
