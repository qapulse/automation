package com.littleinc.MessageMe.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.media.ExifInterface;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import com.coredroid.util.Dimension;
import com.coredroid.util.IOUtil;
import com.coredroid.util.LogIt;
import com.coredroid.util.UIUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;

/**
 * Loading bitmaps has to be done carefully to avoid an OutOfMemoryError,
 * so we follow the Android guidelines:
 * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
 */
public class ImageUtil {

    public static final int MEDIA_TYPE_IMAGE = 1;

    public static final int MEDIA_TYPE_VIDEO = 2;

    public static BitmapFactory.Options getBitmapOptionsFromFile(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(path, options);

        return options;
    }

    public static BitmapFactory.Options getBitmapOptionsFromResource(
            Resources res, int resource) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(res, resource);

        return options;
    }

    /**
     * Calculates the smallest radio to be used as the inSampleSize
     * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     * #load-bitmap
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight, boolean useHighestRatio) {

        int inSampleSize = 1;

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height
                    / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            if (useHighestRatio) {
                // Choose the highest ratio as inSampleSize value, this will
                // economy memory
                inSampleSize = heightRatio > widthRatio ? heightRatio
                        : widthRatio;
            } else {
                // Choose the smallest ratio as inSampleSize value, this will
                // guarantee a final image with both dimensions larger than or
                // equal to the requested height and width.
                inSampleSize = heightRatio < widthRatio ? heightRatio
                        : widthRatio;
            }
        }

        return inSampleSize;
    }

    /**
     * Decodes a sampled version of a given image file using smallest ratio as
     * inSampleSize at the moment of decode the bitmap
     * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     * #load-bitmap
     */
    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth,
            int reqHeight) {
        return decodeSampledBitmapFromFile(path, reqWidth, reqHeight, false,
                true);
    }

    /**
     * Decodes a sampled version of a given image file
     * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     * #load-bitmap
     * 
     * @param useHighestRatio flag to specify desired ratio to be used as
     * inSampleSize
     */
    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth,
            int reqHeight, boolean useHighestRatio) {
        return decodeSampledBitmapFromFile(path, reqWidth, reqHeight,
                useHighestRatio, true);
    }

    /**
     * Decodes a sampled version of a given image file
     * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     * #load-bitmap
     * 
     * @param useHighestRatio flag to specify desired ratio to be used as
     * inSampleSize
     * @param isFirstTime used to identify if this decode is the first try or if
     * is a retry caused by an Out Of Memory Error
     */
    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth,
            int reqHeight, boolean useHighestRatio, boolean isFirstTime) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // Calculate inSampleSize
            if (reqHeight != -1 && reqWidth != -1) {
                options.inSampleSize = calculateInSampleSize(options, reqWidth,
                        reqHeight, useHighestRatio);
            } else {
                options.inSampleSize = 1;
            }

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            // TODO: respectBounding flag should be a parameter to be more
            // maintainable but for now set it as true will work
            return resizeBitmapIfNeeded(
                    BitmapFactory.decodeFile(path, options), options, reqWidth,
                    reqHeight, true);
        } catch (OutOfMemoryError e) {
            if (isFirstTime) {
                LogIt.w(ImageUtil.class,
                        "No more memory, clear memory cache and retry decodeSampledBitmapFromFile");
                ImageLoader.getInstance().clearCache();
                System.gc();

                return decodeSampledBitmapFromFile(path, reqWidth, reqHeight,
                        useHighestRatio, false);
            } else {
                LogIt.e(ImageUtil.class,
                        "OutOfMemoryError on retry, now give up");
            }
        }

        return null;
    }

    /**
     * Decodes a sampled version of a given resource
     * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     * #load-bitmap
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res,
            int resId, int reqWidth, int reqHeight) {
        return decodeSampledBitmapFromResource(res, resId, reqWidth, reqHeight,
                false, true);
    }

    /**
     * Decodes a sampled version of a given resource
     * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     * #load-bitmap
     * 
     * @param useHighestRatio flag to specify desired ratio to be used as
     * inSampleSize
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res,
            int resId, int reqWidth, int reqHeight, boolean useHighestRatio) {
        return decodeSampledBitmapFromResource(res, resId, reqWidth, reqHeight,
                useHighestRatio, true);
    }

    /**
     * Decodes a sampled version of a given resource
     * http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     * #load-bitmap
     * 
     * @param useHighestRatio flag to specify desired ratio to be used as
     * inSampleSize
     * @param isFirstTime used to identify if this decode is the first try or if
     * is a retry caused by an Out Of Memory Error
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res,
            int resId, int reqWidth, int reqHeight, boolean useHighestRatio,
            boolean isFirstTime) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(res, resId, options);

            // Calculate inSampleSize
            if (reqHeight != -1 && reqWidth != -1) {
                options.inSampleSize = calculateInSampleSize(options, reqWidth,
                        reqHeight, useHighestRatio);
            } else {
                options.inSampleSize = 1;
            }

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            // TODO: respectBounding flag should be a parameter to be more
            // maintainable but for now set it as true will work
            return resizeBitmapIfNeeded(
                    BitmapFactory.decodeResource(res, resId, options), options,
                    reqWidth, reqHeight, true);
        } catch (OutOfMemoryError e) {
            if (isFirstTime) {
                LogIt.w(ImageUtil.class,
                        "No more memory, clear memory cache and retry decodeSampledBitmapFromResource");
                ImageLoader.getInstance().clearCache();
                System.gc();

                decodeSampledBitmapFromResource(res, resId, reqWidth,
                        reqHeight, useHighestRatio, false);
            } else {
                LogIt.e(ImageUtil.class,
                        "OutOfMemoryError on retry, now give up");
            }
        }

        return null;
    }

    /**
     * Checks if the require dimensions are lower than the original bitmap and
     * resize it if is needed
     */
    public static Bitmap resizeBitmapIfNeeded(Bitmap bitmap, Options options,
            int reqWidth, int reqHeight, boolean respectBounding) {

        if (bitmap != null) {
            if (reqWidth == -1 && reqHeight == -1 || reqWidth == 0
                    && reqHeight == 0) {
                return bitmap;
            } else if (reqWidth > options.outWidth
                    && reqHeight > options.outHeight) {

                // Use smallest bounding when both required dimensions are
                // higher than the original image
                return resizeImage(bitmap, Math.min(reqWidth, reqHeight),
                        respectBounding);
            } else if (reqWidth > options.outWidth) {
                return resizeImage(bitmap, reqWidth, respectBounding);
            } else if (reqHeight > options.outHeight) {
                return resizeImage(bitmap, reqHeight, respectBounding);
            } else if (reqWidth < options.outWidth
                    && reqHeight < options.outHeight) {

                // Use highest bounding when both required dimensions are lower
                // than the original image
                return resizeImage(bitmap, Math.max(reqWidth, reqHeight),
                        respectBounding);
            } else if (reqWidth < options.outWidth) {
                return resizeImage(bitmap, reqWidth, respectBounding);
            } else if (reqHeight < options.outHeight) {
                return resizeImage(bitmap, reqHeight, respectBounding);
            } else {
                return bitmap;
            }
        } else {
            return null;
        }
    }

    /**
     * Generates a resized bitmap from a nine patch drawable
     */
    public static Bitmap getResizedNinePatch(int resId, int width, int height,
            Context context) throws Exception {
        Bitmap bitmap = null;
        ImageLoader imageLoader = ImageLoader.getInstance();

        if (imageLoader.containsImage(String.valueOf(resId))) {
            bitmap = imageLoader.getImage(String.valueOf(resId));
        } else {
            bitmap = BitmapFactory
                    .decodeResource(context.getResources(), resId);
            imageLoader.addImage(String.valueOf(resId), bitmap);
        }

        return getResizedNinePatch(bitmap, width, height, context);
    }

    /**
     * Generates a resized bitmap from a nine patch drawable
     */
    public static Bitmap getResizedNinePatch(Bitmap bitmap, int width,
            int height, Context context) throws Exception {
        return getResizedNinePatch(bitmap, width, height, context, true);
    }

    /**
     * Generates a resized bitmap from a nine patch drawable
     */
    public static Bitmap getResizedNinePatch(Bitmap bitmap, int width,
            int height, Context context, boolean isFirstTime) throws Exception {

        Bitmap output_bitmap = null;
        byte[] chunk = bitmap.getNinePatchChunk();

        if (chunk == null) {
            LogIt.e(ImageUtil.class, "Cannot decode 9Patch image");
            throw new Exception("Malformed 9patch bitmap");
        } else {
            NinePatchDrawable np_drawable = new NinePatchDrawable(
                    context.getResources(), bitmap, chunk, new Rect(), null);
            np_drawable.setBounds(0, 0, width, height);

            try {
                output_bitmap = Bitmap.createBitmap(width, height,
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(output_bitmap);
                np_drawable.draw(canvas);
            } catch (OutOfMemoryError e) {
                if (isFirstTime) {
                    LogIt.w(ImageUtil.class,
                            "No more memory, clear memory cache and retry decodeSampledBitmapFromResource");
                    ImageLoader.getInstance().clearCache();
                    System.gc();

                    getResizedNinePatch(bitmap, width, height, context, false);
                } else {
                    LogIt.e(ImageUtil.class,
                            "OutOfMemoryError on retry, now give up");
                }
            }
        }

        return output_bitmap;
    }

    /**
     * This method resize the original image file and save the new resized
     * bitmap into the destination file maintaining the aspect ratio
     */
    public static boolean resizeImageToFile(File originalFile,
            File destinationFile, int bounding, boolean respectBounding) {

        Options options = ImageUtil.getBitmapOptionsFromFile(originalFile
                .getAbsolutePath());

        // Only resize the image if surpasses the limit
        if (options.outWidth > bounding || options.outHeight > bounding) {

            LogIt.d(ImageUtil.class, "Resizing image", originalFile,
                    destinationFile, bounding + "px");

            Bitmap resizedBitmap = ImageUtil.decodeSampledBitmapFromFile(
                    originalFile.getAbsolutePath(), bounding, bounding, true,
                    true);

            if (resizedBitmap != null) {
                return saveImage(destinationFile.getAbsolutePath(),
                        resizedBitmap);
            } else {
                return false;
            }
        } else {

            if (originalFile.equals(destinationFile)) {
                LogIt.d(ImageUtil.class, "No need to resize image",
                        originalFile);
                return true;
            } else {
                LogIt.d(ImageUtil.class,
                        "Image did not need to be resized, so copy it",
                        originalFile);
                return copyFile(originalFile, destinationFile);
            }
        }
    }

    /**
     * Resize the given bitmap maintaining the aspect ratio
     */
    public static Bitmap resizeImage(Bitmap originalBitmap, int bounding) {
        return resizeImage(originalBitmap, bounding, true);
    }

    /**
     * Resize the given bitmap maintaining the aspect ratio.
     * 
     * @param respectBounding Use the scale factor necessary to respect the
     * bounding in both width and height
     */
    public static Bitmap resizeImage(Bitmap originalBitmap, int bounding,
            boolean respectBounding) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        float xScale = ((float) bounding) / width;
        float yScale = ((float) bounding) / height;
        float scale = (xScale >= yScale) ? xScale : yScale;

        int newWidth = (int) (originalBitmap.getWidth() * scale);
        int newHeight = (int) (originalBitmap.getHeight() * scale);

        if (respectBounding) {
            // If width or height surpasses the max bound use opposite scale
            // factor
            if (newWidth > bounding || newHeight > bounding) {
                scale = (scale == xScale) ? yScale : xScale;
            }
        }

        newWidth = (int) (originalBitmap.getWidth() * scale);
        newHeight = (int) (originalBitmap.getHeight() * scale);

        LogIt.d(ImageUtil.class, "Resize bitmap", bounding, scale, width + "x"
                + height, newWidth + "x" + newHeight);

        try {
            return Bitmap.createScaledBitmap(originalBitmap, newWidth,
                    newHeight, true);
        } catch (OutOfMemoryError e1) {
            LogIt.w(ImageUtil.class,
                    "No more memory, clear memory cache and retry resizeImage");
            ImageLoader.getInstance().clearCache();
            System.gc();

            try {
                return Bitmap.createScaledBitmap(originalBitmap, newWidth,
                        newHeight, true);
            } catch (OutOfMemoryError e2) {
                LogIt.e(ImageUtil.class,
                        "OutOfMemoryError after clearing cache, don't retry");
            }
        }

        return null;
    }

    /**
     * Calculate how to resize the input width and height to fit inside a
     * square specified by 'bounding', maintaining the aspect ratio.
     */
    public static Dimension calcResizeToBounds(int width, int height,
            int bounding) {

        float xScale = ((float) bounding) / width;
        float yScale = ((float) bounding) / height;
        float scale = (xScale <= yScale) ? xScale : yScale;

        Dimension newDimensions = new Dimension();
        newDimensions.setWidth((int) (width * scale));
        newDimensions.setHeight((int) (height * scale));

        // LogIt.d(ImageUtil.class, "Calculate resize", bounding, scale, width
        // + "x" + height,
        // newDimensions.getWidth() + "x" + newDimensions.getHeight());

        return newDimensions;
    }

    /**
     * Copy sourceFile to destFile.
     * 
     * This method copes with moving the file between the SD card and
     * internal storage.
     */
    public static boolean copyFile(File sourceFile, File destFile) {

        FileChannel source = null;
        FileChannel destination = null;

        try {
            if (!destFile.exists()) {
                destFile.createNewFile();
            }

            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } catch (IOException ioe) {
            LogIt.e(ImageUtil.class, "Error copying file", sourceFile,
                    destFile, ioe);
            return false;
        } finally {
            FileSystemUtil.closeChannel(source);
            FileSystemUtil.closeChannel(destination);
        }

        return true;
    }

    /**
     * Copy file "original" into the application media file cache.
     */
    public static boolean copyFile(String original, String imageKey) {

        if (original == null) {
            LogIt.w(ImageUtil.class, "Cannot copy from a null file");
            return false;
        }

        if (imageKey == null) {
            LogIt.w(ImageUtil.class, "Cannot copy to a null file");
            return false;
        }

        File originalFile = new File(original);
        File newFile = getFile(imageKey);
        LogIt.d(ImageUtil.class, "Copy file", original, newFile);

        return copyFile(originalFile, newFile);
    }

    /**
     * Saves a bitmap into a given file
     */
    public static boolean saveImage(String filePath, Bitmap bitmap) {
        boolean result = false;
        File file = new File(filePath);
        FileOutputStream outputStream = null;

        try {
            if (file.exists()) {
                outputStream = new FileOutputStream(file);

                bitmap.compress(CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();

                result = true;
            } else {
                LogIt.w(ImageUtil.class,
                        "Cannot save image as file does not exist", filePath);
            }
        } catch (FileNotFoundException e) {
            LogIt.w(ImageUtil.class, "File doesn't exist", file);
        } catch (IOException e) {
            LogIt.e(ImageUtil.class, e);
        } finally {
            FileSystemUtil.closeOutputStream(outputStream);
        }

        bitmap.recycle();
        bitmap = null;

        return result;
    }

    /**
     * Move the provided file into our application media disk cache.
     * 
     * This method will NOT work if you are trying to move the file between the
     * SD card and internal storage. If you need to do that, use
     * {@link #transferFile(String, String)} instead.
     */
    public static boolean moveFile(String original, String imageKey) {
        File originalfile = new File(original);
        File newFile = getFile(imageKey);
        LogIt.d(ImageUtil.class, "Move file", original, newFile);
        return originalfile.renameTo(newFile);
    }

    /**
     * Move the provided file into our application media disk cache. This method
     * DOES cope with moving the file between the SD card and internal storage.
     */
    public static boolean transferFile(String original, String imageKey) {
        File originalfile = new File(original);
        File newFile = getFile(imageKey);
        LogIt.d(ImageUtil.class, "Transfer file", original, newFile);

        boolean result = copyFile(originalfile, newFile);

        if (result) {
            if (originalfile.delete()) {
                LogIt.d(ImageUtil.class, "Deleted original file", originalfile);
            } else {
                // We still return true in this case as the new file exists, so
                // the message can be sent
                LogIt.w(ImageUtil.class, "Failed to delete original file",
                        originalfile);
            }
        }

        return result;
    }

    /**
     * Get a File object for storing the provided mediaKey in the application
     * media file cache.
     * 
     * @param mediaKey the unique file name to use in our local media cache,
     * including extension, e.g. VUw6F5.jpg
     */
    public static File newFile(String mediaKey) {
        File file = new File(ImageLoader.getAppCacheDirMedia(), mediaKey);
        return file;
    }

    /**
     * Returns a new file located in the cache file directory
     */
    public static File newCacheFile(String imageKey) {
        File file = new File(getCacheFilesDir(), imageKey);
        return file;
    }

    /**
     * Returns a file located in the cache file directory
     */
    public static File getCacheFile(String imageKey) {
        File file = new File(getCacheFilesDir(), imageKey);
        LogIt.d(ImageUtil.class, "Get cache file", imageKey);
        return file;
    }

    /**
     * Get the File object where this mediaKey would be stored in the
     * application media file cache.
     */
    public static File getFile(String mediaKey) {
        if (mediaKey == null) {
            LogIt.w(ImageUtil.class, "Cannot get a null file");
            return null;
        } else {
            String fileName = mediaKey.substring(mediaKey.lastIndexOf("/") + 1);
            File file = new File(ImageLoader.getAppCacheDirMedia(), fileName);
            // LogIt.d(ImageUtil.class, "Get file", fileName, mediaKey);
            return file;
        }
    }

    /**
     * Validate if a given file is inside the application media cache
     */
    public static boolean isInAppMediaCache(File file) {
        if (file.getParentFile()
                .getAbsolutePath()
                .equalsIgnoreCase(
                        ImageLoader.getAppCacheDirMedia().getAbsolutePath())) {
            return true;
        }

        return false;
    }

    /**
     * Validate if a given file is inside the application profile cache
     */
    public static boolean isInAppProfileCache(File file) {
        if (file.getParentFile()
                .getAbsolutePath()
                .equalsIgnoreCase(
                        ImageLoader.getAppCacheDirProfiles().getAbsolutePath())) {
            return true;
        }

        return false;
    }

    /**
     * Returns a File for the provided 'folder' inside the application
     * files directory, e.g.
     * /data/Android/data/com.littleinc.MessageMe/files/<folder>
     * 
     * We only store files on internal storage because anything stored
     * on the SD card is globally readable, which is too insecure.
     */
    public static File getInternalFilesDir(String folder) {
        return new File(MessageMeApplication.getInstance().getFilesDir(),
                folder);
    }

    /**
     * Returns a File for the provided 'folder' on the SD card, inside the
     * external files directory for the application, e.g.
     * /storage/sdcard0/Android/data/com.littleinc.MessageMe/files/<folder>
     * /mnt/sdcard/Android/data/com.littleinc.MessageMe/files/<folder>
     * 
     * Files stored here are globally readable, so this should only be
     * used for temporary files, e.g. where we rely on an external application
     * to create the file for us (like the camera for "Take Photo").
     */
    public static File getExternalFilesDir(String folder) {
        if (!FileSystemUtil.isWritableSDCardPresent()) {
            LogIt.w(ImageUtil.class,
                    "External media not available, you won't be able to write to this file!");
        }

        return new File(MessageMeApplication.getInstance().getExternalFilesDir(
                null), folder);
    }

    private static final String EXTERNAL_CACHE_FOLDER_MEDIA = "media";

    /**
     * Get a temporary File on the external SD card.
     * 
     * See {@link #getExternalFilesDir(String)}.
     */
    public static File getTemporaryExternalFile(String fileExtension) {

        File folder = getExternalFilesDir(EXTERNAL_CACHE_FOLDER_MEDIA);

        ImageLoader.createFolderIfRequired(folder);

        String fileName = StringUtil.getRandomFilename() + fileExtension;

        return new File(folder, fileName);
    }

    /**
     * Returns the application 'cache' directory, e.g.
     * /mnt/sdcard/Android/data/com.littleinc.MessageMe/cache
     * 
     * We use this to store temporary files in, e.g. search results images.
     */
    public static File getCacheFilesDir() {
        return MessageMeApplication.getInstance().getCacheDir();
    }

    /**
     * Obtains the device screen density
     */
    public static double getScreenDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    /**
     * Download an image from a URL directly to a bitmap, without saving
     * it to file.
     */
    public static Bitmap downloadWebImageToBitmap(String url) {
        Bitmap bitmap = null;
        try {
            LogIt.d(ImageUtil.class, "Download web image", url);
            InputStream is = FileSystemUtil.fetchRemoteInputStream(url);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            LogIt.w(ImageUtil.class, "Exception downloading image", url, e);
        }

        return bitmap;
    }

    /**
     * Download an image from a URL to the provided File.
     */
    public static void downloadWebImage(File file, Context context, String url)
            throws IOException, NoSuchAlgorithmException {
        LogIt.d(ImageUtil.class, "Download web image", file.getAbsolutePath(),
                url);
        File f = file;
        OutputStream os = new FileOutputStream(f);
        InputStream is = FileSystemUtil.fetchRemoteInputStream(url);
        IOUtil.copyAndClose(is, os);
    }

    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile(int type) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtil.getRandomFilename());

        switch (type) {
            case MEDIA_TYPE_IMAGE:
                builder.append(MessageMeConstants.PHOTO_MESSAGE_EXTENSION);
                return newFile(builder.toString());
            case MEDIA_TYPE_VIDEO:
                builder.append(MessageMeConstants.VIDEO_MESSAGE_EXTENSION);
                return newFile(builder.toString());
            default:
                return null;
        }
    }

    /**
     * Stores an image in the device Gallery
     */
    public static void saveToGallery(Context context, Bitmap image,
            String title, String description) {

        if (!FileSystemUtil.isWritableSDCardPresent()) {
            UIUtil.alert(context, R.string.detail_screen_unable_to_copy_title,
                    R.string.detail_screen_saved_to_gallery_no_sdcard_error);
            LogIt.w(ImageUtil.class,
                    "Can't save image to gallery, SD Card not present or doesn't have write permissions");
            return;
        }

        String media = null;
        if (image != null) {
            long imageSize = getBitmapMegaByteSize(image);
            if (FileSystemUtil.getAvailableStorageSpace() < imageSize) {
                // Not enough space in the sd card to store the image
                UIUtil.alert(context,
                        R.string.detail_screen_unable_to_copy_title,
                        R.string.detail_screen_saved_to_gallery_no_space);
            } else {
                // Uses the bitmap to store the image
                media = MediaStore.Images.Media
                        .insertImage(context.getContentResolver(), image,
                                title, description);
            }
        }
        if (media != null) {
            Toast.makeText(context,
                    context.getString(R.string.detail_screen_saved_to_gallery),
                    Toast.LENGTH_SHORT).show();
            LogIt.d(ImageUtil.class, "Image saved to SD Card", media);
        } else {
            UIUtil.alert(context, R.string.detail_screen_unable_to_copy_title,
                    R.string.detail_screen_saved_to_gallery_error);
            LogIt.e(ImageUtil.class, "Error saving image into the SD Card");
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public static long getBitmapMegaByteSize(Bitmap data) {
        long size = 0;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            size = data.getRowBytes() * data.getHeight();
        } else {
            size = data.getByteCount();
        }
        LogIt.d(ImageUtil.class, "Bitmap size in bytes: ", size);
        // Converts byte size into megabytes
        return size / (1024 * 1024);
    }

    /**
     * Fix the rotation of image files
     * caused by some custom camera apps
     */
    public static String rotatePicture(String filePath, Context context) {
        Matrix matrix = new Matrix();
        int orientation = 1;

        Bitmap rotatedBitmap = null;

        // Initialize the file with the original file, in case something's
        // wrong, we can return the original file
        File file = new File(filePath);

        try {
            ExifInterface exif = new ExifInterface(filePath);
            orientation = exif
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            matrix.setRotate(0);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    LogIt.d(ImageUtil.class, "Rotate image 90 degrees");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    LogIt.d(ImageUtil.class, "Rotate image 180 degrees");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(270);
                    LogIt.d(ImageUtil.class, "Rotate image 270 degrees");
                    break;
                default:
                    LogIt.d(ImageUtil.class,
                            "No need to rotate, returning original image");
                    return filePath;
            }

            rotatedBitmap = rotateBitmap(filePath, matrix, context);

            if (rotatedBitmap != null) {

                boolean saveResult = saveImage(filePath, rotatedBitmap);

                if (saveResult) {
                    LogIt.d(ImageUtil.class, "Rotated image succesfully stored");
                } else {
                    LogIt.w(ImageUtil.class, "Error storing the rotated image");
                }

                rotatedBitmap.recycle();
                rotatedBitmap = null;
            }
        } catch (IOException e) {
            LogIt.e(ImageUtil.class, e, e.getMessage());
        } catch (Exception e) {
            LogIt.e(ImageUtil.class, e, e.getMessage());
        }

        return file.getAbsolutePath();
    }

    /**
     * Rotates a bitmap
     */
    public static Bitmap rotateBitmap(String filePath, Matrix matrix,
            Context context) throws Exception {

        int maxPhotoSize = context.getResources().getDimensionPixelSize(
                R.dimen.max_photo_size);

        // This downsamples the image based on the closest power of 2 that
        // is still bigger than the maxPhotoSize, e.g. a 3264x2448 image
        // will be loaded into 1632x1224 bitmap, as that's the smallest
        // size still bigger than our maxPhotoSize of 960px.
        Bitmap bitmap = decodeSampledBitmapFromFile(filePath, maxPhotoSize,
                maxPhotoSize, true, true);

        if ((bitmap.getWidth() > maxPhotoSize)
                || (bitmap.getHeight() > maxPhotoSize)) {

            // Since we are rotating the image we may as well resize it at the
            // same time, so work out what scale factor to use.
            float xScale = ((float) maxPhotoSize) / bitmap.getWidth();
            float yScale = ((float) maxPhotoSize) / bitmap.getHeight();
            float scale = Math.min(xScale, yScale);

            LogIt.d(ImageUtil.class, "Matrix scale", scale);
            matrix.postScale(scale, scale);
        }

        Bitmap finalBitmap = null;

        try {
            finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, false);

            LogIt.d(ImageUtil.class,
                    "Bitmap rotated and resized " + bitmap.getWidth() + "x"
                            + bitmap.getHeight() + " -> "
                            + finalBitmap.getWidth() + "x"
                            + finalBitmap.getHeight());
        } catch (OutOfMemoryError e1) {
            LogIt.w(ImageUtil.class,
                    "No more memory, clear memory cache and retry rotateBitmap");
            ImageLoader.getInstance().clearCache();
            System.gc();

            try {
                finalBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            } catch (OutOfMemoryError e2) {
                LogIt.e(ImageUtil.class,
                        "OutOfMemoryError on retry rotateBitmap, now give up");
            }
        }

        if (bitmap != null) {
            // We know that finalBitmap isn't returning a subset of the
            // bitmap loaded from file as that can only happen if the
            // provided matrix was null. Therefore it should be safe to
            // recycle the original bitmap now.
            bitmap.recycle();
            bitmap = null;
        }

        return finalBitmap;
    }
}