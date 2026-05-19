package pi.pocket.edition.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pi.pocket.edition.data.PrefsManager
import pi.pocket.edition.root.ChrootManager
import pi.pocket.edition.root.NetworkCommands
import pi.pocket.edition.root.RootManager
import pi.pocket.edition.ui.components.SectionHeader
import pi.pocket.edition.ui.components.StatusCard
import pi.pocket.edition.ui.theme.StatusGreen
import pi.pocket.edition.ui.theme.StatusRed

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    prefsManager: PrefsManager
) {
    val scope = rememberCoroutineScope()
    var isPiholeRunning by remember { mutableStateOf(false) }
    var isStarting by remember { mutableStateOf(false) }
    var isStopping by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var usbTetherActive by remember { mutableStateOf(false) }
    var ethTetherActive by remember { mutableStateOf(false) }

    val autoUsbTether by prefsManager.autoUsbTether.collectAsState(initial = false)
    val autoEthTether by prefsManager.autoEthTether.collectAsState(initial = false)

    // Check Pi-hole status periodically
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                isPiholeRunning = RootManager.isPort53InUse()
                usbTetherActive = RootManager.isUsbTetheringActive()
                ethTetherActive = RootManager.isEthTetheringActive()
            }
            delay(5000) // Check every 5 seconds
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Pi-hole Status Card
        SectionHeader(title = "Status")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPiholeRunning) StatusGreen else StatusRed
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPiholeRunning) "Active" else "Inactive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPiholeRunning) StatusGreen else StatusRed
                    )
                }

                // Status message
                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Start / Stop buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // START button with pre-check
                    Button(
                        onClick = {
                            isStarting = true
                            statusMessage = "Memeriksa tethering..."
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    // Step 1: Pre-check - disable tethering if active
                                    if (RootManager.isUsbTetheringActive() || RootManager.isEthTetheringActive()) {
                                        statusMessage = "Mematikan tethering (port 53)..."
                                        NetworkCommands.disableAllTethering()
                                        delay(2000) // Wait for port 53 to be freed
                                    }

                                    // Step 2: Start Pi-hole
                                    statusMessage = "Starting Pi-hole..."
                                    ChrootManager.startPihole()

                                    // Step 3: Wait for port 53
                                    statusMessage = "Menunggu port 53..."
                                    var retries = 0
                                    while (!RootManager.isPort53InUse() && retries < 15) {
                                        delay(1000)
                                        retries++
                                    }

                                    isPiholeRunning = RootManager.isPort53InUse()

                                    // Step 4: Re-enable tethering if configured
                                    if (isPiholeRunning) {
                                        if (autoUsbTether) {
                                            statusMessage = "Mengaktifkan USB tethering..."
                                            NetworkCommands.enableUsbTethering()
                                        }
                                        if (autoEthTether) {
                                            statusMessage = "Mengaktifkan Ethernet tethering..."
                                            NetworkCommands.enableEthTethering()
                                        }
                                        statusMessage = "Pi-hole running ✓"
                                    } else {
                                        statusMessage = "⚠ Pi-hole gagal start di port 53"
                                    }
                                }
                                isStarting = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isPiholeRunning && !isStarting && !isStopping,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                    ) {
                        if (isStarting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("START")
                    }

                    // STOP button
                    OutlinedButton(
                        onClick = {
                            isStopping = true
                            statusMessage = "Stopping Pi-hole..."
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    // Disable tethering first
                                    NetworkCommands.disableAllTethering()
                                    // Stop Pi-hole
                                    ChrootManager.stopPihole()
                                    delay(1000)
                                    isPiholeRunning = RootManager.isPort53InUse()
                                    statusMessage = if (!isPiholeRunning) "Pi-hole stopped" else "⚠ Gagal stop"
                                }
                                isStopping = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isPiholeRunning && !isStarting && !isStopping,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isStopping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Controls
        SectionHeader(title = "Quick Controls")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                title = "USB Tethering",
                value = if (usbTetherActive) "ON" else "OFF",
                icon = Icons.Default.Usb,
                isActive = usbTetherActive,
                modifier = Modifier.weight(1f)
            )
            StatusCard(
                title = "ETH Tethering",
                value = if (ethTetherActive) "ON" else "OFF",
                icon = Icons.Default.SettingsEthernet,
                isActive = ethTetherActive,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
