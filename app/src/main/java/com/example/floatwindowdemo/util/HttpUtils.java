package com.example.floatwindowdemo.util;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.floatwindowdemo.service.FloatingWidgetService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class HttpUtils {

    // 构建RequestBody
    private static String JsonString(String modelName, String userInput, boolean isStream, String systemPrompt, boolean enableThinking) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("model", modelName);
        json.put("stream", isStream);
        if (enableThinking)
            json.put("enable_thinking", true);

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", systemPrompt));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userInput));

        json.put("messages", messages);
        return json.toString();
    }

    // 发起HTTP请求
    public static String sendPostRequest(String url, String modelName, String userInput, String apiKey, String systemPrompt, Boolean thinking) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();
            // 设置请求方法为POST
            conn.setRequestMethod("POST");
            // 设置请求的头部信息
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (apiKey != null) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            // 获取OutputStream，将请求的数据写入流中
            OutputStream os = conn.getOutputStream();
            os.write(JsonString(modelName, userInput, false, systemPrompt, thinking).getBytes());
            os.flush();
            // 获取服务器的响应结果
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                Log.d("HttpUtils", "Return Text: " + response);
                return response.toString();
            } else {
                // 创建StringBuilder对象用于构建错误消息
                StringBuilder errorResponse = new StringBuilder();
                try {
                    // 尝试从错误流中读取数据
                    if (conn.getErrorStream() != null) {
                        reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        reader.close();
                    } else {
                        errorResponse.append("No error stream available");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("HttpUtils", "Error reading error stream");
                }
                return "request error with statue code: " + responseCode + ". Server response: " + errorResponse.toString();
            }

        } catch (ProtocolException e) {
            e.printStackTrace();
            Log.e("HttpUtils", "URL Format Error");
            return null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.e("HttpUtils", "URL Format Error");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("HttpUtils", "IOException Error");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("HttpUtils", "JSONException Error");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return null;
    }

    // 发起HTTP请求并解析SSE
    public static void sendPostRequestStream(String url, String modelName, String userInput, String apiKey, String systemPrompt, Handler handler, TextView outputTextView, TextView thinkTextView, TextView errorTextView, FloatingWidgetService service, ScrollView scrollView, Boolean thinking) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        boolean isReasoning = false;
        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();
            // 设置请求方法为POST
            conn.setRequestMethod("POST");
            // 设置请求的头部信息
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (apiKey != null) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            // 获取OutputStream，将请求的数据写入流中
            OutputStream os = conn.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            osw.write(JsonString(modelName, userInput, true, systemPrompt, thinking));
            osw.flush();
            // 获取服务器的响应结果
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // HTTP状态码200
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    //解析JSON
                    if (line.length() > 6 && line.startsWith("data:")){
                        ParseSSE(line, handler, outputTextView, thinkTextView, errorTextView, scrollView);
                    }
                }
                reader.close();
            } else { // HTTP状态码不为200
                // 创建StringBuilder对象用于构建错误消息
                StringBuilder errorResponse = new StringBuilder();
                errorResponse.append("HTTP error with status code " + responseCode + ". Detail:\n");
                // 尝试从错误流中读取数据
                if (conn.getErrorStream() != null) {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    reader.close();
                } else {
                    errorResponse.append("No error stream available");
                }
                handler.post(() -> {
                    // 更新UI
                    ApplyTokenOnUI(errorResponse, handler, errorTextView, scrollView);
                });
            }
        } catch (ProtocolException e) {
            e.printStackTrace();
            Log.e("HttpUtils", "URL Format Error");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.e("HttpUtils", "URL Format Error");
        } catch (UnknownHostException e){ // 继承自IOException,需在其之前进行捕获
            e.printStackTrace();
            ApplyTokenOnUI("无法解析服务器域名，请检查网络设置", handler, errorTextView, scrollView);
            Log.e("HttpUtils", "Unknown Host Exception");
        } catch (InterruptedIOException e) {
            ApplyTokenOnUI("请求被中断，这条消息已停止", handler, errorTextView, scrollView);
            Log.e("HttpUtils", "Thread Interrupted");
        } catch (SocketException e){
            ApplyTokenOnUI("访问连接失败，请检查网络设置", handler, errorTextView, scrollView);
            Log.e("HttpUtils", "SocketException Error");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("HttpUtils", "IOException Error");
        } catch (JSONException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            // 启用发送按钮
            handler.post(() -> {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                service.SetSubmitBtnStatus(true);
            });
        }
    }

    // 解析SSE的单条数据
    private static void ParseSSE(String lineChunk, Handler handler, TextView output, TextView think, TextView err, ScrollView scrollView){
        String lineData = lineChunk.substring(6);//裁剪data:前缀
        String token = "";
        try {
            //解析数据
            JSONObject jsonObject = new JSONObject(lineData);
            // 正常输出，解析增量token
            if (jsonObject.has("choices")) {
                JSONArray choicesArray = jsonObject.getJSONArray("choices");
                JSONObject firstChoice = choicesArray.getJSONObject(0);
                JSONObject deltaObject = firstChoice.getJSONObject("delta");
                if (deltaObject.has("content") && !deltaObject.isNull("content")) {
                    //读取回答内容
                    token = deltaObject.getString("content");
                    ApplyTokenOnUI(token, handler, output, scrollView);
                }
                else if (deltaObject.has("reasoning_content") && !deltaObject.isNull("reasoning_content")) {
                    token = deltaObject.getString("reasoning_content");
                    ApplyTokenOnUI(token, handler, think, scrollView);
                }
            } else if (jsonObject.has("error")) {
                // 错误输出
                String errorCode = jsonObject.getJSONObject("error").getString("code");
                if (errorCode.equals("data_inspection_failed")){ // 内容审查错误
                    token = "你好，这个问题我暂时无法回答，让我们换个话题再聊聊吧";
                } else { // 其他错误
                    token = jsonObject.toString();
                }
                ApplyTokenOnUI(token, handler, err, scrollView);
            }
        } catch (JSONException e) {
            if(!lineData.equals("[DONE]")){ //[DONE]不处理
                e.printStackTrace();
                ApplyTokenOnUI("[Parse Json Failed]\n" + e.getMessage() , handler, err, scrollView);
                ApplyTokenOnUI("\n[Json Data]\n" + lineData, handler, err, scrollView);
            }
        }
    }

    // 将Token更新到UI
    private static void ApplyTokenOnUI(CharSequence token, Handler handler, TextView v, ScrollView scrollView){
        handler.post(()-> {
            // 显示控件
            if (v.getVisibility() == View.GONE)
                v.setVisibility(View.VISIBLE);
            // 添加文本
            v.append(token);
            // 滚动到页面底部
            if (scrollView != null)
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }
}
