package com.sakuya.reader.tts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

enum class TtsPlaybackStatus { IDLE, PREPARING, PLAYING, PAUSED, ERROR }
data class TtsPlaybackState(val status: TtsPlaybackStatus = TtsPlaybackStatus.IDLE, val title: String = "", val errorMessage: String? = null)

object ReaderTtsPlayback {
    private val mutable = MutableStateFlow(TtsPlaybackState())
    val state = mutable.asStateFlow()
    internal fun update(value: TtsPlaybackState) { mutable.value = value }
}

/**
 * ReaderTtsService.kt
 * 职责说明：使用系统 TextToSpeech 在前台服务中分段朗读正文，并通过通知支持暂停、继续和停止。
 * 执行流程：Repository 写临时正文并启动服务 -> 服务按标点切块串行 speak -> utterance 回调推进；暂停保留块索引。
 */
class ReaderTtsService : Service(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var chunks: List<String> = emptyList()
    private var chunkIndex = 0
    private var title = "阅读朗读"
    private var speechRate = 1f
    private var ready = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        tts = TextToSpeech(this, this).apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) = fail("系统朗读失败")
                override fun onDone(utteranceId: String?) { chunkIndex++; speakCurrent() }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "阅读朗读" }
                speechRate = intent.getFloatExtra(EXTRA_RATE, 1f).coerceIn(0.5f, 2f)
                chunks = intent.getStringExtra(EXTRA_FILE)?.let(::File)?.takeIf(File::isFile)?.readText()?.toSpeechChunks().orEmpty()
                chunkIndex = 0
                ReaderTtsPlayback.update(TtsPlaybackState(TtsPlaybackStatus.PREPARING, title))
                startForeground(NOTIFICATION_ID, notification("正在准备朗读"))
                if (ready) speakCurrent()
            }
            ACTION_PAUSE -> {
                tts?.stop(); ReaderTtsPlayback.update(TtsPlaybackState(TtsPlaybackStatus.PAUSED, title)); refreshNotification("已暂停")
            }
            ACTION_RESUME -> if (ready) speakCurrent()
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) { fail("系统 TTS 初始化失败"); return }
        ready = true
        tts?.language = Locale.getDefault()
        if (chunks.isNotEmpty()) speakCurrent()
    }

    private fun speakCurrent() {
        if (chunkIndex >= chunks.size) { stopPlayback(); return }
        val engine = tts ?: return
        engine.setSpeechRate(speechRate)
        ReaderTtsPlayback.update(TtsPlaybackState(TtsPlaybackStatus.PLAYING, title))
        refreshNotification("正在朗读 ${chunkIndex + 1}/${chunks.size}")
        engine.speak(chunks[chunkIndex], TextToSpeech.QUEUE_FLUSH, null, "reader-$chunkIndex")
    }

    private fun stopPlayback() {
        tts?.stop(); chunks = emptyList(); chunkIndex = 0
        ReaderTtsPlayback.update(TtsPlaybackState())
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    private fun fail(message: String) {
        ReaderTtsPlayback.update(TtsPlaybackState(TtsPlaybackStatus.ERROR, title, message))
        refreshNotification(message)
    }

    private fun notification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setContentTitle(title).setContentText(text).setOnlyAlertOnce(true).setOngoing(true)
        .addAction(android.R.drawable.ic_media_pause, "暂停", serviceIntent(ACTION_PAUSE, 1))
        .addAction(android.R.drawable.ic_media_play, "继续", serviceIntent(ACTION_RESUME, 2))
        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", serviceIntent(ACTION_STOP, 3)).build()

    private fun serviceIntent(action: String, code: Int) = PendingIntent.getService(
        this, code, Intent(this, ReaderTtsService::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    private fun refreshNotification(text: String) { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text)) }
    private fun createChannel() { if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, "阅读朗读", NotificationManager.IMPORTANCE_LOW)) }
    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun String.toSpeechChunks(): List<String> = replace(Regex("<[^>]+>"), " ")
        .split(Regex("(?<=[。！？!?；;\n])"))
        .flatMap { part -> part.trim().chunked(800) }.filter(String::isNotBlank)

    companion object {
        const val ACTION_START = "com.sakuya.reader.tts.START"
        const val ACTION_PAUSE = "com.sakuya.reader.tts.PAUSE"
        const val ACTION_RESUME = "com.sakuya.reader.tts.RESUME"
        const val ACTION_STOP = "com.sakuya.reader.tts.STOP"
        const val EXTRA_FILE = "textFile"
        const val EXTRA_TITLE = "title"
        const val EXTRA_RATE = "rate"
        private const val CHANNEL_ID = "reader_tts"
        private const val NOTIFICATION_ID = 42021
    }
}
