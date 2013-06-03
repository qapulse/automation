package com.littleinc.MessageMe.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.Conversation;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.ui.EmojiUtils;
import com.littleinc.MessageMe.ui.EmojiUtils.EmojiSize;
import com.littleinc.MessageMe.util.ImageLoader.ProfilePhotoSize;
import com.littleinc.MessageMe.util.MessageAdapter.ViewHolder;

public class ConversationAdapter extends BaseAdapter {

    private Handler mHandler;
    
    private List<Conversation> data = new ArrayList<Conversation>();

    private ListView mListView;
    
    private LayoutInflater mInflater;

    private ViewHolder holder;

    private Context context;

    private static Conversation.SortConversation sConversationSorter = new Conversation.SortConversation();
    
    public ConversationAdapter(Context context, ListView listView) {
        this.context = context;
        mListView = listView;

        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mHandler = new Handler();
    }

    public void updateItems(List<Conversation> conversations) {
        data = conversations;
        Collections.sort(data, sConversationSorter);
        notifyDataSetChanged();
    }

    /**
     * Update or add the provided Conversation to the list, and sort the list
     * and notify the adapter that it has changed.
     */
    public void updateItem(Conversation item) {
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

        Collections.sort(data, sConversationSorter);
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
    public Conversation getItem(int position) {
        if (data != null && position < data.size()) {
            return data.get(position);
        } else if (data != null) {
            LogIt.w(ConversationAdapter.class, "OutOfBounds",
                    position + "/" + data.size());
            return null;
        } else {
            LogIt.w(ConversationAdapter.class, "IMessage list is null");
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private void updateConversation(Conversation conversationToUpdate,
            IMessage message) {

        int start = mListView.getFirstVisiblePosition();
        int end = mListView.getLastVisiblePosition();
                
        // LogIt.d(ConversationAdapter.class,
        //         "Updating conversation - search for view", start, end);
        
        for (int i = start; i <= end; i++) {            
            if (conversationToUpdate == mListView.getItemAtPosition(i)) {
                
                // LogIt.d(ConversationAdapter.class,
                //         "Updating conversation - view found",
                //         conversationToUpdate, i);
                
                View view = mListView.getChildAt(i - start);
                
                // Display the channel name
                if (message.getContact() == null) {
                    ((TextView) view.findViewById(R.id.contact_name))
                            .setText("");
                } else {
                    ((TextView) view.findViewById(R.id.contact_name))
                            .setText(message.getContact().getStyledNameWithEmojis());
                }
                
                if (context == null) {
                    LogIt.w(ConversationAdapter.class,
                            "Don't update conversation as context is null",
                            conversationToUpdate);
                } else {
                    ((TextView) view.findViewById(R.id.message_content))
                            .setText(EmojiUtils.convertToEmojisIfRequired(
                                    message.getMessagePreview(context),
                                    EmojiSize.SMALL));
                    
                    ((TextView) view.findViewById(R.id.message_date))
                            .setText(MessageAdapter.formatDate(
                                    message.getCreatedAt(), context));
                }
                
                break;
            }
        }
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
        final Conversation conversation = getItem(position);
        
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
        
        if ((conversation.getChannelId() == MessageMeConstants.WELCOME_ROOM_ID)
                || (conversation.getUnreadCount() == 0)) {
            holder.messageStatus.setVisibility(View.INVISIBLE);
        } else {
            holder.messageStatus.setVisibility(View.VISIBLE);
        }
        
        if (conversation.isCachedIMessageAvailable()) {
            // Get a minimal message suitable for displaying a summary in the list
            IMessage message = conversation.getLastMessageForMessagesList();
            
            holder = MessageAdapter.getMessageContent(message, context, holder);
        } else {    
            // Set the UI to show this row is loading.  The messageStatus was
            // updated above as it doesn't require the latest message.
            holder.contactName.setText(R.string.loading);
            holder.textContent.setText("");
            holder.date.setText("");
            holder.contactImageView.setImageBitmap(null);            
            
            new DatabaseTask(mHandler) {
                
                private IMessage mMsg;
                
                @Override
                public void work() {
                    mMsg = conversation.getLastMessageForMessagesList();
                }
                
                @Override
                public void done() {
                    if (mMsg == null) {
                        LogIt.w(ConversationAdapter.class,
                                "Ignore null message when trying to update conversation",
                                conversation);
                    } else {           
                        ImageLoader.getInstance().displayProfilePicture(
                                mMsg.getContact(), holder.contactImageView,
                                ProfilePhotoSize.SMALL);

                        // Update the conversation if that row is still visible
                        updateConversation(conversation, mMsg);
                    }
                }
            };
        }
        
        return convertView;
    }
}