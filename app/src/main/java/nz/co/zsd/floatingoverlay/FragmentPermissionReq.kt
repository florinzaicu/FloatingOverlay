package nz.co.zsd.floatingoverlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment that prompts user to granted permissions required by the application. The UI contains
 * a list of all permissions needed by the app and a grant permission button. Once the user presses
 * the button, the fragment will request the missing permissions from the android system (user is
 * prompted). After a permission is granted, the UI is updated to indicate what permissions are
 * still required.
 *
 * NOTE: This fragment returns a status code result that tells the host activity if all permissions
 * have been granted (navigate away from this fragment), or if the user wishes to terminate the
 * application (does not want to grant the required permissions). The user can terminate the app
 * by pressing the back button.
 */
class FragmentPermissionReq: Fragment() {

    // ---------- Contracts and Callbacks of Fragment ----------

    /**
     * Create a contract to request and receive runtime (system) permissions. The callback will
     * update the UI depending to indicate what permissions are granted and which are still
     * required by the application.
     */
    private val reqPermContract = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.i(LOG_TAG, "Permission request result received")

        // Iterate through all permission results and check what was granted
        for (perm in permissions) {
            Log.d(LOG_TAG, "Permission ${perm.key} is granted: ${perm.value}")
            when (perm.key) {
                android.Manifest.permission.POST_NOTIFICATIONS -> {
                    if (perm.value) {
                        // Notification permission was granted, update UI
                        updateUIPermCheck()
                    } else {
                        // Permission denied, show a snack bar message
                        Log.d(LOG_TAG, "Notification permission was denied. Showing message")
                        Snackbar.make(
                            requireContext(),
                            requireView().findViewById(R.id.notify_perm_img),
                            getString(R.string.perm_notify_denied),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
                else -> {
                    Log.w(LOG_TAG, "Unknown permission result received: ${perm.key}")
                }
            }
        }
    }

    /**
     * Create a contract to open up the system settings to grant the draw-over-other apps system
     * permission. The callback does not perform any operations as onResume will update the UI
     * automatically (empty callback used).
     */
    private val reqDrawOverPermContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}


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
        val layout = layoutInflater.inflate(R.layout.fragment_permission_req, container, false)

        layout.findViewById<View>(R.id.permission_btn).setOnClickListener {
            requestMissingPermissions()
        }

        return layout
    }

    /**
     * On create of the fragment, instantiate a new on back press handler to return a KILL application
     * result to the host activity. The host activity should exit the app upon receiving the result.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // User pressed back button, terminate the application
            Log.i(LOG_TAG, "User pressed back button. Returning KILL result to host")
            requireActivity().supportFragmentManager.setFragmentResult(
                FRAGMENT_PERM_REQ_RES,
                Bundle().apply {
                    putInt(RES_ID_PERM_GRANTED, KILL_APPLICATION)
                })
        }
    }

    /**
     * When the fragment is attached to the container, hide the action bar
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = context as AppCompatActivity
        activity.supportActionBar?.hide()
    }

    /**
     * When the fragment is detached from the container, show the action bar (restore the original
     * view).
     */
    override fun onDetach() {
        super.onDetach()
        val activity = activity as AppCompatActivity
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity.supportActionBar?.setHomeButtonEnabled(true)
        activity.supportActionBar?.show()
    }

    /**
     * On resume of the fragment update the UI to show what permissions have been granted.
     */
    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG,"Resumed fragment, updating UI and checking if all perms granted")
        updateUIPermCheck()
    }


    // --------- Helper Methods ----------

    /**
     * Check what permissions have been granted and update the UI controls to show which ones are
     * granted and which are still required. If all permissions have been granted, method will return
     * a permissions granted result to the parent (fragment manager). The parent should navigate
     * away from this activity upon receiving this result.
     */
    private fun updateUIPermCheck() {
        val hasNotifyPerm: Boolean = hasNotifyPerm(requireActivity())
        val hasForegroundPerm: Boolean = hasForegroundPerm(requireActivity())
        val hasOverlayPerm: Boolean = hasDrawOverlayPerm(requireActivity())

        // If all permissions have been granted return a granted result to the host activity
        if (hasNotifyPerm && hasForegroundPerm && hasOverlayPerm) {
            Log.i(LOG_TAG, "All permissions granted, returning result")
            requireActivity().supportFragmentManager.setFragmentResult(
                FRAGMENT_PERM_REQ_RES,
                Bundle().apply {
                    putInt(RES_ID_PERM_GRANTED, PERMISSIONS_GRANTED)
                })
        }

        // Update UI if some permissions granted
        if (hasNotifyPerm) {
            Log.d(LOG_TAG, "Notification permission granted. Updating UI")
            requireView().findViewById<ImageView>(R.id.notify_perm_img)
                .setImageResource(R.drawable.ic_check_circle)
            requireView().findViewById<TextView>(R.id.notify_perm_txt)
                .setText(R.string.perm_granted_notify_msg)
        }

        if (hasForegroundPerm) {
            Log.d(LOG_TAG, "Foreground permission granted. Updating UI")
            requireView().findViewById<ImageView>(R.id.foreground_perm_img)
                .setImageResource(R.drawable.ic_check_circle)
            requireView().findViewById<TextView>(R.id.foreground_perm_txt)
                .setText(R.string.perm_granted_foreground_msg)
        }

        if (hasOverlayPerm) {
            Log.d(LOG_TAG, "Overlay permission granted. Updating UI")
            requireView().findViewById<ImageView>(R.id.draw_perm_img)
                .setImageResource(R.drawable.ic_check_circle)
            requireView().findViewById<TextView>(R.id.draw_perm_txt)
                .setText(R.string.perm_granted_overlay_msg)
        }
    }


    /**
     * Function that requests missing permissions from the android system. User will either be prompted
     * to grant the needed permission (via an overlay), or redirected to the appropriate settings
     * page to grant the needed permission (overlay permission).
     */
    private fun requestMissingPermissions() {
        if (!hasNotifyPerm(requireActivity())) {
            // Request notification permission
            Log.i(LOG_TAG, "Requesting notification permission")
            if (Build.VERSION.SDK_INT >= 33) {
                reqPermContract.launch(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
                )
            }
        }

        if (!hasDrawOverlayPerm(requireActivity())) {
            // Open system settings to allow user to grant draw over other apps permission
            Log.i(LOG_TAG, "Requesting draw over other apps permission")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireActivity().packageName}")
            )
            reqDrawOverPermContract.launch(intent)
        }
    }

    companion object {
        // Tag to use when logging information to logcat
        private val LOG_TAG: String = FragmentPermissionReq::class.simpleName ?: "FragmentPermissionReq"

        /**
         * Result ID returned by the fragment to the manager which indicates the status of the
         * permissions or operation to perform (i.e. user requested to exit the app)
         */
        const val FRAGMENT_PERM_REQ_RES = "FragmentPermissionReqResult"
        const val RES_ID_PERM_GRANTED = "PERMISSION_GRANTED"

        /**
         * Activity return code: all permissions granted successfully (continue execution)
         */
        const val PERMISSIONS_GRANTED = 1
        /**
         * Activity return code: user wishes to terminate application execution (stop app)
         */
        const val KILL_APPLICATION = 2

        /**
         * Check if the application has all the permissions it requires to operate. If any of the
         * required permissions are missing, the method returns false. The invoking activity should
         * navigate to the permission request fragment to allow the user to grant the needed
         * permissions.
         * @param app Activity to check permissions for
         * @return BooleanȘ True if all permissions granted, false otherwise
         */
        fun hasRequiredPermissions(app: Activity): Boolean {
            Log.i(LOG_TAG,"Checking if app has all required permissions")
            val hasNotifyPerm: Boolean = hasNotifyPerm(app)
            val hasForegroundPerm: Boolean = hasForegroundPerm(app)
            val hasOverlayPerm: Boolean = hasDrawOverlayPerm(app)

            Log.d(LOG_TAG, "Permissions $hasNotifyPerm $hasForegroundPerm $hasOverlayPerm")
            if (!hasNotifyPerm || !hasForegroundPerm || !hasOverlayPerm) {
                // Permissions are missing, launch permission check activity for result and return
                // false (missing permissions)
                Log.i(LOG_TAG, "Some permissions missing. Please navigate to permission fragment")
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
            return if (Build.VERSION.SDK_INT >= 33) {
                app.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            } else {
                true
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