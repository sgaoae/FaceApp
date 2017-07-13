package com.example.gaoshenlai.faceapp.utils.p2pconnection;

import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gaoshenlai.faceapp.MainMenu;
import com.example.gaoshenlai.faceapp.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gaoshenlai on 21/2/17.
 */

public class WiFiP2pListener implements WifiP2pManager.PeerListListener,WifiP2pManager.ConnectionInfoListener {
    public static String DEBUG_LOG = "FaceAppDebugLog";
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    AppCompatActivity activity;
    public ConnectedPeerInfo peerInfo;

    long scanBeginTime;
    public void setScanBeginTime(long t){scanBeginTime=t;}

    public WiFiP2pListener
            (WifiP2pManager manager,
                    WifiP2pManager.Channel channel,
                    AppCompatActivity activity,
             ConnectedPeerInfo peerInfo){
        this.manager=manager;
        this.channel=channel;
        this.activity=activity;
        this.peerInfo=peerInfo;
    }

    ArrayList<WifiP2pDevice> devices = new ArrayList<WifiP2pDevice>();
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        Log.d(DEBUG_LOG,"onPeersAvailable");
        devices.clear();
        devices.addAll(peers.getDeviceList());
        Log.d(DEBUG_LOG,devices.size()+" peers found");
        ListView list_view = (ListView) activity.findViewById(R.id.peer_list);
        if(devices.size()==0){
            Toast.makeText(activity.getApplicationContext(),"No Peers Found",Toast.LENGTH_SHORT).show();
            list_view.setAdapter(null);
            String name = (String) ((TextView) activity.findViewById(R.id.device_name)).getText();
            Log.d(MainMenu.EXPERIMENT_LOG,name+"|scan end: "+System.currentTimeMillis()+"|"+devices.size());
            return;
        }
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for(int i=0;i<devices.size();++i){
            Map<String, Object> listmap = new HashMap<String, Object>();
            listmap.put("name",devices.get(i).deviceName);
            listmap.put("status",WiFiP2pDirectBroadcastReceiver.deviceStatusToString(devices.get(i).status));
            list.add(listmap);
        }
        SimpleAdapter simpleadapter = new SimpleAdapter(activity, list, R.layout.peer_list_listview,
                new String[]{"name", "status"}, new int[]{R.id.peer_name, R.id.peer_status});
        list_view.setAdapter(simpleadapter);
        String name = (String) ((TextView) activity.findViewById(R.id.device_name)).getText();
        Log.d(MainMenu.EXPERIMENT_LOG,name+"|scan time: "+(System.currentTimeMillis()-scanBeginTime)+"|"+devices.size());
    }

    public AdapterView.OnItemClickListener getOnItemClickListener(){
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiP2pDevice device = devices.get(position);
                connect(device.deviceAddress,device.deviceName);
            }
        };
        return listener;
    }

    public void connect(String deviceAddress,final String deviceName){
            WifiP2pConfig config = new WifiP2pConfig();
            config.groupOwnerIntent = 0;
            config.deviceAddress = deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(activity.getApplicationContext(), "Connecting to " + deviceName, Toast.LENGTH_SHORT).show();
                    Log.d(DEBUG_LOG,"Connecting to " + deviceName);
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(activity.getApplicationContext(), "Connection Failure(reason code: " + Integer.toString(reason) + ")", Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        Log.d(DEBUG_LOG,info.toString());
        if(info.groupFormed){
            if(!info.isGroupOwner){
                Log.d(DEBUG_LOG,"Not a group owner, IP address obtained");
                Thread temp = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String currentIP,currentDevice;
                        currentIP = info.groupOwnerAddress.getHostAddress();
                        currentDevice = info.groupOwnerAddress.getHostName();
                        peerInfo.addConnectedPeer(currentDevice,currentIP);
                        Intent intent = new Intent(activity,WiFiP2pDataTransfer.class);
                        intent.setAction(WiFiP2pDataTransfer.ACTION_SEND_IP);
                        intent.putExtra(WiFiP2pDataTransfer.EXTRAS_IP_ADDRESS,currentIP);
                        intent.putExtra(WiFiP2pDataTransfer.EXTRAS_PORT,WiFiP2pDataTransfer.PORT);
                        activity.startService(intent);
                        Log.d(DEBUG_LOG,"IP address sent service called");
                    }
                });
                temp.start();
            }
        }
    }

    public void sendImageFile(String path,byte action_type){
        if(path==null || path.equals("")){
            Log.d(DEBUG_LOG,"no imageFilePath cannot sendImageFile()");
            return;
        }
        if(peerInfo.size()==0){
            Log.d(DEBUG_LOG,"no target IP address cannot sendImageFile()");
            return;
        }
        Log.d(DEBUG_LOG,"Image sent: "+path);
        ArrayList<String> ips = peerInfo.getAllIP();
        for(int i=0;i<ips.size();++i){
            String IP = ips.get(i);
            Intent intent = new Intent(activity,WiFiP2pDataTransfer.class);
            intent.setAction(WiFiP2pDataTransfer.ACTION_SEND_FILE);
            intent.putExtra(WiFiP2pDataTransfer.EXTRAS_IP_ADDRESS,IP);
            intent.putExtra(WiFiP2pDataTransfer.EXTRAS_PORT,WiFiP2pDataTransfer.PORT);
            intent.putExtra(WiFiP2pDataTransfer.EXTRAS_FILE_PATH,path);
            intent.putExtra(WiFiP2pDataTransfer.EXTRAS_FILE_ACTION,action_type);
            activity.startService(intent);
            Log.d(DEBUG_LOG,"Target IP:  "+IP);
        }
        Log.d(DEBUG_LOG,"sendImageFile() called successfully");
    }

    public void sendString(String str){
        if(peerInfo.size()==0){
            Log.d(DEBUG_LOG,"no target IP address cannot sendString()");
            return;
        }
        Log.d(DEBUG_LOG,"String sent: "+str);
        ArrayList<String> ips = peerInfo.getAllIP();
        for(int i=0;i<ips.size();++i){
            String IP = ips.get(i);
            Intent intent = new Intent(activity,WiFiP2pDataTransfer.class);
            intent.setAction(WiFiP2pDataTransfer.ACTION_SEND_STRING);
            intent.putExtra(WiFiP2pDataTransfer.EXTRAS_IP_ADDRESS,IP);
            intent.putExtra(WiFiP2pDataTransfer.EXTRAS_PORT,WiFiP2pDataTransfer.PORT);
            intent.putExtra(WiFiP2pDataTransfer.EXTRAS_STRING_DATA,str);
            activity.startService(intent);
            Log.d(DEBUG_LOG,"Target IP:  "+IP);
        }
        Log.d(DEBUG_LOG,"sendString() called successfully");
    }
}
