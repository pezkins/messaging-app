package com.intokapp.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class IntokApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide configurations here
    }
}

