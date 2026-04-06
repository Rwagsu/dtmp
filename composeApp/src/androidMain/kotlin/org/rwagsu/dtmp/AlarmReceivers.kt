package org.rwagsu.dtmp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.runBlocking

class PickUpSensorListener(private val context: Context) : SensorEventListener {
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isInitialized = false

    // 配置参数
    private val THRESHOLD = 3.5f       // 灵敏度阈值 (建议 3.0 - 5.0 之间)
    private val SAMPLE_INTERVAL = 1000L // 采样间隔：1000毫秒 (1秒)
    private val TRIGGER_COOLDOWN = 5000L // 触发冷却：5秒 (防止连续响)

    private var lastSampleTime = 0L    // 上次处理数据的时间
    private var lastTriggerTime = 0L   // 上次响铃的时间

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val currentTime = System.currentTimeMillis()

        // 🔥 核心省电逻辑：如果距离上次采样没到 1 秒，直接无视这条数据
        if (currentTime - lastSampleTime < SAMPLE_INTERVAL) {
            return
        }

        // 记录本次采样时间
        lastSampleTime = currentTime

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

        // 计算加速度变化量 (Δ)
        val delta = kotlin.math.sqrt(
            (x - lastX) * (x - lastX) +
                    (y - lastY) * (y - lastY) +
                    (z - lastZ) * (z - lastZ)
        )

        // 只有变化足够大，且不在冷却期内，才触发
        if (delta > THRESHOLD && (currentTime - lastTriggerTime) > TRIGGER_COOLDOWN) {
            Log.d("OwOwO", "📱 [PickUp] 检测到有效挪动! Δ = $delta")
            checkAndTriggerAlarm(context)
            lastTriggerTime = currentTime
        }

        // 更新坐标快照
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
        Log.d("OwOwO", "⛔ [Check] 你忘记开啦! (つД`)ノ")
        return
    }

    // 2. 🔥 恢复时间检查 (早上 6:00 到 10:00 之间)
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY) // 24小时制

    // hour in 6..9 意味着 06:00:00 到 09:59:59
    val isTimeValid = hour in 6..9

    Log.d("OwOwO", "⏰ [Check] 当前小时: $hour, 时间段是否有效: $isTimeValid")

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
        Log.e("OwOwO", "💥 [Check] 啊! 咋回事??: ${e.message}", e)
    }
}