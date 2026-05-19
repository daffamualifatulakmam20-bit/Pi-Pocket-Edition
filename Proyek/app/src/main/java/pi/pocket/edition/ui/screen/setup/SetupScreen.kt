package pi.pocket.edition.ui.screen.setup

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pi.pocket.edition.data.PrefsManager
import pi.pocket.edition.util.ArchDetector
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    prefsManager: PrefsManager,
    onStartSetup: (url: String, piholePass: String, sshPass: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val archLabel = remember { ArchDetector.getArchLabel() }
    var downloadUrl by remember { mutableStateOf(ArchDetector.getDefaultDownloadUrl()) }
    var piholePassword by remember { mutableStateOf("") }
    var sshPassword by remember { mutableStateOf("") }
    var showPiholePass by remember { mutableStateOf(false) }
    var showSshPass by remember { mutableStateOf(false) }
    var isCheckingRoot by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Setup Pi-hole",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Konfigurasi sebelum instalasi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Architecture info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Arsitektur Terdeteksi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = archLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Download URL
            OutlinedTextField(
                value = downloadUrl,
                onValueChange = { downloadUrl = it },
                label = { Text("Source URL") },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Pi-hole password
            OutlinedTextField(
                value = piholePassword,
                onValueChange = { piholePassword = it },
                label = { Text("Password Pi-hole Admin") },
                leadingIcon = {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { showPiholePass = !showPiholePass }) {
                        Icon(
                            imageVector = if (showPiholePass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password"
                        )
                    }
                },
                visualTransformation = if (showPiholePass) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // SSH password
            OutlinedTextField(
                value = sshPassword,
                onValueChange = { sshPassword = it },
                label = { Text("Password SSH") },
                leadingIcon = {
                    Icon(Icons.Default.Terminal, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { showSshPass = !showSshPass }) {
                        Icon(
                            imageVector = if (showSshPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password"
                        )
                    }
                },
                visualTransformation = if (showSshPass) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Start Setup button - now checks root first
            Button(
                onClick = {
                    isCheckingRoot = true
                    scope.launch {
                        val hasRoot = withContext(Dispatchers.IO) {
                            Shell.getShell() // This triggers the root permission dialog
                            Shell.isAppGrantedRoot() == true
                        }
                        isCheckingRoot = false

                        if (hasRoot) {
                            val encodedUrl = URLEncoder.encode(downloadUrl, "UTF-8")
                            val encodedPiPass = URLEncoder.encode(piholePassword.ifEmpty { "admin" }, "UTF-8")
                            val encodedSshPass = URLEncoder.encode(sshPassword.ifEmpty { "raspberry" }, "UTF-8")
                            prefsManager.setDownloadUrl(downloadUrl)
                            onStartSetup(encodedUrl, encodedPiPass, encodedSshPass)
                        } else {
                            Toast.makeText(context, "Root access diperlukan! Pastikan perangkat sudah di-root.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isCheckingRoot,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isCheckingRoot) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Memeriksa root...")
                } else {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mulai Setup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "⚠ Memerlukan akses Root",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
