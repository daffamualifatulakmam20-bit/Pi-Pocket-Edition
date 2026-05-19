package pi.pocket.edition.ui.screen.network

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pi.pocket.edition.data.PrefsManager
import pi.pocket.edition.root.RootManager
import pi.pocket.edition.ui.components.SectionHeader
import pi.pocket.edition.ui.components.ToggleCard

@Composable
fun NetworkScreen(
    modifier: Modifier = Modifier,
    prefsManager: PrefsManager
) {
    val scope = rememberCoroutineScope()

    // Collect preferences
    val autoUsbTether by prefsManager.autoUsbTether.collectAsState(initial = false)
    val autoEthTether by prefsManager.autoEthTether.collectAsState(initial = false)
    val customIp by prefsManager.customIp.collectAsState(initial = "")
    val customIpEnabled by prefsManager.customIpEnabled.collectAsState(initial = false)
    val wifiLockSsid by prefsManager.wifiLockSsid.collectAsState(initial = "")
    val wifiLockPass by prefsManager.wifiLockPass.collectAsState(initial = "")
    val wifiLockEnabled by prefsManager.wifiLockEnabled.collectAsState(initial = false)

    // Local state
    var ipInput by remember(customIp) { mutableStateOf(customIp) }
    var ssidInput by remember(wifiLockSsid) { mutableStateOf(wifiLockSsid) }
    var passInput by remember(wifiLockPass) { mutableStateOf(wifiLockPass) }
    var showWifiPass by remember { mutableStateOf(false) }

    // Real Pi-hole status check
    var isPiholeRunning by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            isPiholeRunning = withContext(Dispatchers.IO) { RootManager.isPort53InUse() }
            delay(5000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Tethering Section
        SectionHeader(title = "Tethering")

        ToggleCard(
            title = "USB Tethering",
            description = "Share koneksi via USB setelah Pi-hole aktif",
            icon = Icons.Default.Usb,
            checked = autoUsbTether,
            onCheckedChange = { scope.launch { prefsManager.setAutoUsbTether(it) } },
            enabled = isPiholeRunning,
            warningText = "⚠ Pi-hole harus aktif terlebih dahulu (port 53)",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (Build.VERSION.SDK_INT >= 30) {
            ToggleCard(
                title = "Ethernet Tethering",
                description = "Share koneksi via Ethernet (Android 11+)",
                icon = Icons.Default.SettingsEthernet,
                checked = autoEthTether,
                onCheckedChange = { scope.launch { prefsManager.setAutoEthTether(it) } },
                enabled = isPiholeRunning,
                warningText = "⚠ Pi-hole harus aktif terlebih dahulu (port 53)",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Custom IP Section
        SectionHeader(title = "Custom IP Address")

        ToggleCard(
            title = "Auto-add IP saat WiFi",
            description = "Tambahkan IP custom ke wlan0 saat WiFi menyala",
            icon = Icons.Default.LocationOn,
            checked = customIpEnabled,
            onCheckedChange = {
                scope.launch {
                    prefsManager.setCustomIpEnabled(it)
                    if (it && ipInput.isNotEmpty()) {
                        prefsManager.setCustomIp(ipInput)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = ipInput,
            onValueChange = { ipInput = it },
            label = { Text("IP Address (contoh: 192.168.1.100/24)") },
            leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )

        if (ipInput != customIp && ipInput.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { scope.launch { prefsManager.setCustomIp(ipInput) } },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan IP")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // WiFi Lock Section
        SectionHeader(title = "WiFi Lock")

        ToggleCard(
            title = "WiFi Lock",
            description = "Auto-connect ke WiFi target setiap 30 detik",
            icon = Icons.Default.WifiLock,
            checked = wifiLockEnabled,
            onCheckedChange = {
                scope.launch {
                    prefsManager.setWifiLockEnabled(it)
                    if (it && ssidInput.isNotEmpty()) {
                        prefsManager.setWifiLock(ssidInput, passInput)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = ssidInput,
            onValueChange = { ssidInput = it },
            label = { Text("SSID WiFi Target") },
            leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = passInput,
            onValueChange = { passInput = it },
            label = { Text("Password WiFi") },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showWifiPass = !showWifiPass }) {
                    Icon(
                        if (showWifiPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (showWifiPass) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if ((ssidInput != wifiLockSsid || passInput != wifiLockPass) && ssidInput.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { scope.launch { prefsManager.setWifiLock(ssidInput, passInput) } },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan WiFi Target")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
