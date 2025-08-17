package com.example.screenshoteditor.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ClipboardShare(private val context: Context) {
    
    companion object {
        private const val AUTHORITY = "com.example.screenshoteditor.fileprovider"
        private const val CLIPBOARD_LABEL = "Screenshot"
    }
    
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val handler = Handler(Looper.getMainLooper())
    private var clearRunnable: Runnable? = null
    
    suspend fun copyImageToClipboard(bitmap: Bitmap, cacheFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            // Save bitmap to cache file
            val success = saveBitmapToFile(bitmap, cacheFile)
            if (!success) return@withContext false
            
            // Create URI for FileProvider
            val uri = FileProvider.getUriForFile(context, AUTHORITY, cacheFile)
            
            // Create ClipData with URI
            val clip = ClipData.newUri(context.contentResolver, CLIPBOARD_LABEL, uri)
            
            withContext(Dispatchers.Main) {
                clipboardManager.setPrimaryClip(clip)
            }
            
            // Grant read permission to all apps that might access clipboard
            context.grantUriPermission(
                "com.android.systemui", 
                uri, 
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun scheduleClear(delaySeconds: Int) {
        clearRunnable?.let { handler.removeCallbacks(it) }
        
        clearRunnable = Runnable {
            try {
                val emptyClip = ClipData.newPlainText("", "")
                clipboardManager.setPrimaryClip(emptyClip)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        handler.postDelayed(clearRunnable!!, delaySeconds * 1000L)
    }
    
    fun clearClipboardNow() {
        clearRunnable?.let { 
            handler.removeCallbacks(it)
            clearRunnable = null
        }
        
        try {
            val emptyClip = ClipData.newPlainText("", "")
            clipboardManager.setPrimaryClip(emptyClip)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun shareImage(bitmap: Bitmap, shareFile: File) = withContext(Dispatchers.IO) {
        try {
            val success = saveBitmapToFile(bitmap, shareFile)
            if (!success) return@withContext
            
            val uri = FileProvider.getUriForFile(context, AUTHORITY, shareFile)
            
            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = Intent.createChooser(shareIntent, "スクリーンショットを共有")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            file.parentFile?.mkdirs()
            val outputStream = FileOutputStream(file)
            outputStream.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}