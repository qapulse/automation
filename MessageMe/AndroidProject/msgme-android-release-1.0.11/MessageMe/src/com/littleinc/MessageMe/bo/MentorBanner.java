package com.littleinc.MessageMe.bo;

import java.util.LinkedList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.User.UserBlockingListener;
import com.littleinc.MessageMe.bo.User.UserBlockingState;
import com.littleinc.MessageMe.bo.User.UserFriendshipListener;
import com.littleinc.MessageMe.bo.User.UserFriendshipState;
import com.littleinc.MessageMe.metrics.MMFirstSessionTracker;
import com.littleinc.MessageMe.ui.ContactProfileActivity;
import com.littleinc.MessageMe.ui.EmojiUtils;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;
import com.littleinc.MessageMe.util.MentorBannerListener;

public class MentorBanner {

    private TextView title;

    private TextView description;

    private Button actionBtn;

    private MentorBannerType type;

    private Contact contact;

    private View rootView;

    private Context context;

    private MentorBannerListener mentorListener;

    private ImageView mentorImage;

    private static LinkedList<MentorBanner> mentorList = new LinkedList<MentorBanner>();

    public MentorBanner(Context mContext, Contact mContact,
            MentorBannerType type, MentorBannerListener listener) {
        this.contact = mContact;
        this.type = type;
        this.context = mContext;
        this.mentorListener = listener;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = inflater.inflate(R.layout.mentor_banner_layout, null);

        initMentorBanner();
    }

    private void initMentorBanner() {
        title = (TextView) rootView.findViewById(R.id.mentor_banner_title);
        description = (TextView) rootView
                .findViewById(R.id.mentor_banner_description);
        actionBtn = (Button) rootView.findViewById(R.id.mentor_banner_btn);
        mentorImage = (ImageView) rootView
                .findViewById(R.id.mentor_banner_image);

        configureMentorBanner();

    }

    public enum MentorBannerType {
        NOT_FRIEND, BLOCKED, BLOCKED_BY;
    }

    private void configureMentorBanner() {

        if (!contact.isUser()) {
            LogIt.e(this, "Can't setup mentor banner, contact must be a User",
                    type);
        }

        User user = (User) contact;

        String titleText = "";
        String descText = "";

        switch (type) {
        case BLOCKED:
            titleText = String.format(
                    context.getString(R.string.mentor_banner_blocked_title),
                    user.getFirstName());

            descText = String.format(
                    context.getString(R.string.mentor_banner_blocked_desc),
                    user.getFirstName());

            actionBtn.setText(context.getString(R.string.unblock_label));
            actionBtn.setOnClickListener(new UnblockContactClickListener());

            mentorImage
                    .setBackgroundResource(R.drawable.mentorbanner_icon_blocked);
            break;
        case BLOCKED_BY:
            titleText = String.format(
                    context.getString(R.string.mentor_banner_blockedby_title),
                    user.getFirstName());

            descText = String.format(
                    context.getString(R.string.mentor_banner_blockedby_desc),
                    user.getFirstName());

            actionBtn.setVisibility(View.INVISIBLE);

            mentorImage
                    .setBackgroundResource(R.drawable.mentorbanner_icon_blocked);
            break;
        case NOT_FRIEND:
            titleText = String
                    .format(context
                            .getString(R.string.mentor_banner_not_contact_title),
                            user.getFirstName());

            descText = String.format(
                    context.getString(R.string.mentor_banner_not_contact_desc),
                    user.getFirstName());

            actionBtn
                    .setText(context.getString(R.string.mentor_banner_add_btn));
            actionBtn.setOnClickListener(new AddContactClickListener());

            mentorImage
                    .setBackgroundResource(R.drawable.mentorbanner_icon_addcontact);
            break;
        default:
            LogIt.e(this, "Unexpected condition");
            return;
        }

        title.setText(EmojiUtils.convertToEmojisIfRequired(titleText,
                EmojiSize.SMALL));

        description.setText(EmojiUtils.convertToEmojisIfRequired(descText,
                EmojiSize.SMALL));
    }

    public View getView() {
        return rootView;
    }

    private class ToggleFriendshipCallback implements UserFriendshipListener {

        String mDisplayName = "";

        public ToggleFriendshipCallback(String displayName) {
            mDisplayName = displayName;
        }

        @Override
        public void onUserFriendshipResponse(UserFriendshipState state) {

            switch (state) {
            case USER_FRIENDED:
                MMFirstSessionTracker.getInstance().abacus(null, "add",
                        "contact", null, null);

                UIUtil.alert(context, null, String.format(context
                        .getString(R.string.friends_invite_contact_added),
                        mDisplayName));
                mentorListener.onClickCompleted();

                return;
            case USER_UNFRIENDED:
                MMFirstSessionTracker.getInstance().abacus(null, "remove",
                        "contact", null, null);
                // XXX Unfriend implementation not required for now
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

    private class ToggleBlockingCallback implements UserBlockingListener {

        @Override
        public void onUserBlockingResponse(UserBlockingState state, User user) {

            switch (state) {
            case USER_BLOCKED:
                MMFirstSessionTracker.getInstance().abacus(null, "block",
                        "contact", null, null);
                // XXX Block implementation not required for now
                break;

            case USER_UNBLOCKED:
                MMFirstSessionTracker.getInstance().abacus(null, "unblock",
                        "contact", null, null);
                mentorListener.onClickCompleted();
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

    private class AddContactClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            LogIt.user(MentorBanner.class, "User clicked in add coontact");
            User newFriend = (User) contact;
            newFriend.toggleFriendship(context, new ToggleFriendshipCallback(
                    newFriend.getDisplayName()));
        }

    }

    private class UnblockContactClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            LogIt.d(MentorBanner.class, "User clicked in unblock contact");
            User user = (User) contact;
            user.toggleBlock(context, user.getContactId(), false,
                    new ToggleBlockingCallback());
        }

    }

    /**
     * Adds a new MentorBanner object to the queue
     */
    public synchronized static void addToQueue(MentorBanner mentorBanner) {
        mentorList.add(mentorBanner);
    }

    /**
     * Returns the first object of the stack
     * and removes it
     */
    public synchronized static View getNext() {
        return mentorList.poll().getView();
    }

    /**
     * Check if the stack is empty
     */
    public synchronized static boolean isHeadNull() {
        return mentorList.isEmpty();
    }

    /**
     * Cleans the stack
     */
    public synchronized static void cleanStack() {
        mentorList.clear();
    }

}
