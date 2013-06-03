package com.littleinc.MessageMe.ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

import org.restlet.resource.ResourceException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.bo.AlertSetting;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.NoticeMessage;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.RoomMember;
import com.littleinc.MessageMe.bo.NoticeType;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.ChatManager;
import com.littleinc.MessageMe.chat.MediaManager;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.chat.UploadS3Listener;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;
import com.littleinc.MessageMe.util.AlertPickClickListener;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.DateUtil;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.ImageLoader.ProfilePhotoSize;
import com.littleinc.MessageMe.util.ImageUtil;
import com.littleinc.MessageMe.util.NetUtil;
import com.littleinc.MessageMe.util.NoticeUtil;
import com.littleinc.MessageMe.util.StringUtil;

@TargetApi(14)
public class GroupProfileActivity extends ActionBarActivity {

    public static final int NUM_MEMBERS_TO_SHOW = 4;

    private static final int DOODLE_REQUEST_CODE = 3;

    private static final int TAKE_PHOTO_REQUEST_CODE = 1;

    private static final int GOOGLE_IMAGES_REQUEST_CODE = 0;

    private static final int CHOOSE_EXISTING_REQUEST_CODE = 2;

    private static final String PIC_TYPE_KEY = "pic_type_key";

    private static final String FILE_PATH_KEY = "file_path_key";

    private Room room;

    private long roomId;

    private Button leaveBtn;

    private Button inviteBtn;

    private Button sendMessageBtn;

    private ImageView coverPicture;

    private TextView roomNumMembers;

    private String picType;

    private String filePath;

    private ImageView profilePicture;

    private boolean showingAllMembers;

    private LinearLayout membersContainer;

    private Button settings;

    private TextView alertPeriod;

    private File mediaImageOutput;

    private List<User> roomMembers;

    private ImageLoader imageLoader = ImageLoader.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_profile);

        messagingServiceConnection = new MessagingServiceConnection();

        Intent intent = getIntent();
        roomId = intent.getLongExtra(MessageMeConstants.RECIPIENT_ID_KEY, -1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        leaveBtn = (Button) findViewById(R.id.group_profile_leave);
        inviteBtn = (Button) findViewById(R.id.group_profile_invite);
        profilePicture = (ImageView) findViewById(R.id.group_profile_image);
        coverPicture = (ImageView) findViewById(R.id.group_profile_cover_image);
        sendMessageBtn = (Button) findViewById(R.id.group_profile_send_message);
        roomNumMembers = (TextView) findViewById(R.id.group_profile_num_members);
        membersContainer = (LinearLayout) findViewById(R.id.group_profile_members);
        settings = (Button) findViewById(R.id.settings_btn);
        alertPeriod = (TextView) findViewById(R.id.alert_period);

        if (savedInstanceState != null) {
            picType = savedInstanceState.getString(PIC_TYPE_KEY);
            filePath = savedInstanceState.getString(FILE_PATH_KEY);

            String mediaImageOutputPath = savedInstanceState
                    .getString(MessageMeConstants.MEDIA_OUTPUT_PATH_KEY);

            if (mediaImageOutputPath != null) {
                mediaImageOutput = new File(
                        savedInstanceState
                                .getString(MessageMeConstants.MEDIA_OUTPUT_PATH_KEY));
            }
        }

        loadGroupInformation();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(PIC_TYPE_KEY, picType);
        outState.putString(FILE_PATH_KEY, filePath);

        if (mediaImageOutput != null) {
            outState.putString(MessageMeConstants.MEDIA_OUTPUT_PATH_KEY,
                    mediaImageOutput.getAbsolutePath());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.no_button_actionbar, menu);

        // Calling super after populating the menu is necessary here to ensure
        // that the
        // action bar helpers have a chance to handle this event.
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

    private void loadGroupInformation() {
        new DatabaseTask() {

            @Override
            public void work() {
                room = new Room(roomId);

                room.load();
                loadMemberUsers();
            }

            @Override
            public void done() {
                setTitle(EmojiUtils.convertToEmojisIfRequired(
                        room.getDisplayName(), EmojiSize.NORMAL));

                leaveBtn.setOnClickListener(new LeaveBtnClickListener());
                inviteBtn.setOnClickListener(new InviteBtnClickListener());
                coverPicture.setOnClickListener(new PictureClickListener());
                profilePicture.setOnClickListener(new PictureClickListener());
                sendMessageBtn
                        .setOnClickListener(new SendMessageBtnClickListener());
                settings.setOnClickListener(new SettingsClickListener());

                imageLoader.displayProfilePicture(room, profilePicture,
                        ProfilePhotoSize.LARGE);

                imageLoader.displayCoverPicture(room.getCoverImageKey(),
                        coverPicture);

                if (AlertSetting.hasAlertBlock(room.getContactId())) {
                    alertPeriod.setText(AlertSetting.getUserAlertBlock(
                            room.getContactId()).getSelectedOption());
                }

                handler.post(new memberListUpdate());
            }
        };
    }

    public User findOnList(List<User> contacts, long userId) {
        for (User contact : contacts) {
            if (contact.getUserId() == userId)
                return contact;
        }

        return null;
    }

    private RelativeLayout getCell(int resId) {
        RelativeLayout view = (RelativeLayout) LayoutInflater.from(this)
                .inflate(R.layout.cell_content, membersContainer, false);

        view.setBackgroundResource(resId);
        view.setClickable(true);

        return view;
    }

    private User getUser(int index) {
        User currentUser = MessageMeApplication.getCurrentUser();

        if (room.getMembers().get(index).getUserId() == currentUser.getUserId()) {
            return currentUser;
        } else {
            return roomMembers.get(index);
        }
    }

    /**
     * This method will fill a list with a user instance of each member of the current room
     */
    private void loadMemberUsers() {
        if (roomMembers == null) {
            roomMembers = new LinkedList<User>();
        }

        for (RoomMember roomMember : room.getMembers()) {
            User user = new User(roomMember.getUserId());

            // Checks if user instance is already loaded
            boolean contains = false;
            for (User item : roomMembers) {
                if (item.getUserId() == user.getUserId()) {
                    contains = true;
                    break;
                }
            }

            // Only add a user if is not present already
            if (!contains) {
                user.load();
                roomMembers.add(user);
            }
        }
    }

    private class memberListUpdate implements Runnable {

        @Override
        public void run() {
            User user = null;
            membersContainer.removeAllViews();
            membersContainer.removeAllViewsInLayout();

            roomNumMembers.setText(room.getMembers().size() + " "
                    + getString(R.string.group_profile_num_members));

            if (room.getMembers().size() == 1) {
                user = getUser(0);
                RelativeLayout view = getCell(R.drawable.groupedtable_cell_base_single);
                view.setTag(user);
                view.setOnClickListener(onMemberClickListener);

                ImageView profilePicView = (ImageView) view
                        .findViewById(R.id.cell_picture);
                TextView nameLabel = (TextView) view
                        .findViewById(R.id.cell_name);

                nameLabel.setText(user.getStyledNameWithEmojis());

                imageLoader.displayProfilePicture(user, profilePicView,
                        ProfilePhotoSize.SMALL);

                membersContainer.addView(view);
            } else {
                if (!showingAllMembers
                        && room.getMembers().size() > NUM_MEMBERS_TO_SHOW) {
                    for (int i = 0; i < NUM_MEMBERS_TO_SHOW; i++) {
                        RelativeLayout view = null;
                        user = getUser(i);

                        if (i == 0) {
                            view = getCell(R.drawable.cell_top_selector);
                        } else {
                            view = getCell(R.drawable.cell_middle_selector);
                        }

                        view.setTag(user);
                        view.setOnClickListener(onMemberClickListener);

                        ImageView profilePicView = (ImageView) view
                                .findViewById(R.id.cell_picture);
                        TextView nameLabel = (TextView) view
                                .findViewById(R.id.cell_name);

                        nameLabel.setText(user.getStyledNameWithEmojis());

                        imageLoader.displayProfilePicture(user, profilePicView,
                                ProfilePhotoSize.SMALL);

                        membersContainer.addView(view);
                    }

                    TextView showAllView = new TextView(
                            GroupProfileActivity.this);
                    showAllView
                            .setBackgroundResource(R.drawable.cell_bottom_selector);
                    showAllView.setText(String.format(
                            getString(R.string.group_profile_view_all),
                            String.valueOf(room.getMembers().size())));
                    showAllView.setOnClickListener(new ShowAllClickListener());
                    showAllView.setGravity(Gravity.CENTER);

                    membersContainer.addView(showAllView);
                } else {
                    for (int i = 0; i < room.getMembers().size(); i++) {
                        RelativeLayout view = null;
                        user = getUser(i);

                        if (i == 0) {
                            view = getCell(R.drawable.cell_top_selector);
                        } else if (i == room.getMembers().size() - 1) {
                            view = getCell(R.drawable.cell_bottom_selector);
                        } else {
                            view = getCell(R.drawable.cell_middle_selector);
                        }

                        view.setTag(user);
                        view.setOnClickListener(onMemberClickListener);

                        ImageView profilePicView = (ImageView) view
                                .findViewById(R.id.cell_picture);
                        TextView nameLabel = (TextView) view
                                .findViewById(R.id.cell_name);

                        nameLabel.setText(user.getStyledNameWithEmojis());

                        imageLoader.displayProfilePicture(user, profilePicView,
                                ProfilePhotoSize.SMALL);

                        membersContainer.addView(view);
                    }
                }
            }
        }
    }

    private class ShowAllClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            showingAllMembers = true;
            loadGroupInformation();
        }
    }

    private class SendMessageBtnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(GroupProfileActivity.this,
                    ChatActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            intent.putExtra(MessageMeConstants.RECIPIENT_ROOM_KEY, room
                    .toPBRoom().toByteArray());

            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case MessageMeConstants.GROUP_INVITE_REQUEST_CODE:
            switch (resultCode) {
            case RESULT_OK:
                final long contactId = data.getLongExtra(
                        MessageMeConstants.RECIPIENT_ID_KEY, -1);

                processRoomJoin(contactId);
                break;
            }
            break;
        case GOOGLE_IMAGES_REQUEST_CODE:
            switch (resultCode) {
            case Activity.RESULT_OK:
                if (data != null) {
                    if (data.hasExtra(SearchImagesActivity.EXTRA_OUTPUT_IMAGE_FILE)) {
                        filePath = data
                                .getStringExtra(SearchImagesActivity.EXTRA_OUTPUT_IMAGE_FILE);
                        LogIt.d(this, "GOOGLE_IMAGES_REQUEST_CODE file",
                                filePath);
                    } else {
                        LogIt.w(this,
                                "Missing image file in GOOGLE_IMAGES_REQUEST_CODE result");
                    }
                } else {
                    LogIt.w(this,
                            "Missing data in GOOGLE_IMAGES_REQUEST_CODE result");
                }
                break;
            }
            break;
        case TAKE_PHOTO_REQUEST_CODE:
            switch (resultCode) {
            case RESULT_OK:
                if (mediaImageOutput != null) {
                    filePath = mediaImageOutput.getAbsolutePath();

                    // Rotates the file if required
                    filePath = ImageUtil.rotatePicture(filePath,
                            GroupProfileActivity.this);

                    if (!(new File(filePath).exists())) {
                        LogIt.w(this, "Photo file does not exist");
                        filePath = null;
                    }
                } else {
                    LogIt.w(this,
                            "Missing mediaImageOutput in TAKE_PHOTO_REQUEST_CODE result");
                }
                break;
            case RESULT_CANCELED:
                LogIt.user(this, "Canceled Take New photo");
                filePath = null;
                break;
            }
            break;
        case CHOOSE_EXISTING_REQUEST_CODE:
            if (data != null) {

                final Intent resultData = data;
                final ProgressDialog progressDialog = UIUtil
                        .showProgressDialog(GroupProfileActivity.this,
                                getString(R.string.loading));

                new BackgroundTask() {

                    @Override
                    public void work() {
                        try {
                            filePath = FileSystemUtil
                                    .getMediaContentFromIntent(
                                            GroupProfileActivity.this,
                                            resultData, false);
                        } catch (MalformedURLException e) {
                            LogIt.e(GroupProfileActivity.class, e);
                            fail(getString(R.string.sending_image_error_title),
                                    getString(R.string.unexpected_error));
                        } catch (IOException e) {
                            LogIt.e(GroupProfileActivity.class, e);
                            fail(getString(R.string.sending_image_error_title),
                                    getString(R.string.network_error));
                        } catch (ResourceException e) {
                            LogIt.e(GroupProfileActivity.class, e);
                            fail(getString(R.string.sending_image_error_title),
                                    getString(R.string.network_error));
                        } catch (Exception e) {
                            LogIt.e(GroupProfileActivity.class, e,
                                    e.getMessage());
                            fail(getString(R.string.sending_image_error_title),
                                    getString(R.string.unexpected_error));
                        }
                    }

                    @Override
                    public void done() {
                        progressDialog.dismiss();

                        if (failed()) {
                            UIUtil.alert(GroupProfileActivity.this,
                                    getExceptionTitle(), getExceptionMessage());
                        } else {
                            if (!StringUtil.isEmpty(filePath)
                                    && mMessagingServiceRef != null) {
                                uploadPicture(filePath, picType,
                                        mMessagingServiceRef);
                            } else if (!StringUtil.isEmpty(filePath)) {
                                LogIt.i(GroupProfileActivity.class,
                                        "Postponed picture upload mMessagingServiceRef is null",
                                        filePath, picType);
                            }
                        }
                    }
                };
            } else {
                LogIt.w(this, "No data in CHOOSE_EXISTING_REQUEST_CODE");
            }
            break;
        case DOODLE_REQUEST_CODE:
            if (data != null) {
                filePath = data
                        .getStringExtra(MessageMeConstants.FILE_NAME_KEY);
            }
            break;
        }

        if (!StringUtil.isEmpty(filePath) && mMessagingServiceRef != null) {
            uploadPicture(filePath, picType, mMessagingServiceRef);
        } else if (!StringUtil.isEmpty(filePath)) {
            LogIt.i(GroupProfileActivity.class,
                    "Postponed picture upload mMessagingServiceRef is null",
                    filePath, picType);
        }
    }

    private class InviteBtnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(GroupProfileActivity.this,
                    MessageContactActivity.class);

            intent.putExtra(MessageMeConstants.IS_GROUP_INVITE, true);

            startActivityForResult(intent,
                    MessageMeConstants.GROUP_INVITE_REQUEST_CODE);
        }
    }

    private class LeaveBtnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    v.getContext());

            builder.setTitle(R.string.leave_group_alert_title);
            builder.setMessage(R.string.leave_group_alert_message);

            builder.setPositiveButton(R.string.group_profile_leave,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogIt.user(GroupProfileActivity.class,
                                    "Pressed leave group");
                            processRoomLeave();
                        }
                    });

            builder.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            builder.create().show();
        }
    }

    /**
     * Process the Room leave action
     * Post the room leave into the restful client
     * and checks for errors, this way we maintain the UI 
     * synchronized with the server
     */
    private void processRoomLeave() {

        final ProgressDialog progressDialog = showProgressDialog(this
                .getString(R.string.group_dialog_leave_message));

        new BackgroundTask() {

            @Override
            public void work() {
                if (NetUtil.checkInternetConnection(GroupProfileActivity.this)) {

                    try {
                        RestfulClient.getInstance().groupUserLeave(
                                room.getRoomId());
                    } catch (IOException e) {
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    } catch (ResourceException e) {
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    } catch (Exception e) {
                        fail(getString(R.string.generic_error_title),
                                getString(R.string.unexpected_error));
                    }
                } else {
                    fail(getString(R.string.network_error_title),
                            getString(R.string.network_error));
                }
            }

            @Override
            public void done() {
                if (!failed()) {
                    // Remove the room from the DB and update the UI
                    new DatabaseTask(GroupProfileActivity.this.handler) {

                        @Override
                        public void work() {

                            Conversation.delete(room.getRoomId());
                            room.disableCursor(false);
                            room.delete();
                        }

                        @Override
                        public void done() {

                            mMessagingServiceRef
                                    .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST);
                            mMessagingServiceRef
                                    .notifyChatClient(MessageMeConstants.INTENT_NOTIFY_CONTACT_LIST);

                            progressDialog.dismiss();

                            Intent intent = getIntent();
                            intent.putExtra(
                                    MessageMeConstants.EXTRA_GROUP_LEAVE, true);

                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    };
                } else {
                    progressDialog.dismiss();
                    alert(getExceptionTitle(), getExceptionMessage());
                }
            }
        };
    }

    /**
     * Process the Room join action
     * Post the room join into the restful client
     * and checks for errors, this way we maintain the UI 
     * synchronized with the server
     */
    private void processRoomJoin(final long contactId) {

        final ProgressDialog progressDialog = showProgressDialog(this
                .getString(R.string.group_dialog_join_message));

        new BackgroundTask() {

            PBCommandEnvelope commandEnvelope;

            @Override
            public void work() {
                if (NetUtil.checkInternetConnection(GroupProfileActivity.this)) {

                    try {
                        commandEnvelope = RestfulClient.getInstance()
                                .groupUserJoin(room.getRoomId(), contactId);
                    } catch (IOException e) {
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    } catch (ResourceException e) {
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    } catch (Exception e) {
                        fail(getString(R.string.generic_error_title),
                                getString(R.string.unexpected_error));
                    }
                } else {
                    fail(getString(R.string.network_error_title),
                            getString(R.string.network_error));
                }
            }

            @Override
            public void done() {
                if (!failed()) {
                    new DatabaseTask(GroupProfileActivity.this.handler) {

                        @Override
                        public void work() {

                            // Add user to the room
                            User newMember = new User(contactId);
                            newMember.load();

                            roomMembers.add(newMember);
                            ChatManager.addParticipantIfChatExists(
                                    room.getRoomId(), newMember);

                            RoomMember roomMember = new RoomMember(newMember,
                                    room);

                            if (!roomMember.exists()) {
                                try {
                                    Room.addMember(newMember, room);

                                    double nextSortedBy = DateUtil
                                            .getCurrentTimeMicros();

                                    // Process the envelop to create the corresponding room notice
                                    NoticeMessage noticeMessage = NoticeUtil
                                            .generateNoticeMessage(
                                                    commandEnvelope,
                                                    NoticeType.ROOM_JOIN,
                                                    nextSortedBy);

                                    if (noticeMessage != null) {
                                        mMessagingServiceRef
                                                .notifyChatClient(
                                                        MessageMeConstants.INTENT_ACTION_MESSAGE_NEW,
                                                        noticeMessage.getId(),
                                                        noticeMessage
                                                                .getChannelId(),
                                                        noticeMessage
                                                                .getSortedBy());
                                    } else {
                                        LogIt.w(GroupProfileActivity.class,
                                                "Unable to create room notice");
                                    }
                                } catch (Exception e) {
                                    LogIt.e(GroupProfileActivity.class, e,
                                            e.getMessage());
                                }
                            } else {
                                LogIt.d(GroupProfileActivity.class, "User",
                                        roomMember.getUserId(),
                                        "is already part of the room",
                                        room.getRoomId());
                            }
                        }

                        @Override
                        public void done() {
                            progressDialog
                                    .setOnDismissListener(new OnDismissListener() {

                                        @Override
                                        public void onDismiss(
                                                DialogInterface dialog) {
                                            loadGroupInformation();
                                        }
                                    });
                            progressDialog.dismiss();
                        };
                    };
                } else {
                    progressDialog.dismiss();
                    alert(getExceptionTitle(), getExceptionMessage());
                }
            }
        };
    }

    private class PictureClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    GroupProfileActivity.this);

            builder.setTitle(R.string.pic_update_dialog_title);
            builder.setItems(R.array.pic_update_dialog_options,
                    new PicUpdateDialogClickListener());

            builder.create().show();
        }
    }

    private class PicUpdateDialogClickListener implements
            DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
            case 0:
                LogIt.user(GroupProfileActivity.class,
                        "Update Profile Picture pressed");
                openImageSelector(MessageMeConstants.PROFILE_PIC);
                break;
            case 1:
                LogIt.user(GroupProfileActivity.class,
                        "Update Cover Picture pressed");
                openImageSelector(MessageMeConstants.COVER_PIC);
                break;
            }

            dialog.dismiss();
        }
    }

    private void openImageSelector(String picType) {
        this.picType = picType;

        AlertDialog.Builder builder = new AlertDialog.Builder(
                GroupProfileActivity.this);

        builder.setTitle(R.string.pic_update_dialog_title);
        builder.setItems(R.array.pic_action_dialog_options,
                new PicActionDialogClickListener());

        builder.create().show();
    }

    private class PicActionDialogClickListener implements
            DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
            case 0:
                LogIt.user(GroupProfileActivity.class, "Google Images pressed");
                googleImages();
                break;
            case 1:
                LogIt.user(GroupProfileActivity.class, "Take Photo pressed");
                takePhoto();
                break;
            case 2:
                LogIt.user(GroupProfileActivity.class,
                        "Choose Existing pressed");
                chooseExisting();
                break;
            case 3:
                LogIt.user(GroupProfileActivity.class, "Doodle pressed");
                createDoodle();
                break;
            }

            dialog.dismiss();
        }
    }

    private void googleImages() {
        Intent intent = new Intent(this, SearchImagesActivity.class);
        intent.putExtra(SearchImagesActivity.EXTRA_SHOW_CONFIRMATION_SCREEN,
                false);
        startActivityForResult(intent, GOOGLE_IMAGES_REQUEST_CODE);
    }

    private void takePhoto() {

        if (DeviceUtil.isCameraAvailable(GroupProfileActivity.this)) {
            if (DeviceUtil
                    .isImageCaptureAppAvailable(GroupProfileActivity.this)) {
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

                startActivityForResult(captureIntent, TAKE_PHOTO_REQUEST_CODE);
            } else {
                UIUtil.alert(GroupProfileActivity.this,
                        R.string.no_image_capture_app_title,
                        R.string.no_image_capture_app_message);
            }
        } else {
            UIUtil.alert(GroupProfileActivity.this, R.string.no_camera_title,
                    R.string.no_camera_message);
        }
    }

    private void chooseExisting() {
        Intent galleryIntent = new Intent();

        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(galleryIntent, CHOOSE_EXISTING_REQUEST_CODE);
    }

    private void createDoodle() {
        startActivityForResult(new Intent(this, DoodleComposerActivity.class),
                DOODLE_REQUEST_CODE);
    }

    public void uploadPicture(final String fileToUpload,
            final String uploadType, MessagingService listener) {

        picType = null;
        filePath = null;
        final ProgressDialog progressDialog = ProgressDialog.show(this, null,
                getString(R.string.uploading_photo), true, true);

        if (!StringUtil.isEmpty(fileToUpload)) {
            int maxSize = -1;
            if (uploadType.equals(MessageMeConstants.PROFILE_PIC)) {
                maxSize = getResources().getDimensionPixelSize(
                        R.dimen.max_profile_photo_size);
            } else if (uploadType.equals(MessageMeConstants.COVER_PIC)) {
                maxSize = getResources().getDimensionPixelSize(
                        R.dimen.max_cover_photo_size);
            }

            listener.getChatManager().uploadProfileAsset(this, fileToUpload,
                    maxSize, MediaManager.PROFILE_FOLDER,
                    new UploadS3Listener() {

                        @Override
                        public void onUploadCompleted(final String mediaKey) {

                            new BackgroundTask() {

                                private String originalCoverPic;

                                private String originalProfilePic;

                                @Override
                                public void work() {
                                    try {
                                        if (uploadType
                                                .equals(MessageMeConstants.PROFILE_PIC)) {
                                            room.setProfileImageKey(mediaKey);
                                        } else if (uploadType
                                                .equals(MessageMeConstants.COVER_PIC)) {
                                            room.setCoverImageKey(mediaKey);
                                        }

                                        RestfulClient.getInstance().updateRoom(
                                                room, uploadType);

                                        // Delete the old profile photos from the cache
                                        //
                                        // Profile photos are stored by contact ID, not
                                        // by image key, so old ones must be deleted.
                                        imageLoader
                                                .deleteProfilePictureFromCaches(room);
                                    } catch (IOException e) {
                                        LogIt.e(GroupProfileActivity.class, e,
                                                "Image upload error");
                                        fail(getString(R.string.network_error_title),
                                                getString(R.string.network_error));
                                    } catch (ResourceException e) {
                                        LogIt.e(GroupProfileActivity.class, e,
                                                "Image upload error");
                                        fail(getString(R.string.network_error_title),
                                                getString(R.string.network_error));
                                    } catch (Exception e) {
                                        LogIt.e(GroupProfileActivity.class, e,
                                                "Image upload error");
                                        fail(getString(R.string.generic_error_title),
                                                getString(R.string.unexpected_error));
                                    }
                                }

                                @Override
                                public void done() {
                                    progressDialog.dismiss();

                                    if (failed()) {
                                        room.setCoverImageKey(originalCoverPic);
                                        room.setProfileImageKey(originalProfilePic);

                                        alert(getExceptionTitle(),
                                                getExceptionMessage());
                                    } else {
                                        new DatabaseTask(handler) {

                                            @Override
                                            public void work() {
                                                room.save();
                                            }

                                            @Override
                                            public void done() {
                                                if (uploadType
                                                        .equals(MessageMeConstants.PROFILE_PIC)) {
                                                    // Display the new profile photo
                                                    imageLoader
                                                            .displayProfilePicture(
                                                                    room,
                                                                    profilePicture,
                                                                    ProfilePhotoSize.LARGE);
                                                } else if (uploadType
                                                        .equals(MessageMeConstants.COVER_PIC)) {
                                                    imageLoader
                                                            .displayCoverPicture(
                                                                    room.getCoverImageKey(),
                                                                    coverPicture);
                                                }
                                            }
                                        };
                                    }
                                }
                            };
                        }

                        @Override
                        public void onUploadError(String messageTitle,
                                String messageError) {

                            LogIt.w(GroupProfileActivity.class,
                                    "Image upload failed", messageError);
                            progressDialog.dismiss();

                            alert(messageTitle, messageError);
                        }
                    });
        } else {
            LogIt.w(this, "Ignore attempt to upload null or empty picture",
                    fileToUpload);
        }
    }

    private OnClickListener onMemberClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            User selectedUser = (User) v.getTag();
            Intent intent = new Intent(GroupProfileActivity.this,
                    ContactProfileActivity.class);

            intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                    selectedUser.getContactId());

            startActivity(intent);
        }
    };

    private class SettingsClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            AlertDialog.Builder builder = new AlertDialog.Builder(
                    GroupProfileActivity.this);

            builder.setTitle(R.string.alerts_dialog_title);
            builder.setItems(R.array.alerts_dialog_options,
                    new AlertPickClickListener(room.getContactId(),
                            GroupProfileActivity.this, alertPeriod));

            builder.create().show();
        }
    }

    private class MessagingServiceConnection implements ServiceConnection {

        /**
         * Register a BroadcastReceiver for the intent INTENT_NOTIFY_MESSAGE_LIST 
         * {@link MessagingService#notifyMessageList(long messageID)}
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogIt.d(GroupProfileActivity.class,
                    "Connected to Messaging Service");
            mMessagingServiceRef = ((MessagingService.MessagingBinder) service)
                    .getService();

            MessageMeApplication.appIsActive(GroupProfileActivity.this,
                    mMessagingServiceRef);

            if (!TextUtils.isEmpty(filePath) && !TextUtils.isEmpty(picType)) {
                LogIt.i(GroupProfileActivity.class, "Resumed picture upload",
                        filePath, picType);
                uploadPicture(filePath, picType, mMessagingServiceRef);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMessagingServiceRef = null;
        }
    }
}