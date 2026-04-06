package org.rwagsu.dtmp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// 1. 创建 DataStore 实例 (单例)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsManager {
    // 定义 Key
    private val MY_BOOL_KEY = booleanPreferencesKey("my_feature_enabled")

    // 读取变量 (返回 Flow，适合在 Compose 中观察)
    fun getMyBooleanFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data
            .map { preferences ->
                // 默认值为 false
                preferences[MY_BOOL_KEY] ?: false
            }
    }

    // 同步读取 (用于 BroadcastReceiver，因为它不在 Coroutine 作用域内)
    suspend fun getMyBooleanSync(context: Context): Boolean {
        return context.dataStore.data
            .map { preferences ->
                preferences[MY_BOOL_KEY] ?: false
            }.first() // 获取第一个值
    }

    // 保存变量
    suspend fun saveMyBoolean(context: Context, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MY_BOOL_KEY] = value
        }
    }
}