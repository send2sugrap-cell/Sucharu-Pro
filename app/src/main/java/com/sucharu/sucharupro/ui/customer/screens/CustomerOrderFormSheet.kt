package com.sucharu.sucharupro.ui.customer.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Order Confirmation Result Payload.
 */
data class OrderSubmissionResult(
    val orderTrackingId: String,
    val templateCode: String,
    val categoryTitle: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val quantity: String,
    val customNotes: String
)

/**
 * Customer Order Placement Form BottomSheet with inline validation and direct ERP dispatch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrderFormSheet(
    templateItem: GalleryTemplateItem?,
    categoryTitle: String,
    onDismiss: () -> Unit,
    onOrderConfirmed: (result: OrderSubmissionResult) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var fullName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var customNotes by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableStateOf("1000 Pcs") }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun validateAndSubmit() {
        var isValid = true

        if (fullName.trim().isBlank()) {
            fullNameError = "আপনার পুরো নাম লিখুন"
            isValid = false
        } else {
            fullNameError = null
        }

        val cleanPhone = mobileNumber.trim()
        if (cleanPhone.isBlank() || !cleanPhone.matches(Regex("^01[3-9]\\d{8}$"))) {
            mobileError = "সঠিক ১১ ডিজিটের মোবাইল নম্বর দিন (যেমন: 01700000000)"
            isValid = false
        } else {
            mobileError = null
        }

        if (deliveryAddress.trim().isBlank()) {
            addressError = "ডেলিভারি ঠিকানা প্রদান করুন"
            isValid = false
        } else {
            addressError = null
        }

        if (isValid) {
            isLoading = true
            val generatedOrderId = "#ORD-2026-" + (1000..9999).random()
            val finalNotes = customNotes.ifBlank { "পরবর্তীতে যোগাযোগের মাধ্যমে সংগৃহীত হবে" }

            val result = OrderSubmissionResult(
                orderTrackingId = generatedOrderId,
                templateCode = templateItem?.templateCode ?: "#TMPL-CUSTOM",
                categoryTitle = categoryTitle,
                customerName = fullName.trim(),
                customerPhone = cleanPhone,
                deliveryAddress = deliveryAddress.trim(),
                quantity = selectedQuantity,
                customNotes = finalNotes
            )

            onOrderConfirmed(result)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "অর্ডার প্লেসমেন্ট ফর্ম (Direct Order Placement)",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238)
            )
            Text(
                text = "আইটেম: ${templateItem?.title ?: categoryTitle} (${templateItem?.templateCode ?: "#CUSTOM"})",
                fontSize = 12.sp,
                color = Color(0xFF0284C7),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    if (fullNameError != null) fullNameError = null
                },
                label = { Text("আপনার নাম (বাধ্যতামূলক / Required)") },
                isError = fullNameError != null,
                supportingText = {
                    if (fullNameError != null) {
                        Text(fullNameError!!, color = Color.Red, fontSize = 11.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Mobile Number
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = {
                    mobileNumber = it
                    if (mobileError != null) mobileError = null
                },
                label = { Text("মোবাইল নম্বর (বাধ্যতামূলক: 01XXXXXXXXX)") },
                isError = mobileError != null,
                supportingText = {
                    if (mobileError != null) {
                        Text(mobileError!!, color = Color.Red, fontSize = 11.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Delivery Address
            OutlinedTextField(
                value = deliveryAddress,
                onValueChange = {
                    deliveryAddress = it
                    if (addressError != null) addressError = null
                },
                label = { Text("ডেলিভারি ঠিকানা (বাধ্যতামূলক / Required)") },
                isError = addressError != null,
                supportingText = {
                    if (addressError != null) {
                        Text(addressError!!, color = Color.Red, fontSize = 11.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Quantity Selector Chips
            Text(
                text = "অর্ডার পরিমাণ সিলেক্ট করুন",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("500 Pcs", "1000 Pcs", "2500 Pcs", "5000 Pcs").forEach { qty ->
                    val isSelected = selectedQuantity == qty
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9))
                            .border(1.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .clickable { selectedQuantity = qty },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = qty,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Custom Notes (Optional)
            OutlinedTextField(
                value = customNotes,
                onValueChange = { customNotes = it },
                label = { Text("কাস্টম টেক্সট / প্রিন্টিং নির্দেশাবলী (ঐচ্ছিক)") },
                placeholder = { Text("যেমন: দোকানের নাম, লোগো টেক্সট বা পছন্দের কালার কোড") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = { validateAndSubmit() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("অর্ডার সাবমিট হচ্ছে...", fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অর্ডার নিশ্চিত করুন (Confirm Order)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Confirmation Success Dialog.
 */
@Composable
fun OrderSuccessConfirmationDialog(
    result: OrderSubmissionResult,
    onGoToOrders: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "অর্ডার সফলভাবে জমা হয়েছে!",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "অর্ডার ট্র্যাকিং আইডি: ${result.orderTrackingId}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "আপনার অর্ডারটি বাণিজ্যিক ইআরপি কিউতে জমা দেওয়া হয়েছে। আমাদের প্রতিনিধি দ্রুততম সময়ে ${result.customerPhone} নম্বরে যোগাযোগ করবেন।",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onGoToOrders()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "অর্ডার ট্র্যাকিং এ যান",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
