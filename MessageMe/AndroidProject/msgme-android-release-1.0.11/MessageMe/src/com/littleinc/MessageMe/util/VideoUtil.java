package com.littleinc.MessageMe.util;

import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;

/**
 * Utility code to get the duration of a video and create an image thumbnail
 * for a video.  
 * 
 * There is similar code in {@link AudioUtil}.
 */
public class VideoUtil {

    /**
     * Extracts a bitmap thumbail of the provided video file, gives it a
     * random filename suitable for uploading to S3, and stores it in the
     * application data directory.
     */
    public static File createVideoThumbnail(File videoFile) {
        Bitmap bitmap = null;
        File thumbFile = null;
        try {
            thumbFile = ImageUtil.newFile(StringUtil.getRandomFilename()
                    + MessageMeConstants.PHOTO_MESSAGE_EXTENSION);
            LogIt.d(VideoUtil.class, "createVideoThumbnail",
                    videoFile.getAbsolutePath(), thumbFile.getAbsolutePath());

            bitmap = ThumbnailUtils.createVideoThumbnail(
                    videoFile.getAbsolutePath(),
                    MediaStore.Video.Thumbnails.MINI_KIND);

            int width = MessageMeApplication.getInstance().getResources()
                    .getDimensionPixelSize(R.dimen.video_message_bubble_width);
            int height = MessageMeApplication.getInstance().getResources()
                    .getDimensionPixelSize(R.dimen.video_message_bubble_height);

            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

            if (bitmap == null) {
                LogIt.w(VideoUtil.class, "Failed to create video thumbnail",
                        videoFile.getAbsolutePath());
            } else {
                FileOutputStream out = new FileOutputStream(thumbFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            }

        } catch (Exception e) {
            LogIt.e(VideoUtil.class, e,
                    "createVideoThumbnail, error creating video thumbnail");
            thumbFile = null;
        }

        if (thumbFile != null) {
            LogIt.d(VideoUtil.class, thumbFile.toString());
        }

        return thumbFile;
    }

    /**
     * Get the duration in seconds of the provided video file.
     * 
     * If it was unable to determine the length then it returns -1.
     */
    public static int getVideoDuration(File videoFile) {

        MediaPlayer mediaPlayer = null;
        int seconds = -1;

        try {
            LogIt.d(VideoUtil.class, "getVideoDuration",
                    videoFile.getAbsolutePath());
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setDataSource(videoFile.getAbsolutePath());
            mediaPlayer.prepare();

            seconds = (int) (mediaPlayer.getDuration() / 1000);
            LogIt.d(VideoUtil.class, "Found video duration", seconds);

        } catch (Exception e) {
            LogIt.e(VideoUtil.class, e, "Error in getVideoDuration");
        } finally {
            tidyUpAudioMediaPlayer(mediaPlayer);
        }

        return seconds;
    }

    private static void tidyUpAudioMediaPlayer(MediaPlayer mp) {

        LogIt.i(VideoUtil.class, "tidyUpAudioMediaPlayer");

        // Discard the MediaPlayer
        if (mp != null) {
            mp.reset();
            mp.release();
            mp = null;
        }
    }
}
