package com.littleinc.MessageMe.ui;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.SearchActivity;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.util.ContactAdapter;
import com.littleinc.MessageMe.util.ContactManagement;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.SearchManager;
import com.littleinc.MessageMe.util.SearchTextWatcher;
import com.littleinc.MessageMe.util.StringUtil;

/**
 * Class designed to search contacts in the modality
 * of search-as-you-type
 * 
 * When the user selects a contact, it automatically starts
 * a conversation with the contact
 *
 */
public class SearchComposeMessage extends SearchActivity implements
        SearchManager {

    private ContactAdapter adapter;

    private List<Contact> contacts;

    private boolean isGroupInvite;

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(android.os.Message msg) {

            if (msg == null) {
                LogIt.w(SearchComposeMessage.class,
                        "Ignore null message received by handler");
                return;
            }

            switch (msg.what) {
            case MessageMeConstants.UPDATE_SEARCH_MESSAGE:
                String terms = (String) msg.obj;

                LogIt.d(SearchComposeMessage.class, "UPDATE_SEARCH_MESSAGE",
                        terms);

                if (StringUtil.isEmpty(terms)) {
                    if (adapter != null) {
                        adapter.deleteAllItems();

                        masterLayout.setBackgroundResource(0);
                        isShowingDarkOverlay = true;

                        setVisible(R.id.emptyElement, false);
                    } else {
                        LogIt.w(SearchComposeMessage.class,
                                "Adapter still null, omitting message");
                    }
                } else {
                    doSearch(terms);
                }
                break;
            default:
                LogIt.w(SearchComposeMessage.class,
                        "Unexpected message received by handler", msg.what);
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isGroupInvite = getIntent().getBooleanExtra(
                MessageMeConstants.IS_GROUP_INVITE, false);

        searchBox.addTextChangedListener(new SearchTextWatcher(mHandler));
        searchBox.setHint(R.string.search_contacts_hint);
    }

    @Override
    public void updateUI() {
        if (listView.getAdapter() == null) {
            LogIt.i(SearchComposeMessage.class,
                    "Contact list adapter null, set it");

            adapter = new ContactAdapter(this);

            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new ContactClickListener());
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Method defined in the search_box layout XML
     */
    @Override
    public void onSearch(View view) {
        LogIt.user(this, "User pressed on the search icon button");

        String terms = searchBox.getText().toString();
        if (!StringUtil.isEmpty(terms)) {
            UIUtil.hideKeyboard(searchBox);
            doSearch(terms);
        }
    }

    private void displayContacts(List<Contact> contacts) {
        adapter.deleteAllItems();
        isShowingDarkOverlay = false;
        masterLayout.setBackgroundResource(R.color.list_view_background);
        if (contacts != null && contacts.size() > 0) {
            LogIt.d(this, "Display " + contacts.size() + " contacts");
            ContactManagement.setAlphabeticallySortedAdapter(contacts, adapter);
            updateUI();
            setVisible(R.id.emptyElement, false);
        } else {
            setVisible(R.id.emptyElement, true);
        }
    }

    private class ContactClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            Contact selectedContact = adapter.getItem(position);

            if (selectedContact == null) {
                LogIt.e(SearchComposeMessage.class,
                        "Ignore selection as selectedContact is null");
                return;
            } else {
                LogIt.user(SearchComposeMessage.class, "Contact selected",
                        position, id, selectedContact.getDisplayName());
            }

            Intent intent = new Intent();

            intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                    selectedContact.getContactId());

            // Only User objects can be blocked (you cannot block or be
            // blocked by a Group)
            if (selectedContact instanceof User) {
                User selectedUser = (User) selectedContact;

                String firstName = selectedUser.getFirstName();
                
                intent.putExtra(MessageMeConstants.RECIPIENT_IS_SHOWN,
                        selectedUser.isShown());

                if (selectedUser.isBlocked()) {
                    LogIt.d(SearchComposeMessage.class,
                            "User is blocked, ignore selection",
                            selectedContact.getDisplayName());
                    UIUtil.alert(SearchComposeMessage.this,
                            getString(R.string.blocked),
                            getString(R.string.blocked_msg, firstName));
                    return;
                } else if (selectedUser.isBlockedBy()) {
                    LogIt.d(SearchComposeMessage.class,
                            "This user blocked us, ignore selection",
                            selectedContact.getDisplayName());
                    UIUtil.alert(SearchComposeMessage.this,
                            getString(R.string.blocked),
                            getString(R.string.blocked_by_msg, firstName));
                    return;
                }
            }

            if (!isGroupInvite) {
                intent.putExtra(Message.ID_COLUMN,
                        getIntent().getLongExtra(Message.ID_COLUMN, -1));
                intent.putExtra(Message.TYPE_COLUMN,
                        getIntent().getIntExtra(Message.TYPE_COLUMN, -1));
            }

            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void doSearch(final String terms) {
        new DatabaseTask() {

            @Override
            public void work() {

                if (!terms.trim().contains(" ")) {
                    if (!isGroupInvite) {
                        contacts = ContactManagement
                                .searchContactListAndRooms(terms.trim());
                    } else {
                        contacts = ContactManagement.searchUsers(terms.trim());
                    }
                } else {
                    String termsArray[] = terms.split(" ");
                    for (String searchTerm : termsArray) {
                        contacts = ContactManagement.doSearch(
                                searchTerm.trim(), contacts);
                    }
                }
            }

            @Override
            public void done() {
                updateUI();
                displayContacts(contacts);
            }
        };
    }
}
