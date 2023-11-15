package nz.co.zsd.floatingvolume

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    /**
     * On create inflate the layout of the activity
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    /**
     * Create am activity result handler for the permission activity to check if we should terminate
     * the app.
     */
    private val permCheckActivityRes = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res: ActivityResult ->
        if (res.resultCode == PermissionCheckActivity.KILL_APPLICATION) {
            Log.i(LOG_TAG,"Permission check activity returned terminate res, stopping app.")
            finish()
        }
    }

    /**
     * On activity resume check if the app has all required permissions
     */
    override fun onResume() {
        super.onResume()

        // Check if application has all required permissions, if not show permission activity
        Log.d(LOG_TAG, "Resumed main activity. Checking permissions")
        PermissionCheckActivity.checkPermissions(this, permCheckActivityRes)
    }

    /**
     * On press of the show overlay button start the foreground service to display the floating
     * overlay controls
     */
    fun startOverlayService (@Suppress("UNUSED_PARAMETER") v: View) {
        startForegroundService(Intent(this, OverlayService::class.java))
    }

    companion object {
        /**
         * Tag to use when logging information to logcat
         */
        private val LOG_TAG: String = MainActivity::class.simpleName ?: "MainActivity"
    }
}