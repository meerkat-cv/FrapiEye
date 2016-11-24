package br.com.meerkat.frapieye;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.res.Configuration;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.java_websocket.WebSocketImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;


public class CameraPreviewSurface extends SurfaceView implements SurfaceHolder.Callback{
    public enum CameraType {
        FRONT_CAMERA,
        BACK_CAMERA;
    }

    private CameraType camType = CameraType.FRONT_CAMERA;
    private int cameraWidth = 720;
    private int cameraHeight = 480;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private CameraDetectorCaller mCamDetector = new CameraDetectorCaller();
    public static final String TAG = "CameraPreviewSurface";
    public SurfaceOverlay overlay;
    private TextView textView;
    private RelativeLayout splashScreen;

    public void closeCamera() {
        if(mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mHolder.removeCallback(this);
            mCamera.release();
        }
    }


    public void linkOverlay(SurfaceOverlay _overlay) {
        overlay = _overlay;

        double overlayScale = defineOverlayScale();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT &&
                !Build.FINGERPRINT.startsWith("generic"))
            // once the overlay is set I can open the camera
            overlay.getHolder().setFixedSize((int)(overlayScale*cameraHeight), (int)(overlayScale*cameraWidth));
        else
            overlay.getHolder().setFixedSize((int)overlayScale*cameraWidth, (int)overlayScale*cameraHeight);
        overlay.setScale(overlayScale);

    }

    private double defineOverlayScale() {
        int screen_height = getResources().getDisplayMetrics().heightPixels;
        if (screen_height > 2*cameraWidth)
            return 2.0;
        else
            return 1.0;
    }

    public CameraPreviewSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCameraPreviewSurface();
    }

    public CameraPreviewSurface(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initCameraPreviewSurface();
    }

    private void initCameraPreviewSurface() {
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (null == mCamera) {
                mCamera = CameraUtils.openFrontFacingCameraGingerbread();
            } else {
                mCamera.release();
                mCamera = CameraUtils.openFrontFacingCameraGingerbread();
            }

            changeCamera();

            mCamera.setPreviewCallbackWithBuffer(mCamDetector);
            splashScreen.setVisibility(View.INVISIBLE);
            mCamDetector.startStream(camType);

            // The camera must be set up again after the a AvcEncoder is created
            changeCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setTextView(TextView textView) {
        this.textView = textView;
    }

    public void setSplashScreen(RelativeLayout splashScreen) {
        this.splashScreen = splashScreen;
    }


    public class CameraDetectorCaller implements Camera.PreviewCallback{
        public static final String TAG = "CameraDetectorCaller";
        private VideoServer stream_;
        String frapiIP_, frapiPort_;
        AvcEncoder encoder;
        Rect lastFace_ = new Rect(0,0,0,0);
        private double fps;
        private long lastTime;
        private int spoofResult = 0;
        private int w=100, h=100;

        boolean isStreaming = false;
        byte[] previewBuffer;
        ArrayList<byte[]> encDataList = new ArrayList<byte[]>();

        Runnable senderRun = new Runnable() {
            @Override
            public void run()
            {
                while (isStreaming) {
                    boolean empty = false;
                    byte[] encData = null;

                    synchronized(encDataList) {
                        if (encDataList.size() == 0) {
                            empty = true;
                        } else {
                            encData = encDataList.remove(0);
                        }
                    }
                    if (empty) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }

                    stream_.SendFrame(encData);
                }
            }
        };


        public void startStream(CameraType camType)
        {
            this.encoder = new AvcEncoder();
            this.encoder.init(w, h, 30, 5000000);

            if (camType == CameraType.BACK_CAMERA) {
                stream_.ChangeCamera("{\"transpose\": 1, \"flip\": 1}");
            }
            else {
                stream_.ChangeCamera("{\"transpose\": 1, \"flip\": -1}");
            }

            this.isStreaming = true;
            Thread thrd = new Thread(senderRun);
            thrd.start();
        }

        private void stopStream()
        {
            this.isStreaming = false;

            if (this.encoder != null)
                this.encoder.close();
            this.encoder = null;
        }

        public CameraDetectorCaller() {
            try {
                WebSocketImpl.DEBUG = false;
                stream_ = new VideoServer(4446);
                stream_.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setSize(int W, int H) {
            w=W; h=H;
            int stride = (int) Math.ceil(w/16.0f) * 16;
            int cStride = (int) Math.ceil(w/32.0f)  * 16;
            final int frameSize = stride * h;
            final int qFrameSize = cStride * h / 2;
            this.previewBuffer = new byte[frameSize + qFrameSize * 2];
        }

        public void onPreviewFrame(byte[] data, Camera cam) {
            mCamera.addCallbackBuffer(this.previewBuffer);

            if (stream_.readyToSend_ == false || this.isStreaming == false)
                return;

            //just to simulate a frontal camera :-) in case of emulator
            if (Build.FINGERPRINT.startsWith("generic")) {
                data = CameraUtils.rotateNV21(data, w, h, 90);
                int aux = h;
                h = w;
                w = aux;
            }

            fps = 1000000000.0 / (System.nanoTime() - lastTime);
            lastTime = System.nanoTime();
            if (overlay != null) {
                overlay.setFPS(fps);
                if (stream_.lastResult_.length() > 0) {
                    try {
                        final JSONObject obj = new JSONObject(stream_.lastResult_);
                        final JSONArray faces;
                        List<Rect> faces_list;
                        List<String> labels_list = new ArrayList<>();
                        List<Float> conf_list = new ArrayList<>();

                        if(obj.has("ok") == false) {
                            if (obj.has("people")) {
                                faces = obj.getJSONArray("people");
                                faces_list  = getFacesFromJSON(faces);
                                labels_list = getLabelsFromJSON(faces, conf_list);
                            } else {
                                faces = obj.getJSONArray("faces");
                                faces_list = getFacesFromJSON(faces);
                            }

                            overlay.setRectangles(faces_list, labels_list, conf_list);
                        }
                    } catch (Exception e) {
                        overlay.setRectangles(new ArrayList<Rect>(), new ArrayList<String>(), new ArrayList<Float>());
                        e.printStackTrace();
                    }
                }

            }

            if (this.isStreaming) {
                try {
                    byte[] encData = this.encoder.offerEncoder(data);
                    if (encData.length > 0) {
                        stream_.readyToSend_ = false;
                        synchronized(this.encDataList) {
                            this.encDataList.add(encData);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        List<String> getLabelsFromJSON(JSONArray facesArray, List<Float> confList) {
            List<String> labels = new ArrayList<>();

            for (int i=0; i<facesArray.length(); i++) {
                try {
                    JSONObject obj = facesArray.getJSONObject(i);

                    JSONObject recog = obj.getJSONObject("recognition");
                    String label = recog.getString("predictedLabel");
                    float conf = (float)recog.getDouble("confidence");
                    labels.add(label);
                    confList.add(conf);

                } catch (Exception e) { }
            }

            return labels;
        }


        List<Rect> getFacesFromJSON(JSONArray facesArray) {
            List<Rect> faces = new ArrayList<Rect>();

            for (int i=0; i<facesArray.length(); i++) {
                try {
                    JSONObject f = facesArray.getJSONObject(i);
                    Rect face = getBBox(f);
                    faces.add(face);
                } catch (Exception e) { }
            }

            return faces;
        }

        Rect getBBox(JSONObject obj) {
            try {
                JSONObject br = obj.getJSONObject("bottom_right");
                JSONObject tl = obj.getJSONObject("top_left");

                int b = br.getInt("y");
                int r = br.getInt("x");
                int t = tl.getInt("y");
                int l = tl.getInt("x");

                return new Rect(l,t,r,b);
            } catch (Exception e) {
                e.printStackTrace();
                return new Rect(0,0,0,0);
            }
        }

        void displayResultOnOverlay(byte[] data) {
            int[] rgb_data = new int[cameraWidth * cameraHeight];
            CameraUtils.YUV_NV21_TO_RGB(rgb_data, data, cameraWidth, cameraHeight);

            Bitmap bitmap = Bitmap.createBitmap(cameraWidth, cameraHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(rgb_data, 0, cameraWidth, 0, 0, cameraWidth, cameraHeight);

            Matrix matrix = new Matrix();
            if (camType == CameraType.FRONT_CAMERA) {
                matrix.preScale(1.0f, -1.0f); // flip horizontally
                matrix.postRotate(-90);
            }
            else
                matrix.postRotate(90);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            overlay.setSpoofResult(spoofResult);
            overlay.showResult(rotated);

        }
    }

    void changeCamera() {
        // first stop the current camera
        if(mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mHolder.removeCallback(this);
            mCamera.release();
        }
        mCamera = null;

        try {
            if (camType == CameraType.BACK_CAMERA) {
                mCamera = CameraUtils.openFrontFacingCameraGingerbread();
                camType = CameraType.FRONT_CAMERA;
            }
            else {
                mCamera = CameraUtils.openBackFacingCameraGingerbread();
                camType = CameraType.BACK_CAMERA;
            }
            int w = cameraWidth;
            int h = cameraHeight;
            mCamera.setPreviewDisplay(mHolder);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(cameraWidth, cameraHeight);
            parameters.setPreviewFormat(ImageFormat.YV12);
            mCamera.setParameters(parameters);

            mCamera.addCallbackBuffer(mCamDetector.previewBuffer);
            mCamera.startPreview();
            mCamDetector.setSize(w, h);
//            mCamera.setPreviewCallback(mCamDetector);
            mCamera.setPreviewCallbackWithBuffer(mCamDetector);
            mCamDetector.startStream(camType);
        } catch (IOException e) {
            Log.e(TAG, "Unable to open camera or set preview display!");
            mCamera.release();
            mCamera = null;
        }

    }


}
