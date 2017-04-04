package com.example.gaoshenlai.faceapp.utils.p2pconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gaoshenlai.faceapp.R;

/**
 * Created by gaoshenlai on 9/2/17.
 */

public class WiFiP2pDirectBroadcastReceiver extends BroadcastReceiver {
    String DEBUG_LOG = "FaceAppDebugLog";
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private AppCompatActivity mActivity;
    private WiFiP2pListener p2pListener;

    public WiFiP2pDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       AppCompatActivity activity,
                                          WiFiP2pListener p2pListener) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
        this.p2pListener = p2pListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
            } else {
                // Wi-Fi P2P is not enabled
                Toast.makeText(mActivity.getApplicationContext(),"WiFiP2p Not Enabled",Toast.LENGTH_SHORT).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            mManager.requestPeers(mChannel,p2pListener);
            Log.d(DEBUG_LOG,"requestpeer");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection information
                mManager.requestConnectionInfo(mChannel,p2pListener);
            } else if(
                    networkInfo.getDetailedState()== NetworkInfo.DetailedState.DISCONNECTED
                    || networkInfo.getDetailedState()== NetworkInfo.DetailedState.DISCONNECTING
                    ) {
                // It's a disconnect
                mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(mActivity.getApplicationContext(),"It is a disconnect",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            ((TextView)mActivity.findViewById(R.id.device_name)).setText(device.deviceName);
            ((TextView)mActivity.findViewById(R.id.connection_status)).setText(deviceStatusToString(device.status));
        }
    }

    public static String deviceStatusToString(int status){
        switch(status) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown Status";
        }
    }
}
