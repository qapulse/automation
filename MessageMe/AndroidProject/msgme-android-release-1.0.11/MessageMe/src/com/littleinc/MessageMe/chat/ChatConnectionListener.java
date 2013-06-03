package com.littleinc.MessageMe.chat;


/**
 * Allows UI components to listen chat events like successful connection, disconnection, or messages reception
 */
public interface ChatConnectionListener {
    public void onConnection();

    public void onDisconnect(int code, String reason);
}
