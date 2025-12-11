package com.intokapp.app.data.repository

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AttachmentUtils {
    private const val TAG = "AttachmentUtils"
    
    /**
     * Save an image to the device gallery
     * Uses MediaStore for Android 10+ (scoped storage)
     * 
     * @param context Application context
     * @param imageUrl URL of the image to download
     * @param fileName Name for the saved file
     * @return true if saved successfully, false otherwise
     */
    suspend fun saveImageToGallery(context: Context, imageUrl: String, fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📥 Downloading image: $imageUrl")
                
                // 1. Download image
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.connect()
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "❌ Failed to download image: HTTP ${connection.responseCode}")
                    return@withContext false
                }
                
                val inputStream = connection.inputStream
                val bytes = inputStream.readBytes()
                inputStream.close()
                connection.disconnect()
                
                Log.d(TAG, "✅ Downloaded ${bytes.size} bytes")
                
                // 2. Determine MIME type
                val mimeType = when {
                    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                    fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
                    fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
                    else -> "image/jpeg"
                }
                
                // 3. Save using MediaStore (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, sanitizeFileName(fileName))
                        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Intok")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    
                    uri?.let { imageUri ->
                        resolver.openOutputStream(imageUri)?.use { outputStream ->
                            outputStream.write(bytes)
                        }
                        
                        // Mark as complete
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                        
                        Log.d(TAG, "✅ Image saved to gallery: $imageUri")
                        return@withContext true
                    }
                } else {
                    // Legacy storage (Android 9 and below)
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val intokDir = File(picturesDir, "Intok")
                    if (!intokDir.exists()) {
                        intokDir.mkdirs()
                    }
                    
                    val file = File(intokDir, sanitizeFileName(fileName))
                    FileOutputStream(file).use { outputStream ->
                        outputStream.write(bytes)
                    }
                    
                    Log.d(TAG, "✅ Image saved to: ${file.absolutePath}")
                    return@withContext true
                }
                
                false
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save image to gallery", e)
                false
            }
        }
    }
    
    /**
     * Download a document using DownloadManager
     * Shows notification with download progress
     * 
     * @param context Application context
     * @param downloadUrl URL of the document to download
     * @param fileName Name for the saved file
     */
    fun downloadDocument(context: Context, downloadUrl: String, fileName: String) {
        try {
            Log.d(TAG, "📥 Starting document download: $downloadUrl")
            
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle(sanitizeFileName(fileName))
                setDescription("Downloading from Intok...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Intok/${sanitizeFileName(fileName)}")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            
            Log.d(TAG, "✅ Download started with ID: $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start document download", e)
        }
    }
    
    /**
     * Alternative document download for Android 10+ using MediaStore
     * 
     * @param context Application context
     * @param downloadUrl URL of the document to download
     * @param fileName Name for the saved file
     * @param mimeType MIME type of the document
     * @return true if saved successfully, false otherwise
     */
    suspend fun downloadDocumentToDownloads(
        context: Context,
        downloadUrl: String,
        fileName: String,
        mimeType: String = "application/octet-stream"
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📥 Downloading document: $downloadUrl")
                
                // 1. Download document
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.connect()
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "❌ Failed to download document: HTTP ${connection.responseCode}")
                    return@withContext false
                }
                
                val inputStream = connection.inputStream
                val bytes = inputStream.readBytes()
                inputStream.close()
                connection.disconnect()
                
                Log.d(TAG, "✅ Downloaded ${bytes.size} bytes")
                
                // 2. Save using MediaStore.Downloads (Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, sanitizeFileName(fileName))
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Intok")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    
                    uri?.let { downloadUri ->
                        resolver.openOutputStream(downloadUri)?.use { outputStream ->
                            outputStream.write(bytes)
                        }
                        
                        // Mark as complete
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(downloadUri, contentValues, null, null)
                        
                        Log.d(TAG, "✅ Document saved to downloads: $downloadUri")
                        return@withContext true
                    }
                } else {
                    // Legacy storage (Android 9 and below)
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val intokDir = File(downloadsDir, "Intok")
                    if (!intokDir.exists()) {
                        intokDir.mkdirs()
                    }
                    
                    val file = File(intokDir, sanitizeFileName(fileName))
                    FileOutputStream(file).use { outputStream ->
                        outputStream.write(bytes)
                    }
                    
                    Log.d(TAG, "✅ Document saved to: ${file.absolutePath}")
                    return@withContext true
                }
                
                false
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save document", e)
                false
            }
        }
    }
    
    /**
     * Sanitize filename to remove invalid characters
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }
}
