package com.github.martinfrank.tcpclientserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.martinfrank.tcpclientserver.BufferSetting.BUFFER_SIZE;

public class TcpClient {

    private final String host;
    private final int port;
    private final ClientMessageReceiver clientMessageReceiver;
    private final ExecutorService executor;
    private BufferedWriter br;
    private Socket socket;



    public TcpClient(String host, int port, ClientMessageReceiver clientMessageReceiver) {
        this.host = host;
        this.port = port;
        this.clientMessageReceiver = clientMessageReceiver;
        executor = Executors.newSingleThreadExecutor();
    }

    public TcpClient(String host, int port, ClientMessageReceiver clientMessageReceiver, ExecutorService executor) {
        this.host = host;
        this.port = port;
        this.clientMessageReceiver = clientMessageReceiver;
        this.executor = executor;
    }

    public void start() {
        try {
            socket = new Socket(host, port);
            createWriter();
            startReader();
        }catch (IOException e){
            clientMessageReceiver.notifyDisconnect(e);
        }
    }

    private void startReader() {
        Runnable r = createReaderRunnable();
        executor.submit(r);
    }

    private Runnable createReaderRunnable() {
        return () -> {
            try (InputStream in = socket.getInputStream()){
                readContinuously(in);
            } catch (IOException e) {
                clientMessageReceiver.notifyDisconnect(e);
            }
        };
    }

    private void readContinuously(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in), BUFFER_SIZE);
        while(true){
            String line = reader.readLine();
            if(line == null){
                break;
            }
            clientMessageReceiver.receive(line);
        }
        clientMessageReceiver.notifyDisconnect(new EOFException("end of server stream reached"));
    }

    private void createWriter() throws IOException {
        br = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void send(String message){
        try {
            br.write(message);
            br.newLine();
            br.flush();
        } catch (IOException e) {
            clientMessageReceiver.notifyDisconnect(e);
        }
    }


    public void close() {
        try {
            socket.close();
        } catch (Exception e) {
            //well, we're done here
        }
        ExecutorCloser.close(executor);

    }
}
