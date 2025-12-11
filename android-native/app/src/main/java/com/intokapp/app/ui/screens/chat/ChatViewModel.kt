package com.intokapp.app.ui.screens.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intokapp.app.data.models.Attachment
import com.intokapp.app.data.models.Conversation
import com.intokapp.app.data.models.Message
import com.intokapp.app.data.network.GiphyGif
import com.intokapp.app.data.network.GiphyService
import com.intokapp.app.data.repository.AttachmentRepository
import com.intokapp.app.data.repository.AttachmentUtils
import com.intokapp.app.data.repository.AuthRepository
import com.intokapp.app.data.repository.AuthState
import com.intokapp.app.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val typingUsers: Set<String> = emptySet(),
    val currentUserId: String? = null,
    val displayName: String = "Chat",
    val isLoading: Boolean = false,
    val hasMoreMessages: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val showAttachmentPicker: Boolean = false,
    val showGifPicker: Boolean = false,
    val showEmojiPicker: Boolean = false,
    val errorMessage: String? = null,
    // GIF state
    val gifs: List<GiphyGif> = emptyList(),
    val isLoadingGifs: Boolean = false,
    val gifSearchQuery: String = "",
    // Document translation dialog state
    val showDocumentTranslationDialog: Boolean = false,
    val pendingDocumentAttachment: Attachment? = null,
    // Reply state
    val replyingTo: Message? = null,
    // Delete message state
    val showDeleteDialog: Boolean = false,
    val messageToDelete: Message? = null,
    val isDeletingMessage: Boolean = false,
    // Download state
    val isDownloading: Boolean = false,
    val downloadSuccess: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val attachmentRepository: AttachmentRepository,
    private val giphyService: GiphyService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState
    
    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    _uiState.update { it.copy(currentUserId = state.user.id) }
                }
            }
        }
        
        viewModelScope.launch {
            chatRepository.messages.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        
        viewModelScope.launch {
            chatRepository.activeConversation.collect { conversation ->
                if (conversation != null) {
                    val displayName = if (conversation.type == "group") {
                        conversation.name ?: "Group Chat"
                    } else {
                        conversation.participants
                            .firstOrNull { it.id != _uiState.value.currentUserId }
                            ?.username ?: "Chat"
                    }
                    _uiState.update { it.copy(conversation = conversation, displayName = displayName) }
                }
            }
        }
        
        viewModelScope.launch {
            chatRepository.typingUsers.collect { typingMap ->
                val conversationId = _uiState.value.conversation?.id
                if (conversationId != null) {
                    _uiState.update { it.copy(typingUsers = typingMap[conversationId] ?: emptySet()) }
                }
            }
        }
        
        viewModelScope.launch {
            chatRepository.hasMoreMessages.collect { hasMore ->
                _uiState.update { it.copy(hasMoreMessages = hasMore) }
            }
        }
        
        viewModelScope.launch {
            chatRepository.isLoadingMessages.collect { isLoading ->
                _uiState.update { it.copy(isLoading = isLoading) }
            }
        }
    }
    
    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val conversations = chatRepository.conversations.value
            val conversation = conversations.find { it.id == conversationId }
            if (conversation != null) {
                chatRepository.selectConversation(conversation)
            }
        }
    }
    
    fun sendMessage(content: String, replyTo: Message? = null) {
        chatRepository.sendMessage(content, replyToMessage = replyTo ?: _uiState.value.replyingTo)
        // Clear reply state after sending
        _uiState.update { it.copy(replyingTo = null) }
    }
    
    // ============================================
    // Reply to Message
    // ============================================
    
    fun setReplyingTo(message: Message?) {
        _uiState.update { it.copy(replyingTo = message) }
    }
    
    fun clearReply() {
        _uiState.update { it.copy(replyingTo = null) }
    }
    
    // ============================================
    // Emoji Picker
    // ============================================
    
    fun showEmojiPicker() {
        _uiState.update { it.copy(showEmojiPicker = true) }
    }
    
    fun hideEmojiPicker() {
        _uiState.update { it.copy(showEmojiPicker = false) }
    }
    
    fun setTyping(isTyping: Boolean) {
        chatRepository.setTyping(isTyping)
    }
    
    fun loadMoreMessages() {
        viewModelScope.launch {
            chatRepository.loadMoreMessages()
        }
    }
    
    fun clearConversation() {
        chatRepository.clearActiveConversation()
    }
    
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    // ============================================
    // Reactions
    // ============================================
    
    fun sendReaction(message: Message, emoji: String) {
        chatRepository.sendReaction(message.id, message.createdAt, emoji)
    }
    
    // ============================================
    // Attachments
    // ============================================
    
    fun toggleAttachmentPicker() {
        _uiState.update { it.copy(showAttachmentPicker = !it.showAttachmentPicker, showGifPicker = false) }
    }
    
    fun hideAttachmentPicker() {
        _uiState.update { it.copy(showAttachmentPicker = false) }
    }
    
    fun toggleGifPicker() {
        val showGifs = !_uiState.value.showGifPicker
        _uiState.update { it.copy(showGifPicker = showGifs, showAttachmentPicker = false) }
        
        // Load trending GIFs when opening picker
        if (showGifs && _uiState.value.gifs.isEmpty()) {
            loadTrendingGifs()
        }
    }
    
    fun hideGifPicker() {
        _uiState.update { it.copy(showGifPicker = false) }
    }
    
    // ============================================
    // GIPHY Integration
    // ============================================
    
    private fun loadTrendingGifs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGifs = true) }
            try {
                val gifs = giphyService.getTrending()
                _uiState.update { it.copy(gifs = gifs, isLoadingGifs = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingGifs = false, errorMessage = "Failed to load GIFs") }
            }
        }
    }
    
    fun searchGifs(query: String) {
        _uiState.update { it.copy(gifSearchQuery = query) }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGifs = true) }
            try {
                val gifs = if (query.isBlank()) {
                    giphyService.getTrending()
                } else {
                    giphyService.search(query)
                }
                _uiState.update { it.copy(gifs = gifs, isLoadingGifs = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingGifs = false) }
            }
        }
    }
    
    fun sendGif(gif: GiphyGif) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, showGifPicker = false) }
            
            try {
                // For GIFs - backend handles skipping translation by type
                chatRepository.sendMessage(
                    content = "GIF",
                    type = "GIF",
                    attachment = mapOf(
                        "id" to "gif-${gif.id}",
                        "key" to gif.originalUrl,
                        "fileName" to "animated.gif",
                        "contentType" to "image/gif",
                        "fileSize" to 0L,
                        "category" to "image",
                        "url" to gif.originalUrl
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to send GIF. Please try again.") }
            } finally {
                _uiState.update { it.copy(isUploading = false) }
            }
        }
    }
    
    fun sendImage(uri: Uri) {
        val conversationId = _uiState.value.conversation?.id ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0f, showAttachmentPicker = false) }
            
            try {
                val attachment = attachmentRepository.uploadImage(
                    context = context,
                    uri = uri,
                    conversationId = conversationId,
                    onProgress = { progress ->
                        _uiState.update { it.copy(uploadProgress = progress) }
                    }
                )
                
                if (attachment != null) {
                    // Images - backend handles skipping translation by type
                    chatRepository.sendMessage(
                        content = attachment.fileName,
                        type = "IMAGE",
                        attachment = mapOf(
                            "id" to attachment.id,
                            "key" to attachment.key,
                            "fileName" to attachment.fileName,
                            "contentType" to attachment.contentType,
                            "fileSize" to attachment.fileSize,
                            "category" to attachment.category
                        )
                    )
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to upload image. Please try again.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to send image. Please try again.") }
            } finally {
                _uiState.update { it.copy(isUploading = false, uploadProgress = 0f) }
            }
        }
    }
    
    fun sendDocument(uri: Uri) {
        val conversationId = _uiState.value.conversation?.id ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadProgress = 0f, showAttachmentPicker = false) }
            
            try {
                val attachment = attachmentRepository.uploadDocument(
                    context = context,
                    uri = uri,
                    conversationId = conversationId,
                    onProgress = { progress ->
                        _uiState.update { it.copy(uploadProgress = progress) }
                    }
                )
                
                if (attachment != null) {
                    // Show translation dialog instead of sending immediately
                    _uiState.update { it.copy(
                        isUploading = false,
                        uploadProgress = 0f,
                        showDocumentTranslationDialog = true,
                        pendingDocumentAttachment = attachment
                    ) }
                } else {
                    _uiState.update { it.copy(
                        isUploading = false,
                        uploadProgress = 0f,
                        errorMessage = "Failed to upload document. Please try again."
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isUploading = false,
                    uploadProgress = 0f,
                    errorMessage = "Failed to send document. Please try again."
                ) }
            }
        }
    }
    
    fun confirmSendDocument(translateDocument: Boolean) {
        val attachment = _uiState.value.pendingDocumentAttachment ?: return
        
        chatRepository.sendMessage(
            content = attachment.fileName,
            type = "FILE",
            attachment = mapOf(
                "id" to attachment.id,
                "key" to attachment.key,
                "fileName" to attachment.fileName,
                "contentType" to attachment.contentType,
                "fileSize" to attachment.fileSize,
                "category" to attachment.category
            ),
            translateDocument = translateDocument
        )
        
        dismissDocumentDialog()
    }
    
    fun dismissDocumentDialog() {
        _uiState.update { it.copy(
            showDocumentTranslationDialog = false,
            pendingDocumentAttachment = null
        ) }
    }
    
    suspend fun getDownloadUrl(key: String): String? {
        return attachmentRepository.getDownloadUrl(key)
    }
    
    // ============================================
    // Delete Messages
    // ============================================
    
    fun showDeleteMessageDialog(message: Message) {
        _uiState.update { it.copy(showDeleteDialog = true, messageToDelete = message) }
    }
    
    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false, messageToDelete = null) }
    }
    
    fun deleteMessage(forEveryone: Boolean) {
        val message = _uiState.value.messageToDelete ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingMessage = true) }
            
            val result = chatRepository.deleteMessage(message.id, forEveryone)
            
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(
                        isDeletingMessage = false,
                        showDeleteDialog = false,
                        messageToDelete = null
                    ) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        isDeletingMessage = false,
                        errorMessage = "Failed to delete message: ${error.message}"
                    ) }
                }
            )
        }
    }
    
    // ============================================
    // Download Attachments
    // ============================================
    
    fun saveImageToGallery(message: Message) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            
            try {
                // Get the image URL - either from attachment or directly from key
                val imageUrl = message.attachment?.url 
                    ?: message.attachment?.key?.let { key ->
                        if (key.startsWith("http")) key else getDownloadUrl(key)
                    }
                    ?: message.originalContent.takeIf { it.startsWith("http") }
                
                if (imageUrl == null) {
                    _uiState.update { it.copy(
                        isDownloading = false,
                        errorMessage = "Unable to get image URL"
                    ) }
                    return@launch
                }
                
                val fileName = message.attachment?.fileName 
                    ?: "intok_image_${System.currentTimeMillis()}.jpg"
                
                val success = AttachmentUtils.saveImageToGallery(context, imageUrl, fileName)
                
                if (success) {
                    _uiState.update { it.copy(
                        isDownloading = false,
                        downloadSuccess = "Image saved to Gallery"
                    ) }
                } else {
                    _uiState.update { it.copy(
                        isDownloading = false,
                        errorMessage = "Failed to save image"
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isDownloading = false,
                    errorMessage = "Failed to save image: ${e.message}"
                ) }
            }
        }
    }
    
    fun downloadDocument(message: Message) {
        viewModelScope.launch {
            try {
                // Get the document URL - either from attachment or directly from key
                val downloadUrl = message.attachment?.url 
                    ?: message.attachment?.key?.let { key ->
                        if (key.startsWith("http")) key else getDownloadUrl(key)
                    }
                
                if (downloadUrl == null) {
                    _uiState.update { it.copy(errorMessage = "Unable to get download URL") }
                    return@launch
                }
                
                val fileName = message.attachment?.fileName 
                    ?: "intok_document_${System.currentTimeMillis()}"
                
                // Use DownloadManager for documents (shows notification)
                AttachmentUtils.downloadDocument(context, downloadUrl, fileName)
                
                _uiState.update { it.copy(downloadSuccess = "Download started") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to start download: ${e.message}") }
            }
        }
    }
    
    fun clearDownloadSuccess() {
        _uiState.update { it.copy(downloadSuccess = null) }
    }
}
