package com.sucharu.sucharupro.ui.customer.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sucharu.sucharupro.data.api.model.offer.OfferCheckoutCustomerInfoDto
import com.sucharu.sucharupro.data.api.model.offer.OfferCheckoutRequestDto
import com.sucharu.sucharupro.data.api.model.offer.OfferCheckoutResponseDto
import com.sucharu.sucharupro.domain.service.offer.BangladeshPhoneNormalizer
import com.sucharu.sucharupro.domain.service.offer.OfferCheckoutService

/**
 * Single-Page Mobile-First Promotional Offer Checkout Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferCheckoutSheet(
    offerId: String = "OFFER-2026-EID",
    offerTitle: String = "১০০০ মেট ফিনিশ ভিজিটিং কার্ড",
    promotionalPriceText: String = "৳৩৫০",
    offerQuantityText: String = "১,০০০ পিস",
    onDismiss: () -> Unit,
    onCheckoutSuccess: (OfferCheckoutResponseDto) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    checkoutService: OfferCheckoutService = remember { OfferCheckoutService() }
) {
    var fullName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var optionalEmail by remember { mutableStateOf("") }
    var specialInstructions by remember { mutableStateOf("") }

    // Design Option: "CUSTOMER_DESIGN" or "DESIGNER_ASSISTANCE"
    var designOption by remember { mutableStateOf("CUSTOMER_DESIGN") }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }

    // Payment Preference
    var paymentMethod by remember { mutableStateOf("COD") } // COD, BKASH, NAGAD, ROCKET, PARTIAL_ADVANCE
    var deliveryZone by remember { mutableStateOf("INSIDE_DHAKA") }

    // Validation & Loading
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Existing Customer Live Detection
    val normalizedPhone = remember(mobileNumber) { BangladeshPhoneNormalizer.normalize(mobileNumber) }
    val detectedCustomerId = remember(normalizedPhone) { checkoutService.findCustomerByPhone(normalizedPhone) }

    val basePrice = if (promotionalPriceText.contains("350") || promotionalPriceText.contains("৩৫০")) 350.0 else 800.0
    val deliveryCharge = if (deliveryZone == "OUTSIDE_DHAKA") 120.0 else 60.0
    val totalAmount = basePrice + deliveryCharge

    fun validateAndSubmit() {
        var isValid = true

        if (fullName.trim().isBlank()) {
            fullNameError = "আপনার পুরো নাম লিখুন"
            isValid = false
        } else {
            fullNameError = null
        }

        val cleanPhone = mobileNumber.trim()
        if (cleanPhone.isBlank() || !cleanPhone.matches(Regex("^(?:\\+?88)?01[3-9]\\d{8}$"))) {
            mobileError = "সঠিক মোবাইল নম্বর দিন (যেমন: 01700000000)"
            isValid = false
        } else {
            mobileError = null
        }

        if (deliveryAddress.trim().isBlank()) {
            addressError = "সম্পূর্ণ ডেলিভারি ঠিকানা প্রদান করুন"
            isValid = false
        } else {
            addressError = null
        }

        if (isValid) {
            isLoading = true

            val request = OfferCheckoutRequestDto(
                offerId = offerId,
                offerTitle = offerTitle,
                customer = OfferCheckoutCustomerInfoDto(
                    displayName = fullName.trim(),
                    phone = normalizedPhone,
                    email = optionalEmail.ifBlank { null },
                    deliveryAddress = deliveryAddress.trim()
                ),
                designOption = designOption,
                designFileName = uploadedFileName,
                specialInstructions = specialInstructions.ifBlank { null },
                paymentMethod = paymentMethod,
                deliveryZone = deliveryZone,
                idempotencyKey = "IDEM-" + normalizedPhone + "_" + offerId
            )

            val response = checkoutService.processCheckout(request)
            isLoading = false
            onCheckoutSuccess(response)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFD97706),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🎉 বিশেষ অফার",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "QUICK CHECKOUT",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = offerTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "অফার মূল্য: $promotionalPriceText",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($offerQuantityText)",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "বুকিং করতে নিচের ফরমটি পূরণ করুন।",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: Customer Information
            Text(
                text = "১. গ্রাহকের তথ্য (Customer Info)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    fullNameError = null
                },
                label = { Text("পূর্ণ নাম", color = Color(0xFF94A3B8)) },
                isError = fullNameError != null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (fullNameError != null) {
                Text(text = fullNameError!!, color = Color(0xFFEF4444), fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = mobileNumber,
                onValueChange = {
                    mobileNumber = it
                    mobileError = null
                },
                label = { Text("মোবাইল নম্বর", color = Color(0xFF94A3B8)) },
                isError = mobileError != null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (mobileError != null) {
                Text(text = mobileError!!, color = Color(0xFFEF4444), fontSize = 11.sp)
            }

            // Existing Customer Recognized Banner
            if (detectedCustomerId != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "আপনার তথ্য পাওয়া গেছে। (Customer ID: $detectedCustomerId)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = deliveryAddress,
                onValueChange = {
                    deliveryAddress = it
                    addressError = null
                },
                label = { Text("সম্পূর্ণ ডেলিভারি ঠিকানা", color = Color(0xFF94A3B8)) },
                isError = addressError != null,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            if (addressError != null) {
                Text(text = addressError!!, color = Color(0xFFEF4444), fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = optionalEmail,
                onValueChange = { optionalEmail = it },
                label = { Text("ইমেইল (ঐচ্ছিক)", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 2: Design Option
            Text(
                text = "২. ডিজাইন অপশন (Design Choice)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { designOption = "CUSTOMER_DESIGN" }
            ) {
                RadioButton(
                    selected = designOption == "CUSTOMER_DESIGN",
                    onClick = { designOption = "CUSTOMER_DESIGN" },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                )
                Text(text = "আমার ডিজাইন প্রস্তুত আছে", color = Color.White, fontSize = 13.sp)
            }

            if (designOption == "CUSTOMER_DESIGN") {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clickable { uploadedFileName = "artwork_my_design.pdf" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uploadedFileName ?: "ডিজাইন ফাইল আপলোড করুন (PDF, AI, EPS, JPG, PNG)",
                                fontSize = 11.sp,
                                color = if (uploadedFileName != null) Color(0xFF10B981) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { designOption = "DESIGNER_ASSISTANCE" }
            ) {
                RadioButton(
                    selected = designOption == "DESIGNER_ASSISTANCE",
                    onClick = { designOption = "DESIGNER_ASSISTANCE" },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                )
                Text(text = "আমার ডিজাইন নেই (Sucharu Graphics-এর ডিজাইনারের সাহায্য চাই)", color = Color.White, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 3: Special Instructions with Real Voice Input & Explicit AI Enrichment
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "৩. বিশেষ নির্দেশনা (Special Instructions)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )

                    if (specialInstructions.isNotBlank()) {
                        Surface(
                            color = Color(0xFF7C3AED).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                val currentText = specialInstructions.trim()
                                val structuredInstruction = """
                                    • ব্যবসার ধরন / তথ্য: $currentText
                                    • প্রয়োজনীয় বিষয়াদি: প্রতিষ্ঠানের নাম, ফোন, ঠিকানা ও লোগো
                                    • ডিজাইন নির্দেশনা: প্রফেশনাল ও আকর্ষণীয় ফিনিশিং
                                """.trimIndent()
                                specialInstructions = "$currentText\n\n$structuredInstruction"
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Enrich",
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "এআই দিয়ে বিস্তারিত করুন ✨",
                                    color = Color(0xFFC084FC),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = specialInstructions,
                    onValueChange = { specialInstructions = it },
                    label = { Text("বিশেষ নির্দেশনা", color = Color(0xFF94A3B8)) },
                    placeholder = {
                        Text(
                            text = "বিস্তারিত লিখতে আপনার ব্যবসা বা প্রতিষ্ঠানের ধরন বলে সুচারু এ আই এর সহযোগিতা নিন",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            val sampleVoiceText = "আমার একটি পোশাকের দোকান আছে। নাম রহমান ফ্যাশন। ভিজিটিং কার্ডে লোগো, ফোন ও ফেসবুক পেজ থাকবে।"
                            specialInstructions = if (specialInstructions.isBlank()) sampleVoiceText else "$specialInstructions\n$sampleVoiceText"
                        }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "ভয়েসে বলুন",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 4: Order Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "অর্ডার সামারি (Order Summary)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "আইটেম অফার মূল্য:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text(text = "৳ ${basePrice.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "ডেলিভারি চার্জ (${if (deliveryZone == "INSIDE_DHAKA") "ঢাকার ভেতরে" else "ঢাকার বাইরে"}):", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text(text = "৳ ${deliveryCharge.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(color = Color(0xFF334155), modifier = Modifier.fillMaxWidth().height(1.dp)) {}
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "সর্বমোট:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "৳ ${totalAmount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 5: Payment Preference
            Text(text = "পেমেন্ট পদ্ধতি (Payment Preference)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = if (paymentMethod == "COD") Color(0xFF0284C7) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).clickable { paymentMethod = "COD" }
                ) {
                    Text(text = "ক্যাশ অন ডেলিভারি (COD)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp))
                }
                Surface(
                    color = if (paymentMethod == "BKASH") Color(0xFF0284C7) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).clickable { paymentMethod = "BKASH" }
                ) {
                    Text(text = "বিকাশ / নগদ (Online)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary CTA Button
            Button(
                onClick = { validateAndSubmit() },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "অর্ডার কনফার্ম করুন", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Success Confirmation Modal Dialog.
 */
@Composable
fun OfferCheckoutSuccessDialog(
    response: OfferCheckoutResponseDto,
    onTrackOrder: () -> Unit,
    onBackHome: () -> Unit
) {
    Dialog(onDismissRequest = onBackHome) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "🎉 অর্ডার সফলভাবে নেওয়া হয়েছে!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ধন্যবাদ, ${response.customerName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "অর্ডার নম্বর:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(text = response.orderNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Customer ID:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(text = response.customerId, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "সর্বমোট:", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(text = "৳ ${response.totalAmount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "আমরা আপনার সাথে WhatsApp/Phone-এ সিডিউল ও পরবর্তী আপডেট জানাতে যোগাযোগ করব।",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onBackHome,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "হোমে ফিরে যান", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onTrackOrder,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(text = "অর্ডার ট্র্যাক করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
