package com.littleinc.MessageMe.ui;

import java.io.File;
import java.io.IOException;
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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.actionbar.ActionBarActivity;
import com.littleinc.MessageMe.bo.WebImageSearchResults;
import com.littleinc.MessageMe.bo.WebImageSearchResults.ImageData;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.util.DeviceUtil;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.ImageUtil;
import com.littleinc.MessageMe.util.SearchManager;
import com.littleinc.MessageMe.util.StringUtil;

@TargetApi(14)
public class SearchImagesActivity extends ActionBarActivity implements
        SearchManager {

    /**
     * Extra for a boolean flag to indicate whether the image search should
     * show the picture confirmation screen after the user has selected the
     * image. 
     */
    public static final String EXTRA_SHOW_CONFIRMATION_SCREEN = "image_search_show_confirmation";

    /**
     * Extra for the full path to the image file being returned.
     */
    public static final String EXTRA_OUTPUT_IMAGE_FILE = "search_images_output_file_image";

    public static final int RESULT_ERROR = 5;

    public static final int NUMBER_OF_RESULTS_TO_SHOW = 50;

    private String fileName;

    private GridView gridView;

    private EditText searchBox;

    private WebImageSearchResults webImages;

    private ImageSearchAdapter adapter;

    private boolean mConfirmAfterSelection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.image_google_search);

        gridView = (GridView) findViewById(R.id.gridView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
            actionBar.setIcon(R.drawable.actionbar_top_icon_back);
        }

        searchBox = (EditText) findViewById(R.id.search_box);
        searchBox.setOnEditorActionListener(new SearchKeyListener());
        searchBox.setHint(R.string.search_google_images);

        setTitle(getString(R.string.image_search));

        updateUI();

        // Check if we need to show the picture confirmation screen after the
        // user selects the image
        if (getIntent().hasExtra(
                SearchImagesActivity.EXTRA_SHOW_CONFIRMATION_SCREEN)) {

            mConfirmAfterSelection = getIntent().getBooleanExtra(
                    SearchImagesActivity.EXTRA_SHOW_CONFIRMATION_SCREEN,
                    mConfirmAfterSelection);
            LogIt.d(this, "mConfirmAfterSelection", mConfirmAfterSelection);
        }

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

    /**
     * Calls the API to do the actual search on the Web
     */
    private void searchImages(final String terms) {

        final ProgressDialog progressDialog = showProgressDialog(this
                .getString(R.string.loading_images_lbl));

        new BackgroundTask() {

            @Override
            public void work() {
                try {
                    webImages = RestfulClient.getInstance().searchWebImages(
                            terms);
                } catch (IOException e) {
                    LogIt.e(SearchImagesActivity.class, e, e.getMessage());
                    fail(getString(R.string.network_error_title),
                            getString(R.string.network_error));
                } catch (ResourceException e) {
                    LogIt.e(SearchImagesActivity.class, e, e.getMessage());
                    fail(getString(R.string.network_error_title),
                            getString(R.string.network_error));
                } catch (Exception e) {
                    LogIt.e(SearchImagesActivity.class, e, e.getMessage());
                    fail(getString(R.string.generic_error_title),
                            getString(R.string.unexpected_error));
                }
            }

            @Override
            public void done() {
                progressDialog.dismiss();
                if (!failed()) {
                    adapter.deleteAllItems();
                    if (webImages.getWebImagesResults().getResults().size() > 0) {
                        adapter.addItems(webImages.getWebImagesResults()
                                .getResults());
                        setVisible(R.id.emptyElement, false);
                    } else {
                        setVisible(R.id.emptyElement, true);
                    }
                    updateUI();
                } else {
                    alert(getExceptionTitle(), getExceptionMessage());
                }
            }
        };
    }

    /**
     * Listener activated when the user press the Search key in the keyboard
     */
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

    /**
     * Method defined in the search_box layout XML
     */
    @Override
    public void onSearch(View view) {
        String searchTerm = searchBox.getText().toString();
        if (!StringUtil.isEmpty(searchTerm)) {
            UIUtil.hideKeyboard(searchBox);
            LogIt.user(this, "user presses the search icon button");
            doSearch(searchTerm);
        }
    }

    private class GridViewItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            final ImageData imageData = adapter.getItem(position);
            LogIt.user(SearchImagesActivity.class, "User selected image",
                    position, imageData.getMediaUrl());

            final ProgressDialog progressDialog = showProgressDialog(SearchImagesActivity.this
                    .getString(R.string.download_image_lbl));

            new BackgroundTask() {

                File file = null;

                @Override
                public void work() {
                    try {
                        // Download the full sized image into our disk cache
                        file = ImageUtil.newFile(StringUtil.getRandomFilename()
                                + MessageMeConstants.PHOTO_MESSAGE_EXTENSION);

                        ImageUtil.downloadWebImage(file,
                                SearchImagesActivity.this,
                                imageData.getMediaUrl());
                    } catch (ResourceException e) {
                        LogIt.e(SearchImagesActivity.class, e, e.getMessage());
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    } catch (IOException e) {
                        LogIt.e(SearchImagesActivity.class, e, e.getMessage());
                        fail(getString(R.string.network_error_title),
                                getString(R.string.network_error));
                    } catch (Exception e) {
                        LogIt.e(SearchImagesActivity.class, e, e.getMessage());
                        fail(getString(R.string.generic_error_title),
                                getString(R.string.unexpected_error));
                    }
                }

                @Override
                public void done() {
                    progressDialog.dismiss();

                    if (!failed()) {
                        fileName = file.toString();

                        if (mConfirmAfterSelection) {
                            LogIt.d(SearchImagesActivity.class,
                                    "Show picture confirmation screen",
                                    fileName);
                            Intent intent = new Intent(
                                    SearchImagesActivity.this,
                                    PictureConfirmationActivity.class);
                            intent.putExtra(
                                    PictureConfirmationActivity.EXTRA_INPUT_IMAGE_FILE,
                                    fileName);
                            startActivityForResult(
                                    intent,
                                    MessageMeConstants.CONFIRMATION_PAGE_REQUEST_CODE);
                        } else {
                            LogIt.d(SearchImagesActivity.class,
                                    "Don't show picture confirmation screen, return to previous activity",
                                    fileName);

                            Intent intent = new Intent();

                            intent.putExtra(
                                    SearchImagesActivity.EXTRA_OUTPUT_IMAGE_FILE,
                                    fileName);

                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    } else {
                        setResult(RESULT_ERROR);
                        finish();
                    }
                }
            };
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case MessageMeConstants.CONFIRMATION_PAGE_REQUEST_CODE:
            switch (resultCode) {
            case RESULT_OK:
                if (data != null) {
                    if (data.hasExtra(PictureConfirmationActivity.EXTRA_OUTPUT_CONFIRMED_IMAGE_FILE)) {

                        String fileName = data
                                .getExtras()
                                .getString(
                                        PictureConfirmationActivity.EXTRA_OUTPUT_CONFIRMED_IMAGE_FILE);
                        LogIt.d(this,
                                "onActivityResult for pressing Confirm on Picture confirmation screen",
                                fileName);

                        Intent intent = new Intent();
                        intent.putExtra(
                                PictureConfirmationActivity.EXTRA_OUTPUT_CONFIRMED_IMAGE_FILE,
                                fileName);
                        setResult(RESULT_OK, intent);
                    } else if (data
                            .hasExtra(PictureConfirmationActivity.EXTRA_OUTPUT_EDIT_IMAGE_FILE)) {

                        String fileName = data
                                .getExtras()
                                .getString(
                                        PictureConfirmationActivity.EXTRA_OUTPUT_EDIT_IMAGE_FILE);
                        LogIt.d(this,
                                "onActivityResult for pressing Edit on Picture confirmation screen",
                                fileName);

                        Intent intent = new Intent();
                        intent.putExtra(
                                DoodleComposerActivity.EXTRA_DOODLE_BACKGROUND_FILE,
                                fileName);
                        setResult(RESULT_OK, intent);
                    } else {
                        LogIt.w(SearchImagesActivity.class,
                                "Unexpected intent data");
                    }
                } else {
                    LogIt.w(this, "Intent data is null");
                    setResult(RESULT_ERROR);
                }
                break;
            }
            break;
        case RESULT_CANCELED:
            LogIt.d(this, "User canceled the picture confirmation");
            break;
        default:
            LogIt.w(this, "Unrecognized user action");
            break;
        }
        finish();
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class ImageSearchAdapter extends BaseAdapter {

        private List<ImageData> data = new ArrayList<ImageData>();

        private ImageLoader mImageLoader;

        private ViewHolder holder;

        private LayoutInflater mInflater;

        private int width;

        private int height;

        public ImageSearchAdapter(Context context) {
            mImageLoader = ImageLoader.getInstance();
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            this.width = context.getResources().getDimensionPixelSize(
                    R.dimen.web_image_search_width);
            this.height = context.getResources().getDimensionPixelSize(
                    R.dimen.web_image_search_height);
        }

        public void addItems(List<ImageData> items) {
            this.data = items;
            notifyDataSetChanged();
        }

        public void deleteAllItems() {
            this.data.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public ImageData getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            holder = new ViewHolder();

            if (convertView == null) { // if it's not recycled, initialize some attributes                
                convertView = mInflater.inflate(R.layout.grid_view_image, null);
                holder.image = (ImageView) convertView
                        .findViewById(R.id.grid_image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder = getImageInformation(position);

            return convertView;
        }

        private ViewHolder getImageInformation(int position) {
            holder.image.setImageBitmap(null);

            // Only displays the Thumbnail of the full sized image.
            String thumbnailUrl = data.get(position).getThumbnail()
                    .getMediaUrl();
            String md5Key = data.get(position).getMd5Key();

            // Adds a tag to the imageView in order for it to not be recycled
            // and maintain the view content when the user scrolls up or down.
            holder.image.setTag(md5Key);

            mImageLoader.displayWebImage(md5Key, thumbnailUrl, holder.image,
                    width, height);

            return holder;
        }
    }

    public static class ViewHolder {
        public ImageView image;
    }

    @Override
    public void doSearch(String terms) {
        searchImages(terms);
    }

    @Override
    public void updateUI() {
        if (gridView.getAdapter() == null) {
            LogIt.i(this, "Image search GridView adapter null, set it");
            adapter = new ImageSearchAdapter(this);
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener(new GridViewItemClickListener());
        } else {
            adapter.notifyDataSetChanged();
        }
    }
}