package com.example.wifi_scanner;

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

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private static final String TAG = "MY_WIFI_SCANNER";
    private List<ScanResult> mScanResults;
    private final Context mContext;

    public RecyclerAdapter(Context context) {
        mContext = context;
        mScanResults = new ArrayList<>();
    }

    int iconWifi = 0;
    String titleWifi = "";

    @NonNull
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        ScanResult scanResult = mScanResults.get(position);
        String name = scanResult.SSID + "";
        String level = scanResult.level + "";
        String capabilities = scanResult.capabilities;
        int wifistandard = scanResult.getWifiStandard();
        Log.d(TAG, "onBindViewHolder: " + wifistandard);
        if (!name.equals("") && !level.equals("")) {
            viewLevel(scanResult.level);
            holder.bitmap.setImageResource(iconWifi);
            holder.wifiName.setText(scanResult.SSID + "");
            holder.wifiStatus.setText(titleWifi);

            if (capabilities.contains("WPA") || capabilities.contains("WEP"))
                holder.iconlock.setVisibility(View.VISIBLE);
            else holder.iconlock.setVisibility(View.GONE);

            if (wifistandard == 6) holder.wifi6.setVisibility(View.VISIBLE);
            else holder.wifi6.setVisibility(View.GONE);

        }
        holder.itemView.setOnClickListener(v -> {
            if (capabilities.contains("WPA") || capabilities.contains("WEP")) {
                Log.d(TAG, "onClick: item");
                Intent intent = new Intent(mContext, PassWifi.class);
                intent.putExtra("NAME WIFI", name);
                mContext.startActivity(intent);
            } else PassWifi.connectToNetWork(name, "", mContext);
        });
    }

    @Override
    public int getItemCount() {
        return mScanResults.size();
    }

    public void updateData(List<ScanResult> wifiList) {
        mScanResults.clear();
        mScanResults.addAll(wifiList);
        mScanResults.sort(new levelSort());
        notifyDataSetChanged();
    }

    private class levelSort implements Comparator<ScanResult> {
        @Override
        public int compare(ScanResult o1, ScanResult o2) {
            return o2.level - o1.level;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView bitmap;
        TextView wifiName;
        TextView wifiStatus;
        ImageView iconlock, wifi6;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bitmap = itemView.findViewById(R.id.wifi_icon);
            wifiName = itemView.findViewById(R.id.tv_wifi_name);
            wifiStatus = itemView.findViewById(R.id.tv_wifi_status);
            iconlock = itemView.findViewById(R.id.icon_lock);
            wifi6 = itemView.findViewById(R.id.wifi6);
        }
    }

    private void viewLevel(int level) {
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
