package com.example.musicvisualizer

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NowPlayingListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private val controllers = mutableListOf<MediaController>()
    private var lastTrackKey: String? = null

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        rebindControllers(controllers ?: emptyList())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            handleState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            handleState()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        mediaSessionManager = getSystemService(MediaSessionManager::class.java)
        val component = ComponentName(this, NowPlayingListenerService::class.java)
        try {
            val active = mediaSessionManager?.getActiveSessions(component) ?: emptyList()
            rebindControllers(active)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, component)
        } catch (e: SecurityException) {
        }
    }

    private fun rebindControllers(newControllers: List<MediaController>) {
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers.clear()
        controllers.addAll(newControllers)
        controllers.forEach { it.registerCallback(controllerCallback) }
        handleState()
    }

    private fun handleState() {
        val playing = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }

        if (playing == null) {
            if (lastTrackKey != null) {
                lastTrackKey = null
                sendStop()
            }
            return
        }

        val metadata = playing.metadata ?: return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val key = "$title|$artist"

        val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        AlbumArtHolder.bitmap = art

        val prefs: SharedPreferences = getSharedPreferences("visualizer_prefs", MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        if (!enabled) return

        if (key != lastTrackKey) {
            lastTrackKey = key
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_SHOW
                putExtra(OverlayService.EXTRA_TITLE, title)
                putExtra(OverlayService.EXTRA_ARTIST, artist)
                putExtra(
                    OverlayService.EXTRA_POSITION,
                    playing.playbackState?.position ?: 0L
                )
            }
            startForegroundService(intent)
        }
    }

    private fun sendStop() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE
        }
        startService(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
    }
}

object AlbumArtHolder {
    var bitmap: android.graphics.Bitmap? = null
}
