package com.cattailsw.mediaplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

private const val TAG = "ThumbnailExtractor"

object ThumbnailExtractor {

    suspend fun extractThumbnail(context: Context, videoUri: Uri): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)

            // Try to get a frame at time 0, scaled.
            // Scale to a width of 320, height will be proportional.
            // If original video is smaller, it won't scale up.
            val bitmap = retriever.getScaledFrameAtTime(
                0, // timeUs
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                320, // width
                180  // height, but retriever maintains aspect ratio, so this is more of a max_height
            )

            if (bitmap != null) {
                val filename = generateFilename(videoUri)
                val cacheDir = context.cacheDir
                val thumbnailFile = File(cacheDir, "$filename.jpg")

                FileOutputStream(thumbnailFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                bitmap.recycle() // Recycle bitmap after saving
                Log.d(TAG, "Thumbnail saved: ${thumbnailFile.absolutePath}")
                return@withContext thumbnailFile.absolutePath
            } else {
                Log.w(TAG, "Failed to extract bitmap from $videoUri")
                return@withContext null
            }
        } catch (e: Exception) {
            // Catching general exceptions: SecurityException, IllegalArgumentException, RuntimeException
            Log.e(TAG, "Error extracting thumbnail for $videoUri", e)
            return@withContext null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
    }

    private fun generateFilename(uri: Uri): String {
        // Create a hash of the URI string to use as a filename
        // This provides a consistent filename for the same URI
        val uriString = uri.toString()
        val digest = MessageDigest.getInstance("MD5")
        val hashedBytes = digest.digest(uriString.toByteArray(Charsets.UTF_8))
        return hashedBytes.joinToString("") {
            String.format("%02x", it)
        }
    }
}
