package nz.co.zsd.floatingvolume;

import com.google.android.material.color.DynamicColors;

public class FloatingOverlayApp extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
