package com.littleinc.MessageMe.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.util.ImageLoader.ProfilePhotoSize;

public class ContactAdapter extends BaseAdapter {

    private static final int TYPE_ITEM = 0;

    private static final int TYPE_SEPARATOR = 1;

    private static final int TYPE_MAX_COUNT = TYPE_SEPARATOR + 1;

    private ViewHolder holder;

    private LayoutInflater mInflater;

    private List<Contact> contacts = new ArrayList<Contact>();

    private Set<Integer> mSeparatorsSet = new HashSet<Integer>();

    public ContactAdapter(Context context) {
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addContact(Contact contact) {
        contacts.add(contact);
        notifyDataSetChanged();
    }

    public void addContactSeparator(Contact contact) {
        contacts.add(contact);

        mSeparatorsSet.add(contacts.size() - 1);
        notifyDataSetChanged();
    }

    public void deleteAllItems() {
        LogIt.d(this, "Delete all contacts from list");
        contacts.clear();
        mSeparatorsSet.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return mSeparatorsSet.contains(position) ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_MAX_COUNT;
    }

    @Override
    public int getCount() {
        return contacts.size();
    }

    @Override
    public Contact getItem(int position) {
        if (position >= contacts.size()) {
            LogIt.w(this,
                    "Ignore attempt to display contact that would cause IndexOutOfBoundsException");
            return null;
        } else {            
            return contacts.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return contacts.get(position).getContactId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Contact contact = getItem(position);
        
        if (contact == null) {
            LogIt.w(this, "No contact, return null view");
            return null;
        }
        
        int type = getItemViewType(position);

        if (convertView == null) {
            holder = new ViewHolder();

            switch (type) {
            case TYPE_ITEM:
                convertView = mInflater.inflate(R.layout.contact_row, null);

                holder.name = (TextView) convertView
                        .findViewById(R.id.contact_name);
                holder.contactImageView = (ImageView) convertView
                        .findViewById(R.id.contact_image);
                break;
            case TYPE_SEPARATOR:
                convertView = mInflater.inflate(
                        R.layout.layout_listview_separator, null);
                holder.separator = (TextView) convertView
                        .findViewById(R.id.separator);
                break;
            }

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder = getMessageContent(parent.getContext(), type, contact);

        return convertView;
    }

    private ViewHolder getMessageContent(Context context, int type,
            Contact contact) {
        switch (type) {
        case TYPE_ITEM:
            holder.name.setText(contact.getStyledNameWithEmojis());
            
            if (contact.isGroup()) {
                holder.name
                        .setCompoundDrawablesWithIntrinsicBounds(
                                context.getResources().getDrawable(
                                        R.drawable.common_icon_group_inline_bglight), null,
                                null, null);
            } else {
                holder.name.setCompoundDrawablesWithIntrinsicBounds(null, null,
                        null, null);
            }

            if (TextUtils.isEmpty(contact.getProfileImageKey())) {
                holder.contactImageView.setTag(null);
                holder.contactImageView.setImageBitmap(null);
            } else {
                holder.contactImageView.setImageBitmap(null);

                ImageLoader.getInstance().displayProfilePicture(contact, 
                        holder.contactImageView,
                        ProfilePhotoSize.SMALL);
            }
            break;
        case TYPE_SEPARATOR:
            holder.separator.setText(contact.getNameInitial().toUpperCase());
            break;
        }

        return holder;
    }

    public static class ViewHolder {
        public TextView name;

        public ImageView contactImageView;

        public TextView separator;
    }
}