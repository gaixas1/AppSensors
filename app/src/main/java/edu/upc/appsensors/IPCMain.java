package edu.upc.appsensors;

import android.os.Message;

/**
 * Created by nya-n-co on 04/11/2016.
 */

public class IPCMain extends IPC {
    private MainActivity main;

    public IPCMain(MainActivity main) {
        super();
        this.main = main;
    }

    @Override
    protected void handle(Message msg) {
        main.handle(msg);
    }
}
