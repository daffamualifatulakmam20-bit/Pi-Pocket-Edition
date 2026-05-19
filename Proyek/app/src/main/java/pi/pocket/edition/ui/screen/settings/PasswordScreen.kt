package pi.pocket.edition.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pi.pocket.edition.root.ChrootManager
import pi.pocket.edition.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var piholePass by remember { mutableStateOf("") }
    var piholeConfirm by remember { mutableStateOf("") }
    var sshPass by remember { mutableStateOf("") }
    var sshConfirm by remember { mutableStateOf("") }
    var showPiPass by remember { mutableStateOf(false) }
    var showSshPass by remember { mutableStateOf(false) }
    var isChangingPiPass by remember { mutableStateOf(false) }
    var isChangingSshPass by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubah Password") },
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
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Pi-hole Password
            SectionHeader(title = "Pi-hole Admin")

            OutlinedTextField(
                value = piholePass,
                onValueChange = { piholePass = it },
                label = { Text("Password Baru") },
                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPiPass = !showPiPass }) {
                        Icon(if (showPiPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (showPiPass) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = piholeConfirm,
                onValueChange = { piholeConfirm = it },
                label = { Text("Konfirmasi Password") },
                visualTransformation = if (showPiPass) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = piholeConfirm.isNotEmpty() && piholePass != piholeConfirm
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    isChangingPiPass = true
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            ChrootManager.setPiholePassword(piholePass)
                        }
                        isChangingPiPass = false
                        if (success) {
                            Toast.makeText(context, "Password Pi-hole berhasil diubah ✓", Toast.LENGTH_SHORT).show()
                            piholePass = ""
                            piholeConfirm = ""
                        } else {
                            Toast.makeText(context, "Gagal mengubah password Pi-hole", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = piholePass.isNotEmpty() && piholePass == piholeConfirm && !isChangingPiPass
            ) {
                if (isChangingPiPass) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Ubah Password Pi-hole")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SSH Password
            SectionHeader(title = "SSH")

            OutlinedTextField(
                value = sshPass,
                onValueChange = { sshPass = it },
                label = { Text("Password Baru") },
                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showSshPass = !showSshPass }) {
                        Icon(if (showSshPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (showSshPass) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = sshConfirm,
                onValueChange = { sshConfirm = it },
                label = { Text("Konfirmasi Password") },
                visualTransformation = if (showSshPass) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = sshConfirm.isNotEmpty() && sshPass != sshConfirm
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    isChangingSshPass = true
                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            ChrootManager.setSshPassword(sshPass)
                        }
                        isChangingSshPass = false
                        if (success) {
                            Toast.makeText(context, "Password SSH berhasil diubah ✓", Toast.LENGTH_SHORT).show()
                            sshPass = ""
                            sshConfirm = ""
                        } else {
                            Toast.makeText(context, "Gagal mengubah password SSH", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = sshPass.isNotEmpty() && sshPass == sshConfirm && !isChangingSshPass
            ) {
                if (isChangingSshPass) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Ubah Password SSH")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
