package nz.co.zsd.floatingoverlay


import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import nz.co.zsd.floatingoverlay.Data.ReleaseInfo

/**
 * Home activity of the application that hosts the fragments and application toolbar. The app uses
 * a fragment navigation controller to switch between fragments (show different screens of the app).
 *
 * NOTE: On resume of the home activity, the applications checks if the user has granted all required
 * permissions. If any permissions are missing, the user is redirected to the request permissions
 * fragment!
 */
class ActivityHome : AppCompatActivity() {
    /**
     * Volley request queue used to make HTTP requests
     */
    private var netQueue: RequestQueue? = null

    /**
     * Release information view model that stores current release info retrieved from github API
     */
    private val releaseInfoVM: ReleaseInfo by viewModels()

    /**
     * On create of the main activity, inflate the layout and configure the app toolbar
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        supportFragmentManager.setFragmentResultListener(
            FragmentPermissionReq.FRAGMENT_PERM_REQ_RES,
            this
        ) { _, bundle ->
            val perm_grant_status = bundle.getInt(FragmentPermissionReq.RES_ID_PERM_GRANTED)
            Log.d(LOG_TAG, "Permission granted result received $perm_grant_status");

            when (perm_grant_status) {
                // User wants to kill the application, terminate
                FragmentPermissionReq.KILL_APPLICATION -> {
                    Log.i(LOG_TAG,"Permission check returned kill result, stopping app")
                    finish()
                }

                // All permissions granted, remove the check permission activity from the stack
                FragmentPermissionReq.PERMISSIONS_GRANTED -> {
                    Log.i(LOG_TAG,"Permission check returned granted result")
                    findNavController(R.id.main_fragment_container).popBackStack()
                }

                // Unknown result returned, log warning and cary on
                else -> {
                    Log.w(LOG_TAG,"Permission check returned unknown result, ignoring")
                }
            }
        }

        // Configure the action bar for the main view and bind to fragment nav controller
        Log.d(LOG_TAG, "Configuring action bar");
        setSupportActionBar(findViewById(R.id.main_action_bar))
        val fragContainer = supportFragmentManager.findFragmentById(R.id.main_fragment_container) as NavHostFragment
        val navController = fragContainer.navController
        setupActionBarWithNavController(navController)

        // Configure network queue
        netQueue = Volley.newRequestQueue(this)
    }

    override fun onResume() {
        super.onResume()

        // Check if application has all required permissions, if not show permission activity
        Log.d(LOG_TAG, "Resumed main activity. Checking permissions")
        if (FragmentPermissionReq.hasRequiredPermissions(this) == false) {
            findNavController(R.id.main_fragment_container).navigate(R.id.fragmentPermissionReq);
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(LOG_TAG, "Paused main activity. Cancelling network requests")

        // Cancel any pending network requests
        netQueue?.cancelAll(LOG_TAG)
    }

    /**
     * Inflate the application toolbar action menu
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * Callback triggered on a successful response from an update check API call. The handler will
     * check if the latest version is newer than the current installed version (update available).
     * If an update exists, the user is asked if they wish to view more details. On an affirmative
     * response, the user is redirected to the update fragment.
     */
    private val checkUpdateSuccess: (obj: ReleaseInfo) -> Unit = {
        obj ->
            Log.i(LOG_TAG, "${obj.data.value?.name}")
            if (obj.isValid() && obj.isNewerVersion(this)) {
                Log.i(LOG_TAG, "Update found, prompting user if they wish to install")

                // Build and display an alert dialog to prompt the user if they wish to view
                // more details about the available update
                AlertDialog.Builder(this)
                .setMessage(
                    String.format(getString(R.string.frag_update_promt_msg),
                        packageManager.getPackageInfo(packageName, 0).versionName,
                        obj.getVersion()
                    )
                )
                .setTitle(R.string.frag_update_promt_title)
                .setPositiveButton(R.string.frag_update_promt_btn_confirm) { _, _ ->
                    Log.i(LOG_TAG, "User wants to view update details")
                    val fragContainer =
                        supportFragmentManager.findFragmentById(R.id.main_fragment_container) as NavHostFragment
                    val navController = fragContainer.navController
                    if (navController.currentBackStackEntry?.destination?.id != R.id.fragmentUpdateInfo)
                        navController.navigate(R.id.fragmentUpdateInfo)

                    // Update the release info view model with the new release info
                    releaseInfoVM.data.value = obj.data.value
                }
                .setNegativeButton(R.string.frag_update_promt_btn_cancel) { _, _ ->
                    Log.i(LOG_TAG, "User cancelled update")
                }
                .create().show()
            } else {
                Log.i(LOG_TAG, "No update available")
            }
    }

    /**
     * Callback triggered on an error response from the check for update API call. The handler will
     * display an error message to the user to notify them of the failure. No further actions taken.
     */
    private val checkUpdateError: (msg: String) -> Unit = {
        msg ->
            Log.e(LOG_TAG, "Error: $msg")
            Snackbar.make(
                findViewById(R.id.main_fragment_container),
                getString(R.string.frag_update_check_error),
                Snackbar.LENGTH_LONG
            ).show()
    }

    /**
     * Listener triggered on toolbar action press. Navigate to the pressed option menu item
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(LOG_TAG, "Menu item selected: ${item.itemId}");

        return when (item.itemId ) {
            // Home menu item was pressed, pop the back stack of the application
            android.R.id.home -> {
                val fragContainer = supportFragmentManager.findFragmentById(R.id.main_fragment_container) as NavHostFragment
                val navController = fragContainer.navController
                navController.popBackStack()
                true;
            }

            // Check for update menu item was selected
            R.id.menu_check_update -> {
                Log.e(LOG_TAG, "Checking for updates")
                netQueue?.add(FragmentUpdate.genLatestReleaseReq(checkUpdateSuccess, checkUpdateError))
                true;
            }

            /*
            // Info menu item was selected
            R.id.menu_info -> {
                Snackbar.make(this,
                    findViewById(R.id.main_fragment_container),
                    getString(R.string.feature_not_implemented),
                    Snackbar.LENGTH_LONG
                ).show()
                true;
            }*/

            // Settings menu item was selected
            R.id.menu_settings -> {
                val fragContainer = supportFragmentManager.findFragmentById(R.id.main_fragment_container) as NavHostFragment
                val navController = fragContainer.navController

                if (navController.currentBackStackEntry?.destination?.id != R.id.fragmentSettings)
                    navController.navigate(R.id.fragmentSettings)
                true;
            }

            // Default call parent
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        // Tag to use when logging information to logcat
        private val LOG_TAG: String = ActivityHome::class.simpleName ?: "ActivityHome"
    }
}