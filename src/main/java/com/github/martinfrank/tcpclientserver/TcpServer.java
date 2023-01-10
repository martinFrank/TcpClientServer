package com.github.martinfrank.tcpclientserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TcpServer {

    private final ServerMessageReceiver serverMessageReceiver;
    private final ExecutorService executor;
    private ServerSocket socket;
    private final int port;
    private final List<ClientWorker> workers = new ArrayList<>();

    public void close() {
        try {
            socket.close();
            workers.forEach(ClientWorker::close);
        } catch (Exception e) {
            //well, we're done here
        }
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(3, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public void sendToAll(String message) {
        for (ClientWorker worker : workers) {
            worker.send(message);
        }
    }

    public TcpServer(int port, ServerMessageReceiver serverMessageReceiver) {
        this(port, serverMessageReceiver, Executors.newFixedThreadPool(10));
    }

    public TcpServer(int port, ServerMessageReceiver serverMessageReceiver, ExecutorService executor) {
        this.port = port;
        this.serverMessageReceiver = serverMessageReceiver;
        this.executor = executor;
    }

    public void start() {
        Runnable r = createSocketListener();
        executor.submit(r);
//        Thread t = new Thread(r);
//        t.setDaemon(true);
//        t.start();
    }


    private Runnable createSocketListener() {
        return () -> {
            try {
                socket = new ServerSocket(port);
                serverMessageReceiver.notifyUp("" + InetAddress.getLocalHost().getHostAddress());
                while (true) {//this is intended
                    ClientWorker worker = new ClientWorker(socket.accept(), TcpServer.this, executor);
                    workers.add(worker);
                    worker.start();
                    serverMessageReceiver.notifyConnect(worker);
                }
            } catch (IOException e) {
                //will silently be terminated then!
//                throw new RuntimeException(e);
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


}
