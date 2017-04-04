package com.example.gaoshenlai.faceapp.utils.p2pconnection;

import android.app.admin.DeviceAdminInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.FaceDetector;
import android.os.BatteryManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.DESedeKeySpec;

import static android.content.Context.BATTERY_SERVICE;

/**
 * Created by gaoshenlai on 22/2/17.
 */

public class WiFiP2pServerThread extends Thread {
    public WiFiP2pServerThread(AppCompatActivity activity, ConnectedPeerInfo peerInfo){
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.peerInfo = peerInfo;
    }
    AppCompatActivity activity;
    Context context;
    ConnectedPeerInfo peerInfo;

    public static String DEBUG_LOG = "FaceAppDebugLog";
    String folderName = "FaceApp";

    boolean status = true;
    public void setStatus(boolean s){
        status=s;
    }

    String IP;
    String device;

    HashMap<String, Integer> ipbatt = new HashMap<>();
    public void clearBatteryInfo(){
        ipbatt.clear();
    }
    private void determineWinner(){
        ArrayList<String> winners = new ArrayList<>();
        Integer max_bat=0;
        Double average=0.0;
        for(String ip:ipbatt.keySet()){
            Integer bat = ipbatt.get(ip);
            if(max_bat<bat){
                max_bat=bat;
                winners.clear();
                winners.add(ip);
            }else if(max_bat==bat){
                winners.add(ip);
            }else{
                // ignore
            }
            average+=bat;
        }
        average/=ipbatt.size();
        Log.d(MainMenu.EXPERIMENT_LOG,"The average is: "+average);
        for(int i=0;i<winners.size();++i){
            sendStringTo("BATTERYRESULT:You are winner!",winners.get(i));
            Log.d(MainMenu.EXPERIMENT_LOG, winners.get(i)+" is winner");
            ipbatt.remove(winners.get(i));
        }
        for(String ip:ipbatt.keySet()){
            sendStringTo("BATTERYRESULT:You lose",ip);
            Log.d(MainMenu.EXPERIMENT_LOG, ip+" lose");
        }
        clearBatteryInfo();
    }

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

                    sendString("REPLYFILE:recieved "+f.getAbsolutePath());

                }else if(data_type[0]==WiFiP2pDataTransfer.IP_DATA){
                    IP = client.getInetAddress().getHostAddress();
                    Log.d(DEBUG_LOG,"WiFiP2pthread: IP Address obtained, IP: "+IP);
                    device = client.getInetAddress().getHostName();
                    peerInfo.addConnectedPeer(device,IP);

                }else if(data_type[0]==WiFiP2pDataTransfer.STRING_DATA){
                    DataInputStream stream = new DataInputStream(inputstream);
                    String data = stream.readUTF();

                    Log.d(MainMenu.EXPERIMENT_LOG, data+"|"+System.currentTimeMillis());

                    if(data.startsWith("REQUESTBATTERY:")){
                        String ip = client.getInetAddress().getHostAddress();
                        int batLevel = getBatteryPercentage(context);
                        String bat = Integer.toString(batLevel);
                        bat = "REPLYBATTERY:"+bat;
                        sendStringTo(bat,ip);
                    }else if(data.startsWith("REPLYBATTERY:")){

                        String ip = client.getInetAddress().getHostAddress();
                        Integer batt = Integer.valueOf(data.replace("REPLYBATTERY:",""));
                        ipbatt.put(ip,batt);
                        if(ipbatt.size()==peerInfo.size()){
                            determineWinner();
                        }
                    }else if(data.startsWith("BATTERYRESULT:")) {
                        final String msg = data.replace("BATTERYRESULT:","");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
                            }
                        });
                    }else if(data.startsWith("REPLYFILE:")){

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

    private void sendString(String str){
        ArrayList<String> ips = peerInfo.getAllIP();
        for(int i=0;i<ips.size();++i){
            String IP = ips.get(i);
            sendStringTo(str,IP);
        }
    }

    private void sendStringTo(String str, String ip){
        Intent intent = new Intent(context,WiFiP2pDataTransfer.class);
        intent.setAction(WiFiP2pDataTransfer.ACTION_SEND_STRING);
        intent.putExtra(WiFiP2pDataTransfer.EXTRAS_IP_ADDRESS,ip);
        intent.putExtra(WiFiP2pDataTransfer.EXTRAS_PORT,WiFiP2pDataTransfer.PORT);
        intent.putExtra(WiFiP2pDataTransfer.EXTRAS_STRING_DATA,str);
        context.startService(intent);
    }
}
