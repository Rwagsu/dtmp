package org.rwagsu.dtmp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EvilAlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val CHANNEL_ID = "evil_alarm_channel"

    // 用于管理音量锁的协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        Log.d("OwOwO", "🚩 Service: GO!!!") // 必须看有没有这一行
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("恭喜你又被耍了 🤣🥳🤪")
            .setContentText("没想到吧我更新啦 ψ(｀∇´)ψ")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX) // 提到最高
            .setCategory(NotificationCompat.CATEGORY_ALARM) // 设置为闹钟类别
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1001, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(1001, notification)
            }
            Log.d("OwOwO", "🚩 Service: startForeground GOOD!")
        } catch (e: Exception) {
            Log.e("OwOwO", "🚩 Service: startForeground 啊!!!: ${e.message}")
        }
    }

    private fun abandonAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        // 归还焦点，让系统知道我们不响了
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 这里需要持有之前的 focusRequest 对象才能优雅释放，
            // 简单处理也可以直接调这个旧 API，在大多数系统上有效
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    override fun onDestroy() {
        Log.d("OwOwO", "🚩 Service: onDestroy BOOM!!！")

        // 【修复点 2】：确保在销毁时，不管三七二十一，必须停掉声音
        stopAlarm()

        // 取消音量锁定协程
        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Log.d("OwOwO", "♻️ 系统重启了 Service，但不属于拔线触发，直接关闭")
            stopSelf()
            return START_NOT_STICKY
        }

        if (mediaPlayer == null || !mediaPlayer!!.isPlaying) {
            requestAudioFocus()

            playAlarmSound()

            // 🔥 核心逻辑：启动音量锁定（锁定 20 秒）
            lockVolume(20)
        }

        return START_STICKY
    }

    private fun lockVolume(seconds: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)

        serviceScope.launch {
            val endTime = System.currentTimeMillis() + (seconds * 1000)
            Log.d("OwOwO", "🔒 已经锁啦() ${seconds}s")

            while (System.currentTimeMillis() < endTime) {
                // 每隔 500 毫秒检查一次，如果音量被调低了，立刻拉回来
                val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
                if (currentVolume < maxVolume) {
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVolume, 0)
                    // Log.d("OwOwO", "💢 抓到你在调低音量！拉回满格！")
                }
                delay(500) // 0.5秒检查一次，反应极快
            }
            Log.d("OwOwO", "🔓 哇塞居然坚持那么久")
        }
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            audioManager.requestAudioFocus(null, android.media.AudioManager.STREAM_ALARM, android.media.AudioManager.AUDIOFOCUS_GAIN)
        }
    }


    private fun playAlarmSound() {
        try {
            // 1. 先强制把系统闹钟音量调到最大
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVolume, 0)

            // 2. 手动构建 MediaPlayer (不要直接用 MediaPlayer.create)
            mediaPlayer = MediaPlayer().apply {
                // 3. 必须在 setDataSource 之前或之后、prepare 之前设置属性
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM) // 强制使用闹钟流
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                // 4. 获取资源路径 (这里用 raw 资源)
                val afd = resources.openRawResourceFd(R.raw.relaxing_and_peaceful_morning_alarm)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                // 5. 关键：申请唤醒锁，防止播放时 CPU 睡觉
                setWakeMode(applicationContext, android.os.PowerManager.PARTIAL_WAKE_LOCK)

                isLooping = true
                prepare() // 同步准备
                start()   // 开始播放
            }
            Log.d("OwOwO", "🔊 Topsy-Turvy Topsy-Turvy GO!!!!!!!: $maxVolume")
        } catch (e: Exception) {
            Log.e("OwOwO", "💥 Topsy-Turvy BOOM! (不要啊!): ${e.message}", e)
        }
    }

    fun stopAlarm() {
        Log.d("OwOwO", "🚩 执行 stopAlarm...")
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop() // 停止播放
                }
                reset()   // 重置状态
                release() // 彻底释放资源 (关键！)
            }
            mediaPlayer = null

            // 释放音频焦点
            abandonAudioFocus()

            Log.d("OwOwO", "🚩 MediaPlayer 已彻底释放")
        } catch (e: Exception) {
            Log.e("OwOwO", "❌ 停止播放器时出错: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "一个普通的警...啊通知()",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "看什么呢, 没见过通知啊()"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}