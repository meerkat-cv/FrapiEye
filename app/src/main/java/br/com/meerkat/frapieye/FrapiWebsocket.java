package br.com.meerkat.frapieye;
import android.os.AsyncTask;
import android.util.Log;

import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.util.List;
import java.util.Map;

public class FrapiWebsocket extends AsyncTask<Void, Void, Void> {
    WebSocketFactory factory_;
    WebSocket ws_;
    String frapiUrl_;
    String streamLabel_;
    String apiKey_;
    public boolean readyToSend_ = true;

    protected Void doInBackground(Void... params) {
        try {
            // Connect to the server and perform an opening handshake.
            // This method blocks until the opening handshake is finished.
            ws_.connect();
        } catch (OpeningHandshakeException e) {
            Log.i("WEBSOCKET", "Violation against the WebSocket protocol was detected on handshake");
        } catch (WebSocketException e) {
            Log.i("WEBSOCKET", "Failed to establish a WebSocket connection");
            e.printStackTrace();
        }

        return null;
    }

    public FrapiWebsocket(String frapiUrl, String streamLabel, String apiKey) {
        frapiUrl_ = frapiUrl;
        streamLabel_ = streamLabel;
        apiKey_ = apiKey;

        // Create a WebSocket factory and set 10000 milliseconds as a timeout
        // value for socket connection.
        factory_ = new WebSocketFactory().setConnectionTimeout(10000);

        // Create a WebSocket. The scheme part can be one of the following:
        // 'ws', 'wss', 'http' and 'https' (case-insensitive). The user info
        // part, if any, is interpreted as expected. If a raw socket failed
        // to be created, an IOException is thrown.
        try {
            ws_ = factory_.createSocket(frapiUrl_+"?api_key="+apiKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Register a listener to receive WebSocket events.
        ws_.addListener(new WebSocketAdapter() {
            @Override
            public void onTextMessage(WebSocket websocket, String message) throws Exception {
                // Received a text message.
                readyToSend_ = true;
            }

            @Override
            public void onConnected(WebSocket websocket, Map<String,List<String>> headers) {
                Log.i("WEBSOCKET", "Connected");
            }
        });
    }

    public Void SendFrame(byte[] imageBin) {
        ws_.sendBinary(imageBin);

        return null;
    }

    public Void ChangeCamera(String msg) {
        ws_.sendText(msg);

        return null;
    }
}
