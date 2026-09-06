package com.sucharu.sucharupro

import android.app.Application
import com.google.firebase.FirebaseApp

class SucharuProApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure Firebase is initialized
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
    }
}
