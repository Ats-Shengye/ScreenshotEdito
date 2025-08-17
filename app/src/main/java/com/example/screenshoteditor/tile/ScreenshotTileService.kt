package com.example.screenshoteditor.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Intent
import com.example.screenshoteditor.capture.CaptureService

class ScreenshotTileService : TileService() {
    
    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }
    
    override fun onClick() {
        super.onClick()
        
        // タイルをクリックしたらCaptureServiceに撮影指示を送信
        val intent = Intent(this, CaptureService::class.java).apply {
            action = CaptureService.ACTION_CAPTURE
        }
        startService(intent)
    }
    
    private fun updateTileState() {
        qsTile?.let { tile ->
            tile.state = if (CaptureService.isRunning) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }
            tile.label = "スクリーンショット"
            tile.contentDescription = "スクリーンショットを撮影"
            tile.updateTile()
        }
    }
}