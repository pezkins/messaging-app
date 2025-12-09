package com.intokapp.app.data.network

import com.intokapp.app.data.models.*
import retrofit2.http.*

/**
 * Retrofit API Service interface for Intok backend
 */
interface ApiService {
    
    // ============================================
    // Auth Endpoints
    // ============================================
    
    @POST("api/auth/oauth")
    suspend fun oauthLogin(@Body request: OAuthLoginRequest): AuthResponse
    
    @GET("api/auth/me")
    suspend fun getMe(): UserResponse
    
    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): RefreshTokenResponse
    
    @POST("api/auth/logout")
    suspend fun logout(@Body request: LogoutRequest)
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
    
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse
    
    @POST("api/auth/check-email")
    suspend fun checkEmail(@Body request: CheckEmailRequest): CheckEmailResponse
    
    // ============================================
    // User Endpoints
    // ============================================
    
    @PATCH("api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): UserResponse
    
    @PATCH("api/users/me/language")
    suspend fun updateLanguage(@Body request: UpdateLanguageRequest): UserResponse
    
    @PATCH("api/users/me/country")
    suspend fun updateCountry(@Body request: UpdateCountryRequest): UserResponse
    
    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String): UsersSearchResponse
    
    // ============================================
    // Conversation Endpoints
    // ============================================
    
    @GET("api/conversations")
    suspend fun getConversations(): ConversationsResponse
    
    @POST("api/conversations")
    suspend fun createConversation(@Body request: CreateConversationRequest): ConversationResponse
    
    @GET("api/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null
    ): MessagesResponse
    
    // ============================================
    // Attachment Endpoints
    // ============================================
    
    @POST("api/attachments/upload-url")
    suspend fun getUploadUrl(@Body request: UploadUrlRequest): UploadUrlResponse
    
    @GET("api/attachments/download-url")
    suspend fun getDownloadUrl(@Query("key") key: String): DownloadUrlResponse
}

// ============================================
// Request Models
// ============================================

data class OAuthLoginRequest(
    val provider: String,
    val providerId: String,
    val email: String,
    val name: String?,
    val avatarUrl: String?
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class LogoutRequest(
    val refreshToken: String
)

data class UpdateProfileRequest(
    val username: String
)

data class UpdateLanguageRequest(
    val preferredLanguage: String
)

data class UpdateCountryRequest(
    val preferredCountry: String
)

data class CheckEmailRequest(
    val email: String
)

data class UploadUrlRequest(
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val conversationId: String
)

// ============================================
// Response Models
// ============================================

data class UserResponse(
    val user: User
)

data class CheckEmailResponse(
    val exists: Boolean,
    val email: String
)

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String
)

data class ConversationResponse(
    val conversation: Conversation
)

data class UploadUrlResponse(
    val attachmentId: String,
    val uploadUrl: String,
    val key: String,
    val category: String,
    val expiresIn: Int
)

data class DownloadUrlResponse(
    val downloadUrl: String,
    val expiresIn: Int? = null
)
