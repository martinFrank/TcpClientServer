package com.github.martinfrank.tcpclientserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class TcpServer {

    private final ServerMessageReceiver serverMessageReceiver;
    private ServerSocket socket;
    private final int port;
    private final List<ClientWorker> workers = new ArrayList<>();

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            //well, we're done here
        }
    }

    public void sendToAll(String message) {
        for(ClientWorker worker: workers){
            worker.send(message);
        }
    }

     public TcpServer(int port, ServerMessageReceiver serverMessageReceiver) {
        this.port = port;
        this.serverMessageReceiver = serverMessageReceiver;

    }
    public void start() {
        Runnable r = createSocketListener();        
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }


    private Runnable createSocketListener() {
        return () -> {
            try {
                socket = new ServerSocket(port);
                while(true){//this is intended
                    ClientWorker worker = new ClientWorker(socket.accept(), TcpServer.this);
                    workers.add(worker);
                    worker.start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };                
    }

    void receive(ClientWorker worker, String message) {
        serverMessageReceiver.receive(worker, message);
    }

    void notifyDisconnect(ClientWorker worker) {
        workers.remove(worker);
        serverMessageReceiver.notifyDisconnect(worker);
    }


    public String getAddress() {
        String address = "unknown";
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            address = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            //we have handled that properly!
        }
        return address;
    }
}
