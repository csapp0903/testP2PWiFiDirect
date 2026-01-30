package com.example.wifidirectp2p;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 主界面 Activity。
 * 实现 Wi-Fi Direct (P2P) 设备发现与连接功能。
 *
 * 主要流程：
 * 1. 初始化 WifiP2pManager 并获取 Channel
 * 2. 注册 BroadcastReceiver 监听 Wi-Fi Direct 事件
 * 3. 用户点击"发现设备"开始扫描附近的 Wi-Fi Direct 设备
 * 4. 点击列表中的设备发起连接请求
 * 5. 连接成功后显示连接信息（Group Owner IP 等）
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WiFiDirectP2P";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // Wi-Fi P2P 核心组件
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;

    // 控件
    private TextView tvSelfDevice;
    private TextView tvStatus;
    private TextView tvConnectionInfo;
    private Button btnDiscover;
    private Button btnDisconnect;
    private RecyclerView rvDevices;
    private TextView tvLog;
    private ScrollView svLog;

    // 数据
    private DeviceListAdapter adapter;
    private boolean isWifiP2pEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initWifiDirect();
        setupIntentFilter();
        checkPermissions();
    }

    /**
     * 初始化界面控件
     */
    private void initViews() {
        tvSelfDevice = findViewById(R.id.tv_self_device);
        tvStatus = findViewById(R.id.tv_status);
        tvConnectionInfo = findViewById(R.id.tv_connection_info);
        btnDiscover = findViewById(R.id.btn_discover);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        rvDevices = findViewById(R.id.rv_devices);
        tvLog = findViewById(R.id.tv_log);
        svLog = findViewById(R.id.sv_log);

        // 设置设备列表
        adapter = new DeviceListAdapter();
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(adapter);

        // 点击设备发起连接
        adapter.setOnDeviceClickListener(this::connectToDevice);

        // 发现设备按钮
        btnDiscover.setOnClickListener(v -> discoverPeers());

        // 断开连接按钮
        btnDisconnect.setOnClickListener(v -> disconnect());
    }

    /**
     * 初始化 Wi-Fi Direct 管理器
     */
    private void initWifiDirect() {
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiP2pManager == null) {
            appendLog("错误: 该设备不支持 Wi-Fi Direct");
            btnDiscover.setEnabled(false);
            return;
        }
        channel = wifiP2pManager.initialize(this, getMainLooper(), null);
        appendLog("Wi-Fi Direct 初始化完成");
    }

    /**
     * 设置 IntentFilter，监听 Wi-Fi Direct 相关广播
     */
    private void setupIntentFilter() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    /**
     * 检查并请求运行时权限
     */
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: 需要 NEARBY_WIFI_DEVICES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }
        }

        // Android 6.0+: 需要位置权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            appendLog("权限已就绪");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                appendLog("所有权限已获取");
            } else {
                appendLog("警告: 部分权限被拒绝，Wi-Fi Direct 功能可能无法正常工作");
                Toast.makeText(this, "需要权限才能使用 Wi-Fi Direct", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, channel, this);
        registerReceiver(receiver, intentFilter);
        appendLog("广播接收器已注册");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) {
            unregisterReceiver(receiver);
            appendLog("广播接收器已注销");
        }
    }

    // ==================== Wi-Fi Direct 操作 ====================

    /**
     * 开始发现附近的 Wi-Fi Direct 设备
     */
    private void discoverPeers() {
        if (!isWifiP2pEnabled) {
            appendLog("错误: Wi-Fi Direct 未启用，请先打开 Wi-Fi");
            Toast.makeText(this, "请先启用 Wi-Fi Direct", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasRequiredPermissions()) {
            appendLog("错误: 缺少必要权限");
            checkPermissions();
            return;
        }

        appendLog("开始发现设备...");
        btnDiscover.setEnabled(false);
        btnDiscover.setText("搜索中...");

        try {
            wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    appendLog("设备发现已启动（等待结果回调）");
                    runOnUiThread(() -> {
                        btnDiscover.setEnabled(true);
                        btnDiscover.setText("发现设备");
                    });
                }

                @Override
                public void onFailure(int reason) {
                    appendLog("设备发现失败，原因: " + getFailureReason(reason));
                    runOnUiThread(() -> {
                        btnDiscover.setEnabled(true);
                        btnDiscover.setText("发现设备");
                    });
                }
            });
        } catch (SecurityException e) {
            appendLog("权限异常: " + e.getMessage());
            btnDiscover.setEnabled(true);
            btnDiscover.setText("发现设备");
        }
    }

    /**
     * 向指定设备发起 Wi-Fi Direct 连接
     */
    private void connectToDevice(WifiP2pDevice device) {
        String name = device.deviceName;
        if (name == null || name.isEmpty()) name = "未知设备";

        String finalName = name;
        new AlertDialog.Builder(this)
                .setTitle("连接设备")
                .setMessage("是否连接到: " + finalName + "\n地址: " + device.deviceAddress)
                .setPositiveButton("连接", (dialog, which) -> {
                    performConnect(device, finalName);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void performConnect(WifiP2pDevice device, String deviceName) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        appendLog("正在连接到: " + deviceName + " (" + device.deviceAddress + ")");

        try {
            wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    appendLog("连接请求已发送到: " + deviceName);
                }

                @Override
                public void onFailure(int reason) {
                    appendLog("连接请求失败: " + getFailureReason(reason));
                }
            });
        } catch (SecurityException e) {
            appendLog("连接权限异常: " + e.getMessage());
        }
    }

    /**
     * 断开 Wi-Fi Direct 连接
     */
    private void disconnect() {
        if (wifiP2pManager != null && channel != null) {
            wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    appendLog("已断开连接");
                    runOnUiThread(() -> {
                        tvConnectionInfo.setText("连接状态: 未连接");
                        btnDisconnect.setEnabled(false);
                    });
                }

                @Override
                public void onFailure(int reason) {
                    appendLog("断开连接失败: " + getFailureReason(reason));
                }
            });
        }
    }

    // ==================== BroadcastReceiver 回调方法 ====================

    /**
     * Wi-Fi P2P 状态变化回调
     */
    public void onWifiP2pStateChanged(boolean enabled) {
        isWifiP2pEnabled = enabled;
        tvStatus.setText("Wi-Fi Direct 状态: " + (enabled ? "已启用" : "已禁用"));
        btnDiscover.setEnabled(enabled);
        if (!enabled) {
            appendLog("Wi-Fi Direct 已禁用");
        } else {
            appendLog("Wi-Fi Direct 已启用，可以开始发现设备");
        }
    }

    /**
     * 请求当前可用的对等设备列表
     */
    public void requestPeers() {
        if (wifiP2pManager == null || channel == null) return;

        try {
            wifiP2pManager.requestPeers(channel, peerList -> {
                List<WifiP2pDevice> devices = new ArrayList<>(peerList.getDeviceList());
                appendLog("发现 " + devices.size() + " 个设备");
                for (WifiP2pDevice d : devices) {
                    String name = d.deviceName;
                    if (name == null || name.isEmpty()) name = "未知";
                    appendLog("  - " + name + " [" + d.deviceAddress + "]");
                }
                adapter.updateDevices(devices);
            });
        } catch (SecurityException e) {
            appendLog("请求设备列表权限异常: " + e.getMessage());
        }
    }

    /**
     * 请求连接信息（连接成功后调用）
     */
    public void requestConnectionInfo() {
        if (wifiP2pManager == null || channel == null) return;

        wifiP2pManager.requestConnectionInfo(channel, info -> {
            if (info == null) return;

            String groupOwnerAddress = info.groupOwnerAddress != null
                    ? info.groupOwnerAddress.getHostAddress() : "未知";
            boolean isGroupOwner = info.isGroupOwner;

            String connectionText = String.format("连接状态: 已连接\nGroup Owner: %s\n本机角色: %s",
                    groupOwnerAddress,
                    isGroupOwner ? "Group Owner (GO)" : "Client");

            runOnUiThread(() -> {
                tvConnectionInfo.setText(connectionText);
                btnDisconnect.setEnabled(true);
            });

            appendLog("连接成功!");
            appendLog("  Group Owner 地址: " + groupOwnerAddress);
            appendLog("  本机是 Group Owner: " + (isGroupOwner ? "是" : "否"));
        });
    }

    /**
     * 断开连接回调
     */
    public void onDisconnected() {
        runOnUiThread(() -> {
            tvConnectionInfo.setText("连接状态: 未连接");
            btnDisconnect.setEnabled(false);
        });
        appendLog("已断开与对等设备的连接");
    }

    /**
     * 本机设备信息变化回调
     */
    public void onSelfDeviceChanged(WifiP2pDevice device) {
        String name = device.deviceName;
        if (name == null || name.isEmpty()) name = "未知";
        String text = "本机设备: " + name + " (" + device.deviceAddress + ")";
        runOnUiThread(() -> tvSelfDevice.setText(text));
    }

    // ==================== 辅助方法 ====================

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private String getFailureReason(int reason) {
        switch (reason) {
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "P2P 不受支持";
            case WifiP2pManager.ERROR:
                return "内部错误";
            case WifiP2pManager.BUSY:
                return "系统繁忙";
            default:
                return "未知错误 (" + reason + ")";
        }
    }

    /**
     * 向日志区域追加一条带时间戳的日志
     */
    private void appendLog(String message) {
        Log.d(TAG, message);
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logLine = "[" + timestamp + "] " + message + "\n";

        runOnUiThread(() -> {
            tvLog.append(logLine);
            svLog.post(() -> svLog.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }
}
