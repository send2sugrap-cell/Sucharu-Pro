package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SafetyCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.ui.navigation.AppDestination

data class MasterModuleCardItem(
    val code: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val destination: AppDestination,
    val accentColor: Color = Color(0xFF00B4D8)
)

/**
 * 24-Module Master ERP Control Center Grid Widget.
 *
 * Provides 1-tap direct navigation access to all Modules 00–24.
 */
@Composable
fun AdminMasterModulesGridCard(
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    val modules = listOf(
        MasterModuleCardItem("M00", "কনফিগারেশন", "সিস্টেম আইসোলেশন", Icons.Default.Tune, AppDestination.Admin.Configuration),
        MasterModuleCardItem("M01", "ব্যবহারকারী ও সিকিউরিটি", "অ্যাক্সেস পারমিশন", Icons.Default.Security, AppDestination.Admin.Users),
        MasterModuleCardItem("M02", "কাস্টমার সিআরএম", "গ্রাহক তথ্য ও লিমিট", Icons.Default.Store, AppDestination.Admin.Users),
        MasterModuleCardItem("M03", "কোটেশন ও অর্ডার", "কমার্শিয়াল ইনটেক", Icons.Default.ShoppingCart, AppDestination.Customer.Quotations),
        MasterModuleCardItem("M04", "প্রোডাকশন প্ল্যানিং", "১৩-স্টেপ জব কার্ড", Icons.Default.Engineering, AppDestination.Staff.Production),
        MasterModuleCardItem("M05", "ডিজাইন ও প্রিপ্রেস", "প্রুফ ও ফাইল অনুমোদন", Icons.Default.Palette, AppDestination.Admin.PrepressOrchestration),
        MasterModuleCardItem("M06", "সিটিপি প্লেট আউটপুট", "এক্সপোজার ও কিউসি", Icons.Default.FactCheck, AppDestination.Staff.CtpOutput),
        MasterModuleCardItem("M07", "তৈরি পণ্য ইনভেন্টরি", "ওয়ারহাউস স্টক", Icons.Default.Inventory, AppDestination.Staff.Inventory),
        MasterModuleCardItem("M08", "চালান ও ডেলিভারি", "লজিস্টিকস ডিসপ্যাচ", Icons.Default.LocalShipping, AppDestination.Staff.Delivery),
        MasterModuleCardItem("M09", "কাস্টমার অ্যাকাউন্টস", "ইনভয়েসিং ও রিসিভেবল", Icons.Default.AccountBalance, AppDestination.Admin.Finance),
        MasterModuleCardItem("M10", "সিস্টেম অ্যালার্ট", "জরুরী নোটিফিকেশন", Icons.Default.Category, AppDestination.Admin.Notifications),
        MasterModuleCardItem("M11", "প্রুফ অফ ডেলিভারি", "ডিজিটাল সাইন ও কনফার্ম", Icons.Default.CheckCircle, AppDestination.Staff.Delivery),
        MasterModuleCardItem("M12", "রিটার্ন গুডস (RMA)", "ড্যামেজ ইন্সপেকশন", Icons.Default.ReceiptLong, AppDestination.Admin.Finance),
        MasterModuleCardItem("M13", "বিজনেস লেজার", "ডাবল-এন্ট্রি জার্নাল", Icons.Default.MonetizationOn, AppDestination.Admin.Finance),
        MasterModuleCardItem("M14", "অটো বিলিং জেনারেটর", "ট্যাক্স/ভ্যাট ইনভয়েস", Icons.Default.ConfirmationNumber, AppDestination.Admin.Finance),
        MasterModuleCardItem("M15", "জব কস্টিং ও রেট কার্ড", "কাগজ ও কালি খরচ", Icons.Default.Assessment, AppDestination.Admin.ProductionJobCosting),
        MasterModuleCardItem("M16", "ইম্পোজিশন ও নেস্টিং", "গ্যাং-রান কম্বাইনিং", Icons.Default.Build, AppDestination.Staff.Imposition),
        MasterModuleCardItem("M17", "মাল্টি-লেভেল অনুমোদন", "ক্রেডিট ছাড়পত্র গভর্নেন্স", Icons.Default.VerifiedUser, AppDestination.Admin.Settings),
        MasterModuleCardItem("M18", "প্রফিটেবিলিটি ইন্টেলিজেন্স", "ওয়েস্টেজ ও কস্ট অ্যানালিটিক্স", Icons.Default.Analytics, AppDestination.Admin.Reports),
        MasterModuleCardItem("M19", "সাবস্ট্রেট রিজার্ভেশন", "কাগজ র মেটেরিয়াল অ্যালটমেন্ট", Icons.Default.Category, AppDestination.Admin.SubstrateReservation),
        MasterModuleCardItem("M20", "অ্যাফিলিয়েট পার্টনার", "রেফারেল বিক্রি ও গভর্নেন্স", Icons.Default.Campaign, AppDestination.Admin.AffiliateManagement),
        MasterModuleCardItem("M21", "মেশিন টেলিমেট্রি ও OEE", "কারখানার ওইই স্কোর", Icons.Default.PrecisionManufacturing, AppDestination.Admin.ShopFloorTracking),
        MasterModuleCardItem("M22", "ভেন্ডর ও রিপ্রেনিশমেন্ট", "পারচেজ অর্ডার ও র র মেটেরিয়াল", Icons.Default.AppRegistration, AppDestination.Admin.Finance),
        MasterModuleCardItem("M23", "অ্যাফিলিয়েট ওয়ালেট", "কমিশন উইথড্রয়াল", Icons.Default.Wallet, AppDestination.Admin.AffiliateManagement),
        MasterModuleCardItem("M24", "সিইও এক্সিকিউটিভ রিপোর্টস", "১৫ ক্যাটাগরির রিপোর্টস", Icons.Default.Dashboard, AppDestination.Admin.Reports)
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00B4D8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Dashboard, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "২৪টি ক্যানোনিকাল মডিউল কন্ট্রোল হাব",
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "মডিউল ০০ থেকে ২৪ — সরাসরি ১-ট্যাপ এক্সেস",
                            fontSize = 9.5.sp,
                            lineHeight = 11.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "২৫টি হাব >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modules.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { item ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onNavigateToDestination(item.destination) },
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF00B4D8).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.code,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = item.title,
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.subtitle,
                                                fontSize = 8.5.sp,
                                                lineHeight = 10.sp,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
