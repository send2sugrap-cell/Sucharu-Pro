package com.sucharu.sucharupro

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Main Application Class for Sucharu Pro ERP Android Application.
 *
 * Initializes Firebase App and configures Firebase App Check Debug Provider
 * for local development and runtime attestation safety.
 */
class SucharuProApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure Firebase is initialized
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        if (BuildConfig.DEBUG) {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        }
    }
}
