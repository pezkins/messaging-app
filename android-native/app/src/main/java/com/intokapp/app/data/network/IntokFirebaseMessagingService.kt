package com.intokapp.app.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.intokapp.app.MainActivity
import com.intokapp.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IntokFirebaseMessagingService : FirebaseMessagingService() {
    
    @Inject
    lateinit var apiService: ApiService
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "intok_messages"
        private const val CHANNEL_NAME = "Messages"
        
        /**
         * Register the current FCM token with the backend.
         * Call this after login or when the app starts with a logged-in user.
         */
        fun registerCurrentToken(apiService: ApiService, tokenManager: TokenManager) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "📱 Got FCM token for registration")
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val authToken = tokenManager.getAccessToken()
                            if (authToken != null) {
                                apiService.registerDeviceToken(RegisterDeviceRequest(token = token))
                                Log.d(TAG, "✅ FCM token registered with backend")
                            } else {
                                Log.d(TAG, "⚠️ User not logged in, skipping token registration")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to register token: ${e.message}")
                        }
                    }
                } else {
                    Log.e(TAG, "❌ Failed to get FCM token", task.exception)
                }
            }
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "📱 New FCM Token received")
        
        // Send token to backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authToken = tokenManager.getAccessToken()
                if (authToken != null) {
                    apiService.registerDeviceToken(RegisterDeviceRequest(token = token))
                    Log.d(TAG, "✅ FCM token registered with backend")
                } else {
                    Log.d(TAG, "⚠️ User not logged in, skipping token registration")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to register token: ${e.message}")
            }
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "📬 Message received from: ${remoteMessage.from}")
        
        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "New Message",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }
        
        // Handle data payload (for silent notifications)
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }
    }
    
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        createNotificationChannel()
        
        val conversationId = data["conversationId"]
        val messageId = data["messageId"]
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            conversationId?.let { putExtra("conversationId", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create Wear OS extender for smartwatch support
        // Notifications automatically bridge to paired watches
        val wearableExtender = NotificationCompat.WearableExtender()
            .setBridgeTag("intok_message") // Prevent duplicate bridging
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // Group notifications by conversation for better organization
            .setGroup(conversationId ?: "intok_messages")
            // Add Wear OS support
            .extend(wearableExtender)
            // Set category for proper handling
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            // Add vibration pattern
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Use unique ID per message to avoid overwriting, but same conversation groups together
        val notificationId = messageId?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        
        Log.d(TAG, "📬 Notification shown: $title (id: $notificationId, conv: $conversationId)")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Intok message notifications"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun handleDataMessage(data: Map<String, String>) {
        // Handle silent push for data sync
        when (data["type"]) {
            "message" -> {
                // Could trigger local notification or sync
                Log.d(TAG, "📬 Data message received: message")
            }
            "read_receipt" -> {
                // Update local read status
                Log.d(TAG, "📬 Data message received: read_receipt")
            }
        }
    }
}
