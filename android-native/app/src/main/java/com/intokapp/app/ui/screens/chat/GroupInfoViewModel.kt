package com.intokapp.app.ui.screens.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.models.Conversation
import com.intokapp.app.data.network.ApiService
import com.intokapp.app.data.network.UploadUrlRequest
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

data class GroupInfoUiState(
    val isLoading: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val conversation: Conversation? = null,
    val currentUserId: String? = null,
    val error: String? = null
)

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val apiService: ApiService
) : ViewModel() {
    
    companion object {
        private const val TAG = "GroupInfoViewModel"
        private const val BUCKET_URL = "https://intok-attachments.s3.amazonaws.com"
    }
    
    private val _uiState = MutableStateFlow(GroupInfoUiState())
    val uiState: StateFlow<GroupInfoUiState> = _uiState
    
    private val okHttpClient = OkHttpClient()
    
    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get current user
                val currentUser = authRepository.currentUser.first()
                
                // Find conversation from repository
                val conversations = chatRepository.conversations.first()
                val conversation = conversations.find { it.id == conversationId }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        conversation = conversation,
                        currentUserId = currentUser?.id
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversation: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load group info") }
            }
        }
    }
    
    fun updateGroupName(conversationId: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            chatRepository.updateConversation(conversationId, name, null)
                .onSuccess { conversation ->
                    _uiState.update { it.copy(isLoading = false, conversation = conversation) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
    
    fun uploadGroupPicture(conversationId: String, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, error = null) }
            
            try {
                // Load and resize image
                val imageData = withContext(Dispatchers.IO) {
                    loadAndResizeImage(imageUri)
                }
                
                if (imageData == null) {
                    _uiState.update { it.copy(isUploadingPhoto = false, error = "Failed to process image") }
                    return@launch
                }
                
                // Get presigned upload URL
                val fileName = "group-${UUID.randomUUID()}.jpg"
                val uploadResponse = apiService.getUploadUrl(
                    UploadUrlRequest(
                        fileName = fileName,
                        contentType = "image/jpeg",
                        fileSize = imageData.size.toLong(),
                        conversationId = conversationId
                    )
                )
                
                // Upload to S3
                val uploadSuccess = withContext(Dispatchers.IO) {
                    uploadToS3(uploadResponse.uploadUrl, imageData)
                }
                
                if (!uploadSuccess) {
                    _uiState.update { it.copy(isUploadingPhoto = false, error = "Failed to upload image") }
                    return@launch
                }
                
                // Update conversation with new picture URL
                val pictureUrl = "$BUCKET_URL/${uploadResponse.key}"
                chatRepository.updateConversation(conversationId, null, pictureUrl)
                    .onSuccess { updatedConversation ->
                        _uiState.update { it.copy(isUploadingPhoto = false, conversation = updatedConversation) }
                        Log.d(TAG, "✅ Group picture updated: $pictureUrl")
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isUploadingPhoto = false, error = e.message) }
                    }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to upload group picture: ${e.message}")
                _uiState.update { it.copy(isUploadingPhoto = false, error = "Failed to upload picture") }
            }
        }
    }
    
    private fun loadAndResizeImage(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                
                // Crop to square
                val size = minOf(originalBitmap.width, originalBitmap.height)
                val xOffset = (originalBitmap.width - size) / 2
                val yOffset = (originalBitmap.height - size) / 2
                val croppedBitmap = Bitmap.createBitmap(originalBitmap, xOffset, yOffset, size, size)
                
                // Resize to 512x512
                val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 512, 512, true)
                
                // Compress to JPEG
                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                
                // Cleanup
                if (croppedBitmap != originalBitmap) croppedBitmap.recycle()
                if (resizedBitmap != croppedBitmap) resizedBitmap.recycle()
                originalBitmap.recycle()
                
                outputStream.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image: ${e.message}")
            null
        }
    }
    
    private fun uploadToS3(uploadUrl: String, imageData: ByteArray): Boolean {
        return try {
            val requestBody = imageData.toRequestBody("image/jpeg".toMediaType())
            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .addHeader("Content-Type", "image/jpeg")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful.also {
                if (!it) Log.e(TAG, "S3 upload failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "S3 upload error: ${e.message}")
            false
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
