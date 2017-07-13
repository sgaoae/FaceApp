package com.example.gaoshenlai.faceapp.utils.p2pconnection;

import android.app.admin.DeviceAdminInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.Image;
import android.os.BatteryManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.gaoshenlai.faceapp.MainMenu;
import com.example.gaoshenlai.faceapp.utils.imageviewhelper.bitmaphelper;
import com.example.gaoshenlai.faceapp.utils.imageviewhelper.imagevieweffecthelper;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.example.gaoshenlai.faceapp.R;

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
        Log.d(MainMenu.EXPERIMENT_LOG,"battery test time: "+(System.currentTimeMillis()-batteryTestBeginTime));
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
        //Log.d(MainMenu.EXPERIMENT_LOG,"The average is: "+average);
        for(int i=0;i<winners.size();++i){
            sendStringTo("BATTERYRESULT:You are winner!",winners.get(i));
            //Log.d(MainMenu.EXPERIMENT_LOG, winners.get(i)+" is winner");
            ipbatt.remove(winners.get(i));
        }
        for(String ip:ipbatt.keySet()){
            sendStringTo("BATTERYRESULT:You lose",ip);
            //Log.d(MainMenu.EXPERIMENT_LOG, ip+" lose");
        }
        clearBatteryInfo();
    }

    @Override
    public void run() {
        while(status){
            //Log.d(DEBUG_LOG,"WiFiP2pServerThread is running");
            try {
                    ServerSocket serverSocket = new ServerSocket(WiFiP2pDataTransfer.PORT);
                            //Log.d(DEBUG_LOG, "WiFiP2pthread: Data Received")
                            Socket client = serverSocket.accept();
                            InputStream inputstream = client.getInputStream();
                            byte data_type[] = new byte[1];
                            inputstream.read(data_type, 0, 1);
                            if (data_type[0] == WiFiP2pDataTransfer.FILE_DATA) {
                                Log.d(DEBUG_LOG, "WiFiP2pthread: File_data");
                                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                                        + folderName + "/wifip2pshared-" + System.currentTimeMillis()
                                        + ".jpg");
                                File dirs = new File(f.getParent());
                                if (!dirs.exists()) {
                                    if (dirs.mkdirs()) {
                                        Log.d(DEBUG_LOG, "succeed to make dir");
                                    } else {
                                        Log.d(DEBUG_LOG, "fail to make dir");
                                    }
                                }
                                if (f.createNewFile()) {
                                    Log.d(DEBUG_LOG, "succeed in creating file");
                                } else {
                                    Log.d(DEBUG_LOG, "fail to create new file");
                                }
                                Log.d(DEBUG_LOG, "WiFiP2pthread: File_prepare");
                                byte action[] = new byte[1];
                                inputstream.read(action);
                                Log.d(DEBUG_LOG, "getFileAction " + action[0]);

                                copyFile(inputstream, new FileOutputStream(f));
                                serverSocket.close();
                                Log.d(DEBUG_LOG, "File received: " + f.getAbsolutePath());
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "File Received", Toast.LENGTH_SHORT).show();
                                    }
                                });

                                String ip = client.getInetAddress().getHostAddress();

                                if (action[0] == WiFiP2pDataTransfer.FILE_ACTION_NORMAL) {
                                    Log.d(DEBUG_LOG, "File Action Normal");
                                    sendStringTo("REPLYFILE:recieved " + f.getAbsolutePath(), ip);
                                } else if (action[0] == WiFiP2pDataTransfer.FILE_ACTION_DETECT) {
                                    Log.d(DEBUG_LOG, "File Action Face Detection");
                                    Bitmap bitmap = bitmaphelper.getProperBitmap(f.getAbsolutePath());
                                    if (bitmap != null) {
                                        FaceDetector detector = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), 10);
                                        FaceDetector.Face[] faces = new FaceDetector.Face[10];
                                        int num = detector.findFaces(bitmap, faces);
                                        String msg = "REPLYFACE:";
                                        for (int i = 0; i < num; ++i) {
                                            FaceDetector.Face face = faces[i];
                                            PointF p = new PointF();
                                            face.getMidPoint(p);
                                            float eyeD = face.eyesDistance();
                                            msg += "x:" + p.x + ";" + "y:" + p.y + ";" + "eyeDistance:" + eyeD + ";" + "|||";
                                        }
                                        sendStringTo(msg, ip);
                                    }
                                } else {
                                    Log.d(DEBUG_LOG, "Action to be performed in this file is NOT recognized");
                                }

                            } else if (data_type[0] == WiFiP2pDataTransfer.IP_DATA) {
                                IP = client.getInetAddress().getHostAddress();
                                Log.d(DEBUG_LOG, "WiFiP2pthread: IP Address obtained, IP: " + IP);
                                device = client.getInetAddress().getHostName();
                                peerInfo.addConnectedPeer(device, IP);

                            } else if (data_type[0] == WiFiP2pDataTransfer.STRING_DATA) {
                                DataInputStream stream = new DataInputStream(inputstream);
                                String data = stream.readUTF();

                                Log.d(DEBUG_LOG, data + "|" + "from" + client.getInetAddress().getHostAddress() + "|" + System.currentTimeMillis());

                                if (data.startsWith("REQUESTBATTERY:")) {
                                    String ip = client.getInetAddress().getHostAddress();
                                    int batLevel = getBatteryPercentage(context);
                                    String bat = Integer.toString(batLevel);
                                    bat = "REPLYBATTERY:" + bat;
                                    sendStringTo(bat, ip);
                                } else if (data.startsWith("REPLYBATTERY:")) {

                                    String ip = client.getInetAddress().getHostAddress();
                                    Integer batt = Integer.valueOf(data.replace("REPLYBATTERY:", ""));
                                    ipbatt.put(ip, batt);
                                    if (ipbatt.size() == peerInfo.size()) {
                                        determineWinner();
                                    }
                                } else if (data.startsWith("BATTERYRESULT:")) {
                                    final String msg = data.replace("BATTERYRESULT:", "");
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else if (data.startsWith("REPLYFILE:")) {
                                    Log.d(MainMenu.EXPERIMENT_LOG, data + "|" + System.currentTimeMillis());
                                    file.add(client.getInetAddress().getHostAddress());
                                    if (file.size() == peerInfo.size()) {
                                        Log.d(MainMenu.EXPERIMENT_LOG, "File Transfer Time: " + (System.currentTimeMillis() - fileBeginTime));
                                    }
                                } else if (data.startsWith("REPLYFACE:")) {
                                    String ip = client.getInetAddress().getHostAddress();
                                    Log.d(MainMenu.EXPERIMENT_LOG,"Time: "+System.currentTimeMillis()+"&From: "+ip+"&Msg: "+data);
                                    final String msg = data.replace("REPLYFACE:", "");
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            StringTokenizer st = new StringTokenizer(msg, "|||");
                                            ArrayList<PointF> leftUp = new ArrayList<>();
                                            ArrayList<PointF> rightDown = new ArrayList<>();
                                            int num = 0;
                                            while (st.hasMoreTokens()) {
                                                num++;
                                                String it = st.nextToken();
                                                float x = 0, y = 0, eyeDistance = 0;
                                                StringTokenizer st2 = new StringTokenizer(it, ";");
                                                while (st2.hasMoreTokens()) {
                                                    String field = st2.nextToken();
                                                    if (field.startsWith("x:")) {
                                                        x = Float.valueOf(field.replace("x:", ""));
                                                    } else if (field.startsWith("y:")) {
                                                        y = Float.valueOf(field.replace("y:", ""));
                                                    } else if (field.startsWith("eyeDistance:")) {
                                                        eyeDistance = Float.valueOf(field.replace("eyeDistance:", ""));
                                                    }
                                                }
                                                leftUp.add(new PointF(x - eyeDistance, y - eyeDistance));
                                                rightDown.add(new PointF(x + eyeDistance, y + eyeDistance));
                                            }
                                            ImageView iv = (ImageView) activity.findViewById(R.id.face_image);
                                            PointF[] lu = new PointF[num];
                                            PointF[] rd = new PointF[num];
                                            for (int i = 0; i < num; ++i) {
                                                lu[i] = new PointF(leftUp.get(i).x, leftUp.get(i).y);
                                                rd[i] = new PointF(rightDown.get(i).x, rightDown.get(i).y);
                                            }
                                            imagevieweffecthelper.highlightFaces(iv, lu, rd, num);
                                            //Log.d(MainMenu.EXPERIMENT_LOG, "Offloading Face Detection Time: " + (System.currentTimeMillis() - offloadingFaceDetectionBeginTime));
                                        }
                                    });
                                } else if (data.startsWith("pingpong:")) {
                                    String ip = client.getInetAddress().getHostAddress();
                                    String msg = data.replace("pingpong:","");
                                    Integer t = Integer.valueOf(msg);
                                    safe=false;
                                    Integer a = pingpongmap.get(ip);
                                    safe=true;
                                    if(a==null){

                                    }else{
                                        if(t>a){
                                            safe=false;
                                            pingpongmap.put(ip,t);
                                            pingpongtime.put(ip,(Long)System.currentTimeMillis());
                                            safe=true;
                                        }
                                    }
                                    if(pingpongtestfinish()){
                                        safe=false;
                                        pingpongmap.clear();
                                        pingpongtime.clear();
                                        doingpingpong=false;
                                        Log.d(MainMenu.EXPERIMENT_LOG,"pingpong time: "+(System.currentTimeMillis()-pingpongBegintime));
                                        if(pingpongtimeout!=null) {
                                            pingpongtimeout.interrupt();
                                            pingpongtimeout = null;
                                        }
                                        ableForAnotherTest=true;
                                        safe=true;
                                    }else{
                                        t = t + 1;
                                        if(t<6) {
                                            sendStringTo("pingpong:" + t, ip);
                                            Log.d(DEBUG_LOG, "normal: send " + msg + " to " + ip);
                                        }
                                    }
                                }


                            } else {
                                Log.d(DEBUG_LOG, "WiFiP2pthread: unrecognized data: " + data_type[0]);
                                Log.d(DEBUG_LOG, "FILE_DATA: " + WiFiP2pDataTransfer.FILE_DATA + " IP_DATA: " + WiFiP2pDataTransfer.IP_DATA);
                                Log.d(DEBUG_LOG,
                                        "If there are some problems with file transfer, check whether the phone has block the permission for read and write external storage");
                            }
                            inputstream.close();
                            if (client.isConnected()) client.close();
                            if (!serverSocket.isClosed()) serverSocket.close();
            } catch (IOException e) {
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

    long batteryTestBeginTime;
    public void setBatteryTestBeginTime(long t){
        batteryTestBeginTime=t;
    }
    long fileBeginTime;
    public void setFileTransferBeginTime(long t){
        fileBeginTime=t;
    }
    ArrayList<String> file = new ArrayList<>();
    public void clearFileTransferInfo(){
        file.clear();
    }

    long offloadingFaceDetectionBeginTime;
    public void setOffloadingFaceDetectionBeginTime(long t){
        offloadingFaceDetectionBeginTime=t;
    }

    HashMap<String,Long> pingpongtime = new HashMap<>();
    HashMap<String,Integer> pingpongmap = new HashMap<>();
    long pingpongBegintime;
    public void beginPingpong(long t){
        if(!ableForAnotherTest)return;
        ableForAnotherTest=false;
        doingpingpong=true;
        pingpongBegintime=t;
        pingpongmap.clear();
        pingpongtime.clear();
        ArrayList<String> ips = peerInfo.getAllIP();
        for(int i=0;i<ips.size();++i){
            pingpongmap.put(ips.get(i),-1);
            pingpongtime.put(ips.get(i),t);
        }
        sendString("pingpong:0");
        pingpongtimeout = new Thread(new Runnable() {
            int timeouttimes = 0;
            @Override
            public void run() {
                while (doingpingpong){
                    try {
                        if (safe) {
                            long t = System.currentTimeMillis();
                            Set<Map.Entry<String, Long>> entryset = pingpongtime.entrySet();
                            for (Map.Entry entry : entryset) {
                                String ip = (String) entry.getKey();
                                Long a = (Long) entry.getValue();
                                //Log.d(DEBUG_LOG,"time interval: "+(t-a)+" "+ip);
                                if ((t - a) > timeout) {
                                    Integer msg = (Integer) pingpongmap.get(ip);
                                    if (msg != null) {
                                        msg = msg + 1;
                                        if (msg < 6) {
                                            sendStringTo("pingpong:" + msg, ip);
                                            pingpongtime.put(ip,(Long)System.currentTimeMillis());
                                            //Log.d(DEBUG_LOG, "timeout: send " + msg + " to " + ip);
                                            timeouttimes++;
                                            if(timeouttimes>50){
                                                doingpingpong=false;
                                                ableForAnotherTest=true;
                                            }
                                        }
                                    }
                                    //Log.d(DEBUG_LOG,"timeout happened!!! "+ip);
                                }
                            }
                        }
                    }catch (ConcurrentModificationException e){

                    }
                }
            }
        });
        pingpongtimeout.start();
    }
    public boolean pingpongtestfinish(){
        safe=false;
        boolean s = true;
        Set<Map.Entry<String,Integer>> entryset = pingpongmap.entrySet();
        if(entryset!=null) {
            for (Map.Entry entry : entryset) {
                Integer a = (Integer) entry.getValue();
                if (a != null) {
                    a=a+1;
                    if (a < 6) {
                        s = false;
                        break;
                    }
                } else s = false;
            }
            if (entryset.size() == 0) s = false;
        }else s=false;
        safe=true;
        return s;
    }
    boolean doingpingpong = false;
    long timeout = 1000;
    Thread pingpongtimeout=null;
    boolean ableForAnotherTest = true;
    boolean safe=true;
}
