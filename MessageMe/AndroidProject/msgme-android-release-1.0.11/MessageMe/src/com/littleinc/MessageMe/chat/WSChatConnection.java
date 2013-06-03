package com.littleinc.MessageMe.chat;

import java.util.Date;

import android.os.Handler;
import android.os.Looper;

import com.coredroid.util.LogIt;
import com.google.protobuf.InvalidProtocolBufferException;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConfig;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.protocol.Commands.PBCommandAWSTokenRequest;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope.CommandType;
import com.littleinc.MessageMe.protocol.Objects.PBAWSToken.TokenType;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;

public class WSChatConnection extends ChatConnection {

    private static final String sWebSocketURL = MessageMeApplication
            .getTargetConfig().get(MessageMeConfig.KEY_WS_URL);

    private static final byte[] PING_DATA = new byte[] { 0x0020 };

    private Handler handler = new Handler();

    private static ChatConnection instance;

    private WebSocketConnection wsClient = new WebSocketConnection();

    private WebSocketStatus mStatus = WebSocketStatus.DISCONNECTED;

    /**
     * The handler that receives data from the web socket connection and
     * passes that data to our ChatConnectionListener and 
     * BinaryMessageListener classes.
     */
    private MessageMeWSHandler mMessageMeWSHandler = new MessageMeWSHandler();
    
    private int mReconnectAttempts;
    
    private int mNextReconnectInterval;
    
    private boolean mIsReconnectPending = false;

    /**
     * The last time a pong was received.
     */
    private long pongReceivedTime = System.currentTimeMillis();

    /**
     * The last time we tried to open the web socket connection.
     */
    private long startConnectingTime;

    /**
     * How long to wait while the web socket is connecting or disconnecting
     * before giving up and recreating the web socket connection.
     */
    public static final long WEB_SOCKET_CONNECT_TIMEOUT_MILLIS = 8000;

    /**
     * How long to go without a pong response before reconnecting the 
     * web socket connection.
     */
    public static final long NETWORK_TIMEOUT_INTERVAL = MessageMeConstants.PING_RETRY_INTERVAL * 8;
    
    /**
     * Web socket reconnect intervals.
     */
    public static final int WS_RECONNECT_INTERVAL_MIN_SECS = 1;
    public static final int WS_RECONNECT_INTERVAL_MAX_SECS = 32;
    
    private WSChatConnection() {
        super();
        mReconnectAttempts = 0;
        mNextReconnectInterval = (int) ((WS_RECONNECT_INTERVAL_MIN_SECS
                + (Math.random() + 0.25)) * 1000);
        LogIt.d(this, "Initialize mNextReconnectInterval",
                mNextReconnectInterval);
    }

    /**
     * Ensure there is only a single web socket connection per process.
     * 
     * The GCMIntentService needs its own WSChatConnection when it 
     * first registers with Google so it can send the PUSH_TOKEN_NEW to
     * the MessageMe servers.  Other than that situation we should only
     * have one web socket connection open.
     */
    public static synchronized ChatConnection getInstance(Object caller) {
        if (instance == null) {
            // Make sure we log who we are creating this connection for, as
            // it gets confusing when GCM needs to register too!
            LogIt.d(WSChatConnection.class, "Create WSChatConnection for "
                    + caller, sWebSocketURL);
            instance = new WSChatConnection();
        } else {
            LogIt.d(WSChatConnection.class,
                    "Reuse existing WSChatConnection for " + caller);
        }

        return instance;
    }

    /**
     * Tell the web sockets layer to connect, if it isn't already connected
     * or in the process of connecting.
     * 
     * Once connected, the web socket layer calls us back in 
     * {@link MessageMeWSHandler#onOpen()}
     */
    @Override
    public synchronized void connect() {
        try {
            boolean doConnect = false;
            
            if (mStatus == WebSocketStatus.DISCONNECTED) {
                LogIt.d(WSChatConnection.class, "connect() when DISCONNECTED");
                doConnect = true;
            } else if ((mStatus == WebSocketStatus.CONNECTING)
                    || (mStatus == WebSocketStatus.DISCONNECTING)
                    || (mStatus == WebSocketStatus.RECONNECTING)) {

                long delta = System.currentTimeMillis() - startConnectingTime;

                if (delta > WEB_SOCKET_CONNECT_TIMEOUT_MILLIS) {
                    LogIt.w(WSChatConnection.class,
                            "Web socket connection has been dis/connecting for too long, restart the network layer",
                            mStatus,
                            delta,
                            WEB_SOCKET_CONNECT_TIMEOUT_MILLIS);
                    resetWebSocketConnection(true);
                    doConnect = true;
                } else {
                    LogIt.d(WSChatConnection.class, "connect() called when " +
                            mStatus + ", ignore");
                }
            } else if (mStatus == WebSocketStatus.CONNECTED) {
                LogIt.d(WSChatConnection.class,
                        "connect() called when already CONNECTED, ignore");
            } else {
                LogIt.e(WSChatConnection.class,
                        "connect() called in unexpected state", mStatus);
            }

            if (doConnect) {
                // Check if we have already been in the background for a long 
                // time, if so then don't connect now
                if (!MessageMeApplication.isInForeground()
                        && MessageMeApplication.shouldDisconnectNow()) {
                    LogIt.i(this,
                            "Been in background too long - do not connect and cancel any pending reconnects");
                    removePendingReconnects();
                    return;
                }
                
                LogIt.i(WSChatConnection.class, "Initiate connect",
                        sWebSocketURL);
                mStatus = WebSocketStatus.CONNECTING;
                ++mReconnectAttempts;
                
                // Reset our timers
                startConnectingTime = System.currentTimeMillis();
                pongReceivedTime = System.currentTimeMillis();

                WebSocketOptions wsOptions = new WebSocketOptions();
                wsOptions.setUserAgent(MessageMeApplication.getUserAgent());

                wsClient.connect(sWebSocketURL, mMessageMeWSHandler,
                        wsOptions);
            }
        } catch (WebSocketException e) {
            LogIt.e(WSChatConnection.class, e, e.getMessage());
        }
    }

    /**
     * Once disconnected, the web socket layer calls us back in 
     * {@link MessageMeWSHandler#onClose(int, String)}
     */
    @Override
    public synchronized void disconnect() {
        if (wsClient != null) {
            if ((mStatus == WebSocketStatus.DISCONNECTED)
                    || (mStatus == WebSocketStatus.DISCONNECTING)) {
                // If we call wsClient.disconnect() on a disconnected web 
                // socket then it logs a warning about a RuntimeException:
                //   Handler (de.tavendo.autobahn.WebSocketWriter) 
                //     {4194a040} sending message to a Handler on a dead thread
                LogIt.i(this, "Ignore disconnect as currently " + mStatus);
                return;
            } else if (mStatus == WebSocketStatus.RECONNECTING) {
                // Don't change the status as this tells the onClose
                // handler to reconnect
                LogIt.i(this, "Initiate reconnect");
            } else {
                LogIt.i(this, "Initiate disconnect");
                mStatus = WebSocketStatus.DISCONNECTING;
            }

            User currentUser = MessageMeApplication.getCurrentUser();
            
            try {                
                currentUser.setOnlinePresence(false);
                wsClient.disconnect();
            } catch (Exception e) {
                // When this happens we need to clean up so we can reconnect
                // again using a new socket connection
                LogIt.e(WSChatConnection.class, e,
                        "Exception during wsClient.disconnect(), reset socket");
                resetWebSocketConnection(false);
                
                if (mStatus == WebSocketStatus.RECONNECTING) {
                    LogIt.w(WSChatConnection.class,
                            "Exception during reconnect, connect again");
                    connect();
                }
            }
        } else {
            LogIt.e(this, null, "Can't disconnect, wsClient is null");
        }
    }

    @Override
    public void removePendingReconnects() {
        LogIt.d(this, "Remove pending reconnects");
        mReconnectAttempts = 0;
        handler.removeCallbacks(reConnect);
        mIsReconnectPending = false;
    }

    @Override
    public synchronized void reconnect(boolean fastReconnect) {
        if (wsClient != null) {
            
            if (fastReconnect) {
                // Sometimes we only need to reconnect so the server can
                // update our subscriptions, in this case make it fast!
                LogIt.d(this, "Do a fast reconnect");
                mNextReconnectInterval = 50;
            }
            
            mStatus = WebSocketStatus.RECONNECTING;
            disconnect();
        } else {
            LogIt.e(this, null, "Can't reconnect, wsClient is null");
        }
    }

    /**
     * Force a new web socket connection to be created.  This should only be
     * used when the previous one is believed to be in a bad state.
     */
    public synchronized void resetWebSocketConnection(boolean tryDisconnect) {

        LogIt.i(this, "Force reconnect", sWebSocketURL);
        
        if (tryDisconnect) {            
            try {
                // Do our best to disconnect the old socket before creating a 
                // new web socket, otherwise we might keep receiving messages
                // from the old socket.
                //
                // Only do this if the calling method hasn't already tried it.
                LogIt.i(this, "Try to disconnect the old web socket");
                wsClient.disconnect();
            } catch (Exception e) {
                LogIt.w(WSChatConnection.class, e,
                        "Ignore error during wsClient.disconnect(), as we are about to reset it");
            }
        }
        
        try {
            if (mMessageMeWSHandler != null) {                
                // Ensure we don't keep receiving messages from the old web 
                // socket connection we are discarding
                mMessageMeWSHandler.close();
            }
            
            // Now create a new instance to use
            mMessageMeWSHandler = new MessageMeWSHandler();
            
            wsClient = new WebSocketConnection();
        } catch (RuntimeException re) {
            // If we have been called from within a BackgroundTask or BatchTask
            // then we would need to have called Looper.prepare() before 
            // creating the new WebSocketConnection above.  Unfortunately,
            // there are a lot of different scenarios we are called in, so 
            // we just detect the condition and then call Looper.prepare()
            // only when it was necessary.
            //  java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
            LogIt.w(this,
                    "Handle RuntimeException and try to create the web socket again with a Looper.prepare() call",
                    re, re.getMessage());
            
            try {            
                Looper.prepare();
                wsClient = new WebSocketConnection();
            } catch (Exception ex) {
                LogIt.e(this, ex,
                        "Unexpected error creating new WebSocketConnection after Looper.prepare()");
            }            
        } catch (Exception ex) {
            LogIt.e(this,
                    ex,
                    "Unexpected error creating new WebSocketConnection");
        }
        
        mStatus = WebSocketStatus.DISCONNECTED;
    }

    @Override
    public void sendCommand(PBCommandEnvelope envelope) {
        if ((wsClient == null) || (!wsClient.isConnected())) {
            // This used to occasionally happen during initialization 
            LogIt.w(WSChatConnection.class,
                    "Failed to send message as web socket not connected",
                    wsClient, envelope.getType());
        } else {
            try {
                LogIt.d(WSChatConnection.class, "Sending command",
                        envelope.getType(), envelope.getClientID(),
                        envelope.getCommandID());

                wsClient.sendBinaryMessage(envelope.toByteArray());
            } catch (Exception e) {
                LogIt.e(WSChatConnection.class, e, e.getMessage());
            }
        }
    }

    @Override
    public boolean isConnected() {
        return mStatus == WebSocketStatus.CONNECTED;
    }

    /** 
     * Using Ping functionality extracted from the Autobahn lib
     */
    @Override
    public void sendPing() {
        if ((wsClient == null) || (!wsClient.isConnected())) {
            if (mIsReconnectPending) {
                LogIt.w(this,
                        "wsClient is null, ignore sendPing(), reconnect is already scheduled");
            } else {                
                // Schedule a reconnect as there isn't one already pending
                LogIt.w(this,
                        "wsClient is null, ignore sendPing(), schedule reconnect");
                mIsReconnectPending = true;
                handler.postDelayed(reConnect, getNextReconnectIntervalInMillis());
            }
        } else {
            long time = System.currentTimeMillis();

            if ((time - pongReceivedTime) > NETWORK_TIMEOUT_INTERVAL) {
                LogIt.d(this,
                        "Didn't received pong in a long time, force reconnect");
                resetWebSocketConnection(true);
                connect();
            } else {
                if (!MessageMeApplication.isInForeground()
                        && MessageMeApplication.shouldDisconnectNow()) {
                    // Our disconnect timer is often unreliable so we use 
                    // our ping to trigger the disconnect if too much
                    // time has passed while in the background
                    LogIt.i(this,
                            "Been in background too long - disconnect instead of sending ping");
                    mStatus = WebSocketStatus.DISCONNECTING;
                    wsClient.disconnect();
                } else {                    
                    try {                        
                        LogIt.d(this, "Sending ping", this);
                        wsClient.sendPing(PING_DATA);
                    } catch (Exception e) {
                        // There are timing windows that can result in a 
                        // NullPointerException in the autobahn library:
                        //  de.tavendo.autobahn.WebSocketConnection.sendPing(WebSocketConnection.java:536)
                        //
                        // Ignore them as the network layer was probably being
                        // pulled down.
                        LogIt.e(this, e, "Ignore exception when sending ping");
                    }
                }
            }
        }
    }

    @Override
    public void requestAWSTokens(boolean forced) {

        Date tokenExpiration = MessageMeApplication.getPreferences()
                .getAwsExpirationDate();
        Date now = new Date();

        if (tokenExpiration == null || now.after(tokenExpiration) || forced) {
            PBCommandEnvelope.Builder envelopeBuilder = PBCommandEnvelope
                    .newBuilder();
            PBCommandAWSTokenRequest.Builder commandBuilder = PBCommandAWSTokenRequest
                    .newBuilder();

            commandBuilder.setType(TokenType.S3_USER_UPLOAD);
            envelopeBuilder.setType(CommandType.AWS_TOKEN_REQUEST);
            envelopeBuilder.setAwsTokenRequest(commandBuilder.build());

            LogIt.i(this, "Get new AWS token");

            sendCommand(envelopeBuilder.build());
        }
    }
    
    private class MessageMeWSHandler implements WebSocket.ConnectionHandler {

        private boolean isClosed = false;
        
        public MessageMeWSHandler() {
            LogIt.d(WSChatConnection.class, "Created new MessageMeWSHandler",
                    this);
        }
        
        /** 
         * Close this WebSocket.ConnectionHandler.  This causes all callbacks
         * to be ignored.  This can happen when an old instance of our 
         * network layer is still alive and receives messages (e.g. when we
         * have given up waiting for a ping response and created a new
         * web socket).
         */
        public void close() {
            LogIt.i(WSChatConnection.class, "Close old MessageMeWSHandler",
                    this);
            isClosed = true;
        }
        
        @Override
        public void onTextMessage(String payload) {
        }

        @Override
        public void onBinaryMessage(byte[] payload) {
            if (isClosed) {
                LogIt.w(WSChatConnection.class,
                        "Ignore onBinaryMessage as we are an old MessageMeWSHandler",
                        this);
            } else {
                try {
                    PBCommandEnvelope envelope = PBCommandEnvelope
                            .parseFrom(payload);
    
                    BinaryMessageListener listener = getMessageListener();
                    
                    if (listener == null) {
                        LogIt.w(this, "No MessageListener to pass message to");
                    } else {                    
                        listener.processBinaryMessage(envelope);
                    }
                } catch (InvalidProtocolBufferException e) {
                    LogIt.e(this, e, e.getMessage());
                }
            }
        }

        @Override
        public void onPongReceived(byte[] payload) {
            if (isClosed) {
                LogIt.w(WSChatConnection.class,
                        "Ignore onPongReceived as we are an old MessageMeWSHandler",
                        this);
            } else {                
                pongReceivedTime = System.currentTimeMillis();
                LogIt.d(WSChatConnection.class, "Pong received at: "
                        + pongReceivedTime);
            }
        }
        
        /**
         * Method called when the web socket layer has finished connecting.
         */
        @Override
        public void onOpen() {

            if (isClosed) {
                LogIt.w(WSChatConnection.class,
                        "Ignore onOpen as we are an old MessageMeWSHandler",
                        this);
            } else { 
                mStatus = WebSocketStatus.CONNECTED;
                LogIt.d(WSChatConnection.class, "onOpen", mStatus, this);
    
                // Reset our retry intervals and remove any pending reconnects
                removePendingReconnects();
                
                // Drop the reconnect interval back down
                mNextReconnectInterval = (int) ((WS_RECONNECT_INTERVAL_MIN_SECS
                        + (Math.random() + 0.25)) * 1000);
    
                User currentUser = MessageMeApplication.getCurrentUser();
    
                if (wsClient == null) {
                    // There isn't much we can do here
                    LogIt.w(this, "wsClient is null, ignore onOpen() callback");
                } else if (currentUser == null) {
                    LogIt.w(this, "CurrentUser is null, ignore onOpen() callback");
                } else {
                    if (!wsClient.isConnected()) {
                        LogIt.w(this,
                                "wsClient is not connected, even during onOpen() callback");
                    } else {
                        // Notifying the ChatConnectionListener will start the ping
                        if (getConnectionListener() == null) {
                            LogIt.i(this, "No ChatConnectionListener to notify");
                        } else {                        
                            LogIt.d(this, "Notify ChatConnectionListener");
                            getConnectionListener().onConnection();
                        }
                    }
                }
            }
        }

        /**
         * Method called when the web socket layer has finished disconnecting.
         */
        @Override
        public void onClose(int code, String reason) {

            if (isClosed) {
                LogIt.w(WSChatConnection.class,
                        "Ignore onClose as we are an old MessageMeWSHandler",
                        this);
            } else { 
                boolean needToReconnect = false;
    
                if (mStatus == WebSocketStatus.DISCONNECTING) {
                    // This is our normal disconnect processing, so should
                    // not trigger a reconnect
                    LogIt.i(WSChatConnection.class,
                            "onClose received after disconnect", code, reason);
                } else if (mStatus == WebSocketStatus.RECONNECTING) {
                    LogIt.d(WSChatConnection.class,
                            "onClose received while reconnecting", code, reason);
                    needToReconnect = true;
                } else {
                    LogIt.d(WSChatConnection.class,
                            "Unexpected onClose, schedule reconnect", code,
                            reason, this);
                    needToReconnect = true;
                }
    
                mStatus = WebSocketStatus.DISCONNECTED;
    
                if (getConnectionListener() == null) {
                    LogIt.i(this, "No ChatConnectionListener to notify");
                } else {                        
                    LogIt.d(this, "Notify ChatConnectionListener");
                    getConnectionListener().onDisconnect(code, reason);
                }
    
                if (needToReconnect) {
                    LogIt.d(WSChatConnection.class, "Post reconnect callback");
                    mIsReconnectPending = true;
                    handler.postDelayed(reConnect, getNextReconnectIntervalInMillis());
                }
            }
        }

        @Override
        public void onRawTextMessage(byte[] payload) {
        }

    }

    @Override
    public void refreshCredentials() {
        requestAWSTokens(true);
    }
    
    private int getNextReconnectIntervalInMillis() {
        
        // The code is structured this way so it is easy to change the interval
        // for fast reconnects
        int intervalMillis = mNextReconnectInterval;
        
        // Work out the next reconnect interval to use.  Keep doubling our 
        // retry interval up to a maximum of WS_RECONNECT_INTERVAL_MAX_SECS.
        //
        // Always add a random jitter between 0.25 and 1.25 to ensure the
        // server doesn't get hit with everybody reconnecting at once.
        double delayInSecs = Math.min(WS_RECONNECT_INTERVAL_MAX_SECS,
                Math.pow(2, mReconnectAttempts))
                + (Math.random() + 0.25);
        mNextReconnectInterval = (int) (delayInSecs * 1000);
        
        LogIt.d(WSChatConnection.class, "getNextReconnectIntervalInMillis", intervalMillis);
        
        return intervalMillis;
    }

    private Runnable reConnect = new Runnable() {

        @Override
        public void run() {
            LogIt.d(WSChatConnection.class, "reConnect Runnable running, call connect()");
            connect();
        }
    };
}