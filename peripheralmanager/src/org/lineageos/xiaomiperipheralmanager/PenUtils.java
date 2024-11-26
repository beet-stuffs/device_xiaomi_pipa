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
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.InputDevice;

public class PenUtils {

    private static final String TAG = "XiaomiPeripheralManagerPenUtils";
    private static final boolean DEBUG = false;

    private static final int PEN_VENDOR_ID = 6421;
    private static final int PEN_PRODUCT_ID = 19841;
    private static final String STYLUS_KEY = "stylus_switch_key";

    private static Context mContext;
    private static InputManager mInputManager;
    private static RefreshRateUtils mRefreshRateUtils;
    private static SharedPreferences mPreferences;

    public static void setup(Context context) {
        mContext = context;
        mInputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        mRefreshRateUtils = new RefreshRateUtils(context);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        mInputManager.registerInputDeviceListener(mInputDeviceListener, null);
        refreshPenMode();
    }

    // Enable pen mode and update refresh rates
    public static void enablePenMode() {
        Log.d(TAG, "enablePenMode: Activating Pen Mode");
        SystemProperties.set("persist.vendor.parts.pen", "18");
        mRefreshRateUtils.setPenModeRefreshRate();
    }

    // Disable pen mode and restore default refresh rates
    public static void disablePenMode() {
        Log.d(TAG, "disablePenMode: Deactivating Pen Mode");
        SystemProperties.set("persist.vendor.parts.pen", "2");
        mRefreshRateUtils.setDefaultRefreshRate();
    }

    private static void refreshPenMode() {
        boolean isStylusEnabled = mPreferences.getBoolean(STYLUS_KEY, false);
        boolean isPenDetected = isStylusEnabled || isDeviceXiaomiPen();

        if (isPenDetected) {
            if (DEBUG) Log.d(TAG, "refreshPenMode: Pen or stylus mode enabled");
            enablePenMode();
        } else {
            if (DEBUG) Log.d(TAG, "refreshPenMode: Pen not detected, disabling pen mode");
            disablePenMode();
        }
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
            refreshPenMode();
        }

        @Override
        public void onInputDeviceRemoved(int id) {
            refreshPenMode();
        }

        @Override
        public void onInputDeviceChanged(int id) {
            refreshPenMode();
        }
    };
}
