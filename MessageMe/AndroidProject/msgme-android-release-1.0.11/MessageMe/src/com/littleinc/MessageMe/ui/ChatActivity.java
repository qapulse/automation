package com.littleinc.MessageMe.ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.restlet.resource.ResourceException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.google.protobuf.InvalidProtocolBufferException;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarFragmentActivity;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.ContactMessage;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.DoodleMessage;
import com.littleinc.MessageMe.bo.DoodlePicMessage;
import com.littleinc.MessageMe.bo.GCMMessage;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.LocationMessage;
import com.littleinc.MessageMe.bo.MentorBanner;
import com.littleinc.MessageMe.bo.MentorBanner.MentorBannerType;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.PhotoMessage;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.SingleImageMessage;
import com.littleinc.MessageMe.bo.SongMessage;
import com.littleinc.MessageMe.bo.TextMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.bo.VideoMessage;
import com.littleinc.MessageMe.bo.VoiceMessage;
import com.littleinc.MessageMe.bo.YoutubeMessage;
import com.littleinc.MessageMe.chat.Chat;
import com.littleinc.MessageMe.chat.ChatManager;
import com.littleinc.MessageMe.chat.DurableCommand;
import com.littleinc.MessageMe.chat.MediaManager;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.data.MMLocalData;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.metrics.MMFirstWeekTracker;
import com.littleinc.MessageMe.metrics.MMHourlyTracker;
import com.littleinc.MessageMe.metrics.MMTracker;
import com.littleinc.MessageMe.net.FacebookPlace;
import com.littleinc.MessageMe.net.ItunesMedia;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope.CommandType;
import com.littleinc.MessageMe.protocol.Commands.PBCommandMessageRead;
import com.littleinc.MessageMe.protocol.Commands.PBCommandPresenceUpdate;
import com.littleinc.MessageMe.protocol.Commands.PBCommandPresenceUpdate.MessageActivity;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;
import com.littleinc.MessageMe.util.AudioUtil;
import com.littleinc.MessageMe.util.ChatAdapter;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.DateUtil;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageUtil;
import com.littleinc.MessageMe.util.MentorBannerListener;
import com.littleinc.MessageMe.util.MessageUtil;
import com.littleinc.MessageMe.util.StringUtil;
import com.littleinc.MessageMe.util.VideoUtil;
import com.littleinc.MessageMe.youtube.ParceableYouTubeItem;
import com.littleinc.MessageMe.youtube.YouTubeItem;

@TargetApi(14)
public class ChatActivity extends ActionBarFragmentActivity implements
        MentorBannerListener {

    private static final int LOCATION_REQUEST_CODE = 2;

    private static final int SONG_REQUEST_CODE = 3;

    public static final int VIDEO_REQUEST_CREATE_NEW_CODE = 4;

    private static final int YOUTUBE_REQUEST_CODE = 7;

    private static final int DOODLE_PIC_REQUEST_CODE = 8;

    private Chat mChat;

    private ChatManager mChatManager;

    public ChatAdapter customAdapter;

    /**
     * When restoring the view we need to remember which item was at the
     * top of the visible area, and what exact offset it was at.
     */
    private int mCurrentListItemIndex = -1;

    private int mCurrentListItemOffset = -1;

    private ImageButton packageChooserBtn;

    private ImageButton mEmojiKeyboardToggleBtn;

    private EmojiEditText chatInputEditTextView;

    private Button chatSendBtn;

    private MenuItem infoMenu;

    private ListView listView;

    private long startTime = 0L;

    private View timerWindow;

    private TextView timerLabel;

    private String timerStop;

    private RelativeLayout inputContainer;

    private InputState mCurrentInputState = InputState.NONE;

    private RelativeLayout voiceMsgComposerContainer;

    private boolean isRecording = false;

    private boolean canRecord = false;

    private RelativeLayout mainLayout;

    private RelativeLayout inputParent;

    private RelativeLayout messageCompositionIndicator;

    // This dummy footer is used as a workaround to avoid a ClassCastException
    // when trying to remove the footer from the list view ("ChatAdapter cannot
    // be cast to HeaderViewListAdapter")
    private RelativeLayout mDummyFooter;

    private GestureDetector mGestureDetector;

    private Contact contact;

    private File currentVoiceFile;

    private File mediaImageOutput = null;

    private ViewPager mViewPager;

    private MMFragmentAdapter mPagerAdapter = null;

    private LinearLayout optionsContainer;

    private LinearLayout mEmojiKeyboardContainer;

    private LinearLayout mEmojiPageIndicator;

    private int mEmojisPerKeyboard;

    View mLoadEarlierMsgsHeader;

    private String fileName;

    private GestureDetector listViewGestureDetector;

    /**
     * Keep track of whether we are visible or not.
     */
    private boolean mIsVisible = false;

    /**
     * Flag to remember if a message arrived while the screen is off.
     * Scrolling the list to the end only works while the screen is
     * visible so we have to wait until we are visible again to do it.
     */
    private boolean mMsgArrivedWhileScreenOff = false;

    private Conversation mConversation = null;

    private LinearLayout mentorBannerContainer;

    private Animation hideAnimation;

    /**
     * The minimum amount of vertical space required before we will
     * show mentor banners. This usually means we hide any mentor banner
     * when the keyboard or package chooser is displayed.
     */
    private static int sMinFreeSpaceRequiredForMentorBanners;

    private static Drawable sCurrentPageDrawable;

    private static Drawable sOtherPageDrawable;

    private static int sEmojiKeyboardPageIndicatorDotGap;

    private List<ImageView> mPageIndicators;

    private OnPageChangeListener mPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int selected) {
            if (mPageIndicators == null) {
                LogIt.w(ChatActivity.class,
                        "Ignore onPageSelected as mPageIndicators is null");
                return;
            }

            LogIt.d(ChatActivity.class, "Emoji keyboard changed", selected);

            for (int i = 0; i < mPageIndicators.size(); ++i) {
                ImageView imgView = mPageIndicators.get(i);

                if (i == selected) {
                    imgView.setImageDrawable(sCurrentPageDrawable);
                } else {
                    imgView.setImageDrawable(sOtherPageDrawable);
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }
    };

    static {
        Resources res = MessageMeApplication.getInstance().getResources();

        sMinFreeSpaceRequiredForMentorBanners = res
                .getDimensionPixelSize(R.dimen.mentor_banner_min_free_space_to_display);

        sCurrentPageDrawable = res
                .getDrawable(R.drawable.page_indicator_current);

        sOtherPageDrawable = res.getDrawable(R.drawable.page_indicator_other);

        sEmojiKeyboardPageIndicatorDotGap = res
                .getDimensionPixelSize(R.dimen.emoji_keyboard_page_indicator_dot_gap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogIt.d(this, "onCreate");

        setContentView(R.layout.chat_client);

        messagingServiceConnection = new ChatMessagingServiceConnection();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        if (savedInstanceState != null) {
            String mediaImageOutputPath = savedInstanceState
                    .getString(MessageMeConstants.MEDIA_OUTPUT_PATH_KEY);

            if (mediaImageOutputPath != null) {
                LogIt.d(this, "Load mediaImageOutput from savedInstanceState",
                        mediaImageOutputPath);
                mediaImageOutput = new File(
                        savedInstanceState
                                .getString(MessageMeConstants.MEDIA_OUTPUT_PATH_KEY));
            }
        }

        mentorBannerContainer = (LinearLayout) findViewById(R.id.mentor_banner_container);

        mDummyFooter = new RelativeLayout(this);
        hideAnimation = AnimationUtils.loadAnimation(this,
                R.anim.chat_input_container_slide_down);

        messageCompositionIndicator = newMessageCompositionIndicator(-1);

        listView = (ListView) findViewById(R.id.chat_list);

        listView.addFooterView(mDummyFooter);

        packageChooserBtn = (ImageButton) findViewById(R.id.package_chooser_button);

        mEmojiKeyboardToggleBtn = (ImageButton) findViewById(R.id.keyboard_toggle_button);

        chatInputEditTextView = (EmojiEditText) findViewById(R.id.chat_input);
        chatInputEditTextView
                .addTextChangedListener(new ChatInputTextWatcher());

        chatInputEditTextView.setOnTouchListener(new ChatInputTouchListener());
        chatInputEditTextView.setOnEditorActionListener(new DoneKeyListener());

        if (DeviceUtil.isPhysicalKeyboardAvailable(this)) {
            chatInputEditTextView.requestFocus();
        }

        chatSendBtn = (Button) findViewById(R.id.send_button);

        ImageView doodleBtn = (ImageView) findViewById(R.id.doodle_btn);
        ImageView locationBtn = (ImageView) findViewById(R.id.location_btn);
        ImageView pictureBtn = (ImageView) findViewById(R.id.picture_btn);
        ImageView songBtn = (ImageView) findViewById(R.id.music_btn);
        ImageView videoBtn = (ImageView) findViewById(R.id.video_btn);
        ImageView voiceBtn = (ImageView) findViewById(R.id.voice_btn);

        doodleBtn.setOnClickListener(new OpenDoodlePicListener());
        locationBtn.setOnClickListener(new OpenLocationListener());
        pictureBtn.setOnClickListener(new OpenPictureListener());
        songBtn.setOnClickListener(new OpenSongListener());
        videoBtn.setOnClickListener(new OpenVideoListener());
        voiceBtn.setOnClickListener(new OpenVoiceListener());

        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);

        // Ask for callbacks every time the layout changes so we can hide
        // the mentor banner when there is not enough space
        mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Get the visible area
                        Rect r = new Rect();
                        mainLayout.getWindowVisibleDisplayFrame(r);

                        int visibleHeight = (r.bottom - r.top);

                        if (mentorBannerContainer == null) {
                            LogIt.w(ChatActivity.class,
                                    "Can't hide mentor banner as it is null");
                        } else {
                            if (mCurrentInputState == InputState.PACKAGE_CHOOSER) {
                                // The package chooser is shown in the
                                // mainLayout so we need to subtract that
                                // from the visible area
                                visibleHeight -= optionsContainer.getHeight();
                            } else if (mCurrentInputState == InputState.EMOJI_KEYBOARD) {
                                visibleHeight -= mEmojiKeyboardContainer
                                        .getHeight();
                            }

                            // Only show the mentor banner container when
                            // there is enough space to also see messages.
                            if (visibleHeight > sMinFreeSpaceRequiredForMentorBanners) {
                                LogIt.d(ChatActivity.class,
                                        "Allow mentor banners to be shown",
                                        visibleHeight,
                                        sMinFreeSpaceRequiredForMentorBanners);
                                mentorBannerContainer
                                        .setVisibility(View.VISIBLE);
                            } else {
                                LogIt.d(ChatActivity.class,
                                        "Hide any mentor banner",
                                        visibleHeight,
                                        sMinFreeSpaceRequiredForMentorBanners);
                                mentorBannerContainer.setVisibility(View.GONE);
                            }
                        }
                    }
                });

        inputParent = (RelativeLayout) findViewById(R.id.input_container);

        timerWindow = LayoutInflater.from(this).inflate(
                R.layout.voice_message_timer_layout, mainLayout, false);

        mLoadEarlierMsgsHeader = getLayoutInflater().inflate(
                R.layout.message_list_header, listView, false);

        inputContainer = (RelativeLayout) findViewById(R.id.input_container);
        voiceMsgComposerContainer = (RelativeLayout) findViewById(R.id.voice_msg_compose_container);

        mGestureDetector = new GestureDetector(this, new LongPressDetector());

        optionsContainer = (LinearLayout) findViewById(R.id.options_container);

        mEmojiKeyboardContainer = (LinearLayout) findViewById(R.id.emoji_keyboard_container);

        listView.setOnTouchListener(new ChatListContianerTouchListener());

        listViewGestureDetector = new GestureDetector(this,
                new ListViewTapListener());

        if (getIntent().hasExtra(MessageMeConstants.RECIPIENT_USER_KEY)) {
            User user = User.parseFrom(getIntent().getByteArrayExtra(
                    MessageMeConstants.RECIPIENT_USER_KEY));
            contact = user;

        } else if (getIntent().hasExtra(MessageMeConstants.RECIPIENT_ROOM_KEY)) {
            contact = Room.parseFrom(getIntent().getByteArrayExtra(
                    MessageMeConstants.RECIPIENT_ROOM_KEY));
        } else {
            LogIt.e(this, "No Channel to load, closing chat thread");
            finish();
        }

        // track that we opened a room in the first session.
        if (contact != null) {
            String subtopic = (contact.getContactId() == MessageMeConstants.WELCOME_ROOM_ID) ? "room_w"
                    : "room";
            MMFirstSessionTracker.getInstance().abacusOnce(null, "open",
                    subtopic, null, null);

            Integer order = MMLocalData.getInstance().getSessionOrder();
            MMFirstSessionTracker.getInstance().abacus(null, subtopic,
                    "screen", order, null);
        }

        // The ViewPager needs to be defined in XML (creating it only in
        // code doesn't work)
        mViewPager = (ViewPager) super.findViewById(R.id.keyboard_view_pager);

        buildEmojiKeyboard(0);

        updateUI();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LogIt.user(this, "Changed to landscape");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            LogIt.user(this, "Changed to portrait");
        }

        int topLeftKey = mEmojisPerKeyboard * mViewPager.getCurrentItem();
        LogIt.d(this, "topLeftKey", topLeftKey);

        buildEmojiKeyboard(topLeftKey);
    }

    private void buildEmojiKeyboard(int topLeftKey) {

        // Allow for the padding at the top, and the height of the page
        // indicator UI in the container (which is twice the padding size)
        int keyboardHeight = mViewPager.getLayoutParams().height
                - mViewPager.getPaddingBottom() - mViewPager.getPaddingTop();

        int keysPerRow = EmojiKeyboardFragment.calculateNumberOfKeysPerRow();

        int rowsPerKeyboard = keyboardHeight
                / EmojiKeyboardFragment.getEmojiKeySize();

        LogIt.d(this, "Number of rows per keyboard", rowsPerKeyboard,
                keyboardHeight);

        // Subtract one to allow for the delete key
        mEmojisPerKeyboard = (keysPerRow * rowsPerKeyboard) - 1;

        int totalKeyCount = EmojiKeyboardFragment.getKeyCount();

        mPageIndicators = new ArrayList<ImageView>();

        mEmojiPageIndicator = (LinearLayout) findViewById(R.id.page_indicator);
        mEmojiPageIndicator.removeAllViews();

        List<Fragment> fragments = new LinkedList<Fragment>();

        for (int i = 0; i < totalKeyCount; i += mEmojisPerKeyboard) {
            int endIndex = i + mEmojisPerKeyboard - 1;

            if (endIndex >= totalKeyCount) {
                endIndex = totalKeyCount - 1;
            }

            Fragment keyboardPage = EmojiKeyboardFragment.newInstance(i,
                    endIndex, keysPerRow, chatInputEditTextView);
            fragments.add(keyboardPage);

            // Add a page indicator dot for each page of the keyboard
            ImageView imgView = new ImageView(this);

            if (i == 0) {
                imgView.setImageDrawable(sCurrentPageDrawable);
            } else {
                imgView.setImageDrawable(sOtherPageDrawable);
            }

            imgView.setPadding(sEmojiKeyboardPageIndicatorDotGap, 0,
                    sEmojiKeyboardPageIndicatorDotGap, 0);

            mEmojiPageIndicator.addView(imgView);

            mPageIndicators.add(imgView);
        }

        if (mPagerAdapter != null) {
            // Remove any existing fragments
            mPagerAdapter.removePages();
        }

        mPagerAdapter = new MMFragmentAdapter(
                super.getSupportFragmentManager(), fragments);

        mViewPager.setAdapter(mPagerAdapter);
        mPagerAdapter.notifyDataSetChanged();
        mViewPager.setOnPageChangeListener(mPageChangeListener);

        // Select the correct page (e.g. if we just did an orientation change)
        int currentPage = topLeftKey / mEmojisPerKeyboard;
        LogIt.d(ChatActivity.class, "Show emoji keyboard", currentPage);
        mViewPager.setCurrentItem(currentPage);
    }

    private void updateUI() {
        boolean msgThreadEnabled = true;
        String initialText = "";

        if ((contact != null) && (contact instanceof User)) {
            User user = (User) contact;

            if (user.isBlocked()) {
                LogIt.d(this, "Recipient is blocked, disable message thread");
                msgThreadEnabled = false;
                initialText = getString(R.string.blocked);
            } else if (user.isBlockedBy()) {
                LogIt.d(this, "Recipient blocked us, disable message thread");
                msgThreadEnabled = false;
                initialText = getString(R.string.blocked);
            }
        }

        packageChooserBtn.setEnabled(msgThreadEnabled);
        chatSendBtn.setEnabled(msgThreadEnabled);
        chatInputEditTextView.setEnabled(msgThreadEnabled);
        chatInputEditTextView.setText(initialText);
        mEmojiKeyboardToggleBtn.setEnabled(msgThreadEnabled);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (mediaImageOutput != null) {
            outState.putString(MessageMeConstants.MEDIA_OUTPUT_PATH_KEY,
                    mediaImageOutput.getAbsolutePath());
        }

        super.onSaveInstanceState(outState);
    }

    private static final long GCM_REMOVE_DELAY_MILLIS = 2000;

    private class GCMNotificationRemover implements Runnable {

        final long mContactID;

        public GCMNotificationRemover(final long contactID) {
            mContactID = contactID;
        }

        public void run() {
            // Clearing an ID of -1 clears all notifications, so make sure we
            // never accidentally do that
            if (mContactID != -1) {
                // Removes the stacked GCM notifications for this user
                GCMMessage.removeGCMMessagesFromSender(mContactID);

                // Remove all the external notifications associated to this user
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotificationManager.cancel((int) mContactID);
            } else {
                LogIt.w(ChatActivity.class, "Don't clear GCM for contact ID -1");
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        LogIt.d(this, "onResume");

        mIsVisible = true;

        // Prior to OS 4.1, some devices would call onResume for an activity
        // even while the lock screen is up.
        // https://code.google.com/p/android/issues/detail?id=18827
        //
        // This means if the ChatActivity is behind the lock screen when the
        // screen is turned on, then the GCM notification would be removed
        // immediately. Users often turn their screen on to see if they have a
        // notification, and it would get removed before they could see it.
        // Adding a delay
        // means the user has a much better chance of seeing the notification.
        if (contact != null) {
            handler.postDelayed(
                    new GCMNotificationRemover(contact.getContactId()),
                    GCM_REMOVE_DELAY_MILLIS);
        }

        // Hide the keyboard and package chooser as we could be reusing this
        // ChatActivity from a different message thread
        changeInputState(InputState.NONE);

        if (mMsgArrivedWhileScreenOff) {
            LogIt.i(this,
                    "Message received while screen was off, scroll to end");
            mMsgArrivedWhileScreenOff = false;
            scrollListToBottom();
        } else if ((mCurrentListItemIndex != -1)
                && (mCurrentListItemOffset != -1) && (listView != null)) {
            LogIt.d(this, "onResume, restore listview position",
                    mCurrentListItemIndex, mCurrentListItemOffset);

            positionListView(listView, mCurrentListItemIndex,
                    mCurrentListItemOffset);
        }
    }

    public static void positionListView(final ListView lv,
            final int itemToShow, final int itemOffset) {
        // It is necessary to post a message to update the position of
        // the ListView, doing it immediately now doesn't work. Source:
        // http://stackoverflow.com/a/8656750/112705
        lv.post(new Runnable() {
            @Override
            public void run() {
                lv.setSelectionFromTop(itemToShow, itemOffset);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceivers();
        MMHourlyTracker.getInstance().abacusOnce(null, "active", "user", null,
                null);
    }

    /**
     * Unregister the chat broadcast receivers
     */
    @Override
    protected void unregisterReceivers() {
        super.unregisterReceivers();

        if (mBroadcastManager != null) {
            LogIt.d(this, "Unregistering receivers");
            mBroadcastManager.unregisterReceiver(messageNewReceiver);
            mBroadcastManager.unregisterReceiver(messageReadReceiver);
            mBroadcastManager.unregisterReceiver(messageActivityReceiver);
            mBroadcastManager.unregisterReceiver(loadEarlierMessagesReceiver);
            mBroadcastManager.unregisterReceiver(userChangeReceiver);
        }
    }

    /**
     * Register the chat broadcast receivers
     */
    @Override
    protected void registerReceivers() {
        super.registerReceivers();

        if (mBroadcastManager != null) {
            LogIt.d(this, "Registering receivers");
            mBroadcastManager.registerReceiver(messageNewReceiver,
                    new IntentFilter(
                            MessageMeConstants.INTENT_ACTION_MESSAGE_NEW));

            mBroadcastManager.registerReceiver(messageReadReceiver,
                    new IntentFilter(
                            MessageMeConstants.INTENT_ACTION_MESSAGE_READ));

            mBroadcastManager.registerReceiver(messageActivityReceiver,
                    new IntentFilter(
                            MessageMeConstants.INTENT_ACTION_MESSAGE_ACTIVITY));

            mBroadcastManager
                    .registerReceiver(
                            loadEarlierMessagesReceiver,
                            new IntentFilter(
                                    MessageMeConstants.INTENT_ACTION_EARLIER_MESSAGES_AVAILABLE));

            mBroadcastManager.registerReceiver(userChangeReceiver,
                    new IntentFilter(
                            MessageMeConstants.INTENT_NOTIFY_USER_CHANGED));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();

        if (contact == null) {
            LogIt.w(this, "Contact is null, don't try to build menus");
        } else {
            if (contact.isUser()) {
                menuInflater.inflate(R.menu.private_chat_menu, menu);
            } else if (contact.isGroup()) {
                menuInflater.inflate(R.menu.group_chat_menu, menu);
            }
        }

        // Calling super after populating the menu is necessary here to ensure
        // that the action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if ((contact != null)
                && (contact.getContactId() == MessageMeConstants.WELCOME_ROOM_ID)) {
            LogIt.d(this, "Hide info button on welcome room");
            setMenuItemVisible(infoMenu, false);
        } else {
            setMenuItemVisible(infoMenu, true);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        infoMenu = (MenuItem) menu.findItem(R.id.contact_info_btn);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                LogIt.user(this, "Top action bar home button pressed");
                finish();
                break;
            case R.id.contact_info_btn:

                LogIt.user(this, "Top action bar info button pressed");
                Intent intent = null;

                if (contact.isUser()) {
                    intent = new Intent(ChatActivity.this,
                            ContactProfileActivity.class);
                } else {
                    intent = new Intent(ChatActivity.this,
                            GroupProfileActivity.class);
                }

                intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                        contact.getContactId());

                startActivityForResult(intent,
                        MessageMeConstants.GROUP_PROFILE_REQUEST_CODE);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void fillConversation() {

        // This is used to move the view to show a message that was found
        // using the search bar. We have to used "sorted by" instead of
        // the creation time as the user can search for a messages that
        // were downloaded from the server after a ROOM_JUMP (and their
        // creation time will be newer than more recent messages).
        final double searchedMessageSortedBy = getIntent().getDoubleExtra(
                MessageMeConstants.SEARCHED_MESSAGE_SORTED_BY, -1.0);

        new DatabaseTask(handler) {

            List<IMessage> messages;

            @Override
            public void work() {
                LogIt.i(ChatActivity.this, "Fill conversations");

                if (searchedMessageSortedBy == -1.0) {
                    // For performance, initialize the message list to only show
                    // the most recent messages
                    messages = MessageUtil.getChatMessages(mChat,
                            ChatAdapter.LOAD_MORE_MESSAGES_BATCH_SIZE);
                } else {
                    LogIt.d(this, "Show message found with search");

                    // We need to display the searched message, so load it and
                    // all messages that have arrived after it
                    messages = MessageUtil.getChatMessagesAfter(mChat,
                            String.valueOf(searchedMessageSortedBy));
                }
                mConversation = Conversation.newInstance(mChat.getChatId());
            }

            @Override
            public void done() {
                boolean showLoadEarlierMessagesBtn = true;

                if ((searchedMessageSortedBy == -1.0)
                        && (messages.size() < ChatAdapter.LOAD_MORE_MESSAGES_BATCH_SIZE)) {
                    // This is not the result of a search, and we already have
                    // all the messages in this thread to display
                    showLoadEarlierMessagesBtn = false;
                }

                customAdapter.initialize(messages, showLoadEarlierMessagesBtn,
                        mConversation);

                if (mConversation.getLastMessage() != null) {

                    if (mConversation.getLastMessage().wasSentByThisUser()) {

                        if (mConversation.getUnreadCount() > 0) {
                            markConversationAsRead();
                        }
                    } else {

                        if (mConversation.getUnreadCount() <= 0) {

                            LogIt.d(ChatActivity.class,
                                    "Most recent message is read, don't send MESSAGE_READ");
                        } else if (mConversation.getLastMessage().getType() == IMessageType.NOTICE) {

                            LogIt.d(ChatActivity.class,
                                    "Most recent message is a room notice, don't send MESSAGE_READ");
                            markConversationAsRead();
                        } else {
                            sendMessageRead(mConversation.getLastMessage()
                                    .getCommandId());
                        }
                    }
                }

                if (searchedMessageSortedBy == -1.0) {
                    // When showing a message thread default to showing the
                    // most recent messages, at the bottom
                    scrollListToBottom();
                } else {
                    // The searched message will be at the top of the list
                    scrollListToPosition(0);
                }
            }
        };
    }

    /**
     * Starts a DBTask to set and save the unread count to 0 and
     * notifies the {@link MessagesFragment} about the update
     */
    private void markConversationAsRead() {

        new DatabaseTask(handler) {

            @Override
            public void work() {

                LogIt.d(ChatActivity.class, "Mark the conversation as read");

                // Updates the unread count in the conversation
                mConversation.setUnreadCount(0);
                mConversation.save();
            }

            @Override
            public void done() {
                mMessagingServiceRef.notifyChatClient(
                        MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                        mChat.getChatId(), true);
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing()) {
            AudioUtil.stopPlaying();
        }

        mIsVisible = false;

        // Remember exactly where we were in the listview
        mCurrentListItemIndex = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        mCurrentListItemOffset = (v == null) ? 0 : v.getTop();

        LogIt.d(this, "onPause, remember listview position",
                mCurrentListItemIndex, mCurrentListItemOffset);

        UIUtil.hideKeyboard(chatInputEditTextView);
    }

    @Override
    public void onBackPressed() {
        LogIt.user(this, "Back pressed");

        if ((mCurrentInputState == InputState.PACKAGE_CHOOSER)
                || (mCurrentInputState == InputState.EMOJI_KEYBOARD)) {
            changeInputState(InputState.NONE);
        } else {
            super.onBackPressed();
        }
    }

    private enum InputState {
        NONE, PACKAGE_CHOOSER, NORMAL_KEYBOARD, EMOJI_KEYBOARD;
    }

    private void changeInputState(InputState newState) {

        if (newState == mCurrentInputState) {
            LogIt.d(this, "Input state hasn't changed", newState);
            return;
        } else {
            LogIt.d(this, "Changing input state", mCurrentInputState + " -> "
                    + newState);
        }

        boolean animationRequired = ((newState == InputState.NORMAL_KEYBOARD) || (mCurrentInputState == InputState.NORMAL_KEYBOARD));

        if (newState != InputState.PACKAGE_CHOOSER) {
            if (animationRequired) {
                // Hide the options menu using a translate animation.
                hideAnimation
                        .setAnimationListener(new HideOptionsMenuAnimListener());
                inputParent.startAnimation(hideAnimation);
            } else {
                optionsContainer.setVisibility(View.GONE);
            }
        }

        if (newState != InputState.EMOJI_KEYBOARD) {
            mEmojiKeyboardToggleBtn
                    .setImageResource(R.drawable.messagesview_mcb_input_icon_stickers_selector);

            if (animationRequired) {
                hideAnimation
                        .setAnimationListener(new HideEmojiKeyboardAnimListener());
                inputParent.startAnimation(hideAnimation);
            } else {
                mEmojiKeyboardContainer.setVisibility(View.GONE);
            }
        }

        if (newState != InputState.NORMAL_KEYBOARD) {
            UIUtil.hideKeyboard(chatInputEditTextView);
        }

        // Show the new state
        switch (newState) {
            case PACKAGE_CHOOSER:
                // tracking
                MMFirstWeekTracker.getInstance().abacusOnce(null, "show",
                        "packages", null, null);
                MMFirstSessionTracker.getInstance().abacusOnce(null, "show",
                        "packages", null, null);
                MMTracker.getInstance().abacus("packages", "show", null, null,
                        null);

                if (animationRequired) {
                    handler.postDelayed(showDelayedOptionsMenu, 250);
                } else {
                    optionsContainer.setVisibility(View.VISIBLE);

                    LogIt.d(this, "Package chooser height",
                            optionsContainer.getHeight());
                }

                break;
            case EMOJI_KEYBOARD:
                mEmojiKeyboardToggleBtn
                        .setImageResource(R.drawable.messagesview_mcb_input_icon_keyboard_selector);

                if (animationRequired) {
                    handler.postDelayed(showDelayedEmojiKeyboard, 250);
                } else {
                    mEmojiKeyboardContainer.setVisibility(View.VISIBLE);
                    chatInputEditTextView.requestFocus();
                }

                break;
            case NORMAL_KEYBOARD:
                mEmojiKeyboardToggleBtn
                        .setImageResource(R.drawable.messagesview_mcb_input_icon_stickers_selector);
                UIUtil.showKeyboard(chatInputEditTextView);
                break;
            default:
                break;
        }

        mCurrentInputState = newState;
    }

    /**
     * Toggle between the emoji keyboard and the normal soft keyboard.
     */
    public void toggleEmojiKeyboard(View view) {
        if (mCurrentInputState == InputState.EMOJI_KEYBOARD) {
            changeInputState(InputState.NORMAL_KEYBOARD);
        } else {
            changeInputState(InputState.EMOJI_KEYBOARD);
        }
    }

    public void toggleOptions(View view) {
        if (mCurrentInputState == InputState.PACKAGE_CHOOSER) {
            changeInputState(InputState.NONE);
        } else {
            changeInputState(InputState.PACKAGE_CHOOSER);
        }
    }

    public void closeVoiceMessageInput(View view) {
        LogIt.user(this, "Hide voice msg composer and show normal input");
        inputContainer.setVisibility(View.VISIBLE);
        voiceMsgComposerContainer.setVisibility(View.GONE);
        canRecord = false;
    }

    public void doSend(View view) {
        String text;

        if (chatInputEditTextView.containsEmojis()) {
            text = chatInputEditTextView.getEmojiText();
        } else {
            text = chatInputEditTextView.getText().toString();
        }

        if (TextUtils.isEmpty(text)) {
            LogIt.w(this, "Ignoring attempt to send empty text message");
        } else {
            LogIt.user(this, "Send pressed for text message");

            // As the send action is processed in the db thread we need to
            // disable the send button in case that the user press more than one
            // time
            chatSendBtn.setEnabled(false);

            chatInputEditTextView.setText("");

            final TextMessage textMessage = new TextMessage();
            textMessage.setText(text);

            final long channelID = contact.getContactId();

            new DatabaseTask(handler) {

                @Override
                public void work() {
                    // setCommonFieldsForSend need to be called inside of a DB
                    // task because the method setChannelId will load the
                    // channel info automatically
                    textMessage.setCommonFieldsForSend(channelID);
                    textMessage.save(false, mConversation);
                }

                @Override
                public void done() {
                    DurableCommand durableCmd = new DurableCommand(textMessage);

                    mMessagingServiceRef.addToDurableSendQueue(durableCmd);
                    mMessagingServiceRef.notifyChatClient(
                            MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                            textMessage.getId(), textMessage.getChannelId());

                    addMessageToAdapter(textMessage);
                    chatSendBtn.setEnabled(true);
                    scrollListToBottom();
                }
            };
        }
    }

    private void doSendPhoto() {
        sendMessageActivity(MessageActivity.PICTURE);
        openImageSelector();
    }

    private void doSendLocation() {

        try {
            sendMessageActivity(MessageActivity.LOCATION);
            startActivityForResult(new Intent(this, GetLocationActivity.class),
                    LOCATION_REQUEST_CODE);
        } catch (NoClassDefFoundError e) {
            LogIt.e(this, e.getMessage());
            showAlertFragment(R.string.no_google_maps_error_title,
                    R.string.no_google_maps_error, false);
        }
    }

    private void doSendSong() {
        sendMessageActivity(MessageActivity.MUSIC);
        startActivityForResult(new Intent(this, SearchMusicActivity.class),
                SONG_REQUEST_CODE);
    }

    private void doSendYouTube() {
        startActivityForResult(
                new Intent(this, SearchYouTubeVidActivity.class),
                YOUTUBE_REQUEST_CODE);
    }

    /**
     * Capture a new video and send it. For now video files are left in
     * whatever location the device chooses to put them in, as copying
     * them into our application internal storage disk cache means they
     * can't be played by external media players.
     */
    private void doSendVideo() {

        if (!DeviceUtil.isCameraAvailable(ChatActivity.this)) {
            LogIt.d(ChatActivity.this,
                    "Device doesn't have a camera, can't record");
            showAlertFragment(R.string.no_camera_title,
                    R.string.no_camera_message, false);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        // Do not specify the MediaStore.EXTRA_OUTPUT location to save
        // the file to as most devices will not let you write the file
        // directly to internal storage. This way we let the device pick
        // where to store it and we move it later.
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY,
                CamcorderProfile.QUALITY_HIGH);

        List<ResolveInfo> matchedApps = getPackageManager()
                .queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);

        if (matchedApps.size() == 0) {
            showAlertFragment(R.string.no_video_camera_app_title,
                    R.string.no_video_camera_app_message, false);
        } else {
            // Any music that is currently playing doesn't pause when starting
            // this activity, so we need to pause it manually.
            AudioUtil.pausePlaying(true);
            startActivityForResult(intent, VIDEO_REQUEST_CREATE_NEW_CODE);
        }
    }

    private void openDoodlePicComposer(String fileName) {
        Intent intent = new Intent(ChatActivity.this,
                DoodleComposerActivity.class);
        intent.putExtra(DoodleComposerActivity.EXTRA_DOODLE_BACKGROUND_FILE,
                fileName);
        startActivityForResult(intent, DOODLE_PIC_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            final Intent data) {
        User currentUser = MessageMeApplication.getCurrentUser();

        final long channelID = contact.getContactId();

        switch (requestCode) {
            case MessageMeConstants.PHOTO_REQUEST_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        fileName = null;
                        if (mediaImageOutput != null) {
                            if (DeviceUtil.isCameraAvailable(ChatActivity.this)) {
                                // Result of "Take New" photo with camera
                                fileName = mediaImageOutput.getAbsolutePath();
                                mediaImageOutput = null;

                                // Rotates the file if required
                                fileName = ImageUtil.rotatePicture(fileName,
                                        ChatActivity.this);
                            } else {
                                LogIt.d(ChatActivity.this,
                                        "Device doesn't have a camera, don't process anything");
                            }
                        } else if (data != null) {
                            if (data.hasExtra(PictureConfirmationActivity.EXTRA_OUTPUT_CONFIRMED_IMAGE_FILE)) {
                                // Result of confirmed google image search
                                fileName = data
                                        .getStringExtra(PictureConfirmationActivity.EXTRA_OUTPUT_CONFIRMED_IMAGE_FILE);
                                LogIt.d(ChatActivity.class,
                                        "onActivityResult for confirmed image search result",
                                        fileName);

                            } else if (data
                                    .hasExtra(DoodleComposerActivity.EXTRA_DOODLE_BACKGROUND_FILE)) {
                                // Result if user pressed on the Edit button of
                                // the picture confirmation activity
                                fileName = data
                                        .getStringExtra(DoodleComposerActivity.EXTRA_DOODLE_BACKGROUND_FILE);
                                LogIt.d(ChatActivity.class,
                                        "onActivityResult for doodle edit of image search result",
                                        fileName);

                                Intent intent = new Intent(ChatActivity.this,
                                        DoodleComposerActivity.class);
                                intent.putExtra(
                                        DoodleComposerActivity.EXTRA_DOODLE_BACKGROUND_FILE,
                                        fileName);
                                startActivityForResult(intent,
                                        DOODLE_PIC_REQUEST_CODE);

                                break;

                            } else {
                                // Result from choosing an image from the
                                // Gallery or an external source. E.g. Dropbox
                                // or Picasa.
                                //
                                // We still need to show the picture
                                // confirmation screen afterwards
                                final MMProgressDialogFragment progressDialog = showProgressDialogFragment(
                                        R.string.loading, true, true);

                                new BackgroundTask() {

                                    @Override
                                    public void work() {
                                        try {
                                            fileName = FileSystemUtil
                                                    .getMediaContentFromIntent(
                                                            ChatActivity.this,
                                                            data, true);
                                        } catch (MalformedURLException e) {
                                            LogIt.e(ChatActivity.class, e);
                                            fail(getString(R.string.sending_image_error_title),
                                                    getString(R.string.unexpected_error));
                                        } catch (IOException e) {
                                            LogIt.e(ChatActivity.class, e);
                                            fail(getString(R.string.sending_image_error_title),
                                                    getString(R.string.network_error));
                                        } catch (Exception e) {
                                            // We aren't using Restlet here, so
                                            // no need to handle
                                            // a ResourceException
                                            LogIt.e(ChatActivity.class, e,
                                                    e.getMessage());
                                            fail(getString(R.string.sending_image_error_title),
                                                    getString(R.string.unexpected_error));
                                        }
                                    }

                                    @Override
                                    public void done() {
                                        progressDialog.dismiss();

                                        if (!failed()) {

                                            if (fileName != null) {
                                                Intent intent = new Intent(
                                                        ChatActivity.this,
                                                        PictureConfirmationActivity.class);

                                                intent.putExtra(
                                                        PictureConfirmationActivity.EXTRA_INPUT_IMAGE_FILE,
                                                        fileName);

                                                startActivityForResult(
                                                        intent,
                                                        MessageMeConstants.CONFIRMATION_PAGE_REQUEST_CODE);
                                            } else {
                                                LogIt.e(ChatActivity.class,
                                                        "Failed to read file from source, File is null");
                                            }
                                        } else {
                                            showAlertFragment(
                                                    getExceptionTitle(),
                                                    getExceptionMessage(), true);
                                        }
                                    }
                                };

                                return;
                            }
                        } else {
                            LogIt.d(ChatActivity.this, "data intent was null");
                        }

                        if (!StringUtil.isEmpty(fileName)) {
                            LogIt.i(this, "Send PHOTO_REQUEST_CODE", fileName);
                            sendImageMessage(fileName, IMessageType.PHOTO,
                                    channelID);
                        }
                        break;
                    case RESULT_CANCELED:
                        LogIt.user(this, "Canceled the PHOTO_REQUEST_CODE");
                        fileName = null;
                        mediaImageOutput = null;
                        sendMessageActivity(MessageActivity.IDLE);
                        break;
                    default:
                        LogIt.w(this,
                                "Unexpected result code for PHOTO_REQUEST_CODE",
                                resultCode);
                        fileName = null;
                        mediaImageOutput = null;
                        sendMessageActivity(MessageActivity.IDLE);
                        break;
                }
                break;
            case LOCATION_REQUEST_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        FacebookPlace location = (FacebookPlace) data
                                .getSerializableExtra(MessageMeConstants.EXTRA_LOCATION);

                        if (location == null) {
                            LogIt.w(this,
                                    "Ignore attempt to send null Facebook location");
                            sendMessageActivity(MessageActivity.IDLE);
                            break;
                        }

                        LogIt.i(this, "Send LOCATION_REQUEST_CODE",
                                location.toString());

                        final LocationMessage locationMessage = new LocationMessage();

                        locationMessage.setLatitude((float) location
                                .getLocation().latitude);
                        locationMessage.setLongitude((float) location
                                .getLocation().longitude);
                        locationMessage.setLocationId(location.getId());
                        locationMessage.setName(location.getName());
                        locationMessage.setAddress(location.getLocation()
                                .getAddress());

                        new DatabaseTask(handler) {

                            @Override
                            public void work() {
                                // setCommonFieldsForSend need to be called
                                // inside of a DB task because
                                // the method setChannelId will load the channel
                                // info automatically
                                locationMessage
                                        .setCommonFieldsForSend(channelID);
                                locationMessage.save(false, mConversation);
                            }

                            @Override
                            public void done() {
                                locationMessage.send(mMessagingServiceRef);
                                addMessageToAdapter(locationMessage);
                                scrollListToBottom();
                            }
                        };
                        break;
                    default:
                        sendMessageActivity(MessageActivity.IDLE);
                        break;
                }
                break;
            case SONG_REQUEST_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        ItunesMedia media = (ItunesMedia) data
                                .getSerializableExtra(MessageMeConstants.EXTRA_MEDIA);

                        LogIt.i(this, "Send SONG_REQUEST_CODE",
                                media.getArtistName(), media.getTrackName());

                        final SongMessage songMessage = new SongMessage();

                        songMessage.setArtistName(media.getArtistName());
                        songMessage.setArtworkUrl(media.getArtworkUrl100());
                        songMessage.setTrackName(media.getTrackName());
                        songMessage.setTrackUrl(media.getTrackViewUrl());
                        songMessage.setPreviewUrl(media.getPreviewUrl());

                        new DatabaseTask(handler) {

                            @Override
                            public void work() {
                                // setCommonFieldsForSend need to be called
                                // inside of a DB task because
                                // the method setChannelId will load the channel
                                // info automatically
                                songMessage.setCommonFieldsForSend(channelID);
                                songMessage.save(false, mConversation);
                            }

                            @Override
                            public void done() {
                                songMessage.send(mMessagingServiceRef);
                                addMessageToAdapter(songMessage);
                                scrollListToBottom();
                            }
                        };
                        break;
                    default:
                        sendMessageActivity(MessageActivity.IDLE);
                        break;
                }
                break;
            case VIDEO_REQUEST_CREATE_NEW_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        LogIt.i(this, "VIDEO_REQUEST_CREATE_NEW_CODE");
                        sendVideo(VIDEO_REQUEST_CREATE_NEW_CODE, data,
                                currentUser);
                        break;
                    default:
                        sendMessageActivity(MessageActivity.IDLE);
                        break;
                }
                break;
            case MessageMeConstants.VIDEO_REQUEST_SELECT_EXISTING_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        LogIt.i(this, "VIDEO_REQUEST_SELECT_EXISTING_CODE");

                        final MMProgressDialogFragment progressDialog = showProgressDialogFragment(
                                R.string.loading, true, true);

                        new BackgroundTask() {

                            @Override
                            public void work() {
                                try {
                                    fileName = FileSystemUtil
                                            .getMediaContentFromIntent(
                                                    ChatActivity.this, data,
                                                    true);
                                } catch (MalformedURLException e) {
                                    LogIt.e(ChatActivity.class, e);
                                    fail(getString(R.string.sending_video_error_title),
                                            getString(R.string.unexpected_error));
                                } catch (IOException e) {
                                    LogIt.e(ChatActivity.class, e);
                                    fail(getString(R.string.sending_video_error_title),
                                            getString(R.string.network_error));
                                } catch (Exception e) {
                                    // We aren't using Restlet here, so no need
                                    // to handle
                                    // a ResourceException
                                    LogIt.e(ChatActivity.class, e,
                                            e.getMessage());
                                    fail(getString(R.string.sending_image_error_title),
                                            getString(R.string.network_error));
                                }
                            }

                            @Override
                            public void done() {
                                progressDialog.dismiss();

                                if (!failed()) {
                                    Intent intent = new Intent(
                                            ChatActivity.this,
                                            PictureConfirmationActivity.class);

                                    intent.putExtra(
                                            PictureConfirmationActivity.EXTRA_INPUT_VIDEO_FILE,
                                            fileName);
                                    intent.putExtra(
                                            PictureConfirmationActivity.EXTRA_INPUT_IS_VIDEO,
                                            true);

                                    startActivityForResult(
                                            intent,
                                            MessageMeConstants.CONFIRMATION_PAGE_REQUEST_CODE);
                                } else {
                                    showAlertFragment(getExceptionTitle(),
                                            getExceptionMessage(), true);
                                }
                            }
                        };

                        break;
                    default:
                        sendMessageActivity(MessageActivity.IDLE);
                        break;
                }
                break;
            case YOUTUBE_REQUEST_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        ParceableYouTubeItem parceableYouTubeItem = data
                                .getParcelableExtra(MessageMeConstants.EXTRA_YOUTUBE);
                        YouTubeItem youtubeItem = parceableYouTubeItem
                                .getYoutubeItem();

                        LogIt.i(this, "Send YOUTUBE_REQUEST_CODE",
                                youtubeItem.getTitle(), youtubeItem.getId());

                        final YoutubeMessage youtubeMessage = new YoutubeMessage();

                        youtubeMessage.setDuration(youtubeItem.getDuration());
                        youtubeMessage.setThumbKey(youtubeItem.getThumbnail()
                                .getMqDefault());
                        youtubeMessage.setVideoTitle(youtubeItem.getTitle());
                        youtubeMessage.setVideoID(youtubeItem.getId());

                        new DatabaseTask(handler) {

                            @Override
                            public void work() {
                                // setCommonFieldsForSend need to be called
                                // inside of a DB task because
                                // the method setChannelId will load the channel
                                // info automatically
                                youtubeMessage
                                        .setCommonFieldsForSend(channelID);
                                youtubeMessage.save(false, mConversation);
                            }

                            @Override
                            public void done() {
                                youtubeMessage.send(mMessagingServiceRef);
                                addMessageToAdapter(youtubeMessage);
                                scrollListToBottom();
                            }
                        };
                        break;
                    default:
                        sendMessageActivity(MessageActivity.IDLE);
                        break;
                }
                break;
            case DOODLE_PIC_REQUEST_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        String doodlePicFileName = data
                                .getStringExtra(MessageMeConstants.FILE_NAME_KEY);

                        LogIt.i(this, "Send DOODLE_PIC_REQUEST_CODE",
                                doodlePicFileName);
                        sendImageMessage(doodlePicFileName,
                                IMessageType.DOODLE_PIC, channelID);
                        break;
                    default:
                        sendMessageActivity(MessageActivity.IDLE);
                        break;
                }
                break;
            case MessageMeConstants.DETAIL_SCREEN_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_CANCELED:
                        // This happens when viewing the detail screen and then
                        // leaving it
                        break;
                    case MessageMeConstants.DETAIL_REPLY_DOODLE_RESULT_CODE:
                        String filePath = data
                                .getStringExtra(DoodleComposerActivity.EXTRA_DOODLE_BACKGROUND_FILE);
                        LogIt.d(ChatActivity.class,
                                "onActivityResult for Reply with Doodle",
                                filePath);

                        Intent intent = new Intent(this,
                                DoodleComposerActivity.class);
                        intent.putExtra(
                                DoodleComposerActivity.EXTRA_DOODLE_BACKGROUND_FILE,
                                filePath);
                        sendMessageActivity(MessageActivity.DOODLE);
                        startActivityForResult(intent, DOODLE_PIC_REQUEST_CODE);
                        break;
                    default:
                        LogIt.w(this,
                                "Unexpected result code from DETAIL_SCREEN_REQUEST_CODE",
                                resultCode);
                        break;
                }
                break;
            case MessageMeConstants.GROUP_PROFILE_REQUEST_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        if (data.getBooleanExtra(
                                MessageMeConstants.EXTRA_GROUP_LEAVE, false)) {
                            LogIt.d(ChatActivity.class,
                                    "Exit chat activity as user has left the group");
                            finish();
                        }
                }
                break;
            case MessageMeConstants.CONFIRMATION_PAGE_REQUEST_CODE:
                switch (resultCode) {
                    case RESULT_OK:
                        // A "Choose Existing" photo message or video message
                        // has been confirmed by the user on the picture
                        // confirmation screen
                        if (data.hasExtra(PictureConfirmationActivity.EXTRA_OUTPUT_CONFIRMED_IMAGE_FILE)) {
                            fileName = data
                                    .getExtras()
                                    .getString(
                                            PictureConfirmationActivity.EXTRA_OUTPUT_CONFIRMED_IMAGE_FILE);
                            LogIt.d(this, "EXTRA_CONFIRMED_IMAGE_FILE",
                                    fileName);
                            sendImageMessage(fileName, IMessageType.PHOTO,
                                    channelID);
                        } else if (data
                                .hasExtra(PictureConfirmationActivity.EXTRA_OUTPUT_EDIT_IMAGE_FILE)) {
                            fileName = data
                                    .getStringExtra(PictureConfirmationActivity.EXTRA_OUTPUT_EDIT_IMAGE_FILE);
                            LogIt.d(this, "EXTRA_EDIT_IMAGE_FILE", fileName);
                            openDoodlePicComposer(fileName);
                        } else if (data
                                .hasExtra(PictureConfirmationActivity.EXTRA_OUTPUT_IS_VIDEO)) {
                            LogIt.i(this, "VIDEO_REQUEST_SELECT_EXISTING_CODE");
                            sendVideo(
                                    MessageMeConstants.VIDEO_REQUEST_SELECT_EXISTING_CODE,
                                    data, currentUser);
                        } else {
                            LogIt.w(this,
                                    "Unexpected result code from CONFIRMATION_PAGE_REQUEST_CODE",
                                    resultCode);
                        }
                        break;
                }
                break;
            case RESULT_CANCELED:
                LogIt.user(this, "Canceled the picture confirmation");
                fileName = null;
                mediaImageOutput = null;
                sendMessageActivity(MessageActivity.IDLE);
                break;
            default:
                LogIt.w(this, "Unhandled result code", resultCode);
                fileName = null;
                mediaImageOutput = null;
                sendMessageActivity(MessageActivity.IDLE);
                break;
        }

        changeInputState(InputState.NONE);
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Adds the given message into the current chat adapter if it is available
     */
    private void addMessageToAdapter(IMessage message) {
        if (customAdapter != null) {
            customAdapter.addSendMessage(message);
        } else {
            LogIt.w(ChatActivity.class, "adapter null, unable to add message",
                    message.getType());
        }
    }

    /**
     * Util method to send {@link SingleImageMessage}
     */
    private void sendImageMessage(final String fileName,
            IMessageType messageType, final long channelID) {

        final SingleImageMessage message = (SingleImageMessage) MessageUtil
                .newMessageInstanceByType(messageType);

        new BackgroundTask() {

            File originalFile;

            String thumbFileKey;

            File newInAppThumbFile;

            @Override
            public void work() {

                originalFile = new File(fileName);

                try {
                    if (ImageUtil.isInAppMediaCache(originalFile)) {
                        // If file is already in our local media cache use the
                        // same key to generate a thumbKey
                        thumbFileKey = MediaManager
                                .generateObjectName(
                                        MediaManager.MESSAGE_FOLDER,
                                        SingleImageMessage
                                                .generateThumbkeyFromImageKey(originalFile
                                                        .getName()));
                    } else {
                        // If is an external file generate a new key
                        thumbFileKey = SingleImageMessage
                                .generateThumbkeyFromImageKey(MediaManager
                                        .generateObjectName(
                                                MediaManager.MESSAGE_FOLDER,
                                                StringUtil.getRandomFilename(),
                                                message.getType()));
                    }

                    // Create a new file into the app pictures folder
                    newInAppThumbFile = ImageUtil.getFile(thumbFileKey);
                    newInAppThumbFile.createNewFile();

                    // Copy a file is faster than make a resize so we first copy
                    // the original file
                    // in the thumb file to be able of add the message in the
                    // chat thread
                    if (ImageUtil.copyFile(originalFile, newInAppThumbFile)) {                        
                        message.setThumbKey(thumbFileKey);
                    } else {
                        LogIt.e(ChatActivity.class, "Failed to copy original file");
                        fail(getString(R.string.sending_image_error_title),
                                getString(R.string.unexpected_error));
                    }
                } catch (IOException e) {
                    LogIt.e(ChatActivity.class, e);
                    fail(getString(R.string.sending_image_error_title),
                            getString(R.string.sending_image_error_disk_space));
                } catch (ResourceException e) {
                    LogIt.e(ChatActivity.class, e);
                    fail(getString(R.string.sending_image_error_title),
                            getString(R.string.network_error));
                } catch (Exception e) {
                    LogIt.e(ChatActivity.class, e, e.getMessage());
                    fail(getString(R.string.sending_image_error_title),
                            getString(R.string.unexpected_error));
                }
            }

            @Override
            public void done() {
                if (!failed()) {
                    new DatabaseTask(handler) {

                        @Override
                        public void work() {
                            // setCommonFieldsForSend need to be called inside
                            // of a DB task because
                            // the method setChannelId will load the channel
                            // info automatically
                            message.setCommonFieldsForSend(channelID);
                            message.save(false, mConversation);
                        }

                        @Override
                        public void done() {

                            addMessageToAdapter(message);
                            scrollListToBottom();

                            new BackgroundTask() {

                                int maxPhotoSize = getResources()
                                        .getDimensionPixelSize(
                                                R.dimen.max_photo_size);

                                int maxThumbSize = getResources()
                                        .getDimensionPixelSize(
                                                R.dimen.max_thumb_photo_size);

                                @Override
                                public void work() {
                                    try {

                                        String detailFileKey = MediaManager
                                                .generateObjectName(
                                                        MediaManager.MESSAGE_FOLDER,
                                                        SingleImageMessage
                                                                .generateDetailkeyFromThumbKey(newInAppThumbFile
                                                                        .getName()));

                                        File newInAppDetailFile = ImageUtil
                                                .getFile(detailFileKey);
                                        newInAppDetailFile.createNewFile();

                                        // At this point the thumb file is a
                                        // full-size image
                                        // we going to use that to create the
                                        // detailed 960px
                                        // version of the image
                                        if (!ImageUtil.resizeImageToFile(
                                                newInAppThumbFile,
                                                newInAppDetailFile,
                                                maxPhotoSize, true)) {
                                            LogIt.w(ChatActivity.class,
                                                    "Failed to resize image "
                                                            + newInAppThumbFile);
                                            fail(getString(R.string.sending_image_error_title),
                                                    getString(R.string.sending_image_error_description));
                                        } else {
                                            // At this point the detail image is
                                            // ready so we just
                                            // have to resize the existing
                                            // full-size image to 300px
                                            if (!ImageUtil.resizeImageToFile(
                                                    newInAppDetailFile,
                                                    newInAppThumbFile,
                                                    maxThumbSize, true)) {
                                                LogIt.w(ChatActivity.class,
                                                        "Failed to resize image "
                                                                + newInAppThumbFile);
                                                fail(getString(R.string.sending_image_error_title),
                                                        getString(R.string.sending_image_error_description));
                                            } else {
                                                // If the original file is in
                                                // our media cache
                                                // this file is not needed
                                                // anymore
                                                if (ImageUtil
                                                        .isInAppMediaCache(originalFile)) {
                                                    LogIt.d(ChatActivity.class,
                                                            "Deleting original file",
                                                            originalFile);
                                                    originalFile.delete();
                                                }

                                                message.setImageKey(detailFileKey);
                                            }
                                        }
                                    } catch (IOException e) {
                                        LogIt.e(ChatActivity.class, e);
                                        fail(getString(R.string.sending_image_error_title),
                                                getString(R.string.sending_image_error_disk_space));
                                    } catch (ResourceException e) {
                                        LogIt.e(ChatActivity.class, e);
                                        fail(getString(R.string.sending_image_error_title),
                                                getString(R.string.network_error));
                                    } catch (Exception e) {
                                        LogIt.e(ChatActivity.class, e,
                                                e.getMessage());
                                        fail(getString(R.string.sending_image_error_title),
                                                getString(R.string.unexpected_error));
                                    }
                                }

                                @Override
                                public void done() {
                                    if (!failed()) {
                                        message.send(mMessagingServiceRef);
                                    } else {
                                        processMessageSendFail(message,
                                                getExceptionTitle(),
                                                getExceptionMessage());
                                    }
                                }
                            };
                        }
                    };
                } else {
                    processMessageSendFail(message, getExceptionTitle(),
                            getExceptionMessage());
                }
            }
        };
    }

    /**
     * Updates the message status into the db and shows an alert with the error
     */
    private void processMessageSendFail(final IMessage message,
            final String title, final String description) {

        new DatabaseTask(handler) {

            @Override
            public void work() {
                message.setCommandId(0);
                message.save(false, mConversation);
            }

            @Override
            public void done() {
                if (customAdapter != null) {
                    customAdapter.notifyDataSetChanged();
                } else {
                    LogIt.w(ChatActivity.class,
                            "adapter null, unable to update message status");
                }
                showAlertFragment(title, description, true);
            }
        };
    }

    /**
     * Add a video message to the send queue.
     * 
     * @param operation the operation being carried out, either
     * VIDEO_REQUEST_SELECT_EXISTING_CODE or
     * VIDEO_REQUEST_CREATE_NEW_CODE.
     * @param data the Intent from the "Take Video" or
     * "Select Video from Gallery" operations.
     * @param currentUser the current User.
     */
    private void sendVideo(final int operation, final Intent data,
            final User currentUser) {

        if ((data == null)) {
            LogIt.w(this, "No data retrieved from Intent, ignoring", operation,
                    data);
            return;
        }

        final MMProgressDialogFragment progressDialog = showProgressDialogFragment(
                R.string.loading, true, true);

        final long channelID = contact.getContactId();

        new BackgroundTask() {

            @Override
            public void work() {
                try {
                    if (data.hasExtra(PictureConfirmationActivity.EXTRA_INPUT_VIDEO_FILE)) {
                        fileName = data
                                .getStringExtra(PictureConfirmationActivity.EXTRA_INPUT_VIDEO_FILE);
                    } else {
                        fileName = FileSystemUtil.getMediaContentFromIntent(
                                ChatActivity.this, data, true);
                    }
                } catch (MalformedURLException e) {
                    LogIt.e(ChatActivity.class, e);
                    fail(getString(R.string.sending_video_error_title),
                            getString(R.string.unexpected_error));
                } catch (IOException e) {
                    LogIt.e(ChatActivity.class, e);
                    fail(getString(R.string.sending_video_error_title),
                            getString(R.string.network_error));
                } catch (ResourceException e) {
                    LogIt.e(ChatActivity.class, e);
                    fail(getString(R.string.sending_video_error_title),
                            getString(R.string.network_error));
                } catch (Exception e) {
                    LogIt.e(ChatActivity.class, e, e.getMessage());
                    fail(getString(R.string.sending_video_error_title),
                            getString(R.string.unexpected_error));
                }
            }

            @Override
            public void done() {
                progressDialog.dismiss();

                if (!failed()) {
                    File videoFile = new File(fileName);
                    int videoDuration = VideoUtil.getVideoDuration(videoFile);

                    if (videoDuration == -1) {
                        // This only happens when something is badly wrong, e.g.
                        // we don't
                        // have access to the video file - so don't try to send
                        // a broken
                        // video.
                        LogIt.i(this,
                                "Cannot get video duration, so don't send this video",
                                videoFile.getAbsolutePath());
                        return;
                    }

                    // Create video thumbnail without any message bubble
                    // masking. This creates a unique file name suitable for
                    // uploading to S3.
                    //
                    // We have to do it here as the ThumbnailUtils package won't
                    // have permission to access the video file once we move it
                    // to our media cache in internal storage.
                    final File videoThumb = VideoUtil
                            .createVideoThumbnail(videoFile);

                    if ((videoThumb != null) && videoThumb.exists()) {

                        LogIt.i(this, "Send VIDEO_REQUEST_CODE",
                                videoFile.getAbsolutePath());

                        final VideoMessage videoMessage = new VideoMessage();

                        // Initially set these to be their on disk locations,
                        // this will
                        // be changed to their S3 keys later
                        videoMessage.setVideoKey(videoFile.toString(), true);
                        videoMessage.setThumbKey(videoThumb.toString());
                        videoMessage.setDuration(videoDuration);

                        videoMessage.setCommonFieldsForSend(channelID);

                        boolean preserveExistingVideoFile = true;

                        // Only newly recorded videos should be moved into the
                        // application
                        // data directory. Existing ones need to be left where
                        // they are.
                        //
                        // XXX This code is currently ignored
                        if (operation == ChatActivity.VIDEO_REQUEST_CREATE_NEW_CODE) {
                            preserveExistingVideoFile = false;
                        }

                        videoMessage.setCommandId(-1);

                        final String fileToUpload = videoFile.getAbsolutePath();
                        final boolean preserveVideoFile = preserveExistingVideoFile;

                        new DatabaseTask(ChatActivity.this.handler) {

                            @Override
                            public void work() {
                                videoMessage.save(false, mConversation);
                            }

                            @Override
                            public void done() {
                                videoMessage.send(ChatActivity.this,
                                        mMessagingServiceRef, fileToUpload,
                                        preserveVideoFile);

                                addMessageToAdapter(videoMessage);
                                scrollListToBottom();
                            }
                        };
                    } else {
                        LogIt.w(this, "Failed to create video thumbnail",
                                videoFile.getAbsolutePath());
                    }
                } else {
                    showAlertFragment(getExceptionTitle(),
                            getExceptionMessage(), true);
                }
            }
        };
    }

    private class ChatMessagingServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogIt.d(ChatActivity.class, "Connected to Messaging Service");

            mMessagingServiceRef = ((MessagingService.MessagingBinder) service)
                    .getService();

            mChatManager = mMessagingServiceRef.getChatManager();

            mBroadcastManager = LocalBroadcastManager
                    .getInstance(mMessagingServiceRef);

            unregisterReceivers();
            registerReceivers();

            if (contact != null) {
                // Disable notifications from this user
                mNotificationHelper.setUserId(contact.getContactId());

                setTitle(EmojiUtils.convertToEmojisIfRequired(
                        contact.getDisplayName(), EmojiSize.NORMAL));

                mChat = mChatManager.getChat(contact.getContactId(),
                        contact.getContactType());

                if (mChat == null) {
                    if (contact.isUser()) {
                        mChat = mChatManager.createChat((User) contact);
                    } else {
                        mChat = mChatManager.createRoom((Room) contact);
                    }
                }

                long forwardedMessageId = getIntent().getLongExtra(
                        Message.ID_COLUMN, -1);

                String forwardedImage = null;

                if (getIntent().hasExtra(MessageMeConstants.EXTRA_IMAGE_KEY)) {
                    forwardedImage = getIntent().getStringExtra(
                            MessageMeConstants.EXTRA_IMAGE_KEY);
                }

                final long channelID = contact.getContactId();

                if ((customAdapter == null)
                        || (customAdapter.getChat().getChatId() != channelID)) {
                    LogIt.d(this, "Create ChatAdapter");
                    customAdapter = new ChatAdapter(ChatActivity.this, mChat,
                            listView, mLoadEarlierMsgsHeader);

                    listView.setAdapter(customAdapter);

                    fillConversation();
                } else {
                    LogIt.d(this, "Reuse ChatAdapter");
                }

                if (forwardedMessageId != -1) {
                    IMessageType forwardedMessageType = IMessageType
                            .valueOf(getIntent().getIntExtra(
                                    Message.TYPE_COLUMN, -1));

                    switch (forwardedMessageType) {
                        case PHOTO:
                            LogIt.d(this, "Forwarded photo");

                            final PhotoMessage photoMessage = new PhotoMessage();
                            photoMessage.setId(forwardedMessageId);

                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    photoMessage.load();

                                    photoMessage.setId(0);
                                    photoMessage
                                            .setCommonFieldsForSend(channelID);

                                    photoMessage.save(false);
                                }

                                @Override
                                public void done() {
                                    photoMessage.forward(customAdapter,
                                            mMessagingServiceRef);
                                    scrollListToBottom();
                                }
                            };
                            break;
                        case LOCATION:
                            LogIt.d(this, "Forwarded location");

                            final LocationMessage locationMessage = new LocationMessage();
                            locationMessage.setId(forwardedMessageId);

                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    locationMessage.load();

                                    locationMessage.setId(0);
                                    locationMessage
                                            .setCommonFieldsForSend(channelID);

                                    locationMessage.save(false);
                                }

                                @Override
                                public void done() {
                                    locationMessage.forward(customAdapter,
                                            mMessagingServiceRef);

                                    scrollListToBottom();
                                }
                            };
                            break;
                        case DOODLE:
                            LogIt.d(this, "Forwarded doodle");

                            final DoodleMessage doodleMessage = new DoodleMessage();
                            doodleMessage.setId(forwardedMessageId);

                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    doodleMessage.load();

                                    doodleMessage.setId(0);
                                    doodleMessage
                                            .setCommonFieldsForSend(channelID);

                                    doodleMessage.save(false);
                                }

                                @Override
                                public void done() {
                                    doodleMessage.forward(customAdapter,
                                            mMessagingServiceRef);

                                    scrollListToBottom();
                                }
                            };
                            break;
                        case DOODLE_PIC:
                            LogIt.d(this, "Forwarded doodle pic");

                            final DoodlePicMessage doodlePicMessage = new DoodlePicMessage();
                            doodlePicMessage.setId(forwardedMessageId);

                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    doodlePicMessage.load();

                                    doodlePicMessage.setId(0);
                                    doodlePicMessage
                                            .setCommonFieldsForSend(channelID);

                                    doodlePicMessage.save(false);
                                }

                                @Override
                                public void done() {
                                    doodlePicMessage.forward(customAdapter,
                                            mMessagingServiceRef);

                                    scrollListToBottom();
                                }
                            };
                            break;
                        case VIDEO:
                            LogIt.d(this, "Forwarded video");

                            final VideoMessage videoMessage = new VideoMessage();
                            videoMessage.setId(forwardedMessageId);

                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    videoMessage.load();

                                    videoMessage.setId(0);
                                    videoMessage
                                            .setCommonFieldsForSend(channelID);

                                    videoMessage.save(false);
                                }

                                @Override
                                public void done() {
                                    videoMessage.forward(customAdapter,
                                            mMessagingServiceRef);

                                    scrollListToBottom();
                                }
                            };
                            break;
                        case YOUTUBE:
                            LogIt.d(this, "Forwarded youtube video");

                            final YoutubeMessage youtubeMessage = new YoutubeMessage();
                            youtubeMessage.setId(forwardedMessageId);

                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    youtubeMessage.load();

                                    youtubeMessage.setId(0);
                                    youtubeMessage
                                            .setCommonFieldsForSend(channelID);

                                    youtubeMessage.save(false);
                                }

                                @Override
                                public void done() {
                                    youtubeMessage.forward(customAdapter,
                                            mMessagingServiceRef);

                                    scrollListToBottom();
                                }
                            };
                            break;
                        case SONG:
                            LogIt.d(this, "Forwarded song message");

                            final SongMessage songMessage = new SongMessage();
                            songMessage.setId(forwardedMessageId);

                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    songMessage.load();

                                    songMessage.setId(0);
                                    songMessage
                                            .setCommonFieldsForSend(channelID);

                                    songMessage.save(false);
                                }

                                @Override
                                public void done() {
                                    songMessage.forward(customAdapter,
                                            mMessagingServiceRef);

                                    scrollListToBottom();
                                }
                            };
                            break;
                        case VOICE:
                            LogIt.d(this, "Forwarded  voice message");

                            final VoiceMessage voiceMessage = new VoiceMessage();
                            voiceMessage.setId(forwardedMessageId);

                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    voiceMessage.load();

                                    voiceMessage.setId(0);
                                    voiceMessage
                                            .setCommonFieldsForSend(channelID);

                                    voiceMessage.save(false);
                                }

                                @Override
                                public void done() {

                                    voiceMessage.forward(customAdapter,
                                            mMessagingServiceRef);
                                    scrollListToBottom();
                                }
                            };
                            break;
                        case TEXT:
                            LogIt.d(this, "Forwarded  text message");

                            final TextMessage textMessage = new TextMessage();
                            textMessage.setId(forwardedMessageId);

                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    textMessage.load();
                                }

                                @Override
                                public void done() {
                                    Date date = new Date();
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.setTime(DateUtil
                                            .convertToDate(textMessage
                                                    .getCreatedAt()));

                                    date = calendar.getTime();

                                    String messageDate = DateFormat
                                            .getDateInstance(DateFormat.SHORT)
                                            .format(date);

                                    String textMeta = String
                                            .format(getString(R.string.forward_text_message_meta),
                                                    textMessage.getSender()
                                                            .getDisplayName(),
                                                    textMessage.getText(),
                                                    messageDate);

                                    chatInputEditTextView.setText(textMeta);

                                    scrollListToBottom();
                                }
                            };
                            break;
                        case CONTACT:
                            LogIt.d(this, "Forwarded contact message");

                            final ContactMessage contactMessage = new ContactMessage();
                            contactMessage.setId(forwardedMessageId);

                            new DatabaseTask() {

                                @Override
                                public void work() {
                                    contactMessage.load();

                                    contactMessage.setId(0);
                                    contactMessage
                                            .setCommonFieldsForSend(channelID);

                                    contactMessage.save(false, mConversation);
                                }

                                @Override
                                public void done() {
                                    contactMessage.forward(customAdapter,
                                            mMessagingServiceRef);

                                    scrollListToBottom();
                                }
                            };
                            break;
                        default:
                            LogIt.w(this, "Unexpected forwarded message type",
                                    forwardedMessageType);
                            break;
                    }
                } else if (!StringUtil.isEmpty(forwardedImage)) {
                    sendImageMessage(forwardedImage, IMessageType.PHOTO,
                            channelID);
                    getIntent().removeExtra(MessageMeConstants.EXTRA_IMAGE_KEY);
                }

                if (contact.isUser()) {

                    User user = (User) contact;

                    MentorBanner.cleanStack();

                    if (contact.getContactId() == MessageMeConstants.WELCOME_ROOM_ID) {
                        LogIt.d(this,
                                "Don't show mentor banner for welcome room");
                    } else {
                        // IsShown can't be read from contact, because the
                        // contact object is parsed from a PBUser, which doesn't
                        // contain an isShown field. Default to true so that the
                        // mentor banner does not get displayed if the extra is
                        // missing from the intent.
                        boolean isShown = getIntent().getBooleanExtra(
                                MessageMeConstants.RECIPIENT_IS_SHOWN, true);

                        MentorBanner banner = null;

                        // Block mentor banners should have priority over
                        // the "add contact" banner
                        if (user.isBlocked()) {
                            banner = new MentorBanner(ChatActivity.this,
                                    contact, MentorBannerType.BLOCKED,
                                    ChatActivity.this);

                            MentorBanner.addToQueue(banner);
                        }

                        if (user.isBlockedBy()) {
                            banner = new MentorBanner(ChatActivity.this,
                                    contact, MentorBannerType.BLOCKED_BY,
                                    ChatActivity.this);

                            MentorBanner.addToQueue(banner);
                        }

                        if (!isShown) {
                            banner = new MentorBanner(ChatActivity.this,
                                    contact, MentorBannerType.NOT_FRIEND,
                                    ChatActivity.this);

                            MentorBanner.addToQueue(banner);
                        }

                        if (!MentorBanner.isHeadNull()) {
                            mentorBannerContainer.addView(MentorBanner
                                    .getNext());
                        }
                    }
                }
            } else {
                // User or Room doesn't exist anymore, exit the ChatActivity
                finish();
            }

            getIntent().removeExtra(Message.ID_COLUMN);
            getIntent().removeExtra(Message.TYPE_COLUMN);
            getIntent().removeExtra(MessageMeConstants.RECIPIENT_IS_SHOWN);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogIt.d(this, "Disconnected from Messaging Service");
        }
    }

    private Runnable mUpdateTimeTask = new Runnable() {

        public void run() {
            final long start = startTime;
            long millis = SystemClock.uptimeMillis() - start;
            int seconds = (int) (millis / 1000);
            timerStop = DateUtils.formatElapsedTime(seconds);
            timerLabel.setText(timerStop);
            handler.postDelayed(this, 200);
        }
    };

    private RelativeLayout newMessageCompositionIndicator(int resId) {
        RelativeLayout layout = new RelativeLayout(this);

        if (resId != -1) {
            ImageView imageView = new ImageView(this);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
                    RelativeLayout.TRUE);

            imageView.setImageResource(resId);
            imageView.setScaleType(ScaleType.FIT_XY);

            layout.addView(imageView, layoutParams);
        }

        return layout;
    }

    private File createUniqueFile(IMessageType type) {
        File file = null;
        String fileName = null;

        switch (type) {
            case VOICE:
                fileName = DateUtil.now().getTime()
                        + MessageMeConstants.VOICE_MESSAGE_EXTENSION;
                file = ImageUtil.newFile(fileName);
                break;
            case VIDEO:
                fileName = DateUtil.now().getTime()
                        + MessageMeConstants.VIDEO_MESSAGE_EXTENSION;
                file = ImageUtil.newFile(fileName);
                break;
            default:
                return null;
        }

        return file;
    }

    private class OpenDoodlePicListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            sendMessageActivity(MessageActivity.DOODLE);

            Intent intent = new Intent(getApplicationContext(),
                    DoodleComposerActivity.class);

            startActivityForResult(intent, DOODLE_PIC_REQUEST_CODE);
        }
    }

    private class OpenLocationListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            doSendLocation();
        }
    }

    private class OpenPictureListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            doSendPhoto();
        }
    }

    private class OpenSongListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            changeInputState(InputState.NONE);
            doSendSong();
        }
    }

    private class OpenVoiceListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            changeInputState(InputState.NONE);
            inputContainer.setVisibility(View.GONE);
            voiceMsgComposerContainer.setVisibility(View.VISIBLE);
            canRecord = true;
        }
    }

    private class OpenVideoListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            sendMessageActivity(MessageActivity.VIDEO);

            VideoSelectorDialog videoSelectorDialog = VideoSelectorDialog
                    .newInstance();
            videoSelectorDialog.show(getSupportFragmentManager(),
                    DEFAULT_DIALOG_TAG);
        }
    }

    public static class VideoSelectorDialog extends MMAlertDialogFragment {

        public static VideoSelectorDialog newInstance() {
            VideoSelectorDialog dialogFragment = new VideoSelectorDialog();

            Bundle args = new Bundle();
            args.putInt(TITLE_RES_ID, R.string.video_dialog_title);

            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        @Override
        protected Builder getAlertDialogBuilder() {

            Builder builder = super.getAlertDialogBuilder();
            ChatActivity chatActivity = (ChatActivity) getActivity();

            builder.setPositiveButton(null, null);
            builder.setOnCancelListener(chatActivity.mOnCancelSelector);
            builder.setItems(R.array.video_dialog_options, mOnClickListener);

            return builder;
        }

        private DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                ChatActivity chatActivity = (ChatActivity) getActivity();
                switch (which) {
                    case 0:
                        LogIt.user(ChatActivity.class, "Youtube Video pressed");
                        chatActivity.changeInputState(InputState.NONE);
                        chatActivity.doSendYouTube();
                        break;
                    case 1:
                        LogIt.user(ChatActivity.class, "Take New video pressed");
                        chatActivity.doSendVideo();
                        break;
                    case 2:
                        LogIt.user(ChatActivity.class,
                                "Choose Existing video pressed");
                        chatActivity.openChooseVideo();
                        break;
                }
                dismiss();
            }
        };
    }

    /**
     * Choose an existing video from the user's gallery and send it. This
     * video should be copied into MessageMe, and the original must be
     * left unaltered in their gallery.
     */
    private void openChooseVideo() {
        final Intent galleryIntent = new Intent();

        galleryIntent.setType("video/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(galleryIntent,
                MessageMeConstants.VIDEO_REQUEST_SELECT_EXISTING_CODE);
    }

    private void openImageSelector() {

        final ImageSelectorDialog imageSelectorDialog = ImageSelectorDialog
                .newInstance(R.string.pic_update_dialog_title);
        imageSelectorDialog.show(getSupportFragmentManager(),
                DEFAULT_DIALOG_TAG);
    }

    public static class ImageSelectorDialog extends MMAlertDialogFragment {

        public static ImageSelectorDialog newInstance(int titleResId) {
            ImageSelectorDialog dialogFragment = new ImageSelectorDialog();

            Bundle args = new Bundle();
            args.putInt(TITLE_RES_ID, titleResId);
            dialogFragment.setArguments(args);

            return dialogFragment;
        }

        @Override
        protected Builder getAlertDialogBuilder() {

            Builder builder = super.getAlertDialogBuilder();
            ChatActivity chatActivity = (ChatActivity) getActivity();

            builder.setPositiveButton(null, null);
            builder.setItems(R.array.photo_message_dialog_options,
                    mOnClickListener);
            builder.setOnCancelListener(chatActivity.mOnCancelSelector);

            return builder;
        }

        private DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                Intent intent = null;
                ChatActivity chatActivity = (ChatActivity) getActivity();

                switch (which) {
                    case 0:
                        LogIt.user(ChatActivity.class, "Google Images search");
                        intent = chatActivity.searchWebImage();
                        chatActivity.changeInputState(InputState.NONE);
                        break;
                    case 1:
                        LogIt.user(ChatActivity.class, "Take Photo");
                        if (DeviceUtil.isCameraAvailable(getActivity())) {
                            if (DeviceUtil
                                    .isImageCaptureAppAvailable(getActivity())) {
                                intent = chatActivity.takePhoto();
                            } else {
                                chatActivity.showAlertFragment(
                                        R.string.no_image_capture_app_title,
                                        R.string.no_image_capture_app_message,
                                        false);
                            }
                        } else {
                            chatActivity.showAlertFragment(
                                    R.string.no_camera_title,
                                    R.string.no_camera_message, false);
                        }
                        break;
                    case 2:
                        LogIt.user(ChatActivity.class, "Choose Existing photo");
                        intent = chatActivity.chooseExistingPhoto();
                        break;

                }
                dismiss();

                if (intent != null) {
                    chatActivity.startActivityForResult(intent,
                            MessageMeConstants.PHOTO_REQUEST_CODE);
                }
            }
        };
    }

    private OnCancelListener mOnCancelSelector = new OnCancelListener() {

        @Override
        public void onCancel(DialogInterface dialog) {
            sendMessageActivity(MessageActivity.IDLE);
        }
    };

    private Intent searchWebImage() {
        Intent intent = new Intent(ChatActivity.this,
                SearchImagesActivity.class);
        intent.putExtra(SearchImagesActivity.EXTRA_SHOW_CONFIRMATION_SCREEN,
                true);
        return intent;
    }

    /**
     * Starts a camera intent to take a new photo
     * 
     * Very similar code is in takePhoto() in MyProfileFragment and
     * GroupProfileActivity.
     */
    private Intent takePhoto() {
        Intent captureIntent = new Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        // Specify an external file to save the photo to as the Camera
        // does not have permission to write to internal storage for our
        // application.
        //
        // We can't let the device pick the location to store the file in
        // as apparently numerous devices don't return that location.
        mediaImageOutput = ImageUtil
                .getTemporaryExternalFile(MessageMeConstants.PHOTO_MESSAGE_EXTENSION);
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(mediaImageOutput));

        LogIt.d(this, "Take photo to file", mediaImageOutput);

        return captureIntent;
    }

    private Intent chooseExistingPhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");

        return intent;
    }

    private void onStartVoiceMessageLongPress(int xCoord, int yCoord) {
        if (FileSystemUtil.isWritableSDCardPresent()) {
            double density = ImageUtil.getScreenDensity(ChatActivity.this);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

            params.leftMargin = (int) ((int) xCoord - (100 * density));
            params.topMargin = (int) ((int) yCoord - (230 * density));
            params.rightMargin = -250;
            params.bottomMargin = -250;

            // XXX Fix to avoid crash adding timerWindow twice into the
            // mainLayout
            if (timerWindow.getParent() != null) {
                LogIt.w(ChatActivity.class,
                        "timerWindow is already added in the main layout");
                mainLayout.removeView(timerWindow);
            }

            mainLayout.addView(timerWindow, params);
            timerLabel = (TextView) timerWindow.findViewById(R.id.timer_text);

            currentVoiceFile = createUniqueFile(IMessageType.VOICE);

            timerStop = "00:00";
            timerLabel.setText(timerStop);
            // Need to initialize the timerStop always, otherwhise after sending
            // a message the next one may not have the correct time length

            if (startTime == 0L) {
                startTime = SystemClock.uptimeMillis();
                handler.removeCallbacks(mUpdateTimeTask);
                handler.post(mUpdateTimeTask);
            }

            AudioUtil.startRecording(currentVoiceFile);
            isRecording = true;
        } else {
            LogIt.w(this, "Unable to make audio recording, no SD card");
            showAlertFragment(
                    getString(R.string.unable_to_record_title),
                    getString(R.string.detail_screen_saved_to_gallery_no_sdcard_error),
                    false);
        }
    }

    private void onEndVoiceMessageLongPress() {
        handler.removeCallbacks(mUpdateTimeTask);
        timerLabel.setText(timerStop);

        if (timerStop != null) {
            startTime = 0L;
            if ((currentVoiceFile == null) || !currentVoiceFile.exists()) {
                LogIt.e(this, "No voice recording file, don't try to send");
                mainLayout.removeView(timerWindow);
                AudioUtil.resetRecording();
                return;
            }

            AudioUtil.stopRecording();

            String timeString = timerLabel.getText().toString();

            int colonIndex = timeString.indexOf(":");

            int secondsDuration = 0;

            if (colonIndex == -1) {
                LogIt.w(this, "No colon found in time string");
            } else {
                // Convert the time string in the format 00:04 back into
                // seconds. On some devices we end up with a space that
                // needs to be trimmed.
                secondsDuration = (Integer.parseInt(timeString.substring(0,
                        colonIndex).trim()) * 60)
                        + Integer.parseInt(timeString.substring(colonIndex + 1)
                                .trim());
            }

            if (secondsDuration == 0) {
                // The user stopped the recording immediately, so don't send it
                LogIt.i(this,
                        "Don't send voice message with a 0 seconds duration");
                mainLayout.removeView(timerWindow);
                currentVoiceFile = null;
                return;
            }

            final VoiceMessage voiceMessage = new VoiceMessage();

            voiceMessage.setSeconds(secondsDuration);

            String mediaKey = MediaManager.generateObjectName(
                    MediaManager.MESSAGE_FOLDER,
                    StringUtil.getRandomFilename(), IMessageType.VOICE);

            // The voice file was moved, so use that new location
            final File cachedVoiceFile = ImageUtil.getFile(mediaKey);

            // Move the voice message file into the application data directory
            if (ImageUtil
                    .moveFile(currentVoiceFile.getAbsolutePath(), mediaKey)) {

                voiceMessage.setSoundKey(cachedVoiceFile.getAbsolutePath());
                voiceMessage.setCommandId(-1);
            } else {
                LogIt.w(this,
                        "Failed to copy voice message file into application data directory");

                // Don't try and send the voice message as it failed to copy
                // into our cache, but display the message with an error in
                // the message list so the user can listen to it again if
                // they want to.
                voiceMessage.setSoundKey(currentVoiceFile.getAbsolutePath());
                voiceMessage.setCommandId(0);
            }

            final long channelID = contact.getContactId();

            new DatabaseTask(handler) {

                @Override
                public void work() {
                    // setCommonFieldsForSend need to be called inside of a DB
                    // task because the method setChannelId will load the
                    // channel info automatically
                    voiceMessage.setCommonFieldsForSend(channelID);
                    voiceMessage.save(false, mConversation);
                }

                @Override
                public void done() {
                    if (timerStop != null) {
                        // Add the voice message to our durable send queue
                        voiceMessage.send(ChatActivity.this,
                                mMessagingServiceRef, cachedVoiceFile);
                    }

                    addMessageToAdapter(voiceMessage);
                    scrollListToBottom();
                    currentVoiceFile = null;
                }
            };
        }

        mainLayout.removeView(timerWindow);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (canRecord) {
            mGestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP && isRecording) {
                onEndVoiceMessageLongPress();
                isRecording = false;
            }
        }

        // For some reason this method is called while or after the Activity is
        // finished causing a window leak so if this happens return true to
        // notify the event as consumed instead of call super
        if (isFinishing()) {
            LogIt.w(ChatActivity.class, "dispatchTouchEvent call invalid");
            return true;
        } else {
            return super.dispatchTouchEvent(event);
        }
    }

    private class LongPressDetector extends
            GestureDetector.SimpleOnGestureListener {

        @Override
        public void onLongPress(MotionEvent event) {
            sendMessageActivity(MessageActivity.SOUND);

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(MessageMeConstants.VIBRATION_SHORT_MILLIS);

            onStartVoiceMessageLongPress((int) event.getX(), (int) event.getY());
        }
    }

    private void sendMessageRead(final long commandId) {

        if (commandId != 0 && commandId != -1) {
            User currentUser = MessageMeApplication.getCurrentUser();
            PBCommandEnvelope.Builder commandEnvelope = PBCommandEnvelope
                    .newBuilder();
            PBCommandMessageRead.Builder commandMessageRead = PBCommandMessageRead
                    .newBuilder();

            commandMessageRead.setCommandID(commandId);
            commandMessageRead.setRecipientID(mChat.getChatId());
            commandMessageRead.setReadDate(DateUtil.getCurrentTimestamp());

            commandEnvelope.setType(CommandType.MESSAGE_READ);
            commandEnvelope.setUserID(currentUser.getUserId());
            commandEnvelope.setClientID(DateUtil.now().getTime());
            commandEnvelope.setMessageRead(commandMessageRead.build());

            DurableCommand command = new DurableCommand(commandEnvelope.build());
            mMessagingServiceRef.addToDurableSendQueue(command);

            markConversationAsRead();
        } else {
            LogIt.e(ChatActivity.class, "Ignore send of MESSAGE_READ",
                    commandId, mChat.getChatId());
        }
    }

    private void sendMessageActivity(MessageActivity messageActivity) {
        if (mChat != null && !mChat.isGroupChat()) {
            User currentUser = MessageMeApplication.getCurrentUser();
            PBCommandEnvelope.Builder commandEnvelope = PBCommandEnvelope
                    .newBuilder();
            PBCommandPresenceUpdate.Builder commandPresenceUpdate = PBCommandPresenceUpdate
                    .newBuilder();

            commandPresenceUpdate.setRecipientID(mChat.getChatId());
            commandPresenceUpdate.setUserID(currentUser.getUserId());
            commandPresenceUpdate.setMessageActivity(messageActivity);

            commandEnvelope.setUserID(currentUser.getUserId());
            commandEnvelope.setType(CommandType.PRESENCE_UPDATE);
            commandEnvelope.setClientID(DateUtil.now().getTime());
            commandEnvelope.setPresenceUpdate(commandPresenceUpdate.build());

            try {
                LogIt.d(ChatActivity.class, "Sending message activity",
                        messageActivity);
                mMessagingServiceRef.sendCommand(commandEnvelope.build());
            } catch (Exception e) {
                LogIt.w(this, e, e.getMessage());
            }
        }
    }

    private class ChatInputTextWatcher implements TextWatcher {

        private boolean messageActivitySent;

        @Override
        public void afterTextChanged(Editable s) {
            if (TextUtils.isEmpty(s) && messageActivitySent) {
                messageActivitySent = false;
                sendMessageActivity(MessageActivity.IDLE);
            } else if (!TextUtils.isEmpty(s) && !messageActivitySent) {
                messageActivitySent = true;
                sendMessageActivity(MessageActivity.GENERIC);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
        }
    }

    private class ChatInputTouchListener implements OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                LogIt.user(ChatActivity.class, "Input text area touched");

                if (mCurrentInputState == InputState.NORMAL_KEYBOARD) {
                    // Leave the normal keyboard up
                } else if (mCurrentInputState != InputState.NONE) {
                    changeInputState(InputState.NONE);
                } else {
                    changeInputState(InputState.NORMAL_KEYBOARD);
                }
            }

            return false;
        }
    }

    Runnable showDelayedOptionsMenu = new Runnable() {

        @Override
        public void run() {
            optionsContainer.setVisibility(View.VISIBLE);
        }
    };

    Runnable showDelayedEmojiKeyboard = new Runnable() {

        @Override
        public void run() {
            mEmojiKeyboardContainer.setVisibility(View.VISIBLE);
        }
    };

    private class HideOptionsMenuAnimListener implements AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {
            optionsContainer.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }

    private class HideEmojiKeyboardAnimListener implements AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {
            mEmojiKeyboardContainer.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }

    private class ListViewTapListener extends
            GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            LogIt.user(ChatActivity.class,
                    "Single tap on message thread list view");
            changeInputState(InputState.NONE);

            return super.onSingleTapConfirmed(e);
        }
    }

    private class ChatListContianerTouchListener implements OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            listViewGestureDetector.onTouchEvent(event);
            return false;
        }
    }

    private BroadcastReceiver messageReadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            LogIt.i(ChatActivity.class, "Intent received", intent.getAction());

            if (intent.hasExtra(PBCommandEnvelope.class.getName())) {
                try {
                    long channelId;
                    User currentUser = MessageMeApplication.getCurrentUser();
                    PBCommandEnvelope commandEnvelope = PBCommandEnvelope
                            .parseFrom(intent
                                    .getByteArrayExtra(PBCommandEnvelope.class
                                            .getName()));

                    if (currentUser.getUserId() == commandEnvelope
                            .getMessageRead().getRecipientID()) {
                        // If current user is the recipient then this is a
                        // private chat,
                        // and the channel is their user ID
                        channelId = commandEnvelope.getUserID();
                    } else {
                        // Current user is not the recipient so this is a group
                        // chat, so the channel is the room ID
                        channelId = commandEnvelope.getMessageRead()
                                .getRecipientID();
                    }

                    if (commandEnvelope.getUserID() != currentUser.getUserId()
                            && channelId == mChat.getChatId()) {

                        customAdapter.notifyDataSetChanged();
                    } else {
                        LogIt.d(ChatActivity.class, "Intent for chat",
                                channelId, "omitting...");
                    }
                } catch (InvalidProtocolBufferException e) {
                    LogIt.e(ChatActivity.class, e);
                }
            } else {
                LogIt.w(ChatActivity.class, "Empty intent, omitting...");
            }
        }
    };

    private BroadcastReceiver messageActivityReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            LogIt.i(ChatActivity.class, "Intent received", intent.getAction());

            if (intent.hasExtra(PBCommandEnvelope.class.getName())) {
                try {
                    PBCommandEnvelope envelope = PBCommandEnvelope
                            .parseFrom(intent
                                    .getByteArrayExtra(PBCommandEnvelope.class
                                            .getName()));
                    PBCommandPresenceUpdate commandPresenceUpdate = envelope
                            .getPresenceUpdate();

                    if (commandPresenceUpdate.getUserID() == mChat.getChatId()
                            || commandPresenceUpdate.getRecipientID() == mChat
                                    .getChatId()) {
                        MessageActivity messageActivity = envelope
                                .getPresenceUpdate().getMessageActivity();

                        // Check if the footerCount is > 1, so we delete only
                        // the visible footer, not the dummy one
                        if ((listView.getFooterViewsCount() > 1)) {
                            listView.removeFooterView(messageCompositionIndicator);
                        }

                        switch (messageActivity) {
                            case DOODLE:
                                messageCompositionIndicator = newMessageCompositionIndicator(R.drawable.messagesview_bubble_compositionindicator_doodle);
                                break;
                            case GENERIC:
                                messageCompositionIndicator = newMessageCompositionIndicator(R.drawable.messagesview_bubble_compositionindicator_generic);
                                break;
                            case LOCATION:
                                messageCompositionIndicator = newMessageCompositionIndicator(R.drawable.messagesview_bubble_compositionindicator_location);
                                break;
                            case MUSIC:
                                messageCompositionIndicator = newMessageCompositionIndicator(R.drawable.messagesview_bubble_compositionindicator_music);
                                break;
                            case PICTURE:
                                messageCompositionIndicator = newMessageCompositionIndicator(R.drawable.messagesview_bubble_compositionindicator_picture);
                                break;
                            case SOUND:
                                messageCompositionIndicator = newMessageCompositionIndicator(R.drawable.messagesview_bubble_compositionindicator_sound);
                                break;
                            case VIDEO:
                                messageCompositionIndicator = newMessageCompositionIndicator(R.drawable.messagesview_bubble_compositionindicator_video);
                                break;
                            case IDLE:
                                LogIt.d(ChatActivity.class,
                                        "IDLE, set msg composition indicator to -1");
                                messageCompositionIndicator = newMessageCompositionIndicator(-1);
                                break;
                        }

                        // Add the real footer view
                        listView.addFooterView(messageCompositionIndicator,
                                null, false);
                        scrollListToBottom();
                    } else {
                        LogIt.d(ChatActivity.class,
                                "Intent for another chat, omitting...");
                    }
                } catch (InvalidProtocolBufferException e) {
                    LogIt.e(ChatActivity.class, e);
                }
            } else {
                LogIt.w(ChatActivity.class, "Empty intent, omitting...");
            }
        }
    };

    private BroadcastReceiver messageNewReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            LogIt.i(ChatActivity.class, "Intent received", intent.getAction());

            if (intent.hasExtra(Message.ID_COLUMN)
                    && intent.hasExtra(Message.CHANNEL_ID_COLUMN)
                    && intent.hasExtra(PBCommandEnvelope.class.getName())
                    && intent.hasExtra(MessageMeConstants.EXTRA_SORTED_BY)) {

                long channelId = intent.getLongExtra(Message.CHANNEL_ID_COLUMN,
                        -1);

                if (channelId == mChat.getChatId()) {
                    try {
                        final PBCommandEnvelope commandEnvelope = PBCommandEnvelope
                                .parseFrom(intent
                                        .getByteArrayExtra(PBCommandEnvelope.class
                                                .getName()));

                        // An individual MESSAGE_NEW arrived so add it to the UI
                        final long messageId = intent.getLongExtra(
                                Message.ID_COLUMN, -1);
                        final int messageIndex = customAdapter
                                .contains(messageId);
                        final double sortedBy = intent.getDoubleExtra(
                                MessageMeConstants.EXTRA_SORTED_BY, -1);

                        if (commandEnvelope.getType() == CommandType.MESSAGE_NEW) {
                            new DatabaseTask(handler) {

                                IMessage message;

                                @Override
                                public void work() {
                                    message = MessageUtil
                                            .newMessageFromPBMessage(commandEnvelope
                                                    .getMessageNew()
                                                    .getMessageEnvelope());

                                    // Message.parseFrom access the db to load
                                    // the sender and channel info so we need a
                                    // DatabaseTask to avoid dead locks
                                    message.parseFrom(commandEnvelope);
                                    message.setId(messageId);
                                    message.setSortedBy(sortedBy);
                                }

                                @Override
                                public void done() {
                                    if (!failed()) {
                                        if (messageIndex == -1) {
                                            LogIt.d(ChatActivity.class,
                                                    "Adding new message in the thread",
                                                    message.getType(),
                                                    message.getCommandId());

                                            customAdapter
                                                    .addReceivedMessage(message);
                                            sendMessageRead(message
                                                    .getCommandId());

                                            if (!mIsVisible) {
                                                mMsgArrivedWhileScreenOff = true;
                                            } else {
                                                scrollListToBottom();
                                            }
                                        } else {
                                            LogIt.d(ChatActivity.class,
                                                    "Message already in chat, update it",
                                                    message.getType(),
                                                    message.getCommandId());
                                            customAdapter.updateMessage(
                                                    message, messageIndex);
                                            sendMessageActivity(MessageActivity.IDLE);
                                        }

                                        // Check if the footerCount is > 1, so
                                        // we delete only the visible
                                        // footer, not the dummy one
                                        if ((listView.getFooterViewsCount() > 1)) {
                                            listView.removeFooterView(messageCompositionIndicator);
                                        }
                                    }
                                }
                            };
                        } else {
                            LogIt.w(ChatActivity.class,
                                    "Received unexpected command",
                                    commandEnvelope.getType());
                        }
                    } catch (InvalidProtocolBufferException e) {
                        LogIt.e(ChatActivity.class, e);
                    }
                } else {
                    LogIt.d(ChatActivity.class, "Intent for chat", channelId,
                            "omitting...");
                }
            } else if (intent.hasExtra(Message.ID_COLUMN)
                    && intent.hasExtra(Message.CHANNEL_ID_COLUMN)
                    && intent.hasExtra(MessageMeConstants.EXTRA_SORTED_BY)) {

                long channelId = intent.getLongExtra(Message.CHANNEL_ID_COLUMN,
                        -1);

                if (channelId == mChat.getChatId()) {

                    // An individual room notice arrived so add it to the UI
                    final long messageId = intent.getLongExtra(
                            Message.ID_COLUMN, -1);
                    final int messageIndex = customAdapter.contains(messageId);

                    if (messageIndex == -1) {
                        new DatabaseTask(handler) {

                            IMessage message;

                            @Override
                            public void work() {
                                message = MessageUtil
                                        .newMessageInstanceByType(IMessageType.NOTICE);
                                message.setId(messageId);
                                // No need to set the sortedBy as we are loading
                                // the message
                                message.load();
                            }

                            @Override
                            public void done() {
                                if (!failed()) {
                                    LogIt.d(ChatActivity.class, "Adding",
                                            message.getType(),
                                            message.getCommandId(),
                                            "in the thread");

                                    customAdapter.addReceivedMessage(message);
                                    markConversationAsRead();

                                    if (!mIsVisible) {
                                        mMsgArrivedWhileScreenOff = true;
                                    } else {
                                        scrollListToBottom();
                                    }

                                    // Check if the footerCount is > 1, so we
                                    // delete only the visible footer, not the
                                    // dummy one
                                    if ((listView.getFooterViewsCount() > 1)) {
                                        listView.removeFooterView(messageCompositionIndicator);
                                    }
                                }
                            }
                        };
                    } else {
                        LogIt.d(ChatActivity.class, "Notice already on thread",
                                messageId);
                    }
                }
            } else if (intent.hasExtra(MessageMeConstants.RECIPIENT_ID_KEY)) {
                long channelId = intent.getLongExtra(
                        MessageMeConstants.RECIPIENT_ID_KEY, -1);

                if (mChat.getChatId() == channelId) {
                    // This happens when the ChatActivity appears before
                    // the network layer has reconnected (e.g. when touching a
                    // GCM notification when the app has been idle for a long
                    // time). More than one message may have arrived so we have
                    // to refresh the whole UI.
                    LogIt.i(ChatActivity.class,
                            "New message(s) and/or read receipt(s) received in BATCH, update the UI again");
                    fillConversation();
                } else {
                    LogIt.d(ChatActivity.class, "Intent for chat", channelId,
                            "omitting...");
                }
            } else {
                LogIt.w(ChatActivity.class, "Empty intent, omitting...");
            }
        }
    };

    /**
     * This is triggered when Load Earlier Messages has downloaded commands
     * from the server, and finished processing them.
     */
    private BroadcastReceiver loadEarlierMessagesReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final long chatId = intent.getLongExtra(
                    MessageMeConstants.RECIPIENT_ID_KEY, -1);

            if (mChat.getChatId() == chatId) {
                LogIt.i(ChatActivity.class,
                        "INTENT_ACTION_EARLIER_MESSAGES_AVAILABLE received, load the messages",
                        chatId);
                customAdapter.loadEarlierMessagesFromDB(true);
            } else {
                LogIt.d(ChatActivity.class,
                        "Ignore INTENT_ACTION_EARLIER_MESSAGES_AVAILABLE for a different chat",
                        chatId);
            }
        }
    };

    /**
     * This is triggered when something about a user has changed. For now this
     * is only used to change the blocked state of private conversations.
     */
    private BroadcastReceiver userChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.hasExtra(MessageMeConstants.RECIPIENT_USER_KEY)) {
                User user = User
                        .parseFrom(intent
                                .getByteArrayExtra(MessageMeConstants.RECIPIENT_USER_KEY));

                if (mChat.getChatId() == user.getContactId()) {
                    LogIt.i(ChatActivity.class,
                            "INTENT_NOTIFY_USER_CHANGED received for this chat");
                    contact = user;
                    updateUI();
                } else {
                    LogIt.d(ChatActivity.class,
                            "Ignore INTENT_NOTIFY_USER_CHANGED for a different chat");
                }
            } else {
                LogIt.w(ChatActivity.class,
                        "Ignore INTENT_NOTIFY_USER_CHANGED intent with missing info");
            }
        }
    };

    private void scrollListToBottom() {
        if (listView != null) {
            scrollListToPosition(listView.getCount() - 1);
        } else {
            LogIt.w(ChatActivity.class, "listView null, unable to scroll chat");
        }
    }

    /**
     * Auto scrolling the chat thread to the bottom
     */
    private void scrollListToPosition(final int position) {
        // For some reason a simple post() is not able to update the UI all the
        // time because of that the postDelayed() with a short arbitrary value
        // of 250 mills have a better accuracy updating the list position
        listView.postDelayed(new Runnable() {

            @Override
            public void run() {
                listView.setSelection(position);
            }
        }, 250);
    }

    /**
     * Sends the text messages when the device is in landscape mode
     * and the Done key is active in the keyboard. Avoids the user to press
     * Done and then send.
     */
    private class DoneKeyListener implements EditText.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doSend(v);
            }
            return false;
        }

    }

    @Override
    public void onClickCompleted() {
        mentorBannerContainer.removeAllViews();

        if (!MentorBanner.isHeadNull()) {
            mentorBannerContainer.addView(MentorBanner.getNext());
        }
    }
}
