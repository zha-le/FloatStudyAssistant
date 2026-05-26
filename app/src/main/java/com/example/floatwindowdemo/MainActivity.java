package com.example.floatwindowdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.floatwindowdemo.service.FloatingWidgetService;
import com.example.floatwindowdemo.service.MyAccessibilityService;
import com.example.floatwindowdemo.util.ShizukuServiceManager;
import com.example.floatwindowdemo.util.StringCounter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String[] ALLOW_DEVICE_NAME = { // 开发设备白名单
            "Redmi K70 Pro",
            "Redmi K60 Pro",
            "FOA-AL00"
    };

    private static final int REQUEST_CODE = 1;
    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;
    private ShizukuServiceManager serviceManager;
    private IUserService userService = null;
    private Handler mainHandler;

    //标签控件
    TextView text_statue;
    //设置项
    SharedPreferences sharedPreferences;
    //初始化Launcher
    private final ActivityResultLauncher<Intent> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                updatePermissionStatusTextView();
            });
    //电池优化Launcher回调
    private final ActivityResultLauncher<Intent> batteryOptimizationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {// 这里是回调
                // 启动第二个电池优化设置页
                requestPermissionLauncher.launch(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                Toast.makeText(this, "请把后台使用情况改为无限制", Toast.LENGTH_SHORT).show();
            }
        );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 检查设备名
        String deviceName = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME);
        if (!checkDeviceList(ALLOW_DEVICE_NAME, deviceName)){
            Toast.makeText(this, "Access Denied", Toast.LENGTH_SHORT).show();
            finish(); // 关闭应用
            return;
        }

        // 检查时间炸弹
        if (isAfterDeadline()) {
            // 显示提示信息并终止应用
            Toast.makeText(this, "此应用已过期，无法使用。", Toast.LENGTH_LONG).show();
            finish(); // 关闭应用
            return;
        }
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // 显示免责声明
        showDisclaimerDialog();
        //更新设置项
        sharedPreferences = getSharedPreferences("PrefsConfig",MODE_PRIVATE);
        boolean forbidCapture = sharedPreferences.getBoolean("forbid_capture", true);
        boolean edgeAttach = sharedPreferences.getBoolean("edge_attach",true);
        ((ToggleButton)findViewById(R.id.forbid_capture)).setChecked(forbidCapture);
        ((ToggleButton)findViewById(R.id.edge_attach)).setChecked(edgeAttach);
        //事件绑定
        findViewById(R.id.btn_launcher).setOnClickListener(this);
        findViewById(R.id.btn_shizuku_launcher).setOnClickListener(this);
        ((ToggleButton)findViewById(R.id.forbid_capture)).setOnCheckedChangeListener(this);
        ((ToggleButton)findViewById(R.id.edge_attach)).setOnCheckedChangeListener(this);
        //获得标签
        text_statue = findViewById(R.id.text_statue);
        //显示授权状态
        updatePermissionStatusTextView();
        //当前界面不允许截屏
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void updatePermissionStatusTextView(){
        // 检查是否获得了权限
        if (Settings.canDrawOverlays(this)) {
            // 权限已授予，可以继续操作
            // 检查无障碍权限
            if(MyAccessibilityService.getMyAccessibilityServiceInstance() != null){
                //检查电池优化
                if(checkIgnoreBatteryOptimization(this)){
                    text_statue.setText("悬浮窗、无障碍、电池优化已授权");
                }else{
                    text_statue.setText("悬浮窗、无障碍已授权，请继续授权电池优化");
                }
            }else{
                text_statue.setText("悬浮窗已授权，请继续授权无障碍");
            }
        } else {
            // 用户拒绝了权限
            text_statue.setText("悬浮窗未授权");
        }
    }

    private void showDisclaimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("免责声明");
        builder.setMessage("本软件仅用于测试与学习，禁止用于考试作弊。");
        builder.setPositiveButton("我已知晓", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // 继续执行其他初始化操作
            }
        });
        builder.setCancelable(false); // 禁止用户取消对话框
        builder.show();
    }

    private boolean checkDeviceList(String[] array, String target) {
        for (String str : array) {
            if (target.equals(str)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAfterDeadline() {
        // 设置截止时间
        String deadlineString = "2025-12-28 21:00";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        try {
            Date deadline = sdf.parse(deadlineString);
            Date currentTime = new Date();

            // 检查当前时间是否在截止时间之后
            return currentTime.after(deadline);
        } catch (ParseException e) {
            e.printStackTrace();
            // 如果解析失败，默认返回false以防止误关闭应用
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_launcher){ // 按下开始按钮
            if (!Settings.canDrawOverlays(this)) {// 未授权权限
                Toast.makeText(this,"请授权悬浮窗权限",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                requestPermissionLauncher.launch(intent);
            } else if (MyAccessibilityService.getMyAccessibilityServiceInstance() == null) {// 未授权无障碍
                Toast.makeText(this, "请授权无障碍权限", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                requestPermissionLauncher.launch(intent);
            } else if (!checkIgnoreBatteryOptimization(this)){// 未授权电池优化
                Toast.makeText(this, "请把省电策略改为无限制", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:"+ getPackageName()));
                //startActivity(intent);
                batteryOptimizationLauncher.launch(intent);
            } else {
                Toast.makeText(this, "启动悬浮窗", Toast.LENGTH_SHORT).show();
                startService(new Intent(this, FloatingWidgetService.class));
            }
        }else if (v.getId() == R.id.btn_shizuku_launcher) { // 通过shizuku启动
            if(CheckShizukuPermission(REQUEST_CODE)){
                try {
                    // 绑定
                    if(userService == null) {
                        bindUserService();
                    }else{
                        executeCommand("pm grant " + getPackageName() + " android.permission.WRITE_SECURE_SETTINGS");
                        executeCommand("appops set " + getPackageName() + " ACCESS_RESTRICTED_SETTINGS allow");
                        executeCommand("dumpsys window windows");
                    }
                }catch (Exception e){
                    text_statue.setText("执行错误");
                    e.printStackTrace();
                }
            }else {
                text_statue.setText("Shizuku启动失败，请检查授权情况");
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (buttonView.getId() == R.id.forbid_capture) {
            editor.putBoolean("forbid_capture",isChecked);
//            if (isChecked) {
//                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
//            }else {
//                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
//            }
            Toast.makeText(this, "设置项重启后生效", Toast.LENGTH_SHORT).show();
        }else if(buttonView.getId() == R.id.edge_attach){ // 小白球吸边
            editor.putBoolean("edge_attach",isChecked);
        }

        editor.apply();
    }

    public boolean checkIgnoreBatteryOptimization(Activity activity) {

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        boolean hasIgnored = false;
        hasIgnored = powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());

        return hasIgnored;
    }

    // ============================================
    // Shizuku
    // ============================================

    /** 在后台线程执行命令 */
    private void executeCommand(String command) {
        //new Thread(() -> {
            Log.d("TAG", "executeCommand: " + command);
            try {
                ExecResult result = userService.exec(command);
                int count = StringCounter.countLinesStartingAndContaining(result.stdout);
                mainHandler.post(() -> {
                    text_statue.setText("隐私窗口个数为"+count);
                });
            } catch (RemoteException e){
                text_statue.setText("执行错误");
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        //});
    }

    private void bindUserService(){
        serviceManager = new ShizukuServiceManager();
        serviceManager.bind(this, new ShizukuServiceManager.OnBindResultCallback() {
            @Override
            public void onResult(boolean success, @Nullable String errorMessage, @Nullable IUserService service) {
                mainHandler.post(()->{
                    if (success) {
                        userService = service;
                        text_statue.setText("服务绑定成功");
                    } else {
                        text_statue.setText("服务绑定失败");
                    }
                });
            }
        });
    }

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
        // 回调
        if (requestCode == REQUEST_CODE) {
            text_statue.setText("授权成功");
        }
    }

    private boolean CheckShizukuPermission(int code){
        // 检查Shizuku安装
        if(!Shizuku.pingBinder()){
            Toast.makeText(this, "Shizuku未安装", Toast.LENGTH_SHORT).show();
            return false;
        } else if (Shizuku.isPreV11()) {
            Toast.makeText(this, "请更新Shizuku", Toast.LENGTH_SHORT).show();
            return false;
        }
        // 检查Shizuku授权
        if(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED){
            return true; //授权成功
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(this, "权限被拒绝且不再询问", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            // 请求权限
            Shizuku.requestPermission(code);
            return false;
        }
    }
}