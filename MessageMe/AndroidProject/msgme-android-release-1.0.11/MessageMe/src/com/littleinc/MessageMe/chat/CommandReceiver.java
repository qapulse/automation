package com.littleinc.MessageMe.chat;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.os.Bundle;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.actions.CommandGetAction;
import com.littleinc.MessageMe.actions.MessageNewAction;
import com.littleinc.MessageMe.actions.MessageReadAction;
import com.littleinc.MessageMe.actions.PresenceUpdateAction;
import com.littleinc.MessageMe.actions.PushTokenNewAction;
import com.littleinc.MessageMe.actions.RoomClearAction;
import com.littleinc.MessageMe.actions.RoomJoinAction;
import com.littleinc.MessageMe.actions.RoomJumpAction;
import com.littleinc.MessageMe.actions.RoomLeaveAction;
import com.littleinc.MessageMe.actions.RoomNewAction;
import com.littleinc.MessageMe.actions.RoomUpdateAction;
import com.littleinc.MessageMe.actions.UserBlockAction;
import com.littleinc.MessageMe.actions.UserFriendAction;
import com.littleinc.MessageMe.actions.UserJoinAction;
import com.littleinc.MessageMe.actions.UserUnblockAction;
import com.littleinc.MessageMe.actions.UserUnfriendAction;
import com.littleinc.MessageMe.actions.UserUpdateAction;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.protocol.Commands.PBCommandAWSTokenReply;
import com.littleinc.MessageMe.protocol.Commands.PBCommandBatch;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope.CommandType;
import com.littleinc.MessageMe.util.BatchTask;
import com.littleinc.MessageMe.util.DateUtil;
import com.littleinc.MessageMe.util.SyncNotificationUtil;

public class CommandReceiver implements Runnable {

    /**
     * The FIFO process queue for handling received commands. 
     */
    BlockingQueue<CommandReceiverEnvelope> mProcessQueue = new LinkedBlockingQueue<CommandReceiverEnvelope>();

    /** Singleton instance of the CommandReceiver. */
    private static CommandReceiver sInstance = null;

    /** The thread that runs the CommandReceiver. */
    private Thread mReceiveThread = null;

    /** Flag for shutting down our processing thread. */
    private volatile boolean mShutdownRequested = false;

    private MessagingService mMessagingService;

    private CommandReceiver(MessagingService messagingService) {

        LogIt.d(this, "Create CommandReceiver");
        mMessagingService = messagingService;
        createThreadsIfRequired();
    }

    /**
     * This may need to be done again later if the threads have been stopped,
     * e.g. if somebody has stopped the MessagingService through the Android 
     * Settings page.
     */
    private synchronized void createThreadsIfRequired() {

        if (mReceiveThread == null) {
            LogIt.d(this, "Create the message receiving thread");
            mShutdownRequested = false;
            mReceiveThread = new Thread(this);
            mReceiveThread.start();
        }
    }

    public static synchronized CommandReceiver getInstance(
            MessagingService messagingService) {
        if (sInstance == null) {
            LogIt.d(CommandReceiver.class, "Creating singleton CommandReceiver");
            sInstance = new CommandReceiver(messagingService);
        } else if (messagingService != null) {
            LogIt.d(CommandReceiver.class, "Updating MessagingService instance");
            sInstance.mMessagingService = messagingService;
        }

        return sInstance;
    }

    public static CommandReceiver getInstance() {
        if (sInstance == null) {
            // This would indicate a programming error
            LogIt.w(CommandReceiver.class,
                    "getInstance() called before CommandReceiver has been created");
        }

        return sInstance;
    }

    /** 
     * Add a PBCommandEnvelope to the receive queue.
     */
    public void addToQueue(CommandReceiverEnvelope crEnvelope) {

        try {
            mProcessQueue.put(crEnvelope);
        } catch (InterruptedException ex) {
            LogIt.w(CommandReceiver.class,
                    "Error adding command to receive queue", crEnvelope
                            .getPBCmdEnvelope().getType(), crEnvelope
                            .getPBCmdEnvelope().getCommandID());
        }

        createThreadsIfRequired();
    }

    /**
     * Count of pending BATCH commands
     */
    public int getCountPendingBatchs() {
        int count = 0;
        for (CommandReceiverEnvelope crEnvelope : mProcessQueue) {
            PBCommandEnvelope envelope = crEnvelope.getPBCmdEnvelope();

            if (CommandType.BATCH == envelope.getType()
                    && envelope.getBatch().getCommandsCount() > 0) {
                count++;
            }
        }

        return count;
    }

    @Override
    public void run() {
        LogIt.d(this, "Starting CommandReceiver processing thread");

        while (!mShutdownRequested && !Thread.currentThread().isInterrupted()) {
            try {
                // This take() call blocks until a PBCommandEnvelope is
                // added to the queue
                consume(mProcessQueue.take());

            } catch (InterruptedException ex) {
                // A thread will only receive an InterruptedException if it was 
                // blocking at the time of interrupt
                LogIt.i(this, "InterruptedException on receive queue");
            }
        }

        LogIt.i(this, "Shutting down CommandReceiver thread");
    }

    private void consume(final CommandReceiverEnvelope crEnvelope) {

        final PBCommandEnvelope envelope = crEnvelope.getPBCmdEnvelope();

        if (mMessagingService == null) {
            // In order to avoid commands to be processed in wrong order this command
            // will be added in front of the queue
            BlockingQueue<CommandReceiverEnvelope> tempQueue = new LinkedBlockingQueue<CommandReceiverEnvelope>();
            mProcessQueue.drainTo(tempQueue);

            LogIt.w(CommandReceiver.class, "Adding", envelope.getType(),
                    "back to queue");
            addToQueue(crEnvelope);
            mProcessQueue.addAll(tempQueue);
        } else {
            final DurableCommandSender durableSender = DurableCommandSender
                    .getInstance(mMessagingService);

            new BatchTask() {

                @Override
                public void work() {

                    if (!envelope.getDuplicate() && !envelope.hasError()) {

                        User currentUser = MessageMeApplication
                                .getCurrentUser();

                        if (currentUser == null) {
                            LogIt.w(CommandReceiver.class,
                                    "Null currentUser, ignore message as user is probably logging out");
                            return;
                        }

                        switch (envelope.getType()) {
                        case AWS_TOKEN_REPLY:
                            // Access keys received
                            LogIt.d(CommandReceiver.class, "AWS_TOKEN_REPLY");
                            PBCommandAWSTokenReply reply = envelope
                                    .getAwsTokenReply();
                            String accessKey = reply.getToken().getAccessKey();
                            String secretKey = reply.getToken().getSecretKey();
                            String sessionToken = reply.getToken()
                                    .getSessionToken();

                            MediaManager.getInstance()
                                    .updateS3ClientCredentials(accessKey,
                                            secretKey, sessionToken);
                            break;
                        case BATCH:
                            // When first connecting to the server the commands are always received in a BATCH

                            // Shows the Sync Notification in the status bar
                            SyncNotificationUtil.INSTANCE
                                    .showSyncNotification();

                            // Checks for the count of pending batch commands, if
                            // the count is zero, then remove the sync notification
                            if (SyncNotificationUtil.INSTANCE
                                    .getPendingBatchCommandCount() - 1 > 0) {
                                SyncNotificationUtil.INSTANCE
                                        .removePendingBatch();
                            } else {
                                SyncNotificationUtil.INSTANCE
                                        .cancelSyncNotification();
                            }

                            final PBCommandBatch commandBatch = envelope
                                    .getBatch();
                            final List<PBCommandEnvelope> commands = commandBatch
                                    .getCommandsList();

                            long channelID = commandBatch.getRecipientID();

                            LogIt.d(CommandReceiver.class, "BATCH", channelID,
                                    commands.size() + " commands");

                            if (envelope.hasError()) {
                                mMessagingService
                                        .processBatchWithError(envelope);
                                return;
                            } else if (commands.size() == 0) {
                                LogIt.d(CommandReceiver.class,
                                        "The channel is already up to date");
                                SyncNotificationUtil.INSTANCE
                                        .cancelSyncNotification();
                                return;
                            }

                            boolean shouldUpdateChatThread = false;
                            boolean shouldUpdateContactsList = false;
                            boolean shouldUpdateUnreadCount = false;
                            PBCommandEnvelope lastCommandEnvelope = null;

                            // When the Messages list needs to be updated, if the
                            // only thing that has changed is the most recent 
                            // message then we pass that with our intent 
                            boolean shouldUpdateMessagesList = false;
                            IMessage lastMsgInConversation = null;

                            boolean isLoadingEarlierMessages = crEnvelope
                                    .isCmdFromLoadEarlierMessages();

                            // Set the sortedBy value so each message will show above
                            // the existing local message that was previously at the top,
                            // even if those messages were loaded from the server.
                            //
                            // This design is copied from iOS as a pragmatic way to get
                            // the UI ordering to work reliably.
                            double nextSortedBy;

                            if (isLoadingEarlierMessages) {
                                nextSortedBy = crEnvelope
                                        .getSortedByTimestamp()
                                        - ((double) commands.size());
                            } else {
                                // Increase the granularity of our sortedBy time down to 
                                // fake microseconds to match iOS
                                nextSortedBy = DateUtil.getCurrentTimeMicros();
                            }

                            Conversation conversation = null;

                            if (currentUser.getUserId() == channelID) {
                                LogIt.d(this, "Private user channel");
                            } else {
                                conversation = Conversation
                                        .newInstance(channelID);
                            }

                            for (PBCommandEnvelope envelopeInBatch : commands) {

                                switch (envelopeInBatch.getType()) {
                                case GET:
                                    CommandGetAction getAction = new CommandGetAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true).build();
                                    getAction.process();
                                    break;
                                case MESSAGE_DELETE:
                                    LogIt.d(CommandReceiver.class,
                                            "MESSAGE_DELETE in batch - not yet implemented");
                                    break;
                                case MESSAGE_NEW:
                                    shouldUpdateChatThread = true;
                                    shouldUpdateUnreadCount = true;
                                    shouldUpdateMessagesList = true;

                                    MessageNewAction action = new MessageNewAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true)
                                            .nextSortedBy(nextSortedBy)
                                            .conversation(conversation).build();

                                    lastMsgInConversation = action.process();
                                    break;
                                case MESSAGE_READ:
                                    shouldUpdateChatThread = true;
                                    shouldUpdateUnreadCount = true;
                                    shouldUpdateMessagesList = true;
                                    lastMsgInConversation = null;

                                    MessageReadAction readAction = new MessageReadAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true)
                                            .conversation(conversation).build();
                                    readAction.process();
                                    break;
                                case PRESENCE_UPDATE:
                                    PresenceUpdateAction presenceUpdateAction = new PresenceUpdateAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true).build();
                                    presenceUpdateAction.process();
                                    break;
                                case PUSH_TOKEN_NEW:
                                    LogIt.d(CommandReceiver.class,
                                            "PUSH_TOKEN_NEW in batch");
                                    PushTokenNewAction pushTokenNewAction = new PushTokenNewAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true).build();
                                    pushTokenNewAction.process();
                                    break;
                                case ROOM_JOIN:
                                    // A room notice needs to be displayed
                                    shouldUpdateChatThread = true;
                                    shouldUpdateUnreadCount = true;

                                    // The room notice will now be the newest
                                    // message in the room so the Messages tab
                                    // needs to display it.  We don't need to 
                                    // update Contacts as a ROOM_NEW will arrive
                                    // first for any room which we are added to.
                                    shouldUpdateMessagesList = true;

                                    RoomJoinAction roomJoinAction = new RoomJoinAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true)
                                            .nextSortedBy(nextSortedBy)
                                            .conversation(conversation).build();

                                    lastMsgInConversation = roomJoinAction
                                            .process();
                                    break;
                                case ROOM_JUMP:
                                    // Everything about the room could have
                                    // changed
                                    shouldUpdateContactsList = true;
                                    shouldUpdateChatThread = true;
                                    shouldUpdateUnreadCount = true;
                                    shouldUpdateMessagesList = true;
                                    lastMsgInConversation = null;

                                    RoomJumpAction roomJumpAction = new RoomJumpAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true)
                                            .conversation(conversation).build();
                                    roomJumpAction.process();
                                    break;
                                case ROOM_LEAVE:
                                    // A room notice needs to be displayed
                                    shouldUpdateChatThread = true;
                                    shouldUpdateUnreadCount = true;

                                    // This room might need to be removed from
                                    // the UI if the current user left it.
                                    shouldUpdateContactsList = true;
                                    shouldUpdateMessagesList = true;

                                    // If somebody other than the current user
                                    // left the room then we'll only need to 
                                    // update the last message instead of 
                                    // removing the conversation from the UI.
                                    RoomLeaveAction roomLeaveAction = new RoomLeaveAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true)
                                            .nextSortedBy(nextSortedBy)
                                            .conversation(conversation).build();
                                    lastMsgInConversation = roomLeaveAction
                                            .process();
                                    break;
                                case ROOM_NEW:
                                    // The new room needs to show up in the
                                    // Messages and Contacts screens.
                                    shouldUpdateContactsList = true;
                                    shouldUpdateMessagesList = true;
                                    lastMsgInConversation = null;

                                    RoomNewAction roomNewAction = new RoomNewAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true)
                                            .nextSortedBy(nextSortedBy)
                                            .conversation(conversation).build();
                                    roomNewAction.process();
                                    break;
                                case ROOM_CLEAR:
                                    // The room will temporarily be cleared and
                                    // hidden from the messages list.  The user 
                                    // must leave the room for it to disappear 
                                    // forever.
                                    shouldUpdateMessagesList = true;
                                    lastMsgInConversation = null;

                                    RoomClearAction roomClearAction = new RoomClearAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true)
                                            .conversation(conversation).build();
                                    roomClearAction.process();
                                    break;
                                case ROOM_UPDATE:
                                    // A room notice needs to be displayed
                                    shouldUpdateChatThread = true;
                                    shouldUpdateUnreadCount = true;

                                    // The profile photo may have changed, and 
                                    // the room notice needs to be displayed in
                                    // the messages list
                                    shouldUpdateContactsList = true;
                                    shouldUpdateMessagesList = true;

                                    RoomUpdateAction roomUpdateAction = new RoomUpdateAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true)
                                            .nextSortedBy(nextSortedBy)
                                            .conversation(conversation).build();
                                    lastMsgInConversation = roomUpdateAction
                                            .process();
                                    break;
                                case USER_BLOCK:

                                    UserBlockAction userBlockAction = new UserBlockAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true).build();
                                    userBlockAction.process();
                                    break;
                                case USER_FRIEND:
                                    shouldUpdateContactsList = true;

                                    UserFriendAction userFriendAction = new UserFriendAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true).build();
                                    userFriendAction.process();
                                    break;
                                case USER_JOIN:
                                    shouldUpdateContactsList = true;

                                    UserJoinAction userJoinAction = new UserJoinAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true).build();
                                    userJoinAction.process();
                                    break;
                                case USER_UNBLOCK:

                                    UserUnblockAction userUnblockAction = new UserUnblockAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true).build();
                                    userUnblockAction.process();
                                    break;
                                case USER_UNFRIEND:
                                    shouldUpdateContactsList = true;

                                    UserUnfriendAction userUnfriendAction = new UserUnfriendAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true).build();
                                    userUnfriendAction.process();
                                    break;
                                case USER_UPDATE:
                                    // The profile photo may have changed
                                    shouldUpdateContactsList = true;
                                    shouldUpdateMessagesList = true;
                                    lastMsgInConversation = null;

                                    UserUpdateAction userUpdateAction = new UserUpdateAction.Builder(
                                            mMessagingService, envelopeInBatch)
                                            .inBatch(true).build();
                                    userUpdateAction.process();
                                    break;
                                default:
                                    // Don't update the cursor for unknown
                                    // commands as we can't be sure what
                                    // channelID to update
                                    LogIt.w(CommandReceiver.class,
                                            "Unknown command in batch",
                                            envelopeInBatch.getType(),
                                            commandBatch.getRecipientID(),
                                            envelopeInBatch.getCommandID());
                                    break;
                                }

                                lastCommandEnvelope = envelopeInBatch;

                                nextSortedBy += 1.0d;
                            }

                            if (conversation != null) {
                                LogIt.d(CommandReceiver.class,
                                        "Save conversation", channelID);
                                conversation.save();
                            }

                            if (commandBatch.getRemaining() > 0
                                    && lastCommandEnvelope != null) {
                                LogIt.d(CommandReceiver.class,
                                        "Send COMMAND_GET",
                                        commandBatch.getRecipientID(),
                                        lastCommandEnvelope.getCommandID());
                                PBCommandEnvelope commandEnvelope = mMessagingService
                                        .buildCommandGet(commandBatch
                                                .getRecipientID(),
                                                lastCommandEnvelope
                                                        .getCommandID());
                                try {
                                    mMessagingService
                                            .sendCommand(commandEnvelope);
                                } catch (Exception e) {
                                    LogIt.e(this, e, e.getMessage());
                                }
                            } else {
                                if (isLoadingEarlierMessages) {
                                    LogIt.d(CommandReceiver.class,
                                            "Finished processing 'Load Earlier Messages' commands from server");

                                    Bundle extras = new Bundle();
                                    extras.putLong(
                                            MessageMeConstants.RECIPIENT_ID_KEY,
                                            commandBatch.getRecipientID());

                                    mMessagingService
                                            .notifyChatClient(
                                                    MessageMeConstants.INTENT_ACTION_EARLIER_MESSAGES_AVAILABLE,
                                                    extras);
                                } else {
                                    if (shouldUpdateMessagesList) {
                                        if (lastMsgInConversation == null) {
                                            mMessagingService
                                                    .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST);
                                        } else {
                                            mMessagingService
                                                    .notifyChatClient(
                                                            MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                                                            lastMsgInConversation
                                                                    .getId(),
                                                            lastMsgInConversation
                                                                    .getChannelId(),
                                                            shouldUpdateUnreadCount);
                                        }
                                    }

                                    if (shouldUpdateChatThread) {
                                        Bundle extras = new Bundle();
                                        extras.putLong(
                                                MessageMeConstants.RECIPIENT_ID_KEY,
                                                commandBatch.getRecipientID());

                                        // New messages can arrive inside a BATCH after 
                                        // the ChatActivity has already displayed the 
                                        // thread (e.g. when the app has been in the
                                        // background for a while and the network layer 
                                        // is reconnecting).  Therefore we need to
                                        // tell the ChatActivity to update as well as the
                                        // Messages list.
                                        mMessagingService
                                                .notifyChatClient(
                                                        MessageMeConstants.INTENT_ACTION_MESSAGE_NEW,
                                                        extras);
                                    }

                                    if (shouldUpdateContactsList) {
                                        mMessagingService
                                                .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST);
                                    }
                                }
                            }
                            break;
                        case GET:
                            CommandGetAction getAction = new CommandGetAction.Builder(
                                    mMessagingService, envelope).build();
                            getAction.process();
                            break;
                        case MESSAGE_DELETE:
                            LogIt.d(CommandReceiver.class,
                                    "MESSAGE_DELETE - not yet implemented");
                            break;
                        case MESSAGE_NEW:
                            LogIt.d(CommandReceiver.class, "MESSAGE_NEW",
                                    envelope.getCommandID());

                            MessageNewAction action = new MessageNewAction.Builder(
                                    mMessagingService, envelope).build();
                            action.process();
                            break;
                        case MESSAGE_READ:
                            LogIt.d(CommandReceiver.class, "MESSAGE_READ",
                                    envelope.getCommandID());

                            MessageReadAction readAction = new MessageReadAction.Builder(
                                    mMessagingService, envelope).build();
                            readAction.process();
                            break;
                        case PUSH_TOKEN_NEW:
                            LogIt.d(CommandReceiver.class, "PUSH_TOKEN_NEW");
                            PushTokenNewAction pushTokenNewAction = new PushTokenNewAction.Builder(
                                    mMessagingService, envelope).build();
                            pushTokenNewAction.process();
                            break;
                        case PRESENCE_UPDATE:
                            LogIt.d(CommandReceiver.class, "PRESENCE_UPDATE");
                            PresenceUpdateAction presenceUpdateAction = new PresenceUpdateAction.Builder(
                                    mMessagingService, envelope).build();
                            presenceUpdateAction.process();
                            break;
                        case ROOM_CLEAR:
                            LogIt.d(CommandReceiver.class, "ROOM_CLEAR",
                                    envelope.getCommandID());
                            RoomClearAction roomClearAction = new RoomClearAction.Builder(
                                    mMessagingService, envelope).build();
                            roomClearAction.process();
                            break;
                        case ROOM_JOIN:
                            LogIt.d(CommandReceiver.class, "ROOM_JOIN",
                                    envelope.getCommandID());
                            RoomJoinAction roomJoinAction = new RoomJoinAction.Builder(
                                    mMessagingService, envelope).build();
                            lastMsgInConversation = roomJoinAction.process();
                            break;
                        case ROOM_JUMP:
                            LogIt.d(CommandReceiver.class, "ROOM_JUMP",
                                    envelope.getCommandID());
                            RoomJumpAction roomJumpAction = new RoomJumpAction.Builder(
                                    mMessagingService, envelope).build();
                            roomJumpAction.process();
                            break;
                        case ROOM_LEAVE:
                            LogIt.d(CommandReceiver.class, "ROOM_LEAVE",
                                    envelope.getCommandID());
                            RoomLeaveAction roomLeaveAction = new RoomLeaveAction.Builder(
                                    mMessagingService, envelope).build();
                            roomLeaveAction.process();
                            break;
                        case ROOM_NEW:
                            LogIt.d(CommandReceiver.class, "ROOM_NEW",
                                    envelope.getCommandID());
                            RoomNewAction roomNewAction = new RoomNewAction.Builder(
                                    mMessagingService, envelope).build();
                            roomNewAction.process();
                            break;
                        case ROOM_UPDATE:
                            LogIt.d(CommandReceiver.class, "ROOM_UPDATE",
                                    envelope.getCommandID());
                            RoomUpdateAction roomUpdateAction = new RoomUpdateAction.Builder(
                                    mMessagingService, envelope).build();
                            roomUpdateAction.process();
                            break;
                        case USER_BLOCK:
                            LogIt.d(CommandReceiver.class, "USER_BLOCK");
                            UserBlockAction userBlockAction = new UserBlockAction.Builder(
                                    mMessagingService, envelope).build();
                            userBlockAction.process();
                            break;
                        case USER_FRIEND:
                            LogIt.d(CommandReceiver.class, "USER_FRIEND");
                            UserFriendAction userFriendAction = new UserFriendAction.Builder(
                                    mMessagingService, envelope).build();
                            userFriendAction.process();
                            break;
                        case USER_IDENTIFY:
                            LogIt.d(CommandReceiver.class, "USER_IDENTIFY");
                            mMessagingService.processUserIdentify(envelope);
                            break;
                        case USER_JOIN:
                            LogIt.d(CommandReceiver.class, "USER_JOIN");
                            UserJoinAction userJoinAction = new UserJoinAction.Builder(
                                    mMessagingService, envelope).build();
                            userJoinAction.process();
                            break;
                        case USER_UNBLOCK:
                            LogIt.d(CommandReceiver.class, "USER_UNBLOCK");
                            UserUnblockAction userUnblockAction = new UserUnblockAction.Builder(
                                    mMessagingService, envelope).build();
                            userUnblockAction.process();
                            break;
                        case USER_UNFRIEND:
                            LogIt.d(CommandReceiver.class, "USER_UNFRIEND");

                            UserUnfriendAction userUnfriendAction = new UserUnfriendAction.Builder(
                                    mMessagingService, envelope).build();
                            userUnfriendAction.process();
                            break;
                        case USER_UPDATE:
                            LogIt.d(CommandReceiver.class, "USER_UPDATE");

                            UserUpdateAction userUpdateAction = new UserUpdateAction.Builder(
                                    mMessagingService, envelope).build();
                            userUpdateAction.process();
                            break;
                        default:
                            LogIt.w(CommandReceiver.class, "Unknown command",
                                    envelope.getType());
                            break;
                        }
                    } else {
                        // This is a duplicate command, or a command in error.
                        //
                        // Testing shows there are some situations where a command 
                        // gets left in the DurableCommandSender, so even on a 
                        // duplicate command we need to check whether it should 
                        // be removed.
                        if (durableSender == null) {
                            // The DurableCommandSender hasn't been initialized 
                            // yet, so there is nothing we can do
                            return;
                        }

                        DurableCommand durableCmd = durableSender
                                .getDurableCommand(envelope.getClientID());

                        if (durableCmd == null) {
                            LogIt.w(CommandReceiver.class,
                                    "Ignore duplicate/error command",
                                    envelope.getType(), envelope.getClientID(),
                                    envelope.getCommandID(),
                                    envelope.hasError());
                        } else {
                            LogIt.w(CommandReceiver.class,
                                    "Remove duplicate/error command from DurableSender",
                                    envelope.getType(), envelope.getClientID(),
                                    envelope.getCommandID(),
                                    envelope.hasError());
                            durableSender.removeFromQueue(durableCmd);
                        }
                    }
                }
            };
        }
    }

    /**
     * Shut down the receive queue thread.
     */
    public synchronized void shutDown() {

        if (mReceiveThread == null) {
            LogIt.i(this, "No receive thread to shut down");
        } else {
            LogIt.i(this, "Shut down the message receive thread");

            // If the thread is not blocked this will cause it to shut down
            mShutdownRequested = true;

            // If the thread is blocked this will make it wake up
            mReceiveThread.interrupt();
            mReceiveThread = null;
        }
    }
}
