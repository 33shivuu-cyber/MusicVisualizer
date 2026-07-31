package com.example.musicvisualizer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "com.example.musicvisualizer.SHOW"
        const val ACTION_HIDE = "com.example.musicvisualizer.HIDE"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_POSITION = "position"
        private const val CHANNEL_ID = "visualizer_channel"
        private const val NOTIF_ID = 42
    }

    private lateinit var windowManager: WindowManager
    private var rootView: FrameLayout? = null
    private var cdView: SpinningCdView? = null
    private var sunView: SunBurstView? = null
    private var lyricsView: TextView? = null

    private var lyrics: List<LyricLine> = emptyList()
    private var trackStartWallClockMs: Long = 0L
    private var trackStartPositionMs: Long = 0L
    private var currentLineIndex = -1

    private val handler = Handler(Looper.getMainLooper())
    private val lyricsTicker = object : Runnable {
        override fun run() {
            updateLyricLine()
            handler.postDelayed(this, 250)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                startForeground(NOTIF_ID, buildNotification())
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Unknown"
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: ""
                val position = intent.getLongExtra(EXTRA_POSITION, 0L)
                showOverlay(title, artist, position)
            }
            ACTION_HIDE -> {
                hideOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(title: String, artist: String, positionMs: Long) {
        if (!Settings.canDrawOverlays(this)) return

        if (rootView == null) {
            buildViews()
        }

        cdView?.setAlbumArt(AlbumArtHolder.bitmap)
        cdView?.startSpin()
        sunView?.start()

        trackStartWallClockMs = System.currentTimeMillis()
        trackStartPositionMs = positionMs
        lyrics = emptyList()
        currentLineIndex = -1
        lyricsView?.text = "$title — $artist"

        LyricsFetcher.fetch(title, artist) { fetched ->
            lyrics = fetched
        }

        handler.removeCallbacks(lyricsTicker)
        handler.post(lyricsTicker)
    }

    private fun updateLyricLine() {
        if (lyrics.isEmpty()) return
        val elapsed = trackStartPositionMs + (System.currentTimeMillis() - trackStartWallClockMs)
        var idx = lyrics.indexOfLast { it.timeMs <= elapsed }
        if (idx == -1) idx = 0
        if (idx != currentLineIndex) {
            currentLineIndex = idx
            lyricsView?.apply {
                alpha = 0f
                scaleX = 0.9f
                scaleY = 0.9f
                text = lyrics[idx].text
                animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(250).start()
            }
        }
    }

    private fun buildViews() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 100
        }

        val container = FrameLayout(this).apply {
            setPadding(0, 40, 0, 40)
        }

        val sun = SunBurstView(this)
        val sunParams = FrameLayout.LayoutParams(500, 500).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        container.addView(sun, sunParams)

        val cd = SpinningCdView(this)
        val cdParams = FrameLayout.LayoutParams(320, 320).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        container.addView(cd, cdParams)

        val lyricsText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setShadowLayer(8f, 0f, 0f, Color.BLACK)
        }
        val lyricsParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            topMargin = 360
        }
        container.addView(lyricsText, lyricsParams)

        windowManager.addView(container, params)

        rootView = container
        cdView = cd
        sunView = sun
        lyricsView = lyricsText
    }

    private fun hideOverlay() {
        handler.removeCallbacks(lyricsTicker)
        cdView?.stopSpin()
        sunView?.stop()
        rootView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
        }
        rootView = null
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Visualizer running")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Visualizer", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
