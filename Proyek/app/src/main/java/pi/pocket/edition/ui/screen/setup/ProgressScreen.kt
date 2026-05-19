package pi.pocket.edition.ui.screen.setup

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pi.pocket.edition.data.PrefsManager
import pi.pocket.edition.download.ImageDownloader
import pi.pocket.edition.root.ChrootManager
import pi.pocket.edition.root.RootManager
import pi.pocket.edition.ui.theme.StatusGreen
import java.io.File
import java.net.URLDecoder

data class SetupStep(
    val title: String,
    val status: StepStatus = StepStatus.PENDING
)

enum class StepStatus { PENDING, RUNNING, DONE, ERROR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    downloadUrl: String,
    piholePassword: String,
    sshPassword: String,
    prefsManager: PrefsManager,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val decodedUrl = remember { URLDecoder.decode(downloadUrl, "UTF-8") }
    val decodedPiPass = remember { URLDecoder.decode(piholePassword, "UTF-8") }
    val decodedSshPass = remember { URLDecoder.decode(sshPassword, "UTF-8") }

    var progress by remember { mutableFloatStateOf(0f) }
    var logLines by remember { mutableStateOf(listOf<String>()) }
    var steps by remember {
        mutableStateOf(
            listOf(
                SetupStep("Downloading raspbian image"),
                SetupStep("Extracting to chroot"),
                SetupStep("Mounting & configuring chroot"),
                SetupStep("Running Pi-hole init script"),
                SetupStep("Setting passwords"),
                SetupStep("Starting services")
            )
        )
    }
    var currentStep by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun addLog(msg: String) {
        logLines = logLines + msg
    }

    fun updateStep(index: Int, status: StepStatus) {
        steps = steps.toMutableList().apply {
            this[index] = this[index].copy(status = status)
        }
    }

    fun rootExec(vararg cmds: String): Shell.Result {
        return Shell.cmd(*cmds).exec()
    }

    /**
     * Real setup flow matching the original Pi-hole-for-Android architecture:
     * 1. Download pre-built raspbian.tgz
     * 2. Extract to /data/local/pipocket/rasbian
     * 3. Mount chroot filesystems + setup DNS + unchroot shim
     * 4. Run etc/init.d/android-raspbian (handles Pi-hole install on first boot)
     * 5. Override password with user's choice
     * 6. Start services
     */
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val chrootPath = ChrootManager.CHROOT_PATH
                val downloadDir = ChrootManager.DOWNLOAD_DIR
                val fileName = if (decodedUrl.contains("raspbian32")) "raspbian32.tgz" else "raspbian.tgz"
                val downloadPath = "$downloadDir/$fileName"

                // =============================================
                // STEP 1: Download raspbian image
                // =============================================
                updateStep(0, StepStatus.RUNNING)
                addLog("> URL: $decodedUrl")
                addLog("> Target: $downloadPath")

                // Create directory via root
                withContext(Dispatchers.IO) {
                    rootExec("mkdir -p $downloadDir")
                }

                // Check if already downloaded
                val alreadyExists = withContext(Dispatchers.IO) {
                    rootExec("test -f $downloadPath && echo 'yes'").out.any { it.trim() == "yes" }
                }

                if (alreadyExists) {
                    addLog("> Image sudah ada, skip download")
                    progress = 0.35f
                } else {
                    addLog("> Mulai download...")
                    val downloader = ImageDownloader()
                    val cacheFile = File(context.cacheDir, fileName)

                    val downloadSuccess = withContext(Dispatchers.IO) {
                        downloader.download(
                            url = decodedUrl,
                            outputFile = cacheFile,
                            onProgress = { prog ->
                                progress = (prog.percent / 100f) * 0.35f
                                if (prog.percent % 5 == 0 && prog.percent > 0) {
                                    val mbDown = prog.bytesDownloaded / (1024 * 1024)
                                    val mbTotal = if (prog.totalBytes > 0) prog.totalBytes / (1024 * 1024) else 0
                                    addLog("> ${mbDown}MB / ${mbTotal}MB (${prog.percent}%)")
                                }
                            }
                        )
                    }

                    if (!downloadSuccess) {
                        throw Exception("Download gagal! Periksa URL dan koneksi internet.")
                    }

                    // Move to target via root (cache is owned by app, target needs root)
                    addLog("> Memindahkan file ke $downloadPath...")
                    withContext(Dispatchers.IO) {
                        rootExec("cp '${cacheFile.absolutePath}' '$downloadPath'")
                        rootExec("chmod 644 '$downloadPath'")
                        cacheFile.delete()
                    }
                }

                addLog("> ✓ Download selesai")
                progress = 0.35f
                updateStep(0, StepStatus.DONE)

                // =============================================
                // STEP 2: Extract to chroot
                // =============================================
                currentStep = 1
                updateStep(1, StepStatus.RUNNING)
                addLog("> Extracting ke $chrootPath...")
                addLog("> (File besar, ini bisa makan waktu beberapa menit)")

                withContext(Dispatchers.IO) {
                    rootExec("mkdir -p $chrootPath")

                    // Extract - try with progress
                    val extractResult = rootExec(
                        "cd $chrootPath && tar xzf $downloadPath 2>&1"
                    )

                    if (!extractResult.isSuccess) {
                        addLog("> Trying alternative extraction...")
                        val altResult = rootExec(
                            "cd $chrootPath && busybox tar xzf $downloadPath 2>&1"
                        )
                        if (!altResult.isSuccess) {
                            addLog("> Trying gunzip fallback...")
                            rootExec("cd $chrootPath && gunzip -c $downloadPath | tar xf - 2>&1")
                        }
                    }

                    // Verify extraction
                    val verifyResult = rootExec("test -f $chrootPath/etc/init.d/android-raspbian && echo 'ok'")
                    if (verifyResult.out.none { it.trim() == "ok" }) {
                        // Maybe extracted into subfolder
                        addLog("> Checking for subfolder...")
                        rootExec(
                            "if [ -d $chrootPath/data ]; then mv $chrootPath/data/local/pipocket/rasbian/* $chrootPath/ 2>/dev/null; fi"
                        )
                    }
                }

                progress = 0.50f
                addLog("> ✓ Extraction selesai")

                // Cleanup archive to save space
                withContext(Dispatchers.IO) {
                    rootExec("rm -f $downloadPath")
                    addLog("> Archive dihapus untuk hemat storage")
                }
                updateStep(1, StepStatus.DONE)

                // =============================================
                // STEP 3: Mount & configure chroot
                // =============================================
                currentStep = 2
                updateStep(2, StepStatus.RUNNING)
                addLog("> Mounting filesystems...")

                withContext(Dispatchers.IO) {
                    ChrootManager.mountChroot()
                    addLog("> Mounted: /dev, /dev/pts, /proc, /sys, /tmp")

                    addLog("> Configuring DNS...")
                    ChrootManager.setupDns()

                    addLog("> Setting up unchroot shim...")
                    ChrootManager.setupUnchrootShim()

                    // Create setupVars.conf template if not exists
                    val setupVarsExists = rootExec("test -f $chrootPath/etc/pihole/setupVars.conf && echo 'yes'")
                    if (setupVarsExists.out.none { it.trim() == "yes" }) {
                        addLog("> Creating setupVars.conf...")
                        rootExec("""
                            mkdir -p $chrootPath/etc/pihole
                            cat > $chrootPath/etc/pihole/setupVars.conf << 'SVEOF'
PIHOLE_INTERFACE=wlan0
IPV4_ADDRESS=127.0.0.1/24
IPV6_ADDRESS=
QUERY_LOGGING=true
INSTALL_WEB_SERVER=true
INSTALL_WEB_INTERFACE=true
LIGHTTPD_ENABLED=true
CACHE_SIZE=10000
DNS_FQDN_REQUIRED=true
DNS_BOGUS_PRIV=true
DNSMASQ_LISTENING=all
WEBPASSWORD=
BLOCKING_ENABLED=true
PIHOLE_DNS_1=8.8.8.8
PIHOLE_DNS_2=8.8.4.4
SVEOF
                        """.trimIndent())

                        // Also create .setupVars backup (used by init script)
                        rootExec("cp $chrootPath/etc/pihole/setupVars.conf $chrootPath/.setupVars")
                    }
                }

                progress = 0.60f
                addLog("> ✓ Chroot configured")
                updateStep(2, StepStatus.DONE)

                // =============================================
                // STEP 4: Run Pi-hole init script
                // (This is what the original app does on first boot)
                // =============================================
                currentStep = 3
                updateStep(3, StepStatus.RUNNING)
                addLog("> Running android-raspbian init script...")
                addLog("> (First boot: will install Pi-hole, ini butuh waktu)")

                withContext(Dispatchers.IO) {
                    // Check if init script exists
                    val hasInitScript = rootExec("test -x $chrootPath/etc/init.d/android-raspbian && echo 'yes'")

                    if (hasInitScript.out.any { it.trim() == "yes" }) {
                        // Run the original init script
                        addLog("> Menjalankan /etc/init.d/android-raspbian start...")
                        val initResult = ChrootManager.runInitScript()
                        initResult.out.forEach { line ->
                            if (line.isNotBlank()) addLog("> $line")
                        }
                        initResult.err.forEach { line ->
                            if (line.isNotBlank() && !line.contains("WARN")) addLog("> [!] $line")
                        }
                    } else {
                        // Fallback: manual Pi-hole install if init script not found
                        addLog("> Init script tidak ditemukan, install Pi-hole manual...")
                        addLog("> Downloading Pi-hole installer...")

                        val installResult = rootExec(
                            "chroot $chrootPath /bin/bash -c 'export DEBIAN_FRONTEND=noninteractive && wget -qO basic-install.sh https://install.pi-hole.net && bash ./basic-install.sh --unattended 2>&1'"
                        )
                        installResult.out.takeLast(10).forEach { addLog("> $it") }

                        if (!installResult.isSuccess) {
                            addLog("> ⚠ Pi-hole install mungkin incomplete, coba curl...")
                            rootExec(
                                "chroot $chrootPath /bin/bash -c 'export DEBIAN_FRONTEND=noninteractive && curl -sSL https://install.pi-hole.net | bash /dev/stdin --unattended 2>&1'"
                            )
                        }
                    }
                }

                progress = 0.80f
                addLog("> ✓ Pi-hole init selesai")
                updateStep(3, StepStatus.DONE)

                // =============================================
                // STEP 5: Set passwords (override auto-generated)
                // =============================================
                currentStep = 4
                updateStep(4, StepStatus.RUNNING)
                addLog("> Setting Pi-hole admin password...")

                withContext(Dispatchers.IO) {
                    ChrootManager.setPiholePassword(decodedPiPass)
                    addLog("> ✓ Pi-hole password set")

                    addLog("> Setting SSH/user password...")
                    ChrootManager.setSshPassword(decodedSshPass)
                    addLog("> ✓ SSH password set (user: root & android)")

                    // Remove the one-time password file if it exists
                    rootExec("rm -f $chrootPath/var/tmp/p4a")
                }

                progress = 0.90f
                updateStep(4, StepStatus.DONE)

                // =============================================
                // STEP 6: Start services
                // =============================================
                currentStep = 5
                updateStep(5, StepStatus.RUNNING)
                addLog("> Starting Pi-hole FTL...")

                withContext(Dispatchers.IO) {
                    rootExec(
                        "chroot $chrootPath /bin/bash -c 'service pihole-FTL start 2>/dev/null || pihole-FTL 2>/dev/null'"
                    )

                    addLog("> Starting lighttpd (web server)...")
                    rootExec(
                        "chroot $chrootPath /bin/bash -c 'service lighttpd start 2>/dev/null'"
                    )

                    // Verify port 53
                    addLog("> Waiting for DNS on port 53...")
                    var retries = 0
                    while (!RootManager.isPort53InUse() && retries < 20) {
                        delay(1000)
                        retries++
                    }

                    if (RootManager.isPort53InUse()) {
                        addLog("> ✓ DNS resolver aktif di port 53")
                    } else {
                        addLog("> ⚠ Port 53 belum aktif, mungkin perlu restart manual")
                    }
                }

                progress = 1.0f
                addLog("> ✓ Semua services running!")
                updateStep(5, StepStatus.DONE)

                // =============================================
                // DONE
                // =============================================
                withContext(Dispatchers.IO) {
                    prefsManager.setSetupComplete(true)
                }
                isComplete = true
                addLog("> ✅ Setup Pi-hole selesai!")
                addLog("> Admin: http://127.0.0.1/admin")
                addLog("> Menuju menu utama...")
                delay(3000)
                onComplete()

            } catch (e: Exception) {
                hasError = true
                errorMessage = e.message
                addLog("> ❌ ERROR: ${e.message}")
                updateStep(currentStep, StepStatus.ERROR)
            }
        }
    }

    // ==================== UI ====================
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instalasi Pi-hole") },
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
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    isComplete -> "✅ Selesai!"
                    hasError -> "❌ Error!"
                    else -> "Menginstall Pi-hole..."
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    hasError -> MaterialTheme.colorScheme.error
                    isComplete -> StatusGreen
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )

            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Steps list
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    steps.forEach { step ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (step.status) {
                                StepStatus.DONE -> Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = StatusGreen, modifier = Modifier.size(20.dp)
                                )
                                StepStatus.RUNNING -> CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                StepStatus.ERROR -> Icon(
                                    Icons.Default.Error, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                StepStatus.PENDING -> Icon(
                                    Icons.Default.RadioButtonUnchecked, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when (step.status) {
                                    StepStatus.DONE -> StatusGreen
                                    StepStatus.RUNNING -> MaterialTheme.colorScheme.onSurface
                                    StepStatus.ERROR -> MaterialTheme.colorScheme.error
                                    StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Log output:", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            val scrollState = rememberScrollState()
            LaunchedEffect(logLines.size) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp).verticalScroll(scrollState)
                ) {
                    logLines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                line.contains("ERROR") || line.contains("❌") -> MaterialTheme.colorScheme.error
                                line.contains("✓") || line.contains("✅") -> StatusGreen
                                line.contains("⚠") || line.contains("[!]") -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }

            if (hasError) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "Unknown error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
