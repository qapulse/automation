package com.littleinc.MessageMe.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.restlet.resource.ResourceException;

import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.Dimension;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeExternalAPI;
import com.littleinc.MessageMe.MessageMeFragmentActivity;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.AbstractMessage;
import com.littleinc.MessageMe.bo.ContactMessage;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.ConversationReader;
import com.littleinc.MessageMe.bo.DoodleMessage;
import com.littleinc.MessageMe.bo.DoodlePicMessage;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.LocationMessage;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.NoticeMessage;
import com.littleinc.MessageMe.bo.PhotoMessage;
import com.littleinc.MessageMe.bo.SongMessage;
import com.littleinc.MessageMe.bo.TextMessage;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.bo.VideoMessage;
import com.littleinc.MessageMe.bo.VoiceMessage;
import com.littleinc.MessageMe.bo.YoutubeMessage;
import com.littleinc.MessageMe.chat.Chat;
import com.littleinc.MessageMe.chat.CommandReceiver;
import com.littleinc.MessageMe.chat.CommandReceiverEnvelope;
import com.littleinc.MessageMe.chat.MediaManager;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Commands.PBCommandBatch;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope.CommandType;
import com.littleinc.MessageMe.protocol.Objects.PBUser;
import com.littleinc.MessageMe.ui.ChatActivity;
import com.littleinc.MessageMe.ui.ContactProfileActivity;
import com.littleinc.MessageMe.ui.DetailScreenActivity;
import com.littleinc.MessageMe.ui.GroupProfileActivity;
import com.littleinc.MessageMe.ui.MMAlertDialogFragment;
import com.littleinc.MessageMe.ui.MapDetailScreenActivity;
import com.littleinc.MessageMe.ui.MessageContactActivity;
import com.littleinc.MessageMe.ui.TabsFragmentActivity;
import com.littleinc.MessageMe.util.ImageLoader.ProfilePhotoSize;
import com.littleinc.MessageMe.widget.PlayButtonImageView;

public class ChatAdapter extends BaseAdapter {

    private Handler mHandler;

    /**
     * ListView rows can be reused. Use the message type as the row ID, but
     * multiply by two as the sent and received layouts are different.
     */
    private final static int sViewTypeCount = IMessageType.values().length * 2;

    /**
     * Parameter to be added into an ACTION_VIEW intent to display a youtube
     * video in the
     * native youtube app
     */
    private static final String NATIVE_YOUTUBE_DATA = "vnd.youtube:";

    /**
     * Parameter to be added into an ACTION_VIEW intent to display a youtube
     * video in the
     * browser
     */
    private static final String YOUTUBE_DEEPLINK = "http://www.youtube.com/watch?v=";

    private Chat chat;

    private FragmentActivity context;

    private ViewHolder holder;

    private LayoutInflater inflater;

    private ImageLoader mImageLoader;

    private ArrayList<IMessage> data = new ArrayList<IMessage>();

    /**
     * How many messages to load each time the user presses the
     * 'Load Earlier Messages' button.
     */
    public static final int LOAD_MORE_MESSAGES_BATCH_SIZE = 30;

    private boolean mDisplayLoadEarlierMsgsBtn = true;

    /**
     * Once we've run out of messages from the local database this
     * flag indicates the next load should be from the server
     */
    private boolean mLoadNextMsgsFromServer = false;

    private View mLoadEarlierMsgsView;

    private TextView mLoadEarlierMsgsBtn;

    /**
     * The list view that this Adapter is for. We need this so we can add and
     * remove the 'Load Earlier Messages' header.
     */
    private ListView mListView;

    private Conversation mConversation;

    /**
     * Cache commonly used bitmaps so we don't need to keep reloading them.
     */
    private static Bitmap sBubbleImageMsgSent;

    private static Bitmap sBubbleImageMsgReceived;

    private static Bitmap sBubbleLocationMsgSent;

    private static Bitmap sBubbleLocationMsgReceived;

    private static Bitmap sEmptyMediaImage;

    private static Bitmap sGenericContactImage;

    /**
     * XXX ideally we would not pass in the ListView that this Adapter is for,
     * but for now this is the quickest way to implement.
     */
    public ChatAdapter(FragmentActivity context, Chat chat,
            ListView messageListView, View loadEarlierMsgsView) {
        this.chat = chat;
        this.context = context;
        this.mImageLoader = ImageLoader.getInstance();
        this.mLoadEarlierMsgsBtn = (TextView) loadEarlierMsgsView
                .findViewById(R.id.show_earlier_messages);
        this.mLoadEarlierMsgsView = loadEarlierMsgsView;

        mListView = messageListView;
        mListView.addHeaderView(loadEarlierMsgsView);
        loadEarlierMsgsView.setVisibility(View.GONE);

        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        sBubbleImageMsgSent = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.messagesview_bubble_thumb_overlay_self);

        sBubbleImageMsgReceived = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.messagesview_bubble_thumb_overlay_other);

        sBubbleLocationMsgSent = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.messagesview_bubble_thumbtext_overlay_self);

        sBubbleLocationMsgReceived = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.messagesview_bubble_thumbtext_overlay_other);

        sEmptyMediaImage = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.empty_photo);

        sGenericContactImage = BitmapFactory.decodeResource(
                context.getResources(),
                R.drawable.profilepic_generic_user_small);

        mHandler = new Handler();
    }

    public int contains(IMessage message) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getClientId() == message.getClientId()) {
                return i;
            }
        }

        return -1;
    }

    public int contains(long messageId) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getId() == messageId) {
                return i;
            }
        }

        return -1;
    }

    public Chat getChat() {
        return chat;
    }

    public void remove(int index) {
        data.remove(index);
        notifyDataSetChanged();
    }

    public void initialize(List<IMessage> messages,
            boolean showLoadEarlierMessagesBtn, Conversation conversation) {

        User currentUser = MessageMeApplication.getCurrentUser();
        mConversation = conversation;

        deleteAllItems();

        // Reverse the order of the messages
        for (int i = messages.size() - 1; i >= 0; --i) {
            IMessage iMessage = messages.get(i);

            if (iMessage.getSenderId() == currentUser.getUserId()) {
                addSendMessageNoNotify(iMessage);
            } else {
                addReceivedMessageNoNotify(iMessage);
            }
        }

        LogIt.d(this, "Initialized chat thread with messages", data.size(),
                showLoadEarlierMessagesBtn);

        if (showLoadEarlierMessagesBtn) {
            mDisplayLoadEarlierMsgsBtn = true;
        } else {
            mDisplayLoadEarlierMsgsBtn = false;
        }

        updateLoadEarlierMsgsBtnVisibility();

        // If the Load Earlier Messages button is available, then it
        // will always try to load from the local database first
        mLoadNextMsgsFromServer = false;

        mLoadEarlierMsgsBtn
                .setOnClickListener(new LoadEarlierMessagesClickListener());

        notifyDataSetChanged();
    }

    public void addSendMessage(IMessage message) {
        addSendMessageNoNotify(message);
        notifyDataSetChanged();
    }

    /**
     * Add the provided message into the chat as a sent message but do NOT
     * notify the UI to update itself.
     * 
     * This method should be used when applying batched updates to the UI to
     * improve performance. notifyDataSetChanged() must be called after all
     * the updates are made.
     */
    public void addSendMessageNoNotify(IMessage message) {
        data.add(message);
    }

    public void addReceivedMessage(IMessage message) {
        addReceivedMessageNoNotify(message);
        notifyDataSetChanged();
    }

    /**
     * Add the provided message into the chat as a sent message but do NOT
     * notify the UI to update itself.
     * 
     * This method should be used when applying batched updates to the UI to
     * improve performance. notifyDataSetChanged() must be called after all
     * the updates are made.
     */
    public void addReceivedMessageNoNotify(IMessage message) {
        data.add(message);
    }

    public void updateMessage(IMessage message, int index) {
        if (index >= data.size()) {
            LogIt.e(this, "updateMessage called for out of bounds",
                    message.getType(), index, data.size());
        } else {
            data.remove(index);
            data.add(index, message);
            notifyDataSetChanged();
        }
    }

    public void deleteAllItems() {
        data.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        IMessage msg = getItem(position);
        return msg.getViewType();
    }

    @Override
    public int getViewTypeCount() {
        return sViewTypeCount;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public IMessage getItem(int position) {
        if (data == null) {
            LogIt.w(ChatAdapter.class, "IMessage list is null");
            return null;
        } else if (position < data.size()) {
            return data.get(position);
        } else {
            LogIt.w(ChatAdapter.class, "OutOfBounds",
                    position + "/" + data.size());
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        IMessage imessage = getItem(position);

        boolean wasSentByThisUser = imessage.wasSentByThisUser();

        if (convertView == null || convertView.getId() < 0) {
            holder = new ViewHolder();

            // Inflate an appropriate layout for this message
            if (wasSentByThisUser) {
                convertView = getSentMsgType(imessage, convertView);
            } else {
                convertView = getReceivedMsgType(imessage, convertView);
            }
        } else {
            // Reuse an already inflated layout
            holder = (ViewHolder) convertView.getTag();

            // Remove any image that was being displayed in the old view
            if (holder.titleTextView != null) {
                holder.titleTextView.setText("");
            }

            if (holder.bodyTextView != null) {
                holder.bodyTextView.setText("");
            }

            // Remove any image that was being displayed in the old view
            if (holder.thumbnailView != null) {
                // Ensure we don't display any image that could still be
                // loading right now
                holder.thumbnailView.setTag("");

                // Get the size of the bubble for this message (as image
                // messages can be different proportions)
                Dimension bubbleSize = AbstractMessage.setMessageBubbleSize(
                        imessage, holder.thumbnailView.getLayoutParams());

                try {
                    Bitmap bg9Patch = null;

                    if (imessage instanceof SongMessage) {
                        holder.thumbnailView.setImageBitmap(sEmptyMediaImage);
                    } else if (imessage instanceof LocationMessage) {
                        if (wasSentByThisUser) {
                            bg9Patch = ImageUtil.getResizedNinePatch(
                                    sBubbleLocationMsgSent,
                                    bubbleSize.getWidth(),
                                    bubbleSize.getHeight(), context);
                        } else {
                            bg9Patch = ImageUtil.getResizedNinePatch(
                                    sBubbleLocationMsgReceived,
                                    bubbleSize.getWidth(),
                                    bubbleSize.getHeight(), context);
                        }

                        holder.thumbnailView.setImageBitmap(bg9Patch);
                    } else if (imessage instanceof ContactMessage) {
                        holder.thumbnailView
                                .setImageBitmap(sGenericContactImage);
                    } else {
                        if (wasSentByThisUser) {
                            bg9Patch = ImageUtil.getResizedNinePatch(
                                    sBubbleImageMsgSent, bubbleSize.getWidth(),
                                    bubbleSize.getHeight(), context);
                        } else {
                            bg9Patch = ImageUtil.getResizedNinePatch(
                                    sBubbleImageMsgReceived,
                                    bubbleSize.getWidth(),
                                    bubbleSize.getHeight(), context);
                        }

                        holder.thumbnailView.setImageBitmap(bg9Patch);
                    }
                } catch (Exception e) {
                    LogIt.e(this, e,
                            "Error setting background message bubble image for reused ViewGroup");
                }
            }
        }

        holder = getMessageContent(imessage, position, wasSentByThisUser);

        convertView.setId(imessage.getViewType());

        return convertView;
    }

    /**
     * Checks if the current message needs to display the sender name
     * The name of the sender is only displayed once per block of received
     * messages.
     */
    private void displaySenderLabel(IMessage currentMessage, int position,
            boolean isSentMsg) {

        if ((currentMessage == null) || (currentMessage.getSender() == null)) {
            // This should never happen, but ensure we don't crash if it does
            LogIt.e(this,
                    "Ignore displaySenderLabel with null message or null sender");
            if (holder.senderTextView != null) {
                holder.senderTextView.setVisibility(View.GONE);
            }
            return;
        }

        // Only display the sender label for received messages
        if (!isSentMsg) {
            User currentUser = MessageMeApplication.getCurrentUser();

            if (currentUser == null) {
                LogIt.w(this,
                        "Ignore displaySenderLabel with null current user");
                return;
            }

            IMessage previousMessage = (position - 1) > -1 ? getItem(position - 1)
                    : null;

            if (currentMessage.getType() != IMessageType.NOTICE) {
                if ((currentMessage.getSenderId() != currentUser.getUserId())
                        && (previousMessage != null)
                        && (previousMessage.getSenderId() != currentMessage
                                .getSenderId())) {
                    holder.senderTextView.setVisibility(View.VISIBLE);
                    holder.senderTextView.setText(currentMessage.getSender()
                            .getStyledNameWithSmallEmojis());
                } else if (position == 0) {
                    holder.senderTextView.setVisibility(View.VISIBLE);
                    holder.senderTextView.setText(currentMessage.getSender()
                            .getStyledNameWithSmallEmojis());
                } else {
                    holder.senderTextView.setVisibility(View.GONE);
                }
            }
        }
    }

    private ViewHolder getMessageContent(IMessage imessage, int position,
            boolean isSentMsg) {
        int width;
        int height;

        boolean showUploadSpinner = false;

        if (holder.statusIndicator != null) {

            if (imessage.getCommandId() == 0) {
                // Message failed to send
                holder.statusIndicator.setVisibility(View.VISIBLE);
                holder.statusIndicator
                        .setImageResource(R.drawable.messagesview_messagestatus_icon_failed);
            } else if (imessage.getCommandId() == -1
                    && (imessage.getChannelId() != MessageMeConstants.WELCOME_ROOM_ID)) {
                // Message is still sending, check if we should show the
                // delayed indicator
                if (Message.isDelayed(imessage)
                        || !NetUtil.checkInternetConnection(this.context)) {
                    // The message has taken too long to be acknowledged, or
                    // there is no network connection
                    holder.statusIndicator.setVisibility(View.VISIBLE);
                    holder.statusIndicator
                            .setImageResource(R.drawable.messagesview_messagestatus_icon_delayed);
                } else {
                    // The message is still sending, and it has been less than
                    // the 'delayed status'. Messages that involve uploading
                    // media should display a spinner now.
                    if ((imessage.getType() == IMessageType.PHOTO)
                            || (imessage.getType() == IMessageType.DOODLE)
                            || (imessage.getType() == IMessageType.DOODLE_PIC)
                            || (imessage.getType() == IMessageType.VIDEO)
                            || (imessage.getType() == IMessageType.VOICE)) {
                        showUploadSpinner = true;
                    }
                }
            } else {
                // Message was delivered to the server, so don't show a status
                holder.statusIndicator.setVisibility(View.GONE);
            }
        }

        if (holder.statusProgressBar != null) {
            if (showUploadSpinner) {
                holder.statusProgressBar.setVisibility(View.VISIBLE);

                // When the spinner is displayed then no status indicator
                // should be shown
                holder.statusIndicator.setVisibility(View.GONE);
            } else {
                holder.statusProgressBar.setVisibility(View.GONE);
            }
        }

        switch (imessage.getType()) {
            case TEXT:
                TextMessage textMessage = (TextMessage) imessage;
                holder.bodyTextView.setText(textMessage.getEmojiText());

                Linkify.addLinks(holder.bodyTextView, Linkify.PHONE_NUMBERS
                        | Linkify.WEB_URLS | Linkify.MAP_ADDRESSES
                        | Linkify.EMAIL_ADDRESSES);

                holder.bodyTextView
                        .setOnLongClickListener(new TextLongClickListener(
                                textMessage));

                break;
            case DOODLE:
                DoodleMessage doodleMessage = (DoodleMessage) imessage;

                mImageLoader.displayMessageBubble(imessage,
                        doodleMessage.getThumbKey(), holder.thumbnailView,
                        isSentMsg);

                holder.thumbnailView
                        .setOnClickListener(new DoodleMessageClickListener(
                                doodleMessage));

                holder.thumbnailView
                        .setOnLongClickListener(new ForwardLongClickListener(
                                doodleMessage));
                break;
            case DOODLE_PIC:

                DoodlePicMessage doodlePicMessage = (DoodlePicMessage) imessage;

                mImageLoader.displayMessageBubble(imessage,
                        doodlePicMessage.getThumbKey(), holder.thumbnailView,
                        isSentMsg);

                holder.thumbnailView
                        .setOnClickListener(new DoodleMessageClickListener(
                                doodlePicMessage));

                holder.thumbnailView
                        .setOnLongClickListener(new ForwardLongClickListener(
                                doodlePicMessage));
                break;
            case LOCATION:
                LocationMessage locationMessage = (LocationMessage) imessage;
                height = context.getResources().getDimensionPixelSize(
                        R.dimen.map_content_height);
                width = context.getResources().getDimensionPixelSize(
                        R.dimen.map_content_width);

                String mapUrl = MapBoxStaticMapUtil.getMapUrl(
                        locationMessage.getLatitude(),
                        locationMessage.getLongitude(), height, width);

                mImageLoader.displayMessageBubble(imessage, mapUrl,
                        holder.thumbnailView, isSentMsg);

                if (!StringUtil.isEmpty(locationMessage.getAddress())) {
                    holder.bodyTextView.setText(locationMessage.getAddress());
                    holder.bodyTextView.setVisibility(View.VISIBLE);
                } else {
                    holder.bodyTextView.setVisibility(View.GONE);
                }

                holder.bubbleView
                        .setOnClickListener(new LocationDetailClickListener(
                                locationMessage));
                holder.titleTextView.setText(locationMessage.getName());

                holder.bubbleView
                        .setOnLongClickListener(new ForwardLongClickListener(
                                locationMessage));

                break;
            case PHOTO:
                PhotoMessage photoMessage = (PhotoMessage) imessage;

                mImageLoader.displayMessageBubble(imessage,
                        photoMessage.getThumbKey(), holder.thumbnailView,
                        isSentMsg);

                holder.thumbnailView
                        .setOnClickListener(new PhotoMessageClickListener(
                                photoMessage));

                holder.thumbnailView
                        .setOnLongClickListener(new ForwardLongClickListener(
                                photoMessage));
                break;
            case SONG:
                SongMessage songMessage = (SongMessage) imessage;

                holder.titleTextView.setText(songMessage.getTrackName());
                holder.bodyTextView.setText(songMessage.getArtistName());

                mImageLoader.displayImage(songMessage.getArtworkUrl(),
                        holder.thumbnailView);

                // need to set holder.commandId before setOnClickListener() and
                // isCurrentlyPlaying()
                holder.commandId = songMessage.getCommandId();
                holder.mediaPlayBtn
                        .setOnClickListener(new MediaPlaybackClickListener(
                                context, this, holder.commandId, songMessage
                                        .getTrackName(), songMessage
                                        .getPreviewUrl(),
                                holder.audioProgressBar));

                AudioUtil.setButtonState(holder.commandId, null,
                        holder.mediaPlayBtn, holder.audioProgressBar);

                holder.bubbleView
                        .setOnLongClickListener(new ForwardLongClickListener(
                                songMessage));
                break;
            case VIDEO:
                VideoMessage videoMessage = (VideoMessage) imessage;

                mImageLoader.displayMessageBubble(imessage,
                        videoMessage.getThumbKey(), holder.thumbnailView,
                        isSentMsg);

                holder.thumbnailView
                        .setOnClickListener(new VideoMessageClickListener(
                                videoMessage));

                holder.thumbnailView
                        .setOnLongClickListener(new ForwardLongClickListener(
                                videoMessage));
                break;
            case VOICE:
                VoiceMessage voiceMessage = (VoiceMessage) imessage;

                holder.commandId = voiceMessage.getCommandId();
                holder.durationTextView.setText(convertToTime(voiceMessage
                        .getSeconds()));
                holder.voicePlayerImageView
                        .setOnClickListener(new VoiceMessageClickListener(
                                holder.commandId, voiceMessage,
                                holder.audioProgressBar));
                AudioUtil.setButtonState(holder.commandId, null,
                        holder.voicePlayerImageView, holder.audioProgressBar);

                holder.bubbleView
                        .setOnLongClickListener(new ForwardLongClickListener(
                                voiceMessage));
                break;
            case YOUTUBE:
                YoutubeMessage youtubeMessage = (YoutubeMessage) imessage;

                mImageLoader.displayMessageBubble(imessage,
                        youtubeMessage.getThumbKey(), holder.thumbnailView,
                        isSentMsg);

                holder.thumbnailView
                        .setOnClickListener(new YouTubeMessageClickListener(
                                youtubeMessage));

                holder.thumbnailView
                        .setOnLongClickListener(new ForwardLongClickListener(
                                youtubeMessage));
                break;
            case NOTICE:
                NoticeMessage noticeMessage = (NoticeMessage) imessage;

                holder.bodyTextView.setText(noticeMessage
                        .getNotificationMessageWithEmojis());

                switch (noticeMessage.getNoticeType()) {
                    case ROOM_JOIN:
                        holder.bodyTextView
                                .setOnClickListener(new RoomNoticeToUserProfile(
                                        noticeMessage));
                        break;
                    case ROOM_LEAVE:
                        holder.bodyTextView
                                .setOnClickListener(new RoomNoticeToUserProfile(
                                        noticeMessage));
                        break;
                    case ROOM_NEW:
                        holder.bodyTextView
                                .setOnClickListener(new RoomNoticeToGroupProfile(
                                        noticeMessage));
                        break;
                    case ROOM_UPDATE:
                        holder.bodyTextView
                                .setOnClickListener(new RoomNoticeToGroupProfile(
                                        noticeMessage));
                        break;
                    default:
                        break;
                }

                break;
            case CONTACT:
                ContactMessage contactMessage = (ContactMessage) imessage;

                PBUser forwardedContact = contactMessage.getPBUser();

                mImageLoader.displayProfilePicture(
                        forwardedContact.getUserID(),
                        forwardedContact.getProfileImageKey(),
                        holder.thumbnailView, ProfilePhotoSize.SMALL);

                holder.bodyTextView.setText(contactMessage.getDisplayName());

                holder.bubbleView
                        .setOnClickListener(new ContactMessageClickListener(
                                contactMessage));

                holder.bubbleView
                        .setOnLongClickListener(new ForwardLongClickListener(
                                contactMessage));
                break;
            case UNSUPPORTED:
                holder.bodyTextView.setText(context
                        .getString(R.string.unsupported_message_text));

                holder.bodyTextView
                        .setOnClickListener(new UnsupportedMessageClickListener());
                break;
        }

        if (mConversation != null
                && mConversation.getLastSentMessage() != null
                && imessage.getId() == mConversation.getLastSentMessage()
                        .getId() && holder.readersLabel != null
                && mConversation.getReadUsers() != null
                && mConversation.getReadUsers().size() > 0) {

            String s = "";
            int count = 0;
            String and = context.getString(R.string.chat_and);

            for (ConversationReader reader : mConversation.getReadUsers()) {

                String readerName = "";

                if (reader.getUser() == null) {
                    LogIt.w(this, "reader.getUser() is null", reader);
                } else {
                    readerName = reader.getUser().getFirstName();
                }

                if (count == 0) {
                    s = "<b>" + readerName + "</b> ";
                } else if (count == 1) {
                    s += and + " <b>" + readerName + "</b> ";
                } else {
                    s = s.replace(" " + and + " ", ", ");
                    break;
                }
                count++;
            }

            if (mConversation.getReadUsers().size() > 2) {
                String others = context.getString(R.string.chat_others);

                s += " " + and + " <b>"
                        + (mConversation.getReadUsers().size() - 2) + " "
                        + others + "</b> ";
            }

            s += getTimeLabel(mConversation.getReadAt());
            String readersLabel = String.format(
                    context.getString(R.string.chat_seen_by), s);

            holder.readersLabel.setVisibility(View.VISIBLE);
            holder.readersLabel.setText(Html.fromHtml(readersLabel));
        } else if (holder.readersLabel != null) {
            holder.readersLabel.setVisibility(View.GONE);
        }

        // This method was moved here, because the received message that
        // displays
        // the date divider, needs to display the sender name
        displaySenderLabel(imessage, position, imessage.wasSentByThisUser());

        IMessage previousMessage = (position - 1) > -1 ? getItem(position - 1)
                : null;

        if (previousMessage != null) {

            int timeStamp = previousMessage.getCreatedAt();

            if ((imessage.getCreatedAt() - timeStamp) > MessageMeConstants.DATE_DIVIDER_INTERVAL) {

                String time = getDateDividerLabel(imessage.getCreatedAt());
                holder.dateDividerHolder.setVisibility(View.VISIBLE);
                holder.dateDivider.setText(time);
                if (!isSentMsg && (imessage.getType() != IMessageType.NOTICE)) {
                    if (imessage.getSender() != null) {
                        // Displays the sender name of the first received
                        // message
                        // that displays the date divider
                        holder.senderTextView.setVisibility(View.VISIBLE);
                        holder.senderTextView.setText(imessage.getSender()
                                .getStyledNameWithSmallEmojis());
                    } else {
                        LogIt.w(this, "Don't display null sender name",
                                imessage, imessage.getType());
                    }
                }
            } else {
                holder.dateDividerHolder.setVisibility(View.GONE);
            }
        } else {
            // The were no previous messages in the chat (it's the first one)
            // Needs to display the date divider
            String time = getDateDividerLabel(imessage.getCreatedAt());
            holder.dateDividerHolder.setVisibility(View.VISIBLE);
            holder.dateDivider.setText(time);
        }

        return holder;
    }

    /**
     * Generate the formated date for the date dividers
     */
    private String getDateDividerLabel(int timeStamp) {
        String dateLabel = "";

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DateUtil.convertToDate(timeStamp));

        dateLabel = context.getString(R.string.date_divier_text);

        String day = DateFormat.format("MMM dd, yyyy", calendar).toString();

        String hour = DateFormat.format("hh:mm aa", calendar).toString();

        return String.format(dateLabel, day, hour);
    }

    private String getTimeLabel(int timestamp) {
        String s = "";
        Calendar calendar = Calendar.getInstance();

        Calendar readDate = Calendar.getInstance();
        readDate.setTime(DateUtil.convertToDate(timestamp));

        if (calendar.get(Calendar.DAY_OF_YEAR) == readDate
                .get(Calendar.DAY_OF_YEAR)) {
            s = context.getString(R.string.chat_at) + " ";
            s += DateFormat.format("hh:mm aa", readDate).toString();
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, -1);

            if (calendar.get(Calendar.DAY_OF_YEAR) == readDate
                    .get(Calendar.DAY_OF_YEAR)) {
                s = context.getString(R.string.chat_yesterday);
            } else {
                calendar = Calendar.getInstance();
                long diff = calendar.getTimeInMillis()
                        - readDate.getTimeInMillis();
                long days = diff / (24 * 60 * 60 * 1000);

                if (days <= 7) {
                    s = context.getString(R.string.chat_on) + " ";
                    s += new SimpleDateFormat("EEEE")
                            .format(readDate.getTime());
                } else {
                    s = context.getString(R.string.chat_on) + " ";
                    s += new SimpleDateFormat("MM/dd/yyyy").format(readDate
                            .getTime());
                }
            }
        }

        return s;
    }

    private View getSentMsgType(IMessage imessage, View convertView) {

        switch (imessage.getType()) {
            case TEXT:
                convertView = inflater.inflate(
                        R.layout.sent_text_message_content, null);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.text_message_body);
                break;
            case DOODLE:
                convertView = inflater.inflate(
                        R.layout.sent_image_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.image_message_body);
                break;
            case DOODLE_PIC:
                convertView = inflater.inflate(
                        R.layout.sent_image_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.image_message_body);
                break;
            case LOCATION:
                convertView = inflater.inflate(
                        R.layout.sent_location_message_content, null);
                holder.bubbleView = (LinearLayout) convertView
                        .findViewById(R.id.bubble);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.map_message_body);
                holder.titleTextView = (TextView) convertView
                        .findViewById(R.id.map_message_title);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.map_message_description);
                break;
            case PHOTO:
                convertView = inflater.inflate(
                        R.layout.sent_image_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.image_message_body);
                break;
            case SONG:
                convertView = inflater.inflate(
                        R.layout.sent_song_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.media_thumbnail);
                holder.titleTextView = (TextView) convertView
                        .findViewById(R.id.media_title);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.media_author);
                holder.mediaPlayBtn = (PlayButtonImageView) convertView
                        .findViewById(R.id.play_button);
                holder.audioProgressBar = (ProgressBar) convertView
                        .findViewById(R.id.progress_spinner);
                holder.bubbleView = (LinearLayout) convertView
                        .findViewById(R.id.media_message_wrapper);
                break;
            case VIDEO:
                convertView = inflater.inflate(
                        R.layout.sent_video_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.video_message_body);
                break;
            case VOICE:
                convertView = inflater.inflate(
                        R.layout.sent_audio_message_content, null);
                holder.voicePlayerImageView = (PlayButtonImageView) convertView
                        .findViewById(R.id.voice_audio_play_btn);
                holder.durationTextView = (TextView) convertView
                        .findViewById(R.id.audio_message_timer);
                holder.audioProgressBar = (ProgressBar) convertView
                        .findViewById(R.id.progress_spinner);
                holder.bubbleView = (LinearLayout) convertView
                        .findViewById(R.id.voice_message_wrapper);
                break;
            case YOUTUBE:
                convertView = inflater.inflate(
                        R.layout.sent_youtube_video_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.video_thumbnail);
                break;
            case NOTICE:
                convertView = inflater.inflate(R.layout.room_notices_layout,
                        null);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.notice_message_body);
                break;
            case CONTACT:
                convertView = inflater.inflate(
                        R.layout.sent_contact_message_content, null);
                holder.bubbleView = (LinearLayout) convertView
                        .findViewById(R.id.bubble);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.contact_name);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.contact_image);
                break;
            case UNSUPPORTED:
                convertView = inflater.inflate(
                        R.layout.sent_unsupported_message_content, null);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.text_message_body);
                break;
            default:
                LogIt.e(this, "Unexpected message type", imessage.getType());
                break;
        }

        holder.statusIndicator = (ImageButton) convertView
                .findViewById(R.id.status_indicator);
        holder.statusProgressBar = (ProgressBar) convertView
                .findViewById(R.id.status_indicator_spinner);
        holder.readersLabel = (TextView) convertView
                .findViewById(R.id.readers_label);

        holder.dateDivider = (TextView) convertView
                .findViewById(R.id.date_divider_label);

        holder.dateDividerHolder = (RelativeLayout) convertView
                .findViewById(R.id.date_divider);

        convertView.setTag(holder);

        return convertView;
    }

    private View getReceivedMsgType(IMessage imessage, View convertView) {

        switch (imessage.getType()) {
            case TEXT:
                convertView = inflater.inflate(
                        R.layout.received_text_message_content, null);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.text_message_body);
                break;
            case DOODLE:
                convertView = inflater.inflate(
                        R.layout.received_image_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.image_message_body);
                break;
            case DOODLE_PIC:
                convertView = inflater.inflate(
                        R.layout.received_image_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.image_message_body);
                break;
            case LOCATION:
                convertView = inflater.inflate(
                        R.layout.received_location_message, null);
                holder.bubbleView = (LinearLayout) convertView
                        .findViewById(R.id.bubble);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.map_message_body);
                holder.titleTextView = (TextView) convertView
                        .findViewById(R.id.map_message_title);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.map_message_description);
                break;
            case PHOTO:
                convertView = inflater.inflate(
                        R.layout.received_image_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.image_message_body);
                break;
            case SONG:
                convertView = inflater.inflate(
                        R.layout.received_song_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.media_thumbnail);
                holder.titleTextView = (TextView) convertView
                        .findViewById(R.id.media_title);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.media_author);
                holder.mediaPlayBtn = (PlayButtonImageView) convertView
                        .findViewById(R.id.play_button);
                holder.audioProgressBar = (ProgressBar) convertView
                        .findViewById(R.id.progress_spinner);
                holder.bubbleView = (LinearLayout) convertView
                        .findViewById(R.id.media_message_wrapper);
                break;
            case VIDEO:
                convertView = inflater.inflate(
                        R.layout.received_video_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.video_message_body);
                break;
            case VOICE:
                convertView = inflater.inflate(
                        R.layout.received_audio_message_content, null);
                holder.voicePlayerImageView = (PlayButtonImageView) convertView
                        .findViewById(R.id.voice_audio_play_btn);
                holder.durationTextView = (TextView) convertView
                        .findViewById(R.id.audio_message_timer);
                holder.audioProgressBar = (ProgressBar) convertView
                        .findViewById(R.id.progress_spinner);
                holder.bubbleView = (LinearLayout) convertView
                        .findViewById(R.id.voice_message_wrapper);
                break;
            case YOUTUBE:
                convertView = inflater.inflate(
                        R.layout.received_youtube_video_message_content, null);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.video_thumbnail);
                break;
            case NOTICE:
                convertView = inflater.inflate(R.layout.room_notices_layout,
                        null);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.notice_message_body);
                break;
            case CONTACT:
                convertView = inflater.inflate(
                        R.layout.received_contact_message_content, null);
                holder.bubbleView = (LinearLayout) convertView
                        .findViewById(R.id.bubble);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.contact_name);
                holder.thumbnailView = (ImageView) convertView
                        .findViewById(R.id.contact_image);
                break;
            case UNSUPPORTED:
                convertView = inflater.inflate(
                        R.layout.received_unsupported_message_content, null);
                holder.bodyTextView = (TextView) convertView
                        .findViewById(R.id.text_message_body);
                break;
            default:
                LogIt.e(this, "Unexpected message type", imessage.getType());
                break;
        }

        holder.readersLabel = null;
        holder.statusIndicator = null;
        holder.statusProgressBar = null;

        // Every received message has a sender
        holder.senderTextView = (TextView) convertView
                .findViewById(R.id.message_sender);

        holder.dateDivider = (TextView) convertView
                .findViewById(R.id.date_divider_label);

        holder.dateDividerHolder = (RelativeLayout) convertView
                .findViewById(R.id.date_divider);

        convertView.setTag(holder);

        return convertView;
    }

    private void updateLoadEarlierMsgsBtnVisibility() {
        if (mDisplayLoadEarlierMsgsBtn) {
            mLoadEarlierMsgsView.setVisibility(View.VISIBLE);

            if (mListView.getHeaderViewsCount() >= 1) {
                LogIt.d(this, "Load Earlier Messages view already displayed");
            } else {
                // An Exception is thrown if we call addHeaderView here as
                // setAdapter has already been called on the ListView.
                //
                // That only happens each time a new ChatAdapter is
                // instantiated, so we can safely ignore this as it means the
                // chat is being reloaded.
                LogIt.w(this,
                        "Ignore attempt to show Load Earlier Messages button as ChatAdapter is being recreated",
                        mListView.getHeaderViewsCount());
            }
        } else {
            // Once the header view is removed we won't need to display it
            // again as all the user's messages have been loaded
            LogIt.d(this, "Remove Earlier Messages button");
            mListView.removeHeaderView(mLoadEarlierMsgsView);
        }
    }

    public Context getContext() {
        return context;
    }

    public static class ViewHolder {

        public TextView dateDivider;

        public RelativeLayout dateDividerHolder;

        /**
         * The sender name, which is only shown in certain conditions.
         */
        public TextView senderTextView;

        /**
         * Main layout holding the message bubble.
         */
        public LinearLayout bubbleView;

        public ImageView thumbnailView;

        /**
         * Holds the title associated with a message, e.g. song title or
         * location title.
         */
        public TextView titleTextView;

        /**
         * Holds the main text associated with a message, e.g. the text
         * message content, location detail, artist name, etc.
         */
        public TextView bodyTextView;

        public PlayButtonImageView voicePlayerImageView;

        /**
         * Only thumbnails for voice messages display the media duration.
         * Video and Youtube messages do not display the duration.
         */
        public TextView durationTextView;

        public PlayButtonImageView mediaPlayBtn;

        public ProgressBar audioProgressBar;

        public ImageButton statusIndicator;

        public ProgressBar statusProgressBar;

        public TextView readersLabel;

        public long commandId;
    }

    private class VoiceMessageClickListener implements OnClickListener {

        private VoiceMessage message;

        private ProgressBar mProgressBar;

        private long mCommandId;

        public VoiceMessageClickListener(long commandId,
                VoiceMessage voiceMessage, ProgressBar progressBar) {
            mCommandId = commandId;
            message = voiceMessage;
            mProgressBar = progressBar;
        }

        @Override
        public void onClick(View v) {

            LogIt.user(this, "Play/pause pressed for audio message",
                    message.getSoundKey());

            File file = ImageUtil.getFile(message.getSoundKey());

            String audioPath = file.getAbsolutePath();

            if (file.exists()) {
                // We don't need a network connection to play a cached
                // message (the media for some sent messages might be
                // cached).
                LogIt.d(this, "Play voice message from file", audioPath);

                AudioUtil.startPlaying(ChatAdapter.this, mCommandId, null,
                        file, mProgressBar);
            } else {
                // The Android media player does not cope nicely with
                // being offline, so we display an alert if there is
                // no connection.
                if (NetUtil.checkInternetConnection(context)) {
                    audioPath = MediaManager
                            .getS3FileURL(message.getSoundKey());
                    LogIt.d(this, "Stream voice message from URL", audioPath);

                    AudioUtil.startPlaying(ChatAdapter.this, mCommandId,
                            audioPath, null, mProgressBar);
                } else {
                    UIUtil.alert(getContext(),
                            getContext()
                                    .getString(R.string.network_error_title),
                            getContext().getString(R.string.network_error));
                }
            }
        }
    }

    private class ForwardLongClickListener implements OnLongClickListener {

        private IMessage message;

        public ForwardLongClickListener(IMessage message) {
            this.message = message;
        }

        @Override
        public boolean onLongClick(View view) {

            LogIt.user(this, "Long pressed message");

            Vibrator vibrator = (Vibrator) context
                    .getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(MessageMeConstants.VIBRATION_SHORT_MILLIS);

            MessageOptionsDialog dialogFragment = MessageOptionsDialog
                    .newInstance(ChatAdapter.this, message);
            dialogFragment.show(context.getSupportFragmentManager(),
                    MessageMeFragmentActivity.DEFAULT_DIALOG_TAG, true);

            return true;
        }
    }

    public static class MessageOptionsDialog extends MMAlertDialogFragment {

        protected IMessage mMessage;

        protected ChatAdapter mAdapter;

        public static MessageOptionsDialog newInstance(ChatAdapter adapter,
                IMessage message) {
            MessageOptionsDialog dialogFragment = new MessageOptionsDialog();

            dialogFragment.mMessage = message;
            dialogFragment.mAdapter = adapter;

            return dialogFragment;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAvailableToRestore() {
            return mMessage != null && mAdapter != null;
        }

        @Override
        protected Builder getAlertDialogBuilder() {

            Builder builder = super.getAlertDialogBuilder();

            builder.setPositiveButton(null, null);
            builder.setTitle(R.string.select_action);
            builder.setItems(R.array.generic_message_options,
                    mItemClickListener);

            return builder;
        }

        private DialogInterface.OnClickListener mItemClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        LogIt.user(this, "Pressed Forward Message");
                        mAdapter.forwardMessage(mMessage);
                        break;
                }
            }
        };
    }

    public static class TextMessageOptionsDialog extends MessageOptionsDialog {

        private View mMessageView;

        private TextMessage mMessage;

        public static TextMessageOptionsDialog newInstance(ChatAdapter adapter,
                View messageView, TextMessage message) {
            TextMessageOptionsDialog dialogFragment = new TextMessageOptionsDialog();

            dialogFragment.mMessage = message;
            dialogFragment.mAdapter = adapter;
            dialogFragment.mMessageView = messageView;

            return dialogFragment;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAvailableToRestore() {
            return super.isAvailableToRestore() && mMessageView != null
                    && mMessage != null;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            if (mMessageView != null) {
                mMessageView.setSelected(false);
            }
        }

        @Override
        protected Builder getAlertDialogBuilder() {

            Builder builder = super.getAlertDialogBuilder();

            builder.setPositiveButton(null, null);
            builder.setTitle(R.string.select_action);
            builder.setItems(R.array.text_message_options, mClickListener);

            return builder;
        }

        private DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        LogIt.user(this, "User pressed Copy Message");
                        mAdapter.copyMessage(mMessage.getText());
                        break;
                    case 1:
                        LogIt.user(this, "User pressed Delete Message");
                        mAdapter.deleteTextMessage(mMessage);
                        break;
                    case 2:
                        LogIt.user(this, "User pressed Forward Message");
                        mAdapter.forwardMessage(mMessage);
                        break;
                }
            }
        };
    }

    /**
     * LongClickListener for text messages. When the message
     * is pressed, changes it's background.
     * 
     * This method MUST return FALSE to inform the textview that
     * more listeners can be registered
     * 
     */
    private class TextLongClickListener implements OnLongClickListener {

        private TextMessage message;

        public TextLongClickListener(TextMessage message) {
            this.message = message;
        }

        @Override
        public boolean onLongClick(View view) {

            LogIt.user(this, "User long clicked text message");

            Vibrator vibrator = (Vibrator) context
                    .getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(MessageMeConstants.VIBRATION_SHORT_MILLIS);

            view.setSelected(true);

            TextMessageOptionsDialog dialogFragment = TextMessageOptionsDialog
                    .newInstance(ChatAdapter.this, view, message);
            dialogFragment.show(context.getSupportFragmentManager(),
                    MessageMeFragmentActivity.DEFAULT_DIALOG_TAG, true);

            return false;
        }
    }

    private void forwardMessage(IMessage message) {
        Intent intent = new Intent(context, MessageContactActivity.class);

        intent.putExtra(Message.ID_COLUMN, message.getId());
        intent.putExtra(Message.TYPE_COLUMN, message.getType()
                .getProtobufNumber());

        context.startActivity(intent);
        context.finish();
    }

    /**
     * Checks if a message exist in the list
     * and deletes it from the local DB
     */
    private void deleteTextMessage(TextMessage message) {
        int position = contains(message);

        if (position != -1) {
            // Removes the item from the adapter
            remove(position);

            // Removes the message from the local DB
            message.delete();

            LogIt.d(this, "TextMessage deleted");
        } else {
            LogIt.d(this, "Could not find text message for deletion");
        }
    }

    /**
     * Copies the content of a text message
     * to the clipboard
     */
    @SuppressWarnings("deprecation")
    @TargetApi(11)
    private void copyMessage(String messageContent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipboardManager clipManager = (ClipboardManager) context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", messageContent);
            if (clip != null) {
                clipManager.setPrimaryClip(clip);
            } else {
                LogIt.w(this, "Can't copy to clipboard, clip is null");
            }
        } else {
            android.text.ClipboardManager clipManager = (android.text.ClipboardManager) context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipManager.setText(messageContent);
        }

        LogIt.d(this, "TextMessage copied to clipboard");

        Toast.makeText(context, context.getString(R.string.clipboard_copy),
                Toast.LENGTH_SHORT).show();

    }

    private class VideoMessageClickListener implements OnClickListener {

        private VideoMessage message;

        public VideoMessageClickListener(VideoMessage videoMessage) {
            this.message = videoMessage;
        }

        @Override
        public void onClick(View v) {
            UIUtil.hideKeyboard(v);

            LogIt.user(this, "Play/pause pressed for video message",
                    message.getVideoKey());

            // Video files are currently stored wherever the device decides,
            // not in the application internal storage cache
            File file = new File(message.getVideoKey());

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);

            boolean playVideo = false;

            if (file.exists()) {
                // We don't need a network connection to play a cached
                // message (the media for some sent messages might be
                // cached).
                LogIt.d(this, "Play video message from file", file);
                intent.setDataAndType(Uri.fromFile(file), "video/*");
                playVideo = true;
            } else {
                if (NetUtil.checkInternetConnection(context)) {
                    String url = MediaManager.getS3FileURL(message
                            .getVideoKey());
                    LogIt.d(this, "Stream video message from URL", url);
                    intent.setDataAndType(Uri.parse(url), "video/*");
                    playVideo = true;
                } else {
                    // The Android media player does not cope nicely with
                    // being offline, so we display an alert if there is
                    // no connection.
                    UIUtil.alert(getContext(),
                            getContext()
                                    .getString(R.string.network_error_title),
                            getContext().getString(R.string.network_error));
                }
            }

            if (playVideo) {
                List<ResolveInfo> matchedApps = context.getPackageManager()
                        .queryIntentActivities(intent,
                                PackageManager.MATCH_DEFAULT_ONLY);

                // No app installed that can play this video URL
                if (matchedApps.size() == 0) {
                    LogIt.w(ChatAdapter.class,
                            "No app available that can playback our video stream");
                    UIUtil.alert(
                            context,
                            context.getString(R.string.no_video_playback_app_title),
                            context.getString(R.string.no_video_playback_app_message));
                } else {
                    AudioUtil.pausePlaying(true);
                    context.startActivity(intent);
                }
            }
        }
    }

    private class PhotoMessageClickListener implements OnClickListener {

        private PhotoMessage message;

        public PhotoMessageClickListener(PhotoMessage photoMessage) {
            this.message = photoMessage;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(context, DetailScreenActivity.class);

            intent.putExtra(Message.ID_COLUMN, message.getId());
            intent.putExtra(Message.TYPE_COLUMN, message.getType()
                    .getProtobufNumber());

            context.startActivityForResult(intent,
                    MessageMeConstants.DETAIL_SCREEN_REQUEST_CODE);
        }
    }

    private class LocationDetailClickListener implements OnClickListener {
        LocationMessage message;

        public LocationDetailClickListener(LocationMessage message) {
            this.message = message;
        }

        @Override
        public void onClick(View v) {
            LogIt.user(ChatAdapter.class, "Pressed location message",
                    message.getName());

            try {
                Intent intent = new Intent(context,
                        MapDetailScreenActivity.class);

                intent.putExtra(Message.ID_COLUMN, message.getId());
                intent.putExtra(Message.TYPE_COLUMN, message.getType()
                        .getProtobufNumber());
                intent.putExtra(MessageMeConstants.EXTRA_LATITUDE,
                        message.getLatitude());
                intent.putExtra(MessageMeConstants.EXTRA_LONGITUDE,
                        message.getLongitude());

                context.startActivityForResult(intent,
                        MessageMeConstants.DETAIL_SCREEN_REQUEST_CODE);
            } catch (NoClassDefFoundError e) {

                LogIt.w(this,
                        "User doesn't have an application to open maps, opening maps in browser");
                String data = String.format(MessageMeExternalAPI.G_MAPS_URL,
                        message.getLatitude(), message.getLongitude());

                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse(data));

                context.startActivity(intent);
            }
        }
    }

    private class DoodleMessageClickListener implements OnClickListener {

        private DoodleMessage doodleMessage = null;

        private DoodlePicMessage doodlePicMessage = null;

        public DoodleMessageClickListener(IMessage message) {
            if (message.getType() == IMessageType.DOODLE) {
                doodleMessage = (DoodleMessage) message;
            } else if (message.getType() == IMessageType.DOODLE_PIC) {
                doodlePicMessage = (DoodlePicMessage) message;
            }
        }

        @Override
        public void onClick(View v) {

            Intent intent = new Intent(context, DetailScreenActivity.class);
            if (doodleMessage != null) {
                intent.putExtra(Message.ID_COLUMN, doodleMessage.getId());
                intent.putExtra(Message.TYPE_COLUMN, doodleMessage.getType()
                        .getProtobufNumber());
            } else if (doodlePicMessage != null) {
                intent.putExtra(Message.ID_COLUMN, doodlePicMessage.getId());
                intent.putExtra(Message.TYPE_COLUMN, doodlePicMessage.getType()
                        .getProtobufNumber());
            }

            context.startActivityForResult(intent,
                    MessageMeConstants.DETAIL_SCREEN_REQUEST_CODE);

        }
    }

    public static String convertToTime(int value) {
        if (value < 60) {
            if (value < 10) {
                return "00:0" + String.valueOf(value);
            } else {
                return "00:" + String.valueOf(value);
            }
        } else {
            int minutes = value / 60;
            int seconds = value - (minutes * 60);
            if (seconds < 10) {
                return String.valueOf(minutes) + ":0" + String.valueOf(seconds);
            } else {
                return String.valueOf(minutes) + ":" + String.valueOf(seconds);
            }
        }
    }

    private class YouTubeMessageClickListener implements OnClickListener {

        private YoutubeMessage message;

        public YouTubeMessageClickListener(YoutubeMessage youtubeMessage) {
            this.message = youtubeMessage;
        }

        @Override
        public void onClick(View v) {

            String data = new StringBuilder().append(NATIVE_YOUTUBE_DATA)
                    .append(message.getVideoID()).toString();
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(data));

            List<ResolveInfo> matchedApps = context
                    .getPackageManager()
                    .queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY);

            // No native youtube app installed open URL instead
            if (matchedApps.size() == 0) {
                data = new StringBuilder().append(YOUTUBE_DEEPLINK)
                        .append(message.getVideoID()).toString();

                i = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
            }

            LogIt.user(ChatAdapter.class, "Pressed youtube message opening",
                    data);
            context.startActivity(i);
        }
    }

    private class RoomNoticeToGroupProfile implements OnClickListener {

        private NoticeMessage message;

        public RoomNoticeToGroupProfile(NoticeMessage noticeMessage) {
            this.message = noticeMessage;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(context, GroupProfileActivity.class);

            intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                    message.getChannelId());

            context.startActivity(intent);
        }
    }

    private class RoomNoticeToUserProfile implements OnClickListener {

        private NoticeMessage message;

        public RoomNoticeToUserProfile(NoticeMessage noticeMessage) {
            this.message = noticeMessage;
        }

        @Override
        public void onClick(View v) {

            if (message.getActionUserID() != MessageMeApplication
                    .getCurrentUser().getContactId()) {
                Intent intent = new Intent(context,
                        ContactProfileActivity.class);

                if (message.getActionUserID() == -1) {
                    intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                            message.getSenderId());
                } else {

                    intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY,
                            message.getActionUserID());
                }

                context.startActivity(intent);

            }
        }
    }

    private class ContactMessageClickListener implements OnClickListener {

        private ContactMessage message;

        public ContactMessageClickListener(ContactMessage msg) {
            this.message = msg;
        }

        @Override
        public void onClick(View v) {

            if (message.getPBUser().getUserID() == MessageMeApplication
                    .getCurrentUser().getContactId()) {
                LogIt.user(ContactMessageClickListener.class,
                        "Forwarded contact is current user, show My Profile");

                Intent intent = new Intent(context, TabsFragmentActivity.class);
                intent.putExtra(TabsFragmentActivity.EXTRA_TAB_TO_SHOW,
                        TabsFragmentActivity.TAB_PROFILE);
                context.startActivity(intent);
            } else {
                LogIt.user(ContactMessageClickListener.class,
                        "Forwarded contact selected", message.getDisplayName());

                Intent intent = new Intent(context,
                        ContactProfileActivity.class);
                intent.putExtra(MessageMeConstants.RECIPIENT_ID_KEY, message
                        .getPBUser().getUserID());
                context.startActivity(intent);
            }
        }
    }

    private class UnsupportedMessageClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            // Get the application package name
            String packageName = context.getPackageName();

            try {
                LogIt.user(
                        UnsupportedMessageClickListener.class,
                        "Unsupported message selected, take them to Google Play store",
                        packageName);

                // The preferred method of opening Google Play is with the
                // market://
                // protocol
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse("market://details?id=" + packageName)));
            } catch (ActivityNotFoundException anfe) {
                LogIt.i(UnsupportedMessageClickListener.class,
                        "Device does not have Google Play, launch URL instead",
                        anfe);
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse("http://play.google.com/store/apps/details?id="
                                + packageName)));
            }
        }
    }

    /**
     * Get the next LOAD_MORE_MESSAGES_BATCH_SIZE set of messages from
     * the local DB
     */
    public void loadEarlierMessagesFromDB(final boolean msgsWereLoadedFromServer) {

        final IMessage earliestMsgInUI = getItem(0);

        if (earliestMsgInUI == null) {
            // This can happen if the UI is being destroyed
            LogIt.w(this,
                    "Earliest message in UI is null, ignore request to Load Earlier Messages");
            return;
        }

        LogIt.d(ChatAdapter.class, "loadEarlierMessagesFromDB",
                earliestMsgInUI.getSortedBy());

        new DatabaseTask(mHandler) {

            List<IMessage> messages = new ArrayList<IMessage>();

            @Override
            public void work() {
                // Always try to load one extra message from the database, but
                // don't display it. This tells us whether there are more
                // messages in the local database or not. That means we can
                // go to the server for the next set of loads, rather than
                // trying to load from the local database when there are no
                // more messages (which can happen if there were exactly
                // LOAD_MORE_MESSAGES_BATCH_SIZE left in the local database).
                messages = MessageUtil.getChatMessagesBefore(chat,
                        earliestMsgInUI,
                        ChatAdapter.LOAD_MORE_MESSAGES_BATCH_SIZE + 1);
            }

            @Override
            public void done() {

                int topItemOffset = 0;

                boolean areMoreLocalMsgs = (messages.size() == (LOAD_MORE_MESSAGES_BATCH_SIZE + 1));

                if (areMoreLocalMsgs) {
                    // We don't want to display the extra message we loaded
                    // as we only loaded it to confirm if there are more
                    LogIt.d(ChatAdapter.class, "Remove the marker message");
                    messages.remove(LOAD_MORE_MESSAGES_BATCH_SIZE);
                }

                if (messages.size() == 0) {
                    LogIt.d(ChatAdapter.class, "No earlier messages to show");
                } else {
                    LogIt.d(ChatAdapter.class, "Loaded earlier messages",
                            messages.size());

                    // Remember the offset before adding the earlier
                    // messages into the UI
                    View itemView = mListView.getChildAt(0);

                    if (itemView != null) {
                        topItemOffset = itemView.getTop();
                    }

                    // We need to include the header view height as that will
                    // now be scrolled off screen
                    if (mListView.getHeaderViewsCount() > 0) {
                        topItemOffset += mLoadEarlierMsgsView.getHeight();
                    }

                    for (IMessage msg : messages) {
                        data.add(0, msg);
                    }
                    notifyDataSetChanged();
                }

                if (!areMoreLocalMsgs) {
                    if (msgsWereLoadedFromServer) {
                        LogIt.d(ChatAdapter.class,
                                "No more remote messages, hide Load Earlier Messages");

                        mDisplayLoadEarlierMsgsBtn = false;
                        updateLoadEarlierMsgsBtnVisibility();
                    } else {
                        LogIt.d(ChatAdapter.class,
                                "No more local messages, do next loads from the server");
                        mLoadNextMsgsFromServer = true;
                    }
                }

                ChatActivity.positionListView(mListView, messages.size(),
                        topItemOffset);

                // Enable the Load Earlier Messages button again
                mLoadEarlierMsgsBtn.setEnabled(true);
            }
        };
    }

    /**
     * Downloads earlier commands from the server and then passes them
     * to our single threaded receive queue. Once the commands are
     * parsed and processed an intent is sent to tell us to update the
     * UI from the local database again.
     */
    private void loadEarlierMessagesFromServer(final IMessage earliestMsgInUI) {

        LogIt.d(ChatAdapter.class, "loadEarlierMessagesFromServer",
                earliestMsgInUI.getSortedBy());

        new BackgroundTask() {

            private PBCommandEnvelope pbCmdEnvelope;

            private int mDownloadedBatchCmdsSize;

            @Override
            public void work() {
                try {
                    pbCmdEnvelope = RestfulClient.getInstance().commandQuery(
                            chat.getChatId(), earliestMsgInUI.getCommandId());

                    if (pbCmdEnvelope.hasError()) {
                        LogIt.w(ChatAdapter.class, "Command query error",
                                pbCmdEnvelope.getError().getCode());
                        fail(context.getString(R.string.generic_error_title),
                                context.getString(R.string.unexpected_error));
                    } else {
                        if (pbCmdEnvelope.getType() == CommandType.BATCH) {
                            final PBCommandBatch commandBatch = pbCmdEnvelope
                                    .getBatch();

                            mDownloadedBatchCmdsSize = commandBatch
                                    .getCommandsList().size();

                            LogIt.d(ChatAdapter.class,
                                    "Successfully loaded earlier commands - BATCH size",
                                    mDownloadedBatchCmdsSize);
                        } else {
                            LogIt.w(ChatAdapter.class,
                                    "Command query did not contain BATCH");
                            fail(context
                                    .getString(R.string.generic_error_title),
                                    context.getString(R.string.unexpected_error));
                        }
                    }
                } catch (ResourceException e) {
                    fail(context.getString(R.string.network_error_title),
                            context.getString(R.string.network_error));
                } catch (IOException e) {
                    fail(context.getString(R.string.network_error_title),
                            context.getString(R.string.network_error));
                } catch (Exception e) {
                    LogIt.e(e, "Failed to load earlier messages");
                    fail(context.getString(R.string.generic_error_title),
                            context.getString(R.string.unexpected_error));
                }
            }

            @Override
            public void done() {
                if (failed()) {
                    UIUtil.alert(context, getExceptionTitle(),
                            getExceptionMessage());
                    mLoadEarlierMsgsBtn.setEnabled(true);
                } else {
                    if (mDownloadedBatchCmdsSize == 0) {
                        LogIt.d(ChatAdapter.class,
                                "No commands on server, hide Load Earlier Messages");
                        mDisplayLoadEarlierMsgsBtn = false;
                        updateLoadEarlierMsgsBtnVisibility();
                    } else {
                        CommandReceiver cmdReceiver = CommandReceiver
                                .getInstance();

                        if (cmdReceiver == null) {
                            LogIt.e(ChatAdapter.class,
                                    "Failed to get CommandReceiver");
                        } else {
                            // Add this BATCH to the receive queue
                            LogIt.d(ChatAdapter.class,
                                    "Add downloaded commands to receive queue");
                            cmdReceiver.addToQueue(new CommandReceiverEnvelope(
                                    pbCmdEnvelope, earliestMsgInUI
                                            .getSortedBy()));
                        }
                    }
                }
            }
        };
    }

    private class LoadEarlierMessagesClickListener implements OnClickListener {

        @Override
        public void onClick(final View v) {

            LogIt.user(this, "Load Earlier Messages pressed");

            // Disable Load Earlier Messages button while we are retrieving
            // the messages
            mLoadEarlierMsgsBtn.setEnabled(false);

            final IMessage earliestMsgInUI = getItem(0);

            if (mLoadNextMsgsFromServer) {
                loadEarlierMessagesFromServer(earliestMsgInUI);

                // The messages loaded from the server get inserted into the
                // database, so that's where we need to load them from next
                mLoadNextMsgsFromServer = false;
            } else {
                loadEarlierMessagesFromDB(false);
            }
        }
    }
}