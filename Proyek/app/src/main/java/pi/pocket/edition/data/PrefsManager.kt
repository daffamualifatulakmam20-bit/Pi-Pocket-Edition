package pi.pocket.edition.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pi_pocket_prefs")

class PrefsManager(private val context: Context) {

    companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        val KEY_THEME_SELECTED = booleanPreferencesKey("theme_selected")
        val KEY_BOOT_DELAY = intPreferencesKey("boot_delay")
        val KEY_AUTO_START = booleanPreferencesKey("auto_start")
        val KEY_AUTO_USB_TETHER = booleanPreferencesKey("auto_usb_tether")
        val KEY_AUTO_ETH_TETHER = booleanPreferencesKey("auto_eth_tether")
        val KEY_CUSTOM_IP = stringPreferencesKey("custom_ip")
        val KEY_CUSTOM_IP_ENABLED = booleanPreferencesKey("custom_ip_enabled")
        val KEY_WIFI_LOCK_SSID = stringPreferencesKey("wifi_lock_ssid")
        val KEY_WIFI_LOCK_PASS = stringPreferencesKey("wifi_lock_pass")
        val KEY_WIFI_LOCK_ENABLED = booleanPreferencesKey("wifi_lock_enabled")
        val KEY_DOWNLOAD_URL = stringPreferencesKey("download_url")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_THEME] ?: true }
    val isSetupComplete: Flow<Boolean> = context.dataStore.data.map { it[KEY_SETUP_COMPLETE] ?: false }
    val isThemeSelected: Flow<Boolean> = context.dataStore.data.map { it[KEY_THEME_SELECTED] ?: false }
    val bootDelay: Flow<Int> = context.dataStore.data.map { it[KEY_BOOT_DELAY] ?: 30 }
    val autoStart: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_START] ?: false }
    val autoUsbTether: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_USB_TETHER] ?: false }
    val autoEthTether: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_ETH_TETHER] ?: false }
    val customIp: Flow<String> = context.dataStore.data.map { it[KEY_CUSTOM_IP] ?: "" }
    val customIpEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_CUSTOM_IP_ENABLED] ?: false }
    val wifiLockSsid: Flow<String> = context.dataStore.data.map { it[KEY_WIFI_LOCK_SSID] ?: "" }
    val wifiLockPass: Flow<String> = context.dataStore.data.map { it[KEY_WIFI_LOCK_PASS] ?: "" }
    val wifiLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_WIFI_LOCK_ENABLED] ?: false }
    val downloadUrl: Flow<String> = context.dataStore.data.map { it[KEY_DOWNLOAD_URL] ?: "" }

    suspend fun setDarkTheme(value: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = value }
    }

    suspend fun setSetupComplete(value: Boolean) {
        context.dataStore.edit { it[KEY_SETUP_COMPLETE] = value }
    }

    suspend fun setThemeSelected(value: Boolean) {
        context.dataStore.edit { it[KEY_THEME_SELECTED] = value }
    }

    suspend fun setBootDelay(value: Int) {
        context.dataStore.edit { it[KEY_BOOT_DELAY] = value }
    }

    suspend fun setAutoStart(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_START] = value }
    }

    suspend fun setAutoUsbTether(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_USB_TETHER] = value }
    }

    suspend fun setAutoEthTether(value: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_ETH_TETHER] = value }
    }

    suspend fun setCustomIp(value: String) {
        context.dataStore.edit { it[KEY_CUSTOM_IP] = value }
    }

    suspend fun setCustomIpEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_CUSTOM_IP_ENABLED] = value }
    }

    suspend fun setWifiLock(ssid: String, pass: String) {
        context.dataStore.edit {
            it[KEY_WIFI_LOCK_SSID] = ssid
            it[KEY_WIFI_LOCK_PASS] = pass
        }
    }

    suspend fun setWifiLockEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_WIFI_LOCK_ENABLED] = value }
    }

    suspend fun setDownloadUrl(value: String) {
        context.dataStore.edit { it[KEY_DOWNLOAD_URL] = value }
    }
}
