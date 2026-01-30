package com.example.wifidirectp2p;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于在 RecyclerView 中展示发现的 Wi-Fi Direct 设备列表。
 */
public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

    private final List<WifiP2pDevice> devices = new ArrayList<>();
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(WifiP2pDevice device);
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void updateDevices(List<WifiP2pDevice> newDevices) {
        devices.clear();
        devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WifiP2pDevice device = devices.get(position);
        String name = device.deviceName;
        if (name == null || name.isEmpty()) {
            name = "未知设备";
        }
        holder.tvDeviceName.setText(name);
        holder.tvDeviceAddress.setText(device.deviceAddress);
        holder.tvDeviceStatus.setText(getDeviceStatus(device.status));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    /**
     * 将设备状态码转换为可读的中文描述
     */
    private String getDeviceStatus(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return "可用";
            case WifiP2pDevice.INVITED:
                return "已邀请";
            case WifiP2pDevice.CONNECTED:
                return "已连接";
            case WifiP2pDevice.FAILED:
                return "失败";
            case WifiP2pDevice.UNAVAILABLE:
                return "不可用";
            default:
                return "未知";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDeviceName;
        final TextView tvDeviceAddress;
        final TextView tvDeviceStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvDeviceAddress = itemView.findViewById(R.id.tv_device_address);
            tvDeviceStatus = itemView.findViewById(R.id.tv_device_status);
        }
    }
}
