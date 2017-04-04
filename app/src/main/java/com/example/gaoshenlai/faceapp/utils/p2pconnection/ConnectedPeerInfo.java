package com.example.gaoshenlai.faceapp.utils.p2pconnection;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by gaoshenlai on 4/4/17.
 */

public class ConnectedPeerInfo {
    class Peer{
        public Peer(String ip){
            IP=ip;
        }
        public String IP;
    }
    public static String DEBUG_LOG = "FaceAppDebugLog";

    HashMap<String, Peer> map;

    public ConnectedPeerInfo(){
        map=new HashMap<>();
    }
    public void addConnectedPeer(String address,String ip){
        Peer peer = new Peer(ip);
        map.put(address,peer);
        Log.d(DEBUG_LOG,"addConnectedPeer: address: "+address+" | IP: "+ip);
    }
    public void removeConnectedPeer(String address){
        map.remove(address);
    }
    public void clearAllConnectedPeers(){
        map.clear();
    }
    public int size(){
        return map.size();
    }
    public ArrayList<String> getAllIP(){
        ArrayList<String> iplist = new ArrayList<>();
        for(String key:map.keySet()){
            Peer temppeer = map.get(key);
            iplist.add(temppeer.IP);
            Log.d(DEBUG_LOG,"getAllIP: IP: "+temppeer.IP);
        }
        return iplist;
    }
}
