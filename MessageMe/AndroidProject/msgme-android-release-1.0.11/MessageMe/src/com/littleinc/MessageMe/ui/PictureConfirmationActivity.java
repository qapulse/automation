package com.littleinc.MessageMe.ui;

import java.io.File;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeActivity;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.util.ImageLoader;

public class PictureConfirmationActivity extends MessageMeActivity {

    /**
     * Extra for passing in an image file to the picture confirmation 
     * activity.
     */
    public static final String EXTRA_INPUT_IMAGE_FILE = "picture_confirmation_input_image_file";

    /**
     * Extra for passing in an video file to the video confirmation 
     * activity.
     */
    public static final String EXTRA_INPUT_VIDEO_FILE = "video_confirmation_input_video_file";

    /**
     * Extra for passing in a boolean flag to say that a video filename 
     * is included in the intent data.
     */
    public static final String EXTRA_INPUT_IS_VIDEO = "picture_confirmation_input_is_image_file";

    /**
     * Extra for passing out a boolean flag to say that a video filename 
     * is included in the intent data.
     */
    public static final String EXTRA_OUTPUT_IS_VIDEO = "picture_confirmation_output_is_video_file";

    /**
     * Extra for the full path to the image file to edit in a doodle.  
     */
    public static final String EXTRA_OUTPUT_EDIT_IMAGE_FILE = "picture_confirmation_edit_image_file";

    /**
     * Extra for the full path to the image file being returned by the picture 
     * confirmation screen.  
     * 
     * This is only included when the user chose to Confirm the selection.
     */
    public static final String EXTRA_OUTPUT_CONFIRMED_IMAGE_FILE = "picture_confirmation_confirm_file_image";

    private ImageLoader imageLoader;

    private ImageButton pictureCancel;

    private ImageButton pictureConfirm;

    private ImageButton pictureEdit;

    private FrameLayout pictureContent;

    private String fileName;

    private boolean isVideo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_confirmation_layout);

        imageLoader = ImageLoader.getInstance();

        pictureCancel = (ImageButton) findViewById(R.id.picture_confirmation_cancel);
        pictureConfirm = (ImageButton) findViewById(R.id.picture_confirmation_accept);
        pictureEdit = (ImageButton) findViewById(R.id.picture_confirmation_edit);
        pictureContent = (FrameLayout) findViewById(R.id.picture_confirmation_content);

        pictureConfirm.setOnClickListener(new ConfirmPictureClickListener());
        pictureCancel.setOnClickListener(new CancelPictureClickListener());
        pictureEdit.setOnClickListener(new EditPictureClickListener());

        openMedia(getIntent());
    }

    private void openMedia(Intent data) {
        ImageView imageView = new ImageView(this);
        VideoView videoView = new VideoView(this);

        int screenWidth = MessageMeApplication.getScreenSize().getWidth();
        int screenHeight = MessageMeApplication.getScreenSize().getHeight();

        if (data.hasExtra(PictureConfirmationActivity.EXTRA_INPUT_IMAGE_FILE)) {
            // Intent data is an image 
            isVideo = false;
            fileName = data
                    .getStringExtra(PictureConfirmationActivity.EXTRA_INPUT_IMAGE_FILE);

            pictureContent.addView(imageView);

            imageLoader.displayImage(fileName, imageView, screenWidth,
                    screenHeight, true);
        } else if (data
                .hasExtra(PictureConfirmationActivity.EXTRA_INPUT_IS_VIDEO)) {
            // Intent data is a video
            isVideo = true;
            fileName = data
                    .getStringExtra(PictureConfirmationActivity.EXTRA_INPUT_VIDEO_FILE);
            File videoFile = new File(fileName);

            videoView.setVideoURI(Uri.fromFile(videoFile));
            videoView.setMediaController(new MediaController(this));
            videoView.requestFocus();

            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT, Gravity.CENTER);

            videoView.setLayoutParams(params);

            pictureEdit.setVisibility(View.INVISIBLE);
            pictureContent.addView(videoView);
            videoView.start();
        }
    }

    /**
     * Click listener to confirm a picture     
     */
    private class ConfirmPictureClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            LogIt.user(PictureConfirmationActivity.class,
                    "Pressed Yes on picture/video confirmation screen",
                    fileName);

            if (isVideo) {
                Intent videoData = new Intent();

                videoData
                        .putExtra(
                                PictureConfirmationActivity.EXTRA_OUTPUT_IS_VIDEO,
                                true);
                videoData.putExtra(
                        PictureConfirmationActivity.EXTRA_INPUT_VIDEO_FILE,
                        fileName);

                setResult(RESULT_OK, videoData);
            } else {
                Intent intent = new Intent();
                intent.putExtra(
                        PictureConfirmationActivity.EXTRA_OUTPUT_CONFIRMED_IMAGE_FILE,
                        fileName);
                setResult(RESULT_OK, intent);
            }
            finish();
        }
    }

    /**
     * Click listener to cancel the confirmation     
     */
    private class CancelPictureClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            LogIt.user(PictureConfirmationActivity.class,
                    "Pressed Cancel on picture/video confirmation screen");

            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * Click listener that opens the doodle composer to edit the image     
     */
    private class EditPictureClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            LogIt.user(PictureConfirmationActivity.class,
                    "Pressed Doodle Edit on picture confirmation screen",
                    fileName);

            if (!isVideo) {
                Intent intent = new Intent();
                intent.putExtra(
                        PictureConfirmationActivity.EXTRA_OUTPUT_EDIT_IMAGE_FILE,
                        fileName);
                setResult(RESULT_OK, intent);
                finish();
            } else {
                LogIt.e(PictureConfirmationActivity.class,
                        "There should be no Edit option on video confirmation screen");
            }
        }
    }
}
