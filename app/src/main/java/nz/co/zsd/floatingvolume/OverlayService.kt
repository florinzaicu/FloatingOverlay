package nz.co.zsd.floatingvolume

import android.app.ActivityManager
import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class OverlayService : Service() {
    private var overlay: OverlayView? = null

    /**
     * Broadcast receiver that will trigger a service state refresh.
     */
    private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(javaClass.simpleName, "Broadcast receiver received a message with action ${intent.action}")
            if (intent.action == BROADCAST_INTENT_ACTION_EXIT) {
                Log.i(javaClass.simpleName, "Stopping service")
                context.stopService(Intent(context, OverlayService::class.java))
            }
        }
    }

    /**
     * On create register the broadcast receiver to allow sending intents to stop showing the overlay
     */
    override fun onCreate() {
        super.onCreate()

        Log.d(javaClass.simpleName, "On create called, registering broadcast receiver")
        ContextCompat.registerReceiver(this, messageReceiver, IntentFilter(BROADCAST_INTENT_ACTION_EXIT), ContextCompat.RECEIVER_EXPORTED)
    }

    /**
     * Service received start command request. Check for the required
     * permissions, prepare the service notification and display the
     * overlay on screen.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(javaClass.simpleName, "Service on start command called")
        try {
            // Display the overlay on screen
            showOverlay()

            // Create a pending intent triggered on exit notification action press and build the
            // overlay persistent notification
            val pendingExitIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(BROADCAST_INTENT_ACTION_EXIT),
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, "1")
                .setContentTitle("Displaying Overlay")
                .setContentText("Floating volume controls are currently visible")
                .setSmallIcon(R.drawable.ic_logo_circ_mono)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(R.drawable.ic_close, "Close", pendingExitIntent)

            // If the current version is Android O and above, create a notification chanel and add
            // it to the notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification.setChannelId(packageName)
                val channel = NotificationChannel(
                    packageName,
                    "Overlay Service Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                // Older android build have no service type
                startForeground(1, notification.build())
            } else if (Build.VERSION.SDK_INT >= 34) {
                // Newer android builds require we specify a service type
                startForeground(1, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                // In-between android builds have no special use foreground services
                startForeground(1, notification.build(), 0)
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException) {
                Log.e(this::class.simpleName, "Foreground service not allowed error thrown");
            } else {
                Log.e(this::class.simpleName, "Error starting foreground service");
                Log.e(this::class.simpleName, "Error: ${e.message}")
                e.printStackTrace()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Component wants to bind to the service. Do not allow binding.
     */
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    /**
     * Show the floating overlay on screen if not already visible (instance not already set)
     */
    private fun showOverlay() {
        Log.d(javaClass.simpleName, "Showing service overlay")

        // If the overlay already exists don't create a new instance
        if (overlay != null) return
        overlay = OverlayView(this)
    }

    /**
     * On destroy of the notification service clean up and hide the floating overlay
     */
    override fun onDestroy() {
        Log.d(javaClass.simpleName, "Service OnDestroy called, cleaning up")

        // Unregister the broadcast receiver, hide the overlay and stop the foreground service
        unregisterReceiver(messageReceiver)
        hideOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /**
     * Destroy the overlay view and make the local reference null
     */
    private fun hideOverlay() {
        if (overlay == null) return
        Log.d(javaClass.simpleName, "SERVICE: Hiding overlay (not null)")

        // Restore the brightness and destroy the old overlay
        overlay!!.destroy()
        overlay = null
    }

    companion object {
        /**
         * Broadcast intent action that tells the service to stop showing the overlay
         */
        const val BROADCAST_INTENT_ACTION_EXIT = "nz.co.zsd.floatingvolume.SERVICE.EXIT"
    }
}