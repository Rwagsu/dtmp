package org.rwagsu.dtmp

import android.app.*
import android.content.*
import android.hardware.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat

class SensorMonitorService : Service() {
    private var sensorManager: SensorManager? = null
    private var pickUpListener: PickUpSensorListener? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val CHANNEL_ID = "sensor_monitor_channel"

    override fun onCreate() {
        super.onCreate()

        // 1. 获取 WakeLock，防止 CPU 在熄屏时休眠
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Dtmp:SensorWakeLock")
        wakeLock?.acquire()

        // 2. 初始化传感器
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pickUpListener = PickUpSensorListener(this)

        // 在 SensorMonitorService 的 onCreate 或注册位置
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager?.registerListener(
            pickUpListener,
            sensor,
            1_000_000 // 1,000,000 us = 1s
        )

        Log.d("OwOwO", "🚀 后台监控服务已启动")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour !in 6..9) {
            stopSelf()
            return START_NOT_STICKY
        }

        // 创建前台通知（Android 8.0+ 必须）
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("手机防挪用监控中")
            .setContentText("正在监控加速度传感器...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .build()

        // 启动为前台服务
        startForeground(2002, notification)

        return START_STICKY // 被系统杀掉后尝试重启
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "监控服务", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(pickUpListener)
        wakeLock?.release()
        Log.d("OwOwO", "🛑 后台监控服务已停止")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}