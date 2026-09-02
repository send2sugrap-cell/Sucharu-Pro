package com.sucharu.sucharupro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sucharu.sucharupro.data.composition.DevelopmentDemoRuntimeComposition
import com.sucharu.sucharupro.data.composition.ProductionRuntimeComposition
import com.sucharu.sucharupro.ui.shell.SucharuGraphicsAppShell
import com.sucharu.sucharupro.ui.theme.SucharuProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SucharuProTheme {
                SucharuProMainApp()
            }
        }
    }
}

@Composable
fun SucharuProMainApp() {
    val isDemo = BuildConfig.DEMO_MODE
    val sessionManager = remember {
        val composition = if (isDemo) {
            DevelopmentDemoRuntimeComposition()
        } else {
            ProductionRuntimeComposition()
        }
        composition.createSessionManager()
    }

    SucharuGraphicsAppShell(
        sessionManager = sessionManager,
        isDemoMode = isDemo,
        modifier = Modifier.fillMaxSize()
    )
}