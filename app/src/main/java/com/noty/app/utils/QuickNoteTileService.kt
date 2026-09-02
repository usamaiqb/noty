package com.noty.app.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.noty.app.R
import com.noty.app.ui.MainActivity

/**
 * Quick Settings Tile Service for quickly adding notes
 * Appears in the Quick Settings panel (swipe down from top)
 *
 * Usage: Users can add this tile to their Quick Settings by:
 * 1. Swipe down twice from the top of the screen
 * 2. Tap the edit button (pencil icon)
 * 3. Find "Quick Note" tile and drag it to the active tiles
 */
class QuickNoteTileService : TileService() {

    companion object {
        const val ACTION_ADD_NOTE = "com.noty.app.ACTION_ADD_NOTE_FROM_TILE"
    }

    override fun onStartListening() {
        super.onStartListening()
        // Update tile state when it becomes visible
        updateTileState()
    }

    // Below API 34 the only available overload is startActivityAndCollapse(Intent);
    // the PendingIntent overload (used on API 34+) does not exist there. The call is
    // correctly version-guarded, so the deprecation warning is suppressed.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    override fun onClick() {
        super.onClick()

        // Create intent to launch MainActivity with add note action
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_ADD_NOTE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Launch the activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (API 34+): Use startActivityAndCollapse with PendingIntent
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        } else {
            // Older versions: Direct start
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        qsTile?.apply {
            // Set tile to inactive state (appears as a neutral button, not toggled on)
            state = Tile.STATE_INACTIVE

            // Set the icon
            icon = Icon.createWithResource(applicationContext, R.drawable.ic_tile_note)

            // Set label
            label = getString(R.string.tile_add_note)

            // Set subtitle for Android 10+ (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = getString(R.string.tile_subtitle)
            }

            // Update the tile
            updateTile()
        }
    }
}
