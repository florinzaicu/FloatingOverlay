package nz.co.zsd.floatingoverlay

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

/**
 * Foreground service that displays the floating overlay. The overlay contains several controls
 * that allow the user to perform specific actions.
 */
class OverlayService : Service() {
    // Floating overlay view (UI shown on screen)
    private var overlay: OverlayView? = null

    /**
     * Broadcast receiver that will trigger the service to stop.
     */
    private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(LOG_TAG, "Broadcast receiver received a message with action ${intent.action}")
            when (intent.action) {
                // If the action is to exit the service, stop the service
                OVERLAY_SERVICE_ACTION_EXIT -> {
                    Log.i(LOG_TAG, "Received exit operation, stopping service")
                    context.stopService(Intent(context, OverlayService::class.java))
                }

                // If the action is to refresh the overlay, refresh the overlay
                OVERLAY_SERVICE_ACTION_REFRESH -> {
                    Log.i(LOG_TAG, "Received refresh operation, refreshing overlay")
                    overlay?.refreshUI()
                }

                // Otherwise log a warning and do nothing
                else -> {
                    Log.w(LOG_TAG, "Received unknown operation, ignoring")
                }
            }
        }
    }

    /**
     * On create register the broadcast receiver to allow sending intents to stop showing the overlay
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "On create called, registering broadcast receiver")

        // Register the broadcast receiver to listen for exit and refresh intents
        ContextCompat.registerReceiver(this, messageReceiver, IntentFilter(
            OVERLAY_SERVICE_ACTION_EXIT), ContextCompat.RECEIVER_EXPORTED)
        ContextCompat.registerReceiver(this, messageReceiver, IntentFilter(
            OVERLAY_SERVICE_ACTION_REFRESH), ContextCompat.RECEIVER_EXPORTED)
    }

    /**
     * Service received start command request. Check for the required
     * permissions, prepare the service notification and display the
     * overlay on screen.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "Service on start command called")
        try {
            // Display the overlay on screen
            showOverlay()

            // Create a pending intent triggered on exit notification action press and build the
            // overlay persistent notification
            val pendingExitIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(OVERLAY_SERVICE_ACTION_EXIT),
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, "1")
                .setContentTitle(getString(R.string.overlay_serv_notif_title))
                .setContentText(getString(R.string.overlay_serv_notif_text))
                .setSmallIcon(R.drawable.ic_logo_circ_mono)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(R.drawable.ic_close, getString(R.string.overlay_serv_notif_close_action), pendingExitIntent)

            // If the current version is Android O and above, create a notification chanel and add
            // it to the notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notification.setChannelId(packageName)
                val channel = NotificationChannel(
                    packageName,
                    getString(R.string.overlay_serv_notif_channel),
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
                Log.e(LOG_TAG, "Foreground service not allowed error thrown");
            } else {
                Log.e(LOG_TAG, "Error starting foreground service");
                Log.e(LOG_TAG, "Error: ${e.message}")
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
        Log.d(LOG_TAG, "Showing service overlay")

        // If the overlay already exists don't create a new instance
        if (overlay != null) return
        overlay = OverlayView(this)
    }

    /**
     * On destroy of the notification service clean up and hide the floating overlay
     */
    override fun onDestroy() {
        Log.d(LOG_TAG, "Service OnDestroy called, cleaning up")

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
        Log.d(LOG_TAG, "SERVICE: Hiding overlay (not null)")

        // Restore the brightness and destroy the old overlay
        overlay!!.destroy()
        overlay = null
    }

    companion object {
        // Tag to use when logging information to logcat
        private val LOG_TAG: String = OverlayView::class.simpleName ?: "OverlayService"

        /**
         * Broadcast intent action that tells the service to stop showing the overlay
         */
        const val OVERLAY_SERVICE_ACTION_EXIT       = "nz.co.zsd.floatingoverlay.SERVICE.EXIT"
        const val OVERLAY_SERVICE_ACTION_REFRESH    = "nz.co.zsd.floatingoverlay.SERVICE.REFRESH"

        fun broadcastRefreshUI (context: Context) {
            // Send a broadcast intent to the overlay service to refresh the UI
            context.sendBroadcast(Intent(OVERLAY_SERVICE_ACTION_REFRESH))
        }
    }
}