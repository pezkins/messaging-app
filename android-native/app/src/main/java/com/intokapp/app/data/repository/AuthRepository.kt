package com.intokapp.app.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.intokapp.app.BuildConfig
import com.intokapp.app.data.models.AuthResponse
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
import kotlinx.coroutines.flow.first
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
    
    suspend fun signInWithGoogle(activityContext: Context): Result<Boolean> {
        Log.d(TAG, "🔐 Starting Google Sign-In...")
        
        return try {
            val credentialManager = CredentialManager.create(activityContext)
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            
            handleGoogleSignInResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Sign-In error: ${e.message}")
            _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
            Result.failure(e)
        }
    }
    
    private suspend fun handleGoogleSignInResult(result: GetCredentialResponse): Result<Boolean> {
        val credential = result.credential
        
        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        
                        Log.d(TAG, "✅ Google credential received: ${googleIdTokenCredential.id}")
                        
                        // Call backend OAuth
                        val response = apiService.oauthLogin(
                            OAuthLoginRequest(
                                provider = "google",
                                providerId = googleIdTokenCredential.id,
                                email = googleIdTokenCredential.id, // Google ID is usually the email
                                name = googleIdTokenCredential.displayName,
                                avatarUrl = googleIdTokenCredential.profilePictureUri?.toString()
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
                        
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "❌ Failed to parse Google ID token: ${e.message}")
                        _authState.value = AuthState.Error("Failed to parse credentials")
                        Result.failure(e)
                    }
                } else {
                    Log.e(TAG, "❌ Unexpected credential type: ${credential.type}")
                    _authState.value = AuthState.Error("Unexpected credential type")
                    Result.failure(Exception("Unexpected credential type"))
                }
            }
            else -> {
                Log.e(TAG, "❌ Unexpected credential class: ${credential.javaClass}")
                _authState.value = AuthState.Error("Unexpected credential")
                Result.failure(Exception("Unexpected credential"))
            }
        }
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
