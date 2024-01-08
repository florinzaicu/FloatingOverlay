package nz.co.zsd.floatingoverlay

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.volley.BuildConfig
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONArray
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext
import nz.co.zsd.floatingoverlay.Data.RelaseInfo

class FragmentUpdate: Fragment() {
    /**
    * Release information view model that stores current release info retrieved from github API
    */
    private val releaseInfoVM: RelaseInfo by lazy {
        ViewModelProvider(this)[RelaseInfo::class.java]
    }

    override fun onResume() {
        super.onResume()
        if (releaseInfoVM.isValid())
            Log.e("QQQ", "valid ${releaseInfoVM.get().value?.name}")
    }

    companion object {
        private const val UPDATE_CHECK_URL = "https://api.github.com/repos/florinzaicu/FloatingOverlay/releases/latest"

        /**
         * Check if a newer version of the application exists. If a new version was found,
         * display a confirmation dialog to prompt the user to update. On confirmation, redirect
         * open the Update fragment to display the new release information and allow them to
         * download and install.
         */
        public fun checkForUpdates(context: Context): JsonObjectRequest {
            val updateReq = JsonObjectRequest(
                Request.Method.GET, UPDATE_CHECK_URL,null,
                { response ->
                    val releaseInfo = RelaseInfo()
                    releaseInfo.loadFromJSON(response)
                    if (releaseInfo.isValid()) {
                        Log.i("TTT", "valid")
                    } else {
                        Log.i("TTT", "invalid")
                    }

                    Log.i("TTT", "Release: ${releaseInfo.splitVersion()}")
                    releaseInfo.isNewerVersion(context)
                },
                { error ->
                    // TODO: Handle error
                    Log.e("TTT", "Error: ${error.message}")
                }
            )
            
            return updateReq
        }
    }
}