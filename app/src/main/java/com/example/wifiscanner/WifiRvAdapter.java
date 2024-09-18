package com.example.wifiscanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WifiRvAdapter extends RecyclerView.Adapter<WifiRvAdapter.ViewHolder> {
    private static final String TAG = "MY_WIFI_SCANNER";
    private final List<ScanResult> mScanResults;
    private final Context mContext;
    public int iconWifi = 0;
    public String titleWifi = "";

    public WifiRvAdapter(Context context) {
        mContext = context;
        mScanResults = new ArrayList<>();
    }

    @NonNull
    @Override
    public WifiRvAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiRvAdapter.ViewHolder holder, int position) {
        ScanResult scanResult = mScanResults.get(position);
        String name = scanResult.SSID;
        int level = scanResult.level;
        String capabilities = scanResult.capabilities;

        @SuppressLint({"NewApi", "LocalSuppress"}) int wifiStandard = scanResult.getWifiStandard();
        Log.d(TAG, "onBindViewHolder: " + wifiStandard);
        if (!name.isEmpty()) {
            viewLevel(level);
            holder.wifiIcon.setImageResource(iconWifi);
            holder.wifiName.setText(scanResult.SSID);
            holder.wifiStatus.setText(titleWifi);

            if (capabilities.contains("WPA") || capabilities.contains("WEP")) holder.wifiLock.setVisibility(View.VISIBLE);
            else holder.wifiLock.setVisibility(View.GONE);

            if (wifiStandard == 6) holder.wifi6.setVisibility(View.VISIBLE);
            else holder.wifi6.setVisibility(View.GONE);

        }
        holder.itemView.setOnClickListener(v -> {
            if (capabilities.contains("WPA") || capabilities.contains("WEP")) {
                Log.d(TAG, "onClick: item");
                Intent intent = new Intent(mContext, PasswordActivity.class);
                intent.putExtra("NAME WIFI", name);
                mContext.startActivity(intent);
            } else PasswordActivity.connectToNetWork(name, "", mContext);
        });
    }

    @Override
    public int getItemCount() {
        return mScanResults.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<ScanResult> wifiList) {
        mScanResults.clear();
        mScanResults.addAll(wifiList);
        mScanResults.sort(new levelSort());
        notifyDataSetChanged();
    }

    private static class levelSort implements Comparator<ScanResult> {
        @Override
        public int compare(ScanResult o1, ScanResult o2) {
            return o2.level - o1.level;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView wifiIcon;
        TextView wifiName;
        TextView wifiStatus;
        ImageView wifiLock, wifi6;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            wifiIcon = itemView.findViewById(R.id.img_wifi_icon);
            wifiName = itemView.findViewById(R.id.tv_wifi_name);
            wifiStatus = itemView.findViewById(R.id.tv_wifi_status);
            wifiLock = itemView.findViewById(R.id.img_wifi_lock);
            wifi6 = itemView.findViewById(R.id.img_wifi_6);
        }
    }

    public void viewLevel(int level) {
        if (level >= -65) {
            iconWifi = R.drawable.lv4;
            titleWifi = "very good";
        } else if (level >= -80) {
            iconWifi = R.drawable.wifi_lv3;
            titleWifi = "good";
        } else if (level >= -90) {
            iconWifi = R.drawable.wifi_lv2;
            titleWifi = "normal";
        } else {
            iconWifi = R.drawable.wifi_lv1;
            titleWifi = "effect";
        }
    }
}
