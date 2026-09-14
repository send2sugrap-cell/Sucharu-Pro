package com.sucharu.sucharupro.data.repository.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CategoryDetailsRepositoryImpl : CategoryDetailsRepository {

    override fun getCategoryServices(categoryKey: String): Flow<List<CategoryItemDetail>> = flow {
        val items = when (categoryKey.uppercase()) {
            "OFFSET", "অফসেট" -> listOf(
                CategoryItemDetail("SRV-OFF-01", "Offset", "ক্যাশ মেমো / মেমো বই", "50GSM NCR Carbonless Paper", Icons.Default.Print, Color(0xFFE8F5E9), Color(0xFF2E7D32), "৳180.00 / বই"),
                CategoryItemDetail("SRV-OFF-02", "Offset", "কমার্শিয়াল ফ্লায়ার ও হ্যান্ডবিল", "120GSM Art Paper • Full Color", Icons.Default.Description, Color(0xFFE3F2FD), Color(0xFF1565C0), "৳3.50 / Pc"),
                CategoryItemDetail("SRV-OFF-03", "Offset", "প্রমোশনাল লিফলেট", "150GSM Glossy Art Paper", Icons.Default.Star, Color(0xFFFFF3E0), Color(0xFFEF6C00), "৳4.80 / Pc"),
                CategoryItemDetail("SRV-OFF-04", "Offset", "প্রোডাক্ট ক্যাটালগ ও বুকলেট", "200GSM Inner + 300GSM Cover", Icons.Default.Book, Color(0xFFF3E5F5), Color(0xFF7B1FA2), "৳45.00 / Pc")
            )
            "DIGITAL", "ডিজিটাল" -> listOf(
                CategoryItemDetail("SRV-DIG-01", "Digital", "সেম-ডে ফাস্ট প্রিন্ট", "300GSM Matte Board • Instant", Icons.Default.Description, Color(0xFFE3F2FD), Color(0xFF1565C0), "৳12.00 / Pc"),
                CategoryItemDetail("SRV-DIG-02", "Digital", "কাস্টম পোস্টার ও সার্টিফিকেট", "250GSM Velvet Card", Icons.Default.Star, Color(0xFFFFF8E1), Color(0xFFF57F17), "৳25.00 / Pc"),
                CategoryItemDetail("SRV-DIG-03", "Digital", "শর্ট-রান রেস্টুরেন্ট মেনু", "350GSM Laminated Board", Icons.Default.Book, Color(0xFFE0F7FA), Color(0xFF00838F), "৳35.00 / Pc"),
                CategoryItemDetail("SRV-DIG-04", "Digital", "পারসোনালাইজড আইডি কার্ড", "PVC Plastic Board • Laminated", Icons.Default.AccountBox, Color(0xFFEDE7F6), Color(0xFF512DA8), "৳50.00 / Pc")
            )
            "PACKAGING", "প্যাকেজিং" -> listOf(
                CategoryItemDetail("SRV-PKG-01", "Packaging", "কাস্টম পেপার কার্টুন বক্স", "350GSM Duplex Board • Die-Cut", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Color(0xFFEF6C00), "৳18.00 / Pc"),
                CategoryItemDetail("SRV-PKG-02", "Packaging", "লাক্সারি রিজিড বক্সেস", "1200GSM Binding Board + Velvet", Icons.Default.Home, Color(0xFFEFEBE9), Color(0xFF4E342E), "৳150.00 / Pc"),
                CategoryItemDetail("SRV-PKG-03", "Packaging", "ফুড-গ্রেড টেকঅ্যাওয়ে বক্স", "300GSM Kraft Board • Food Grade", Icons.Default.CheckCircle, Color(0xFFE8F5E9), Color(0xFF2E7D32), "৳14.00 / Pc"),
                CategoryItemDetail("SRV-PKG-04", "Packaging", "ডাই-কাট গিফট প্যাক", "300GSM Art Card + Matt Lam", Icons.Default.Star, Color(0xFFFBE9E7), Color(0xFFD84315), "৳28.00 / Pc")
            )
            "BANNER", "ব্যানার" -> listOf(
                CategoryItemDetail("SRV-BAN-01", "Banner", "পিভিসি ফ্লেক্স ব্যানার", "380GSM Heavy Outdoor Flex", Icons.Default.Star, Color(0xFFF3E5F5), Color(0xFF7B1FA2), "৳18.00 / Sft"),
                CategoryItemDetail("SRV-BAN-02", "Banner", "প্রমোশনাল এক্স-স্ট্যান্ড ব্যানার", "Star Flex + Aluminum Stand", Icons.Default.Build, Color(0xFFFFF8E1), Color(0xFFF57F17), "৳650.00 / Set"),
                CategoryItemDetail("SRV-BAN-03", "Banner", "ব্যাকলিট সাইনবোর্ড ব্যানার", "Translucent PVC Flex", Icons.Default.Description, Color(0xFFE0F7FA), Color(0xFF00838F), "৳35.00 / Sft"),
                CategoryItemDetail("SRV-BAN-04", "Banner", "কাস্টম স্টিকার ব্যাকড্রপ", "Vinyl Glossy Self-Adhesive", Icons.Default.ThumbUp, Color(0xFFF1F8E9), Color(0xFF33691E), "৳25.00 / Sft")
            )
            else -> listOf(
                CategoryItemDetail("SRV-GEN-01", "All", "অফসেট প্রিন্টিং সমাধান", "কমার্শিয়াল ক্যাটালগ ও ফ্লায়ার", Icons.Default.Print, Color(0xFFE8F5E9), Color(0xFF2E7D32), "৳180.00 / বই"),
                CategoryItemDetail("SRV-GEN-02", "All", "ডিজিটাল সেম-ডে প্রিন্ট", "৩০০ জিএসএম মেট বোর্ড", Icons.Default.Description, Color(0xFFE3F2FD), Color(0xFF1565C0), "৳12.00 / Pc"),
                CategoryItemDetail("SRV-GEN-03", "All", "কাস্টম প্যাকেজিং বক্স", "৩৫০ জিএসএম ডুপ্লেক্স বোর্ড", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Color(0xFFEF6C00), "৳18.00 / Pc"),
                CategoryItemDetail("SRV-GEN-04", "All", "পিভিসি ফ্লেক্স ব্যানার", "৩৮০ জিএসএম হেভি ফ্লেক্স", Icons.Default.Star, Color(0xFFF3E5F5), Color(0xFF7B1FA2), "৳18.00 / Sft")
            )
        }
        emit(items)
    }

    override fun getCategoryProducts(categoryKey: String): Flow<List<CategoryItemDetail>> = flow {
        val items = when (categoryKey.uppercase()) {
            "CARD", "ভিজিটিং কার্ড" -> listOf(
                CategoryItemDetail("PROD-CRD-01", "Card", "ম্যাট ল্যামিনেশন কার্ড", "300GSM Art Card + Soft Matte", Icons.Default.AccountBox, Color(0xFFE0F7FA), Color(0xFF00838F), "৳1.80 / Pc"),
                CategoryItemDetail("PROD-CRD-02", "Card", "স্পট ইউভি লাক্সারি কার্ড", "350GSM Board + Raised Gloss UV", Icons.Default.Star, Color(0xFFEDE7F6), Color(0xFF512DA8), "৳3.50 / Pc"),
                CategoryItemDetail("PROD-CRD-03", "Card", "এমবসিং টেক্সচার্ড কার্ড", "300GSM Imported Linen Board", Icons.Default.CheckCircle, Color(0xFFFFF8E1), Color(0xFFF57F17), "৳4.20 / Pc"),
                CategoryItemDetail("PROD-CRD-04", "Card", "মেটালিক গোল্ড/সিলভার কার্ড", "350GSM Premium Metallic Foil", Icons.Default.ThumbUp, Color(0xFFFBE9E7), Color(0xFFD84315), "৳8.50 / Pc")
            )
            "BROCHURE", "ব্রোশিওর" -> listOf(
                CategoryItemDetail("PROD-BRC-01", "Brochure", "২-ফোল্ড কমার্শিয়াল ব্রোশিওর", "150GSM Glossy Art Paper", Icons.Default.Book, Color(0xFFFBE9E7), Color(0xFFD84315), "৳6.50 / Pc"),
                CategoryItemDetail("PROD-BRC-02", "Brochure", "৩-ফোল্ড ট্রাই-ফোল্ড ফ্লায়ার", "170GSM Matte Art Paper", Icons.Default.Description, Color(0xFFE3F2FD), Color(0xFF1565C0), "৳8.00 / Pc"),
                CategoryItemDetail("PROD-BRC-03", "Brochure", "মাল্টি-পেজ ক্যাটালগ", "200GSM Inner + Cover", Icons.Default.Book, Color(0xFFF3E5F5), Color(0xFF7B1FA2), "৳42.00 / Pc"),
                CategoryItemDetail("PROD-BRC-04", "Brochure", "প্রেস কিট ফোল্ডার", "300GSM Laminated Pocket", Icons.Default.Description, Color(0xFFE8EAF6), Color(0xFF283593), "৳35.00 / Pc")
            )
            "RIGIDBOX", "রিজিড বক্স" -> listOf(
                CategoryItemDetail("PROD-RGB-01", "RigidBox", "ম্যাগনেটিক ক্লোজার বুক বক্স", "1200GSM Board + Velvet Lining", Icons.Default.Home, Color(0xFFEFEBE9), Color(0xFF4E342E), "৳180.00 / Pc"),
                CategoryItemDetail("PROD-RGB-02", "RigidBox", "টু-পিস কাস্টম গিফট বক্স", "1000GSM Rigid Board", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Color(0xFFEF6C00), "৳140.00 / Pc"),
                CategoryItemDetail("PROD-RGB-03", "RigidBox", "স্লাইডিং ড্রয়ার বক্স", "1200GSM Kappa Board", Icons.Default.Build, Color(0xFFECEFF1), Color(0xFF455A64), "৳165.00 / Pc"),
                CategoryItemDetail("PROD-RGB-04", "RigidBox", "হেক্সাগন লাক্সারি বক্স", "1200GSM Rigid Board", Icons.Default.Star, Color(0xFFEDE7F6), Color(0xFF512DA8), "৳210.00 / Pc")
            )
            "TAG", "ট্যাগ / লেবেল" -> listOf(
                CategoryItemDetail("PROD-TAG-01", "Tag", "গার্মেন্টস হ্যাং ট্যাগ", "350GSM Board + Eyelet Punch", Icons.Default.CheckCircle, Color(0xFFEDE7F6), Color(0xFF512DA8), "৳1.50 / Pc"),
                CategoryItemDetail("PROD-TAG-02", "Tag", "কাস্টম প্রোডাক্ট ইউভি লেবেল", "Self-Adhesive Vinyl", Icons.Default.ThumbUp, Color(0xFFF1F8E9), Color(0xFF33691E), "৳2.20 / Pc"),
                CategoryItemDetail("PROD-TAG-03", "Tag", "ট্রান্সপারেন্ট পিভিসি লেবেল", "Clear Water-Proof Vinyl", Icons.Default.Star, Color(0xFFE0F7FA), Color(0xFF00838F), "৳3.00 / Pc"),
                CategoryItemDetail("PROD-TAG-04", "Tag", "বারকোড ও প্রাইস স্টিকার", "Paper Adhesive Roll", Icons.Default.Description, Color(0xFFE8EAF6), Color(0xFF283593), "৳0.80 / Pc")
            )
            "CHALLAN", "চালান বই" -> listOf(
                CategoryItemDetail("PROD-CHN-01", "Challan", "২-পার্ট ডুপ্লিকেট চালান বই", "NCR Carbonless Paper (50 Sets)", Icons.Default.Description, Color(0xFFE8EAF6), Color(0xFF283593), "৳120.00 / বই"),
                CategoryItemDetail("PROD-CHN-02", "Challan", "৩-পার্ট ট্রিপ্লিকেট ক্যাশ মেমো", "NCR Multi-Color Copies (50 Sets)", Icons.Default.Description, Color(0xFFE3F2FD), Color(0xFF1565C0), "৳150.00 / বই"),
                CategoryItemDetail("PROD-CHN-03", "Challan", "ডেলিভারি নোট বুক", "50GSM Offset Paper (100 Leaves)", Icons.Default.CheckCircle, Color(0xFFE8F5E9), Color(0xFF2E7D32), "৳90.00 / বই"),
                CategoryItemDetail("PROD-CHN-04", "Challan", "মানি রিসিট বুক", "Serial Numbered Duplicate", Icons.Default.ThumbUp, Color(0xFFFFF8E1), Color(0xFFF57F17), "৳110.00 / বই")
            )
            "3D", "৩ডি লেটার" -> listOf(
                CategoryItemDetail("PROD-3D-01", "3D", "এক্রিলিক ৩ডি ব্যাকলিট লেটার", "LED Channel Letter • Acrylic", Icons.Default.Build, Color(0xFFFFF8E1), Color(0xFFF57F17), "৳450.00 / Sft"),
                CategoryItemDetail("PROD-3D-02", "3D", "এসএস গোল্ড মেটালিক লেটার", "Mirror Finish Stainless Steel", Icons.Default.Star, Color(0xFFFBE9E7), Color(0xFFD84315), "৳580.00 / Sft"),
                CategoryItemDetail("PROD-3D-03", "3D", "নিওন সাইন বোর্ড", "Flexible LED Neon Tube", Icons.Default.ThumbUp, Color(0xFFF3E5F5), Color(0xFF7B1FA2), "৳350.00 / Sft"),
                CategoryItemDetail("PROD-3D-04", "3D", "রাউটার কাট কাস্টম লোগো", "10mm Foam PVC Board", Icons.Default.Home, Color(0xFFECEFF1), Color(0xFF455A64), "৳280.00 / Sft")
            )
            "STICKER", "স্টিকার" -> listOf(
                CategoryItemDetail("PROD-STK-01", "Sticker", "ডাই-কাট কাস্টম শেপ স্টিকার", "Glossy Waterproof Vinyl", Icons.Default.ThumbUp, Color(0xFFF1F8E9), Color(0xFF33691E), "৳2.50 / Pc"),
                CategoryItemDetail("PROD-STK-02", "Sticker", "মেটালিক ফয়েল স্টিকার", "Gold/Silver Foil Adhesive", Icons.Default.Star, Color(0xFFFFF8E1), Color(0xFFF57F17), "৳4.50 / Pc"),
                CategoryItemDetail("PROD-STK-03", "Sticker", "কার ড্রাফটিং ভিনাইল", "UV Resistant Exterior Vinyl", Icons.Default.Build, Color(0xFFE0F7FA), Color(0xFF00838F), "৳35.00 / Sft"),
                CategoryItemDetail("PROD-STK-04", "Sticker", "পেপার প্রাইস স্টিকার", "Matte Self-Adhesive Paper", Icons.Default.Description, Color(0xFFE8EAF6), Color(0xFF283593), "৳0.90 / Pc")
            )
            else -> listOf(
                CategoryItemDetail("PROD-GEN-01", "Others", "কর্পোরেট চামড়ার ডায়েরি ২০২৭", "PU Leather Hardbound", Icons.Default.Book, Color(0xFFFBE9E7), Color(0xFFD84315), "৳450.00 / Pc"),
                CategoryItemDetail("PROD-GEN-02", "Others", "৭-পাতা কমার্শিয়াল ওয়াল ক্যালেন্ডার", "170GSM Art Paper", Icons.Default.Home, Color(0xFFEFEBE9), Color(0xFF4E342E), "৳120.00 / Pc"),
                CategoryItemDetail("PROD-GEN-03", "Others", "ডেস্কটপ টেবিল ক্যালেন্ডার", "210GSM Board + Wire-O Binding", Icons.Default.Star, Color(0xFFEDE7F6), Color(0xFF512DA8), "৳160.00 / Pc"),
                CategoryItemDetail("PROD-GEN-04", "Others", "কাস্টম ব্র্যান্ডেড মগ ও কলম", "Sublimation Ceramic Mugs", Icons.Default.MoreHoriz, Color(0xFFECEFF1), Color(0xFF455A64), "৳220.00 / Set")
            )
        }
        emit(items)
    }
}
