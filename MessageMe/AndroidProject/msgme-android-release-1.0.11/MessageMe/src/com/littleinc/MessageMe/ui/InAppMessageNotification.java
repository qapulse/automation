package com.littleinc.MessageMe.ui;

import static com.littleinc.MessageMe.MessageMeConstants.EXTRA_CONTACT_ID;
import static com.littleinc.MessageMe.MessageMeConstants.EXTRA_DESCRIPTION;
import static com.littleinc.MessageMe.MessageMeConstants.EXTRA_IMAGE_KEY;
import static com.littleinc.MessageMe.MessageMeConstants.EXTRA_TITLE;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeConstants.InAppNotificationTargetScreen;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;
import com.littleinc.MessageMe.util.AudioUtil;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.ImageLoader.ProfilePhotoSize;

public class InAppMessageNotification extends Fragment {

    ImageView profilePicture;

    ImageView mediaThumbnail;

    int notificationThumbSize;

    OnClickListener dismissListener;

    public static InAppMessageNotification newInstance(Bundle arguments) {
        InAppMessageNotification fragment = new InAppMessageNotification();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        DisplayMetrics metrics = new DisplayMetrics();
        notificationThumbSize = UIUtil.getPixeslFromDip(getResources()
                .getDimension(R.dimen.notification_thumbnail_size), metrics);
    }

    public void setDismissListener(OnClickListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Bundle arguments = getArguments();

        String title = arguments.getString(EXTRA_TITLE);
        String description = arguments.getString(EXTRA_DESCRIPTION);
        String imageKey = arguments.getString(EXTRA_IMAGE_KEY);
        long userId = arguments.getLong(EXTRA_CONTACT_ID, 0);

        LogIt.d(this, "Creating in-app notification", title, description);

        int layout = R.layout.internal_text_notification;

        final View root = inflater.inflate(layout, null);

        ImageButton dismiss = (ImageButton) root
                .findViewById(R.id.internal_notification_close);
        TextView titleView = (TextView) root
                .findViewById(R.id.notification_title);
        TextView descriptionView = (TextView) root
                .findViewById(R.id.notification_description);

        profilePicture = (ImageView) root
                .findViewById(R.id.notification_user_picture);

        root.setTag(userId);

        titleView.setText(EmojiUtils.convertToEmojisIfRequired(title,
                EmojiSize.SMALL));
        descriptionView.setText(EmojiUtils.convertToEmojisIfRequired(
                description, EmojiSize.SMALL));

        if (profilePicture != null) {
            ImageLoader.getInstance().displayProfilePicture(userId, imageKey,
                    profilePicture, ProfilePhotoSize.SMALL);
        }

        if (dismissListener == null) {
            dismissListener = new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    root.setVisibility(View.GONE);
                }
            };
        }

        boolean isVibrateAlertActive = MessageMeApplication.getPreferences()
                .isVibrateAlertActive();

        Vibrator v = (Vibrator) getActivity().getSystemService(
                Context.VIBRATOR_SERVICE);

        // Setup the alert notification status
        if (isVibrateAlertActive) {
            // We only want to vibrate in internal notifications
            // to match the iOS behavior
            v.vibrate(MessageMeConstants.VIBRATION_LONG_MILLIS);
        }

        dismiss.setOnClickListener(dismissListener);
        root.setOnClickListener(new OnNotificationClickListener());

        return root;
    }

    private class OnNotificationClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Bundle arguments = getArguments();

            final long contactId = arguments.getLong(EXTRA_CONTACT_ID, -1);

            final int screenToShowOnTouch = arguments.getInt(
                    MessageMeConstants.EXTRA_SCREEN_TO_SHOW,
                    InAppNotificationTargetScreen.MESSAGE_THREAD.ordinal());

            AudioUtil.stopPlaying();

            if (screenToShowOnTouch == InAppNotificationTargetScreen.CONTACT_PROFILE
                    .ordinal()) {
                LogIt.d(InAppMessageNotification.class,
                        "In-app notification pressed, show contact profile page",
                        contactId);
                Intent intent = new Intent(getActivity(),
                        ContactProfileActivity.class);
                intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY, contactId);
                startActivity(intent);
            } else if (screenToShowOnTouch == InAppNotificationTargetScreen.CONTACTS_TAB
                    .ordinal()) {
                LogIt.d(InAppMessageNotification.class,
                        "In-app notification pressed, show contacts tab");
                Intent intent = new Intent(getActivity(),
                        TabsFragmentActivity.class);
                intent.putExtra(TabsFragmentActivity.EXTRA_TAB_TO_SHOW,
                        TabsFragmentActivity.TAB_CONTACTS);
                startActivity(intent);
            } else if (screenToShowOnTouch == InAppNotificationTargetScreen.MESSAGE_THREAD
                    .ordinal()) {
                LogIt.d(this,
                        "In-app notification touched, show message thread",
                        contactId);

                new DatabaseTask() {

                    Contact contact;

                    @Override
                    public void work() {
                        contact = Contact.newInstance(contactId);

                        if (contact != null) {
                            contact.load();
                        }
                    }

                    @Override
                    public void done() {
                        if (contact != null) {
                            if (getActivity() != null) {
                                Intent intent = new Intent(getActivity(),
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
                                startActivity(intent);
                            } else {
                                LogIt.w(InAppMessageNotification.class,
                                        "Activity is now null, ignore item click");
                            }
                        }
                    }
                };
            } else {
                LogIt.e(InAppMessageNotification.class,
                        "Unexpected extra value for EXTRA_SCREEN_TO_SHOW",
                        screenToShowOnTouch);
            }
        }
    }
}