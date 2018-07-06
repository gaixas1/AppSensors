package edu.upc.appsensors;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Created by nya-n-co on 28/10/2016.
 */

public class ServerThread implements Runnable {
    private MainService main;

    private int port;

    private static Thread t;
    private boolean live;


    public ServerThread(MainService service) {
        main = service;
        t = new Thread(this);
        live = false;
        this.port = 9999;
    };

    public void start(int port) {
        this.port = port;
        live = true;
        t.start();
    };

    public void stop() {
        this.live = false;
    }

    @Override
    public void run() {
        ServerSocket welcomeSocket = null;
        try {
            welcomeSocket = new ServerSocket(port);
            welcomeSocket.setSoTimeout(2000);
        } catch (SocketException e) {
            System.out.println("Cannot set socket timeout");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cannot connect to port "+ port);
        }

        if(welcomeSocket== null || welcomeSocket.isClosed()) {
            return;
        }

        while(true) {
            try {
                Socket connectionSocket = welcomeSocket.accept();
                PrintWriter out = new PrintWriter(connectionSocket.getOutputStream(), true);
                main.setClient(connectionSocket, out);
            } catch (SocketTimeoutException s) {
                if(!this.live) {
                    break;
                }
            } catch (IOException e) {
                System.out.println("IOException exception");
                System.out.println(e.getMessage());
                e.printStackTrace();
                break;
            }
        }

        try {
            System.out.println("Close server socket");
            welcomeSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
