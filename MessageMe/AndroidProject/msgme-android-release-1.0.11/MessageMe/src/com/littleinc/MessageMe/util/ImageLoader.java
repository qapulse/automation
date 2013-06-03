package com.littleinc.MessageMe.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.restlet.resource.ResourceException;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.text.TextUtils;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.Toast;

import com.amazonaws.AmazonClientException;
import com.coredroid.util.BackgroundTask;
import com.coredroid.util.Dimension;
import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.AbstractMessage;
import com.littleinc.MessageMe.bo.Contact;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.chat.MediaManager;

public class ImageLoader {

    private Context context;

    private ImageCache memoryCache;

    /** Message bubble images cache location */
    public static final String CACHE_FOLDER_MESSAGE_BUBBLES = "bubbles";

    public static final String CACHE_VERSION_MESSAGE_BUBBLES = "v1";

    /** Profile photo cache location */
    public static final String CACHE_FOLDER_PROFILE_PHOTOS = "profiles";

    public static final String CACHE_VERSION_PROFILE_PHOTOS = "v1";

    /** Media cache location */
    public static final String CACHE_FOLDER_MEDIA = "media";

    public static final String CACHE_VERSION_MEDIA = "v1";

    private static ImageLoader instance;

    public static ImageLoader getInstance() {
        if (instance == null) {
            instance = new ImageLoader();
        }

        return instance;
    }

    private ImageLoader() {
        context = MessageMeApplication.getInstance().getApplicationContext();

        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        int memoryClassBytes = activityManager.getMemoryClass() * 1024 * 1024;

        // The Android development docs suggest using 1/8th of the available
        // memory for the memory cache.
        //  http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
        memoryCache = new ImageCache(memoryClassBytes / 8);
    }

    public static File getAppCacheDirProfiles() {
        File cacheFolder = ImageUtil
                .getInternalFilesDir(ImageLoader.CACHE_FOLDER_PROFILE_PHOTOS
                        + File.separator
                        + ImageLoader.CACHE_VERSION_PROFILE_PHOTOS);
        createFolderIfRequired(cacheFolder);
        return cacheFolder;
    }

    public static File getAppCacheDirBubbles() {

        File cacheFolder = ImageUtil
                .getInternalFilesDir(ImageLoader.CACHE_FOLDER_MESSAGE_BUBBLES
                        + File.separator
                        + ImageLoader.CACHE_VERSION_MESSAGE_BUBBLES);
        createFolderIfRequired(cacheFolder);
        return cacheFolder;
    }

    public static File getAppCacheDirMedia() {

        File cacheFolder = ImageUtil
                .getInternalFilesDir(ImageLoader.CACHE_FOLDER_MEDIA
                        + File.separator + ImageLoader.CACHE_VERSION_MEDIA);
        createFolderIfRequired(cacheFolder);
        return cacheFolder;
    }

    public static void createFolderIfRequired(File cacheFolder) {

        if (!cacheFolder.exists()) {

            // Create parent folders if required
            if (cacheFolder.mkdirs()) {
                LogIt.i(ImageLoader.class, "Create cache folder", cacheFolder);
            } else {
                LogIt.w(ImageLoader.class, "Failed to create cache folder",
                        cacheFolder);
            }
        }
    }

    /**
     * Display the image at the provided URL in the provided ImageView, but
     * keeping the image at its full size.  
     */
    public void displayImageFullSize(final String url, ImageView imageView) {
        displayImage(url, imageView, -1, -1, false);
    }

    /**
     * Display the image at the provided URL in the provided ImageView, but
     * first resizing the image down to the thumbnail size.
     */
    public void displayImage(String imageKey, ImageView imageView) {
        int width = imageView.getResources().getDimensionPixelSize(
                R.dimen.thumbnail_width);
        int height = imageView.getResources().getDimensionPixelSize(
                R.dimen.thumbnail_height);

        displayImage(imageKey, imageView, width, height, false);
    }

    /**
     * Display the image at the provided URL in the provided ImageView, but
     * first resizing the image down to the provided width and height.
     * 
     * Retrieves the image from our local image cache if it is already present,
     * or downloads it and stores it in the cache if it isn't there.
     */
    public void displayImage(String imageKey, ImageView imageView,
            final int width, final int height, final boolean useHighestRatio) {
        if (imageKey == null) {
            LogIt.w(this, "displayImage called with null image, ignore");
            return;
        }

        Bitmap bitmap = memoryCache.get(imageKey);

        // LogIt.d(this, "Memory", memoryCache.size() / (1024 * 1024), "Hits:",
        //         memoryCache.hitCount(), "miss:", memoryCache.missCount(),
        //         "evictions:", memoryCache.evictionCount());

        if (bitmap != null) {
            // LogIt.d(this, "Bitmap retrieved from cache " + imageKey);
            imageView.setImageBitmap(bitmap);
        } else {
            final PhotoToLoad photoToLoad = new PhotoToLoad(imageKey, imageView);

            new BackgroundTask() {
                Bitmap bitmap;

                @Override
                public void work() {
                    try {
                        bitmap = getBitmap(photoToLoad.url, width, height,
                                useHighestRatio);
                    } catch (IOException e) {
                        LogIt.w(ImageLoader.class,
                                "IOException downloading image",
                                e.getMessage(), photoToLoad.url);
                        fail("IOException downloading image " + photoToLoad.url);
                    } catch (ResourceException e) {
                        LogIt.w(ImageLoader.class,
                                "ResourceException downloading image",
                                e.getMessage(), photoToLoad.url);
                        fail("ResourceException downloading image "
                                + photoToLoad.url);
                    } catch (Exception e) {
                        LogIt.e(ImageLoader.class, e, e.getMessage());
                        fail("Unexpected Exception downloading image "
                                + photoToLoad.url);
                    }
                }

                @Override
                public void done() {
                    if (failed() || (bitmap == null)) {
                        LogIt.w(ImageLoader.class, "Failed to download image",
                                photoToLoad.url);
                    } else {
                        addImage(photoToLoad.url, bitmap);
                        photoToLoad.imageView.setImageBitmap(bitmap);
                    }
                }
            };
        }
    }

    /**
     * Display the image at the provided URL in the provided ImageView, but
     * first resizing the image down to the thumbnail default size.
     * 
     * Retrieves the image from our local image cache if it is already present,
     * or downloads it and stores it in the cache if it isn't there.
     * 
     * This method differs from displayImage, because here we're setting up 
     * a tag for each bitmap, in order to find them easily
     */
    public void displayWebImage(String imageKey, String url, ImageView imageView) {
        int width = imageView.getResources().getDimensionPixelSize(
                R.dimen.thumbnail_width);
        int height = imageView.getResources().getDimensionPixelSize(
                R.dimen.thumbnail_height);

        displayWebImage(imageKey, url, imageView, width, height);
    }

    /**
     * Display the image at the provided URL in the provided ImageView, but
     * first resizing the image down to the provided width and height.
     * 
     * Retrieves the image from our local image cache if it is already present,
     * or downloads it and stores it in the cache if it isn't there.
     * 
     * This method differs from displayImage, because here we're setting up 
     * a tag for each bitmap, in order to find them easily
     */
    public void displayWebImage(String imageKey, String url,
            ImageView imageView, final int width, final int height) {
        Bitmap bitmap = memoryCache.get(imageKey);

        // LogIt.d(this, "Memory", memoryCache.size() / (1024 * 1024), "Hits:",
        //         memoryCache.hitCount(), "miss:", memoryCache.missCount(),
        //         "evictions:", memoryCache.evictionCount());

        if (bitmap != null && imageView.getTag() != null
                && imageView.getTag().equals(imageKey)) {
            // LogIt.d(this, "Bitmap retrieved from cache " + imageKey);
            imageView.setImageBitmap(bitmap);
        } else {
            final PhotoToLoad photoToLoad = new PhotoToLoad(url, imageKey,
                    imageView);

            new BackgroundTask() {

                Bitmap bitmap;

                @Override
                public void work() {
                    try {
                        bitmap = getBitmap(photoToLoad.imageKey,
                                photoToLoad.url, width, height);
                    } catch (AmazonClientException e) {
                        fail("S3 Error downloading image " + photoToLoad.url);
                    } catch (IOException e) {
                        fail("Timeout Error downloading image "
                                + photoToLoad.url);
                    } catch (ResourceException e) {
                        fail("ResourceException downloading image "
                                + photoToLoad.url);
                    } catch (Exception e) {
                        LogIt.e(ImageLoader.class, e, e.getMessage());
                        fail("Unexpected Exception downloading image "
                                + photoToLoad.url);
                    }
                }

                @Override
                public void done() {
                    if (!failed()) {
                        if (bitmap != null
                                && photoToLoad.imageView.getTag() != null
                                && photoToLoad.imageView.getTag().equals(
                                        photoToLoad.imageKey)) {
                            addImage(photoToLoad.imageKey, bitmap);
                            photoToLoad.imageView.setImageBitmap(bitmap);
                        }
                    }
                }
            };
        }
    }

    /**
     * Creates or loads a masked bitmap to be applied on a media message bubble
     * 
     * @param imageKey the URL or S3 mediaKey of the file to display, e.g.
     *                   http://i.ytimg.com/vi/Tx1XIm6q4r4/default.jpg (YouTube thumbnail)
     *                   u/885307588739207168/m/y2r1k0_2.jpg (Photo thumbnail)                 
     * @param wasMsgSent true if the message bubble being displayed is for a 
     *                   sent message, false for a received message.
     */
    public void displayMessageBubble(final IMessage imessage,
            final String imageKey, final ImageView holder, boolean wasMsgSent) {

        final IMessageType messageType = imessage.getType();

        if (imageKey == null) {
            LogIt.w(this, "displayMessageBubble called with null imageKey");
            return;
        }

        Dimension bubbleSize = AbstractMessage.setMessageBubbleSize(imessage,
                holder.getLayoutParams());

        // LogIt.d(this, "Bubble width and height", bubbleSize.getWidth(),
        //         bubbleSize.getHeight());

        // Message bubbles are cached as the MD5 sum of the S3 image key, with 
        // a suffix for whether the message was sent or received, e.g.
        //  9d9bb1cdff6a4094a43813f1c8925e45-r
        //  9d9bb1cdff6a4094a43813f1c8925e45-s
        StringBuilder cacheIDBuilder = new StringBuilder(
                FileSystemUtil.md5(imageKey));
        cacheIDBuilder.append("-");

        if (wasMsgSent) {
            cacheIDBuilder.append("s");
        } else {
            cacheIDBuilder.append("r");
        }

        final String cacheID = cacheIDBuilder.toString();

        // Remember what image is being loaded for this ImageView.  If the view
        // is reused before the image is ready then we know not to display the
        // older image.
        holder.setTag(cacheID);

        // LogIt.d(this, "Display message bubble", imageKey, cacheID);

        // First try and get the message bubble from the in-memory cache
        Bitmap bitmap = memoryCache.get(cacheID);

        if (bitmap != null) {
            LogIt.d(this, "Message bubble retrieved from memory cache", cacheID);
            holder.setImageBitmap(bitmap);
            return;
        }

        // The photo is not in the in-memory cache, so check if it is
        // available on disk
        final File cacheFile = new File(getAppCacheDirBubbles(), cacheID);

        if ((cacheFile != null) && cacheFile.exists()) {
            bitmap = ImageUtil.decodeSampledBitmapFromFile(
                    cacheFile.toString(), -1, -1);

            if (bitmap == null) {
                LogIt.w(this, "Error reading message bubble bitmap from file",
                        cacheFile);
            } else {
                LogIt.d(this, "Message bubble retrieved from disk", cacheFile);
                holder.setImageBitmap(bitmap);

                // Add the bitmap to our in-memory cache
                addImage(cacheID, bitmap);

                return;
            }
        }

        // The photo was not found in memory, or on disk, so create the masked
        // version and save it into all our caches
        final int overlayResId;
        final int maskResId;

        final int boundingWidth = bubbleSize.getWidth();
        final int boundingHeight = bubbleSize.getHeight();

        if (wasMsgSent) {
            if (messageType == IMessageType.LOCATION) {
                maskResId = R.drawable.messagesview_bubble_thumbtext_mask_self;
                overlayResId = R.drawable.messagesview_bubble_thumbtext_overlay_self;
            } else {
                maskResId = R.drawable.messagesview_bubble_thumb_mask_self;
                overlayResId = R.drawable.messagesview_bubble_thumb_overlay_self;
            }
        } else {
            if (messageType == IMessageType.LOCATION) {
                maskResId = R.drawable.messagesview_bubble_thumbtext_mask_other;
                overlayResId = R.drawable.messagesview_bubble_thumbtext_overlay_other;
            } else {
                maskResId = R.drawable.messagesview_bubble_thumb_mask_other;
                overlayResId = R.drawable.messagesview_bubble_thumb_overlay_other;
            }
        }

        new BackgroundTask() {

            Bitmap bitmap;

            @Override
            public void work() {
                try {
                    Bitmap image = getBitmap(imageKey, boundingWidth,
                            boundingHeight);

                    if (image != null) {
                        LogIt.d(ImageLoader.class, "Thumbnail image loaded",
                                imageKey,
                                image.getWidth() + "x" + image.getHeight());

                        // Omit resize for location messages
                        if (messageType != IMessageType.LOCATION) {
                            // Resize to fit inside the bigger of the two 
                            // bounding dimensions
                            if (boundingWidth > boundingHeight) {
                                image = ImageUtil.resizeImage(image,
                                        boundingWidth);
                            } else {
                                image = ImageUtil.resizeImage(image,
                                        boundingHeight);
                            }
                        }

                        // Mask the media thumbnail
                        Bitmap maskedImage = Bitmap.createBitmap(
                                image.getWidth(), image.getHeight(),
                                Config.ARGB_8888);

                        Bitmap mask = ImageUtil.getResizedNinePatch(maskResId,
                                image.getWidth(), image.getHeight(), context);

                        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        paint.setXfermode(new PorterDuffXfermode(
                                PorterDuff.Mode.DST_IN));

                        Canvas canvas = new Canvas(maskedImage);
                        canvas.drawBitmap(image, 0, 0, null);
                        canvas.drawBitmap(mask, 0, 0, paint);

                        // Now combine the background overlay with the
                        // masked thumbnail image
                        bitmap = Bitmap.createBitmap(image.getWidth(),
                                image.getHeight(), Config.ARGB_8888);

                        Bitmap backgroundBubble = ImageUtil
                                .getResizedNinePatch(overlayResId,
                                        image.getWidth(), image.getHeight(),
                                        context);

                        canvas = new Canvas(bitmap);
                        canvas.drawBitmap(maskedImage, 0, 0, null);
                        canvas.drawBitmap(backgroundBubble, 0, 0, null);

                        // Some sanity checks so we get a warning if something
                        // didn't go as planned
                        if ((bitmap == null) || (backgroundBubble == null)
                                || (mask == null) || (maskedImage == null)) {
                            LogIt.w(ImageLoader.class,
                                    "Failed to create bitmap for message bubble",
                                    bitmap, backgroundBubble, mask, maskedImage);
                        }

                        // Save the masked bitmap to the in-memory cache
                        addImage(cacheID, bitmap);

                        // Save the masked bitmap to the disk cache
                        FileOutputStream outputStream = new FileOutputStream(
                                cacheFile);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100,
                                outputStream);
                        FileSystemUtil.closeOutputStream(outputStream);

                        if (!cacheFile.exists()) {
                            LogIt.w(ImageLoader.class,
                                    "Failed to save save masked bitmap to disk",
                                    cacheFile);
                        }

                        paint.setXfermode(null);
                    } else {
                        fail("Image " + imageKey + " is not available");
                    }
                } catch (IOException e) {
                    LogIt.w(ImageLoader.class, "IOException downloading image",
                            e.getMessage(), imageKey);
                    fail("IOException downloading image " + imageKey);
                } catch (Exception e) {
                    LogIt.e(ImageLoader.class, e,
                            "Exception downloading image", imageKey);
                    fail("Error creating bitmap: " + e.getMessage());
                }
            }

            @Override
            public void done() {
                if (failed()) {
                    LogIt.w(ImageLoader.class, "Failed to download image",
                            imageKey);
                } else {
                    if (bitmap != null) {
                        // Check the message bubble still needs to be displayed
                        // in this view (the view may have been reused to 
                        // display another message before the image was ready).
                        if ((holder.getTag() != null)
                                && (holder.getTag().equals(cacheID))) {
                            holder.setImageBitmap(bitmap);
                        } else {
                            LogIt.d(ImageLoader.class,
                                    "Do not display bitmap as view has been reused",
                                    cacheID);
                        }
                    }
                }
            }
        };
    }

    public enum ProfilePhotoSize {
        SMALL, MEDIUM, LARGE;

        public String toString() {
            if (this == SMALL) {
                return "small";
            } else if (this == MEDIUM) {
                return "medium";
            } else if (this == LARGE) {
                return "large";
            } else {
                LogIt.e(this, "Unknown ProfilePhotoSize", this);
                return "unknown";
            }
        }
    };

    /**
     * Displays a masked profile picture into an ImageView.
     * 
     * @param contact - the Contact whose profile photo to load
     * @param view - the ImageView in which to display the loaded picture
     * @param size - the ProfilePhotoSize to be displayed, i.e. what mask to use
     */
    public void displayProfilePicture(Contact contact, ImageView imageView,
            ProfilePhotoSize size) {
        if (contact == null) {
            LogIt.w(this,
                    "Don't try to display profile photo for null contact");
        } else {
            displayProfilePicture(contact.getContactId(),
                    contact.getProfileImageKey(), imageView, size);
        }
    }

    /**
     * @param contactID - the ID of the contact whose profile photo to display
     * @param imageKey - the S3 media key, e.g. u/891508046298222592/p/Zcytuu.jpg 
     * @param imageView - the ImageView in which to display the loaded picture
     * @param size - the ProfilePhotoSize to be displayed, i.e. what mask to use
     */
    public void displayProfilePicture(long contactID, String imageKey,
            ImageView imageView, ProfilePhotoSize size) {

        if (!StringUtil.isEmpty(imageKey)) {
            imageView.setTag(imageKey);
        } else {
            imageView.setTag(null);
            return;
        }

        //        LogIt.d(this, "Display profile photo", contactID, imageKey, size);

        // Profile photos are cached in the form <contactID>-<size>, e.g. 
        //   88503111861993881-medium
        final String cacheID = contactID + "-" + size;

        // First try and get the message bubble from the in-memory cache
        Bitmap bitmap = memoryCache.get(cacheID);

        if ((bitmap != null) && (imageView.getTag() != null)
                && imageView.getTag().equals(imageKey)) {
            //            LogIt.d(this, "Profile photo retrieved from memory cache", cacheID);
            imageView.setImageBitmap(bitmap);
            return;
        }

        File cacheFolder = getAppCacheDirProfiles();

        // The photo is not in the in-memory cache, so check if it is
        // available on disk
        final File cacheFile = new File(cacheFolder, cacheID);

        if ((cacheFile != null) && cacheFile.exists()) {
            bitmap = ImageUtil.decodeSampledBitmapFromFile(
                    cacheFile.toString(), -1, -1);

            if (bitmap == null) {
                LogIt.w(this, "Error reading profile photo bitmap from file",
                        cacheFile);
            } else {
                //                LogIt.d(this, "Profile photo retrieved from disk", cacheFile);
                imageView.setImageBitmap(bitmap);

                // Add the bitmap to our in-memory cache
                addImage(cacheID, bitmap);

                return;
            }
        }

        // The photo was not found in memory, or on disk, so create the masked
        // version and save it into all our caches
        int maskResId = R.drawable.profilepic_mask_small;

        if (size == ProfilePhotoSize.MEDIUM) {
            maskResId = R.drawable.profilepic_mask_medium;
        } else if (size == ProfilePhotoSize.LARGE) {
            maskResId = R.drawable.profilepic_mask_large;
        }

        final PhotoToLoad photoToLoad = new PhotoToLoad(imageKey, imageView,
                maskResId);

        new BackgroundTask() {

            Bitmap bitmap;

            @Override
            public void work() {
                final Bitmap mask = loadMask(
                        photoToLoad.imageView.getResources(),
                        photoToLoad.maskResId);
                try {
                    Bitmap userPic = getBitmap(photoToLoad.url,
                            mask.getWidth(), mask.getHeight());

                    if (userPic != null) {
                        Bitmap userPicThumb = ImageUtil.resizeImage(userPic,
                                mask.getWidth(), false);

                        bitmap = Bitmap.createBitmap(mask.getWidth(),
                                mask.getHeight(), Config.ARGB_8888);

                        Canvas canvas = new Canvas(bitmap);

                        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        paint.setXfermode(new PorterDuffXfermode(
                                PorterDuff.Mode.DST_IN));

                        canvas.drawBitmap(
                                userPicThumb,
                                (mask.getWidth() - userPicThumb.getWidth()) / 2,
                                (mask.getHeight() - userPicThumb.getHeight()) / 2,
                                null);
                        canvas.drawBitmap(mask, 0, 0, paint);

                        // Save the masked bitmap to the in-memory cache
                        addImage(cacheID, bitmap);

                        // Save the masked bitmap to the disk cache
                        LogIt.d(ImageLoader.class,
                                "Save masked bitmap to disk", cacheFile);

                        FileOutputStream outputStream = new FileOutputStream(
                                cacheFile);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100,
                                outputStream);
                        FileSystemUtil.closeOutputStream(outputStream);

                        if (!cacheFile.exists()) {
                            LogIt.w(ImageLoader.class,
                                    "Failed to save save masked bitmap to disk",
                                    cacheFile);
                        }

                        paint.setXfermode(null);
                    } else {
                        fail("Image " + photoToLoad.url + " not available");
                    }
                } catch (IOException e) {
                    LogIt.w(ImageLoader.class, "IOException downloading image",
                            e.getMessage(), photoToLoad.url);
                    fail("IOException downloading image " + e.getMessage());
                } catch (Exception e) {
                    LogIt.e(ImageLoader.class, e,
                            "Exception downloading image", photoToLoad.url);
                    fail(e);
                }
            }

            @Override
            public void done() {
                if (!failed()) {
                    if (bitmap != null) {

                        // Display the image in the provided view.  Check the
                        // tag to ensure the view isn't now supposed to be
                        // displaying something else.
                        if ((photoToLoad.imageView.getTag() != null)
                                && photoToLoad.imageView.getTag().equals(
                                        photoToLoad.url)) {

                            photoToLoad.imageView.setImageBitmap(bitmap);
                        }
                    }
                } else {
                    LogIt.w(ImageLoader.class, "Error downloading image",
                            photoToLoad.url);
                }
            }
        };
    }

    /**
     * When the user changes their profile picture we need to delete the
     * old one from our in-memory and on disk caches.
     */
    public void deleteProfilePictureFromCaches(Contact contact) {

        long contactID = contact.getContactId();

        //        LogIt.d(this, "Delete profile photos from cache", contactID);

        // Profile photos are cached in the form <contactID>-<size>, e.g. 
        //   88503111861993881-medium
        List<String> cacheIDs = new ArrayList<String>();

        for (ProfilePhotoSize photoSize : ProfilePhotoSize.values()) {
            String size = photoSize.toString();
            cacheIDs.add(contactID + "-" + size);
        }

        File cacheFolder = getAppCacheDirProfiles();

        for (String cacheID : cacheIDs) {
            memoryCache.remove(cacheID);

            final File cacheFile = new File(cacheFolder, cacheID);

            if (cacheFile.exists()) {
                if (cacheFile.delete()) {
                    LogIt.d(this, "Deleted cached profile photo from disk",
                            cacheFile);
                } else {
                    // This means the old profile photo will continue to show up
                    LogIt.w(this,
                            "Failed to delete cached profile photo from disk",
                            cacheFile);
                }
            } else {
                //                LogIt.d(this, "No cached file on disk to delete", cacheFile);
            }
        }
    }

    /**
     * Loads a cover picture into an ImageView, or shows the plain
     * background color instead.
     * 
     * @param imageKey - Picture to load
     * @param view - ImageView to display the loaded picture
     */
    public void displayCoverPicture(String imageKey, ImageView view) {
        // Only set the background color if there is no cover photo, otherwise
        // there is a flicker as the photo loads
        if (StringUtil.isEmpty(imageKey)) {
            LogIt.d(this, "No cover photo, set the background color");
            view.setBackgroundColor(view.getResources().getColor(
                    R.color.profile_image_background));
        } else {
            int myProfileCoverPictureHeight = view.getResources()
                    .getDimensionPixelSize(
                            R.dimen.my_profile_cover_picture_height);

            int screenWidth = MessageMeApplication.getScreenSize().getWidth();

            displayImage(imageKey, view, screenWidth,
                    myProfileCoverPictureHeight, false);
        }
    }

    /**
     * Loads mask into the memory cache
     */
    private Bitmap loadMask(Resources resources, int maskResId) {
        String resourceKey = String.valueOf(maskResId);

        if (containsImage(resourceKey)) {
            return getImage(resourceKey);
        } else {
            Bitmap mask = ImageUtil.decodeSampledBitmapFromResource(resources,
                    maskResId, -1, -1);

            addImage(resourceKey, mask);
            return mask;
        }
    }

    /**
     * Loads an image from the file system or synchronously downloads it 
     * if the image is not present.
     */
    public Bitmap getBitmap(String imageKey, int width, int height)
            throws AmazonClientException, IOException {
        return getBitmap(imageKey, width, height, false);
    }

    /**
     * Loads an image from the file system or synchronously downloads it 
     * if the image is not present.
     */
    public Bitmap getBitmap(String imageKey, int width, int height,
            boolean useHighestRatio) throws AmazonClientException, IOException {

        if (TextUtils.isEmpty(imageKey)) {
            return null;
        }

        Bitmap bitmap = null;
        File f = new File(imageKey).exists() ? new File(imageKey) : ImageUtil
                .getFile(imageKey);

        if (f.exists()) {
            LogIt.d(this, "Loading image", f, width + "x" + height);
            bitmap = ImageUtil.decodeSampledBitmapFromFile(f.getAbsolutePath(),
                    width, height, useHighestRatio);
        } else if (URLUtil.isValidUrl(imageKey)) {
            try {
                LogIt.d(this, "Image does not exist, download it", imageKey,
                        width + "x" + height);
                String md5Key = FileSystemUtil.md5(imageKey);
                f = ImageUtil.newFile(md5Key);
                ImageUtil.downloadWebImage(f, context, imageKey);

                bitmap = ImageUtil.decodeSampledBitmapFromFile(
                        f.getAbsolutePath(), width, height, useHighestRatio);
            } catch (IOException e) {
                LogIt.w(this, e.getMessage(), imageKey);
            } catch (NoSuchAlgorithmException e) {
                LogIt.e(this, e, e.getMessage());
            }
        } else {
            LogIt.d(this, "Downloading image", imageKey, width + "x" + height);

            // The S3 download client is always ready to download as it does
            // not use an access token
            f = MediaManager.getInstance().downloadFile(imageKey);
            bitmap = ImageUtil.decodeSampledBitmapFromFile(f.getAbsolutePath(),
                    width, height, useHighestRatio);
        }

        return bitmap;
    }

    /**
     * Loads an image from the cache file system or start a download task in case
     * that the image is not present yet.
     */
    public Bitmap getBitmap(String imageKey, String url, int width, int height)
            throws AmazonClientException, IOException {
        if (TextUtils.isEmpty(imageKey)) {
            return null;
        }

        Bitmap bitmap = null;
        File f = ImageUtil.getCacheFile(imageKey);

        if (f.exists()) {
            LogIt.d(this, "Loading image", f);
            bitmap = ImageUtil.decodeSampledBitmapFromFile(f.getAbsolutePath(),
                    width, height);
        } else if (URLUtil.isValidUrl(url)) {
            try {
                LogIt.d(ImageLoader.class, "Image does not exist, download it",
                        url);
                f = ImageUtil.newCacheFile(imageKey);
                ImageUtil.downloadWebImage(f, context, url);

                bitmap = ImageUtil.decodeSampledBitmapFromFile(
                        f.getAbsolutePath(), width, height);
            } catch (IOException e) {
                LogIt.e(ImageLoader.class, e, e.getMessage());
            } catch (ResourceException e) {
                LogIt.e(ImageLoader.class, e, e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                LogIt.e(ImageLoader.class, e, e.getMessage());
            } catch (Exception e) {
                LogIt.e(ImageLoader.class, e, e.getMessage());
            }
        }

        return bitmap;
    }

    // Task for the queue
    private class PhotoToLoad {

        public String url;

        public int maskResId;

        public String imageKey;

        public ImageView imageView;

        public PhotoToLoad(String url, ImageView imageView) {
            this.url = url;
            this.maskResId = 0;
            this.imageView = imageView;
        }

        public PhotoToLoad(String url, String imageKey, ImageView imageView) {
            this.url = url;
            this.maskResId = 0;
            this.imageKey = imageKey;
            this.imageView = imageView;
        }

        public PhotoToLoad(String url, ImageView imageView, int maskResId) {
            this.url = url;
            this.maskResId = maskResId;
            this.imageView = imageView;
        }
    }

    public void clearCache() {
        memoryCache.evictAll();
    }

    public void addImage(String cacheKey, Bitmap bitmap) {
        // LogIt.d(this, "Add bitmap to in-memory cache", cacheKey);
        memoryCache.put(cacheKey, bitmap);
    }

    /**
     * Get a specific bitmap from the in-memory cache.
     * 
     * Typical cacheIDs are:
     *   9d9bb1cdff6a4094a43813f1c8925e45-r (MD5 sum for received message bubble)
     *   88503111861993881-medium (profile photo of particular size)
     *   u/885307588739207168/m/h8Vciz_2.jpg (Amazon S3 mediaKey)
     *   http://i.ytimg.com/vi/mIA0W69U2_Y/mqdefault.jpg (search result thumbnail URL)
     */
    public Bitmap getImage(String cacheID) {
        LogIt.d(this, "Get image from memory cache", cacheID);
        return memoryCache.get(cacheID);
    }

    public boolean containsImage(String imageKey) {
        if (imageKey == null) {
            return false;
        }
        return memoryCache.get(imageKey) != null;
    }

    /**
     * Stores the image provided in the imageKey to the device Gallery
     * 
     * Checks if the image is already stored into the LRU cache, if that«s the case
     * then reuse the bitmap and stores it, if that's not the case
     * then proceed to download the image
     */
    public void storeIntoGallery(Context context, final String imageKey,
            final String title, final String description) {
        if (imageKey == null) {
            LogIt.w(this, "displayImage called with null image, ignore");
            return;
        }

        Bitmap bitmap = memoryCache.get(imageKey);

        LogIt.d(this, "Memory", memoryCache.size() / (1024 * 1024), "Hits:",
                memoryCache.hitCount(), "miss:", memoryCache.missCount(),
                "evictions:", memoryCache.evictionCount());

        if (bitmap != null) {
            LogIt.d(this, "Bitmap retrived from cache " + imageKey);
            ImageUtil.saveToGallery(context, bitmap, title, description);
        } else {
            // Image has not being yet downloaded or has been cleared from the LRU cache
            LogIt.w(ImageLoader.class,
                    "Can't save image into gallery, not yet downloaded");
            Toast.makeText(context,
                    context.getString(R.string.detail_screen_wait_download),
                    Toast.LENGTH_SHORT).show();
        }
    }

}