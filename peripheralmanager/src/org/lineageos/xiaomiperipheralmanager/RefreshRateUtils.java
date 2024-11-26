package org.lineageos.xiaomiperipheralmanager;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

public class RefreshRateUtils {

    private static final String TAG = "RefreshRateManager";
    private static final boolean DEBUG = false;

    private static final int DEFAULT_MIN_REFRESH_RATE = 60;
    private static final int DEFAULT_MAX_REFRESH_RATE = 144;
    private static final int PEN_MAX_REFRESH_RATE = 120;

    private final Context mContext;

    public RefreshRateUtils(Context context) {
        mContext = context;
    }

    // Sets refresh rates for pen mode (60-120Hz).
    public void setPenModeRefreshRate() {
        if (DEBUG) Log.d(TAG, "setPenModeRefreshRate: Setting refresh rate to 60-120Hz for pen mode");
        setMinRefreshRate(DEFAULT_MIN_REFRESH_RATE);
        setPeakRefreshRate(PEN_MAX_REFRESH_RATE);
    }

    // Resets refresh rates to default mode (60-144Hz).
    public void setDefaultRefreshRate() {
        if (DEBUG) Log.d(TAG, "setDefaultRefreshRate: Restoring refresh rate to 60-144Hz");
        setMinRefreshRate(DEFAULT_MIN_REFRESH_RATE);
        setPeakRefreshRate(DEFAULT_MAX_REFRESH_RATE);
    }

    // Set the minimum refresh rate in system settings.
    private void setMinRefreshRate(int refreshRate) {
        Settings.System.putFloat(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, (float) refreshRate);
    }

    // Set the peak (maximum) refresh rate in system settings.
    private void setPeakRefreshRate(int refreshRate) {
        Settings.System.putFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, (float) refreshRate);
    }
}
