package nz.co.zsd.floatingoverlay


import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar

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
    }

    override fun onResume() {
        super.onResume()

        // Check if application has all required permissions, if not show permission activity
        Log.d(LOG_TAG, "Resumed main activity. Checking permissions")
        if (FragmentPermissionReq.hasRequiredPermissions(this) == false) {
            findNavController(R.id.main_fragment_container).navigate(R.id.fragmentPermissionReq);
        }
    }

    /**
     * Inflate the application toolbar action menu
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
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
            /*R.id.menu_check_update -> {
                Snackbar.make(this,
                    findViewById(R.id.main_fragment_container),
                    getString(R.string.feature_not_implemented),
                    Snackbar.LENGTH_LONG
                ).show()
                true;
            }

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