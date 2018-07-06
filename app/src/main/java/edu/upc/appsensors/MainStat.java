package edu.upc.appsensors;

import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by nya-n-co on 07/11/2016.
 */

public class MainStat {
    public int currentPage;
    public ArrayList<String> sensor_names;
    public ArrayList<Integer> sensor_types;
    public ArrayList<Integer> sensor_status;

    public boolean log;

    public boolean clientSt;
    public int clientPort;
    public String clientAddrs;

    public boolean serverSt;
    public String serverAddr;
    public String serverUsername;
    public String serverPassword;
    public int serverPort;
    TextView clientAddrsContainer;

    public MainStat(){
        currentPage = -1;
        sensor_names = new ArrayList<>();
        sensor_types = new ArrayList<>();
        sensor_status = new ArrayList<>();

        log = false;

        clientSt = false;
        clientPort = 9999;
        clientAddrs = "";

        serverSt = false;
        serverAddr = "mobilesensors.sans-ac-upc.org";
        serverPort = 9990;
        serverUsername = "user";
        serverPassword = "****";
        clientAddrsContainer= null;
    }

    public void setClientAddrs(String clientAddrs) {
        this.clientAddrs = clientAddrs;
        if(clientAddrsContainer != null) {
            clientAddrsContainer.setText(clientAddrs);
        }
    }

    public void setClientAddrsContainer(TextView clientAddrsContainer) {
        this.clientAddrsContainer = clientAddrsContainer;
        setClientAddrs( WiFiReceiver.getLocalIpAddress());
    }
}
