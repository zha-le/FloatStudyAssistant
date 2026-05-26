package com.example.floatwindowdemo.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.floatwindowdemo.ExecResult;
import com.example.floatwindowdemo.IUserService;

import java.util.concurrent.CountDownLatch;

import rikka.shizuku.Shizuku;

public class AdbCommandHelper {

    private ShizukuServiceManager serviceManager;
    private IUserService userService;
    private Context context;
    private ExecResult result;

    public AdbCommandHelper(Context context) throws UserNotAuthenticatedException {
        if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                this.serviceManager = new ShizukuServiceManager();
            } else {
                Log.d("TAG", "SecureCounter: Shizuku未授权");
                throw new UserNotAuthenticatedException();
            }
            this.context = context;
            bindUserService(context);
            // 等待连接成功
            try {
                Thread.sleep(3000); // 等待与Shizuku的连接完成
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            Log.d("TAG", "SecureCounter: Shizuku未启动");
            throw new UserNotAuthenticatedException();
        }
    }

    public int GetOuterSecureWindowCount() throws RemoteException {
        // if (userService == null) bindUserService(context);
        result = userService.exec("dumpsys window windows");
        return StringCounter.countLinesStartingAndContaining(result.stdout);
    }

    public ExecResult TryExecuteCommand(String cmd){
        try {
            if (userService == null) bindUserService(context);
            return userService.exec(cmd);
        }catch (RemoteException e){
            e.printStackTrace();
        }
        return null;
    }

    public void bindUserService(Context context){ // 绑定时需要传入context获取包名
        serviceManager.bind(context, new ShizukuServiceManager.OnBindResultCallback() {
            @Override
            public void onResult(boolean success, @Nullable String errorMessage, @Nullable IUserService service) {
                if (success) {
                    userService = service;
                    Log.d("TAG", "Shizuku用户服务绑定成功");
                } else {
                    Log.d("TAG", "Shizuku用户服务绑定失败");
                }
            }
        });
    }
}
