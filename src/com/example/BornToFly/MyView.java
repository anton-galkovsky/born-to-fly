package com.example.BornToFly;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

public class MyView extends SurfaceView implements SurfaceHolder.Callback {

    private MyThread myThread;
    private Activity activity; // to display Game Over dialog in GUI thread
    private boolean dialogIsDisplayed = false;
    private boolean gameIsOver;

    private int screenWidth;
    private int screenHeight;

    private int wallWidth;
    private int backgroundInitialSpeed;
    private int manHorizontalSpeed;
    private int maxModuleVelocity;
    private int maxGravityVelocity;
    private int boostDelta;

    private double flightTime;
    private double pathDelayBeforeBarrierGenerate;
    private double pathDelayBeforeBonusGenerate;
    private Dot man;
    private Dot background;
    private ArrayList<Dot> objects;

    private double backgroundModule;

    private Paint velocityTextPaint;
    private int velocityTextSize;
    private int timeTextSize;
    private Paint timeTextPaint;
    private Paint manPaint;
    private Paint backgroundPaint;
    private Paint[] objectPaint;
    private Bitmap[] bitmaps;

    ObjectType lastBarrierType;

    private float x;
    private boolean hasTouch;

    private Bitmap myBitmap;
    private Bitmap velocityBitmap;
    private Bitmap timeBitmap;
    private Bitmap backgroundBitmap;

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);

        activity = (Activity) context;
        getHolder().addCallback(this);  // register SurfaceHolder.Callback listener

        man = new Dot();
        background = new Dot();
        objects = new ArrayList<>();

        velocityTextPaint = new Paint();
        timeTextPaint = new Paint();
        backgroundPaint = new Paint();
        manPaint = new Paint();
        objectPaint = new Paint[9];
        bitmaps = new Bitmap[9];
        for (int i = 0; i < 9; i++)
            objectPaint[i] = new Paint();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w;
        screenHeight = h;

        man.setFields(h, w);

        boostDelta = h / 10;
        backgroundInitialSpeed = h / 5;
        maxGravityVelocity = h * 2 / 3;
        maxModuleVelocity = h;
        manHorizontalSpeed = 2 * w;
        wallWidth = w / 15;

        man.x = w / 2;
        man.y = h / 2;

        lastBarrierType = ObjectType.BOOSTER_ARROW;
        pathDelayBeforeBarrierGenerate = 0;
        pathDelayBeforeBonusGenerate = 0;

        velocityTextSize = w / 8;
        velocityTextPaint.setTextSize(velocityTextSize);
        velocityTextPaint.setAntiAlias(true);
        timeTextSize = w / 10;
        timeTextPaint.setTextSize(timeTextSize);
        timeTextPaint.setAntiAlias(true);

        backgroundPaint.setColor(Color.LTGRAY);
        manPaint.setColor(Color.RED);
        objectPaint[0].setColor(Color.RED);
        objectPaint[1].setColor(Color.BLUE);
        objectPaint[2].setColor(Color.GREEN);
        objectPaint[3].setColor(Color.MAGENTA);
        objectPaint[4].setColor(Color.DKGRAY);
        objectPaint[5].setColor(Color.DKGRAY);
        objectPaint[6].setColor(Color.DKGRAY);
        objectPaint[7].setColor(Color.BLACK);

        bitmaps[0] = BitmapFactory.decodeResource(getResources(), R.drawable.booster);
        bitmaps[1] = BitmapFactory.decodeResource(getResources(), R.drawable.t_shirts);
        bitmaps[2] = BitmapFactory.decodeResource(getResources(), R.drawable.shorts);
        bitmaps[3] = BitmapFactory.decodeResource(getResources(), R.drawable.balloon);
        bitmaps[4] = BitmapFactory.decodeResource(getResources(), R.drawable.center_balcony);
        bitmaps[5] = BitmapFactory.decodeResource(getResources(), R.drawable.left_balcony);
        bitmaps[6] = BitmapFactory.decodeResource(getResources(), R.drawable.right_balcony);
        bitmaps[7] = BitmapFactory.decodeResource(getResources(), R.drawable.helicopter);

        myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.man);
        velocityBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.veliocity_frame);
        timeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.time_frame);
        backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background);

        backgroundModule = 5.225 * screenWidth;

        newGame();
    }

    public void newGame() {
        man.x = screenWidth / 2;
        man.y = screenHeight / 2;
        flightTime = 0.0;
        background.y = 0;
        background.x = 0;
        background.v = -backgroundInitialSpeed;
        objects.clear();
        if (gameIsOver) {
            gameIsOver = false;
            myThread = new MyThread(getHolder());
            myThread.start();
        }
    }

    private class MyThread extends Thread {
        private SurfaceHolder surfaceHolder; // for manipulating canvas
        private boolean threadIsRunning = true;

        public MyThread(SurfaceHolder holder) {
            surfaceHolder = holder;
            setName("GameThread");
        }

        public void setRunning(boolean running) {
            threadIsRunning = running;
        }

        @Override
        public void run() {
            Canvas canvas = null;
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning) {
                try {
                    canvas = surfaceHolder.lockCanvas(null);

                    synchronized (surfaceHolder) {
                        long currentTime = System.currentTimeMillis();
                        double dtMil = currentTime - previousFrameTime;
                        double dtSec = dtMil / 1000;
                        flightTime += dtSec;
                        updatePositions(dtMil);
                        generateObjects();
                        drawGameElements(canvas);
                        previousFrameTime = currentTime;
                    }
                } finally {
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void generateObjects() {
        if (pathDelayBeforeBarrierGenerate * background.v >= 0) {
            Dot object = new Dot();
            object.y = screenHeight * (0.5 - Math.signum(background.v));

            if (Math.abs(background.v) / maxModuleVelocity < 0.5)
                pathDelayBeforeBarrierGenerate = Math.signum(-background.v) * screenHeight * (0.2 + new Random().nextDouble() * 0.1);
            else if (Math.abs(background.v) / maxModuleVelocity < 0.75)
                pathDelayBeforeBarrierGenerate = Math.signum(-background.v) * screenHeight * (0.33 + new Random().nextDouble() * 0.1);
            else
                pathDelayBeforeBarrierGenerate = Math.signum(-background.v) * screenHeight * (0.5 + new Random().nextDouble() * 0.1);

            if (lastBarrierType == ObjectType.BARRIER_CENTER_BALCONY || lastBarrierType == ObjectType.BARRIER_HELICOPTER) {
                object.type = ObjectType.values()[5 + new Random().nextInt(2)];
            } else {
                if (new Random().nextDouble() > 0.7)
                    object.type = ObjectType.BARRIER_HELICOPTER;
                else if (new Random().nextDouble() > 0.4)
                    object.type = ObjectType.BARRIER_CENTER_BALCONY;
                else
                    object.type = ObjectType.values()[4 + new Random().nextInt(3)];
            }

            object.setFields(screenHeight, screenWidth);
            if (object.type == ObjectType.BARRIER_LEFT_BALCONY)
                object.x = wallWidth + object.widthHalf;
            else if (object.type == ObjectType.BARRIER_RIGHT_BALCONY)
                object.x = screenWidth - wallWidth - object.widthHalf;
            else
                object.x = wallWidth + object.widthHalf + new Random().nextInt(screenWidth - 2 * wallWidth - 2 * object.widthHalf + 1);
            object.v = background.v;
            objects.add(object);

            if (new Random().nextDouble() > 0.75) {
                Dot object1 = new Dot();
                object1.y = (screenHeight * (0.5 - Math.signum(background.v))) * (1 + Math.abs(pathDelayBeforeBarrierGenerate) / 2);
                object1.type = ObjectType.values()[5 + new Random().nextInt(2)];
                object1.setFields(screenHeight, screenWidth);
                if (object1.type == ObjectType.BARRIER_LEFT_BALCONY)
                    object1.x = wallWidth + object1.widthHalf;
                else
                    object1.x = screenWidth - wallWidth - object1.widthHalf;
                object1.v = background.v;
                objects.add(object1);
            }

            lastBarrierType = object.type;
        }
        if (pathDelayBeforeBonusGenerate * background.v >= 0) {
            Dot object = new Dot();
            object.y = screenHeight * (0.5 - Math.signum(background.v) * 0.75);

            boolean boost = false;
            if (Math.abs(background.v) / maxModuleVelocity < 0.5) {
                pathDelayBeforeBonusGenerate = Math.signum(-background.v) * screenHeight * (0.2 + new Random().nextDouble() * 0.1);
                if (new Random().nextDouble() < 0.7)
                    boost = true;
            } else if (Math.abs(background.v) / maxModuleVelocity < 0.75) {
                pathDelayBeforeBonusGenerate = Math.signum(-background.v) * screenHeight * (0.3 + new Random().nextDouble() * 0.1);
                if (new Random().nextDouble() < 0.5)
                    boost = true;
            } else {
                pathDelayBeforeBonusGenerate = Math.signum(-background.v) * screenHeight * (0.4 + new Random().nextDouble() * 0.1);
                if (new Random().nextDouble() < 0.4)
                    boost = true;
            }
            if (boost)
                object.type = ObjectType.BOOSTER_ARROW;
            else
                object.type = ObjectType.values()[1 + new Random().nextInt(3)];
            object.setFields(screenHeight, screenWidth);

            object.x = wallWidth + object.widthHalf + new Random().nextInt(screenWidth - 2 * wallWidth - 2 * object.widthHalf + 1);
            boolean add = true;
            for (int i = 0; i < objects.size(); i++)
                if (Math.abs(objects.get(i).y - object.y) < objects.get(i).heightHalf + object.heightHalf &&
                        Math.abs(objects.get(i).x - object.x) < objects.get(i).widthHalf + object.widthHalf) {
                    add = false;
                    break;
                }
            object.v = background.v;
            if (add)
                objects.add(object);
        }
    }

    private void updatePositions(double dt) {
        double acceleration = (maxGravityVelocity * Math.signum(-background.v) + background.v) / 30;

        double interval = dt / 1000.0;
        background.v -= acceleration * interval;

        if (hasTouch) {
            double delta = manHorizontalSpeed * Math.signum(x - screenWidth / 2) * interval;
            if (man.x + delta - man.widthHalf >= wallWidth && man.x + delta + man.widthHalf <= screenWidth - wallWidth)
                man.x += delta;
        }

        pathDelayBeforeBarrierGenerate += interval * background.v;
        pathDelayBeforeBonusGenerate += interval * background.v;

        background.y = (background.y + interval * background.v) % backgroundModule;
        for (int i = 0; i < objects.size(); i++) {
            Dot object = objects.get(i);
            if (object.y < -screenHeight || object.y > 2 * screenHeight) {
                objects.remove(i);
                i--;
                continue;
            }
            object.y += interval * background.v;

            boolean stopGame = false;
            if (Math.abs(man.y - object.y) < man.heightHalf + object.heightHalf && Math.abs(man.x - object.x) < man.widthHalf + object.widthHalf) {
                if (object.type == ObjectType.BARRIER_CENTER_BALCONY ||
                        object.type == ObjectType.BARRIER_RIGHT_BALCONY ||
                        object.type == ObjectType.BARRIER_LEFT_BALCONY ||
                        object.type == ObjectType.BARRIER_HELICOPTER)
                    stopGame = true;
                else if (object.type == ObjectType.BOOSTER_ARROW) {
                    background.v -= boostDelta;
                    if (background.v < -maxModuleVelocity)
                        background.v %= maxModuleVelocity;
                } else {
                    background.v += boostDelta;
                    if (background.v > maxModuleVelocity)
                        background.v %= maxModuleVelocity;
                }

                objects.remove(i);
                i--;
            }
            if (stopGame) {
                gameIsOver = true;
                myThread.setRunning(false);
                showGameOverDialog();
            }
        }
    }

    public void touchAction(float x, boolean touch) {
        this.x = x;
        hasTouch = touch;
    }

    public void drawGameElements(Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
        Matrix backgroundMatrix = new Matrix();
        backgroundMatrix.postScale(1.0f * screenWidth / backgroundBitmap.getWidth(), 1.0f * screenWidth / backgroundBitmap.getWidth());
        backgroundMatrix.postTranslate(0, (float) background.y);
        canvas.drawBitmap(backgroundBitmap, backgroundMatrix, null);

        for (int i = 0; i < objects.size(); i++) {
            Matrix objectMatrix = new Matrix();
            objectMatrix.postScale(2.0f * objects.get(i).widthHalf / bitmaps[objects.get(i).type.ordinal()].getWidth(), 2.0f * objects.get(i).heightHalf / bitmaps[objects.get(i).type.ordinal()].getHeight());
            objectMatrix.postTranslate((float) objects.get(i).x - objects.get(i).widthHalf, (float) objects.get(i).y - objects.get(i).heightHalf);
            canvas.drawBitmap(bitmaps[objects.get(i).type.ordinal()], objectMatrix, null);
        }

        Matrix velocityMatrix = new Matrix();
        velocityMatrix.postScale(1.6f * velocityTextSize / velocityBitmap.getWidth(), 1.2f * velocityTextSize / velocityBitmap.getHeight());
        velocityMatrix.postTranslate(screenWidth - 2.1f * velocityTextSize, velocityTextSize * 0.25f);
        canvas.drawBitmap(velocityBitmap, velocityMatrix, null);

        Matrix timeMatrix = new Matrix();
        timeMatrix.postScale((float) (0.6f * timeTextSize * (Math.floor(Math.log10(flightTime + 1)) + 2.7) / timeBitmap.getWidth()), 1.2f * timeTextSize / timeBitmap.getHeight());
        timeMatrix.postTranslate(0.1f * timeTextSize, timeTextSize * 0.1f);
        canvas.drawBitmap(timeBitmap, timeMatrix, null);

        canvas.drawText((int) (100 * Math.abs(background.v) / maxModuleVelocity) / 10 + " " +
                (int) (100 * Math.abs(background.v) / maxModuleVelocity) % 10, screenWidth - velocityTextSize * 2, screenWidth / 40 + velocityTextSize, velocityTextPaint);

        canvas.drawText(getResources().getString(R.string.flight_time_format, flightTime), screenWidth / 40, screenWidth / 20 + timeTextSize / 2, timeTextPaint);

        Matrix manMatrix = new Matrix();
        manMatrix.postScale(2.0f * man.widthHalf / myBitmap.getWidth(), 2.0f * man.heightHalf / myBitmap.getHeight());
        manMatrix.postTranslate((float) man.x - man.widthHalf, (float) man.y - man.heightHalf);
        canvas.drawBitmap(myBitmap, manMatrix, null);
    }

    private void showGameOverDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setTitle("Wow! U win!");
        dialogBuilder.setCancelable(false);

        dialogBuilder.setMessage(getResources().getString(R.string.results_format, flightTime));
        dialogBuilder.setPositiveButton(R.string.reset_game, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialogIsDisplayed = false;
                        newGame();
                    }
                }
        );

        activity.runOnUiThread(
                new Runnable() {
                    public void run() {
                        dialogIsDisplayed = true;
                        dialogBuilder.show();
                    }
                }
        );
    }

    public void stopGame() {
        if (myThread != null)
            myThread.setRunning(false);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!dialogIsDisplayed) {
            myThread = new MyThread(holder);
            myThread.setRunning(true);
            myThread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        myThread.setRunning(false);

        while (retry) {
            try {
                myThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }
}