package com.github.martinfrank.tcpclientserver;

public interface ServerMessageReceiver {

    void receive(ClientWorker worker, String message);

    void notifyDisconnect(ClientWorker worker);


}
