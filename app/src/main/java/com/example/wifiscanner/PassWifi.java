package com.example.wifiscanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class PassWifi extends AppCompatActivity implements View.OnClickListener {
    private Context context;
    Button button;
    EditText password;
    String SSID;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_pass_wifi);
        password = findViewById(R.id.input_password);
        button = findViewById(R.id.connect_button);
        button.setOnClickListener(this);
        Bundle bundle = getIntent().getExtras();
        TextView title = findViewById(R.id.connect_title);
        SSID = bundle.getString("NAME WIFI");
        title.setText("Connect to " + SSID);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.connect_button) {
            connectToNetWork(SSID, password.getText().toString(), context);
            finish();
        }
    }

    static void connectToNetWork(String SSID, String password, Context mContext) {
        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
        builder.setSsid(SSID);
        builder.setWpa2Passphrase(password);
        WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);
        NetworkRequest networkRequest = networkRequestBuilder.build();
        ConnectivityManager cm = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            cm.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    cm.bindProcessToNetwork(network);
                }
            });
        }
    }
}