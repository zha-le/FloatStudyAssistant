package com.example.floatwindowdemo.CustomView;

public class RectangleInfo {
    private int[] location;
    public int width,height;

    public RectangleInfo(int x, int y, int width, int height) {
        this.location = new int[]{x,y};
        this.width = width;
        this.height = height;
    }

    public RectangleInfo(int[] location, int width, int height) {
        this.location = location;
        this.width = width;
        this.height = height;
    }

    public int[] getLocation(){
        return location;
    }

    public void setX(int x){
        location[0] = x;
    }

    public void setY(int y){
        location[1] = y;
    }

    public int getX(){
        return location[0];
    }

    public int getY(){
        return location[1];
    }
}
