package com.littleinc.MessageMe.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.restlet.resource.ResourceException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.bo.Contact.SortContacts;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.RoomMember;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.DateUtil;
import com.littleinc.MessageMe.widget.ContactAutoCompleteTextView;

@TargetApi(14)
public class GroupContactsActivity extends ActionBarActivity {

    private String groupName;

    private List<User> contacts;

    private ImageView addContactButton;

    private AutoCompleteContactAdapter adapter;

    private ContactAutoCompleteTextView contactAutoCompleteTextView;

    private List<User> mSelectedContactsList;

    /**
     * Array of booleans to indicate which of the contacts is selected
     * to be members of the group.
     */
    private boolean[] mSelectedContactsArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_contacts);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        mSelectedContactsList = new LinkedList<User>();
        setGroupName(getIntent().getStringExtra(
                MessageMeConstants.GROUP_NAME_KEY));

        setTitle(EmojiUtils.convertToEmojisIfRequired(getGroupName(),
                EmojiSize.NORMAL));

        contactAutoCompleteTextView = (ContactAutoCompleteTextView) findViewById(R.id.contacts_input);

        addContactButton = (ImageView) findViewById(R.id.add_contact_button);
        addContactButton
                .setOnClickListener(new AddContactButtonClickListener());

        new DatabaseTask() {

            @Override
            public void work() {
                contacts = User.getContactList();
                Collections.sort(contacts, new SortContacts());

                adapter = new AutoCompleteContactAdapter(
                        GroupContactsActivity.this,
                        android.R.layout.simple_dropdown_item_1line, contacts);
            }

            @Override
            public void done() {
                contactAutoCompleteTextView.setAdapter(adapter);
                contactAutoCompleteTextView
                        .setOnItemClickListener(new ContactItemClickListener());
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.group_contacts_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            updateContactListUI();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
        } else if (itemId == R.id.action_done) {
            createGroup();
        }

        return super.onOptionsItemSelected(item);
    }

    private void createGroup() {
        if (mSelectedContactsList.size() > 0) {
            final Room newRoom = new Room();
            List<RoomMember> members = new LinkedList<RoomMember>();
            User currentUser = MessageMeApplication.getCurrentUser();

            for (User user : mSelectedContactsList) {
                members.add(RoomMember.parseFrom(user));
            }

            newRoom.setName(groupName);
            newRoom.setMembers(members);
            newRoom.setCreatorId(currentUser.getUserId());
            newRoom.setDateCreated(DateUtil.getCurrentTimestamp());

            final ProgressDialog progressDialog = UIUtil.showProgressDialog(
                    this, getString(R.string.loading));

            new BackgroundTask() {

                private Room realRoom;

                @Override
                public void work() {
                    try {
                        realRoom = RestfulClient.getInstance().createNewGroup(
                                newRoom);
                    } catch (ResourceException e) {
                        LogIt.e(GroupContactsActivity.class, e, e.getMessage());
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    } catch (IOException e) {
                        LogIt.e(GroupContactsActivity.class, e, e.getMessage());
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    } catch (Exception e) {
                        LogIt.e(GroupContactsActivity.class, e, e.getMessage());
                        fail(getString(R.string.group_warning_title),
                                getString(R.string.unexpected_error));
                    }
                }

                @Override
                public void done() {
                    progressDialog.dismiss();

                    if (failed()) {
                        UIUtil.alert(GroupContactsActivity.this,
                                getExceptionTitle(), getExceptionMessage());
                    } else {
                        new DatabaseTask() {

                            @Override
                            public void work() {
                                realRoom.save();
                            }

                            @Override
                            public void done() {
                                // track room creates in the first session.
                                MMFirstSessionTracker.getInstance().abacusOnce(
                                        null, "create", "room", null, null);

                                Intent intent = new Intent();
                                intent.putExtra(
                                        MessageMeConstants.RECIPIENT_ID_KEY,
                                        realRoom.getRoomId());

                                setResult(RESULT_OK, intent);
                                finish();
                            }
                        };
                    }
                }
            };
        } else {
            UIUtil.alert(GroupContactsActivity.this,
                    getString(R.string.group_warning_title),
                    getString(R.string.group_profile_no_contacts));
        }
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    private class AddContactButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            CharSequence[] contactsNames = new CharSequence[contacts.size()];
            mSelectedContactsArray = new boolean[contacts.size()];

            for (int i = 0; i < contacts.size(); i++) {
                contactsNames[i] = contacts.get(i).getStyledNameWithEmojis();
                if (GroupContactsActivity.this.mSelectedContactsList
                        .contains(contacts.get(i))) {
                    mSelectedContactsArray[i] = true;
                } else {
                    mSelectedContactsArray[i] = false;
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    v.getContext());

            builder.setTitle(R.string.group_select_contacts_title);
            builder.setMultiChoiceItems(contactsNames, mSelectedContactsArray,
                    new OnContactClickListener());
            builder.setNeutralButton(R.string.group_next,
                    new NextButtonClickListener());

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        private class OnContactClickListener implements
                OnMultiChoiceClickListener {

            @Override
            public void onClick(DialogInterface dialog, int which,
                    boolean isChecked) {
                User newlySelectedContact = contacts.get(which);

                LogIt.user(GroupContactsActivity.this,
                        "Selected contact via checkbox",
                        newlySelectedContact.getDisplayName());

                final AlertDialog alert = (AlertDialog) dialog;
                final ListView list = alert.getListView();

                if (isChecked) {
                    if (newlySelectedContact.isBlocked()) {
                        // Uncheck the selection
                        mSelectedContactsArray[which] = false;
                        list.setItemChecked(which, false);

                        UIUtil.alert(
                                GroupContactsActivity.this,
                                getString(R.string.blocked),
                                getString(R.string.blocked_msg,
                                        newlySelectedContact.getFirstName()));
                        return;
                    } else if (newlySelectedContact.isBlockedBy()) {
                        // Uncheck the selection
                        mSelectedContactsArray[which] = false;
                        list.setItemChecked(which, false);

                        UIUtil.alert(
                                GroupContactsActivity.this,
                                getString(R.string.blocked),
                                getString(R.string.blocked_by_msg,
                                        newlySelectedContact.getFirstName()));
                        return;
                    } else {
                        mSelectedContactsList.add(newlySelectedContact);
                    }
                } else {
                    mSelectedContactsList.remove(newlySelectedContact);
                }

                updateContactListUI();
            }
        }
    }

    /**
     * Display all the currently selected users in the UI. 
     */
    private void updateContactListUI() {
        LogIt.d(this, "updateContactListUI");
        contactAutoCompleteTextView.setText("");
        for (User selectedContact : mSelectedContactsList) {
            contactAutoCompleteTextView.append(" \"");
            contactAutoCompleteTextView
                    .append(selectedContact.getDisplayName());
            contactAutoCompleteTextView.append("\"");
            contactAutoCompleteTextView.append(contactAutoCompleteTextView
                    .getSeperator());
        }
    }

    private class ContactItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            User selectedUser = adapter.getItem(position);

            LogIt.user(GroupContactsActivity.this,
                    "Selected contact via autocomplete drop down",
                    selectedUser.getDisplayName());

            if (selectedUser.isBlocked()) {
                // The ContactAutoCompleteTextView replaceText method was 
                // already called automatically, so we now need to remove the 
                // user who was just selected.  Since that user wasn't added to  
                // the selectedContacts, refreshing the UI removes that user.
                updateContactListUI();

                UIUtil.alert(
                        GroupContactsActivity.this,
                        getString(R.string.blocked),
                        getString(R.string.blocked_msg,
                                selectedUser.getFirstName()));
            } else if (selectedUser.isBlockedBy()) {
                updateContactListUI();

                UIUtil.alert(
                        GroupContactsActivity.this,
                        getString(R.string.blocked),
                        getString(R.string.blocked_by_msg,
                                selectedUser.getFirstName()));
            } else {
                mSelectedContactsList.add(selectedUser);
            }
        }
    }

    private class NextButtonClickListener implements
            android.content.DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            createGroup();
        }
    }

    private class AutoCompleteContactAdapter extends ArrayAdapter<User> {

        private List<User> originalValues, values;

        public AutoCompleteContactAdapter(Context context,
                int textViewResourceId, List<User> objects) {
            super(context, textViewResourceId, objects);
            values = objects;
        }

        @Override
        public int getCount() {
            return values.size();
        }

        @Override
        public User getItem(int position) {
            return values.get(position);
        }

        @Override
        public long getItemId(int position) {
            return values.get(position).getUserId();
        }

        @Override
        public int getPosition(User item) {
            return values.indexOf(item);
        }

        @Override
        public Filter getFilter() {
            return new ContactFilter();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView,
                    parent);

            view.setText(getItem(position).getDisplayName());
            view.setTextColor(getResources().getColor(R.color.text_pill_color));

            return view;
        }

        private class ContactFilter extends Filter {

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                User user = (User) resultValue;
                return user.getDisplayName();
            }

            /**
             * Filter the list of available contacts based on the 
             * characters entered so far.
             */
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (originalValues == null) {
                    originalValues = new ArrayList<User>(values);
                }

                if (constraint == null || constraint.length() == 0) {
                    ArrayList<User> list;
                    list = new ArrayList<User>(originalValues);

                    results.values = list;
                    results.count = list.size();
                } else {
                    String prefixString = constraint.toString().toLowerCase();

                    ArrayList<User> values;
                    values = new ArrayList<User>(originalValues);

                    final int count = values.size();
                    final ArrayList<User> newValues = new ArrayList<User>();

                    for (int i = 0; i < count; i++) {
                        final User value = values.get(i);
                        final String valueText = value.getDisplayName()
                                .toString().toLowerCase();

                        if (valueText.startsWith(prefixString)) {
                            newValues.add(value);
                        } else {
                            final String[] words = valueText.split(" ");
                            final int wordCount = words.length;

                            for (int k = 0; k < wordCount; k++) {
                                if (words[k].startsWith(prefixString)) {
                                    newValues.add(value);
                                    break;
                                }
                            }
                        }
                    }

                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint,
                    FilterResults results) {
                values = (List<User>) results.values;

                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        }
    }
}