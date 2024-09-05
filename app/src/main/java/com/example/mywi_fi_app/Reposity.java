package com.example.mywi_fi_app;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import java.util.List;

public class Reposity {
    private static final Reposity INSTANCE = new Reposity();
    private final MediatorLiveData<List<ScanResult>> mData = new MediatorLiveData<>();
    private final MediatorLiveData<Boolean> mmIsON = new MediatorLiveData<>();
    private final MediatorLiveData<WifiInfo> mWifi = new MediatorLiveData<>();

    private Reposity() {
    }

    public static Reposity instance() {
        return INSTANCE;
    }

    public LiveData<List<ScanResult>> getData() {
        return mData;
    }

    public LiveData<Boolean> getWifiStatus() {
        return mmIsON;
    }

    public LiveData<WifiInfo> getWifiAP() {
        return mWifi;
    }

    public void addDataSource(LiveData<List<ScanResult>> data, LiveData<Boolean> status, LiveData<WifiInfo> wifiInfoLiveData) {
        mData.addSource(data, mData::setValue);
        mmIsON.addSource(status, mmIsON::setValue);
        mWifi.addSource(wifiInfoLiveData, mWifi::setValue);
    }

    public void removeDataSource(LiveData<List<ScanResult>> data, LiveData<Boolean> status, LiveData<WifiInfo> wifiInfoLiveData) {
        mData.removeSource(data);
        mmIsON.removeSource(status);
        mWifi.removeSource(wifiInfoLiveData);
    }
}
