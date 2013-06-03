package com.littleinc.MessageMe.chat;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;

import com.coredroid.util.LogIt;
import com.littleinc.MessageMe.bo.IMessage;
import com.littleinc.MessageMe.bo.IMessageType;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.util.MessageUtil;

/**
 * Important commands should be sent in a durable way so the client will
 * retry sending them if they don't get through first time, e.g. all 
 * new messages, delete message, and read receipts.
 *
 * Transient information like PRESENCE commands are not sent in a durable way. 
 */
public class DurableCommand implements Serializable {

    /**
     * Specify the "version" of this class.  If its member fields change
     * then this value should be updated to ensure that newer code 
     * cannot deserialize an old serialized object (as the 
     * deserialization code will not know how to read the old object any
     * more).
     */
    private static final long serialVersionUID = 1L;

    /**
     * This uniquely identifies every command. 
     */
    private final long clientId;

    private IMessageType mMessageType;

    private final Calendar mDateCreated;

    public enum MediaUploadDestination {
        S3, IMAGE_SERVER
    };

    /**
     * Optional field for any media File that needs to be uploaded, and where 
     * it needs to be uploaded to. 
     */
    private File mMediaToUpload = null;

    private MediaUploadDestination mMediaUploadDestination;

    private boolean mIsMediaUploaded = false;

    /**
     * Optional field for any thumb File to upload.  This only applies for
     * thumbnails that are directly uploaded to S3 (e.g. for video messages).  
     * The image server creates thumbnails for any images uploaded directly 
     * to it.
     */
    private File mThumbToUpload = null;

    private boolean mIsThumbUploaded = false;

    private long localMessageId;

    private IMessageType localMessageType;

    /**
     * Message classes do not implement Serializable so the transient modifier
     * will help us to exclude this field
     */
    private transient IMessage localMessage;

    private PBCommandEnvelope mPBCommandEnvelope;

    private boolean mIsUploading = false;

    /**
     * Timestamp for the last time we tried to send this message.
     */
    private Calendar mPreviousSendAttemptTime = null;

    public DurableCommand(IMessage message, File mediaToUpload,
            File thumbToUpload) {
        this(message);

        // At the moment only voice and video messages use this constructor, 
        // and all their media goes to S3
        mMediaUploadDestination = MediaUploadDestination.S3;

        mMessageType = message.getType();

        mMediaToUpload = mediaToUpload;
        mThumbToUpload = thumbToUpload;
        LogIt.d(this, "mMediaToUpload", mMediaToUpload);
        LogIt.d(this, "mThumbToUpload", mThumbToUpload);
    }

    /**
     * Any uploads that go through the image server do not need to provide a
     * thumbnail as the image server provides the thumbnail.
     */
    public DurableCommand(PBCommandEnvelope envelope, File mediaToUpload,
            IMessageType messageType) {
        this(envelope);

        // At the moment only SingleImageMessages objects use this constructor,
        // and all their media goes to S3
        mMediaUploadDestination = MediaUploadDestination.IMAGE_SERVER;

        mMessageType = messageType;

        mMediaToUpload = mediaToUpload;
        LogIt.d(this, "mMediaToUpload", mMediaToUpload);
    }

    /**
     * Any uploads that go through the image server do not need to provide a
     * thumbnail as the image server provides the thumbnail.
     */
    public DurableCommand(IMessage message, File mediaToUpload) {
        this(message.serialize(), mediaToUpload, message.getType());
        localMessage = message;
    }

    public DurableCommand(PBCommandEnvelope pbCommand) {
        clientId = pbCommand.getClientID();

        mPBCommandEnvelope = pbCommand;
        mDateCreated = Calendar.getInstance();
    }

    public DurableCommand(IMessage message) {
        this(message.serialize());
        localMessage = message;
    }

    public IMessage getLocalMessage() {
        return localMessage;
    }

    public long getClientId() {
        return clientId;
    }

    public File getMediaToUpload() {
        return mMediaToUpload;
    }

    public File getThumbToUpload() {
        return mThumbToUpload;
    }

    public void setPBCommandEnvelop(PBCommandEnvelope pbCommandEnvelope) {
        mPBCommandEnvelope = pbCommandEnvelope;
    }

    public PBCommandEnvelope getPBCommandEnvelop() {
        return mPBCommandEnvelope;
    }

    /**
     * Only applicable for MESSAGE_NEW commands.
     */
    public IMessageType getMessageType() {
        return mMessageType;
    }

    public MediaUploadDestination getMediaUploadDestination() {
        return mMediaUploadDestination;
    }

    public void updateLastSentTime() {
        LogIt.d(this, "Update last sent time to now");
        mPreviousSendAttemptTime = Calendar.getInstance();
    }

    public void setMediaUploaded() {
        mIsMediaUploaded = true;
    }

    boolean doesMediaNeedUpload() {
        if (mMediaToUpload == null) {
            LogIt.d(this, "No media for upload");
            return false;
        } else {
            LogIt.d(this, "Has media been uploaded?", mIsMediaUploaded);
            return !mIsMediaUploaded;
        }
    }

    public void setThumbUploaded() {
        mIsThumbUploaded = true;
    }

    boolean doesThumbNeedUpload() {
        if (mThumbToUpload == null) {
            LogIt.d(this, "No thumb for upload");
            return false;
        } else {
            LogIt.d(this, "Has thumb been uploaded?", mIsThumbUploaded);
            return !mIsThumbUploaded;
        }
    }

    public boolean getIsUploading() {
        return mIsUploading;
    }
    
    public void setUploading(boolean isUploading) {
        mIsUploading = isUploading;
    }

    /**
     * Is the media file for this DurableCommand ready to be sent to the 
     * server?
     */
    public boolean isReadyToUploadMedia() {

        if (mIsUploading) {
            LogIt.d(this, "Media is still uploading, so just wait",
                    getClientId());
            return false;
        } else if (doesMediaNeedUpload()) {
            LogIt.d(this, "Ready to upload media", getClientId());
            return true;
        } else {
            LogIt.d(this, "No media file needs to be uploaded", getClientId());
            return false;
        }
    }

    /**
     * Is the thumbnail for this DurableCommand media ready to be sent to the 
     * server?
     */
    public boolean isReadyToUploadThumb() {

        if (mIsUploading) {
            LogIt.d(this, "Media is still uploading, so just wait",
                    getClientId());
            return false;
        } else if (doesThumbNeedUpload()) {
            LogIt.d(this, "Ready to upload thumb", getClientId());
            return true;
        } else {
            LogIt.d(this, "No thumb needs to be uploaded", getClientId());
            return false;
        }
    }

    /**
     * Is the PBCommandEnvelope for this DurableCommand ready to be sent to 
     * the server?
     */
    public boolean isReadyToSendPBCommand() {

        if (doesMediaNeedUpload() || doesThumbNeedUpload()) {
            LogIt.d(this, "Media or thumb still needs upload", getClientId());
            return false;
        } else {
            if (mPreviousSendAttemptTime == null) {
                LogIt.d(this,
                        "Ready to send (never tried to send this DurableCommand before)",
                        getClientId());
                return true;
            } else {
                // Subtract the retry interval from the current time.  We do this
                // instead of adding to mPreviousSendAttemptTime as we don't want
                // to modify mPreviousSendAttemptTime.
                Calendar compareTime = Calendar.getInstance();
                compareTime.add(Calendar.MILLISECOND,
                        -DurableCommandSender.CMD_RETRY_INTERVAL);

                if (mPreviousSendAttemptTime.after(compareTime)) {
                    LogIt.d(this,
                            "Not ready to send as too soon since previous send attempt",
                            getClientId());
                    return false;
                } else {
                    LogIt.d(this, "Ready to send (retry interval has passed)",
                            getClientId());
                    return true;
                }
            }
        }
    }

    /**
     * Custom method to implement Serializable, for writing out objects
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {

        // IMessage can't be serialized so just store the message id and type
        if (localMessage != null) {
            localMessageId = localMessage.getId();
            localMessageType = localMessage.getType();
        }

        // The default serialization method will write out all the Serializable
        // fields in this DurableCommand, including all primitives and the Date
        // object.
        oos.defaultWriteObject();

        // Now handle objects that do not implement Serializable
        oos.writeObject(mPBCommandEnvelope);
    }

    /**
     * Custom method to implement Serializable, for reading objects
     */
    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {

        // The default serialization method will read in all the Serializable
        // fields in this DurableCommand.
        ois.defaultReadObject();

        // Checks if a message should be loaded from the database
        if (localMessageId != 0 && localMessageType != null) {
            LogIt.d(DurableCommand.class, "Loading message from database",
                    localMessageId, localMessageType);

            localMessage = MessageUtil
                    .newMessageInstanceByType(localMessageType);
            localMessage.setId(localMessageId);
            localMessage.load();
        }

        Object pbCommand = ois.readObject();

        if (pbCommand instanceof PBCommandEnvelope) {
            mPBCommandEnvelope = (PBCommandEnvelope) pbCommand;
        } else {
            LogIt.w(this,
                    "Error reading PBCommandEnvelope from DurableCommand file",
                    clientId);
        }
    }

    @Override
    public int hashCode() {
        if (this == null) {
            return 0;
        } else {
            return Long.valueOf(this.getClientId()).hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj.getClass() != getClass()) {
            return false;
        }

        DurableCommand cmd = (DurableCommand) obj;

        return (this.getClientId() == cmd.getClientId());
    }

    @Override
    public String toString() {
        return localMessageId + ", " + localMessageType + ", " + clientId
                + ", " + mDateCreated + ", " + mPBCommandEnvelope;
    }
}
