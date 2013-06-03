package com.littleinc.MessageMe.ui;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
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
import android.widget.ListView;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeFragment;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.AddressBook;
import com.littleinc.MessageMe.bo.AddressBook.AddressBookState;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.MessageMeAppPreferences;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.util.ContactAdapter;
import com.littleinc.MessageMe.util.ContactManagement;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.FriendsInviteUtil;

public class ContactListFragment extends MessageMeFragment {

    private ListView contactList;

    private ContactAdapter adapter;

    private List<Contact> contacts;

    private Handler mHandler = new Handler();

    private TextView inviteFriendsHeader;

    private TextView createGroupChatHeader;

    private LocalBroadcastManager broadcastManager;
    
    /**
     * Keep track of whether we are visible or not.
     */
    private boolean mIsVisible = false;
    
    /**
     * Flag telling us whether the contact list needs to be reloaded the next
     * time the user views this fragment.
     */
    private boolean mIsDirty = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        messagingServiceConnection = new MessagingServiceConnection();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        mIsVisible = true;
        
        if (mIsDirty) {
            mIsDirty = false;
            updateUI();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        mIsVisible = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_list, container, false);

        inviteFriendsHeader = (TextView) inflater.inflate(
                R.layout.invite_friends_header, null, false);
        createGroupChatHeader = (TextView) inflater.inflate(
                R.layout.create_group_header, null, false);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle("");

        contactList = (ListView) getActivity().findViewById(R.id.contact_list);
        contactList.addHeaderView(inviteFriendsHeader);
        contactList.addHeaderView(createGroupChatHeader);

        if (adapter == null) {
            adapter = new ContactAdapter(getActivity());

            contactList.setAdapter(adapter);
            updateUI();
        } else {
            contactList.setAdapter(adapter);
        }

        contactList.setOnItemClickListener(new ContactClickListener());

        MessageMeAppPreferences appPreferences = MessageMeApplication
                .getPreferences();

        // Show AB sync pop-up again if the state is denied and the current user doesn't have
        // any friend
        if (appPreferences.getABState() == AddressBookState.AB_STATE_DENIED
                && contacts != null && contacts.size() == 0) {

            appPreferences.setABState(null);

            AddressBook addressBook = AddressBook.getInstance(getActivity());
            addressBook.askForAllPermissionsAndUploadAll(getActivity());
        }
    }

    @Override
    public void onDestroy() {
        if (broadcastManager != null) {
            broadcastManager.unregisterReceiver(updateContactListReceiver);
        }

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MessageMeConstants.COUNTRY_SELECTION_REQUEST_CODE) {
            // Delegate the result back to the AddressBook class
            AddressBook.onActivityResult(getActivity(), requestCode,
                    resultCode, data);
        } else {
            LogIt.w(this, "Unexpected call to onActivityResult", requestCode,
                    resultCode);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateUI() {
        new DatabaseTask(mHandler) {

            @Override
            public void work() {
                LogIt.d(ContactListFragment.class, "Loading contacts");
                contacts = ContactManagement.loadContactListAndRooms(contacts);
            }

            @Override
            public void done() {
                LogIt.d(ContactListFragment.class, "Contacts loaded");
                displayContacts(contacts);
            }
        };
    }

    private class ContactClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            if (view.getId() == R.id.create_group_chat_button) {
                LogIt.user(ContactListFragment.class,
                        "Pressed create new room button");
                startActivity(new Intent(getActivity(), GroupNameActivity.class));
            } else if (view.getId() == R.id.invite_friends_chat_button) {

                LogIt.user(ContactListFragment.class,
                        "Pressed invite friends button");

                FriendsInviteUtil.openFriendsInvite(getActivity());
            } else {
                Intent intent = null;
                Contact selectedContact = adapter.getItem(position - 2);

                if (selectedContact == null) {
                    LogIt.w(ContactListFragment.class, "No contact, ignore");
                    return;
                }

                if (selectedContact.isGroup()) {
                    intent = new Intent(getActivity(),
                            GroupProfileActivity.class);

                    intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                            selectedContact.getContactId());

                    startActivity(intent);
                } else {
                    intent = new Intent(getActivity(),
                            ContactProfileActivity.class);

                    intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                            selectedContact.getContactId());

                    startActivity(intent);
                }
            }
        }
    }

    /**
    * Refresh and fill the alphabetical sorted adapter 
    */
    private void displayContacts(List<Contact> contacts) {
        LogIt.d(ContactListFragment.class, "Display " + contacts.size()
                + " contacts");

        adapter.deleteAllItems();
        ContactManagement.setAlphabeticallySortedAdapter(contacts, adapter);
    }

    private BroadcastReceiver updateContactListReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIsVisible) {  
                updateUI();
            } else {                        
                LogIt.i(ContactListFragment.class, "Intent received - mark Contacts list as dirty",
                        intent.getAction());
                mIsDirty = true;
            }
        }
    };

    class MessagingServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogIt.d(ContactListFragment.class, "onServiceConnected");
            mMessagingServiceRef = ((MessagingService.MessagingBinder) service)
                    .getService();

            broadcastManager = LocalBroadcastManager
                    .getInstance(mMessagingServiceRef);

            broadcastManager.unregisterReceiver(updateContactListReceiver);
            broadcastManager.registerReceiver(updateContactListReceiver,
                    new IntentFilter(
                            MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMessagingServiceRef = null;
        }
    }
}