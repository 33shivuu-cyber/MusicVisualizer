package com.example.musicvisualizer

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

data class LyricLine(val timeMs: Long, val text: String)

object LyricsFetcher {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun fetch(title: String, artist: String, callback: (List<LyricLine>) -> Unit) {
        Thread {
            val lines = try {
                val q = URLEncoder.encode("$artist $title", "UTF-8")
                val url = "https://lrclib.net/api/search?q=$q"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@use emptyList<LyricLine>()
                    val arr = org.json.JSONArray(body)
                    if (arr.length() == 0) return@use emptyList<LyricLine>()
                    val first = arr.getJSONObject(0)
                    val synced = first.optString("syncedLyrics", "")
                    parseLrc(synced)
                }
            } catch (e: Exception) {
                emptyList()
            }
            mainHandler.post { callback(lines) }
        }.start()
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        if (lrc.isBlank()) return emptyList()
        val regex = Regex("""\[(\d+):(\d+)\.(\d+)]\s*(.*)""")
        val result = mutableListOf<LyricLine>()
        lrc.lines().forEach { line ->
            val match = regex.find(line) ?: return@forEach
            val (min, sec, hundredths, text) = match.destructured
            val timeMs = min.toLong() * 60000 + sec.toLong() * 1000 + hundredths.toLong() * 10
            if (text.isNotBlank()) result.add(LyricLine(timeMs, text))
        }
        return result.sortedBy { it.timeMs }
    }
}
