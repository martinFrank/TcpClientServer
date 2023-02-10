package com.github.martinfrank.tcpclientserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import static com.github.martinfrank.tcpclientserver.BufferSetting.BUFFER_SIZE;

public class ClientWorker {

    private final Socket socket;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final TcpServer tcpServer;
    private BufferedWriter br;
    private static long workerCount = 0;
    private final long workerId;
    private final ExecutorService executor;

    public ClientWorker(Socket socket, TcpServer tcpServer, ExecutorService executor) {
        this.socket = socket;
        this.tcpServer = tcpServer;
        this.executor = executor;
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
            br.newLine();
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
        executor.submit(r);
    }

    private void createWriter() throws IOException {
        br = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    private void readContinuously(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in), BUFFER_SIZE);
        while(true){
            String line = reader.readLine();
            if(line == null){
                break;
            }
            tcpServer.receive(ClientWorker.this, line);
        }
//        int read;
//        while ((read = in.read(buffer)) != -1) {
//            String output = new String(buffer, 0, read);
//            tcpServer.receive(ClientWorker.this, output);
//        }
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

    public void close() {
        try {
            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
