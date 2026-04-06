package org.rwagsu.dtmp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.runBlocking

class PowerDisconnectReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
            Log.d("OwOwO", "🔌 [Power] bang! 线被拔了!")

            // 申请临时唤醒锁，防止 Service 还没起来 CPU 就又睡着了
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Dtmp:WakeLock")
            wakeLock.acquire(10000) // 锁 10 秒

            checkAndTriggerAlarm(context)
        }
    }
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