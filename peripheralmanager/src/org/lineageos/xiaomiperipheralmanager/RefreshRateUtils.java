package org.lineageos.xiaomiperipheralmanager;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

public class RefreshRateUtils {

    private static final String TAG = "RefreshRateManager";
    private static final boolean DEBUG = true;

    private static final int DEFAULT_MIN_REFRESH_RATE = 60;
    private static final int DEFAULT_MAX_REFRESH_RATE = 144;
    private static final int PEN_DETACHED_REFRESH_RATE = 120;

    private final Context mContext;

    public RefreshRateUtils(Context context) {
        mContext = context;
    }

    // Sets refresh rate for pen detached mode (60-120Hz).
    public void setPenModeRefreshRate() {
        if (DEBUG) Log.d(TAG, "setPenModeRefreshRate: Setting refresh rate to 60-120Hz for pen detached mode");
        setMinRefreshRate(DEFAULT_MIN_REFRESH_RATE);
        setPeakRefreshRate(PEN_DETACHED_REFRESH_RATE);
    }

    // Sets refresh rate for pen docked mode (60-144Hz).
    public void setDefaultRefreshRate() {
        if (DEBUG) Log.d(TAG, "setHighRefreshRate: Setting refresh rate to 60-144Hz for pen docked mode");
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
