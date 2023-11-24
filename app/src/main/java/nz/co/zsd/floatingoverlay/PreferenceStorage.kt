package nz.co.zsd.floatingoverlay

import android.R
import android.content.Context
import android.content.SharedPreferences


/**
 * Sealed static class that provides controller to save and retrieve application configuration
 * attributes from the shared preferences.
 */
sealed class PreferenceStorage {
    companion object {
        /**
         * Shared preference key used by the application to store data
         */
        private const val PREF_NAME: String = "zsd.co.nz.floatingoverlay.PREFS"

        /**
         * Keys of shared preference values
         */
        private const val UI_SCALE: String = "Overlay.Scale"
        private const val UI_COLLAPSE_TIMER: String = "Overlay.CollapseTimer"
        private const val UI_TRANSPARENCY: String = "Overlay.Transparency"

        /**
         * Get an instance of the current application shared preferences storage container. Mode
         * used by app is private (not sharable with other apps).
         * @return Instance of the application shared preference data store
         */
        private fun getSharedPref(context: Context): SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        /**
         * Get the current preference scale factor to apply to the overlay UI components
         * @param context Current context of the application to retrieve settings
         * @return Scale factor to apply to overlay UI size (defaults to 1.0f)
         */
        fun getUIScale(context: Context) = getSharedPref(context).getFloat(UI_SCALE, 1.0f)

        /**
         * Get the current timeout to apply to the overlay collapse timer (after expanding overlay
         * collapse after n-seconds).
         * @param context Current context of the application
         * @return Overlay collapse timeout value in seconds (defaults to 5 seconds)
         */
        fun getUICollapseTimer(context: Context) = getSharedPref(context).getInt(UI_COLLAPSE_TIMER, 5)

        /**
         * Get the current transparency to apply to the overlay (alpha 0 - 1.0)
         * @param context Current context of the application
         * @return Overlay transparency (defaults to 1.0 or no transparency)
         */
        fun getUITransparency(context: Context) = getSharedPref(context).getFloat(UI_TRANSPARENCY, 1.0f)

        /**
         * Update the scale factor to apply to the UI overlay
         * @param scale New scale factor to apply to the floating overlay
         * @param context Current context of the application to retrieve settings
         * @throws IllegalArgumentException Scale factor value is invalid (cannot be 0.0)
         */
        fun saveUIScale (scale: Float, context: Context) {
            if (scale == 0.0f)
                throw IllegalArgumentException("Cannot scale UI to size 0")

            val editor = getSharedPref(context).edit()
            editor.putFloat(UI_SCALE, scale)
            editor.commit()
        }

        /**
         * Update the collapse timeout value of the floating overlay. Value controls the time after
         * which the floating overlay collapses after expansion.
         * @param time Overlay collapse timeout time
         * @param context Current context of the application
         * @throws IllegalArgumentException Time is invalid (cannot be less than 1 seconds)
         */
        fun saveCollapseTimer (time: Int, context: Context) {
            if (time < 1)
                throw IllegalArgumentException("Timer value cannot be less than 1 second!")

            val editor = getSharedPref(context).edit()
            editor.putInt(UI_COLLAPSE_TIMER, time)
            editor.commit()
        }

        /**
         * Update the transparency to apply to the UI overlay
         * @param scale New transparency to apply to the floating overlay (alpha 0 - 1.0)
         * @param context Current context of the application to retrieve settings
         * @throws IllegalArgumentException transparency is invalid
         */
        fun saveUITransparency (alpha: Float, context: Context) {
            if (alpha < 0.0f || alpha > 1.0f)
                throw IllegalArgumentException("Transparency needs to be between 0 and 1")

            val editor = getSharedPref(context).edit()
            editor.putFloat(UI_TRANSPARENCY, alpha)
            editor.commit()
        }
    }
}