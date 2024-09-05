package com.example.mywi_fi_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mywi_fi_app.databinding.LayoutMainBinding;

import java.util.List;

public class Main extends AppCompatActivity implements View.OnClickListener {
    private static final int QR_CONNECT_REQUEST = 102;
    private LayoutMainBinding mBinding;
    private WifiReceiver mReceiver;
    private WifiManager mWifiManager;
    private final RecyclerAdapter mAdapter = new RecyclerAdapter(this);
    private IntentFilter filterRefreshUpdate;
    private boolean mMode = false;
    private int iconWifi;
    private String titleWifi;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.layout_main);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mReceiver = new WifiReceiver(mWifiManager);
        mBinding.swOnOff.setOnClickListener(this);
        mBinding.wifiQrCode.setOnClickListener(this);
        initFilterAction();
        registerReceiver(mReceiver, filterRefreshUpdate);
        Reposity.instance().addDataSource(mReceiver.getData(), mReceiver.getWifistatus(), mReceiver.getWifiConnected());
        setMode(mMode);
        setupViewModel();
        setupAdapter();
        Log.d("giang", "wifiStatus: " + mWifiManager.isWifiEnabled());
        mMode = mWifiManager.isWifiEnabled();
        hideActionBar();
    }

    private void hideActionBar() {
        if (getSupportActionBar() != null)
            this.getSupportActionBar().hide();
    }

    private void setupViewModel() {
        WifiViewModel viewModel = new ViewModelProvider(this).get(WifiViewModel.class);
        viewModel.getData().observe(this, scanResults -> {
            if (mWifiManager.isWifiEnabled() && scanResults != null && !scanResults.isEmpty()) {
                Log.d("giang2", "onChanged: " + scanResults.size());
                mAdapter.updateData(scanResults);
            }
        });
        viewModel.getStatus().observe(this, aBoolean -> {
            Log.d("giang2", "onChanged viewModel.getStatus() " + aBoolean);
            mBinding.viewWifioff.setVisibility(aBoolean ? View.GONE : View.VISIBLE);
            setMode(aBoolean);
        });
        viewModel.getConnectedAP().observe(this, wifiInfo -> {
            Log.d("giang17", "setupViewModel: "+ wifiInfo);
            viewConnectDisconnect(wifiInfo);
        });
    }
    void viewConnectDisconnect(WifiInfo wifiInfo){
        if(wifiInfo != null) {
            Log.d("giang12", "onChanged viewModel.wifiInfo " + wifiInfo.getSSID());
            int level = wifiInfo.getRssi();
            String ssid = wifiInfo.getSSID();
            mBinding.currentnetwork.setVisibility(View.VISIBLE);
            mBinding.connectedlayout.tvConnectedName.setText(ssid.substring(1,ssid.length()-1));
            viewLevel(level);
            mBinding.connectedlayout.tvConnectedStatus.setText(titleWifi);
            mBinding.connectedlayout.wifiIcon.setImageResource(iconWifi);
        } else {
            mBinding.currentnetwork.setVisibility(View.GONE);
            Log.d("giang17", "setupViewModel: ");
        };
    }
    void setMode(boolean checkOnWifi) {
        mBinding.infoWifi.setVisibility(checkOnWifi ? View.VISIBLE : View.GONE);
        mBinding.viewWifioff.setVisibility(checkOnWifi ? View.GONE : View.VISIBLE);
        mBinding.viewQrCode.setVisibility(checkOnWifi ? View.VISIBLE : View.GONE);
        mBinding.swOnOff.setText(checkOnWifi ? "ON" : "OFF");
        mBinding.swOnOff.setChecked(checkOnWifi);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        setupConnectedView();
        if(checkOnWifi) startScan();
    }
    @Override
    protected void onResume() {
        startScan();
        setupConnectedView();
        super.onResume();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
    }
    private void startScan(){
        mWifiManager.startScan();
    }
    private void initFilterAction(){
        filterRefreshUpdate = new IntentFilter();
        filterRefreshUpdate.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filterRefreshUpdate.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filterRefreshUpdate.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }
    private void setupAdapter(){
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerView.setAdapter(mAdapter);
        mBinding.recyclerView.setItemAnimator(new DefaultItemAnimator());
    }
    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        Reposity.instance().removeDataSource(mReceiver.getData(), mReceiver.getWifistatus(),mReceiver.getWifiConnected());
        super.onDestroy();
    }
    public  void onClick(View v){
        int id = v.getId();
        if (id == R.id.swOnOff){
            setEnableWifi();
            setupConnectedView();
            mMode = mWifiManager.isWifiEnabled();
            if (mBinding.swOnOff.isChecked()){
                setMode(mMode);
            }
        } else if (id == R.id.wifi_qr_code){
            startActivityForResult(new Intent(this, ScannedBarcodeActivity.class), QR_CONNECT_REQUEST);
        }
    }
    void setEnableWifi(){
        Intent settingsIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
        Log.d("giang", "setEnableWifi: ");
        startActivityForResult(settingsIntent, 119);
        mBinding.swOnOff.setChecked(mWifiManager.isWifiEnabled());
    }
    public void setupConnectedView(){
        String ssid;
        ConnectivityManager connManager = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnectedOrConnecting()) {
            Log.d("giang2", "setupConnectedView: "+networkInfo.isConnectedOrConnecting());
            final WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
                int level = connectionInfo.getRssi();
                int wifi6 = connectionInfo.getWifiStandard();
                ssid = ssid.substring(1,ssid.length()-1);
                mBinding.connectedlayout.tvConnectedName.setText(ssid);
                viewLevel(level);
                mBinding.connectedCardview.setVisibility(View.VISIBLE);
                mBinding.connectedlayout.tvConnectedStatus.setText(titleWifi);
                mBinding.connectedlayout.wifiIcon.setImageResource(iconWifi);
                if(wifi6 == 6) mBinding.connectedlayout.wifi6.setVisibility(View.VISIBLE);
                else mBinding.connectedlayout.wifi6.setVisibility(View.GONE);
                @SuppressLint("MissingPermission")
                List<ScanResult> networkList = mWifiManager.getScanResults();
                if (networkList != null) {
                    for (ScanResult network : networkList) {
                        //check if current connected SSID
                        if (ssid.equals(network.SSID)) {
                            //get capabilities of current connection
                            String capabilities =  network.capabilities;

                            if (capabilities.contains("WPA")||capabilities.contains("WEP")) {
                                mBinding.connectedlayout.iconlockconnected.setVisibility(View.VISIBLE);
                            }
                            else  mBinding.connectedlayout.iconlockconnected.setVisibility(View.GONE);
                        }
                    }
                }
            }else {
                mBinding.connectedCardview.setVisibility(View.GONE);
            }
        } else{
            mBinding.currentnetwork.setVisibility(View.GONE);
        }
    }
    private void viewLevel(int level){
        if(level >= -65){
            iconWifi = R.drawable.lv4;
            titleWifi = "connected / very good";
        } else if (level >= -80) {
            iconWifi = R.drawable.wifi_lv3;
            titleWifi =  "connected / good";
        } else if (level >= -90) {
            iconWifi = R.drawable.wifi_lv2;
            titleWifi = "connected / normal";
        } else {
            iconWifi = R.drawable.wifi_lv1;
            titleWifi = "connected / effect";
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == QR_CONNECT_REQUEST) {
                String SSID = data.getStringExtra("QR_SSID");
                String Password = data.getStringExtra("QR_PWD");
                PassWifi.connectToNetWork(SSID, Password, this);
            }
        }
    }

}