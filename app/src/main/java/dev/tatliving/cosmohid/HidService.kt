package dev.tatliving.cosmohid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

/**
 * Foreground service so the registered HID app and its host connection survive
 * while the user is in another app (e.g. editing on the Find N2).
 */
class HidService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        // Keep the CPU awake so keystrokes are sent without wake-up latency between presses.
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cosmohid:relay").apply {
            setReferenceCounted(false)
            acquire()
        }
        HidManager.start(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        HidManager.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL, "CosmoHID", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL) else
            @Suppress("DEPRECATION") Notification.Builder(this)
        return builder
            .setContentTitle("CosmoHID active")
            .setContentText("Relaying keyboard over Bluetooth")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL = "cosmohid"
        private const val NOTIF_ID = 1

        fun start(ctx: Context) {
            val i = Intent(ctx, HidService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, HidService::class.java))
        }
    }
}
