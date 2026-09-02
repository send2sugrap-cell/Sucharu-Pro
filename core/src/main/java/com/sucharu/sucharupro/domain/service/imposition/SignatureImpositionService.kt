package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.repository.imposition.SignatureImpositionRepository
import java.math.BigDecimal

/**
 * Service Layer Interface for Multi-Page Signature Imposition.
 * Module 18 Step 04.
 */
interface SignatureImpositionService {
    suspend fun optimizeAndSave(
        tenantId: String,
        name: String,
        jobId: String,
        orderId: String,
        orderItemId: String,
        productName: String,
        totalPages: Int,
        signaturePageCount: Int = 16,
        bindingMethod: BindingMethod = BindingMethod.SADDLE_STITCH,
        sheetTurningMethod: SheetTurningMethod = SheetTurningMethod.SHEETWISE,
        foldingScheme: FoldingScheme = FoldingScheme.RIGHT_ANGLE_16PP,
        pageDimension: PrintingDimension,
        parentSheetDimension: PrintingDimension,
        requiredQuantity: Long,
        paperStockType: PaperStockType,
        gsm: BigDecimal,
        customCaliperMm: BigDecimal? = null,
        marginSpec: ImpositionMarginSpec = ImpositionMarginSpec(),
        gutterSpec: SignatureGutterSpec = SignatureGutterSpec(),
        enableCreepCompensation: Boolean = true,
        saveSpecification: Boolean = true,
        actor: String = "prepress_operator"
    ): SignatureImpositionSpecification

    suspend fun getSpecification(tenantId: String, signatureImpositionId: String): SignatureImpositionSpecification?
    suspend fun listSpecifications(tenantId: String, limit: Int = 50, offset: Int = 0): List<SignatureImpositionSpecification>
    suspend fun listSpecificationsByJob(tenantId: String, jobId: String): List<SignatureImpositionSpecification>
    suspend fun updateStatus(tenantId: String, signatureImpositionId: String, status: SignatureStatus, actor: String, notes: String? = null): Boolean
    suspend fun exportHandoffContract(tenantId: String, signatureImpositionId: String): Module18Step04SignatureHandoffContract?
}

/**
 * Production implementation of SignatureImpositionService.
 * Module 18 Step 04.
 */
class SignatureImpositionServiceImpl(
    private val repository: SignatureImpositionRepository
) : SignatureImpositionService {

    override suspend fun optimizeAndSave(
        tenantId: String,
        name: String,
        jobId: String,
        orderId: String,
        orderItemId: String,
        productName: String,
        totalPages: Int,
        signaturePageCount: Int,
        bindingMethod: BindingMethod,
        sheetTurningMethod: SheetTurningMethod,
        foldingScheme: FoldingScheme,
        pageDimension: PrintingDimension,
        parentSheetDimension: PrintingDimension,
        requiredQuantity: Long,
        paperStockType: PaperStockType,
        gsm: BigDecimal,
        customCaliperMm: BigDecimal?,
        marginSpec: ImpositionMarginSpec,
        gutterSpec: SignatureGutterSpec,
        enableCreepCompensation: Boolean,
        saveSpecification: Boolean,
        actor: String
    ): SignatureImpositionSpecification {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }

        val spec = SignatureImpositionEngine.optimizeSignatureImposition(
            tenantId = tenantId,
            name = name,
            jobId = jobId,
            orderId = orderId,
            orderItemId = orderItemId,
            productName = productName,
            totalPages = totalPages,
            signaturePageCount = signaturePageCount,
            bindingMethod = bindingMethod,
            sheetTurningMethod = sheetTurningMethod,
            foldingScheme = foldingScheme,
            pageDimension = pageDimension,
            parentSheetDimension = parentSheetDimension,
            requiredQuantity = requiredQuantity,
            paperStockType = paperStockType,
            gsm = gsm,
            customCaliperMm = customCaliperMm,
            marginSpec = marginSpec,
            gutterSpec = gutterSpec,
            enableCreepCompensation = enableCreepCompensation,
            actor = actor
        )

        return if (saveSpecification) {
            repository.saveSpecification(tenantId, spec)
        } else {
            spec
        }
    }

    override suspend fun getSpecification(
        tenantId: String,
        signatureImpositionId: String
    ): SignatureImpositionSpecification? {
        return repository.getSpecificationById(tenantId, signatureImpositionId)
    }

    override suspend fun listSpecifications(
        tenantId: String,
        limit: Int,
        offset: Int
    ): List<SignatureImpositionSpecification> {
        val all = repository.listSpecifications(tenantId)
        return all.drop(offset).take(limit)
    }

    override suspend fun listSpecificationsByJob(
        tenantId: String,
        jobId: String
    ): List<SignatureImpositionSpecification> {
        return repository.listSpecificationsByJob(tenantId, jobId)
    }

    override suspend fun updateStatus(
        tenantId: String,
        signatureImpositionId: String,
        status: SignatureStatus,
        actor: String,
        notes: String?
    ): Boolean {
        return repository.updateStatus(tenantId, signatureImpositionId, status, actor, notes)
    }

    override suspend fun exportHandoffContract(
        tenantId: String,
        signatureImpositionId: String
    ): Module18Step04SignatureHandoffContract? {
        val spec = repository.getSpecificationById(tenantId, signatureImpositionId) ?: return null
        return Module18Step04SignatureHandoffContract(
            contractVersion = "1.0.0",
            signatureImpositionId = spec.signatureImpositionId,
            tenantId = spec.tenantId,
            jobId = spec.jobId,
            orderId = spec.orderId,
            orderItemId = spec.orderItemId,
            productName = spec.productName,
            totalSignatures = spec.totalSignaturesCount,
            signaturePageCount = spec.signaturePageCount,
            paperStockType = spec.paperStockType.name,
            gsm = spec.gsm,
            parentSheetWidthMm = spec.parentSheetDimension.width,
            parentSheetHeightMm = spec.parentSheetDimension.height,
            sheetsPerSignature = spec.commonRequiredSheets,
            totalParentSheetsRequired = spec.totalParentSheetsRequired,
            bindingMethod = spec.bindingMethod.name,
            sheetTurningMethod = spec.sheetTurningMethod.name,
            integrityHash = spec.integrityHash,
            emittedAt = System.currentTimeMillis()
        )
    }
}
