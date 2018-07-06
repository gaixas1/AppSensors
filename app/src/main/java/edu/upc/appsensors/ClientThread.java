package edu.upc.appsensors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * Created by nya-n-co on 13/11/2016.
 */
public class ClientThread implements Runnable {
    private final String address;
    private final int port;
    private final String username;
    private final String password;
    private final MainService main;
    private String log;
    private final String welcome;

    public ClientThread(MainService mainService, String serverAddress, int serverPort, String serverUsername, String serverPassword, ArrayList<String> serverLog, String sensorsList) {
        this.main = mainService;

        this.address = serverAddress;
        this.port = serverPort;
        this.username = serverUsername;
        this.password = serverPassword;

        this.welcome = sensorsList;
        this.log = "";
        for(String l : serverLog) {
            this.log += l;
        }
    }

    public void start() {
        Thread t = new Thread(this);
        t.start();
    };

    @Override
    public void run() {
        Socket socket = null;
        try {
            socket = new Socket(address, port);
            socket.setSoTimeout(2000);
        } catch (SocketException e) {
            System.out.println("Cannot set socket timeout");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cannot connect to port "+ port);
        }
        main.clearServerLog();

        if(socket== null || socket.isClosed()) {
            main.serverConnectError();
            return;
        }

        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("LOGIN\t"+username+"\t"+password);
            out.flush();

            String response = in.readLine();
            if(response == "ABORT") {
                System.out.println("Server aborted connection");
                socket.close();
                main.serverLogError();
                return;
            }

            out.print(welcome);
            out.print(log);
            out.print("END");
            out.flush();
            out.close();
        }  catch (SocketTimeoutException s) {
            s.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
