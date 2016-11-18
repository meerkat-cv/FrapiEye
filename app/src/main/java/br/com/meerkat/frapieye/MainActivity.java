package br.com.meerkat.frapieye;

import android.util.Log;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class MainActivity extends Activity {
    private CameraPreviewSurface preview = null;
    private SurfaceOverlay overlay;

    private static final int REQUEST_CAMERA_RESULT = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 112;
    private static final String TAG = "MainActivity";

    private RelativeLayout aboutLayout;
    private FrameLayout pnlFlash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean hasWritePermission = false;
        boolean hasCameraPermission = false;

        // should request permission if android api > 23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasWritePermission = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

            if (hasWritePermission == false) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);

                while (hasWritePermission == false) {
                    try {
                        Thread.sleep(50);                 //1000 milliseconds is one second.
                        hasWritePermission = (ContextCompat.checkSelfPermission(this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED);
            if (hasCameraPermission == false) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_RESULT);

                while (hasCameraPermission == false) {
                    try {
                        Thread.sleep(50);                 //1000 milliseconds is one second.
                        hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        setContentView(R.layout.activity_main);
        TextView t2 = (TextView) findViewById(R.id.aboutTextView);
        t2.setMovementMethod(LinkMovementMethod.getInstance());

        aboutLayout = (RelativeLayout) findViewById(R.id.relativeLayoutAbout);
        aboutLayout.setVisibility(View.INVISIBLE);
        overlay = (SurfaceOverlay) findViewById(R.id.surfaceOverlayView);
        preview = (CameraPreviewSurface) findViewById(R.id.surfaceView);
        preview.linkOverlay(overlay);
        preview.setTextView((TextView) findViewById(R.id.statusText));


        pnlFlash = (FrameLayout) findViewById(R.id.pnlFlash);
        overlay.setFlashPanel(pnlFlash);

        FrameLayout resultLayout = (FrameLayout) findViewById(R.id.resultScreen);
        overlay.setResultLayout(resultLayout);

        ImageView resultImageView = (ImageView) findViewById(R.id.resultFace);
        overlay.setResultImageView(resultImageView);

        RelativeLayout splashScreen = (RelativeLayout) findViewById(R.id.splashScreen);
        preview.setSplashScreen(splashScreen);

        final ImageButton button = (ImageButton) findViewById(R.id.changeCamButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                preview.changeCamera();
            }
        });

        final ImageButton buttonMeerkat = (ImageButton) findViewById(R.id.meerkatButton);
        buttonMeerkat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                aboutLayout.setVisibility(View.VISIBLE);
            }
        });

        final ImageButton buttonCloseWindow = (ImageButton) findViewById(R.id.closeWindowButton);
        buttonCloseWindow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                aboutLayout.setVisibility(View.INVISIBLE);
            }
        });

        Log.i(TAG, "Loading finished");
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_RESULT:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Cannot run application because camera service permission have not been granted", Toast.LENGTH_SHORT).show();
                }
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
            case REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Cannot run application because write permission have not been granted", Toast.LENGTH_SHORT).show();
                }
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(preview != null) {
            preview.closeCamera();
            preview = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();

        Log.i(TAG, "Pausing");
    }

    @Override
    public void onStop() {

        super.onStop();
        Log.i(TAG, "Stopping");
    }

}
