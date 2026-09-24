package com.sucharu.sucharupro.ui.customer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Gallery Template Item Model.
 */
data class GalleryTemplateItem(
    val templateId: String,
    val templateCode: String,
    val title: String,
    val specsGsm: String,
    val estimatedPrice: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconColor: Color
)

/**
 * Universal Product Gallery & Sample Showcase Screen.
 *
 * Displays a 2-Column M3 Grid of design templates tailored to [categoryId] / [categoryTitle].
 * Clicking any template opens a preview dialog with "এই ডিজাইনটি অর্ডার করুন" CTA opening [CustomerOrderFormSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductGalleryScreen(
    categoryId: String = "ALL",
    categoryTitle: String = "ডিজাইন গ্যালারি",
    onNavigate: (AppDestination) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (categoryTitle.contains("বিশেষ অফার") || categoryId.equals("OFFERS", ignoreCase = true) || categoryId.equals("Offers", ignoreCase = true)) {
        SpecialOfferGalleryScreen(
            onNavigateBack = { onNavigate(AppDestination.Public.Home) },
            onOrderClick = { item -> onNavigate(AppDestination.Customer.Quotations) },
            onCustomizeClick = { item -> onNavigate(AppDestination.Public.ProductGallery(item.templateId, item.title)) },
            onCustomOrderClick = { onNavigate(AppDestination.Customer.Quotations) },
            modifier = modifier
        )
        return
    }
    var selectedTemplateForPreview by remember { mutableStateOf<GalleryTemplateItem?>(null) }
    var selectedTemplateForOrderForm by remember { mutableStateOf<GalleryTemplateItem?>(null) }
    var showCustomOrderForm by remember { mutableStateOf(false) }
    var confirmedOrderResult by remember { mutableStateOf<OrderSubmissionResult?>(null) }

    val templates = remember(categoryId) { getMockGalleryTemplates(categoryId, categoryTitle) }

    CustomerTheme(colors = com.sucharu.sucharupro.ui.customer.theme.CustomerColors.light()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FA))
        ) {
            // 1. Top Bar with Back Arrow
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onNavigate(AppDestination.Public.Home) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF263238)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$categoryTitle ডিজাইন গ্যালারি",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. 2-Column Responsive LazyVerticalGrid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(templates) { template ->
                        GalleryTemplateCard(
                            item = template,
                            onClick = { selectedTemplateForPreview = template }
                        )
                    }
                }
            }

            // 3. Persistent Bottom Action Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showCustomOrderForm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কাস্টম রিকোয়েস্ট / নতুন অর্ডার করুন (Order Now)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Template Detail Preview Dialog
        if (selectedTemplateForPreview != null) {
            val template = selectedTemplateForPreview!!
            Dialog(onDismissRequest = { selectedTemplateForPreview = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large Preview Icon Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(template.bgColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = template.icon,
                                    contentDescription = template.title,
                                    tint = template.iconColor,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = template.iconColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = template.templateCode,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = template.iconColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = template.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF263238),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = template.specsGsm,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF6F8FA), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "আনুমানিক প্রাইস:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(text = template.estimatedPrice, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val current = template
                                selectedTemplateForPreview = null
                                selectedTemplateForOrderForm = current
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "এই ডিজাইনটি অর্ডার করুন",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Customer Order Placement Form Sheet
        if (selectedTemplateForOrderForm != null || showCustomOrderForm) {
            CustomerOrderFormSheet(
                templateItem = selectedTemplateForOrderForm,
                categoryTitle = categoryTitle,
                onDismiss = {
                    selectedTemplateForOrderForm = null
                    showCustomOrderForm = false
                },
                onOrderConfirmed = { result ->
                    selectedTemplateForOrderForm = null
                    showCustomOrderForm = false
                    confirmedOrderResult = result
                }
            )
        }

        // Order Confirmation Success Dialog
        if (confirmedOrderResult != null) {
            OrderSuccessConfirmationDialog(
                result = confirmedOrderResult!!,
                onGoToOrders = {
                    confirmedOrderResult = null
                    onNavigate(AppDestination.Customer.Orders)
                },
                onDismiss = { confirmedOrderResult = null }
            )
        }
    }
}

@Composable
private fun GalleryTemplateCard(
    item: GalleryTemplateItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 165.dp)
            .wrapContentHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(item.bgColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.templateCode,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.iconColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.specsGsm,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.estimatedPrice,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getMockGalleryTemplates(categoryId: String, categoryTitle: String): List<GalleryTemplateItem> {
    return listOf(
        GalleryTemplateItem("TMPL-101", "#TMPL-101", "$categoryTitle প্রিমিয়াম নমুনা ১", "300GSM Art Card • Soft Matte Finish", "১,২০০ ৳ / ১০০০ পিস", Icons.Default.Print, Color(0xFFE8F5E9), Color(0xFF2E7D32)),
        GalleryTemplateItem("TMPL-102", "#TMPL-102", "$categoryTitle লাক্সারি ডিজাইন ২", "350GSM Board • Raised Spot UV", "১,৮৫০ ৳ / ১০০০ পিস", Icons.Default.Star, Color(0xFFEDE7F6), Color(0xFF512DA8)),
        GalleryTemplateItem("TMPL-103", "#TMPL-103", "$categoryTitle ক্লাসিক স্টাইল ৩", "250GSM Glossy Art Paper • Full Color", "৮৫০ ৳ / ৫০০ পিস", Icons.Default.Description, Color(0xFFE3F2FD), Color(0xFF1565C0)),
        GalleryTemplateItem("TMPL-104", "#TMPL-104", "$categoryTitle মেটালিক গোল্ড ফয়েল ৪", "350GSM Board • Gold Foil Embossing", "২,৪০০ ৳ / ১০০০ পিস", Icons.Default.CheckCircle, Color(0xFFFFF8E1), Color(0xFFF57F17)),
        GalleryTemplateItem("TMPL-105", "#TMPL-105", "$categoryTitle কাস্টম ডাই-কাট ৫", "300GSM Duplex Board • Precision Cut", "১,৫০০ ৳ / ৫০০ পিস", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Color(0xFFEF6C00)),
        GalleryTemplateItem("TMPL-106", "#TMPL-106", "$categoryTitle মিনিমালিস্ট লুক ৬", "300GSM Imported Linen Board", "১,৩৫০ ৳ / ১০০০ পিস", Icons.Default.ThumbUp, Color(0xFFF1F8E9), Color(0xFF33691E))
    )
}
