package com.github.martinfrank;

import com.github.martinfrank.tcpclientserver.ClientWorker;
import com.github.martinfrank.tcpclientserver.ServerMessageReceiver;
import com.github.martinfrank.tcpclientserver.TcpServer;

import java.util.Scanner;

public class ServerApp {

    public static void main(String[] args){
        ServerMessageReceiver myServerMessageReceiver = new ServerMessageReceiver() {
            @Override
            public void receive(ClientWorker worker, String message) {
                System.out.println("i got a message:"+message+" from "+worker);
            }

            @Override
            public void notifyDisconnect(ClientWorker worker) {
                System.out.println("client disconnected: "+worker);
            }

            @Override
            public void notifyUp(String serverAddress) {
                System.out.println("server is now running at: "+serverAddress);
            }

            @Override
            public void notifyConnect(ClientWorker worker) {
                System.out.println("a new client connected : "+worker);
            }
        };

        TcpServer tcpServer = new TcpServer(4711, myServerMessageReceiver);
        tcpServer.start();

        Scanner scanner = new Scanner(System.in);
        String message = "test";
        while(!"EXIT".equalsIgnoreCase(message)){
            message = scanner.nextLine();
            tcpServer.sendToAll(message);
        }
        tcpServer.close();
    }
}
