package com.github.martinfrank.tcpclientserver;

public interface ClientMessageReceiver {

    void receive(String message) ;

    void notifyDisconnect() ;
}
