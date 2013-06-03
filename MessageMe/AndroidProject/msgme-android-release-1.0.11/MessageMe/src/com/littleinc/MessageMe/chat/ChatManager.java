package com.littleinc.MessageMe.chat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.restlet.resource.ResourceException;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.coredroid.util.BackgroundTask;
import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeExternalAPI;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.ContactType;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.MessageMeAppPreferences;
import com.littleinc.MessageMe.bo.Room;
import com.littleinc.MessageMe.bo.User;

/**
 * The chat manager keeps track of references to all current chats. It will not
 * hold any references in memory on its own so it is necessary to keep a
 * reference to the chat object itself. 
 */
public class ChatManager {

    private MediaManager mediaManager = MediaManager.getInstance();

    private static HashMap<Long, Chat> uidChats = new HashMap<Long, Chat>();

    public ChatManager(ChatConnection connection) {
        updateS3ClientCredentials();
    }

    public static void updateS3ClientCredentials() {
        MessageMeAppPreferences appPreferences = MessageMeApplication
                .getPreferences();

        MediaManager mediaManager = MediaManager.getInstance();

        mediaManager.updateS3ClientCredentials(
                appPreferences.getAwsAccessKey(),
                appPreferences.getAwsSecretKey(),
                appPreferences.getAwsSessionToken());
    }

    public static Chat getChat(long channelID) {
        return uidChats.get(channelID);
    }

    /**
     * Creates a chat between user and local user and return it
     * 
     * @param user the User this chat is with
     * @param listener the ChatMessageListener which will listen for new 
     *                  messages from this chat
     */
    public Chat createChat(User user) {
        Chat chat = createChat(user, true);
        return chat;
    }

    /**
     * Create a chat and return it
     * 
     * @param user the User this chat is with
     * @param createdLocally true if chat was created from local user, false 
     *                       if it was created by user
     */
    private Chat createChat(User user, boolean createdLocally) {
        Chat chat = new Chat(user);
        uidChats.put(user.getContactId(), chat);

        LogIt.d(this, "Created chat with user", user.getContactId(),
                user.getDisplayName());

        return chat;
    }

    public Chat creatRoom(Room group, boolean createdLocally) {
        Chat chat = new Chat(group);
        uidChats.put(group.getRoomId(), chat);

        LogIt.d(this, "Created chat with group", group.getRoomId(),
                group.getDisplayName());

        return chat;
    }

    public Chat createRoom(Room group) {
        Chat chat = new Chat(group);

        uidChats.put(group.getRoomId(), chat);

        LogIt.d(this, "Created chat group", group.getRoomId(),
                group.getDisplayName());

        return chat;
    }

    /**
     * Returns a chat associated to a specific user
     * 
     * @return existing chat between user and local user
     */
    public Chat getChat(long contactId, int contactType) {
        for (Chat chat : uidChats.values()) {
            if (chat.isChatRoom()
                    && ContactType.GROUP.getValue() == contactType
                    && contactId == chat.getChatId()) {
                return chat;
            } else if (chat.isChatRoom()
                    && ContactType.USER.getValue() == contactType
                    && contactId == chat.getChatId()) {
                return chat;
            }
        }

        return null;
    }

    public void retrieveFile(final String key, final IMessageType messageType,
            final MediaDownloadListener listener) {

        new BackgroundTask() {

            MediaDownloadListener mListener = listener;

            File file = null;

            @Override
            public void work() {
                try {
                    LogIt.d(ChatManager.class, "Downloading file", key,
                            messageType);
                    updateS3ClientCredentials();
                    file = mediaManager.downloadFile(key, messageType);
                } catch (AmazonServiceException e) {
                    fail(e);
                } catch (IOException e) {
                    fail(e);
                }
            }

            @Override
            public void done() {
                if (!failed()) {
                    LogIt.d(ChatManager.class, "Downloaded file", file);
                    mListener.onDownloadCompleted(file.toString());
                } else {
                    LogIt.w(ChatManager.class, "Error downloading file", file);
                    mListener.onDownloadError(getExceptionMessage());
                }
            }
        };
    }

    /**
     * Check whether the provided Throwable was an Exception from Amazon, and
     * if so then notify our Amazon client management code that it occurred 
     * (e.g. so it can refresh its S3 token).
     * 
     * @return whether the provided Throwable was an Amazon Exception.
     */
    public static boolean checkForAmazonException(Throwable ex) {
        boolean wasAmazonException = false;

        if (ex instanceof AmazonServiceException) {
            AmazonServiceException amazonEx = (AmazonServiceException) ex;

            if (amazonEx.getStatusCode() == 400
                    || amazonEx.getStatusCode() == 403) {

                wasAmazonException = true;
                notifyInvalidAmazonToken();
            }
        }

        return wasAmazonException;
    }

    public static boolean checkAWSTimeOffsetError(Throwable ex) {
        if (ex instanceof AmazonServiceException) {
            AmazonServiceException amazonEx = (AmazonServiceException) ex;

            if (amazonEx.getErrorCode().equals(
                    MessageMeExternalAPI.AWS_TIME_OFFSET_ERROR)) {
                return true;
            }
        }

        return false;
    }

    public static void notifyInvalidAmazonToken() {

        LocalBroadcastManager manager = LocalBroadcastManager
                .getInstance(MessageMeApplication.getInstance());

        LogIt.d(ChatManager.class, "Sending broadcast:",
                MessagingService.INVALID_TOKEN);
        manager.sendBroadcast(new Intent(MessagingService.INVALID_TOKEN));
    }

    public void uploadProfileAsset(final Context context,
            final String fileName, final int maxSize, final String folder,
            final UploadS3Listener listener) {
        new BackgroundTask() {

            String fileKey;

            boolean invalidToken = false;

            UploadS3Listener mListener = listener;

            @Override
            public void work() {
                LogIt.d(ChatManager.class, "Uploading file: ", fileName);

                try {
                    updateS3ClientCredentials();

                    File file = new File(fileName);
                    fileKey = mediaManager.uploadFile(file.getAbsoluteFile(),
                            folder, maxSize);
                } catch (IOException e) {
                    fail(context.getString(R.string.network_error_title),
                            context.getString(R.string.network_error));
                } catch (ResourceException e) {
                    fail(context.getString(R.string.network_error_title),
                            context.getString(R.string.network_error));
                } catch (AmazonServiceException e) {
                    LogIt.e(ChatManager.class, "AmazonServiceException",
                            e.getCause(), e.toString());

                    // Check for the AWS RequestTimeTooSkewed error
                    if (e.getErrorCode().equals(
                            MessageMeExternalAPI.AWS_TIME_OFFSET_ERROR)) {

                        fail(context
                                .getString(R.string.aws_time_offset_error_title),
                                context.getString(R.string.aws_time_offset_error_message));

                        LogIt.w(ChatManager.class,
                                "Can't upload file to AWS, time difference too big between the client and the server");
                    } else if (e.getStatusCode() == 400
                            || e.getStatusCode() == 403) {
                        LogIt.d(ChatManager.class, "InvalidToken");
                        invalidToken = true;

                        fail(context
                                .getString(R.string.image_upload_fail_title),
                                context.getString(R.string.image_upload_fail));
                    }

                } catch (AmazonClientException e) {
                    LogIt.d(ChatManager.class, "AmazonClientException",
                            e.getCause(), e.toString());

                    if ((e.getCause() != null)
                            && (e.getCause() instanceof IOException)) {
                        fail(context.getString(R.string.network_error_title),
                                context.getString(R.string.network_error));
                    } else {
                        fail(context
                                .getString(R.string.image_upload_fail_title),
                                context.getString(R.string.image_upload_fail));
                    }
                } catch (Exception e) {
                    LogIt.e(ChatManager.class, e, "Failed to upload photo",
                            e.getMessage());
                    fail(context.getString(R.string.generic_error_title),
                            context.getString(R.string.unexpected_error));
                }
            }

            @Override
            public void done() {
                if (!failed()) {
                    mListener.onUploadCompleted(fileKey);
                } else {
                    mListener.onUploadError(getExceptionTitle(),
                            getExceptionMessage());
                    LocalBroadcastManager manager = LocalBroadcastManager
                            .getInstance(MessageMeApplication.getInstance());
                    if (invalidToken) {
                        LogIt.d(this, "Sending broadcast:",
                                MessagingService.INVALID_TOKEN);
                        manager.sendBroadcast(new Intent(
                                MessagingService.INVALID_TOKEN));
                    }
                }
            }
        };
    }

    /**
     * Checks if the given chat is already loaded and if it is adds the given user
     * if is not present already
     */
    public static boolean addParticipantIfChatExists(long channelId, User user) {
        Chat chat = getChat(channelId);

        if (chat != null) {
            LogIt.d(ChatManager.class, "Adding participant", user.getUserId(),
                    "to chat", channelId);
            return chat.addParticipand(user);
        }

        return false;
    }
}