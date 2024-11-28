/*
 * Copyright (C) 2023 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xiaomiperipheralmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.os.FileObserver;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.InputDevice;

import org.lineageos.xiaomiperipheralmanager.FileUtils;

public class PenUtils {

    private static final String TAG = "XiaomiPeripheralManagerPenUtils";
    private static final boolean DEBUG = true;

    private static final int PEN_VENDOR_ID = 6421;
    private static final int PEN_PRODUCT_ID = 19841;
    private static final String PEN_CHARGE_NODE = "/sys/class/power_supply/wireless/reverse_chg_mode";
    private static final String STYLUS_KEY = "stylus_switch_key";

    private static Context mContext;
    private static InputManager mInputManager;
    private static RefreshRateUtils mRefreshRateUtils;
    private static SharedPreferences mPreferences;

    private static FileObserver mFileObserver;
    private static boolean lastDockedState = false;

    public static void setup(Context context) {
        mContext = context;
        mInputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        mRefreshRateUtils = new RefreshRateUtils(context);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mInputManager.registerInputDeviceListener(mInputDeviceListener, null);

        setupFileObserver();
        refreshPenState();
    }

    public static void enablePenMode() {
        if (DEBUG) Log.d(TAG, "enablePenMode: Activating Pen Mode");
        SystemProperties.set("persist.vendor.parts.pen", "18");
        refreshPenState();
    }

    public static void disablePenMode() {
        if (DEBUG) Log.d(TAG, "disablePenMode: Deactivating Pen Mode");
        SystemProperties.set("persist.vendor.parts.pen", "2");
        refreshPenState();
    }

    private static void refreshPenState() {
        boolean isStylusEnabled = mPreferences.getBoolean(STYLUS_KEY, false);
        boolean isPenDetected = isStylusEnabled || isDeviceXiaomiPen();
        boolean isPenDocked = isPenDocked();

        if (DEBUG) Log.d(TAG, "refreshPenState: StylusEnabled=" + isStylusEnabled + ", PenDetected=" + isPenDetected + ", PenDocked=" + isPenDocked);

        if (isStylusEnabled || isPenDetected) {
            if (isPenDocked) {
                if (DEBUG) Log.d(TAG, "Pen detected and docked: Setting refresh rate to 144Hz");
                mRefreshRateUtils.setDefaultRefreshRate();
            } else {
                if (DEBUG) Log.d(TAG, "Pen detected or stylus enabled: Setting refresh rate to 120Hz");
                mRefreshRateUtils.setPenModeRefreshRate();
            }
        } else {
            if (DEBUG) Log.d(TAG, "No pen detected or stylus disabled: Restoring default refresh rate");
            mRefreshRateUtils.setDefaultRefreshRate();
        }

        // Force refresh rate update
        enforceRefreshRateUpdate();
        reinitializeInputDevices();
    }

    private static void setupFileObserver() {
        mFileObserver = new FileObserver(PEN_CHARGE_NODE, FileObserver.MODIFY) {
            @Override
            public void onEvent(int event, String path) {
                if (event == FileObserver.MODIFY) {
                    boolean isDocked = isPenDocked();
                    if (isDocked != lastDockedState) {
                        refreshPenState();
                        lastDockedState = isDocked;
                    }
                }
            }
        };
        mFileObserver.startWatching();
    }

    private static boolean isPenDocked() {
        String state = FileUtils.readOneLine(PEN_CHARGE_NODE);
        return "1".equals(state); // 1 indicates charging (docked)
    }

    private static boolean isDeviceXiaomiPen() {
        for (int id : mInputManager.getInputDeviceIds()) {
            InputDevice inputDevice = mInputManager.getInputDevice(id);
            if (inputDevice != null &&
                inputDevice.getVendorId() == PEN_VENDOR_ID &&
                inputDevice.getProductId() == PEN_PRODUCT_ID) {
                return true;
            }
        }
        return false;
    }

    private static InputDeviceListener mInputDeviceListener = new InputDeviceListener() {
        @Override
        public void onInputDeviceAdded(int id) {
            refreshPenState();
        }

        @Override
        public void onInputDeviceRemoved(int id) {
            refreshPenState();
        }

        @Override
        public void onInputDeviceChanged(int id) {
            refreshPenState();
        }
    };

    private static void enforceRefreshRateUpdate() {
        if (DEBUG) Log.d(TAG, "enforceRefreshRateUpdate: Forcing display refresh update");
        DisplayManager displayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.recalculateRefreshRates();
        }
    }

    private static void reinitializeInputDevices() {
        if (DEBUG) Log.d(TAG, "reinitializeInputDevices: Re-registering input devices");
        mInputManager.unregisterInputDeviceListener(mInputDeviceListener);
        mInputManager.registerInputDeviceListener(mInputDeviceListener, null);
    }
}
