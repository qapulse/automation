package com.littleinc.MessageMe.ui;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.restlet.resource.ResourceException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.MessageMeExternalAPI;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.util.ChatAdapter;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.SearchManager;
import com.littleinc.MessageMe.util.StringUtil;
import com.littleinc.MessageMe.youtube.ParceableYouTubeItem;
import com.littleinc.MessageMe.youtube.VideoFeed;
import com.littleinc.MessageMe.youtube.YouTubeClient;
import com.littleinc.MessageMe.youtube.YouTubeItem;
import com.littleinc.MessageMe.youtube.YouTubeUrl;

@TargetApi(14)
public class SearchYouTubeVidActivity extends ActionBarActivity implements
        SearchManager {

    private YouTubeClient client;

    private YouTubeUrl url;

    private YouTubeAdapter adapter;

    protected ListView listView;

    protected EditText searchBox;

    protected RelativeLayout masterLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_list_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        masterLayout = (RelativeLayout) findViewById(R.id.master_layout);

        listView = (ListView) findViewById(R.id.list_view);

        searchBox = (EditText) findViewById(R.id.search_box);

        client = new YouTubeClient();

        url = new YouTubeUrl(MessageMeExternalAPI.VIDEOS_FEED);

        setTitle(R.string.search_youtube_hint);

        searchBox.setHint(R.string.search_youtube_hint);
        searchBox.setOnEditorActionListener(new SearchKeyListener());

        updateUI();

        // Always show the keyboard when launching search
        UIUtil.showKeyboard(searchBox);
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

    @Override
    protected void onPause() {
        super.onPause();
        UIUtil.hideKeyboard(searchBox);
    }

    @Override
    protected void onResume() {
        super.onResume();
        masterLayout.setBackgroundResource(R.color.list_view_background);
    }

    private class ListViewItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapter, View view,
                int position, long id) {
            YouTubeItem media = (YouTubeItem) adapter
                    .getItemAtPosition(position);
            Intent intent = new Intent();
            ParceableYouTubeItem bundleData = new ParceableYouTubeItem(media);
            intent.putExtra(MessageMeConstants.EXTRA_YOUTUBE, bundleData);
            setResult(RESULT_OK, intent);
            finish();
        }

    }

    private class SearchKeyListener implements EditText.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (DeviceUtil.wasActionPressed(v, actionId, event)) {
                UIUtil.hideKeyboard(v);
                String searchTerms = searchBox.getText().toString();
                doSearch(searchTerms);
                return true;
            } else {
                return false;
            }
        }

    }

    private void searchVideos(final String terms) {

        final ProgressDialog progressDialog = showProgressDialog(this
                .getString(R.string.loading_videos_lbl));

        new BackgroundTask() {
            List<YouTubeItem> items = new ArrayList<YouTubeItem>();

            @Override
            public void work() {
                try {
                    url.q = terms;
                    VideoFeed feed = client.executeGetVideoFeed(url);
                    if (feed.items != null) {
                        for (YouTubeItem item : feed.items) {
                            items.add(item);
                        }
                    }
                } catch (IOException e) {
                    LogIt.e(SearchYouTubeVidActivity.class, e, e.getMessage());
                    fail(getString(R.string.network_error_title),
                            getString(R.string.network_error));
                } catch (ResourceException e) {
                    LogIt.e(SearchYouTubeVidActivity.class, e, e.getMessage());
                    fail(getString(R.string.network_error_title),
                            getString(R.string.network_error));
                } catch (Exception e) {
                    LogIt.e(SearchYouTubeVidActivity.class, e, e.getMessage());
                    fail(getString(R.string.unexpected_error_title),
                            getString(R.string.network_error));
                }
            }

            @Override
            public void done() {
                progressDialog.dismiss();
                if (!failed()) {
                    adapter.deleteAllItems();
                    if (items.size() > 0) {
                        adapter.updateList(items);
                        setVisible(R.id.emptyElement, false);
                    } else {
                        setVisible(R.id.emptyElement, true);
                    }

                } else {
                    alert(getExceptionTitle(), getExceptionMessage());
                }
            }
        };

    }

    /**
     * Method defined in the search_box layout XML
     */
    @Override
    public void onSearch(View view) {
        LogIt.user(this, "User pressed on the search icon button");
        String term = searchBox.getText().toString();
        if (!StringUtil.isEmpty(term)) {
            UIUtil.hideKeyboard(searchBox);
            doSearch(term);
        }
    }

    private static class YouTubeAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        private List<YouTubeItem> items;

        private ImageLoader mImageLoader;

        private Context mContext;

        public YouTubeAdapter(Context context) {
            this.items = new ArrayList<YouTubeItem>();
            inflater = (LayoutInflater) context
                    .getSystemService(LAYOUT_INFLATER_SERVICE);
            mImageLoader = ImageLoader.getInstance();
            mContext = context;
        }

        public void updateList(List<YouTubeItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        public void deleteAllItems() {
            this.items.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VideoViewHolder holder;
            YouTubeItem videoEntry = items.get(position);
            if (convertView == null) {
                convertView = inflater.inflate(
                        R.layout.search_youtube_video_result_item, parent,
                        false);
                holder = new VideoViewHolder();
                convertView.setTag(holder);

                holder.thumbnail = (ImageView) convertView
                        .findViewById(R.id.video_thumbnail);
                holder.title = (TextView) convertView
                        .findViewById(R.id.video_title);
                holder.author = (TextView) convertView
                        .findViewById(R.id.video_author);
                holder.totalViews = (TextView) convertView
                        .findViewById(R.id.video_views);
                holder.duration = (TextView) convertView
                        .findViewById(R.id.video_duration);

            } else {
                holder = (VideoViewHolder) convertView.getTag();
            }

            // Display YouTube view counts with commas to match their website
            NumberFormat formatter = new DecimalFormat("###,###,###,###");
            String formattedViews = formatter.format(videoEntry.getViewCount());
            holder.totalViews.setText(formattedViews + " "
                    + mContext.getString(R.string.views_label));

            holder.title.setText(videoEntry.getTitle());
            holder.author.setText(videoEntry.getUploader());
            holder.duration.setText(ChatAdapter.convertToTime(videoEntry
                    .getDuration()));

            String md5Key = null;
            String thumbnailUrl = null;
            holder.thumbnail.setImageBitmap(null);
            if (videoEntry.getThumbnail().getMqDefault() != null) {
                LogIt.d(this, "Show medium sized thumbnail", videoEntry
                        .getThumbnail().getMqDefault());

                thumbnailUrl = videoEntry.getThumbnail().getMqDefault();
                md5Key = FileSystemUtil.md5(videoEntry.getThumbnail()
                        .getMqDefault());
                holder.thumbnail.setTag(md5Key);
                mImageLoader.displayWebImage(md5Key, thumbnailUrl,
                        holder.thumbnail);
            } else {
                LogIt.d(this, "Show small thumbnail as medium does not exist",
                        videoEntry.getThumbnail().getSqDefault(), videoEntry
                                .getThumbnail().getMqDefault());

                thumbnailUrl = videoEntry.getThumbnail().getSqDefault();
                md5Key = FileSystemUtil.md5(videoEntry.getThumbnail()
                        .getSqDefault());
                holder.thumbnail.setTag(md5Key);
                mImageLoader.displayWebImage(md5Key, thumbnailUrl,
                        holder.thumbnail);
            }

            return convertView;
        }
    }

    static class VideoViewHolder {
        ImageView thumbnail;

        TextView title;

        TextView author;

        TextView totalViews;

        TextView duration;
    }

    @Override
    public void doSearch(String terms) {
        searchVideos(terms);
    }

    @Override
    public void updateUI() {
        if (listView.getAdapter() == null) {
            LogIt.i(this, "Contact list adapter null, set it");
            adapter = new YouTubeAdapter(this);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new ListViewItemClickListener());
        } else {
            adapter.notifyDataSetChanged();
        }
    }
}