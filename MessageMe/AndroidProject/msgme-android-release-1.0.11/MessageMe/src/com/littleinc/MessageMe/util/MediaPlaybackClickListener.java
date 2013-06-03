package com.littleinc.MessageMe.util;

import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;

public class MediaPlaybackClickListener implements OnClickListener {

    private Context mContext;

    private BaseAdapter mMediaAdapter;

    private String mTrackName;

    private String mPreviewUrl;

    private ProgressBar mProgressBar;

    private long mCommandId;

    public MediaPlaybackClickListener(Context context,
            BaseAdapter mediaAdapter, long commandId, String trackName,
            String previewUrl, ProgressBar progressBar) {
        mMediaAdapter = mediaAdapter;
        mCommandId = commandId;
        mTrackName = trackName;
        mPreviewUrl = previewUrl;
        mProgressBar = progressBar;
        mContext = context;
    }

    @Override
    public void onClick(View v) {
        LogIt.user(this, "Play/pause pressed for song", mTrackName);

        if (NetUtil.checkInternetConnection()) {
            AudioUtil.startPlaying(mMediaAdapter, mCommandId, mPreviewUrl,
                    null, mProgressBar);

            mMediaAdapter.notifyDataSetChanged();
        } else {
            // The Android media player does not cope nicely with
            // being offline, so we display an alert if there is
            // no connection.
            UIUtil.alert(mContext,
                    mContext.getString(R.string.network_error_title),
                    mContext.getString(R.string.network_error));
        }
    }

}
