package com.intokapp.app.ui.screens.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.models.User
import com.intokapp.app.data.network.ApiService
import com.intokapp.app.data.network.ProfilePictureUploadRequest
import com.intokapp.app.data.network.UpdateProfilePictureRequest
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val uploadProgress: Float = 0f,
    val isSignedOut: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "SettingsViewModel"
        private const val MAX_IMAGE_SIZE = 512 // Max dimension for profile picture
    }
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState
    
    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        _uiState.update { it.copy(user = state.user) }
                    }
                    is AuthState.Unauthenticated -> {
                        _uiState.update { it.copy(isSignedOut = true) }
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun updateUsername(username: String) {
        if (username.length < 2) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.updateUsername(username)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.updateLanguage(languageCode)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun updateCountry(countryCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            authRepository.updateCountry(countryCode)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
    
    fun uploadProfilePicture(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, uploadProgress = 0f, error = null) }
            
            try {
                // 1. Read and resize image
                val imageData = withContext(Dispatchers.IO) {
                    resizeImage(imageUri)
                }
                
                if (imageData == null) {
                    _uiState.update { it.copy(
                        isUploadingPhoto = false,
                        error = "Failed to process image"
                    ) }
                    return@launch
                }
                
                _uiState.update { it.copy(uploadProgress = 0.2f) }
                
                // 2. Get presigned URL from backend
                val fileName = "profile_${System.currentTimeMillis()}.jpg"
                val contentType = "image/jpeg"
                
                val uploadUrlResponse = apiService.getProfilePictureUploadUrl(
                    ProfilePictureUploadRequest(
                        fileName = fileName,
                        contentType = contentType,
                        fileSize = imageData.size.toLong()
                    )
                )
                
                _uiState.update { it.copy(uploadProgress = 0.4f) }
                
                // 3. Upload to S3
                val uploadSuccess = withContext(Dispatchers.IO) {
                    uploadToS3(uploadUrlResponse.uploadUrl, imageData, contentType)
                }
                
                if (!uploadSuccess) {
                    _uiState.update { it.copy(
                        isUploadingPhoto = false,
                        error = "Failed to upload image"
                    ) }
                    return@launch
                }
                
                _uiState.update { it.copy(uploadProgress = 0.8f) }
                
                // 4. Update user profile with new image key
                val userResponse = apiService.updateProfilePicture(
                    UpdateProfilePictureRequest(key = uploadUrlResponse.key)
                )
                
                // 5. Update local user state
                authRepository.updateUserLocally(userResponse.user)
                
                _uiState.update { it.copy(
                    isUploadingPhoto = false,
                    uploadProgress = 1f,
                    successMessage = "Profile picture updated!"
                ) }
                
                Log.d(TAG, "✅ Profile picture uploaded successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to upload profile picture: ${e.message}", e)
                _uiState.update { it.copy(
                    isUploadingPhoto = false,
                    error = "Failed to upload profile picture: ${e.message}"
                ) }
            }
        }
    }
    
    fun deleteProfilePicture() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val userResponse = apiService.deleteProfilePicture()
                authRepository.updateUserLocally(userResponse.user)
                
                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Profile picture removed"
                ) }
                
                Log.d(TAG, "✅ Profile picture deleted successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to delete profile picture: ${e.message}", e)
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to remove profile picture: ${e.message}"
                ) }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    private fun resizeImage(uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) return null
            
            // Calculate new dimensions maintaining aspect ratio
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = MAX_IMAGE_SIZE.toFloat() / maxOf(width, height)
            
            val newWidth: Int
            val newHeight: Int
            
            if (scale < 1) {
                newWidth = (width * scale).toInt()
                newHeight = (height * scale).toInt()
            } else {
                newWidth = width
                newHeight = height
            }
            
            // Create scaled bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            
            // Create square crop (center crop)
            val size = minOf(scaledBitmap.width, scaledBitmap.height)
            val xOffset = (scaledBitmap.width - size) / 2
            val yOffset = (scaledBitmap.height - size) / 2
            val croppedBitmap = Bitmap.createBitmap(scaledBitmap, xOffset, yOffset, size, size)
            
            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            
            // Clean up
            if (originalBitmap != scaledBitmap) originalBitmap.recycle()
            if (scaledBitmap != croppedBitmap) scaledBitmap.recycle()
            croppedBitmap.recycle()
            
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error resizing image: ${e.message}", e)
            null
        }
    }
    
    private fun uploadToS3(uploadUrl: String, data: ByteArray, contentType: String): Boolean {
        return try {
            val requestBody = data.toRequestBody(contentType.toMediaType())
            
            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .header("Content-Type", contentType)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val success = response.isSuccessful
            
            if (!success) {
                Log.e(TAG, "S3 upload failed: ${response.code} - ${response.message}")
            }
            
            response.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "S3 upload error: ${e.message}", e)
            false
        }
    }
}
