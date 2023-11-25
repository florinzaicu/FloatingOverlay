package nz.co.zsd.floatingoverlay

import android.app.Application
import com.google.android.material.color.DynamicColors

class FloatingOverlayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}