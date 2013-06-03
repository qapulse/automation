package com.littleinc.MessageMe.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarFragmentActivity;
import com.littleinc.MessageMe.bo.AddressBook;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.MessageMeAppPreferences;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.metrics.mixpanel.MixpanelAPI;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.StringUtil;
import com.littleinc.MessageMe.widget.PagerAdapter;

@TargetApi(14)
public class TabsFragmentActivity extends ActionBarFragmentActivity implements
        TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {

    public static final String EXTRA_TAB_TO_SHOW = "tab_to_show";

    /**
     * Constants that identify each of the tabs.
     */
    public static final String TAB_CONTACTS = "tab1";

    public static final String TAB_MESSAGES = "tab2";

    public static final String TAB_PROFILE = "tab3";

    public static final int TAB_PROFILE_POSITION = 2;

    private Map<String, TabInfo> mapTabInfo = new HashMap<String, TabInfo>();

    private TabHost tabHost;

    /** 
     * Container for displaying or hiding in-app notifications
     */
    private FrameLayout notificationsContainer;

    private MenuItem searchContact;

    private MenuItem addContact;

    private MenuItem searchMessage;

    private MenuItem composeMessage;

    private MenuItem editProfile;

    private ViewPager viewPager;

    private PagerAdapter pagerAdapter;
    
    private TextView messagesCounter;

    private static Dialog alert;

    /**
     * Whether the GCM error popup has been shown yet.  This is static 
     * so it persists for the lifetime of the application, as we don't
     * want it to be shown each time the orientation changes.
     */
    private static boolean sHasGCMErrorBeenShown = false;

    private class TabInfo {
        private String tag;

        private Class<?> clss;

        private Bundle args;

        private Fragment fragment;

        TabInfo(String tag, Class<?> clazz, Bundle args) {
            this.tag = tag;
            this.clss = clazz;
            this.args = args;
        }

        public Fragment getFragment() {
            return fragment;
        }

        public void setFragment(Fragment fragment) {
            this.fragment = fragment;
        }
    }

    class TabFactory implements TabContentFactory {

        private Context context;

        public TabFactory(Context context) {
            this.context = context;
        }

        @Override
        public View createTabContent(String tag) {
            View view = new View(this.context);
            view.setMinimumWidth(0);
            view.setMinimumHeight(0);
            return view;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            LogIt.d(TabsFragmentActivity.class,
                    "onCreate - activity is being created");
        } else {
            LogIt.d(TabsFragmentActivity.class,
                    "onCreate - activity is being recreated");
        }

        MessageMeApplication.registerWithCrittercism(this);

        // If the caller intent is from the recent apps and has the channelId extra
        // we should remove it to avoid open the chat thread again
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {

            getIntent().removeExtra(MessageMeConstants.RECIPIENT_ID_KEY);
            getIntent().removeExtra(EXTRA_TAB_TO_SHOW);
            LogIt.d(TabsFragmentActivity.class,
                    "Activity being relaunched, remove extras");
        }

        if (MessageMeApplication.getCurrentUser() == null) {

            LogIt.w(this,
                    "onCreate - current user is null, return to landing page");
            Intent intent = new Intent(this, LandingActivity.class);
            startActivity(intent);
            finish();

            // By default onCreate() will continue to the end unless we
            // manually exit it.
            return;
        }

        setContentView(R.layout.tabhost_layout);

        intialiseViewPager();

        initialiseTabHost(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(false);
            actionBar.setIcon(R.drawable.actionbar_top_icon_logo);
        }

        // If the tab to show was provided in the intent then use that,
        // otherwise try to restore the tab that was previously being shown.
        if (!changeTabIfRequired(getIntent())) {
            String tabToShow = "";

            if (savedInstanceState != null) {
                tabToShow = savedInstanceState.getString("tab");
                LogIt.d(this, "Show tab that was previously being viewed",
                        tabToShow);
            } else {
                tabToShow = TAB_MESSAGES;
            }
            tabHost.setCurrentTabByTag(tabToShow);
        }

        notificationsContainer = (FrameLayout) findViewById(R.id.notification_container);

        // Call ab/upload just after the login or signup and every time that the app launched
        AddressBook addressBook = AddressBook.getInstance(this);
        addressBook.askForAllPermissionsAndUploadAll(this);

        // TODO: Figure out how auto dismiss this dialog when an auto-logout is fired
        alert = addressBook.getAlert();

        MessageMeAppPreferences prefs = MessageMeApplication.getPreferences();

        int launchCount = prefs.getLaunchCount();
        ++launchCount;
        prefs.setLaunchCount(launchCount);

        LogIt.d(this, "App launch count is now", launchCount);

        // We only check if the rate app popup should appear if the AB dialog 
        // is not present after open the application
        if (alert == null) {
            checkIfRatePopUpIsNeeded();
        }

        checkAccountAndStoreState();

        Intent receivedIntent = getIntent();
        String action = receivedIntent.getAction();
        String type = receivedIntent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                String fileName = handleReceivedImage(receivedIntent);
                if (!StringUtil.isEmpty(fileName)) {

                    Intent intent = new Intent(this,
                            MessageContactActivity.class);
                    intent.putExtra(MessageMeConstants.EXTRA_IMAGE_KEY,
                            fileName);
                    intent.putExtra(Message.TYPE_COLUMN,
                            IMessageType.PHOTO_VALUE);
                    startActivity(intent);

                } else {
                    LogIt.w(this, "Can't forward image, file is null");
                    UIUtil.alert(this,
                            R.string.sharing_image_not_available_title,
                            R.string.sharing_image_not_available_message);
                }
            } else {
                LogIt.w(this, "Ignore unrecognized ACTION_SEND intent", type);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MixpanelAPI api = MixpanelAPI.getInstance(getApplicationContext(), "");
        api.flush();
    }

    /**
     * Obtains the corresponding file from the Intent
     */
    private String handleReceivedImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        File destFile = null;

        if (imageUri != null) {

            String filePath = FileSystemUtil.getRealPathFromURI(imageUri, this);

            String fileExtension = FileSystemUtil.getExtension(filePath);

            if (StringUtil.isEmpty(fileExtension)) {
                fileExtension = MessageMeConstants.PHOTO_MESSAGE_EXTENSION;
            }

            //String fileExtension = MessageMeConstants.PHOTO_MESSAGE_EXTENSION;

            String randomFileName = new StringBuilder()
                    .append(StringUtil.getRandomFilename())
                    .append(fileExtension).toString();
            try {
                destFile = FileSystemUtil.downloadFileFromUri(randomFileName,
                        ImageLoader.getAppCacheDirMedia(), imageUri);

            } catch (FileNotFoundException e) {
                LogIt.e(this, e, e.getMessage());
            } catch (IOException e) {
                LogIt.e(this, e, e.getMessage());
            } catch (Exception e) {
                LogIt.e(this, e, e.getMessage());
            }
        } else {
            LogIt.e(this, "Can't process received image, URI is null");
        }
        return destFile != null && destFile.exists() ? destFile
                .getAbsolutePath() : null;
    }

    private void checkIfRatePopUpIsNeeded() {
        if (MessageMeApplication.shouldShowRateAppPopup()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.rating_prompt_title);
            builder.setMessage(R.string.rating_prompt_msg);
            builder.setPositiveButton(R.string.rating_prompt_option_rate,
                    new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogIt.user(TabsFragmentActivity.class,
                                    "Pressed rate app, opening google play store");

                            MessageMeApplication.getPreferences().setAppRated(
                                    true);
                            MessageMeApplication
                                    .updateLastTimeShowedRatePopup();

                            startActivity(new Intent(Intent.ACTION_VIEW, Uri
                                    .parse("market://details?id="
                                            + getPackageName())));

                            dialog.dismiss();
                        }
                    });
            builder.setNeutralButton(R.string.remind_me_later,
                    new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogIt.user(TabsFragmentActivity.class,
                                    "Pressed remind me later");

                            MessageMeApplication.getPreferences().setAppRated(
                                    false);
                            MessageMeApplication
                                    .updateLastTimeShowedRatePopup();

                            dialog.cancel();
                        }
                    });

            Dialog alert = builder.create();

            // TODO: Figure out how auto dismiss this dialog when an auto-logout is fired
            alert.show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MessageMeConstants.COUNTRY_SELECTION_REQUEST_CODE) {
            // Delegate the result back to the AddressBook class
            AddressBook.onActivityResult(this, requestCode, resultCode, data);
        } else {
            LogIt.w(this, "Unexpected call to onActivityResult", requestCode,
                    resultCode);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Checks if a google account and the Android Play Store is installed 
     * on the device
     */
    private void checkAccountAndStoreState() {

        if (sHasGCMErrorBeenShown) {
            LogIt.d(this,
                    "GCM error popup was already shown, don't show it again");
        } else if (!MessageMeApplication.getPreferences()
                .isGCMNotificationsAvailable()) {

            sHasGCMErrorBeenShown = true;

            boolean isAccountSetup = DeviceUtil
                    .checkIsGoogleAccountRegistered(this);

            boolean isPlayAppInstalled = DeviceUtil
                    .checkIfPlayMarketInstalled(this);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                // Checks if the current OS version is > 4.0.3
                // For devices that uses 4.0.4 or greater, GCM only requires the Play
                // Store installed, for lower versions, requires the Play Store and a
                // Google Account setup on the device
                if (!isPlayAppInstalled) {
                    LogIt.w(this, "no play app installed, can't setup gcm");
                    UIUtil.alert(this, R.string.no_gcm_notification_title,
                            R.string.no_gcm_notification_message_play_app);
                } else {
                    LogIt.w(this, "unrecognized error setting up gcm");
                    UIUtil.alert(this, R.string.no_gcm_notification_title,
                            R.string.no_gcm_notification_message_unrecognized);
                }
            } else {
                if (!isAccountSetup && !isPlayAppInstalled) {
                    LogIt.d(this,
                            "no google account and play app, can't setup GCM");
                    UIUtil.alert(this, R.string.no_gcm_notification_title,
                            R.string.no_gcm_notification_message_both);
                } else if (!isAccountSetup) {
                    LogIt.w(this, "no google account, can't setup gcm");
                    UIUtil.alert(this, R.string.no_gcm_notification_title,
                            R.string.no_gcm_notification_message_account);
                } else if (!isPlayAppInstalled) {
                    LogIt.w(this, "no play app installed, can't setup gcm");
                    UIUtil.alert(this, R.string.no_gcm_notification_title,
                            R.string.no_gcm_notification_message_play_app);
                } else {
                    LogIt.w(this, "unrecognized error setting up gcm");
                    UIUtil.alert(this, R.string.no_gcm_notification_title,
                            R.string.no_gcm_notification_message_unrecognized);
                }
            }
        }
    }

    public static Dialog getDialog() {
        return alert;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.tabs_menu, menu);

        // Calling super after populating the menu is necessary here to ensure
        // that the action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        searchMessage = (MenuItem) menu
                .findItem(R.id.top_actionbar_search_message);
        searchContact = (MenuItem) menu
                .findItem(R.id.top_actionbar_search_contacts);
        editProfile = (MenuItem) menu.findItem(R.id.top_actionbar_edit);
        addContact = (MenuItem) menu.findItem(R.id.top_actionbar_add_contact);
        composeMessage = (MenuItem) menu.findItem(R.id.top_actionbar_compose);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            //finish();
            break;
        case R.id.top_actionbar_search_contacts:
            searchContactsOption();
            break;
        case R.id.top_actionbar_add_contact:
            addContactOption();
            break;
        case R.id.top_actionbar_search_message:
            searchMessageOption();
            break;
        case R.id.top_actionbar_compose:
            composeMessageOption();
            break;
        case R.id.top_actionbar_edit:
            editUserProfileOption();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            setHomeIcon(R.drawable.actionbar_top_icon_logo);
            disableHomeButton();
            // Needs to hide these menu items  here
            // Per a compatibility issue with pre HoneyCumb devices
            hideMenuItem(searchContact);
            hideMenuItem(addContact);
            hideMenuItem(editProfile);
        }
    }

    /**
     * This Activity has the launchMode 'singleTop' to avoid open more than one instance of this activity,
     * for example each time that a GCM is pressed. This onNewIntent method is used to override the intent 
     * instance returned by the getIntent()
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        LogIt.d(TabsFragmentActivity.class, "New intent received", intent);
        setIntent(intent);

        changeTabIfRequired(getIntent());
    }

    private boolean changeTabIfRequired(Intent intent) {
        String tabToShow = intent.getStringExtra(EXTRA_TAB_TO_SHOW);

        if (tabToShow != null) {
            if (tabToShow.equalsIgnoreCase(TAB_CONTACTS)
                    || tabToShow.equalsIgnoreCase(TAB_MESSAGES)
                    || tabToShow.equalsIgnoreCase(TAB_PROFILE)) {
                LogIt.d(this, "Show tab received in intent", tabToShow);
                tabHost.setCurrentTabByTag(tabToShow);
                return true;
            } else {
                LogIt.w(this, "Ignore unexpected value in EXTRA_TAB_TO_SHOW",
                        tabToShow);
            }
        }

        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mNotificationHelper != null) {
            mNotificationHelper.closeFragment();
        }

        if ((alert != null) && alert.isShowing()) {
            alert.dismiss();
        }
    }

    public Handler getHandler() {
        return handler;
    }

    protected void onSaveInstanceState(Bundle outState) {
        // Save the tab selected
        outState.putString("tab", tabHost.getCurrentTabTag());
        super.onSaveInstanceState(outState);
    }

    /**
     * Initialise ViewPager
     */
    private void intialiseViewPager() {

        List<Fragment> fragments = new Vector<Fragment>();
        fragments.add(Fragment.instantiate(this,
                ContactListFragment.class.getName()));
        fragments.add(Fragment.instantiate(this,
                MessagesFragment.class.getName()));
        fragments.add(Fragment.instantiate(this,
                MyProfileFragment.class.getName()));
        this.pagerAdapter = new PagerAdapter(super.getSupportFragmentManager(),
                fragments);
        this.viewPager = (ViewPager) super.findViewById(R.id.viewpager);
        this.viewPager.setAdapter(this.pagerAdapter);
        this.viewPager.setOnPageChangeListener(this);

    }

    private void initialiseTabHost(Bundle args) {
        tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        TabInfo tabInfo = null;
        TabsFragmentActivity
                .addTab(this,
                        this.tabHost,
                        this.tabHost
                                .newTabSpec(TAB_CONTACTS)
                                .setIndicator(
                                        makeTabIndicator(getString(R.string.contacts_tab_header))),
                        (tabInfo = new TabInfo(TAB_CONTACTS,
                                ContactListFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        TabsFragmentActivity
                .addTab(this,
                        this.tabHost,
                        this.tabHost
                                .newTabSpec(TAB_MESSAGES)
                                .setIndicator(
                                        makeMessagesTabIndicator(getString(R.string.messages_tab_header))),
                        (tabInfo = new TabInfo(TAB_MESSAGES,
                                MessagesFragment.class, args)));
        this.mapTabInfo.put(tabInfo.tag, tabInfo);
        TabsFragmentActivity
                .addTab(this,
                        this.tabHost,
                        this.tabHost
                                .newTabSpec(TAB_PROFILE)
                                .setIndicator(
                                        makeTabIndicator(getString(R.string.myprofile_tab_header))),
                        (tabInfo = new TabInfo(TAB_PROFILE,
                                MyProfileFragment.class, args)));

        this.mapTabInfo.put(tabInfo.tag, tabInfo);

        tabHost.setOnTabChangedListener(this);
    }

    private static void addTab(TabsFragmentActivity activity, TabHost tabHost,
            TabHost.TabSpec tabSpec, TabInfo tabInfo) {
        tabSpec.setContent(activity.new TabFactory(activity));
        String tag = tabSpec.getTag();
        tabInfo.setFragment(activity.getSupportFragmentManager()
                .findFragmentByTag(tag));
        if (tabInfo.getFragment() != null
                && !tabInfo.getFragment().isDetached()) {
            FragmentTransaction ft = activity.getSupportFragmentManager()
                    .beginTransaction();
            ft.detach(tabInfo.getFragment());
            ft.commit();
            activity.getSupportFragmentManager().executePendingTransactions();
        }
        tabHost.addTab(tabSpec);
    }

    private View makeTabIndicator(String text) {
        View tabView = LayoutInflater.from(getApplicationContext()).inflate(
                R.layout.tab_item, null);
        TextView tabText = (TextView) tabView.findViewById(R.id.tabText);
        tabText.setText(text);
        return tabView;
    }
    
    private View makeMessagesTabIndicator(String text) {
        View tabView = LayoutInflater.from(getApplicationContext()).inflate(
                R.layout.messages_tab_item, null);
        TextView tabText = (TextView) tabView.findViewById(R.id.tabText);
        messagesCounter = (TextView) tabView.findViewById(R.id.counterText);
        tabText.setText(text);
        return tabView;
    }

    @Override
    public void onTabChanged(String tabId) {
        // first session tracking
        Integer order = MMLocalData.getInstance().getSessionOrder();
        if (tabId.equals(TAB_PROFILE)) {
            MMFirstSessionTracker.getInstance().abacus(null, "my_profile",
                    "screen", order, null);
        } else if (tabId.equals(TAB_MESSAGES)) {
            MMFirstSessionTracker.getInstance().abacus(null, "convos",
                    "screen", order, null);
        } else if (tabId.equals(TAB_CONTACTS)) {
            MMFirstSessionTracker.getInstance().abacus(null, "contacts",
                    "screen", order, null);
        }

        int pos = this.tabHost.getCurrentTab();
        this.viewPager.setCurrentItem(pos);
        // Updates the actionbar icons of the current tab
        changeIcons(tabId);
    }
    
    public void setMessageCount(int i) {
        messagesCounter.setVisibility(View.VISIBLE);
        if (i > 99) {
            messagesCounter
                    .setText(getString(R.string.unread_messages_top_value));
        } else if (i <= 0) {
            messagesCounter.setVisibility(View.GONE);
        } else {
            messagesCounter.setText(String.valueOf(i));
        }
    }

    private void changeIcons(String tabId) {

        if (tabId.equals(TAB_CONTACTS)) {

            showItem(notificationsContainer);

            setMenuItemVisible(searchContact, true);
            setMenuItemVisible(addContact, true);

            setMenuItemVisible(searchMessage, false);
            setMenuItemVisible(composeMessage, false);
            setMenuItemVisible(editProfile, false);

        } else if (tabId.equals(TAB_MESSAGES)) {

            hideItem(notificationsContainer);

            setMenuItemVisible(searchMessage, true);
            setMenuItemVisible(composeMessage, true);

            setMenuItemVisible(searchContact, false);
            setMenuItemVisible(addContact, false);
            setMenuItemVisible(editProfile, false);

        } else if (tabId.equals(TAB_PROFILE)) {

            showItem(notificationsContainer);

            setMenuItemVisible(editProfile, true);

            setMenuItemVisible(searchContact, false);
            setMenuItemVisible(addContact, false);
            setMenuItemVisible(searchMessage, false);
            setMenuItemVisible(composeMessage, false);

        }
    }

    private void hideItem(View view) {
        if (view == null) {
            LogIt.w(this, "Ignore hideItem called with null view");
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void showItem(View view) {
        if (view == null) {
            LogIt.w(this, "Ignore showItem called with null view");
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Click listener when the user press the add contact button
     * from the top actionbar
     *
     */
    private void addContactOption() {
        LogIt.user(this, "Add users button pressed");
        startActivity(new Intent(TabsFragmentActivity.this,
                FriendsInviteActivity.class));
    }

    /**
     * Click listener when the user press the search button under the contacts
     * tab on the top actionbar
     *
     */
    private void searchContactsOption() {
        LogIt.user(this, "Search contacts pressed");
        Intent intent = new Intent(TabsFragmentActivity.this,
                SearchContactsActivity.class);
        startActivity(intent);
    }

    /**
     * Click listener when the user press the compose message from the
     * top actionbar
     *
     */
    private void composeMessageOption() {
        LogIt.user(this, "Compose new message button pressed");
        Intent intent = new Intent(TabsFragmentActivity.this,
                MessageContactActivity.class);
        startActivity(intent);
    }

    /**
     * Click listener when the user press the search contacts
     * from the top actionbar
     *
     */
    private void searchMessageOption() {
        LogIt.user(this, "Search button pressed");
        Intent intent = new Intent(TabsFragmentActivity.this,
                SearchMessagesActivity.class);
        startActivity(intent);
    }

    /**
     * Click listener when user press the edit profile button
     * from the top action bar
     */
    private void editUserProfileOption() {
        LogIt.d(this, "Clicked on open Edit Profile dialog");
        MyProfileFragment fragment = (MyProfileFragment) pagerAdapter
                .getItem(TAB_PROFILE_POSITION);
        fragment.openEditProfileDialog(this);

    }

    /**
     * Method called from the layout XML, when
     * the user press the add users button at the bottom actionbar or
     * the fiend friends btn at MyProfileFragment
     */
    public void findFriends(View view) {
        LogIt.user(this, "Add users button pressed");
        startActivity(new Intent(this, FriendsInviteActivity.class));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        LogIt.user(this, "Pressed BACK key to exit the app");
        MessageMeApplication.sendOfflinePresence(TabsFragmentActivity.this,
                mMessagingServiceRef);
        MessageMeApplication.startDisconnectTimer(mMessagingServiceRef, this);
    }

    public void updateUserInformation() {
        LogIt.user(this, "User first/last name updated");
        MyProfileFragment fragment = (MyProfileFragment) pagerAdapter
                .getItem(TAB_PROFILE_POSITION);
        fragment.loadContactName();
    }

    @Override
    public void onPageSelected(int position) {
        this.tabHost.setCurrentTab(position);

    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

}