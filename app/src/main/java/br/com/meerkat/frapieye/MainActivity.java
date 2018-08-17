package br.com.meerkat.frapieye;

import android.app.DownloadManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.support.design.widget.TextInputLayout;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static CameraPreviewSurface preview = null;
    private static SurfaceOverlay overlay;
    private static boolean created = false;

    private static final int REQUEST_CAMERA_RESULT = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 112;
    private static final String TAG = "MainActivity";

    private static RelativeLayout aboutLayout;
    private static RelativeLayout inputLayout;
    private static FrameLayout pnlFlash;



    private static Toolbar toolbar;
    private static EditText inputName, inputIp, inputPort;
    private static TextInputLayout inputLayoutName, inputLayoutEmail, inputLayoutPassword;
    private static Button btnSignUp;

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

        pnlFlash = (FrameLayout) findViewById(R.id.pnlFlash);
        overlay.setFlashPanel(pnlFlash);

        FrameLayout resultLayout = (FrameLayout) findViewById(R.id.resultScreen);
        overlay.setResultLayout(resultLayout);

        ImageView resultImageView = (ImageView) findViewById(R.id.resultFace);
        overlay.setResultImageView(resultImageView);

        Typeface tf = Typeface.createFromAsset(getAssets(),"fonts/SourceSansPro-Regular.ttf");
        overlay.setFont(tf);

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


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        inputLayout = (RelativeLayout) findViewById(R.id.relativeLayoutInput);
        setSupportActionBar(toolbar);

        inputLayoutName = (TextInputLayout) findViewById(R.id.input_layout_name);
        inputLayoutEmail = (TextInputLayout) findViewById(R.id.input_layout_ip);
        inputLayoutPassword = (TextInputLayout) findViewById(R.id.input_layout_port);
        inputName = (EditText) findViewById(R.id.input_name);
        inputIp = (EditText) findViewById(R.id.input_ip);
        inputPort = (EditText) findViewById(R.id.input_port);
        btnSignUp = (Button) findViewById(R.id.btn_signup);

        inputName.addTextChangedListener(new MyTextWatcher(inputName));
        inputIp.addTextChangedListener(new MyTextWatcher(inputIp));
        inputPort.addTextChangedListener(new MyTextWatcher(inputPort));

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });


        final ImageButton buttonInput = (ImageButton) findViewById(R.id.inputButton);
        buttonInput.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (inputLayout.getVisibility()==View.INVISIBLE) {
                    inputLayout.setVisibility(View.VISIBLE);
                } else {
                    inputLayout.setVisibility(View.INVISIBLE);
                }
            }
        });


        File dir  = this.getFilesDir();
        File file = new File(dir, "frapi_config.txt");

        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            byte bytes_input[] = new byte[(int)file.length()];
            fin.read(bytes_input);
            String frapi_config = new String(bytes_input);
            Log.i(TAG, "frAPI config: "+frapi_config);

            BufferedReader reader = new BufferedReader(new StringReader(frapi_config));

            String line = reader.readLine();
            inputName.setText(line);
            line = reader.readLine();
            inputIp.setText(line);
            line = reader.readLine();
            inputPort.setText(line);
        } catch (Exception e) {
            try {
                FileOutputStream outputStream;
                outputStream = this.openFileOutput("frapi_config.txt", Context.MODE_PRIVATE);
                String cfg = "test\n192.168.25.4\n4444";
                outputStream.write(cfg.getBytes());
                outputStream.close();
            } catch (Exception e2){
                e2.printStackTrace();
            }
        }

        inputLayout.setVisibility(View.INVISIBLE);

        Log.i(TAG, "Loading finished");
    }


    private class PostAsyncTask extends AsyncTask<String, Void, Void> {
        private boolean success = false;

        @Override
        protected Void doInBackground(String... params) {
            String url_post = params[0];
            String json     = params[1];
            String url_get  = params[2];

            try {
                success = true;
                OkHttpClient client = new OkHttpClient();

                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(JSON, json);

                Request request = new Request.Builder()
                        .url(url_post)
                        .post(body)
                        .addHeader("content-type", "application/json")
                        .addHeader("cache-control", "no-cache")
                        .addHeader("postman-token", "8ff7da38-ef3f-2ec2-1eb0-1436d86ece0d")
                        .build();

                Response response = client.newCall(request).execute();

                if(response.code() != 200)
                    success = false;
                else {
                    Log.e("WIFIIP", "Response code != 200: "+response.toString());
                }

                if(success) {
                    request = new Request.Builder()
                            .url(url_get)
                            .get()
                            .addHeader("cache-control", "no-cache")
                            .addHeader("postman-token", "f751f18d-e65b-9b87-d093-97a04b94aa38")
                            .build();

                    response = client.newCall(request).execute();
                }

                if(response.code() != 200)
                    success = false;
                else {
                    Log.e("WIFIIP", "Response code != 200: "+response.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            if(success)
                Toast.makeText(getBaseContext(), "Request sent to frAPI! Transmition should start soon.", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getBaseContext(), "Failed to send request to frAPI! Please, check your frAPI configurations and network connectivity.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }


    protected String wifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }


    /**
     * Validating form
     */
    private void submitForm() {
        if (!validateName()) {
            return;
        }

        if (!validateIp()) {
            return;
        }

        if (!validatePort()) {
            return;
        }

        inputLayout.setVisibility(View.INVISIBLE);
        // hide the keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(inputLayout.getWindowToken(), 0);

        Toast.makeText(getApplicationContext(), "Thank You!", Toast.LENGTH_SHORT).show();

        String my_ip = wifiIpAddress(this);
        if (my_ip.length() <= 0) {
            Toast.makeText(getApplicationContext(), "There was a problem finding your WiFi ip.", Toast.LENGTH_SHORT).show();
            return;
        }

        String label = inputName.getText().toString().trim();
        String ip    = inputIp.getText().toString().trim();
        String port  = inputPort.getText().toString().trim();
        String url   = "http://"+ip+":"+port+"/stream/upsert";
        String json  = "{" +
                        "\"stream_label\": \""+label+"\"," +
                        "\"stream_type\":  \"websocket_h264\"," +
                        "\"path\":         \"ws://"+my_ip+":4446\"," +
                        "\"algo_type\":    \"recognition\"" +
                        "}";

        String url_get = "http://"+ip+":"+port+"/stream/run/"+label;

        new PostAsyncTask().execute(url, json, url_get);

        try {
            FileOutputStream outputStream;
            outputStream = this.openFileOutput("frapi_config.txt", Context.MODE_PRIVATE);
            String cfg = label+"\n"+ip+"\n"+port;
            outputStream.write(cfg.getBytes());
            outputStream.close();
        } catch (Exception e2){
            e2.printStackTrace();
        }
    }

    private boolean validateName() {
        if (inputName.getText().toString().trim().isEmpty()) {
            inputLayoutName.setError(getString(R.string.err_msg_name));
            requestFocus(inputName);
            return false;
        } else {
            inputLayoutName.setErrorEnabled(false);
        }

        return true;
    }

    private boolean validateIp() {
        String ip = inputIp.getText().toString().trim();

        if (ip.isEmpty() || !isValidIp(ip)) {
            inputLayoutEmail.setError(getString(R.string.err_msg_ip));
            requestFocus(inputIp);
            return false;
        } else {
            inputLayoutEmail.setErrorEnabled(false);
        }

        return true;
    }

    private boolean validatePort() {
        if (inputPort.getText().toString().trim().isEmpty()) {
            inputLayoutPassword.setError(getString(R.string.err_msg_port));
            requestFocus(inputPort);
            return false;
        } else {
            inputLayoutPassword.setErrorEnabled(false);
        }

        return true;
    }

    private static boolean isValidIp(String ip) {
        return !TextUtils.isEmpty(ip) && android.util.Patterns.IP_ADDRESS.matcher(ip).matches();
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private class MyTextWatcher implements TextWatcher {

        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.input_name:
                    validateName();
                    break;
                case R.id.input_ip:
                    validateIp();
                    break;
                case R.id.input_port:
                    validatePort();
                    break;
            }
        }
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
