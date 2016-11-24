package br.com.meerkat.frapieye;


import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.FrameBuilder;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;


public class VideoServer extends WebSocketServer {
    private static int counter = 0;
    private String camType_ = "";
    public boolean readyToSend_ = false;
    public String lastResult_ = "";
    WebSocket ws_ = null;

    public VideoServer( int port ) throws UnknownHostException {
        super( new InetSocketAddress( port ) );
    }

    @Override
    public void onOpen( WebSocket conn, ClientHandshake handshake ) {
        counter++;
        if (counter > 1) {
            conn.close();
            return;
        }

        ws_ = conn;
        System.out.println( "///////////Opened connection number" + counter );
        ws_.send(camType_);
        readyToSend_ = true;
    }

    @Override
    public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
        System.out.println( "Websocket closed" );
        counter--;

        if(counter <= 0)
            readyToSend_ = false;
    }

    @Override
    public void onError( WebSocket conn, Exception ex ) {
        System.out.println( "Error:" );
        ex.printStackTrace();
    }

    @Override
    public void onMessage( WebSocket conn, String message ) {
        lastResult_ = message;
        readyToSend_ = true;
    }

    @Override
    public void onWebsocketMessageFragment( WebSocket conn, Framedata frame ) {
        FrameBuilder builder = (FrameBuilder) frame;
        builder.setTransferemasked( false );
        conn.sendFrame( frame );
    }

    public void SendFrame(byte[] data) {
        if(ws_ == null || ws_.isClosed())
            return;

        try {
            ws_.send(data);
        } catch (Exception e) { }
    }

    public void ChangeCamera(String msg) {
        camType_ = msg;

        if(ws_ != null && ws_.isClosed() == false) {
            try {
                ws_.send(camType_);
            } catch (Exception e) { }
        }
    }
}