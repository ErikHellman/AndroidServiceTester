package se.hellsoft.servicetester

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MyRemoteService : Service() {

    private val startIds = mutableListOf<Int>()
    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        log("MyRemoteService.onCreate called")
    }

    override fun onBind(intent: Intent?): IBinder? {
        log("MyRemoteService.onBind called")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        log("MyRemoteService.onUnbind called")
        return false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("MyRemoteService.onStartCommand called with startId $startId")
        startIds += startId
        val startForeground = intent?.getBooleanExtra(MainActivity.START_FOREGROUND, true) ?: true
        if (startForeground) {
            log("MyRemoteService starting in foreground")
            startForeground(NOTIFICATION_ID, defaultNotification(this))
        }

        return intent?.getIntExtra(MainActivity.START_RETURN_KEY, START_STICKY) ?: START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        log("MyRemoteService.onDestroy called")
    }

    inner class LocalBinder : IMyServiceInterface.Stub() {
        override fun myPid(): Int = android.os.Process.myPid()

        override fun startIds(): IntArray = startIds.toIntArray()

        override fun removeOldestStartId(): Int {
            return if (startIds.isNotEmpty()) {
                startIds.removeAt(0)
            } else {
                -1
            }
        }

        override fun callStopSelf(startId: Int) = stopSelf(startId)
    }

    companion object {
        const val NOTIFICATION_ID = 202

        const val CHANNEL_ID = "remote-service"

        fun defaultNotification(context: Context): Notification {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                    ?: NotificationChannel(CHANNEL_ID, "service", 0)
                notificationManager.createNotificationChannel(notificationChannel)
            }
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Remote Service")
                .setContentText("The Remote Service is running!")
                .setSmallIcon(R.drawable.ic_cloud_queue_black_24dp)
                .build()
        }
    }
}
