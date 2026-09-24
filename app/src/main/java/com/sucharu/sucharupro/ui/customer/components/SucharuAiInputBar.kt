package com.sucharu.sucharupro.ui.customer.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

enum class MicState {
    IDLE, LISTENING, PROCESSING, SUCCESS
}

/**
 * Universal Reusable Keyboard-Safe Sucharu AI Input Bar with Microphone & Send right side placement.
 */
@Composable
fun SucharuAiInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "সুচারু AI-কে প্রশ্ন করুন...",
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
            android.widget.Toast.makeText(context, "ভয়েস ইনপুট এই ডিভাইসে উপলব্ধ নয়", android.widget.Toast.LENGTH_SHORT).show()
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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            android.widget.Toast.makeText(context, "ভয়েস ইনপুট ব্যবহারের জন্য মাইক্রোফোন পারমিশন প্রয়োজন", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun handleMicTap() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            permissionLauncher.launch(permission)
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill Shaped Input Text Field (+ Icon on left, Microphone Icon on right)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = if (micState == MicState.LISTENING) "কথা বলুন... (বাংলা)" else placeholderText,
                        color = if (micState == MicState.LISTENING) Color(0xFFEF4444) else Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (micState == MicState.LISTENING) Color(0xFFEF4444) else Color.Transparent)
                            .clickable { handleMicTap() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (micState == MicState.PROCESSING) {
                            CircularProgressIndicator(
                                color = Color(0xFF38BDF8),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "ভয়েসে বলুন",
                                tint = if (micState == MicState.LISTENING) Color.White else Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF38BDF8)
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Far Right Circular Up Arrow Send Button
            Surface(
                color = if (value.isNotBlank() && !isThinking) Color(0xFF38BDF8) else Color(0xFF334155),
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        if (value.isNotBlank() && !isThinking) {
                            onSendClick(value)
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
