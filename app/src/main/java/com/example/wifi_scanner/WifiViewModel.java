package com.example.wifi_scanner;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class WifiViewModel extends ViewModel {
    public LiveData<List<ScanResult>> getData() {
        return Reposity.instance().getData();
    }

    public LiveData<Boolean> getStatus() {
        return Reposity.instance().getWifiStatus();
    }

    public LiveData<WifiInfo> getConnectedAP() {
        return Reposity.instance().getWifiAP();
    }
}
