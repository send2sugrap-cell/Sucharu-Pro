package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MicState {
    IDLE, LISTENING, PROCESSING, SUCCESS
}

/**
 * Universal Reusable Keyboard-Safe Sucharu AI Input Bar with Microphone & Send capabilities.
 */
@Composable
fun SucharuAiInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "প্রিন্টিং সম্পর্কে প্রশ্ন লিখুন বা ভয়েসে বলুন...",
    isThinking: Boolean = false,
    onVoiceResult: (String) -> Unit = {}
) {
    var micState by remember { mutableStateOf(MicState.IDLE) }

    fun simulateVoiceInput() {
        if (micState == MicState.LISTENING) {
            micState = MicState.IDLE
            return
        }

        micState = MicState.LISTENING
        // Voice listening simulation for Bengali
        val sampleVoicePrompts = listOf(
            "আমার একটি পোশাকের দোকান আছে, সুচারু এআই আমাকে আধুনিক ভিজিটিং কার্ডের বিবরণ দাও",
            "১৫০ GSM আর্ট পেপারের ক্যাটাগরি এবং সুবিধা কি?",
            "১০০০ পিস বিজনেস কার্ডে স্পট ইউভি ল্যামিনেশন যোগ করো",
            "রেস্টুরেন্ট মেনু কার্ড ও টেবিল স্ট্যান্ডের প্রডাকশন সময় কত?"
        )

        val recognizedText = sampleVoicePrompts.random()
        micState = MicState.PROCESSING
        onVoiceResult(recognizedText)
        micState = MicState.SUCCESS
        micState = MicState.IDLE
    }

    Surface(
        color = Color(0xFF1E293B),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Universal Microphone Button
            Surface(
                color = when (micState) {
                    MicState.LISTENING -> Color(0xFFEF4444)
                    MicState.PROCESSING -> Color(0xFFF59E0B)
                    else -> Color(0xFF334155)
                },
                shape = CircleShape,
                modifier = Modifier
                    .size(42.dp)
                    .clickable { simulateVoiceInput() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (micState == MicState.PROCESSING) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Input Text Field
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = if (micState == MicState.LISTENING) "শুনছি... বলুন..." else placeholderText,
                        color = if (micState == MicState.LISTENING) Color(0xFFEF4444) else Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            Surface(
                color = if (value.isNotBlank() && !isThinking) Color(0xFF7C3AED) else Color(0xFF334155),
                shape = CircleShape,
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        if (value.isNotBlank() && !isThinking) {
                            onSendClick(value)
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
