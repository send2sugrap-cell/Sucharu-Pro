package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.UUID

/**
 * High-performance, deterministic Signature Layout, Multi-Page Imposition & Sheet-Turning Engine.
 * Module 18 Step 04.
 */
object SignatureImpositionEngine {

    private const val SCALE = 4
    private val ROUNDING = RoundingMode.HALF_UP
    private val HUNDRED = BigDecimal("100.0000")

    /**
     * Optimizes multi-page publication imposition layout and computes folding forms.
     */
    fun optimizeSignatureImposition(
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
        paperStockType: com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType,
        gsm: BigDecimal,
        customCaliperMm: BigDecimal? = null,
        marginSpec: ImpositionMarginSpec = ImpositionMarginSpec(),
        gutterSpec: SignatureGutterSpec = SignatureGutterSpec(),
        enableCreepCompensation: Boolean = true,
        actor: String = "prepress_operator"
    ): SignatureImpositionSpecification {
        require(tenantId.isNotBlank()) { "Tenant ID must not be blank." }
        require(totalPages > 0) { "Total publication pages must be greater than 0: $totalPages" }
        require(requiredQuantity > 0L) { "Required quantity must be greater than 0: $requiredQuantity" }
        require(signaturePageCount in listOf(4, 8, 12, 16, 24, 32)) {
            "Unsupported signature page count: $signaturePageCount. Must be 4, 8, 12, 16, 24, or 32."
        }
        require(actor.isNotBlank()) { "Actor must not be blank." }

        // 1. Normalize dimensions to MM
        val pageDimMm = ImpositionMathUtils.toMillimeters(pageDimension)
        val pageW = pageDimMm.width
        val pageH = pageDimMm.height

        val parentDimMm = ImpositionMathUtils.toMillimeters(parentSheetDimension)
        val parentW = parentDimMm.width
        val parentH = parentDimMm.height

        require(pageW > BigDecimal.ZERO && pageH > BigDecimal.ZERO) { "Page dimensions must be positive." }
        require(parentW > BigDecimal.ZERO && parentH > BigDecimal.ZERO) { "Parent sheet dimensions must be positive." }

        // 2. Compute signature counts and page padding
        val pagesPerSignature = signaturePageCount
        val totalSignatures = Math.ceil(totalPages.toDouble() / pagesPerSignature.toDouble()).toInt().coerceAtLeast(1)
        val paddedTotalPages = totalSignatures * pagesPerSignature

        // 3. Creep / Shingling Calculation
        val caliper = customCaliperMm ?: gsm.multiply(BigDecimal("0.0012")).setScale(SCALE, ROUNDING)
        val totalSheetsInBooklet = Math.ceil(paddedTotalPages.toDouble() / 4.0).toInt().coerceAtLeast(1)
        val creepPerSheet = if (enableCreepCompensation && bindingMethod == BindingMethod.SADDLE_STITCH) {
            caliper.multiply(BigDecimal("2.0000")).setScale(SCALE, ROUNDING)
        } else {
            BigDecimal.ZERO.setScale(SCALE, ROUNDING)
        }
        val totalCreepMm = creepPerSheet.multiply(BigDecimal(totalSheetsInBooklet)).setScale(SCALE, ROUNDING)
        val innermostShiftMm = totalCreepMm

        val creepSummary = CreepCompensationSummary(
            isEnabled = enableCreepCompensation,
            paperCaliperMm = caliper,
            totalCreepMm = totalCreepMm,
            creepPerSheetMm = creepPerSheet,
            innermostPageShiftMm = innermostShiftMm
        )

        // 4. Determine Grid Dimensions per Form Side
        val pagesPerSide = pagesPerSignature / 2
        val (cols, rows) = when (pagesPerSignature) {
            4 -> Pair(2, 1)   // 2 cols x 1 row (2 pages per side)
            8 -> Pair(2, 2)   // 2 cols x 2 rows (4 pages per side)
            12 -> Pair(3, 2)  // 3 cols x 2 rows (6 pages per side)
            16 -> Pair(4, 2)  // 4 cols x 2 rows (8 pages per side)
            24 -> Pair(4, 3)  // 4 cols x 3 rows (12 pages per side)
            32 -> Pair(4, 4)  // 4 cols x 4 rows (16 pages per side)
            else -> Pair(4, 2)
        }

        // Calculate slot step sizes with gutters and margins
        val bleed2x = gutterSpec.bleedMm.multiply(BigDecimal("2.0000")).setScale(SCALE, ROUNDING)
        val effectivePageW = pageW.add(bleed2x).setScale(SCALE, ROUNDING)
        val effectivePageH = pageH.add(bleed2x).setScale(SCALE, ROUNDING)

        val totalGridW = effectivePageW.multiply(BigDecimal(cols))
            .add(gutterSpec.spineGutterMm.multiply(BigDecimal((cols - 1).coerceAtLeast(0))))
            .setScale(SCALE, ROUNDING)
        val totalGridH = effectivePageH.multiply(BigDecimal(rows))
            .add(gutterSpec.headGutterMm.multiply(BigDecimal((rows - 1).coerceAtLeast(0))))
            .setScale(SCALE, ROUNDING)

        val rawUsableW = parentW.subtract(marginSpec.totalHorizontalMarginMm).setScale(SCALE, ROUNDING)
        val rawUsableH = parentH.subtract(marginSpec.totalVerticalMarginMm).setScale(SCALE, ROUNDING)

        val isRotatedSheet = (totalGridW > rawUsableW || totalGridH > rawUsableH) &&
                (totalGridW <= rawUsableH && totalGridH <= rawUsableW)

        val (usableW, usableH) = if (isRotatedSheet) {
            Pair(rawUsableH, rawUsableW)
        } else {
            Pair(rawUsableW, rawUsableH)
        }

        require(totalGridW <= usableW && totalGridH <= usableH) {
            "Signature grid ($cols x $rows = ${totalGridW}mm x ${totalGridH}mm) exceeds usable press sheet area (${usableW}mm x ${usableH}mm)."
        }

        // Center grid on usable sheet area
        val startXMm = marginSpec.leftMm.add(usableW.subtract(totalGridW).divide(BigDecimal("2.0000"), SCALE, ROUNDING))
        val startYMm = marginSpec.topMm.add(usableH.subtract(totalGridH).divide(BigDecimal("2.0000"), SCALE, ROUNDING))

        // 5. Generate Signature Forms and Pages
        val signatureForms = mutableListOf<SignatureForm>()

        for (sigIdx in 1..totalSignatures) {
            val sigStartPage = (sigIdx - 1) * pagesPerSignature + 1
            val sigEndPage = sigIdx * pagesPerSignature

            // Page permutations for this signature
            val (frontPages, backPages, frontOrients, backOrients) = computeSignaturePageScheme(
                pagesPerSignature = pagesPerSignature,
                sigStartPage = sigStartPage,
                sigEndPage = sigEndPage,
                cols = cols,
                rows = rows
            )

            // Creep offset multiplier for this signature in saddle stitch
            val sigCreepOffset = if (enableCreepCompensation && bindingMethod == BindingMethod.SADDLE_STITCH) {
                val sheetFromOuter = (sigIdx - 1).toDouble() * (totalSheetsInBooklet.toDouble() / totalSignatures.toDouble())
                creepPerSheet.multiply(BigDecimal(sheetFromOuter.toInt())).setScale(SCALE, ROUNDING)
            } else {
                BigDecimal.ZERO
            }

            // Build Front Form Placements
            val frontPlacements = buildFormPlacements(
                formPrefix = "SIG-${sigIdx}-FRONT",
                pageNumbers = frontPages,
                orientations = frontOrients,
                cols = cols,
                rows = rows,
                startXMm = startXMm,
                startYMm = startYMm,
                effectivePageW = effectivePageW,
                effectivePageH = effectivePageH,
                pageW = pageW,
                pageH = pageH,
                gutterSpec = gutterSpec,
                creepShiftXMm = sigCreepOffset,
                maxPublicationPage = totalPages
            )

            val singlePageArea = pageW.multiply(pageH).setScale(SCALE, ROUNDING)
            val occupiedAreaForm = singlePageArea.multiply(BigDecimal(pagesPerSide)).setScale(SCALE, ROUNDING)
            val usableAreaForm = usableW.multiply(usableH).setScale(SCALE, ROUNDING)
            val formYieldPct = if (usableAreaForm > BigDecimal.ZERO) {
                occupiedAreaForm.multiply(HUNDRED).divide(usableAreaForm, SCALE, ROUNDING)
            } else BigDecimal.ZERO

            signatureForms.add(
                SignatureForm(
                    formId = "FORM-SIG-${sigIdx}-FRONT",
                    signatureNumber = sigIdx,
                    formSide = if (sheetTurningMethod == SheetTurningMethod.WORK_AND_TURN) {
                        SignatureFormSide.WORK_AND_TURN_COMBINED
                    } else {
                        SignatureFormSide.FRONT_SIDE_OUTER
                    },
                    pagesPerSide = pagesPerSide,
                    columns = cols,
                    rows = rows,
                    pagePlacements = frontPlacements,
                    formSheetWidthMm = parentW,
                    formSheetHeightMm = parentH,
                    occupiedAreaMm2 = occupiedAreaForm,
                    usableAreaMm2 = usableAreaForm,
                    yieldPercentage = formYieldPct
                )
            )

            // Build Back Form Placements if not Work-and-Turn combined
            if (sheetTurningMethod != SheetTurningMethod.WORK_AND_TURN) {
                val backPlacements = buildFormPlacements(
                    formPrefix = "SIG-${sigIdx}-BACK",
                    pageNumbers = backPages,
                    orientations = backOrients,
                    cols = cols,
                    rows = rows,
                    startXMm = startXMm,
                    startYMm = startYMm,
                    effectivePageW = effectivePageW,
                    effectivePageH = effectivePageH,
                    pageW = pageW,
                    pageH = pageH,
                    gutterSpec = gutterSpec,
                    creepShiftXMm = sigCreepOffset,
                    maxPublicationPage = totalPages
                )

                signatureForms.add(
                    SignatureForm(
                        formId = "FORM-SIG-${sigIdx}-BACK",
                        signatureNumber = sigIdx,
                        formSide = SignatureFormSide.BACK_SIDE_INNER,
                        pagesPerSide = pagesPerSide,
                        columns = cols,
                        rows = rows,
                        pagePlacements = backPlacements,
                        formSheetWidthMm = parentW,
                        formSheetHeightMm = parentH,
                        occupiedAreaMm2 = occupiedAreaForm,
                        usableAreaMm2 = usableAreaForm,
                        yieldPercentage = formYieldPct
                    )
                )
            }
        }

        // 6. Press Run Lengths & Sheet Requirements
        // If Work-and-Turn, 1 press sheet produces 2 copies of the signature; so sheets per signature = ceil(required / 2)
        val sheetsPerSignature = if (sheetTurningMethod == SheetTurningMethod.WORK_AND_TURN) {
            Math.ceil(requiredQuantity.toDouble() / 2.0).toLong().coerceAtLeast(1L)
        } else {
            requiredQuantity
        }

        val totalParentSheetsRequired = sheetsPerSignature * totalSignatures.toLong()
        val totalProducedCopies = if (sheetTurningMethod == SheetTurningMethod.WORK_AND_TURN) {
            sheetsPerSignature * 2L
        } else {
            sheetsPerSignature
        }
        val overageCopies = (totalProducedCopies - requiredQuantity).coerceAtLeast(0L)

        // 7. Area & Utilization Calculations
        val totalSheetAreaMm2 = parentW.multiply(parentH).setScale(SCALE, ROUNDING)
        val usableAreaMm2 = usableW.multiply(usableH).setScale(SCALE, ROUNDING)
        val totalOccupiedAreaMm2 = pageW.multiply(pageH).multiply(BigDecimal(pagesPerSide)).setScale(SCALE, ROUNDING)
        val wasteAreaMm2 = totalSheetAreaMm2.subtract(totalOccupiedAreaMm2).setScale(SCALE, ROUNDING)

        val sheetUtilizationPercentage = if (totalSheetAreaMm2 > BigDecimal.ZERO) {
            totalOccupiedAreaMm2.multiply(HUNDRED).divide(totalSheetAreaMm2, SCALE, ROUNDING)
        } else BigDecimal.ZERO

        val usableYieldPercentage = if (usableAreaMm2 > BigDecimal.ZERO) {
            totalOccupiedAreaMm2.multiply(HUNDRED).divide(usableAreaMm2, SCALE, ROUNDING)
        } else BigDecimal.ZERO

        val signatureImpositionId = "SIG-IMP-${UUID.randomUUID().toString().take(8).uppercase()}"

        // 8. Generate SHA-256 Tamper-Evident Integrity Seal
        val rawIntegrityString = buildString {
            append(tenantId).append("|")
            append(jobId).append("|")
            append(totalPages).append("|")
            append(signaturePageCount).append("|")
            append(bindingMethod.name).append("|")
            append(sheetTurningMethod.name).append("|")
            append(foldingScheme.name).append("|")
            append(parentW.toPlainString()).append("x").append(parentH.toPlainString()).append("|")
            append(pageW.toPlainString()).append("x").append(pageH.toPlainString()).append("|")
            append(totalParentSheetsRequired).append("|")
            append(usableYieldPercentage.toPlainString())
        }
        val integrityHash = computeSha256(rawIntegrityString)

        return SignatureImpositionSpecification(
            signatureImpositionId = signatureImpositionId,
            tenantId = tenantId,
            name = name.ifBlank { "Signature Imposition $signatureImpositionId" },
            jobId = jobId,
            orderId = orderId,
            orderItemId = orderItemId,
            productName = productName,
            totalPages = totalPages,
            paddedTotalPages = paddedTotalPages,
            signaturePageCount = signaturePageCount,
            totalSignaturesCount = totalSignatures,
            bindingMethod = bindingMethod,
            sheetTurningMethod = sheetTurningMethod,
            foldingScheme = foldingScheme,
            paperStockType = paperStockType,
            gsm = gsm,
            pageDimension = PrintingDimension(pageW, pageH, MeasurementUnit.MILLIMETERS),
            parentSheetDimension = PrintingDimension(parentW, parentH, MeasurementUnit.MILLIMETERS),
            marginSpec = marginSpec,
            gutterSpec = gutterSpec,
            creepSummary = creepSummary,
            signatureForms = signatureForms,
            commonRequiredSheets = sheetsPerSignature,
            totalParentSheetsRequired = totalParentSheetsRequired,
            totalProducedCopies = totalProducedCopies,
            overageCopies = overageCopies,
            totalSheetAreaMm2 = totalSheetAreaMm2,
            usableAreaMm2 = usableAreaMm2,
            occupiedAreaMm2 = totalOccupiedAreaMm2,
            wasteAreaMm2 = wasteAreaMm2,
            sheetUtilizationPercentage = sheetUtilizationPercentage,
            usableYieldPercentage = usableYieldPercentage,
            version = 1,
            status = SignatureStatus.OPTIMIZED,
            integrityHash = integrityHash,
            notes = "$totalSignatures signature(s) x $signaturePageCount pp ($sheetTurningMethod, $bindingMethod, Creep: ${totalCreepMm}mm).",
            createdAt = System.currentTimeMillis(),
            createdBy = actor
        )
    }

    private fun buildFormPlacements(
        formPrefix: String,
        pageNumbers: List<Int>,
        orientations: List<PageHeadOrientation>,
        cols: Int,
        rows: Int,
        startXMm: BigDecimal,
        startYMm: BigDecimal,
        effectivePageW: BigDecimal,
        effectivePageH: BigDecimal,
        pageW: BigDecimal,
        pageH: BigDecimal,
        gutterSpec: SignatureGutterSpec,
        creepShiftXMm: BigDecimal,
        maxPublicationPage: Int
    ): List<SignaturePagePlacement> {
        val placements = mutableListOf<SignaturePagePlacement>()
        var slotIdx = 0

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (slotIdx >= pageNumbers.size) break

                val pageNum = pageNumbers[slotIdx]
                val orient = orientations[slotIdx]

                val posX = startXMm.add(
                    effectivePageW.multiply(BigDecimal(c))
                ).add(
                    gutterSpec.spineGutterMm.multiply(BigDecimal(c))
                ).setScale(SCALE, ROUNDING)

                val posY = startYMm.add(
                    effectivePageH.multiply(BigDecimal(r))
                ).add(
                    gutterSpec.headGutterMm.multiply(BigDecimal(r))
                ).setScale(SCALE, ROUNDING)

                val isBlank = pageNum <= 0 || pageNum > maxPublicationPage
                val displayPage = if (isBlank) 0 else pageNum

                placements.add(
                    SignaturePagePlacement(
                        placementId = "$formPrefix-P$slotIdx",
                        pageNumber = displayPage,
                        slotIndex = slotIdx,
                        row = r,
                        column = c,
                        xMm = posX,
                        yMm = posY,
                        widthMm = pageW,
                        heightMm = pageH,
                        headOrientation = orient,
                        creepShiftXMm = creepShiftXMm,
                        creepShiftYMm = BigDecimal.ZERO,
                        isBlankPage = isBlank
                    )
                )

                slotIdx++
            }
        }

        return placements
    }

    /**
     * Computes the standard imposition page ordering and head orientation arrays for a signature form.
     */
    private fun computeSignaturePageScheme(
        pagesPerSignature: Int,
        sigStartPage: Int,
        sigEndPage: Int,
        cols: Int,
        rows: Int
    ): SignatureSchemeResult {
        return when (pagesPerSignature) {
            4 -> {
                // 4pp: 2 cols x 1 row
                // Front: Page 4 (left), Page 1 (right)
                // Back:  Page 2 (left), Page 3 (right)
                SignatureSchemeResult(
                    frontPages = listOf(sigEndPage, sigStartPage),
                    backPages = listOf(sigStartPage + 1, sigEndPage - 1),
                    frontOrientations = listOf(PageHeadOrientation.HEAD_UP_0, PageHeadOrientation.HEAD_UP_0),
                    backOrientations = listOf(PageHeadOrientation.HEAD_UP_0, PageHeadOrientation.HEAD_UP_0)
                )
            }
            8 -> {
                // 8pp: 2 cols x 2 rows (Head-to-Head fold)
                // Front: Top: 5, 4 (Head Down). Bottom: 8, 1 (Head Up).
                // Back:  Top: 3, 6 (Head Down). Bottom: 2, 7 (Head Up).
                val p1 = sigStartPage
                val p2 = sigStartPage + 1
                val p3 = sigStartPage + 2
                val p4 = sigStartPage + 3
                val p5 = sigStartPage + 4
                val p6 = sigStartPage + 5
                val p7 = sigStartPage + 6
                val p8 = sigEndPage

                SignatureSchemeResult(
                    frontPages = listOf(p5, p4, p8, p1),
                    backPages = listOf(p3, p6, p2, p7),
                    frontOrientations = listOf(
                        PageHeadOrientation.HEAD_DOWN_180, PageHeadOrientation.HEAD_DOWN_180,
                        PageHeadOrientation.HEAD_UP_0, PageHeadOrientation.HEAD_UP_0
                    ),
                    backOrientations = listOf(
                        PageHeadOrientation.HEAD_DOWN_180, PageHeadOrientation.HEAD_DOWN_180,
                        PageHeadOrientation.HEAD_UP_0, PageHeadOrientation.HEAD_UP_0
                    )
                )
            }
            16 -> {
                // 16pp standard right-angle book fold: 4 cols x 2 rows
                // Front (Outer):
                // Top row (Head Down): 5, 12, 9, 8
                // Bottom row (Head Up): 4, 13, 16, 1
                // Back (Inner):
                // Top row (Head Down): 7, 10, 11, 6
                // Bottom row (Head Up): 2, 15, 14, 3
                val base = sigStartPage - 1
                val fPages = listOf(
                    base + 5, base + 12, base + 9, base + 8,
                    base + 4, base + 13, base + 16, base + 1
                )
                val bPages = listOf(
                    base + 7, base + 10, base + 11, base + 6,
                    base + 2, base + 15, base + 14, base + 3
                )
                val fOrients = listOf(
                    PageHeadOrientation.HEAD_DOWN_180, PageHeadOrientation.HEAD_DOWN_180, PageHeadOrientation.HEAD_DOWN_180, PageHeadOrientation.HEAD_DOWN_180,
                    PageHeadOrientation.HEAD_UP_0, PageHeadOrientation.HEAD_UP_0, PageHeadOrientation.HEAD_UP_0, PageHeadOrientation.HEAD_UP_0
                )
                val bOrients = listOf(
                    PageHeadOrientation.HEAD_DOWN_180, PageHeadOrientation.HEAD_DOWN_180, PageHeadOrientation.HEAD_DOWN_180, PageHeadOrientation.HEAD_DOWN_180,
                    PageHeadOrientation.HEAD_UP_0, PageHeadOrientation.HEAD_UP_0, PageHeadOrientation.HEAD_UP_0, PageHeadOrientation.HEAD_UP_0
                )
                SignatureSchemeResult(fPages, bPages, fOrients, bOrients)
            }
            32 -> {
                // 32pp: 4 cols x 4 rows
                val total = pagesPerSignature
                val base = sigStartPage - 1
                val fPages = mutableListOf<Int>()
                val bPages = mutableListOf<Int>()
                val fOrients = mutableListOf<PageHeadOrientation>()
                val bOrients = mutableListOf<PageHeadOrientation>()

                // Populate standard 32pp permutation pairing
                for (i in 0 until 16) {
                    val pageFront = if (i % 2 == 0) base + (total - i) else base + (i + 1)
                    val pageBack = if (i % 2 == 0) base + (i + 2) else base + (total - i - 1)
                    fPages.add(pageFront)
                    bPages.add(pageBack)
                    val orient = if ((i / 4) % 2 == 0) PageHeadOrientation.HEAD_DOWN_180 else PageHeadOrientation.HEAD_UP_0
                    fOrients.add(orient)
                    bOrients.add(orient)
                }

                SignatureSchemeResult(fPages, bPages, fOrients, bOrients)
            }
            else -> {
                // Default fallback pairing
                val pages = (sigStartPage..sigEndPage).toList()
                val half = pages.size / 2
                val fPages = pages.take(half)
                val bPages = pages.drop(half)
                val fOrients = List(half) { PageHeadOrientation.HEAD_UP_0 }
                val bOrients = List(half) { PageHeadOrientation.HEAD_UP_0 }
                SignatureSchemeResult(fPages, bPages, fOrients, bOrients)
            }
        }
    }

    private data class SignatureSchemeResult(
        val frontPages: List<Int>,
        val backPages: List<Int>,
        val frontOrientations: List<PageHeadOrientation>,
        val backOrientations: List<PageHeadOrientation>
    )

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
