package org.rwagsu.dtmp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.runBlocking

class MonitoringReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("OwOwO", "📢 收到广播: $action")

        // 获取当前开关状态（从 DataStore 同步读取）
        val isEnabled = runBlocking { SettingsManager.getMyBooleanSync(context) }

        if (!isEnabled) {
            Log.d("OwOwO", "💤 开关没开，不执行任何操作")
            return
        }

        when (action) {
            MonitoringScheduler.ACTION_START_MONITOR -> {
                Log.d("OwOwO", "⏰ 闹钟响了：06:00，启动监控！")
                MonitoringScheduler.startMonitorService(context)
                // 🔥 关键：触发后立刻预定明天的闹钟，实现循环
                MonitoringScheduler.updateSchedules(context, true)
            }
            MonitoringScheduler.ACTION_STOP_MONITOR -> {
                Log.d("OwOwO", "⏰ 闹钟响了：10:00，停止监控！")
                MonitoringScheduler.stopMonitorService(context)
                // 🔥 关键：触发后立刻预定明天的闹钟
                MonitoringScheduler.updateSchedules(context, true)
            }
            // 🔥 核心修复：当你在设置里手动改时间、改时区、或者重启手机时
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("OwOwO", "🕒 检测到系统时间变动或重启，重新计算状态...")
                MonitoringScheduler.updateSchedules(context, true)
            }
        }
    }
}