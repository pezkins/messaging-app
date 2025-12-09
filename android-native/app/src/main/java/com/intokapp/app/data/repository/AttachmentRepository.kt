package com.intokapp.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.intokapp.app.data.models.Attachment
import com.intokapp.app.data.network.ApiService
import com.intokapp.app.data.network.UploadUrlRequest
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.buffer
import okio.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RequestBody wrapper that reports upload progress
 */
private class ProgressRequestBody(
    private val delegate: RequestBody,
    private val contentLength: Long,
    private val onProgress: (Float) -> Unit
) : RequestBody() {
    
    override fun contentType(): MediaType? = delegate.contentType()
    
    override fun contentLength(): Long = contentLength
    
    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val bufferedSink = sink.buffer()
        var uploaded = 0L
        
        delegate.writeTo(object : okio.ForwardingSink(bufferedSink) {
            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                uploaded += byteCount
                val progress = uploaded.toFloat() / contentLength.toFloat()
                onProgress(progress.coerceIn(0f, 1f))
            }
        }.buffer())
        
        bufferedSink.flush()
    }
}

@Singleton
class AttachmentRepository @Inject constructor(
    private val apiService: ApiService,
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "AttachmentRepository"
    
    // Cached download URLs to avoid repeated API calls
    private val downloadUrlCache = mutableMapOf<String, Pair<String, Long>>()
    private val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    
    /**
     * Upload an image from URI to S3
     */
    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        conversationId: String,
        onProgress: ((Float) -> Unit)? = null
    ): Attachment? {
        return uploadFile(context, uri, conversationId, onProgress)
    }
    
    /**
     * Upload a document from URI to S3
     */
    suspend fun uploadDocument(
        context: Context,
        uri: Uri,
        conversationId: String,
        onProgress: ((Float) -> Unit)? = null
    ): Attachment? {
        return uploadFile(context, uri, conversationId, onProgress)
    }
    
    /**
     * Generic file upload with progress reporting
     */
    private suspend fun uploadFile(
        context: Context,
        uri: Uri,
        conversationId: String,
        onProgress: ((Float) -> Unit)? = null
    ): Attachment? = withContext(Dispatchers.IO) {
        try {
            // Get file info from URI
            val (fileName, fileSize, contentType) = getFileInfo(context, uri) ?: run {
                Log.e(TAG, "❌ Could not get file info")
                return@withContext null
            }
            
            Log.d(TAG, "📤 Uploading $fileName ($fileSize bytes, $contentType)")
            
            // Get presigned upload URL from backend
            val uploadUrlResponse = apiService.getUploadUrl(
                UploadUrlRequest(
                    fileName = fileName,
                    contentType = contentType,
                    fileSize = fileSize,
                    conversationId = conversationId
                )
            )
            
            Log.d(TAG, "✅ Got upload URL for ${uploadUrlResponse.key}")
            
            // Report initial progress
            onProgress?.invoke(0.1f)
            
            // Read file content
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: run {
                Log.e(TAG, "❌ Could not read file")
                return@withContext null
            }
            inputStream.close()
            
            // Report progress after reading file
            onProgress?.invoke(0.3f)
            
            // Upload to S3 with progress tracking
            val requestBody = ProgressRequestBody(
                bytes.toRequestBody(contentType.toMediaType()),
                bytes.size.toLong()
            ) { progress ->
                // Scale progress from 30% to 90% during upload
                onProgress?.invoke(0.3f + (progress * 0.6f))
            }
            
            val request = Request.Builder()
                .url(uploadUrlResponse.uploadUrl)
                .put(requestBody)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ S3 upload failed: ${response.code}")
                return@withContext null
            }
            
            // Report completion
            onProgress?.invoke(1.0f)
            
            Log.d(TAG, "✅ Uploaded to S3: ${uploadUrlResponse.key}")
            
            // Return attachment object
            Attachment(
                id = uploadUrlResponse.attachmentId,
                key = uploadUrlResponse.key,
                fileName = fileName,
                contentType = contentType,
                fileSize = fileSize,
                category = uploadUrlResponse.category
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload failed: ${e.message}")
            null
        }
    }
    
    /**
     * Get a presigned download URL for an attachment
     */
    suspend fun getDownloadUrl(key: String): String? = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cached = downloadUrlCache[key]
            if (cached != null && System.currentTimeMillis() - cached.second < CACHE_DURATION_MS) {
                return@withContext cached.first
            }
            
            // Get fresh URL from backend
            val response = apiService.getDownloadUrl(key)
            
            // Cache the URL
            downloadUrlCache[key] = response.downloadUrl to System.currentTimeMillis()
            
            response.downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get download URL: ${e.message}")
            null
        }
    }
    
    /**
     * Extract file info from URI
     */
    private fun getFileInfo(context: Context, uri: Uri): Triple<String, Long, String>? {
        return try {
            var fileName = "file"
            var fileSize = 0L
            
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    
                    if (nameIndex >= 0) fileName = cursor.getString(nameIndex)
                    if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
                }
            }
            
            val contentType = context.contentResolver.getType(uri) ?: getMimeTypeFromFileName(fileName)
            
            Triple(fileName, fileSize, contentType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get file info: ${e.message}")
            null
        }
    }
    
    /**
     * Fallback MIME type detection from file extension
     */
    private fun getMimeTypeFromFileName(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
