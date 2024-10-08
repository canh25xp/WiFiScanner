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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PasswordActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText password;
    private String SSID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Hide the action bar on top
        if (getSupportActionBar() != null) this.getSupportActionBar().hide();

        password = findViewById(R.id.et_password);
        Button button = findViewById(R.id.btn_connect);
        button.setOnClickListener(this);
        Bundle bundle = getIntent().getExtras();
        TextView title = findViewById(R.id.tv_connect_title);
        assert bundle != null;
        SSID = bundle.getString("NAME WIFI");
        title.setText("Connect to " + SSID);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_connect) {
            connectToNetWork(SSID, password.getText().toString(), this);
            finish();
        }
    }

    public static void connectToNetWork(String SSID, String password, Context mContext) {
        NetworkRequest networkRequest = getNetworkRequest(SSID, password);
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
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

    @SuppressLint("NewApi")
    private static NetworkRequest getNetworkRequest(String SSID, String password) {
        WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
        builder.setSsid(SSID);
        builder.setWpa2Passphrase(password);
        WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);
        return networkRequestBuilder.build();
    }
}