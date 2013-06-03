package com.littleinc.MessageMe.bo;

import java.io.File;

import android.graphics.BitmapFactory.Options;

import com.coredroid.util.Dimension;
import com.coredroid.util.LogIt;
import com.google.protobuf.InvalidProtocolBufferException;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.chat.DurableCommand;
import com.littleinc.MessageMe.chat.MessagingService;
import com.littleinc.MessageMe.protocol.Commands.PBCommandEnvelope;
import com.littleinc.MessageMe.protocol.Objects.PBImage;
import com.littleinc.MessageMe.protocol.Objects.PBImage.ImageType;
import com.littleinc.MessageMe.protocol.Objects.PBImageBundle;
import com.littleinc.MessageMe.util.FileSystemUtil;
import com.littleinc.MessageMe.util.ImageUtil;

/**
 * Common implementation for messages that are represented by a single image.
 * 
 * This class defines most of the common methods and database fields required 
 * by the {@link IMessage} interface.  We cannot implement the IMessage 
 * interface here, as we need each of the subclasses to define their own
 * database tables.  A few if the interface methods need to be defined as
 * abstract so the common code can call them here.
 * 
 * The database column IDs need to be unique, so each subclass should define
 * a suffix (e.g. {@link DoodleMessage#DB_COLUMN_ALIAS_SUFFIX} for use in 
 * the database operations.
 */
public abstract class SingleImageMessage extends AbstractMessage {

    public static final String IMAGE_KEY_COLUMN = "imageKey";

    /** The key for the thumbnail image */
    public static final String THUMB_KEY_COLUMN = "thumbKey";

    /** 
     * The PBImageBundle associated with the image.  We need to store this in
     * case the message gets forwarded later. 
     */
    public static final String IMAGE_BUNDLE_COLUMN = "imgBundle";

    @DatabaseField(columnName = SingleImageMessage.IMAGE_KEY_COLUMN, dataType = DataType.STRING)
    protected String imageKey;

    @DatabaseField(columnName = SingleImageMessage.THUMB_KEY_COLUMN, dataType = DataType.STRING)
    protected String thumbKey;

    /**
     * Using DataType.SERIALIZABLE might be preferable, but to be sure there
     * can't be any versioning issues we are going to use the Protobuf 
     * parsing and serializing methods instead of the OrmLite ones.
     */
    @DatabaseField(columnName = SingleImageMessage.IMAGE_BUNDLE_COLUMN, dataType = DataType.BYTE_ARRAY)
    protected byte[] pbImageBundleAsBytes;

    /**
     * A PBImageBundle is presented instead of its representation in a 
     * byte array.
     */
    protected PBImageBundle pbImageBundle = null;

    public SingleImageMessage(Message message) {
        super(message);
    }

    // Common methods for accessing image keys, thumbnails and PBImageBundles

    public String getImageKey() {
        return imageKey;
    }

    /**
     * 
     * For sent messages this will be the full path of the local cached 
     * file, e.g.
     *   /storage/sdcard0/Android/data/com.littleinc.MessageMe/files/Pictures/sYfWr1.jpg
     */
    public void setImageKey(String imageKey) {
        //        LogIt.d(this, "Set image key", imageKey);
        this.imageKey = imageKey;
    }

    public String getThumbKey() {
        if ((thumbKey == null) || (thumbKey.length() == 0)) {
            LogIt.d(this, "No thumbnail, returning normal image key instead");
            return imageKey;
        } else {
            return thumbKey;
        }
    }

    public void setThumbKey(String thumbKey) {
        this.thumbKey = thumbKey;
    }

    public PBImageBundle getImageBundle() {

        if ((pbImageBundle == null) && (pbImageBundleAsBytes != null)) {

            try {
                pbImageBundle = PBImageBundle.parseFrom(pbImageBundleAsBytes);
            } catch (InvalidProtocolBufferException e) {
                LogIt.e(this, e, "Error parsing PBImageBundle from database");
            }
        }

        return pbImageBundle;
    }

    public void setImageBundle(PBImageBundle imageBundle) {

        this.pbImageBundle = imageBundle;

        if (pbImageBundle == null) {
            // This happens when retrieving the message before its image has
            // been uploaded to the image server
            LogIt.d(this, "Setting pbImageBundle to null");
        } else {
            pbImageBundleAsBytes = pbImageBundle.toByteArray();
        }
    }

    /**
     * Only to be used by database methods.  Other callers should use
     * {@link #setImageBundle(PBImageBundle)}.
     */
    public void setImageBundleAsBytes(byte[] imageBundleAsBytes) {

        this.pbImageBundleAsBytes = imageBundleAsBytes;
    }

    /**
     * Get the size of the thumbnail image for this message.
     */
    public Dimension getThumbnailSize() {

        Dimension dim = new Dimension();

        // Use local thumb dimensions if the image bundle is null or if sizes are 0
        if (getImageBundle() == null
                || (getImageBundle() != null
                        && getImageBundle().getThumbStandardResolution()
                                .getWidth() == 0 && getImageBundle()
                        .getThumbStandardResolution().getHeight() == 0)) {

            // Sent messages might not have been uploaded to the image server
            // yet, so they won't have an image bundle
            Options opts = ImageUtil
                    .getBitmapOptionsFromFile(getCachedThumbFile().toString());

            dim.setWidth(opts.outWidth);
            dim.setHeight(opts.outHeight);

            LogIt.d(this, "No image bundle yet, got size of image from file",
                    getCachedThumbFile(),
                    dim.getWidth() + "x" + dim.getHeight());
        } else {
            PBImage thumb = getImageBundle().getThumbStandardResolution();

            dim.setWidth(thumb.getWidth());
            dim.setHeight(thumb.getHeight());

            // LogIt.d(this, "Got size of image from bundle", dim.getWidth() + "x"
            //         + dim.getHeight());
        }

        return dim;
    }

    /**
     * Get the locally cached image file.  This should only be called after the
     * image key has been set. 
     */
    public File getCachedImageFile() {
        if (getImageKey() == null) {
            LogIt.w(SingleImageMessage.class, "No image key, return null file");
            return null;
        } else {
            File inAppFile = ImageUtil.getFile(getImageKey());
            return inAppFile.exists() ? inAppFile : new File(getImageKey());
        }
    }

    /**
     * Get the locally cached thumb file.  This should only be called after the
     * thumb key has been set. 
     */
    public File getCachedThumbFile() {
        if (getThumbKey() == null) {
            LogIt.w(this, "No thumb key, return null file");
            return null;
        } else {
            return ImageUtil.getFile(getThumbKey());
        }
    }

    public void send(MessagingService service) {
        LogIt.d(SingleImageMessage.class, "Send SingleImageMessage",
                message.getCommandId());

        DurableCommand durableCmd = new DurableCommand(this,
                getCachedImageFile());

        service.addToDurableSendQueue(durableCmd);
        service.notifyChatClient(MessageMeConstants.INTENT_NOTIFY_MESSAGE_LIST,
                getId(), getChannelId());
    }

    abstract public PBCommandEnvelope serialize();

    // Utility methods

    public void parseFrom(PBCommandEnvelope commandEnvelope,
            PBImageBundle pbImageBundle, String messageImageKey) {

        StringBuilder imageKey = null;
        StringBuilder thumbKey = null;

        if (pbImageBundle == null) {
            LogIt.d(this, "No image bundle in message", message.getType());
        } else {
            imageKey = SingleImageMessage.getImageKeyFromBundle(pbImageBundle);
            thumbKey = SingleImageMessage.getThumbKeyFromBundle(pbImageBundle);
        }

        if (imageKey == null) {
            // We don't expect this to happen in normal usage.  It may occur
            // when testing with accounts that have used the old version of
            // the Android app that did not have proper image support.
            LogIt.w(this, "No base key or image bundle key, use main key",
                    messageImageKey);
            imageKey = new StringBuilder(messageImageKey);
        }

        if (thumbKey == null) {
            // This is a normal condition, e.g. when the standard image is
            // very small then no thumbnail will be generated
            LogIt.d(this, "No thumbnail for this image");
        }

        if (pbImageBundle != null) {
            setImageBundle(pbImageBundle);
        }

        if (imageKey != null) {
            setImageKey(imageKey.toString());
        }

        if (thumbKey != null) {
            setThumbKey(thumbKey.toString());
        }
    }

    /**
     * Utility method to get the standard resolution image key from a 
     * PBImageBundle.
     */
    public static StringBuilder getImageKeyFromBundle(
            PBImageBundle pbImageBundle) {
        StringBuilder key = null;

        if (pbImageBundle.hasNormalStandardResolution()) {

            PBImage normalImage = pbImageBundle.getNormalStandardResolution();

            // The logic here is copied from the iOS client
            if (pbImageBundle.hasBaseKey() && normalImage.hasType()) {
                key = new StringBuilder(
                        FileSystemUtil.removeExtension(pbImageBundle
                                .getBaseKey()));
                key.append("_");
                key.append(normalImage.getType().getNumber());
                key.append(FileSystemUtil.getExtension(pbImageBundle
                        .getBaseKey()));
            } else {
                // getKey() on the individual PBImage objects is deprecated
                // so this code should not be called any more
                LogIt.w(SingleImageMessage.class,
                        "Use image key from normal image (deprecated)");
                key = new StringBuilder(normalImage.getKey());
            }

            //            LogIt.d(SingleImageMessage.class, "PBImageBundle image", key,
            //                    normalImage.getType(), normalImage.getWidth() + " x "
            //                            + normalImage.getHeight());
        }

        return key;
    }

    /**
     * Utility method to get the standard resolution thumbnail key from a 
     * PBImageBundle.
     */
    static StringBuilder getThumbKeyFromBundle(PBImageBundle pbImageBundle) {
        StringBuilder key = null;

        if (pbImageBundle.hasThumbStandardResolution()) {

            PBImage thumbImage = pbImageBundle.getThumbStandardResolution();

            // The logic here is copied from the iOS client
            if (pbImageBundle.hasBaseKey() && thumbImage.hasType()) {
                key = new StringBuilder(
                        FileSystemUtil.removeExtension(pbImageBundle
                                .getBaseKey()));
                key.append("_");
                key.append(thumbImage.getType().getNumber());
                key.append(FileSystemUtil.getExtension(pbImageBundle
                        .getBaseKey()));
            } else {
                // getKey() on the individual PBImage objects is deprecated
                // so this code should not be called any more
                LogIt.w(SingleImageMessage.class,
                        "Use image key from thumb image (deprecated)");
                key = new StringBuilder(thumbImage.getKey());
            }

            //            LogIt.d(SingleImageMessage.class, "PBImageBundle thumb", key,
            //                    thumbImage.getType(), thumbImage.getWidth() + " x "
            //                            + thumbImage.getHeight());
        }

        return key;
    }

    /**
     * This method will transform a normal key into a thumbKey
     * u/XXXXXXX/m/xxxxx.jpg or u/XXXXXXX/m/xxxxx_0.jpg to u/XXXXXXX/m/xxxxx_2.jpg
     */
    public static String generateThumbkeyFromImageKey(String imageKey) {

        String detailKeySuffix = new StringBuilder().append("_")
                .append(ImageType.NORMAL_STANDARD_VALUE).toString();

        if (imageKey.contains(detailKeySuffix)) {
            String thumbKeySuffix = new StringBuilder().append("_")
                    .append(ImageType.THUMB_STANDARD_VALUE).toString();
            return imageKey.replace(detailKeySuffix, thumbKeySuffix);
        } else {
            return new StringBuilder(FileSystemUtil.removeExtension(imageKey))
                    .append("_").append(ImageType.THUMB_STANDARD_VALUE)
                    .append(FileSystemUtil.getExtension(imageKey)).toString();
        }
    }

    /**
     * This method will transform a thumb key into a detailKey 
     * u/XXXXXXX/m/xxxxx.jpg or u/XXXXXXX/m/xxxxx_2.jpg to u/XXXXXXX/m/xxxxx_0.jpg
     */
    public static String generateDetailkeyFromThumbKey(String thumbKey) {

        String thumbKeySuffix = new StringBuilder().append("_")
                .append(ImageType.THUMB_STANDARD_VALUE).toString();

        if (thumbKey.contains(thumbKeySuffix)) {
            String detailKeySuffix = new StringBuilder().append("_")
                    .append(ImageType.NORMAL_STANDARD_VALUE).toString();
            return thumbKey.replace(thumbKeySuffix, detailKeySuffix);
        } else {
            return new StringBuilder(FileSystemUtil.removeExtension(thumbKey))
                    .append("_").append(ImageType.NORMAL_STANDARD_VALUE)
                    .append(FileSystemUtil.getExtension(thumbKey)).toString();
        }
    }

    /**
     * This method will remove the suffix in a given key e.g 
     * u/XXXXXXX/m/xxxxx_2.jpg or u/XXXXXXX/m/xxxxx.jpg
     */
    public static String removeSuffix(String imageKey) {
        int i = imageKey.indexOf("_");
        String suffix = imageKey.substring(i, i + 2);
        return imageKey.replace(suffix, "");
    }
}