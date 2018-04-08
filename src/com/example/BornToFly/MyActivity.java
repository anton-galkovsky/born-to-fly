package com.example.BornToFly;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;

public class MyActivity extends Activity implements View.OnTouchListener, View.OnClickListener {

    private MyView myView;

    @Override
    public void onPause() {
        super.onPause();
        myView.stopGame();
    }

    @Override
    public void onResume() {
        super.onResume();
        setContentView(R.layout.menu);
        findViewById(R.id.imageView).setOnClickListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getPointerCount() == 1) {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                myView.touchAction(event.getX(), true);
            if (event.getAction() == MotionEvent.ACTION_UP)
                myView.touchAction(event.getX(), false);
        } else if (event.getPointerCount() == 2) {
            MotionEvent.PointerCoords point1 = new MotionEvent.PointerCoords();
            MotionEvent.PointerCoords point2 = new MotionEvent.PointerCoords();
            event.getPointerCoords(0, point1);
            event.getPointerCoords(1, point2);
            if (event.getAction() == MotionEvent.ACTION_POINTER_1_DOWN || event.getAction() == MotionEvent.ACTION_POINTER_2_UP)
                myView.touchAction(event.getX(0), true);
            if (event.getAction() == MotionEvent.ACTION_POINTER_1_UP || event.getAction() == MotionEvent.ACTION_POINTER_2_DOWN)
                myView.touchAction(event.getX(1), true);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        setContentView(R.layout.main);
        myView = (MyView) findViewById(R.id.gameView);
        myView.setOnTouchListener(this);
    }
}