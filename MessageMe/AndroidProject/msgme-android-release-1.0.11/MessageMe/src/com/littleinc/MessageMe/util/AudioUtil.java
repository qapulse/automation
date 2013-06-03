package com.littleinc.MessageMe.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaRecorder;
import android.os.Handler;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.widget.PlayButtonImageView;

/**
 * Utility code to play and record audio.  
 * 
 * There is similar code in {@link VideoUtil}.
 */
public class AudioUtil {
    public enum AudioPlaybackState {
        PREPARING, PAUSED, PLAYING;
    }

    public static final long COMMAND_ID_NONE = -2;

    private static MediaRecorder recorder;

    private static MediaPlayer mediaPlayer = null;

    private static OnAudioFocusChangeListener sOnAudioFocusChangeListener;

    private static AudioManager sAudioManager;

    // The commandId of the message the song is being played from
    private static long sCurrentCommandId = COMMAND_ID_NONE;

    // The current audio path being played
    private static String sCurrentAudioPath;

    // The progress bar for any audio currently loading or playing
    private static ProgressBar sCurrentProgressBar;

    private static final Handler sCurrentProgressHandler = new Handler();

    private static BaseAdapter sCurrentMediaAdapter;

    private static boolean sIsPreparing = false;

    // This is tracked by sUpdateProgressTask 
    // May want to put this in a custom Runnable class
    private static int sPreviousProgress = 0;

    // This task assumes sCurrentProgressHandler != null
    private static Runnable sUpdateProgressTask = new Runnable() {

        private ProgressBar previousProgressBar = null;

        public void run() {
            // sCurrentProgressBar == null if it goes off screen and is recycled
            if (sCurrentProgressBar != null) {
                // MediaPlayer has weird behavior where getCurrentPosition()
                // erroneously decreases when playback nears the end of a
                // short audio file, so there is logic here to ensure progress
                // is only updated if currentPosition has increased from the
                // previous iteration
                int currentProgress = mediaPlayer.getCurrentPosition();

                if (sCurrentProgressBar != previousProgressBar) {
                    sCurrentProgressBar.setMax(mediaPlayer.getDuration());
                    sCurrentProgressBar.setProgress(currentProgress);
                    previousProgressBar = sCurrentProgressBar;
                    sPreviousProgress = currentProgress;
                } else if (currentProgress > sPreviousProgress) {
                    sCurrentProgressBar.setProgress(currentProgress);
                    sPreviousProgress = currentProgress;
                }
            }

            // Task keeps running even if sCurrentProgressBar isn't visible
            sCurrentProgressHandler.postDelayed(this, 33);
        }
    };

    /**
     * Starts a new recording.
     */
    public static void startRecording(File audioFile) {
        try {
            if (FileSystemUtil.isWritableSDCardPresent()) {

                if (audioFile != null) {
                    // Stop any media that's currently playing before recording
                    pausePlaying(false);

                    // Don't check if audioFile exists, because this parameter is just a 
                    // logic reference to a file that has not being created yet.
                    // The actual file is created after stop().
                    recorder = new MediaRecorder();
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

                    recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {

                        @Override
                        public void onError(MediaRecorder mr, int what,
                                int extra) {
                            resetRecording();
                            LogIt.w(this,
                                    "Media recorder onError, resetting MediaRecorder",
                                    what, extra);
                        }
                    });

                    recorder.setOutputFile(audioFile.getAbsolutePath());
                    recorder.prepare();
                    recorder.start();

                } else {
                    LogIt.w(AudioUtil.class,
                            "Cannot record as output audio file is null");
                }
            } else {
                LogIt.w(AudioUtil.class,
                        "Cannot record as not in MEDIA_MOUNTED state");
            }
        } catch (IOException e) {
            LogIt.d(AudioUtil.class, e.getMessage());
        } catch (IllegalStateException e) {
            // Happens if the recorder.start() failed to execute because the recorder had a bad state
            LogIt.e(AudioUtil.class, e, e.getMessage());
        } catch (Exception e) {
            LogIt.e(AudioUtil.class, e, e.getMessage());
        }
    }

    /**
     * Resets the state of the MediaRecorder
     */
    public static void resetRecording() {
        if (recorder != null) {
            try {
                recorder.reset();
                recorder.release();
                recorder = null;
                LogIt.d(AudioUtil.class, "MediaRecorder has been reset");
            } catch (RuntimeException e) {
                LogIt.w(AudioUtil.class,
                        "RuntimeException resetting MediaRecorder",
                        e.getMessage());
            }
        }
    }

    /**
     * Stops a recording that has been previously started.
     */
    public static void stopRecording() {
        if (recorder != null) {
            try {
                LogIt.user(AudioUtil.class, "Stopped recording");
                recorder.stop();
                resetRecording();
            } catch (RuntimeException e) {
                // Happens if there no valid recorder to stop, or 
                // if the stop is executed right after the start
                LogIt.w(AudioUtil.class,
                        "RuntimeException stopping MediaRecorder",
                        e.getMessage());
            }
        } else {
            LogIt.w(AudioUtil.class, "Recorder is null, ignore call to stop it");
        }
    }

    /**
     *  Most people should use startPlaying().  pausePlaying() is used in
     *  SearchMusicActivity to pause all music that is currently playing
     *  regardless of URI, file, playPauseButton, etc.
     *
     *  Set abandonFocus to false if pausing audio in reaction to a transient
     *  audio focus loss (e.g. notification sound being played).  This will
     *  ensure audio automatically resumes when you regain audio focus.
     */
    public static void pausePlaying(boolean abandonFocus) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try {
                LogIt.i(AudioUtil.class, "Pausing media player");

                mediaPlayer.pause();
                if (abandonFocus) {
                    abandonAudioFocus();
                }

                sCurrentProgressHandler.removeCallbacks(sUpdateProgressTask);
                sCurrentMediaAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                LogIt.e(AudioUtil.class, e, "Error in pausePlaying");
                tidyUpAudioMediaPlayer(mediaPlayer);
            }
        }
    }

    private static void abandonAudioFocus() {
        if (sAudioManager != null) {
            sAudioManager.abandonAudioFocus(sOnAudioFocusChangeListener);
        }
        sOnAudioFocusChangeListener = null;
    }

    // mediaPlayer may be null after this 
    public static void stopPlaying() {
        if (mediaPlayer != null) {
            LogIt.i(AudioUtil.class,
                    "Stopping media player if it's playing or preparing");

            if (sIsPreparing) {
                tidyUpAudioMediaPlayer(mediaPlayer);
            } else if (mediaPlayer.isPlaying()) {
                try {
                    mediaPlayer.pause();
                    abandonAudioFocus();
                    mediaPlayer.seekTo(0);
                    sCurrentProgressHandler
                            .removeCallbacks(sUpdateProgressTask);
                    resetProgressBar(sCurrentProgressBar);
                } catch (Exception e) {
                    LogIt.e(AudioUtil.class, e, "Error in stopPlaying");
                }
            }
        }
    }

    private static void setViewState(final PlayButtonImageView playButton,
            final ProgressBar progressBar, final AudioPlaybackState state) {
        if (playButton != null) {
            playButton.setState(state);
        }

        if (progressBar != null) {
            progressBar.setIndeterminate(state == AudioPlaybackState.PREPARING);
        }
    }

    public static void setButtonState(final long commandId, final String path,
            final PlayButtonImageView playPauseView,
            final ProgressBar progressBar) {
        if (isMediaAlreadyLoaded(commandId, path) && mediaPlayer != null) {
            if (sIsPreparing) {
                setViewState(playPauseView, progressBar,
                        AudioPlaybackState.PREPARING);
            } else if (mediaPlayer.isPlaying()) {
                setViewState(playPauseView, progressBar,
                        AudioPlaybackState.PLAYING);
                progressBar.setMax(mediaPlayer.getDuration());
                progressBar.setProgress(mediaPlayer.getCurrentPosition());
                // The handler should start updating the new ProgressBar
                // once sCurrentProgressView is set to the new ProgressBar below
            } else {
                setViewState(playPauseView, progressBar,
                        AudioPlaybackState.PAUSED);
                progressBar.setMax(mediaPlayer.getDuration());
                progressBar.setProgress(mediaPlayer.getCurrentPosition());
            }

            sCurrentProgressBar = progressBar;
        } else {
            setViewState(playPauseView, progressBar, AudioPlaybackState.PAUSED);
            resetProgressBar(progressBar);
            // Playback is stopped or currently tracked views are being
            // recycled, stop tracking them
            if (sCurrentProgressBar == progressBar) {
                sCurrentProgressBar = null;
            }
        }
    }

    private static void startMediaPlayer(final BaseAdapter mediaAdapter,
            final MediaPlayer mp) {
        if (sOnAudioFocusChangeListener == null) {
            sOnAudioFocusChangeListener = new OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        // We never request AUDIOFOCUS_GAIN_TRANSIENT or AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                        // so we will probably never get it
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                        LogIt.i(AudioUtil.class,
                                "Gained audio focus; resuming playback");

                        // if sOnAudioFocusChangeListener == null then the mediaPlayer was paused by user,
                        // not by a loss of audio focus, so don't resume playback on audio focus gain
                        if (sOnAudioFocusChangeListener != null
                                && MessageMeApplication.isInForeground()) {
                            startPlaying(mediaAdapter, sCurrentCommandId,
                                    sCurrentAudioPath, null,
                                    sCurrentProgressBar);
                        }

                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        LogIt.i(AudioUtil.class, "Lost audio focus");
                        pausePlaying(true);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        LogIt.i(AudioUtil.class,
                                "Transient loss of audio focus");
                        pausePlaying(false);
                        break;
                    }
                }
            };
        }

        if (sAudioManager == null) {
            sAudioManager = (AudioManager) MessageMeApplication.getInstance()
                    .getSystemService(Context.AUDIO_SERVICE);
        }

        final int result = sAudioManager.requestAudioFocus(
                sOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        // Regardless of whether or not audio focus is granted we are still able to play
        // media.  Unless this is found to be terribly disruptive to other apps, we can leave this as is.
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            LogIt.w(AudioUtil.class, "Audio focus request failed");
        }

        sPreviousProgress = mediaPlayer.getCurrentPosition();
        sCurrentProgressHandler.post(sUpdateProgressTask);
        mp.start();
        sCurrentMediaAdapter.notifyDataSetChanged();
    }

    private static boolean isMediaAlreadyLoaded(long commandId, String path) {
        if (mediaPlayer == null) {
            return false;
        }

        if (sCurrentCommandId == AudioUtil.COMMAND_ID_NONE
                && commandId == AudioUtil.COMMAND_ID_NONE) {
            return sCurrentAudioPath != null
                    && sCurrentAudioPath.equalsIgnoreCase(path);
        }

        return commandId == sCurrentCommandId;
    }

    private static void createNewMediaPlayer(final BaseAdapter mediaAdapter) {
        LogIt.i(AudioUtil.class, "Creating media player");
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                LogIt.d(AudioUtil.class, "onCompletion", sCurrentAudioPath);
                tidyUpAudioMediaPlayer(mp);
            }
        });

        // Create callback listeners so we can prepare the media player
        // asynchronously
        mediaPlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                LogIt.w(this, "Media player onError", what, extra);

                // Returning false will cause the OnCompletionListener to 
                // be called, which contains our common tidy up logic
                return false;
            }
        });

        mediaPlayer.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                LogIt.i(this, "Media player onPrepared, starting playback");
                sIsPreparing = false;
                startMediaPlayer(mediaAdapter, mp);
            }
        });
    }

    /**
     * Plays or pauses the audio, depending on whether it is currently 
     * already playing.
     */
    public static void startPlaying(BaseAdapter adapter, long commandId,
            String uriPath, File localFile, ProgressBar progressBar) {
        try {
            // This is used for identifying which stream is actually playing
            String path = (localFile == null) ? uriPath : localFile
                    .getAbsolutePath();

            if (isMediaAlreadyLoaded(commandId, path)) {
                if (sIsPreparing) {
                    LogIt.i(AudioUtil.class, path,
                            "was loading, cancel playback");
                    tidyUpAudioMediaPlayer(mediaPlayer);
                } else if (mediaPlayer.isPlaying()) {
                    LogIt.i(AudioUtil.class, path,
                            "was already playing, pause it");
                    pausePlaying(true);
                } else {
                    LogIt.i(AudioUtil.class, path,
                            "was already paused, resume playback");
                    sCurrentProgressBar = progressBar;
                    sCurrentMediaAdapter = adapter;
                    startMediaPlayer(sCurrentMediaAdapter, mediaPlayer);
                }
            } else {
                LogIt.d(AudioUtil.class, "Start playing", path);

                if (sIsPreparing) {
                    LogIt.d(AudioUtil.class,
                            "The media player was previously loading different audio");
                    // tidyUpAudioMediaPlayer will update view state, so notifyDataSetChanged() is not necessary here
                    tidyUpAudioMediaPlayer(mediaPlayer);
                } else if (mediaPlayer != null
                        && (mediaPlayer.isPlaying() || mediaPlayer
                                .getCurrentPosition() != 0)) {
                    LogIt.d(AudioUtil.class,
                            "The media player was previously playing different audio");
                    stopPlaying();

                    // update view state on whatever was previously playing
                    if (sCurrentMediaAdapter != null) {
                        sCurrentMediaAdapter.notifyDataSetChanged();
                    }
                }

                if (mediaPlayer == null) {
                    // The media player is an expensive resource, so we should only
                    // create one
                    createNewMediaPlayer(adapter);
                } else {
                    mediaPlayer.reset();
                }

                sCurrentAudioPath = path;
                sCurrentCommandId = commandId;
                sCurrentProgressBar = progressBar;
                sCurrentMediaAdapter = adapter;
                sIsPreparing = true;
                sCurrentMediaAdapter.notifyDataSetChanged();

                if (localFile != null) {
                    // Workaround to play files from internal storage
                    LogIt.d(AudioUtil.class, "Play from local file descriptor",
                            uriPath);
                    FileInputStream fileInputStream = new FileInputStream(
                            localFile);
                    mediaPlayer.setDataSource(fileInputStream.getFD());
                    FileSystemUtil.closeInputStream(fileInputStream);
                } else {
                    LogIt.d(AudioUtil.class, "Play from URL", uriPath);
                    mediaPlayer.setDataSource(uriPath);
                }

                // Prepare the media play asynchronously, which triggers a 
                // callback to our OnPreparedListener above.  This ensures 
                // the UI thread remains responsive.
                mediaPlayer.prepareAsync();
            }
        } catch (Exception e) {
            LogIt.e(AudioUtil.class, e, "Error in startPlaying");
            tidyUpAudioMediaPlayer(mediaPlayer);
        }
    }

    private static void resetProgressBar(ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(0);
        }
    }

    /**
     *  tidyUpAudioMediaPlayer should only be called when errors have occurred
     *  and we think the previous MediaPlayer needs to be cleaned up to clear
     *  any error states.
     */
    private static void tidyUpAudioMediaPlayer(MediaPlayer mp) {
        LogIt.i(AudioUtil.class, "tidyUpAudioMediaPlayer");

        sCurrentProgressHandler.removeCallbacks(sUpdateProgressTask);

        resetProgressBar(sCurrentProgressBar);
        sCurrentProgressBar = null;

        if (mediaPlayer != null) {
            mp.release();
            mediaPlayer = null;
        }

        abandonAudioFocus();
        sAudioManager = null;
        sOnAudioFocusChangeListener = null;

        sCurrentCommandId = COMMAND_ID_NONE;
        sCurrentAudioPath = null;
        sIsPreparing = false;

        if (sCurrentMediaAdapter != null) {
            sCurrentMediaAdapter.notifyDataSetChanged();
            sCurrentMediaAdapter = null;
        }
    }
}
