package com.sucharu.sucharupro.domain.preflight

import com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource
import com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl
import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngineImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.preflight.rules.*
import com.sucharu.sucharupro.domain.service.preflight.PreflightServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TypographyPreflightRulesTest {

    private lateinit var dataSource: FakePreflightDataSource
    private lateinit var repository: PreflightRepositoryImpl
    private lateinit var registry: PreflightRuleRegistry
    private lateinit var engine: PreflightEngineImpl
    private lateinit var service: PreflightServiceImpl

    private val tenantId = "TENANT-STEP05"
    private val artworkId = "ARTWORK-TYPO-01"

    @Before
    fun setUp() {
        dataSource = FakePreflightDataSource()
        repository = PreflightRepositoryImpl(dataSource)
        registry = PreflightRuleRegistry()

        // Register Step 01 - Step 05 Rules
        registry.registerRule(FileExistenceRule())
        registry.registerRule(FileNonEmptyRule())
        registry.registerRule(FileFormatSupportedRule())
        registry.registerRule(FileFormatMatchRule())
        registry.registerRule(FileSignatureValidRule())
        registry.registerRule(DocumentParseableRule())
        registry.registerRule(DocumentPageCountRule())
        registry.registerRule(ImageStructureReadableRule())

        registry.registerRule(SpecPageCountMatchRule())
        registry.registerRule(SpecDocumentSizeMatchRule())
        registry.registerRule(SpecOrientationMatchRule())
        registry.registerRule(SpecFormatMatchRule())
        registry.registerRule(SpecDocumentTypeMatchRule())

        registry.registerRule(AssetDpiResolutionRule())
        registry.registerRule(AssetColorSpaceRule())
        registry.registerRule(AssetColorProfileRule())
        registry.registerRule(AssetIntegrityMissingRule())
        registry.registerRule(AssetIntegrityCorruptRule())

        registry.registerRule(FontPresenceAndMissingRule())
        registry.registerRule(FontEmbeddingStatusRule())
        registry.registerRule(FontTypeCompatibilityRule())
        registry.registerRule(FontSubstitutionRule())
        registry.registerRule(FontRequirementComplianceRule())

        engine = PreflightEngineImpl(registry, repository)
        service = PreflightServiceImpl(engine, repository)
    }

    @Test
    fun test01_fontPresenceAndMissingRule_intactAndMissingFonts() = runBlocking {
        val rule = FontPresenceAndMissingRule()

        // Intact fonts
        val intactContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("hasMissingFonts" to false, "missingFonts" to emptyList<String>())
        )
        assertTrue(rule.isApplicable(intactContext))
        assertEquals(PreflightExecutionResult.PASS, rule.execute(intactContext).result)

        // Missing font
        val missingContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("missingFonts" to listOf("Helvetica-Bold"))
        )
        val missingRes = rule.execute(missingContext)
        assertEquals(PreflightExecutionResult.ERROR, missingRes.result)
        assertTrue(missingRes.findings.first().message.contains("Helvetica-Bold"))
    }

    @Test
    fun test02_fontEmbeddingStatusRule_embeddedAndNonEmbeddedFonts() = runBlocking {
        val rule = FontEmbeddingStatusRule()

        // Embedded fonts without strict requirement -> PASS
        val embeddedContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("embeddedFonts" to listOf("Calibri-Subset", "Arial-Embedded"))
        )
        val passRes = rule.execute(embeddedContext)
        assertEquals(PreflightExecutionResult.PASS, passRes.result)

        // Non-embedded font without strict requirement -> WARNING
        val nonEmbeddedContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = emptyMap(),
            artworkMetadataMap = mapOf("nonEmbeddedFonts" to listOf("TimesNewRomanPSMT"))
        )
        val warnRes = rule.execute(nonEmbeddedContext)
        assertEquals(PreflightExecutionResult.WARNING, warnRes.result)
        assertEquals(PreflightRuleSeverity.WARNING, warnRes.findings.first().severity)

        // Non-embedded font WITH strict requirement -> ERROR
        val reqEmbeddedContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("requireEmbeddedFonts" to true),
            artworkMetadataMap = mapOf("nonEmbeddedFonts" to listOf("TimesNewRomanPSMT"))
        )
        val errorRes = rule.execute(reqEmbeddedContext)
        assertEquals(PreflightExecutionResult.ERROR, errorRes.result)
        assertEquals(PreflightRuleSeverity.ERROR, errorRes.findings.first().severity)
    }

    @Test
    fun test03_fontTypeCompatibilityRule_type3BitmapFontWarning() = runBlocking {
        val rule = FontTypeCompatibilityRule()

        val type3Context = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("fontTypes" to mapOf("Font_Bitmap1" to "Type3"))
        )
        val res = rule.execute(type3Context)
        assertEquals(PreflightExecutionResult.WARNING, res.result)
        assertTrue(res.findings.first().message.contains("Type 3 (bitmap)"))
    }

    @Test
    fun test04_fontTypeCompatibilityRule_prohibitedFontType_returnsError() = runBlocking {
        val rule = FontTypeCompatibilityRule()

        val prohibitedContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("prohibitedFontTypes" to listOf("TYPE1")),
            artworkMetadataMap = mapOf("fontTypes" to mapOf("OldFont" to "Type1"))
        )
        val res = rule.execute(prohibitedContext)
        assertEquals(PreflightExecutionResult.ERROR, res.result)
        assertTrue(res.findings.first().message.contains("prohibited font format"))
    }

    @Test
    fun test05_fontSubstitutionRule_detectedAndProhibitedSubstitutions() = runBlocking {
        val rule = FontSubstitutionRule()

        // Allowed substitution -> WARNING
        val warnContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            artworkMetadataMap = mapOf("substitutedFonts" to mapOf("Garamond" to "Georgia"))
        )
        val warnRes = rule.execute(warnContext)
        assertEquals(PreflightExecutionResult.WARNING, warnRes.result)

        // Prohibited substitution -> ERROR
        val errorContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("prohibitSubstitutedFonts" to true),
            artworkMetadataMap = mapOf("substitutedFonts" to mapOf("Garamond" to "Georgia"))
        )
        val errorRes = rule.execute(errorContext)
        assertEquals(PreflightExecutionResult.ERROR, errorRes.result)
    }

    @Test
    fun test06_fontRequirementComplianceRule_matchingAndDisallowedFontFamilies() = runBlocking {
        val rule = FontRequirementComplianceRule()

        // Matching font family -> PASS
        val matchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("allowedFontFamilies" to listOf("Arial", "Helvetica")),
            artworkMetadataMap = mapOf("fonts" to listOf("Arial-BoldMT", "Helvetica-Light"))
        )
        assertEquals(PreflightExecutionResult.PASS, rule.execute(matchContext).result)

        // Disallowed font family -> ERROR
        val mismatchContext = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            orderSpecificationMap = mapOf("allowedFontFamilies" to listOf("Arial", "Helvetica")),
            artworkMetadataMap = mapOf("fonts" to listOf("ComicSansMS"))
        )
        val mismatchRes = rule.execute(mismatchContext)
        assertEquals(PreflightExecutionResult.ERROR, mismatchRes.result)
        assertTrue(mismatchRes.findings.first().message.contains("ComicSansMS"))
    }

    @Test
    fun test07_fullPreflightRun_withMissingFont_returnsError() = runBlocking {
        val context = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            jobId = "JOB-TYPO-01",
            artworkMetadataMap = mapOf(
                "fileSize" to 2000000L,
                "format" to "PDF",
                "extension" to "pdf",
                "mimeType" to "application/pdf",
                "pageCount" to 4,
                "missingFonts" to listOf("MyriadPro-Regular") // Missing font -> ERROR
            )
        )

        val result = service.runPreflight(context, "STAFF-PREPRESS")
        assertTrue(result is DomainResult.Success)
        val run = (result as DomainResult.Success).data

        assertEquals(PreflightRunStatus.COMPLETED, run.status)
        assertEquals(PreflightOverallResult.ERROR, run.overallResult)

        val findingsRes = service.listFindingsForRun(tenantId, run.preflightRunId)
        assertTrue(findingsRes is DomainResult.Success)
        val findings = (findingsRes as DomainResult.Success).data
        assertTrue(findings.any { it.ruleCode == "RULE_301_FONT_MISSING_CHECK" && it.severity == PreflightRuleSeverity.ERROR })
    }

    @Test
    fun test08_canonicalProductionWorkflow_regressionCheck() {
        val expectedSequence = listOf(
            ProductionStageType.DESIGN,
            ProductionStageType.APPROVAL,
            ProductionStageType.QC,
            ProductionStageType.ITEM_APPROVAL,
            ProductionStageType.CTP,
            ProductionStageType.PRINTING,
            ProductionStageType.LAMINATION,
            ProductionStageType.FOLDING,
            ProductionStageType.BINDING,
            ProductionStageType.FINAL_QC,
            ProductionStageType.PACKAGING,
            ProductionStageType.READY,
            ProductionStageType.DELIVERED
        )
        assertEquals(13, ProductionStageType.entries.size)
        assertEquals(expectedSequence, ProductionStageType.entries)
    }
}
