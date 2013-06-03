package com.littleinc.MessageMe.ui;

import static com.littleinc.MessageMe.ui.SearchImagesActivity.RESULT_ERROR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageUtil;
import com.littleinc.MessageMe.util.StringUtil;
import com.littleinc.MessageMe.widget.ColorPicker;
import com.littleinc.MessageMe.widget.ColorPicker.DoodleBrushColor;
import com.littleinc.MessageMe.widget.ColorPicker.DoodleBrushSize;
import com.littleinc.MessageMe.widget.DrawerView;

@TargetApi(14)
public class DoodleComposerActivity extends ActionBarActivity {

    /**
     * Extra for the full path to the file to use as the doodle background.
     * This must be a local file. 
     */
    public static final String EXTRA_DOODLE_BACKGROUND_FILE = "doodle_compose_background_file";

    private static final int SELECT_PICTURE_RQC = 55;

    private static final int TAKE_PICTURE_RQC = 56;

    private static final int GOOGLE_IMAGE_RQC = 62;

    private DrawerView drawerView;

    private ColorPicker colorPicker;

    private LinearLayout sizePicker;

    private ImageButton brushButton;

    private ImageButton eraserButton;

    private ImageButton picturePicker;

    private OnPaintUpdateListener listener;

    private File mediaImageOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.doodle_composer);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        listener = new DoodleComposerRefresh();

        drawerView = (DrawerView) findViewById(R.id.drawer_view);
        sizePicker = (LinearLayout) findViewById(R.id.size_picker);
        colorPicker = (ColorPicker) findViewById(R.id.color_picker);
        brushButton = (ImageButton) findViewById(R.id.brush_button);
        eraserButton = (ImageButton) findViewById(R.id.eraser_button);
        picturePicker = (ImageButton) findViewById(R.id.picture_button);

        brushButton.setSelected(true);
        findViewById(R.id.size00_button).setSelected(true);

        colorPicker.setListener(listener);
        brushButton.setOnClickListener(new BrushButtonClickListener());
        eraserButton.setOnClickListener(new EraserButtonClickListener());
        picturePicker.setVisibility(View.VISIBLE);

        int bounds = MessageMeApplication.getScreenSize().getWidth();

        drawerView.getLayoutParams().height = bounds;

        if (savedInstanceState != null) {

            String mediaImageOutputPath = savedInstanceState
                    .getString(MessageMeConstants.MEDIA_OUTPUT_PATH_KEY);

            if (mediaImageOutputPath != null) {
                mediaImageOutput = new File(
                        savedInstanceState
                                .getString(MessageMeConstants.MEDIA_OUTPUT_PATH_KEY));
            }
        }

        init();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (mediaImageOutput != null) {
            outState.putString(MessageMeConstants.MEDIA_OUTPUT_PATH_KEY,
                    mediaImageOutput.getAbsolutePath());
        }

        super.onSaveInstanceState(outState);
    }

    private void init() {
        String originalPhotoKey = getIntent().getStringExtra(
                DoodleComposerActivity.EXTRA_DOODLE_BACKGROUND_FILE);

        if (originalPhotoKey != null) {
            try {
                LogIt.d(this, "Set doodle background image", originalPhotoKey);
                drawerView.drawNewBackground(originalPhotoKey);
            } catch (Exception e) {
                LogIt.w(this, e);
                Toast.makeText(this, R.string.background_is_not_available,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.doodler_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            LogIt.user(DoodleComposerActivity.class,
                    "Action bar back button pressed");
            if (drawerView.isDoodleCreated()) {
                createConfirmationDialog();
            } else {
                finish();
            }
        } else if (itemId == R.id.menu_send) {
            LogIt.user(DoodleComposerActivity.class, "Send Doodle");
            Intent intent = getIntent();
            String fileName = saveDoodle();

            intent.putExtra(MessageMeConstants.FILE_NAME_KEY, fileName);
            setResult(RESULT_OK, intent);

            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private String saveDoodle() {
        String fileName = StringUtil.getRandomFilename()
                + MessageMeConstants.PHOTO_MESSAGE_EXTENSION;
        File file = ImageUtil.newFile(fileName);

        try {
            FileOutputStream fos = new FileOutputStream(file);

            Bitmap doodle = drawerView.getDoodle();
            Bitmap finalDoodle = Bitmap.createBitmap(doodle.getWidth(),
                    doodle.getHeight(), Bitmap.Config.ARGB_8888);

            Canvas tempCanvas = new Canvas(finalDoodle);

            tempCanvas.drawColor(getResources().getColor(
                    R.color.doodle_canvas_bg_color));
            tempCanvas.drawBitmap(doodle, 0, 0, new Paint());

            finalDoodle.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.flush();
            fos.close();

            finalDoodle.recycle();
            finalDoodle = null;
        } catch (IOException e) {
            LogIt.e(this, e);
        } catch (Exception e) {
            LogIt.e(this, e, e.getMessage());
        }

        return file.getAbsolutePath();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            final Intent data) {
        switch (requestCode) {
        case SELECT_PICTURE_RQC:
            switch (resultCode) {
            case RESULT_OK:
                final ProgressDialog progressDialog = showProgressDialog(R.string.loading);

                new BackgroundTask() {

                    String fileName = null;

                    @Override
                    public void work() {
                        try {
                            fileName = FileSystemUtil
                                    .getMediaContentFromIntent(
                                            DoodleComposerActivity.this, data,
                                            true);
                        } catch (MalformedURLException e) {
                            LogIt.e(DoodleComposerActivity.class, e);
                            fail(getString(R.string.generic_error_title),
                                    getString(R.string.unexpected_error));
                        } catch (IOException e) {
                            LogIt.e(DoodleComposerActivity.class, e);
                            fail(getString(R.string.generic_error_title),
                                    getString(R.string.network_error));
                        } catch (Exception e) {
                            // We aren't using Restlet here, so no need to handle
                            // a ResourceException
                            LogIt.e(DoodleComposerActivity.class, e,
                                    e.getMessage());
                            fail(getString(R.string.generic_error_title),
                                    getString(R.string.unexpected_error));
                        }
                    }

                    @Override
                    public void done() {
                        progressDialog.dismiss();
                        if (!failed()) {
                            if (fileName != null) {
                                LogIt.user(DoodleComposerActivity.class,
                                        "Background changed to existing image",
                                        fileName);
                                try {
                                    drawerView.drawNewBackground(fileName);
                                } catch (Exception e) {
                                    LogIt.w(this, e, e.getMessage());
                                    toast(R.string.background_is_not_available);
                                }
                            } else {
                                LogIt.e(DoodleComposerActivity.class,
                                        "Failed to read file from source, File is null");
                            }
                        } else {
                            UIUtil.alert(DoodleComposerActivity.this,
                                    getExceptionTitle(), getExceptionMessage());
                        }
                    }
                };
                break;
            default:
                LogIt.w(this,
                        "onActivityResult unhandled intent for SELECT_PICTURE_RQC");
                break;
            }
            break;
        case TAKE_PICTURE_RQC:
            switch (resultCode) {
            case RESULT_OK:
                if (mediaImageOutput != null) {
                    String filePath = mediaImageOutput.getAbsolutePath();

                    if (!(new File(filePath).exists())) {
                        LogIt.w(this, "Photo file does not exist");
                        filePath = null;
                    } else {
                        LogIt.user(this,
                                "Background changed to newly taken photo");

                        // Rotates the file if required
                        filePath = ImageUtil.rotatePicture(filePath,
                                DoodleComposerActivity.this);

                        Bitmap backgroundPhoto = ImageUtil
                                .decodeSampledBitmapFromFile(filePath,
                                        drawerView.getWidth(),
                                        drawerView.getHeight());
                        drawerView.drawNewBackground(backgroundPhoto);

                        if (mediaImageOutput.delete()) {
                            LogIt.d(DoodleComposerActivity.this,
                                    "Photo deleted from storage");
                        } else {
                            LogIt.w(DoodleComposerActivity.this,
                                    "Couldn't delete photo from storage");
                        }
                    }
                } else {
                    LogIt.w(this,
                            "Missing mediaImageOutput in TAKE_PHOTO_REQUEST_CODE result");
                }
                break;
            default:
                LogIt.w(this,
                        "onActivityResult unhandled intent for TAKE_PICTURE_RQC");
                break;
            }
            break;
        case GOOGLE_IMAGE_RQC:
            switch (resultCode) {
            case RESULT_OK:
                // By this point the image will already have been downloaded
                // into our local cache, so we don't need to download anything 
                // in a background task
                String path = data
                        .getStringExtra(SearchImagesActivity.EXTRA_OUTPUT_IMAGE_FILE);

                try {
                    LogIt.user(this, "Background changed to Google image", path);
                    drawerView.drawNewBackground(path);
                } catch (Exception e) {
                    LogIt.e(this, e);
                }
                break;
            case RESULT_ERROR:
                toast(R.string.error_downloading_image);
                break;
            default:
                LogIt.w(this,
                        "onActivityResult unhandled intent for GOOGLE_IMAGE_RQC");
                break;
            }
            break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void clear(View v) {
        drawerView.clear();
    }

    public void undo(View v) {
        drawerView.undo();
    }

    public void addBackground(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.background_dialog_title);
        builder.setItems(R.array.background_dialog_options,
                new BackgroundDialogClickListener());

        builder.create().show();
    }

    public void selectedBrushSize(View v) {
        int id = v.getId();

        if (id == R.id.size00_button) {
            listener.onPaintUpdate(DoodleBrushSize.SIZE_00);
        } else if (id == R.id.size01_button) {
            listener.onPaintUpdate(DoodleBrushSize.SIZE_01);
        } else if (id == R.id.size02_button) {
            listener.onPaintUpdate(DoodleBrushSize.SIZE_02);
        } else if (id == R.id.size03_button) {
            listener.onPaintUpdate(DoodleBrushSize.SIZE_03);
        }

        for (int i = 0; i < sizePicker.getChildCount(); i++) {
            sizePicker.getChildAt(i).setSelected(false);
        }

        v.setSelected(true);
        sizePicker.setVisibility(View.GONE);
    }

    private class BackgroundDialogClickListener implements
            DialogInterface.OnClickListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = null;

            switch (which) {
            case 0:
                LogIt.user(DoodleComposerActivity.class,
                        "Set doodle background from Google Images");
                intent = new Intent(DoodleComposerActivity.this,
                        SearchImagesActivity.class);
                intent.putExtra(
                        SearchImagesActivity.EXTRA_SHOW_CONFIRMATION_SCREEN,
                        false);
                startActivityForResult(intent, GOOGLE_IMAGE_RQC);
                break;
            case 1:
                if (DeviceUtil.isCameraAvailable(DoodleComposerActivity.this)) {
                    if (DeviceUtil
                            .isImageCaptureAppAvailable(DoodleComposerActivity.this)) {
                        LogIt.user(DoodleComposerActivity.class,
                                "Set doodle background from Take New photo");
                        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                        mediaImageOutput = ImageUtil
                                .getTemporaryExternalFile(MessageMeConstants.PHOTO_MESSAGE_EXTENSION);

                        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(mediaImageOutput));

                        LogIt.d(this, "Take photo to file", mediaImageOutput);

                        startActivityForResult(intent, TAKE_PICTURE_RQC);
                    } else {
                        UIUtil.alert(DoodleComposerActivity.this,
                                R.string.no_image_capture_app_title,
                                R.string.no_image_capture_app_message);
                    }
                } else {
                    UIUtil.alert(DoodleComposerActivity.this,
                            R.string.no_camera_title,
                            R.string.no_camera_message);
                }
                break;
            case 2:
                LogIt.user(DoodleComposerActivity.class,
                        "Set doodle background from Choose Existing photo");
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");

                startActivityForResult(intent, SELECT_PICTURE_RQC);
                break;
            }

            dialog.dismiss();
        }
    }

    private class BrushButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            brushButton.setSelected(true);
            eraserButton.setSelected(false);

            LayoutParams params = (LayoutParams) sizePicker.getLayoutParams();
            params.leftMargin = getResources().getDimensionPixelSize(
                    R.dimen.size_picker_margin_brush);

            sizePicker.setVisibility(View.VISIBLE);
            sizePicker.setLayoutParams(params);

            listener.onPaintUpdate(colorPicker.getSelectedColor());
        }
    }

    private class EraserButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            brushButton.setSelected(false);
            eraserButton.setSelected(true);

            LayoutParams params = (LayoutParams) sizePicker.getLayoutParams();
            params.leftMargin = getResources().getDimensionPixelSize(
                    R.dimen.size_picker_margin_eraser);

            sizePicker.setVisibility(View.VISIBLE);
            sizePicker.setLayoutParams(params);

            listener.onPaintUpdate(DoodleBrushColor.TRANSPARENT);
        }
    }

    private class DoodleComposerRefresh implements OnPaintUpdateListener {

        @Override
        public void onPaintUpdate(DoodleBrushColor doodleBrushColor) {
            if (DoodleBrushColor.TRANSPARENT != doodleBrushColor) {
                colorPicker.setSelectedColor(doodleBrushColor);
            }

            drawerView.onPaintUpdate(doodleBrushColor);
        }

        @Override
        public void onPaintUpdate(DoodleBrushSize doodleBrushSize) {
            drawerView.onPaintUpdate(doodleBrushSize);
        }
    }

    public interface OnPaintUpdateListener {
        void onPaintUpdate(DoodleBrushColor doodleBrushColor);

        void onPaintUpdate(DoodleBrushSize doodleBrushSize);
    }

    @Override
    public void onBackPressed() {
        if (drawerView.isDoodleCreated()) {
            createConfirmationDialog();
        } else {
            super.onBackPressed();
        }

    }

    /**
     * Creates the dialog to confirm exiting the Doodle Composer
     */
    public void createConfirmationDialog() {
        UIUtil.confirmYesNo(DoodleComposerActivity.this,
                R.string.doodle_exit_title, R.string.doodle_exit_msg,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogIt.user(this,
                                "User discards the doodle, exits DoodleComposer");
                        dialog.dismiss();
                        finish();
                    }
                }, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogIt.user(this,
                                "User discards the dialog, remains in DoodleComposer");
                        dialog.dismiss();
                    }
                });
    }
}