package com.google.blockly.android.demo;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

public class WifiAccessManager {

    private static final String SSID = "1ba5f6";
    public static boolean setWifiApState(Context context,String SSIDin, String password, boolean enabled) {
        //config = Preconditions.checkNotNull(config);
        try {
            WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (enabled) {
                mWifiManager.setWifiEnabled(false);
            }
            //WifiConfiguration conf = getWifiApConfiguration();

            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID =  SSIDin;
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            conf.preSharedKey = password;

            mWifiManager.addNetwork(conf);

            return (Boolean) mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class).invoke(mWifiManager, conf, enabled);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static WifiConfiguration getWifiApConfiguration() {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID =  SSID;
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.preSharedKey = "374beccc";
        return conf;
    }

    public static WifiConfiguration changeConfig(String SSIDin, String password){

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID =  SSIDin;
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.preSharedKey = password;
        return conf;


    }

}