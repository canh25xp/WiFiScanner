package com.example.wifiscanner;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class WifiViewModel extends ViewModel {
    public LiveData<List<ScanResult>> getData() {
        return Repository.instance().getData();
    }

    public LiveData<Boolean> getStatus() {
        return Repository.instance().getWifiStatus();
    }

    public LiveData<WifiInfo> getConnectedAP() {
        return Repository.instance().getWifiAP();
    }
}
