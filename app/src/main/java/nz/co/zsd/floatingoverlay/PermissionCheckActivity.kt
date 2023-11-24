package nz.co.zsd.floatingoverlay

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class PermissionCheckActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.permission_check_activity)
    }

    override fun onResume() {
        super.onResume()

        Log.i(LOG_TAG, "Resumed permission check activity. Checking permissions")
        var hasNotifyPerm: Boolean = hasNotifyPerm(this)
        var hasForegroundPerm: Boolean = hasForegroundPerm(this)
        var hasOverlayPerm: Boolean = hasDrawOverlayPerm(this)

        // If all permissions have been granted terminate the activity and return granted result
        if (hasNotifyPerm && hasForegroundPerm && hasOverlayPerm) {
            Log.i(LOG_TAG, "All permissions granted, closing permission check activity")
            setResult(PERMISSIONS_GRANTED)
            finish()
        }

        // Update UI if some permissions granted
        if (hasNotifyPerm) {
            Log.i(LOG_TAG, "Notification permission granted. Updating UI")
            findViewById<ImageView>(R.id.notify_perm_img).setImageResource(R.drawable.ic_check_circle)
            findViewById<TextView>(R.id.notify_perm_txt).setText(R.string.perm_granted_notify_msg)
        }

        if (hasForegroundPerm) {
            Log.i(LOG_TAG, "Foreground permission granted. Updating UI")
            findViewById<ImageView>(R.id.foreground_perm_img).setImageResource(R.drawable.ic_check_circle)
            findViewById<TextView>(R.id.foreground_perm_txt).setText(R.string.perm_granted_foreground_msg)
        }

        if (hasOverlayPerm) {
            Log.i(LOG_TAG, "Overlay permission granted. Updating UI")
            findViewById<ImageView>(R.id.draw_perm_img).setImageResource(R.drawable.ic_check_circle)
            findViewById<TextView>(R.id.draw_perm_txt).setText(R.string.perm_granted_overlay_msg)
        }
    }

    /**
     * If user pressed the back button terminate the activity and return the kill app result to the
     * invoking activity. The callling activity should terminate the app once this code is returned
     */
    override fun onBackPressed() {
        setResult(KILL_APPLICATION)
        finish()
    }

    /**
     * On press of grant permissions button request the required missing system permissions in
     * order they appear on UI. User needs to press several times to grant the system permissions
     */
    fun grantPermissionsButtonClick(@Suppress("UNUSED_PARAMETER") v: View) {
        if (!hasNotifyPerm(this)) {
            // Request notification permission
            Log.i(LOG_TAG, "Requesting notification permission")
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),PERMISSION_CHECK_REQUEST_CODE)
        }

        if (!hasDrawOverlayPerm(this)) {
            // Open system settings to allow user to grant draw over other apps permission
            Log.i(LOG_TAG, "Requesting draw over other apps permission")
            intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermReqRes.launch(intent)
        }
    }

    /**
     * Create am activity result handler for the request overlay permission setting. This handler
     * currently does not perform any checks as the onResume will update the UI
     */
    private val overlayPermReqRes = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    /**
     * On request permission result, check if permissions were granted
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_CHECK_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                    Log.i(LOG_TAG, "Request permission result, notify permission granted")
                    findViewById<ImageView>(R.id.notify_perm_img).setImageResource(R.drawable.ic_check_circle)
                    findViewById<TextView>(R.id.notify_perm_txt).setText(R.string.perm_granted_notify_msg)
                } else {
                    // Permission denied, show a message
                    Log.i(LOG_TAG, "Request permission result, notify permission denied")
                    Snackbar.make(this,
                        findViewById(R.id.notify_perm_img),
                        "Notification permission was denied!",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            else -> {
                // Unknown permission request result received
                Log.w(LOG_TAG, "Unknown result permission request code: $requestCode")
            }
        }
    }

    companion object {
        /**
         * Tag to use when logging information to logcat
         */
        private val LOG_TAG: String = PermissionCheckActivity::class.simpleName ?: "PermissionCheckActivity"
        
        /**
         * Activity return code: all permissions granted successfully (continue execution)
         */
        const val PERMISSIONS_GRANTED = 1
        // Kill return code (permissions were not granted and user wants to kill the app)
        /**
         * Activity return code: user wishes to terminate application execution (stop app)
         */
        const val KILL_APPLICATION = 2
        // Permission check activity spawn ID
        /**
         * Request ID for permission check activity to explain required permissions to user
         */
        const val EXPLAIN_REQUIRED_PERMISSIONS_ACTIVITY = 5
        // Request code used to swap settings activity to get required permissions
        private const val PERMISSION_CHECK_REQUEST_CODE = 10

        /**
         * Check if the application has all the required permissions granted. If any permissions
         * are missing, start the permission check activity to allow the user to grant them.
         * NOTE: Once permission check activity terminates, returns to previous activity (uses
         * start for result invocation). See `PERMISSIONS_GRANTED` and `KILL_APPLICATION` for
         * constant result IDs of activity.
         */
        /**
         * Check if the application has all required permissions to operate. If any permissions
         * are missing, call the activity result callback to start the permission check activity
         * and handle a response.
         * @return Boolean: True if all permissions granted, false otherwise
         */
        fun checkPermissions(app: Activity, contract: ActivityResultLauncher<Intent>): Boolean {
            Log.i(LOG_TAG,"Checking if app has all required permissions")
            var hasNotifyPerm: Boolean = hasNotifyPerm(app)
            var hasForegroundPerm: Boolean = hasForegroundPerm(app)
            var hasOverlayPerm: Boolean = hasDrawOverlayPerm(app)

            Log.d(LOG_TAG, "Permissions ${hasNotifyPerm} ${hasForegroundPerm} ${hasOverlayPerm}")
            if (!hasNotifyPerm || !hasForegroundPerm || !hasOverlayPerm) {
                // Permissions are missing, launch permission check activity for result and return
                // false (missing permissions)
                Log.i(LOG_TAG, "Some permissions missing. Starting permission check activity")
                contract.launch(Intent(app, PermissionCheckActivity::class.java))
                return false
            }

            // All permissions granted
            Log.i(LOG_TAG, "All required permissions are granted. Continuing with execution")
            return true
        }

        /* ------ PERMISSION CHECK HELPER METHODS ----- */

        /**
         * Check if application has permission to show notifications for the overlay service. POST
         * notification permission was added in Android 33 and above. For all other versions, just
         * return true (permission will be implicitly granted).
         * @return Boolean: True if permission granted, false otherwise
         */
        private fun hasNotifyPerm(app: Activity): Boolean {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                return app.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            } else {
                return true;
            }
        }

        /**
         * Check if the application has permission to start a foreground service
         * @return Boolean: True if permission granted, false otherwise
         */
        private fun hasForegroundPerm(app: Activity): Boolean = (
                app.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE) ==
                        PackageManager.PERMISSION_GRANTED
                )

        /**
         * Check if the application has permission to draw over other apps
         * @return Boolean: True if permission granted, false otherwise
         */
        private fun hasDrawOverlayPerm(app: Activity): Boolean = Settings.canDrawOverlays(app)
    }
}