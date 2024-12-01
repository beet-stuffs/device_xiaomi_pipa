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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import android.os.FileObserver;

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

    // Enable pen mode and update refresh rates
    public static void enablePenMode() {
        if (DEBUG) Log.d(TAG, "enablePenMode: Activating Pen Mode");
        SystemProperties.set("persist.vendor.parts.pen", "18");
        refreshPenState();
    }

    // Disable pen mode and restore default refresh rates
    public static void disablePenMode() {
        if (DEBUG) Log.d(TAG, "disablePenMode: Deactivating Pen Mode");
        SystemProperties.set("persist.vendor.parts.pen", "2");
        refreshPenState();
    }

    // Refresh the pen state based on detection, docking, and stylus_key preference
    private static void refreshPenState() {
        boolean isStylusEnabled = mPreferences.getBoolean(STYLUS_KEY, false);
        boolean isPenDetected = isStylusEnabled || isDeviceXiaomiPen();
        boolean isPenDocked = isPenDocked();

        if (isStylusEnabled) {
             if (DEBUG) Log.d(TAG, "Stylus enabled: Setting refresh rate to 120Hz");
            mRefreshRateUtils.setPenModeRefreshRate();
        } else if (isPenDetected) {
            if (isPenDocked) {
                if (DEBUG) Log.d(TAG, "Pen detected and docked: Setting refresh rate to 144Hz");
                mRefreshRateUtils.setDefaultRefreshRate();
            } else {
                if (DEBUG) Log.d(TAG, "Pen detected but not docked: Setting refresh rate to 120Hz");
                mRefreshRateUtils.setPenModeRefreshRate();
            }
        } else {
            if (DEBUG) Log.d(TAG, "No pen or stylus detected: Restoring default refresh rate");
            mRefreshRateUtils.setDefaultRefreshRate();
        }
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

    // Check if the pen is docked (charging)
    private static boolean isPenDocked() {
        try (BufferedReader br = new BufferedReader(new FileReader(PEN_CHARGE_NODE))) {
            String state = br.readLine();
            return "1".equals(state); // 1 indicates charging (docked)
        } catch (IOException e) {
            Log.e(TAG, "Failed to read reverse_chg_mode node", e);
            return false;
        }
    }

    // Detect if the Xiaomi pen is connected
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
}
