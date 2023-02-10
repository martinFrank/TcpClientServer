package com.github.martinfrank.tcpclientserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.martinfrank.tcpclientserver.BufferSetting.BUFFER_SIZE;

public class TcpClient {

    private final String host;
    private final int port;
    private final ClientMessageReceiver clientMessageReceiver;
    private final ExecutorService executor;
    private BufferedWriter br;
    private final byte[] buffer = new byte[BUFFER_SIZE];
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
            clientMessageReceiver.notifyDisconnect();
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
                //throw new RuntimeException(e);
                clientMessageReceiver.notifyDisconnect();
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
        clientMessageReceiver.notifyDisconnect();
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
//            throw new RuntimeException(e);
            clientMessageReceiver.notifyDisconnect();
        }
    }


    public void close() {
        try {
            socket.close();
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
}
