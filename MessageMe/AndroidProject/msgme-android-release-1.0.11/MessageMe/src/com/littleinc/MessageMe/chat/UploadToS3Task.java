package com.littleinc.MessageMe.chat;

import java.io.File;

import org.restlet.resource.ResourceException;

import android.content.Intent;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.VideoMessage;
import com.littleinc.MessageMe.bo.VoiceMessage;
import com.littleinc.MessageMe.protocol.Messages.PBMessageVideo;
import com.littleinc.MessageMe.util.BatchTask;
import com.littleinc.MessageMe.util.FileSystemUtil;

/**
 * Upload the media for a provided DurableCommand to Amazon S3.
 * 
 * Only the media for Voice and Video messages is uploaded to S3.
 */
public class UploadToS3Task implements Runnable {

    DurableCommand mDurableCmd;

    MessagingService mMessagingService;

    public UploadToS3Task(DurableCommand durableCmd,
            MessagingService messagingService) {
        mDurableCmd = durableCmd;
        mMessagingService = messagingService;
    }

    @Override
    public void run() {
        final File mediaFile = mDurableCmd.getMediaToUpload();
        LogIt.d(UploadToS3Task.class, "Uploading media file", mediaFile);

        try {
            ChatManager.updateS3ClientCredentials();

            MediaManager mediaManager = MediaManager.getInstance();

            if (mDurableCmd.doesMediaNeedUpload()) {

                String mediaKeyFilename = FileSystemUtil
                        .removeExtension(mediaFile.getName());

                // Horrible hack until we can move video messages into internal
                // storage.
                if (mDurableCmd.getMessageType() == IMessageType.VIDEO) {

                    PBMessageVideo messageVideo = mDurableCmd
                            .getPBCommandEnvelop().getMessageNew()
                            .getMessageEnvelope().getVideo();

                    String mediaKey = messageVideo.getVideoKey();

                    mediaKeyFilename = FileSystemUtil.removeExtension(mediaKey
                            .substring(mediaKey.lastIndexOf("/") + 1));

                    LogIt.d(this, "Video message media key", mediaKeyFilename);
                }

                String s3MediaPath = mediaManager.uploadMessage(mediaFile,
                        mediaKeyFilename, mDurableCmd.getMessageType());

                onMediaUploadCompleted(mediaFile, s3MediaPath);
            }

            // Check if a thumbnail needs to be uploaded (only applies to 
            // video messages)
            if (mDurableCmd.doesThumbNeedUpload()) {

                final File thumbFile = mDurableCmd.getThumbToUpload();
                LogIt.d(UploadToS3Task.class, "Uploading thumb file", thumbFile);

                String thumbKey = FileSystemUtil.removeExtension(thumbFile
                        .getName());

                // Pretend this is part of a photo message so it gets uploaded
                // to the correct place
                String s3ThumbPath = mediaManager.uploadMessage(thumbFile,
                        thumbKey, IMessageType.PHOTO);

                onThumbUploadCompleted(thumbFile, s3ThumbPath);
            }

            if (mDurableCmd.isReadyToSendPBCommand()) {
                DurableCommandSender.sendPBMessageToServer(mDurableCmd,
                        mMessagingService);
            } else {
                // By now we expect all the media to have been uploaded, so
                // we should be ready to send the PBCommand
                LogIt.w(UploadToS3Task.class, "Not ready to send PBCommand yet");
            }

            // Any media and thumbnails are now both uploaded
            mDurableCmd.setUploading(false);

        } catch (ResourceException e) {
            // Ensure recoverable errors retry uploads
            mDurableCmd.setUploading(false);

            ChatManager.checkForAmazonException(e);
            LogIt.w(UploadToS3Task.class, "ResourceException uploading to S3",
                    mediaFile, e.getMessage());
        } catch (Exception e) {

            // S3 uploads never throw an IOException - they will come through
            // as Amazon exceptions
            if (ChatManager.checkAWSTimeOffsetError(e)) {

                LogIt.e(UploadToS3Task.class,
                        "Can't upload file, date/hour of the device is different from the Server");

                Intent intent = new Intent();
                intent.setAction(MessageMeConstants.INTENT_NOTIFY_TABS_MESSAGE);
                intent.putExtra(
                        MessageMeConstants.EXTRA_TITLE,
                        MessageMeApplication.getInstance().getString(
                                R.string.aws_time_offset_error_title));

                intent.putExtra(
                        MessageMeConstants.EXTRA_DESCRIPTION,
                        MessageMeApplication.getInstance().getString(
                                R.string.aws_time_offset_error_message));

                mMessagingService.sendBroadcast(intent);

                DurableCommandSender.handleUnrecoverableError(e, mediaFile,
                        mDurableCmd);

            } else if (ChatManager.checkForAmazonException(e)) {
                // Ensure recoverable errors retry uploads
                mDurableCmd.setUploading(false);

                LogIt.w(UploadToS3Task.class,
                        "Amazon Exception uploading media", mediaFile,
                        e.getMessage());

            } else {
                DurableCommandSender.handleUnrecoverableError(e, mediaFile,
                        mDurableCmd);
            }
        } catch (OutOfMemoryError e) {
            DurableCommandSender.handleUnrecoverableError(e, mediaFile,
                    mDurableCmd);
        }
    }

    private void onMediaUploadCompleted(File originalFile, String mediaKey) {

        IMessage msg = mDurableCmd.getLocalMessage();

        if (msg instanceof VoiceMessage) {
            LogIt.d(UploadToS3Task.class, "S3 media uploaded for VoiceMessage",
                    originalFile, mediaKey);

            final VoiceMessage voiceMsg = (VoiceMessage) msg;
            voiceMsg.setSoundKey(mediaKey);

            new BatchTask() {

                @Override
                public void work() {
                    voiceMsg.save(false);

                    // Push the updated PBCommandEnvelope into our DurableCommand
                    mDurableCmd.setPBCommandEnvelop(voiceMsg.serialize());
                }
            };
        } else if (msg instanceof VideoMessage) {
            // All the VideoMessage fields are already setup correctly
            LogIt.d(UploadToS3Task.class, "S3 media uploaded for VideoMessage",
                    originalFile, mediaKey);
        } else {
            LogIt.w(UploadToS3Task.class,
                    "S3 onMediaUploadCompleted called for unexpected message type",
                    originalFile, mediaKey, mDurableCmd.getMessageType(),
                    mDurableCmd.getLocalMessage().getId());
            return;
        }

        // Update the DurableCommand to indicate the media upload has
        // completed successfully
        mDurableCmd.setMediaUploaded();

        // Push that change to disk as well
        DurableCommandSender.writeCommandToDisk(mDurableCmd);
    }

    private void onThumbUploadCompleted(File originalFile, String mediaKey) {

        IMessage msg = mDurableCmd.getLocalMessage();

        if (msg instanceof VideoMessage) {
            LogIt.d(UploadToS3Task.class, "S3 thumb uploaded for VideoMessage",
                    originalFile, mediaKey);
        } else {
            LogIt.w(UploadToS3Task.class,
                    "S3 onThumbUploadCompleted called for unexpected message type",
                    originalFile, mediaKey, mDurableCmd.getMessageType(),
                    mDurableCmd.getLocalMessage().getId());
            return;
        }

        // Update the DurableCommand to indicate the thumb upload has
        // completed successfully
        mDurableCmd.setThumbUploaded();

        // Push that change to disk as well
        DurableCommandSender.writeCommandToDisk(mDurableCmd);
    }
}