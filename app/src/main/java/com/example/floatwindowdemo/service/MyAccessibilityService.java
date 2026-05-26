package com.example.floatwindowdemo.service;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import java.util.List;

public class MyAccessibilityService extends AccessibilityService {
    // 单例
    private static MyAccessibilityService myAService;
    // 窗口
    private AccessibilityNodeInfo root;
    public MyAccessibilityService() {
        super();
        myAService = this;
    }

    // 搜索关键字
    private String keyword = null;
    // 被更新UI的View
    private TextView textView;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //只使用界面变化的监听，避免点击事件监听进入死循环
        if(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED != event.getEventType()){
            return;
        }

//        Log.d("变量", "keyword null: "+String.valueOf(keyword==null)+"-textview null:"+String.valueOf(textView==null));

        if(keyword!=null && textView!=null)
            getFullTextFromPart();

//        if(myAService!=null
//                && myAService.getRootInActiveWindow()!=null
//                && myAService.getRootInActiveWindow().getPackageName()!=null) {
//            Log.d("活动窗口包名", myAService.getRootInActiveWindow().getPackageName().toString());
//            if(myAService.root != null)
//                Log.d("静态变量包名", myAService.root.getPackageName().toString());
//            else
//                Log.d("静态变量包名", "null");
//        }

    }

    @Override
    public void onInterrupt() {
        myAService = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myAService = null;
    }

    public static boolean SetKeyword(String text){
        if(myAService != null) {
            myAService.keyword = text;
            getFullTextFromPart();
            return true;
        }else{
            return false;
        }
    }

    public static void SetTextView(TextView v){
        myAService.textView = v;
    }

    public static MyAccessibilityService getMyAccessibilityServiceInstance(){
        return myAService;
    }

    public static void getFullTextFromPart(){
        AccessibilityNodeInfo activeWindow = myAService.getRootInActiveWindow();
        if(activeWindow != null && !activeWindow.getPackageName().equals("com.example.floatwindowdemo")) {
            myAService.root = activeWindow;
        }

        myAService.traverseNodeText(myAService.root);
//        Log.d("遍历", "=== End ===");
    }

    private void traverseNodeText(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return;

        // 当前节点的文本内容
        CharSequence text = nodeInfo.getText();
        if (myAService.keyword!=null && !TextUtils.isEmpty(text) && text.toString().contains(myAService.keyword)) {
//            Log.d("MyAccessibilityService", "取得完整文本: " + text);
            myAService.textView.setText(text);
            return;
        }

        // 遍历子节点
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
            if (childNodeInfo != null) {
                traverseNodeText(childNodeInfo);
            }
        }
    }

    public static void getRectanglesZoneContains(Rect rectZoneOnScreen, List<Rect> rectListToDraw, List<Rect> rectListToHint, StringBuilder resText){
        if(myAService == null) {
            resText.append("服务未运行，请检查无障碍权限是否开启");
            return;
        }

        myAService.root = myAService.getRootInActiveWindow();
        rectListToDraw.clear();

        //修正区域
        int offset = 30;
        rectZoneOnScreen.left -= offset;
        rectZoneOnScreen.top -= offset;
        rectZoneOnScreen.right += offset;
        rectZoneOnScreen.bottom += offset;

        if(myAService.root != null) {
            //遍历活动窗口所有节点
            Log.d("当前root", myAService.root.getPackageName().toString());
            Log.d("搜索区域", rectZoneOnScreen.toString());
            myAService.traverseNodeRect(myAService.root, rectZoneOnScreen, rectListToDraw, rectListToHint, resText);

            Log.d("遍历结束", "=== End ===");
        }
    }

    private void traverseNodeRect(AccessibilityNodeInfo nodeInfo, Rect selectZoneOnScreen, List<Rect> rectListToDraw, List<Rect> rectListToHint, StringBuilder resText) {
        if (nodeInfo == null) return;

        // 当前节点的屏幕空间矩形
        Rect nodeRect = new Rect();
        nodeInfo.getBoundsInScreen(nodeRect);

        // 比对是否包括在选区内
        // Log.d("比对", selectZoneOnScreen.toString() + "中寻找" + nodeRect.toString() + nodeInfo.getText() + selectZoneOnScreen.contains(nodeRect) + Rect.intersects(selectZoneOnScreen, nodeRect));
        if(selectZoneOnScreen.contains(nodeRect)){
            CharSequence text = nodeInfo.getText();
            if(text!=null && !text.toString().isEmpty()) {
                Log.d("拾取选区内文本",text.toString());
                rectListToDraw.add(nodeRect);
                if(resText.length()!=0) resText.append('\n');
                resText.append(text);
            }
        }else if(Rect.intersects(selectZoneOnScreen, nodeRect)){//交叉
            CharSequence text = nodeInfo.getText();
            if(text!=null && !text.toString().isEmpty()) {
                rectListToHint.add(nodeRect);
            }
        }

        // 遍历子节点
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
            if (childNodeInfo != null) {
                traverseNodeRect(childNodeInfo, selectZoneOnScreen, rectListToDraw, rectListToHint,resText);
            }
        }
    }

}
