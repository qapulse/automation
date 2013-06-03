package com.littleinc.MessageMe.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;

import org.apache.commons.codec.digest.DigestUtils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;

import com.coredroid.util.IOUtil;
import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.ui.ChatActivity;

import fr.cryptohash.Digest;
import fr.cryptohash.MD5;

/**
 * @author davidelizondo
 *
 */
public class FileSystemUtil {

    private static Digest digestInstance;

    /**
     * Calculates md5 for a given string
     * 
     * Now we are using MD5 provided by sphlib 3.0 - http://www.saphir2.com/sphlib/
     * 
     * "Sphlib is an opensource implementation of many cryptographic hash functions, in C and in 
     * Java. The code has been optimized for speed, and, in practice, the Java version turns out 
     * to be faster than what the standard JRE from Sun/Oracle offers"
     */
    public static String md5(String in) {
        if (digestInstance == null) {
            digestInstance = new MD5();
        } else {
            digestInstance.reset();
        }

        return DigestUtils.md5Hex(digestInstance.digest(in.getBytes()));
    }

    public static File getLocalFile(Context context, URL remoteUrl) {
        File storeDir = context.getFilesDir();
        return new File(storeDir, remoteUrl.getFile());
    }

    public static InputStream fetchRemoteInputStream(String strUrl)
            throws IOException, MalformedURLException {
        URL url = new URL(strUrl);
        return url.openStream();
    }

    public static InputStream openFileStream(Uri url)
            throws FileNotFoundException {
        return MessageMeApplication.getInstance().getContentResolver()
                .openInputStream(url);
    }

    /**
     * Remove the file extension from a filename, that may include a path.
     * 
     * e.g. /path/to/myfile.jpg -> /path/to/myfile 
     */
    public static String removeExtension(String filename) {
        if (filename == null) {
            return null;
        }

        int index = indexOfExtension(filename);

        if (index == -1) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
    }

    /**
     * Return the file extension from a filename, including the "."
     * 
     * e.g. /path/to/myfile.jpg -> .jpg
     */
    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }

        int index = indexOfExtension(filename);

        if (index == -1) {
            // If file doesn't have an extension we should return
            // an empty string instead of the entire name
            return "";
        } else {
            return filename.substring(index);
        }
    }

    private static final char EXTENSION_SEPARATOR = '.';

    private static final char DIRECTORY_SEPARATOR = '/';

    public static int indexOfExtension(String filename) {

        if (filename == null) {
            return -1;
        }

        // Check that no directory separator appears after the 
        // EXTENSION_SEPARATOR
        int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);

        int lastDirSeparator = filename.lastIndexOf(DIRECTORY_SEPARATOR);

        if (lastDirSeparator > extensionPos) {
            LogIt.w(FileSystemUtil.class,
                    "A directory separator appears after the file extension, assuming there is no file extension");
            return -1;
        }

        return extensionPos;
    }

    /**
     * Close an InputStream, logging but ignoring any errors.
     */
    public static void closeInputStream(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ioe) {
                LogIt.w(FileSystemUtil.class, "Error closing InputStream",
                        stream, ioe);
            }
        }
    }

    /**
     * Close an OutputStream, logging but ignoring any errors.
     */
    public static void closeOutputStream(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ioe) {
                LogIt.w(FileSystemUtil.class, "Error closing OutputStream",
                        stream, ioe);
            }
        }
    }

    /**
     * Close a FileChannel, logging but ignoring any errors.
     */
    public static void closeChannel(FileChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ioe) {
                LogIt.w(FileSystemUtil.class, "Error closing InputStream",
                        channel, ioe);
            }
        }
    }

    /**
     * Recursive method that runs inside all the 
     * sub folders and deletes all the files of each one
     */
    public static void deleteFiles(File dir) throws IOException {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles())
                deleteFiles(file);
        }
        if (!dir.delete()) {
            throw new FileNotFoundException("Failed to delete file: " + dir);
        } else {
            LogIt.d(FileSystemUtil.class, "Deleted file: ", dir);
        }
    }

    /**
     * Retrieves a file from either the gallery, external local source e.g. Dropbox
     * and Picasa
     */
    public static String getMediaContentFromIntent(Context context,
            Intent data, boolean isMediaMessage) throws MalformedURLException,
            IOException {

        // Result of "Choose Existing" photo
        Uri selectedImageUri = data.getData();

        if (selectedImageUri != null) {
            if (selectedImageUri.getScheme().equals("file")) {
                // Image was selected from a local source, 
                // e.g. Dropbox, file manager, etc.

                return selectedImageUri.getPath();
            } else {
                String[] projection = { MediaStore.Images.Media.DATA,
                        MediaColumns.DISPLAY_NAME };

                Cursor cursor = context.getContentResolver().query(
                        selectedImageUri, projection, null, null, null);

                if (cursor != null) {
                    cursor.moveToFirst();

                    int columnIndexData = cursor
                            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                    if (columnIndexData != -1) {
                        String contentPath = cursor.getString(columnIndexData);
                        File localFile = contentPath != null ? new File(
                                contentPath) : null;

                        if (localFile != null && localFile.exists()) {
                            // It's a media file picked from the local gallery
                            return localFile.getAbsolutePath();
                        } else {
                            // It's a media file picked from any external source         
                            columnIndexData = cursor
                                    .getColumnIndex(MediaColumns.DISPLAY_NAME);
                            String originalFileName = cursor
                                    .getString(columnIndexData);
                            String fileExtension = getExtension(originalFileName);

                            if (TextUtils.isEmpty(fileExtension)) {
                                LogIt.w(FileSystemUtil.class, "original file",
                                        originalFileName,
                                        " doesn't have any ext");

                                if (!isMediaMessage) {
                                    fileExtension = MessageMeConstants.PHOTO_MESSAGE_EXTENSION;
                                }
                            }

                            String randomFileName = new StringBuilder()
                                    .append(StringUtil.getRandomFilename())
                                    .append(fileExtension).toString();

                            File destFile = null;
                            if (isMediaMessage) {
                                // Download a file into the Media Folder
                                if (localFile != null) {
                                    destFile = downloadFileFromURL(
                                            randomFileName,
                                            ImageLoader.getAppCacheDirMedia(),
                                            contentPath);
                                } else {
                                    destFile = downloadFileFromUri(
                                            randomFileName,
                                            ImageLoader.getAppCacheDirMedia(),
                                            selectedImageUri);
                                }
                            } else {
                                // Download a file into the Profiles folder
                                if (localFile != null) {
                                    destFile = downloadFileFromURL(
                                            randomFileName,
                                            ImageLoader
                                                    .getAppCacheDirProfiles(),
                                            contentPath);
                                } else {
                                    destFile = downloadFileFromUri(
                                            randomFileName,
                                            ImageLoader
                                                    .getAppCacheDirProfiles(),
                                            selectedImageUri);
                                }
                            }

                            return destFile.getAbsolutePath();
                        }
                    } else {
                        LogIt.w(ChatActivity.class, "Invalid column",
                                MediaStore.Images.Media.DATA,
                                "in cursor with URI", selectedImageUri);
                        return null;
                    }
                } else {
                    LogIt.w(ChatActivity.class,
                            "Error getting Cursor with URI", selectedImageUri);
                    return null;
                }
            }
        } else {
            LogIt.w(FileSystemUtil.class, "No URI in data to load image from",
                    selectedImageUri);
            return null;
        }
    }

    public static File downloadFileFromUri(String fileName, File dir, Uri url)
            throws FileNotFoundException, IOException {

        File f = null;
        f = new File(dir, fileName);
        OutputStream os = new FileOutputStream(f);
        InputStream is = FileSystemUtil.openFileStream(url);
        IOUtil.copyAndClose(is, os);

        return f;
    }

    public static File downloadFileFromURL(String fileName, File dir, String url)
            throws FileNotFoundException, IOException {

        File f = new File(dir, fileName);
        OutputStream os = new FileOutputStream(f);
        InputStream is = FileSystemUtil.fetchRemoteInputStream(url);
        IOUtil.copyAndClose(is, os);

        return f;
    }

    /**
     * Checks is a external storage device is mounted
     */
    public static boolean isWritableSDCardPresent() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                LogIt.d(FileSystemUtil.class,
                        "SD Card found and ready to be written");
                return true;
            } else {
                LogIt.w(FileSystemUtil.class,
                        "SD Card found, NO write premissions");
                return false;
            }
        } else {
            LogIt.d(FileSystemUtil.class, "SD Card not present");
            return false;
        }
    }

    /**
     * Returns the amount of Megabytes available in local storage
     */
    public static long getAvailableStorageSpace() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
                .getPath());
        long bytesAvailable = (long) stat.getBlockSize()
                * (long) stat.getAvailableBlocks();
        long megsAvailable = (long) (bytesAvailable / (1024.f * 1024.f));
        LogIt.d(FileSystemUtil.class, "Available space in Megabytes ",
                megsAvailable);
        return megsAvailable;
    }
    
    
    /**
     * Returns the local storage path of the given URI
     */
    public static String getRealPathFromURI(Uri contentUri, Context context) {
        if (contentUri.getScheme().equals("file")) {
            return contentUri.getPath();
        }else{
            String[] proj = new String[] { MediaStore.Images.Media.DATA };
            Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }        
    }
}