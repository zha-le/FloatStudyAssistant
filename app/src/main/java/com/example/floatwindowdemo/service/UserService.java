package com.example.floatwindowdemo.service;

import android.util.Log;

import com.example.floatwindowdemo.ExecResult;
import com.example.floatwindowdemo.IUserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserService extends IUserService.Stub {

    public UserService(){
        Log.i("UserService", "constructor");
    }

    @Override
    public void destroy() {
        Log.i("UserService", "destroy");
        System.exit(0);
    }

    @Override
    public void exit() {
        destroy();
    }

    @Override
    public ExecResult exec(String command) {
        Process process = null;
        StringBuilder stdoutSB = new StringBuilder();
        StringBuilder stderrSB = new StringBuilder();

        ExecResult result = new ExecResult();

        // 检查命令是否为空或仅包含空白字符
        if (command == null || command.trim().isEmpty()) {
            result.stdout = "";
            result.stderr = "Error: Empty command";
            result.exitCode = -1;
            return result;
        }

        try {
            // 分割命令（按一个或多个空白字符）
            String[] cmdArray = command.trim().split("\\s+");
            process = Runtime.getRuntime().exec(cmdArray);

            // 读取标准输出
            try (BufferedReader stdoutReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = stdoutReader.readLine()) != null) {
                    stdoutSB.append(line).append('\n');
                }
            }

            // 读取标准错误
            try (BufferedReader stderrReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = stderrReader.readLine()) != null) {
                    stderrSB.append(line).append('\n');
                }
            }

            // 等待进程结束并获取退出码
            result.exitCode = process.waitFor();
            result.stdout = stdoutSB.toString();
            result.stderr = stderrSB.toString();

        } catch (SecurityException e) {
            result.stdout = "";
            result.stderr = "Permission denied: " + e.getMessage();
            result.exitCode = -2;
        } catch (IOException e) {
            result.stdout = "";
            result.stderr = "IO error: " + e.getMessage();
            result.exitCode = -3;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.stdout = "";
            result.stderr = "Execution interrupted: " + e.getMessage();
            result.exitCode = -4;
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
        return result;
    }
}
