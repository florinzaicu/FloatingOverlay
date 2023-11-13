package nz.co.zsd.floatingvolume

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
     * On activity resume check if the app has all required permissions
     */
    override fun onResume() {
        super.onResume()

        // Check if application has all required permissions, if not show permission activity
        Log.d(javaClass.simpleName, "Resumed main activity. Checking permissions")
        PermissionCheckActivity.checkPermissions(this)
    }

    /**
     * On activity result check if the user did not grant the required permissions or if they simply
     * want to close the app
     * @param requestCode Request code of the activity spawned to retrieve the result
     * @param resultCode Result returned by the activity
     * @param data Intent data returned from the activity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // If explain permission result returned check if we need to terminate app
        Log.i(javaClass.simpleName, "Activity ($requestCode) returned result $resultCode")
        if (requestCode == PermissionCheckActivity.EXPLAIN_REQUIRED_PERMISSIONS_ACTIVITY) {
            if (resultCode == PermissionCheckActivity.KILL_APPLICATION) {
                Log.i(javaClass.simpleName,"Result was terminate application. Stopping app")
                finish()
            }
        }
    }

    /**
     * On press of the show overlay button start the foreground service to display the floating
     * overlay controls
     */
    fun startOverlayService (@Suppress("UNUSED_PARAMETER") v: View) {
        startForegroundService(Intent(this, OverlayService::class.java))
    }
}