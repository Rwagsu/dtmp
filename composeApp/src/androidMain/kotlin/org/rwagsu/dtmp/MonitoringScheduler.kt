package org.rwagsu.dtmp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object MonitoringScheduler {
    const val ACTION_START_MONITOR = "org.rwagsu.dtmp.START_MONITOR"
    const val ACTION_STOP_MONITOR = "org.rwagsu.dtmp.STOP_MONITOR"

    fun updateSchedules(context: Context, isEnabled: Boolean) {
        if (isEnabled) {
            // 1. 重新设定闹钟 (不管是今天还是明天)
            scheduleAlarm(context, 6, 0, ACTION_START_MONITOR, 101)
            scheduleAlarm(context, 10, 0, ACTION_STOP_MONITOR, 102)

            // 2. 核心逻辑：立即检查当前时间，决定 Service 现在的状态
            val calendar = java.util.Calendar.getInstance()
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

            if (hour in 6..9) {
                Log.d("OwOwO", "📍 当前在 6-10 点内，启动服务")
                startMonitorService(context)
            } else {
                Log.d("OwOwO", "📍 当前在监控时间外，确保服务已停止")
                stopMonitorService(context)
            }
        } else {
            // ... (之前取消闹钟的代码)
            stopMonitorService(context)
        }
    }

    private fun scheduleAlarm(context: Context, hour: Int, minute: Int, action: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MonitoringReceiver::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1) // 如果时间已过，定在明天
            }
        }

        // 使用精确闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, action: String, requestCode: Int) {
        val intent = Intent(context, MonitoringReceiver::class.java).apply { this.action = action }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }

    fun startMonitorService(context: Context) {
        val intent = Intent(context, SensorMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopMonitorService(context: Context) {
        context.stopService(Intent(context, SensorMonitorService::class.java))
    }
}