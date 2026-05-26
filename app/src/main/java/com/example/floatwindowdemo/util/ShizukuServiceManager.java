package com.example.floatwindowdemo.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.floatwindowdemo.IUserService;
import com.example.floatwindowdemo.service.UserService;

import rikka.shizuku.Shizuku;

/**
 * Shizuku 用户服务管理器
 * 用于绑定/解绑通过 Shizuku 运行的特权服务
 */
public class ShizukuServiceManager {
    private static final String TAG = "ShizukuServiceManager";

    @Nullable
    private IUserService userService = null;

    @Nullable
    private ServiceConnection serviceConnection = null;

    /**
     * 绑定 Shizuku 用户服务
     */
    public void bind(Context context, OnBindResultCallback callback) {
        // 如果服务已连接，直接返回现有实例
        if (userService != null) {
            Log.d(TAG, "Shizuku service 已经连接");
            callback.onResult(true, null, userService);
            return;
        }

        try {
            // 构建服务参数
            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                    new ComponentName(context, UserService.class.getName())
            )
                    .daemon(true)
                    .processNameSuffix(context.getPackageName() + ".shizuku.service")
                    .debuggable(true);

            // 创建服务连接回调
            serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    userService = IUserService.Stub.asInterface(service);
                    Log.d(TAG, "Shizuku service 成功连接: " + name.getClassName());
                    callback.onResult(true, null, userService);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    userService = null;
                    Log.d(TAG, "Shizuku service 断开: " + name.getClassName());
                }
            };

            // 执行绑定
            Shizuku.bindUserService(args, serviceConnection);
        } catch (Exception e) {
            Log.e(TAG, "绑定失败", e);
            callback.onResult(false, "绑定异常: " + e.getMessage(), null);
        }
    }

    /**
     * 解绑 Shizuku 用户服务
     */
    public void unbind(Context context) {
        try {
            if (serviceConnection != null) {
                Shizuku.unbindUserService(
                        new Shizuku.UserServiceArgs(new ComponentName(context, UserService.class.getName())),
                        serviceConnection,
                        true
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "解绑出错", e);
        } finally {
            // 清理状态
            userService = null;
            serviceConnection = null;
            Log.d(TAG, "Shizuku service 解绑");
        }
    }

    /**
     * 绑定结果回调接口
     */
    public interface OnBindResultCallback {
        void onResult(
                boolean success,
                @Nullable String errorMessage,
                @Nullable IUserService userService
        );
    }
}
