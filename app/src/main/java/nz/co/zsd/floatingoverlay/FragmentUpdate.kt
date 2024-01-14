package nz.co.zsd.floatingoverlay

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.PackageManagerCompat.LOG_TAG
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.snackbar.Snackbar
import io.noties.markwon.Markwon
import nz.co.zsd.floatingoverlay.Data.ReleaseInfo
import nz.co.zsd.floatingoverlay.Data.ReleaseInfoData

class FragmentUpdate: Fragment() {

    /**
    * Release information view model that stores current release info retrieved from github API
    */
    private val releaseInfoVM: ReleaseInfo by activityViewModels()

    /**
     * Callback invoked when the release info in the view model changes. This callback is used to
     * update the UI with the latest release information.
     */
    private val releaseInfoChangeObserver: (ReleaseInfoData) -> Unit = { data : ReleaseInfoData ->
        updateReleaseInfoUI(data)
    }

    // ---------- Fragment Lifecycle Methods ----------

    /**
     * On create of view, inflate the layout, bind listeners and return the view to show on screen.
     * @param inflater Layout inflater to use to inflate view
     * @param container Container that will hold our view (parent container)
     * @param savedInstanceState Bundle containing saved state of the fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = layoutInflater.inflate(R.layout.fragment_update_info, container, false);
        return layout
    }

    /**
     * Once the fragment has been instantiated (layout inflated), display the update information
     * held in the release information view model. If the view model does not contain a valid
     * release, display an error message and tell the user ty try to check for updates again.
     * @param view Inflated fragment layout
     * @param savedInstanceState Bundle containing saved state of the fragment
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateReleaseInfoUI(releaseInfoVM.data.value)
        view.findViewById<Button>(R.id.downloadUpdateBtn).setOnClickListener { _ ->
            Log.d(LOG_TAG, "Download update button clicked")
        }
    }

    /**
     * On resume of the fragment, bind a listener to the view model to update release info on change
     */
    override fun onResume() {
        super.onResume()
        releaseInfoVM.data.observe(viewLifecycleOwner, releaseInfoChangeObserver)
    }

    /**
     * On pause of the fragment, remove the listener from the view model to stop receiving updates
     */
    override fun onPause() {
        super.onPause()
        releaseInfoVM.data.removeObserver(releaseInfoChangeObserver)
    }

    // ---------- Helper Methods ----------

    /**
     * Update the UI to show the details of the latest release. If the release info object is invalid,
     * an error message is shown, placeholders are shown on the UI and the download button is disabled.
     * @param data Release info data object to display on the UI
     */
    private fun updateReleaseInfoUI(data: ReleaseInfoData?) {
        if (ReleaseInfo.isDataValid(data)) {
            Log.d(LOG_TAG, "Release info is valid, displaying update info")

            // Parse the release notes and display in text view on UI
            val markdownParsers: Markwon = Markwon.create(requireContext())
            val changeLog = markdownParsers.toMarkdown(data?.note ?: "No Release Note")
            requireView().findViewById<TextView>(R.id.updateChangeLog).text = changeLog

            // Update info fields on the UI with release information
            requireView().findViewById<TextView>(R.id.newAppVer).text =
                String.format(getString(R.string.frag_update_info_update_ver_lbl),
                    ReleaseInfo.getVersion(data)
                )
            requireView().findViewById<TextView>(R.id.currentAppVer).text =
                String.format(getString(R.string.frag_update_info_current_ver_lbl),
                    requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName,
                )
            requireView().findViewById<TextView>(R.id.downloadSize).text =
                String.format(getString(R.string.frag_update_info_download_size_lbl),
                    ReleaseInfo.getUpdateSize(data, requireContext())
                )

            // Enable the download button
            requireView().findViewById<Button>(R.id.downloadUpdateBtn).isEnabled = true
        } else {
            Log.d(LOG_TAG, "Release info is invalid, displaying error message")

            // Replace changelog text with error message and clear version text
            requireView().findViewById<TextView>(R.id.updateChangeLog).text = getString(R.string.frag_update_info_changelog_error)

            requireView().findViewById<TextView>(R.id.newAppVer).text =
                String.format(getString(R.string.frag_update_info_update_ver_lbl), "")
            requireView().findViewById<TextView>(R.id.currentAppVer).text =
                String.format(getString(R.string.frag_update_info_current_ver_lbl), "")
            requireView().findViewById<TextView>(R.id.downloadSize).text =
                String.format(getString(R.string.frag_update_info_download_size_lbl), "")

            // Disable the download button
            requireView().findViewById<Button>(R.id.downloadUpdateBtn).isEnabled = false

            // Show an error message on screen
            Snackbar.make(requireView(),
                R.string.frag_update_info_error_msg,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        // Tag to use when logging information to logcat
        private val LOG_TAG: String = FragmentUpdate::class.simpleName ?: "FragmentUpdate"

        // API URL used to get the info of latest release of the application (check for updates)
        private const val UPDATE_CHECK_URL = "https://api.github.com/repos/florinzaicu/FloatingOverlay/releases/latest"

        /**
         * Create Volley request to get the information of the latest release of the application.
         * This method should be called when checking for updates. On a successful response from
         * the API (release object valid), invokes the successful callback. If an error occurred,
         * or the release info response from the API is invalid (cannot deserialize JSON), the
         * error callback is invoked. The returned volley request should be added to a request
         * queue to execute it.
         * @param successCallback Callback invoked on successful response from the API
         * @param errorCallback Callback invoked on an API error or invalid response from API
         * @returns Volley request to retrieve the latest release info from the API endpoint.
         */
        public fun genLatestReleaseReq(successCallback: (ReleaseInfo) -> Unit,
                                       errorCallback: (String) -> Unit): JsonObjectRequest {
            val updateReq = JsonObjectRequest(
                Request.Method.GET, UPDATE_CHECK_URL,null,
                { response ->
                    val releaseInfo = ReleaseInfo()
                    releaseInfo.loadFromJSON(response)
                    if (releaseInfo.isValid()) {
                        Log.d(LOG_TAG, "CheckUpdate response is valid")
                        successCallback(releaseInfo)
                    } else {
                        Log.d(LOG_TAG, "CheckUpdate response is invalid")
                        errorCallback("API response returned Invalid release info obj")
                    }
                },
                { error ->
                    Log.e(LOG_TAG, "CheckUpdate error: ${error.message}")
                    errorCallback("API error: ${error.message}")
                }
            )
            
            return updateReq
        }
    }
}