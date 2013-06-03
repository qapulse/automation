package com.littleinc.MessageMe.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.bo.AlertSetting;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.bo.User.UserBlockingListener;
import com.littleinc.MessageMe.bo.User.UserBlockingState;
import com.littleinc.MessageMe.bo.User.UserFriendshipListener;
import com.littleinc.MessageMe.bo.User.UserFriendshipState;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;
import com.littleinc.MessageMe.util.AlertPickClickListener;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.ImageLoader.ProfilePhotoSize;

@TargetApi(14)
public class ContactProfileActivity extends ActionBarActivity {

    private Handler mHandler = new Handler();

    private ImageView contactImage;

    private Button sendBtn;

    private User mUser;

    private ImageView coverImage;

    private TextView blockedTitle;

    private TextView blockedText;

    private long contactId;

    private TextView sectionTitle;

    private RelativeLayout alertSettingsContainer;

    private Button alertBtn;

    private TextView alertPeriod;

    private Button addToMyContactsBtn;

    private Button removeFromContactsBtn;

    private Button blockBtn;

    private Button unblockBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_profile_layout);

        contactId = getIntent().getLongExtra(
                MessageMeConstants.RECIPIENT_ID_KEY, -1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        blockedTitle = (TextView) findViewById(R.id.notice_title);
        blockedText = (TextView) findViewById(R.id.notice_text);
        coverImage = (ImageView) findViewById(R.id.cover_image);
        sectionTitle = (TextView) findViewById(R.id.section_title);
        alertBtn = (Button) findViewById(R.id.settings_btn);
        alertSettingsContainer = (RelativeLayout) findViewById(R.id.alerts_container);
        alertPeriod = (TextView) findViewById(R.id.alert_period);
        contactImage = (ImageView) findViewById(R.id.profile_image);
        sendBtn = (Button) findViewById(R.id.contact_profile_send_btn);
        addToMyContactsBtn = (Button) findViewById(R.id.contact_profile_add_btn);
        removeFromContactsBtn = (Button) findViewById(R.id.contact_profile_remove_btn);
        blockBtn = (Button) findViewById(R.id.contact_profile_block_btn);
        unblockBtn = (Button) findViewById(R.id.contact_profile_unblock_btn);

        loadContactInformation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.no_button_actionbar, menu);

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
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadContactInformation() {
        new DatabaseTask(mHandler) {

            @Override
            public void work() {
                LogIt.d(ContactProfileActivity.class, "Load contact", contactId);
                mUser = new User(contactId);
                mUser.load();
            }

            @Override
            public void done() {
                updateUI(mUser);
            }
        };
    }

    private void updateUI(User user) {
        setTitle(EmojiUtils.convertToEmojisIfRequired(user.getDisplayName(),
                EmojiSize.NORMAL));

        alertBtn.setOnClickListener(new SettingsClickListener());
        sendBtn.setOnClickListener(new SendMessageClickListener());

        if (user.isShown()) {
            showRemoveFromContactsBtn();
        } else {
            showAddToMyContactsBtn();
        }

        if (user.isBlocked() || user.isBlockedBy()) {
            LogIt.d(ContactProfileActivity.class, "Show blocked UI");

            blockedTitle.setVisibility(View.VISIBLE);
            blockedText.setVisibility(View.VISIBLE);

            String text;
                    
            if (user.isBlocked()) {
                // We blocked them
                text = getString(R.string.mentor_banner_blocked_desc,
                        user.getFirstName());

                blockBtn.setVisibility(View.GONE);
                unblockBtn.setVisibility(View.VISIBLE);
            } else {
                // They blocked us
                text = getString(R.string.blocked_by_msg, user.getFirstName());

                blockBtn.setVisibility(View.VISIBLE);
                unblockBtn.setVisibility(View.GONE);
            }
            
            blockedText.setText(EmojiUtils.convertToEmojisIfRequired(text,
                    EmojiSize.SMALL));

            // You cannot send messages or change the alert block if this
            // user has blocked you, or if you have blocked them
            sendBtn.setVisibility(View.GONE);
            sectionTitle.setVisibility(View.GONE);
            alertSettingsContainer.setVisibility(View.GONE);

            contactImage
                    .setBackgroundResource(R.drawable.profileheader_icon_blocked);
            contactImage.setImageDrawable(null);

            coverImage.setBackgroundColor(getResources().getColor(
                    R.color.profile_image_background));
            coverImage.setImageDrawable(null);
        } else {
            LogIt.d(ContactProfileActivity.class, "Show unblocked UI");

            blockedTitle.setVisibility(View.GONE);
            blockedText.setVisibility(View.GONE);
            blockedText.setText("");

            sendBtn.setVisibility(View.VISIBLE);
            sectionTitle.setVisibility(View.VISIBLE);
            alertSettingsContainer.setVisibility(View.VISIBLE);

            blockBtn.setVisibility(View.VISIBLE);
            unblockBtn.setVisibility(View.GONE);

            ImageLoader imgLoader = ImageLoader.getInstance();

            contactImage
                    .setBackgroundResource(R.drawable.profilepic_generic_user_large);
            imgLoader.displayProfilePicture(user, contactImage,
                    ProfilePhotoSize.LARGE);

            imgLoader.displayCoverPicture(user.getCoverImageKey(), coverImage);
        }

        if (AlertSetting.hasAlertBlock(user.getContactId())) {
            alertPeriod.setText(AlertSetting.getUserAlertBlock(
                    user.getContactId()).getSelectedOption());
        }
    }

    /**
     * Shows AddToMyContactsBtn and hides removeFromContactsBtn
     */
    private void showAddToMyContactsBtn() {
        addToMyContactsBtn.setVisibility(View.VISIBLE);
        removeFromContactsBtn.setVisibility(View.GONE);
    }

    /**
     * Shows removeFromContactsBtn and hides addToMyContactsBtn
     */
    private void showRemoveFromContactsBtn() {
        addToMyContactsBtn.setVisibility(View.GONE);
        removeFromContactsBtn.setVisibility(View.VISIBLE);
    }

    /**
     * Toggles the friendship with the selected contact
     * onClick defined in the XML
     */
    public void toggleContact(View view) {
        Button button = (Button) view;
        if (mUser != null) {
            LogIt.user(this, "toggleContact pressed", button.getText(),
                    mUser.getUserId());
            mUser.toggleFriendship(this,
                    new ToggleFriendshipCallback(mUser.getDisplayName()));
        } else {
            LogIt.e(this, "Cannot add contact, new contact is null");
        }
    }

    /**
     * Blocks the selected contact
     * onClick defined in the XML
     */
    public void blockContact(View view) {

        if (mUser != null) {
            LogIt.user(this, "Block pressed", mUser.getUserId());
            toggleBlock(true);
        } else {
            LogIt.e(this, "Cannot block contact, contact is null");
        }
    }

    /**
     * Unblocks the selected contact
     * onClick defined in the XML
     */
    public void unblockContact(View view) {

        if (mUser != null) {
            LogIt.user(this, "Unblock pressed", mUser.getUserId());
            toggleBlock(false);
        } else {
            LogIt.e(this, "Cannot unblock contact, contact is null");
        }
    }

    public void toggleBlock(boolean isBlockingThisUser) {
        mUser.toggleBlock(ContactProfileActivity.this, mUser.getContactId(),
                isBlockingThisUser, new ToggleBlockingCallback());

    }

    private class ToggleBlockingCallback implements UserBlockingListener {

        @Override
        public void onUserBlockingResponse(UserBlockingState state, User user) {

            switch (state) {
            case USER_BLOCKED:
                MMFirstSessionTracker.getInstance().abacus(null, "block",
                        "contact", null, null);
                if (user != null) {
                    updateUI(user);
                }
                break;

            case USER_UNBLOCKED:
                MMFirstSessionTracker.getInstance().abacus(null, "unblock",
                        "contact", null, null);
                if (user != null) {
                    updateUI(user);
                }
                break;

            case USER_BLOCKING_ERROR:
                LogIt.w(ContactProfileActivity.class,
                        "Error blocking/unblocking contact");
                break;

            default:
                LogIt.e(ContactProfileActivity.class,
                        "Unexpected state in blocking response", state);
                return;
            }

        }

    }

    private class ToggleFriendshipCallback implements UserFriendshipListener {

        String mDisplayName = "";

        public ToggleFriendshipCallback(String displayName) {
            mDisplayName = displayName;
        }

        @Override
        public void onUserFriendshipResponse(UserFriendshipState state) {
            loadContactInformation();

            switch (state) {
            case USER_FRIENDED:
                MMFirstSessionTracker.getInstance().abacus(null, "add",
                        "contact", null, null);
                alert(null, String.format(
                        getString(R.string.friends_invite_contact_added),
                        mDisplayName));
                return;
            case USER_UNFRIENDED:
                MMFirstSessionTracker.getInstance().abacus(null, "remove",
                        "contact", null, null);
                alert(null, String.format(
                        getString(R.string.friends_invite_contact_removed),
                        mDisplayName));
                return;
            case USER_FRIENDSHIP_ERROR:
                LogIt.w(ContactProfileActivity.class,
                        "Error adding/removing contact");
                return;
            default:
                LogIt.e(ContactProfileActivity.class,
                        "Unexpected state in friendship response", state);
                return;
            }
        }
    }

    private class SendMessageClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(ContactProfileActivity.this,
                    ChatActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            intent.putExtra(MessageMeConstants.RECIPIENT_USER_KEY, mUser
                    .toPBUser().toByteArray());

            intent.putExtra(MessageMeConstants.RECIPIENT_IS_SHOWN,
                    mUser.isShown());

            startActivity(intent);
            finish();
        }
    }

    private class SettingsClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    ContactProfileActivity.this);

            builder.setTitle(R.string.alerts_dialog_title);
            builder.setItems(R.array.alerts_dialog_options,
                    new AlertPickClickListener(mUser.getContactId(),
                            ContactProfileActivity.this, alertPeriod));

            builder.create().show();
        }
    }
}