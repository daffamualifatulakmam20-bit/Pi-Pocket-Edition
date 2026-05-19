package pi.pocket.edition.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import pi.pocket.edition.data.PrefsManager
import pi.pocket.edition.root.ChrootManager
import pi.pocket.edition.root.NetworkCommands
import pi.pocket.edition.root.RootManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val prefsManager = PrefsManager(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val autoStart = prefsManager.autoStart.first()
                if (!autoStart) {
                    pendingResult.finish()
                    return@launch
                }

                val bootDelay = prefsManager.bootDelay.first()

                // Step 1: Wait for boot delay
                if (bootDelay > 0) {
                    delay(bootDelay * 1000L)
                }

                // Step 2: Pre-start safety - disable tethering
                NetworkCommands.disableAllTethering()

                // Step 3: Start Pi-hole
                ChrootManager.startPihole()

                // Step 4: Wait for port 53
                var retries = 0
                while (!RootManager.isPort53InUse() && retries < 30) {
                    delay(1000)
                    retries++
                }

                // Step 5: Re-enable tethering if configured
                val autoUsb = prefsManager.autoUsbTether.first()
                val autoEth = prefsManager.autoEthTether.first()

                if (autoUsb) NetworkCommands.enableUsbTethering()
                if (autoEth) NetworkCommands.enableEthTethering()

                // Step 6: Add custom IP if configured
                val ipEnabled = prefsManager.customIpEnabled.first()
                val customIp = prefsManager.customIp.first()
                if (ipEnabled && customIp.isNotEmpty()) {
                    NetworkCommands.addIpAddress(customIp)
                }

            } finally {
                pendingResult.finish()
            }
        }
    }
}
