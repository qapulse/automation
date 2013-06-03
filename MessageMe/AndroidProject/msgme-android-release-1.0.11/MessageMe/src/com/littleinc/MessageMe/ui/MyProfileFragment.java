package com.littleinc.MessageMe.ui;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.restlet.resource.ResourceException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeFragment;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.chat.MediaManager;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.chat.UploadS3Listener;
import com.littleinc.MessageMe.metrics.MMFirstWeekTracker;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.EditUserProfile;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.ImageLoader.ProfilePhotoSize;
import com.littleinc.MessageMe.util.ImageUtil;
import com.littleinc.MessageMe.util.StringUtil;

public class MyProfileFragment extends MessageMeFragment {

    private static final int DOODLE_REQUEST_CODE = 3;

    private static final int TAKE_PHOTO_REQUEST_CODE = 1;

    private static final int GOOGLE_IMAGES_REQUEST_CODE = 0;

    private static final int CHOOSE_EXISTING_REQUEST_CODE = 2;

    private static final String PIC_TYPE_KEY = "pic_type_key";

    private static final String FILE_PATH_KEY = "file_path_key";

    private TextView contactID;

    private ImageView contactImage;

    private ImageView coverImage;

    private String picType;

    private String filePath;

    private File mediaImageOutput;

    private TextView settings;

    private TextView profileName;

    // These need to be saved as member fields as getResources() can sometimes
    // be null when uploadPicture is called
    private int maxProfilePhotoSize;

    private int maxCoverPhotoSize;

    private ImageLoader imageLoader;

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

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

        imageLoader = ImageLoader.getInstance();
        messagingServiceConnection = new MessagingServiceConnection();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.myprofile_layout, container,
                false);

        contactID = (TextView) view.findViewById(R.id.user_pin);
        coverImage = (ImageView) view.findViewById(R.id.cover_image);
        contactImage = (ImageView) view.findViewById(R.id.profile_image);
        settings = (TextView) view.findViewById(R.id.myprofile_settings);
        profileName = (TextView) view.findViewById(R.id.profile_name);

        maxProfilePhotoSize = getResources().getDimensionPixelSize(
                R.dimen.max_profile_photo_size);

        maxCoverPhotoSize = getResources().getDimensionPixelSize(
                R.dimen.max_cover_photo_size);

        LogIt.d(this, "Max dimensions for profile and cover photos are",
                maxProfilePhotoSize, maxCoverPhotoSize);

        return view;
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle("");

        coverImage.setOnClickListener(new PictureClickListener());
        contactImage.setOnClickListener(new PictureClickListener());

        settings.setOnClickListener(new SettingsClickListener());
        profileName.setVisibility(View.VISIBLE);

        loadContactInfo();
    }

    private void loadContactInfo() {
        User currentUser = MessageMeApplication.getCurrentUser();

        loadContactName();
        contactID.setText(formatPIN(currentUser.getPin()));

        imageLoader.displayProfilePicture(currentUser, contactImage,
                ProfilePhotoSize.LARGE);
        imageLoader.displayCoverPicture(currentUser.getCoverImageKey(),
                coverImage);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MessageMeApplication.getCurrentUser() != null) {
            loadContactName();
        }       
    }

    private String formatPIN(String rawPin) {

        String pin = rawPin;

        if (!StringUtil.isEmpty(pin)) {
            pin = pin.substring(0, 2) + " " + pin.substring(2, 5) + " "
                    + pin.substring(5, pin.length());
        }

        return pin;
    }

    public void loadContactName() {
        User currentUser = MessageMeApplication.getCurrentUser();
        profileName.setText(EmojiUtils.convertToEmojisIfRequired(
                currentUser.getDisplayName(), EmojiSize.NORMAL));
    }

    private void openImageSelector(String picType) {
        this.picType = picType;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.pic_update_dialog_title);
        builder.setItems(R.array.pic_action_dialog_options,
                new PicActionDialogClickListener());

        builder.create().show();
    }

    private class PictureClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (getActivity() != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        getActivity());

                builder.setTitle(R.string.pic_update_dialog_title);
                builder.setItems(R.array.pic_update_dialog_options,
                        new PicUpdateDialogClickListener());

                builder.create().show();
            } else {
                LogIt.w(this, "Ignore profile/cover click as activity is null");
            }
        }
    }

    private class PicUpdateDialogClickListener implements
            DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
            case 0:
                openImageSelector(MessageMeConstants.PROFILE_PIC);
                break;
            case 1:
                openImageSelector(MessageMeConstants.COVER_PIC);
                break;
            }

            dialog.dismiss();
        }
    }

    private class PicActionDialogClickListener implements
            DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {

            case 0:
                googleImages();
                break;
            case 1:
                takePhoto();
                break;
            case 2:
                chooseExisting();
                break;
            case 3:
                createDoodle();
                break;
            }

            dialog.dismiss();
        }
    }

    private void googleImages() {
        Intent intent = new Intent(getActivity(), SearchImagesActivity.class);
        intent.putExtra(SearchImagesActivity.EXTRA_SHOW_CONFIRMATION_SCREEN,
                false);
        startActivityForResult(intent, GOOGLE_IMAGES_REQUEST_CODE);
    }

    private void takePhoto() {

        if (DeviceUtil.isCameraAvailable(getActivity())) {
            if (DeviceUtil.isImageCaptureAppAvailable(getActivity())) {
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
                UIUtil.alert(getActivity(),
                        R.string.no_image_capture_app_title,
                        R.string.no_image_capture_app_message);
            }
        } else {
            UIUtil.alert(getActivity(), R.string.no_camera_title,
                    R.string.no_camera_message);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
        case GOOGLE_IMAGES_REQUEST_CODE:
            switch (resultCode) {
            case Activity.RESULT_OK:
                if (data != null) {
                    if (data.hasExtra(SearchImagesActivity.EXTRA_OUTPUT_IMAGE_FILE)) {
                        filePath = data
                                .getStringExtra(SearchImagesActivity.EXTRA_OUTPUT_IMAGE_FILE);
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
            case Activity.RESULT_OK:
                if (mediaImageOutput != null) {
                    filePath = mediaImageOutput.getAbsolutePath();

                    // Rotates the file if required
                    filePath = ImageUtil.rotatePicture(filePath, getActivity());

                    if (!(new File(filePath).exists())) {
                        LogIt.w(this, "Photo file does not exist");
                        filePath = null;
                    }
                } else {
                    LogIt.d(this,
                            "Missing mediaImageOutput in TAKE_PHOTO_REQUEST_CODE result");
                }
                break;
            case Activity.RESULT_CANCELED:
                LogIt.user(this, "Canceled Take New photo");
                filePath = null;
                break;
            }
            break;
        case CHOOSE_EXISTING_REQUEST_CODE:
            if (data != null) {

                final Intent resultData = data;
                final ProgressDialog progressDialog = UIUtil
                        .showProgressDialog(getActivity(),
                                getString(R.string.loading));

                new BackgroundTask() {

                    @Override
                    public void work() {
                        try {
                            filePath = FileSystemUtil
                                    .getMediaContentFromIntent(getActivity(),
                                            resultData, false);
                        } catch (MalformedURLException e) {
                            LogIt.e(MyProfileFragment.class, e);
                            fail(getString(R.string.sending_image_error_title),
                                    getString(R.string.unexpected_error));
                        } catch (IOException e) {
                            LogIt.e(MyProfileFragment.class, e);
                            fail(getString(R.string.sending_image_error_title),
                                    getString(R.string.network_error));
                        } catch (Exception e) {
                            LogIt.e(MyProfileFragment.class, e, e.getMessage());
                            fail(getString(R.string.sending_image_error_title),
                                    getString(R.string.unexpected_error));
                        }
                    }

                    @Override
                    public void done() {
                        progressDialog.dismiss();

                        if (failed()) {
                            UIUtil.alert(getActivity(), getExceptionTitle(),
                                    getExceptionMessage());
                        } else {
                            if (!StringUtil.isEmpty(filePath)
                                    && mMessagingServiceRef != null) {
                                uploadPicture(filePath, picType,
                                        mMessagingServiceRef);
                            } else if (!StringUtil.isEmpty(filePath)) {
                                LogIt.i(MyProfileFragment.class,
                                        "Postponed picture upload mMessagingServiceRef is null",
                                        filePath, picType);
                            }
                        }
                    }
                };
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
            LogIt.i(MyProfileFragment.class,
                    "Postponed picture upload mMessagingServiceRef is null",
                    filePath, picType);
        }
    }

    private void chooseExisting() {
        Intent galleryIntent = new Intent();

        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(galleryIntent, CHOOSE_EXISTING_REQUEST_CODE);
    }

    private void createDoodle() {
        startActivityForResult(new Intent(getActivity(),
                DoodleComposerActivity.class), DOODLE_REQUEST_CODE);
    }

    public void uploadPicture(final String fileToUpload,
            final String uploadType, MessagingService listener) {

        picType = null;
        filePath = null;
        final ProgressDialog dialog = ProgressDialog.show(getActivity(), null,
                getString(R.string.uploading_photo), true, true);

        if (!StringUtil.isEmpty(fileToUpload)) {
            int maxSize = -1;
            if (uploadType.equals(MessageMeConstants.PROFILE_PIC)) {
                maxSize = maxProfilePhotoSize;
            } else if (uploadType.equals(MessageMeConstants.COVER_PIC)) {
                maxSize = maxCoverPhotoSize;
            }

            final User currentUser = MessageMeApplication.getCurrentUser();

            listener.getChatManager().uploadProfileAsset(getActivity(),
                    fileToUpload, maxSize, MediaManager.PROFILE_FOLDER,
                    new UploadS3Listener() {

                        @Override
                        public void onUploadCompleted(final String mediaKey) {

                            new BackgroundTask() {

                                private String originalCoverPic;

                                private String originalProfilePic;

                                @Override
                                public void work() {

                                    originalCoverPic = currentUser
                                            .getCoverImageKey();
                                    originalProfilePic = currentUser
                                            .getProfileImageKey();

                                    try {
                                        if (uploadType
                                                .equals(MessageMeConstants.PROFILE_PIC)) {
                                            currentUser
                                                    .setProfileImageKey(mediaKey);
                                        } else if (uploadType
                                                .equals(MessageMeConstants.COVER_PIC)) {
                                            currentUser
                                                    .setCoverImageKey(mediaKey);
                                        }

                                        RestfulClient.getInstance().updateUser(
                                                currentUser, uploadType);

                                        // Delete the old profile photos from the cache
                                        //
                                        // Profile photos are stored by contact ID, not
                                        // by image key, so old ones must be deleted.
                                        imageLoader
                                                .deleteProfilePictureFromCaches(currentUser);
                                    } catch (IOException e) {
                                        LogIt.e(MyProfileFragment.class, e,
                                                "Image upload error");
                                        fail(getString(R.string.network_error_title),
                                                getString(R.string.network_error));
                                    } catch (ResourceException e) {
                                        LogIt.e(MyProfileFragment.class, e,
                                                "Image upload error");
                                        fail(getString(R.string.network_error_title),
                                                getString(R.string.network_error));
                                    } catch (Exception e) {
                                        LogIt.e(MyProfileFragment.class, e,
                                                "Image upload error");
                                        fail(getString(R.string.generic_error_title),
                                                getString(R.string.unexpected_error));
                                    }
                                }

                                @Override
                                public void done() {
                                    dialog.dismiss();

                                    if (failed()) {
                                        currentUser
                                                .setCoverImageKey(originalCoverPic);
                                        currentUser
                                                .setProfileImageKey(originalProfilePic);

                                        UIUtil.alert(getActivity(),
                                                getExceptionTitle(),
                                                getExceptionMessage());
                                    } else {
                                        new DatabaseTask(mHandler) {

                                            @Override
                                            public void work() {
                                                currentUser.save();
                                            }

                                            @Override
                                            public void done() {
                                                if (uploadType
                                                        .equals(MessageMeConstants.PROFILE_PIC)) {
                                                    MMFirstWeekTracker
                                                            .getInstance()
                                                            .abacus(null,
                                                                    "prof_img",
                                                                    "user",
                                                                    null, null);
                                                    // Display the new profile photo
                                                    imageLoader
                                                            .displayProfilePicture(
                                                                    currentUser,
                                                                    contactImage,
                                                                    ProfilePhotoSize.LARGE);
                                                } else if (uploadType
                                                        .equals(MessageMeConstants.COVER_PIC)) {
                                                    MMFirstWeekTracker
                                                            .getInstance()
                                                            .abacus(null,
                                                                    "cov_img",
                                                                    "user",
                                                                    null, null);
                                                    imageLoader
                                                            .displayCoverPicture(
                                                                    currentUser
                                                                            .getCoverImageKey(),
                                                                    coverImage);
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

                            LogIt.w(MyProfileFragment.class,
                                    "Image upload failed", messageError);
                            dialog.dismiss();

                            UIUtil.alert(getActivity(), messageTitle,
                                    messageError);
                        }
                    });
        }
    }

    private class SettingsClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);

        }
    }

    public void openEditProfileDialog(Context context) {
        EditUserProfile editUserProfile = new EditUserProfile(profileName);
        editUserProfile.createEditProfileDialog(context);
    }

    private class MessagingServiceConnection implements ServiceConnection {

        /**
         * Register a BroadcastReceiver for the intent INTENT_NOTIFY_MESSAGE_LIST 
         * {@link MessagingService#notifyMessageList(long messageID)}
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogIt.d(MyProfileFragment.class, "Connected to Messaging Service");
            mMessagingServiceRef = ((MessagingService.MessagingBinder) service)
                    .getService();

            MessageMeApplication.appIsActive(getActivity(),
                    mMessagingServiceRef);

            if (!TextUtils.isEmpty(filePath) && !TextUtils.isEmpty(picType)) {
                LogIt.i(MyProfileFragment.class, "Resumed picture upload",
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