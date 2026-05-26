package com.example.floatwindowdemo.service;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.PowerManager;
import android.provider.Settings;
import android.security.keystore.UserNotAuthenticatedException;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import com.example.floatwindowdemo.R;
import com.example.floatwindowdemo.util.AdbCommandHelper;

import rikka.shizuku.Shizuku;

/*
ADB授权
adb shell "pm grant com.example.floatwindowdemo android.permission.WRITE_SECURE_SETTINGS; appops set com.example.floatwindowdemo ACCESS_RESTRICTED_SETTINGS allow"
ADB夺权
adb shell "pm revoke com.example.floatwindowdemo android.permission.WRITE_SECURE_SETTINGS; appops set com.example.floatwindowdemo ACCESS_RESTRICTED_SETTINGS deny"
 */
public class MyQSTileService extends TileService {

    Tile qsTile;

    AdbCommandHelper commandHelper;

    @Override
    public void onStartListening() {
        super.onStartListening();
        qsTile = getQsTile();
        qsTile.setState(FloatingWidgetService.isActive?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);
        qsTile.setIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground));
        qsTile.setLabel("AI助手");
        qsTile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (qsTile.getState() == Tile.STATE_ACTIVE) {
            if (DisableAccessibleService()) {
                stopService(new Intent(this, FloatingWidgetService.class));
                qsTile.setState(Tile.STATE_INACTIVE); // 快捷开关
            } else {
                qsTile.setLabel("权限不足");
            }
        } else if (qsTile.getState() == Tile.STATE_INACTIVE) {
            if(Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    qsTile.setIcon(Icon.createWithResource(this, R.drawable.ic_wait));
                    int state = qsTile.getState();
                    qsTile.setState(Tile.STATE_UNAVAILABLE);
                    qsTile.setLabel("请稍等");
                    qsTile.updateTile();
                    qsTile.setState(state);
                    if (CheckPermission()) {//检查权限
                        // 启动
                        startService(new Intent(this, FloatingWidgetService.class));
                        qsTile.setLabel(getString(R.string.app_name));
                        qsTile.setIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground));
                        qsTile.setState(Tile.STATE_ACTIVE); // 快捷开关
                    } else {
                        qsTile.setIcon(Icon.createWithResource(this, R.drawable.ic_refresh));
                        qsTile.setLabel("请重试");
                    }
                } else {
                    startService(new Intent(this, FloatingWidgetService.class));
                    qsTile.setIcon(Icon.createWithResource(this, R.drawable.ic_error));
                    qsTile.setLabel("权限不足");
                }
            }else {
                startService(new Intent(this, FloatingWidgetService.class));
                qsTile.setIcon(Icon.createWithResource(this, R.drawable.ic_error));
                qsTile.setLabel("未安装");
            }
        }
        qsTile.updateTile();
    }

    private boolean CheckPermission() {
        //尝试授权
        if (!Settings.canDrawOverlays(this)) { // 授权悬浮窗
            if(InitializeAdbCommandHelper())
                commandHelper.TryExecuteCommand("appops set " + this.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
            else
                return false;
        }
        if (!canWriteSecureSettings()) { // 写入安全设置
            if(InitializeAdbCommandHelper())
                commandHelper.TryExecuteCommand("pm grant " + this.getPackageName() + " android.permission.WRITE_SECURE_SETTINGS");
            else
                return false;
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager.isIgnoringBatteryOptimizations(this.getPackageName())) { // 写入电池优化
            if(InitializeAdbCommandHelper())
                commandHelper.TryExecuteCommand("dumpsys deviceidle whitelist +" + this.getPackageName());
            else
                return false;
        }
        if (MyAccessibilityService.getMyAccessibilityServiceInstance() == null) { // 启动无障碍
            if(!EnableAccessibleService()) {
                commandHelper.TryExecuteCommand("pm grant " + this.getPackageName() + " android.permission.WRITE_SECURE_SETTINGS");
            }
        }
        //检查权限
        boolean granted1 = Settings.canDrawOverlays(this);
        boolean granted2 = canWriteSecureSettings();
        boolean granted3 = powerManager.isIgnoringBatteryOptimizations(this.getPackageName());
        boolean granted4 = MyAccessibilityService.getMyAccessibilityServiceInstance() == null;
        Log.d("TAG", "CheckPermission: " + granted1 + granted2 + granted3 + granted4);
        return granted1;
    }

    private boolean InitializeAdbCommandHelper() {
        try {
            if (commandHelper == null) {
                commandHelper = new AdbCommandHelper(this);
                Log.d("TAG", "InitializeAdbCommandHelper: 实例化helper");
                return false;
            } else {
                Log.d("TAG", "InitializeAdbCommandHelper: helper已存在");
                return true;
            }
        } catch (UserNotAuthenticatedException e){
            return false;
        }
    }

    private boolean DisableAccessibleService(){
        try {
            // 无障碍服务名
            String serviceName = getPackageName() + "/com.example.floatwindowdemo.service.MyAccessibilityService";
            // 读取当前已启用的服务列表
            String enabledServices = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            // 避免重复添加，检查是否已不包含
            if (enabledServices != null && enabledServices.contains(serviceName)) {
                // 分割为数组
                String[] services = enabledServices.split(":");
                StringBuilder newServices = new StringBuilder();

                // 重建列表，排除当前服务
                for (String service : services) {
                    if (!service.equals(serviceName)) {
                        if (newServices.length() > 0) {
                            newServices.append(":");
                        }
                        newServices.append(service);
                    }
                }
                // 写回设置
                Settings.Secure.putString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        newServices.toString()
                );
            }
            return true;
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean EnableAccessibleService(){
        try {
            // 开启全局无障碍总开关
            String name = Settings.Secure.ACCESSIBILITY_ENABLED;
            Settings.Secure.putInt(getContentResolver(),name,1);
            // 无障碍服务名
            String serviceName = getPackageName() + "/com.example.floatwindowdemo.service.MyAccessibilityService";
            // 读取当前已启用的服务列表
            String enabledServices = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            // 避免重复添加，检查是否已包含
            if (enabledServices == null || !enabledServices.contains(serviceName)) {
                // 追加服务到现有列表（不同服务用冒号分隔）
                String newEnabledServices = enabledServices == null
                        ? serviceName
                        : enabledServices + ":" + serviceName;

                // 写入设置
                Settings.Secure.putString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        newEnabledServices
                );
            }
            return true;
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean canWriteSecureSettings() {
        try {
            // 尝试实际操作验证权限
            Settings.Global.getString(getContentResolver(), "adb_enabled");
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
