package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.repository.imposition.CtpOutputRepository
import com.sucharu.sucharupro.domain.repository.imposition.ImpositionRepository
import com.sucharu.sucharupro.domain.repository.imposition.SignatureImpositionRepository
import java.math.BigDecimal
import java.time.Instant

/**
 * Service interface for CTP Prepress Output & Plate Imposition Packaging.
 * Module 18 Step 05.
 */
interface CtpOutputService {
    suspend fun generateFromSignature(
        tenantId: String,
        signatureImpositionId: String,
        plateDimensionSpec: PlateDimensionSpec? = null,
        resolutionDpi: OutputResolutionDpi = OutputResolutionDpi.DPI_2540,
        screeningMethod: ScreeningMethod = ScreeningMethod.AM_CONVENTIONAL,
        screenRulingLpi: BigDecimal = BigDecimal("175.0000"),
        markPolicy: PrepressMarkPolicy = PrepressMarkPolicy(),
        colorSeparations: List<PlateColorSeparation> = listOf(
            PlateColorSeparation.CYAN,
            PlateColorSeparation.MAGENTA,
            PlateColorSeparation.YELLOW,
            PlateColorSeparation.BLACK
        ),
        spotColorNames: List<String> = emptyList(),
        actor: String = "ctp_operator"
    ): CtpOutputSpecification

    suspend fun generateFromSingleJob(
        tenantId: String,
        impositionId: String,
        plateDimensionSpec: PlateDimensionSpec? = null,
        resolutionDpi: OutputResolutionDpi = OutputResolutionDpi.DPI_2540,
        screeningMethod: ScreeningMethod = ScreeningMethod.AM_CONVENTIONAL,
        screenRulingLpi: BigDecimal = BigDecimal("175.0000"),
        markPolicy: PrepressMarkPolicy = PrepressMarkPolicy(),
        colorSeparations: List<PlateColorSeparation> = listOf(
            PlateColorSeparation.CYAN,
            PlateColorSeparation.MAGENTA,
            PlateColorSeparation.YELLOW,
            PlateColorSeparation.BLACK
        ),
        actor: String = "ctp_operator"
    ): CtpOutputSpecification

    suspend fun getSpecification(tenantId: String, ctpOutputId: String): CtpOutputSpecification?

    suspend fun listSpecifications(tenantId: String, jobId: String? = null): List<CtpOutputSpecification>

    suspend fun updateStatus(
        tenantId: String,
        ctpOutputId: String,
        newStatus: CtpOutputStatus,
        actor: String,
        reason: String? = null
    ): CtpOutputSpecification

    suspend fun getHandoffContract(tenantId: String, ctpOutputId: String): Module18Step05CtpHandoffContract
}

/**
 * Production implementation of [CtpOutputService].
 * Module 18 Step 05.
 */
class CtpOutputServiceImpl(
    private val ctpOutputRepository: CtpOutputRepository,
    private val signatureImpositionRepository: SignatureImpositionRepository? = null,
    private val impositionRepository: ImpositionRepository? = null
) : CtpOutputService {

    override suspend fun generateFromSignature(
        tenantId: String,
        signatureImpositionId: String,
        plateDimensionSpec: PlateDimensionSpec?,
        resolutionDpi: OutputResolutionDpi,
        screeningMethod: ScreeningMethod,
        screenRulingLpi: BigDecimal,
        markPolicy: PrepressMarkPolicy,
        colorSeparations: List<PlateColorSeparation>,
        spotColorNames: List<String>,
        actor: String
    ): CtpOutputSpecification {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(signatureImpositionId.isNotBlank()) { "Signature Imposition ID must not be blank." }

        val signatureSpec = signatureImpositionRepository?.getSpecificationById(tenantId, signatureImpositionId)
            ?: throw IllegalArgumentException("Signature imposition specification not found: $signatureImpositionId for tenant: $tenantId")

        val generated = CtpOutputGenerationEngine.generateFromSignatureImposition(
            signatureSpec = signatureSpec,
            plateDimensionSpec = plateDimensionSpec,
            resolutionDpi = resolutionDpi,
            screeningMethod = screeningMethod,
            screenRulingLpi = screenRulingLpi,
            markPolicy = markPolicy,
            colorSeparations = colorSeparations,
            spotColorNames = spotColorNames,
            packageVersion = 1,
            actor = actor
        )

        return ctpOutputRepository.save(generated)
    }

    override suspend fun generateFromSingleJob(
        tenantId: String,
        impositionId: String,
        plateDimensionSpec: PlateDimensionSpec?,
        resolutionDpi: OutputResolutionDpi,
        screeningMethod: ScreeningMethod,
        screenRulingLpi: BigDecimal,
        markPolicy: PrepressMarkPolicy,
        colorSeparations: List<PlateColorSeparation>,
        actor: String
    ): CtpOutputSpecification {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(impositionId.isNotBlank()) { "Imposition ID must not be blank." }

        val impositionSpec = impositionRepository?.getSpecificationById(tenantId, impositionId)
            ?: throw IllegalArgumentException("Single job imposition specification not found: $impositionId for tenant: $tenantId")

        require(impositionSpec.tenantId == tenantId) { "Cross-tenant access violation." }

        val generated = CtpOutputGenerationEngine.generateFromSingleJobImposition(
            impositionSpec = impositionSpec,
            plateDimensionSpec = plateDimensionSpec,
            resolutionDpi = resolutionDpi,
            screeningMethod = screeningMethod,
            screenRulingLpi = screenRulingLpi,
            markPolicy = markPolicy,
            colorSeparations = colorSeparations,
            packageVersion = 1,
            actor = actor
        )

        return ctpOutputRepository.save(generated)
    }

    override suspend fun getSpecification(tenantId: String, ctpOutputId: String): CtpOutputSpecification? {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        return ctpOutputRepository.findById(tenantId, ctpOutputId)
    }

    override suspend fun listSpecifications(tenantId: String, jobId: String?): List<CtpOutputSpecification> {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        return if (jobId != null) {
            ctpOutputRepository.findByJobId(tenantId, jobId)
        } else {
            ctpOutputRepository.listAll(tenantId)
        }
    }

    override suspend fun updateStatus(
        tenantId: String,
        ctpOutputId: String,
        newStatus: CtpOutputStatus,
        actor: String,
        reason: String?
    ): CtpOutputSpecification {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(actor.isNotBlank()) { "Actor must not be blank." }

        val existing = ctpOutputRepository.findById(tenantId, ctpOutputId)
            ?: throw IllegalArgumentException("CTP output specification not found: $ctpOutputId for tenant: $tenantId")

        validateStatusTransition(existing.status, newStatus)

        return ctpOutputRepository.updateStatus(tenantId, ctpOutputId, newStatus.name, actor, reason)
            ?: throw IllegalStateException("Failed to update status for CTP output specification: $ctpOutputId")
    }

    override suspend fun getHandoffContract(tenantId: String, ctpOutputId: String): Module18Step05CtpHandoffContract {
        val spec = getSpecification(tenantId, ctpOutputId)
            ?: throw IllegalArgumentException("CTP output specification not found: $ctpOutputId for tenant: $tenantId")

        return Module18Step05CtpHandoffContract(
            contractVersion = "1.0.0",
            tenantId = spec.tenantId,
            ctpOutputId = spec.ctpOutputId,
            jobId = spec.jobId,
            orderId = spec.orderId,
            orderItemId = spec.orderItemId,
            sourceImpositionType = spec.sourceImpositionType,
            sourceImpositionId = spec.sourceImpositionId,
            sourceImpositionHash = spec.sourceImpositionHash,
            status = spec.status.name,
            packageVersion = spec.packageVersion,
            totalPlatesCount = spec.outputPackage.totalPlatesCount,
            frontPlatesCount = spec.outputPackage.frontPlatesCount,
            backPlatesCount = spec.outputPackage.backPlatesCount,
            resolutionDpi = spec.resolutionDpi.dpi,
            screeningMethod = spec.screeningMethod.name,
            defaultScreenRulingLpi = spec.defaultScreenRulingLpi,
            plateWidthMm = spec.plateDimensionSpec.plateWidthMm,
            plateHeightMm = spec.plateDimensionSpec.plateHeightMm,
            pressSheetWidthMm = spec.outputPackage.pressSheetWidthMm,
            pressSheetHeightMm = spec.outputPackage.pressSheetHeightMm,
            gripperMarginMm = spec.plateDimensionSpec.gripperMarginMm,
            tailMarginMm = spec.plateDimensionSpec.tailMarginMm,
            ctpOutputIntegrityHash = spec.integrityHash,
            generatedTimestamp = Instant.now().toString()
        )
    }

    private fun validateStatusTransition(current: CtpOutputStatus, target: CtpOutputStatus) {
        if (current == target) return
        val isValid = when (current) {
            CtpOutputStatus.DRAFT -> target in setOf(CtpOutputStatus.GENERATED, CtpOutputStatus.CANCELLED)
            CtpOutputStatus.GENERATED -> target in setOf(CtpOutputStatus.APPROVED, CtpOutputStatus.REJECTED, CtpOutputStatus.CANCELLED)
            CtpOutputStatus.APPROVED -> target in setOf(CtpOutputStatus.EXPORTED_TO_RIP, CtpOutputStatus.CANCELLED)
            CtpOutputStatus.REJECTED -> target in setOf(CtpOutputStatus.DRAFT, CtpOutputStatus.GENERATED, CtpOutputStatus.CANCELLED)
            CtpOutputStatus.EXPORTED_TO_RIP -> false
            CtpOutputStatus.CANCELLED -> false
        }
        if (!isValid) {
            throw IllegalStateException("Invalid CTP output status transition: from $current to $target")
        }
    }
}
