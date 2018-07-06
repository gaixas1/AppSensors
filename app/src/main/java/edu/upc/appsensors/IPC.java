package edu.upc.appsensors;

import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

/**
 * Created by nya-n-co on 04/11/2016.
 */

public class IPC {

    protected Messenger mClient;
    protected final Messenger mMessenger;

    protected void _handle(Message msg) {
        mClient = msg.replyTo;
        handle(msg);
    }

    protected void handle(Message msg) {}
    protected void send(Message msg) {
        try {
            msg.replyTo = mMessenger;
            mClient.send(msg);
        } catch (RemoteException e) {
            mClient = null;
            System.out.println("Client disconnected");
        }
    }

    IPC() {
        this.mMessenger = new Messenger(new IncomingHandler());
        this.mClient = null;
    }

    public void setMClient(Messenger mClient) {
        this.mClient = mClient;
    }


    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            IPC.this._handle(msg);
        }

    }

}
