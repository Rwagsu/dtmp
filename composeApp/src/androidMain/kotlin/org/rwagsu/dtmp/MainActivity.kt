package org.rwagsu.dtmp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    // 请求通知权限的 launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "GET! 开始搞怪吧！😈", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "啊...... 你是不是...... 点错了？😰", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 1. 创建通知渠道 (每次启动都调一下没关系，系统会去重)
        NotificationScheduler.createNotificationChannel(this)

        // 2. 请求通知权限 (Android 13+)
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 3. 设置闹钟 (每天 22:30)
        // 注意：每次启动 App 都会重置闹钟，确保它是最新的
        NotificationScheduler.scheduleDailyAlarm(this, 22, 30)

        // 4. 注册解锁监听器
        val unlockFilter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(UnlockReceiver(), unlockFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(UnlockReceiver(), unlockFilter)
        }

        // 5. 🔥 启动前台服务来监听加速度传感器 (这样即使应用划到后台也能继续工作)
        val sensorServiceIntent = Intent(this, SensorMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(sensorServiceIntent)
        } else {
            startService(sensorServiceIntent)
        }

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
