package pi.pocket.edition.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pi.pocket.edition.data.PrefsManager
import pi.pocket.edition.ui.components.SectionHeader
import pi.pocket.edition.ui.components.ToggleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefsManager: PrefsManager,
    onBack: () -> Unit,
    onNavigateToPassword: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isDarkTheme by prefsManager.isDarkTheme.collectAsState(initial = true)
    val autoStart by prefsManager.autoStart.collectAsState(initial = false)
    val bootDelay by prefsManager.bootDelay.collectAsState(initial = 30)

    var delayInput by remember(bootDelay) { mutableStateOf(bootDelay.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Appearance
            SectionHeader(title = "Tampilan")

            ToggleCard(
                title = "Dark Mode (AMOLED)",
                description = "Tema hitam murni untuk layar AMOLED",
                icon = Icons.Default.DarkMode,
                checked = isDarkTheme,
                onCheckedChange = { scope.launch { prefsManager.setDarkTheme(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Auto Start
            SectionHeader(title = "Startup")

            ToggleCard(
                title = "Auto-Start saat Boot",
                description = "Jalankan Pi-hole otomatis setelah perangkat boot",
                icon = Icons.Default.PowerSettingsNew,
                checked = autoStart,
                onCheckedChange = { scope.launch { prefsManager.setAutoStart(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Boot delay
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Delay setelah Boot",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Waktu tunggu sebelum start Pi-hole (0 = tanpa delay)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = delayInput,
                        onValueChange = { delayInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Detik") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (delayInput != bootDelay.toString()) {
                                TextButton(onClick = {
                                    val value = delayInput.toIntOrNull() ?: 30
                                    scope.launch { prefsManager.setBootDelay(value) }
                                }) {
                                    Text("Simpan")
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Security
            SectionHeader(title = "Keamanan")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                onClick = onNavigateToPassword
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ubah Password",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Pi-hole Admin & SSH",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About
            SectionHeader(title = "Tentang")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pi-Pocket Edition v1.0.0",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Pi-hole DNS manager for rooted Android",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
