package com.littleinc.MessageMe.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.ui.EmojiUtils;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;
import com.littleinc.MessageMe.util.ImageLoader.ProfilePhotoSize;

public class MessageAdapter extends BaseAdapter {

    private List<IMessage> data = new ArrayList<IMessage>();

    private LayoutInflater mInflater;

    private ViewHolder holder;

    private Context context;

    private static Message.SortMessages sMessageSorter = new Message.SortMessages();

    public MessageAdapter(Context context) {
        this.context = context;

        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void updateItems(List<IMessage> messages) {
        data = messages;
        notifyDataSetChanged();
    }

    /**
     * Add the provided IMessage to the adapter but do not notify
     * the adapter that the data has changed.  Callers of this 
     * method should call {@link #sortAndUpdate()} after adding all 
     * the items.
     */
    public void addItem(IMessage item) {
        data.add(item);
    }

    /**
     * Sort the data and notify the adapter that it has changed.
     */
    public void sortAndUpdate() {
        Collections.sort(data, sMessageSorter);
        notifyDataSetChanged();
    }

    /**
     * Update or add the provided IMessage to the list, and sort the list
     * and notify the adapter that it has changed.
     */
    public void updateItem(IMessage item) {
        boolean isReplaced = false;

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getChannelId() == item.getChannelId()) {
                data.set(i, item);
                isReplaced = true;
                break;
            }
        }

        if (!isReplaced) {
            data.add(item);
        }

        Collections.sort(data, sMessageSorter);
        notifyDataSetChanged();
    }

    public void deleteAllItems() {
        data.clear();
        notifyDataSetChanged();
    }

    public void deleteItemtAt(int position) {
        data.remove(position);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public IMessage getItem(int position) {
        if (data != null && position < data.size()) {
            return data.get(position);
        } else if (data != null) {
            LogIt.w(MessageAdapter.class, "OutOfBounds",
                    position + "/" + data.size());
            return null;
        } else {
            LogIt.w(MessageAdapter.class, "IMessage list is null");
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        IMessage message = getItem(position);

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.message_list_row, null);
            holder.messageStatus = (ImageView) convertView
                    .findViewById(R.id.message_status);
            holder.contactImageView = (ImageView) convertView
                    .findViewById(R.id.contact_image);
            holder.contactName = (TextView) convertView
                    .findViewById(R.id.contact_name);
            holder.textContent = (TextView) convertView
                    .findViewById(R.id.message_content);
            holder.date = (TextView) convertView
                    .findViewById(R.id.message_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder = getMessageContent(message, context, holder);

        // This adapter is only used to display message search results
        // where we never show the unread state        
        holder.messageStatus.setVisibility(View.INVISIBLE);

        return convertView;
    }

    public static ViewHolder getMessageContent(IMessage message,
            Context context, ViewHolder holder) {

        if (message == null) {
            LogIt.e(MessageAdapter.class,
                    "Null message passed to getMessageContent");
            holder.contactName.setText("");
            holder.textContent.setText("");
            holder.date.setText("");
            holder.contactImageView.setTag(null);
            holder.contactImageView.setImageBitmap(null);

            return holder;
        }

        Contact sender = message.getContact();
        StringBuilder profileImageKey = new StringBuilder("");

        if (sender != null) {
            holder.contactName.setText(EmojiUtils.convertToEmojisIfRequired(
                    sender.getDisplayName(), EmojiSize.NORMAL));
            profileImageKey.append(sender.getProfileImageKey());
        } else {

            holder.contactName.setText("");
            LogIt.w(MessageAdapter.class,
                    "getContact() is null for message, display blank sender",
                    message);
        }

        holder.textContent.setText(EmojiUtils.convertToEmojisIfRequired(
                message.getMessagePreview(context), EmojiSize.SMALL));

        if (message.wasSentByThisUser()) {
            Drawable left = context.getResources().getDrawable(
                    R.drawable.common_icon_reply_inline_bglight);

            holder.textContent.setCompoundDrawablePadding(context
                    .getResources().getDimensionPixelSize(
                            R.dimen.convo_preview_msg_desc_pad));
            holder.textContent.setCompoundDrawablesWithIntrinsicBounds(left,
                    null, null, null);
        } else {
            holder.textContent.setCompoundDrawablesWithIntrinsicBounds(null,
                    null, null, null);
        }

        // The messageStatus indicator visibility is set outside this method
        if (message.getChannelId() == MessageMeConstants.WELCOME_ROOM_ID) {
            // Welcome room messages have command ID zero, but should not
            // show as in error
            LogIt.d(MessageAdapter.class,
                    "Avoid displaying error message for welcome message");
            holder.textContent.setTextColor(context.getResources().getColor(
                    R.color.gray));
        } else if (message.getCommandId() == 0) {
            holder.messageStatus
                    .setBackgroundResource(R.drawable.room_status_icon_fail);
            holder.textContent.setText(R.string.messages_failure);
            holder.textContent.setTextColor(context.getResources()
                    .getColor(R.color.red));
        } else {
            holder.messageStatus
                    .setBackgroundResource(R.drawable.roomsview_roomstatus_icon_new);
            holder.textContent.setTextColor(context.getResources().getColor(
                    R.color.gray));
        }

        holder.date.setText(formatDate(message.getCreatedAt(), context));

        if (TextUtils.isEmpty(profileImageKey)) {
            holder.contactImageView.setTag(null);
            holder.contactImageView.setImageBitmap(null);
        } else {
            holder.contactImageView.setImageBitmap(null);

            ImageLoader.getInstance().displayProfilePicture(
                    message.getContact(), holder.contactImageView,
                    ProfilePhotoSize.SMALL);
        }

        return holder;
    }

    public static String formatDate(int timeStamp, Context context) {

        String msgTime = "";

        Date date = DateUtil.convertToDate(timeStamp);
        Time messageTime = new Time();
        messageTime.set(date.getTime());

        Time currentTime = new Time();
        currentTime.setToNow();

        if (messageTime.month == currentTime.month) {
            if (messageTime.monthDay == currentTime.monthDay) {
                msgTime = DateFormat.format("h:mm aa", date).toString();
            } else if (messageTime.monthDay == (currentTime.monthDay - 1)) {
                msgTime = context.getString(R.string.yesterday_label);
            } else {
                msgTime = messageTime.format("%m/%d/%Y");
            }
        } else {
            msgTime = messageTime.format("%m/%d/%Y");
        }
        return msgTime;
    }

    public static class ViewHolder {
        public ImageView messageStatus;

        public ImageView contactImageView;

        public TextView contactName;

        public TextView textContent;

        public TextView date;
    }
}