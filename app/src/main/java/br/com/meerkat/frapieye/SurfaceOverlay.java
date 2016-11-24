package br.com.meerkat.frapieye;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.fill;


public class SurfaceOverlay extends SurfaceView implements SurfaceHolder.Callback{
    private static final String TAG = "SurfaceOverlay";
    private int spoofResult;
    private List<Rect> detections = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private List<Float> confs = new ArrayList<>();
    private List<Point> landmarks = new ArrayList<>();
    private float[] blinks = new float[40];
    private float[] blinks2 = new float[40];
    private int curr_blink = 0;
    private DrawingThread drawingThread;
    private double[] FPS = new double[10];
    private int frameCount = 0;
    private SurfaceHolder mHolder;
    private double scale = 1.0;
    private FrameLayout flashPanel;

    private ImageView resultImageView;
    private FrameLayout resultLayout;


    public SurfaceOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSurfaceOverlay();
    }

    public SurfaceOverlay(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initSurfaceOverlay();
    }

    private void initSurfaceOverlay() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        fill(FPS, 0.0);
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setSpoofResult(int spoofResult) {
        if (this.spoofResult == 0 && spoofResult == 1) {
            this.flashScreen();
        }
        this.spoofResult = spoofResult;
    }

    public void setBlinks(float blink1, float blink2) {
        if (curr_blink >= blinks.length)
            curr_blink = curr_blink % blinks.length;

        blinks[curr_blink] = blink1;
        blinks2[curr_blink] = blink2;
        curr_blink++;
    }

    public void setFlashPanel(FrameLayout flashPanel) {
        this.flashPanel = flashPanel;
    }

    public int getSpoofResult() {
        return spoofResult;
    }


    public void setResultImageView(ImageView resultImageView) {
        this.resultImageView = resultImageView;
    }

    public void setResultLayout(FrameLayout resultLayout) {
        this.resultLayout = resultLayout;
    }


    public void showResult(Bitmap resultImage) {
        Drawable ob = new BitmapDrawable(getResources(), resultImage);
        resultImageView.setBackground(ob);

        if (spoofResult == 1) {
            resultLayout.setBackgroundColor(Color.argb(240, 108, 198, 100));
        }
        else if (spoofResult == 2) {
            resultLayout.setBackgroundColor(Color.argb(240, 255, 90, 79));
        }
        
        resultLayout.setVisibility(View.VISIBLE);

    }

    public void hideResult() {
        resultLayout.setVisibility(View.INVISIBLE);
    }

    public double getScale() {
        return scale;
    }


    class DrawingThread extends Thread {

        private boolean mRun;

        public void setRunning(boolean b) {
            mRun = b;
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mHolder.lockCanvas(null);
                    synchronized (mHolder) {
                        doDraw(c);
                        sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    if (c != null) {
                        mHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        private void doDraw(Canvas canvas) {
            if (canvas != null) {

                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                Paint paint = new Paint();

                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.LEFT);

                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize((int) (20 * scale));
                canvas.drawText("FPS:" + String.format("%.2f", getFPS()), (int) (30 * scale), (int) (40 * scale), paint);

                paint.setColor(Color.GREEN);
                paint.setStrokeWidth(6);

                paint.setStyle(Paint.Style.STROKE);

                for (int i=0; i<detections.size(); i++) {
                    if (detections.get(i).height() > 30) {
                        Rect det = detections.get(i);
                        String text = labels.get(i);
                        int conf = Math.round(confs.get(i));
                        int maxLength = (text.length() < 12)?text.length():12;
                        text = text.substring(0, maxLength) + " - "+String.valueOf(conf);

                        canvas.drawRect(det, paint);
                        float text_scale = det.width() / 80.0f;
                        paint.setTextSize(8 * text_scale);

                        paint.setColor(Color.BLACK);
                        paint.setStrokeWidth(1.3f * text_scale);
                        Rect bounds = new Rect(0, 0, 0, 0);
                        paint.getTextBounds(text, 0, text.length(), bounds);
                        canvas.drawText(text, det.left, det.bottom + bounds.height() * 1.3f, paint);

                        paint.setColor(Color.GREEN);
                        paint.setStrokeWidth(0.8f * text_scale);
                        canvas.drawText(text, det.left, det.bottom + bounds.height() * 1.3f, paint);

                    }
                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        holder.setFormat(PixelFormat.RGBA_8888);

        this.setZOrderOnTop(true);
        drawingThread = new DrawingThread();
        drawingThread.setRunning(true);
        drawingThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void setRectangles(List<Rect> dets, List<String> labels, List<Float> confidences) {
        this.detections.clear();
        this.labels.clear();
        this.confs.clear();

        for(Rect det : dets) {
            if (scale != 1.0)
                det.set((int)(det.left*scale), (int)(det.top*scale), (int)(det.right*scale), (int)(det.bottom*scale));

            this.detections.add(det);
        }

        for(String l: labels) {
            this.labels.add(l);
        }

        for(float f:confidences) {
            this.confs.add(f);
        }
    }

    public void setPoints(List<Point> pts) {
        this.landmarks = pts;
        if (scale != 1.0) {
            for (Point p : pts) {
                p.x = (int)(p.x*scale);
                p.y = (int)(p.y*scale);
            }
        }
    }

    public double getFPS() {
        double sum = 0;
        for (double d : FPS) sum += d;

        return sum/FPS.length;
    }

    public void setFPS(double fps) {
        this.FPS[frameCount % FPS.length] = fps;
        frameCount++;
    }

    public void flashScreen() {

        flashPanel.setVisibility(View.VISIBLE);

        AlphaAnimation fade = new AlphaAnimation(1, 0);
        fade.setDuration(50);
        fade.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation anim) {
                flashPanel.setVisibility(View.GONE);
            }
            public void onAnimationStart(Animation a) { }
            public void onAnimationRepeat(Animation a) { }
        });
        flashPanel.startAnimation(fade);
    }
}
