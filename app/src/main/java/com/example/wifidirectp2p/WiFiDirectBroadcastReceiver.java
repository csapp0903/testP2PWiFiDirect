package com.example.wifidirectp2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * 监听 Wi-Fi Direct 相关系统广播的 BroadcastReceiver。
 * 处理以下事件：
 * - Wi-Fi P2P 状态变化
 * - 可用对等设备列表变化
 * - 连接状态变化
 * - 本机设备信息变化
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WiFiDirectReceiver";

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final MainActivity activity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager,
                                       WifiP2pManager.Channel channel,
                                       MainActivity activity) {
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                // Wi-Fi Direct 开关状态变化
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                boolean isEnabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
                activity.onWifiP2pStateChanged(isEnabled);
                Log.d(TAG, "Wi-Fi P2P 状态: " + (isEnabled ? "已启用" : "已禁用"));
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                // 可用对等设备列表发生变化，请求最新列表
                Log.d(TAG, "对等设备列表已变化");
                activity.requestPeers();
                break;

            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                // 连接状态变化
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    Log.d(TAG, "已连接到对等设备");
                    activity.requestConnectionInfo();
                } else {
                    Log.d(TAG, "已断开连接");
                    activity.onDisconnected();
                }
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                // 本机设备信息变化
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                if (device != null) {
                    Log.d(TAG, "本机设备: " + device.deviceName);
                    activity.onSelfDeviceChanged(device);
                }
                break;
        }
    }
}
