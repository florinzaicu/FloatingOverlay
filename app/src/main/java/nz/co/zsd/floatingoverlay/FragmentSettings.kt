package nz.co.zsd.floatingoverlay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class FragmentSettings: Fragment() {

    // ---------- Fragment Lifecycle Methods ----------

    /**
     * On create of view, inflate the layout, bind listeners and return the view.
     * @param inflater Layout inflater to use for the view
     * @param container Container to inflate the view into
     * @param savedInstanceState UI element values previously saved by the fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.d(FragmentSettings.LOG_TAG, "Creating home fragment view")
        val layout = layoutInflater.inflate(R.layout.fragment_settings, container, false)

        // Bind on change listeners to seek bars to update value label text fields
        layout.findViewById<SeekBar>(R.id.overlayUIScale).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val valueText = formatFloat(OVERLAY_SCALE_FACTORS[progress])
                layout.findViewById<TextView>(R.id.overlayUIScaleValue).text = valueText
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        layout.findViewById<SeekBar>(R.id.overlayCollapseTimer).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                layout.findViewById<TextView>(R.id.overlayCollapseTimerValue).text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        layout.findViewById<SeekBar>(R.id.overlayTransparency).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val valueText = formatFloat(OVERLAY_TRANSPARENCY[progress])
                layout.findViewById<TextView>(R.id.overlayTransparencyValue).text = valueText
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        layout.findViewById<View>(R.id.update_settings_btn).setOnClickListener(View.OnClickListener {
            saveUISettings()
        })

        return layout
    }

    /**
     * On resume of the fragment update the UI to show the overlay setting values
     */
    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG,"Resumed fragment, updating UI and checking if all perms granted")
        updateUISettingValues()
    }


    // ---------- Helper Methods ----------

    /**
     * Format a float value to a string with one decimal place, respecting the default local
     * @args value: Float value to format
     * @return Formatted string with one decimal place
     */
    private fun formatFloat(value: Float): String {
        return String.format(resources.configuration.locales.get(0), "%.1f", value)
    }

    /**
     * Update the UI to show the current overlay setting values on the seek bars and text views.
     * This method will also send a broadcast message to the overlay service to refresh its UI (in
     * case there are changes that the UI was not aware of).
     */
    private fun updateUISettingValues() {
        // Update the UI to reflect the current settings
        requireView().findViewById<SeekBar>(R.id.overlayUIScale).progress =
            OVERLAY_SCALE_FACTORS.indexOf(PreferenceStorage.getUIScale(requireContext()))
        requireView().findViewById<SeekBar>(R.id.overlayCollapseTimer).progress =
            PreferenceStorage.getUICollapseTimer(requireContext())
        requireView().findViewById<SeekBar>(R.id.overlayTransparency).progress =
            OVERLAY_TRANSPARENCY.indexOf(PreferenceStorage.getUITransparency(requireContext()))

        requireView().findViewById<TextView>(R.id.overlayUIScaleValue).text =
            formatFloat(PreferenceStorage.getUIScale(requireContext()))
        requireView().findViewById<TextView>(R.id.overlayCollapseTimerValue).text =
            "${PreferenceStorage.getUICollapseTimer(requireContext())}"
        requireView().findViewById<TextView>(R.id.overlayTransparencyValue).text =
            formatFloat(PreferenceStorage.getUITransparency(requireContext()))

        // Send a broadcast intent to the overlay service to refresh the UI
        OverlayService.broadcastRefreshUI(requireContext())
    }

    /**
     * On press of save settings save the overlay UI settings and send a broadcast intent to the overlay
     * to refresh the UI (apply settings)
     */
    fun saveUISettings () {
        val scaleProgress = requireView().findViewById<SeekBar>(R.id.overlayUIScale).progress
        val collapseTimerProgress = requireView().findViewById<SeekBar>(R.id.overlayCollapseTimer).progress
        val transparencyProgress = requireView().findViewById<SeekBar>(R.id.overlayTransparency).progress

        PreferenceStorage.saveUIScale(OVERLAY_SCALE_FACTORS[scaleProgress], requireContext())
        PreferenceStorage.saveCollapseTimer(collapseTimerProgress, requireContext())
        PreferenceStorage.saveUITransparency(OVERLAY_TRANSPARENCY[transparencyProgress], requireContext())

        // Send a broadcast intent to the overlay service to refresh the UI
        OverlayService.broadcastRefreshUI(requireContext())
    }

    companion object {
        // Tag to use when logging information to logcat
        private val LOG_TAG: String = FragmentSettings::class.simpleName ?: "FragmentSettings"

        /**
         * Map of overlay scale factor float values to seek bar positions
         */
        private val OVERLAY_SCALE_FACTORS: List<Float> = listOf(
            0.6f, 0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f
        )

        /**
         * Map of overlay transparency factor float values to seek bar positions
         */
        private val OVERLAY_TRANSPARENCY: List<Float> = listOf(
            0.1f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f
        )
    }
}