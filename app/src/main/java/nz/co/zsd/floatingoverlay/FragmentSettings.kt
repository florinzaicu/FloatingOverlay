package nz.co.zsd.floatingoverlay

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
     * On create of view, inflate the layout, bind listeners and return the view to show in the
     * fragment manager.
     * @param inflater Layout inflater to use for the view
     * @param container Container to inflate the view into
     * @param savedInstanceState UI element values previously saved by the fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.d(LOG_TAG, "Creating fragment view")
        val layout = layoutInflater.inflate(R.layout.fragment_settings, container, false)

        // Bind on change listeners to seek bars to update value label text fields
        layout.findViewById<SeekBar>(R.id.overlayUIScale).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val valueText = formatFloat(OVERLAY_SCALE_FACTORS[progress])
                layout.findViewById<TextView>(R.id.overlayUIScaleValue).text = valueText

                updateOverlayPreview()
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

                updateOverlayPreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Save and reset setting click listeners
        layout.findViewById<View>(R.id.save_settings_btn).setOnClickListener(View.OnClickListener {
            saveSettings()
        })

        layout.findViewById<View>(R.id.reset_settings_btn).setOnClickListener(View.OnClickListener {
            resetSettings()
        })

        return layout
    }

    /**
     * Once the fragment has been instantiated (layout inflated), restore the form state from the
     * previous saved bundled state or from the shared preference values
     * @param view Inflated fragment
     * @param savedInstanceState Previous saved state value of form
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "Fragment view created, restoring form state")

        // Retrieve the default overlay settings from the shared preferences
        val defaultOverlayScale = PreferenceStorage.getUIScale(requireContext())
        val defaultOverlayCollapseTimer = PreferenceStorage.getUICollapseTimer(requireContext())
        val defaultOverlayTransparency = PreferenceStorage.getUITransparency(requireContext())

        val scale = savedInstanceState?.getInt(
            BUNDLE_OVERLAY_SCALE, OVERLAY_SCALE_FACTORS.indexOf(defaultOverlayScale)
        ) ?: OVERLAY_SCALE_FACTORS.indexOf(defaultOverlayScale)

        val collapseTimer = savedInstanceState?.getInt(
            BUNDLE_OVERLAY_COLLAPSE_TIMER, defaultOverlayCollapseTimer
        ) ?: defaultOverlayCollapseTimer

        val transparency = savedInstanceState?.getInt(
                BUNDLE_OVERLAY_TRANSPARENCY, OVERLAY_TRANSPARENCY.indexOf(defaultOverlayTransparency)
        ) ?: OVERLAY_TRANSPARENCY.indexOf(defaultOverlayTransparency)


        // Update the UI to reflect the current settings
        view.findViewById<SeekBar>(R.id.overlayUIScale).progress = scale
        view.findViewById<SeekBar>(R.id.overlayCollapseTimer).progress = collapseTimer
        view.findViewById<SeekBar>(R.id.overlayTransparency).progress = transparency

        view.findViewById<TextView>(R.id.overlayUIScaleValue).text =
            formatFloat(OVERLAY_SCALE_FACTORS[scale])
        view.findViewById<TextView>(R.id.overlayCollapseTimerValue).text =
            "$collapseTimer"
        view.findViewById<TextView>(R.id.overlayTransparencyValue).text =
            formatFloat(OVERLAY_TRANSPARENCY[transparency])
    }

    /**
     * On resume of the fragment update the preview and broadcast a refresh message to update the
     * overlay UI (if currently running).
     */
    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG,"Resumed fragment, updating preview and broadcasting overlay refresh")

        OverlayService.broadcastRefreshUI(requireContext())
        updateOverlayPreview()
    }

    /**
     * Save the form state to preserve configuration attributes between fragment lifecycle events
     */
    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(LOG_TAG, "Saving form state")
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_OVERLAY_SCALE, requireView().findViewById<SeekBar>(R.id.overlayUIScale).progress)
        outState.putInt(BUNDLE_OVERLAY_TRANSPARENCY, requireView().findViewById<SeekBar>(R.id.overlayTransparency).progress)
        outState.putInt(BUNDLE_OVERLAY_COLLAPSE_TIMER, requireView().findViewById<SeekBar>(R.id.overlayTransparency).progress)
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
     * Update the overlay preview to reflect the current UI setting values defined by the user.
     */
    private fun updateOverlayPreview() {
        // Get the current preview overlay view and retrieve the UI settings from the input fields
        val overlay = requireView().findViewById<View>(R.id.floating_overlay_container)

        val scale = OVERLAY_SCALE_FACTORS[requireView().findViewById<SeekBar>(R.id.overlayUIScale).progress]
        val transparency = OVERLAY_TRANSPARENCY[requireView().findViewById<SeekBar>(R.id.overlayTransparency).progress]

        // Update the style of the preview overlay by applying the input fields UI setting values
        OverlayView.updateStyle(overlay, requireContext(), OverlayView.OverlayUIAttributes(scale, transparency))
    }

    /**
     * On press of save settings save the overlay UI settings and send a broadcast intent to the overlay
     * to refresh the UI (apply settings)
     */
    private fun saveSettings () {
        val scaleProgress = requireView().findViewById<SeekBar>(R.id.overlayUIScale).progress
        val collapseTimerProgress = requireView().findViewById<SeekBar>(R.id.overlayCollapseTimer).progress
        val transparencyProgress = requireView().findViewById<SeekBar>(R.id.overlayTransparency).progress

        PreferenceStorage.saveUIScale(OVERLAY_SCALE_FACTORS[scaleProgress], requireContext())
        PreferenceStorage.saveCollapseTimer(collapseTimerProgress, requireContext())
        PreferenceStorage.saveUITransparency(OVERLAY_TRANSPARENCY[transparencyProgress], requireContext())

        // Send a broadcast intent to the overlay service to refresh the UI
        OverlayService.broadcastRefreshUI(requireContext())
    }

    /**
     * Clear the form settings by restoring shared preference values
     */
    private fun resetSettings() {
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

        // Update the floating overlay preview
        updateOverlayPreview()
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

        // Constant IDs of input field values to save in bundled state (restore form state on resume)
        private const val BUNDLE_OVERLAY_SCALE = "overlay_scale"
        private const val BUNDLE_OVERLAY_COLLAPSE_TIMER = "overlay_collapse_timer"
        private const val BUNDLE_OVERLAY_TRANSPARENCY = "overlay_transparency"
    }
}