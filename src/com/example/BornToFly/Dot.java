package com.example.BornToFly;

import android.graphics.Paint;

public class Dot {

    double x, y, v;
    ObjectType type;
    int heightHalf, widthHalf;

    public Dot() {
        x = y = v = 0;
        type = ObjectType.MAN;
        heightHalf = widthHalf = 0;
    }

    public void setFields(int h, int w) {
        switch (type) {
            case MAN:
                heightHalf = h / 30;
                widthHalf = w / 25;
                break;
            case BOOSTER_ARROW:
                heightHalf = h / 30;
                widthHalf = w / 20;
                break;
            case SLOWER_SHORTS:
                heightHalf = h / 40;
                widthHalf = w / 20;
                break;
            case SLOWER_T_SHIRTS:
                heightHalf = h / 30;
                widthHalf = w / 15;
                break;
            case SLOWER_BALLOON:
                heightHalf = h / 25;
                widthHalf = w / 25;
                break;
            case BARRIER_CENTER_BALCONY:
                heightHalf = h / 20;
                widthHalf = w / 6;
                break;
            case BARRIER_LEFT_BALCONY:
                heightHalf = h / 30;
                widthHalf = w / 15;
                break;
            case BARRIER_RIGHT_BALCONY:
                heightHalf = h / 30;
                widthHalf = w / 15;
                break;
            case BARRIER_HELICOPTER:
                heightHalf = h / 10;
                widthHalf = w / 5;
                break;
        }
    }
}
