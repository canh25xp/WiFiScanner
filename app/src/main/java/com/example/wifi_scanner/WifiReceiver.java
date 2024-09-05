package com.example.wifi_scanner;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

public class WifiReceiver extends BroadcastReceiver {
    private static final String TAG = "MY_WIFI_SCANNER";
    WifiManager wifiManager;
    private List<ScanResult> mScanResults = new ArrayList<>();
    String currentWifiConnected;
    private Context context;

    public WifiReceiver(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    private final MutableLiveData<List<ScanResult>> mData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isWifiOn = new MutableLiveData<>();
    private final MutableLiveData<WifiInfo> connectedAP = new MutableLiveData<>();

    public LiveData<List<ScanResult>> getData() {
        return mData;
    }

    public LiveData<Boolean> getWifistatus() {
        return isWifiOn;
    }

    public LiveData<WifiInfo> getWifiConnected() {
        return connectedAP;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);

        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            Log.d(TAG, "onReceive: SCAN_RESULTS_AVAILABLE_ACTION" + mScanResults.size());
            ScanResultWifiChange(wifiManager.getScanResults());
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            currentWifiConnected = getCurrentSsid(context);
            if (currentWifiConnected != null) {
                connectedAP.setValue(wifiManager.getConnectionInfo());
            } else
                connectedAP.setValue(null);
            ScanResultWifiChange(wifiManager.getScanResults());
        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "onReceive: WIFI_STATE_CHANGED_ACTION" + wifiManager.isWifiEnabled());
            if (wifiManager.isWifiEnabled()) {
                isWifiOn.setValue(true);
            } else {
                isWifiOn.setValue(false);
            }
        }
    }

    public String getCurrentSsid(@NonNull Context context) {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String ssid = null;
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    ssid = wifiInfo.getSSID();
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
            }
        }
        return ssid;
    }

    private void ScanResultWifiChange(@Nullable List<ScanResult> ScanResults) {
        mScanResults.clear();
        for (ScanResult scanResult : ScanResults) {
            String name = scanResult.SSID + "";
            if (currentWifiConnected != null && currentWifiConnected != "" &&
                    currentWifiConnected.equals(name)) {
                continue;
            }

            Log.d(TAG, "SSID " + name);
            if (!name.equals("") && scanResult.level >= -100) {
                mScanResults.add(scanResult);
            }
        }
        mData.setValue(mScanResults);
    }
}
