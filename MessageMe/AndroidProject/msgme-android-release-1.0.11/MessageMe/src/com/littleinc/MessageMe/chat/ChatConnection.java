package com.littleinc.MessageMe.chat;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;

/**
 * Defines basic actions to interact with chat service.
 */
public abstract class ChatConnection {

    /**
     * Calling connect() on our web socket is asynchronous, and the
     * WebSocketConnection object only provides an isConnected() method, so 
     * we need to track the current state ourselves.
     */
    public enum WebSocketStatus {
        DISCONNECTED, CONNECTING, RECONNECTING, DISCONNECTING, CONNECTED
    };

    private ChatConnectionListener connectionListener = null;

    private BinaryMessageListener binaryMessageListener = null;        

    private ChatManager chatManager;

    public ChatConnection() {
        LogIt.i(this, "Creating new ChatConnection");
        chatManager = new ChatManager(this);
    }

    public abstract void connect();

    public abstract void disconnect();

    public abstract void reconnect(boolean fastReconnect);

    public abstract void removePendingReconnects();
    
    public abstract void sendCommand(PBCommandEnvelope envelope);

    public abstract void refreshCredentials();

    public abstract void requestAWSTokens(boolean forced);

    public void setConnectionListener(ChatConnectionListener listener) {
        
        if ((listener != null) && (connectionListener != null)) {
            // A single MessagingService should be the only listener.
            LogIt.w(ChatConnection.class,
                    "There was an old ChatConnectionListener that hadn't been removed",
                    connectionListener);
        }
        
        LogIt.d(this, "setConnectionListener", listener);
        connectionListener = listener;
    }

    public void clearConnectionListener() {
        
        if (connectionListener == null) {
            // This would be unexpected, but shouldn't cause any problems
            LogIt.w(ChatConnection.class,
                    "No ChatConnectionListener to remove");
        } else {            
            LogIt.d(this, "clearConnectionListener");
            connectionListener = null;
        }
    }
    
    public ChatConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public void setMessageListener(BinaryMessageListener listener) {
        
        if ((listener != null) && (binaryMessageListener != null)) {
            // A single MessagingService should be the only listener.
            //
            // The normal flow is that the MessagingService will remove itself 
            // as a listener each time it is destroyed, but some scenarios 
            // result in multiple calls to this method.  We log this condition
            // as it caused bugs in the previous implementation.
            LogIt.w(ChatConnection.class,
                    "There was an old MessageListener that hadn't been removed",
                    binaryMessageListener);
        }
        
        LogIt.d(this, "setMessageListener", listener);
        binaryMessageListener = listener;
    }
    
    public void clearMessageListener() {
        
        if (binaryMessageListener == null) {
            // This would be unexpected, but shouldn't cause any problems
            LogIt.w(ChatConnection.class,
                    "No MessageListener to remove");
        } else {            
            LogIt.d(this, "clearMessageListener");
            binaryMessageListener = null;
        }

    }

    /**
     * Return the current binaryMessageListener.  This can return null if no
     * listeners are currently registered.
     */
    public BinaryMessageListener getMessageListener() {
        return binaryMessageListener;
    }

    public abstract void sendPing();

    public abstract boolean isConnected();

    public ChatManager getChatManager() {
        return chatManager;
    }
}