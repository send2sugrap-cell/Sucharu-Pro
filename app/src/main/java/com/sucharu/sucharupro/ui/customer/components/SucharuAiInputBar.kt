package com.sucharu.sucharupro.ui.customer.components

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MicState {
    IDLE, LISTENING, PROCESSING, SUCCESS
}

/**
 * Universal Reusable Keyboard-Safe Sucharu AI Input Bar with Real Android Speech Recognition (bn-BD).
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
    val context = LocalContext.current
    var micState by remember { mutableStateOf(MicState.IDLE) }

    val speechRecognizer = remember {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    fun startListening() {
        if (micState == MicState.LISTENING) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // Ignore
            }
            micState = MicState.IDLE
            return
        }

        if (speechRecognizer == null) {
            micState = MicState.IDLE
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "কথা বলুন (বাংলা)...")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                micState = MicState.LISTENING
            }
            override fun onBeginningOfSpeech() {
                micState = MicState.LISTENING
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                micState = MicState.PROCESSING
            }
            override fun onError(error: Int) {
                micState = MicState.IDLE
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "কথা বোঝা যায়নি, আবার চেষ্টা করুন"
                    SpeechRecognizer.ERROR_NETWORK -> "নেটওয়ার্ক সমস্যা, আবার চেষ্টা করুন"
                    else -> "ভয়েস ইনপুট সমস্যা হয়েছে"
                }
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedSpeech = matches[0]
                    val updated = if (value.isBlank()) recognizedSpeech else "$value $recognizedSpeech"
                    onValueChange(updated)
                    onVoiceResult(updated)
                }
                micState = MicState.IDLE
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            micState = MicState.LISTENING
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            micState = MicState.IDLE
        }
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
            // Universal Real Microphone Button
            Surface(
                color = when (micState) {
                    MicState.LISTENING -> Color(0xFFEF4444)
                    MicState.PROCESSING -> Color(0xFFF59E0B)
                    else -> Color(0xFF334155)
                },
                shape = CircleShape,
                modifier = Modifier
                    .size(42.dp)
                    .clickable { startListening() }
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
                            contentDescription = "ভয়েসে বলুন",
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
                        text = if (micState == MicState.LISTENING) "কথা বলুন... (বাংলা)" else placeholderText,
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
