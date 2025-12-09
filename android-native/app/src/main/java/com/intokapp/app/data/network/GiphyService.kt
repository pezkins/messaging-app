package com.intokapp.app.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intokapp.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing a GIF from GIPHY
 */
data class GiphyGif(
    val id: String,
    val previewUrl: String,  // Small preview for grid display
    val originalUrl: String, // Full size for sending
    val width: Int,
    val height: Int
)

/**
 * GIPHY API response models
 */
private data class GiphyResponse(
    val data: List<GiphyData>,
    val pagination: GiphyPagination?
)

private data class GiphyData(
    val id: String,
    val images: GiphyImages
)

private data class GiphyImages(
    @SerializedName("fixed_width")
    val fixedWidth: GiphyImageData?,
    val original: GiphyImageData?,
    @SerializedName("preview_gif")
    val previewGif: GiphyImageData?
)

private data class GiphyImageData(
    val url: String,
    val width: String?,
    val height: String?
)

private data class GiphyPagination(
    val offset: Int,
    val count: Int,
    @SerializedName("total_count")
    val totalCount: Int
)

/**
 * Service for interacting with GIPHY API
 */
@Singleton
class GiphyService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "GiphyService"
    private val gson = Gson()
    private val baseUrl = "https://api.giphy.com/v1/gifs"
    
    // Cache trending GIFs
    private var trendingCache: List<GiphyGif>? = null
    private var trendingCacheTime: Long = 0
    private val CACHE_DURATION = 5 * 60 * 1000L // 5 minutes
    
    /**
     * Get trending GIFs
     */
    suspend fun getTrending(limit: Int = 25): List<GiphyGif> = withContext(Dispatchers.IO) {
        // Check cache
        val now = System.currentTimeMillis()
        trendingCache?.let { cached ->
            if (now - trendingCacheTime < CACHE_DURATION) {
                return@withContext cached
            }
        }
        
        val apiKey = BuildConfig.GIPHY_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "GIPHY API key not configured")
            return@withContext emptyList()
        }
        
        try {
            val url = "$baseUrl/trending?api_key=$apiKey&limit=$limit&rating=g"
            val request = Request.Builder().url(url).build()
            
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "GIPHY trending request failed: ${response.code}")
                return@withContext emptyList()
            }
            
            val body = response.body?.string() ?: return@withContext emptyList()
            val giphyResponse = gson.fromJson(body, GiphyResponse::class.java)
            
            val gifs = giphyResponse.data.mapNotNull { data ->
                val preview = data.images.fixedWidth ?: data.images.previewGif
                val original = data.images.original ?: data.images.fixedWidth
                
                if (preview?.url != null && original?.url != null) {
                    GiphyGif(
                        id = data.id,
                        previewUrl = preview.url,
                        originalUrl = original.url,
                        width = original.width?.toIntOrNull() ?: 200,
                        height = original.height?.toIntOrNull() ?: 200
                    )
                } else null
            }
            
            // Update cache
            trendingCache = gifs
            trendingCacheTime = now
            
            Log.d(TAG, "✅ Loaded ${gifs.size} trending GIFs")
            gifs
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load trending GIFs: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Search for GIFs
     */
    suspend fun search(query: String, limit: Int = 25): List<GiphyGif> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext getTrending(limit)
        }
        
        val apiKey = BuildConfig.GIPHY_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "GIPHY API key not configured")
            return@withContext emptyList()
        }
        
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$baseUrl/search?api_key=$apiKey&q=$encodedQuery&limit=$limit&rating=g"
            val request = Request.Builder().url(url).build()
            
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "GIPHY search request failed: ${response.code}")
                return@withContext emptyList()
            }
            
            val body = response.body?.string() ?: return@withContext emptyList()
            val giphyResponse = gson.fromJson(body, GiphyResponse::class.java)
            
            val gifs = giphyResponse.data.mapNotNull { data ->
                val preview = data.images.fixedWidth ?: data.images.previewGif
                val original = data.images.original ?: data.images.fixedWidth
                
                if (preview?.url != null && original?.url != null) {
                    GiphyGif(
                        id = data.id,
                        previewUrl = preview.url,
                        originalUrl = original.url,
                        width = original.width?.toIntOrNull() ?: 200,
                        height = original.height?.toIntOrNull() ?: 200
                    )
                } else null
            }
            
            Log.d(TAG, "✅ Found ${gifs.size} GIFs for '$query'")
            gifs
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to search GIFs: ${e.message}")
            emptyList()
        }
    }
}
