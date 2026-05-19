package pi.pocket.edition.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import pi.pocket.edition.MainActivity

class WifiLockService : Service() {

    companion object {
        const val CHANNEL_ID = "wifi_lock_service"
        const val NOTIFICATION_ID = 1002
        const val SCAN_INTERVAL_MS = 30_000L
    }

    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("WiFi Lock aktif - mencari jaringan target...")
        startForeground(NOTIFICATION_ID, notification)

        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                // TODO: Implement WiFi scan and auto-connect logic
                // 1. Check if connected to target SSID
                // 2. If not, scan available networks
                // 3. If target found, connect
                // 4. Wait 30 seconds
                delay(SCAN_INTERVAL_MS)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
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
            .setContentTitle("WiFi Lock")
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
                "WiFi Lock Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "WiFi Lock auto-connect service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
