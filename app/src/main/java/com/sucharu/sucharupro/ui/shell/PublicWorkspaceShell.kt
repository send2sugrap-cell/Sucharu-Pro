package com.sucharu.sucharupro.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sucharu.sucharupro.ui.customer.components.MobileTopBar
import com.sucharu.sucharupro.ui.customer.wall.SucharuWallScreen
import com.sucharu.sucharupro.ui.customer.wall.SucharuWallViewModel
import com.sucharu.sucharupro.ui.navigation.AppDestination

import com.sucharu.sucharupro.ui.features.category.PrintingServicesScreen
import com.sucharu.sucharupro.ui.features.category.ProductsScreen

/**
 * Clean Light-Theme Public Workspace Shell.
 *
 * Replaces top dark horizontal tab bar with a standard MobileTopBar with Back button
 * that reliably returns to Home / SucharuWallScreen.
 */
@Composable
fun PublicWorkspaceShell(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    isDemoMode: Boolean = false,
    onTryDemo: (() -> Unit)? = null
) {
    if (currentDestination is AppDestination.Public.Home) {
        SucharuWallScreen(
            viewModel = viewModel { SucharuWallViewModel() },
            principal = null,
            onNavigateToDestination = onNavigate,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FA))
        ) {
            when (currentDestination) {
                is AppDestination.Public.PrintingServices -> {
                    val key = (currentDestination as? AppDestination.Public.PrintingServices)?.categoryKey ?: "ALL"
                    PrintingServicesScreen(categoryKey = key, onNavigate = onNavigate)
                }
                is AppDestination.Public.Products -> {
                    val key = (currentDestination as? AppDestination.Public.Products)?.categoryKey ?: "ALL"
                    ProductsScreen(categoryKey = key, onNavigate = onNavigate)
                }
                is AppDestination.Public.Offers -> PublicOffersView(onNavigate = onNavigate)
                is AppDestination.Public.Portfolio -> PublicGalleryView(onNavigate = onNavigate)
                is AppDestination.Public.About -> PublicAboutView(onNavigate = onNavigate)
                is AppDestination.Public.Faq -> PublicFaqView(onNavigate = onNavigate)
                is AppDestination.Public.Contact, is AppDestination.Public.Location -> PublicContactView(onNavigate = onNavigate)
                is AppDestination.Public.PublicAiAssistant -> PublicAiAssistantView(onNavigate = onNavigate)
                else -> SucharuWallScreen(
                    viewModel = viewModel { SucharuWallViewModel() },
                    principal = null,
                    onNavigateToDestination = onNavigate
                )
            }
        }
    }
}

@Composable
private fun PublicServicesView(onNavigate: (AppDestination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        MobileTopBar(
            title = "প্রিন্টিং সার্ভিসেস",
            onBackClick = { onNavigate(AppDestination.Public.Home) }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ServiceDetailCard("অফসেট প্রিন্টিং (Offset Printing)", "কমার্শিয়াল ক্যাটালগ, ব্রোশিওর, বই, ও লিফলেট প্রিমিয়াম অফসেট প্রিন্টিং।", Icons.Default.Print, Color(0xFFE8F5E9), Color(0xFF2E7D32))
            ServiceDetailCard("ডিজিটাল প্রিন্টিং (Digital Printing)", "দ্রুততম সময়ে সেম-ডে ডেলিভারি ও শর্ট-রান কাস্টম ডিজিটাল প্রিন্টিং।", Icons.Default.Description, Color(0xFFE3F2FD), Color(0xFF1565C0))
            ServiceDetailCard("কাস্টম প্যাকেজিং (Packaging Box)", "রিজিড বক্স, পেপার কার্টুন, ও লাক্সারি ডাই-কাট প্যাকেজিং বক্সেস।", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Color(0xFFEF6C00))
            ServiceDetailCard("ব্যানার ও সাইনেজ (Large Banners)", "বড় সাইজের পিভিসি ব্যানার, এক্স-স্ট্যান্ড, মেটালিক ৩ডি লেটার।", Icons.Default.Star, Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        }
    }
}

@Composable
private fun ServiceDetailCard(
    title: String,
    description: String,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bgColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, fontSize = 12.sp, color = Color(0xFF607D8B))
            }
        }
    }
}

@Composable
private fun PublicProductsView(onNavigate: (AppDestination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        MobileTopBar(
            title = "জনপ্রিয় প্রোডাক্টস",
            onBackClick = { onNavigate(AppDestination.Public.Home) }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ServiceDetailCard("ভিজিটিং কার্ড (Business Cards)", "৩০০ জিএসএম আর্ট কার্ড, ম্যাট/গ্লস স্পট ইউভি ল্যামিনেশন।", Icons.Default.AccountBox, Color(0xFFE0F7FA), Color(0xFF00838F))
            ServiceDetailCard("ব্রোশিওর ও ফ্লায়ার (Brochures)", "ফুল কালার আর্ট পেপার অফসেট প্রিমিয়াম প্রিন্টিং।", Icons.Default.Book, Color(0xFFFBE9E7), Color(0xFFD84315))
            ServiceDetailCard("রিজিড প্যাকেজিং বক্স (Rigid Boxes)", "শক্ত লাক্সারি গিফট ও প্রোডাক্ট প্যাকেজিং বক্স।", Icons.Default.Home, Color(0xFFEFEBE9), Color(0xFF4E342E))
            ServiceDetailCard("ট্যাগ ও স্টিকার (Tags & Stickers)", "প্রোডাক্ট হ্যাং ট্যাগ, ট্রান্সপারেন্ট ও মেটালিক স্টিকার।", Icons.Default.CheckCircle, Color(0xFFEDE7F6), Color(0xFF512DA8))
            ServiceDetailCard("চালান ও মেমো বই (Memo Books)", "২/৩ পার্ট এনসিআর ডুপ্লিকেট চালান ও মেমো বই।", Icons.Default.List, Color(0xFFE8EAF6), Color(0xFF283593))
            ServiceDetailCard("৩ডি সাইন লেটার (3D Signage)", "এক্রিলিক ও এসএস মেটালিক আলোকিত ৩ডি সাইনেজ।", Icons.Default.Build, Color(0xFFFFF8E1), Color(0xFFF57F17))
        }
    }
}

@Composable
private fun PublicOffersView(onNavigate: (AppDestination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        MobileTopBar(
            title = "স্পেশাল অফার্স",
            onBackClick = { onNavigate(AppDestination.Public.Home) }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ServiceDetailCard("ই ঈদ বিশেষ ছাড় - ১৫% OFF", "কাস্টম ভিজিটিং কার্ড ও অফসেট প্যাকেজিং-এ বিশেষ ডিসকাউন্ট।", Icons.Default.Star, Color(0xFFFFF3E0), Color(0xFFE65100))
            ServiceDetailCard("বাল্ক প্যাকেজিং ডিল - ১০% OFF", "৫,০০০০+ ইউনিট প্যাকেজিং অর্ডারে ফ্রি ডিজাইন ও ডেলিভারি।", Icons.Default.ShoppingCart, Color(0xFFE8F5E9), Color(0xFF2E7D32))
        }
    }
}

@Composable
private fun PublicGalleryView(onNavigate: (AppDestination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        MobileTopBar(
            title = "পোর্টফোলিও গ্যালারি",
            onBackClick = { onNavigate(AppDestination.Public.Home) }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("পোর্টফোলিও গ্যালারি", fontWeight = FontWeight.Bold, color = Color(0xFF263238), fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("সুচারু গ্রাফিক্সের সেরা প্রিন্টিং ও প্যাকেজিং কাজের নমুনা।", color = Color(0xFF607D8B), fontSize = 12.sp)
        }
    }
}

@Composable
private fun PublicAboutView(onNavigate: (AppDestination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        MobileTopBar(
            title = "আমাদের সম্পর্কে",
            onBackClick = { onNavigate(AppDestination.Public.Home) }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SUCHARU GRAPHICS", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "সুচারু গ্রাফিক্স একটি শীর্ষস্থানীয় কমার্শিয়াল প্রিন্টিং ও কাস্টম প্যাকেজিং প্রতিষ্ঠান। আধুনিক অফসেট ও ডিজিটাল প্রিন্টিং প্রযুক্তি ব্যবহার করে আমরা আন্তর্জাতিক মানের পিন্ট সেবা প্রদান করে থাকি।",
                        color = Color(0xFF263238), fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicFaqView(onNavigate: (AppDestination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        MobileTopBar(
            title = "সাধারণ জিজ্ঞাসা (FAQ)",
            onBackClick = { onNavigate(AppDestination.Public.Home) }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ServiceDetailCard("প্রশ্ন: সর্বনিম্ন কত কপি অর্ডার করা যাবে?", "উত্তর: ডিজিটাল প্রিন্টিং-এ ১ কপি এবং অফসেট প্রিন্টিং-এ ৫০০ কপি সর্বনিম্ন অর্ডার।", Icons.Default.CheckCircle, Color(0xFFE3F2FD), Color(0xFF1565C0))
            ServiceDetailCard("প্রশ্ন: ডেলিভারি কত দিনে পাওয়া যায়?", "উত্তর: ডিজাইন এপ্রুভালের পর ২-৪ কর্মদিবসের মধ্যে ডেলিভারি দেওয়া হয়।", Icons.Default.CheckCircle, Color(0xFFE8F5E9), Color(0xFF2E7D32))
        }
    }
}

@Composable
private fun PublicContactView(onNavigate: (AppDestination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        MobileTopBar(
            title = "যোগাযোগ করুন",
            onBackClick = { onNavigate(AppDestination.Public.Home) }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ServiceDetailCard("ঠিকানা", "১২৩ প্রিন্টিং স্ট্রিট, মতিঝিল বাণিজ্যিক এলাকা, ঢাকা-১০০০", Icons.Default.LocationOn, Color(0xFFE0F7FA), Color(0xFF00838F))
            ServiceDetailCard("হটলাইন সাপোর্ট", "+৮৮০ ১৭০০-০০০০০ (সকাল ৯টা - রাত ৮টা)", Icons.Default.Phone, Color(0xFFDCEDC8), Color(0xFF33691E))
            ServiceDetailCard("ইমেইল", "info@sucharu.com", Icons.Default.Email, Color(0xFFFFCDD2), Color(0xFFC62828))
        }
    }
}

@Composable
private fun PublicAiAssistantView(onNavigate: (AppDestination) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        MobileTopBar(
            title = "স্মার্ট AI সহকারী",
            onBackClick = { onNavigate(AppDestination.Public.Home) }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("স্মার্ট AI সহকারী", fontWeight = FontWeight.Bold, color = Color(0xFF263238), fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("প্রিন্টিং এর খরচ, জিএসএম পেপার কোয়ালিটি ও যেকোনো তথ্যের জন্য এআই এর সাথে চ্যাট করুন।", color = Color(0xFF607D8B), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED), contentColor = Color.White)
            ) {
                Text("চ্যাট শুরু করুন")
            }
        }
    }
}
