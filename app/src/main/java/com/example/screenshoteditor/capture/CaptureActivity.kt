package com.example.screenshoteditor.capture

import androidx.activity.ComponentActivity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowMetrics
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResult // Added import
import androidx.activity.result.contract.ActivityResultContracts
import com.example.screenshoteditor.R
import com.example.screenshoteditor.data.TempCache
import com.example.screenshoteditor.data.SettingsDataStore
import com.example.screenshoteditor.ui.EditorActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
// Removed: import android.app.Activity

class CaptureActivity : ComponentActivity() { // Changed superclass

    companion object {
        private const val TAG = "CaptureActivity"
    }

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var settingsDataStore: SettingsDataStore
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val screenCapturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult -> // Added type annotation
        if (result.resultCode == RESULT_OK && result.data != null) {
            startScreenCapture(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, getString(R.string.message_permission_required), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting capture activity")

        try {
            settingsDataStore = SettingsDataStore(this)
            mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                ?: throw IllegalStateException("MediaProjectionManager not available")

            // Request screen capture permission
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            Log.d(TAG, "onCreate: Launching screen capture permission request")
            screenCapturePermissionLauncher.launch(captureIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, getString(R.string.message_screenshot_failed), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        Log.d(TAG, "startScreenCapture: resultCode=$resultCode")
        
        // Ensure CaptureService is running for MediaProjection
        if (!CaptureService.isRunning) {
            Log.d(TAG, "startScreenCapture: Starting CaptureService first")
            val serviceIntent = Intent(this, CaptureService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            // Wait briefly for service to start
            Handler(Looper.getMainLooper()).postDelayed({
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                continueScreenCapture()
            }, 500)
        } else {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            continueScreenCapture()
        }
    }
    
    private fun continueScreenCapture() {

        if (mediaProjection == null) {
            Log.e(TAG, "continueScreenCapture: Failed to get MediaProjection")
            Toast.makeText(this, getString(R.string.message_screenshot_failed), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Log.d(TAG, "continueScreenCapture: MediaProjection obtained successfully")
        
        // Apply delay if configured
        activityScope.launch {
            val settings = settingsDataStore.settings.first()
            if (!settings.immediateCapture && settings.delaySeconds > 0) {
                Log.d(TAG, "continueScreenCapture: Applying delay of ${settings.delaySeconds} seconds")
                delay(settings.delaySeconds * 1000L)
            }
            
            withContext(Dispatchers.Main) {
                performScreenCapture()
            }
        }
    }
    
    private fun performScreenCapture() {

        // Get display metrics using new API (Android 11+)
        val windowMetrics = windowManager.currentWindowMetrics
        val bounds = windowMetrics.bounds
        val width = bounds.width()
        val height = bounds.height()
        val density = resources.displayMetrics.densityDpi
        Log.d(TAG, "performScreenCapture: Display size=${width}x${height}, density=$density")

        // Create ImageReader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
        Log.d(TAG, "performScreenCapture: ImageReader created")

        // Create VirtualDisplay
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
        Log.d(TAG, "performScreenCapture: VirtualDisplay created")

        // Capture screenshot after a short delay
        Log.d(TAG, "performScreenCapture: Scheduling capture in 100ms")
        Handler(Looper.getMainLooper()).postDelayed({
            captureScreenshot()
        }, 100)
    }

    private fun captureScreenshot() {
        Log.d(TAG, "captureScreenshot: Starting capture process")
        try {
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                Log.d(TAG, "captureScreenshot: Image acquired, size=${image.width}x${image.height}")
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * image.width

                // Create bitmap
                val bitmap = Bitmap.createBitmap(
                    image.width + rowPadding / pixelStride,
                    image.height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                // Trim status bar
                Log.d(TAG, "captureScreenshot: Trimming status bar")
                val trimmedBitmap = trimStatusBar(bitmap)
                Log.d(TAG, "captureScreenshot: Trimmed bitmap size=${trimmedBitmap.width}x${trimmedBitmap.height}")

                // Save to temp file
                Log.d(TAG, "captureScreenshot: Saving to temp file")
                val tempFile = saveBitmapToTemp(trimmedBitmap)

                // Clean up resources
                cleanup()

                // Open editor
                if (tempFile != null) {
                    Log.d(TAG, "captureScreenshot: Opening editor with file: ${tempFile.absolutePath}")
                    openEditor(tempFile.absolutePath)
                } else {
                    Log.e(TAG, "captureScreenshot: Failed to save temp file")
                    Toast.makeText(this, getString(R.string.message_screenshot_failed), Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                Log.e(TAG, "captureScreenshot: Failed to acquire image from ImageReader")
                Toast.makeText(this, getString(R.string.message_screenshot_failed), Toast.LENGTH_SHORT).show()
                cleanup()
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "captureScreenshot: Exception occurred", e)
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.message_screenshot_failed), Toast.LENGTH_SHORT).show()
            cleanup()
            finish()
        }
    }

    private fun trimStatusBar(bitmap: Bitmap): Bitmap {
        val statusBarHeight = StatusBarInsets.getStatusBarHeight(this)
        return if (statusBarHeight > 0 && statusBarHeight < bitmap.height) {
            Bitmap.createBitmap(
                bitmap,
                0,
                statusBarHeight,
                bitmap.width,
                bitmap.height - statusBarHeight
            )
        } else {
            bitmap
        }
    }
    
    private fun saveBitmapToTemp(bitmap: Bitmap): File? {
        return try {
            val tempFile = TempCache.createTempFile(this)
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun openEditor(imagePath: String) {
        Log.d(TAG, "openEditor: Creating intent to EditorActivity")
        val intent = Intent(this, EditorActivity::class.java).apply {
            putExtra(EditorActivity.EXTRA_IMAGE_PATH, imagePath)
        }
        Log.d(TAG, "openEditor: Starting EditorActivity")
        startActivity(intent)
        finish()
    }
    
    private fun cleanup() {
        imageReader?.close()
        virtualDisplay?.release()
        mediaProjection?.stop()
        
        imageReader = null
        virtualDisplay = null
        mediaProjection = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cleanup()
        activityScope.cancel()
    }
}
