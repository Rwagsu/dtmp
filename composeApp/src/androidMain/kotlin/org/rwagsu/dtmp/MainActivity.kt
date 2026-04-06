package org.rwagsu.dtmp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
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
            Toast.makeText(this, "GET! 开始搞怪吧! 😈", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "啊...... 你是不是...... 点错了? 😰", Toast.LENGTH_SHORT).show()
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

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        // 注意：Android 14+ 动态注册必须指定 RECEIVER_EXPORTED 或 RECEIVER_NOT_EXPORTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(PowerDisconnectReceiver(), filter, Context.RECEIVER_EXPORTED)
            registerReceiver(UnlockReceiver(), filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(PowerDisconnectReceiver(), filter)
            registerReceiver(UnlockReceiver(), filter)
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