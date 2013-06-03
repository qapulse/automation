package com.littleinc.MessageMe.ui;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeFragment;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.GCMMessage;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.DurableCommand;
import com.littleinc.MessageMe.chat.LocalMessages;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.metrics.MMHourlyTracker;
import com.littleinc.MessageMe.util.ConversationAdapter;
import com.littleinc.MessageMe.util.DatabaseTask;

public class MessagesFragment extends MessageMeFragment {

    private ListView messageList;

    private ConversationAdapter adapter;

    private List<Conversation> conversations;

    private Handler mHandler = new Handler();

    private LocalBroadcastManager broadcastManager;

    /**
     * Keep track of whether we are visible or not.
     */
    private boolean mIsVisible = false;

    /**
     * Flag telling us whether the message list needs to be reloaded the next
     * time the user views this fragment.
     */
    private boolean mIsDirty = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        messagingServiceConnection = new MessagingServiceConnection();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.messages_layout, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        mIsVisible = true;

        if (mIsDirty) {
            mIsDirty = false;
            updateUI(true, -1, true);
        }

        // This extra indicates which message thread should be shown
        final long contactId = getActivity().getIntent().getLongExtra(
                MessageMeConstants.RECIPIENT_ID_KEY, -1);

        if (contactId != -1) {
            LogIt.d(MessagesFragment.class,
                    "Launch message thread for channel", contactId);

            // Remove it as we only want to try and show the message once
            getActivity().getIntent().removeExtra(
                    MessageMeConstants.RECIPIENT_ID_KEY);

            new DatabaseTask(mHandler) {

                Contact contact;

                @Override
                public void work() {
                    // Check that the contact ID corresponds to a User or Room
                    // in our local database.
                    if (User.exists(contactId)) {
                        LogIt.d(getActivity(), "Show private message thread",
                                contactId);
                        contact = new User(contactId);
                    } else if (Room.exists(contactId)) {
                        LogIt.d(getActivity(), "Show room", contactId);
                        contact = new Room(contactId);
                    } else {
                        // A Contact won't exist if a GCM notification is
                        // received from a new contact, as they won't be added
                        // to our local database until we receive web socket API
                        // commands that include the contact. In this case we
                        // just want to show the list of messages, not a
                        // specific thread. When the web socket downloads the
                        // latest cursor the new thread will appear.
                        fail("Contact " + contactId + " does not exist");
                    }

                    if (contact != null) {
                        contact.load();
                    }
                }

                @Override
                public void done() {

                    if (!failed()) {
                        if (contact != null) {
                            Activity activity = getActivity();

                            if (activity == null) {
                                // If we were already destroyed there is no way
                                // for us to start the activity to show the
                                // message thread
                                LogIt.w(this,
                                        "Activity was destroyed, don't try to show the message thread");
                            } else {
                                // Removes the stacked GCM notifications for
                                // this user
                                GCMMessage.removeGCMMessagesFromSender(contact
                                        .getContactId());

                                Intent showThreadIntent = new Intent(activity,
                                        ChatActivity.class);
                                showThreadIntent
                                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                if (contact.isUser()) {
                                    User user = (User) contact;

                                    showThreadIntent
                                            .putExtra(
                                                    MessageMeConstants.RECIPIENT_USER_KEY,
                                                    user.toPBUser()
                                                            .toByteArray());
                                } else if (contact.isGroup()) {
                                    Room room = (Room) contact;

                                    showThreadIntent
                                            .putExtra(
                                                    MessageMeConstants.RECIPIENT_ROOM_KEY,
                                                    room.toPBRoom()
                                                            .toByteArray());
                                }

                                startActivity(showThreadIntent);
                            }
                        }
                    }
                }
            };
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mIsVisible = false;
    }

    @Override
    public void onStart() {
        super.onStart();

        // first session screen tracking
        Integer order = MMLocalData.getInstance().getSessionOrder();
        MMFirstSessionTracker.getInstance().abacus(null, "convos", "screen",
                order, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle("");

        messageList = (ListView) getActivity().findViewById(
                R.id.message_listview);

        if (adapter == null) {
            adapter = new ConversationAdapter(getActivity(), messageList);
            messageList.setAdapter(adapter);
            updateUI(true, -1, true);
        } else {
            messageList.setAdapter(adapter);
        }

        messageList.setOnItemClickListener(conversationClickListener);
        messageList.setOnItemLongClickListener(new ItemLongClickListener());
    }

    /**
     * Unregister the broadcast receivers
     */
    private void unregisterReceivers() {
        if (broadcastManager != null) {
            LogIt.d(this, "Unregistering receivers");
            broadcastManager.unregisterReceiver(messagesReceiver);
        }
    }

    /**
     * Register the chat broadcast receivers
     */
    private void registerReceivers() {
        if (broadcastManager != null) {
            LogIt.d(this, "Registering receivers");
            broadcastManager.registerReceiver(messagesReceiver,
                    new IntentFilter(
                            MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST));
        }
    }

    /**
     * Combined UI update operations. These are done simultaneously to avoid
     * needing to run lots of separate DataBaseTasks.
     * 
     * @param updateAllConversations whether to reload all Conversations
     * @param channelToUpdate if updateAllConversations is false, then this
     * parameter can be used to indicate a specific conversation to update. A
     * value of -1 is ignored.
     * @param updateUnreadCount whether to load and update the unread count
     */
    private void updateUI(final boolean updateAllConversations,
            final long channelToUpdate, final boolean updateUnreadCount) {

        // This doesn't apply if updateAllConversations is true
        final boolean updateIndividualConversation = (channelToUpdate != -1);

        new DatabaseTask(mHandler) {

            private int unreadCount;

            private Conversation conversation;

            private List<Conversation> reloadedConversations;

            @Override
            public void work() {
                if (updateAllConversations) {
                    LogIt.i(MessagesFragment.this, "Loading conversations");
                    reloadedConversations = Conversation
                            .getVisibleConversations();
                } else if (updateIndividualConversation) {
                    conversation = Conversation.newInstance(channelToUpdate);
                }

                if (updateUnreadCount) {
                    LogIt.d(MessagesFragment.class, "Loading unread count");
                    unreadCount = LocalMessages.loadConversationUnreadCount();
                } else {
                    unreadCount = -1;
                }
            }

            @Override
            public void done() {
                if (updateAllConversations) {
                    if (reloadedConversations == null) {
                        LogIt.e(MessagesFragment.class,
                                "Loaded conversations are null!");
                    } else {
                        LogIt.i(MessagesFragment.this, "Loaded conversations",
                                reloadedConversations.size());
                        conversations = reloadedConversations;
                        adapter.updateItems(conversations);
                    }
                } else if (updateIndividualConversation) {
                    if ((conversation == null) || (adapter == null)) {
                        // This seems to be able to happen if the user touches
                        // a GCM notification and tries to send a message in
                        // that thread when it hasn't be received from the
                        // server yet (e.g. during a login).
                        LogIt.w(MessagesFragment.class,
                                "Don't update conversation something is null",
                                conversation, adapter);
                    } else {
                        LogIt.d(MessagesFragment.class,
                                "Adding/updating single conversation in the list");
                        adapter.updateItem(conversation);
                    }
                }

                if (updateUnreadCount) {
                    if (unreadCount == -1) {
                        LogIt.w(MessagesFragment.class,
                                "Error getting unread count, ignore it");
                    } else {
                        LogIt.d(MessagesFragment.class, "Loaded unread count",
                                unreadCount);
                        TabsFragmentActivity activity = (TabsFragmentActivity) getActivity();
                        if (activity != null) {
                            activity.setMessageCount(unreadCount);
                        }
                    }
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        unregisterReceivers();
        super.onDestroy();
    }

    /**
     * For handling {@link MessageMeConstants#INTENT_NOTIFY_MESSAGE_LIST}
     */
    private BroadcastReceiver messagesReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (adapter == null) {
                LogIt.w(MessagesFragment.class,
                        "MessageAdapter is null, ignore onReceive");
                return;
            }

            if (intent == null) {
                LogIt.w(MessagesFragment.class,
                        "Ignore null intent in our messagesReceiver");
                return;
            }

            final boolean updateUnreadCount = intent.getBooleanExtra(
                    MessageMeConstants.EXTRA_UPDATE_UNREAD_COUNT, false);

            if (intent.hasExtra(Message.CHANNEL_ID_COLUMN)) {

                final long channelId = intent.getLongExtra(
                        Message.CHANNEL_ID_COLUMN, -1);

                if (channelId != -1) {
                    LogIt.d(MessagesFragment.class,
                            "INTENT_NOTIFY_MESSAGE_LIST includes channel ID",
                            channelId);

                    updateUI(false, channelId, updateUnreadCount);
                } else {
                    LogIt.w(MessagesFragment.class,
                            "Channel ID missing from INTENT_NOTIFY_MESSAGE_LIST with CHANNEL_ID_COLUMN");
                }
            } else {
                LogIt.d(MessagesFragment.class,
                        "INTENT_NOTIFY_MESSAGE_LIST received");

                if (mIsVisible) {
                    updateUI(true, -1, updateUnreadCount);
                } else {
                    LogIt.d(MessagesFragment.class,
                            "Mark Messages list as dirty");
                    mIsDirty = true;
                }
            }
        }
    };

    private OnItemClickListener conversationClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            final Conversation conversation = adapter.getItem(position);

            if (conversation != null) {
                new DatabaseTask(mHandler) {

                    Contact contact;

                    @Override
                    public void work() {
                        contact = Contact.newInstance(conversation
                                .getChannelId());

                        if (contact != null) {
                            contact.load();
                        }
                    }

                    @Override
                    public void done() {
                        if (contact != null) {
                            if (getActivity() != null) {
                                Intent intent = new Intent(getActivity(),
                                        ChatActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                if (contact.isUser()) {
                                    User user = (User) contact;

                                    intent.putExtra(
                                            MessageMeConstants.RECIPIENT_USER_KEY,
                                            user.toPBUser().toByteArray());

                                    intent.putExtra(
                                            MessageMeConstants.RECIPIENT_IS_SHOWN,
                                            user.isShown());
                                } else if (contact.isGroup()) {
                                    Room room = (Room) contact;

                                    intent.putExtra(
                                            MessageMeConstants.RECIPIENT_ROOM_KEY,
                                            room.toPBRoom().toByteArray());
                                }

                                MMHourlyTracker.getInstance().abacusOnce(null,
                                        "active", "user", null, null);
                                startActivity(intent);
                            } else {
                                LogIt.w(MessagesFragment.class,
                                        "Activity is now null, ignore item click");
                            }
                        }
                    }
                };
            } else {
                LogIt.w(MessagesFragment.class,
                        "Conversation object is null, ignore click on it",
                        position);
            }
        }
    };

    class MessagingServiceConnection implements ServiceConnection {

        /**
         * Register a BroadcastReceiver for the intent
         * INTENT_NOTIFY_MESSAGE_LIST
         * {@link MessagingService#notifyMessageList(long messageID)}
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogIt.d(MessagesFragment.class, "Connected to Messaging Service");
            mMessagingServiceRef = ((MessagingService.MessagingBinder) service)
                    .getService();

            MessageMeApplication.appIsActive(getActivity(),
                    mMessagingServiceRef);

            broadcastManager = LocalBroadcastManager
                    .getInstance(mMessagingServiceRef);

            unregisterReceivers();
            registerReceivers();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMessagingServiceRef = null;
        }
    }

    /**
     * Item long click listener for the listview
     * 
     */
    private class ItemLongClickListener implements OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view,
                int position, long id) {

            LogIt.user(this, "Long click on conversation");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(R.string.conversation_lbl);
            builder.setItems(R.array.delete_options,
                    new DeleteDialogClickListener(position));

            builder.create().show();
            return true;
        }
    }

    /**
     * Opens a dialog to confirm informing
     * the deletion of the message
     * 
     */
    private class DeleteDialogClickListener implements
            DialogInterface.OnClickListener {

        private int position;

        public DeleteDialogClickListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case 0:
                    displayConfirmationDialog(position);
                    break;

            }

            dialog.dismiss();
        }
    }

    /**
     * Opens a dialog to confirm the deletion
     * of the message
     */
    private void displayConfirmationDialog(final int position) {
        LogIt.d(this, "Open confirmation dialog to delete conversation");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.confirm_deletion));
        builder.setMessage(
                getActivity().getString(R.string.confirm_deletion_message))
                .setCancelable(false)
                .setPositiveButton(
                        getActivity().getString(R.string.delete_conversation),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                LogIt.user(MessagesFragment.class,
                                        "Delete conversation");
                                deleteConversation(adapter.getItem(position),
                                        position);
                            }
                        })
                .setNegativeButton(getActivity().getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                LogIt.user(MessagesFragment.class,
                                        "Cancel confirmation dialog");
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Deletes a conversation
     */
    private void deleteConversation(final Conversation conversation,
            final int position) {

        if (conversation != null) {
            // Get a minimal message so we can get the Contact from it
            IMessage lastMessage = conversation.getLastMessageForMessagesList();

            if (lastMessage != null) {
                final Contact contact = lastMessage.getContact();

                if (contact != null) {
                    DurableCommand durableCommand = new DurableCommand(
                            contact.serializeRoomClear(lastMessage
                                    .getCommandId()));
                    mMessagingServiceRef.addToDurableSendQueue(durableCommand);

                    new DatabaseTask(mHandler) {

                        @Override
                        public void work() {
                            // Deletes the messages related to that thread from
                            // the local DB
                            contact.clear();

                            conversation.setShown(false);
                            conversation.save();

                            LogIt.d(MessagesFragment.class,
                                    "Deleted messages and hid conversation",
                                    contact.getContactId());
                        }

                        @Override
                        public void done() {
                            if (adapter != null) {
                                // Remove the item from the adapter
                                adapter.deleteItemtAt(position);
                            }
                        }
                    };
                }
            } else {
                LogIt.w(MessagesFragment.class,
                        "No last message, cannot clear thread",
                        conversation.getChannelId());
            }
        } else {
            LogIt.w(MessagesFragment.class,
                    "No conversation, cannot clear thread");
        }
    }
}