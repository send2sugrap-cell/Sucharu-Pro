package com.sucharu.sucharupro.ui.customer.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.ai.FirebaseAiLogicProvider
import com.sucharu.sucharupro.domain.service.ai.SucharuAiProvider
import kotlinx.coroutines.launch

data class ChatMessageItem(
    val messageId: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String
)

/**
 * Interactive Live AI Assistant Chat Screen powered by Google Gemini AI & Sucharu Printing Knowledge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantChatScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    aiProvider: SucharuAiProvider = remember { FirebaseAiLogicProvider() }
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var userQueryInput by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val messageList = remember {
        mutableStateListOf(
            ChatMessageItem(
                messageId = "MSG-001",
                text = "আসসালামু আলাইকুম! আমি সুচারু প্রো প্রফেশনাল প্রিন্টিং ও প্যাকেজিং এআই সহকারী। কাগজের GSM, অফসেট বনাম ডিজিটাল প্রিন্টিং, স্পট UV ল্যামিনেশন বা যেকোনো তথ্যের জন্য আমাকে প্রশ্ন করুন।",
                isUser = false,
                timestamp = "এখন"
            )
        )
    }

    val quickPrompts = listOf(
        "কাগজের GSM এবং থিকনেস কোনটা ভালো?",
        "অফসেট বনাম ডিজিটাল প্রিন্টিং খরচ",
        "স্পট ইউভি ও ম্যাট ল্যামিনেশন কি?",
        "১০০০ ভিজিটিং কার্ডের প্রডাকশন সময়"
    )

    fun sendUserMessage(prompt: String) {
        if (prompt.isBlank() || isThinking) return

        val userMsg = ChatMessageItem(
            messageId = "USR-" + System.currentTimeMillis(),
            text = prompt.trim(),
            isUser = true,
            timestamp = "এখন"
        )
        messageList.add(userMsg)
        userQueryInput = ""
        isThinking = true

        coroutineScope.launch {
            listState.animateScrollToItem(messageList.size - 1)

            val conversationHistory = messageList.map { Pair(it.text, it.isUser) }
            val aiResult = if (aiProvider is FirebaseAiLogicProvider) {
                aiProvider.generateChatResponse(conversationHistory, prompt)
            } else {
                aiProvider.generatePrintingAdvice(prompt, null)
            }

            val replyText = aiResult.getOrElse {
                getFallbackPrintingAdvice(prompt)
            }

            val aiMsg = ChatMessageItem(
                messageId = "AI-" + System.currentTimeMillis(),
                text = replyText,
                isUser = false,
                timestamp = "এখন"
            )
            messageList.add(aiMsg)
            isThinking = false
            listState.animateScrollToItem(messageList.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top App Bar
        Surface(
            color = Color(0xFF1E293B),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C3AED).copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "স্মার্ট AI প্রিন্টিং সহকারী",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gemini AI Advisor • Online",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.clickable { sendUserMessage(prompt) }
                ) {
                    Text(
                        text = prompt,
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Messages History Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(messageList, key = { it.messageId }) { msg ->
                ChatMessageBubble(msg = msg)
            }

            if (isThinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFC084FC),
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সুচারু এআই এজেন্ট উত্তর তৈরি করছে...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Reusable Keyboard-Safe Universal Sucharu AI Input Bar with Microphone
        com.sucharu.sucharupro.ui.customer.components.SucharuAiInputBar(
            value = userQueryInput,
            onValueChange = { userQueryInput = it },
            onSendClick = { prompt -> sendUserMessage(prompt) },
            isThinking = isThinking,
            placeholderText = "প্রিন্টিং বা দাম সম্পর্কে লিখুন বা মাইক্রোফোনে বলুন...",
            onVoiceResult = { voiceText ->
                userQueryInput = voiceText
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(msg: ChatMessageItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C3AED).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFC084FC),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = if (msg.isUser) Color(0xFF0284C7) else Color(0xFF1E293B),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (msg.isUser) 14.dp else 2.dp,
                bottomEnd = if (msg.isUser) 2.dp else 14.dp
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = msg.text,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        if (msg.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getFallbackPrintingAdvice(prompt: String): String {
    val text = prompt.lowercase()
    return when {
        text.contains("সালাম") || text.contains("আসসালামু") || text.contains("হ্যালো") || text.contains("hi") || text.contains("hello") -> {
            "ওয়ালাইকুম আসসালাম! সুচারু প্রো প্রিন্টিং ও প্যাকেজিং সেবায় আপনাকে স্বাগতম। বলুন, আপনার ব্যবসার জন্য কী ধরনের কার্ড বা প্রিন্টিং সহায়তা প্রয়োজন?"
        }
        text.contains("কার্ড") || text.contains("দোকান") || text.contains("চাউল") || text.contains("ভিজিটিং") -> {
            "আপনার প্রতিষ্ঠানের ব্যবসার জন্য ৩০০ GSM প্রিমিয়াম ম্যাট ল্যামিনেশন ও স্পট UV ভিজিটিং কার্ড সবচেয়ে আকর্ষণীয় হবে। ১০০০ পিসের বিশেষ অফারে প্রফেশনাল আর্ট কার্ড প্রিন্ট করে নিতে পারবেন।"
        }
        text.contains("gsm") || text.contains("জিএসএম") -> {
            "প্রিন্টিং-এ সাধারণ ভিজিটিং কার্ডের জন্য ৩০০ GSM আর্ট কার্ড, পোস্টার ও ফ্লাইয়ারের জন্য ১৫০ GSM আর্ট পেপার এবং লিফলেটের জন্য ১০০ GSM আর্ট পেপার সবচেয়ে উপযুক্ত।"
        }
        text.contains("অফসেট") || text.contains("ডিজিটাল") -> {
            "১০০০ পিস বা তার বেশি অর্ডারের জন্য অফসেট প্রিন্টিং খরচ অনেক কম হয়। তবে দ্রুত ১ ঘণ্টার মধ্যে বা অল্প ২০-৫০ পিস প্রিন্টের জন্য ডিজিটাল প্রিন্টিং সেরা।"
        }
        text.contains("স্পট") || text.contains("ল্যামিনেশন") -> {
            "ম্যাট ল্যামিনেশনের ওপর লোগো বা নির্দিষ্ট টেক্সটকে চকচকে ও উঁচিয়ে দেখানোর জন্য 'স্পট UV' ল্যামিনেশন ব্যবহার করা হয়, যা কার্ডকে প্রফেশনাল আউটলুক দেয়।"
        }
        else -> {
            "সুচারু কমার্শিয়াল প্রিন্টিং ও প্যাকেজিং অর্ডারের জন্য ১০০০ পিসে বিশেষ ছাড় চলছে। আপনার পছন্দের সাইজ, ডিজাইন ও পরিমাণ জানান, আমরা বিস্তারিত হিসাব জানিয়ে দেব।"
        }
    }
}
