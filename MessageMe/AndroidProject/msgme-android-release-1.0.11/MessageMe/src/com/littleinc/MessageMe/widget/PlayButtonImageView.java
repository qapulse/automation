package com.littleinc.MessageMe.widget;

import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.util.AudioUtil.AudioPlaybackState;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class PlayButtonImageView extends ImageView {
    AudioPlaybackState mCurrentState;

    public PlayButtonImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setState(AudioPlaybackState.PAUSED);
    }

    public void setState(AudioPlaybackState state) {
        if (state == mCurrentState) {
            return;
        }

        switch (state) {
            case PREPARING:
                setImageDrawable(getResources().getDrawable(
                        R.drawable.messagesview_bubble_audio_player_base_other));
                break;
            case PAUSED:
                setImageDrawable(getResources().getDrawable(
                        R.drawable.play_button));
                break;
            case PLAYING:
                setImageDrawable(getResources().getDrawable(
                        R.drawable.pause_button));
                break;
        }

        mCurrentState = state;
    }
}
