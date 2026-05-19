package pi.pocket.edition.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import pi.pocket.edition.MainActivity
import pi.pocket.edition.data.PrefsManager
import pi.pocket.edition.root.NetworkCommands

class IpWatcherService : Service() {

    companion object {
        const val CHANNEL_ID = "ip_watcher_service"
        const val NOTIFICATION_ID = 1003
    }

    private var wifiReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Monitoring WiFi untuk auto-add IP...")
        startForeground(NOTIFICATION_ID, notification)

        wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(3000) // Wait for connection to establish
                        val prefs = PrefsManager(context)
                        val ip = prefs.customIp.first()
                        if (ip.isNotEmpty()) {
                            NetworkCommands.addIpAddress(ip)
                        }
                    }
                }
            }
        }

        registerReceiver(
            wifiReceiver,
            IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        )

        return START_STICKY
    }

    override fun onDestroy() {
        wifiReceiver?.let { unregisterReceiver(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IP Watcher")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "IP Watcher Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Auto-add IP address on WiFi connect"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
