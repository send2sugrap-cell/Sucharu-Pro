package com.sucharu.sucharupro.ui.customer.screens

import android.speech.tts.TextToSpeech
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.ai.FirebaseAiLogicProvider
import com.sucharu.sucharupro.data.persistence.copilot.CopilotChatDatabaseHelper
import com.sucharu.sucharupro.domain.service.ai.SucharuAiProvider
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatMessageItem(
    val messageId: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String,
    val isConfirmationPending: Boolean = false,
    val proposalId: String? = null
)

/**
 * Interactive Live AI Assistant Chat Screen powered by Google Gemini AI & Sucharu Printing Knowledge.
 * Integrates Cloud Run Gateway, Dynamic Option Chips (`OPTIONS: [...]`), Bengali Voice STT/TTS,
 * Client Image Compression, RAG Knowledge, and MCP Action Proposal Confirmation Gates (R0–R3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantChatScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    composition: com.sucharu.sucharupro.data.composition.AppRuntimeComposition? = null,
    aiProvider: SucharuAiProvider = remember(composition) {
        val client = (composition as? com.sucharu.sucharupro.data.composition.ProductionRuntimeComposition)?.client
        FirebaseAiLogicProvider(apiClient = client)
    }
) {
    val context = LocalContext.current
    val dbHelper = remember { CopilotChatDatabaseHelper(context.applicationContext) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var userQueryInput by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val messageList = remember { mutableStateListOf<ChatMessageItem>() }

    // Bengali Text-to-Speech (TTS) Engine Initialization
    var ttsEngine: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = instance?.setLanguage(Locale.forLanguageTag("bn-BD"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    instance?.setLanguage(Locale.ENGLISH)
                }
            }
        }
        ttsEngine = instance
        onDispose {
            instance?.stop()
            instance?.shutdown()
        }
    }

    fun speakText(text: String) {
        val (cleanText, _) = parseOptionChips(text)
        ttsEngine?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "TTS_CHAT_MSG")
    }

    // Load persisted chat history from local SQLite database
    LaunchedEffect(Unit) {
        val saved = dbHelper.getAllMessages()
        if (saved.isEmpty()) {
            val initialGreeting = ChatMessageItem(
                messageId = "MSG-INIT-001",
                text = "আসসালামু আলাইকুম! আমি সুচারু গ্রাফিক্সের প্রিন্টিং অ্যাডভাইজার। অফসেট বনাম ডিজিটাল প্রিন্টিং, কাগজের GSM, স্পট UV ল্যামিনেশন বা যেকোনো তথ্যের জন্য আমাকে প্রশ্ন করুন।\nOPTIONS: [\"কাগজের GSM এবং থিকনেস কোনটা ভালো?\", \"অফসেট বনাম ডিজিটাল প্রিন্টিং খরচ\", \"১০০০ ভিজিটিং কার্ডের প্রডাকশন সময়\"]",
                isUser = false,
                timestamp = "এখন"
            )
            dbHelper.insertMessage(initialGreeting)
            messageList.clear()
            messageList.add(initialGreeting)
        } else {
            messageList.clear()
            messageList.addAll(saved)
        }
        if (messageList.isNotEmpty()) {
            listState.scrollToItem(messageList.size - 1)
        }
    }

    val quickPrompts = listOf(
        "কাগজের GSM এবং থিকনেস কোনটা ভালো?",
        "অফসেট বনাম ডিজিটাল প্রিন্টিং খরচ",
        "স্পট ইউভি ও ম্যাট ল্যামিনেশন কি?",
        "১০০০ ভিজিটিং কার্ডের প্রডাকশন সময়"
    )

    fun sendUserMessage(prompt: String) {
        if (prompt.isBlank() || isThinking) return

        val trimmedPrompt = prompt.trim()

        // 1. Capture history before adding current message
        val previousHistory = messageList.map { Pair(it.text, it.isUser) }

        // 2. Add user message to UI & local database
        val userMsg = ChatMessageItem(
            messageId = "USR-" + System.currentTimeMillis(),
            text = trimmedPrompt,
            isUser = true,
            timestamp = "এখন"
        )
        messageList.add(userMsg)
        userQueryInput = ""
        isThinking = true

        coroutineScope.launch {
            dbHelper.insertMessage(userMsg)
            listState.animateScrollToItem(messageList.size - 1)

            // 3. Dispatch to AI Provider / Gateway
            val aiResult = if (aiProvider is FirebaseAiLogicProvider) {
                aiProvider.generateChatResponse(previousHistory, trimmedPrompt)
            } else {
                aiProvider.generatePrintingAdvice(trimmedPrompt, null)
            }

            // 4. Handle Response & Failure Fallbacks
            val replyText = aiResult.getOrElse { error ->
                android.util.Log.e("ChatViewModel", "AI Error: ${error.message}")
                "দুঃখিত, সার্ভারের সাথে সংযোগে সমস্যা হচ্ছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
            }

            val aiMsg = ChatMessageItem(
                messageId = "AI-" + System.currentTimeMillis(),
                text = replyText,
                isUser = false,
                timestamp = "এখন"
            )
            messageList.add(aiMsg)
            dbHelper.insertMessage(aiMsg)
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "সুচারু গ্রাফিক্সের প্রিন্টিং অ্যাডভাইজার",
                        fontSize = 18.sp,
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
                            text = "Online • Smart Advisor",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Clear Chat History Option
                IconButton(onClick = {
                    coroutineScope.launch {
                        dbHelper.clearHistory()
                        val initialGreeting = ChatMessageItem(
                            messageId = "MSG-INIT-" + System.currentTimeMillis(),
                            text = "আসসালামু আলাইকুম! আমি সুচারু গ্রাফিক্সের প্রিন্টিং অ্যাডভাইজার। অফসেট বনাম ডিজিটাল প্রিন্টিং, কাগজের GSM, স্পট UV ল্যামিনেশন বা যেকোনো তথ্যের জন্য আমাকে প্রশ্ন করুন।\nOPTIONS: [\"কাগজের GSM এবং থিকনেস কোনটা ভালো?\", \"অফসেট বনাম ডিজিটাল প্রিন্টিং খরচ\", \"১০০০ ভিজিটিং কার্ডের প্রডাকশন সময়\"]",
                            isUser = false,
                            timestamp = "এখন"
                        )
                        dbHelper.insertMessage(initialGreeting)
                        messageList.clear()
                        messageList.add(initialGreeting)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Chat",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
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
                        fontSize = 12.sp,
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
                ChatMessageBubble(
                    msg = msg,
                    onOptionClick = { optionPrompt -> sendUserMessage(optionPrompt) },
                    onSpeakClick = { textToSpeak -> speakText(textToSpeak) },
                    onConfirmProposal = {
                        sendUserMessage("হ্যাঁ, আমি নিশ্চিত করছি। কোটেশন প্রসেস করুন।")
                    }
                )
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
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Reusable Keyboard-Safe Universal Sucharu AI Input Bar with Microphone (STT) & Image Attachment (+)
        com.sucharu.sucharupro.ui.customer.components.SucharuAiInputBar(
            value = userQueryInput,
            onValueChange = { userQueryInput = it },
            onSendClick = { prompt -> sendUserMessage(prompt) },
            isThinking = isThinking,
            placeholderText = "প্রিন্টিং বা স্পেক্স সম্পর্কে লিখুন বা মাইক্রোফোনে বলুন...",
            onVoiceResult = { voiceText ->
                userQueryInput = voiceText
            },
            onImageSelected = { _ ->
                sendUserMessage("[রেফারেন্স ছবি যুক্ত করা হয়েছে]")
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    msg: ChatMessageItem,
    onOptionClick: (String) -> Unit = {},
    onSpeakClick: (String) -> Unit = {},
    onConfirmProposal: () -> Unit = {}
) {
    val (cleanMessageText, optionChips) = parseOptionChips(msg.text)

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

        Column(modifier = Modifier.fillMaxWidth(0.85f)) {
            Surface(
                color = if (msg.isUser) Color(0xFF0284C7) else Color(0xFF1E293B),
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (msg.isUser) 14.dp else 2.dp,
                    bottomEnd = if (msg.isUser) 2.dp else 14.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = cleanMessageText,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 21.sp
                    )

                    if (!msg.isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Listen",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onSpeakClick(cleanMessageText) }
                            )
                        }
                    }
                }
            }

            // Task 4.3: Dynamic Option Chips Engine
            if (!msg.isUser && optionChips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(optionChips) { chipText ->
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                            modifier = Modifier.clickable { onOptionClick(chipText) }
                        ) {
                            Text(
                                text = chipText,
                                color = Color(0xFF00F0FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Task 5.1: MCP Action Proposal Gate (R2 - Confirmation Required)
            if (msg.isConfirmationPending) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF0A1224),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ক্যালকুলেশন স্পেক্স নিশ্চিত করবেন?",
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = Color(0xFF10B981),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onConfirmProposal() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "অনুমোদন করুন", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
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

/**
 * Task 4.3: Parses OPTIONS: ["option 1", "option 2"] embedded in AI responses.
 */
private fun parseOptionChips(rawText: String): Pair<String, List<String>> {
    val regex = Regex("""OPTIONS:\s*\[(.*?)\]""", RegexOption.IGNORE_CASE)
    val match = regex.find(rawText)
    if (match != null) {
        val optionsContent = match.groupValues[1]
        val optionsList = optionsContent
            .split(",")
            .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
            .filter { it.isNotBlank() }
        val cleanText = rawText.replace(regex, "").trim()
        return Pair(cleanText, optionsList)
    }
    return Pair(rawText, emptyList())
}
