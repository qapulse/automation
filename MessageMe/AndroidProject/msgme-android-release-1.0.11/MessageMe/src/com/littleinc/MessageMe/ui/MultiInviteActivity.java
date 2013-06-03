package com.littleinc.MessageMe.ui;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarListActivity;
import com.littleinc.MessageMe.bo.ABContactInfo;
import com.littleinc.MessageMe.bo.AddressBook;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.metrics.MMFirstWeekTracker;
import com.littleinc.MessageMe.metrics.MMTracker;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.sms.MMSmsSender;
import com.littleinc.MessageMe.sms.SmsInvite;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.DateUtil;
import com.littleinc.MessageMe.util.FriendsInviteUtil;
import com.littleinc.MessageMe.util.StringUtil;

@TargetApi(14)
public class MultiInviteActivity extends ActionBarListActivity {

    /**
     * Maximum number of invites that can be sent without request user
     * permission
     */
    public static final int MAX_NUM_SMS_INVITES_WITHOUT_PERMISSION = 15;

    /**
     * Request code to be used in the email send intent
     */
    public static final int SEND_INVITE_EMAIL_RQ = 150;

    public static final int SEARCH_CONTACT_RQ = 152;

    private static final int EMAILS_KEY = 1;

    private static final int PHONES_KEY = 2;

    private ListView contactList;

    private CheckBox checkboxAll;

    private TextView checkedContatcsLabel;

    private List<ABContactInfo> abContacts;

    private ABContactAdapter contactAdapter;

    private Integer checkedEmailsCount;

    private Integer checkedPhonesCount;

    private MenuItem sendMenuItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.invite_friends_label);
        setContentView(R.layout.multi_invite_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        init();
        updateUI();

        // first session tracking
        Integer order = MMLocalData.getInstance().getSessionOrder();
        MMFirstSessionTracker.getInstance().abacus(null, "invites", "screen",
                order, null);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        sendMenuItem = (MenuItem) menu.findItem(R.id.send_btn);

        return super.onPrepareOptionsMenu(menu);
    }

    private void init() {

        contactList = getListView();

        checkboxAll = (CheckBox) findViewById(R.id.multi_invite_check_all);
        checkedContatcsLabel = (TextView) findViewById(R.id.multi_invite_selected_friends);
    }

    @Override
    protected void onPause() {
        super.onPause();
        UIUtil.hideKeyboard(contactList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageMeApplication.getAppState().setAbContacts(null);
        MessageMeApplication.getAppState().setABContactsChecked(null);
    }

    private void updateUI() {

        if (contactList.getAdapter() == null) {

            final ProgressDialog progressDialog = UIUtil.showProgressDialog(
                    this, getString(R.string.loading));

            progressDialog.setCancelable(true);

            new DatabaseTask(handler) {

                @Override
                public void work() {

                    try {
                        abContacts = AddressBook
                                .loadNonMMUsersInAddressBook(MultiInviteActivity.this);
                        Collections.sort(abContacts, new SortContacts());

                        MessageMeApplication.getAppState().setAbContacts(
                                abContacts);
                    } catch (SQLException e) {
                        fail(e);
                    }
                }

                @Override
                public void done() {

                    if (!failed()) {

                        contactAdapter = new ABContactAdapter(
                                MultiInviteActivity.this);

                        fillAdapterAlphabeticallySorted(abContacts,
                                contactAdapter);

                        contactList.setAdapter(contactAdapter);
                        contactList.setOnItemClickListener(mOnContactClicked);

                        checkboxAll.setChecked(false);

                        updateBottomLabels();
                    }

                    progressDialog.dismiss();
                }
            };
        } else {
            contactAdapter.notifyDataSetChanged();
            updateBottomLabels();
        }

    }

    private void updateBottomLabels() {

        int numberChecked = contactAdapter.getCheckedCount();

        boolean isAllSelected = (numberChecked == contactAdapter
                .getContactsCount());

        checkboxAll.setOnCheckedChangeListener(null);
        checkboxAll.setChecked(isAllSelected ? true : false);
        checkboxAll
                .setText(isAllSelected ? getString(R.string.multi_invite_deselect_all)
                        : getString(R.string.multi_invite_select_all));
        checkboxAll.setOnCheckedChangeListener(mOnCheckAllListener);

        checkedContatcsLabel.setText(String.format(
                getString(R.string.multi_invite_friends_selected),
                contactAdapter.getCheckedCount()));

        if (sendMenuItem != null) {
            if (numberChecked == 0) {
                LogIt.d(this, "Disable send button");
                setMenuItemEnabled(sendMenuItem, false);
            } else {
                LogIt.d(this, "Enable send button");
                setMenuItemEnabled(sendMenuItem, true);
            }
        } else {
            LogIt.w(this, "Cannot update send button as menu item null");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.multi_invite_menu, menu);

        // Calling super after populating the menu is necessary here to ensure
        // that the action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case R.id.send_btn:

            LogIt.user(MultiInviteActivity.class, "Pressed send button");

            if (contactAdapter == null) {

                LogIt.w(MultiInviteActivity.class,
                        "Ignore send action, adapter is null");
            } else if (contactAdapter.getCheckedPhonesCount() > MAX_NUM_SMS_INVITES_WITHOUT_PERMISSION) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.confirm_invites_title);
                builder.setMessage(String.format(
                        getString(R.string.confirm_invites_body),
                        contactAdapter.getCheckedPhonesCount()));

                builder.setNegativeButton(R.string.cancel,
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {

                                LogIt.user(MultiInviteActivity.class,
                                        "Declined send more than 15 SMS invites");
                                dialog.cancel();
                            }
                        });
                builder.setPositiveButton(R.string.send_message,
                        new OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {

                                LogIt.user(MultiInviteActivity.class,
                                        "Accepted send more than 15 SMS invites");
                                startSendingInvites();
                            }
                        });

                builder.create().show();
            } else {

                startSendingInvites();
            }
            break;
        case R.id.search_btn:

            LogIt.user(MultiInviteActivity.class, "Pressed search button");

            MessageMeApplication.getAppState().setABContactsChecked(
                    contactAdapter.getCheckedEntries());

            Intent intent = new Intent(this, SearchABContactsActivity.class);
            startActivityForResult(intent, SEARCH_CONTACT_RQ);
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startSendingInvites() {

        LogIt.d(MultiInviteActivity.class, "Start Sending invites");
        SparseArray<List<ABContactInfo>> checkedEntries = contactAdapter
                .getCheckedEntriesByType();

        if (checkedEntries.size() > 0) {

            List<ABContactInfo> checkedPhones = checkedEntries.get(PHONES_KEY);
            List<ABContactInfo> checkedEmails = checkedEntries.get(EMAILS_KEY);

            checkedEmailsCount = checkedEmails.size();
            checkedPhonesCount = checkedPhones.size();

            if (checkedPhones.size() > 0) {
                // Tracking
                MMTracker.getInstance().abacus("invite", "start",
                        "sms_unselected0", null, checkedPhonesCount);
                MMFirstWeekTracker.getInstance().abacus(null, "start",
                        "sms_unselected0", null, checkedPhonesCount);
                MMFirstSessionTracker.getInstance().abacus(null, "start",
                        "sms_unselected0", null, checkedPhonesCount);

                for (ABContactInfo contactInfo : checkedPhones) {

                    SmsInvite invite = new SmsInvite();

                    invite.setPhoneNumber(contactInfo.getPhone());
                    invite.setMessageBody(getString(R.string.sms_invite_body));
                    invite.setDateCreated(DateUtil.now());

                    MMSmsSender.INSTANCE.addToQueue(invite);
                }
            } else {

                LogIt.d(MultiInviteActivity.class, "No SMS invites to send");
            }

            if (checkedEmails.size() == 0) {

                LogIt.d(MultiInviteActivity.class, "No emails invites to send");
                showThanksAlert();
            } else {

                // Tracking
                MMTracker.getInstance().abacus("invite", "start",
                        "email_unselected0", null, checkedEmailsCount);
                MMFirstWeekTracker.getInstance().abacus(null, "start",
                        "email_unselected0", null, checkedEmailsCount);
                MMFirstSessionTracker.getInstance().abacus(null, "start",
                        "email_unselected0", null, checkedEmailsCount);

                List<String> emails = new LinkedList<String>();
                JSONObject postData = new JSONObject();
                JSONArray postEmails = new JSONArray();

                for (ABContactInfo contactInfo : checkedEmails) {

                    // Check to avoid duplicate emails
                    if (!emails.contains(contactInfo.getEmail())
                            && StringUtil.isEmailValid(contactInfo.getEmail())) {
                        emails.add(contactInfo.getEmail());

                        try {
                            JSONObject postEmail = new JSONObject();
                            postEmail.put("first_name",
                                    contactInfo.getFirstName());
                            postEmail.put("last_name",
                                    contactInfo.getLastName());
                            postEmail.put("email", contactInfo.getEmail());

                            postEmails.put(postEmail);
                        } catch (JSONException e) {
                            LogIt.d(this, "posting email invites; json error:",
                                    e);
                        }
                    }
                }

                try {
                    postData.put("emails", postEmails);
                } catch (JSONException e) {
                }

                if (postEmails.length() > 0) {
                    final JSONObject inviteData = postData;
                    new BackgroundTask() {

                        @Override
                        public void work() {
                            RestfulClient.getInstance().contactInvite(
                                    inviteData);
                        }

                        @Override
                        public void done() {
                        }
                    };
                }

                Intent sendEmailIntent = IntentBuilder.from(this)
                        .addEmailTo(emails.toArray(new String[] {}))
                        .setChooserTitle(R.string.invite_friends_label)
                        .setSubject(getString(R.string.email_invite_subject))
                        .setText(getString(R.string.email_invite_body))
                        .setType(FriendsInviteUtil.MAIL_MIME_TYPE)
                        .createChooserIntent();

                startActivityForResult(sendEmailIntent, SEND_INVITE_EMAIL_RQ);
            }
        } else {

            LogIt.d(MultiInviteActivity.class, "Nothing selected to send");
        }
    }

    private void showThanksAlert() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.invites_sent_title);
        builder.setMessage(R.string.invites_sent_body);
        builder.setNeutralButton(R.string.okay, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                finish();
            }
        });

        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case SEND_INVITE_EMAIL_RQ:
            MMTracker.getInstance().abacus("invite", "send",
                    "email_unselected0", 1, checkedEmailsCount);
            MMFirstSessionTracker.getInstance().abacus(null, "send",
                    "email_unselected0", 1, checkedEmailsCount);
            MMFirstWeekTracker.getInstance().abacus(null, "send",
                    "email_unselected0", 1, checkedEmailsCount);

            LogIt.d(MultiInviteActivity.class,
                    "Email invites have been sent, show thanks dialog");
            showThanksAlert();
            break;
        case SEARCH_CONTACT_RQ:
            switch (resultCode) {
            case RESULT_OK:

                if (data != null
                        && data.hasExtra(MessageMeConstants.SELECTED_AB_CONTACT)) {

                    final ABContactInfo selectedContact = (ABContactInfo) data
                            .getSerializableExtra(MessageMeConstants.SELECTED_AB_CONTACT);

                    if (selectedContact != null) {

                        if (contactAdapter.containsCheckedItem(selectedContact)) {

                            contactAdapter.removeCheckedItem(selectedContact);
                        } else {

                            contactAdapter.addCheckedItem(selectedContact);
                        }
                        contactAdapter.notifyDataSetChanged();

                        // For some reason a simple post() is not able to update the UI all the time 
                        // because of that the postDelayed() with a short arbitrary value of 250 mills 
                        // have a better accuracy updating the list position
                        handler.postDelayed(new Runnable() {

                            @Override
                            public void run() {

                                int index = contactAdapter
                                        .getItemPosition(selectedContact);
                                contactList.setSelection(index);

                                updateBottomLabels();
                            }
                        }, 250);
                    } else {

                        LogIt.w(MultiInviteActivity.class,
                                "Received null contact from search");
                    }
                }
                break;
            default:

                LogIt.d(MultiInviteActivity.class, "Non AB Contact selected");
                break;
            }
            break;
        }
    }

    public void fillAdapterAlphabeticallySorted(List<ABContactInfo> contacts,
            ABContactAdapter adapter) {

        for (int i = 0; i < contacts.size(); i++) {

            if (!StringUtil.isEmpty(contacts.get(i).getDisplayName())) {

                if (i == 0) {

                    adapter.addSeparator(contacts.get(i));
                }

                adapter.addContact(contacts.get(i));

                if ((i < contacts.size() - 1)
                        && (!contacts
                                .get(i + 1)
                                .getNameInitial()
                                .equalsIgnoreCase(
                                        contacts.get(i).getNameInitial()))) {

                    adapter.addSeparator(contacts.get(i + 1));
                }
            }
        }
    }

    private OnItemClickListener mOnContactClicked = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            if (contactAdapter.getItemViewType(position) == ABContactAdapter.TYPE_CONTACT) {

                LogIt.user(MultiInviteActivity.class, "Pressed contact");
                CheckBox checkBox = (CheckBox) view
                        .findViewById(R.id.contact_entry_check);

                if (checkBox != null) {

                    checkBox.performClick();
                    ABContactInfo contact = (ABContactInfo) checkBox.getTag();

                    if (checkBox.isChecked()) {
                        contactAdapter.addCheckedItem(contact);
                    } else {
                        contactAdapter.removeCheckedItem(contact);
                    }

                    contactAdapter.notifyDataSetChanged();
                    updateBottomLabels();
                } else {
                    LogIt.w(MultiInviteActivity.class,
                            "Unable to perform click CheckBox is null");
                }
            } else {

                LogIt.user(MultiInviteActivity.class, "Pressed separator");
            }
        }
    };

    private OnCheckedChangeListener mOnCheckAllListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {

            if (isChecked) {

                contactAdapter.selectAll();
                LogIt.user(MultiInviteActivity.class, "Deselected all contacts");
            } else {

                contactAdapter.deselectAll();
                LogIt.user(MultiInviteActivity.class, "Selected all contacts");
            }

            updateBottomLabels();
        }
    };

    private class ABContactAdapter extends BaseAdapter {

        public static final int TYPE_CONTACT = 0;

        public static final int TYPE_SEPARATOR = 1;

        private LayoutInflater inflater;

        private List<ABContactInfo> abContacts;

        private Set<ABContactInfo> checkedABContacts;

        private TreeSet<Integer> separatorsSet = new TreeSet<Integer>();

        public ABContactAdapter(Context context) {

            abContacts = new ArrayList<ABContactInfo>();
            checkedABContacts = new HashSet<ABContactInfo>();

            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return abContacts.size();
        }

        @Override
        public ABContactInfo getItem(int position) {

            if (abContacts != null && position < abContacts.size()) {

                return abContacts.get(position);
            } else if (abContacts != null) {

                LogIt.w(ABContactAdapter.class, "OutOfBounds", position + "/"
                        + abContacts.size());

                return null;
            } else {

                LogIt.w(ABContactAdapter.class, "abContacts list is null");

                return null;
            }
        }

        public int getItemPosition(ABContactInfo contact) {
            return abContacts.indexOf(contact);
        }

        public boolean addCheckedItem(ABContactInfo contact) {
            return checkedABContacts.add(contact);
        }

        public boolean containsCheckedItem(ABContactInfo contact) {
            return checkedABContacts.contains(contact);
        }

        public boolean removeCheckedItem(ABContactInfo contact) {
            return checkedABContacts.remove(contact);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return separatorsSet.contains(position) ? TYPE_SEPARATOR
                    : TYPE_CONTACT;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        public void addSeparator(ABContactInfo contact) {

            ABContactInfo contactSeparator = new ABContactInfo();
            contactSeparator.setDisplayName(contact.getDisplayName());

            abContacts.add(contactSeparator);
            separatorsSet.add(abContacts.size() - 1);
        }

        public void addContact(ABContactInfo contact) {

            abContacts.add(contact);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;

            if (convertView == null) {

                holder = new ViewHolder();

                if (TYPE_CONTACT == getItemViewType(position)) {

                    convertView = inflater.inflate(R.layout.contact_entry,
                            parent, false);

                    holder.checkBox = (CheckBox) convertView
                            .findViewById(R.id.contact_entry_check);
                    holder.nameLabel = (TextView) convertView
                            .findViewById(R.id.contact_entry_name);
                    holder.contactInfoLabel = (TextView) convertView
                            .findViewById(R.id.contact_entry_info);
                    holder.contactTypeLabel = (TextView) convertView
                            .findViewById(R.id.contact_entry_type);
                } else if (TYPE_SEPARATOR == getItemViewType(position)) {

                    convertView = inflater.inflate(
                            R.layout.layout_listview_separator, parent, false);

                    holder.separator = (TextView) convertView
                            .findViewById(R.id.separator);
                }

                convertView.setTag(holder);
            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            ABContactInfo contact = getItem(position);

            if (TYPE_CONTACT == getItemViewType(position)) {

                holder.nameLabel.setText(contact.getDisplayName());
                holder.contactInfoLabel.setText(contact.getData());

                if (checkedABContacts.contains(contact)) {
                    holder.checkBox.setChecked(true);
                } else {
                    holder.checkBox.setChecked(false);
                }
                holder.checkBox.setTag(contact);

                holder.contactTypeLabel.setText(getString(
                        contact.getTypeLabelResource()).toUpperCase());
            } else if (TYPE_SEPARATOR == getItemViewType(position)) {

                holder.separator.setText(contact.getNameInitial());
            }

            return convertView;
        }

        public void selectAll() {

            checkedABContacts.clear();
            checkedABContacts.addAll(abContacts);

            notifyDataSetChanged();
        }

        public void deselectAll() {

            checkedABContacts.clear();

            notifyDataSetChanged();
        }

        /**
         * Counts all the contacts selected
         */
        public int getCheckedCount() {

            int count = 0;
            for (int i = 0; i < abContacts.size(); i++) {

                if (getItemViewType(i) == TYPE_CONTACT) {

                    ABContactInfo contactInfo = abContacts.get(i);

                    if (checkedABContacts.contains(contactInfo)) {
                        count++;
                    }
                }
            }

            return count;
        }

        /**
         * Counts all the contacts, ignore separators
         */
        public int getContactsCount() {

            int count = 0;
            for (int i = 0; i < abContacts.size(); i++) {

                if (getItemViewType(i) == TYPE_CONTACT) {
                    count++;
                }
            }

            return count;
        }

        /**
         * Counts all the contact phones selected
         */
        public int getCheckedPhonesCount() {

            int count = 0;
            for (int i = 0; i < abContacts.size(); i++) {

                if (getItemViewType(i) == TYPE_CONTACT) {

                    ABContactInfo contactInfo = abContacts.get(i);

                    if (checkedABContacts.contains(contactInfo)
                            && contactInfo.isPhone()) {
                        count++;
                    }
                }
            }

            return count;
        }

        public SparseArray<List<ABContactInfo>> getCheckedEntriesByType() {

            SparseArray<List<ABContactInfo>> sparseArray = new SparseArray<List<ABContactInfo>>();

            List<ABContactInfo> checkedPhones = new LinkedList<ABContactInfo>();
            List<ABContactInfo> checkedEmails = new LinkedList<ABContactInfo>();

            for (ABContactInfo abContactInfo : checkedABContacts) {

                if (abContactInfo.isEmail()) {
                    checkedEmails.add(abContactInfo);
                } else if (abContactInfo.isPhone()) {
                    checkedPhones.add(abContactInfo);
                }
            }

            sparseArray.put(EMAILS_KEY, checkedEmails);
            sparseArray.put(PHONES_KEY, checkedPhones);

            return sparseArray;
        }

        public List<ABContactInfo> getCheckedEntries() {

            List<ABContactInfo> entries = new LinkedList<ABContactInfo>();

            for (ABContactInfo abContactInfo : checkedABContacts) {
                entries.add(abContactInfo);
            }

            return entries;
        }

        class ViewHolder {

            CheckBox checkBox;

            TextView nameLabel;

            TextView contactInfoLabel;

            TextView contactTypeLabel;

            TextView separator;
        }
    }

    /**
     * Sorts the contacts using the whole display name
     */
    public static class SortContacts implements Comparator<ABContactInfo> {

        @Override
        public int compare(ABContactInfo lhs, ABContactInfo rhs) {
            ABContactInfo contactOne = (ABContactInfo) lhs;
            ABContactInfo contactTwo = (ABContactInfo) rhs;

            return contactOne.getDisplayName().compareToIgnoreCase(
                    contactTwo.getDisplayName());
        }
    }
}
