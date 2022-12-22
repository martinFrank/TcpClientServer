package com.github.martinfrank.tcpclientserver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class TcpClient {

    private final String host;
    private final int port;
    private final ClientMessageReceiver clientMessageReceiver;
    private BufferedWriter br;
    private final byte[] buffer = new byte[256];



    public TcpClient(String host, int port, ClientMessageReceiver clientMessageReceiver) {
        this.host = host;
        this.port = port;
        this.clientMessageReceiver = clientMessageReceiver;
    }

    public void start() {
        try {
            Socket socket = new Socket(host, port);
            createWriter(socket);
            startReader(socket);
        }catch (IOException e){
            clientMessageReceiver.notifyDisconnect();
        }
    }

    private void startReader(Socket socket) {
        Runnable r = () -> {
            try (InputStream in = socket.getInputStream()){
                readContinuously(in);
            } catch (IOException e) {
                //throw new RuntimeException(e);
                clientMessageReceiver.notifyDisconnect();
            }
        };
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.start();
    }

    private void readContinuously(InputStream in) throws IOException {
        int read;
        while ((read = in.read(buffer)) != -1) {
            String output = new String(buffer, 0, read);
            clientMessageReceiver.receive(output);
        }
    }

    private void createWriter(Socket socket) throws IOException {
        br = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void send(String message){
        try {
            br.write(message);
            br.flush();
        } catch (IOException e) {
//            throw new RuntimeException(e);
            clientMessageReceiver.notifyDisconnect();
        }
    }
}
