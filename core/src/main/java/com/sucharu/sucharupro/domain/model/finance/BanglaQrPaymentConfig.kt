package com.sucharu.sucharupro.domain.model.finance

/**
 * Operating Mode for Bangla QR Integration.
 */
enum class BanglaQrMode {
    STATIC,
    DYNAMIC // Reserved for future API gateway integration
}

/**
 * Banking Provider for Bangla QR.
 */
enum class BanglaQrBankProvider(val displayName: String) {
    BRAC_BANK("BRAC Bank"),
    EBL("Eastern Bank Ltd"),
    CITY_BANK("City Bank"),
    OTHER("Other Bank")
}

/**
 * Static Merchant Bangla QR Configuration Model.
 */
data class BanglaQrPaymentConfig(
    val configId: String = "CFG-BRAC-QR-01",
    val merchantName: String = "Sucharu Graphics & Printing",
    val merchantAccountNo: String = "1501200000001",
    val bracBankQrAssetPath: String = "drawable/brac_bank_bangla_qr_merchant",
    val mode: BanglaQrMode = BanglaQrMode.STATIC,
    val bankProvider: BanglaQrBankProvider = BanglaQrBankProvider.BRAC_BANK,
    val isMerchantQrConfigured: Boolean = true,
    val paymentInstructionBn: String = "আপনার ব্যাংক বা মোবাইল ব্যাংকিং অ্যাপের 'Bangla QR' দিয়ে স্ক্যান করে পেমেন্ট সম্পন্ন করুন এবং ট্রানজেকশন আইডি নিচে প্রদান করুন।"
)
