package nz.co.zsd.floatingoverlay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
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

        // Bind on change listeners to seek bars to update value label text fields and save value
        findViewById<SeekBar>(R.id.overlayUIScale).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                findViewById<TextView>(R.id.overlayUIScaleValue).text = "${OVERLAY_SCALE_FACTORS[progress]}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<SeekBar>(R.id.overlayCollapseTimer).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                findViewById<TextView>(R.id.overlayCollapseTimerValue).text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<SeekBar>(R.id.overlayTransparency).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                findViewById<TextView>(R.id.overlayTransparencyValue).text = "${OVERLAY_TRANSPARENCY[progress]}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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

        // Update the UI to reflect the current settings
        findViewById<SeekBar>(R.id.overlayUIScale).progress = OVERLAY_SCALE_FACTORS.indexOf(PreferenceStorage.getUIScale(this))
        findViewById<SeekBar>(R.id.overlayCollapseTimer).progress = PreferenceStorage.getUICollapseTimer(this)
        findViewById<SeekBar>(R.id.overlayTransparency).progress = OVERLAY_TRANSPARENCY.indexOf(PreferenceStorage.getUITransparency(this))

        findViewById<TextView>(R.id.overlayUIScaleValue).text = "${PreferenceStorage.getUIScale(this)}"
        findViewById<TextView>(R.id.overlayCollapseTimerValue).text = "${PreferenceStorage.getUICollapseTimer(this)}"
        findViewById<TextView>(R.id.overlayTransparencyValue).text = "${PreferenceStorage.getUITransparency(this)}"
    }

    /**
     * On press of the show overlay button start the foreground service to display the floating
     * overlay controls
     */
    fun startOverlayService (@Suppress("UNUSED_PARAMETER") v: View) {
        startForegroundService(Intent(this, OverlayService::class.java))
    }

    fun saveUISettings (@Suppress("UNUSED_PARAMETER") v: View) {
        val overlayUIScale = OVERLAY_SCALE_FACTORS[findViewById<SeekBar>(R.id.overlayUIScale).progress]
        val overlayCollapseTimer = findViewById<SeekBar>(R.id.overlayCollapseTimer).progress
        val overlayTransparency = OVERLAY_TRANSPARENCY[findViewById<SeekBar>(R.id.overlayTransparency).progress]

        PreferenceStorage.saveUIScale(overlayUIScale, this)
        PreferenceStorage.saveCollapseTimer(overlayCollapseTimer, this)
        PreferenceStorage.saveUITransparency(overlayTransparency, this)

        // Send a broadcast intent to the overlay service to refresh the UI
        sendBroadcast(Intent(OverlayService.OVERLAY_SERVICE_ACTION_REFRESH))
    }

    companion object {
        /**
         * Tag to use when logging information to logcat
         */
        private val LOG_TAG: String = MainActivity::class.simpleName ?: "MainActivity"

        // Map of overlay scale factor float values to seek bar positions
        private val OVERLAY_SCALE_FACTORS: List<Float> = listOf(
            0.6f, 0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f
        )

        // Map of overlay transparency factor float values to seek bar positions
        private val OVERLAY_TRANSPARENCY: List<Float> = listOf(
            0.1f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f
        )
    }
}