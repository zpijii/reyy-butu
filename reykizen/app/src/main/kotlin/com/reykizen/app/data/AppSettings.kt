package com.reykizen.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rey_kizen_settings")

object PrefsKeys {
    val PRIVILEGE_MODE      = stringPreferencesKey("privilege_mode")       // ROOT | SHIZUKU | ADB
    val SELECTED_PRESET_ID  = stringPreferencesKey("selected_preset_id")
    val AUTO_APPLY          = booleanPreferencesKey("auto_apply")           // apply on BS launch
    val AUTO_RESTORE        = booleanPreferencesKey("auto_restore")         // restore on BS exit
    val SHOW_FLOATING       = booleanPreferencesKey("show_floating")        // show overlay panel
    val ADB_HOST            = stringPreferencesKey("adb_host")
    val ADB_PORT            = intPreferencesKey("adb_port")
    val CUSTOM_WIDTH        = intPreferencesKey("custom_width")
    val CUSTOM_HEIGHT       = intPreferencesKey("custom_height")
    val CUSTOM_DENSITY      = intPreferencesKey("custom_density")
}

enum class PrivilegeMode { ROOT, SHIZUKU, ADB }

data class ReyKizenPrefs(
    val privilegeMode: PrivilegeMode = PrivilegeMode.ROOT,
    val selectedPresetId: String = "bs_performance",
    val autoApply: Boolean = true,
    val autoRestore: Boolean = true,
    val showFloating: Boolean = true,
    val adbHost: String = "127.0.0.1",
    val adbPort: Int = 5555,
    val customWidth: Int = 720,
    val customHeight: Int = 1600,
    val customDensity: Int = 320
)

fun Context.prefsFlow(): Flow<ReyKizenPrefs> =
    dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            ReyKizenPrefs(
                privilegeMode   = runCatching {
                    PrivilegeMode.valueOf(p[PrefsKeys.PRIVILEGE_MODE] ?: "ROOT")
                }.getOrDefault(PrivilegeMode.ROOT),
                selectedPresetId = p[PrefsKeys.SELECTED_PRESET_ID] ?: "bs_performance",
                autoApply        = p[PrefsKeys.AUTO_APPLY]          ?: true,
                autoRestore      = p[PrefsKeys.AUTO_RESTORE]        ?: true,
                showFloating     = p[PrefsKeys.SHOW_FLOATING]       ?: true,
                adbHost          = p[PrefsKeys.ADB_HOST]            ?: "127.0.0.1",
                adbPort          = p[PrefsKeys.ADB_PORT]            ?: 5555,
                customWidth      = p[PrefsKeys.CUSTOM_WIDTH]        ?: 720,
                customHeight     = p[PrefsKeys.CUSTOM_HEIGHT]       ?: 1600,
                customDensity    = p[PrefsKeys.CUSTOM_DENSITY]      ?: 320
            )
        }

suspend fun Context.updatePrefs(block: suspend MutablePreferences.() -> Unit) {
    dataStore.edit { prefs -> prefs.block() }
}
