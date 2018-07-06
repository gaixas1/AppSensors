package edu.upc.appsensors;

import android.os.Bundle;
import android.os.Message;

import java.util.ArrayList;

/**
 * Created by nya-n-co on 04/11/2016.
 */

public class AppMessages {
    static public final int MSG_REGISTER    = 0;
    static public final int MSG_LISTEN      = 1;
    static public final int MSG_DATA        = 2;
    static public final int MSG_CLIENT      = 3;
    static public final int MSG_SERVER      = 4;
    static public final int MSG_SENSORS     = 5;
    static public final int MSG_LOG         = 6;

    static public final int LISTEN_METHOD_NO        = 0;
    static public final int LISTEN_METHOD_LISTEN    = 1;
    static public final int LISTEN_METHOD_SHARE     = 2;

    static Message register() {
        Message m = new Message();
        m.what = MSG_REGISTER;
        return m;
    }

    static Message listen(int sensorId, int method) {
        Message m = new Message();
        m.what = MSG_LISTEN;
        m.arg1 = sensorId;
        m.arg2 = method;
        return m;
    }

    static Message data(int sensorId, float[] vals) {
        Message m = new Message();
        m.what = MSG_DATA;
        m.arg1 = sensorId;
        Bundle b = new Bundle();
        b.putFloatArray("values", vals);
        m.setData(b);
        return m;
    }

    static Message client(int port, boolean stat) {
        Message m = new Message();
        m.what = MSG_CLIENT;
        m.arg1 = port;
        m.arg2 = stat? 1:0;
        return m;
    }

    static Message server(String address, int port, String username, String password, boolean stat) {
        Message m = new Message();
        m.what = MSG_SERVER;
        m.arg1 = port;
        m.arg2 = stat? 1:0;
        Bundle b = new Bundle();
        b.putString("address", address);
        b.putString("username", username);
        b.putString("password", password);
        m.setData(b);
        return m;
    }

    static Message sensors (ArrayList<String> name, ArrayList<Integer> type, ArrayList<Integer> status) {
        Message m = new Message();
        m.what = MSG_SENSORS;
        Bundle b = new Bundle();
        b.putStringArrayList("name", name);
        b.putIntegerArrayList("type", type);
        b.putIntegerArrayList("status", status);
        m.setData(b);
        return m;
    }

    public static Message recordLog(boolean rec) {
        Message m = new Message();
        m.what = MSG_LOG;
        m.arg1 = rec? 1:0;
        return m;
    }
}
