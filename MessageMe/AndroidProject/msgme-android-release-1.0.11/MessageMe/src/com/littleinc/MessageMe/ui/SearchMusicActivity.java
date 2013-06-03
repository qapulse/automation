package com.littleinc.MessageMe.ui;

import java.util.ArrayList;
import java.util.List;

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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.net.ITunesService;
import com.littleinc.MessageMe.net.ItunesMedia;
import com.littleinc.MessageMe.net.ItunesServiceException;
import com.littleinc.MessageMe.util.AudioUtil;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.MediaPlaybackClickListener;
import com.littleinc.MessageMe.util.SearchManager;
import com.littleinc.MessageMe.util.StringUtil;
import com.littleinc.MessageMe.widget.PlayButtonImageView;

@TargetApi(14)
public class SearchMusicActivity extends ActionBarActivity implements
        SearchManager {

    private ITunesService mItunesService = new ITunesService();

    private MusicAdapter adapter;

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

        setTitle(R.string.search_music_title);

        searchBox.setOnEditorActionListener(new SearchKeyListener());

        searchBox.setHint(R.string.search_music_hint);

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

        if (adapter.hasPlayedMusic() && isFinishing()) {
            AudioUtil.stopPlaying();
        } else if (!MessageMeApplication.isInForeground()) {
            AudioUtil.pausePlaying(true);
        }
    }

    private class ListViewItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapter, View view,
                int position, long id) {
            ItunesMedia media = (ItunesMedia) adapter
                    .getItemAtPosition(position);
            Intent intent = new Intent();
            intent.putExtra(MessageMeConstants.EXTRA_MEDIA, media);
            setResult(RESULT_OK, intent);
            finish();
        }

    }

    private class SearchKeyListener implements EditText.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (DeviceUtil.wasActionPressed(v, actionId, event)) {
                UIUtil.hideKeyboard(v);
                if (adapter.hasPlayedMusic()) {
                    AudioUtil.stopPlaying();
                }

                String searchTerms = searchBox.getText().toString();
                doSearch(searchTerms);
                return true;
            } else {
                return false;
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        masterLayout.setBackgroundResource(R.color.list_view_background);
    }

    private void searchMusic(final String terms) {

        final ProgressDialog progressDialog = showProgressDialog(this
                .getString(R.string.loading_songs_lbl));

        new BackgroundTask() {
            List<ItunesMedia> mediaItems = new ArrayList<ItunesMedia>();

            @Override
            public void work() {
                try {
                    mediaItems = mItunesService.searchMusic(terms, 50);
                } catch (ItunesServiceException e) {
                    LogIt.e(this, e, e.getMessage());
                    fail(getString(R.string.network_error));
                }
            }

            @Override
            public void done() {
                progressDialog.dismiss();
                if (!failed()) {
                    adapter.updateList(mediaItems);
                    if (mediaItems.size() > 0) {
                        setVisible(R.id.emptyElement, false);
                    } else {
                        setVisible(R.id.emptyElement, true);
                    }
                    updateUI();

                } else {
                    alert(SearchMusicActivity.this
                            .getString(R.string.generic_error_title),
                            getExceptionMessage());
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

    static class MusicAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        private List<ItunesMedia> items;

        private ImageLoader mImageLoader;

        private Context mContext;

        private boolean mPlayedMusic = false;

        private class SearchMediaPlaybackClickListener extends
                MediaPlaybackClickListener {
            public SearchMediaPlaybackClickListener(Context context,
                    BaseAdapter mediaAdapter, String trackName,
                    String previewUrl, PlayButtonImageView playPauseButton,
                    ProgressBar progressBar) {
                super(context, mediaAdapter, AudioUtil.COMMAND_ID_NONE,
                        trackName, previewUrl, progressBar);
            }

            @Override
            public void onClick(View v) {
                super.onClick(v);
                mPlayedMusic = true;
            }
        }

        public MusicAdapter(Context context) {
            this.items = new ArrayList<ItunesMedia>();
            inflater = (LayoutInflater) context
                    .getSystemService(LAYOUT_INFLATER_SERVICE);
            mImageLoader = ImageLoader.getInstance();
            mContext = context;
        }

        public boolean hasPlayedMusic() {
            return mPlayedMusic;
        }

        public void updateList(List<ItunesMedia> items) {
            this.items = items;
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
            MusicViewHolder holder;
            ItunesMedia media = (ItunesMedia) getItem(position);
            if (convertView == null) {
                convertView = inflater.inflate(
                        R.layout.search_media_result_item, parent, false);
                holder = new MusicViewHolder();
                convertView.setTag(holder);

                holder.audioProgressBar = (ProgressBar) convertView
                        .findViewById(R.id.progress_spinner);
                holder.mediaPlayBtn = (PlayButtonImageView) convertView
                        .findViewById(R.id.play_button);
                holder.thumbnail = (ImageView) convertView
                        .findViewById(R.id.media_thumbnail);
                holder.title = (TextView) convertView
                        .findViewById(R.id.media_title);
                holder.author = (TextView) convertView
                        .findViewById(R.id.media_author);
            } else {
                holder = (MusicViewHolder) convertView.getTag();
            }

            holder.title.setText(media.getTrackName());
            holder.author.setText(media.getArtistName());

            holder.thumbnail.setImageBitmap(null);
            String thumbnailUrl = media.getArtworkUrl60();
            String md5Key = FileSystemUtil.md5(thumbnailUrl);

            holder.thumbnail.setTag(md5Key);
            mImageLoader
                    .displayWebImage(md5Key, thumbnailUrl, holder.thumbnail);

            holder.mediaPlayBtn
                    .setOnClickListener(new SearchMediaPlaybackClickListener(
                            mContext, this, media.getTrackName(), media
                                    .getPreviewUrl(), holder.mediaPlayBtn,
                            holder.audioProgressBar));
            AudioUtil.setButtonState(AudioUtil.COMMAND_ID_NONE,
                    media.getPreviewUrl(), holder.mediaPlayBtn,
                    holder.audioProgressBar);

            return convertView;
        }
    }

    static class MusicViewHolder {
        ProgressBar audioProgressBar;

        ImageView thumbnail;

        PlayButtonImageView mediaPlayBtn;

        TextView title;

        TextView author;

        String previewUrl;
    }

    @Override
    public void doSearch(String terms) {
        searchMusic(terms);
    }

    @Override
    public void updateUI() {
        if (listView.getAdapter() == null) {
            LogIt.i(this, "Contact list adapter null, set it");
            adapter = new MusicAdapter(this);

            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new ListViewItemClickListener());
        } else {
            adapter.notifyDataSetChanged();
        }
    }

}
