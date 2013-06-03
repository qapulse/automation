package com.littleinc.MessageMe.ui;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.SearchActivity;
import com.littleinc.MessageMe.bo.Contact;
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
 */
public class SearchContactsActivity extends SearchActivity implements
        SearchManager {

    private ContactAdapter adapter;

    private List<Contact> contacts;

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            if (msg == null) {
                LogIt.w(SearchContactsActivity.class,
                        "Ignore null message received by handler");
                return;
            }

            switch (msg.what) {
            case MessageMeConstants.UPDATE_SEARCH_MESSAGE:
                String terms = (String) msg.obj;

                LogIt.d(SearchContactsActivity.class, "UPDATE_SEARCH_MESSAGE",
                        terms);

                if (StringUtil.isEmpty(terms)) {
                    if (adapter != null) {
                        adapter.deleteAllItems();

                        isShowingDarkOverlay = true;
                        masterLayout.setBackgroundResource(0);

                        setVisible(R.id.emptyElement, false);
                    } else {
                        LogIt.w(SearchContactsActivity.class,
                                "Adapter still null, omitting message");
                    }
                } else {
                    doSearch(terms);
                }
                break;
            default:
                LogIt.w(SearchContactsActivity.class,
                        "Unexpected message received by handler", msg.what);
                break;
            }
        }
    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchBox.addTextChangedListener(new SearchTextWatcher(mHandler));
        searchBox.setHint(R.string.search_contacts_hint);
    };

    @Override
    public void updateUI() {
        if (listView.getAdapter() == null) {
            LogIt.i(this, "Contact list adapter null, set it");
            adapter = new ContactAdapter(this);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new ContactClickListener());
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private class ContactClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            if (view.getId() == R.id.create_group_chat_button) {
                startActivity(new Intent(SearchContactsActivity.this,
                        GroupNameActivity.class));
            } else {
                Intent intent = null;
                Contact selectedContact = adapter.getItem(position);

                if (selectedContact == null) {
                    LogIt.w(this, "No contact, ignore");
                    return;
                }

                if (selectedContact.isGroup()) {
                    intent = new Intent(SearchContactsActivity.this,
                            GroupProfileActivity.class);

                    intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                            selectedContact.getContactId());

                    startActivity(intent);
                } else {
                    intent = new Intent(SearchContactsActivity.this,
                            ContactProfileActivity.class);

                    intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                            selectedContact.getContactId());

                    startActivity(intent);
                }
            }
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

    @Override
    public void doSearch(final String terms) {
        new DatabaseTask() {

            @Override
            public void work() {
                if (!terms.trim().contains(" ")) {
                    contacts = ContactManagement
                            .searchContactListAndRooms(terms.trim());
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
