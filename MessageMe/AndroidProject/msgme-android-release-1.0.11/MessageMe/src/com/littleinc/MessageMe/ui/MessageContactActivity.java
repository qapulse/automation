package com.littleinc.MessageMe.ui;

import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.util.ContactAdapter;
import com.littleinc.MessageMe.util.ContactManagement;
import com.littleinc.MessageMe.util.DatabaseTask;

@TargetApi(14)
public class MessageContactActivity extends ActionBarActivity {

    private ListView contactList;

    private boolean isGroupInvite;

    private ContactAdapter adapter;

    private List<Contact> contacts;

    /**
     * The 'Create New Group' header view
     */
    private TextView createGroupChatHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_list);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        adapter = new ContactAdapter(this);
        contactList = (ListView) findViewById(R.id.contact_list);

        isGroupInvite = getIntent().getBooleanExtra(
                MessageMeConstants.IS_GROUP_INVITE, false);

        if (!isGroupInvite) {
            setTitle(R.string.new_message_lbl);

            createGroupChatHeader = (TextView) LayoutInflater.from(this)
                    .inflate(R.layout.create_group_header, null, false);
            contactList.addHeaderView(createGroupChatHeader);
        } else {
            setTitle("");
        }

        contactList.setAdapter(adapter);
        contactList.setOnItemClickListener(new ContactClickListener());

        loadContacts();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case R.id.menu_search_btn:
            openSearchComposeMessage();
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.search_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    private void openSearchComposeMessage() {
        LogIt.user(this, "Actionbar search button pressed");
        Intent intent = new Intent(this, SearchComposeMessage.class);
        if (isGroupInvite) {
            intent.putExtra(MessageMeConstants.IS_GROUP_INVITE, true);
        }

        startActivityForResult(intent,
                MessageMeConstants.SEARCH_CONTACT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case MessageMeConstants.SEARCH_CONTACT_REQUEST_CODE:
            switch (resultCode) {
            case RESULT_OK:
                long id = data.getLongExtra(
                        MessageMeConstants.RECIPIENT_ID_KEY, -1);
                
                // The SearchComposeMessage activity does not allow the user 
                // to select a recipient user who we have blocked, or who has  
                // blocked us, so we pass in false for those params below. 
                if (isGroupInvite) {    
                    inviteGroupClick(id, "", false, false);
                } else {
                    openChatClick(id, "", false, false);
                }
                break;
            }
            break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void displayContacts(List<Contact> contacts) {
        adapter.deleteAllItems();
        ContactManagement.setAlphabeticallySortedAdapter(contacts, adapter);
    }

    private void loadContacts() {
        new DatabaseTask() {

            @Override
            public void work() {
                if (!isGroupInvite) {
                    contacts = ContactManagement
                            .loadContactListAndRooms(contacts);
                } else {
                    contacts = ContactManagement.loadContactList(contacts);
                }
            }

            @Override
            public void done() {
                displayContacts(contacts);
            }
        };
    }

    private class ContactClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view,
                int position, long id) {
            LogIt.user(MessageContactActivity.class, "Contact selected",
                    position, id);
            
            if (view.getId() == R.id.create_group_chat_button) {
                startActivity(new Intent(MessageContactActivity.this,
                        GroupNameActivity.class));
            } else {
                int headerCount = contactList.getHeaderViewsCount();

                // Subtract one when the 'Create New Group' row is displayed
                Contact selectedContact = (Contact) adapter.getItem(position
                        - headerCount);

                if (selectedContact == null) {
                    LogIt.w(this, "No user, ignore");
                    return;
                }

                if (isGroupInvite) {
                    if (!(selectedContact instanceof User)) {
                        LogIt.e(MessageContactActivity.class,
                                "Ignore selection of unexpected object type",
                                selectedContact.getClass());
                        return;
                    }

                    // We know this is a User as only users can be invited
                    // to groups
                    User selectedUser = (User) selectedContact;

                    inviteGroupClick(id, selectedUser.getFirstName(),
                            selectedUser.isBlocked(),
                            selectedUser.isBlockedBy());
                } else {
                    boolean isBlocked = false;
                    boolean blockedBy = false;

                    // Cope with Group objects as well as User objects
                    if (selectedContact instanceof User) {
                        isBlocked = ((User) selectedContact).isBlocked();
                        blockedBy = ((User) selectedContact).isBlockedBy();
                    }

                    openChatClick(selectedContact.getContactId(),
                            selectedContact.getFirstName(), isBlocked,
                            blockedBy);
                }
            }
        }
    }

    /**
     * Sends a resultIntent to its parent activity to add a selected user to 
     * a group, so long as the current user is not blocking/blocked by them.
     */
    private void inviteGroupClick(long selectedUserID, String userFirstName,
            boolean isBlocked, boolean blockedBy) {
        
        // Ignore the selection if the user is blocked, or if they
        // have blocked us
        if (isBlocked) {
            UIUtil.alert(
                    MessageContactActivity.this,
                    getString(R.string.blocked),
                    getString(R.string.blocked_msg,
                            userFirstName));
        } else if (blockedBy) {
            UIUtil.alert(
                    MessageContactActivity.this,
                    getString(R.string.blocked),
                    getString(R.string.blocked_by_msg,
                            userFirstName));
        } else {            
            Intent intent = new Intent();
            
            intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY, selectedUserID);
            
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    /**
     * Start a new activity for a private or group chat, so long as the 
     * current user is not blocking/blocked by them.
     */
    private void openChatClick(final long contactID, String userFirstName,
            boolean isBlocked, boolean blockedBy) {
        
        // Ignore the selection if the user is blocked, or if they
        // have blocked us
        if (isBlocked) {
            UIUtil.alert(
                    MessageContactActivity.this,
                    getString(R.string.blocked),
                    getString(R.string.blocked_msg,
                            userFirstName));
        } else if (blockedBy) {
            UIUtil.alert(
                    MessageContactActivity.this,
                    getString(R.string.blocked),
                    getString(R.string.blocked_by_msg,
                            userFirstName));
        } else {
            new DatabaseTask() {
    
                Contact contact;
    
                @Override
                public void work() {
                    contact = Contact.newInstance(contactID);
    
                    if (contact != null) {
                        contact.load();
                    }
                }
    
                @Override
                public void done() {
                    if (contact != null) {
                        Intent intent = new Intent(MessageContactActivity.this,
                                ChatActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    
                        if (contact.isUser()) {
                            User user = (User) contact;
    
                            intent.putExtra(MessageMeConstants.RECIPIENT_USER_KEY,
                                    user.toPBUser().toByteArray());
                            
                            intent.putExtra(
                                    MessageMeConstants.RECIPIENT_IS_SHOWN,
                                    user.isShown());
                        } else if (contact.isGroup()) {
                            Room room = (Room) contact;
    
                            intent.putExtra(MessageMeConstants.RECIPIENT_ROOM_KEY,
                                    room.toPBRoom().toByteArray());
                        }
    
                        intent.putExtra(Message.ID_COLUMN, getIntent()
                                .getLongExtra(Message.ID_COLUMN, -1));
                        intent.putExtra(Message.TYPE_COLUMN, getIntent()
                                .getIntExtra(Message.TYPE_COLUMN, -1));
                        if (getIntent()
                                .hasExtra(MessageMeConstants.EXTRA_IMAGE_KEY)) {
                            intent.putExtra(
                                    MessageMeConstants.EXTRA_IMAGE_KEY,
                                    getIntent().getStringExtra(
                                            MessageMeConstants.EXTRA_IMAGE_KEY));
                        }                 
    
                        startActivity(intent);
                        finish();
                    }
                }
            };
        }
    }
}