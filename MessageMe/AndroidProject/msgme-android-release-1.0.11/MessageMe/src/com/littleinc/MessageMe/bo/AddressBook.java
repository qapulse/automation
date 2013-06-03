package com.littleinc.MessageMe.bo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.ResourceException;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.SparseArray;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.StringUtil;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeConstants.InAppNotificationTargetScreen;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.metrics.MMTracker;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Commands.PBCommandABUpload;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.ui.CountryListActivity;
import com.littleinc.MessageMe.util.BatchTask;
import com.littleinc.MessageMe.util.MessageUtil;
import com.littleinc.MessageMe.util.NetUtil;

/**
 * This Class provides methods to interact and manage the state of the address
 * book sync
 */
public class AddressBook {

    private static final String EMAILS_JSON_NAME = "emails";

    private static final String PHONES_JSON_NAME = "phones";

    private static final String LAST_NAME_JSON_NAME = "lastName";

    private static final String FIRST_NAME_JSON_NAME = "firstName";

    private Dialog alert;

    /**
     * Possible states of the address book sync XXX Update this enum could cause
     * problems to live users so avoid modify this for now
     */
    public enum AddressBookState {
        AB_STATE_AUTHORIZED, AB_STATE_DENIED;
    }

    private static AddressBook instance;

    private Context context;

    private AddressBook(Context context) {
        this.context = context;
    }

    public static AddressBook getInstance(Context context) {
        if (instance == null) {
            instance = new AddressBook(context);
        } else {
            instance.context = context;
        }

        return instance;
    }

    /**
     * Sets the AB state as authorized and perform a sync process
     * 
     * @param uploadAll
     *            This flag defines if the process should upload the entire
     *            address book or just the changes
     */
    public void registerForAddressBookSyncing() {
        MessageMeApplication.getPreferences().setABState(
                AddressBookState.AB_STATE_AUTHORIZED);
        syncAndUploadAddressBook();
    }

    /**
     * Checks the current AB state and perform the corresponding action
     */
    public void askForAllPermissionsAndUploadAll(Activity activity) {
        MessageMeAppPreferences appPreferences = MessageMeApplication
                .getPreferences();

        if (appPreferences.getABState() == null) {
            askForAddressBookSyncPermission(activity);
        } else {
            switch (appPreferences.getABState()) {
            case AB_STATE_AUTHORIZED:
                syncAndUploadAddressBook();
                break;
            case AB_STATE_DENIED:
                LogIt.w(this, "Address book sync is disable");
                break;
            }
        }
    }

    private void askForAddressBookSyncPermission(Activity activity) {

        ABYesOnClickListener alertSyncContactsOnYes = new ABYesOnClickListener(
                activity);

        alert = UIUtil.confirmYesNo(activity,
                R.string.alert_sync_contacts_title,
                R.string.alert_sync_contacts_message, alertSyncContactsOnYes,
                alertSyncContactsOnNo);
    }

    public Dialog getAlert() {
        return alert;
    }

    /**
     * Performs an address book upload call if the sync is authorized
     * 
     * @param uploadAll
     *            This flag defines if the process should upload the entire
     *            address book or just the changes
     */
    private void syncAndUploadAddressBook() {
        if (AddressBookState.AB_STATE_AUTHORIZED == MessageMeApplication
                .getPreferences().getABState()
                && NetUtil.checkInternetConnection(context)) {

            new BackgroundTask() {

                PBCommandABUpload abUpload;

                SparseArray<List<ABContactInfo>> contactInfoMap;

                @Override
                public void work() {
                    contactInfoMap = getABContactsMap(context);

                    if ((contactInfoMap == null)
                            || (contactInfoMap.size() == 0)) {
                        LogIt.d(AddressBook.this,
                                "No new AB contacts to upload");
                        return;
                    }

                    try {
                        LogIt.i(AddressBook.this, "AB upload started");
                        PBCommandEnvelope commandEnvelope = RestfulClient
                                .getInstance().abUpload(
                                        buildAddressBookJson(contactInfoMap));
                        LogIt.i(AddressBook.this, "AB upload completed");

                        if (commandEnvelope.hasError()) {
                            LogIt.w(AddressBook.class, commandEnvelope
                                    .getError(), commandEnvelope.getError()
                                    .getReason(),
                                    "Error uploading address book");
                            fail(commandEnvelope.getError().getReason());
                            return;
                        }

                        abUpload = commandEnvelope.getAbUpload();
                    } catch (ResourceException e) {
                        LogIt.w(AddressBook.class, e,
                                "ResourceException uploading address book");
                        fail(e);
                    } catch (IOException e) {
                        LogIt.w(AddressBook.class, e,
                                "IOException uploading address book");
                        fail(e);
                    } catch (Exception e) {
                        LogIt.e(AddressBook.class, e,
                                "Unexpected Exception uploading address book");
                        fail(e);
                    }
                }

                @Override
                public void done() {

                    if (!failed() && (contactInfoMap.size() > 0)) {
                        new BatchTask() {

                            @Override
                            public void work() {
                                int matchCount = abUpload.getClientIDsCount();
                                // track the ab matches for the first session
                                MMFirstSessionTracker.getInstance().abacus(
                                        null, "ab_match", null, null,
                                        matchCount);

                                for (int i = 0; i < matchCount; i++) {
                                    MatchedABRecord.save(
                                            abUpload.getClientIDs(i),
                                            abUpload.getUserIDs(i));
                                }

                                for (int i = 0; i < contactInfoMap.size(); i++) {
                                    List<ABContactInfo> entries = contactInfoMap
                                            .valueAt(i);

                                    for (ABContactInfo entry : entries) {
                                        entry.save();
                                    }
                                }

                                if (matchCount <= 0) {
                                    LogIt.d(AddressBook.class,
                                            "No address book matches with existing MessageMe users");
                                } else {
                                    Context context = MessageMeApplication
                                            .getInstance();

                                    LocalBroadcastManager broadcastManager = LocalBroadcastManager
                                            .getInstance(context);

                                    // Generate an in-app notification for any "Friends Found"
                                    // events. These are very important for growth, so they do
                                    // not respect the Alert blocks, and they also appear on the
                                    // main Messages tab (other in-app notifications usually do not).
                                    if (matchCount == 1) {
                                        LogIt.d(AddressBook.class,
                                                "Show in-app notification for single matched contact",
                                                abUpload.getUserIDs(0));

                                        // This will show the contacts profile page for
                                        // the single matched contact. We don't have the
                                        // contact name or photo at this time, so we leave
                                        // them blank.
                                        MessagingService
                                                .sendInAppNotification(
                                                        context.getString(R.string.pabmo),
                                                        context.getString(R.string.ab_match_desc),
                                                        null,
                                                        abUpload.getUserIDs(0),
                                                        InAppNotificationTargetScreen.CONTACT_PROFILE,
                                                        broadcastManager);
                                    } else {
                                        LogIt.d(AddressBook.class,
                                                "Show in-app notification for "
                                                        + matchCount
                                                        + " matched contacts");

                                        String title = String.format(context
                                                .getString(R.string.pabm),
                                                matchCount);

                                        // As multiple users were matched we just show the
                                        // Contacts tab and provide the MM_SYSTEM_NOTICE_ID
                                        // that will never match a specific thread
                                        MessagingService
                                                .sendInAppNotification(
                                                        title,
                                                        context.getString(R.string.ab_match_desc),
                                                        null,
                                                        MessageMeConstants.SYSTEM_NOTICES_CHANNEL_ID,
                                                        InAppNotificationTargetScreen.CONTACTS_TAB,
                                                        broadcastManager);
                                    }
                                }

                                LogIt.d(AddressBook.class,
                                        "Address book changes registered");
                            }
                        };
                    }
                }
            };
        } else {
            LogIt.w(AddressBook.this,
                    "Address book sync postponed, no internet connection available");
        }
    }

    /**
     * This method parses a give map with the contacts info into the json
     * expected by the backend
     * 
     * Here is an example of the JSON built 
     * {
     *   "3":{
     *      "firstName":"Android",
     *      "lastName":"Froyo",
     *      "phones":[
     *         "###########",
     *         "###########"
     *      ],
     *      "emails":[
     *         "android@gmail.com",
     *         "froyo@hotmail.com"
     *      ]
     *   }
     * }
     */
    private JSONObject buildAddressBookJson(
            SparseArray<List<ABContactInfo>> contactInfoMap)
            throws JSONException {
        JSONObject addressbookJson = new JSONObject();

        for (int i = 0; i < contactInfoMap.size(); i++) {
            JSONObject contactInfo = new JSONObject();
            List<ABContactInfo> contactEntries = contactInfoMap.valueAt(i);

            JSONArray emails = new JSONArray();
            for (ABContactInfo emailEntry : contactEntries) {
                if (emailEntry.isEmail()) {
                    emails.put(emailEntry.getEmail());
                }
            }

            JSONArray phones = new JSONArray();
            for (ABContactInfo phoneEntry : contactEntries) {
                if (phoneEntry.isPhone()) {
                    phones.put(phoneEntry.getPhone());
                }
            }

            ABContactInfo structuredNameEntry = ABContactInfo
                    .findContactNameInfo(contactEntries);

            contactInfo.put(EMAILS_JSON_NAME, emails);
            contactInfo.put(PHONES_JSON_NAME, phones);

            if (structuredNameEntry != null) {
                contactInfo.put(LAST_NAME_JSON_NAME,
                        structuredNameEntry.getLastName());
                contactInfo.put(FIRST_NAME_JSON_NAME,
                        structuredNameEntry.getFirstName());
            }

            addressbookJson.put(String.valueOf(contactInfoMap.keyAt(i)),
                    contactInfo);
        }

        LogIt.d(this, "Built address book JSON", contactInfoMap.size());

        return addressbookJson;
    }

    /**
     * Queries the ContactsProvider and returns a map with the information of
     * each contact
     */
    public static SparseArray<List<ABContactInfo>> getABContactsMap(
            Context context) {
        SparseArray<List<ABContactInfo>> results = null;

        String sortOrder = Data.CONTACT_ID;

        String[] projection = new String[] { Data.CONTACT_ID, Data._ID,
                Data.DISPLAY_NAME, Data.DATA_VERSION, Data.MIMETYPE,
                Email.DATA, Phone.DATA, StructuredName.GIVEN_NAME,
                StructuredName.FAMILY_NAME };

        String selection = new StringBuilder().append("(")
                .append(Data.MIMETYPE).append("='")
                .append(Email.CONTENT_ITEM_TYPE).append("'").append(" AND ")
                .append(Email.DATA).append("<>'')").append(" OR ").append("(")
                .append(Data.MIMETYPE).append("='")
                .append(Phone.CONTENT_ITEM_TYPE).append("'").append(" AND ")
                .append(Phone.DATA).append("<>'')").append(" OR ")
                .append(Data.MIMETYPE).append("='")
                .append(StructuredName.CONTENT_ITEM_TYPE).append("'")
                .toString();

        Cursor contactsCursor = null;

        try {
            contactsCursor = context.getContentResolver().query(
                    Data.CONTENT_URI, projection, selection, null, sortOrder);

            results = ABContactInfo.parseFromToMap(contactsCursor);
        } catch (Exception e) {
            LogIt.e(MessageUtil.class, e, e.getMessage());
        } finally {
            if (contactsCursor != null) {
                contactsCursor.close();
            }
        }

        return results;
    }

    /**
     * Common handler for collecting the user's country with CountryListActivity
     * before syncing the address book.
     * 
     * This is only required if the user does not have a registered phone
     * number.
     */
    public static void onActivityResult(Activity activity, int requestCode,
            int resultCode, Intent data) {
        if (requestCode == MessageMeConstants.COUNTRY_SELECTION_REQUEST_CODE) {
            switch (resultCode) {
            case Activity.RESULT_OK:
                int countryCode = Integer.valueOf(data
                        .getStringExtra(MessageMeConstants.EXTRA_CONTRY_CODE));

                LogIt.d(AddressBook.class,
                        "Set the user country and sync the address book",
                        countryCode);

                MessageMeApplication.getPreferences().setUserCountryCode(
                        countryCode);

                break;
            default:
                LogIt.d(AddressBook.class,
                        "Canceled choosing country, sync address book anyway");
                break;
            }

            AddressBook ab = AddressBook.getInstance(activity);
            ab.registerForAddressBookSyncing();
        } else {
            LogIt.w(AddressBook.class, "Unexpected call to onActivityResult",
                    requestCode, resultCode);
        }
    }

    private class ABYesOnClickListener implements OnClickListener {

        Activity mActivity;

        public ABYesOnClickListener(Activity activity) {
            mActivity = activity;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            LogIt.user(this, "Pressed 'yes', enabling address book sync");
            // track that we got sync permissions
            MMTracker.getInstance().abacus("perms", "grant", "ab_sync", null,
                    null);

            if (StringUtil.isEmpty(MessageMeApplication.getPreferences()
                    .getRegisteredPhoneNumber())) {
                LogIt.d(this,
                        "No registered phone number, ask user their country");
                Intent intent = new Intent(mActivity, CountryListActivity.class);
                intent.putExtra(CountryListActivity.EXTRA_INPUT_TITLE,
                        mActivity.getString(R.string.choose_country_title));
                intent.putExtra(CountryListActivity.EXTRA_INPUT_LEGEND_TEXT,
                        mActivity.getString(R.string.choose_country_legend));
                mActivity.startActivityForResult(intent,
                        MessageMeConstants.COUNTRY_SELECTION_REQUEST_CODE);
            } else {
                LogIt.d(this,
                        "We have a registered phone number, go straight into syncing");
                registerForAddressBookSyncing();
            }

            dialog.dismiss();
            alert = null;
        }
    };

    private OnClickListener alertSyncContactsOnNo = new OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            LogIt.user(this, "Pressed 'no', disabling address book sync");
            // track the deny
            MMTracker.getInstance().abacus("perms", "deny", "ab_sync", null,
                    null);

            MessageMeApplication.getPreferences().setABState(
                    AddressBookState.AB_STATE_DENIED);

            dialog.dismiss();
            alert = null;
        }
    };

    public static int countNonMMUsersInAddressBook(Context context)
            throws SQLException {
        List<MatchedABRecord> matchedABRecords = MatchedABRecord.getDao()
                .queryForAll();

        Cursor contactsCursor = null;
        String[] projection = new String[] { Data._ID };

        String selection = buildSelectionToShowOnlyPhonesAndEmails();
        selection = buildSelectionToExcludeMMUsers(selection, matchedABRecords);

        try {
            contactsCursor = context.getContentResolver().query(
                    Data.CONTENT_URI, projection, selection, null, null);

            if (contactsCursor != null) {
                return contactsCursor.getCount();
            }
        } finally {

            if (contactsCursor != null) {
                contactsCursor.close();
            }
        }

        return 0;
    }

    public static List<ABContactInfo> loadNonMMUsersInAddressBook(
            Context context) throws SQLException {

        Cursor contactsCursor = null;
        List<MatchedABRecord> matchedABRecords = MatchedABRecord.getDao()
                .queryForAll();

        String sortOrder = Data.DISPLAY_NAME;
        String[] projection = new String[] { Data.DISPLAY_NAME, Data.MIMETYPE,
                Email.DATA, Phone.DATA, Email.TYPE, Phone.TYPE };

        String selection = buildSelectionToShowOnlyPhonesAndEmails();
        selection = buildSelectionToExcludeMMUsers(selection, matchedABRecords);

        try {
            contactsCursor = context.getContentResolver().query(
                    Data.CONTENT_URI, projection, selection, null, sortOrder);

            return ABContactInfo.parseFromToList(contactsCursor);
        } finally {
            if (contactsCursor != null) {
                contactsCursor.close();
            }
        }
    }

    /**
     * 
     * @return ((mimetype='vnd.android.cursor.item/email_v2' AND data1<>'') OR 
     *          (mimetype='vnd.android.cursor.item/phone_v2' AND data1<>'' AND length(data1) > 6 AND data1 NOT LIKE '%#%'))
     */
    private static String buildSelectionToShowOnlyPhonesAndEmails() {
        return new StringBuilder().append("((").append(Data.MIMETYPE)
                .append("='").append(Email.CONTENT_ITEM_TYPE).append("'")
                .append(" AND ").append(Email.DATA).append("<>'')")
                .append(" OR ").append("(").append(Data.MIMETYPE).append("='")
                .append(Phone.CONTENT_ITEM_TYPE).append("'").append(" AND ")
                .append(Phone.DATA).append("<>''").append(" AND ")
                .append("length(").append(Phone.DATA).append(") > 6 AND ")
                .append(Phone.DATA).append(" NOT LIKE '%#%'))").toString();
    }

    /**
     * 
     * @return Original selection + AND contact_id NOT IN (XXXX,XXXX,XXXX,XXXX)
     */
    private static String buildSelectionToExcludeMMUsers(String selection,
            List<MatchedABRecord> matchedABRecords) {

        if (matchedABRecords.size() > 0) {

            StringBuilder builder = null;

            if (TextUtils.isEmpty(selection)) {
                builder = new StringBuilder(selection).append(Data.CONTACT_ID)
                        .append(" NOT IN (");
            } else {
                builder = new StringBuilder(selection).append(" AND ")
                        .append(Data.CONTACT_ID).append(" NOT IN (");
            }

            for (int i = 0; i < matchedABRecords.size(); i++) {

                if (i == 0) {
                    builder.append(matchedABRecords.get(i).getAbRecordId());
                } else {
                    builder.append(",").append(
                            matchedABRecords.get(i).getAbRecordId());
                }
            }
            builder.append(") ");

            return buildSelectionToExcludeCurrentUser(builder.toString());
        } else {
            return buildSelectionToExcludeCurrentUser(selection);
        }
    }

    /**
     * 
     * @return Original selection + AND (data1<>'XXXX@XXXX.XXX' AND
     *         data1<>'+###########')
     */
    private static String buildSelectionToExcludeCurrentUser(String selection) {

        User currentUser = MessageMeApplication.getCurrentUser();

        if (currentUser == null) {

            LogIt.w(AddressBook.class, "CurrentUser is null");
            return selection;
        } else {

            if (TextUtils.isEmpty(currentUser.getEmail())
                    && TextUtils.isEmpty(currentUser.getPhone())) {

                LogIt.w(AddressBook.class,
                        "CurrentUser is doesn't have any email or phone registered");
                return selection;
            } else {

                StringBuilder builder = new StringBuilder(selection);

                builder.append("AND (");
                if (!TextUtils.isEmpty(currentUser.getEmail())) {

                    builder.append(Email.DATA).append("<>'")
                            .append(currentUser.getEmail()).append("'");
                }

                if (!TextUtils.isEmpty(currentUser.getPhone())) {

                    builder.append(" AND ").append(Phone.DATA)
                            .append(" NOT LIKE '%")
                            .append(currentUser.getPhone()).append("%'");
                }
                builder.append(")");

                return builder.toString();
            }
        }
    }
}