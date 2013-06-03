package com.littleinc.MessageMe.chat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

import android.text.TextUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.coredroid.util.IOUtil;
import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConfig;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.MessageMeAppPreferences;
import com.littleinc.MessageMe.bo.User;
import com.littleinc.MessageMe.ui.ChatActivity;
import com.littleinc.MessageMe.util.ImageUtil;
import com.littleinc.MessageMe.util.NetUtil;
import com.littleinc.MessageMe.util.StringUtil;

/**
 * Uploads and downloads media files to Amazon S3
 */
public class MediaManager {

    public static final String GROUP_FOLDER = "/g";

    public static final String PROFILE_FOLDER = "/p";

    public static final String MESSAGE_FOLDER = "/m";

    public static final String AMAZON_S3_BUCKET = MessageMeApplication
            .getTargetConfig().get(MessageMeConfig.KEY_S3_BUCKET);

    public static final String AMAZON_S3_DIRECT_ACCESS_URL = MessageMeApplication
            .getTargetConfig().get(MessageMeConfig.KEY_S3_DIRECT_ACCESS_URL);

    private static MediaManager instance;

    /**
     * S3 client to use with an access token for uploading media.
     */
    private AmazonS3Client s3Client;

    private BasicAWSCredentials anonymousCredentials = null;

    /**
     * Anonymous S3 client to use for downloads.  Using null credentials forces
     * the client to be anonymous, which means it can only access globally 
     * readable resources.  The advantage is that its credentials can never
     * expire, so token expiry will not stop downloads working.
     */
    private AmazonS3Client s3DownloadClient = new AmazonS3Client(
            anonymousCredentials);

    private MediaManager() {
    }

    public static MediaManager getInstance() {
        if (instance == null) {
            instance = new MediaManager();
        }

        return instance;
    }

    /**
     * Creates or updates the AmazonS3Client if the parameters are valid
     */
    public void updateS3ClientCredentials(String accessKey, String secretKey,
            String sessionToken) {

        MessageMeAppPreferences appPrefs = MessageMeApplication
                .getPreferences();

        if ((s3Client == null) && !TextUtils.isEmpty(accessKey)
                && !TextUtils.isEmpty(secretKey)
                && !TextUtils.isEmpty(sessionToken)) {
            LogIt.d(this, "Create AmazonS3Client");
            createS3Client(accessKey, secretKey, sessionToken);

        } else if (isAWSCredentialValid(appPrefs.getAwsAccessKey(), accessKey)
                && isAWSCredentialValid(appPrefs.getAwsSecretKey(), secretKey)
                && isAWSCredentialValid(appPrefs.getAwsSessionToken(),
                        sessionToken)) {

            LogIt.i(this, "New credentials - update AmazonS3Client");
            createS3Client(accessKey, secretKey, sessionToken);

            // Remember when this token expires
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR_OF_DAY,
                    MessageMeConstants.AWS_TOKEN_REFRESH_INTERVAL);
            appPrefs.setAwsExpirationDate(expirationDate.getTime());

            // Save the new credentials
            appPrefs.setAwsAccessKey(accessKey);
            appPrefs.setAwsSecretKey(secretKey);
            appPrefs.setAwsSessionToken(sessionToken);
        } else {
            LogIt.d(this, "Do not update AmazonS3Client");
            return;
        }
    }

    /**
     * Creates a new instance of the AmazonS3Client
     */
    private void createS3Client(String accessKey, String secretKey,
            String sessionToken) {
        BasicSessionCredentials credentials = new BasicSessionCredentials(
                accessKey, secretKey, sessionToken);
        s3Client = new AmazonS3Client(credentials);

        // Client is 1 hour (3600 seconds) ahead of AWS 
        // http://mobile.awsblog.com/post/Tx2KKPVXE69XJAO/Managing-Device-Time-with-the-AWS-Mobile-SDKs
        //s3Client.setTimeOffset(3600);
    }

    /**
     * Upload the provided file to Amazon S3.
     * 
     * @param file the file to upload. 
     * @param mediaKey the file name to use for storage, e.g. IwV8BN.mp3
     * @param messageType the type of the message which the media is being 
     *                    uploaded for.
     */
    public String uploadMessage(File file, String mediaKey,
            IMessageType messageType) throws AmazonServiceException,
            AmazonClientException {
        // Get the path to store the media file in S3 at, e.g.
        //   u/885307588739207168/m/6VvVA2.mp3
        String fileKey = generateObjectName(MESSAGE_FOLDER, mediaKey,
                messageType);

        LogIt.d(this, "Uploading file with name", fileKey);
        uploadAmazonFile(fileKey, file);

        return fileKey;
    }

    /**
     * This method upload a given file to the Amazon S3 service and also
     * resize the image if it is surpassing the max size
     * 
     * @param file File to be uploaded to the S3 service
     * @param folder Folder to be used in the final imageKey
     * @param bounding Image max size
     */
    public String uploadFile(File file, String folder, int bounding)
            throws AmazonServiceException, AmazonClientException, IOException {
        String fileKey = null;
        File fileToUpload = null;
        User user = MessageMeApplication.getCurrentUser();

        // Check if the file exists in the app media cache directory
        if (ImageUtil.isInAppMediaCache(file)
                || ImageUtil.isInAppProfileCache(file)) {
            fileToUpload = file;
            fileKey = "u/" + user.getUserId() + folder + "/" + file.getName();
        } else {
            String photoName = StringUtil.getRandomFilename();

            fileKey = "u/" + user.getUserId() + folder + "/" + photoName
                    + MessageMeConstants.PHOTO_MESSAGE_EXTENSION;

            // Create a new file into the app pictures folder 
            fileToUpload = ImageUtil.getFile(fileKey);
            fileToUpload.createNewFile();
        }

        // Resize image if is needed to match bounding
        if (!ImageUtil.resizeImageToFile(file, fileToUpload, bounding, true)) {
            LogIt.w(ChatActivity.class, "Failed to resize image", file);
        }

        uploadAmazonFile(fileKey, fileToUpload);

        return fileKey;
    }

    /**
     * Upload the provided file to Amazon S3, at the location indicated by
     * the fileKey, which is the path the file should be stored at, e.g:
     *   u/885307588739207168/m/IwV8BN.mp3
     */
    public void uploadAmazonFile(String fileKey, File file)
            throws AmazonServiceException, AmazonClientException {
        LogIt.d(this, "Upload file to Amazon", fileKey, file);
        PutObjectRequest por = new PutObjectRequest(
                MediaManager.AMAZON_S3_BUCKET, fileKey, file);
        s3Client.putObject(por);
    }

    /**
     * Generate the base file path used to store the file at in S3, e.g. 
     *   u/885307588739207168/m/0bcl38.jpg
     *   
     * Note that any image media uploaded through the image server will
     * not be saved at that path location.  The image server generates
     * different resolution versions of the image and saves them with
     * different file names.
     */
    public static String generateObjectName(String folder, String imageKey,
            IMessageType messageType) {
        User user = MessageMeApplication.getCurrentUser();

        String fileName = "";
        switch (messageType) {
        case VOICE:
            fileName = "u/" + user.getUserId() + folder + "/" + imageKey
                    + MessageMeConstants.VOICE_MESSAGE_EXTENSION;
            break;
        case VIDEO:
            fileName = "u/" + user.getUserId() + folder + "/" + imageKey
                    + MessageMeConstants.VIDEO_MESSAGE_EXTENSION;
            break;
        case PHOTO:
            fileName = "u/" + user.getUserId() + folder + "/" + imageKey
                    + MessageMeConstants.PHOTO_MESSAGE_EXTENSION;
            break;
        case DOODLE:
            fileName = "u/" + user.getUserId() + folder + "/" + imageKey
                    + MessageMeConstants.DOODLE_MESSAGE_EXTENSION;
            break;
        case DOODLE_PIC:
            fileName = "u/" + user.getUserId() + folder + "/" + imageKey
                    + MessageMeConstants.DOODLE_MESSAGE_EXTENSION;
            break;
        default:
            // No other message types have media to upload, so this method 
            // should never be called with them
            LogIt.w(MediaManager.class, "Unexpected message type", messageType);
            break;
        }

        LogIt.d(MediaManager.class, "Generated S3 file path", fileName);

        return fileName;
    }

    /**
     * Generate the base file path without add a file extension, 
     * used to store the file at in S3, e.g. 
     *   u/885307588739207168/m/0bcl38
     * 
     * Note that any image media uploaded through the image server will
     * not be saved at that path location.  The image server generates
     * different resolution versions of the image and saves them with
     * different file names.
     */
    public static String generateObjectName(String folder, String imageKey) {
        User user = MessageMeApplication.getCurrentUser();
        return "u/" + user.getUserId() + folder + "/" + imageKey;
    }

    /** 
     * Get a URL (as a String) suitable for accessing the provided S3 media key
     * from, e.g. for displaying images or playing voice or video files 
     * directly through a media player.
     * 
     * @param mediaKey u/885307588739207168/m/oKq29N.mp4
     */
    public static String getS3FileURL(String mediaKey) {
        StringBuilder url = new StringBuilder(AMAZON_S3_DIRECT_ACCESS_URL);
        url.append(mediaKey);
        return url.toString();
    }

    public File downloadFile(String imageKey) throws IOException,
            AmazonClientException {
        File outputFile = ImageUtil.getFile(imageKey);

        return downloadFile(imageKey, outputFile);
    }

    /**
     * Download the file imageKey from S3, and save it into the specified 
     * outputFile.  
     */
    public File downloadFile(String imageKey, File outputFile)
            throws IOException, AmazonClientException {
        S3Object obj = null;
        OutputStream outputStream = null;

        if (imageKey == null) {
            LogIt.w(this, "Ignore attempt to download null file");
        } else {
            // Avoid trying to download from S3 when we don't have
            // a connection as the AmazonHttpClient prints loads of
            // warnings to Logcat
            if (NetUtil.checkInternetConnection()) {
                LogIt.d(this, "Downloading file", imageKey, outputFile);

                obj = s3DownloadClient.getObject(MediaManager.AMAZON_S3_BUCKET,
                        imageKey);

                outputStream = new BufferedOutputStream(new FileOutputStream(
                        outputFile));
                IOUtil.copyAndClose(obj.getObjectContent(), outputStream);
            } else {
                throw new IOException("No network connection");
            }
        }

        return outputFile;
    }

    public File downloadFile(String imageKey, IMessageType messageType)
            throws AmazonServiceException, IOException {
        S3Object obj = null;
        OutputStream outputStream = null;

        // Avoid trying to download from S3 when we don't have
        // a connection as the AmazonHttpClient prints loads of
        // warnings to Logcat
        if (NetUtil.checkInternetConnection()) {
            LogIt.d(this, "Downloading file", imageKey, messageType);
            obj = s3DownloadClient.getObject(MediaManager.AMAZON_S3_BUCKET,
                    imageKey);

            File file = ImageUtil.getFile(imageKey);

            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            IOUtil.copyAndClose(obj.getObjectContent(), outputStream);

            return file;
        } else {
            throw new IOException("No network connection");
        }
    }

    /**
     * Returns true if the received AWS credential is not empty and is 
     * different from the stored AWS credential.
     */
    private boolean isAWSCredentialValid(String storedAWSCredential,
            String receivedAWSCredential) {

        if (TextUtils.isEmpty(receivedAWSCredential)) {
            // This shouldn't ever happen
            LogIt.w(this, "Empty AWS credential", receivedAWSCredential);
            return false;
        } else if (receivedAWSCredential.equals(storedAWSCredential)) {
            LogIt.d(this, "AWS credential has not changed",
                    receivedAWSCredential);
            return false;
        }

        return true;
    }
}