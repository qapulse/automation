package com.littleinc.MessageMe.ui;

import java.util.List;

import android.annotation.TargetApi;
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
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.LocalMessages;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.MessageAdapter;
import com.littleinc.MessageMe.util.SearchManager;
import com.littleinc.MessageMe.util.SearchTextWatcher;
import com.littleinc.MessageMe.util.StringUtil;

@TargetApi(14)
public class SearchMessagesActivity extends SearchActivity implements
        SearchManager {

    private MessageAdapter adapter;

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            if (msg == null) {
                LogIt.w(SearchMessagesActivity.class,
                        "Ignore null message received by handler");
                return;
            }

            switch (msg.what) {
            case MessageMeConstants.UPDATE_SEARCH_MESSAGE:
                String terms = (String) msg.obj;

                LogIt.d(SearchMessagesActivity.class, "UPDATE_SEARCH_MESSAGE",
                        terms);

                if (StringUtil.isEmpty(terms)) {
                    if (adapter != null) {
                        adapter.deleteAllItems();

                        isShowingDarkOverlay = true;
                        masterLayout.setBackgroundResource(0);

                        setVisible(R.id.emptyElement, false);
                    } else {
                        LogIt.d(SearchMessagesActivity.class,
                                "Adapter still null, omitting message");
                    }
                } else {
                    doSearch(terms);
                }

                break;

            default:
                LogIt.w(SearchMessagesActivity.class,
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

        searchBox.setHint(R.string.search_messages_lbl);

        updateUI();
    }

    private void displayMessages(List<IMessage> messages) {
        adapter.deleteAllItems();
        isShowingDarkOverlay = false;
        masterLayout.setBackgroundResource(R.color.list_view_background);
        if (messages.size() > 0) {
            for (IMessage iMessage : messages) {
                adapter.addItem(iMessage);
            }
            adapter.sortAndUpdate();
            setVisible(R.id.emptyElement, false);
        } else {
            setVisible(R.id.emptyElement, true);
        }
        updateUI();

    }

    @Override
    protected void onPause() {
        UIUtil.hideKeyboard(searchBox);
        super.onPause();
    }

    private class MessageClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            final IMessage message = adapter.getItem(position);

            if (message != null) {
                new DatabaseTask() {

                    Contact contact;

                    @Override
                    public void work() {
                        contact = Contact.newInstance(message.getChannelId());

                        if (contact != null) {
                            contact.load();
                        }
                    }

                    @Override
                    public void done() {
                        if (contact != null) {
                            Intent intent = new Intent(
                                    SearchMessagesActivity.this,
                                    ChatActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                            if (contact.isUser()) {
                                User user = (User) contact;

                                intent.putExtra(
                                        MessageMeConstants.RECIPIENT_USER_KEY,
                                        user.toPBUser().toByteArray());
                            } else if (contact.isGroup()) {
                                Room room = (Room) contact;

                                intent.putExtra(
                                        MessageMeConstants.RECIPIENT_ROOM_KEY,
                                        room.toPBRoom().toByteArray());
                            }

                            intent.putExtra(
                                    MessageMeConstants.SEARCHED_MESSAGE_SORTED_BY,
                                    message.getSortedBy());

                            startActivity(intent);
                        }
                    }
                };
            } else {
                LogIt.w(SearchMessagesActivity.class,
                        "IMessage object is null, ignore click on it", position);
            }
        }
    }

    @Override
    public void doSearch(final String terms) {
        new DatabaseTask() {

            List<IMessage> messages;

            @Override
            public void work() {
                messages = LocalMessages.getConversations(terms);
            }

            @Override
            public void done() {
                displayMessages(messages);
            }
        };
    }

    @Override
    public void updateUI() {
        if (listView.getAdapter() == null) {
            LogIt.i(this, "Contact list adapter null, set it");
            adapter = new MessageAdapter(this);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new MessageClickListener());
        } else {
            adapter.notifyDataSetChanged();
        }
    }
}
