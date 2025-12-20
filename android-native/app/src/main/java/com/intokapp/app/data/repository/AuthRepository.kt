/**
 * ⛔⛔⛔ CRITICAL FILE - DO NOT MODIFY WITHOUT APPROVAL ⛔⛔⛔
 *
 * This file handles Google Sign-In and authentication.
 * Changes to this file can break authentication for ALL users.
 *
 * Dependencies:
 * - google-services.json must have valid OAuth client IDs
 * - Firebase Console must have correct SHA-1 fingerprints
 *
 * See: team/authentication-config.md
 */
package com.intokapp.app.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.intokapp.app.data.models.User
import com.intokapp.app.data.network.ApiService
import com.intokapp.app.data.network.OAuthLoginRequest
import com.intokapp.app.data.network.RegisterDeviceRequest
import com.intokapp.app.data.network.TokenManager
import com.intokapp.app.data.network.UnregisterDeviceRequest
import com.intokapp.app.data.network.UpdateCountryRequest
import com.intokapp.app.data.network.UpdateLanguageRequest
import com.intokapp.app.data.network.UpdateRegionRequest
import com.intokapp.app.data.network.UpdateProfileRequest
import com.intokapp.app.data.network.WebSocketService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User, val needsSetup: Boolean = false) : AuthState()
    data class Error(val message: String) : AuthState()
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val webSocketService: WebSocketService
) {
    private val TAG = "AuthRepository"
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState
    
    val currentUser: Flow<User?> = tokenManager.userFlow
    
    // Web Client ID from google-services.json (client_type: 3)
    // This is required for Google Sign-In to work with multiple signing keys
    private val webClientId = "75949298562-t5r741c9ud61cnbsrhc95eghutjo5h4m.apps.googleusercontent.com"
    
    // Google Sign-In client - using Web Client ID for multi-key support
    // This validates against ALL registered SHA-1 fingerprints in Firebase
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)  // Required for Play Store builds
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    suspend fun initialize() {
        Log.d(TAG, "🚀 Initializing auth...")
        
        val accessToken = tokenManager.getAccessToken()
        val user = tokenManager.getUser()
        
        if (accessToken == null || user == null) {
            Log.d(TAG, "No stored auth, showing login")
            _authState.value = AuthState.Unauthenticated
            return
        }
        
        try {
            // Verify token is still valid
            val response = apiService.getMe()
            tokenManager.saveUser(response.user)
            
            // Connect WebSocket
            webSocketService.connect()
            
            // Register FCM token for push notifications
            registerFCMToken()
            
            _authState.value = AuthState.Authenticated(response.user)
            Log.d(TAG, "✅ Auth restored for: ${response.user.email}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Token validation failed, trying refresh: ${e.message}")
            
            try {
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken != null) {
                    val tokens = apiService.refreshToken(
                        com.intokapp.app.data.network.RefreshTokenRequest(refreshToken)
                    )
                    tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                    
                    val response = apiService.getMe()
                    tokenManager.saveUser(response.user)
                    
                    webSocketService.connect()
                    
                    // Register FCM token for push notifications
                    registerFCMToken()
                    
                    _authState.value = AuthState.Authenticated(response.user)
                    Log.d(TAG, "✅ Token refreshed for: ${response.user.email}")
                } else {
                    throw Exception("No refresh token")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Refresh failed, clearing auth: ${e.message}")
                tokenManager.clearAll()
                _authState.value = AuthState.Unauthenticated
            }
        }
    }
    
    // Returns the Intent to launch Google Sign-In
    fun getGoogleSignInIntent(): Intent {
        Log.d(TAG, "🔐 Getting Google Sign-In Intent...")
        return googleSignInClient.signInIntent
    }
    
    // Handle the result from Google Sign-In activity
    suspend fun handleGoogleSignInResult(data: Intent?, resultCode: Int): Result<Boolean> {
        Log.d(TAG, "📥 Handling Google Sign-In result... resultCode=$resultCode, hasData=${data != null}")
        
        // If no data at all, provide helpful error based on result code
        if (data == null) {
            val errorMessage = when (resultCode) {
                android.app.Activity.RESULT_CANCELED -> "Sign-in was cancelled"
                else -> "Google Sign-In failed - no response data (code: $resultCode)"
            }
            Log.e(TAG, "❌ Google Sign-In returned null data: $errorMessage")
            _authState.value = AuthState.Error(errorMessage)
            return Result.failure(Exception(errorMessage))
        }
        
        return try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            Log.d(TAG, "✅ Google Sign-In successful: ${account.email}, idToken=${account.idToken?.take(20)}...")
            
            // Call backend OAuth
            val response = apiService.oauthLogin(
                OAuthLoginRequest(
                    provider = "google",
                    providerId = account.id ?: account.email ?: "",
                    email = account.email ?: "",
                    name = account.displayName,
                    avatarUrl = account.photoUrl?.toString()
                )
            )
            
            // Save auth
            tokenManager.saveAll(
                response.accessToken,
                response.refreshToken,
                response.user
            )
            
            // Connect WebSocket
            webSocketService.connect()
            
            val isNewUser = response.isNewUser || 
                response.user.username.isEmpty() || 
                response.user.username == response.user.email
            
            _authState.value = AuthState.Authenticated(response.user, isNewUser)
            
            // Register FCM token for push notifications
            registerFCMToken()
            
            Log.d(TAG, "✅ Backend auth successful: ${response.user.email}, isNew: $isNewUser")
            Result.success(isNewUser)
            
        } catch (e: ApiException) {
            Log.e(TAG, "❌ Google Sign-In ApiException: code=${e.statusCode}, message=${e.message}")
            // Provide helpful error messages for common issues
            val errorMessage = when (e.statusCode) {
                10 -> "Configuration error (DEVELOPER_ERROR). SHA-1 fingerprint not registered. Contact support."
                12500 -> "Sign-in failed. Google Play Services error."
                12501 -> "Sign-in was cancelled"
                12502 -> "Sign-in currently in progress"
                7 -> "Network error. Check your connection."
                8 -> "Internal error. Try again."
                else -> "Google Sign-In failed (error ${e.statusCode})"
            }
            _authState.value = AuthState.Error(errorMessage)
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Sign-In Exception: ${e.javaClass.simpleName}: ${e.message}")
            val errorMessage = e.message ?: "Sign-in failed unexpectedly"
            _authState.value = AuthState.Error(errorMessage)
            Result.failure(Exception(errorMessage))
        }
    }
    
    fun setLoading() {
        _authState.value = AuthState.Loading
    }
    
    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }
    
    suspend fun updateUsername(username: String): Result<User> {
        return try {
            val response = apiService.updateProfile(UpdateProfileRequest(username))
            tokenManager.saveUser(response.user)
            
            val currentState = _authState.value
            if (currentState is AuthState.Authenticated) {
                _authState.value = AuthState.Authenticated(response.user, false)
            }
            
            Result.success(response.user)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update username failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateLanguage(language: String): Result<User> {
        return try {
            val response = apiService.updateLanguage(UpdateLanguageRequest(language))
            tokenManager.saveUser(response.user)
            
            val currentState = _authState.value
            if (currentState is AuthState.Authenticated) {
                _authState.value = AuthState.Authenticated(response.user, currentState.needsSetup)
            }
            
            Result.success(response.user)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update language failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateCountry(country: String): Result<User> {
        return try {
            val response = apiService.updateCountry(UpdateCountryRequest(country))
            tokenManager.saveUser(response.user)

            val currentState = _authState.value
            if (currentState is AuthState.Authenticated) {
                _authState.value = AuthState.Authenticated(response.user, currentState.needsSetup)
            }

            Result.success(response.user)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update country failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateRegion(region: String): Result<User> {
        return try {
            val response = apiService.updateRegion(UpdateRegionRequest(region))
            tokenManager.saveUser(response.user)

            val currentState = _authState.value
            if (currentState is AuthState.Authenticated) {
                _authState.value = AuthState.Authenticated(response.user, currentState.needsSetup)
            }

            Result.success(response.user)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update region failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update user data locally (used after profile picture update)
     */
    suspend fun updateUserLocally(user: User) {
        tokenManager.saveUser(user)
        
        val currentState = _authState.value
        if (currentState is AuthState.Authenticated) {
            _authState.value = AuthState.Authenticated(user, currentState.needsSetup)
        }
        
        Log.d(TAG, "✅ User updated locally: ${user.username}")
    }

    suspend fun completeSetup() {
        val currentState = _authState.value
        if (currentState is AuthState.Authenticated) {
            _authState.value = AuthState.Authenticated(currentState.user, false)
        }
    }
    
    /**
     * Register the FCM token with the backend for push notifications.
     * Called after successful login/registration.
     */
    private fun registerFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "📱 Got FCM token, registering with backend...")
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        apiService.registerDeviceToken(RegisterDeviceRequest(token = token))
                        Log.d(TAG, "✅ FCM token registered with backend")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to register FCM token: ${e.message}")
                    }
                }
            } else {
                Log.e(TAG, "❌ Failed to get FCM token", task.exception)
            }
        }
    }
    
    /**
     * Unregister the FCM token from the backend before logout.
     */
    private suspend fun unregisterFCMToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            apiService.unregisterDeviceToken(UnregisterDeviceRequest(token = token))
                            Log.d(TAG, "✅ FCM token unregistered from backend")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to unregister FCM token: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unregistering FCM token: ${e.message}")
        }
    }
    
    suspend fun logout() {
        Log.d(TAG, "🚪 Logging out...")
        
        // Unregister FCM token first
        unregisterFCMToken()
        
        try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                apiService.logout(com.intokapp.app.data.network.LogoutRequest(refreshToken))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Logout API call failed: ${e.message}")
        }
        
        webSocketService.disconnect()
        tokenManager.clearAll()
        _authState.value = AuthState.Unauthenticated
        
        Log.d(TAG, "✅ Logged out")
    }
    
    // Email Auth Methods
    suspend fun checkEmail(email: String): Result<Boolean> {
        Log.d(TAG, "📧 Checking if email exists: $email")
        return try {
            val response = apiService.checkEmail(
                com.intokapp.app.data.network.CheckEmailRequest(email)
            )
            Log.d(TAG, "📧 Email check result: exists=${response.exists}")
            Result.success(response.exists)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Email check failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun loginWithEmail(email: String, password: String): Result<Boolean> {
        Log.d(TAG, "🔐 Logging in with email: $email")
        _authState.value = AuthState.Loading
        
        return try {
            val response = apiService.login(
                com.intokapp.app.data.models.LoginRequest(email, password)
            )
            
            // Save auth
            tokenManager.saveAll(
                response.accessToken,
                response.refreshToken,
                response.user
            )
            
            // Connect WebSocket
            webSocketService.connect()
            
            _authState.value = AuthState.Authenticated(response.user, false)
            
            // Register FCM token for push notifications
            registerFCMToken()
            
            Log.d(TAG, "✅ Email login successful: ${response.user.email}")
            Result.success(false) // Not a new user
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Email login failed: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Login failed")
            Result.failure(e)
        }
    }
    
    suspend fun registerWithEmail(
        email: String,
        password: String,
        username: String,
        preferredLanguage: String,
        preferredCountry: String
    ): Result<Boolean> {
        Log.d(TAG, "📝 Registering with email: $email")
        _authState.value = AuthState.Loading
        
        return try {
            val response = apiService.register(
                com.intokapp.app.data.models.RegisterRequest(
                    email = email,
                    password = password,
                    username = username,
                    preferredLanguage = preferredLanguage,
                    preferredCountry = preferredCountry
                )
            )
            
            // Save auth
            tokenManager.saveAll(
                response.accessToken,
                response.refreshToken,
                response.user
            )
            
            // Connect WebSocket
            webSocketService.connect()
            
            _authState.value = AuthState.Authenticated(response.user, false)
            
            // Register FCM token for push notifications
            registerFCMToken()
            
            Log.d(TAG, "✅ Registration successful: ${response.user.email}")
            Result.success(true) // Is a new user
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Registration failed: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Registration failed")
            Result.failure(e)
        }
    }
}


