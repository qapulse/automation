package com.littleinc.MessageMe.chat;

import java.util.Date;
import java.util.List;

import org.messageMe.OpenUDID.OpenUDID_manager;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.coredroid.util.LogIt;
import com.facebook.internal.Utility;
import com.google.android.gcm.GCMRegistrar;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeConstants.InAppNotificationTargetScreen;
import com.littleinc.MessageMe.MessageMeExternalAPI;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.ContactMessage;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.LocationMessage;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.MessageMeAppPreferences;
import com.littleinc.MessageMe.bo.MessageMeCursor;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.SongMessage;
import com.littleinc.MessageMe.bo.TextMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.bo.YoutubeMessage;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope.CommandType;
import com.littleinc.MessageMe.protocol.Commands.PBCommandGet;
import com.littleinc.MessageMe.protocol.Commands.PBCommandGet.CommandGetMode;
import com.littleinc.MessageMe.protocol.Commands.PBCommandPresenceUpdate;
import com.littleinc.MessageMe.protocol.Commands.PBCommandPresenceUpdate.OnlineStatus;
import com.littleinc.MessageMe.protocol.Commands.PBCommandUserGet;
import com.littleinc.MessageMe.protocol.Commands.PBCommandUserIdentify;
import com.littleinc.MessageMe.protocol.Objects.PBCursor;
import com.littleinc.MessageMe.protocol.Objects.PBDevice;
import com.littleinc.MessageMe.protocol.Objects.PBError;
import com.littleinc.MessageMe.ui.ChatActivity;
import com.littleinc.MessageMe.ui.MessagesFragment;
import com.littleinc.MessageMe.util.DateUtil;
import com.littleinc.MessageMe.util.NetUtil;
import com.littleinc.MessageMe.util.StringUtil;
import com.littleinc.MessageMe.util.SyncNotificationUtil;

/**
 * Manages communication with Chat service, updates UI through Binder and
 * Broadcast messages and if required alerts user through Notification alerts
 */
public class MessagingService extends Service implements
        ChatConnectionListener, BinaryMessageListener {

    public static final String MESSAGING_SERVICE_CONNECTED = "com.littleinc.MessageMe.MESSAGE_SERVICE_CONNECTED";

    public static final String MESSAGING_SERVICE_DISCONNECTED = "com.littleinc.MessageMe.MESSAGE_SERVICE_DISCONNECTED";

    public static final String INVALID_TOKEN = "com.littleinc.MessageMe.INVALID_TOKEN";

    public static final String SET_NOTIFICATION = "com.littleinc.MessageMe.SET_NOTIFICATION";

    private final IBinder mBinder = new MessagingBinder();

    private static ChatConnection mConnection;

    private MediaManager mediaManager = MediaManager.getInstance();

    private BroadcastReceiver mBroadcastReceiver;

    private PingTask pingTaskInstance;

    private Handler handler = new Handler();

    private DurableCommandSender mDurableSender;

    private CommandReceiver mCommandReceiver;

    private LocalBroadcastManager localBroadcastManager;

    private static final int CNONCE_STRING_LENGTH = 10;

    /**
     * Flag to indicate whether we have sent our challenge response to the
     * server to complete the authentication process.  We must only send
     * messages once we have fully completed authentication, otherwise 
     * the server will close our connection and we'll have to start again.
     */
    private volatile boolean isAuthorized = false;

    @Override
    public void onCreate() {
        super.onCreate();
        LogIt.d(MessagingService.class, "Messaging Service created", this);

        mBroadcastReceiver = new MessagingReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SET_NOTIFICATION);
        intentFilter.addAction(INVALID_TOKEN);

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        SyncNotificationUtil.INSTANCE.init(this);
        mDurableSender = DurableCommandSender.getInstance(this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager
                .registerReceiver(mBroadcastReceiver, intentFilter);

        mCommandReceiver = CommandReceiver.getInstance(this);

        mConnection = WSChatConnection.getInstance(this);

        mConnection.setConnectionListener(this);
        mConnection.connect();
    }

    @Override
    public void onDestroy() {

        LogIt.i(MessagingService.class, "onDestroy()", this);

        super.onDestroy();
        if (localBroadcastManager != null) {
            localBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        }

        disablePingTask();
        mConnection.disconnect();
        mConnection.clearConnectionListener();

        mDurableSender.shutDown();
        mCommandReceiver.shutDown();
    }

    /**
     * This method can be called multiple times.  Code that should only be
     * run once should go into onCreate instead. 
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogIt.d(this, "onStartCommand", this);
        super.onStartCommand(intent, flags, startId);

        return Service.START_STICKY;
    }

    public void sendOnlineStatus(boolean isOnline) {

        PBCommandPresenceUpdate.Builder presenceUpdatBuilder = PBCommandPresenceUpdate
                .newBuilder();

        LogIt.d(MessagingService.class, "Send PRESENCE_UPDATE",
                (isOnline ? OnlineStatus.ONLINE : OnlineStatus.OFFLINE));

        presenceUpdatBuilder.setOnlineStatus(isOnline ? OnlineStatus.ONLINE
                : OnlineStatus.OFFLINE);

        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();

        commandEnvelopeBuilder.setType(CommandType.PRESENCE_UPDATE);
        commandEnvelopeBuilder.setPresenceUpdate(presenceUpdatBuilder);

        try {
            sendCommand(commandEnvelopeBuilder.build());
        } catch (Exception e) {
            LogIt.e(MessagingService.class, e, e.getMessage());
        }
    }

    /**
     * @see BinaryMessageListener#processBinaryMessage(com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope)
     */
    @Override
    public void processBinaryMessage(PBCommandEnvelope envelope) {
        mCommandReceiver.addToQueue(new CommandReceiverEnvelope(envelope));
    }

    /**
     * @see ChatConnectionListener#onConnection()
     */
    @Override
    public void onConnection() {

        SyncNotificationUtil.INSTANCE.cancelSyncNotification();

        LogIt.d(MessagingService.class, "isAuthorized - true");
        isAuthorized = true;

        mConnection.setMessageListener(this);

        mCommandReceiver = CommandReceiver.getInstance(MessagingService.this);

        MessageMeAppPreferences appPreferences = MessageMeApplication
                .getPreferences();
        mediaManager.updateS3ClientCredentials(
                appPreferences.getAwsAccessKey(),
                appPreferences.getAwsSecretKey(),
                appPreferences.getAwsSessionToken());

        notifyChatClient(MESSAGING_SERVICE_CONNECTED);

        boolean areWeDisconnecting = false;

        // There are some cases where the connection only gets set up after
        // the app has been closed or moved into the background.  When need to 
        // check if we are in the background now and schedule that disconnect 
        // timer, otherwise the network may stay alive forever.
        if (!MessageMeApplication.isInForeground(this)) {
            // Check if we need to disconnect now as our disconnect timer is
            // often not reliable
            if (MessageMeApplication.shouldDisconnectNow()) {
                LogIt.i(this,
                        "App not in foreground after onConnection - disconnect now");
                areWeDisconnecting = true;
                shutdownNetworkAndThreads();
            } else {
                LogIt.i(this,
                        "App not in foreground after onConnection - start disconnect timer");
                MessageMeApplication.startDisconnectTimer(this, this);
            }
        } else {
            LogIt.d(this, "App in foreground after onConnection");
        }

        if (!areWeDisconnecting) {
            if (!mConnection.isConnected()) {
                LogIt.d(MessagingService.class,
                        "onConnection - connect not as not connected");
                mConnection.connect();
            } else {
                LogIt.d(MessagingService.class,
                        "onConnection - connected already, schedule ping if required");
                enablePingTask();
            }
        }
    }

    /**
     * @see ChatConnectionListener#onDisconnect(int, String)
     */
    @Override
    public void onDisconnect(int code, String reason) {

        LogIt.d(MessagingService.class, "isAuthorized - false");
        isAuthorized = false;

        notifyChatClient(MESSAGING_SERVICE_DISCONNECTED);

        mCommandReceiver.shutDown();

        disablePingTask();

        User currentUser = MessageMeApplication.getCurrentUser();

        if (currentUser != null) {
            currentUser.setOnlinePresence(false);
        }

        mConnection.clearMessageListener();
    }

    /**
     * Start everything up again that may have been stopped after being in
     * the background for a long time.
     */
    public void connectWebSocketIfNeeded() {
        LogIt.i(this,
                "The UI is visible again, make sure the web socket is open");
        if (!mConnection.isConnected()) {
            SyncNotificationUtil.INSTANCE.showSyncNotification();
        } else {
            SyncNotificationUtil.INSTANCE.cancelSyncNotification();
        }
        mConnection.connect();
        mDurableSender = DurableCommandSender.getInstance(this);
        mDurableSender.createThreadsIfRequired();
    }

    /**
     * If the UI has been in the background for a long time then call this
     * to shut down all the stuff that uses battery.
     * 
     * We don't need to shutdown the CommandReceiver or DurableCommandSender 
     * as they block when there are no commands to receive, so that won't 
     * affect battery life.
     */
    public void shutdownNetworkAndThreads() {

        disablePingTask();
        disconnectWebSocket();

        // Remove any pending reconnects, otherwise they will cause the web
        // socket to be opened up again when they fire
        mConnection.removePendingReconnects();

        // Need to call this method here, in case the disconnect task
        // shuts down the connection
        SyncNotificationUtil.INSTANCE.cancelSyncNotification();
    }

    public void disconnectWebSocket() {
        mConnection.disconnect();
    }

    public synchronized void enablePingTask() {
        if (pingTaskInstance == null) {
            LogIt.d(MessagingService.class, "enablePingTask - schedule ping");
            // This calls schedules the first ping too
            pingTaskInstance = new PingTask(mConnection, handler);
        } else {
            LogIt.d(MessagingService.class,
                    "enablePingTask - ping is already scheduled");
        }
    }

    public synchronized void disablePingTask() {
        if (pingTaskInstance == null) {
            LogIt.d(MessagingService.class,
                    "disablePingTask - no ping task to disable");
        } else {
            LogIt.d(MessagingService.class,
                    "disablePingTask - disable any outstanding pings");
            pingTaskInstance.shutdown();
            pingTaskInstance = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MessagingBinder extends Binder {
        public MessagingService getService() {
            return MessagingService.this;
        }
    }

    public ChatManager getChatManager() {
        return mConnection.getChatManager();
    }

    public DurableCommandSender getDurableCommandSender() {
        return mDurableSender;
    }

    public void addToDurableSendQueue(DurableCommand durableCmd) {

        if (durableCmd.getPBCommandEnvelop().getMessageNew() != null) {
            long recipient = durableCmd.getPBCommandEnvelop().getMessageNew()
                    .getRecipientID();

            if (recipient != MessageMeConstants.WELCOME_ROOM_ID) {
                mDurableSender.addToQueue(durableCmd);
            } else {
                LogIt.d(this, "Don't send messages to the Welcome room");
            }
        } else {
            mDurableSender.addToQueue(durableCmd);
        }
    }

    public boolean sendCommand(final PBCommandEnvelope envelope) {

        if (NetUtil.checkInternetConnection(this) && isConnected()) {

            if ((envelope.getType() == CommandType.USER_IDENTIFY)
                    || isAuthorized) {
                mConnection.sendCommand(envelope);
                return true;
            } else {
                LogIt.w(MessagingService.class,
                        "Cannot send command as not authorized",
                        envelope.getType());
                return false;
            }
        } else {
            LogIt.w(MessagingService.class,
                    "No connection, failed to send message", envelope.getType());
            return false;
        }
    }

    private class MessagingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            LogIt.d(MessagingService.class, "Received intent with action:",
                    action);

            if (INVALID_TOKEN.equals(action)) {
                LogIt.i(MessagingService.class,
                        "INVALID_TOKEN received, refreshing credentials");
                mConnection.refreshCredentials();
            } else if (SET_NOTIFICATION.equals(action)) {
                // TODO check shared preferences . Shared preferences override
                // DEFAULT_NOTIFICATION_VALUE
                LogIt.d(MessagingService.class, "SET_NOTIFICATION received");
            }
        }
    }

    /**
     * This method sends a UserGet command to ask for any updates to user 
     * profile since we were last online.  If there have been any updates then
     * a BATCH of USER_UPDATE commands will be received, which gets handled
     * with our existing code.  The server does not send a USER_GET response,
     * so we don't try to handle it.
     */
    public void sendUserGet() {

        User currentUser = MessageMeApplication.getCurrentUser();

        int lastModified = MessageMeApplication.getPreferences()
                .getUserGetLastModified();

        LogIt.i(MessagingService.class, "Send UserGet", lastModified);

        PBCommandUserGet.Builder userGetBuilder = PBCommandUserGet.newBuilder();

        // We do not need to setUserID here as our own USER_UPDATE commands
        // will always arrive on our personal channel.  This is consistent
        // with iOS.
        userGetBuilder.setModifiedAfter(lastModified);

        PBCommandEnvelope.Builder envelopeBuilder = PBCommandEnvelope
                .newBuilder();

        envelopeBuilder.setType(CommandType.USER_GET);
        envelopeBuilder.setUserGet(userGetBuilder.build());
        envelopeBuilder.setClientID(DateUtil.now().getTime());
        envelopeBuilder.setUserID(currentUser.getUserId());

        try {
            sendCommand(envelopeBuilder.build());
        } catch (Exception e) {
            LogIt.e(MessagingService.class, e, e.getMessage());
        }
    }

    /**
     * This method builds and sends a GET command with all the active cursors
     */
    public void sendGetCommandForActiveCursors() {
        List<MessageMeCursor> cursors = MessageMeCursor.findAllActives();

        LogIt.i(MessagingService.class, "Sending", cursors.size()
                + " active cursors");

        PBCursor.Builder cursorBuilder = null;
        User currentUser = MessageMeApplication.getCurrentUser();
        PBCommandGet.Builder commandGet = PBCommandGet.newBuilder();
        PBCommandEnvelope.Builder envelopeBuilder = PBCommandEnvelope
                .newBuilder();

        for (MessageMeCursor messageMeCursor : cursors) {
            cursorBuilder = PBCursor.newBuilder();

            // Send -1 in the commandId can cause problems in the server
            // and also in the server responses so to avoid that we going to avoid
            // serialize this value
            if (messageMeCursor.getLastCommandId() != -1) {
                cursorBuilder.setCommandID(messageMeCursor.getLastCommandId());
            }

            cursorBuilder.setRecipientID(messageMeCursor.getChannelId());

            commandGet.addCursors(cursorBuilder.build());
        }

        // Only return BATCHes for cursors that were out of date, instead of
        // returning empty BATCHes for them
        commandGet.setMode(CommandGetMode.STALE_ONLY);

        envelopeBuilder.setType(CommandType.GET);
        envelopeBuilder.setGet(commandGet.build());
        envelopeBuilder.setUserID(currentUser.getUserId());
        envelopeBuilder.setClientID(DateUtil.now().getTime());

        try {
            sendCommand(envelopeBuilder.build());
        } catch (Exception e) {
            LogIt.e(MessagingService.class, e, e.getMessage());
        }
    }

    /**
     * Process a GET response.  This callback only happens when our GET is in
     * STALE_ONLY mode.
     */
    void processGetResponse(PBCommandEnvelope envelope, boolean wasInBatch) {
        PBCommandGet commandGetResponse = envelope.getGet();

        LogIt.d(CommandReceiver.class, "GET response for STALE_ONLY mode",
                commandGetResponse.getStaleCursorsCount() + " stale cursors");

        SyncNotificationUtil.INSTANCE
                .setPendingBatchCommandsCount(commandGetResponse
                        .getStaleCursorsCount());
    }

    /**
     * Process a received BATCH with error
     */
    void processBatchWithError(PBCommandEnvelope envelope) {
        PBError error = envelope.getError();
        LogIt.d(MessagingService.class, "BATCH contains error",
                error.getReason());

        switch (error.getCode()) {
        case UNAUTHORIZED:
            // This cursor should be removed
            Contact channel = Contact.newInstance(envelope.getBatch()
                    .getRecipientID());

            LogIt.d(MessagingService.class,
                    "Removing room and disabling cursor",
                    channel.getContactId());

            Conversation.delete(channel.getContactId());
            channel.disableCursor(false);
            channel.delete();

            notifyChatClient(MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST);
            notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST);
            break;
        default:
            break;
        }
    }

    void processUserIdentify(PBCommandEnvelope envelope) {

        PBCommandUserIdentify pbUserUdentify = envelope.getUserIdentify();

        User currentUser = MessageMeApplication.getCurrentUser();

        // It is useful to log the current user so we know which is the
        // personal channel.  We log it here so it only gets logged once.
        LogIt.d(this, "Current user", currentUser.getUserId());

        if (pbUserUdentify.hasValid() && !pbUserUdentify.getValid()) {
            LogIt.w(this, "UserIdentify is invalid, shutting down");
            mCommandReceiver.shutDown();
            // An error occurred during the user authentication process
            // so the app needs to logout.  The USER_IDENTIFIER_INVALID
            // broadcast is used to trigger the logout.
            Bundle extras = new Bundle();
            extras.putBoolean(MessageMeConstants.USER_IDENTIFIER_FAILED, true);
            notifyChatClient(MessageMeConstants.USER_IDENTIFIER_INVALID, extras);
        } else if (pbUserUdentify.hasNonce()) {
            // Process a PBUserIdentifier and sends it to the server.
            //
            // IMPORTANT: we use the Facebook lib Utility.md5hash() instead
            // of the FileSystemUtil.md5(), as the Utility.md5hash() is the only
            // one that returns a md5 hash that the server accepts.
            String cnonce = Utility.md5hash(StringUtil
                    .getRandomString(CNONCE_STRING_LENGTH));

            String nonce = pbUserUdentify.getNonce();

            String authToken = MessageMeApplication.getPreferences().getToken();

            String challengeResponse = Utility.md5hash(String.format("%s%s%s",
                    nonce, authToken, cnonce));

            PBCommandEnvelope.Builder envelopeBuilder = PBCommandEnvelope
                    .newBuilder();
            PBCommandUserIdentify.Builder commandBuilder = PBCommandUserIdentify
                    .newBuilder();

            // No need to setDeviceName for now as it is optional
            PBDevice.Builder deviceBuilder = PBDevice.newBuilder();
            deviceBuilder.setDeviceID(OpenUDID_manager.getOpenUDID());
            deviceBuilder.setDeviceType(PBDevice.DeviceType.DEVICE_ANDROID);
            LogIt.d(this, "Include PBDevice ID in USER_IDENTIFY",
                    OpenUDID_manager.getOpenUDID());
            LogIt.d(this, "USER_IDENTIFY A", nonce);
            LogIt.d(this, "USER_IDENTIFY B", cnonce);
            LogIt.d(this, "USER_IDENTIFY C", authToken);
            LogIt.d(this, "USER_IDENTIFY D", challengeResponse);

            commandBuilder.setDevice(deviceBuilder.build());

            // We include the Nonce in our response so the server can
            // figure out what USER_IDENTIFY we are responding to
            commandBuilder.setNonce(nonce);
            commandBuilder.setCnonce(cnonce);
            commandBuilder.setChallengeResponse(challengeResponse);

            envelopeBuilder.setType(CommandType.USER_IDENTIFY);
            envelopeBuilder.setUserID(currentUser.getUserId());
            envelopeBuilder.setUserIdentify(commandBuilder.build());

            try {
                if (sendCommand(envelopeBuilder.build())) {
                    // We have now authenticated so can start to send commands
                    // to the server.  Don't send our presence here as the
                    // GCMIntentService also opens a web socket connection (to 
                    // register the device with Google), and we don't want that
                    // to affect presence.
                    LogIt.d(MessagingService.class, "isAuthorized - true");
                    isAuthorized = true;

                    mConnection.requestAWSTokens(false);
                    sendUserGet();
                    sendGetCommandForActiveCursors();
                    registerGCM();

                    Date tokenExpiration = MessageMeApplication
                            .getPreferences().getAwsExpirationDate();
                    Date now = new Date();

                    if (tokenExpiration == null || now.after(tokenExpiration)) {
                        LogIt.i(this,
                                "AWS token expired, refreshing credentials");
                        mConnection.refreshCredentials();
                    }
                } else {
                    LogIt.w(this, "Could not send challenge response, give up");
                }

            } catch (Exception e) {
                LogIt.e(this, e, e.getMessage());
            }
        } else {
            LogIt.w(this, "Unrecognized action in USER_IDENTIFY", envelope);
        }
    }

    /**
     * Sometimes the client needs to reconnect to the server so the server
     * can refresh its list of channels that the client needs to receive 
     * commands for.
     * 
     * Eventually the server will be enhanced so this step is not necessary. 
     */
    public void fastReconnect() {
        LogIt.d(MessagingService.class,
                "Reconnect so the server can refresh the client's subscriptions");

        try {
            mConnection.reconnect(true);
        } catch (Exception e) {
            LogIt.e(MessagingService.class, e, e.getMessage());
        }
    }

    private void registerGCM() {
        Context context = getApplicationContext();

        // Check the device supports GCM, and the manifest meets all 
        // the requirements of GCM
        GCMRegistrar.checkDevice(context);
        GCMRegistrar.checkManifest(context);

        final String regId = GCMRegistrar.getRegistrationId(context);

        // We only register for GCM after we've already sent the 
        // USER_IDENTIFY, otherwise the PUSH_TOKEN_NEW will get 
        // rejected by the server, which could break authentication
        if (regId.equals("")) {
            LogIt.i(MessagingService.class, "Register for GCM",
                    MessageMeExternalAPI.SENDER_ID);
            GCMRegistrar.register(context, MessageMeExternalAPI.SENDER_ID);
        } else {
            LogIt.i(MessagingService.class,
                    "Already registered with Google for GCM", regId);

            // Check whether we've actually saved the token.  If not then we 
            // send it to the MessageMe servers in a PUSH_TOKEN_NEW command.
            String gcmToken = MessageMeApplication.getPreferences()
                    .getGcmRegisterId();

            if ((gcmToken == null) || (gcmToken.length() == 0)) {
                // XXX Temporary hack to trigger the re-registration from the 
                // beginning - ideally we would just send the PUSH_TOKEN_NEW 
                // again without re-registering with Google
                LogIt.w(MessagingService.class,
                        "GCM token not registered with MessageMe - trigger it again",
                        gcmToken);
                GCMRegistrar.register(context, MessageMeExternalAPI.SENDER_ID);
            } else {
                LogIt.i(MessagingService.class,
                        "GCM token already registered with MessageMe", gcmToken);
            }
        }
    }

    public void generateInAppNotificationNewMessage(IMessage message) {

        if (message == null) {
            LogIt.w(MessagingService.class,
                    "Ignore generateInAppNotificationNewMessage for null message");
            return;
        }

        String description = null;
        String title = message.getSender().getDisplayName();
        String mediaKey = message.getSender().getProfileImageKey();

        switch (message.getType()) {
        case TEXT:
            description = ((TextMessage) message).getText();
            break;
        case DOODLE:
            description = getString(R.string.notification_doodle_description);
            break;
        case DOODLE_PIC:
            description = getString(R.string.notification_doodlepic_description);
            break;
        case LOCATION:
            description = ((LocationMessage) message).getName();
            break;
        case PHOTO:
            description = getString(R.string.notification_image_description);
            break;
        case SONG:
            String songsName = ((SongMessage) message).getTrackName();
            String artistName = ((SongMessage) message).getArtistName();

            description = getString(R.string.notification_song_description,
                    songsName, artistName);
            break;
        case VIDEO:
            description = getString(R.string.notification_video_description);
            break;
        case VOICE:
            description = getString(R.string.notification_voice_description);
            break;
        case YOUTUBE:
            String videoName = ((YoutubeMessage) message).getVideoTitle();
            description = getString(
                    R.string.notification_video_with_name_description,
                    videoName);
            break;
        case CONTACT:
            description = getString(R.string.notification_contact_description,
                    ((ContactMessage) message).getDisplayName());
            break;
        default:
            // This covers future messages types too
            description = getString(R.string.notification_unsupported_msg_type);
            break;
        }

        if (message.getChannelId() != message.getSender().getContactId()) {
            if (Room.exists(message.getChannelId())) {
                sendInAppNotification(title, description, mediaKey,
                        message.getChannelId(),
                        InAppNotificationTargetScreen.MESSAGE_THREAD);
            }
        } else {
            sendInAppNotification(title, description, mediaKey,
                    message.getChannelId(),
                    InAppNotificationTargetScreen.MESSAGE_THREAD);
        }
    }

    public PBCommandEnvelope buildCommandGet(long recipientId, long commandId) {
        PBCommandEnvelope.Builder commandEnvelopeBuilder = PBCommandEnvelope
                .newBuilder();
        PBCommandGet.Builder commandGetBuilder = PBCommandGet.newBuilder();
        PBCursor.Builder cursorBuilder = PBCursor.newBuilder();

        // Send -1 in the commandId can cause problems in the server
        // and also in the server responses so to avoid that we going to avoid
        // serialize this value
        if (commandId != -1) {
            cursorBuilder.setCommandID(commandId);
        }

        cursorBuilder.setRecipientID(recipientId);

        commandGetBuilder.addCursors(cursorBuilder.build());

        commandEnvelopeBuilder.setType(CommandType.GET);
        commandEnvelopeBuilder.setGet(commandGetBuilder.build());
        commandEnvelopeBuilder.setUserID(MessageMeApplication.getCurrentUser()
                .getUserId());
        commandEnvelopeBuilder.setClientID(DateUtil.now().getTime());

        return commandEnvelopeBuilder.build();
    }

    public boolean isConnected() {
        if (mConnection == null) {
            LogIt.w(MessagingService.class,
                    "isConnected() called on null mConnection, returning false");
            return false;
        } else {
            return mConnection.isConnected();
        }
    }

    /**
     * This method broadcast a notification to update the UI of {@link MessagesFragment}
     * or {@link ChatActivity} and adds the given extras
     */
    public void notifyChatClient(String action, Bundle extras) {
        Intent intent = new Intent(action);

        if (extras != null) {
            intent.putExtras(extras);
            LogIt.i(MessagingService.class, "Sending broadcast with extras",
                    action);
        } else {
            LogIt.i(MessagingService.class, "Sending broadcast", action);
        }

        localBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Broadcast a notification to update the UI of {@link MessagesFragment}
     * or {@link ChatActivity}.  The bundle includes the ID of the channel 
     * that was updated, and the ID of the most recent message.
     */
    public void notifyChatClient(String action, long messageId, long channelId,
            boolean updateUnreadCount) {
        Bundle extras = new Bundle();
        extras.putLong(Message.ID_COLUMN, messageId);
        extras.putLong(Message.CHANNEL_ID_COLUMN, channelId);
        extras.putBoolean(MessageMeConstants.EXTRA_UPDATE_UNREAD_COUNT,
                updateUnreadCount);

        notifyChatClient(action, extras);
    }

    public void notifyChatClient(String action, long messageId, long channelId) {
        // By default do not update the unread count
        notifyChatClient(action, messageId, channelId, false);
    }

    public void notifyChatClient(String action, long channelId,
            boolean updateUnreadCount) {
        Bundle extras = new Bundle();
        extras.putLong(Message.CHANNEL_ID_COLUMN, channelId);
        extras.putBoolean(MessageMeConstants.EXTRA_UPDATE_UNREAD_COUNT,
                updateUnreadCount);

        notifyChatClient(action, extras);
    }

    /**
     * This method broadcast a notification to update the UI of {@link MessagesFragment}
     * or {@link ChatActivity} and creates a bundle that contain the clientId extra
     */
    public void notifyChatClient(String action, long messageId, long channelId,
            byte[] messageInfo, double sortedBy) {

        Bundle extras = new Bundle();

        extras.putLong(Message.ID_COLUMN, messageId);
        extras.putLong(Message.CHANNEL_ID_COLUMN, channelId);
        extras.putByteArray(PBCommandEnvelope.class.getName(), messageInfo);

        // The sortedBy time needs to be included separately as it is
        // a client side only value, and isn't listed in the PBCommandEnvelope
        extras.putDouble(MessageMeConstants.EXTRA_SORTED_BY, sortedBy);

        notifyChatClient(action, extras);
    }

    /**
     * This method broadcast a notification to update the UI of {@link MessagesFragment}
     * or {@link ChatActivity} and creates a bundle that contain the clientId extra
     */
    public void notifyChatClient(String action, long messageId, long channelId,
            double sortedBy) {
        Bundle extras = new Bundle();

        extras.putLong(Message.ID_COLUMN, messageId);
        extras.putLong(Message.CHANNEL_ID_COLUMN, channelId);

        // The sortedBy time needs to be included separately as it is
        // a client side only value, and isn't listed in the PBCommandEnvelope
        extras.putDouble(MessageMeConstants.EXTRA_SORTED_BY, sortedBy);

        notifyChatClient(action, extras);
    }

    /**
     * This method broadcast a notification to update the UI of {@link MessagesFragment}
     * or {@link ChatActivity} without any extra value
     */
    public void notifyChatClient(String action) {
        notifyChatClient(action, null);
    }

    /**
     * @see #sendInAppNotification(String, String, String, long, boolean, LocalBroadcastManager)
     */
    public void sendInAppNotification(String title, String description,
            String imageKey, long contactId,
            InAppNotificationTargetScreen screenToShow) {
        sendInAppNotification(title, description, imageKey, contactId,
                screenToShow, localBroadcastManager);
    }

    /**
     * Display an in-app notification with the provided fields.
     * 
     * @param title the title for the in-app notification
     * @param description the main text of the notification
     * @param imageKey the imageKey to display in the notification
     * @param contactId the contact ID that this notification relates to
     * @param screenToShow an {@link InAppNotificationTargetScreen}
     *                     to say which screen to show the user
     *                     if they touch this notification.
     */
    public static void sendInAppNotification(String title, String description,
            String imageKey, long contactId,
            InAppNotificationTargetScreen screenToShow,
            LocalBroadcastManager broadcastManager) {
        Intent intent = new Intent(
                MessageMeConstants.INTENT_ACTION_SHOW_IN_APP_NOTIFICATION);

        intent.putExtra(MessageMeConstants.EXTRA_TITLE, title);
        intent.putExtra(MessageMeConstants.EXTRA_IMAGE_KEY, imageKey);
        intent.putExtra(MessageMeConstants.EXTRA_CONTACT_ID, contactId);
        intent.putExtra(MessageMeConstants.EXTRA_DESCRIPTION, description);
        intent.putExtra(MessageMeConstants.EXTRA_SCREEN_TO_SHOW,
                screenToShow.ordinal());

        if (broadcastManager == null) {
            LogIt.e(MessagingService.class,
                    "Cannot send in-app notification as localBroadcastManager is null",
                    title, description);
        } else {
            broadcastManager.sendBroadcast(intent);
        }
    }
}