package org.rwagsu.dtmp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.runBlocking

/**
 * 🔥 前台服务：用于在后台持续监听加速度传感器
 * 即使应用被划到后台，这个服务也会继续运行
 */
class SensorMonitorService : Service() {
    private var sensorManager: SensorManager? = null
    private var pickUpListener: PickUpSensorListener? = null
    private val CHANNEL_ID = "sensor_monitor_channel"

    override fun onCreate() {
        super.onCreate()
        Log.d("OwOwO", "🔍 [SensorService] 传感器服务启动啦!")
        
        createNotificationChannel()
        startForeground()
        
        // 初始化加速度传感器
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pickUpListener = PickUpSensorListener(this)
        
        // 注册加速度传感器监听器 - 使用 SENSOR_DELAY_FASTEST 以获得更快的响应
        sensorManager?.registerListener(
            pickUpListener,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST // 🔥 改为最快采样率
        )
        
        Log.d("OwOwO", "🔍 [SensorService] 传感器已注册")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("OwOwO", "🔍 [SensorService] onStartCommand")
        return START_STICKY // 确保服务被杀死后会自动重启
    }

    override fun onDestroy() {
        Log.d("OwOwO", "🔍 [SensorService] 服务销毁，取消传感器注册")
        sensorManager?.unregisterListener(pickUpListener)
        super.onDestroy()
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("👀 正在监听着呢...")
            .setContentText("手机被拿起时会触发警报 ψ(｀∇´)ψ")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_LOW) // 低优先级，不打扰用户
            .setOngoing(true) // 不可滑动删除
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1002, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SENSORS)
            } else {
                startForeground(1002, notification)
            }
            Log.d("OwOwO", "🔍 [SensorService] 前台服务启动成功")
        } catch (e: Exception) {
            Log.e("OwOwO", "🔍 [SensorService] 前台服务启动失败：${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "传感器监控服务",
                NotificationManager.IMPORTANCE_LOW // 低重要性，不发出声音
            ).apply {
                description = "用于检测手机是否被拿起"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/**
 * 📱 加速度传感器监听器
 * 检测手机是否被拿起（通过加速度变化）
 */
class PickUpSensorListener(private val context: Context) : SensorEventListener {
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isInitialized = false
    private val THRESHOLD = 2.5f // 加速度变化阈值
    private val TIME_WINDOW = 3000L // 3 秒内检测变化
    private var lastTriggerTime = 0L

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (!isInitialized) {
            lastX = x
            lastY = y
            lastZ = z
            isInitialized = true
            return
        }

        val currentTime = System.currentTimeMillis()
        
        // 计算加速度变化量
        val delta = kotlin.math.sqrt(
            (x - lastX) * (x - lastX) +
            (y - lastY) * (y - lastY) +
            (z - lastZ) * (z - lastZ)
        )

        Log.d("OwOwO", "📱 [PickUp] Δ = $delta")

        // 如果变化超过阈值，且不在冷却时间内，触发闹钟
        if (delta > THRESHOLD && (currentTime - lastTriggerTime) > TIME_WINDOW) {
            Log.d("OwOwO", "📱 [PickUp] 手机被拿起了! Δ = $delta")
            checkAndTriggerAlarm(context)
            lastTriggerTime = currentTime
        }

        lastX = x
        lastY = y
        lastZ = z
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT) {
            Log.d("OwOwO", "🔓 [Unlock] UNLOCKED! STOP!")
            val serviceIntent = Intent(context, EvilAlarmService::class.java)
            context.stopService(serviceIntent)
        }
    }
}

// AlarmReceivers.kt

fun checkAndTriggerAlarm(context: Context) {
    // 1. 读取开关状态
    val isEnabled = runBlocking {
        try { SettingsManager.getMyBooleanSync(context) } catch (e: Exception) {
            Log.e("OwOwO", "❌ 哇...... 完啦", e)
            false
        }
    }

    if (!isEnabled) {
        Log.d("OwOwO", "⛔ [Check] 你忘记开啦！(つД`) ノ")
        return
    }

    // 2. 🔥 恢复时间检查 (早上 6:00 到 10:00 之间)
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY) // 24 小时制

    // hour in 6..9 意味着 06:00:00 到 09:59:59
    val isTimeValid = hour in 6..9

    Log.d("OwOwO", "⏰ [Check] 当前小时：$hour, 时间段是否有效：$isTimeValid")

    if (!isTimeValid) {
        Log.d("OwOwO", "😴 [Check] 现在才几点啊......")
        return
    }

    // 3. 启动服务 (接你之前的代码...)
    Log.d("OwOwO", "✅ [Check] ARE YOU READY?")
    val serviceIntent = Intent(context, EvilAlarmService::class.java)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    } catch (e: Exception) {
        Log.e("OwOwO", "💥 [Check] 啊！咋回事？?: ${e.message}", e)
    }
}
