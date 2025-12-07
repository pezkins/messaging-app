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
import com.intokapp.app.data.models.User
import com.intokapp.app.data.network.ApiService
import com.intokapp.app.data.network.OAuthLoginRequest
import com.intokapp.app.data.network.TokenManager
import com.intokapp.app.data.network.UpdateCountryRequest
import com.intokapp.app.data.network.UpdateLanguageRequest
import com.intokapp.app.data.network.UpdateProfileRequest
import com.intokapp.app.data.network.WebSocketService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    
    // Google Sign-In client - matching iOS approach (no ID token needed)
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
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
    suspend fun handleGoogleSignInResult(data: Intent?): Result<Boolean> {
        Log.d(TAG, "📥 Handling Google Sign-In result...")
        
        return try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            Log.d(TAG, "✅ Google Sign-In successful: ${account.email}")
            
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
            
            Log.d(TAG, "✅ Backend auth successful: ${response.user.email}, isNew: $isNewUser")
            Result.success(isNewUser)
            
        } catch (e: ApiException) {
            Log.e(TAG, "❌ Google Sign-In failed with code: ${e.statusCode}, message: ${e.message}")
            _authState.value = AuthState.Error("Google Sign-In failed: ${e.statusCode}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Sign-In error: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
            Result.failure(e)
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
    
    suspend fun completeSetup() {
        val currentState = _authState.value
        if (currentState is AuthState.Authenticated) {
            _authState.value = AuthState.Authenticated(currentState.user, false)
        }
    }
    
    suspend fun logout() {
        Log.d(TAG, "🚪 Logging out...")
        
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
}
