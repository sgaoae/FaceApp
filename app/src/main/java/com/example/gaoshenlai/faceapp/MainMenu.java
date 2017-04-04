package com.example.gaoshenlai.faceapp;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.FaceDetector;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gaoshenlai.faceapp.utils.imageviewhelper.*;
import com.example.gaoshenlai.faceapp.utils.p2pconnection.*;
import com.example.gaoshenlai.faceapp.offloading_setting.*;

import java.io.File;

public class MainMenu extends AppCompatActivity {
    public static String EXPERIMENT_LOG = "FaceAppExperimentLog";
    String DEBUG_LOG = "FaceAppDebugLog";
    int LOAD_A_PIC=8;
    String imageFilePath="";
    ImageView face;
    float width,height=0;

    RelativeLayout layout_m,layout_p;
    View.OnClickListener btn_listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.detect_btn:
                    int MAX_FACE=10;
                    Bitmap image = imagevieweffecthelper.getBitmapFromImageView(face);
                    if(image==null)break;
                    FaceDetector detector = new FaceDetector(image.getWidth(),image.getHeight(),MAX_FACE);
                    FaceDetector.Face[] results = new FaceDetector.Face[MAX_FACE];
                    int numOfFaces = detector.findFaces(image,results);
                    imagevieweffecthelper.highlightFaces(face,results,numOfFaces);
                    break;
                case R.id.load_btn:
                    try {
                        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                            Intent intent = new Intent(MainMenu.this,Browser.class);
                            startActivityForResult(intent,LOAD_A_PIC);
                        } else {
                            Toast.makeText(getApplicationContext(), "Storage not present", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.property_btn:
                    File file = new File(imageFilePath);
                    String msg;
                    if(file.exists()) {
                        long size = file.length();
                        msg = "Name:\t\t" + file.getName();
                        msg += "\n" + size / 1024.00 + " KB";
                        msg += "\nDisplay Size:\t\t" + width + " X " + height;
                        msg += "\nPath:\t\t" + imageFilePath;
                    }else msg="the file doesn't exists!";
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainMenu.this);
                    dialog.setTitle("Properties");
                    dialog.setMessage(msg);
                    dialog.setPositiveButton("EXIT", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    Dialog result_dialog = dialog.create();
                    result_dialog.show();
                    break;
                case R.id.list_btn:
                    layout_m = (RelativeLayout) findViewById(R.id.main_layout);
                    layout_p = (RelativeLayout) findViewById(R.id.peer_list_layout);
                    layout_p.setVisibility(View.VISIBLE);
                    layout_m.setVisibility(View.GONE);
                    if(mManager!=null){
                        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(DEBUG_LOG,"successfully discover");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Toast.makeText(getApplicationContext(),"Discover Failure",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;
                case R.id.send_btn:
                    Log.d(DEBUG_LOG,"sendImageFile is pressed");
                    mP2pListener.sendImageFile(imageFilePath);
                    break;
                case R.id.off_setting_btn:
                    Intent intent = new Intent(MainMenu.this, setting_menu.class);
                    startActivity(intent);
                    break;
                case R.id.back_btn:
                    ((ListView) findViewById(R.id.peer_list)).setAdapter(null);
                    layout_m = (RelativeLayout) findViewById(R.id.main_layout);
                    layout_p = (RelativeLayout) findViewById(R.id.peer_list_layout);
                    layout_m.setVisibility(View.VISIBLE);
                    layout_p.setVisibility(View.GONE);
                    break;
                case R.id.search_btn:
                    String name = (String) ((TextView) findViewById(R.id.device_name)).getText();
                    Log.d(MainMenu.EXPERIMENT_LOG,name+"|scan begin: "+System.currentTimeMillis());
                    if(mManager!=null){
                        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(DEBUG_LOG,"successfully discover");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Toast.makeText(getApplicationContext(),"Discover Failure",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;

                case R.id.test_btn1:
                    Log.d(EXPERIMENT_LOG,"REQUESTBATTERY:|"+System.currentTimeMillis());
                    mP2pListener.sendString("REQUESTBATTERY:");
                    mP2pThread.clearBatteryInfo();
                    break;
                case R.id.test_btn2:
                    /*Log.d(EXPERIMENT_LOG,"TRANSFERFILE:|"+System.currentTimeMillis());
                    String path = Environment.getExternalStorageDirectory()+"/FaceApp/test_10MB.jpg";
                    mP2pListener.sendImageFile(path);*/
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==LOAD_A_PIC && resultCode==RESULT_OK && data!=null){
            imageFilePath = data.getStringExtra(Browser.ImageFilePath);
            Bitmap image = bitmaphelper.getProperBitmap(imageFilePath);
            width=image.getWidth();
            height=image.getHeight();
            face.setImageBitmap(image);
        }
    }

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    WiFiP2pListener mP2pListener;
    WiFiP2pServerThread mP2pThread;
    ConnectedPeerInfo peerInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Button load_btn = (Button) findViewById(R.id.load_btn);
        Button detect_btn = (Button) findViewById(R.id.detect_btn);
        Button property_btn = (Button) findViewById(R.id.property_btn);
        load_btn.setOnClickListener(btn_listener);
        detect_btn.setOnClickListener(btn_listener);
        property_btn.setOnClickListener(btn_listener);

        Button search_btn = (Button) findViewById(R.id.search_btn);
        Button send_btn = (Button) findViewById(R.id.send_btn);
        Button offloading_set_btn = (Button) findViewById(R.id.off_setting_btn);
        search_btn.setOnClickListener(btn_listener);
        send_btn.setOnClickListener(btn_listener);
        offloading_set_btn.setOnClickListener(btn_listener);

        Button back_btn = (Button) findViewById(R.id.back_btn);
        back_btn.setOnClickListener(btn_listener);
        Button list_btn = (Button) findViewById(R.id.list_btn);
        list_btn.setOnClickListener(btn_listener);

        Button test_btn1 = (Button) findViewById(R.id.test_btn1);
        test_btn1.setOnClickListener(btn_listener);
        Button test_btn2 = (Button) findViewById(R.id.test_btn2);
        test_btn2.setOnClickListener(btn_listener);

        face = (ImageView) findViewById(R.id.face_image);
        imagevieweffecthelper.addZoomEffect(face);

        peerInfo = new ConnectedPeerInfo();
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mP2pListener = new WiFiP2pListener(mManager,mChannel,this,peerInfo);
        mReceiver = new WiFiP2pDirectBroadcastReceiver(mManager, mChannel, this,mP2pListener);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mP2pThread = new WiFiP2pServerThread(this,peerInfo);
        mP2pThread.start();

        ListView p_list = (ListView) findViewById(R.id.peer_list);
        p_list.setOnItemClickListener(mP2pListener.getOnItemClickListener());
    }
    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        if(mP2pThread!=null){
            mP2pThread.setStatus(false);
            mP2pThread.interrupt();
            mP2pThread=null;
        }
        super.onDestroy();
    }

}
