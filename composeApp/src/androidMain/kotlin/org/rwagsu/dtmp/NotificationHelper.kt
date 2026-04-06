package org.rwagsu.dtmp

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking

// 1. 定义一个 Receiver，用于接收闹钟广播
class VariableCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // 1. 从 DataStore 读取最新状态
        // 因为 onReceive 不是 suspend 函数，我们需要用 runBlocking 或者启动一个协程
        // 对于简单的读取，runBlocking 是可以接受的，因为它很快
        val isVariableOpen = runBlocking {
            SettingsManager.getMyBooleanSync(context)
        }

        // 2. 判断逻辑：如果变量是 False (未打开)，则发送通知
        if (!isVariableOpen) {
            sendNotification(context)
        }

        NotificationScheduler.scheduleDailyAlarm(context, 22, 30)
    }

    private fun sendNotification(context: Context) {
        val channelId = "variable_check_channel"
        val notificationId = 1

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("是不是需要做点什么? 🤔")
            .setContentText("现在可以进入 Dtmp Mode 了吗?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // 🔥 新增：检查权限（解决 Lint 报错）
        val notificationManager = NotificationManagerCompat.from(context)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(notificationId, builder.build())
        } else {
            Log.w("OwOwO", "⚠️ 等一下没拿到权限啊😰")
        }
    }
}

// 2.  helper 对象，方便调用
object NotificationScheduler {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "变量检查提醒"
            val descriptionText = "用于提醒变量状态"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("variable_check_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, VariableCheckReceiver::class.java)

        // FLAG_IMMUTABLE 是 Android 12+ 必须的
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 计算触发时间
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            // 如果今天已过此时间，则设为明天
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 设置精确闹钟 (允许在休眠模式下唤醒)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
