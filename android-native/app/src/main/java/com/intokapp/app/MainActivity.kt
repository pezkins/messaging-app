package com.intokapp.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.intokapp.app.data.network.ApiService
import com.intokapp.app.data.network.IntokFirebaseMessagingService
import com.intokapp.app.data.network.TokenManager
import com.intokapp.app.data.network.WebSocketService
import com.intokapp.app.data.repository.LocalizationManager
import com.intokapp.app.data.repository.LocalStringProvider
import com.intokapp.app.data.repository.StringProvider
import com.intokapp.app.ui.navigation.IntokNavigation
import com.intokapp.app.ui.theme.IntokTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject
    lateinit var apiService: ApiService
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    @Inject
    lateinit var webSocketService: WebSocketService
    
    @Inject
    lateinit var localizationManager: LocalizationManager
    
    @Inject
    lateinit var stringProvider: StringProvider
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "✅ Notification permission granted")
            registerFCMTokenIfLoggedIn()
        } else {
            Log.w(TAG, "⚠️ Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Apply saved language preference (must be done before setContent)
        localizationManager.applyLanguagePreference()
        
        // Request notification permission for Android 13+
        requestNotificationPermission()
        
        // Handle deep link from notification
        val pendingConversationId = intent?.getStringExtra("conversationId")
        if (pendingConversationId != null) {
            Log.d(TAG, "📬 Opened from notification: $pendingConversationId")
        }
        
        setContent {
            CompositionLocalProvider(LocalStringProvider provides stringProvider) {
                IntokTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        IntokNavigation(pendingConversationId = pendingConversationId)
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle notification tap when app is already running
        intent.getStringExtra("conversationId")?.let { conversationId ->
            Log.d(TAG, "📬 New intent with conversation: $conversationId")
            // For now, restart the activity to apply the deep link
            // A more sophisticated approach would use a ViewModel/StateFlow
            setIntent(intent)
            recreate()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure WebSocket is connected when app comes to foreground
        Log.d(TAG, "🔄 App resumed - ensuring WebSocket connection")
        webSocketService.ensureConnected()
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    registerFCMTokenIfLoggedIn()
                }
                else -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // No runtime permission needed for Android < 13
            registerFCMTokenIfLoggedIn()
        }
    }
    
    /**
     * Register FCM token with backend if user is logged in.
     * This ensures push notifications work on app launch for returning users.
     */
    private fun registerFCMTokenIfLoggedIn() {
        Log.d(TAG, "📱 Checking if user is logged in to register FCM token...")
        IntokFirebaseMessagingService.registerCurrentToken(apiService, tokenManager)
    }
}

