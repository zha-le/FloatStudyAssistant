package com.example.floatwindowdemo.CustomView;

import static com.example.floatwindowdemo.service.MyAccessibilityService.getRectanglesZoneContains;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.List;

public class RectangleDrawableConstraintLayout extends ConstraintLayout{

    private final Paint redPaint, yellowPaint, bluePaint, blueLinePaint, whitePaint;
    private final ArrayList<Rect> rectanglesToDraw = new ArrayList<>();
    private final ArrayList<Rect> rectanglesToHint = new ArrayList<>();

    //缩放手势
    private int screenHeight,screenWidth;
    private float initialWidth, initialHeight;
    private float initialDistanceX, initialDistanceY;
    private boolean isResizing = false;
    public boolean isResizingDown = false;//双指缩放完但两根手指未都抬起
    public boolean blueLine = false;

    public RectangleDrawableConstraintLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //设置画笔
        redPaint = new Paint();
        redPaint.setColor(Color.RED);
        redPaint.setStyle(Paint.Style.FILL);
        redPaint.setAlpha(63);
        yellowPaint = new Paint();
        yellowPaint.setColor(Color.YELLOW);
        yellowPaint.setStyle(Paint.Style.FILL);
        yellowPaint.setAlpha(31);
        bluePaint = new Paint();
        bluePaint.setColor(Color.BLUE);
        bluePaint.setStyle(Paint.Style.STROKE);
        bluePaint.setStrokeWidth(1);
        bluePaint.setAlpha(63);
        whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStrokeWidth(30);//线宽
        blueLinePaint = new Paint();
        blueLinePaint.setColor(Color.BLUE);
        blueLinePaint.setStrokeWidth(30);//线宽
        // 获取屏幕尺寸
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        //属于
        for (Rect r: rectanglesToDraw) {
            canvas.drawRect(r, redPaint);
            canvas.drawRect(r, bluePaint);//边界
        }
        //交集
        for (Rect r: rectanglesToHint) {
            canvas.drawRect(r, yellowPaint);
            canvas.drawRect(r, bluePaint);//边界
        }
        //四角指示图标
        int lineLength = 100;
        Paint linePaint = blueLine ? blueLinePaint : whitePaint;
        canvas.drawLine(0,0,lineLength,0,linePaint);//左上
        canvas.drawLine(0,0,0,lineLength,linePaint);
        canvas.drawLine(0,getHeight(),lineLength,getHeight(),linePaint);//左下
        canvas.drawLine(0,getHeight(),0,getHeight() - lineLength,linePaint);
        canvas.drawLine(getWidth() - lineLength, 0, getWidth(), 0, linePaint); //右上
        canvas.drawLine(getWidth(), 0, getWidth(), lineLength, linePaint);
        canvas.drawLine(getWidth() - lineLength, getHeight(), getWidth(), getHeight(), linePaint); //右下
        canvas.drawLine(getWidth(), getHeight() - lineLength, getWidth(), getHeight(), linePaint);
    }

    public StringBuilder UpdateRectangleDraw(){
        //遍历选区内的节点
        int[] point = new int[2];
        this.getLocationOnScreen(point);
        int zoneLeft = point[0];
        int zoneTop = point[1];
        int zoneRight = zoneLeft + getWidth();
        int zoneBottom = zoneTop + getHeight();
        Rect rectZone = new Rect(zoneLeft,zoneTop,zoneRight,zoneBottom);
        StringBuilder sb = new StringBuilder();
        getRectanglesZoneContains(rectZone, rectanglesToDraw, rectanglesToHint,sb);

        //坐标转换
        ScreenToViewCoordinates(this,rectanglesToDraw);
        ScreenToViewCoordinates(this,rectanglesToHint);

        //重绘
        invalidate();

        return sb;
    }

    public void ClearRectangleDraw(){
        rectanglesToDraw.clear();
        rectanglesToHint.clear();
        invalidate();
    }

    //屏幕坐标转换为控件坐标
    private void ScreenToViewCoordinates(ViewGroup targetView, List<Rect> rectList){
        int[] location = new int[2];
        targetView.getLocationOnScreen(location);
        for (Rect r: rectList) {
            //targetView.offsetDescendantRectToMyCoords(targetView, r);
            r.left -= location[0];
            r.top -= location[1];
            r.right -= location[0];
            r.bottom -= location[1];
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2) { // Check for two-finger touch
            if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                // Start resizing when the second finger touches down
                initialWidth = getWidth();
                initialHeight = getHeight();
                initialDistanceX = getDistance(event, 0, 1).x;
                initialDistanceY = getDistance(event, 0, 1).y;
                isResizing = true;
                blueLine = true;
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && isResizing) {
                // Adjust width and height based on finger movement
                float newDistanceX = getDistance(event, 0, 1).x;
                float newDistanceY = getDistance(event, 0, 1).y;

                // Calculate new dimensions
                int newWidth = (int) (initialWidth + (newDistanceX - initialDistanceX));
                int newHeight = (int) (initialHeight + (newDistanceY - initialDistanceY));

                // Apply new dimensions
                ViewGroup.LayoutParams params = getLayoutParams();
                params.width = Math.min(Math.max(newWidth, 300),screenWidth);
                params.height = Math.min(Math.max(newHeight, 150),screenHeight);
                setLayoutParams(params);

                // Optionally invalidate to redraw the view with new dimensions
                // invalidate();
            } else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
                // Stop resizing when one of the fingers is lifted
                isResizing = false;
                isResizingDown = true;
                blueLine = false;
                invalidate();
            }
        }
        return true; // Consume the touch event
    }

    private PointF getDistance(MotionEvent event, int pointerIndex1, int pointerIndex2) {
        float x = Math.abs(event.getX(pointerIndex1) - event.getX(pointerIndex2));
        float y = Math.abs(event.getY(pointerIndex1) - event.getY(pointerIndex2));
        return new PointF(x, y);
    }
}
