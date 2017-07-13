package com.example.gaoshenlai.faceapp.utils.p2pconnection;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by gaoshenlai on 22/2/17.
 */

public class WiFiP2pDataTransfer extends IntentService {
    String DEBUG_LOG = "FaceAppDebugLog";

    public static final int SOCKET_TIMEOUT = 10000;
    public static final String ACTION_SEND_FILE = "com.example.gaoshenlai.faceapp.SEND_FILE";
    public static final String ACTION_SEND_IP = "com.example.gaoshenlai.faceapp.SEND_IP";
    public static final String ACTION_SEND_STRING = "com.example.gaoshenlai.faceapp.SEND_STRING";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_IP_ADDRESS = "go_host";
    public static final String EXTRAS_PORT = "go_port";
    public static final String EXTRAS_STRING_DATA = "string_data";
    public static final String EXTRAS_FILE_ACTION = "file_action";

    public static int PORT = 6666;

    public static byte IP_DATA = 10;
    public static byte FILE_DATA = 20;
    public static byte STRING_DATA = 30;

    public static byte FILE_ACTION_NORMAL = 21;
    public static byte FILE_ACTION_DETECT = 22;

    public WiFiP2pDataTransfer(){
        super("WiFiP2pFileTransfer");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(DEBUG_LOG,"onHandleIntent() called: "+intent.getAction());
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_IP_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_PORT);
            byte action = intent.getExtras().getByte(EXTRAS_FILE_ACTION);

            try {
                socket.bind(null);
                //Log.d(DEBUG_LOG,"WiFiP2pDataTransfer: Try to connect to transfer file");
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                //Log.d(DEBUG_LOG,"WiFiP2pDataTransfer: socket connected to transfer file");
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.fromFile(new File(fileUri)));
                    stream.write(FILE_DATA);
                    stream.write(action);
                    copyFile(is, stream);
                } catch (FileNotFoundException e) {
                    Log.d(DEBUG_LOG,"WiFiP2pDataTransfer: FileNotFoundException Error");
                    e.printStackTrace();
                }
            } catch (IOException e) {

            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
                Log.d(DEBUG_LOG,"WiFiP2pDataTransfer: end of file transfer");
            }
        }else if(intent.getAction().equals(ACTION_SEND_IP)){
            try {
                //Log.d(DEBUG_LOG,"begin to send IP address");
                String host = intent.getExtras().getString(EXTRAS_IP_ADDRESS);
                int port = intent.getExtras().getInt(EXTRAS_PORT);
                Socket socket = new Socket();
                DataOutputStream outputStream = null;
                //Log.d(DEBUG_LOG,"parameters prepared: host: "+host+" port: "+port);
                //socket.setReuseAddress(true);
                //Log.d(PC,"WiFiDeviceManager.onConnectionInfoAvailable: client reused address set to true");
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                //Log.d(DEBUG_LOG,"socket isConnected():  "+socket.isConnected());
                outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.write(IP_DATA);
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }catch (SocketException e){
                Log.d(DEBUG_LOG,"WiFiP2pDataTransfer.onHandleIntent.ACTION_SEND_IP: SocketException Error");
                Log.d(DEBUG_LOG,e.toString());
            } catch (IOException e) {
                Log.d(DEBUG_LOG,"WiFiP2pDataTransfer.onHandleIntent.ACTION_SEND_IP: IOException Error");
                Log.d(DEBUG_LOG,e.toString());
            }
        }else if(intent.getAction().equals(ACTION_SEND_STRING)){
            try {
                //Log.d(DEBUG_LOG,"begin to send IP address");
                String host = intent.getExtras().getString(EXTRAS_IP_ADDRESS);
                int port = intent.getExtras().getInt(EXTRAS_PORT);
                String data = intent.getExtras().getString(EXTRAS_STRING_DATA);
                Socket socket = new Socket();
                DataOutputStream outputStream = null;
                //Log.d(DEBUG_LOG,"parameters prepared: host: "+host+" port: "+port);
                //socket.setReuseAddress(true);
                //Log.d(PC,"WiFiDeviceManager.onConnectionInfoAvailable: client reused address set to true");
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(DEBUG_LOG,"socket isConnected():  "+socket.isConnected());
                outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.write(STRING_DATA);
                outputStream.writeUTF(data);
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }catch (SocketException e){
                Log.d(DEBUG_LOG,"WiFiP2pDataTransfer.onHandleIntent.ACTION_SEND_STRING: SocketException Error");
                Log.d(DEBUG_LOG,e.toString());
            } catch (IOException e) {
                Log.d(DEBUG_LOG,"WiFiP2pDataTransfer.onHandleIntent.ACTION_SEND_STRING: IOException Error");
                Log.d(DEBUG_LOG,e.toString());
            }
        }
    }



    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long startTime=System.currentTimeMillis();

        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
            long endTime=System.currentTimeMillis()-startTime;

        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
