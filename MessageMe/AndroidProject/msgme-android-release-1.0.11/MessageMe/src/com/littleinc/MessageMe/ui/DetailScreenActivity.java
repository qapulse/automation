package com.littleinc.MessageMe.ui;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeActivity;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.DoodleMessage;
import com.littleinc.MessageMe.bo.DoodlePicMessage;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.Message;
import com.littleinc.MessageMe.bo.PhotoMessage;
import com.littleinc.MessageMe.util.DatabaseTask;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.ImageUtil;
import com.littleinc.MessageMe.util.MessageUtil;
import com.littleinc.MessageMe.util.PinchToZoomView;

public class DetailScreenActivity extends MessageMeActivity {

    private TextView actionBtn;

    private FrameLayout content;

    private ImageButton closeBtn;

    private ImageButton exportsBtn;

    private IMessage currentMessage;

    private ImageLoader imageLoader = ImageLoader.getInstance();

    private String imageKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_screen);

        actionBtn = (TextView) findViewById(R.id.detail_screen_action);
        closeBtn = (ImageButton) findViewById(R.id.detail_screen_close);
        content = (FrameLayout) findViewById(R.id.detail_screen_content);
        exportsBtn = (ImageButton) findViewById(R.id.detail_screen_exports);

        new DatabaseTask() {

            @Override
            public void work() {
                long selectedMessageId = getIntent().getLongExtra(
                        Message.ID_COLUMN, -1);
                int selectedMessageType = getIntent().getIntExtra(
                        Message.TYPE_COLUMN, -1);

                currentMessage = MessageUtil
                        .newMessageInstanceByType(IMessageType
                                .valueOf(selectedMessageType));
                currentMessage.setId(selectedMessageId);
                currentMessage.load();
            }

            @Override
            public void done() {
                init();
            }
        };
    }

    private void init() {

        int screenWidth = MessageMeApplication.getScreenSize().getWidth();
        int screenHeight = MessageMeApplication.getScreenSize().getHeight();

        PinchToZoomView imageView = new PinchToZoomView(this);

        switch (currentMessage.getType()) {
        case LOCATION:
            actionBtn.setText(R.string.detail_screen_open_map);
            actionBtn
                    .setCompoundDrawablesWithIntrinsicBounds(
                            getResources()
                                    .getDrawable(
                                            R.drawable.detailview_actionbar_button_icon_location),
                            null, null, null);
            break;
        case PHOTO:
            PhotoMessage photoMessage = (PhotoMessage) currentMessage;

            actionBtn.setText(R.string.detail_screen_reply_doodle);
            actionBtn.setOnClickListener(new ReplyWithDoodleClickListener(
                    photoMessage.getImageKey()));
            actionBtn
                    .setCompoundDrawablesWithIntrinsicBounds(
                            getResources()
                                    .getDrawable(
                                            R.drawable.detailview_actionbar_button_icon_doodle),
                            null, null, null);

            content.addView(imageView);

            setImageKey(photoMessage.getImageKey());
            imageLoader.displayImage(photoMessage.getImageKey(), imageView,
                    screenWidth, screenHeight, true);
            break;
        case DOODLE:
            DoodleMessage doodleMessage = (DoodleMessage) currentMessage;

            actionBtn.setText(R.string.detail_screen_reply_doodle);
            actionBtn.setOnClickListener(new ReplyWithDoodleClickListener(
                    doodleMessage.getImageKey()));
            actionBtn
                    .setCompoundDrawablesWithIntrinsicBounds(
                            getResources()
                                    .getDrawable(
                                            R.drawable.detailview_actionbar_button_icon_doodle),
                            null, null, null);

            content.addView(imageView);

            setImageKey(doodleMessage.getImageKey());
            imageLoader.displayImage(doodleMessage.getImageKey(), imageView,
                    screenWidth, screenHeight, true);
            break;

        case DOODLE_PIC:
            DoodlePicMessage doodlePicMessage = (DoodlePicMessage) currentMessage;

            actionBtn.setText(R.string.detail_screen_reply_doodle);
            actionBtn.setOnClickListener(new ReplyWithDoodleClickListener(
                    doodlePicMessage.getImageKey()));
            actionBtn
                    .setCompoundDrawablesWithIntrinsicBounds(
                            getResources()
                                    .getDrawable(
                                            R.drawable.detailview_actionbar_button_icon_doodle),
                            null, null, null);

            content.addView(imageView);

            setImageKey(doodlePicMessage.getImageKey());
            imageLoader.displayImage(doodlePicMessage.getImageKey(), imageView,
                    screenWidth, screenHeight, true);
            break;
        default:
            break;
        }

        closeBtn.setOnClickListener(new CloseBtnClickListener());
        exportsBtn.setOnClickListener(new ExportsBtnClickListener());

        registerForContextMenu(exportsBtn);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();

        switch (currentMessage.getType()) {
        case PHOTO:
            inflater.inflate(R.menu.detail_screen_pm_menu, menu);
            break;
        case DOODLE:
            inflater.inflate(R.menu.detail_screen_pm_menu, menu);
            break;
        case DOODLE_PIC:
            inflater.inflate(R.menu.detail_screen_pm_menu, menu);
            break;
        default:
            break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.forward_pm:
            Intent intent = new Intent(this, MessageContactActivity.class);

            intent.putExtra(Message.ID_COLUMN, currentMessage.getId());
            intent.putExtra(Message.TYPE_COLUMN, currentMessage.getType()
                    .getProtobufNumber());

            startActivity(intent);
            finish();
            return true;
        case R.id.save_pm:
            saveToGallery();
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Click listener for Reply with doodle option
     * Applies for photo, doodles and doodle pic messages
     * 
     * Checks before sending the result back to the chatActivity
     * checks if the image has been added to the cache, in this way 
     * we ensure the image has been already downloaded
     *
     */
    private class ReplyWithDoodleClickListener implements OnClickListener {

        private String imageKey;

        public ReplyWithDoodleClickListener(String imageKey) {
            this.imageKey = imageKey;
        }

        @Override
        public void onClick(View v) {
            LogIt.user(DetailScreenActivity.class, "Reply with Doodle pressed");

            Intent data = new Intent();

            File file = ImageUtil.getFile(imageKey);

            // Checks if the image has been added to the memory
            if (imageLoader.containsImage(imageKey)) {
                data.putExtra(
                        DoodleComposerActivity.EXTRA_DOODLE_BACKGROUND_FILE,
                        file.toString());

                setResult(MessageMeConstants.DETAIL_REPLY_DOODLE_RESULT_CODE,
                        data);
                finish();
            } else {
                LogIt.d(this,
                        "User tried to open the doodle composer with an image that is not downloaded yet");
                toast(R.string.detail_screen_wait_download);
            }
        }
    }

    private class ExportsBtnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            LogIt.user(DetailScreenActivity.class, "Share button pressed");
            openContextMenu(v);
        }
    }

    private class CloseBtnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            LogIt.user(DetailScreenActivity.class, "Close button pressed");
            finish();
        }
    }

    private void saveToGallery() {
        imageLoader.storeIntoGallery(DetailScreenActivity.this, getImageKey(),
                "", "");
    }

    private void setImageKey(String mImageKey) {
        this.imageKey = mImageKey;
    }

    private String getImageKey() {
        return this.imageKey;
    }
}