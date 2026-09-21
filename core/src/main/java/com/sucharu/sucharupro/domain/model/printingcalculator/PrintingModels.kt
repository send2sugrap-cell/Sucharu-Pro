package com.sucharu.sucharupro.domain.model.printingcalculator

import java.util.UUID

// Common Options
enum class PlateSizeOption(val displayName: String) {
    GTO("GTO (15.5\" x 20.5\")"),
    MO("MO (19\" x 25.5\")"),
    KORD("KORD (18\" x 22.5\")"),
    DOUBLE_DEMY("Double Demy (22\" x 35\")"),
    DEMY("Demy (18\" x 23\")"),
    CROWN("Crown (15\" x 20\")"),
    CUSTOM("Custom")
}

enum class ColorTypeOption(val displayName: String, val colorCount: Int) {
    ONE_COLOR("1 Color", 1),
    TWO_COLOR("2 Color", 2),
    THREE_COLOR("3 Color", 3),
    FOUR_COLOR("4 Color (CMYK)", 4),
    FOUR_PLUS_SPOT("4+Spot Color", 5)
}

enum class LaminationType(val displayName: String) {
    GLOSSY("Glossy (গ্লসি)"),
    MATT("Matt (ম্যাট)"),
    THERMAL_GLOSSY("Thermal Glossy (থার্মাল গ্লসি)"),
    THERMAL_MATT("Thermal Matt (থার্মাল ম্যাট)")
}

enum class DigitalLaminationType(val displayName: String) {
    COLD_MATT("Cold Matt (কোল্ড ম্যাট)"),
    COLD_GLOSSY("Cold Glossy (কোল্ড গ্লসি)"),
    FLOOR_LAM("Floor Lamination (ফ্লোর ল্যামিনেশন)")
}

enum class DiaryCoverCategory(val categoryName: String, val code: String) {
    SOFT_COVER("ক্যাটাগরি ক: সফট কভার (Softcover Art Card)", "A"),
    HARD_COVER("ক্যাটাগরি খ: হার্ডকভার (Printed Paper Wrapped over Board)", "B"),
    REXINE_HARDBOUND("ক্যাটাগরি গ: রেক্সিন কভার (Rexine Hardbound)", "C"),
    PREMIUM_PU_LEATHER("ক্যাটাগরি ঘ: প্রিমিয়াম পিইউ লেদার / ফোম প্যাডেড (PU Leather / Padded)", "D")
}

enum class DigitalMaterial(val displayName: String, val defaultRatePerSqFt: Double) {
    FLEX("ফ্রন্টলিট ফ্লেক্স ব্যানার (Frontlit Flex 13oz)", 18.0),
    STAR_FLEX("স্টার ফ্লেক্স (Star Flex 18oz)", 25.0),
    VINYL_MATT("ম্যাট ভিনাইল (Matt Vinyl)", 35.0),
    VINYL_GLOSSY("গ্লসি ভিনাইল (Glossy Vinyl)", 35.0),
    FROSTED("ফ্রস্টেড ভিনাইল (Frosted Vinyl)", 50.0),
    BACKLIT("বেকলিট ফিল্ম (Backlit Film)", 90.0),
    CANVAS("ক্যানভাস ফেব্রিক (Canvas Fabric)", 120.0)
}

// Sector 2: Wedding Card Options
enum class WeddingCardArchitecture(val displayName: String) {
    SINGLE_PAGE("সিঙ্গেল পেজ কার্ড (Single Page Card)"),
    BI_FOLD("বাই-ফোল্ড / ২ ভাঁজ (Bi-Fold Card)"),
    BOOKLET("বুকলেট স্টাইল (Booklet Style)"),
    PREMIUM_BOX("প্রিমিয়াম বক্স কার্ড (Premium Box Card)")
}

// Sector 6: Gift Options
enum class GiftCategoryItem(val displayName: String) {
    MUG("সিরামিক মগ (Mug)"),
    TSHIRT("টি-শার্ট (T-Shirt)"),
    CREST("ক্রেস্ট / অ্যাওয়ার্ড (Crest/Award)"),
    KEYRING("কি-রিং (Keyring)"),
    PENDRIVE("পেনড্রাইভ (Pendrive)"),
    PEN_SET("পেন সেট (Pen Set)"),
    CLOCK("মেটাল/কাঠের ক্লক (Clock)"),
    CUSTOM("কাস্টম গিফট (Custom Item)")
}

enum class GiftCustomizationMethod(val displayName: String) {
    SUBLIMATION("সাবলিমেশন ప్రింట్ (Sublimation)"),
    DTF("ডিটিএফ প্রিন্ট (DTF Printing)"),
    SCREEN_PRINT("স্ক্রিন প্রিন্টিং (Screen Print)"),
    UV_FLATBED("ইউভি ফ্ল্যাটবেড (UV Flatbed)"),
    LASER_ENGRAVE("লেজার এনগ্রেভিং (Laser Engrave)"),
    EMBROIDERY("এমব্রয়ডারি (Embroidery)")
}

// Sector 7: Seal & Stamp Options
enum class StampCategory(val displayName: String) {
    SELF_INKING("সেলফ-ইঙ্কিং স্ট্যাম্প (Flash / Dater Stamp)"),
    RUBBER_STAMP("ট্র্যাডিশনাল রাবার সিল (Traditional Rubber Stamp)"),
    ACRYLIC_LASER("লেজার এক্রিলিক সিল (Laser Acrylic Stamp)"),
    EMBOSSING_SEAL("এমবসিং সিল (Pocket / Desk Embosser)")
}

// Sector 8: Packaging Options
enum class BoxCategory(val displayName: String) {
    FOLDING_CARTON("ফোল্ডিং কার্টন (সুইট, কসমেটিক, ওষুধ বক্স)"),
    RIGID_BOX("রিজিড শক্ত বক্স (প্রিমিয়াম মোবাইল/গিফট বক্স)"),
    CORRUGATED_CARTON("কোরুগেটেড মাস্টার কার্টন (৩-প্লাই, ৫-প্লাই)")
}

// Inner Forme Group State for Book/Diary Estimators
data class InnerFormeGroupState(
    val id: String = UUID.randomUUID().toString(),
    val formaCount: Int = 1,
    val plateType: String = PlateSizeOption.GTO.displayName,
    val printingColor: String = ColorTypeOption.FOUR_COLOR.displayName,
    val cutsPerSheet: Int = 1,
    val isJuri: Boolean = false,
    val isBackToBack: Boolean = false,
    val plateRate: Double = 250.0,
    val printingRate1k: Double = 150.0,
    val printingMinCharge: Double = 300.0,
    val isLamEnabled: Boolean = false,
    val lamRate: Double = 0.002,
    val lamMin: Double = 100.0,
    val lamBothSides: Boolean = false
)

// Sector Results Data Classes

data class OffsetCostResult(
    val mainSheets: Int,
    val machineSheets: Int,
    val paperCost: Double,
    val totalPlates: Int,
    val plateCost: Double,
    val totalImpressions: Int,
    val impressionCost: Double,
    val laminationSqInches: Double,
    val laminationCost: Double,
    val dieCuttingCost: Double,
    val totalDieCost: Double,
    val totalFoilCost: Double,
    val pastingCost: Double,
    val numberingCost: Double,
    val perforationCost: Double,
    val packagingCost: Double,
    val totalBindingCost: Double,
    val totalFinishing: Double,
    val subTotal: Double,
    val profitAmount: Double,
    val grandTotal: Double,
    val perUnitCost: Double
)

data class WeddingCostResult(
    val mainCardSheets: Double,
    val insertSheetsTotal: Double,
    val cardPaperCost: Double,
    val envSheets: Double,
    val envelopeCost: Double,
    val laminationSqIn: Double,
    val laminationCost: Double,
    val foilBlockCost: Double,
    val foilRunningCost: Double,
    val embossBlockCost: Double,
    val embossRunningCost: Double,
    val dieMakingCharge: Double,
    val diePunchingCost: Double,
    val assemblyLaborCost: Double,
    val platesAndPrintCost: Double,
    val totalFinishingAndLabor: Double,
    val subTotal: Double,
    val profitAmount: Double,
    val grandTotal: Double,
    val perUnitCost: Double
)

data class DigitalCostResult(
    val totalSqFtPerUnit: Double,
    val totalSqFtAll: Double,
    val printCost: Double,
    val digitalLamCost: Double,
    val grommetCost: Double,
    val perimeterFeet: Double,
    val hemmingCost: Double,
    val boardMountCost: Double,
    val standCost: Double,
    val subTotal: Double,
    val profitAmount: Double,
    val grandTotal: Double,
    val perUnitCost: Double
)

data class InnerFormeGroupResult(
    val calculatedPlates: Int,
    val groupPlateCost: Double,
    val fullSheetsForGroup: Double,
    val machineSheets: Double,
    val calculatedImpressions: Int,
    val groupPrintCost: Double,
    val lamAreaSqIn: Double,
    val groupLamCost: Double,
    val groupTotal: Double
)

data class BookCostResult(
    val totalFormas: Double,
    val totalRequiredMainSheets: Double,
    val totalReams: Double,
    val innerPaperCost: Double,
    val formeGroupResults: List<InnerFormeGroupResult>,
    val totalInnerPlatesCost: Double,
    val totalInnerPrintCost: Double,
    val totalInnerLamCost: Double,
    val epFullSheetQty: Double,
    val epPaperCost: Double,
    val epPlateCost: Double,
    val epPrintCost: Double,
    val epLamCost: Double,
    val calculatedSpine: Double,
    val coverWidth: Double,
    val coverHeight: Double,
    val cvFullSheetQty: Double,
    val cvPaperCost: Double,
    val cvPlateCost: Double,
    val cvPrintCost: Double,
    val cvLamCost: Double,
    val sandiCost: Double,
    val spotUvCost: Double,
    val foilCost: Double,
    val embossCost: Double,
    val totalExtraFinishing: Double,
    val totalBindingCost: Double,
    val totalPackaging: Double,
    val subTotal: Double,
    val profitAmount: Double,
    val grandTotal: Double,
    val perBookCost: Double
)

data class DiaryCostResult(
    val bookBaseResult: BookCostResult,
    val boardCost: Double,
    val wrappingPaperCost: Double,
    val wrappingPlateCost: Double,
    val wrappingPrintCost: Double,
    val wrappingLamCost: Double,
    val boardPastingLaborCost: Double,
    val rexineCost: Double,
    val rexineFoilCost: Double,
    val rexineBindingLaborCost: Double,
    val foamPaddingCost: Double,
    val puLeatherCost: Double,
    val stitchingCost: Double,
    val debossCost: Double,
    val ribbonCost: Double,
    val elasticCost: Double,
    val penLoopCost: Double,
    val metalCornerCost: Double,
    val boxCost: Double,
    val diaryCoverTotal: Double,
    val accessoriesTotal: Double,
    val subTotal: Double,
    val profitAmount: Double,
    val grandTotal: Double,
    val perDiaryCost: Double
)

data class GiftCostResult(
    val baseBlankCost: Double,
    val printMethodCost: Double,
    val packagingCost: Double,
    val subTotal: Double,
    val profitAmount: Double,
    val grandTotal: Double,
    val perUnitCost: Double
)

data class StampCostResult(
    val baseCasingCost: Double,
    val plateMakingCost: Double,
    val inkPadCost: Double,
    val subTotal: Double,
    val profitAmount: Double,
    val grandTotal: Double,
    val perUnitCost: Double
)

data class PackagingCostResult(
    val flatSheetW: Double,
    val flatSheetH: Double,
    val itemsPerMachineSheet: Int,
    val totalMachineSheets: Double,
    val boxMaterialCost: Double,
    val plateCost: Double,
    val printCost: Double,
    val laminationSqIn: Double,
    val laminationCost: Double,
    val dieBlockFinalCost: Double,
    val dieRunCost: Double,
    val windowCost: Double,
    val pastingCost: Double,
    val handleCost: Double,
    val subTotal: Double,
    val profitAmount: Double,
    val grandTotal: Double,
    val perUnitCost: Double
)
