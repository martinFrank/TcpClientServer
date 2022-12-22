package com.github.martinfrank.tcpclientserver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientWorker {

    private final Socket socket;
    private final byte[] buffer = new byte[256];
    private final TcpServer tcpServer;
    private BufferedWriter br;
    private static long workerCount = 0;
    private final long workerId;

    public ClientWorker(Socket socket, TcpServer tcpServer) {
        this.socket = socket;
        this.tcpServer = tcpServer;
        workerId = getNextWorkerId();
    }

    public void start() {
        try{
            createWriter();
            startReader();
        }catch (IOException e){
            tcpServer.notifyDisconnect(ClientWorker.this);
        }
    }

    public void send(String message){
        try {
            br.write(message);
            br.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long getId(){
        return workerId;
    }

    private void startReader() {
        Runnable r = () -> {
            try (InputStream in = socket.getInputStream()){
                readContinuously(in);
                tcpServer.notifyDisconnect(ClientWorker.this);
            } catch (IOException e) {
                tcpServer.notifyDisconnect(ClientWorker.this);
                //we are done here, disconnected!
            }
        };
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.start();
    }

    private void createWriter() throws IOException {
        br = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    private void readContinuously(InputStream in) throws IOException {
        int read;
        while ((read = in.read(buffer)) != -1) {
            String output = new String(buffer, 0, read);
            tcpServer.receive(ClientWorker.this, output);
        }
    }

    private long getNextWorkerId() {
        workerCount = workerCount + 1;
        return workerCount;
    }

    @Override
    public String toString(){
        return "Client{"+
                "id="+workerId+
                ", "+ socket.getInetAddress().toString()+
                "}";
    }
}
