package com.github.martinfrank;

import com.github.martinfrank.tcpclientserver.ClientMessageReceiver;
import com.github.martinfrank.tcpclientserver.TcpClient;

import java.util.Scanner;

public class ClientApp {

    public static void main(String[] args){
        ClientMessageReceiver serverMessageReceiver = new ClientMessageReceiver() {
            @Override
            public void receive(String message) {
                System.out.println("received: "+message);
            }

            @Override
            public void notifyDisconnect() {
                System.out.println("disconnect");
            }
        };

        TcpClient client = new TcpClient("192.168.0.65", 4711, serverMessageReceiver);
        client.start();

        Scanner scanner = new Scanner(System.in);
        String message = "test";
        while(!"EXIT".equalsIgnoreCase(message)){
            message = scanner.nextLine();
            client.send(message);
        }
    }
}
