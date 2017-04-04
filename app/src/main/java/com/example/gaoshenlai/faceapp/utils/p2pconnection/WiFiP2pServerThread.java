package com.example.gaoshenlai.faceapp.utils.p2pconnection;

import android.app.admin.DeviceAdminInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.FaceDetector;
import android.os.BatteryManager;
import android.os.Environment;
import android.util.Log;

import com.example.gaoshenlai.faceapp.MainMenu;
import com.example.gaoshenlai.faceapp.utils.imageviewhelper.bitmaphelper;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.crypto.spec.DESedeKeySpec;

import static android.content.Context.BATTERY_SERVICE;

/**
 * Created by gaoshenlai on 22/2/17.
 */

public class WiFiP2pServerThread extends Thread {
    public WiFiP2pServerThread(Context context,WiFiP2pListener listener){
        this.context = context;
        this.listener = listener;
    }
    Context context;
    WiFiP2pListener listener;

    public static String DEBUG_LOG = "FaceAppDebugLog";
    String folderName = "FaceApp";

    boolean status = true;
    public void setStatus(boolean s){
        status=s;
    }

    String IP;
    String device;

    @Override
    public void run() {
        while(status){
            //Log.d(DEBUG_LOG,"WiFiP2pServerThread is running");
            try{
                ServerSocket serverSocket = new ServerSocket(WiFiP2pDataTransfer.PORT);
                Socket client = serverSocket.accept();
                Log.d(DEBUG_LOG,"WiFiP2pthread: Data Received");
                InputStream inputstream = client.getInputStream();
                byte data_type[] = new byte[1];
                inputstream.read(data_type,0,1);
                if(data_type[0]==WiFiP2pDataTransfer.FILE_DATA) {
                    Log.d(DEBUG_LOG,"WiFiP2pthread: File Received");
                    final File f = new File(Environment.getExternalStorageDirectory() + "/"
                            + folderName + "/wifip2pshared-" + System.currentTimeMillis()
                            + ".jpg");
                    File dirs = new File(f.getParent());
                    if (!dirs.exists()) dirs.mkdirs();
                    f.createNewFile();

                    copyFile(inputstream, new FileOutputStream(f));
                    serverSocket.close();
                    Log.d(DEBUG_LOG, "File received: " + f.getAbsolutePath());

                    listener.sendString("REPLYFILE:recieved "+f.getAbsolutePath());

                }else if(data_type[0]==WiFiP2pDataTransfer.IP_DATA){
                    IP = client.getInetAddress().getHostAddress();
                    Log.d(DEBUG_LOG,"WiFiP2pthread: IP Address obtained, IP: "+IP);
                    device = client.getInetAddress().getHostName();
                    listener.updateDeviceIPMap(device,IP);

                }else if(data_type[0]==WiFiP2pDataTransfer.STRING_DATA){
                    DataInputStream stream = new DataInputStream(inputstream);
                    String data = stream.readUTF();

                    if(data.startsWith("REQUESTBATTERY:")){
                        int batLevel = getBatteryPercentage(context);
                        String bat = Integer.toString(batLevel);
                        bat = "REPLYBATTERY:"+bat;
                        listener.sendString(bat);
                    }else if(data.startsWith("REPLYBATTERY:")){
                        Log.d(MainMenu.EXPERIMENT_LOG, data+"|"+System.currentTimeMillis());
                    }else if(data.startsWith("REPLYFILE:")){
                        Log.d(MainMenu.EXPERIMENT_LOG, data+"|"+System.currentTimeMillis());
                    }

                }else{
                    Log.d(DEBUG_LOG,"WiFiP2pthread: unrecognized data: "+data_type[0]);
                    Log.d(DEBUG_LOG,"FILE_DATA: "+WiFiP2pDataTransfer.FILE_DATA+" IP_DATA: "+WiFiP2pDataTransfer.IP_DATA);
                }
                inputstream.close();
                if (client.isConnected()) client.close();
                if (!serverSocket.isClosed()) serverSocket.close();

            }catch (Exception e){
                //Log.d(DEBUG_LOG,"WiFiP2pthread: Exception");
                //e.printStackTrace();
            }
        }
    }

    public static int getBatteryPercentage(Context context) {

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
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
            Log.d(DEBUG_LOG,"file copied");
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
