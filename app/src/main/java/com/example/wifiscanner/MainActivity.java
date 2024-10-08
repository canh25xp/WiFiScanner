package com.example.wifiscanner;

import android.Manifest;
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

/** @noinspection ALL*/
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MY_WIFI_SCANNER";
    private ActivityMainBinding mBinding;
    private WifiReceiver mWifiReceiver;
    private WifiManager mWifiManager;
    private WifiRvAdapter mAdapter;
    private IntentFilter mFilterRefreshUpdate;
    private boolean mWifiState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Create");
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        // Enabled Edge to Edge display
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_main), (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // Hide the action bar on top
        if (getSupportActionBar() != null) this.getSupportActionBar().hide();

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiReceiver = new WifiReceiver(mWifiManager);
        mBinding.swWifi.setOnClickListener(this);
        mBinding.btnQrCode.setOnClickListener(this);

        // Init Filter Action
        mFilterRefreshUpdate = new IntentFilter();
        mFilterRefreshUpdate.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilterRefreshUpdate.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilterRefreshUpdate.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mWifiReceiver, mFilterRefreshUpdate);
        Repository.instance().addDataSource(mWifiReceiver.getData(), mWifiReceiver.getWifiStatus(), mWifiReceiver.getWifiConnected());

        mWifiState = mWifiManager.isWifiEnabled();
        setMode(mWifiState);
        setupViewModel();

        // Setup Adapter
        mAdapter = new WifiRvAdapter(this);
        mBinding.rcAvailableNetworks.setLayoutManager(new LinearLayoutManager(this));
        mBinding.rcAvailableNetworks.setAdapter(mAdapter);
        mBinding.rcAvailableNetworks.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "Start");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Resume");
        super.onResume();
        mWifiManager.startScan();
        setupConnectedView();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Pause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Destroy");
        unregisterReceiver(mWifiReceiver);
        Repository.instance().removeDataSource(mWifiReceiver.getData(), mWifiReceiver.getWifiStatus(), mWifiReceiver.getWifiConnected());
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
            Intent intent = new Intent(this, QRCodeActivity.class);
            this.startActivity(intent);
        }
    }

    void setMode(boolean checkOnWifi) {
        mBinding.svWifi.setVisibility(checkOnWifi ? View.VISIBLE : View.GONE);
        mBinding.tvWifiOff.setVisibility(checkOnWifi ? View.GONE : View.VISIBLE);
        mBinding.cvQrCode.setVisibility(checkOnWifi ? View.VISIBLE : View.GONE);
        mBinding.swWifi.setText(checkOnWifi ? "On" : "Off");
        mBinding.swWifi.setChecked(checkOnWifi);
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
            mAdapter.viewLevel(level);
            mBinding.layoutConnected.tvConnectedStatus.setText(mAdapter.titleWifi);
            mBinding.layoutConnected.imgWifiIcon.setImageResource(mAdapter.iconWifi);
        } else {
            mBinding.cvCurrentNetwork.setVisibility(View.GONE);
            Log.d(TAG, "setupViewModel: ");
        }
    }

    public void setupConnectedView() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Log.d(TAG, "setupConnectedView: " + networkInfo.isConnectedOrConnecting());
            final WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                mBinding.cvConnected.setVisibility(View.VISIBLE);
                String ssid = connectionInfo.getSSID();
                ssid = ssid.substring(1, ssid.length() - 1); // Remove outline quotes
                int level = connectionInfo.getRssi();
                int standard = connectionInfo.getWifiStandard();

                String bssid = connectionInfo.getBSSID();
                int freq = connectionInfo.getFrequency();

                mBinding.layoutConnected.tvConnectedName.setText(ssid);
                mAdapter.viewLevel(level);
                mBinding.layoutConnected.tvConnectedStatus.setText(mAdapter.titleWifi);
                mBinding.layoutConnected.imgWifiIcon.setImageResource(mAdapter.iconWifi);
                mBinding.layoutConnected.imgWifi6.setVisibility((standard == 6) ? View.VISIBLE : View.GONE);
                List<ScanResult> networkList = mWifiManager.getScanResults();
                if (networkList != null) {
                    for (ScanResult network : networkList) {
                        if (ssid.equals(network.SSID)) {
                            String capabilities = network.capabilities;
                            boolean secured = capabilities.contains("WPA") || capabilities.contains("WEP");
                            mBinding.layoutConnected.imgWifiLockConnected.setVisibility(secured ? View.VISIBLE : View.GONE);
                            break;
                        }
                    }
                }
            } else {
                mBinding.cvConnected.setVisibility(View.GONE);
            }
        }
    }
}