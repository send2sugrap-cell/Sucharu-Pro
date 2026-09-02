package com.sucharu.sucharupro.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Enhanced Public Workspace Shell providing navigation between Home, Services, Products, etc.
 */
@Composable
fun PublicWorkspaceShell(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
    isDemoMode: Boolean = false,
    onTryDemo: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
    ) {
        // Navigation Menu (Scrollable Row for mobile)
        ScrollableTabRow(
            selectedTabIndex = getTabIndex(currentDestination),
            containerColor = Color(0xFF1C2541),
            contentColor = Color(0xFF9ECAFF),
            edgePadding = 16.dp,
            divider = {}
        ) {
            PublicNavItem("HOME", currentDestination == AppDestination.Public.Home) { onNavigate(AppDestination.Public.Home) }
            PublicNavItem("SERVICES", currentDestination == AppDestination.Public.PrintingServices) { onNavigate(AppDestination.Public.PrintingServices) }
            PublicNavItem("PRODUCTS", currentDestination == AppDestination.Public.Products) { onNavigate(AppDestination.Public.Products) }
            PublicNavItem("OFFERS", currentDestination == AppDestination.Public.Offers) { onNavigate(AppDestination.Public.Offers) }
            PublicNavItem("GALLERY", currentDestination == AppDestination.Public.Portfolio) { onNavigate(AppDestination.Public.Portfolio) }
            PublicNavItem("ABOUT", currentDestination == AppDestination.Public.About) { onNavigate(AppDestination.Public.About) }
            PublicNavItem("FAQ", currentDestination == AppDestination.Public.Faq) { onNavigate(AppDestination.Public.Faq) }
            PublicNavItem("CONTACT", currentDestination == AppDestination.Public.Contact) { onNavigate(AppDestination.Public.Contact) }
            PublicNavItem("AI ASSISTANT", currentDestination == AppDestination.Public.PublicAiAssistant) { onNavigate(AppDestination.Public.PublicAiAssistant) }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentDestination) {
                is AppDestination.Public.Home -> PublicHomeView(isDemoMode = isDemoMode, onTryDemo = onTryDemo)
                is AppDestination.Public.PrintingServices -> PublicServicesView()
                is AppDestination.Public.Products -> PublicProductsView()
                is AppDestination.Public.Offers -> PublicOffersView()
                is AppDestination.Public.Portfolio -> PublicGalleryView()
                is AppDestination.Public.About -> PublicAboutView()
                is AppDestination.Public.Faq -> PublicFaqView()
                is AppDestination.Public.Contact, is AppDestination.Public.Location -> PublicContactView()
                is AppDestination.Public.PublicAiAssistant -> PublicAiAssistantView()
                else -> PublicHomeView(isDemoMode = isDemoMode, onTryDemo = onTryDemo)
            }
        }
    }
}

@Composable
private fun PublicNavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
    )
}

private fun getTabIndex(dest: AppDestination): Int {
    return when (dest) {
        AppDestination.Public.Home -> 0
        AppDestination.Public.PrintingServices -> 1
        AppDestination.Public.Products -> 2
        AppDestination.Public.Offers -> 3
        AppDestination.Public.Portfolio -> 4
        AppDestination.Public.About -> 5
        AppDestination.Public.Faq -> 6
        AppDestination.Public.Contact -> 7
        AppDestination.Public.PublicAiAssistant -> 8
        else -> 0
    }
}

@Composable
private fun PublicHomeView(
    isDemoMode: Boolean = false,
    onTryDemo: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val cardBorderGradient = Brush.horizontalGradient(
            colors = listOf(Color(0xFF0061A4), Color(0xFF9ECAFF), Color(0xFF0061A4))
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .border(1.dp, cardBorderGradient, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541).copy(alpha = 0.9f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SUCHARU GRAPHICS", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), letterSpacing = 2.sp)
                Text("Commercial Printing & Creative Packaging Ecosystem", fontSize = 12.sp, color = Color(0xFFB7C8D8), modifier = Modifier.padding(top = 4.dp))
            }
        }

        // DEVELOPMENT DEMO MODE ENTRY POINT (ONLY RENDERED WHEN isDemoMode == true)
        if (isDemoMode && onTryDemo != null) {
            val demoBorder = Brush.horizontalGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF38BDF8))
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clickable { onTryDemo() }
                    .border(1.5.dp, demoBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Try Demo",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF0284C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "DEV DEMO",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TRY DEMO",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Explore customer workspace, order tracking, and live print pipeline.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        PublicFeatureSection("PREMIUM PRINTING SERVICES", listOf(
            "Offset Printing" to "High-volume commercial catalog, brochure, and magazine printing.",
            "Digital & Variable Print" to "Fast turnaround short-run custom printing.",
            "Custom Packaging" to "Rigid boxes, corrugated boxes, and luxury packaging."
        ))

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Brush.linearGradient(listOf(Color(0xFF934B00), Color(0xFFFFB77C))), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF301400).copy(alpha = 0.8f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("INTEGRATED AI ASSISTANT", fontWeight = FontWeight.Bold, color = Color(0xFFFFB77C), fontSize = 14.sp)
                Text("Ask about paper GSM, finishing types (Spot UV, Embossing), and custom quotes.", color = Color(0xFFFFDCC2), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun PublicFeatureSection(title: String, items: List<Pair<String, String>>) {
    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), modifier = Modifier.padding(bottom = 12.dp))
    items.forEach { (name, desc) ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541).copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text(text = desc, color = Color(0xFFB7C8D8), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun PublicServicesView() {
    PublicSimpleListView("OUR SERVICES", listOf(
        "Digital Printing", "Offset Printing", "Large Format Banners", "Creative Design", "Packaging Solutions", "Corporate Branding"
    ))
}

@Composable
private fun PublicProductsView() {
    PublicSimpleListView("FEATURED PRODUCTS", listOf(
        "Business Cards", "Flyers & Brochures", "Product Labels", "Rigid Packaging Boxes", "Hanging Tags", "Invoices & Challan Books"
    ))
}

@Composable
private fun PublicOffersView() {
    PublicSimpleListView("CURRENT OFFERS", listOf(
        "First Order Discount - 10% OFF", "Bulk Packaging Deal - 15% OFF", "Free Delivery on Orders above 5000 TK"
    ))
}

@Composable
private fun PublicGalleryView() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF9ECAFF), modifier = Modifier.size(64.dp))
        Text("PORTFOLIO GALLERY", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))
        Text("Showcasing our best commercial and creative works.", color = Color(0xFFB7C8D8), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun PublicAboutView() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("ABOUT SUCHARU GRAPHICS", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 18.sp)
        Text(
            text = "Sucharu Graphics is a leading commercial printing and creative packaging provider. We specialize in high-quality offset and digital printing solutions for businesses of all sizes.\n\nOur mission is to deliver excellence in every print, combining traditional craftsmanship with modern technology.",
            color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun PublicFaqView() {
    PublicSimpleListView("FREQUENTLY ASKED QUESTIONS", listOf(
        "Q: What is the minimum order quantity?\nA: It depends on the product. For digital print, it's 1 unit. For offset, it's usually 500-1000.",
        "Q: How long does delivery take?\nA: Standard delivery takes 3-5 business days after artwork approval.",
        "Q: Can I get a custom quote?\nA: Yes, use our AI Assistant or contact us directly for custom specifications."
    ))
}

@Composable
private fun PublicContactView() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("CONTACT US", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        ContactItem(Icons.Default.LocationOn, "123 Printing Street, Motijheel, Dhaka")
        ContactItem(Icons.Default.Phone, "+880 1234 567890")
        ContactItem(Icons.Default.Email, "info@sucharu.com")
        ContactItem(Icons.Default.Language, "www.sucharu.com")
    }
}

@Composable
private fun ContactItem(icon: ImageVector, text: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF9ECAFF), modifier = Modifier.size(24.dp))
        Text(text, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun PublicAiAssistantView() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFB77C), modifier = Modifier.size(64.dp))
        Text("PUBLIC AI ASSISTANT", fontWeight = FontWeight.Bold, color = Color(0xFFFFB77C), fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))
        Text("Our AI agent is ready to help with your printing queries.", color = Color(0xFFFFDCC2), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = {}, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB77C))) {
            Text("START CHATTING")
        }
    }
}

@Composable
private fun PublicSimpleListView(title: String, items: List<String>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        items.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541).copy(alpha = 0.6f))
            ) {
                Text(item, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
