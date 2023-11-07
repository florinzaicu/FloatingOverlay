package nz.co.zsd.floatingvolume

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log

class PermissionCheckActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.permission_check_activity)
    }

    override fun onResume() {
        super.onResume()

        // If all permissions granted, stop the activity
        if (hasPermission(this)) {
            Log.i(javaClass.simpleName, "All permissions granted, closing permission check activity")
            finish()
        }

        Log.i(javaClass.simpleName, "Some permissions missing, showing info and prompt")
    }

    companion object {
        /**
         * Check if the application has all the required permissions granted. If any permissions
         * are missing, start the permission check activity to ask the user to grant them.
         */
        public fun checkPermissions(activity: Activity) {
            // If any of the permissions are missing, show the permission check activity to ask the
            // user to grant them
            Log.i(javaClass.simpleName, "Checking if app has all required permissions")
            if (hasPermission(activity) == false) {
                Log.i(javaClass.simpleName, "Some permissions missing. Spawning permission check activity")
                activity.startActivity(Intent(activity, PermissionCheckActivity::class.java))
            }
        }

        /**
         * Check if the application has all the required permissions granted.
         * @return true if all permissions are granted, false otherwise
         */
        public fun hasPermission(activity: Activity): Boolean {
            var hasNotifyPerm: Boolean = (
                    activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    )

            // Check if the user has granted the foreground service permission
            var hasForegroundPerm: Boolean = false;
            if (Build.VERSION.SDK_INT > 34) {
                // If API greater than 34 check if user granted the special use permission
                hasForegroundPerm = (
                        activity.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                        )
            } else {
                hasForegroundPerm = (
                        activity.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                        )
            }

            // Check if the application has permission to draw over other apps
            var hasOverlayPerm: Boolean = Settings.canDrawOverlays(activity)

            // If any permissions are missing return false
            Log.d(javaClass.simpleName, "Permissions ${hasNotifyPerm} ${hasForegroundPerm} ${hasOverlayPerm}")
            if (hasNotifyPerm == false || hasForegroundPerm == false || hasOverlayPerm == false) {
                return false
            }

            // All permissions have been granted, return true
            return true
        }
    }
}