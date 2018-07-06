package edu.upc.appsensors;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by nya-n-co on 04/11/2016.
 */

public class MainService extends Service implements SensorEventListener, LocationListener {

    private static final int MAX_SERVER_LOG = 5000;
    private static final int MIN_SERVER_LOG = 500;
    private static final long MIN_SERVER_TIME = 1000;
    private IPCService ipc;


    private LocationManager locationManager;
    private SensorManager sensorManager;

    private boolean hasGPS;
    private ArrayList<Sensor> sensor_list;
    private ArrayList<String> sensor_names;
    private ArrayList<Integer> sensor_types;
    private ArrayList<Integer> sensor_status;
    private TreeMap<String, Integer> sensor_id;

    BufferedWriter log_writer;
    private ServerThread serverT;

    ArrayList<Socket> clientSockets;
    ArrayList<PrintWriter> clientOuts;
    private boolean serverStat;
    private String serverAddress;
    private int serverPort;
    private String serverUsername;
    private String serverPassword;
    private long serverLast;
    private ArrayList serverLog;
    private ReentrantLock serverMut;
    private int serverUntil;

    public MainService() {
        locationManager = null;
        sensorManager = null;
        hasGPS = false;

        sensor_list = new ArrayList<>();
        sensor_names = new ArrayList<>();
        sensor_types = new ArrayList<>();
        sensor_status = new ArrayList<>();
        sensor_id = new TreeMap<>();

        clientSockets = new ArrayList<>();
        clientOuts = new ArrayList<>();

        serverLog = new ArrayList<>();
        serverMut = new ReentrantLock();
        serverUntil = 0;
    }

    @Override
    public void onCreate() {
        System.out.println("Create Service on thread " + Thread.currentThread().getId());
        ipc = new IPCService(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        int sensorId = 0;
        //Initialize GPS info
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            hasGPS = true;
            sensor_list.add(null);
            sensor_names.add("GPS");
            sensor_types.add(0);
            sensor_status.add(AppMessages.LISTEN_METHOD_NO);
            sensor_id.put("GPS", sensorId++);
        }

        //Initialize sensors info
        ArrayList<Sensor> sensors = new ArrayList<>(sensorManager.getSensorList(Sensor.TYPE_ALL));
        for (Sensor s : sensors) {
            sensor_list.add(s);
            sensor_names.add(s.getName());
            sensor_types.add(s.getType());
            sensor_status.add(AppMessages.LISTEN_METHOD_NO);
            sensor_id.put(s.getName(), sensorId++);
        }

        //Check is wifi
        checkWifi();
        System.out.println("WIFI "+ (WiFiReceiver.isWifi? "ON":"OFF"));

        serverT = new ServerThread(this);
    }

    public boolean checkWifi() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WiFiReceiver.isWifi = networkInfo.isConnected();
        return WiFiReceiver.isWifi;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Start Service on thread " + Thread.currentThread().getId());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(log_writer != null) {
            try {
                log_writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("Binded");
        return ipc.mMessenger.getBinder();
    }

    @Override
    public void onLocationChanged(Location location) {
        receiveData(0, new float[]{
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                location.getSpeed()
        });
    }

    private void receiveData(int sensorId, float[] values) {
        if(sensor_status.get(sensorId) == AppMessages.LISTEN_METHOD_SHARE) {
            String lg = sensorId+"\t"+System.currentTimeMillis();
            for(float f : values) {
                lg += "\t"+f;
            }
            lg+="\n";

            if (log_writer != null) {
                try {
                    log_writer.write(lg);
                    log_writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            LinkedList<Integer> removeClients = new LinkedList <>();
            for(int i = 0; i < clientOuts.size(); i++) {
                PrintWriter out = clientOuts.get(i);
                if (out == null || out.checkError()) {
                    removeClients.addFirst(i);
                } else {
                    out.write(lg);
                }
            }
            for(int r : removeClients) {
                try {
                    clientOuts.get(r).close();
                    clientSockets.get(r).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                clientOuts.remove(r);
                clientSockets.remove(r);
            }

            if(serverStat) {
                serverMut.lock();
                if(serverLog.size() < MAX_SERVER_LOG) {
                    serverLog.add(lg);
                }

                if(serverLog.size() >= serverUntil + MIN_SERVER_LOG && SystemClock.currentThreadTimeMillis() > serverLast + MIN_SERVER_TIME) {
                    if(checkWifi()) {
                        serverUntil = serverLog.size();
                        String sensorsList = "";
                        for (int i = 0; i < sensor_id.size(); i++) {
                            sensorsList += i + "\t" + sensor_names.get(i) + "\n";
                        }
                        sensorsList += "\n";
                        ClientThread t = new ClientThread(this, serverAddress, serverPort, serverUsername, serverPassword, serverLog, sensorsList);
                        t.start();
                    }

                }
                serverMut.unlock();
            }
        }
        ipc.send(AppMessages.data(sensorId, values));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor s = event.sensor;
        receiveData(sensor_id.get(s.getName()), event.values);
    }


    public void handle(Message msg) {
        switch (msg.what) {
            case AppMessages.MSG_REGISTER:
                doRegister();
                break;
            case AppMessages.MSG_LOG:
                doLog(msg.arg1 == 1);
                break;
            case AppMessages.MSG_CLIENT:
                doClient(msg.arg1, msg.arg2==1);
                break;
            case AppMessages.MSG_SERVER:
                doServer(msg.getData().getString("address"), msg.arg1,msg.getData().getString("username"),msg.getData().getString("password"), msg.arg2==1);
                break;
            case AppMessages.MSG_LISTEN:
                doListen(msg.arg1, msg.arg2);
                break;
        }
    }

    private void doLog(boolean b) {
        if(b && log_writer == null) {
            String logName = "log_" + SystemClock.currentThreadTimeMillis();
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS), "UPC_Sensors");
                if (!dir.isDirectory()) {
                    if (!dir.mkdirs()) {
                        System.out.println("Log dir fail");
                        //Inform of unlogable??
                        return;
                    }
                }
                System.out.println(dir.getAbsolutePath());

                File file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS), "UPC_Sensors/" + logName + ".csv");

                try {
                    log_writer = new BufferedWriter(new FileWriter(file));
                    for (int i = 0; i < sensor_id.size(); i++) {
                        log_writer.write(i + "\t" + sensor_names.get(i) + "\n");
                    }
                    log_writer.newLine();
                    log_writer.flush();
                } catch (IOException e) {
                    log_writer = null;
                }

            }
        }  else if(!b && log_writer != null) {
            try {
                log_writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            log_writer = null;
        }
    }

    private void doListen(int sensorId, int stat) {
        System.out.println("Listen " + sensorId + " => " + stat);
        int curState = sensor_status.get(sensorId);
        sensor_status.set(sensorId, stat);

        boolean listen = (curState == AppMessages.LISTEN_METHOD_NO && stat != curState);
        boolean unlisten = (stat == AppMessages.LISTEN_METHOD_NO && stat != curState);
        if (sensorId == 0 && hasGPS) {
            // Do gps change
            if (listen) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0, this);
            }else if(unlisten) {
                locationManager.removeUpdates(this);
            }


        } else {
            // Do sensor change
            if(listen) {
                sensorManager.registerListener(this, sensor_list.get(sensorId), SensorManager.SENSOR_DELAY_NORMAL);
            } else if (unlisten) {
                sensorManager.unregisterListener(this, sensor_list.get(sensorId));
            }
        }
        doRegister();
    }

    private void doRegister() {
        ipc.send(AppMessages.sensors(
                (ArrayList<String>) sensor_names.clone(),
                (ArrayList<Integer>) sensor_types.clone(),
                (ArrayList<Integer>) sensor_status.clone()));
    }

    private void doClient(int port, boolean stat) {
        System.out.println("Client at " + port + " :: " + (stat? "START":"STOP"));
        if(stat) {
            serverT.start(port);
        } else {
            serverT.stop();
            for(int i = 0; i < clientSockets.size(); i++) {
                try {
                    clientOuts.get(i).close();
                    clientSockets.get(i).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            clientOuts.clear();
            clientSockets.clear();
        }
    }
    public void setClient(Socket connectionSocket, PrintWriter out) {
        for (int i = 0; i < sensor_id.size(); i++) {
            out.write(i + "\t" + sensor_names.get(i) + "\n");
        }
        out.write("\n");

        clientSockets.add(connectionSocket);
        clientOuts.add(out);

    }

    private void doServer(String address, int port, String username, String password, boolean stat) {
        System.out.println("Send to server at "+address +":"+ port + " :: " + (stat? "START":"STOP"));

        this.serverStat = stat;
        if(stat) {
            this.serverAddress = address;
            this.serverPort = port;
            this.serverUsername = username;
            this.serverPassword = password;
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    public void clearServerLog() {
        serverMut.lock();
        serverLog.subList(0, serverUntil).clear();
        serverLast = SystemClock.currentThreadTimeMillis();
        serverUntil = 0;
        serverMut.unlock();
    }

    public void serverLogError() {
        System.out.println("Error login into server");

        serverMut.lock();
        serverMut.unlock();

    }

    public void serverConnectError() {
        System.out.println("Error connecting to server");
    }
}
