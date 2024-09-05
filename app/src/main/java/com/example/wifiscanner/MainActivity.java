package com.example.wifiscanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.wifiscanner.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MY_WIFI_SCANNER";
    private ActivityMainBinding mBinding;
    private WifiReceiver mReceiver;
    private WifiManager mWifiManager;
    private final RecyclerAdapter mAdapter = new RecyclerAdapter(this);
    private IntentFilter filterRefreshUpdate;
    private boolean mWifiState = false;
    private int iconWifi;
    private String titleWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mReceiver = new WifiReceiver(mWifiManager);
        mBinding.swWifi.setOnClickListener(this);
        mBinding.btnQrCode.setOnClickListener(this);
        initFilterAction();
        registerReceiver(mReceiver, filterRefreshUpdate);
        Repository.instance().addDataSource(mReceiver.getData(), mReceiver.getWifistatus(), mReceiver.getWifiConnected());
        setMode(mWifiState);
        setupViewModel();
        setupAdapter();
        mWifiState = mWifiManager.isWifiEnabled();
        Log.d(TAG, "mWifiState: " + mWifiState);

        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_main), (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        if (getSupportActionBar() != null) this.getSupportActionBar().hide();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        mWifiManager.startScan();
        setupConnectedView();
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        Repository.instance().removeDataSource(mReceiver.getData(), mReceiver.getWifistatus(), mReceiver.getWifiConnected());
        super.onDestroy();
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.sw_wifi) {
            setupConnectedView();
            mWifiState = mWifiManager.isWifiEnabled();
            mBinding.swWifi.setChecked(mWifiState);
            if (mBinding.swWifi.isChecked()) {
                mWifiManager.setWifiEnabled(false);
                Toast.makeText(this, "WiFi Off", Toast.LENGTH_SHORT).show();
                setMode(mWifiState);
            } else {
                mWifiManager.setWifiEnabled(true);
                Toast.makeText(this, "WiFi On", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.btn_qr_code) {
            Toast.makeText(this, "TODO: Implement this", Toast.LENGTH_SHORT).show();
        }
    }

    void setMode(boolean checkOnWifi) {
        mBinding.svWifi.setVisibility(checkOnWifi ? View.VISIBLE : View.GONE);
        mBinding.tvWifiOff.setVisibility(checkOnWifi ? View.GONE : View.VISIBLE);
        mBinding.cvQrCode.setVisibility(checkOnWifi ? View.VISIBLE : View.GONE);
        mBinding.swWifi.setText(checkOnWifi ? "ON" : "OFF");
        mBinding.swWifi.setChecked(checkOnWifi);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        setupConnectedView();

        if (checkOnWifi) mWifiManager.startScan();
    }

    private void initFilterAction() {
        filterRefreshUpdate = new IntentFilter();
        filterRefreshUpdate.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filterRefreshUpdate.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filterRefreshUpdate.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    private void setupAdapter() {
        mBinding.rcAvailableNetworks.setLayoutManager(new LinearLayoutManager(this));
        mBinding.rcAvailableNetworks.setAdapter(mAdapter);
        mBinding.rcAvailableNetworks.setItemAnimator(new DefaultItemAnimator());
    }

    private void setupViewModel() {
        WifiViewModel viewModel = new ViewModelProvider(this).get(WifiViewModel.class);
        viewModel.getData().observe(this, scanResults -> {
            if (mWifiManager.isWifiEnabled() && scanResults != null && !scanResults.isEmpty()) {
                Log.d(TAG, "onChanged: " + scanResults.size());
                mAdapter.updateData(scanResults);
            }
        });
        viewModel.getStatus().observe(this, aBoolean -> {
            Log.d(TAG, "onChanged viewModel.getStatus() " + aBoolean);
            mBinding.tvWifiOff.setVisibility(aBoolean ? View.GONE : View.VISIBLE);
            setMode(aBoolean);
        });
        viewModel.getConnectedAP().observe(this, wifiInfo -> {
            Log.d(TAG, "setupViewModel: " + wifiInfo);
            viewConnectDisconnect(wifiInfo);
        });
    }

    void viewConnectDisconnect(WifiInfo wifiInfo) {
        if (wifiInfo != null) {
            Log.d(TAG, "onChanged viewModel.wifiInfo " + wifiInfo.getSSID());
            int level = wifiInfo.getRssi();
            String ssid = wifiInfo.getSSID();
            mBinding.cvCurrentNetwork.setVisibility(View.VISIBLE);
            mBinding.layoutConnected.tvConnectedName.setText(ssid.substring(1, ssid.length() - 1));
            viewLevel(level);
            mBinding.layoutConnected.tvConnectedStatus.setText(titleWifi);
            mBinding.layoutConnected.wifiIcon.setImageResource(iconWifi);
        } else {
            mBinding.cvCurrentNetwork.setVisibility(View.GONE);
            Log.d(TAG, "setupViewModel: ");
        }
    }

    public void setupConnectedView() {
        String ssid;
        ConnectivityManager connManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnectedOrConnecting()) {
            Log.d(TAG, "setupConnectedView: " + networkInfo.isConnectedOrConnecting());
            final WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
                int level = connectionInfo.getRssi();
                @SuppressLint({"NewApi", "LocalSuppress"}) int wifi6 = connectionInfo.getWifiStandard();
                ssid = ssid.substring(1, ssid.length() - 1);
                mBinding.layoutConnected.tvConnectedName.setText(ssid);
                viewLevel(level);
                mBinding.cvConnected.setVisibility(View.VISIBLE);
                mBinding.layoutConnected.tvConnectedStatus.setText(titleWifi);
                mBinding.layoutConnected.wifiIcon.setImageResource(iconWifi);
                if (wifi6 == 6) mBinding.layoutConnected.wifi6.setVisibility(View.VISIBLE);
                else mBinding.layoutConnected.wifi6.setVisibility(View.GONE);
                @SuppressLint("MissingPermission")
                List<ScanResult> networkList = mWifiManager.getScanResults();
                if (networkList != null) {
                    for (ScanResult network : networkList) {
                        if (ssid.equals(network.SSID)) {
                            String capabilities = network.capabilities;

                            if (capabilities.contains("WPA") || capabilities.contains("WEP"))
                                mBinding.layoutConnected.iconlockconnected.setVisibility(View.VISIBLE);
                            else
                                mBinding.layoutConnected.iconlockconnected.setVisibility(View.GONE);
                        }
                    }
                }
            } else {
                mBinding.cvConnected.setVisibility(View.GONE);
            }
        } else {
            mBinding.cvCurrentNetwork.setVisibility(View.GONE);
        }
    }

    private void viewLevel(int level) {
        if (level >= -65) {
            iconWifi = R.drawable.lv4;
            titleWifi = "connected / very good";
        } else if (level >= -80) {
            iconWifi = R.drawable.wifi_lv3;
            titleWifi = "connected / good";
        } else if (level >= -90) {
            iconWifi = R.drawable.wifi_lv2;
            titleWifi = "connected / normal";
        } else {
            iconWifi = R.drawable.wifi_lv1;
            titleWifi = "connected / effect";
        }
    }
}