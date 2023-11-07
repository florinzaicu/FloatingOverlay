package nz.co.zsd.floatingvolume

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager




class OverlayService : Service() {
    private var overlay: OverlayView? = null

    /**
     * Service received start command request. Check for the required
     * permissions, prepare the service notification and display the
     * overlay on screen.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check that we have the required permissions
        // TODO: what permissions do we actually require?

        try {
            // Display the overlay on screen
            showOverlay()

            // Create the notification that will be displayed in the notification bar
            val notification = NotificationCompat.Builder(this, "1234")
                .setContentTitle("Floating Volume")
                .setContentText("Volume control overlay")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setColor(ContextCompat.getColor(this, R.color.purple_500))

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
                startForeground(1, notification.build())
            } else {
                startForeground(1, notification.build(),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    }
                )
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

        return START_STICKY
    }

    /**
     * Component wants to bind to the service. Do not allow binding.
     */
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun showOverlay() {
        Log.d(javaClass.simpleName, "SERVICE: Showing overlay")
        // If the overlay already exists don't create a new instance
        if (overlay != null) return
        overlay = OverlayView(this)
    }

    override fun onDestroy() {
        Log.d(javaClass.simpleName, "SERVICE: OnDestroy, cleaning up")
        hideOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun hideOverlay() {
        if (overlay == null) return
        Log.d(javaClass.simpleName, "SERVICE: Hiding overlay (not null)")

        // Restore the brightness and destroy the old overlay
        overlay!!.destroy()
        overlay = null
    }
}