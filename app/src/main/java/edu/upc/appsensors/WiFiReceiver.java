package edu.upc.appsensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by nya-n-co on 07/11/2016.
 */

public class WiFiReceiver extends BroadcastReceiver {
    public static boolean isWifi = false;

    public void onReceive(Context context, Intent intent) {

        context.getSystemService(Context.CONNECTIVITY_SERVICE);
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        isWifi = networkInfo.isConnected();

        System.out.println("WIFI "+ (isWifi? "ON":"OFF"));

        MainActivity.st.setClientAddrs(getLocalIpAddress());
    }

    public static String getLocalIpAddress() {
        String addresses = "";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        addresses += inetAddress.getHostAddress().toString() + "\n";
                    }
                }
            }
        } catch (Exception ex) {}
        return addresses;
    }

}