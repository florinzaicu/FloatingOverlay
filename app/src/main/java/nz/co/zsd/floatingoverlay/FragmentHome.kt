package nz.co.zsd.floatingoverlay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class FragmentHome: Fragment() {

    /**
     * On create of view, inflate the layout, bind listeners and return the view.
     * @param inflater Layout inflater to use for the view
     * @param container Container to inflate the view into
     * @param savedInstanceState UI element values previously saved by the fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(LOG_TAG, "Creating home fragment view")

        // Do not attach the fragment to the root container (force listeners of calls into this
        // class - prevent parent from directly registering callbacks)
        val layout = layoutInflater.inflate(R.layout.fragment_home, container, false);

        // Bind listener to start service button
        layout.findViewById<View>(R.id.startOverlayServiceBtn).setOnClickListener(View.OnClickListener {
            requireContext().startForegroundService(Intent(requireContext(), OverlayService::class.java))
        })
        return layout
    }

    companion object {
        // Tag to use when logging information to logcat
        private val LOG_TAG: String = FragmentHome::class.simpleName ?: "FragmentHome"
    }
}