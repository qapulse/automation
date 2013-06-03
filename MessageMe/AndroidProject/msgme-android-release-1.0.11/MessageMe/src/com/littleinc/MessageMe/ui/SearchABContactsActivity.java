package com.littleinc.MessageMe.ui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.SearchActivity;
import com.littleinc.MessageMe.bo.ABContactInfo;
import com.littleinc.MessageMe.util.SearchManager;
import com.littleinc.MessageMe.util.SearchTextWatcher;
import com.littleinc.MessageMe.util.StringUtil;

public class SearchABContactsActivity extends SearchActivity implements
        SearchManager {

    private List<ABContactInfo> mContacts;

    private ABContactAdapter mABContactAdapter;

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            if (msg == null) {

                LogIt.w(SearchABContactsActivity.class,
                        "Ignore null message received by handler");
                return;
            }

            switch (msg.what) {
            case MessageMeConstants.UPDATE_SEARCH_MESSAGE:

                String terms = (String) msg.obj;

                LogIt.d(SearchABContactsActivity.class,
                        "UPDATE_SEARCH_MESSAGE", terms);

                if (StringUtil.isEmpty(terms)) {

                    if (mABContactAdapter != null) {

                        doSearch(terms);
                        isShowingDarkOverlay = true;
                        masterLayout.setBackgroundResource(0);

                        setVisible(R.id.emptyElement, false);
                    } else {

                        LogIt.w(SearchABContactsActivity.class,
                                "Adapter still null, omitting message");
                    }
                } else {

                    doSearch(terms);
                }
                break;
            default:

                LogIt.w(SearchABContactsActivity.class,
                        "Unexpected message received by handler", msg.what);
                break;
            }
        }
    };

    /**
     * Method defined in the search_box layout XML
     */
    @Override
    public void onSearch(View view) {

        LogIt.user(SearchABContactsActivity.class,
                "Pressed on the search icon button");

        String terms = searchBox.getText().toString();
        UIUtil.hideKeyboard(searchBox);
        doSearch(terms);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchBox.addTextChangedListener(new SearchTextWatcher(mHandler));
        searchBox.setHint(R.string.search_contacts_hint);

        listView.setBackgroundColor(getResources().getColor(R.color.white));

        mContacts = MessageMeApplication.getAppState().getAbContacts();
    };

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void updateUI() {

        if (listView.getAdapter() == null) {

            LogIt.i(SearchABContactsActivity.class,
                    "Contact list adapter null, set it");

            mABContactAdapter = new ABContactAdapter(this, mContacts);

            listView.setAdapter(mABContactAdapter);
            listView.setOnItemClickListener(mOnSearchResultClicked);
        } else {

            mABContactAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void doSearch(final String terms) {

        isShowingDarkOverlay = false;
        masterLayout.setBackgroundResource(R.color.list_view_background);

        mABContactAdapter.getFilter().filter(terms);

        // Sometimes after filter mABContactAdapter the getCount method doesn't
        // return the updated count of elements causing a problem on UI. Adding a
        // small delay the problem have been fixed
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {

                if (TextUtils.isEmpty(terms)
                        || mABContactAdapter.getCount() > 0) {
                    setVisible(R.id.emptyElement, false);
                } else {
                    setVisible(R.id.emptyElement, true);
                }
            }
        }, 100);
    }

    private OnItemClickListener mOnSearchResultClicked = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            ABContactInfo contact = mABContactAdapter.getItem(position);

            if (contact != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(MessageMeConstants.SELECTED_AB_CONTACT,
                        contact);

                setResult(RESULT_OK, resultIntent);
            } else {

                LogIt.w(SearchABContactsActivity.class, "Pressed null item");
                setResult(RESULT_CANCELED);
            }
            finish();
        }
    };

    class ABContactAdapter extends BaseAdapter implements Filterable {

        private LayoutInflater inflater;

        private List<ABContactInfo> abContacts;

        private List<ABContactInfo> filteredABContacts;

        private List<ABContactInfo> abContactsChecked = MessageMeApplication
                .getAppState().getABContactsChecked();

        private ItemFilter mItemFilter = new ItemFilter();

        public ABContactAdapter(Context context, List<ABContactInfo> data) {

            abContacts = data;
            filteredABContacts = new LinkedList<ABContactInfo>();

            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return filteredABContacts.size();
        }

        @Override
        public ABContactInfo getItem(int position) {

            if (filteredABContacts != null
                    && position < filteredABContacts.size()) {

                return filteredABContacts.get(position);
            } else if (filteredABContacts != null) {

                LogIt.w(ABContactAdapter.class, "OutOfBounds", position + "/"
                        + filteredABContacts.size());

                return null;
            } else {

                LogIt.w(ABContactAdapter.class, "abContacts list is null");

                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;

            if (convertView == null) {

                holder = new ViewHolder();

                convertView = inflater.inflate(R.layout.contact_entry, parent,
                        false);

                holder.checkBox = (CheckBox) convertView
                        .findViewById(R.id.contact_entry_check);
                holder.nameLabel = (TextView) convertView
                        .findViewById(R.id.contact_entry_name);
                holder.contactInfoLabel = (TextView) convertView
                        .findViewById(R.id.contact_entry_info);
                holder.contactTypeLabel = (TextView) convertView
                        .findViewById(R.id.contact_entry_type);

                convertView.setTag(holder);
            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            ABContactInfo contact = getItem(position);

            if (abContactsChecked != null
                    && abContactsChecked.contains(contact)) {
                holder.checkBox.setChecked(true);
            } else {
                holder.checkBox.setChecked(false);
            }

            holder.nameLabel.setText(contact.getDisplayName());
            holder.contactInfoLabel.setText(contact.getData());
            holder.contactTypeLabel.setText(getString(
                    contact.getTypeLabelResource()).toUpperCase());

            return convertView;
        }

        class ViewHolder {

            CheckBox checkBox;

            TextView nameLabel;

            TextView contactInfoLabel;

            TextView contactTypeLabel;
        }

        @Override
        public Filter getFilter() {
            return mItemFilter;
        }

        class ItemFilter extends Filter {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();
                final List<ABContactInfo> list = abContacts;
                String filterString = constraint.toString().toLowerCase();

                int count = list.size();
                ABContactInfo filterableString;
                ArrayList<ABContactInfo> nlist = null;

                if (!TextUtils.isEmpty(constraint)) {

                    nlist = new ArrayList<ABContactInfo>(count);

                    for (int i = 0; i < count; i++) {

                        filterableString = list.get(i);

                        if ((filterableString.getDisplayName().toLowerCase()
                                .contains(filterString) || (filterableString
                                .getData() != null && filterableString
                                .getData().toLowerCase().contains(filterString)))) {

                            nlist.add(filterableString);
                        }
                    }
                } else {

                    nlist = new ArrayList<ABContactInfo>();
                }

                results.values = nlist;
                results.count = nlist.size();

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint,
                    FilterResults results) {

                filteredABContacts = (ArrayList<ABContactInfo>) results.values;
                notifyDataSetChanged();
            }
        }
    }
}
