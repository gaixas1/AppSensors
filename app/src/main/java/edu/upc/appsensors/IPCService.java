package edu.upc.appsensors;

import android.os.Message;
import android.os.RemoteException;

/**
 * Created by nya-n-co on 04/11/2016.
 */

public class IPCService extends IPC {
    private MainService main;

    public IPCService(MainService main) {
        super();
        this.main = main;
    }

    @Override
    protected void handle(Message msg) {
        main.handle(msg);
    }
}
