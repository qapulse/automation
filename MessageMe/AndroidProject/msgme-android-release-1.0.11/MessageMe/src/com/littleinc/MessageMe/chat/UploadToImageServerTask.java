package com.littleinc.MessageMe.chat;

import java.io.File;
import java.io.IOException;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.bo.SingleImageMessage;
import com.littleinc.MessageMe.net.RestfulClient;
import com.littleinc.MessageMe.protocol.Objects.PBImageBundle;
import com.littleinc.MessageMe.util.BatchTask;
import com.littleinc.MessageMe.util.ImageLoader;
import com.littleinc.MessageMe.util.ImageUtil;
import com.littleinc.MessageMe.util.StringUtil;

/**
 * Upload the media file for a provided DurableCommand to the image server.
 * 
 * Photo, Doodle and DoodlePic messages are uploaded to the MessageMe  
 * image server instead of directly to S3.  This is so our image server
 * can generate the thumbnails for them.
 */
public class UploadToImageServerTask implements Runnable {

    DurableCommand mDurableCmd;

    MessagingService mMessagingService;

    public UploadToImageServerTask(DurableCommand durableCmd,
            MessagingService messagingService) {
        mDurableCmd = durableCmd;
        mMessagingService = messagingService;
    }

    @Override
    public void run() {
        PBImageBundle pbImageBundle;
        final File originalFile = mDurableCmd.getMediaToUpload();
        final IMessageType messageType = mDurableCmd.getMessageType();

        if ((originalFile == null) || !originalFile.exists()) {
            LogIt.w(UploadToImageServerTask.class, "No image file to upload",
                    originalFile);
            DurableCommandSender.handleUnrecoverableError(null, originalFile,
                    mDurableCmd);
            return;
        }
        
        if (mDurableCmd.getLocalMessage() == null) {
            // This can happen in rare edge cases, so make sure we 
            // don't crash if the local message is null
            LogIt.d(UploadToImageServerTask.class, "Uploading image",
                    originalFile, messageType);
        } else {            
            LogIt.d(UploadToImageServerTask.class, "Uploading image",
                    originalFile, messageType, mDurableCmd.getLocalMessage()
                            .getId());
        }

        File newFileToUpload = originalFile;

        String fileKey = null;

        try {
            // Checks if the file exists in the app media cache directory
            if (ImageUtil.isInAppMediaCache(originalFile)) {
                LogIt.d(UploadToImageServerTask.class,
                        "File is already cached", originalFile);
                fileKey = MediaManager
                        .generateObjectName(MediaManager.MESSAGE_FOLDER,
                                SingleImageMessage.removeSuffix(originalFile
                                        .getName()));
            } else {
                fileKey = MediaManager.generateObjectName(
                        MediaManager.MESSAGE_FOLDER,
                        StringUtil.getRandomFilename(), messageType);

                // Create a new file into the app pictures folder 
                newFileToUpload = ImageUtil.getFile(fileKey);
                newFileToUpload.createNewFile();
            }

            LogIt.d(UploadToImageServerTask.class, "Uploading file with name",
                    newFileToUpload, fileKey);

            // The image server returns the PBImageBundle containing
            // the thumbnail sizes
            pbImageBundle = RestfulClient.getInstance().uploadPhoto(
                    newFileToUpload.getAbsolutePath(), fileKey);

            if (ImageUtil.isInAppMediaCache(newFileToUpload)) {
                // Find out what the detailed image file name is (this usually
                // just has "_0.jpg" on the end)
                String imgKey = SingleImageMessage.getImageKeyFromBundle(
                        pbImageBundle).toString();

                // Rename the detailed image to match the image key name so we 
                // don't have to download it again
                File detailImgInMediaCache = new File(
                        newFileToUpload.getParent(), imgKey.substring(imgKey
                                .lastIndexOf(File.separator)));

                if (newFileToUpload.renameTo(detailImgInMediaCache)) {
                    LogIt.d(this,
                            "Renamed image file to match file name from image server",
                            newFileToUpload, detailImgInMediaCache);
                } else {
                    // As a consequence the user will have to download the detailed 
                    // image from S3 instead of being able to use the local version
                    LogIt.w(this,
                            "Failed to rename image file to match file name from image server",
                            newFileToUpload, detailImgInMediaCache);
                }
            } else {
                LogIt.w(UploadToImageServerTask.class,
                        "Uploaded image is not in our media cache",
                        newFileToUpload);
            }

            if (ImageUtil.isInAppMediaCache(originalFile)) {
                LogIt.d(UploadToImageServerTask.class,
                        "No need to delete original file as is already in our internal cache",
                        originalFile);
            } else {
                // Always try to delete any temporary image file that we created on
                // the external disk, as that location is not protected (we need to 
                // put images there as that's the only place the Camera can write to).

                if (originalFile.getParent() == ImageLoader
                        .getAppCacheDirMedia().toString()) {
                    // Only deletes the originalFile if it's stored in the application directory
                    // Otherwise, it's a file selected from another source e.g. Gallery
                    if (originalFile.delete()) {
                        LogIt.d(UploadToImageServerTask.class,
                                "Successfully deleted original file from external SD card",
                                originalFile);
                    } else {
                        LogIt.w(UploadToImageServerTask.class,
                                "Failed to delete original file from external SD card",
                                originalFile);
                    }
                } else {
                    LogIt.d(this,
                            "This is a image from the gallery or local source, NOT deleting");
                }
            }

            // The pbImageBundle contains the new paths for the image
            onUploadCompleted(pbImageBundle);
            
            // Uploading has now finished
            mDurableCmd.setUploading(false);

        } catch (IOException e) {
            // Ensure recoverable errors retry uploads
            mDurableCmd.setUploading(false);

            ChatManager.checkForAmazonException(e);
            LogIt.w(UploadToImageServerTask.class,
                    "IOException uploading photo", newFileToUpload, fileKey,
                    e.getMessage());
        } catch (ResourceException e) {
            // Ensure recoverable errors retry uploads
            mDurableCmd.setUploading(false);

            if (!ChatManager.checkForAmazonException(e)) {
                // The Restlet library swallows the Amazon exception and
                // instead throws a ResourceException, so we need to check
                // for those 400 and 403 errors and refresh our Amazon S3 
                // token.
                Status status = e.getStatus();

                if (status != null) {
                    LogIt.d(UploadToImageServerTask.class,
                            "ResourceException status code", status.getCode());
                    
                    if ((status.getCode() == 400) || (status.getCode() == 403)) {
                        ChatManager.notifyInvalidAmazonToken();
                    } else if ((status.getCode() == 415)
                            || (status.getCode() == 422)) {
                        // This indicates the uploaded media is bad and we 
                        // should never try to upload it again
                        DurableCommandSender.handleUnrecoverableError(e, newFileToUpload,
                                mDurableCmd);
                    }
                }
            }
            
            LogIt.w(UploadToImageServerTask.class,
                    "ResourceException uploading photo", newFileToUpload,
                    fileKey, e.getMessage(), e.getStatus());
        } catch (OutOfMemoryError e) {
            DurableCommandSender.handleUnrecoverableError(e, newFileToUpload,
                    mDurableCmd);
        } catch (Exception e) {
            DurableCommandSender.handleUnrecoverableError(e, newFileToUpload,
                    mDurableCmd);
        }
    }

    private void onUploadCompleted(PBImageBundle pbImageBundle) {

        String imageKey = SingleImageMessage.getImageKeyFromBundle(
                pbImageBundle).toString();

        LogIt.i(UploadToImageServerTask.class, "Image server upload completed",
                imageKey);

        IMessage msg = mDurableCmd.getLocalMessage();

        if (msg != null && msg instanceof SingleImageMessage) {
            LogIt.d(UploadToImageServerTask.class,
                    "Update the SingleImageMessage for this message",
                    pbImageBundle.getBaseKey());

            // Retrieve the message and update it with the image bundle
            final SingleImageMessage singleImageMsg = (SingleImageMessage) msg;
            
            singleImageMsg.setImageKey(imageKey);
            singleImageMsg.setImageBundle(pbImageBundle);

            new BatchTask() {

                @Override
                public void work() {
                    singleImageMsg.save(false);

                    // Push the updated PBCommandEnvelope into our DurableCommand
                    mDurableCmd.setPBCommandEnvelop(singleImageMsg.serialize());

                    // Update the DurableCommand to indicate the image upload has
                    // completed successfully
                    mDurableCmd.setMediaUploaded();

                    // Push that change to disk as well
                    DurableCommandSender.writeCommandToDisk(mDurableCmd);

                    if (mDurableCmd.isReadyToSendPBCommand()) {
                        DurableCommandSender.sendPBMessageToServer(mDurableCmd,
                                mMessagingService);
                    } else {
                        // Image server uploads should never have any other media to upload
                        LogIt.w(UploadToImageServerTask.class,
                                "Unexpected that media needs to be uploaded, don't send PBCommand yet");
                    }
                }
            };
        } else {
            LogIt.w(UploadToImageServerTask.class,
                    "Unexpected condition in image server onUploadCompleted",
                    mDurableCmd.getMessageType(), msg);
            return;
        }
    }
}